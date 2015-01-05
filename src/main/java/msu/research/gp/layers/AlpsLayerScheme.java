package msu.research.gp.layers;

import java.util.List;
import java.util.Vector;

import msu.research.gp.simple.representation.Individual;
import msu.research.gp.simple.util.Config;

/**
 * Uses the aging scheme as defined in Hornby's ALPS, where each individual
 * takes the age of its oldest parent, plus one. Elites have their age
 * incremented only if they were used as a parent for the new generation.
 * 
 * Here, individuals (try to) move up to the next layer if their age has reached
 * the max allowable age for their layer.
 * 
 * @author Armand R. Burks
 * 
 */
public class AlpsLayerScheme extends LayerScheme {
	/**
	 * Creates a new AlpsAgingScheme object with the given configuration.
	 * 
	 * @param config
	 *            the configuration object to use for this aging scheme
	 * 
	 * @param layeredPopulation
	 *            the layered population we'll be updating
	 * 
	 * @param parents
	 *            the list that will hold the current-generation parents
	 */
	public AlpsLayerScheme(Config config,
			List<Vector<Individual>> layeredPopulation, List<Individual> parents) {
		super(config, layeredPopulation, parents);
	}

	@Override
	public void updateAge(Individual individual, List<Individual> parents) {
		// Do nothing.
	}

	@Override
	public void updateAges(Vector<Individual> layer, List<Individual> parents,
			int generation) {
		// Do nothing.

	}

	@Override
	public void updateElitesAges(Vector<Individual> layer,
			List<Individual> parents, int numElites) {
		// Check all the elites and increment their age as necesary.
		for (int i = 0; i < numElites; i++) {
			int eliteIndex = layer.size() - 1 - i;
			Individual elite = layer.get(eliteIndex);

			// If the elite was a parent, increment its age by one
			if (parents.contains(elite)) {
				elite.ageIncr();
				parents.remove(elite);
			}
		}
	}

	@Override
	public boolean moveUpCriteria(Individual individual, int layer) {
		// Get the max age for the next layer up (polynomial scheme)
		int maxAge = AlpsLayerScheme.calculateMaxAge(layer, config.getAgeGap(),
				config.getNumLayers());

		// Move them once they've passed the max age.
		return (maxAge > 0 && individual.getAge() > maxAge);
	}

	@Override
	public void updateLayers(LayeredGP layeredGP)
			throws CloneNotSupportedException {
		// Add a new layer if it's time.
		if (doAddLayer(layeredGP)) {
			layeredGP.addLayer();
		}

		// Move old individuals up. Start at the bottom.
		for (int layerIndex = 0; layerIndex < this.population.size()
				&& layerIndex < maxNumLayers - 1; layerIndex++) {

			Vector<Individual> layer = this.population.get(layerIndex);

			// Now check all the individuals to see if they're too old
			for (int i = 0; i < layer.size(); i++) {
				if (moveUpCriteria(layer.get(i), layerIndex)) {
					layeredGP.moveIndividualUp(i, layerIndex, false, true);

					// Rewind i since we just shrunk the layer!
					i -= 1;
				}
			}
		}

		// See if we need to regenerate the initial layer.
		layeredGP.checkAndRegenInitialLayer();

		// Clear out the current generation parents.
		this.parents.clear();
	}

	/**
	 * Calculates the max age for a given layer, using the polynomial aging
	 * scheme defined by Hornby's ALPS.
	 * 
	 * Based on the layer index, we multiply the ageGap by a certain multiplier
	 * as follows:<br/>
	 * 0:1, 1:2, 2:4, 3:9, 4:16, 5:25, 6:36, etc. (basically n^2 after index 1).
	 * 
	 * @param layerIndex
	 *            the layerIndex of the layer of interest
	 * 
	 * @param ageGap
	 *            the age gap to use for determining the max age
	 * 
	 * @param maxNumLayers
	 *            the maximum (total) number of layers for this run
	 * 
	 * @return the max age defined by Hornby's ALPS polynomial aging scheme OR
	 *         -1 if the layerIndex is of the last layer (the last layer doesn't
	 *         have a max age)
	 */
	public static int calculateMaxAge(int layerIndex, int ageGap,
			int maxNumLayers) {
		if (layerIndex == 0) {
			return ageGap;
		} else if (layerIndex == 1) {
			return ageGap * 2;
		} else if (layerIndex == maxNumLayers - 1) {
			return -1;
		}

		return layerIndex * layerIndex * ageGap;
	}

	/**
	 * Determines whether or not it's time to add a new layer to the population.
	 * We add a layer once the number of generations reaches the max age in the
	 * current top-most layer.
	 * 
	 * @param layeredGP
	 *            the {@link LayeredGP} that we're checking for.
	 * 
	 * @return true if it is time to add a new layer to the population, given
	 *         the above criteria.
	 */
	public boolean doAddLayer(LayeredGP layeredGP) {
		// Have we already added maxNumLayers layers?
		if (this.population.size() >= layeredGP.getMaxNumLayers()) {
			return false;
		}

		return (layeredGP.getGenerationNum() >= calculateMaxAge(
				this.population.size() - 1, layeredGP.getAgeGap(),
				layeredGP.getMaxNumLayers()));
	}
}
