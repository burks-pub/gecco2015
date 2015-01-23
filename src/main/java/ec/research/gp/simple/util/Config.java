package ec.research.gp.simple.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import ec.research.gp.simple.bool.nodes.Di;
import ec.research.gp.simple.gp.GP;
import ec.research.gp.simple.gp.GP.STOP_ON;
import ec.research.gp.simple.multiplexer.nodes.Ai;
import ec.research.gp.simple.problem.Problem;
import ec.research.gp.simple.representation.Node;



public class Config {
	/**
	 * Special annotation so we can easily enumerate/print out all of the
	 * current configuration options.
	 **/
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Option {
		/**
		 * @return The name of the configuration option
		 */
		String value();

		/**
		 * @return Short description of the configuration option
		 */
		String desc();

		/**
		 * 
		 * @return The configuration category to which this option belongs
		 */
		String cat() default "[General]";
	}

	@Option(value = "seed", desc = "Holds the seed for the run")
	private long seed;

	@Option(value = "crossProb", desc = "Holds the crossover probability.")
	private double crossProb;

	@Option(value = "crossFuncProb", desc = "The probability of selecting a function node for crossover")
	private double crossFuncProb;

	@Option(value = "mutProb", desc = "Holds the mutation probability.")
	private double mutProb;

	@Option(value = "popSize", desc = "Holds the population size.")
	private int popSize;

	@Option(value = "numGenerations", desc = "Holds the max number of generations.")
	private int numGenerations;

	@Option(value = "numEvaluations", desc = "Holds the max number of evaluations.")
	private long numEvaluations;

	@Option(value = "numThreads", desc = "Holds the number of threads to use (currently for evals).")
	private int numThreads;

	@Option(value = "stopOn", desc = "Holds the stop-on criteria type (default is GENERATION).")
	private STOP_ON stopOn;

	@Option(value = "maxDepth", desc = "Holds the max depth for a tree.")
	private int maxDepth;

	@Option(value = "maxSize", desc = "the max allowed size (number of nodes) for a tree")
	private int maxSize;

	@Option(value = "minBuildDepth", desc = "Holds the min depth for a newly built random tree.")
	private int minBuildDepth;

	@Option(value = "maxBuildDepth", desc = "Holds the max depth for a newly bulit random tree.")
	private int maxBuildDepth;

	@Option(value = "maxCrossAttempts", desc = "Holds the max attempts for crossover.")
	private int maxCrossAttempts;

	@Option(value = "maxUniqueRetries", desc = "Max number of retries for unique individuals on init")
	private int maxUniqueRetries;

	@Option(value = "tournamentSize", desc = "Number of individuals to compete in tournament selection.")
	private int tournamentSize;

	@Option(value = "discardSecondChild", desc = "Whether or not we should discard the second child during crossover.")
	private boolean discardSecondChild;

	@Option(value = "stopOnOptimal", desc = "Whether or not we stop when we find an individual with optimal fitness")
	private boolean stopOnOptimal;

	@Option(value = "numElites", desc = "Num elites if using elitism.")
	private int numElites;

	@Option(value = "problem", desc = "The problem instance to evolve on")
	private Problem problem;

	@Option(value = "functionSet", desc = "The function set for the problem. Add .i for each node.")
	private List<Node> functionSet;

	@Option(value = "terminalSet", desc = "The terminal set for the problem. Add .i for each node.")
	private List<Node> terminalSet;

	@Option(value = "numLayers", desc = "The number of layers", cat = "[ALPS/LayeredGP]")
	private int numLayers;

	@Option(value = "layerScheme", desc = "The layer scheme to use for LayeredGP", cat = "[ALPS/LayeredGP]")
	private String layerScheme;

	@Option(value = "ageGap", desc = "The age gap", cat = "[ALPS/LayeredGP]")
	private int ageGap;

	@Option(value = "outputDir", desc = "The output directory for the run.")
	private String outputDir;

	@Option(value = "statCollectGens", desc = "How often (in generations) do we collect general stats", cat = "[Statistics]")
	private int statCollectGens;

	@Option(value = "statCollectEvals", desc = "How often (in evaluations) do we collect fitness stats", cat = "[Statistics]")
	private long statCollectEvals;

	@Option(value = "doTrackTreeTags", desc = "Whether or not to track tree tags", cat = "[Statistics]")
	private boolean doTrackTreeTags;

	@Option(value = "maxTagTrackingLevel", desc = "The maximum level at which to track the tree tags, if tracking them", cat = "[Statistics]")
	private int maxTagTrackingLevel;

	@Option(value = "tagTrackingDepth", desc = "The tag depth to use for the tree tag tracking.", cat = "[Statistics]")
	private int tagTrackingDepth;

	@Option(value = "doChangeTagLevel", desc = "Whether or not we should change the tag level", cat = "[Tree Tagging]")
	private boolean doChangeTagLevel;

	@Option(value = "doCycleTagLevels", desc = "Whether or not we should cycle the tag levels", cat = "[Tree Tagging]")
	private boolean doCycleTagLevels;

	@Option(value = "tagLevelChageGens", desc = "How often (generations) we change the tag level", cat = "[Tree Tagging]")
	private int tagLevelChangeGens;

	@Option(value = "tagLevel", desc = "The level in the tree at which to start creating a tree tag", cat = "[Tree Tagging]")
	private int tagLevel;

	@Option(value = "tagDepth", desc = "How deep to go (from tagLevel) to create a tree tag", cat = "[Tree Tagging]")
	private int tagDepth;

	@Option(value = "objectives", desc = "The objectives (ParetoGP.OBJECTIVES) to use in the ParetoGP", cat = "[ParetoGP]")
	private String objectives;

	@Option(value = "doUniqueTagRandom", desc = "Whether or not to try to create a unique individual at the tag level when we create new random individuals", cat = "[ParetoGP]")
	private boolean doUniqueTagRandom;

	@Option(value = "doSizeBreakTies", desc = "Whether or not to try to break ties by size", cat = "[ParetoGP]")
	private boolean doSizeBreakTies;

	// Node mappings to make Individual.fromString() easier
	private Map<String, Node> nodeMappings;

	// Holds the config params for any classes that might need special params
	private Properties params;

	// Regex for generic address bit.
	private static final Pattern GENERIC_ADDR_BIT = Pattern
			.compile("(.+)\\.A_([\\d]+)");

	// Regex for generic data bit.
	private static final Pattern GENERIC_DATA_BIT = Pattern
			.compile("(.+)\\.D_([\\d]+)");

	// log4j logger for any messages.
	private static final Logger logger = Logger.getLogger(Config.class);

	/**
	 * Convenience method to setup the problem from the config file.
	 */
	private void setupProblem() {
		try {
			Class<?> problemClass = Class
					.forName(params.getProperty("problem"));
			this.problem = (Problem) problemClass.newInstance();
		} catch (Exception e) {
			logger.fatal(e);
			System.exit(1);
		}
	}

	/**
	 * Convenience method to setup terminalSetSize Di nodes and add them to the
	 * set.
	 * 
	 * @param terminalSet
	 *            the terminal set to which to add the Di nodes
	 * 
	 * @param size
	 *            the size of the terminal set (i.e. how many Di nodes we'll
	 *            add)
	 */
	private void setupMultiD(List<Node> terminalSet, int size) {
		for (int i = 0; i < size; i++) {
			// Add a new D_i node with it's inputNum = i.
			Di d = new Di();
			d.setIndex(i);
			terminalSet.add(d);

			// Be sure to add a mapping for this node.
			this.nodeMappings.put(d.toString(), d);
		}
	}

	/**
	 * Convenience method to setup the terminal node for a boolean problem using
	 * the generic address bit. These will have the same node name, such as A,
	 * but will have a number, i, at the end which lets us know how to create
	 * the Ai terminal node.
	 * 
	 * @param m
	 *            the regex match from the GENERIC_ADDR_BIT regex
	 * @param terminalSet
	 *            the terminal set we'll add the node to after we're done
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	private void setupGenericAddrBit(Matcher m, List<Node> terminalSet)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException {

		// Setup a new Ai node.
		Ai ai = new Ai();

		// Set i
		ai.setIndex(Integer.parseInt(m.group(2)));

		// Add the node to the terminal set
		terminalSet.add(ai);

		// Add a mapping for this node.
		this.nodeMappings.put(ai.toString(), ai);
	}

	/**
	 * Convenience method to setup the terminal node for a boolean problem using
	 * the generic data bit. These will have the same node name, such as D, but
	 * will have a number, i, at the end which lets us know how to create the Di
	 * terminal node.
	 * 
	 * @param m
	 *            the regex match from the GENERIC_DATA_BIT regex
	 * @param terminalSet
	 *            the terminal set we'll add the node to after we're done
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	private void setupGenericDataBit(Matcher m, List<Node> terminalSet)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException {

		// Setup a new Di node.
		Di di = new Di();

		// Set i
		di.setIndex(Integer.parseInt(m.group(2)));

		// Add the node to the terminal set
		terminalSet.add(di);

		// Add a mapping for this node.
		this.nodeMappings.put(di.toString(), di);
	}

	/**
	 * Convenience method to set up the function set or the terminal set.
	 * 
	 * @param baseParameter
	 *            the base parameter for the set (terminalSet or functionSet) so
	 *            we can generically look for terminalSet.0, etc.
	 */
	private void setupNodeSet(String baseParameter, List<Node> set) {
		// Setup set (Assumes default constructor sets the numChildren)
		if (set == null) {
			set = new ArrayList<Node>();
		}

		// Get the size of the set.
		int size = Integer.parseInt(params.getProperty(baseParameter + "Size"));

		try {
			for (int i = 0; i < size; i++) {
				// Get the node name based on the param
				String property = baseParameter + "." + i;
				String nodeName = params.getProperty(property);

				// Error if the node doesn't exist!
				if (nodeName == null) {
					logger.fatal(String.format(
							"%s size is %s but didn't get %s!", baseParameter,
							size, property));
					System.exit(1);
				}

				// Handle multi-input terminal nodes specially
				Matcher genAddrBitMatch = GENERIC_ADDR_BIT.matcher(nodeName);
				Matcher genDataBitMatch = GENERIC_DATA_BIT.matcher(nodeName);

				if (genAddrBitMatch.matches()) {
					setupGenericAddrBit(genAddrBitMatch, set);
				} else if (genDataBitMatch.matches()) {
					setupGenericDataBit(genDataBitMatch, set);
				}

				// If we got "Di", add terminalSetSize # Di nodes
				else if (nodeName.equals(Di.class.getName())) {
					setupMultiD(set, size);
					break; // We should only have Di as terminalSet.0
				}

				// Otherwise, add the simple node
				else {
					Class<?> nodeClass = Class.forName(nodeName);
					Node node = (Node) nodeClass.newInstance();

					set.add(node);

					this.nodeMappings.put(node.toString(), node);
				}
			}
		} catch (Exception e) {
			logger.fatal(e);
			System.exit(1);
		}
	}

	/**
	 * Creates a new Config object with the configuration in the properties file
	 * at the specified path.
	 * 
	 * @param path
	 *            the path to the properties file for this configuration.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Config(String path) throws FileNotFoundException, IOException {
		// Load the configuration from the properties file at the path.
		params = new Properties();
		params.load(new FileInputStream(path));

		// Set the seed
		this.seed = System.currentTimeMillis();

		// Load the parameters from the properties file
		init();
	}

	/**
	 * Initializes and sets all the parameters that were either present in the
	 * config file or were overridden with setParameter." This is called by the
	 * constructor, but it can be called again to reset everything by using the
	 * file as well as set the parameters that have been overridden. Note that
	 * the file is only read by the constructor and afterwards, we just load
	 * everything from the params object.
	 */
	public void init() {
		String seedStr = params.getProperty("seed");
		if (seedStr != null) {
			this.seed = Long.parseLong(seedStr);
		}

		// Set the problem to run
		setupProblem();

		// Initialize the node mappings (mappings added when we get the sets).
		this.nodeMappings = new HashMap<String, Node>();

		// Setup the function set
		this.functionSet = new ArrayList<Node>();
		setupNodeSet("functionSet", this.functionSet);

		// Setup the terminal set
		this.terminalSet = new ArrayList<Node>();
		setupNodeSet("terminalSet", this.terminalSet);

		// Setup the output directory
		this.outputDir = params.getProperty("outputDir", "output");

		// Set the crossover probability
		this.crossProb = Double.parseDouble(params.getProperty("crossProb",
				"1.0"));

		// Set the probability of selecting a function node for crossover
		this.crossFuncProb = Double.parseDouble(params.getProperty(
				"crossFuncProb", "0.9"));

		// Set the mutation probability
		this.mutProb = Double
				.parseDouble(params.getProperty("mutProb", "0.05"));

		// Set the pop size
		this.popSize = Integer.parseInt(params.getProperty("popSize", "100"));

		// Set the max num generations
		this.numGenerations = Integer.parseInt(params.getProperty(
				"numGenerations", "50"));

		// Set the max number of evaluations
		this.numEvaluations = Long.parseLong(params.getProperty(
				"numEvaluations", "600000"));

		// Set the number of threads to use for evals (default 2)
		this.numThreads = Integer.parseInt(params
				.getProperty("numThreads", "2"));

		// Set the STOP_ON type
		this.stopOn = GP.STOP_ON.valueOf(params.getProperty("stopOn",
				"GENERATIONS"));

		// Set the max-depth for the trees
		this.maxDepth = Integer.parseInt(params.getProperty("maxDepth", "17"));

		// Set the max allowed size (number of nodes) for a tree
		this.maxSize = Integer.parseInt(params.getProperty("maxSize", "300"));

		// Set the min build depth
		this.minBuildDepth = Integer.parseInt(params.getProperty(
				"minBuildDepth", "0"));

		// Set the max build depth
		this.maxBuildDepth = Integer.parseInt(params.getProperty(
				"maxBuildDepth", "5"));

		// Set the max attempts for crossover
		this.maxCrossAttempts = Integer.parseInt(params.getProperty(
				"maxCrossAttempts", "2"));

		// Set the max attempts for finding unique individuals on init
		this.maxUniqueRetries = Integer.parseInt(params.getProperty(
				"maxUniqueRetries", "50"));

		// Set the tournament size
		this.tournamentSize = Integer.parseInt(params.getProperty(
				"tournamentSize", "7"));

		// Set the discardSecondChild flag
		this.discardSecondChild = Boolean.parseBoolean(params.getProperty(
				"discardSecondChild", "false"));

		// Set the stopOnOptimal flag
		this.stopOnOptimal = Boolean.parseBoolean(params.getProperty(
				"stopOnOptimal", "true"));

		// Set the number of elites to use (default 0)
		this.numElites = Integer.parseInt(params.getProperty("numElites", "0"));

		// Set the number of layers
		this.numLayers = Integer
				.parseInt(params.getProperty("numLayers", "10"));

		// Set the age gap
		this.ageGap = Integer.parseInt(params.getProperty("ageGap", "10"));

		// Set the layer scheme
		this.layerScheme = params.getProperty("layerScheme",
				"ec.research.gp.layers.AlpsLayerScheme");


		// Set how often we collect general stats
		this.statCollectGens = Integer.parseInt(params.getProperty(
				"statCollectGens", "100"));

		// Set how often we collect fitness stats (if not by gens)
		this.statCollectEvals = Long.parseLong(params.getProperty(
				"statCollectEvals", "100"));

		// Do we collect the stats on the tree tags?
		this.doTrackTreeTags = Boolean.parseBoolean(params.getProperty(
				"doTrackTreeTags", "false"));

		// Max level at which to track tree tags
		this.maxTagTrackingLevel = Integer.parseInt(params.getProperty(
				"maxTagTrackingLevel", "4"));

		// What tag depth do we use for tracking the tree tags?
		this.tagTrackingDepth = Integer.parseInt(params.getProperty(
				"tagTrackingDepth", "1"));

		// Set whether or not the tag levels should change (for ParetoGP DF)
		this.doChangeTagLevel = Boolean.parseBoolean(params.getProperty(
				"doChangeTagLevel", "false"));

		// Set whether or not the tag level change should cycle (for ParetoGP
		// DF)
		this.doCycleTagLevels = Boolean.parseBoolean(params.getProperty(
				"doCycleTagLevels", "false"));

		// Set the #gens for changing the tag level (for ParetoGP DF)
		this.tagLevelChangeGens = Integer.parseInt(params.getProperty(
				"tagLevelChangeGens", "200"));

		// Set the initial tag level
		this.tagLevel = Integer.parseInt(params.getProperty("tagLevel", "0"));

		// Set the initial tag depth
		this.tagDepth = Integer.parseInt(params.getProperty("tagDepth", "200"));

		// Set the ParetoGP objectives, if any
		this.objectives = this.params.getProperty("objectives");

		// Do we try to create a uniquely tagged random individual?
		this.doUniqueTagRandom = Boolean.parseBoolean(this.params.getProperty(
				"doUniqueTagRandom", "false"));

		// Do we try to break ties by size?
		this.doSizeBreakTies = Boolean.parseBoolean(this.params.getProperty(
				"doSizeBreakTies", "true"));
	}

	/**
	 * Sets the problem to the given problem
	 * 
	 * @param p
	 *            the problem
	 */
	public void setProblem(Problem p) {
		this.problem = p;
	}

	/**
	 * 
	 * @return the Problem to run
	 */
	public Problem getProblem() {
		return this.problem;
	}

	/**
	 * Sets the function set to the given set of nodes and also adds node
	 * mappings to be used with Individual.fromString().
	 * 
	 * @param functionSet
	 *            the function set
	 */
	public void setFunctionSet(List<Node> functionSet) {
		this.functionSet = functionSet;

		for (Node node : functionSet) {
			this.nodeMappings.put(node.toString(), node);
		}

	}

	/**
	 * @return the function set
	 */
	public List<Node> getFunctionSet() {
		return functionSet;
	}

	/**
	 * Sets the terminal set to the given set of nodes and also adds node
	 * mappings to be used with Individual.fromString().
	 * 
	 * @param terminalSet
	 *            the terminal set
	 */
	public void setTerminalSet(List<Node> terminalSet) {
		this.terminalSet = terminalSet;

		for (Node node : terminalSet) {
			this.nodeMappings.put(node.toString(), node);
		}
	}

	/**
	 * 
	 * @return the terminal set
	 */
	public List<Node> getTerminalSet() {
		return terminalSet;
	}

	/**
	 * Gets the node associated with the given string value. Must be a node from
	 * the function or terminal set.
	 * 
	 * @param nodeString
	 *            the toString() value of the node
	 * @return the node associated with the given string
	 */
	public Node getMappedNode(String nodeString) {
		return this.nodeMappings.get(nodeString);
	}

	/**
	 * Sets the crossover probability.
	 * 
	 * @param crossProbability
	 *            the crossover probability
	 */
	public void setCrossProbability(double crossProbability) {
		this.crossProb = crossProbability;
	}

	/**
	 * @return the crossover probability.
	 */
	public double getCrossProbability() {
		return this.crossProb;
	}

	/**
	 * Sets the probability of selecting a function node for crossover
	 * 
	 * @param crossFuncProb
	 *            the probability of selecting a function node for crossover
	 */
	public void setCrossFuncProbability(double crossFuncProb) {
		this.crossFuncProb = crossFuncProb;
	}

	/**
	 * @return the probability of selecting a function node for crossover
	 */
	public double getCrossFuncProbability() {
		return this.crossFuncProb;
	}

	/**
	 * Sets the output directory to the given path
	 * 
	 * @param path
	 *            the path to which to output the run data.
	 */
	public void setOutputDir(String path) {
		this.outputDir = path;
	}

	/**
	 * 
	 * @return the path where the run data is output.
	 */
	public String getOutputDir() {
		return this.outputDir;
	}

	/**
	 * Sets the mutation probability.
	 * 
	 * @param mutationProbability
	 *            the mutation probability
	 */
	public void setMutationProbability(double mutationProbability) {
		mutProb = mutationProbability;
	}

	/**
	 * @return the mutation probability.
	 */
	public double getMutationProbability() {
		return mutProb;
	}

	/**
	 * Sets the random seed for the run.
	 */
	public void setSeed(long seed) {
		this.seed = seed;
	}

	/**
	 * @return the seed that was set for the run, if any.
	 */
	public long getSeed() {
		return seed;
	}

	/**
	 * Sets the population size to the requested number.
	 * 
	 * @param size
	 *            the requested population size.
	 */
	public void setPopSize(int size) {
		popSize = size;
	}

	/**
	 * @return the population size.
	 */
	public int getPopSize() {
		return popSize;
	}

	/**
	 * Sets the number of generations to the requested number.
	 */
	public void setNumGenerations(int numGenerations) {
		this.numGenerations = numGenerations;
	}

	/**
	 * @return the number of generations.
	 */
	public int getNumGenerations() {
		return numGenerations;
	}

	/**
	 * Sets the max number of generations to the requested number.
	 * 
	 * @param numEvaluations
	 *            the max number of evaluations
	 */
	public void setNumEvaluations(long numEvaluations) {
		this.numEvaluations = numEvaluations;
	}

	/**
	 * @return the max number of evaluations
	 */
	public long getNumEvaluations() {
		return this.numEvaluations;
	}

	/**
	 * @return the number of threads to use for evaluations.
	 */
	public int getNumThreads() {
		return numThreads;
	}

	/**
	 * Sets the number of threads to use for evaluations.
	 * 
	 * @param n
	 *            the number of threads to use for evaluations
	 */
	public void setNumThreads(int n) {
		this.numThreads = n;
	}

	/**
	 * 
	 * @return the stop-on type (default is by max num generations)
	 */
	public STOP_ON getStopOn() {
		return this.stopOn;
	}

	/**
	 * Sets the max depth for a tree to the given depth.
	 * 
	 * @param depth
	 *            the max depth for a tree
	 */
	public void setMaxDepth(int depth) {
		maxDepth = depth;
	}

	/**
	 * 
	 * @return the max depth for a tree.
	 */
	public int getMaxDepth() {
		return maxDepth;
	}

	/**
	 * Sets the max allowed size (number of nodes) for a tree.
	 * 
	 * @param maxSize
	 *            the max allowed size (number of nodes) for a tree
	 */
	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	/**
	 * @return the max allowed size (number of nodes) for a tree
	 */
	public int getMaxSize() {
		return this.maxSize;
	}

	/**
	 * Sets the min depth for a newly built random tree
	 * 
	 * @param minBuildDepth
	 *            the min depth for a newly built random tree
	 */
	public void setMinBuildDepth(int minBuildDepth) {
		this.minBuildDepth = minBuildDepth;
	}

	/**
	 * @return the min depth for a newly built random tree
	 */
	public int getMinBuildDepth() {
		return minBuildDepth;
	}

	/**
	 * Sets the max depth for a newly built random tree
	 * 
	 * @param maxBuildDepth
	 *            the max depth for a newly built random tree
	 */
	public void setMaxBuildDepth(int maxBuildDepth) {
		this.maxBuildDepth = maxBuildDepth;
	}

	/**
	 * 
	 * @return the max depth for a newly built random tree
	 */
	public int getMaxBuildDepth() {
		return maxBuildDepth;
	}

	/**
	 * @return the number of individuals to compete in tournament selection.
	 */
	public int getTournamentSize() {
		return tournamentSize;
	}

	/**
	 * @return Whether or not we should discard the second child during
	 *         crossover.
	 */

	public boolean doDiscardSecondChild() {
		return discardSecondChild;
	}

	/**
	 * 
	 * @return Whether or not we should stop when we find an individual with
	 *         optimal fitness.
	 */
	public boolean stopOnOptimal() {
		return stopOnOptimal;
	}

	/**
	 * 
	 * @return the maximum number of crossover attempts before bailing.
	 */
	public int getMaxCrossAttempts() {
		return maxCrossAttempts;
	}

	/**
	 * 
	 * @return the maximum number of retires to find unique individuals on init
	 */
	public int getMaxUniqueRetries() {
		return maxUniqueRetries;
	}

	/**
	 * Sets the number of elites to the requested value.
	 * 
	 * @param numElites
	 *            the number of elites to use for the run
	 */
	public void setNumElites(int numElites) {
		this.numElites = numElites;
	}

	/**
	 * 
	 * @return the number of elites to be kept per generation
	 */
	public int getNumElites() {
		return this.numElites;
	}

	/**
	 * Sets the number of layers to use to the requested number
	 * 
	 * @param numLayers
	 *            the number of layers
	 */
	public void setNumLayers(int numLayers) {
		this.numLayers = numLayers;
	}

	/**
	 * 
	 * @return the number of layers
	 */
	public int getNumLayers() {
		return this.numLayers;
	}

	/**
	 * Sets the age gap to the requested value.
	 * 
	 * @param ageGap
	 *            the age gap
	 */
	public void setAgeGap(int ageGap) {
		this.ageGap = ageGap;
	}

	/**
	 * 
	 * @return the age gap
	 */
	public int getAgeGap() {
		return this.ageGap;
	}

	/**
	 * Sets the layer scheme to the given fully-qualified class name.
	 * 
	 * @param fullyQualifiedClass
	 *            the fully-qualified class for the layer scheme to use
	 */
	public void setLayerScheme(String fullyQualifiedClass) {
		this.layerScheme = fullyQualifiedClass;
	}

	/**
	 * @return the fully-qualified class name of the layer scheme to use
	 */
	public String getLayerScheme() {
		return this.layerScheme;
	}

	/**
	 * @return how often (generations) to collect general stats
	 */
	public int getStatCollectGens() {
		return this.statCollectGens;
	}

	/**
	 * @return how often (evaluations) to collect fitness stats
	 */
	public long getStatCollectEvals() {
		return this.statCollectEvals;
	}

	/**
	 * Sets the flag for whether or not to collect stats on tree tags
	 * 
	 * @param doTrackTreeTags
	 *            whether or not to collect stats on tree tags
	 */
	public void setDoTrackTreeTags(boolean doTrackTreeTags) {
		this.doTrackTreeTags = doTrackTreeTags;
	}

	/**
	 * @return whether or not to track tree tag stats
	 */
	public boolean doTrackTreeTags() {
		return this.doTrackTreeTags;
	}

	/**
	 * Sets the maximum level at which to collect stats on the tree tags
	 * 
	 * @param maxTagTrackingLevel
	 *            the maximum level at which to collect stats on the tree tags
	 */
	public void setMaxTagTrackingLevel(int maxTagTrackingLevel) {
		this.maxTagTrackingLevel = maxTagTrackingLevel;
	}

	/**
	 * @return the maximum level at which to collect stats on the tree tags
	 */
	public int getMaxTagTrackingLevel() {
		return this.maxTagTrackingLevel;
	}

	/**
	 * Sets the tag depth to use for tree tag tracking.
	 * 
	 * @param tagTrackingDepth
	 *            the tag depth to use for tree tag tracking
	 */
	public void setTagTrackingDepth(int tagTrackingDepth) {
		this.tagTrackingDepth = tagTrackingDepth;
	}

	/**
	 * @return the tag depth to use for tree tag tracking
	 */
	public int getTagTrackingDepth() {
		return this.tagTrackingDepth;
	}

	/**
	 * Sets the flag for whether or not we should try to generate a unique
	 * individual at the tag level when we create random individuals.
	 * 
	 * @param doUniqueTagRandom
	 *            whether or not we should try to generate a unique individual
	 *            at the tag level when we create random individuals.
	 */
	public void setDoUniqueTagRandom(boolean doUniqueTagRandom) {
		this.doUniqueTagRandom = doUniqueTagRandom;
	}

	/**
	 * @return whether or not we should try to generate a unique individual at
	 *         the tag level when we create random individuals.
	 */
	public boolean getDoUniqueTagRandom() {
		return this.doUniqueTagRandom;
	}

	/**
	 * Sets how often (generations) to collect general stats.
	 * 
	 * @param statCollectGens
	 *            number of generations between general stats collection
	 */
	public void setStatCollectGens(int statCollectGens) {
		this.statCollectGens = statCollectGens;
	}

	/**
	 * Sets the generic parameter to the given value. This should be used for
	 * non-member parameters except for those that we check for such as seed,
	 * output directory, crossProb, mutProb, popSize, numGenerations,
	 * numEvaluations, numThreads. Use the appropriate setter for all other
	 * parameters.
	 * 
	 * @param paramName
	 *            the name of the parameter
	 * @param value
	 *            the value to set
	 */
	public void setParameter(String paramName, String value) {
		// There's gotta be a better way to do this...
		// if (paramName.equals("seed")) {
		// this.seed = Long.parseLong(value);
		// } else if (paramName.equals("outputDir")) {
		// this.outputDir = value;
		// } else if (paramName.equals("crossProb")) {
		// this.crossProb = Double.parseDouble(value);
		// } else if (paramName.equals("mutProb")) {
		// this.mutProb = Double.parseDouble(value);
		// } else if (paramName.equals("numGenerations")) {
		// this.numGenerations = Integer.parseInt(value);
		// } else if (paramName.equals("numEvaluations")) {
		// this.numEvaluations = Long.parseLong(value);
		// } else if (paramName.equals("popSize")) {
		// this.popSize = Integer.parseInt(value);
		// } else if (paramName.equals("numThreads")) {
		// this.numThreads = Integer.parseInt(value);
		// }
		this.params.setProperty(paramName, value);
	}

	/**
	 * Gets a named parameter's value.
	 * 
	 * @param paramName
	 *            the name of the parameter whose value we should fetch
	 * @return the value for the parameter or null if it doesn't exist
	 */
	public String getParameter(String paramName) {
		return params.getProperty(paramName);
	}

	/**
	 * Set whether or not the tag levels should change (for ParetoGP DF)
	 * 
	 * @param doChangeTagLevel
	 *            whether or not the tag levels should change
	 */
	public void setDoChangeTagLevel(boolean doChangeTagLevel) {
		this.doChangeTagLevel = doChangeTagLevel;
	}

	/**
	 * @return whether or not the tag levels should change
	 */
	public boolean doChangeTagLevel() {
		return this.doChangeTagLevel;
	}

	/**
	 * Set whether or not the tag level change should cycle (for ParetoGP DF)
	 * 
	 * @param doCycleTagLevels
	 *            whether or not the tag level change should cycle
	 */
	public void setDoCycleTagLevels(boolean doCycleTagLevels) {
		this.doCycleTagLevels = doCycleTagLevels;
	}

	/**
	 * Set whether or not the tag level change should cycle (for ParetoGP DF)
	 * 
	 * @param doCycleTagLevels
	 *            whether or not the tag level change should cycle
	 */
	public boolean doCycleTagLevels() {
		return this.doCycleTagLevels;
	}

	/**
	 * Set the #gens for changing the tag level (for ParetoGP DF)
	 * 
	 * @param tagLevelChangeGens
	 *            the #gens for changing the tag level
	 */
	public void setTagLevelChangeGens(int tagLevelChangeGens) {
		this.tagLevelChangeGens = tagLevelChangeGens;
	}

	/**
	 * @return the #gens for changing the tag level
	 */
	public int getTagLevelChangeGens() {
		return this.tagLevelChangeGens;
	}

	/**
	 * Set the initial tag level
	 * 
	 * @param tagLevel
	 *            the initial tag level
	 */
	public void setTagLevel(int tagLevel) {
		this.tagLevel = tagLevel;
	}

	/**
	 * @return the initial tag level
	 */
	public int getTagLevel() {
		return this.tagLevel;
	}

	/**
	 * Set the initial tag depth
	 * 
	 * @param tagDepth
	 *            the initial tag depth
	 */
	public void setTagDepth(int tagDepth) {
		this.tagDepth = tagDepth;
	}

	/**
	 * @return the initial tag depth
	 */
	public int getTagDepth() {
		return this.tagDepth;
	}

	/**
	 * Sets the flag for whether or not to try to size-break ties.
	 * 
	 * @param doSizeBreakTies
	 *            whether or not to try to size-break ties
	 */
	public void setDoSizeBreakTies(boolean doSizeBreakTies) {
		this.doSizeBreakTies = doSizeBreakTies;
	}

	/**
	 * @return whether or not to try to size-break ties
	 */
	public boolean getDoSizeBreakTies() {
		return this.doSizeBreakTies;
	}

	/**
	 * Set the {@link ParetoGP.OBJECTIVES} to use
	 * 
	 * @param objectives
	 *            the objectives to use for the {@link ParetoGP}
	 */
	public void setObjectives(String objectives) {

	}

	/**
	 * @return the {@link ParetoGP.OBJECTIVES} to use for the {@link ParetoGP}
	 */
	public String getObjectives() {
		return this.objectives;
	}
}
