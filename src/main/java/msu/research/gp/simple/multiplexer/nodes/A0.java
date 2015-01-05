package msu.research.gp.simple.multiplexer.nodes;

import msu.research.gp.simple.multiplexer.Multiplexer;
import msu.research.gp.simple.problem.Problem;
import msu.research.gp.simple.representation.Node;

public class A0 extends Node {

	public A0() {
		super(0);
	}

	@Override
	public Boolean evaluate(Problem p, Object data) {
		Multiplexer mux = (Multiplexer) p;
		return (((1 << mux.getNumDataBits()) & (Integer) data) > 0);
	}

	@Override
	public String toString() {
		return "A0";
	}

}
