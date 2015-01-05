package msu.research.gp.statistics;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import msu.research.gp.simple.representation.Individual;
import msu.research.gp.simple.util.Config;
import msu.research.gp.util.Utils;

/**
 * Similar to the SimpleGPStatistics class, but customized to handle a layered
 * population structure.
 * 
 * @author Armand R. Burks
 * 
 */
public class LayeredGPStatistics extends Statistics {
	// Holds the layered population.
	private List<Vector<Individual>> population;

	// Holds the total number of layers that will be populated during the run.
	private int totalLayers;

	// The path to the layer fitness stats file.
	private static final String LAYER_FITNESS_FILE = "fitness";

	// The writer responsible for writing the layer fitness stats file.
	private BufferedWriter layerFitnessOutput;

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
	 * Convenience method to setup all the (many) writers needed for the
	 * different output files. This is done to hide all the ugliness from the
	 * constructor.
	 * 
	 * @param timestamp
	 *            the timestamp to append to the end of the filename
	 * 
	 * @throws IOException
	 */
	private void setupOutput(String timestamp) throws IOException {
		// Just initialize all the writers.
		this.layerFitnessOutput = new BufferedWriter(new FileWriter(
				config.getOutputDir() + "/" + LAYER_FITNESS_FILE + timestamp));
	}

	/**
	 * Creates a new LayeredGPStatistics object, using the given layered
	 * population.
	 * 
	 * @param population
	 *            the layered population on which to collect statistics.
	 * @throws IOException
	 */
	public LayeredGPStatistics(List<Vector<Individual>> population,
			Config config) throws IOException {
		super(config);

		// Get the current time so we can organize our output with a timestamp.
		String timestamp = TIMESTAMP_FORMAT.format(this.startTime);

		this.population = population;
		this.totalLayers = config.getNumLayers();

		// Setup all the writers.
		setupOutput(timestamp);

		// Save the seed to file to make life easier if we need to re-run
		saveSeed(timestamp);
	}

	/**
	 * Convenience method to collect stats on the fitness in each layer. We
	 * collect the average and max fitness for each layer.
	 * 
	 * @param generation
	 *            the current generation number
	 */
	public void layerFitnessStats(int generation) {
		// Output the generation number first
		StringBuilder output = new StringBuilder();
		output.append(String
				.format("%s\t%s", generation, this.totalEvaluations));

		// For each layer, output the max (for now) fitness.
		for (int i = 0; i < population.size(); i++) {
			Vector<Individual> layer = population.get(i);

			if (!layer.isEmpty()) {
				// Find the average and max fitness for the layer.
				double avgFitness = 0, maxFitness = 0;

				for (Individual individual : layer) {
					double fitness = individual.getFitness();
					avgFitness += fitness;

					if (fitness > maxFitness) {
						maxFitness = fitness;
					}
				}

				avgFitness /= layer.size();

				output.append(String.format("\t%f:%f", avgFitness, maxFitness));
			} else {
				output.append("\t0:0");
			}
		}

		// Fill zeroes for non-existent layers.
		for (int i = population.size(); i < totalLayers; i++) {
			output.append("\t0:0");
		}

		// Write the record
		Utils.writeOutput(output.toString(), this.layerFitnessOutput);
	}

	@Override
	public void preGenerationStats(int generation) {
	}

	@Override
	public void postEvaluationStats(int generation) {
		// Do the layer fitness stats.
		layerFitnessStats(generation);

		// Output the tree stats
		treeStats(generation);
	}

	@Override
	public void postGenerationStats(int generation) {
	}

	@Override
	public void postEvolutionStats(int generation) {
		// Do a final post-eval stats since it wouldn't get called otherwise.
		postEvaluationStats(generation);
	}
}
