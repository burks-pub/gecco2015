package ec.research.gp.simple.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;


import org.apache.log4j.PropertyConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;

import ec.research.gp.simple.operators.PointMutation;
import ec.research.gp.simple.problem.ProblemRunner;
import ec.research.gp.simple.regression.nodes.R;
import ec.research.gp.simple.representation.Individual;
import ec.research.gp.simple.util.Config;
import ec.research.gp.simple.util.Context;


/**
 * Tests {@link PointMutation} for functionality.
 * 
 */
public class PointMutationTest {
	private static Config config;
	private static Context context;

	@BeforeClass
	public static void setup() throws FileNotFoundException, IOException {
		// Make log4j be quiet!
		PropertyConfigurator.configure("log4j.properties.unittest");

		// Setup the output directory so the test won't fail if it was deleted.
		ProblemRunner.checkDirs("testOutput");

		config = new Config("src/test/resources/regression.properties");

		// Make sure config points to the test output directory
		config.setOutputDir("testOutput");

		context = new Context(config);
	}

	/**
	 * Very simple test to make sure that mutation doesn't fail and throw some
	 * kind of error.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testMutateSimple() throws CloneNotSupportedException {
		Individual ind = Individual.fromString("(+ x (* x x))", config);
		config.setMutationProbability(0.8);
		PointMutation.mutate(ind, context);
	}

	/**
	 * Tests that random ephemeral constants just get mutated by having their
	 * values changed.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testMutateConstantValues() throws CloneNotSupportedException {
		Individual ind = Individual.fromString("(+ 0.2 (* 1 -5.7302489))",
				config);
		// Make sure all nodes get mutated so we can check the terminals
		config.setMutationProbability(1.0);
		Individual mutant = PointMutation.mutate(ind, context);

		// We have a seed set so we don't have to worry about the very very
		// unlikely case that the values are mutated to the exact same value ;)
		int terminalNodeNums[] = { 1, 3, 4 };

		// Go ahead and do multiple asserts to check them all...
		for (int termNodeNum : terminalNodeNums) {
			assertFalse((Double) ((R) ind.findNode(termNodeNum)).evaluate(
					config.getProblem(), 0) == (Double) ((R) mutant
					.findNode(termNodeNum)).evaluate(config.getProblem(), 0));
		}
	}

	/**
	 * Make sure no point mutations happen when the probability is zero!
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testZeroProb() throws CloneNotSupportedException {
		Individual ind = Individual.fromString("(+ x (* x (- (% x x) 1)))",
				config);
		config.setMutationProbability(0.0);
		Individual mutant = PointMutation.mutate(ind, context);

		assertEquals(ind.toString(), mutant.toString());
	}

	/**
	 * Makes sure that no nodes end up with null parents, except for the root
	 * node of course.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testNodeParents() throws CloneNotSupportedException {
		Individual ind = Individual.fromString("(+ x (* x (- (% x x) 1)))",
				config);
		config.setMutationProbability(1.0);
		Individual mutant = PointMutation.mutate(ind, context);

		for (int i = 1; i < mutant.getNumNodes(); i++) {
			assertTrue(mutant.findNode(i).getParent() != null);
		}
	}

	/**
	 * Makes sure that all nodes have their numbers set correctly after
	 * mutation.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testNodeNumbers() throws CloneNotSupportedException {
		Individual ind = Individual.fromString("(+ x (* x (- (% x x) 1)))",
				config);
		config.setMutationProbability(1.0);
		Individual mutant = PointMutation.mutate(ind, context);

		assertEquals(ind.getRoot().subtreeToNumberedString(), mutant.getRoot()
				.subtreeToNumberedString());
	}
}
