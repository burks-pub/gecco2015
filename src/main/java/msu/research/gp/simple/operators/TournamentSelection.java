package msu.research.gp.simple.operators;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import msu.research.gp.simple.representation.Individual;
import msu.research.gp.simple.util.Context;

/**
 * Simple implementation of tournament selection.
 * 
 * @author Armand R. Burks
 * 
 */
public class TournamentSelection extends Selection {
	/**
	 * Selects the best individual from the collection of individuals.
	 * 
	 * @param individuals
	 *            the collection of individuals from which to select
	 * 
	 * @return the index of the best individual in the collection.
	 */
	public static int selectFromAllIndividuals(Vector<Individual> individuals) {
		double bestFitness = -1; // Fitness is guaranteed between 0 and 1,
		// inclusive
		int bestIndividualIndex = 0;

		// Just take the best among all individuals.
		for (int i = 0; i < individuals.size(); i++) {
			// Check the individual's fitness and see if it's the best so far.
			double fitness = individuals.get(i).getFitness();
			if (fitness > bestFitness) {
				bestFitness = fitness;
				bestIndividualIndex = i;
			}
		}

		return bestIndividualIndex;
	}

	/**
	 * Performs tournament selection on the current population. Ties are broken
	 * by the first individual we come across with that value.
	 * 
	 * @param individuals
	 *            the individuals from which to select
	 * 
	 * @return the index of the selected individual
	 */
	public int selectOne(Context context, Vector<Individual> individuals) {
		int tournamentSize = context.getConfig().getTournamentSize();

		// If tournamentSize is <= individuals.size(), use them all!
		if (tournamentSize >= individuals.size()) {
			return selectFromAllIndividuals(individuals);
		}

		// Holds the set of indices for the tournament. We do this to ensure
		// that we don't pick the same individuals.
		Set<Integer> tournamentIndices = new HashSet<Integer>();

		// Pick tournamentSize random individuals (or all individuals if less!)
		while (tournamentIndices.size() < tournamentSize) {
			tournamentIndices
					.add(context.randBetween(0, individuals.size() - 1));
		}

		// Get the best individual and add its index to the set
		double bestFitness = -1; // Fitness is between 0 and 1, inclusive
		int bestIndividualIndex = 0;

		for (int i : tournamentIndices) {
			// Check the individual's fitness and see if it's the best so far.
			double fitness = individuals.get(i).getFitness();
			if (fitness > bestFitness) {
				bestFitness = fitness;
				bestIndividualIndex = i;
			}
		}

		return bestIndividualIndex;
	}

	/**
	 * Performs tournament selection on the current population. Ties are broken
	 * by the first individual we come across with that value.
	 * 
	 * @param individuals
	 *            the individuals from which to select
	 * 
	 * @return the indices of the selected individuals
	 */
	public int[] select(Context context, Vector<Individual> individuals) {
		int[] parents = new int[2];

		parents[0] = selectOne(context, individuals);
		parents[1] = selectOne(context, individuals);

		// Try up to one more time to get different parents
		if (parents[1] == parents[0]) {
			parents[1] = selectOne(context, individuals);
		}

		return parents;
	}
}
