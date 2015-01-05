package msu.research.gp.simple.regression.nodes;

import msu.research.gp.simple.problem.Problem;
import msu.research.gp.simple.representation.Node;

/**
 * Simple Sin node that performs the sin function on it's child.
 * 
 * @author Armand R. Burks
 * 
 */
public class SIN extends Node {
	// String representation of this node
	private static final String STR = "SIN";

	/**
	 * Create a new SIN node with one child.
	 */
	public SIN() {
		super(1);
	}

	@Override
	public Double evaluate(Problem p, Object data) {
		// Evaluate the first child and return sin of it.
		return (Double)Math.sin((Double) children[0].evaluate(p, data));
	}

	@Override
	public String toString() {
		return STR;
	}
}
