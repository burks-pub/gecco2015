#!/usr/bin/python
import sys
import os
import re
import matplotlib.pyplot as plt
import numpy as np
import scipy.stats as stats
from matplotlib.lines import Line2D
import matplotlib

#Colors to plot in...
COLORS = ["b", "g", "r", "c", "m", "y", "k"]
MARKERS = ["o", "s", "^", "v", "o"]

#Location of the legend
LEGEND_LOC=1 #bottom right

#Matplotlib font stuff (for making figures with legible text)
font = {'size': '9'}
matplotlib.rc('font', **font)

#USE TYPE 1 FONTS!!
matplotlib.rcParams['ps.useafm'] = True
matplotlib.rcParams['pdf.use14corefonts'] = True
matplotlib.rcParams['text.usetex'] = True

#Convenience method to make sure the output directory exists
def checkOutDir(outputDir):
		if not os.path.exists(outputDir):
			os.makedirs(outputDir)


#Convenience method to get the tree tag stats files
def getTreeTagStatsFiles(inputDir):
	allFiles = []
	
	for dirname, dirs, filenames in os.walk(inputDir):
		for filename in filenames:
			if not filename.endswith(".png") and re.match("treeTags.+", filename) != None:	
				allFiles.append(os.path.join(dirname, filename))
				
	return allFiles


#Convenience method to tally the total tags at each level for each run
def collectGlobalTagInfo(collection, gen, tagLevel, runNum, totalRuns):
	if tagLevel <=4:
		#Do we have any data for this tagLevel?
		if tagLevel not in collection:
			collection[tagLevel] = {}
		
		#Do we have any data for this generation?
		if gen not in collection[tagLevel]:
			collection[tagLevel][gen] = [0.0] * totalRuns
	
		#Increment the count for the current run
		collection[tagLevel][gen][runNum-1] += 1.0


#Convenience method to collect the max-density/fitness data for each tag level for each run
#for the line plots. It's easier to do the line and scatter separately.
#Data should be in the form: (tagLevel, density, avg. fitness)
def collectGlobalTagData(collection, gen, data, runNum, totalRuns):
	tagLevel = int(data[0])

	if tagLevel <= 4:
		#Do we have any data for this tag level?
		if tagLevel not in collection:
			collection[tagLevel] = {}
	
		#Do we have any data for this generation?
		if gen not in collection[tagLevel]:
			collection[tagLevel][gen] = [(0.0,0.0)] * totalRuns
	
		#Add the entry if this is the first time around
		if collection[tagLevel][gen][runNum-1] == (0.0, 0.0):
			densityFitness = (data[1], data[2])
			collection[tagLevel][gen][runNum-1] = densityFitness
	
		#Otherwise, update the entry for this generation if necessary
		else:
			#Get the data so we can easily compare what we have so far
			currMaxDensity, currFitness = collection[tagLevel][gen][runNum-1]
		
			#if the density is greater, update it
			if data[1] > currMaxDensity:
				collection[tagLevel][gen][runNum-1] = (data[1], data[2])
		
			#Otherwise if density is equal and fitness is greater, update it
			elif data[1] == currMaxDensity and data[2] > currFitness:
				collection[tagLevel][gen][runNum-1] = (data[1], data[2])

#Plots a line of the density of the most dense tree tags over time
#If multiple tree tags have the same density, we take the one with the highest avg. fitness
#Plots represent the average max density at each level
def plotDensityLines(inputDir, outputDir):
	allData = {} #tagLevel -> generation -> [(density, fitness) for top tag] for each run
	
	#Explicitly set the figure size
	plt.figure(figsize=(3.0,2.5))	

	#Make sure the output dir exists
	checkOutDir(outputDir)
	
	#Gather all the data
	runNum = 1
	files = getTreeTagStatsFiles(inputDir)
	
	for filename in files:
		for line in open(filename, "r"):
			items = line.split("\t")
			gen = long(items[0])
		
			#Update the max density/fitness entry for each tag level
			for tagInfo in items[1:]:
				data = [float(d) for d in tagInfo.split(":")]
				collectGlobalTagData(allData, gen, data, runNum, len(files))
				
			
		#Increment the total runs
		runNum += 1
		
	#Make sure the output dir exists
	checkOutDir(outputDir)

	#Grab the tag levels for convenience
	tagLevels = sorted(allData.keys())
	
	i = 0
	
	for tagLevel in tagLevels:
		gens = sorted(allData[tagLevel].keys())
				
		#Transform the data in place to get the averages from their lists
		for gen in gens:
			allData[tagLevel][gen] = (np.average([data[0] for data in allData[tagLevel][gen]]), \
				np.average([data[1] for data in allData[tagLevel][gen]]))

		#Now plot the data in a multi-series (each tag level)
		fill = "full"
		edge = "none"
		
		if i == len(MARKERS)-1:
			fill = "none"
			edge = "black"
			

		plt.plot(gens, [allData[tagLevels[tagLevel]][gen][0] for gen in gens], linestyle="none", marker=MARKERS[i], markevery=3, markeredgecolor=edge, markersize=3.5, label=r'$L_{%s:%s}$' %(tagLevel, tagLevel+1), color="black", fillstyle=fill)
		i += 1
	
	
	plt.xlabel("Generations")
	plt.ylabel("Max. Density")
	plt.ylim(0, 1)
	plt.xlim(0, 1000)
	plt.legend(loc=LEGEND_LOC, prop={'size':10}, numpoints=1, ncol=2)
	
	plt.savefig(os.path.join(outputDir, "density.eps"), bbox_inches="tight")
	plt.clf()		

#------------------ MAIN ---------------------------
if __name__ == "__main__":
	if len(sys.argv) != 4:
		print "Usage treeTagPlots.py <INPUT_DIR> <LEGEND_LOC [0,10]> <OUTPUT_DIR>"
		print "Example: treeTagPlots.py inputDir 1 outputDir"
		quit()
		
	#Grab the command line arguments
	inputDir = sys.argv[1]
	LEGEND_LOC = int(sys.argv[2])
	outputDir= sys.argv[3]

	#Plot the total tags stats
	plotDensityLines(inputDir, outputDir)

