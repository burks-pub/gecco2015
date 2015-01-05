package msu.research.gp.simple.regression.nodes;

import msu.research.gp.simple.problem.Problem;
import msu.research.gp.simple.representation.Node;

/**
 * Simple Subtract node that subtracts the result of the second child from the
 * result of the first child.
 * 
 * @author Armand R. Burks
 * 
 */
public class SUB extends Node {
	// String representation of this node
	private static final String STR = "-";

	/**
	 * Create a new SUB node with two children.
	 */
	public SUB() {
		super(2);
	}

	@Override
	public Double evaluate(Problem p, Object data) {
		// Evaluate the first child
		Double res = (Double) children[0].evaluate(p, data);

		// Evaluate the second child and subtract it from the first child's
		// result
		res -= (Double) children[1].evaluate(p, data);

		return res;
	}

	@Override
	public String toString() {
		return STR;
	}

}
