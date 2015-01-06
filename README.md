gecco2015
=========

This project contains the source code necessary for reproducing the results from our paper "An Efficient Structural Diversity Technique for Genetic Programming." This also includes all the configuration files used for each experiment that is discussed in this paper.

See LICENSE.txt for license information. NOTE: Although this code can be used as-is, it is not intended to be a general GP library and is therefore not documented as such. The intended use of this code is for reproducing the results, as well as potentially extending this technique. For a highly-documented, general GP (as well as EC in general) framework, we recommend trying a larger project such as ECJ.


DEPENDENCIES:

Java 1.6 or above

Apache Maven - for building the project (and automatically gathering required dependencies)

Matplotlib - for plotting results

Apache Commons Lang 3

Apache Log4j 1.2.17

JUnit 4.4




BUILDING THE CODE:

The easiest way to build this project into an executable jar file is to use Apache Maven:

1. Download and install Apache Maven (http://maven.apache.org/)

2. Navigate to the top-level directory of this project and build with Maven: Execute "mvn package" (Assuming that you have Maven installed correctly and have the correct permissions, this will download all required dependencies and place them into the executable jar file).

RUNNING THE CODE:

After you have built the executable jar file as described above, simply execute the following: 

java -jar target/gp-research-0.1.jar conf/CONFIG_FILE.properties

This will use the config file located at conf/CONFIG_FILE.properties, and the resulting output will be saved to the directory "output" by default (Specify a path, which can be new if you have the appropriate permissions, with the outputDir=DIRECTORY option to use a different output directory. This is useful when automating execution to perform several independent trials).

NOTE: The configuration files were all taken directly from our experiments, which were run in a high-performance computing environment. You may need to adjust the number of threads (numThreads option in the configuration files) for your system.


GENERATING PLOTS:

We have also included all the scripts necessary for recreating the plots from the paper. These scripts are intended to run on a directory containing data files from multiple runs of the code. The scripts are located in the postRunScripts directory:

1. compareFitness.py - Plots mean best fitness over time.

    Example usage: python compareFitness.py directory1,directory2,directory3 label1,label2,label3 outputDirectory

2. convergence.py - Plots bar charts of the mean number of fitness evaluations required to find a solution, for the runs in which a solution was found. Also creates (1) a file containing pairwise Mann-Whitney U test p-values for every input directory supplied, and (2) a file containing the mean number of fitness evaluations required.

    Example usage: python convergence.py directory1,directory2,directory3 label1,label2,label3 outputDirectory

3. successRates.py - Creates (1) a text file containing the success rates (percentage of runs in which a solution was found) for each input directory supplied, and (2) a file containing pairwise Fisher's Exact test p-values for every input directory supplied.

    Example usage: python successRates.py directory1,directory2,directory3 label1,label2,label3 outputDirectory

4. treeTagPlots.py - Plots the mean density of the most dense genetic marker over time (in generations), as described in our paper. Note: The LEGEND_CODE corresponds to the Matplotlib legend location integer code, which is used to position the legend. We used 1 (upper right) for results from our approach and 4 (lower right) for results from standard GP.

    Example usage: python treeTagPlots.py inputDirectory LEGEND_CODE outputDirectory
