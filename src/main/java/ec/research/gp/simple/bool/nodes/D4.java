package ec.research.gp.simple.bool.nodes;

import ec.research.gp.simple.problem.Problem;
import ec.research.gp.simple.representation.Node;

public class D4 extends Node {

	public D4() {
		super(0);
	}

	@Override
	public Boolean evaluate(Problem p, Object data) {
		return (((1 << 4) & (Integer) data) > 0);
	}

	@Override
	public String toString() {
		return "D4";
	}

}
