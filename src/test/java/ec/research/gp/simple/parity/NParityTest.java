package ec.research.gp.simple.parity;

import static org.junit.Assert.assertTrue;

import java.util.Vector;

import org.apache.log4j.PropertyConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;

import ec.research.gp.layers.LayeredGP;
import ec.research.gp.pareto.ParetoGP;
import ec.research.gp.simple.gp.GP;
import ec.research.gp.simple.parity.NParity;
import ec.research.gp.simple.problem.ProblemRunner;
import ec.research.gp.simple.representation.Individual;
import ec.research.gp.simple.util.Config;
import ec.research.gp.simple.util.Context;



public class NParityTest {
	private static Config config;
	private static Context context;
	private static NParity nParity;
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
		nParity = new NParity();
		config = new Config("src/test/resources/paretoNParity.properties");
		
		// Make sure config points to the test output directory
		config.setOutputDir("testOutput");
		
		context = new Context(config);
		nParity.init(context);

		// Setup the GPs
		gp = new GP(context);
		layeredGP = new LayeredGP(context);
		paretoGP = new ParetoGP(context);
	}

	/**
	 * Tests that a correct solution to the 2-parity problem gets the ideal
	 * fitness. We use the same solution as in de Jong et al 2001.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testCorrectSolution() throws CloneNotSupportedException {
		Individual ind = Individual.fromString("(OR (AND D0 D1) (NOR D0 D1))",
				config);// new Individual();

		// Evaluate the individual and check its fitness.
		nParity.fitness(ind);

		assertTrue(ind.getFitness() == 1.0);
	}

	/**
	 * Tests that an incorrect solution doesn't get perfect fitness. With n=2
	 * and always choosing the first bit, fitness is 2/4.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testIncorrectSolution() throws CloneNotSupportedException {
		// Create a single-node (DO) individual.
		Individual ind = Individual.fromString("D0", config);
		// Evaluate the individual and check its fitness.
		nParity.fitness(ind);

		assertTrue(ind.getFitness() == 0.50);
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
					"(OR (AND D0 D1) (NOR D0 D1))", config);
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
						"(OR (AND D0 D1) (NOR D0 D1))", config);
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
					"(OR (AND D0 D1) (NOR D0 D1))", config);
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
					"(OR (AND D0 D1) (NOR D0 D1))", config);
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
					"(OR (AND D0 D1) (NOR D0 D1))", config);
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
