package ec.research.gp.simple.bool.nodes;

import ec.research.gp.simple.problem.Problem;
import ec.research.gp.simple.representation.Node;

/**
 * Simple IF node. Nothing special.
 * 
 */
public class IF extends Node {
	/**
	 * Create a new IF node.
	 * 
	 */
	public IF() {
		super(3);
	}

	@Override
	public Boolean evaluate(Problem p, Object data) {
		// Evaluate the first child.
		Boolean c1Result = (Boolean) children[0].evaluate(p, data);

		// If the first child is true, return it.
		if (c1Result) {
			return (Boolean) children[1].evaluate(p, data);
		}

		// Otherwise return the third child
		else {
			return (Boolean) children[2].evaluate(p, data);
		}
	}

	@Override
	public String toString() {
		return "IF";
	}
}
