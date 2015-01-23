package ec.research.gp.simple.representation;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;


import org.apache.log4j.PropertyConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;

import ec.research.gp.simple.representation.Individual;
import ec.research.gp.simple.representation.Node;
import ec.research.gp.simple.util.Config;


public class NodeTest {
	private static Config config;

	@BeforeClass
	public static void setup() throws FileNotFoundException, IOException {
		// Make log4j be quiet!
		PropertyConfigurator.configure("log4j.properties");
		config = new Config("src/test/resources/paretoNParity.properties");
	}

	/**
	 * Tests a previously problematic case where swapNode depth was being
	 * violated.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testSwapDepth() throws CloneNotSupportedException {
		Individual ind1 = Individual
				.fromString(
						"(AND (OR (OR (NOR D4 D1) (AND D0 D3)) (OR (NOR D0 (OR (AND D3 D2) (NOR D0 (AND (OR D4 D3) D1)))) (NOR D2 D1))) (OR (NOR (OR (AND (AND (AND (AND (OR D4 D3) D1) (NOR D3 D2)) (AND (AND D3 D1) (OR D4 D1))) (AND D1 (OR (AND D3 D2) (NOR D0 (AND (OR D4 D3) D1))))) D3) (AND (OR (NOR D4 D1) (AND D0 D3)) D1)) (OR (AND D2 D2) (AND D4 D3))))",
						config);
		Individual ind2 = Individual
				.fromString(
						"(AND (OR (OR (NOR D4 D1) (AND D0 D3)) (OR (NOR D0 (OR (AND D3 D2) (NOR D0 (AND (OR (AND D3 D1) D3) D1)))) (NOR D2 D1))) (OR (NOR (OR (AND (AND (AND (AND (OR D4 D3) D1) (NOR D3 D2)) (AND (AND D3 D1) (OR D4 D1))) (AND (OR (OR D1 D3) (AND D4 (NOR D4 D1))) (OR (AND D3 D2) (NOR D0 (AND (OR D4 D3) D1))))) D3) (AND (OR (NOR D4 D1) (AND (AND (OR D4 D3) D1) D3)) D1)) (OR (AND D2 D2) (AND D4 D3))))",
						config);

		Node p1CrossPoint = ind1.findNode(9);
		Node p2CrossPoint = ind2.findNode(81);

		int expectedDepth1 = Math.max(ind1.getDepth(), p1CrossPoint.getDepth()
				+ p2CrossPoint.depthFrom());
		int expectedDepth2 = Math.max(ind2.getDepth(), p2CrossPoint.getDepth()
				+ p1CrossPoint.depthFrom());

		Node child1 = ind1.getRoot().swapNode(p1CrossPoint, p2CrossPoint);
		Node child2 = ind2.getRoot().swapNode(p2CrossPoint, p1CrossPoint);

		assertEquals(expectedDepth1, child1.depthFrom());
		assertEquals(expectedDepth2, child2.depthFrom());
	}

	/**
	 * Tests that the resulting two offspring trees are as expected when we
	 * perform crossover (aka Node.swapNode).
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testSwapNodeResult() throws CloneNotSupportedException {
		Individual ind1 = Individual.fromString("(OR (AND D0 D1) D1)", config);
		Individual ind2 = Individual.fromString("(NAND D0 (OR D1 D2))", config);

		Node child1 = ind1.getRoot().swapNode(ind1.findNode(3),
				ind2.findNode(0));
		Node child2 = ind2.getRoot().swapNode(ind2.findNode(0),
				ind1.findNode(3));

		// Make life easier break the rules and just do an assert for each child
		assertEquals("(OR (AND D0 (NAND D0 (OR D1 D2))) D1)",
				child1.subtreeToString());
		assertEquals("D1", child2.subtreeToString());
	}

	/**
	 * Tests that sizeFrom() works as expected when called on the root.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testSizeFromRoot() throws CloneNotSupportedException {
		Individual ind = Individual.fromString("(OR (AND D0 D1) D1)", config);

		assertTrue(ind.getRoot().sizeFrom() == ind.getNumNodes());
	}

	/**
	 * Tests that sizeFrom() works as expected on an interior node
	 */
	@Test
	public void testSizeFromInterior() throws CloneNotSupportedException {
		Individual ind = Individual.fromString("(OR (AND D0 D1) D1)", config);
		Node and = ind.findNode(1);

		assertTrue(and.sizeFrom() == 3);
	}

	/**
	 * Tests that sizeFrom() works as expected on a terminal node.
	 */
	@Test
	public void testSizeFromLeaf() throws CloneNotSupportedException {
		Individual ind = Individual.fromString("(OR (AND D0 D1) D1)", config);
		Node d0 = ind.findNode(2);

		assertTrue(d0.sizeFrom() == 1);
	}

	/**
	 * Tests that the swapNodes() method works as expected.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testSwapNodes() throws CloneNotSupportedException {
		Individual ind1 = Individual.fromString(
				"(AND (AND D0 D1) (AND D1 D2))", config);
		Individual ind2 = Individual.fromString(
				"(OR (OR D0 D1) (OR D1 (AND D2 D3)))", config);

		// Setup the pairs of nodes to swap for ind1
		ArrayList<Node[]> pairs1 = new ArrayList<Node[]>();
		ArrayList<Node[]> pairs2 = new ArrayList<Node[]>();

		// Setup the expected expressions.
		String expected1 = "(AND (OR D0 D1) (AND D1 (AND D2 D3)))";
		String expected2 = "(OR (AND D0 D1) (OR D1 D2))";

		// Let's swap the first AND//OR child as well as the last D2 with NOT
		pairs1.add(new Node[] { ind1.findNode(1), ind2.findNode(1) });
		pairs1.add(new Node[] { ind1.findNode(6), ind2.findNode(6) });

		// We need the same pairs, but with nodes in the reverse order for ind2
		pairs2.add(new Node[] { ind2.findNode(1), ind1.findNode(1) });
		pairs2.add(new Node[] { ind2.findNode(6), ind1.findNode(6) });

		// Do the swaps.
		Node c1Root = ind1.getRoot().swapNodes(pairs1);
		Node c2Root = ind2.getRoot().swapNodes(pairs2);

		// Verify both the genotype strings -- just do 2 asserts ;)
		assertEquals(expected1, c1Root.subtreeToString());
		assertEquals(expected2, c2Root.subtreeToString());
	}
}
