package msu.research.gp.simple.bool.nodes;

import msu.research.gp.simple.problem.Problem;
import msu.research.gp.simple.representation.Node;

public class D2 extends Node {

	public D2() {
		super(0);
	}

	@Override
	public Boolean evaluate(Problem p, Object data) {
		return (((1 << 2) & (Integer) data) > 0);
	}

	@Override
	public String toString() {
		return "D2";
	}

}
