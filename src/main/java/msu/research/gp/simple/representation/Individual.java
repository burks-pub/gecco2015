package msu.research.gp.simple.representation;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.log4j.Logger;

import msu.research.gp.simple.regression.nodes.R;
import msu.research.gp.simple.util.Config;
import msu.research.gp.simple.util.Context;

/**
 * Defines a GP individual.
 * 
 * @author Armand R. Burks
 * 
 */
public class Individual implements Cloneable, Comparable<Individual> {
	// The individual's ID, which is a UUID with |generation appended.
	private String id;

	// Holds a tag which can be used for grouping individuals.
	private String tag;

	// Holds the root of the tree.
	private Node root;

	// Holds the depth of the tree
	private int depth;

	// The individual's fitness.
	private double fitness;

	// The number of hits the individual has.
	private int hits;

	// Holds the total number of nodes in the tree, based on the numbering
	// scheme
	private int numNodes;

	// Whether or not the individual has been evaluated.
	private boolean isEvaluated;

	// Whether or not the individual is optimal.
	private boolean isOptimal;

	// Holds the age of the individual; age can be defined a number of ways.
	private int age;

	// Holds the current layer to which the individual belongs
	private int currentLayer;

	/**
	 * Remembers whether or not the root node was chosen to be the crossover
	 * point for this individual. If so, the other individual becomes the root
	 * parent.
	 */
	private boolean isRootCrossPoint = false;

	// Log4j logger for any output messages
	private static final Logger logger = Logger.getLogger(Individual.class);

	/**
	 * Convenience method to check both individuals to see if either cross point
	 * is the root.
	 * 
	 * @param p1CrossPoint
	 *            the cross point in this individual
	 * @param p2CrossPoint
	 *            the cross point in the other individual
	 * @param parent2
	 *            the other individual
	 */
	private void checkRootCrossPoints(Node p1CrossPoint, Node p2CrossPoint,
			Individual parent2) {
		if (p1CrossPoint.getParent() == null) {
			this.isRootCrossPoint = true;
		}

		if (p2CrossPoint.getParent() == null) {
			parent2.setIsRootCrossPoint(true);
		}
	}

	/**
	 * Recursive helper that returns the node with the given number.
	 * 
	 * @param nodeNumber
	 *            the node number of the node we're looking for
	 * 
	 * @param start
	 *            the node at which to start the search
	 */
	private Node findNode(int nodeNumber, Node start) {
		Stack<Node> stack = new Stack<Node>();
		stack.push(start);

		while (!stack.isEmpty()) {
			Node test = stack.pop();

			// See if we've found it.
			if (test.getNodeNumber() == nodeNumber) {
				return test;
			}

			// Otherwise push all the children onto the stack.
			for (int i = 0; i < test.getNumChildren(); i++) {
				stack.push(test.getChild(i));
			}
		}

		// We'd better not get here!
		return null;
	}

	/**
	 * Convenience method to perform a recursive pre-order DFS on the tree and
	 * number each node as well as tally up the total number of nodes in the
	 * tree.
	 * 
	 * @param currentNode
	 *            the current node in the traversal, should start at root
	 */
	private void numberAndCountNodes(Node currentNode) {
		currentNode.setNodeNumber(getNumNodes());
		numNodes++;

		for (int i = 0; i < currentNode.getNumChildren(); i++) {
			numberAndCountNodes(currentNode.getChild(i));
		}
	}

	/**
	 * Helper method to do the full Koza-style subtree crossover on the
	 * individuals.
	 * 
	 * @param parent2
	 * @param children
	 * @param p1CrossPoint
	 * @param p2CrossPoint
	 * @throws CloneNotSupportedException
	 */
	private void doFullCross(Individual parent2, Individual[] children,
			Node p1CrossPoint, Node p2CrossPoint)
			throws CloneNotSupportedException {
		Node child1Root = root.swapNode(p1CrossPoint, p2CrossPoint);
		Node child2Root = parent2.getRoot()
				.swapNode(p2CrossPoint, p1CrossPoint);

		children[0].setRoot(child1Root);
		children[1].setRoot(child2Root);
	}

	public Individual() {
		// We don't want to say an individual belongs to layer 0 by default!
		this.currentLayer = -1;
	}

	/**
	 * Sets the individual's ID to a new UUID with |generation appended.
	 * 
	 * @param gen
	 *            the individual's generation number
	 */
	public void setId(int gen) {
		UUID uuid = UUID.randomUUID();
		id = uuid.toString() + "|" + gen;
	}

	/**
	 * Sets the individual's ID to the requested string.
	 * 
	 * @param id
	 *            the id to which to set the individual's id
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the individual's ID.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the individual's tag to the requested value.
	 * 
	 * @param tag
	 *            the tag to which to give the individual
	 */
	public void setTag(String tag) {
		this.tag = tag;
	}

	/**
	 * @return the individual's tag.
	 */
	public String getTag() {
		return this.tag;
	}

	/**
	 * Sets the age of the individual to the given value.
	 * 
	 * @param age
	 *            the age of the individual
	 */
	public void setAge(int age) {
		this.age = age;
	}

	/**
	 * Sets the age of the individual to the maximum age of the two parents plus
	 * one, as in Hornby's ALPS.
	 * 
	 * @param child
	 *            the individual whose age to set
	 * @param parent1
	 *            the first parent
	 * @param parent2
	 *            the second parent
	 */
	public static void setAge(Individual child, Individual parent1,
			Individual parent2) {
		int maxAge = Math.max(parent1.getAge(), parent2.getAge());

		child.setAge(maxAge);
	}

	/**
	 * Increments the individual's age by one.
	 */
	public void ageIncr() {
		this.age++;
	}

	/**
	 * 
	 * @return the age of the individual
	 */
	public int getAge() {
		return age;
	}

	/**
	 * Sets the individual's current layer to the requested number.
	 * 
	 * @param layer
	 *            the current layer to which the individual belongs
	 */
	public void setCurrentLayer(int layer) {
		this.currentLayer = layer;
	}

	/**
	 * @return the current layer to which the individual belongs
	 */
	public int getCurrentLayer() {
		return this.currentLayer;
	}

	/**
	 * Sets the individual's fitness to the given value.
	 * 
	 * @param fitness
	 *            the value to which to set the individual's fitness
	 */
	public void setFitness(double fitness) {
		this.fitness = fitness;

		// See if it's optimal
		if (fitness == 1.0) {
			isOptimal = true;
		}
	}

	/**
	 * @return the individual's fitness.
	 */
	public double getFitness() {
		return fitness;
	}

	/**
	 * Sets the individual's hits to the given value.
	 * 
	 * @param hits
	 *            the value to which to set the individual's hits
	 */
	public void setHits(int hits) {
		this.hits = hits;
	}

	/**
	 * @return the individual's number of hits
	 */
	public int getHits() {
		return hits;
	}

	/**
	 * Convenience method to check a crossover node to make sure it is a good
	 * point for crossover.
	 * 
	 * @param crossPoint
	 *            the node selected as the crossover point
	 * @param nodeToSwap
	 *            the node that will be swapped in at the cross point
	 * 
	 * @param recipientSize
	 *            the original size of the tree to receive the node and it's
	 *            subtree
	 * @param context
	 *            the Context
	 * @return true if the crossover point is acceptable or false otherwise
	 */
	public static boolean crossPointGood(Node crossPoint, Node nodeToSwap,
			int recipientSize, Context context) {
		// If the crosspoint is the root, then it's just fine.
		if (crossPoint.getParent() == null) {
			return true;
		}

		// Otherwise, just make sure that the max depth/size aren't violated
		boolean depthGood = crossPoint.getDepth() + nodeToSwap.depthFrom() <= context
				.getConfig().getMaxDepth();

		boolean sizeGood = (recipientSize - crossPoint.sizeFrom())
				+ nodeToSwap.sizeFrom() <= context.getConfig().getMaxSize();

		return (depthGood && sizeGood);

	}

	/**
	 * Does the standard Koza-style subtree crossover.
	 * 
	 * @param parent2
	 *            the individual to crossover with this individual
	 * @param context
	 *            the Context for the run
	 * @return an array of the two children that were produced
	 * @throws CloneNotSupportedException
	 */
	public Individual[] subtreeCrossover(Individual parent2, Context context)
			throws CloneNotSupportedException {
		Individual children[] = { new Individual(), new Individual() };
		boolean p1CrossPointGood = false, p2CrossPointGood = false;
		boolean child1IsCopy = false, child2IsCopy = false;
		boolean didCross = false;
		Node p1CrossPoint = null, p2CrossPoint = null;

		for (int i = 0; i < context.getConfig().getMaxCrossAttempts()
				&& (!p1CrossPointGood && !p2CrossPointGood); i++) {

			// Get a node in p1
			if (!p1CrossPointGood) {
				p1CrossPoint = getRandomNode(context);
			}

			// Get a node in p2 and check it.
			if (!p2CrossPointGood) {
				p2CrossPoint = parent2.getRandomNode(context);
			}

			// Check p1CrossPoint since we have to wait on p2 until now
			p1CrossPointGood = crossPointGood(p1CrossPoint, p2CrossPoint,
					this.numNodes, context);
			p2CrossPointGood = crossPointGood(p2CrossPoint, p1CrossPoint,
					parent2.getNumNodes(), context);

			if (p1CrossPointGood && p2CrossPointGood) {
				// Swap them!
				doFullCross(parent2, children, p1CrossPoint, p2CrossPoint);
				didCross = true;
			}
		}

		if (!didCross) {
			// Check again. One may be ok and the other not.
			p1CrossPointGood = crossPointGood(p1CrossPoint, p2CrossPoint,
					this.numNodes, context);
			p2CrossPointGood = crossPointGood(p2CrossPoint, p1CrossPoint,
					parent2.getNumNodes(), context);

			// If we never found a good cross, just produce copies of the
			// parents
			if (!p1CrossPointGood && !p2CrossPointGood) {
				children[0] = this.lightClone();
				children[1] = parent2.lightClone();
				child1IsCopy = true;
				child2IsCopy = true;
			}

			// If p1 is good swap it and just copy of p2
			else if (p1CrossPointGood & !p2CrossPointGood) {
				children[0].setRoot(root.swapNode(p1CrossPoint, p2CrossPoint));
				children[1] = parent2.lightClone();
				child2IsCopy = true;
			}
			// If p2 is good swap it and just copy p1
			else if (p2CrossPointGood && !p1CrossPointGood) {
				children[0] = this.lightClone();
				children[1].setRoot(parent2.getRoot().swapNode(p2CrossPoint,
						p1CrossPoint));
				child1IsCopy = true;
			}
		}

		// Now we need to renumber the nodes in both individuals.
		children[0].numberAndCountNodes();
		children[1].numberAndCountNodes();

		// Set the depth also...
		children[0].setDepth(children[0].getRoot().depthFrom());
		children[1].setDepth(children[1].getRoot().depthFrom());

		// This better not ever happen!
		if (children[0].getDepth() > context.getConfig().getMaxDepth()
				|| children[1].getDepth() > context.getConfig().getMaxDepth()) {
			logger.fatal("Max depth was exceeded by crossover! Exiting.");
			logger.debug(String.format("cross points good? %b: %b",
					p1CrossPointGood, p2CrossPointGood));
			System.exit(1);
		}

		// Check if either cross point is the root node
		checkRootCrossPoints(p1CrossPoint, p2CrossPoint, parent2);

		// Set the tracking info
		setTrackingInfo(this, parent2, children, child1IsCopy, child2IsCopy,
				context.getConfig());

		return children;
	}

	/**
	 * Crosses the individual with the requested individual. Based on the ECJ
	 * implementation, this tries at most max times to find an acceptable
	 * crossover point and just produces a copy if unsuccessful. Also discards
	 * the second child unless told not to.
	 * 
	 * @throws CloneNotSupportedException
	 */
	public Individual[] crossover(Individual parent2, Context context)
			throws CloneNotSupportedException {
		return subtreeCrossover(parent2, context);
	}

	/**
	 * Creates an offspring that is an exact copy of this individual.
	 * 
	 * @return an offspring that is an exact copy of this individual.
	 * 
	 * @throws CloneNotSupportedException
	 */
	public Individual replicate() throws CloneNotSupportedException {
		// Get a clone.
		Individual child = this.lightClone();

		child.numberAndCountNodes();

		return child;
	}

	/**
	 * Convenience method to set the age as in Hornby's ALPS, but we leave
	 * incrementing the age to the calling algorithm, as it could be different.
	 * 
	 * @param parent1
	 *            the first parent
	 * @param parent2
	 *            the second parent
	 * @param children
	 *            the children array
	 * 
	 * @param child1IsCopy
	 *            whether or not child1 is an exact copy of parent1
	 * @param chil2IsCopy
	 *            whether or not child2 is an exact copy of parent2
	 * @param config
	 *            the Config object
	 * @throws CloneNotSupportedException
	 */
	public static void setTrackingInfo(Individual parent1, Individual parent2,
			Individual[] children, boolean child1IsCopy, boolean child2IsCopy,
			Config config) {
		Individual child1 = children[0];
		Individual child2 = children[1];

		// Update the memIds if the children aren't exact copies
		if (!child1IsCopy) {
			// Set age.
			setAge(child1, parent1, parent2);
		} else {
			// Otherwise age is parent's age + 1
			child1.setAge(parent1.getAge());
		}

		if (!child2IsCopy) {
			// Set age.
			setAge(child2, parent1, parent2);
		} else {
			// Otherwise age is parent's age + 1
			child2.setAge(parent2.getAge());
		}
	}

	/**
	 * Returns the node with the given number.
	 * 
	 * @param nodeNumber
	 *            the node number
	 * 
	 * @return the node with the given node number.
	 */
	public Node findNode(int nodeNumber) {
		// Start at root and get the node. There's gotta be a better way.
		Node ret = findNode(nodeNumber, root);

		if (ret == null) {
			logger.warn("NODE NOT FOUND!! RETURNING NULL.");
		}

		return ret;
	}

	/**
	 * Convenience method to recursively gather all the nodes of this tree. This
	 * uses a standard in-order traversal to place the nodes into a list.
	 * 
	 * @param currentNode
	 *            the current node in the traversal - should pass the root
	 * 
	 * @param nodes
	 *            the list of nodes (length numNodes) where we'll collect the
	 *            nodes
	 */
	private void collectAllNodes(Node currentNode, List<Node> nodes) {
		// Add the current node
		nodes.add(currentNode);

		// Visit all the children of the current node
		for (int i = 0; i < currentNode.getNumChildren(); i++) {
			collectAllNodes(currentNode.getChild(i), nodes);
		}
	}

	/**
	 * @return a list containing all the nodes in the tree
	 */
	public List<Node> getAllNodes() {
		List<Node> nodes = new ArrayList<Node>();

		collectAllNodes(this.root, nodes);

		return nodes;
	}

	/**
	 * Chooses a random node in this individual's tree.
	 * 
	 * @return a reference to the chosen node.
	 */
	public Node getRandomNode(Context context) {
		// Do we select a function node or a terminal?
		boolean doCrossFunc = context.nextBool(context.getConfig()
				.getCrossFuncProbability());

		// Let's get all the nodes into an array to save later expense
		List<Node> allNodes = getAllNodes();

		Node node = null;

		while (node == null || (doCrossFunc && node.isTerminal())
				|| (!doCrossFunc && !node.isTerminal())) {

			node = allNodes.get(context.randBetween(0, allNodes.size() - 1));

			if (numNodes == 1) {
				break;
			}
		}

		return node;
	}

	/**
	 * Sets the given node to be the root of the tree.
	 * 
	 * @param node
	 *            a pointer to the node to set as the root
	 */
	public void setRoot(Node node) {

		root = node;
	}

	/**
	 * @return the root of the tree
	 */
	public Node getRoot() {

		return root;
	}

	/**
	 * Sets the depth of the tree. Use with care!
	 * 
	 * @param d
	 *            the depth
	 */
	public void setDepth(int d) {
		depth = d;
	}

	/**
	 * @return the depth of the tree.
	 */
	public int getDepth() {

		return depth;
	}

	/**
	 * Sets the number of nodes to the given number, which is based on a
	 * traversal or newly generated expression numbering.
	 * 
	 * @param num
	 *            the number of nodes
	 */
	public void setNumNodes(int num) {
		numNodes = num;
	}

	/**
	 * @return the number of nodes in this individual.
	 */
	public int getNumNodes() {
		return numNodes;
	}

	/**
	 * Returns a string representation of the individual, which is just the
	 * lisp-style representation of the tree just like ECJ does it.
	 */
	public String toString() {
		return root.subtreeToString();
	}

	/**
	 * Creates an individual from the a string containing its genotype. The
	 * string should be a lisp-style tree.
	 * 
	 * @param genotype
	 *            the genotype string with which to create the individual.
	 * 
	 * @param config
	 *            the config object containing the function and terminal set
	 * 
	 * @return a newly created individual based on the given genotype string
	 * @throws CloneNotSupportedException
	 */
	public static Individual fromString(String genotype, Config config)
			throws CloneNotSupportedException {
		Individual ind = new Individual();
		Stack<Node> nodeStack = new Stack<Node>();
		String[] nodeStrings = genotype.split(" ");

		for (int i = 0; i < nodeStrings.length; i++) {
			String nodeString = nodeStrings[i].replaceAll("\\(|\\)", "");
			Node mappedNode = null;

			// First see if it's a mapped node
			mappedNode = config.getMappedNode(nodeString);

			// Otherwise, see if the node is a constant node
			if (mappedNode == null) {
				try {
					double constant = Double.parseDouble(nodeString);
					mappedNode = new R();
					((R) mappedNode).setValue(constant);
				} catch (Exception e) {
					// Do nothing.
				}
			}

			if (mappedNode != null) {
				// Grab a clone for the individual to own
				Node node = mappedNode.clone();
				node.setId();
				node.setMemId(node.getId());
				node.setNodeNumber(i);

				// Either we're at the beginning or we have a single node tree.
				if (i == 0) {
					ind.setRoot(node);

					// Single node tree
					if (node.isTerminal()) {
						break;
					}

					// Push the node onto the stack
					else {
						nodeStack.push(node);
					}
				}

				// Now we need to pop from the stack and keep building
				else {
					// First pop the current node and see if it's done
					Node currentNode = nodeStack.pop();

					int childIndex = 0;
					for (childIndex = 0; childIndex < currentNode
							.getNumChildren(); childIndex++) {
						if (currentNode.getChild(childIndex) == null) {
							currentNode.setChild(childIndex, node);
							node.setParent(currentNode);

							break;
						}
					}

					// Push the current node back on if we're not done
					if (childIndex < currentNode.getNumChildren() - 1) {
						nodeStack.push(currentNode);
					}

					// If the next node is a function, push it as well
					if (!node.isTerminal()) {
						nodeStack.push(node);
					}
				}
			} else {
				logger.error(nodeStrings[i].replaceAll("\\(|\\)", "")
						+ " is not mapped. Returning null!");
				return null;
			}

		}

		// Set the number of nodes
		ind.setNumNodes(nodeStrings.length);

		// Set the depth
		ind.setDepth(ind.getRoot().depthFrom());

		return ind;
	}

	/**
	 * Sets the evaluated flag for this individual.
	 * 
	 * @param isEvaluated
	 *            whether or not the individual has been evaluated.
	 */
	public void setIsEvaluated(boolean isEvaluated) {
		this.isEvaluated = isEvaluated;
	}

	/**
	 * 
	 * @return whether or not this individual has been evaluated.
	 */
	public boolean isEvaluated() {
		return this.isEvaluated;
	}

	/**
	 * Sets the isOptimal flag to the given value.
	 * 
	 * @param isOptimal
	 *            whether or not the individual is optimal
	 */
	public void setIsOptimal(boolean isOptimal) {
		this.isOptimal = isOptimal;
	}

	/**
	 * @return whether or not this individual is optimal
	 */
	public boolean isOptimal() {
		return this.isOptimal;
	}

	/**
	 * Does a DFS traversal starting at the root, and (re)numbers every node in
	 * the tree, setting the individual's numNodes after all nodes have been
	 * numbered and counted. This is needed after crossover or any (add/delete)
	 * modification to the individual's tree.
	 */
	public void numberAndCountNodes() {
		numNodes = 0;
		numberAndCountNodes(root);
	}

	/**
	 * Determines whether or not this individual's tree is equal to the other
	 * indiviudal's tree, where equals simply means that all nodes are the same.
	 * 
	 * @param other
	 *            the other tree to compare against
	 * @return true if the trees are equal or false otherwise
	 */
	public boolean treesEqual(Individual other) {
		return root.subtreeToString().equals(other.getRoot().subtreeToString());
	}

	/**
	 * Creates a deep clone of this individual. Cloning the tree as well as all
	 * the other members.
	 */
	public Individual clone() throws CloneNotSupportedException {
		Individual newIndividual = (Individual) super.clone();
		newIndividual.setRoot(root.clone());
		newIndividual.setNumNodes(numNodes);
		newIndividual.setDepth(depth);
		newIndividual.setFitness(fitness);
		newIndividual.setHits(hits);
		newIndividual.setAge(age);
		newIndividual.setIsEvaluated(isEvaluated);
		newIndividual.setIsOptimal(isOptimal);
		newIndividual.setTag(tag);
		newIndividual.setId(id);
		newIndividual.setCurrentLayer(currentLayer);
		newIndividual.numberAndCountNodes();

		return newIndividual;
	}

	/**
	 * Similar to clone(), but doesn't copy everything.
	 * 
	 * @return a clone of this Individual
	 * @throws CloneNotSupportedException
	 */
	public Individual lightClone() throws CloneNotSupportedException {
		Individual newIndividual = (Individual) super.clone();
		newIndividual.setRoot(root.clone());
		newIndividual.setNumNodes(numNodes);
		newIndividual.setDepth(depth);
		newIndividual.setFitness(fitness);
		newIndividual.setHits(hits);
		newIndividual.setAge(age);
		newIndividual.setTag(tag);
		newIndividual.setIsEvaluated(isEvaluated);
		newIndividual.setIsOptimal(isOptimal);
		newIndividual.numberAndCountNodes();

		return newIndividual;
	}

	/**
	 * Simply returns -1 if this individual's fitness is less, 0 if equal, and 1
	 * if greater than the other individual.
	 * 
	 * @param other
	 *            the other individual to compare against.
	 * 
	 * @return -1 if this individual's fitness is less, 0 if equal, and 1 if
	 *         greater than the other individual.
	 */
	public int compareTo(Individual other) {
		double otherFitness = other.getFitness();

		if (fitness == otherFitness) {
			return 0;
		} else if (fitness < otherFitness) {
			return -1;
		}
		return 1;
	}

	/**
	 * Sets whether or not the root node was chosen to be the crossover point.
	 * Use with much care!
	 * 
	 * @param isRootCrossPoint
	 *            whether or not the root node was chosen to be the crossover
	 *            point
	 */
	public void setIsRootCrossPoint(boolean isRootCrossPoint) {
		this.isRootCrossPoint = isRootCrossPoint;
	}

	/**
	 * 
	 * @return whether or not the root node was the crossover point for this
	 *         individual.
	 */
	public boolean isRootCrossPoint() {
		return this.isRootCrossPoint;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}

		if (other == this) {
			return true;
		}

		if (other.getClass() != getClass()) {
			return false;
		}

		Individual ind = (Individual) other;

		return new EqualsBuilder().append(id, ind.getId()).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).append(id).toHashCode();

	}
}
