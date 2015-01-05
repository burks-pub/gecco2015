package msu.research.gp.statistics;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Vector;

import msu.research.gp.pareto.DiversityUtils;
import msu.research.gp.simple.representation.Individual;
import msu.research.gp.simple.util.Config;
import msu.research.gp.util.Utils;

/**
 * Simple Statistics class defining the behavior for performing general
 * statstics.
 * 
 * @author Armand R. Burks
 * 
 */
public abstract class Statistics {
	// Holds the output file to which we will write the tree stats
	private BufferedWriter treeStatsOutput;

	// Holds the output file to which we will write the tree tags stats
	private BufferedWriter treeTagStatsOutput;

	// Timestamp format to append to the end of each file for multiple runs.
	protected static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd-HHmm.ssS");

	// Holds the start time which will get appended to output file names
	protected Date startTime;

	// Config object for the run, shared with subclasses.
	protected Config config;

	// Holds the current avg tree size from GP so we don't have to recalculate
	// it.
	private double avgTreeSize;

	// Holds the current avg tree depth from GP so we don't have to recalculate
	// it.
	private double avgTreeDepth;

	// The DiversityUtils object for doing the tree tags stats
	private DiversityUtils diversityUtils;

	// Holds the current total evaluations from GP so we don't have to
	// recalculate it.
	protected long totalEvaluations;

	// Most recent generation where post-eval stats were output.
	protected int lastOutputGen;

	// The path to the tree stats file.
	private static final String TREE_STATS_FILE = "size";

	// The path to the tree tags stats file.
	private static final String TREE_TAG_STATS_FILE = "treeTags";

	/**
	 * Creates a new statistics object and sets up some shared objects for
	 * subclasses. Should be called by all subclasses.
	 * 
	 * @throws IOException
	 */
	public Statistics(Config config) throws IOException {
		this.config = config;
		this.startTime = new Date(System.currentTimeMillis());
		String timestamp = TIMESTAMP_FORMAT.format(this.startTime);

		// Setup the output writers
		this.treeStatsOutput = new BufferedWriter(new FileWriter(
				this.config.getOutputDir() + "/" + TREE_STATS_FILE + timestamp));
		this.treeTagStatsOutput = new BufferedWriter(new FileWriter(
				config.getOutputDir() + "/" + TREE_TAG_STATS_FILE + timestamp));

		// Setup the DiversityUtils object for doing the tree tags stats
		this.diversityUtils = new DiversityUtils(this.config);
	}

	/**
	 * Sets the current average tree size and depth. Should be called at the end
	 * of the generation before postEvaluation stats. This is done so we don't
	 * have to recalculate it.
	 * 
	 * @param avgSize
	 *            the current average tree size
	 * @param avgDepth
	 *            the current average tree depth
	 */
	public void setTreeStatsInfo(double avgSize, double avgDepth) {
		this.avgTreeSize = avgSize;
		this.avgTreeDepth = avgDepth;
	}

	/**
	 * Sets the current total of evaluations performed.
	 * 
	 * @param evaluations
	 *            the current total number of evaluations
	 */
	public void setNumEvaluations(long evaluations) {
		this.totalEvaluations = evaluations;
	}

	/**
	 * Convenience method to collect and output stats on the tree
	 * size/depth/etc.
	 * 
	 * @param generation
	 *            the current generation
	 */
	public void treeStats(int generation) {
		// Write the tree stats output
		Utils.writeOutput(String.format("%s\t%s\t%s\t%s", generation,
				this.totalEvaluations, this.avgTreeSize, this.avgTreeDepth),
				this.treeStatsOutput);
	}

	/**
	 * Collects stats on the tree tags at various levels.
	 * 
	 * @param generation
	 *            the current population
	 * 
	 * @param population
	 *            the population to collect the stats on
	 */
	public void treeTagStats(int generation, Vector<Individual> population) {
		// Should we even bother?
		if (this.config.doTrackTreeTags()) {
			StringBuilder tagOutput = new StringBuilder();
			tagOutput.append(String.format("%s\t%s", generation,
					this.totalEvaluations));

			for (int i = 0; i <= this.config.getMaxTagTrackingLevel(); i++) {
				// Collect the tags at from level i
				Map<String, double[]> tagStats = this.diversityUtils
						.getIndividualTags(population, i,
								this.config.getTagTrackingDepth());

				for (String tag : tagStats.keySet()) {
					double[] stats = tagStats.get(tag);

					// We'll put all the levels into a single line
					// The output is: tag level:tag density:avg. fitness
					tagOutput.append(String.format("\t%s:%s:%s", i, stats[0],
							stats[1]));
				}
			}

			// Finally output the data
			Utils.writeOutput(tagOutput.toString(),
					this.treeTagStatsOutput);
		}
	}

	/**
	 * Handles the stats collection/calculation before a new generation gets
	 * created.
	 * 
	 * @param generation
	 *            the current generation number
	 */
	public abstract void preGenerationStats(int generation);

	/**
	 * Handles the stats collection/calculation that needs to be done
	 * immediately after a population has been evaluated.
	 * 
	 * @param generation
	 */
	public abstract void postEvaluationStats(int generation);

	/**
	 * Handles the stats collection/calculation after a new generation gets
	 * created.
	 * 
	 * @param generation
	 *            the current generation number
	 */
	public abstract void postGenerationStats(int generation);

	/**
	 * Handles the stats collection/calculation after the evolution loop has
	 * terminated.
	 * 
	 * @param generation
	 *            the current generation number
	 */
	public abstract void postEvolutionStats(int generation);
}
