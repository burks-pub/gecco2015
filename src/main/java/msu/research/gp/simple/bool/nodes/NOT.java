package msu.research.gp.simple.bool.nodes;

import msu.research.gp.simple.problem.Problem;
import msu.research.gp.simple.representation.Node;

public class NOT extends Node {

	public NOT() {
		super(1);
	}

	@Override
	public Boolean evaluate(Problem p, Object data) {
		Node[] children = getChildren();
		
		//Evaluate the child
		Boolean c1Result = (Boolean) children[0].evaluate(p, data);
		
		//Return the NOT of the result
		return (!c1Result);
	}

	@Override
	public String toString() {
		return "NOT";
	}

}
