package ec.research.gp.simple.util;

import java.util.Random;

import org.apache.log4j.Logger;

import ec.research.gp.statistics.Statistics;



public class Context {
	// Holds the config object
	private Config config;

	// Holds the stats object
	private Statistics stats;

	/**
	 * Holds the random number generator for the run.
	 */
	private Random rand;

	/**
	 * Log4J logger for error/debug messages, etc.
	 */
	private static final Logger logger = Logger.getLogger(Context.class);

	/**
	 * Create a new Context object containing the given Config object.
	 * 
	 * @param cfg
	 *            the Config for this run.
	 */
	public Context(Config cfg) {
		// Set the configuration object, and seed the random number generator
		this.config = cfg;

		this.rand = new Random();

		logger.info("Seed: " + config.getSeed());
		rand.setSeed(cfg.getSeed());
	}

	/**
	 * @return the configuration for this context.
	 */
	public Config getConfig() {
		return this.config;
	}

	/**
	 * Sets the {@link Statistics} object to the given object.
	 * 
	 * @param stats
	 *            the stats object for the run
	 */
	public void setStats(Statistics stats) {
		this.stats = stats;
	}

	/**
	 * @return the stats object for this run
	 */
	public Statistics getStats() {
		return this.stats;
	}

	/**
	 * Returns the next random boolean, given the probability to use.
	 * 
	 * @param probability
	 *            the probability to use
	 * @return the next random boolean, given the probability to use
	 */
	public boolean nextBool(double probability) {
		return rand.nextDouble() <= probability;
	}

	/**
	 * Generates a random number between min and max (inclusive).
	 * 
	 * @param min
	 *            the minimum in the range
	 * @param max
	 *            the maximum in the range
	 * @return a random number between min and max
	 */
	public int randBetween(int min, int max) {
		return rand.nextInt(max - min + 1) + min;
	}

	/**
	 * Generates a random double between min and max (inclusive).
	 * 
	 * @param min
	 *            the minimum value in the range
	 * @param max
	 *            the maximum in the range
	 * @return a random double between min and max
	 */
	public double randBetween(double min, double max) {
		return min + (max - min) * rand.nextDouble();
	}

	/**
	 * @return a random number in the range [0, 1]
	 */
	public double randDouble() {
		return rand.nextDouble();
	}

	/**
	 * @return a reference to this run's random number generator. Use with care.
	 */
	public Random getRand() {
		return this.rand;
	}
}
