package ec.research.gp.simple.regression.nodes;

import ec.research.gp.simple.problem.Problem;
import ec.research.gp.simple.representation.Node;

/**
 * Simple protected division node that divides the result of the first child by
 * the result of the second child (protecting against division by 0 -- by
 * dividing by 1 if the denominator is 0).
 * 
 */
public class DIV extends Node {
	// String representation of this node
	private static final String STR = "%";

	/**
	 * Create a new DIV node with two children.
	 */
	public DIV() {
		super(2);
	}

	@Override
	public Double evaluate(Problem p, Object data) {
		// Evaluate the 1st child
		Double res = (Double) children[0].evaluate(p, data);

		// Evaluate the 2nd child
		double c2Res = (Double) children[1].evaluate(p, data);

		// Divide 1st by 2nd if 2nd is not zero, otherwise divide by 1 (i.e.
		// don't divide at all).
		if (c2Res != 0.0) {
			res /= c2Res;
		}

		return res;
	}

	@Override
	public String toString() {
		return STR;
	}
}