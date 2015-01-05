package msu.research.gp.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;

import msu.research.gp.simple.representation.Individual;

/**
 * Contains various utility methods for GP statistics.
 * 
 * @author Armand R. Burks
 * 
 */
public class Utils {
	private static final Logger logger = Logger.getLogger(Utils.class);

	/**
	 * Calculates the density of each tag (the generic tag) in the given
	 * collection of individuals.
	 * 
	 * @param individuals
	 *            the layer from the population
	 * 
	 * @param capacity
	 *            the capacity of each layer (so we can more accurately
	 *            calculate the density)
	 * 
	 * @return a mapping of the density of each tag in the given layer
	 */
	public static Map<String, Double> getTagDensities(
			Collection<Individual> individuals, int capacity) {
		HashMap<String, Double> densities = new HashMap<String, Double>();

		// Tally up the total descendants for each tag in the layer
		for (Individual individual : individuals) {
			String tag = individual.getTag();
			if (!densities.containsKey(tag)) {
				densities.put(tag, 0.0);
			}
			densities.put(tag, densities.get(tag) + 1.0);
		}

		// Now calculate the density for each tag
		for (String tag : densities.keySet()) {
			densities.put(tag, densities.get(tag) / (double) capacity);
		}

		return densities;
	}

	/**
	 * Convenience method to write a line of output using the specified data and
	 * the given BufferedWriter.
	 * 
	 * @param data
	 *            the line of output to write
	 * @param writer
	 *            the BufferedWriter to use
	 */
	public static void writeOutput(String data, BufferedWriter writer) {
		try {
			writer.write(data);
			writer.newLine();
			writer.flush();
		} catch (IOException e) {
			logger.error(e);
		}
	}

}
