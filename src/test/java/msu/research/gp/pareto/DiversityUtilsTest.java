package msu.research.gp.pareto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import msu.research.gp.pareto.DiversityUtils;
import msu.research.gp.simple.problem.ProblemRunner;
import msu.research.gp.simple.representation.Individual;
import msu.research.gp.simple.util.Config;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the {@link DiversityUtils} class for functionality.
 * 
 * @author Armand R. Burks
 * 
 */
public class DiversityUtilsTest {
	private static DiversityUtils diversityUtils;
	private static Config config;

	@BeforeClass
	public static void setup() throws Exception {
		// Make log4j be quiet!
		PropertyConfigurator.configure("log4j.properties");

		// Setup the output directory so the test won't fail if it was deleted.
		ProblemRunner.checkDirs("testOutput");

		// Setup the config
		config = new Config("src/test/resources/paretoGPRegression.properties");

		// Make sure config points to the test output directory
		config.setOutputDir("testOutput");

		// Setup the output directory so the test won't fail if it was deleted.
		ProblemRunner.checkDirs("testOutput");

		// Setup the diversity utils object
		diversityUtils = new DiversityUtils(config);
	}

	/**
	 * Make sure our objects are in a "neutral" state before each test runs.
	 */
	@Before
	public void setupTest() {
		diversityUtils.setTagDepth(0);
		diversityUtils.setTagLevel(0);
	}

	/**
	 * Tests that the individual gets the correct tag, when we only use the root
	 * node for the tag.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testTagIndividualRootOnly() throws CloneNotSupportedException {
		// Create the tree: (+ (* x x) x)
		Individual ind = Individual.fromString("(+ (* x x) x)", config);

		// Setup the expected output for 3 nodes
		String expected = "+";

		// Manually set the tag depth
		diversityUtils.setTagDepth(0);

		// Tag the individual
		diversityUtils.tagIndividual(ind);

		assertEquals(expected, ind.getTag());

	}

	/**
	 * Tests that the individual gets the correct tag, when we don't use the
	 * whole tree for the tag.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testTagIndividualPartialTree()
			throws CloneNotSupportedException {
		// Create the tree: (+ (* x x) x)
		Individual ind = Individual.fromString("(+ (* x x) x)", config);

		// Setup the expected output for depth 1
		String expected = "(+ (*) x)";

		// Manually set the tag depth
		diversityUtils.setTagDepth(1);

		// Tag the individual
		diversityUtils.tagIndividual(ind);

		assertEquals(expected, ind.getTag());

	}

	/**
	 * Tests that the individual gets the correct tag, when we use all the last
	 * level of the tree for the tag.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testTagIndividualAllButOne() throws CloneNotSupportedException {
		// Create the tree: (+ (* x (+ x x)) x)
		Individual ind = Individual.fromString("(+ (* x (+ x x)) x)", config);

		// Setup the expected output for 3 nodes
		String expected = "(+ (* x (+)) x)";

		// Manually set the tag depth
		diversityUtils.setTagDepth(2);

		// Tag the individual
		diversityUtils.tagIndividual(ind);

		assertEquals(expected, ind.getTag());
	}

	/**
	 * Tests that the individual gets the correct tag, when we use the whole
	 * tree for the tag.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testTagIndividualWholeTree() throws CloneNotSupportedException {
		// Create the tree: (+ (* x x) x)
		Individual ind = Individual.fromString("(+ (* x x) x)", config);

		// Setup the expected output for the whole tree (all 5 nodes)
		String expected = "(+ (* x x) x)";

		// Manually set the tag depth
		diversityUtils.setTagDepth(2);

		// Tag the individual
		diversityUtils.tagIndividual(ind);

		assertEquals(expected, ind.getTag());
	}

	/**
	 * Tests that the individual gets the correct tag, when we don't use the
	 * whole tree for the tag, but the tree is bigger than in the other tests.
	 * This is really to make sure that intermediate (full) trees get the
	 * correct parentheses.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testTagIndividualBiggerTree() throws CloneNotSupportedException {
		// Create the tree: (+ (* x (+ x x)) x)
		Individual ind = Individual.fromString("(+ (* x x) x)", config);

		// Setup the expected output for 3 nodes
		String expected = "(+ (*) x)";

		// Manually set the tag depth
		diversityUtils.setTagDepth(1);

		// Tag the individual
		diversityUtils.tagIndividual(ind);

		assertEquals(expected, ind.getTag());
	}

	/**
	 * Tests that the tagging method works when we start at level 1 (instead of
	 * the root) and go down 1 level. The expected output is an ordered
	 * (left-to-right) list of fragments from level one down to level 2,
	 * excluding the root.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testTagIndividualLevel1() throws CloneNotSupportedException {
		// Create the tree: (+ (* x (+ x x)) x)
		Individual ind = Individual.fromString("(+ (* x x) x)", config);

		// Setup the expeted output for tag level 1 and tag depth 1
		String expected = "(* x x),x";

		// Manually set the tag level
		diversityUtils.setTagLevel(1);

		// Manually set the tag depth
		diversityUtils.setTagDepth(1);

		// Tag the individual
		diversityUtils.tagIndividual(ind);

		assertEquals(expected, ind.getTag());
	}

	/**
	 * Tests the weird case where the tag depth is 0 and the tag level is > 0.
	 * This means that we should only collect the nodes at that level (depth),
	 * in left-to-right order. Not sure that we'd ever do this, but it does test
	 * the method more fully.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testTagIndividualLevel2Depth0()
			throws CloneNotSupportedException {
		// Create the tree: (+ (* x (+ x x)) x)
		Individual ind = Individual.fromString("(+ (* x x) x)", config);

		// Setup the expeted output for tag level 2 and tag depth 0
		String expected = "x,x";

		// Manually set the tag level
		diversityUtils.setTagLevel(2);

		// Manually set the tag depth
		diversityUtils.setTagDepth(0);

		// Tag the individual
		diversityUtils.tagIndividual(ind);

		assertEquals(expected, ind.getTag());
	}

	/**
	 * Tests that the tag comes out right when the tag level is below the root
	 * and the tag depth is only 0, but the nodes at the tag level are all
	 * functions instead of terminal nodes. Since they are functions, they will
	 * be enclosed in parenthesis
	 */
	@Test
	public void testTagIndividualLevel1Depth0Func()
			throws CloneNotSupportedException {
		// Create the tree: (+ (+ x x) (+ x x ))
		Individual ind = Individual.fromString("(+ (+ x x) (+ x x))", config);

		// Setup the expeted output for tag level 1 and tag depth 0
		String expected = "(+),(+)";

		// Manually set the tag level
		diversityUtils.setTagLevel(1);

		// Manually set the tag depth
		diversityUtils.setTagDepth(0);

		// Tag the individual
		diversityUtils.tagIndividual(ind);

		assertEquals(expected, ind.getTag());
	}

	/**
	 * Tests that the tag comes out as expected for a bigger tree when the tag
	 * level is deeper than the root.
	 */
	@Test
	public void testTagIndividualBigLevel2() throws CloneNotSupportedException {
		// Create the tree: (+ (+ (* x x) x) (+ (SIN x) x))
		Individual ind = Individual.fromString(
				"(+ (+ (* x x) x) (+ (SIN x) x))", config);

		// Setup the expeted output for tag level 2 and tag depth 0
		String expected = "(* x x),x,(SIN x),x";

		// Manually set the tag level
		diversityUtils.setTagLevel(2);

		// Manually set the tag depth
		diversityUtils.setTagDepth(1);

		// Tag the individual
		diversityUtils.tagIndividual(ind);

		assertEquals(expected, ind.getTag());
	}

	/**
	 * Tests that the tag comes out right when all nodes at the tag level are
	 * leaves. Again, probably not something we'd actually do, but it's testing
	 * the logic behind the method.
	 */
	@Test
	public void testTagIndividualLeafLevel() throws CloneNotSupportedException {
		// Create the tree: (+ (+ x x) (+ x x))
		Individual ind = Individual.fromString("(+ (+ x x) (+ x x))", config);

		// Setup the expeted output for tag level 2 and tag depth 0
		String expected = "x,x,x,x";

		// Manually set the tag level
		diversityUtils.setTagLevel(2);

		// Manually set the tag depth
		diversityUtils.setTagDepth(0);

		// Tag the individual
		diversityUtils.tagIndividual(ind);

		assertEquals(expected, ind.getTag());
	}

	/**
	 * Tests that the tag comes out right when the tree is as deep as the tag
	 * level, but the tag depth goes beyond the depth of the tree. In this case,
	 * we should still just end up with the fragments up to the level of the
	 * tree.
	 */
	@Test
	public void testTagIndividualDepthBeyond()
			throws CloneNotSupportedException {
		// Create the tree: (+ (+ x x) (+ x x))
		Individual ind = Individual.fromString("(+ (+ x x) (+ x x))", config);

		// Setup the expeted output for tag level 1 and tag depth 2
		String expected = "(+ x x),(+ x x)";

		// Manually set the tag level
		diversityUtils.setTagLevel(1);

		// Manually set the tag depth
		diversityUtils.setTagDepth(2);

		// Tag the individual
		diversityUtils.tagIndividual(ind);

		assertEquals(expected, ind.getTag());
	}

	/**
	 * Tests that the tagIndividual() method works as expected when the tag
	 * level is deeper than the whole tree. This could actually happen if we've
	 * started tagging a few levels down but we generate a new random individual
	 * whose depth is less than the tag level.
	 * 
	 * Right now, it'll just be an empty string, because how could you otherwise
	 * tag it fairly?
	 */
	@Test
	public void testTagIndividualLevelGreater()
			throws CloneNotSupportedException {
		// Create the tree: (+ (+ x x) (+ x x))
		Individual ind = Individual.fromString("(+ (+ x x) (+ x x))", config);

		// Setup the expected output for tag level 3 and tag depth 2
		String expected = "";

		// Manually set the tag level
		diversityUtils.setTagLevel(3);

		// Manually set the tag depth
		diversityUtils.setTagDepth(2);

		// Tag the individual
		diversityUtils.tagIndividual(ind);

		assertEquals(expected, ind.getTag());
	}

	/**
	 * Tests that the tagDepth method that uses a tagLevel and tagDepth argument
	 * works the same way that the other method does.
	 */
	@Test
	public void testTagIndividualSpecified() throws CloneNotSupportedException {
		// Create the tree: (+ (+ x x) (+ x x))
		Individual ind = Individual.fromString("(+ (+ x x) (+ x x))", config);

		// Setup the expeted output for tag level 1 and tag depth 1
		String expected = "(+ x x),(+ x x)";

		// Tag the individual
		diversityUtils.tagIndividual(ind, 1, 1);

		assertEquals(expected, ind.getTag());
	}

	/**
	 * Tests that the getIndividualTags() method doesn't set individuals' tags
	 * when called
	 */
	@Test
	public void testGetIndividualTagsNotSet() throws CloneNotSupportedException {
		// Setup some individuals with the same genotype to make things easy
		String genotype = "(+ (+ x x) (+ x x))";
		Vector<Individual> inds = new Vector<Individual>();

		for (int i = 0; i < 10; i++) {
			Individual ind = Individual.fromString(genotype, config);
			ind.setId(0);
			inds.add(ind);
		}

		// Just call the getIndividualTags() method. Return value doesn't matter
		diversityUtils.getIndividualTags(inds);

		// Make sure all the individuals' tags are still unset
		for (Individual ind : inds) {
			assertTrue(ind.getTag() == null);
		}
	}

	/**
	 * Tests that the getIndividualTags() method -- the version with the
	 * specified tag level and tag depth -- doesn't set individuals' tags when
	 * called
	 */
	@Test
	public void testGetIndividualTagsSpecifiedNotSet()
			throws CloneNotSupportedException {
		// Setup some individuals with the same genotype to make things easy
		String genotype = "(+ (+ x x) (+ x x))";
		Vector<Individual> inds = new Vector<Individual>();

		for (int i = 0; i < 10; i++) {
			Individual ind = Individual.fromString(genotype, config);
			ind.setId(0);
			inds.add(ind);
		}

		// Just call the getIndividualTags() method. Return value doesn't matter
		diversityUtils.getIndividualTags(inds, 0, 1);

		// Make sure all the individuals' tags are still unset
		for (Individual ind : inds) {
			assertTrue(ind.getTag() == null);
		}
	}

	/**
	 * Tests that the getIndividualTags() method returns the tags as expected
	 */
	@Test
	public void testGetIndividualTagsVals() throws CloneNotSupportedException {
		// Setup some individuals with the same genotype to make things easy
		String genotype = "(+ (+ x x) (+ x x))";
		Vector<Individual> inds = new Vector<Individual>();

		// Setup the expected tag (same for all inds)
		String expectedTag = "(+ (+) (+))";

		for (int i = 0; i < 10; i++) {
			Individual ind = Individual.fromString(genotype, config);
			ind.setId(0);
			inds.add(ind);
		}

		// Manually set the tag depth
		diversityUtils.setTagDepth(1);

		// Get the tags. We don't care about the stats for this test.
		Map<String, double[]> tagStats = diversityUtils.getIndividualTags(inds);

		// Easy. The keyset should only contain one value, the expected tag.
		assertTrue(tagStats.keySet().size() == 1
				&& tagStats.keySet().contains(expectedTag));
	}

	/**
	 * Tests that the getIndividualTags() method -- the version that takes the
	 * specified tag level and tag depth -- returns the tags as expected
	 */
	@Test
	public void testGetIndividualTagsSpecifiedVals()
			throws CloneNotSupportedException {
		// Setup some individuals with the same genotype to make things easy
		String genotype = "(+ (+ x x) (+ x x))";
		Vector<Individual> inds = new Vector<Individual>();

		// Setup the expected tag (same for all inds)
		String expectedTag = "(+ (+) (+))";

		for (int i = 0; i < 10; i++) {
			Individual ind = Individual.fromString(genotype, config);
			ind.setId(0);
			inds.add(ind);
		}

		// Manually set the tag level/depth to make sure it is overridden
		diversityUtils.setTagLevel(300);
		diversityUtils.setTagDepth(300);

		// Get the tags. We don't care about the stats for this test.
		Map<String, double[]> tagStats = diversityUtils.getIndividualTags(inds,
				0, 1);

		// Easy. The keyset should only contain one value, the expected tag.
		assertTrue(tagStats.keySet().size() == 1
				&& tagStats.keySet().contains(expectedTag));
	}

	/**
	 * Tests that the getIndividualTags() method returns the correct number of
	 * density/fitness mappings for the tags we setup.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testGetIndividualTagsSize() throws CloneNotSupportedException {
		// Setup our genotypes and their expected tags
		String genotypes[] = { "(+ (+ x x) (+ x x))", "(+ (+ x x) x)" };
		String expectedTags[] = { "(+ (+) (+))", "(+ (+) x)" };

		// Setup the expected stats to make the assertions easier
		Map<String, double[]> expectedStats = new HashMap<String, double[]>();

		expectedStats.put(expectedTags[0], new double[] { 0.5, 0.5 });
		expectedStats.put(expectedTags[1], new double[] { 0.5, 0.8 });

		// Setup some individuals
		Vector<Individual> inds = new Vector<Individual>();

		// Make half the inds have the first genotype and the rest the second
		for (int i = 0; i < 10; i++) {
			Individual ind = Individual.fromString((i < 5) ? genotypes[0]
					: genotypes[1], config);
			ind.setId(0);
			inds.add(ind);

			// Give the first tag group fitness=0.5 and the second fitness=0.8
			if (i < 5) {
				ind.setFitness(0.5);
			} else {
				ind.setFitness(0.8);
			}
		}

		// Manually set the tag depth
		diversityUtils.setTagDepth(1);

		// Get the tag stats
		Map<String, double[]> tagStats = diversityUtils.getIndividualTags(inds);

		assertTrue(tagStats.size() == expectedStats.size());
	}

	/**
	 * Tests that the getIndividualTags() method -- the version that takes the
	 * specified tag level and tag depth -- returns the correct number of
	 * density/fitness mappings for the tags we setup.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testGetIndividualTagsSpecifiedSize()
			throws CloneNotSupportedException {
		// Setup our genotypes and their expected tags
		String genotypes[] = { "(+ (+ x x) (+ x x))", "(+ (+ x x) x)" };
		String expectedTags[] = { "(+ (+) (+))", "(+ (+) x)" };

		// Setup the expected stats to make the assertions easier
		Map<String, double[]> expectedStats = new HashMap<String, double[]>();

		expectedStats.put(expectedTags[0], new double[] { 0.5, 0.5 });
		expectedStats.put(expectedTags[1], new double[] { 0.5, 0.8 });

		// Setup some individuals
		Vector<Individual> inds = new Vector<Individual>();

		// Make half the inds have the first genotype and the rest the second
		for (int i = 0; i < 10; i++) {
			Individual ind = Individual.fromString((i < 5) ? genotypes[0]
					: genotypes[1], config);
			ind.setId(0);
			inds.add(ind);

			// Give the first tag group fitness=0.5 and the second fitness=0.8
			if (i < 5) {
				ind.setFitness(0.5);
			} else {
				ind.setFitness(0.8);
			}
		}

		// Manually set the tag level/depth to make sure it's overridden
		diversityUtils.setTagLevel(100);
		diversityUtils.setTagDepth(100);

		// Get the tag stats
		Map<String, double[]> tagStats = diversityUtils.getIndividualTags(inds,
				0, 1);

		assertTrue(tagStats.size() == expectedStats.size());
	}

	/**
	 * Tests that the getIndividualTags() method returns the correct
	 * density/fitness values for the tags we setup.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testGetIndividualTagsDensityFitness()
			throws CloneNotSupportedException {
		// Setup our genotypes and their expected tags
		String genotypes[] = { "(+ (+ x x) (+ x x))", "(+ (+ x x) x)" };
		String expectedTags[] = { "(+ (+) (+))", "(+ (+) x)" };

		// Setup the expected stats to make the assertions easier
		Map<String, double[]> expectedStats = new HashMap<String, double[]>();

		expectedStats.put(expectedTags[0], new double[] { 0.5, 0.5 });
		expectedStats.put(expectedTags[1], new double[] { 0.5, 0.8 });

		// Setup some individuals
		Vector<Individual> inds = new Vector<Individual>();

		// Make half the inds have the first genotype and the rest the second
		for (int i = 0; i < 10; i++) {
			Individual ind = Individual.fromString((i < 5) ? genotypes[0]
					: genotypes[1], config);
			ind.setId(0);
			inds.add(ind);

			// Give the first tag group fitness=0.5 and the second fitness=0.8
			if (i < 5) {
				ind.setFitness(0.5);
			} else {
				ind.setFitness(0.8);
			}
		}

		// Manually set the tag depth
		diversityUtils.setTagDepth(1);

		// Get the tag stats
		Map<String, double[]> tagStats = diversityUtils.getIndividualTags(inds);

		// We can't do equals on the maps because the mapped values are arrays
		for (String expectedTag : expectedStats.keySet()) {
			// We at least don't need to do contains() because it'll fail if its
			// not there
			double[] expectedDensityFitness = expectedStats.get(expectedTag);
			double[] densityFitness = tagStats.get(expectedTag);

			assertTrue(densityFitness[0] == expectedDensityFitness[0]
					&& densityFitness[1] == expectedDensityFitness[1]);
		}
	}

	/**
	 * Tests that the getIndividualTags() method -- the version that takes the
	 * specified tag level and tag depth -- returns the correct density/fitness
	 * values for the tags we setup.
	 * 
	 * @throws CloneNotSupportedException
	 */
	@Test
	public void testGetIndividualTagsSpecifiedDensityFitness()
			throws CloneNotSupportedException {
		// Setup our genotypes and their expected tags
		String genotypes[] = { "(+ (+ x x) (+ x x))", "(+ (+ x x) x)" };
		String expectedTags[] = { "(+ (+) (+))", "(+ (+) x)" };

		// Setup the expected stats to make the assertions easier
		Map<String, double[]> expectedStats = new HashMap<String, double[]>();

		expectedStats.put(expectedTags[0], new double[] { 0.5, 0.5 });
		expectedStats.put(expectedTags[1], new double[] { 0.5, 0.8 });

		// Setup some individuals
		Vector<Individual> inds = new Vector<Individual>();

		// Make half the inds have the first genotype and the rest the second
		for (int i = 0; i < 10; i++) {
			Individual ind = Individual.fromString((i < 5) ? genotypes[0]
					: genotypes[1], config);
			ind.setId(0);
			inds.add(ind);

			// Give the first tag group fitness=0.5 and the second fitness=0.8
			if (i < 5) {
				ind.setFitness(0.5);
			} else {
				ind.setFitness(0.8);
			}
		}

		// Manually set the tag level/depth to make sure it's overridden
		diversityUtils.setTagLevel(100);
		diversityUtils.setTagDepth(100);

		// Get the tag stats
		Map<String, double[]> tagStats = diversityUtils.getIndividualTags(inds,
				0, 1);

		// We can't do equals on the maps because the mapped values are arrays
		for (String expectedTag : expectedStats.keySet()) {
			// We at least don't need to do contains() because it'll fail if its
			// not there
			double[] expectedDensityFitness = expectedStats.get(expectedTag);
			double[] densityFitness = tagStats.get(expectedTag);

			assertTrue(densityFitness[0] == expectedDensityFitness[0]
					&& densityFitness[1] == expectedDensityFitness[1]);
		}
	}

	/**
	 * Tests that the threaded tagIndividuals method works as expected.
	 */
	@Test
	public void testTagIndividualsThreads() throws CloneNotSupportedException {
		// Setup our genotypes and their expected tags
		String genotypes[] = { "(+ (+ x x) (+ x x))", "(+ (+ x x) x)" };
		String expectedTags[] = { "(+ (+) (+))", "(+ (+) x)" };

		// Make sure we have more than one thread
		config.setNumThreads(4);

		// Manually set the tag level and tag depth
		diversityUtils.setTagLevel(0);
		diversityUtils.setTagDepth(1);

		// Setup some individuals
		Vector<Individual> inds = new Vector<Individual>();

		// Make half the inds have the first genotype and the rest the second
		for (int i = 0; i < 10; i++) {
			Individual ind = Individual.fromString((i < 5) ? genotypes[0]
					: genotypes[1], config);
			ind.setId(0);
			inds.add(ind);
		}

		// Set their tags
		diversityUtils.tagIndividuals(inds);

		// Now make sure they all have their expected tags set
		for (int i = 0; i < 10; i++) {
			String expectedTag = (i < 5) ? expectedTags[0] : expectedTags[1];

			// Yes, multiple asserts...
			assertEquals(expectedTag, inds.get(i).getTag());
		}
	}

	/**
	 * Tests that the threaded tagIndividuals method -- the version that takes a
	 * specified tag level and tag depth -- works as expected.
	 */
	@Test
	public void testTagIndividualsSpecifiedThreads()
			throws CloneNotSupportedException {
		// Setup our genotypes and their expected tags
		String genotypes[] = { "(+ (+ x x) (+ x x))", "(+ (+ x x) x)" };
		String expectedTags[] = { "(+ (+) (+))", "(+ (+) x)" };

		// Make sure we have more than one thread
		config.setNumThreads(4);

		// Set the tag level and tag depth so we can make sure it's overridden
		diversityUtils.setTagLevel(100);
		diversityUtils.setTagDepth(100);

		// Setup some individuals
		Vector<Individual> inds = new Vector<Individual>();

		// Make half the inds have the first genotype and the rest the second
		for (int i = 0; i < 10; i++) {
			Individual ind = Individual.fromString((i < 5) ? genotypes[0]
					: genotypes[1], config);
			ind.setId(0);
			inds.add(ind);
		}

		// Set their tags
		diversityUtils.tagIndividuals(inds, 0, 1, true, 0);

		// Now make sure they all have their expected tags set
		for (int i = 0; i < 10; i++) {
			String expectedTag = (i < 5) ? expectedTags[0] : expectedTags[1];

			// Yes, multiple asserts...
			assertEquals(expectedTag, inds.get(i).getTag());
		}
	}

	/**
	 * Tests that the threaded tagIndividuals method -- the version that takes a
	 * specified tag level and tag depth -- works as expected when we tell it
	 * NOT to set the tag.
	 */
	@Test
	public void testTagIndividualsSpecifiedNotSet()
			throws CloneNotSupportedException {
		// Setup our genotypes and their expected tags
		String genotypes[] = { "(+ (+ x x) (+ x x))", "(+ (+ x x) x)" };

		// Make sure we have more than one thread
		config.setNumThreads(4);

		// Setup some individuals
		Vector<Individual> inds = new Vector<Individual>();

		// Make half the inds have the first genotype and the rest the second
		for (int i = 0; i < 10; i++) {
			Individual ind = Individual.fromString((i < 5) ? genotypes[0]
					: genotypes[1], config);
			ind.setId(0);
			inds.add(ind);
		}

		// Generate their tags but DON'T set them
		diversityUtils.tagIndividuals(inds, 0, 1, false, 0);

		// Now make sure their tags are NOT set.
		for (int i = 0; i < 10; i++) {
			// Yes, multiple asserts...
			assertTrue(inds.get(i).getTag() == null);
		}
	}
}
