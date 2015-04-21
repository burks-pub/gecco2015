#!/usr/bin/python
import sys
import os
import re
import matplotlib
import matplotlib.pyplot as plt
from matplotlib.lines import Line2D
import numpy as np
import scipy.stats as stats


#Matplotlib font stuff (for making figures with legible text)
font = {'size': '9'}
matplotlib.rc('font', **font)

#This script takes a set of directories and gets the average ending evaluation#
#including error bars for runs that found the optimal solution. This works on
#simple and layered population data. In the case of layered, we take the max
#fitness of all layers for each point in time.

#Colors to plot in...
COLORS = ["#336699", "#993300", "#999933", "#666699", "#CC9933", "#006666", "#99CCFF"]


#Plots the average convergence evals in a bar chart for each of the algorithms
#Also returns the list of ending evals for each algorithm so we can do stats tests
def plotConvergence(resultsDirs, outDir, labels):
    #Make sure the output directory exists
	if not os.path.exists(outDir):
		os.makedirs(outDir)

	allEndingEvals = [] #[list of ending evals for each successful run in each dir]

	for i in range(len(resultsDirs)):
		resultsDir = resultsDirs[i]
		label = labels[i]
		endingEvals = []

		for dirname, dirs, files in os.walk(resultsDir):
			for fName in files:
				m = re.match("fitness([\d]+.*)", fName)

				if m != None:
					outFile = open(os.path.join(dirname, fName))
					foundOptimal=False

					#Go line-by-line and get the earliest perfect fitness
					for line in outFile:
						if not foundOptimal:
							data = line.strip().split("\t")

							#We have layered population data
							if line.find(":") >= 0:
								numEvals = long(data[1])

								start = True

								for lyrData in data[2:]:
									avgFit, maxFit = [float(item) for item in lyrData.split(":")]

									#See if the max fitness is 1.0
									if maxFit == 1.0:
										endingEvals.append(numEvals)
										foundOptimal = True
										break

							#We have Simple population data
							else:
								numEvals = long(data[1])

								maxFit, avgFit = [float(item) for item in data[2:]]

								#See if the max fitness is 1.0
								if maxFit == 1.0:
									endingEvals.append(numEvals)
									foundOptimal = True
									break

							#Stop reading the file once we've found the optimal
							if foundOptimal:
								break

		if len(endingEvals) > 0:
			allEndingEvals.append(endingEvals)

	#Get the averages and 95% confidence intervals
	averages = []
	cis = []
	for endingEvals in allEndingEvals:
		averages.append(np.average(endingEvals))
		cis.append(1.96 * stats.sem(endingEvals))

	#Plot the data
	numAlgs = len(allEndingEvals)
	ind = np.arange(numAlgs)
	xTicks = [i + .3 for i in range(numAlgs)]
	
        #Explicitly set the figure size
        plt.figure(figsize=(3.0,2.0))
	plt.bar(xTicks, averages, .5,
		         color= "grey", yerr=cis, ecolor="black", align="center")


	plt.xticks(xTicks, labels)
	plt.ticklabel_format(style='sci', axis='y', scilimits=(0,0))
	plt.ylabel('Mean Evaluations')
	plt.savefig(os.path.join(outDir, "convergence.eps"), bbox_inches="tight")

	#Return the collection of all ending evals
	return allEndingEvals

#Does pairwise Mann-Whitney U tests for the ending evals for all experiment sets
#Writes the results in a table (matrix) form to outDir/convergencePVals.txt
#Also writes out all the averages to outDir/convergenceAverages.txt
def doStatsTests(allEndingEvals, outDir, labels):
	#We need at least 2 sets to comparedd
	if len(allEndingEvals) >= 1:
		#Setup the output files
		pValsFile = open("%s/convergencePVals.txt" %outDir, "w")
		avgFile = open("%s/convergenceAverages.txt" %outDir, "w")

		#Holds the "table" to print out to file
		allPVals = []

		#Do the pairwise comparisons
		for i in range(len(allEndingEvals)-1):
			#Calculate and write the average
			avgEndingEvals = np.average(allEndingEvals[i])
			avgFile.write("%s\t%s\n" %(labels[i], avgEndingEvals))
			
			pVals = []
			for j in range(len(allEndingEvals)):
				if j > i: #only fill in the upper diag (lower is the same...)
					zStat, pVal = stats.ranksums(allEndingEvals[i], allEndingEvals[j])
					pVals.append("%.5f" %pVal)
				else:
					pVals.append("--")
			allPVals.append(pVals)

		#Add a header row of labels
		pValsFile.write("\t" + "\t".join(labels) + "\n")

		for i in range(len(allPVals)):
			pValsFile.write(labels[i] + "\t" + "\t".join(allPVals[i]) + "\n")
			
		#Calculate and write the average for the last alg
		avgEndingEvals = np.average(allEndingEvals[-1])
		avgFile.write("%s\t%s\n" %(labels[-1], avgEndingEvals))		

		#Done!
		pValsFile.close()
		avgFile.close()




#-------------- MAIN ------------------------

if __name__ == "__main__":
	if len(sys.argv) != 4:
		print "Usage convergence.py <INPUT_DIR_LIST (no spaces!)> <LABELS (no spaces!)> <OUTPUT_DIR>"
		print "Example: convergence.py dir1,dir2,dir3 label1,label2,label3 outputDir"
		quit()

	#Directories from which we should get the results
	resultsDirs = [dirname for dirname in sys.argv[1].split(",")]

	labels = [label for label in sys.argv[2].split(",")]

	#Output directory
	outDir = sys.argv[3]

	#Plot the convergence times
	allEndingEvals = plotConvergence(resultsDirs, outDir, labels)

	#Do pairwise Mann-Whitney U tests on each algorithm
	if len(allEndingEvals) >= 1:
		doStatsTests(allEndingEvals, outDir, labels)








