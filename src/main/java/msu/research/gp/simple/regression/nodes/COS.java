package msu.research.gp.simple.regression.nodes;

import msu.research.gp.simple.problem.Problem;
import msu.research.gp.simple.representation.Node;

/**
 * Simple Cosine node that returns the cosine of the child.
 * 
 * @author Armand R. Burks
 * 
 */
public class COS extends Node {
	// String representation of this node
	private static final String STR = "COS";

	/**
	 * Create a new COS node with one child.
	 */
	public COS() {
		super(1);
	}

	@Override
	public Double evaluate(Problem p, Object data) {
		// Evaluate the first child and return sin of it.
		return Math.cos((Double) children[0].evaluate(p, data));
	}

	@Override
	public String toString() {
		return STR;
	}
}
