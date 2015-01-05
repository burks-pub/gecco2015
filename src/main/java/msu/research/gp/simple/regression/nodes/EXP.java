package msu.research.gp.simple.regression.nodes;

import msu.research.gp.simple.problem.Problem;
import msu.research.gp.simple.representation.Node;

/**
 * Simple exp function (natural exponential function e^x).
 * 
 * @author Armand R. Burks
 * 
 */
public class EXP extends Node {
	// String representation of this node
	private static final String STR = "EXP";

	/**
	 * Create a new EXP node with one child.
	 */
	public EXP() {
		super(1);
	}

	@Override
	public Double evaluate(Problem p, Object data) {
		// Evaluate the child
		Double res = (Double) children[0].evaluate(p, data);

		// Now do e^res and return it.
		return (Double)Math.exp(res);
	}

	@Override
	public String toString() {
		return STR;
	}
}
