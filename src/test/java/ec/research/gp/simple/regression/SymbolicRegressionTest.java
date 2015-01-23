package ec.research.gp.simple.regression;

import static org.junit.Assert.assertTrue;

import java.util.Vector;

import org.apache.log4j.PropertyConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;

import ec.research.gp.layers.LayeredGP;
import ec.research.gp.pareto.ParetoGP;
import ec.research.gp.simple.gp.GP;
import ec.research.gp.simple.problem.ProblemRunner;
import ec.research.gp.simple.regression.SymbolicRegression;
import ec.research.gp.simple.regression.nodes.ADD;
import ec.research.gp.simple.regression.nodes.MULT;
import ec.research.gp.simple.regression.nodes.X;
import ec.research.gp.simple.representation.Individual;
import ec.research.gp.simple.util.Config;
import ec.research.gp.simple.util.Context;



/**
 * Tests the SymbolicRegression problem class for functionality.
 * 
 */
public class SymbolicRegressionTest {
	private static SymbolicRegression symbolicRegression;

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

		// Now setup the symbolic regression problem
		symbolicRegression = new SymbolicRegression();
		config = new Config("src/test/resources/regression.properties");
		context = new Context(config);
		symbolicRegression.init(context);

		// Make sure config points to the test output directory
		config.setOutputDir("testOutput");

		// Setup the GPs
		gp = new GP(context);
		layeredGP = new LayeredGP(context);
		paretoGP = new ParetoGP(context);
	}

	/**
	 * Makes sure a hand-crafted solution is evaluated correctly.
	 */
	@Test
	public void testIdealSolution() {
		Individual soln = new Individual();

		// Create the tree manually. (4x^4 + 3x^3 + 2x^2 + x)
		// fromString() sure would be nice here!
		ADD add1 = new ADD();
		ADD add2 = new ADD();
		ADD add3 = new ADD();
		ADD add4 = new ADD();
		ADD add5 = new ADD();
		ADD add6 = new ADD();
		ADD add7 = new ADD();
		ADD add8 = new ADD();
		ADD add9 = new ADD();

		MULT mult1 = new MULT();
		MULT mult2 = new MULT();
		MULT mult3 = new MULT();
		MULT mult4 = new MULT();
		MULT mult5 = new MULT();
		MULT mult6 = new MULT();

		X x1 = new X();
		X x2 = new X();
		X x3 = new X();
		X x4 = new X();
		X x5 = new X();
		X x6 = new X();
		X x7 = new X();
		X x8 = new X();
		X x9 = new X();
		X x10 = new X();
		X x11 = new X();
		X x12 = new X();
		X x13 = new X();
		X x14 = new X();
		X x15 = new X();
		X x16 = new X();

		add1.setChild(0, add2);
		add1.setChild(1, add3);

		add2.setChild(0, mult1);
		add2.setChild(1, mult2);

		add3.setChild(0, mult3);
		add3.setChild(1, x2);

		mult1.setChild(0, mult4);
		mult1.setChild(1, mult5);

		mult2.setChild(0, mult6);
		mult2.setChild(1, x7);

		mult3.setChild(0, add8);
		mult3.setChild(1, x1);

		mult4.setChild(0, add4);
		mult4.setChild(1, x3);

		mult5.setChild(0, x4);
		mult5.setChild(1, x5);

		mult6.setChild(0, add7);
		mult6.setChild(1, x6);

		add4.setChild(0, add5);
		add4.setChild(1, add6);

		add5.setChild(0, x8);
		add5.setChild(1, x9);

		add6.setChild(0, x10);
		add6.setChild(1, x11);

		add7.setChild(0, add9);
		add7.setChild(1, x14);

		add8.setChild(0, x15);
		add8.setChild(1, x16);

		add9.setChild(0, x12);
		add9.setChild(1, x13);

		// Simple x4 + x3 + x2 + x below...
		// add1.setChild(0, x1);
		// add1.setChild(1, mult1);
		//
		// mult1.setChild(0, add2);
		// mult1.setChild(1, x2);
		//
		// add2.setChild(0, x3);
		// add2.setChild(1, mult2);
		//
		// mult2.setChild(0, add3);
		// mult2.setChild(1, x4);
		//
		// add3.setChild(0, x5);
		// add3.setChild(1, mult3);
		//
		// mult3.setChild(0, x6);
		// mult3.setChild(1, x7);

		soln.setRoot(add1);

		// Now evaluate the individual and check its fitness and hits.
		symbolicRegression.fitness(soln);
		assertTrue(soln.getFitness() == 1.0 && soln.getHits() == 20);
	}

	/**
	 * Tests that an incorrect solution doesn't get perfect fitness.
	 */
	@Test
	public void testIncorrectSolution() {
		Individual soln = new Individual();

		// Create the simple tree (+ x x)
		ADD add = new ADD();
		X x1 = new X();
		X x2 = new X();

		add.setChild(0, x1);
		add.setChild(1, x2);

		soln.setRoot(add);

		// Now evaluate the individual and check its fitness and hits.
		symbolicRegression.fitness(soln);
		assertTrue(soln.getFitness() < 1.0 && soln.getHits() < 20);
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
			Individual ind = Individual
					.fromString(
							"(+ (+ (* (* (+ (+ x x) (+ x x)) x) (* x x)) (* (* (+ (+ x x) x) x) x)) (+ (* (+ x x) x) x))",
							config);
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
				Individual ind = Individual
						.fromString(
								"(+ (+ (* (* (+ (+ x x) (+ x x)) x) (* x x)) (* (* (+ (+ x x) x) x) x)) (+ (* (+ x x) x) x))",
								config);
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
			Individual ind = Individual
					.fromString(
							"(+ (+ (* (* (+ (+ x x) (+ x x)) x) (* x x)) (* (* (+ (+ x x) x) x) x)) (+ (* (+ x x) x) x))",
							config);
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
			Individual ind = Individual
					.fromString(
							"(+ (+ (* (* (+ (+ x x) (+ x x)) x) (* x x)) (* (* (+ (+ x x) x) x) x)) (+ (* (+ x x) x) x))",
							config);
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
			Individual ind = Individual
					.fromString(
							"(+ (+ (* (* (+ (+ x x) (+ x x)) x) (* x x)) (* (* (+ (+ x x) x) x) x)) (+ (* (+ x x) x) x))",
							config);
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
