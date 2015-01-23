package ec.research.gp.simple.regression.nodes;

import ec.research.gp.simple.problem.Problem;
import ec.research.gp.simple.representation.Node;

/**
 * Simple Multiply node that multiplies the result of the first child with the
 * result of the second child.
 * 
 */
public class MULT extends Node {
	// String representation of this node
	private static final String STR = "*";

	/**
	 * Create a new MULT node with two children.
	 */
	public MULT() {
		super(2);
	}

	@Override
	public Double evaluate(Problem p, Object data) {
		// Evaluate the first child
		Double res = (Double) children[0].evaluate(p, data);

		// Evaluate the second child and multiply it with the result of the
		// first
		res *= (Double) children[1].evaluate(p, data);

		return res;
	}

	@Override
	public String toString() {
		return STR;
	}

}
