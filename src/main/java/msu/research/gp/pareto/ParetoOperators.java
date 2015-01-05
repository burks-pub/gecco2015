package msu.research.gp.pareto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

import msu.research.gp.pareto.ParetoGP.OBJECTIVES;
import msu.research.gp.simple.representation.Individual;
import msu.research.gp.simple.util.Context;
import msu.research.gp.util.Utils;

/**
 * Tournament selection based on the Schmidt and Lipson Age-Fitness Pareto
 * Optimization approach.
 * 
 * This implementation uses fitness and genetic marker diversity as the
 * objectives that define the Pareto front. Following Schmidt and Lipson, we
 * select tournamentSize individuals and form the local non-dominated front from
 * them, discarding any dominated individuals. This is done until the population
 * reaches the target size or there are no dominated solutions.
 * 
 * @author Armand R. Burks
 * 
 */
public class ParetoOperators {
	// Log4J logger for any output messages.
	private static Logger logger = Logger.getLogger(ParetoOperators.class);

	/**
	 * Convenience method to get a set of tournament indices.
	 * 
	 * @param context
	 *            the context for this run
	 * 
	 * @param population
	 *            the population from which to select
	 * 
	 * @return a set of tournamentSize indices of individuals from the
	 *         population to be used for the tournament
	 */
	public static Set<Integer> getTournamentIndices(Context context,
			Vector<Individual> population) {
		// Use a linked hash set so the other rand won't interfere with
		// reproducibility!
		Set<Integer> tournamentIndices = new LinkedHashSet<Integer>();
		int tournamentSize = context.getConfig().getTournamentSize();

		if (tournamentSize >= population.size()) {
			tournamentSize = population.size();
		}

		// Pick tournamentSize random individuals (or all individuals if
		// less!)
		while ((int) tournamentIndices.size() < tournamentSize) {
			tournamentIndices
					.add(context.randBetween(0, population.size() - 1));
		}

		return tournamentIndices;
	}

	/**
	 * Determines if the first individual is better on the age objective
	 * 
	 * @param candidate1
	 *            the individual we're testing to see if it is dominated
	 * 
	 * @param candidate2
	 *            the individual we're comparing the first candidate against
	 * 
	 * @return true if the first individual is better on the age objective,
	 *         false otherwise
	 */
	public static boolean isBetterAge(Individual candidate1,
			Individual candidate2) {
		return (candidate1.getAge() < candidate2.getAge());
	}

	/**
	 * Determines if the first individual is better on the density objective
	 * 
	 * @param candidate1
	 *            the individual we're testing to see if it is dominated
	 * 
	 * @param candidate2
	 *            the individual we're comparing the first candidate against
	 * 
	 * @param densities
	 *            the mapping of genetic marker densities in the population
	 * 
	 * @return true if the first individual is better on the density objective,
	 *         false otherwise
	 */
	public static boolean isBetterDensity(Individual candidate1,
			Individual candidate2, Map<String, Double> densities) {
		return (densities.get(candidate1.getTag())
				- densities.get(candidate2.getTag()) < 0);
	}

	/**
	 * Determines if the first individual is better on the fitness objective
	 * 
	 * @param candidate1
	 *            the individual we're testing to see if it is dominated
	 * 
	 * @param candidate2
	 *            the individual we're comparing the first candidate against
	 * 
	 * @return true if the first individual is better on the fitness objective,
	 *         false otherwise
	 */
	public static boolean isBetterFitness(Individual candidate1,
			Individual candidate2) {
		return (candidate1.getFitness() - candidate2.getFitness() > 0);
	}

	/**
	 * Determines if the first individual is better on at least one of the
	 * objectives.
	 * 
	 * @param candidate1
	 *            the individual we're testing to see if it is dominated
	 * 
	 * @param candidate2
	 *            the individual we're comparing the first candidate against
	 * 
	 * @param densities
	 *            the mapping of tag densities in the population
	 * 
	 * @param objectives
	 *            the objectives against which to compare individuals
	 * 
	 * @return true if the first individual is better on at least one of the
	 *         objectives, false otherwise
	 */
	public static boolean isBetterOnOne(Individual candidate1,
			Individual candidate2, ParetoGP.OBJECTIVES objectives,
			Map<String, Double> densities) {
		boolean ret = false;

		// Compare on age/density
		if (objectives.equals(OBJECTIVES.AGE_DENSITY)) {
			ret = isBetterAge(candidate1, candidate2)
					|| isBetterDensity(candidate1, candidate2, densities);
		}

		// Compare on age/fitness
		else if (objectives.equals(OBJECTIVES.AGE_FITNESS)) {
			// logger.debug("Comparing on age/fitness objectives");
			ret = isBetterAge(candidate1, candidate2)
					|| isBetterFitness(candidate1, candidate2);
		}

		// Compare on density/fitness
		else if (objectives.equals(OBJECTIVES.DENSITY_FITNESS)) {
			ret = isBetterDensity(candidate1, candidate2, densities)
					|| isBetterFitness(candidate1, candidate2);
		}

		// Compare on age/density/fitness
		else if (objectives.equals(OBJECTIVES.AGE_DENSITY_FITNESS)) {
			ret = isBetterAge(candidate1, candidate2)
					|| isBetterDensity(candidate1, candidate2, densities)
					|| isBetterFitness(candidate1, candidate2);
		}

		return ret;
	}

	/**
	 * Convenience method to determine whether or not individuals are equal on
	 * all objectives, depending on what those objectives are.
	 * 
	 * @param ageDiff
	 *            the age difference of the two individuals being compared
	 * 
	 * @param densityDiff
	 *            the density difference of the two individuals being compared
	 * 
	 * @param fitnessComparison
	 *            the fitness comparison of the two individuals being compared
	 *            (i.e. from ind1.compareTo(ind2))
	 * 
	 * @param objectives
	 *            the objectives we're using to compare the individuals
	 * 
	 * @return whether or not the individuals are equal on all objectives
	 */
	public static boolean individualsEqualOnObjectives(int ageDiff,
			double densityDiff, int fitnessComparison, OBJECTIVES objectives) {
		boolean ret = false;

		if (objectives.equals(OBJECTIVES.AGE_DENSITY_FITNESS)) {
			return (ageDiff == 0 && densityDiff == 0.0 && fitnessComparison == 0);
		} else if (objectives.equals(OBJECTIVES.AGE_DENSITY)) {
			return (ageDiff == 0 && densityDiff == 0.0);
		} else if (objectives.equals(OBJECTIVES.AGE_FITNESS)) {
			return (ageDiff == 0 && fitnessComparison == 0);
		} else if (objectives.equals(OBJECTIVES.DENSITY_FITNESS)) {
			return (densityDiff == 0.0 && fitnessComparison == 0);
		}

		return ret;
	}

	/**
	 * Utility method to determine if the first candidate is dominated by the
	 * second candidate. Ties are broken by size, and if the size is equal, we
	 * pick an individual over the other.
	 * 
	 * @param candidate1
	 *            the individual we're testing to see if it is dominated
	 * 
	 * @param candidate2
	 *            the individual we're comparing the first candidate against
	 * 
	 * @param densities
	 *            the mapping of tag densities in the population
	 * 
	 * @param objectives
	 *            the objectives against which to compare individuals
	 * 
	 * @return true if the first individual is dominated by the second
	 *         individual
	 */
	public static boolean individualIsDominated(Individual candidate1,
			Individual candidate2, Map<String, Double> densities,
			Context context, ParetoGP.OBJECTIVES objectives) {
		boolean ret = false;
		
		// First compare their fitnesses.
		int fitnessComparison = candidate1.compareTo(candidate2);

		// Now see if one of them belong to a more rare tag.
		double densityDiff = densities.get(candidate1.getTag())
				- densities.get(candidate2.getTag());

		// Is one younger than the other?
		int ageDiff = candidate1.getAge() - candidate2.getAge();

		// Are they equal on all objectives?
		boolean tie = individualsEqualOnObjectives(ageDiff, densityDiff,
				fitnessComparison, objectives);

		if (!tie
				&& !isBetterOnOne(candidate1, candidate2, objectives, densities)) {
			ret = true;

		}

		// Break ties by size if they aren't the same size.
		if (tie) {
			// Prefer the smaller individual (don't do this for original alg.)
			if (objectives != OBJECTIVES.AGE_FITNESS
					&& context.getConfig().getDoSizeBreakTies()) {
				if (candidate1.getNumNodes() < candidate2.getNumNodes()) {
					ret = false;
				} else if (candidate2.getNumNodes() < candidate1.getNumNodes()) {
					ret = true;
				} else {
					ret = true;
				}
			}

			/*
			 * If we couldn't break the tie, just pick the first. We have to be
			 * sure that we pick one of them because if we pick
			 * probabilistically, we could end up allowing the tie.
			 */
			else {
				ret = true;
			}
		}
		return ret;
	}

	/**
	 * Finds the global pareto front from the given population
	 * 
	 * @param context
	 *            the context for the run
	 * 
	 * @param population
	 *            the population to use
	 * 
	 * @param objectives
	 *            the objectives against which to compare individuals
	 * 
	 * @return the set of individuals that form global pareto front from the
	 *         given population
	 */
	public static Set<Individual> getGlobalNonDominatedFront(Context context,
			Vector<Individual> population, ParetoGP.OBJECTIVES objectives) {
		HashSet<Individual> paretoFront = new HashSet<Individual>();

		// Maps the indices of individuals to their set of dominated indices
		HashMap<Integer, Set<Integer>> dominationMap = new HashMap<Integer, Set<Integer>>();

		for (int i = 0; i < population.size(); i++) {
			dominationMap.put(i, new HashSet<Integer>());
		}

		// Calculate the tag densities.
		Map<String, Double> densities = Utils.getTagDensities(population,
				population.size());

		// Compare every individual to every other individual and find the
		// non-dominated individuals.
		for (int i = 0; i < population.size(); i++) {
			Individual ind1 = population.get(i);
			boolean isDominated = false;

			for (int j = 0; j < population.size(); j++) {
				Individual ind2 = population.get(j);

				// We have to check dominated to make sure inds can't eliminate
				// each other in case of tie breaking
				if (!dominationMap.get(i).contains(j)) { // only happens on tie
					if (j != i
							&& individualIsDominated(ind1, ind2, densities,
									context, objectives)) {
						isDominated = true;
						dominationMap.get(j).add(i); // Ind2 dominates ind1
					}
				}
			}

			// Is the individual non-dominated?
			if (!isDominated) {
				paretoFront.add(ind1);
			}
		}

		return paretoFront;
	}

	/**
	 * Helper method to do the deletion (using a single tag level for the
	 * density objective) after we've already created the global non-dominated
	 * front.
	 * 
	 * @param population
	 *            the population to shrink
	 * 
	 * @param targetSize
	 *            the target population size to shrink down to
	 * 
	 * @param nonDominatedFront
	 *            the global non-dominated front
	 * 
	 * @param objectives
	 *            the objectives we're optimizing for
	 * 
	 * @param context
	 *            the context for the run
	 */
	public static void doSingleTagDeletion(Vector<Individual> population,
			int targetSize, Set<Individual> nonDominatedFront,
			OBJECTIVES objectives, Context context) {
		// Calculate the tag densities (so we can find the less frequent tags)
		Map<String, Double> tagDensities = Utils.getTagDensities(population,
				population.size());

		while (population.size() > targetSize
				&& population.size() > nonDominatedFront.size()) {
			// Holds the set of indices of dominated individuals this round.
			HashSet<Individual> dominated = new HashSet<Individual>();

			// Get the set of individuals for the tournament.
			Vector<Individual> tournamentIndividuals = new Vector<Individual>();

			for (int index : getTournamentIndices(context, population)) {
				tournamentIndividuals.add(population.get(index));
			}

			// Remove dominated individuals
			for (Individual candidate1 : tournamentIndividuals) {
				// Don't even consider if it is non-dominated or dominated
				if (!dominated.contains(candidate1)
						&& !nonDominatedFront.contains(candidate1)) {
					// Compare candidate1 to every other individual until we
					// find one that dominates it.
					for (Individual candidate2 : tournamentIndividuals) {
						if (!candidate1.equals(candidate2)
								&& !dominated.contains(candidate2)) {

							// Is candidate 1 dominated by candidate 2?
							if (nonDominatedFront.contains(candidate2)
									|| individualIsDominated(candidate1,
											candidate2, tagDensities, context,
											objectives)) {
								dominated.add(candidate1);
								population.remove(candidate1);
								break;
							}

							// Is candidate 2 dominated by candidate 1?
							else if (individualIsDominated(candidate2,
									candidate1, tagDensities, context,
									objectives)) {
								dominated.add(candidate2);
								population.remove(candidate2);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Performs the pareto selection (really deletion) as in Schmidt and Lipson
	 * 2011 using fitness and genetic marker diversity as the objectives. The
	 * non-dominated front is also returned for convenience.
	 * 
	 * @param context
	 *            the context for the run
	 * 
	 * @param population
	 *            the population on which to operate
	 * 
	 * @param objectives
	 *            the objectives against which to compare individuals
	 * 
	 * @return the current pareto front in the population that was used this
	 *         round
	 */
	public static Set<Individual> delete(Context context,
			Vector<Individual> population, ParetoGP.OBJECTIVES objectives) {
		int targetSize = context.getConfig().getPopSize();

		// Add an extra empty space for the new individual if we're doing the
		// density version
		if (!objectives.equals(OBJECTIVES.AGE_FITNESS)) {
			targetSize -= 1;
		}

		// Get the global pareto front
		Set<Individual> nonDominatedFront = null;

		nonDominatedFront = getGlobalNonDominatedFront(context, population,
				objectives);

		// Stop now if the pareto front is the whole population
		if (nonDominatedFront.size() == population.size()) {
			logger.error("pop size = front size. returning");
			return nonDominatedFront;
		}

		// Do deletion using a single tag level for the objectives
		doSingleTagDeletion(population, targetSize, nonDominatedFront,
				objectives, context);

		return nonDominatedFront;
	}
}
