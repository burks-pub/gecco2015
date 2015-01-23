package ec.research.gp.layers;

import java.util.List;
import java.util.Vector;

import ec.research.gp.simple.representation.Individual;
import ec.research.gp.simple.util.Config;



/**
 * Defines the behavior of a generic aging scheme to be used for updating ages
 * in each layer.
 * 
 */
public abstract class LayerScheme {
	// Holds the configuration object needed for various parameters.
	protected Config config;

	// Holds a reference to the layered population.
	protected List<Vector<Individual>> population;

	// Holds a reference to the current generation parents.
	protected List<Individual> parents;

	// Holds the max number of layers, from config.
	protected int maxNumLayers;

	// Holds the number of individuals per layer.
	protected int layerCapacity;

	// Keeps track of the last generation where we regenerated the initial
	// layer.
	protected int lastRegen;

	// Keeps track of the last generation where we added a new layer.
	protected int lastLayerAdd;

	/**
	 * Creates a new AgingScheme object with the given configuration.
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
	public LayerScheme(Config config,
			List<Vector<Individual>> layeredPopulation, List<Individual> parents) {
		this.config = config;
		this.population = layeredPopulation;
		this.parents = parents;
		this.maxNumLayers = config.getNumLayers();
		this.layerCapacity = config.getPopSize() / this.maxNumLayers;
	}

	/**
	 * Updates the age of the given individual according to the implementation
	 * of the aging scheme.
	 * 
	 * @param individual
	 *            the individual whose age to update
	 */
	public abstract void updateAge(Individual individual,
			List<Individual> parents);

	/**
	 * Updates the ages in the given layer according to the implementation of
	 * the aging scheme.
	 * 
	 * @param layer
	 *            the current layer of individuals to update
	 */
	public abstract void updateAges(Vector<Individual> layer,
			List<Individual> parents, int generation);

	/**
	 * Updates the ages of elites separate from the rest of the individuals, in
	 * the case that they are handled specially (i.e. as in ALPS).
	 * 
	 * @param layer
	 *            the current layer of individuals to update.
	 * 
	 * @param parents
	 *            the set of individuals used as parents this generation
	 * @param numElites
	 *            the number of elites in the layer
	 */
	public abstract void updateElitesAges(Vector<Individual> layer,
			List<Individual> parents, int numElites);

	/**
	 * Tests the given individual to see if it meets the criteria to move up to
	 * the next layer, based on the implementation of the aging scheme.
	 * 
	 * @param individual
	 *            the individual to test
	 * @param layer
	 *            the current layer index to which the individual belongs
	 * @return true if the individual meets the criteria for moving up to the
	 *         next layer, or false otherwise.
	 */
	public abstract boolean moveUpCriteria(Individual individual, int layer);

	/**
	 * Updates the layer based on the particular layer scheme implementation for
	 * creating new layers, regenerating the initial layer, and moving
	 * individuals out of and into layers.
	 * 
	 * @param layeredGP
	 *            the LayeredGP object
	 * @throws CloneNotSupportedException
	 */
	public void updateLayers(LayeredGP layeredGP)
			throws CloneNotSupportedException {
		// Add a new layer if it's time.
		layeredGP.checkAndAddLayer();

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
	 * Determines if ageGap generations have passed (starting at 0).
	 * 
	 * @param generation
	 *            the current generation number
	 * 
	 * @param ageGap
	 *            the ageGap parameter for the run
	 * 
	 * @param lastRegen
	 *            the last generation that the initial layer was regenerated
	 * 
	 * @return true if this generation is at the ageGap or false otherwise
	 */
	public boolean doRegenerateInitialLayer(int generation, int ageGap,
			int lastRegen) {
		boolean ret = generation != 0 && generation - lastRegen >= ageGap;

		return ret;
	}

	/**
	 * Determines if it is time to add a new layer (if possible). Basically,
	 * this happens every ageGap generations (until max layers), but it can
	 * happen independently of when the initial layer is regenerated, with
	 * certain schemes.
	 * 
	 * @param generation
	 *            the current generation number
	 * 
	 * @param ageGap
	 *            the age gap parameter for the run
	 * 
	 * @param lastLayerAdd
	 *            the generation at which we added the last layer
	 * 
	 * @return true if ageGap generations have passed since the last time we
	 *         added a new layer or false otherwise
	 */
	public boolean doAddLayer(int generation, int ageGap, int lastLayerAdd) {
		boolean ret = generation != 0 && generation - lastLayerAdd >= ageGap;

		return ret;
	}

}
