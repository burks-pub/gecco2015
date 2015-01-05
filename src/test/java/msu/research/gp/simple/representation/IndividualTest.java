package msu.research.gp.simple.representation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import msu.research.gp.simple.multiplexer.Multiplexer;
import msu.research.gp.simple.multiplexer.nodes.A0;
import msu.research.gp.simple.multiplexer.nodes.A1;
import msu.research.gp.simple.problem.ProblemRunner;
import msu.research.gp.simple.util.Config;
import msu.research.gp.simple.util.Context;

import org.apache.log4j.PropertyConfigurator;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the {@link Individual} class for functionality.
 * 
 * @author Armand R. Burks
 * 
 */
public class IndividualTest {
	private static Multiplexer mux;
	private static Config config;
	private static Config regressionConfig;

	@BeforeClass
	public static void setup() throws FileNotFoundException, IOException {
		// Make log4j be quiet!
		PropertyConfigurator.configure("log4j.properties.unittest");

		// Setup the output directory so the test won't fail if it was deleted.
		ProblemRunner.checkDirs("testOutput");

		// Now setup the multiplexer problem
		mux = new Multiplexer();
		config = new Config("src/test/resources/multiplexer.properties");

		// Make sure config points to the test output directory
		config.setOutputDir("testOutput");

		regressionConfig = new Config(
				"src/test/resources/regression.properties");
		mux.init(new Context(config));

		// Setup the output directory so the test won't fail if it was deleted.
		ProblemRunner.checkDirs("testOutput");
	}

	/**
	 * Reset some stuff after tests run.
	 */
	@After
	public void cleanup() {
		// Put the crossFuncProb back to the default 0.9.
		config.setCrossFuncProbability(0.9);
	}

	/**
	 * Make sure equals() works as expected with individuals of different IDs
	 * but the same tree.
	 */
	@Test
	public void testEqualsDiffIdSameTree() {
		Individual ind1 = new Individual();
		ind1.setId(0);
		ind1.setRoot(new A0());

		Individual ind2 = new Individual();
		ind2.setId(0);
		ind2.setRoot(new A0());

		assertFalse(ind1.equals(ind2));
	}

	/**
	 * Make sure equals() works as expected with individuals of different IDs
	 * and different trees.
	 */
	@Test
	public void testEqualsDiffIdDiffTree() {
		Individual ind1 = new Individual();
		ind1.setId(0);
		ind1.setRoot(new A0());

		Individual ind2 = new Individual();
		ind2.setId(0);
		ind2.setRoot(new A1());

		assertFalse(ind1.equals(ind2));
	}

	/**
	 * Tests that our equals and hashCode is working correctly. We test that the
	 * set says it contains the individual we added.
	 */
	@Test
	public void testSetContains() {
		HashSet<Individual> inds = new HashSet<Individual>();
		Individual ind = new Individual();
		ind.setId(0);
		ind.setRoot(new A0());

		inds.add(ind);

		assertTrue(inds.contains(ind));
	}

	/**
	 * Tests that our equals and hashCode is working correctly. We test that the
	 * set says it contains individual if we test on a clone of it.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testSetContainsClone() throws CloneNotSupportedException {
		HashSet<Individual> inds = new HashSet<Individual>();
		Individual ind = new Individual();
		ind.setId(0);
		ind.setRoot(new A0());

		inds.add(ind);

		Individual clone = ind.clone();
		assertTrue(inds.contains(clone));
	}

	/**
	 * Tests that equals and hashCode work correctly with Sets. Here we make
	 * sure that an individual isn't added multiple times if we call add on it
	 * multiple times.
	 */
	@Test
	public void testSetMultiAdd() {
		HashSet<Individual> inds = new HashSet<Individual>();
		Individual ind = new Individual();
		ind.setId(0);
		ind.setRoot(new A0());

		inds.add(ind);
		inds.add(ind);
		inds.add(ind);

		assertEquals(1, inds.size());
	}

	/**
	 * Tests that fromString() creates the correct individual for a one-level
	 * even tree.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testFromStringSingleLevel() throws CloneNotSupportedException {
		String genotype = "(AND D0 D1)";

		Individual ind = Individual.fromString(genotype, config);

		assertEquals(genotype, ind.toString());
	}

	/**
	 * Tests that fromString() creates the correct individual for a one-level
	 * tree that only has a root and a child.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testFromStringSingleLevelSingleChild()
			throws CloneNotSupportedException {
		String genotype = "(NOT D0)";

		Individual ind = Individual.fromString(genotype, config);

		assertEquals(genotype, ind.toString());
	}

	/**
	 * Tests that fromString() creates the correct individual for a multi-level
	 * even tree.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testFromStringMultiLevelEven()
			throws CloneNotSupportedException {
		String genotype = "(AND (AND D0 D1) D1)";

		Individual ind = Individual.fromString(genotype, config);

		assertEquals(genotype, ind.toString());
	}

	/**
	 * Tests that fromString() creates the correct individual for a multi-level
	 * odd tree.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testFromStringMultiLevelOdd() throws CloneNotSupportedException {
		String genotype = "(AND (AND D0 (NOT D1)) D1)";

		Individual ind = Individual.fromString(genotype, config);

		assertEquals(genotype, ind.toString());
	}

	/**
	 * Tests that fromString() creates the correct individual for a single-node
	 * tree.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testFromStringSingleNodeTree()
			throws CloneNotSupportedException {
		String genotype = "D0";

		Individual ind = Individual.fromString(genotype, config);

		assertEquals(genotype, ind.toString());
	}

	/**
	 * Make sure the individual's expression evaluates correctly when it is
	 * created from a string.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testFromStringEval() throws CloneNotSupportedException {
		Individual ind = Individual.fromString("(AND D0 D1)", config);

		// Boolean string "11 (AND D0 D1) is true"
		assertEquals(true, ind.getRoot().evaluate(mux, 3));
	}

	/**
	 * Tests that the number of nodes is set correctly when creating an
	 * individual from string.
	 */
	@Test
	public void testFromStringNumNodes() throws CloneNotSupportedException {
		Individual ind = Individual.fromString("(OR (AND D0 D1) D1", config);

		assertEquals(5, ind.getNumNodes());
	}

	/**
	 * Tests that the node numbers are set up as expected when the individual is
	 * created from string. We just call subTreeToNumberedString() on the root
	 * which essentially tests that the numbers are correct because the
	 * resulting tree wouldn't be right otherwise.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testFromStringNodeNumbers() throws CloneNotSupportedException {
		Individual ind = Individual.fromString("(OR (AND D0 D1) D1", config);

		assertEquals("(0 (1 2 3) 4)", ind.getRoot().subtreeToNumberedString());
	}

	/**
	 * Makes sure that the individual is created correctly when the string has
	 * an ephemeral constant node in it.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testFromStringEphemeral() throws CloneNotSupportedException {
		Individual ind = Individual.fromString("(* x 1.5)", regressionConfig);

		assertEquals("(* x 1.5)", ind.toString());
	}

	/**
	 * Simply tests that age is set after individuals are crossed.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testCrossoverAgeSet() throws CloneNotSupportedException {
		Individual p1 = Individual.fromString("(AND D0 D1)", config);
		Individual p2 = Individual.fromString("(OR D0 D1)", config);

		p1.ageIncr();
		p2.ageIncr();

		Individual[] children = p1.crossover(p2, new Context(config));

		// Go ahead and do two asserts :)
		assertEquals(1, children[0].getAge());
		assertEquals(1, children[1].getAge());
	}

	/**
	 * Simply tests that age is set to the max of the parents after individuals
	 * are crossed. Currently, we don't consider whether or not an individual
	 * was crossed at the root for age, so we can expect age to always be the
	 * max of the two parents' age.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testCrossoverMaxParentAge() throws CloneNotSupportedException {
		Individual p1 = Individual.fromString("(AND D0 D1)", config);
		Individual p2 = Individual.fromString("(OR D0 D1)", config);

		p1.setAge(30);
		p2.setAge(2);

		Individual[] children = p1.crossover(p2, new Context(config));

		// Go ahead and do two asserts :)
		assertEquals(30, children[0].getAge());
		assertEquals(30, children[1].getAge());
	}

	/**
	 * Simply tests that age is set to the max of the parents after individuals
	 * are crossed. Currently, we don't consider whether or not an individual
	 * was crossed at the root for age, so we can expect age to always be the
	 * max of the two parents' age.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testSetAgeMaxAge() throws CloneNotSupportedException {
		Individual p1 = Individual.fromString("(AND D0 D1)", config);
		Individual p2 = Individual.fromString("(OR D0 D1)", config);
		Individual c1 = Individual.fromString("D0", config);

		p1.setAge(30);
		p2.setAge(2);

		Individual.setAge(c1, p1, p2);

		assertEquals(30, c1.getAge());
	}

	/**
	 * Tests that the getAllNodes() method works as expected.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testGetAllNodes() throws CloneNotSupportedException {
		Individual ind = Individual.fromString("(AND (AND D1 D2) (OR D0 D1))",
				config);

		List<Node> nodes = ind.getAllNodes();

		boolean allNotNull = true;

		for (int i = 0; i < nodes.size(); i++) {
			if (nodes.get(i) == null) {
				allNotNull = false;
				break;
			}
		}

		assertTrue(nodes.size() == 7 && allNotNull);
	}

	/**
	 * Tests that we get a function node from getRandomNode() if we set the
	 * crossFuncProb to 1.0.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testGetRandomNodeAllFunc() throws CloneNotSupportedException {
		Individual ind = Individual.fromString("(AND (AND D1 D2) (OR D0 D1))",
				config);

		// Set the crossFuncProb to 1.0
		config.setCrossFuncProbability(1.0);

		// Make sure that the randomly selected node is a function node
		assertFalse(ind.getRandomNode(new Context(config)).isTerminal());

	}

	/**
	 * Tests that we get a terminal node from getRandomNode() if we set the
	 * crossFuncProb to 0.0.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testGetRandomNodeAllTerm() throws CloneNotSupportedException {
		Individual ind = Individual.fromString("(AND (AND D1 D2) (OR D0 D1))",
				config);

		// Set the crossFuncProb to 0.0
		config.setCrossFuncProbability(0.0);

		// Make sure that the randomly selected node is a terminal node
		assertTrue(ind.getRandomNode(new Context(config)).isTerminal());
	}

	@Test
	public void testCrossPointGoodSizeBad() throws CloneNotSupportedException {
		Individual ind1 = Individual.fromString("(AND (AND D1 D2) (OR D0 D1))",
				config);

		Individual ind2 = Individual.fromString(
				"(AND (AND D1 (AND D0 D1)) (OR D0 D1))", config);

		Node p1CrossPoint = ind1.findNode(6); // Right-most D1 terminal node
		Node p2CrossPoint = ind2.findNode(1); // Second AND node

		// Setup the max size
		config.setMaxSize(10);

		// Now call crossPointGood()
		boolean p2SwapGood = Individual.crossPointGood(p1CrossPoint,
				p2CrossPoint, ind1.getNumNodes(), new Context(config));

		// p2CrossPoint should violate the maxSize
		assertFalse(p2SwapGood);
	}
}
