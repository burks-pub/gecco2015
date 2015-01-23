package ec.research.gp.simple.problem;

import ec.research.gp.simple.representation.Individual;
import ec.research.gp.simple.util.Context;

/**
 * Base class for a GP problem.
 * 
 */
public abstract class Problem {
	protected Context context;

	/**
	 * Sets this problem's context.
	 * 
	 * @param c
	 *            the context
	 */
	public void setContext(Context c) {
		this.context = c;
	}

	/**
	 * Performs any post-construction initialization that needs to be done,
	 * specific to the actual problem. This should be used for things like
	 * loading problem-specific params from config, etc. This is done outside
	 * the constructor because we can't expect the context to already be setup
	 * on initialization, since problems are specified in config.
	 * 
	 * @param c
	 *            the context
	 */
	public abstract void init(Context c) throws Exception;

	/**
	 * Fitness function for the problem that evaluates the individual on the
	 * problem and assigns its fitness.
	 * 
	 * @param individual
	 *            the individual to run and evaluate
	 */
	public abstract void fitness(Individual individual);
}
