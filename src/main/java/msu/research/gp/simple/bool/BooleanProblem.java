package msu.research.gp.simple.bool;

import msu.research.gp.simple.problem.Problem;
import msu.research.gp.simple.representation.Individual;
import msu.research.gp.simple.util.Context;

/**
 * 
 * @author Armand R. Burks Abstract {@link Problem} for boolean GP problems
 *         (such as multiplexer and parity).
 * 
 */
public abstract class BooleanProblem extends Problem {
	@Override
	public abstract void init(Context c);

	@Override
	public abstract void fitness(Individual individual);
}
