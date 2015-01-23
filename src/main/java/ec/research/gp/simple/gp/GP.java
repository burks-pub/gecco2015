package ec.research.gp.simple.gp;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;

import ec.research.gp.simple.operators.PointMutation;
import ec.research.gp.simple.operators.Selection;
import ec.research.gp.simple.operators.TournamentSelection;
import ec.research.gp.simple.problem.Problem;
import ec.research.gp.simple.regression.nodes.R;
import ec.research.gp.simple.representation.Individual;
import ec.research.gp.simple.representation.Node;
import ec.research.gp.simple.util.Context;
import ec.research.gp.statistics.SimpleGPStatistics;
import ec.research.gp.statistics.Statistics;



/**
 * This is the basic implementation of tree-based GP using ramped half & half
 * tree generation. Other GP implementations should extend this.
 * 
 */
public class GP {
	// Holds the context for the run
	protected Context context;

	// Holds the population.
	protected Vector<Individual> population;

	// Holds the current generation number.
	protected int generation;

	// Holds the instance of the problem
	protected Problem problem;

	// Holds the best individual seen over the run
	protected Individual bestIndividual;

	// Holds the best fitness seen over the run
	protected double bestFitness;

	// Whether or not we've found the optimal fitness value.
	protected boolean foundOptimal;

	// Whether or not we should stop when we find an individual with optimal
	// fitness
	protected boolean doStopOnOptimal;

	// Lock for threading.
	private Object lock = new Object();

	// The selection scheme we're using for reproduction.
	protected Selection selection;

	// Whether we're stopping the run based on numGenerations or numEvaluations
	public static enum STOP_ON {
		GENERATIONS, EVALUATIONS
	};

	// Whether or not we're stopping based on generations (default) or
	// evaluations
	protected STOP_ON stopOn;

	// Holds the statistics
	protected Statistics statistics;

	// Holds the number of evaluations performed
	protected long numEvaluations;

	// Holds the evaluation number at which we last output fitness info (so we
	// don't keep doing it over and over)
	protected long lastFitnessOutput;

	// Whether or not we've logged the optimal fitness when stopping early
	protected boolean loggedOptimal;

	// Holds the average fitness of the current population
	protected double avgFitness;

	// Holds the average size of the individuals in the population
	protected double avgSize;

	// Holds the average depth of the individuals in the population
	protected double avgDepth;

	// Holds the most recent generation at which the max fitness ever increased
	protected long lastFitnessImprovementGen;

	// Holds the most recent eval number at which the max fitness ever increased
	protected long lastFitnessImprovementEval;

	// Log4j logger for any output messages.
	private static final Logger logger = Logger.getLogger(GP.class);

	/**
	 * Checks to see if termination criteria has been met.
	 * 
	 * @return true if termination criteria has been met, false otherwise.
	 */
	protected boolean doTerminate() {
		return (this.stopOn == STOP_ON.GENERATIONS && generation == context
				.getConfig().getNumGenerations() - 1)
				|| (this.stopOn == STOP_ON.EVALUATIONS && this.numEvaluations >= this.context
						.getConfig().getNumEvaluations())
				|| (doStopOnOptimal && foundOptimal);
	}

	/**
	 * Convenience method to add elites to the end of the population, if elitism
	 * is enabled.
	 * 
	 * @param newPopulation
	 *            the new, filled population up to popSize - numElites
	 * @param numElites
	 *            the number of elites to add
	 * @throws CloneNotSupportedException
	 */
	protected void addElites(Vector<Individual> newPopulation, int numElites)
			throws CloneNotSupportedException {
		// Sort the population if using elitism, then add the elites
		if (numElites > 0) {
			Collections.sort(this.population);

			// Now add the elites
			for (int i = 0; i < numElites; i++) {
				int eliteIndex = this.population.size() - 1 - i;
				newPopulation.add(this.population.get(eliteIndex).clone());
			}
		}
	}

	/**
	 * Create a new GP to run with the given Context and Problem.
	 * 
	 * @param c
	 *            the Context for the GP
	 * @throws Exception
	 */
	public GP(Context c) throws Exception {
		this.context = c;
		this.problem = this.context.getConfig().getProblem();
		this.problem.init(this.context);
		this.population = new Vector<Individual>();
		this.doStopOnOptimal = this.context.getConfig().stopOnOptimal();
		this.numEvaluations = 0;
		this.stopOn = this.context.getConfig().getStopOn();

		// Setup the selection scheme
		setupSelection();
	}

	/**
	 * Generates pop-size random individuals using the Koza ramped approach as
	 * described in "A Field Guide to Genetic Programming."
	 * 
	 */
	public void init() throws Exception {
		int popSize = context.getConfig().getPopSize();
		int half = popSize / 2;
		int uniqueRetries = 0;
		int maxUniqueRetries = context.getConfig().getMaxUniqueRetries();
		HashSet<String> expressions = new HashSet<String>();

		while (population.size() < popSize) {
			// Use the grow method for half, and the full method for the other
			// half.
			int mode = (population.size() < half ? 0 : 1);

			Individual individual = getRandomIndividual(mode);

			// Avoid duplicates until we run out of tries
			String expression = individual.toString();
			boolean isUnique = expressions.add(expression);

			// Allow duplicates if we've run out of retries.
			if (isUnique || uniqueRetries > maxUniqueRetries) {
				// Set the individual's ID and add the individual.
				individual.setId(0);
				population.add(individual);
			} else {
				uniqueRetries++;
			}
		}

		// Now that we have our population, initialize the stats object.
		try {
			this.statistics = new SimpleGPStatistics(this.population,
					this.context.getConfig());
			this.context.setStats(this.statistics);

		} catch (IOException e) {
			logger.fatal(e);
			System.exit(1);
		}

		// Evaluate the population
		evaluatePop();

		// Do the post-evaluation stats.
		if (stopOn.equals(STOP_ON.GENERATIONS)) {
			statistics.postEvaluationStats(this.generation);
		}

		// Post-generation stats.
		statistics.postGenerationStats(generation);
	}

	/**
	 * Convenience method to perform tournament selection on the parents and add
	 * up to two children to the new population using crossover.
	 * 
	 * @param tmpNewPop
	 *            placeholder where the offspring go
	 * @param fillSize
	 *            the number of individuals to be added to the layer during this
	 *            breed cycle.
	 * 
	 * @throws CloneNotSupportedException
	 */
	private void doCrossover(Vector<Individual> tmpNewPop, int fillSize)
			throws CloneNotSupportedException {

		// Get two parents with tournament selection
		int[] parents = selection.select(this.context, this.population);

		// Cross the individuals and add the child(ren) to the new population
		Individual[] children = this.population.get(parents[0]).crossover(
				this.population.get(parents[1]), this.context);

		// Set the children's IDs
		children[0].setId(this.generation);
		children[1].setId(this.generation);

		tmpNewPop.add(children[0]);

		// Add child 2 unless we're discarding it or we need
		// space for elites.
		if (!this.context.getConfig().doDiscardSecondChild()
				&& tmpNewPop.size() < fillSize) {
			tmpNewPop.add(children[1]);
		}
	}

	/**
	 * Convenience method to perform tournament selection on the parents and add
	 * a single child (replicated by a single parent) to the new population.
	 * 
	 * @param tmpNewPopulation
	 *            the placeholder where the offspring go
	 * @param fillSize
	 *            the number of individuals to be added to the layer during this
	 *            breed cycle.
	 * 
	 * @throws CloneNotSupportedException
	 */
	protected void doReplication(Vector<Individual> tmpNewPopulation,
			int fillSize) throws CloneNotSupportedException {
		// Don't even continue if the layer is too full.
		if (tmpNewPopulation.size() == fillSize) {
			return;
		}

		// Find a parent to replicate
		Individual parent = this.population.get(selection.selectOne(
				this.context, this.population));

		// Replicate the parent to get the child.
		Individual child = parent.replicate();

		// Set the child's ID.
		child.setId(this.generation);

		// Add the child to the layer.
		tmpNewPopulation.add(child);
	}

	/**
	 * Convenience method to setup the selection scheme. Defaults to
	 * TournamentSelection.
	 * 
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	private void setupSelection() throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		String selectionParam = this.context.getConfig().getParameter(
				"selection");

		// Try to setup the selection scheme as dictated by config.
		if (selectionParam != null) {
			@SuppressWarnings("rawtypes")
			Class selectionClass = Class.forName(selectionParam);
			this.selection = (Selection) selectionClass.newInstance();
		}

		// Otherwise, default to TournamentSelection
		else {
			this.selection = new TournamentSelection();
		}
	}

	/**
	 * Convenience method to do the mutation on a set of offspring individuals.
	 * 
	 * @param individuals
	 *            the individuals to (possibly) mutate
	 * 
	 * @param context
	 *            the {@link Context} for the run
	 * 
	 * @throws CloneNotSupportedException
	 */
	protected void doMutation(Collection<Individual> individuals,
			Context context) throws CloneNotSupportedException {
		for (Individual individual : individuals) {
			if (context.nextBool(context.getConfig().getMutationProbability())) {
				individual = PointMutation.mutate(individual, context);
				individual.setId(this.generation);
			}
		}
	}

	/**
	 * Convenience method to evaluate all the individuals of the population as
	 * well as track the best individual of the generation and overall.
	 * 
	 * @throws CloneNotSupportedException
	 */
	public void evaluatePop() {
		int numThreads = this.context.getConfig().getNumThreads();
		int chunkSize = this.population.size() / numThreads;
		int start = 0, end = 0;
		Thread[] threads = new Thread[numThreads];

		// Fire off all the evaluation threads
		for (int i = 0; i < numThreads; i++) {
			start = i * chunkSize;

			// Set the end index (account for uneven popSize/numThreads)
			end = (i == numThreads - 1) ? this.population.size()
					: (start + chunkSize); // subList end is exclusive so no -1

			EvalThread thread = new EvalThread(this.population.subList(start,
					end));
			threads[i] = new Thread(thread);
			threads[i].start();
		}

		// Join them all together
		for (int i = 0; i < numThreads; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				logger.error(e);
				System.exit(1);
			}
		}

		// Calculate some population averages
		calculateAverages();

		// Output generation fitness info to STDOUT every 100 generations
		if (this.generation % 100 == 0) {
			outputGenerationFitnessInfo();
		}
	}

	/**
	 * Performs one evolutionary step (generation).
	 * 
	 * @throws CloneNotSupportedException
	 */
	public void step() throws CloneNotSupportedException {
		// Don't continue if it's time to terminate.
		if (!doTerminate()) {
			// Pre-generation stats.
			statistics.preGenerationStats(generation);

			// Increment the generation number
			this.generation++;

			// Breed the new population
			breed();

			// Evaluate the population
			evaluatePop();

			// Do the post-evaluation stats (if running based on gens).
			if (this.stopOn.equals(STOP_ON.GENERATIONS)
					&& this.generation
							% this.context.getConfig().getStatCollectGens() == 0) {
				statistics.postEvaluationStats(this.generation);
			}

			// Post-generation stats.
			statistics.postGenerationStats(generation);
		}
	}

	/**
	 * Executes the main evolution loop, which continues until termination.
	 * 
	 * @throws CloneNotSupportedException
	 */
	public void evolve() throws CloneNotSupportedException {
		while (!doTerminate()) {
			step();
		}

		// Output the best solution found
		logger.info(String.format(
				"Best fitness overall=%s Hits=%s Total nodes=%s",
				this.bestFitness, this.bestIndividual.getHits(),
				this.bestIndividual.getNumNodes()));

		logger.info(String.format("Genotype of best individual: %s",
				this.bestIndividual.toString()));

		// Now do our post evolution statistics
		statistics.postEvolutionStats(generation);
	}

	/**
	 * Simply calculates the current average fitness, depth and size of the
	 * individuals in the population.
	 */
	public void calculateAverages() {
		this.avgFitness = 0;
		this.avgDepth = 0;
		this.avgSize = 0;

		for (Individual individual : this.population) {
			this.avgFitness += individual.getFitness();
			this.avgDepth += individual.getDepth();
			this.avgSize += individual.getNumNodes();
		}

		this.avgFitness /= this.population.size();
		this.avgDepth /= this.population.size();
		this.avgSize /= this.population.size();

		// Be sure to set the averages in the stats object.
		SimpleGPStatistics stats = (SimpleGPStatistics) this.statistics;

		stats.setFitnessInfo(this.bestFitness, this.avgFitness);
		stats.setNumEvaluations(this.numEvaluations);

		stats.setTreeStatsInfo(this.avgSize, this.avgDepth);
	}

	/**
	 * 
	 * @return the current average fitness of the individuals in the population.
	 */
	public double getAverageFitness() {
		return this.avgFitness;
	}

	/**
	 * 
	 * @return the current average depth of the individuals in the population.
	 */
	public double getAverageDepth() {
		return this.avgDepth;
	}

	/**
	 * 
	 * @return the current average size (number of nodes) of the individuals in
	 *         the population.
	 */
	public double getAverageSize() {
		return this.avgSize;
	}

	/**
	 * 
	 * @return the current total number of evaluations performed on individuals.
	 */
	public long getNumEvaluations() {
		return this.numEvaluations;
	}

	/**
	 * Displays the current generation fitness info.
	 */
	public void outputGenerationFitnessInfo() {
		logger.info(String.format(
				"Generation %s. Best Individual: Fitness=%s. Hits=%s."
						+ " Size=%s Depth=%s. Avg Fitness=%s Avg Size=%s"
						+ " Avg Depth=%s", this.generation, this.bestFitness,
				this.bestIndividual.getHits(),
				this.bestIndividual.getNumNodes(),
				this.bestIndividual.getDepth(), this.avgFitness, this.avgSize,
				this.avgDepth));
	}

	/**
	 * Helper to recursively generate the random expression, and add the nodes
	 * of that expression to the vector.
	 * 
	 * @param maxDepth
	 *            the maximum depth of the tree
	 * @param method
	 *            0 for grow and 1 for full
	 * @param nodeNumber
	 *            the number of the next node to be created
	 * @throws CloneNotSupportedException
	 */
	public Node generateRandomExpression(int maxDepth, int method,
			int nodeNumber, Individual individual)
			throws CloneNotSupportedException {

		List<Node> functionSet = context.getConfig().getFunctionSet();
		List<Node> terminalSet = context.getConfig().getTerminalSet();

		Node node;

		// Choose from terminal set if we've hit the max or if using the grow
		// method
		if (maxDepth == 0
				|| (method == 0 && context.randDouble() < (terminalSet.size() / (terminalSet
						.size() + functionSet.size())))) {

			// Pick a random terminal.
			int index = context.randBetween(0, terminalSet.size() - 1);

			// Assuming the terminal set is small enough!!
			node = terminalSet.get(index).clone();

			// Set the node number and increment nodeNumber
			node.setNodeNumber(individual.getNumNodes());

			// Set the ephemeral random constant value if that's what we got.
			if (node.getClass() == R.class) {
				((R) node).setValue(context.getRand().nextGaussian());
			}

			// Increment the number of nodes for the individual.
			individual.setNumNodes(individual.getNumNodes() + 1);
		} else {
			// Pick a random function
			int index = context.randBetween(0, functionSet.size() - 1);
			node = (Node) functionSet.get(index).clone();

			// Set the node's number and increment nodeNumber
			node.setNodeNumber(individual.getNumNodes());

			// Increment the number of nodes for the individual.
			individual.setNumNodes(individual.getNumNodes() + 1);

			// Now add children to the node, based on its arity
			for (int i = 0; i < node.getNumChildren(); i++) {
				Node child = generateRandomExpression(maxDepth - 1, method,
						nodeNumber, individual);
				node.setChild(i, child);
				child.setParent(node);
			}
		}

		return node;
	}

	/**
	 * Recursively generates a random tree using the Koza ramped approach as
	 * described in McPhee et al "A Field Guide to Genetic Programming."
	 * 
	 * @param maxDepth
	 *            the maximum depth of the tree
	 * @param method
	 *            0 for grow and 1 for full
	 * @return the root node of a randomly generated tree
	 * @throws CloneNotSupportedException
	 */
	public Node generateRandomExpression(int maxDepth, int method,
			Individual individual) throws CloneNotSupportedException {
		int nodeNumber = 0;
		return generateRandomExpression(maxDepth, method, nodeNumber,
				individual);
	}

	/**
	 * Generates a random individual. The generated tree is of depth range [1,
	 * max-depth].
	 * 
	 * @param mode
	 *            the method for generating the tree (0 for grow method,
	 *            otherwise full is used)
	 * @return the generated individual
	 * @throws CloneNotSupportedException
	 */
	public Individual getRandomIndividual(int mode)
			throws CloneNotSupportedException {
		Individual individual = new Individual();

		// Choose a depth between 0 and maxDepth (inclusive)
		int depth = context.randBetween(context.getConfig().getMinBuildDepth(),
				context.getConfig().getMaxBuildDepth());

		// Generate the random tree
		Node root = generateRandomExpression(depth, mode, 0, individual);

		// Set the random tree as the individual's genotype
		individual.setRoot(root);

		// Set the tree depth, so we don't have to calculate it later!
		individual.setDepth(depth);

		return individual;
	}

	/**
	 * 
	 * @return the population of individuals
	 */
	public Vector<Individual> getPopulation() {
		return population;
	}

	/**
	 * Breeds the parents to create a new generation.
	 * 
	 * @param parent1Index
	 *            the index of the first parent in the current population
	 * @param parent2Index
	 *            the index of the second parent in the current population
	 * @throws CloneNotSupportedException
	 */
	public void breed() throws CloneNotSupportedException {
		// Temporary holder for the new population
		Vector<Individual> tmpNewPopulation = new Vector<Individual>();
		int fillSize = context.getConfig().getPopSize()
				- context.getConfig().getNumElites();

		while (tmpNewPopulation.size() < fillSize) {
			// Figure out whether to do crossover or replication
			if (this.context.nextBool(this.context.getConfig()
					.getCrossProbability())) {
				doCrossover(tmpNewPopulation, fillSize);
			} else {
				doReplication(tmpNewPopulation, fillSize);
			}
		}

		// Probabilistically mutate the individuals
		doMutation(tmpNewPopulation, this.context);

		// Add elites if necessary
		addElites(tmpNewPopulation, context.getConfig().getNumElites());

		// Replace the old population with the new population.
		population.clear();
		population.addAll(tmpNewPopulation);
	}

	/**
	 * Convenience method to set IDs for each individual in the population if it
	 * doesn't already have one. This is used after breeding, because it's
	 * easier to do here than trying to pass along the generation and worry
	 * about incrementing correctly, etc.
	 */
	public void setIds() {
		for (Individual individual : population) {
			if (individual.getId() == null) {
				individual.setId(generation);
			}
		}
	}

	/**
	 * Helper class for multi-threading the fitness evaluations.
	 * 
	 */
	public class EvalThread implements Runnable {
		// Reference to the individuals to evaluate.
		private Collection<Individual> individuals;

		/**
		 * Creates a new {@link EvalThread} for evaluating the given subset of
		 * individuals
		 * 
		 * @param individuals
		 *            the subset of individuals to evaluate
		 */
		public EvalThread(Collection<Individual> individuals) {
			this.individuals = individuals;
		}

		@Override
		public void run() {
			for (Individual individual : this.individuals) {
				if (!individual.isEvaluated()) {
					problem.fitness(individual);

					synchronized (lock) {
						numEvaluations++;

						// See if the best individual of the generation is the
						// best overall
						if (individual.getFitness() > bestFitness
								|| bestIndividual == null) {
							bestFitness = individual.getFitness();
							lastFitnessImprovementGen = generation;
							lastFitnessImprovementEval = numEvaluations;

							try {
								bestIndividual = individual.clone();
							} catch (Exception e) {
								logger.error(e);
							}
						}

						// See if the best fitness is the optimal fitness (force
						// 1.0 as best)
						if (bestFitness == 1.0) {
							foundOptimal = true;
						}

						// Handle fitness stats logging
						if (stopOn.equals(STOP_ON.EVALUATIONS)) {
							// Output fitness every X evals
							boolean doLogFitness = (numEvaluations <= context
									.getConfig().getNumEvaluations()
									&& numEvaluations != lastFitnessOutput && (numEvaluations % context
									.getConfig().getStatCollectEvals()) == 0);

							boolean loggingOptimal = foundOptimal
									&& doStopOnOptimal && !loggedOptimal;

							if (doLogFitness || loggingOptimal) {
								// Calculate some population averages
								calculateAverages();

								((SimpleGPStatistics) statistics)
										.postEvaluationStats(generation);
								lastFitnessOutput = numEvaluations;

								if (loggingOptimal) {
									loggedOptimal = true;
								}
							}
						} else {
							calculateAverages();
						}
					}
				}
			}
		}
	}
}
