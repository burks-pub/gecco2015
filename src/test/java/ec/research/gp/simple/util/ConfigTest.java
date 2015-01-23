package ec.research.gp.simple.util;

import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;

import org.junit.BeforeClass;
import org.junit.Test;

import ec.research.gp.simple.util.Config;


/**
 * Tests the {@link Config} class for functionality.
 * 
 */
public class ConfigTest {
	private static Config config;

	@BeforeClass
	public static void setup() throws FileNotFoundException, IOException {
		config = new Config("src/test/resources/multiplexer.properties");
	}

	/**
	 * Makes sure the Option annotation works correctly and is accessible
	 * 
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 */
	@Test
	public void testOptionAnnotation() throws NoSuchFieldException,
			SecurityException {
		// Just make sure the seed option is annotated as expected.
		Field seed = config.getClass().getDeclaredField("seed");

		assertTrue(seed.getAnnotation(Config.Option.class) != null);
	}
}
