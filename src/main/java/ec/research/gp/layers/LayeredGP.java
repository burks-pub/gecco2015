package ec.research.gp.layers;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;

import ec.research.gp.simple.gp.GP;
import ec.research.gp.simple.representation.Individual;
import ec.research.gp.simple.util.Config;
import ec.research.gp.simple.util.Context;
import ec.research.gp.statistics.LayeredGPStatistics;

/**
 * The layered GP, based on Hornby's ALPS, but extended to handle different
 * layer migration schemes.
 * 
 */
public class LayeredGP extends GP {
	// Holds the total num layers from config.
	private int maxNumLayers;

	// Holds the age gap from config.
	private int ageGap;

	// Holds the total number of individuals per layer so we don't recalculate.
	private int numIndividualsPerLayer;

	// Holds the set of individuals that were chosen as parents most recently.
	private List<Individual> parents;

	// Holds the layered population.
	private List<Vector<Individual>> population;

	// The Aging scheme we will use to update ages, defined by config.
	private LayerScheme layerScheme;

	// Keeps track of whether or not we just added a new layer.
	private boolean addedLayer;

	// Keeps track of the last generation where we regenerated the initial
	// layer.
	protected int lastRegen;

	// Keeps track of the last generation where we added a new layer.
	protected int lastLayerAdd;

	// Maps the individuals generated to the number of times we generated them.
	private Map<String, Integer[]> generatedExpressions;

	// Total number of (global) attempts to generate unique individuals.
	private int uniqueRetries;

	// The maximum number of unique individual creation attempts we can make.
	private static int MAX_UNIQUE_RETRIES;

	// Lock for threading.
	private Object lock = new Object();

	// Log4j logger for any output messages.
	private static final Logger logger = Logger.getLogger(LayeredGP.class);

	/**
	 * Convenience method to setup the layer scheme, based on the config. It
	 * defaults to AlpsLayerScheme if not present in the configuration.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void setupLayerScheme() {
		String scheme = this.context.getConfig().getLayerScheme();

		// Set the default, AlpsLayerScheme if none is present.
		if (scheme == null) {
			this.layerScheme = new AlpsLayerScheme(context.getConfig(),
					this.population, this.parents);
		}

		// Otherwise, try to setup the scheme as dictated in config
		else {
			logger.debug("Using " + scheme + " as the layer scheme.");
			try {
				Class schemeClass = Class.forName(scheme);
				Constructor<?> constructor = schemeClass.getConstructor(
						Config.class, List.class, List.class);
				this.layerScheme = (LayerScheme) constructor.newInstance(
						context.getConfig(), this.population, this.parents);
			} catch (Exception e) {
				logger.fatal(e);
				System.exit(1);
			}
		}
	}

	/**
	 * Convenience method to add elites to the end of each layer in the
	 * population, if elitism is enabled.
	 * 
	 * @param layerIndex
	 *            the index of the layer in the population
	 * 
	 * @param newLayer
	 *            the new, filled population up to popSize - numElites
	 * 
	 * @param numElites
	 *            the number of elites to add to each layer
	 * 
	 * @throws CloneNotSupportedException
	 */
	private void addElitesToLayer(int layerIndex, Vector<Individual> newLayer,
			int numElites) throws CloneNotSupportedException {
		// Sort the layer if using elitism, then add the elites
		if (numElites > 0) {
			Vector<Individual> layer = this.population.get(layerIndex);
			Collections.sort(layer);

			// Now add the elites
			for (int i = 0; i < numElites; i++) {
				int eliteIndex = layer.size() - 1 - i;

				if (eliteIndex >= 0) {
					Individual elite = layer.get(eliteIndex);
					newLayer.add(elite.clone());
				}
			}

			// Update their ages using the layer scheme
			layerScheme.updateElitesAges(layer, this.parents, numElites);
		}
	}

	/**
	 * Convenience method to add the parents to this generation's parents.
	 * 
	 * @param parent1
	 *            the first parent
	 * @param parent2
	 *            the second parent
	 */
	private void addParents(Individual parent1, Individual parent2) {
		if (!this.parents.contains(parent1)) {
			this.parents.add(parent1);
		}

		if (!this.parents.contains(parent2)) {
			this.parents.add(parent2);
		}
	}

	/**
	 * Convenience method to perform tournament selection on the parents and add
	 * up to two children to the layer using crossover.
	 * 
	 * @param parents
	 *            the parents from which to select
	 * @param layer
	 *            the layer in which to place the offspring
	 * @param fillSize
	 *            the number of individuals to be added to the layer during this
	 *            breed cycle.
	 * 
	 * @throws CloneNotSupportedException
	 */
	private void doCrossover(Vector<Individual> parents,
			Vector<Individual> layer, int fillSize)
			throws CloneNotSupportedException {
		// Get two parents with tournament selection
		int[] parentIndices = this.selection.select(this.context, parents);

		// Cross the individuals and add the child(ren) to the
		// new layer.
		Individual[] children = parents.get(parentIndices[0]).crossover(
				parents.get(parentIndices[1]), this.context);

		// Set the children's IDs
		children[0].setId(this.generation);
		children[1].setId(this.generation);

		// Update their ages (they just need to be incremented).
		children[0].ageIncr();
		children[1].ageIncr();

		layer.add(children[0]);

		// Add child 2 unless we're discarding it or we need space for elites.
		if (!this.context.getConfig().doDiscardSecondChild()
				&& layer.size() < fillSize) {
			layer.add(children[1]);
		}

		// Remember the current generation parents for updating ages.
		addParents(parents.get(parentIndices[0]), parents.get(parentIndices[1]));
	}

	/**
	 * Convenience method to perform tournament selection on the parents and add
	 * a single child (replicated by a single parent) to the new population.
	 * 
	 * @param parents
	 *            the parents from which to select
	 * @param layer
	 *            the layer in which to place the offspring
	 * @param fillSize
	 *            the number of individuals to be added to the layer during this
	 *            breed cycle.
	 * 
	 * @throws CloneNotSupportedException
	 */
	private void doReplication(Vector<Individual> parents,
			Vector<Individual> layer, int fillSize)
			throws CloneNotSupportedException {
		// Don't even continue if the layer is too full.
		if (layer.size() == fillSize) {
			return;
		}

		// Find a parent to replicate
		Individual parent = parents.get(this.selection.selectOne(this.context,
				parents));

		// Replicate the parent to get the child.
		Individual child = parent.replicate();

		// Set the child's ID.
		child.setId(this.generation);

		// Set the child's age to the parent's age + 1
		child.ageIncr();

		// Add the parent to the current generation parents
		if (!this.parents.contains(parent)) {
			this.parents.add(parent);
		}

		// Add the child to the layer.
		layer.add(child);
	}

	/**
	 * Simple initialization that just calls GP.init() and initializes the layer
	 * variables. Nothing special here.
	 * 
	 * @param c
	 *            the Context
	 * @throws Exception
	 */
	public LayeredGP(Context c) throws Exception {
		super(c);

		// Setup the population
		this.population = new ArrayList<Vector<Individual>>();

		// Setup everything else.
		this.maxNumLayers = context.getConfig().getNumLayers();
		this.ageGap = context.getConfig().getAgeGap();
		this.numIndividualsPerLayer = context.getConfig().getPopSize()
				/ maxNumLayers;
		this.parents = new ArrayList<Individual>();

		this.generatedExpressions = new HashMap<String, Integer[]>();
		this.uniqueRetries = 0;
		MAX_UNIQUE_RETRIES = context.getConfig().getMaxUniqueRetries();
	}

	/**
	 * Convenience method to evaluate all the individuals of the population as
	 * well as track the best individual of the generation and overall.
	 * 
	 * We override GP.evaluatePop() since the population structure is quite
	 * different due to the layers.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Override
	public void evaluatePop() {
		int numThreads = this.context.getConfig().getNumThreads();
		int start = 0, end = 0;
		Thread[] threads = new Thread[numThreads];

		// Put all the individuals into a collection so we can break into chunks
		Vector<Individual> allInds = new Vector<Individual>();
		for (Vector<Individual> layer : this.population) {
			allInds.addAll(layer);
		}

		// Now we can break the evals into multiple threads
		int chunkSize = allInds.size() / numThreads;

		// Fire off all the evaluation threads
		for (int i = 0; i < numThreads; i++) {
			start = i * chunkSize;

			// Set the end index (account for uneven popSize/numThreads)
			end = (i == numThreads - 1) ? allInds.size() : (start + chunkSize);

			EvalThread thread = new EvalThread(allInds.subList(start, end));
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
	 * Evaluates the given layer of individuals against the problem.
	 * 
	 * @param layer
	 *            the layer of individuals to evaluate
	 * @throws CloneNotSupportedException
	 */
	public void evaluateLayer(Vector<Individual> layer) {
		int numThreads = this.context.getConfig().getNumThreads();
		int chunkSize = layer.size() / numThreads;
		int start = 0, end = 0;
		Thread[] threads = new Thread[numThreads];

		// Fire off all the evaluation threads
		for (int i = 0; i < numThreads; i++) {
			start = i * chunkSize;

			// Set the end index (account for uneven popSize/numThreads)
			end = (i == numThreads - 1) ? layer.size() : (start + chunkSize);

			EvalThread thread = new EvalThread(layer.subList(start, end));
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

		// Calculate the population averages.
		calculateAverages();
	}

	/**
	 * Same as GP.init(), but it only fills the initial layer with individuals.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Override
	public void init() throws CloneNotSupportedException {
		// Initialize the first layer.
		this.population.add(new Vector<Individual>());
		Vector<Individual> initialLayer = this.population.get(0);

		// Initialize it with random individuals.
		initLayer(initialLayer);

		// Now that we have our population, initialize the stats object.
		try {
			this.statistics = new LayeredGPStatistics(this.population,
					context.getConfig());
			this.context.setStats(this.statistics);
		} catch (IOException e) {
			logger.fatal(e);
			System.exit(1);
		}

		// Evaluate the initial population
		evaluatePop();

		// logger.debug("Total unique retries used: " + uniqueRetries);
		logger.debug("init() done. Pop size=" + initialLayer.size());

		// Setup the layer scheme.
		setupLayerScheme();
	}

	/**
	 * Basically the same as GP.step(), but we update the layers before
	 * breeding.
	 */
	@Override
	public void step() throws CloneNotSupportedException {
		// Update the layer indices for all individuals.
		updateLayerIndicies();

		// Update the layers AFTER everyone's evaluated.
		this.addedLayer = false;
		this.layerScheme.updateLayers(this);

		// Do the post-evaluation stats (if running by gens)
		if (this.stopOn == STOP_ON.GENERATIONS
				&& this.generation
						% this.context.getConfig().getStatCollectGens() == 0) {
			this.statistics.postEvaluationStats(this.generation);
		}

		// Post-generation stats.
		this.statistics.postGenerationStats(this.generation);

		// Don't continue if it's time to terminate
		if (!doTerminate()) {
			// Increment the generation number
			this.generation++;

			// Breed the new population
			breed();
		}
	}

	/**
	 * Simply calculates the current average fitness, depth and size of the
	 * individuals in the population (across all layers).
	 */
	@Override
	public void calculateAverages() {
		this.avgFitness = 0;
		this.avgDepth = 0;
		this.avgSize = 0;

		int totalInds = 0;
		for (Vector<Individual> layer : this.population) {
			for (Individual individual : layer) {
				avgFitness += individual.getFitness();
				avgDepth += individual.getDepth();
				avgSize += individual.getNumNodes();
				totalInds++;
			}
		}

		avgFitness /= totalInds;
		avgDepth /= totalInds;
		avgSize /= totalInds;

		this.statistics.setNumEvaluations(this.numEvaluations);
		this.statistics.setTreeStatsInfo(this.avgSize, this.avgDepth);
	}

	/**
	 * Simply chooses the actual breed method to use and does it.
	 */
	public void breed() throws CloneNotSupportedException {
		interLayerBreed();
	}

	/**
	 * Mostly like GP.breed(), but this does a layer-by-layer breed to make sure
	 * that each layer is filled.
	 * 
	 * Also, we don't allow breeding of a layer that just got created, as well
	 * as the initial layer if it just got regenerated.
	 * 
	 * @throws CloneNotSupportedException
	 */
	public void interLayerBreed() throws CloneNotSupportedException {
		// Get the number of elites, for convenience.
		int numElites = context.getConfig().getNumElites();

		// Sanity check on numElites.
		if (numElites == numIndividualsPerLayer) {
			logger.fatal("Trying to breed new generation but numElites = numIndividualsPerLayer!!");
			System.exit(1);
		}

		// Temporary holder for the new population.
		List<Vector<Individual>> tmpNewPop = new ArrayList<Vector<Individual>>();

		// Breed in each layer separately, but be sure to look at the old pop!
		for (int i = 0; i < population.size(); i++) {
			// don't breed the last layer if it was just created with addLayer()
			if (!(i == population.size() - 1 && this.addedLayer && this.population
					.get(i).size() == this.numIndividualsPerLayer)) {

				// Temporary holder for the newly-bred layer
				Vector<Individual> tmpNewLayer = new Vector<Individual>();

				// Make a temporary combined super "layer" to make selection
				// easy.
				Vector<Individual> previousLayer = (i > 0) ? population
						.get(i - 1) : null;
				Vector<Individual> combinedLayer = new Vector<Individual>();

				if (previousLayer != null && !previousLayer.isEmpty()) {
					combinedLayer.addAll(previousLayer);
				}
				combinedLayer.addAll(population.get(i));

				// Proceed if the combined layer is not empty.
				if (!combinedLayer.isEmpty()) {
					// Adjust for the case of too small layers (< num elites)!
					int currentLayerSize = this.population.get(i).size();
					int fillSize = (currentLayerSize < numElites) ? this.numIndividualsPerLayer
							- currentLayerSize
							: (this.numIndividualsPerLayer - numElites);

					while (tmpNewLayer.size() < fillSize) {
						// Figure out whether to do crossover or replication
						if (this.context.nextBool(this.context.getConfig()
								.getCrossProbability())) {
							doCrossover(combinedLayer, tmpNewLayer, fillSize);
						} else {
							doReplication(combinedLayer, tmpNewLayer, fillSize);
						}
					}

					// Probabilistically mutate the individuals
					doMutation(tmpNewLayer, this.context);

					// Add elites if necessary
					addElitesToLayer(i, tmpNewLayer, numElites);

					// Add the newly-bred layer to the new population
					tmpNewPop.add(tmpNewLayer);
				} else {
					// The layer is empty, nothing to breed.
					tmpNewPop.add(population.get(i));
				}
			} else {
				tmpNewPop.add(population.get(i));
			}
		}

		// Replace the old population with the new one.
		this.population.clear();
		this.population.addAll(tmpNewPop);

		// Evaluate the new population
		evaluatePop();
	}

	/**
	 * Adds a new layer to the population, placing only the individual in it
	 * (i.e. it doesn't immediately get filled).
	 * 
	 * @param individual
	 *            the individual to add to the new layer
	 */
	public void addLayer(Individual individual) {
		logger.debug("addLayer(ind) called.");

		if (population.size() < this.maxNumLayers) {
			// Create the new layer.
			this.population.add(new Vector<Individual>());

			// Now add the individual to the newly-created layer.
			this.population.get(this.population.size() - 1).add(individual);

			// Remember the current generation in which we added a new layer.
			this.lastLayerAdd = this.generation;
		}
	}

	/**
	 * Convenience method to add a layer to the end of the current population.
	 * This should be called once each time the topmost layer reaches its age
	 * limit in generations, and should be done before the layers are updated.
	 * That way, the population size should be correct, and old individuals will
	 * compete with the newly generated layer individuals above them. To create
	 * a new layer, we just use the layer below to breed and fill it up, just as
	 * in Hornby's ALPS.<br/>
	 * <br/>
	 * 
	 * These individuals are considered to be part of the next generation, since
	 * they are bred from parents of the current generation, and they also
	 * require a new round of evaluations!.
	 * 
	 * @throws CloneNotSupportedException
	 */
	public void addLayer() throws CloneNotSupportedException {
		logger.debug("addLayer() called. Total layers: " + population.size());

		// Do we even have space left in the population for a new layer?
		if (population.size() < this.maxNumLayers) {
			// Use the previous layer for selection.
			Vector<Individual> previousLayer = population
					.get(population.size() - 1);

			// Add a new layer!
			Vector<Individual> newLayer = new Vector<Individual>();
			population.add(newLayer);

			// Keep adding individuals to the end until we've filled the layer
			while (newLayer.size() < this.numIndividualsPerLayer) {
				// Figure out whether to do crossover or replication
				if (this.context.nextBool(this.context.getConfig()
						.getCrossProbability())) {
					doCrossover(previousLayer, newLayer,
							this.numIndividualsPerLayer);
				} else {
					doReplication(previousLayer, newLayer,
							this.numIndividualsPerLayer);
				}
			}

			// Probabilistically mutate the new offspring
			doMutation(newLayer, this.context);

			// Remember the current generation in which we added a layer
			this.lastLayerAdd = this.generation;
			this.addedLayer = true;
		}

		logger.debug("addLayer() done. Total layers: " + population.size());
	}

	/**
	 * Updates the layer indices for every individual in the population so we
	 * can keep track of which layer each individual is in. This needs to happen
	 * post-breed and pre-updateLayers so that we can move individuals correctly
	 * (i.e. we don't want to have individuals replacing their fellow layer
	 * individuals as they move up layers in the standard approach).
	 */
	public void updateLayerIndicies() {
		for (int layerIndex = 0; layerIndex < this.population.size(); layerIndex++) {
			for (Individual individual : this.population.get(layerIndex)) {
				individual.setCurrentLayer(layerIndex);
			}
		}
	}

	/**
	 * Tries to move the individual up to the next layer. The individual will
	 * try to displace an individual that meets the criteria for moving up or
	 * that it is better than, unless the layer is not yet full.
	 * 
	 * We'll use a configurable "move-up criteria." That way, we can try
	 * different approaches other than just ALPS's age.
	 * 
	 * @param index
	 *            the index of the individual to move
	 * 
	 * @param currentLayerIndex
	 *            the index of the current layer that the individual is in
	 * 
	 * @param doCountMoves
	 *            whether or not we should count the moves from a layer
	 * 
	 * @param doMoveDisplaced
	 *            whether or not to try to move up the displaced individual
	 * 
	 * @return whether or not the individual was successfully moved up.
	 */
	public boolean moveIndividualUp(int index, int currentLayerIndex,
			boolean doCountMoves, boolean doMoveDisplaced) {
		boolean success = false;

		// Get the current layer, for convenience
		Vector<Individual> currentLayer = this.population
				.get(currentLayerIndex);

		// Get the individual to move, for convenience
		Individual indToMove = currentLayer.get(index);

		// Are we out of layers to go up?
		if (currentLayerIndex < this.population.size() - 1) {
			// Get the next layer up, for convenience
			Vector<Individual> nextLayer = this.population
					.get(currentLayerIndex + 1);

			// First, see if the next layer has open space
			if (nextLayer.size() < this.numIndividualsPerLayer) {
				// Add the individual at the end and it will be in the layer
				nextLayer.add(currentLayer.get(index));
				success = true;
			}

			// Otherwise find an individual to displace
			else {
				success = displace(indToMove, currentLayerIndex + 1,
						doCountMoves, doMoveDisplaced);
			}
		}

		// Remove the individual from its old spot
		currentLayer.remove(index);

		return success;
	}

	/**
	 * Tries to move the individual up to the next layer. The individual will
	 * try to displace an individual that meets the criteria for moving up or
	 * that it is better than, unless the layer is not yet full.
	 * 
	 * We'll use a configurable "move-up criteria." That way we can try
	 * different approaches other than just ALPS's age.
	 * 
	 * @param inToMove
	 *            the individual to move
	 * 
	 * @param currentLayerIndex
	 *            the index of the current layer that the individual is in
	 * 
	 * @param doCountMoves
	 *            whether or not we should count the moves from a layer
	 * 
	 * @param doMoveDisplaced
	 *            whether or not to try to move up the displaced individual
	 * 
	 * @return whether or not the individual was successfully moved up.
	 */
	public boolean moveIndividualUp(Individual indToMove,
			int currentLayerIndex, boolean doCountMoves, boolean doMoveDisplaced) {
		boolean success = false;

		// Get the current layer, for convenience
		Vector<Individual> currentLayer = this.population
				.get(currentLayerIndex);

		// Are we out of layers to go up?
		if (currentLayerIndex < this.population.size() - 1) {
			// Get the next layer up, for convenience
			Vector<Individual> nextLayer = this.population
					.get(currentLayerIndex + 1);

			// First, see if the next layer has open space
			if (nextLayer.size() < this.numIndividualsPerLayer) {
				// Add the individual at the end and it will be in the layer
				nextLayer.add(indToMove);
				success = true;
			}

			// Otherwise find an individual to displace
			else {
				success = displace(indToMove, currentLayerIndex + 1,
						doCountMoves, doMoveDisplaced);
			}
		}

		// Remove the individual from its old spot
		currentLayer.remove(indToMove);

		return success;
	}

	/**
	 * Tries to find a spot in the layer above by finding an individual to
	 * displace as in ALPS. An individual gets displaced if it's fitness is
	 * worse or it meets the criteria for needing to move up.
	 * 
	 * @param indToMove
	 *            the individual we're trying to move
	 * 
	 * @param layerAboveIndex
	 *            the index of the layer above the individual that we're trying
	 *            to move
	 * 
	 * @param doCountMoves
	 *            whether or not we should count moves out of layers
	 * 
	 * @param doMoveDisplaced
	 *            whether or not to try to move up the displaced individual
	 * 
	 * @return true if the individual successfully displaced an individual in
	 *         the layer above it; false otherwise.
	 */
	public boolean displace(Individual indToMove, int layerAboveIndex,
			boolean doCountMoves, boolean doMoveDisplaced) {
		Vector<Individual> layerAbove = this.population.get(layerAboveIndex);

		/*
		 * Find an individual to displace.
		 */
		for (int i = 0; i < layerAbove.size(); i++) {
			Individual indToDisplace = layerAbove.get(i);

			// Only displace if it just moved from the same layer!
			if (indToDisplace.getCurrentLayer() != indToMove.getCurrentLayer()) {
				/*
				 * Fitness has to be better or the individual to displace must
				 * meet the move-up criteria
				 */
				if (indToMove.compareTo(indToDisplace) > 0
						|| (doMoveDisplaced && this.layerScheme.moveUpCriteria(
								indToDisplace, layerAboveIndex))) {

					// Try to move the displaced individual up if requested.
					if (doMoveDisplaced
							&& this.layerScheme.moveUpCriteria(indToDisplace,
									layerAboveIndex)) {
						moveIndividualUp(i, layerAboveIndex, doCountMoves,
								doMoveDisplaced);
					}
					// Otherwise, just delete it.
					else {
						layerAbove.remove(i);
					}

					// Put the individual in it's spot (really at the end, but
					// essentially in its spot since it was removed).
					layerAbove.add(indToMove);

					// We successfully displaced an individual.
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Convenience method to add a layer to the population if we've reached the
	 * "age gap" and we haven't reached the maximum number of layers.
	 * 
	 * @throws CloneNotSupportedException
	 */
	public void checkAndAddLayer() throws CloneNotSupportedException {
		if (this.layerScheme.doAddLayer(this.generation, this.ageGap,
				this.lastLayerAdd)
				&& this.population.size() < this.maxNumLayers) {
			// Go ahead and breed the next layer
			addLayer();

			/*
			 * Now we need to evaluate the new layer so that individuals can
			 * move up fairly. We're considering this layer part of the next
			 * generation, since it is bred from parents of the current
			 * generation. Although we're calling evaluatePop() only the new
			 * individuals will actually be evaluated.
			 */
			evaluatePop();
		}
	}

	/**
	 * Convenience method to check if the initial layer needs to be regenerated.
	 * This happens if updateLayers() left it empty, or if we've reached the age
	 * gap.
	 * 
	 * @param reachedAgeGap
	 *            whether or not we've reached the age gap
	 * @throws CloneNotSupportedException
	 */
	public void checkAndRegenInitialLayer() throws CloneNotSupportedException {
		// If update left the initial layer empty, go ahead and regenerate it.
		if (this.population.get(0).isEmpty()) {
			logger.debug("Initial layer became empty at generation: "
					+ this.generation);
			regenerateInitialLayer();

			this.lastRegen = this.generation;
		}

		// Otherwise, if the layer scheme says it's time, regenerate it.
		else if (this.layerScheme.doRegenerateInitialLayer(this.generation,
				this.ageGap, this.lastRegen)) {
			logger.debug("Reached age gap at generation " + this.generation);

			regenerateInitialLayer();

			this.lastRegen = this.generation;
		}
	}

	/**
	 * 
	 * @return the total number of individuals per layer.
	 */
	public int getNumIndividualsPerLayer() {
		return numIndividualsPerLayer;
	}

	/**
	 * Convenience method to initialize a layer with randomly-generated
	 * individuals. This is done so that the initialization code is in one
	 * place. Note: The individuals are NOT evaluated after the layer has been
	 * filled. Callers need to do this afterwards if necessary.
	 * 
	 * @param layer
	 *            the layer to initialize.
	 * 
	 * @throws CloneNotSupportedException
	 */
	public void initLayer(Vector<Individual> layer)
			throws CloneNotSupportedException {
		int half = this.numIndividualsPerLayer / 2;

		// Clear the initial layer. It better have been updated before now!
		layer.clear();

		while (layer.size() < this.numIndividualsPerLayer) {
			// Use the grow method for half, and the full method for half
			int mode = (layer.size() < half ? 0 : 1);

			Individual individual = getRandomIndividual(mode);

			// Avoid duplicates until we run out of tries.
			String expression = individual.toString();
			int numNodes = individual.getNumNodes();
			Integer numDups[] = this.generatedExpressions.get(expression);

			boolean isUnique = (numDups == null);

			// Remember the expression we just regenerated.
			if (isUnique) {
				this.generatedExpressions.put(expression, new Integer[] { 1,
						numNodes });
			} else {
				this.generatedExpressions.get(expression)[0] += 1;
			}

			// Allow duplicates if we've run out of retries.
			if (isUnique || this.uniqueRetries > MAX_UNIQUE_RETRIES) {
				// Set the individual's ID and add the individual.
				individual.setId(this.generation);

				layer.add(individual);
			} else {
				this.uniqueRetries++;
			}
		}
	}

	/**
	 * Generates random individuals in the initial layer, replacing all the old
	 * individuals.
	 * 
	 * @throws CloneNotSupportedException
	 */
	public void regenerateInitialLayer() throws CloneNotSupportedException {
		// Regenerate the initial layer with random individuals.
		initLayer(this.population.get(0));

		// We need to evaluate the new initial layer, because they can become
		// parents to the layer above.
		evaluateLayer(this.population.get(0));
	}

	/**
	 * Sets the generation number to the given value. This should be used with
	 * much care. Use cases are testing or doing complicated
	 * backtracking/resetting.
	 * 
	 * @param generationNum
	 *            the generation number.
	 */
	public void setGenerationNum(int generationNum) {
		this.generation = generationNum;
	}

	/**
	 * 
	 * @return the current generation number (0-based).
	 */
	public int getGenerationNum() {
		return this.generation;
	}

	/**
	 * Convenience method to get a reference to the population. This should
	 * always be used instead of the inherited GP.getPopulation, as the
	 * population structure is much different.
	 * 
	 * @return the layered population
	 */
	public List<Vector<Individual>> getLayeredPopulation() {
		return this.population;
	}

	/**
	 * Convenience method to get a reference to the layer scheme. This is really
	 * only used for testing right now.
	 * 
	 * @return a reference to the {@link LayerScheme}
	 */
	public LayerScheme getLayerScheme() {
		return this.layerScheme;
	}

	/**
	 * @return the age gap for the layers
	 */
	public int getAgeGap() {
		return this.ageGap;
	}

	/**
	 * @return the maximum number of layers to be used
	 */
	public int getMaxNumLayers() {
		return this.maxNumLayers;
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
						if (!(foundOptimal && context.getConfig()
								.stopOnOptimal())) {
							numEvaluations++;
						}

						// See if the best individual of the generation is the
						// best overall
						if (individual.getFitness() > bestFitness
								|| bestIndividual == null) {
							bestFitness = individual.getFitness();

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
							boolean doLogFitness = (numEvaluations != lastFitnessOutput && (numEvaluations % context
									.getConfig().getStatCollectEvals()) == 0);

							boolean loggingOptimal = foundOptimal
									&& doStopOnOptimal && !loggedOptimal;

							// Output fitness info every X evaluations
							if (doLogFitness || loggingOptimal) {
								// Calculate some population averages
								calculateAverages();

								((LayeredGPStatistics) statistics)
										.setNumEvaluations(numEvaluations);

								((LayeredGPStatistics) statistics)
										.postEvaluationStats(generation);
								lastFitnessOutput = numEvaluations;

								if (loggingOptimal) {
									loggedOptimal = true;
								}
							}
						} else {
							// Calculate some population averages
							calculateAverages();

							((LayeredGPStatistics) statistics)
									.setNumEvaluations(numEvaluations);
							lastFitnessOutput = numEvaluations;
						}
					}
				}
			}
		}
	}
}
