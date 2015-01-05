package msu.research.gp.simple.operators;

import java.util.Vector;

import msu.research.gp.simple.representation.Individual;
import msu.research.gp.simple.util.Context;

/**
 * Simple abstract representation of a Selection scheme.
 * 
 * @author Armand R. Burks
 * 
 */
public abstract class Selection {
	/**
	 * Selects a parent index from the collection of individuals, depending on
	 * the implementation of the selection criteria.
	 * 
	 * @param individuals
	 *            the collection of individuals from which to select
	 * 
	 * @param context
	 *            the Context for this run
	 * 
	 * @return the index of the selected parent.
	 */
	public abstract int selectOne(Context context,
			Vector<Individual> individuals);

	/**
	 * Selects parent indices from the collection of individuals, depending on
	 * the implementation of the selection criteria.
	 * 
	 * @param individuals
	 *            the collection of individuals from which to select
	 * 
	 * @param context
	 *            the Context for this run
	 * 
	 * @return the indices of the two selected parents.
	 */
	public abstract int[] select(Context context, Vector<Individual> individuals);
}
