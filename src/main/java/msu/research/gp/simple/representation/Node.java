package msu.research.gp.simple.representation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import msu.research.gp.simple.problem.Problem;

/**
 * Generic node of a GP tree.
 * 
 * @author Armand R. Burks
 * 
 */
public abstract class Node implements Cloneable {
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(Node.class);

	/**
	 * Holds the node's number in the tree.
	 */
	protected int nodeNumber;

	/**
	 * Holds the (fixed) number of children this node can have.
	 */
	protected int numChildren;

	/**
	 * Whether or not this node belongs to the terminal set.
	 */
	private boolean isTerminal;

	/**
	 * Points to the node's parent in the tree.
	 */
	protected Node parent;

	/**
	 * Contains the node's children in the tree.
	 */
	protected Node children[];

	/**
	 * Holds the ID of the node, as defined by McPhee/Hopper, GECCO '99 .
	 */
	private UUID id;
	/**
	 * Holds the memID of the node, as defined by McPhee/Hopper, GECCO '99 .
	 */
	private UUID memId;

	/**
	 * Holds the "mutant ID, which gets generated when a mutation occurs."
	 */
	private UUID mutId;

	/**
	 * Whether or not this node was the crossover point.
	 */
	private boolean isCrossPoint;

	/**
	 * Sets the node's ID to a random UUID.
	 */
	public void setId() {
		this.id = UUID.randomUUID();
	}

	/**
	 * Sets the node's ID to the given UUID.
	 * 
	 * @param id
	 *            the UUID to which to set the node's ID
	 */
	public void setId(UUID id) {
		this.id = id;
	}

	/**
	 * Sets the node's memID to a random UUID.
	 */
	public void setMemId() {
		this.memId = UUID.randomUUID();
	}

	/**
	 * Sets the node's memID to the given UUID.
	 * 
	 * @param memId
	 *            the UUID to which to set the node's memID
	 */
	public void setMemId(UUID memId) {
		this.memId = memId;
	}

	/**
	 * Sets the node's mutID to a random UUID.
	 */
	public void setMutId() {
		this.mutId = UUID.randomUUID();
	}

	/**
	 * Sets the node's mutID to the given UUID.
	 * 
	 * @param mutId
	 *            the UUID to which to set the node's mutID
	 */
	public void setMutId(UUID mutId) {
		this.mutId = mutId;
	}

	/**
	 * 
	 * @return the node's ID, as defined by McPhee/Hopper, GECCO '99.
	 */
	public UUID getId() {
		return this.id;
	}

	/**
	 * 
	 * @return the node's memID, as defined by McPhee/Hopper, GECCO '99.
	 */
	public UUID getMemId() {
		return this.memId;
	}

	/**
	 * 
	 * @return the nodes mutId.
	 */
	public UUID getMutId() {
		return this.mutId;
	}

	/**
	 * Sets whether or not this node is the crossover point. Use with much care!
	 * 
	 * @param isCrossPoint
	 *            whether or not this node is the crossover point in its
	 *            individual.
	 */
	public void setIsCrossPoint(boolean isCrossPoint) {
		this.isCrossPoint = isCrossPoint;
	}

	/**
	 * 
	 * @return whether or not this node is the crossover point. This should only
	 *         be called from CrossoverPipeline by a TrackingGPIndividual after
	 *         the crossover has swapped the trees.
	 */
	protected boolean isCrossPoint() {
		return this.isCrossPoint;
	}

	/**
	 * Private helper that prints a lisp-style tree starting at the subtree
	 * rooted at this node, basically the same way that ECJ does it.
	 * 
	 * @param buffer
	 *            the StringBuilder holding the current string as we traverse
	 * @return a lisp-style tree starting at the subtree rooted at this node,
	 *         basically the same way that ECJ does it.
	 */
	private String subtreeToString(StringBuilder buffer) {
		// Once we've reached the terminal in this branch just append the
		// toString
		if (isTerminal) {
			buffer.append(toString());
			return buffer.toString();
		} else {
			// Otherwise, append toString and keep traversing on all the
			// children
			buffer.append("(" + toString());

			for (int i = 0; i < numChildren; i++) {
				buffer.append(" ");
				children[i].subtreeToString(buffer);
			}
			buffer.append(")");
		}
		return buffer.toString();
	}

	/**
	 * Creates the in-fix expression as in ECJ's pseudo-C style method.
	 * 
	 * @param buffer
	 * @return
	 */
	private String subtreeToCString(StringBuilder buffer) {
		if (this.isTerminal) {
			buffer.append(toString());
		} else if (this.numChildren == 1) {
			buffer.append(toString() + "(");
			this.children[0].subtreeToCString(buffer);
			buffer.append(")");
		} else if (this.numChildren == 2) {
			buffer.append("(");
			this.children[0].subtreeToCString(buffer);
			buffer.append(" " + toString() + " ");
			this.children[1].subtreeToCString(buffer);
			buffer.append(")");
		} else {
			buffer.append(toString() + "(");
			for (int i = 0; i < this.numChildren; i++) {
				if (i > 0) {
					buffer.append(", ");
				}
				this.children[i].subtreeToCString(buffer);
			}
			buffer.append(")");
		}

		return buffer.toString();
	}

	/**
	 * Private helper that prints a lisp-style tree starting at the subtree
	 * rooted at this node, basically the same way that ECJ does it except using
	 * the node numbers instead of the node's toString() value.
	 * 
	 * @param buffer
	 *            the StringBuilder holding the current string as we traverse
	 * @return a lisp-style tree starting at the subtree rooted at this node,
	 *         basically the same way that ECJ does it except using the node
	 *         numbers instead of the node's toString() value.
	 */
	private String subtreeToNumberedString(StringBuilder buffer) {
		// Once we've reached the terminal in this branch just append the
		// toString
		if (isTerminal) {
			buffer.append(nodeNumber);
			return buffer.toString();
		} else {
			// Otherwise, append toString and keep traversing on all the
			// children
			buffer.append("(" + nodeNumber);

			for (int i = 0; i < numChildren; i++) {
				buffer.append(" ");
				children[i].subtreeToNumberedString(buffer);
			}
			buffer.append(")");
		}
		return buffer.toString();
	}

	/**
	 * Private helper that prints a dot tree starting at the subtree rooted at
	 * this node, basically the same way that ECJ does it except using the node
	 * numbers instead of the node's toString() value.
	 * 
	 * @param buffer
	 *            the StringBuilder holding the current string as we traverse
	 * @return a dot tree starting at the subtree rooted at this node, basically
	 *         the same way that ECJ does it except using the node numbers
	 *         instead of the node's toString() value.
	 */
	private String subtreeToNumberedDotString(StringBuilder buffer) {
		// Once we've reached the terminal in this branch just return the string
		if (isTerminal) {
			return buffer.toString();
		} else {
			// Otherwise, keep traversing on all the
			for (int i = 0; i < numChildren; i++) {
				buffer.append("\"" + nodeNumber + ":" + getDepth() + "\""
						+ "-> \"" + children[i].getNodeNumber() + ":"
						+ children[i].getDepth() + "\"" + "\n");
				children[i].subtreeToNumberedDotString(buffer);
			}
		}
		return buffer.toString();
	}

	/**
	 * Private helper that prints a dot tree starting at the subtree rooted at
	 * this node, basically the same way that ECJ does it.
	 * 
	 * @param buffer
	 *            the StringBuilder holding the current string as we traverse
	 * @return a dot tree starting at the subtree rooted at this node, basically
	 *         the same way that ECJ does it except using the node numbers
	 *         instead of the node's toString() value.
	 */
	private String subtreeToDotString(StringBuilder buffer) {
		// Once we've reached the terminal in this branch just return the string
		if (isTerminal) {
			return buffer.toString();
		} else {
			// Otherwise, keep traversing on all the
			for (int i = 0; i < numChildren; i++) {
				// Add a definition for the node. Don't care about repeats.
				if (i == 0) {
					buffer.append(nodeNumber + " [label=\"" + toString()
							+ "\"]\n");
				}

				// Add a definition for the child.
				String label = (children[i].isTerminal() ? " [label=\""
						+ children[i].toString() + "\" shape=\"none\"]\n"
						: " [label=\"" + children[i].toString() + "\"]\n");

				buffer.append(children[i].getNodeNumber() + label);

				// Add a parent-> child link
				buffer.append(nodeNumber + "->" + children[i].getNodeNumber()
						+ "\n");
				children[i].subtreeToDotString(buffer);
			}
		}
		return buffer.toString();
	}

	/**
	 * Recursively calculates the depth of the subtree rooted at this node.
	 * 
	 * @param currentDepth
	 *            the current depth from recursion so far.
	 * @return the depth of the subtree rooted at this node
	 */
	private int depthFrom(int currentDepth) {
		int maxDepth = currentDepth;

		// Branch out to all the children and get the max depth
		for (int i = 0; i < numChildren; i++) {
			if (children[i] != null) {
				int childDepth = children[i].depthFrom(currentDepth + 1);
				if (maxDepth < childDepth) {
					maxDepth = childDepth;
				}
			}
		}
		return maxDepth;
	}

	/**
	 * Setup a new Node with the given number of children.
	 * 
	 * @param numChildren
	 *            the number of children this node has
	 */
	public Node(int numChildren) {
		// Set the num children
		this.numChildren = numChildren;

		this.children = new Node[numChildren];

		// Initialize parent to null
		parent = null;

		// If numChildren is 0, then mark this as a terminal node
		if (this.numChildren == 0) {
			isTerminal = true;
		} else {
			isTerminal = false;
		}
	}

	/**
	 * @return a lisp-style tree starting at the subtree rooted at this node,
	 *         basically the same way that ECJ does it.
	 */
	public String subtreeToString() {
		return subtreeToString(new StringBuilder());
	}

	/**
	 * @return a lisp-style tree starting at the subtree rooted at this node,
	 *         basically the same way that ECJ does it, but using the node
	 *         numbers instead of their toString() values.
	 */
	public String subtreeToNumberedString() {
		return subtreeToNumberedString(new StringBuilder());
	}

	/**
	 * Creates the in-fix expression as in ECJ's pseudo-C style method.
	 * 
	 * @return
	 */
	public String subtreeToCString() {
		return subtreeToCString(new StringBuilder());
	}

	/**
	 * @return a dot tree starting at the subtree rooted at this node, basically
	 *         the same way that ECJ does it, but using the node numbers instead
	 *         of their toString() values.
	 */
	public String subtreeToNumberedDotString() {
		StringBuilder tree = new StringBuilder();
		tree.append("digraph genotype {\n");
		subtreeToNumberedDotString(tree);
		tree.append("}");

		return tree.toString();
	}

	/**
	 * @return a graphviz dot string starting at the root
	 */
	public String subtreeToDotString() {
		StringBuilder tree = new StringBuilder();
		tree.append("digraph genotype {\n");
		subtreeToDotString(tree);
		tree.append("}");

		return tree.toString();
	}

	/**
	 * Sets this node's parent to be the requested node.
	 */
	public void setParent(Node node) {
		parent = node;
	}

	/**
	 * @return the node's parent.
	 */
	public Node getParent() {
		return parent;
	}

	/**
	 * @return the depth of the subtree rooted at this node.
	 */
	public int depthFrom() {
		return depthFrom(0);
	}

	/**
	 * Helper method to build the list of nodes contained in the subtree rooted
	 * at the current node.
	 * 
	 * @param currentNode
	 *            the current node in the traversal
	 * 
	 * @param subtree
	 *            the list containing the nodes contained in the subtree rooted
	 *            at the node that was originally passed into this method
	 */
	private void buildSubtreeFrom(Node currentNode, List<Node> subtree) {
		// Add the current node
		subtree.add(currentNode);

		// Visit all of the node's children
		for (int i = 0; i < currentNode.getNumChildren(); i++) {
			buildSubtreeFrom(currentNode.getChild(i), subtree);
		}
	}

	/**
	 * @return a list of the nodes of the subtree rooted at this node
	 */
	public List<Node> subtreeFrom() {
		List<Node> subtree = new ArrayList<Node>();

		buildSubtreeFrom(this, subtree);

		return subtree;

	}

	/**
	 * @return the size of the subtree rooted at this node (or just 1 if it is a
	 *         terminal node).
	 */
	public int sizeFrom() {
		return subtreeFrom().size();
	}

	/**
	 * Returns the node's depth, which is the number of edges that must be
	 * traversed, starting at the root, in order to reach this node O(n).
	 */
	public int getDepth() {
		int depth = 0;

		// If this node is the root, its depth is 0.
		Node parentNode = getParent();
		if (parentNode == null) {
			return 0;
		}// Otherwise, follow its parents until we reach the root, incrementing
			// depth.
		else {
			while (parentNode != null) {
				depth++;
				parentNode = parentNode.getParent();
			}
		}

		return depth;
	}

	/**
	 * Sets the node's ith child to be the requested node.
	 * 
	 * @param index
	 *            the index of the child
	 * @param node
	 *            the node to set as a child
	 */
	public void setChild(int index, Node node) {
		children[index] = node;
	}

	/**
	 * Gets the node's ith child.
	 * 
	 * @param index
	 *            the index of the child to get
	 * @return the child at the requested index
	 */
	public Node getChild(int index) {
		return children[index];
	}

	/**
	 * @return the node's children.
	 */
	public Node[] getChildren() {
		return children;
	}

	/**
	 * 
	 * @return the number of children this node has.
	 */
	public int getNumChildren() {
		return numChildren;
	}

	/**
	 * Sets the node number, which is its position in its tree.
	 * 
	 * @param number
	 *            the number to which to set this node's number.
	 */
	public void setNodeNumber(int number) {
		this.nodeNumber = number;
	}

	/**
	 * 
	 * @return this node's number within its tree.
	 */
	public int getNodeNumber() {
		return this.nodeNumber;
	}

	/**
	 * Whether or not this node belongs to the terminal set.
	 */
	public boolean isTerminal() {
		return this.isTerminal;
	}

	public Node lightClone() throws CloneNotSupportedException {
		Node node = (Node) super.clone();

		node.numChildren = this.numChildren;
		node.children = new Node[node.numChildren];

		// Set the ID, memID, and mutID
		node.setId(this.id);
		node.setMemId(this.memId);
		node.setMutId(this.mutId);

		return node;
	}

	public Node clone() throws CloneNotSupportedException {
		Node node = lightClone();

		// Clone children.
		for (int i = 0; i < numChildren; i++) {
			if (this.children[i] != null) {
				Node child = this.children[i].clone();
				node.setChild(i, child);
				child.setParent(node);
			}
		}

		// Set the ID, memID, and mutID
		node.setId(this.id);
		node.setMemId(this.memId);
		node.setMutId(this.mutId);

		return node;
	}

	/**
	 * Puts node2 in node1's place, copying over the entire subtree from node2.
	 * This is done basically the same as ECJ's GPNode.cloneReplacing.
	 * 
	 * @param node1
	 *            the old node to be replaced
	 * @param node2
	 *            the new node with which to replace the old one
	 * @throws CloneNotSupportedException
	 */
	public Node swapNode(Node node1, Node node2)
			throws CloneNotSupportedException {
		// If we're already at the node, just replace it with node2
		if (node1 == this) {
			// Mark node2 as being the cross point, then return it
			Node newNode = node2.clone();
			newNode.setIsCrossPoint(true);
			return newNode;
		}

		// Otherwise, keep cloning until we find the swap point
		Node newNode = lightClone();
		for (int i = 0; i < children.length; i++) {
			newNode.children[i] = children[i].swapNode(node1, node2);
			newNode.children[i].setParent(newNode);
		}

		return newNode;
	}

	/**
	 * Same as swapNode(), but it works by swapping a list of paired nodes. This
	 * is useful for uniform crossover.
	 * 
	 * @param pairs
	 *            the list (really a FIFO queue of pairs of nodes to swap.
	 * 
	 * @return the newly, rooted, tree containing the nodes swapped (the second
	 *         node in the pair is swapped at/with the first node in the pair).
	 * @throws CloneNotSupportedException
	 */
	public Node swapNodes(List<Node[]> pairs) throws CloneNotSupportedException {
		// See if the current node needs to be swapped. It should be at the
		// beginning of the list (because of in-order creation of the list)
		if (pairs.size() > 0) {
			Node[] nextPair = pairs.get(0);
			if (this == nextPair[0]) {
				// Mark node2 as being the cross point, then return it
				Node newNode = nextPair[1].clone();
				newNode.setIsCrossPoint(true);

				// Remove the pair from the list
				pairs.remove(0);

				return newNode;
			}
		}

		// Otherwise, keep cloning until we find the swap point
		Node newNode = lightClone();
		for (int i = 0; i < children.length; i++) {
			newNode.children[i] = children[i].swapNodes(pairs);
			newNode.children[i].setParent(newNode);
		}

		return newNode;
	}

	public abstract Object evaluate(Problem problem, Object data);

	public abstract String toString();
}
