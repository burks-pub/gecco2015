gecco2015
=========

This project contains the source code necessary for reproducing the results from our paper "An Efficient Structural Diversity Technique for Genetic Programming." This also includes all the configuration files used for each experiment that is discussed in this paper.

See LICENSE.txt for license information. NOTE: Although this code can be used as-is, it is not intended to be a general GP library and is therefore not documented as such. The intended use of this code is for reproducing the results, as well as potentially extending this technique. For a highly-documented, general GP (as well as EC in general) framework, we recommend trying a larger project such as ECJ.

DEPENDENCIES:
Java 1.6 or above
Apache Maven - for building the project (and automatically gathering required dependencies)

Apache Commons Lang 3
Apache Log4j 1.2.17
JUnit 4.4
Matplotlib

BUILDING:
The easiest way to build this project into an executable jar file is to use Apache Maven:
1. Download and install Apache Maven (http://maven.apache.org/)
2. Navigate to the top-level directory of this project and build with Maven: Execute "mvn package" (Assuming that you have Maven installed correctly and have the correct permissions, this will download all required dependencies and place them into the executable jar file).

RUNNING THE CODE:
After you have built the executable jar file as described above, simply execute the following: 

java -jar target/gp-research-0.1.jar conf/<CONFIG_FILE>.properties

This will use the config file located in conf/<CONFIG_FILE>.properties, and the resulting output will be saved to the directory "output" by default (Specify a path, which can be new if you have the appropriate permissions, at the end of the command to use a different output directory. This is useful when automating execution to perform several independent trials).

NOTE: The configuration files were all taken directly from our experiments, which were run in a high-performance computing environment. You may need to adjust the number of threads (numThreads option in the configuration files) for your system.

GENERATING PLOTS:


