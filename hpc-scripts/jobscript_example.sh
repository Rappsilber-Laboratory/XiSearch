#!/bin/bash
#SBATCH --job-name=xiSEARCH
#SBATCH --mail-type=NONE
#SBATCH --partition=cpuq
#SBATCH --time=04:00:00
#SBATCH --nodes=1
#SBATCH --cpus-per-task=20
#SBATCH --mem 64G

/path/to/java -Xmx64G -cp xiSEARCH_1.7.6.7/xiSEARCH.jar rappsilber.applications.Xi --fasta=sequence_database.fasta --peaks=./peakfiles.zip --config=cofig.conf --output=output_name.csv
