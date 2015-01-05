package msu.research.gp.layers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import msu.research.gp.layers.LayeredGP;
import msu.research.gp.simple.problem.ProblemRunner;
import msu.research.gp.simple.representation.Individual;
import msu.research.gp.simple.util.Config;
import msu.research.gp.simple.util.Context;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the {@link LayeredGP} for functionality.
 * 
 * @author Armand R. Burks
 * 
 */
public class LayeredGPTest {
	private static Config alpsConfig;

	private static Context alpsContext;

	private static LayeredGP alpsGP;

	@BeforeClass
	public static void setup() throws FileNotFoundException, IOException {
		// Make log4j be quiet!
		PropertyConfigurator.configure("log4j.properties.unittest");

		// Setup the output directory so the test won't fail if it was deleted.
		ProblemRunner.checkDirs("testOutput");

		// Now setup the symbolic regression problem
		alpsConfig = new Config("src/test/resources/alpsTest.properties");
		alpsContext = new Context(alpsConfig);

		// Make sure the configs point to the test output directory.
		alpsConfig.setOutputDir("testOutput");
	}

	@Before
	public void setupTest() throws Exception {
		// Setup the LayeredGP
		alpsGP = new LayeredGP(alpsContext);
	}

	/**
	 * Just makes sure that init creates a single layer full of individuals.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testInitNumLayers() throws CloneNotSupportedException {
		alpsGP.init();

		assertEquals(1, alpsGP.getLayeredPopulation().size());
	}

	/**
	 * Make sure that all individuals in the population are evaluated after
	 * init().
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testInitEvals() throws CloneNotSupportedException {
		alpsGP.init();

		for (Vector<Individual> layer : alpsGP.getLayeredPopulation()) {
			for (Individual ind : layer) {
				assertEquals(true, ind.isEvaluated());
			}

		}
	}

	/**
	 * Make sure that all individuals in the population are evaluated after
	 * step().
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testStepEvals() throws CloneNotSupportedException {
		alpsGP.init();
		alpsGP.step();

		for (Vector<Individual> layer : alpsGP.getLayeredPopulation()) {
			for (Individual ind : layer) {
				assertEquals(true, ind.isEvaluated());
			}
		}
	}

	/**
	 * Tests that the individual we try to move up is removed from its previous
	 * layer, using the moveUp method that works on the individual's index in
	 * its layer.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testMoveUpIndexIndGone() throws CloneNotSupportedException {
		alpsGP.init();

		/*
		 * Lets just move the first individual from layer 0. Using density
		 * scheme, so it shouldn't matter that we only have one layer
		 */
		Individual indToMove = alpsGP.getLayeredPopulation().get(0).get(0);

		alpsGP.moveIndividualUp(0, 0, false, false);

		assertFalse(alpsGP.getLayeredPopulation().get(0).contains(indToMove));
	}

	/**
	 * Tests that the individual we try to move up is removed from its previous
	 * layer, using the moveUp method that works on the actual individual
	 * instead of using its index in its layer.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testMoveUpIndIndGone() throws CloneNotSupportedException {
		alpsGP.init();

		/*
		 * Lets just move the first individual from layer 0. Using density
		 * scheme, so it shouldn't matter that we only have one layer
		 */
		Individual indToMove = alpsGP.getLayeredPopulation().get(0)
				.get(0);

		alpsGP.moveIndividualUp(indToMove, 0, false, false);

		assertFalse(alpsGP.getLayeredPopulation().get(0)
				.contains(indToMove));
	}
}
