package msu.research.gp.simple.multiplexer.nodes;

import msu.research.gp.simple.multiplexer.Multiplexer;
import msu.research.gp.simple.problem.Problem;
import msu.research.gp.simple.representation.Node;

public class A2 extends Node {

	public A2() {
		super(0);
	}

	@Override
	public Boolean evaluate(Problem p, Object data) {
		Multiplexer mux = (Multiplexer) p;
		return (((1 << (mux.getNumDataBits() + 2)) & (Integer) data) > 0);
	}

	@Override
	public String toString() {
		return "A2";
	}

}
