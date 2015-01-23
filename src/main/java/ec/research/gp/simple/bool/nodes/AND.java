package ec.research.gp.simple.bool.nodes;

import ec.research.gp.simple.problem.Problem;
import ec.research.gp.simple.representation.Node;

public class AND extends Node {

	public AND() {
		super(2);
	}

	@Override
	public Boolean evaluate(Problem p, Object data) {
		Node[] children = getChildren();
		
		//Evaluate the first child
		boolean c1Result = (Boolean) children[0].evaluate(p, data);
		
		//Evaluate the second child
		boolean c2Result = (Boolean) children[1].evaluate(p, data);
		
		//Return the ANDed result of the two children
		return (c1Result & c2Result);
	}

	@Override
	public String toString() {
		return "AND";
	}

}
