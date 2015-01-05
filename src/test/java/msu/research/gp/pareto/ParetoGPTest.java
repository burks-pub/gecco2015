package msu.research.gp.pareto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


import msu.research.gp.pareto.ParetoGP;
import msu.research.gp.simple.problem.ProblemRunner;
import msu.research.gp.simple.representation.Individual;
import msu.research.gp.simple.util.Config;
import msu.research.gp.simple.util.Context;

import org.apache.log4j.PropertyConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the {@link ParetoGP} for functionality.
 * 
 * @author Armand R. Burks
 * 
 */
public class ParetoGPTest {
	private static Context context;
	private static ParetoGP gp;

	@BeforeClass
	public static void setup() throws Exception {
		// Make log4j be quiet!
		PropertyConfigurator.configure("log4j.properties.unittest");

		// Setup the output directory so the test won't fail if it was deleted.
		ProblemRunner.checkDirs("testOutput");

		// Setup the config/context and the GP
		Config config = new Config(
				"src/test/resources/paretoGPRegression.properties");

		// Make sure config points to the test output directory
		config.setOutputDir("testOutput");

		context = new Context(config);

		gp = new ParetoGP(context);
	}

	/**
	 * Make sure that all individuals in the population are evaluated after
	 * init().
	 * 
	 * @throws Exception
	 */
	@Test
	public void testInitEvals() throws Exception {
		gp.init();

		for (Individual ind : gp.getPopulation()) {
			assertEquals(true, ind.isEvaluated());
		}
	}

	/**
	 * Make sure that all individuals in the population are evaluated after
	 * step().
	 * 
	 * @throws Exception
	 */
	@Test
	public void testStepEvals() throws Exception {
		gp.init();
		gp.step();

		for (Individual ind : gp.getPopulation()) {
			assertEquals(true, ind.isEvaluated());
		}
	}

	/**
	 * Make sure that all individuals are tagged after step(). We're making sure
	 * that the TagThreads actually got and tagged all the individuals.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testStepTags() throws Exception {
		gp.init();
		gp.step();

		for (Individual ind : gp.getPopulation()) {
			assertTrue(ind.getTag() != null);
		}
	}
}
