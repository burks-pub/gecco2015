package msu.research.gp.simple.bool.nodes;

import msu.research.gp.simple.problem.Problem;
import msu.research.gp.simple.representation.Node;

public class D0 extends Node {

	public D0() {
		super(0);
	}

	@Override
	public Boolean evaluate(Problem p, Object data) {
		return ((1 & (Integer) data) > 0);
	}

	@Override
	public String toString() {
		return "D0";
	}

}
