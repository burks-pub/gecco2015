package ec.research.gp.pareto;

import java.util.Vector;

import org.apache.log4j.Logger;

import ec.research.gp.simple.gp.GP;
import ec.research.gp.simple.operators.RandomMatingSelection;
import ec.research.gp.simple.representation.Individual;
import ec.research.gp.simple.util.Context;



/**
 * GP implementation based on the Schmidt and Lipson's Age-Fitness Pareto
 * Optimization approach.
 * 
 * This implementation uses the pareto optimization idea with genetic marker
 * (AKA tag) density as one of the objectives to enforce exploring different
 * rooted tree structures.
 * 
 */
public class ParetoGP extends GP {
	// Log4J logger for any output messages.
	private static final Logger logger = Logger.getLogger(ParetoGP.class);

	// Method to use for creating the random individual each generation.
	protected int randMethod = 0;

	// Holds the current objectives for this run
	protected OBJECTIVES objectives;

	// The diversity utils object for tagging trees
	protected DiversityUtils diversityUtils;

	// The most recent generation that we changed the tag level
	protected long lastTagLevelChangeGen;

	// The different types of objectives we can use for the pareto front.
	public static enum OBJECTIVES {
		AGE_FITNESS, AGE_DENSITY, AGE_DENSITY_FITNESS, DENSITY_FITNESS
	};

	/**
	 * Convenience method to do the random mating.
	 * 
	 * @param population
	 *            the new population to which to add the offspring
	 * 
	 * @param fillSize
	 *            the target size of the new population.
	 * @throws CloneNotSupportedException
	 */
	private void doRandomMating(Vector<Individual> population, int fillSize)
			throws CloneNotSupportedException {
		// Randomly select the parents
		int[] parents = this.selection.select(this.context, this.population);

		// Cross the parents and add the child(ren) to the new population.
		Individual[] children = this.population.get(parents[0]).crossover(
				this.population.get(parents[1]), this.context);

		// Set the children's IDs
		children[0].setId(this.generation);
		children[1].setId(this.generation);

		// Add the 1st child to the population
		population.add(children[0]);

		// Add the 2nd child if possible
		if (population.size() < fillSize
				&& !context.getConfig().doDiscardSecondChild()) {
			population.add(children[1]);
		}
	}

	/**
	 * Convenience method to setup the objectives to use.
	 */
	private void setupObjectives() {
		String objs = this.context.getConfig().getObjectives();

		// Default to density and fitness if not in config.
		if (objs == null) {
			this.objectives = OBJECTIVES.DENSITY_FITNESS;
		}

		// Otherwise, set it
		else {
			for (OBJECTIVES objectives : OBJECTIVES.values()) {
				if (objs.equals(objectives.toString())) {
					this.objectives = objectives;
				}
			}

			// Make sure we got something legit.
			if (this.objectives == null) {
				logger.error("No supported objectives named " + objs
						+ ". Defaulting to DENSITY_FITNESS");
				this.objectives = OBJECTIVES.DENSITY_FITNESS;
			}
		}
	}

	/**
	 * Convenience method to perform tournament selection on the parents and add
	 * a single child (replicated by a single parent) to the new population.
	 * 
	 * @param tmpNewPopulation
	 *            the place-holder where the offspring go
	 * 
	 * @param fillSize
	 *            the number of individuals to be added to the layer during this
	 *            breed cycle.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Override
	protected void doReplication(Vector<Individual> tmpNewPopulation,
			int fillSize) throws CloneNotSupportedException {
		// Don't even continue if the layer is too full.
		if (tmpNewPopulation.size() >= fillSize) {
			return;
		}

		// Find a parent to replicate
		Individual parent = this.population.get(this.selection.selectOne(
				this.context, this.population));

		// Replicate the parent to get the child.
		Individual child = parent.replicate();

		// Set the child's ID.
		child.setId(this.generation);

		// Add the child to the pop.
		tmpNewPopulation.add(child);
	}

	/**
	 * Create a new {@link ParetoGP}.
	 * 
	 * @param c
	 *            the context for this run
	 * @throws Exception
	 */
	public ParetoGP(Context c) throws Exception {
		super(c);

		// Be extra careful to use random mating selection!
		if (!(this.selection instanceof RandomMatingSelection)) {
			this.selection = new RandomMatingSelection();
		}
	}

	/**
	 * Same as GP.init() but we make the individual's tags their root node
	 * toString() for now.
	 * 
	 * @throws Exception
	 */
	@Override
	public void init() throws Exception {
		super.init();

		// Setup the {@link DiversityUtils} object
		this.diversityUtils = new DiversityUtils(this.context.getConfig());

		// Simply set each individual's tag and age.
		for (Individual individual : this.population) {
			this.diversityUtils.tagIndividual(individual);

			// Initialize the age to 1
			individual.setAge(1);
		}

		// Collect the tags in the population.
		this.diversityUtils.collectTags(this.population);

		// Setup the objectives to use for the pareto front.
		setupObjectives();

		logger.info("Using " + this.objectives.toString()
				+ " as pareto objectives.");
	}

	/**
	 * Convenience method to breed and mutate the current population to create
	 * the temporary new population.
	 * 
	 * @return a newly bred (and possibly mutated) population
	 * @throws CloneNotSupportedException
	 */
	public Vector<Individual> breedAndMutate()
			throws CloneNotSupportedException {
		int fillSize = context.getConfig().getPopSize() - 1;

		// Temporary place-holder for the newly-bred individuals.
		Vector<Individual> tmpNewPopulation = new Vector<Individual>();

		// Fill the population, leaving one extra spot.
		while (tmpNewPopulation.size() < fillSize) {
			// Determine whether to do random mating & crossover or replication
			if (context.nextBool(context.getConfig().getCrossProbability())) {
				doRandomMating(tmpNewPopulation, fillSize);
			} else {
				doReplication(tmpNewPopulation, fillSize);
			}
		}

		// Probabilistically mutate the individuals
		doMutation(tmpNewPopulation, context);

		return tmpNewPopulation;
	}

	@Override
	public void breed() throws CloneNotSupportedException {
		// Breed the new temporary population
		Vector<Individual> tmpNewPopulation = breedAndMutate();

		// Tag all the offspring now
		this.diversityUtils.tagIndividuals(tmpNewPopulation);

		// Add a new random individual to the population
		if (this.objectives.equals(OBJECTIVES.AGE_FITNESS)) {
			addRandomIndividual(tmpNewPopulation);
		}

		// Evaluate the new individuals BEFORE doing the Pareto selection!
		evaluateOffspring(tmpNewPopulation);

		// Temporarily add all the new individuals to the population
		this.population.addAll(tmpNewPopulation);

		// Now do the Pareto selection to shrink the pop down to size
		ParetoOperators.delete(this.context, this.population, this.objectives);

		// Increment all the survivors' ages
		for (Individual ind : this.population) {
			ind.ageIncr();
		}

		// For density-based runs, add the individual AFTER pareto selection.
		if (!this.objectives.equals(OBJECTIVES.AGE_FITNESS)) {
			if (this.context.getConfig().getDoUniqueTagRandom()) {
				addUniqueRandomIndividual(this.population);
			} else {
				addRandomIndividual(this.population);
				this.diversityUtils
						.tagIndividual(this.population.lastElement());
			}

			// Evaluate the individual
			this.problem.fitness(this.population.lastElement());
		}
	}

	/**
	 * <b>EXPERIMENTAL</b>: Override GP.step() so we can update our tag level
	 * and/or tag depth over time.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Override
	public void step() throws CloneNotSupportedException {
		// Just do the regular GP.step(), and then update the tag level/density
		super.step();

		// Now update the tag level/density as necessary
		if (context.getConfig().doChangeTagLevel()) {
			if (this.generation > 0
					&& (this.generation + 1)
							% context.getConfig().getTagLevelChangeGens() == 0) {
				// Can we still go deeper?
				int currentTagLevel = this.diversityUtils.getTagLevel();

				if (currentTagLevel < this.context.getConfig().getMaxDepth() - 1) {
					this.diversityUtils.setTagLevel(currentTagLevel + 1);
					logger.debug("Set tag level at generation "
							+ this.generation);
				}

				// What happens if we start over at the top??
				else if (context.getConfig().doCycleTagLevels()) {
					this.diversityUtils.setTagLevel(0);
				}

				this.lastTagLevelChangeGen = this.generation;
			}
		}
	}

	/**
	 * Convenience method to evaluate the offspring after breeding. This is
	 * because the offspring will compete with their parents for placement when
	 * we use {@link ParetoOperators} to shrink the population down to the
	 * target size.
	 * 
	 * @param offspring
	 *            the newly-bred individuals to evaluate
	 * @throws CloneNotSupportedException
	 */
	public void evaluateOffspring(Vector<Individual> offspring) {
		int numThreads = this.context.getConfig().getNumThreads();
		int chunkSize = offspring.size() / numThreads;
		int start = 0, end = 0;
		Thread[] threads = new Thread[numThreads];

		// Fire off all the evaluation threads
		for (int i = 0; i < numThreads; i++) {
			start = i * chunkSize;

			// Set the end index (account for uneven popSize/numThreads)
			end = (i == numThreads - 1) ? offspring.size()
					: (start + chunkSize); // subList end is exclusive so no -1

			EvalThread thread = new EvalThread(offspring.subList(start, end));
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
	}

	/**
	 * Adds a new random individual to the temporary new population. We
	 * alternate between full and grow each time this is called.
	 * 
	 * @param tmpNewPopulation
	 *            the temporary new population to which to add the new random
	 *            individual
	 * @throws CloneNotSupportedException
	 */
	public void addRandomIndividual(Vector<Individual> tmpNewPopulation)
			throws CloneNotSupportedException {
		// Generate a random individual.
		Individual individual = getRandomIndividual(this.randMethod);

		// Give the individual a tag
		this.diversityUtils.tagIndividual(individual);

		// Give the individual an ID
		individual.setId(this.generation);

		tmpNewPopulation.add(individual);

		// Remember to flip the random individual generation method
		this.randMethod ^= 1;
	}

	/**
	 * Adds a new random individual to the temporary new population and tries to
	 * ensure that the individual is unique (in the current and temp new
	 * population) down to the tag depth.
	 * 
	 * @param tmpNewPopulation
	 *            the temporary new population to which to add the new random
	 *            individual
	 */
	public void addUniqueRandomIndividual(Vector<Individual> tmpNewPopulation)
			throws CloneNotSupportedException {
		// Make sure we have all the tags in the current population.
		this.diversityUtils.collectTags(this.population);

		// Also make sure we have all the tags from the new individuals.
		this.diversityUtils.collectTags(tmpNewPopulation);

		// Now spin until we get a uniquely-tagged individual
		Individual individual = null;

		individual = generateUniqueIndividualSingleLevel();

		// Give the individual an ID
		individual.setId(this.generation);

		// Finally add the individual to the temp new population.
		tmpNewPopulation.add(individual);
	}

	/**
	 * Helper method to generate a tag-unique (at the single tag level) random
	 * individual. levels.
	 * 
	 * @return the generated individual
	 * @throws CloneNotSupportedException
	 */
	public Individual generateUniqueIndividualSingleLevel()
			throws CloneNotSupportedException {
		String newTag = null;
		int numTries = 0;
		Individual individual = null;

		// Set the max tries. Be more careful with smaller tag depths!
		int maxTries = this.diversityUtils.getTagDepth() > 1 ? this.context
				.getConfig().getMaxUniqueRetries() : 100;

		while ((newTag == null || !this.diversityUtils.isTagUnique(newTag))
				&& numTries < maxTries) {
			individual = getRandomIndividual(this.randMethod);

			// Tag the individual so we can see if it's unique.
			this.diversityUtils.tagIndividual(individual);
			newTag = individual.getTag();

			// Flip the random individual generation method.
			this.randMethod ^= 1;
			numTries++;
		}

		// Be sure to add the new individual's tag to the collection.
		this.diversityUtils.addTag(individual.getTag());

		if (this.generation % 100 == 0) {
			logger.debug("Spent " + numTries
					+ " iterations generating random individual. Total tags = "
					+ this.diversityUtils.getNumTags());
		}

		return individual;
	}

}
