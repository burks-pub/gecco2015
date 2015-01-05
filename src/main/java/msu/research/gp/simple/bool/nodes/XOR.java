package msu.research.gp.simple.bool.nodes;

import msu.research.gp.simple.problem.Problem;
import msu.research.gp.simple.representation.Node;

public class XOR extends Node {

	public XOR() {
		super(2);
	}

	@Override
	public Boolean evaluate(Problem p, Object data) {
		Node[] children = getChildren();
		
		//Evaluate the first child
		Boolean c1Result = (Boolean) children[0].evaluate(p, data);
		
		//Evaluate the second child
		Boolean c2Result = (Boolean) children[1].evaluate(p, data);
		
		//Return the XOR result of the two children
		return (c1Result ^ c2Result);
	}

	@Override
	public String toString() {
		return "XOR";
	}

}
