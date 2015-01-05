package msu.research.gp.simple.regression.nodes;

import msu.research.gp.simple.problem.Problem;
import msu.research.gp.simple.representation.Node;

/**
 * Simple X terminal node for symbolic regression.
 * 
 * @author Armand R. Burks
 * 
 */
public class X extends Node {
	private static final String STR = "x";

	/**
	 * Create a new terminal X node (no children).
	 */
	public X() {
		super(0);
	}

	@Override
	public Object evaluate(Problem p, Object data) {
		// Simply return the current input from the problem.
		return (Double) data;
	}

	@Override
	public String toString() {
		return STR;
	}

}
