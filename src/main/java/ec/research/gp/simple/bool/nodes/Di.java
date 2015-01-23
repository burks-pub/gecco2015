package ec.research.gp.simple.bool.nodes;

import ec.research.gp.simple.problem.Problem;
import ec.research.gp.simple.representation.Node;

public class Di extends Node {
	private static final String STR = "D";

	// Holds the index (bit number -- zero-based) of the data bit in the input
	// bit string
	private int i;

	public Di() {
		super(0);
	}

	/**
	 * Sets the bit's index to the requested value. This needs to be done after
	 * instantiation and before the node is used!
	 * 
	 * @param index
	 *            the index of the bit in the input string.
	 */
	public void setIndex(int index) {
		this.i = index;
	}

	/**
	 * @return the bit's index in the input string.
	 */
	public int getIndex() {
		return this.i;
	}

	@Override
	public Boolean evaluate(Problem p, Object data) {
		return (((1 << this.i) & (Integer) data) > 0);
	}

	@Override
	public Node lightClone() throws CloneNotSupportedException {
		Di clone = (Di) super.lightClone();
		clone.setIndex(this.i);

		return clone;
	}

	@Override
	public Node clone() throws CloneNotSupportedException {
		Di clone = (Di) super.clone();
		clone.setIndex(this.i);

		return clone;
	}

	@Override
	public String toString() {
		return String.format("%s%d", STR, this.i);
	}

}
