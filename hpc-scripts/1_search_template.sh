#!/bin/bash

#SBATCH -J memCOUNT	# Job Name COUNT will be replaced with the count num
#SBATCH --nodes=1
#SBATCH --cpus-per-task=18 	# number of threads
#SBATCH --mem=32G
#SBATCH --time=01-00:00:00 #

/path/to/java -Xmx100G -cp ../../XiSearch_1.7.6.4.jar rappsilber.applications.Xi --config=../../myconfig.conf --locale=en --peaks=../../peakfiles/PEAKFILEXYZ --fasta=../../database.fasta --output=./OUTPUTNAME.csv