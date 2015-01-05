package msu.research.gp.simple.gp;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;

import msu.research.gp.simple.problem.ProblemRunner;
import msu.research.gp.simple.regression.SymbolicRegression;
import msu.research.gp.simple.representation.Individual;
import msu.research.gp.simple.util.Config;
import msu.research.gp.simple.util.Context;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the {@link GP} class for functionality.
 * 
 * @author Armand R. Burks
 * 
 */
public class GPTest {
	private static SymbolicRegression symbolicRegression;

	private static Config config;

	private static Context context;

	private static GP gp;

	@BeforeClass
	public static void setup() throws FileNotFoundException, IOException {
		// Make log4j be quiet!
		PropertyConfigurator.configure("log4j.properties.unittest");

		// Setup the output directory so the test won't fail if it was deleted.
		ProblemRunner.checkDirs("testOutput");

		config = new Config("src/test/resources/simpleRegression.properties");

		// Make sure config points to the test output directory
		config.setOutputDir("testOutput");

		context = new Context(config);

		// Now setup the symbolic regression problem
		symbolicRegression = new SymbolicRegression();
		symbolicRegression.init(context);

	}

	@Before
	public void setupTest() throws Exception {
		// Setup the GP
		gp = new GP(context);
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
}
