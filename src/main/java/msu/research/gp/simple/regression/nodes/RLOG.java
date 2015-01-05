package msu.research.gp.simple.regression.nodes;

import msu.research.gp.simple.problem.Problem;
import msu.research.gp.simple.representation.Node;

/**
 * Simple protected natural log function as described in Koza '92. Has one child
 * and returns 0 if its child is evaluated as 0 or the natural log of the
 * absolute value of its child's result.
 * 
 * @author Armand R. Burks
 * 
 */
public class RLOG extends Node {
	// String representation of this node
	private static final String STR = "RLOG";

	/**
	 * Create a new RLOG node with one child.
	 */
	public RLOG() {
		super(1);
	}

	@Override
	public Double evaluate(Problem p, Object data) {
		// Evaluate the child
		Double res = (Double) children[0].evaluate(p, data);

		// If the result is nonzero, return the natural log of its absolute
		// value (Otherwise return 0)
		if (res != 0.0) {
			res = (Double)Math.log(Math.abs(res));
		}

		return res;
	}

	@Override
	public String toString() {
		return STR;
	}
}
