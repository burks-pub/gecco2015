package msu.research.gp.simple.bool.nodes;

import msu.research.gp.simple.problem.Problem;
import msu.research.gp.simple.representation.Node;

public class D3 extends Node {

	public D3() {
		super(0);
	}

	@Override
	public Boolean evaluate(Problem p, Object data) {
		return (((1 << 3) & (Integer) data) > 0);
	}

	@Override
	public String toString() {
		return "D3";
	}

}
