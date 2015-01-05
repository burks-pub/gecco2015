package msu.research.gp.simple.operators;

import java.util.Vector;

import msu.research.gp.simple.representation.Individual;
import msu.research.gp.simple.util.Context;

/**
 * Simple random mating selection class.
 * 
 * @author Armand R. Burks
 * 
 */
public class RandomMatingSelection extends Selection {
	/**
	 * Selects a random parent index from the collection of individuals..
	 * 
	 * @param individuals
	 *            the collection of individuals from which to select
	 * 
	 * @param context
	 *            the Context for this run
	 * 
	 * @return the index of the selected parent.
	 */
	public int selectOne(Context context, Vector<Individual> individuals) {
		return context.randBetween(0, individuals.size() - 1);
	}

	/**
	 * Simple random mating selection method that returns two randomly-selected
	 * parent indices.
	 * 
	 * @param context
	 *            the context for the run
	 * @param population
	 *            the population from which to select
	 * @return an array of two parent indices
	 */
	public int[] select(Context context, Vector<Individual> population) {
		int[] parents = new int[2];

		parents[0] = selectOne(context, population);
		parents[1] = selectOne(context, population);

		// Try up to one more time to get different parents
		if (parents[1] == parents[0]) {
			parents[1] = selectOne(context, population);
		}

		return parents;
	}
}
