package msu.research.gp.simple.parity;

import org.apache.log4j.Logger;

import msu.research.gp.simple.bool.BooleanProblem;
import msu.research.gp.simple.representation.Individual;
import msu.research.gp.simple.util.Context;

public class NParity extends BooleanProblem {
	/**
	 * Holds the number of bits for the problem.
	 */
	private int n;

	/**
	 * Holds the number of combinations of input bit strings (2^n).
	 */
	private int combinations;

	// Cache of the correct values, so we don't recalculate over and over.
	private boolean[] correctValues;

	/**
	 * Logger for any output/messages.
	 */
	private static final Logger logger = Logger.getLogger(NParity.class);

	/**
	 * Set up the context multiplexer parameters.
	 */
	@Override
	public void init(Context c) {
		setContext(c);

		// Set n (default to 5).
		String nParam = context.getConfig().getParameter("n");

		if (nParam != null) {
			this.n = Integer.parseInt(nParam);
		} else {
			logger.warn("n parameter not specified. Defaulting to 5");
			this.n = 5;
		}

		// Setup the variables from the config.
		combinations = (int) Math.pow(2, this.n);

		// Now that we know the combinations, set up the correct answers.
		this.correctValues = new boolean[combinations];

		for (int testPoint = 0; testPoint < combinations; testPoint++) {
			// Could use bit magic but this is more legible and we do it once.
			String binary = Integer.toBinaryString(testPoint);
			int numOnes = 0;

			for (int j = 0; j < binary.length(); j++) {
				if (binary.charAt(j) == '1') {
					numOnes++;
				}
			}

			this.correctValues[testPoint] = (numOnes % 2 == 0);
		}

		logger.info(String.format("n=%s combinations=%s", n, combinations));
	}

	@Override
	public void fitness(Individual individual) {
		if (!individual.isEvaluated()) {
			double hits = 0; // Hits also is the raw fitness in this case.

			for (int i = 0; i < combinations; i++) {
				Boolean res = (Boolean) individual.getRoot().evaluate(this, i);

				if (res == this.correctValues[i]) {
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

	public int getN() {
		return this.n;
	}

}
