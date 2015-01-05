package msu.research.gp.statistics;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import msu.research.gp.simple.gp.GP.STOP_ON;
import msu.research.gp.simple.representation.Individual;
import msu.research.gp.simple.util.Config;
import msu.research.gp.util.Utils;

/**
 * Handles the statistics for simple GP.
 * 
 * @author Armand R. Burks
 * 
 */
public class SimpleGPStatistics extends Statistics {
	// Holds a reference to the GP's population so we can gather statistics
	private Vector<Individual> population;

	// The path to the fitness stats file.
	private static final String FITNESS_FILE = "fitness";

	// Holds the output file to which we will write the fitness stats
	private BufferedWriter fitnessOutput;

	// Holds the current best fitness from GP so we don't have to recalculate it
	private double bestFitness;

	// Holds the current avg fitness from GP so we don't have to recalculate it.
	private double avgFitness;

	/**
	 * Convenience method to save the seed to file so we can have it if we need
	 * to re-run this again.
	 * 
	 * @param timestamp
	 *            the timestamp to append to the file name.
	 * 
	 * @throws IOException
	 */
	private void saveSeed(String timestamp) throws IOException {
		BufferedWriter seedFile = new BufferedWriter(new FileWriter(
				config.getOutputDir() + "/" + "seed" + timestamp + ".txt"));

		seedFile.write(Long.toString(this.config.getSeed()));
		seedFile.close();
	}

	/**
	 * Convenience method to setup all the writers for the output files
	 * 
	 * @param timestamp
	 *            the timestamp string to append to the file name.
	 * @throws IOException
	 */
	private void setupOutput(String timestamp) throws IOException {
		this.fitnessOutput = new BufferedWriter(new FileWriter(
				this.config.getOutputDir() + "/" + FITNESS_FILE + timestamp));
	}

	/**
	 * Initializes a new SimpleGPStatistics object with the given population.
	 * 
	 * @param population
	 *            the initial population
	 * @throws IOException
	 */
	public SimpleGPStatistics(Vector<Individual> population, Config config)
			throws IOException {
		super(config);

		this.population = population;

		// Save the seed.
		saveSeed(TIMESTAMP_FORMAT.format(this.startTime));

		// Setup all the output file writers
		setupOutput(TIMESTAMP_FORMAT.format(this.startTime));
	}

	/**
	 * Convenience method to collect and output stats on fitness.
	 * 
	 * The output format is tab-delimited: generation, total evaluations, best
	 * fitness, average fitness
	 * 
	 * @param generation
	 *            the current generation
	 */
	public void fitnessStats(int generation) {
		Utils.writeOutput(String.format("%s\t%s\t%s\t%s", generation,
				this.totalEvaluations, this.bestFitness, this.avgFitness),
				this.fitnessOutput);
	}

	/**
	 * Sets the current generation best and average fitness. Should be called at
	 * the end of the generation before postEvaluation stats. This is to save
	 * time so we don't have to recalculate it.
	 * 
	 * @param best
	 *            the current best fitness value
	 * @param avg
	 *            the current average fitness
	 */
	public void setFitnessInfo(double best, double avg) {
		this.bestFitness = best;
		this.avgFitness = avg;
	}

	public void postEvaluationStats(int generation) {
		fitnessStats(generation);

		// Output the tree stats
		treeStats(generation);

		// Output the tree tag stats
		treeTagStats(generation, this.population);
	}

	public void preGenerationStats(int generationNum) {

	}

	public void postGenerationStats(int generation) {

	}

	/**
	 * Method to run after the evolution has completed.
	 * 
	 * @param generation
	 *            the current generation number
	 */
	public void postEvolutionStats(int generation) {
		// Do we need to do one last round of postEval stats?
		if (config.getStopOn().equals(STOP_ON.GENERATIONS)
				&& generation <= config.getNumGenerations()) {
			postEvaluationStats(generation);
		} else if (totalEvaluations < config.getNumEvaluations()) {
			postEvaluationStats(generation);
		}
	}
}
