package msu.research.gp.simple.multiplexer.nodes;

import msu.research.gp.simple.multiplexer.Multiplexer;
import msu.research.gp.simple.problem.Problem;
import msu.research.gp.simple.representation.Node;

/**
 * Generic address bit.
 * 
 * @author Armand R. Burks
 * 
 */
public class Ai extends Node {
	private static final String STR = "A";

	// Holds the index (bit number -- zero-based) of the address bit in the
	// input bit string. Note that address bits are at the (left-most) end of
	// the string, not the beginning.
	private int i;

	public Ai() {
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
		Multiplexer mux = (Multiplexer) p;
		return (((1 << (mux.getNumDataBits() + this.i)) & (Integer) data) > 0);
	}

	@Override
	public String toString() {
		return String.format("%s%d", STR, this.i);
	}

}
