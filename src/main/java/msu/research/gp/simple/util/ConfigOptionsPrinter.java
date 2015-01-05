package msu.research.gp.simple.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Simple utility class to print out all the current config options by category.
 * We just print them out in HTML form to the given path or to the current
 * directory at "configOptions.html".
 * 
 * @author Armand R. Burks
 * 
 */
public class ConfigOptionsPrinter {
	/**
	 * @return The current set of available configuration options in the
	 *         {@link Config} class, grouped by category.
	 */
	public static Map<String, List<Config.Option>> collectOptions() {
		// Holds all the configuration options (grouped by sorted category)
		TreeMap<String, List<Config.Option>> groupedOptions = new TreeMap<String, List<Config.Option>>();

		for (Field f : Config.class.getDeclaredFields()) {
			Config.Option option = f.getAnnotation(Config.Option.class);

			if (option != null) {
				String category = option.cat();

				// Add a new category to the collection if necessary
				if (!groupedOptions.containsKey(category)) {
					groupedOptions
							.put(category, new ArrayList<Config.Option>());
				}

				// Add this option to the collection by its category
				groupedOptions.get(category).add(option);
			}
		}

		return groupedOptions;
	}

	public static void main(String[] args) throws IOException {
		// Get all the currently available configuration options.
		Map<String, List<Config.Option>> groupedOptions = collectOptions();

		String outPath = "configOptions.html";

		if (args.length > 0) {
			outPath = args[0];
		}

		BufferedWriter writer = new BufferedWriter(new FileWriter(outPath));

		// Print a nice little heading
		writer.write("<center><h1>Configuration Options</h1></center>\n");

		// Print the current date
		writer.write(String.format(
				"Below are all the available options as of: %s.<br/>\n",
				new SimpleDateFormat("MM/dd/yyyy -- HH:mm").format(System
						.currentTimeMillis())));

		// Print out the options by category in a table
		writer.write("<table>\n");
		for (String category : groupedOptions.keySet()) {
			writer.write(String
					.format("<tr colspan=\"2\"><td><center><h3>%s</h3></center></td></tr>\n",
							category));

			for (Config.Option option : groupedOptions.get(category)) {
				writer.write(String.format("<tr><td>%s</td><td>%s</td></tr>\n",
						option.value(), option.desc()));
			}
		}
		writer.write("<tr><td></tr></td>\n");
		writer.write("</table>\n");
		writer.close();
	}
}
