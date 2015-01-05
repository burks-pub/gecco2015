package msu.research.gp.simple.operators;

import java.util.HashSet;
import java.util.List;

import msu.research.gp.simple.regression.nodes.R;
import msu.research.gp.simple.representation.Individual;
import msu.research.gp.simple.representation.Node;
import msu.research.gp.simple.util.Context;

/**
 * Simple point mutation operator.
 * 
 * @author Armand R. Burks
 * 
 */
public class PointMutation {

	public static Node getRandomFunctionNode(Node oldNode, Context context)
			throws CloneNotSupportedException {
		List<Node> functionSet = context.getConfig().getFunctionSet();

		// We'll keep trying until we've run out of nodes in the set
		HashSet<String> triedNodes = new HashSet<String>();

		while (triedNodes.size() < functionSet.size()) {
			Node newNode = functionSet.get(context.randBetween(0,
					functionSet.size() - 1));

			// Did we find something with the same arity?
			if (newNode.getNumChildren() == oldNode.getNumChildren()) {
				return newNode.clone();
			} else {
				triedNodes.add(newNode.toString());
			}
		}

		return null;
	}

	public static Node getRandomTerminalNode(Node oldNode, Context context)
			throws CloneNotSupportedException {
		List<Node> terminalSet = context.getConfig().getTerminalSet();

		return terminalSet.get(context.randBetween(0, terminalSet.size() - 1))
				.clone();
	}

	public static void replaceNode(Individual individual, Node oldNode,
			Node newNode) {
		// Take the old node's parent
		Node parent = oldNode.getParent();
		if (parent != null) {
			newNode.setParent(parent);

			for (int i = 0; i < parent.getNumChildren(); i++) {
				if (parent.getChild(i) == oldNode) {
					parent.setChild(i, newNode);
				}
			}
		}

		// If it has no parent, it'd better be the root!
		else {
			individual.setRoot(newNode);
		}

		// Take ownership of the old node's children (if newNode is a function!)
		if (!newNode.isTerminal()) {
			for (int i = 0; i < oldNode.getNumChildren(); i++) {
				Node child = oldNode.getChild(i);
				child.setParent(newNode);
				newNode.setChild(i, child);
			}
		}

		// Don't forget to set the node number
		newNode.setNodeNumber(oldNode.getNodeNumber());

		// Null out the old node so it won't persist?
		oldNode = null;

	}

	/**
	 * Performs point mutation on the given individual. Assumes that the
	 * individual has it's numNodes set, and that all the nodes are numbered.
	 * 
	 * @param individual
	 *            the individual to mutate
	 * @param context
	 *            the Context for the run
	 * @return a (potentially) mutated version of the given individual
	 * @throws CloneNotSupportedException
	 */
	public static Individual mutate(Individual individual, Context context)
			throws CloneNotSupportedException {
		Individual mutant = individual.clone();

		// Since we have a clone, we need to unset it's fitness!
		mutant.setIsEvaluated(false);
		mutant.setFitness(0.0);
		mutant.setHits(0);

		// Probabilistically mutate each node
		for (int i = 0; i < individual.getNumNodes(); i++) {
			if (context.nextBool(context.getConfig().getMutationProbability())) {
				// Try to mutate the ith node.
				Node node = mutant.findNode(i);

				// If replacing a function, match arity.
				if (!node.isTerminal()) {
					Node newNode = getRandomFunctionNode(node, context);

					// If we could find a node, go ahead and replace it
					if (newNode != null) {
						replaceNode(mutant, node, newNode);
					}
				}

				// If it's a constant, just change the value
				else if (node instanceof R) {
					((R) node).setValue(context.getRand().nextGaussian());
				}

				// Otherwise, replace the terminal with another terminal node
				else {
					Node newNode = getRandomTerminalNode(node, context);

					// If we actually have other terminals, replace it.
					if (newNode != null) {
						// If the new terminal is a random ephemeral constant,
						// we need to set it.
						if (newNode instanceof R) {
							((R) newNode).setValue(context.getRand()
									.nextGaussian());
						}
						replaceNode(mutant, node, newNode);
					}
				}
			}
		}

		return mutant;

	}
}
