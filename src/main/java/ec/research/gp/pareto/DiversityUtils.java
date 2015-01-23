package ec.research.gp.pareto;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

import ec.research.gp.simple.representation.Individual;
import ec.research.gp.simple.representation.Node;
import ec.research.gp.simple.util.Config;



/**
 * Contains utility methods for implementing the genotypic diversity objective,
 * such as tagging trees, etc.
 * 
 */
public class DiversityUtils {
	// Log4J Logger for any output/messages
	private static final Logger logger = Logger.getLogger(DiversityUtils.class);

	// The {@link Config} object for the run
	private Config config;

	// The maximum number of nodes to use in tagging individuals.
	private int tagDepth;

	// The current level in the trees that we start tagging at (for sliding time
	// window).
	private int currentTagLevel;

	// Collects the global set of unique tags in the population (each tag level)
	protected Map<Integer, Set<String>> allTags;

	// Lock for the TagThread
	private Object lock = new Object();

	/**
	 * Initialize a new {@link DiversityUtils} object
	 */
	public DiversityUtils(Config config) {
		this.config = config;

		// Get the tag depth from config
		this.tagDepth = config.getTagDepth();

		// Initialize the global collection of tags
		this.allTags = new HashMap<Integer, Set<String>>();
	}

	/**
	 * Convenience method to recursively generate a tag for the individual by
	 * traversing the tree in-order up unto the specified depth. Function nodes
	 * at the max depth are given a closed parenthesis to keep the resulting
	 * structure recreatable.
	 * 
	 * @param node
	 *            the current node in the traversal
	 * 
	 * @param tagLevel
	 *            the tag level to use
	 * 
	 * @param tagDepth
	 *            the tag depth to use
	 * 
	 * @param currentDepth
	 *            the current depth in the traversal
	 * 
	 * @param buffer
	 *            the string buffer containing the tag we're building
	 * 
	 * @return a tag based on the in-order traversal of the tree. Simply
	 *         contains the nodes along the traversal in lisp-style (functions
	 *         are closed off with ending parenthesis, even if their terminals
	 *         aren't included. That way we could still recreate that subtree --
	 *         excluding the terminals of course).
	 */
	private String buildIndividualTag(Node node, int tagLevel, int tagDepth,
			int currentDepth, StringBuilder buffer) {
		// Is the node in the tag range?
		boolean inRange = tagLevel <= currentDepth
				&& currentDepth <= tagLevel + tagDepth;

		if (inRange) {
			// If it's a terminal, just add the node to the output
			if (node.isTerminal() && inRange) {
				buffer.append(node.toString());
				return buffer.toString();
			}

			// Otherwise, it's a function, add it
			buffer.append("(" + node.toString());

			// Add it's children unless we've reached the end of the range
			if (currentDepth < tagLevel + tagDepth) {
				for (Node child : node.getChildren()) {
					buffer.append(" ");
					buildIndividualTag(child, tagLevel, tagDepth,
							currentDepth + 1, buffer);
				}
			}

			// Add closing parenthesis even if we didn't add the whole subtree
			if (node.getNumChildren() > 0) {
				buffer.append(")");
			}
		}

		// If it's not in range, but not past it yet, visit its children
		else if (currentDepth < tagLevel) {
			// We want to represent a "set of fragments," so add a comma
			boolean nextUp = node.getNumChildren() > 0
					&& currentDepth == tagLevel - 1;

			for (int i = 0; i < node.getNumChildren(); i++) {
				// Add a comma to the fragments if the children are the first
				// level in the tag
				if (nextUp && (i > 0 || buffer.length() > 0)) {
					buffer.append(",");
				}

				Node child = node.getChild(i);
				buildIndividualTag(child, tagLevel, tagDepth, currentDepth + 1,
						buffer);
			}
		}

		return buffer.toString();
	}

	/**
	 * Adds the given individuals' tags to the global collection of unique tags
	 * seen.
	 * 
	 * @param individuals
	 *            the individuals whose tags to collect
	 */
	public void collectTags(Collection<Individual> individuals) {
		// Lazy-initialize the tags for the current tag level
		if (!this.allTags.containsKey(this.currentTagLevel)) {
			this.allTags.put(this.currentTagLevel, new HashSet<String>());
		}

		// Add all the tags
		for (Individual individual : individuals) {
			this.allTags.get(this.currentTagLevel).add(individual.getTag());
		}
	}

	/**
	 * Adds the given individuals' tags to the global collection of unique tags
	 * seen for the given tag level.
	 * 
	 * @param individuals
	 *            the individuals whose tags to collect
	 * 
	 * @param tagLevel
	 *            the level at which to collect the tags
	 */
	public void collectTags(Collection<Individual> individuals, int tagLevel) {
		// Lazy-initialize the tags for the given tag level
		if (!this.allTags.containsKey(tagLevel)) {
			this.allTags.put(tagLevel, new HashSet<String>());
		}

		// Add all the tags
		for (Individual individual : individuals) {
			this.allTags.get(tagLevel).add(individual.getTag());
		}
	}

	/**
	 * Adds the given tree tag to the global collection of unique tags.
	 * 
	 * @param tag
	 *            the tree tag to add
	 */
	public void addTag(String tag) {
		// Lazy-initialize the tags for the current tag level
		if (!this.allTags.containsKey(this.currentTagLevel)) {
			this.allTags.put(this.currentTagLevel, new HashSet<String>());
		}

		// Add the tag
		this.allTags.get(this.currentTagLevel).add(tag);
	}

	/**
	 * Adds the given tree tag to the global collection of unique tags.
	 * 
	 * @param tag
	 *            the tree tag to add
	 * 
	 * @param tagLevel
	 *            the tag level for the given tag
	 */
	public void addTag(String tag, int tagLevel) {
		// Lazy-initialize the tags for the given tag level
		if (!this.allTags.containsKey(tagLevel)) {
			this.allTags.put(tagLevel, new HashSet<String>());
		}

		// Add the tag
		this.allTags.get(tagLevel).add(tag);
	}

	/**
	 * Adds the given tree tags to the global collection of unique tags. This
	 * assumes that the indices in the array correspond to the tag levels.
	 * 
	 * @param tags
	 *            the array of tags to add to the collection
	 */
	public void addTags(String[] tags) {
		for (int i = 0; i < tags.length; i++) {
			addTag(tags[i], i);
		}
	}

	/**
	 * @return the total number of unique tree tags collected so far (all tag
	 *         levels).
	 */
	public int getNumTags() {
		int totalTags = 0;

		for (Set<String> tags : this.allTags.values()) {
			totalTags += tags.size();
		}

		return totalTags;
	}

	/**
	 * @param tagLevel
	 *            the tag level of interest
	 * @return the total number of unique tree tags collected so far at the
	 *         given tag level
	 */
	public int getNumTags(int tagLevel) {
		if (!this.allTags.containsKey(tagLevel)) {
			return 0;
		}

		return this.allTags.get(tagLevel).size();
	}

	/**
	 * Tags an individual based on the current tag level. The tag is used to
	 * group individuals and is ultimately what determines how individuals rank
	 * on the density objective. This uses the in-order traversal of the tree up
	 * until the specified maximum number of nodes have been reached (defaults
	 * to one).
	 * 
	 * @param individual
	 *            the individual to tag.
	 */
	public void tagIndividual(Individual individual) {
		// Just use the root node if we're only using one node.
		if (this.tagDepth == 0 && this.currentTagLevel == 0) {
			individual.setTag(individual.getRoot().toString());
		}

		// Otherwise, build the tag by traversing the tree in-order
		else {
			// Now set the individual's tag
			individual
					.setTag(buildIndividualTag(individual.getRoot(),
							this.currentTagLevel, this.tagDepth, 0,
							new StringBuilder()));
		}
	}

	/**
	 * Tags an individual using the specified tag level and tag depth. This also
	 * uses the in-order traversal of the tree up until the specified depth,
	 * starting at the specified level.
	 * 
	 * @param individual
	 *            the individual to tag
	 * 
	 * @param tagLevel
	 *            the level in the individual's tree to start tagging
	 * 
	 * @param tagDepth
	 *            how far down to go, from the tagLevel, when building the tag
	 */
	public void tagIndividual(Individual individual, int tagLevel, int tagDepth) {
		// Just use the root node if we're only using one node.
		if (tagDepth == 0 && tagLevel == 0) {
			individual.setTag(individual.getRoot().toString());
		}

		// Otherwise, build the tag by traversing the tree in-order
		else {
			// Now set the individual's tag
			individual.setTag(buildIndividualTag(individual.getRoot(),
					tagLevel, tagDepth, 0, new StringBuilder()));
		}
	}

	/**
	 * Tags the given collection of individuals. This uses the current tag level
	 * and tag depth that is set to set numTagObjectives tags in the
	 * indivdiuals' tags array.
	 * 
	 * @param individuals
	 *            the individuals to tag.
	 * 
	 */
	public void tagIndividuals(Vector<Individual> individuals) {
		tagIndividuals(individuals, this.currentTagLevel, this.tagDepth, true,
				0);
	}

	/**
	 * Convenience method to tag the given collection of individuals and collect
	 * the set of generated tags instead of directly setting them in the
	 * individuals. This uses the current tag level and tag depth that is set.
	 * 
	 * @param individuals
	 *            the individuals to tag.
	 * 
	 * @return the set of generated tags, mapped to their density and avg.
	 *         fitness
	 */
	public Map<String, double[]> getIndividualTags(
			Vector<Individual> individuals) {
		Map<String, double[]> tagStats = (Map<String, double[]>) tagIndividuals(
				individuals, this.currentTagLevel, this.tagDepth, false, 0);

		return tagStats;
	}

	/**
	 * Convenience method to tag the given collection of individuals and collect
	 * the set of generated tags instead of directly setting them in the
	 * individuals.
	 * 
	 * @param individuals
	 *            the individuals to tag.
	 * 
	 * @param tagLevel
	 *            the tag level
	 * 
	 * @param tagDepth
	 *            the tag depth
	 * 
	 * @return the set of generated tags, mapped to their density and average
	 *         fitness
	 */
	public Map<String, double[]> getIndividualTags(
			Vector<Individual> individuals, int tagLevel, int tagDepth) {
		Map<String, double[]> tagStats = (Map<String, double[]>) tagIndividuals(
				individuals, tagLevel, tagDepth, false, 0);

		return tagStats;
	}

	/**
	 * Convenience method to tag the given collection of individuals.
	 * 
	 * @param individuals
	 *            the individuals to tag.
	 * 
	 * @param tagLevel
	 *            the level in the tree to start building the tag
	 * 
	 * @param tagDepth
	 *            how far down from the tagLevel to go in order to make the tag
	 * 
	 * @return the set of tags generated, mapped to their densities and average
	 *         fitness, or null if we're setting the tags on the individuals
	 *         instead of collecting them
	 * 
	 */
	public Map<String, double[]> tagIndividuals(Vector<Individual> individuals,
			int tagLevel, int tagDepth, boolean doSetTags, int tagMethod) {
		int numThreads = config.getNumThreads();
		int chunkSize = individuals.size() / numThreads;
		int start = 0, end = 0;
		Thread[] threads = new Thread[numThreads];
		Map<String, double[]> tagStats = null; // holds the tags (if not setting
												// them)

		// Are we setting the tags or collecting them?
		if (!doSetTags) {
			if (tagMethod == 0) {
				tagStats = new HashMap<String, double[]>();
			}
		}

		// Fire off all the tagging threads
		for (int i = 0; i < numThreads; i++) {
			start = i * chunkSize;

			// Set the end index (account for uneven popSize/numThreads)
			end = (i == numThreads - 1) ? individuals.size()
					: (start + chunkSize); // subList end is exclusive so no -1

			TagThread thread = new TagThread(individuals.subList(start, end),
					tagStats, tagLevel, tagDepth, doSetTags, tagMethod);
			threads[i] = new Thread(thread);
			threads[i].start();
		}

		// Join them all together
		for (int i = 0; i < numThreads; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				logger.error(e);
				System.exit(1);
			}
		}

		// We need to set the density and average fitness if collecting stats
		if (!doSetTags) {
			if (tagMethod == 0) {
				for (String tag : tagStats.keySet()) {
					double[] stats = tagStats.get(tag);

					// First set the average fitness
					stats[1] /= stats[0];

					// Now transform the counts into a density
					stats[0] /= individuals.size();
				}
			}
		}

		return tagStats;
	}

	/**
	 * Sets the tree depth to use for tagging individuals based on their tree
	 * traversal.
	 * 
	 * @param tagDepth
	 *            the number of levels to use in creating the tag
	 */
	public void setTagDepth(int tagDepth) {
		this.tagDepth = tagDepth;
	}

	/**
	 * @return the depth to use for creating tree tags
	 */
	public int getTagDepth() {
		return this.tagDepth;
	}

	/**
	 * Sets the level at which to start tagging trees to the given number. NOTE:
	 * This is not to be confused with the tag depth, which dictates how far
	 * down to traverse from the desired level.
	 * 
	 * @param tagLevel
	 */
	public void setTagLevel(int tagLevel) {
		this.currentTagLevel = tagLevel;

		// Also initialize the collection for this level if necessary
		if (!this.allTags.containsKey(this.currentTagLevel)) {
			this.allTags.put(this.currentTagLevel, new HashSet<String>());
		}
	}

	/**
	 * @return the level at which we start tagging trees
	 */
	public int getTagLevel() {
		return this.currentTagLevel;
	}

	/**
	 * Determines if the given tree tag is globally unique (across all the tags
	 * we've seen throughout the course of this run) at the current tag level.
	 * 
	 * @param tag
	 *            the tree tag to test
	 * 
	 * @return whether or not the given tree tag is globally unique
	 */
	public boolean isTagUnique(String tag) {
		return !this.allTags.containsKey(this.currentTagLevel)
				|| !this.allTags.get(this.currentTagLevel).contains(tag);
	}

	/**
	 * Determines if the given tree tag is globally unique (across all the tags
	 * we've seen throughout the course of this run) at the given tag level.
	 * 
	 * @param tag
	 *            the tree tag to test
	 * 
	 * @param tagLevel
	 *            the tag level at which to check for
	 * @return whether or not the given tree tag is globally unique
	 */
	public boolean isTagUnique(String tag, int tagLevel) {
		return !this.allTags.containsKey(tagLevel)
				|| !this.allTags.get(tagLevel).contains(tag);
	}

	/**
	 * @return the collection of unique tags seen so far at the current tag
	 *         level or null if no tags have ever been collected at that level.
	 */
	public Set<String> getTags() {
		// Make sure the requested tag level is legit.
		if (!this.allTags.containsKey(this.currentTagLevel)) {
			return null;
		}

		return this.allTags.get(this.currentTagLevel);
	}

	/**
	 * @param tagLevel
	 *            the tag level for which to get the tags
	 * @return the collection of unique tags seen so far at the given tag level
	 *         or null if no tags have ever been collected at that level.
	 */
	public Set<String> getTags(int tagLevel) {
		// Make sure the requested tag level is legit.
		if (!this.allTags.containsKey(tagLevel)) {
			return null;
		}

		return this.allTags.get(tagLevel);
	}

	/**
	 * Simple helper class to handle tagging individuals in a multi-threaded
	 * fashion. Might as well take advantage of the threads since evaluations
	 * won't be using them when we're tagging individuals.
	 * 
	 */
	private class TagThread implements Runnable {
		private Collection<Individual> individuals;
		private int tagLevel;
		private int tagDepth;
		private boolean doSetTag;

		/*
		 * Holds the mapping of tag densities and avg. fitness. We make it a
		 * generic object because it could be a list of mappings if we're using
		 * mult. tag levels, or a flat mapping for a single tag level.
		 */
		private Map<String, double[]> tagCounts;

		/**
		 * Creates a new {@link TagThread} for tagging a collection of
		 * individuals.
		 * 
		 * @param individuals
		 *            the collection of individuals to tag
		 * 
		 * @param tagCounts
		 *            either a Map<String, double[]> or a List of Map<String,
		 *            double[]> that maps each tag to it's density and average
		 *            fitness, depending on if we're using a single tag level,
		 *            or multiple tag levels, respectively
		 * 
		 * @param tagLevel
		 *            the tag level
		 * 
		 * @param tagDepth
		 *            the tag depth
		 * 
		 * @param doSetTag
		 *            whether or not to set the tag for the individuals or to
		 *            return a collection of the individuals' tags
		 * 
		 * @param tagMethod
		 *            0 to set just a single tag
		 */
		public TagThread(Collection<Individual> individuals,
				Map<String, double[]> tagCounts, int tagLevel, int tagDepth,
				boolean doSetTag, int tagMethod) {
			this.individuals = individuals;
			this.tagLevel = tagLevel;
			this.tagDepth = tagDepth;
			this.doSetTag = doSetTag;
			this.tagCounts = tagCounts;
		}

		/**
		 * Tags the given individual with a single tag at the tag level and
		 * depth.
		 * 
		 * @param individual
		 *            the individual to tag
		 */
		public void tagSingleLevel(Individual individual) {
			// Set the individual's tag
			if (this.doSetTag) {
				tagIndividual(individual, this.tagLevel, this.tagDepth);
			}
			// Otherwise, just add it to the collection
			else {
				String tag = buildIndividualTag(individual.getRoot(),
						this.tagLevel, this.tagDepth, 0, new StringBuilder());

				synchronized (lock) {
					// Increment the tag's count and add cumulative fitness.
					if (!tagCounts.containsKey(tag)) {
						tagCounts.put(tag, new double[] { 0.0, 0.0 });
					}
					double[] tagStats = tagCounts.get(tag);
					tagStats[0]++;
					tagStats[1] += individual.getFitness();
				}
			}
		}

		@Override
		public void run() {
			// Simply tag all the individuals we were given.
			for (Individual individual : this.individuals) {
				tagSingleLevel(individual);
			}
		}
	}
}
