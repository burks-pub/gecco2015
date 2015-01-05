#!/usr/bin/python
import sys
import os
import re
import scipy.stats as stats

#Regex to match for the end of run line in the log file
END_RUN_MATCH  = "overall="

#Regex to match for the successful (end of) runs in the log file
SUCCESS_MATCH = "overall=1.0"

#Gets the success rates for each run, for each input dir (2d list total sucessful and total unsuccessful)
#Also prints out the success rate (percentage) to outDir/successRates.txt
def getSuccessRates(inDirs, outDir, labels):
	#[[successful, unsuccessful]...]
	allOutcomes = []

	#Make sure the output directory exists
	if not os.path.exists(outDir):
		os.makedirs(outDir)

	#Setup the output file
	outFile = open(os.path.join(outDir, "successRates.txt"), "w")

	#Now get the sucessRates
	for i in range(len(inDirs)):
		inDir = inDirs[i]
		totalSuccessful = 0.0
		totalRuns = 0.0

		for dirname, dirs, filenames in os.walk(inDir):
			for fName in filenames:
				if re.match("out-[\d]+\\.log", fName) != None:

					for line in open(os.path.join(inDir, fName), "r"):
						if line.find(END_RUN_MATCH) >= 0:
							totalRuns += 1.0

							if line.find(SUCCESS_MATCH) >= 0:
								totalSuccessful += 1.0

		#Add the outcome of this input dir (experiment set)
		allOutcomes.append([int(totalSuccessful), int(totalRuns - totalSuccessful)])

		#Now get the percentage of successful runs
		successRate = totalSuccessful/totalRuns
		successRate *= 100

		#Write the success rate to the output file
		outFile.write("%s\t%s%%\n" %(labels[i], successRate))
	outFile.close()

	#Return the outcomes
	return allOutcomes


#Does pairwise Fisher's Exact test for all the outcomes of all experiment sets
#Writes the results in a table (matrix) form to outDir/pVals.txt
def doStatsTests(allOutcomes, labels, outDir):
	#We need at least 2 sets to compare
	if len(allOutcomes) > 1:
		#Setup the output file
		outFile = open("%s/successPVals.txt" %outDir, "w")

		#Holds the "table" to print out to file
		allPVals = []

		#Do the pairwise comparisons
		for i in range(len(allOutcomes)-1): #We don't need the last row
			pVals = []
			for j in range(len(allOutcomes)):
				if j > i: #only fill in the upper diag (lower is the same...)
					oddsRatio, pVal = stats.fisher_exact([allOutcomes[i], allOutcomes[j]])
					pVals.append("%.5f" %pVal)

				else:
					pVals.append("--")
			allPVals.append(pVals)

		
		#Add a header row of labels
		outFile.write("\t" + "\t".join(labels) + "\n")

		for i in range(len(allPVals)):
			outFile.write(labels[i] + "\t" + "\t".join(allPVals[i]) + "\n")


		#Done!
		outFile.close()	
			
#------------------------- MAIN ---------------------------------
if __name__ == "__main__":
	#Check arguments
	if len(sys.argv) < 4:
		print "Usage: python successRates.py <INPUT_DIRS" +\
			" (comma-sep no spaces unless quoted)> <LABELS (comma-sep no spaces unless quoted)> <OUTPUT_DIR>"
		quit()

	inDirs = sys.argv[1].split(",")
	labels = sys.argv[2].split(",")
	outDir = sys.argv[3]

	#Get the outcomes and print the success rates
	allOutcomes = getSuccessRates(inDirs, outDir, labels)

	#Get the pairwise Fisher's Exact Test p-values if we have more than 2 sets
	if len(allOutcomes) > 1:
		doStatsTests(allOutcomes, labels, outDir)



	

