#!/usr/bin/python
import sys
import os
import re
import matplotlib
import matplotlib.pyplot as plt
from matplotlib.lines import Line2D
import numpy as np
import scipy.stats as stats
import bisect


#Matplotlib font stuff (for making figures with legible text)
font = {'size': '9'}
matplotlib.rc('font', **font)

#This script takes a set of directories and gets the average fitness over time
#for each set of runs, including error bars. This works on simple and layered
#population data. In the case of layered, we take the max fitness of all layers
#for each point in time.

#Assumes that all the different runs have the same number of evals, or it was
#stopped after finding the solution!

#Line style/marker pairs for each line we're plotting (Shouldn't ever be more than this!)
LINE_STYLES=("solid", "dotted", "dashed", "solid")
LINE_MARKERS=(None, "o", "s", "^", "v")

#Colors to plot in...
#COLORS = ["b", "g", "r", "c", "m", "y", "k"]
COLORS = ["gray", "black", "gray", "black"]

CUTOFF=10000000

#Convenience method to add the data point to the global collection
def addGlobalData(collection, evals, dataPoint):
	if evals not in collection:
		collection[evals] = [dataPoint]
	else:
		collection[evals].append(dataPoint)

#Convenience method to get the sequence of eval nums so we can fill in runs that
#stopped at the optimal fitness
def getEvalNums():
	evalNums = []
	spaces = np.logspace(3, 7, num=30)
	runEvals = range(1000, 10001000, 1000)
	
	#This is a ton of work, but at least it looks good...
	for i in range(len(spaces)):
		evalNums.append(min(runEvals, key=lambda evals:abs(evals-spaces[i])))
		
	return evalNums

#Convenience method to get all the fitness files
def getFiles(resultsDir):
	allFiles = []
	for dirname, dirs, files in os.walk(resultsDir):
		for fName in files:
			m = re.match("fitness([\d]+.*)", fName)

			if m != None:
				allFiles.append(os.path.join(dirname, fName))

	return allFiles

#Convenience method to setup the fitness collection for the set of runs
#We'll put -1 here to easily catch where to fill in fitness in the case that
#the run stopped after the optimal solution was found.
#This is fine because fitness is assumed to go from 0 to 1
def setupGlobalCollection(collection, evals, numRuns):
	for numEvals in evals:
		collection[numEvals] = [-1.0] * numRuns

def plotFitness(resultsDir, label, lineStyle, lineMarker, lineColor, endingEvals):
	allMaxFitness = {} #evals -> max fitness from each run
	#allAvgFitness = {} #evals -> [average fitness from each run]
	optimalSolnRuns = set() #indices of the runs that found the optimal fitness
	evals = getEvalNums() #All the eval numbers through the cutoff

	#Get the collection of fitness files
	allFiles = getFiles(resultsDir)

	#Setup the allMaxFitness collection
	setupGlobalCollection(allMaxFitness, evals, len(allFiles))

	#Collect all the data
	for run in range(len(allFiles)):
		outFile = open(allFiles[run])
		avgFit = 0.0
		maxFit = 0.0

		for line in outFile:
			data = line.strip().split("\t")
			
			#We have layered population data
			if line.find(":") >= 0:
				numEvals = long(data[1])

				lyrMaxMaxFitness = 0.0 #largest max fitness among layers
				#lyrMaxAvgFitness = 0.0 #largest avg fitness among layers

				for lyrData in data[2:]:
					avgFit, maxFit = [float(item) for item in lyrData.split(":")]

					#Find the largest avg fitness among all layers
					#if avgFit > lyrMaxAvgFitness:
					#	lyrMaxAvgFitness = avgFit

					#Find the largest max fitness among all layers
					if maxFit > lyrMaxMaxFitness:
						lyrMaxMaxFitness = maxFit

				#Was this a successful run?
				if lyrMaxMaxFitness == 1.0:
					optimalSolnRuns.add(run) #let set handle exists()

					#Round up to the next eval mark if stopped on optimal
					#This doesn't alter the data because this is where it would
					#have been recorded next, and it of course would've been 1.0
					if numEvals not in evals:
						numEvals = evals[bisect.bisect_right(evals, numEvals)]
			
				#Add the data to the global collection
				if numEvals <= CUTOFF and numEvals in evals:
					allMaxFitness[numEvals][run] = lyrMaxMaxFitness
					
				#Go ahead and stop early if we've hit the optimal (it gets filled from there)
				if lyrMaxMaxFitness == 1.0:
					break

			#We have Simple population data
			else:
				numEvals = long(data[1])
				maxFit, avgFit = [float(item) for item in data[2:]]

				#Was this a successful run?
				if maxFit == 1.0:
					optimalSolnRuns.add(run) #let set handle exists()

					#Round up to the next eval mark if stopped on optimal
					#This doesn't alter the data because this is where it would
					#have been recorded next, and it of course would've been 1.0
					if numEvals not in evals:
						numEvals = evals[bisect.bisect_right(evals, numEvals)]

				#Add the data to the global collection
				if numEvals <= CUTOFF and numEvals in evals:
					allMaxFitness[numEvals][run] = maxFit

		#Fill in the fitness if the run quit at the optimal fitness!
		for numEvals in evals:
			if run in optimalSolnRuns and allMaxFitness[numEvals][run] == -1.0:
				allMaxFitness[numEvals][run] = 1.0

	#Calculate the averages over all the runs
	for numEvals in evals:
		if -1.0 in allMaxFitness[numEvals]:
			print "GOT -1 AT EVALS %s!!!!!!!" %numEvals
			for i in range(len(allMaxFitness[numEvals])):
				if allMaxFitness[numEvals][i] == -1.0:
					print "\t" + allFiles[i]
	avgBestFitness = [np.average(allMaxFitness[numEvals]) for numEvals in evals]

	#Get the 95% confidence interval for the mean at each eval
	conf = [1.96 * stats.sem(allMaxFitness[numEvals]) for numEvals in evals]

	#Add in error bars using the 95% confidence interval (space them out some)
	width=1.5
	if lineStyle == "dotted":
		width=2.5

	return plt.plot(evals, avgBestFitness, label=label, color=lineColor, ls=lineStyle, lw=width)#, marker=lineMarker, markersize=3.5, markeredgecolor="none")#1.2)

#--------------------------------------		

#Main.
if __name__ == "__main__":
	if len(sys.argv) < 4:
		print "Usage compareFitness.py <INPUT_DIR_LIST (no spaces!)> <LABELS (no spaces!)> <OUTPUT_DIR>"
		print "Example: compareFitness.py dir1,dir2,dir3 label1,label2,label3 outputDir"
		quit()

	if len(sys.argv) == 5:
		CUTOFF = long(sys.argv[4])

	#Directories from which we should get the results
	resultsDirs = [dirname for dirname in sys.argv[1].split(",")]

	labels = [label for label in sys.argv[2].split(",")]

	#Output directory
	outDir = sys.argv[3]

	if not os.path.exists(outDir):
		os.makedirs(outDir)

	
	#keep track of the ending evaluations in each set so we can locally stop at the same point
	endingEvals = []

	#Explicitly set the figure size
	plt.figure(figsize=(3.0,2.5))

	#Plot the fitness data
        plotted = []
	for i in range(len(resultsDirs)):
		plotted.append(plotFitness(resultsDirs[i], labels[i], LINE_STYLES[i], LINE_MARKERS[i], COLORS[i], endingEvals)[0])

	#Add labels, legend, etc and save the figure.
	plt.ylabel("Best Fitness")
	plt.xlabel("Evaluations")
 	plt.ylim(0, 1)
	plt.xscale("log")
	
	plt.legend(plotted, labels, loc=4, numpoints=1, prop={'size':8})
	plt.savefig(os.path.join(outDir, "fitness.eps"), bbox_inches="tight")

	

