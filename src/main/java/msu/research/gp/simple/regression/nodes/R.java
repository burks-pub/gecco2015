package msu.research.gp.simple.regression.nodes;

import msu.research.gp.simple.problem.Problem;
import msu.research.gp.simple.representation.Node;

/**
 * Simple ephemeral random constant node. Value can be set (instantiation or
 * mutation), but otherwise doesn't change.
 * 
 * @author Armand R. Burks
 * 
 */
public class R extends Node {
	// Holds the value of this node.
	private double value;

	/**
	 * Create a new terminal ephemeral random constant node (no children).
	 */
	public R() {
		super(0);
	}

	@Override
	public Object evaluate(Problem p, Object data) {
		// Simply return the value of the node.
		return (Double) this.value;
	}

	@Override
	public String toString() {
		return Double.toString(this.value);
	}

	/**
	 * Sets the ephemeral random constant node's value to the given value. This
	 * should only be used for initializing or mutating, and the range of values
	 * should be enforced by the caller.
	 * 
	 * @param value
	 *            the value to which to set this node
	 */
	public void setValue(double value) {
		this.value = value;
	}

	@Override
	public Node lightClone() throws CloneNotSupportedException {
		R clone = (R) super.lightClone();
		clone.setValue(this.value);

		return clone;
	}

	@Override
	public Node clone() throws CloneNotSupportedException {
		R clone = (R) super.clone();
		clone.setValue(this.value);

		return clone;
	}

}
