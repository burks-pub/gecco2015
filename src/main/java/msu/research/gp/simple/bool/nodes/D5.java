package msu.research.gp.simple.bool.nodes;

import msu.research.gp.simple.problem.Problem;
import msu.research.gp.simple.representation.Node;

public class D5 extends Node {

	public D5() {
		super(0);
	}

	@Override
	public Boolean evaluate(Problem p, Object data) {
		return (((1 << 5) & (Integer) data) > 0);
	}

	@Override
	public String toString() {
		return "D5";
	}

}
