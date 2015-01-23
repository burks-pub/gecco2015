package ec.research.gp.simple.multiplexer;

import org.apache.log4j.Logger;

import ec.research.gp.simple.bool.BooleanProblem;
import ec.research.gp.simple.representation.Individual;
import ec.research.gp.simple.util.Context;



public class Multiplexer extends BooleanProblem {
	/**
	 * Holds the number of bits for the problem.
	 */
	private int n;

	/**
	 * Holds the k bits number so we don't have to calculate it...
	 */
	private int k;

	/**
	 * Holds the number of data bits (2^k) so we only calculate it once.
	 */
	private int numDataBits;

	/**
	 * Holds the number of combinations of input bit strings (2^n).
	 */
	private double combinations;

	/**
	 * Holds the target values so that we don't have to keep recalculating them.
	 */
	private boolean[] targetValues;

	/**
	 * Logger for any output/messages.
	 */
	private static final Logger logger = Logger.getLogger(Multiplexer.class);

	/**
	 * Set up the context multiplexer parameters.
	 */
	@Override
	public void init(Context c) {
		setContext(c);

		this.k = Integer.parseInt(context.getConfig().getParameter(
				"numAddressBits"));
		this.n = (int) Math.pow(2, this.k) + this.k;

		// Setup the variables from the config.
		numDataBits = this.n - this.k;
		combinations = (int) Math.pow(2, this.n);

		// Setup the target values.
		targetValues = new boolean[(int) combinations];
		for (int i = 0; i < combinations; i++) {
			// Get the value of the address bits
			int outputIndex = (i >> numDataBits);

			// Set the expected value.
			targetValues[i] = (((1 << outputIndex) & i) > 0);
		}

		logger.info(String.format("n=%s k=%s data bits=%s combinations=%s", n,
				k, numDataBits, combinations));
	}

	@Override
	public void fitness(Individual individual) {
		if (!individual.isEvaluated()) {
			double hits = 0; // Hits also is the raw fitness in this case.

			for (int i = 0; i < combinations; i++) {
				Boolean res = (Boolean) individual.getRoot().evaluate(this, i);

				if (res == targetValues[i]) {
					hits++;
				}
			}

			// Set the hits
			individual.setHits((int) hits);

			// Set the fitness (between 0 and 1) as the fraction correct
			individual.setFitness(hits / combinations);

			// Mark that baby as evaluated!
			individual.setIsEvaluated(true);
		}
	}

	public int getNumDataBits() {
		return this.numDataBits;
	}

	public int getN() {
		return this.n;
	}

}
