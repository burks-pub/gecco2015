package ec.research.gp.simple.problem;

import java.io.File;
import java.lang.reflect.Constructor;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import ec.research.gp.simple.gp.GP;
import ec.research.gp.simple.util.Config;
import ec.research.gp.simple.util.Context;



/**
 * Simple main entry point for the run. This is responsible for loading the
 * config and running the GP.
 * 
 */
public class ProblemRunner implements Runnable {
	// Log4J Logger for any output messages.
	private static final Logger logger = Logger.getLogger(ProblemRunner.class);

	// Holds the command-line arguments, so we can use them in run()
	private String[] args;

	/**
	 * Gets a new GP object from the config. If the "gp" property is not
	 * present, we'll default to the basic GP class.
	 * 
	 * @param c
	 *            the context object to use
	 * @return a new GP object loaded by using the config.
	 * @throws Exception
	 */
	public static GP getGP(Context c) throws Exception {
		String gpName = c.getConfig().getParameter("gp");

		// If the config property wasn't provided, just return a simple GP
		if (gpName == null) {
			return new GP(c);
		}
		// Otherwise, create a new GP object based on the config.
		else {
			Class<?> gpClass = Class.forName(gpName);
			Constructor<?> constructor = gpClass.getConstructor(Context.class);

			logger.debug("Creating a new GP of type: " + gpName);

			return (GP) constructor.newInstance(c);
		}
	}

	/**
	 * Convenience method used to make sure the output directories exist before
	 * running.
	 */
	public static void checkDirs(String path) {
		File dirs = new File(path);

		if (!dirs.exists()) {
			dirs.mkdirs();
		}
	}

	/**
	 * Create a new ProblemRunner with the given command-line arguments.
	 * 
	 * @param args
	 *            the command-line arguments
	 */
	public ProblemRunner(String[] args) {
		// run() will handle the args.
		this.args = args;
	}

	/**
	 * Main entry point for the evolution.
	 * 
	 * @param args
	 *            only expects the path to the config file to use.
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		ProblemRunner p = new ProblemRunner(args);
		p.run();
	}

	@Override
	public void run() {
		// Make sure we got the right number of arguments
		if (args.length < 1) {
			logger.fatal("Expected at least one argument (The path to the config .properties file)!");
			System.exit(1);
		}

		PropertyConfigurator.configure("log4j.properties");
		// Setup the config and context
		Config config;
		try {
			config = new Config(args[0]);

			// Override config vars from the command line
			if (args.length > 1) {
				for (int i = 1; i < args.length; i++) {
					String nameVal[] = args[i].split("=");

					config.setParameter(nameVal[0].trim(), nameVal[1].trim());
				}

				// If we did get some overrides, do a fresh init() to set them
				config.init();
			}

			// Default to top-level project dir (or same dir as jar).
			PropertyConfigurator.configure("log4j.properties");

			Context context = new Context(config);

			// Make sure the output directories exist.
			checkDirs(config.getOutputDir());

			// Fire up the GP!
			GP gp = getGP(context);

			gp.init();
			gp.evolve();
		} catch (Exception e) {
			logger.fatal(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}

	}

}
