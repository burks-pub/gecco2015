package ec.research.gp.simple.bool;

import ec.research.gp.simple.problem.Problem;
import ec.research.gp.simple.representation.Individual;
import ec.research.gp.simple.util.Context;

/**
 * 
 * Abstract {@link Problem} for boolean GP problems
 *         (such as multiplexer and parity).
 * 
 */
public abstract class BooleanProblem extends Problem {
	@Override
	public abstract void init(Context c);

	@Override
	public abstract void fitness(Individual individual);
}
