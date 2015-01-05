package msu.research.gp.simple.multiplexer.nodes;

import msu.research.gp.simple.multiplexer.Multiplexer;
import msu.research.gp.simple.problem.Problem;
import msu.research.gp.simple.representation.Node;

public class A1 extends Node {

	public A1() {
		super(0);
	}

	@Override
	public Boolean evaluate(Problem p, Object data) {
		Multiplexer mux = (Multiplexer) p;
		return (((1 << (mux.getNumDataBits() + 1)) & (Integer) data) > 0);
	}

	@Override
	public String toString() {
		return "A1";
	}

}
