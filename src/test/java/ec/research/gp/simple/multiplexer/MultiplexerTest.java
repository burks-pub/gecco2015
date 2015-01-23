package ec.research.gp.simple.multiplexer;

import static org.junit.Assert.assertTrue;

import java.util.Vector;


import org.apache.log4j.PropertyConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;

import ec.research.gp.layers.LayeredGP;
import ec.research.gp.pareto.ParetoGP;
import ec.research.gp.simple.bool.nodes.D0;
import ec.research.gp.simple.gp.GP;
import ec.research.gp.simple.multiplexer.Multiplexer;
import ec.research.gp.simple.problem.ProblemRunner;
import ec.research.gp.simple.representation.Individual;
import ec.research.gp.simple.util.Config;
import ec.research.gp.simple.util.Context;


/**
 * Tests the multiplexer problem.
 * 
 */
public class MultiplexerTest {
	private static Multiplexer mux;
	private static Config config;
	private static Context context;
	private static GP gp;
	private static LayeredGP layeredGP;
	private static ParetoGP paretoGP;

	@BeforeClass
	public static void setup() throws Exception {
		// Make log4j be quiet!
		PropertyConfigurator.configure("log4j.properties.unittest");

		// Setup the output directory so the test won't fail if it was deleted.
		ProblemRunner.checkDirs("testOutput");

		// Now setup the multiplexer problem
		mux = new Multiplexer();
		config = new Config("src/test/resources/multiplexer.properties");
		context = new Context(config);
		mux.init(context);

		// Make sure config points to the test output directory
		config.setOutputDir("testOutput");

		// Setup the GPs
		gp = new GP(context);
		layeredGP = new LayeredGP(context);
		paretoGP = new ParetoGP(context);
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

	/**
	 * Tests that the threaded fitness() evaluates individuals correctly in the
	 * standard GP!
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGPThreadedFitness() throws Exception {
		// Add multiple threads
		context.getConfig().setNumThreads(4);

		gp.init();

		// Clear out the population so we can create it as we see fit.
		gp.getPopulation().clear();

		// Fill the population with perfect-fitness individuals.
		for (int i = 0; i < context.getConfig().getPopSize(); i++) {
			Individual ind = Individual.fromString(
					"(IF (AND A0 A1) D3 (IF A0 D1 (IF A1 D2 D0)))", config);
			ind.setId(0);
			gp.getPopulation().add(ind);
		}

		// Now evaluate the population (This will call fitness() as needed).
		gp.evaluatePop();

		// Now make sure that ALL the individuals are assigned perfect fitness.
		for (Individual ind : gp.getPopulation()) {
			assertTrue(ind.getFitness() == 1.0);
		}
	}

	/**
	 * Tests that the threaded fitness() evaluates individuals correctly in the
	 * layered GP's evaluatePop()!
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testLayeredGPThreadedFitness()
			throws CloneNotSupportedException {
		// Add multiple threads
		context.getConfig().setNumThreads(4);

		layeredGP.init();

		// Clear out the population so we can create it as we see fit.
		layeredGP.getLayeredPopulation().clear();

		// Fill the population with 10 layers of perfect-fitness individuals.
		for (int i = 0; i < 10; i++) {
			Vector<Individual> layer = new Vector<Individual>();
			layeredGP.getLayeredPopulation().add(layer);

			for (int j = 0; j < 50; j++) {
				Individual ind = Individual.fromString(
						"(IF (AND A0 A1) D3 (IF A0 D1 (IF A1 D2 D0)))", config);
				ind.setId(0);
				layeredGP.getLayeredPopulation().get(i).add(ind);
			}
		}

		// Now evaluate the population (This will call fitness() as needed).
		layeredGP.evaluatePop();

		// Now make sure that ALL the individuals are assigned perfect fitness.
		for (int i = 0; i < 10; i++) {
			for (Individual ind : layeredGP.getLayeredPopulation().get(i)) {
				assertTrue(ind.getFitness() == 1.0);
			}
		}
	}

	/**
	 * Tests that the threaded fitness() evaluates individuals correctly in the
	 * layered GP's evaluateLayer()!
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testLayeredGPThreadedLayerFitness()
			throws CloneNotSupportedException {
		// Add multiple threads
		context.getConfig().setNumThreads(4);

		layeredGP.init();

		// Fill a layer with perfect-fitness individuals.
		Vector<Individual> layer = new Vector<Individual>();

		for (int j = 0; j < 50; j++) {
			Individual ind = Individual.fromString(
					"(IF (AND A0 A1) D3 (IF A0 D1 (IF A1 D2 D0)))", config);
			ind.setId(0);
			layer.add(ind);
		}

		// Now evaluate the layer (This will call fitness() as needed).
		layeredGP.evaluateLayer(layer);

		// Now make sure that ALL the individuals are assigned perfect fitness.
		for (Individual ind : layer) {
			assertTrue(ind.getFitness() == 1.0);
		}
	}

	/**
	 * Tests that the threaded fitness() evaluates individuals correctly in the
	 * pareto GP!
	 * 
	 * @throws Exception
	 */
	@Test
	public void testParetoGPThreadedFitness() throws Exception {
		// Add multiple threads
		context.getConfig().setNumThreads(4);

		paretoGP.init();

		// Clear out the population so we can create it as we see fit.
		paretoGP.getPopulation().clear();

		// Fill the population with perfect-fitness individuals.
		for (int i = 0; i < context.getConfig().getPopSize(); i++) {
			Individual ind = Individual.fromString(
					"(IF (AND A0 A1) D3 (IF A0 D1 (IF A1 D2 D0)))", config);
			ind.setId(0);
			paretoGP.getPopulation().add(ind);
		}

		// Now evaluate the population (This will call fitness() as needed).
		paretoGP.evaluatePop();

		// Now make sure that ALL the individuals are assigned perfect fitness.
		for (Individual ind : paretoGP.getPopulation()) {
			assertTrue(ind.getFitness() == 1.0);
		}
	}

	/**
	 * Tests that the threaded fitness() evaluates individuals correctly in the
	 * pareto GP's evaluateOffspring()!
	 * 
	 * @throws Exception
	 */
	@Test
	public void testParetoGPThreadedFitnessOffspring() throws Exception {
		// Add multiple threads
		context.getConfig().setNumThreads(4);

		paretoGP.init();

		// Fill a vector with perfect-fitness individuals.
		Vector<Individual> inds = new Vector<Individual>();

		for (int i = 0; i < context.getConfig().getPopSize(); i++) {
			Individual ind = Individual.fromString(
					"(IF (AND A0 A1) D3 (IF A0 D1 (IF A1 D2 D0)))", config);
			ind.setId(0);
			inds.add(ind);
		}

		// Now evaluate the vector (This will call fitness() as needed).
		paretoGP.evaluateOffspring(inds);

		// Now make sure that ALL the individuals are assigned perfect fitness.
		for (Individual ind : inds) {
			assertTrue(ind.getFitness() == 1.0);
		}
	}
}
