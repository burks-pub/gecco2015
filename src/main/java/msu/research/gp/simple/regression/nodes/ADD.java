package msu.research.gp.simple.regression.nodes;

import msu.research.gp.simple.problem.Problem;
import msu.research.gp.simple.representation.Node;

/**
 * Simple Add node that adds the result of the first child to the result of the
 * second child.
 * 
 * @author Armand R. Burks
 * 
 */
public class ADD extends Node {
	// String representation of this node.
	private static final String STR = "+";

	/**
	 * Create a new ADD node with two children.
	 * 
	 */
	public ADD() {
		super(2);
	}

	@Override
	public Double evaluate(Problem p, Object data) {
		// Evaluate the first child
		Double res = (Double) children[0].evaluate(p, data);

		// Evaluate the second child and add it to the result of the first
		res += (Double) children[1].evaluate(p, data);

		return res;
	}

	@Override
	public String toString() {
		return STR;
	}

}
