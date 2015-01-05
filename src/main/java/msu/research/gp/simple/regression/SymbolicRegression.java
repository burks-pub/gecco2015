package msu.research.gp.simple.regression;

import java.util.HashSet;
import java.util.Set;

import msu.research.gp.simple.problem.Problem;
import msu.research.gp.simple.representation.Individual;
import msu.research.gp.simple.util.Context;

/**
 * Symbolic Regression problem for the function: 4x^4 + x^3 + x^2 + x as in Koza
 * '92 (with coefficients added) using 20 (unique) random points in the range
 * [-1, 1].
 * 
 * @author Armand R. Burks
 * 
 */
public class SymbolicRegression extends Problem {
	// Holds the randomly-generated test points in the range [-1, 1] as in Koza.
	private static Double[] TEST_POINTS = new Double[20];

	// Holds the calculated target values for the test points.
	private static Double[] TARGET_VALUES = new Double[20];

	// Smallest acceptable error to reward a hit in the fitness function.
	public static final double HITS_CRITERION = 0.01;

	// Anything smaller than this is considered 0, as in ECJ.
	public static final double SMALL = 1.11e-15;

	// Don't accept anything larger than this value, as in ECJ.
	public static final double LARGE = 1.0e15;

	@Override
	public void init(Context c) {
		setContext(c);

		// Tracks the points we added so we can make sure they're unique
		Set<Double> addedPoints = new HashSet<Double>();

		// Initialize the 20 test points. Make them all unique.
		int i = 0;

		while (i < 20) {
			Double randPoint = this.context.randBetween(-1.0, 1.0);
			if (addedPoints.add(randPoint)) {
				// Add the test point.
				TEST_POINTS[i] = randPoint;

				// Calculate the target function value.
				TARGET_VALUES[i] = (4.0 * Math.pow(randPoint, 4))
						+ (3.0 * Math.pow(randPoint, 3))
						+ (2.0 * Math.pow(randPoint, 2)) + randPoint;

				// On to the next point.
				i++;
			}
		}

	}

	@Override
	public void fitness(Individual individual) {
		if (!individual.isEvaluated()) {
			// Total number of points for which the error is small enough.
			int hits = 0;

			// Total error
			double totalError = 0.0;

			for (int i = 0; i < TEST_POINTS.length; i++) {
				// Let the individual loose on the input and get the result
				Double result = (Double) individual.getRoot().evaluate(this,
						TEST_POINTS[i]);

				// Get the abs error and potentially reward a hit
				double error = Math.abs(TARGET_VALUES[i] - result);

				// Normalize the error value to avoid getting NaN, as in ECJ.
				if (!(error < LARGE)) {
					error = LARGE;
				} else if (error < SMALL) {
					error = 0.0;
				}

				if (error <= HITS_CRITERION) {
					hits++;
				}
				// Don't add error if we reward a hit.
				else {
					totalError += error;
				}
			}

			// Set the individual's hits
			individual.setHits(hits);

			// Adjust raw fitness (error) to map from 0 to 1.
			individual.setFitness(1.0 / (1.0 + totalError));

			// Mark the individual as evaluated.
			individual.setIsEvaluated(true);

			// See if the individual is ideal on all of the test cases.
			if (hits == TEST_POINTS.length) {
				individual.setIsOptimal(true);
			}
		}
	}
}
