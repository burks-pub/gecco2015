package ec.research.gp.pareto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;


import org.apache.log4j.PropertyConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;

import ec.research.gp.pareto.ParetoGP;
import ec.research.gp.pareto.ParetoOperators;
import ec.research.gp.pareto.ParetoGP.OBJECTIVES;
import ec.research.gp.simple.problem.ProblemRunner;
import ec.research.gp.simple.representation.Individual;
import ec.research.gp.simple.util.Config;
import ec.research.gp.simple.util.Context;


/**
 * Tests the {@link ParetoOperators} for functionality.
 * 
 */
public class ParetoOperatorsTest {
	private static Context context;

	/*
	 * Individuals for making a pareto front smaller than the target population
	 * size. All individuals aren't actually non-dominated here. This is just an
	 * easy way to make it work. The first for each tag is non-dominated.
	 */
	private static final Object[][] SMALL_FRONT = { { 1.0, "A" }, { 0.4, "A" },
			{ 0.3, "A" }, { 0.2, "A" }, { 0.1, "A" }, { 0.09, "B" },
			{ 0.08, "B" }, { 0.07, "B" }, { 0.06, "C" }, { 0.05, "C" },
			{ 0.04, "D" } };

	// Pareto front whose size is equal to the target population size
	private static final Object[][] EQUAL_FRONT = { { 1.0, "A" }, { 1.0, "A" },
			{ 1.0, "A" }, { 1.0, "A" }, { 0.5, "B" }, { 0.5, "B" },
			{ 0.5, "B" }, { 0.2, "C" }, { 0.2, "C" }, { 0.1, "D" } };

	// Large pareto front (larger than target pop size)
	private static final Object[][] LARGE_FRONT = { { 1.0, "A" }, { 1.0, "A" },
			{ 1.0, "A" }, { 1.0, "A" }, { 0.5, "B" }, { 0.5, "B" },
			{ 0.5, "B" }, { 0.2, "C" }, { 0.2, "C" }, { 0.1, "D" },
			{ 0.1, "E" }, { 0.1, "F" } };

	// Pareto front on the age/fitness objectives.
	private static final Object[][] AF_FRONT = { { 1.0, 10, "A" },
			{ 0.9, 9, "A" }, { 0.8, 8, "A" }, { 0.7, 7, "B" }, { 0.6, 6, "B" },
			{ 0.5, 5, "B" }, { 0.4, 4, "C" }, { 0.3, 3, "D" }, { 0.2, 2, "D" },
			{ 0.1, 1, "E" } };

	@BeforeClass
	public static void setup() throws FileNotFoundException, IOException {
		// Make log4j be quiet!
		PropertyConfigurator.configure("log4j.properties.unittest");

		// Setup the output directory so the test won't fail if it was deleted.
		ProblemRunner.checkDirs("testOutput");

		Config config = new Config(
				"src/test/resources/paretoGPRegression.properties");

		// Make sure config points to the test output directory
		config.setOutputDir("testOutput");

		context = new Context(config);
	}

	/**
	 * Tests that the global pareto front is created correctly for the small
	 * test front.
	 */
	@Test
	public void testGlobalFrontSmall() {
		Vector<Individual> population = new Vector<Individual>();

		int size = 10;
		for (Object[] items : SMALL_FRONT) {
			Individual ind = new Individual();
			ind.setId(0);

			ind.setFitness((Double) items[0]);
			ind.setTag((String) items[1]);
			ind.setNumNodes(size);
			size++;

			population.add(ind);
		}

		Set<Individual> paretoFront = ParetoOperators.getGlobalNonDominatedFront(
				context, population, ParetoGP.OBJECTIVES.AGE_DENSITY_FITNESS);

		assertEquals(4, paretoFront.size());
	}

	/**
	 * Tests that the global pareto front is correct using Age/Fitness as the
	 * objectives.
	 */
	@Test
	public void testGlobalFrontAF() {
		Vector<Individual> population = new Vector<Individual>();

		for (Object[] items : AF_FRONT) {
			Individual ind = new Individual();
			ind.setId(0);

			ind.setFitness((Double) items[0]);
			ind.setAge((Integer) items[1]);
			ind.setTag((String) items[2]);
			population.add(ind);
		}

		// Now fill the population with dominated individuals.
		for (int i = 0; i < 20; i++) {
			Individual ind = new Individual();
			ind.setFitness(0);
			ind.setAge(100);
			ind.setTag("A");
			ind.setId(0);

			population.add(ind);
		}

		Set<Individual> paretoFront = ParetoOperators.getGlobalNonDominatedFront(
				context, population, ParetoGP.OBJECTIVES.AGE_FITNESS);

		assertEquals(10, paretoFront.size());
	}

	/**
	 * Tests that the global pareto front is created correctly for the large
	 * test front.
	 */
	@Test
	public void testGlobalFrontLarge() {
		Vector<Individual> population = new Vector<Individual>();

		int size = 10;
		for (Object[] items : LARGE_FRONT) {
			Individual ind = new Individual();
			ind.setId(0);

			ind.setFitness((Double) items[0]);
			ind.setTag((String) items[1]);
			ind.setNumNodes(size);
			size++;

			population.add(ind);
		}

		Set<Individual> paretoFront = ParetoOperators.getGlobalNonDominatedFront(
				context, population, ParetoGP.OBJECTIVES.AGE_DENSITY_FITNESS);

		assertEquals(4, paretoFront.size());
	}

	/**
	 * Tests that the population shrinks to (less than or equal to -- as in
	 * Lipson's approach)the target population size when the pareto front is
	 * smaller than the population size.
	 */
	@Test
	public void testParetoFrontSmaller() {
		Vector<Individual> population = new Vector<Individual>();

		// Manually set the target pop size to easily make the numbers work.
		int targetSize = 10;
		context.getConfig().setPopSize(targetSize);

		for (int i = 0; i < SMALL_FRONT.length; i++) {
			Individual ind = new Individual();
			ind.setId(0);

			ind.setFitness((Double) SMALL_FRONT[i][0]);
			ind.setTag((String) SMALL_FRONT[i][1]);

			population.add(ind);
		}

		// Now fill the population up to 2x the target size with dominated
		// individuals.
		for (int i = 0; i < (targetSize * 2) - SMALL_FRONT.length; i++) {
			Individual ind = new Individual();
			ind.setId(0);
			ind.setFitness(0);
			ind.setTag("ZZZ");

			population.add(ind);
		}
		// Now run the ParetoTournamentSelection.
		ParetoOperators.delete(context, population,
				ParetoGP.OBJECTIVES.AGE_DENSITY_FITNESS);

		// Now make sure the population made it to the target population size.
		assertTrue(population.size() <= targetSize);
	}

	/**
	 * Tests that the population goes to the size of the pareto front, which
	 * happens to be equal to the target population size.
	 */
	@Test
	public void testParetoFrontEqual() {
		Vector<Individual> population = new Vector<Individual>();

		// Manually set the target pop size to easily make the numbers work.
		int targetSize = 4;
		context.getConfig().setPopSize(targetSize);

		for (int i = 0; i < EQUAL_FRONT.length; i++) {
			Individual ind = new Individual();
			ind.setId(0);

			ind.setFitness((Double) EQUAL_FRONT[i][0]);
			ind.setTag((String) EQUAL_FRONT[i][1]);

			population.add(ind);
		}

		// Now fill the population with dominated individuals.
		for (int i = 0; i < 20 - EQUAL_FRONT.length; i++) {
			Individual ind = new Individual();
			ind.setFitness(0);
			ind.setTag("ZZZ");
			ind.setId(0);

			population.add(ind);
		}

		// Now run the ParetoTournamentSelection.
		ParetoOperators.delete(context, population,
				ParetoGP.OBJECTIVES.AGE_DENSITY_FITNESS);

		// Now make sure the population made it to the target population size.
		assertEquals(targetSize, population.size());
	}

	/**
	 * Tests that the individual is actually non-dominated when it is better in
	 * fitness and density.
	 */
	@Test
	public void testNotDominatedBothBetter() {
		HashMap<String, Double> densities = new HashMap<String, Double>();
		// Make some test individuals.
		Individual ind1 = new Individual();
		Individual ind2 = new Individual();

		// Make individual 1 more fit than individual 2
		ind1.setFitness(1.0);
		ind1.setTag("A");

		ind2.setFitness(0.2);
		ind2.setTag("B");

		// Make individual 1's marker density less than individual 2
		densities.put("A", 0.2);
		densities.put("B", 0.8);

		assertEquals(false, ParetoOperators.individualIsDominated(ind1, ind2,
				densities, context, ParetoGP.OBJECTIVES.AGE_DENSITY_FITNESS));
	}

	/**
	 * Tests that the individual is actually non-dominated when it is equal in
	 * all objectives, and has fewer nodes.
	 * 
	 */
	@Test
	public void testNotDominatedEqual() {
		HashMap<String, Double> densities = new HashMap<String, Double>();
		// Make some test individuals.
		Individual ind1 = new Individual();
		Individual ind2 = new Individual();

		// Make them equal in fitness
		ind1.setFitness(1.0);
		ind1.setTag("A");

		ind2.setFitness(1.0);
		ind2.setTag("A");

		// Make ind1 smaller than ind2
		ind1.setNumNodes(5);
		ind2.setNumNodes(6);

		// Make individual 1's marker density the same as individual 2
		densities.put("A", 0.2);

		assertEquals(false, ParetoOperators.individualIsDominated(ind1, ind2,
				densities, context, ParetoGP.OBJECTIVES.AGE_DENSITY_FITNESS));
	}

	/**
	 * Tests that the individual is actually dominated when it is worse in both
	 * fitness and density.
	 */
	@Test
	public void testIsDominatedBothWorse() {
		HashMap<String, Double> densities = new HashMap<String, Double>();
		// Make some test individuals.
		Individual ind1 = new Individual();
		Individual ind2 = new Individual();

		// Make individual 1 less fit than individual 2
		ind1.setFitness(0.2);
		ind1.setTag("A");

		ind2.setFitness(1.0);
		ind2.setTag("B");

		// Make individual 1's marker density worse than individual 2
		densities.put("A", 0.8);
		densities.put("B", 0.2);

		assertEquals(true, ParetoOperators.individualIsDominated(ind1, ind2,
				densities, context, ParetoGP.OBJECTIVES.AGE_DENSITY_FITNESS));
	}

	/**
	 * Tests that the individual is not dominated when it is better in fitness
	 * but worse in density.
	 */
	@Test
	public void testNotDominatedBetterFitness() {
		HashMap<String, Double> densities = new HashMap<String, Double>();
		// Make some test individuals.
		Individual ind1 = new Individual();
		Individual ind2 = new Individual();

		// Make individual 1 more fit than individual 2
		ind1.setFitness(1.0);
		ind1.setTag("A");

		ind2.setFitness(0.2);
		ind2.setTag("B");

		// Make individual 1's marker density worse than individual 2
		densities.put("A", 0.8);
		densities.put("B", 0.2);

		assertEquals(false, ParetoOperators.individualIsDominated(ind1, ind2,
				densities, context, ParetoGP.OBJECTIVES.AGE_DENSITY_FITNESS));
	}

	/**
	 * Tests that the individual is not dominated when it is worse in fitness
	 * but better in density.
	 */
	@Test
	public void testNotDominatedBetterDensity() {
		HashMap<String, Double> densities = new HashMap<String, Double>();
		// Make some test individuals.
		Individual ind1 = new Individual();
		Individual ind2 = new Individual();

		// Make individual 1 less fit than individual 2
		ind1.setFitness(0.2);
		ind1.setTag("A");

		ind2.setFitness(1.0);
		ind2.setTag("B");

		// Make individual 1's marker density less than individual 2
		densities.put("A", 0.2);
		densities.put("B", 0.8);

		assertEquals(false, ParetoOperators.individualIsDominated(ind1, ind2,
				densities, context, ParetoGP.OBJECTIVES.AGE_DENSITY_FITNESS));
	}

	/**
	 * Tests the case where the first individual is better in all three
	 * objectives.
	 */
	@Test
	public void testAllBetter() {
		HashMap<String, Double> densities = new HashMap<String, Double>();
		// Make some test individuals.
		Individual ind1 = new Individual();
		Individual ind2 = new Individual();

		// Make individual 1 more fit than individual 2
		ind1.setFitness(1.0);
		ind1.setTag("A");

		ind2.setFitness(0.2);
		ind2.setTag("B");

		// Make individual 1's marker density less than individual 2
		densities.put("A", 0.2);
		densities.put("B", 0.8);

		// Make individual 1 the youngest
		ind1.setAge(1);
		ind2.setAge(3);

		assertEquals(false, ParetoOperators.individualIsDominated(ind1, ind2,
				densities, context, ParetoGP.OBJECTIVES.AGE_DENSITY_FITNESS));
	}

	/**
	 * Tests the case where the individual is worse on all objectives.
	 */
	@Test
	public void testAllWorse() {
		HashMap<String, Double> densities = new HashMap<String, Double>();
		// Make some test individuals.
		Individual ind1 = new Individual();
		Individual ind2 = new Individual();

		// Make individual 1 less fit than individual 2
		ind1.setFitness(0.2);
		ind1.setTag("A");

		ind2.setFitness(1.0);
		ind2.setTag("B");

		// Make individual 1's marker density greater than individual 2
		densities.put("A", 0.8);
		densities.put("B", 0.2);

		// Make individual 1 the oldest
		ind1.setAge(3);
		ind2.setAge(1);

		assertEquals(true, ParetoOperators.individualIsDominated(ind1, ind2,
				densities, context, ParetoGP.OBJECTIVES.AGE_DENSITY_FITNESS));
	}

	/**
	 * Tests the case where the individual is better in age only.
	 */
	@Test
	public void testAgeBetter() {
		HashMap<String, Double> densities = new HashMap<String, Double>();
		// Make some test individuals.
		Individual ind1 = new Individual();
		Individual ind2 = new Individual();

		// Make individual 1 less fit than individual 2
		ind1.setFitness(0.2);
		ind1.setTag("A");

		ind2.setFitness(1.0);
		ind2.setTag("B");

		// Make individual 1's marker density greater than individual 2
		densities.put("A", 0.8);
		densities.put("B", 0.2);

		// Make individual 1 the youngest
		ind1.setAge(1);
		ind2.setAge(3);

		assertEquals(false, ParetoOperators.individualIsDominated(ind1, ind2,
				densities, context, ParetoGP.OBJECTIVES.AGE_DENSITY_FITNESS));
	}

	/**
	 * Tests age/fitness objectives when age is equal and fitness is worse.
	 */
	@Test
	public void testAFAgeEqualFitnessWorse() {
		// Make some test individuals.
		Individual ind1 = new Individual();
		Individual ind2 = new Individual();
		HashMap<String, Double> densities = new HashMap<String, Double>();

		// Fake the densities.
		ind1.setTag("A");
		ind2.setTag("B");

		densities.put("A", 0.3);
		densities.put("B", 0.3);

		// Make individual 1 less fit than individual 2
		ind1.setFitness(0.2);

		ind2.setFitness(1.0);

		// Make individual 1 the youngest
		ind1.setAge(7);
		ind2.setAge(7);

		assertEquals(true, ParetoOperators.individualIsDominated(ind1, ind2,
				densities, context, OBJECTIVES.AGE_FITNESS));
	}

	/**
	 * Tests the Age/Fitness pareto objectives on a problematic case taken from
	 * a real run.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testAFProblematic() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(
				"src/test/resources/afPareto.txt"));
		Vector<Individual> population = new Vector<Individual>();
		context.getConfig().setPopSize(256);

		String line = null;
		while ((line = reader.readLine()) != null) {
			String items[] = line.split("\t");
			Individual ind = new Individual();
			ind.setId(0);

			ind.setTag(items[0].trim());
			ind.setAge(Integer.parseInt(items[1]));
			ind.setFitness(Double.parseDouble(items[2]));
			population.add(ind);
		}
		reader.close();

		Set<Individual> paretoFront = ParetoOperators.delete(context,
				population, OBJECTIVES.AGE_FITNESS);

		assertTrue(paretoFront.size() < population.size());
	}

	/**
	 * Tests the age/fitness objectives when age is equal but fitness is better
	 */
	@Test
	public void testAFAgeEqualFitnessBetter() {
		// Make some test individuals.
		Individual ind1 = new Individual();
		Individual ind2 = new Individual();
		HashMap<String, Double> densities = new HashMap<String, Double>();

		// Fake the densities.
		ind1.setTag("A");
		ind2.setTag("B");

		densities.put("A", 0.3);
		densities.put("B", 0.3);

		// Make individual 1 less fit than individual 2
		ind1.setFitness(1.0);

		ind2.setFitness(0.2);

		// Make individual 1 the youngest
		ind1.setAge(7);
		ind2.setAge(7);

		assertEquals(false, ParetoOperators.individualIsDominated(ind1, ind2,
				densities, context, OBJECTIVES.AGE_FITNESS));
	}

	/**
	 * Tests the case where the individual is worse in age and better in fitness
	 * and density.
	 */
	@Test
	public void testAgeWorse() {
		HashMap<String, Double> densities = new HashMap<String, Double>();
		// Make some test individuals.
		Individual ind1 = new Individual();
		Individual ind2 = new Individual();

		// Make individual 1 less fit than individual 2
		ind1.setFitness(1.0);
		ind1.setTag("A");

		ind2.setFitness(0.2);
		ind2.setTag("B");

		// Make individual 1's marker density less than individual 2
		densities.put("A", 0.2);
		densities.put("B", 0.8);

		// Make individual 1 the oldest
		ind1.setAge(3);
		ind2.setAge(1);

		assertEquals(false, ParetoOperators.individualIsDominated(ind1, ind2,
				densities, context, ParetoGP.OBJECTIVES.AGE_DENSITY_FITNESS));
	}
}
