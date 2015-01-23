package ec.research.gp.simple.multiplexer;

import static org.junit.Assert.assertTrue;

import org.apache.log4j.PropertyConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;

import ec.research.gp.simple.bool.nodes.D0;
import ec.research.gp.simple.multiplexer.Multiplexer;
import ec.research.gp.simple.problem.ProblemRunner;
import ec.research.gp.simple.representation.Individual;
import ec.research.gp.simple.util.Config;
import ec.research.gp.simple.util.Context;



/**
 * Tests Multiplexer with generic address and data bits for functionality.
 * 
 */
public class GenericBitsMultiplexerTest {
	private static Multiplexer mux;
	private static Config config;
	private static Context context;

	@BeforeClass
	public static void setup() throws Exception {
		// Make log4j be quiet!
		PropertyConfigurator.configure("log4j.properties.unittest");

		// Setup the output directory so the test won't fail if it was deleted.
		ProblemRunner.checkDirs("testOutput");

		// Now setup the multiplexer problem
		mux = new Multiplexer();
		config = new Config(
				"src/test/resources/simple6Mux_GenericBits.properties");
		context = new Context(config);
		mux.init(context);

		// Make sure config points to the test output directory
		config.setOutputDir("testOutput");
	}

	/**
	 * Makes sure a hand-crafted solution to the 6-multiplexer is evaluated
	 * correctly.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testIdealSolution6Mux() throws CloneNotSupportedException {
		Individual soln = Individual.fromString(
				"(IF (AND A0 A1) D3 (IF A0 D1 (IF A1 D2 D0)))", config);

		// Now evaluate the individual and check its fitness and hits.
		mux.fitness(soln);

		assertTrue(soln.getFitness() == 1.0 && soln.getHits() == 64);

	}

	/**
	 * Tests that an incorrect solution doesn't get perfect fitness.
	 */
	@Test
	public void testIncorrectSolution() {
		// Create a single-node (DO) individual.
		Individual ind = new Individual();
		ind.setRoot(new D0());

		// Evaluate the individual and check its fitness.
		mux.fitness(ind);

		assertTrue(ind.getFitness() > 0 && ind.getFitness() < 1.0);
	}
}
