package ec.research.gp.layers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;


import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ec.research.gp.layers.AlpsLayerScheme;
import ec.research.gp.layers.LayeredGP;
import ec.research.gp.simple.problem.ProblemRunner;
import ec.research.gp.simple.regression.SymbolicRegression;
import ec.research.gp.simple.representation.Individual;
import ec.research.gp.simple.util.Config;
import ec.research.gp.simple.util.Context;


public class AlpsLayerSchemeTest {
	private static SymbolicRegression symbolicRegression;

	private static Config config;

	private static Context context;

	private static LayeredGP layeredGP;

	private AlpsLayerScheme layerScheme;

	@BeforeClass
	public static void setup() throws FileNotFoundException, IOException {
		// Make log4j be quiet!
		PropertyConfigurator.configure("log4j.properties.unittest");

		// Now setup the symbolic regression problem
		symbolicRegression = new SymbolicRegression();
		config = new Config("src/test/resources/alpsTest.properties");
		context = new Context(config);
		symbolicRegression.init(new Context(config));

		// Make sure config points to the test output directory
		config.setOutputDir("testOutput");

		// Setup the output directory so the test won't fail if it was deleted.
		ProblemRunner.checkDirs("testOutput");
	}

	/**
	 * Various things needed by multiple tests
	 * 
	 * @throws Exception
	 */
	@Before
	public void setupTest() throws Exception {
		// Setup the LayeredGP
		layeredGP = new LayeredGP(context);
		layeredGP.init();
		layerScheme = (AlpsLayerScheme) layeredGP.getLayerScheme();
	}

	/**
	 * Tests that the add-layer test returns true when the generation is in
	 * range (i.e. it's time to add a layer because the number of generations is
	 * equal to the max age of the current top-most layer).
	 */
	@Test
	public void testAddLayer_genInRange() {
		// Fake the generation number.
		layeredGP.setGenerationNum(360);

		// At generation 360, we should have 7 previous layers.
		for (int i = 0; i < 6; i++) {
			layeredGP.getLayeredPopulation().add(new Vector<Individual>());
		}

		// Make sure the AlpsLayerScheme says it's time to add a new layer.
		assertTrue(layerScheme.doAddLayer(layeredGP));
	}

	/**
	 * Tests that the add-layer test returns false when the generation is just
	 * past the generation at which we most recently should've added a layer
	 * (i.e. we don't want it to add layers back to back). This should work even
	 * though the check does >= because we use the population size for the layer
	 * index in the test
	 */
	@Test
	public void testAddLayer_genOneOver() {
		// Fake the generation number.
		layeredGP.setGenerationNum(361);

		// After generation 360, we should have 8 layers.
		for (int i = 0; i < 7; i++) {
			layeredGP.getLayeredPopulation().add(new Vector<Individual>());
		}

		// Make sure the AlpsLayerScheme says it's time to add a new layer.
		assertFalse(layerScheme.doAddLayer(layeredGP));
	}

	/**
	 * Tests that the add-layer test returns true when the generation is in
	 * range (i.e. it's time to add a layer because the number of generations is
	 * equal to the max age of the current top-most layer) and we're just about
	 * to add the last layer.
	 */
	@Test
	public void testAddLayer_genInRangeMax() {
		// Fake the generation number.
		layeredGP.setGenerationNum(490);

		// At generation 360, we should have 7 previous layers.
		for (int i = 0; i < 7; i++) {
			layeredGP.getLayeredPopulation().add(new Vector<Individual>());
		}

		// Make sure the AlpsLayerScheme says it's time to add a new layer.
		assertTrue(layerScheme.doAddLayer(layeredGP));
	}

	/**
	 * Tests that the add-layer test returns false when the generation one past
	 * the generation at which we reached the maxNumLayers. With maxNumLayers =
	 * 9, and ageGap=10, this should be generation 491.
	 */
	@Test
	public void testAddLayer_genOneOverMax() {
		// Fake the generation number.
		layeredGP.setGenerationNum(491);

		// At generation 491, we should have 9 previous layers.
		for (int i = 0; i < 8; i++) {
			layeredGP.getLayeredPopulation().add(new Vector<Individual>());
		}

		// Make sure the AlpsLayerScheme says it's time to add a new layer.
		assertFalse(layerScheme.doAddLayer(layeredGP));
	}
}
