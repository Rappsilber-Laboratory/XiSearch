## xiSEARCH 1.7.6.7 documentation


XiSearch is implemented as a Java Application. Therefore it requires that java is installed. We recommend to use the the latest update of JAVA version 8 or above. 
The latest version as binary can be downloaded from https://rappsilberlab.org/downloads/

### Background
xiSEARCH is a search engine for crosslinking mass spectrometry (crosslinking MS). It supports data acquired with ThermoFisher Orbitrap instruments (.raw format) that have been converted to peak. files (.mgf format), for example with [ProteoWizard MsConvert](https://proteowizard.sourceforge.io/). It then searches the peakfiles against a sequence database in .fasta format to identify crosslinked peptide pairs from mass spectra. The search algorithm uses a target-decoy approach outlined in XXX, which enables false-discovery rate (FDR) estimation. The FDR calculation on the xiSEARCH result is performed by xiFDR.

xiSEARCH is a flexible search engine that allows for extensive configuration of the search options and of the search scoring methods in crosslink identification. Nevertheless, its design suits best data acquired at high resolution in MS1 and MS2- in the Rappsilber lab, we acquire with 120k resolution in ms1 and 60k resolution ms2. Currently, xiSEARCH does not support MS3 approaches.

The xiSEARCH algorithm is described in detail in [Mendez, Fischer *et al.* Mol. Sys. Bio. 2019](https://www.embopress.org/doi/full/10.15252/msb.20198994). The xiSEARCH scoring function is made up of several terms accounting for the goodness of fit of the spectra to the peptide pair selected from the database, including fragment mass error, percentage intensity explained, number of fragments, number of crosslinked fragments. 
The scoring function is applied to explain each spectrum with a target-target pair, a target-decoy pair, a decoy-decoy pair, a linear target sequence or a linear decoy sequence. Error control by false discovery rate estimation is then performed in a separate step with xiFDR.



### The interface
the interface provides several tabs to configure your search. In the first tab (Run) the result file (result) and optionally a for writing out full annotations of the spectra (peak annotations) can be defined.

In the second tab (Peak Lists) the peak lists to be searched can be defined. Supported formats are mgf-files as generated from msconvert or apl-files as produced by MaxQuant. For mgf-files of other sources the config might need to be adapted to define how to read out a run-name and a scan-number from the TITLE= tags in the files.

The third tab defines the FASTA files to be searched. Multiple files can be defined. 

The fourth tab provides the search configuration. Here all parameters are defined as a in form of a text file. A default config is provided as an example that defines a search with BS3 as cross-linker and Carboxyamidomethylation on Cystein as fixed modification and oxidation of methionine as variable modification. Each option contains a short description of what it does and how to change it.

When everything is configured as desired then start-button on the first tab can be pressed to start the search.

Depending on the size of the sequence-database and the number of search threads the start-script might need to be adapted to permit xi to use a larger amount of memory (-Xmx option). This should not exceed the amount of free memory available without running xi. E.g. if a computer has 8GB of RAM but per default 5 are used by other programs then Xi should get at most 3GB as otherwise part of the programm will be swapped out to disk and in effect run extremely slow. For searches involving dozens of spectra and hundreds of proteins, we recommend running xiSEARCH on an HPC node with large Xmx values or a server, as the RAM requirements increase with the square of the size of the database. As an example, we run searches for [this publication](https://pubs.acs.org/doi/full/10.1021/acs.jproteome.9b00541) with -Xmx 256G, specifying 256Gb of RAM.

### Setting up a search in the interface

1. Upload files

The "files" tab allows for the upload of the mass spec data in .mgf format and the database in .fasta format. The path to where the results of the search are written also needs to be set. The decoy database is automatically generated from the uploaded .fasta fikes. However, the user has the liberty to define a custom decoy database by designing one of the fasta files as the decoy database instead.


2. Setting options - basic config.

We will cover the advanced config in the "setting up a search on the command line" section.

##### crosslinker selection
Normally, all searches are performed with 2 crosslinkers selected: the crosslinker used in the sample (be it BS3, DSS, SDA or other) and "NonCovalent", which allows the search engine to match spectra with a pair of co-eluting and co-fragmenting linear peptides, which is a common source of misinterpretation of crosslinking MS spectra. Thus, the "multiple" crosslinker box should be ticked and then both the crosslinker of interest and "nonCovalent" (near the bottom) should be selected.

##### crosslinker selection - presets
"Large scale" presets refer to search parameters optimised for searches with lots (>100) of proteins in the database. They search for crosslinker modifications (amidated, hydrolysed, crosslinks within a peptide) only on linear peptides rather than peptide pairs. For crosslinkers using NHS-ester chemistry (DSS, BS3, DSBU, BS2G), S/T/Y is considered a side reaction and a score penalty is applied to the match relative to matching spectra crosslinked to K or Nterm.

| Preset      | Description | 
| ----------- | ----------- | 
| BS2G (Large Scale)      | BS2G crosslinker, K/S/T/Y/Nterm to  K/S/T/Y/Nterm           |
| BS2G (Small Scale)    | BS2G crosslinker, K/S/T/Y/Nterm to  K/S/T/Y/Nterm            |
| SDA    | SDA crosslinker, K/S/T/Y/Nterm to any amino acid       |
| BS3 (Large Scale)    | BS3 crosslinker, K/S/T/Y/Nterm to  K/S/T/Y/Nterm. Also for DSS.    |
| BS3 (Small Scale)    |  BS3 crosslinker, K/S/T/Y/Nterm to  K/S/T/Y/Nterm. Also for DSS. |
| DSSO (Large Scale)    | DSSO crosslinker,  K/S/T/Y/Nterm to  K/S/T/Y/Nterm  with cleavable stub fragment identification      |
| DSSO (Small Scale)    | DSSO crosslinker, K/S/T/Y/Nterm to  K/S/T/Y/Nterm with cleavable stub fragment        |
| EDC    | EDC crosslinker, K/S/T/Y, Nterm to E/D/Cterm. No modifications defined.       |
| DSBU (Large Scale)    | DSBU crosslinker, K/S/T/Y/Nterm with cleavable stun fragment   |
| DSBU (Small Scale)    | DSBU crosslinker, K/S/T/Y/Nterm with cleavable stun fragment        |
| NonCovalent    | Include noncovalent identification in search    |
| Linear Search    | perform linear peptide search ONLY (overrides all other options)        |


##### Tolerance 
Set the MS1 and MS2 search tolerances. If you are working with high-resolution orbitrap (120K MS1 and 60k MS2) data that has been previously recalibrated with msfragger or a linear search, we suggest very tight tolerances such as 3ppm MS1 and 5ppm MS2. Non-recalibrated data is usually searched with looser tolerances such as 6ppm MS1 and 10ppm MS2. Notice that xiSEARCH does not perform recalibration. Thus, some information on MS1 and MS2 error from quality control runs or linear proteomic searches of the same samples is necessary to set sensible tolerances.


#### Enzyme
Select an enzyme or multiple enzyme used to digest the sample. 

| Preset      | Description | 
| ----------- | ----------- | 
| BS2G (Large Scale)      | BS2G crosslinker, K/S/T/Y/Nterm to  K/S/T/Y/Nterm           |

#### Miscleavages
The number of miscleavages to consider in the search. Given that crosslinked peptides generate spectra that are similar to spectra of long, miscleaved linear peptides, we suggest setting this number to 3, or even 4 if the database and set of modifications included in the search is small. This allows for alternative explanations of crosslinked spectra with miscleaved peptides.

#### Number of Threads
Number of threads to be used for the search. The memory usage scales with the number of threads. If the program runs out of memory, consider re-launching xiSEARCH with increased memory via the -Xmx option (see above).

#### Modifications
Modifications are considered in 3 flavours:

1) Fixed: occurring on every instance of a residue
2) Variable: may or may not be present on a residue
3) Variable - linear peptides: may or may not be present on a residue, but will only be considered to explain spectra of non-crosslinked peptides. This option is used to simplify the search problem in searches with large (hundreds of proteins) databases.

In the modifications tab, it is important to select the appropriate crosslinker modifications for your sample. In particular, loop modifications and hydrolysed crosslinkers are very common for both NHS-ester and diazirine crosslinkers. If a modification is selected in variable or variable-linear, it should not be selected in the other tab. Remember that the search problem (and therefore the memory and time necessary for the search) scales exponentially with the number of proteins in the database and their modifications.

#### Ions
Which fragment ion types to consider in the search.

#### Losses
Which fragment losses to consider in the search.

### Custom config
Here, additional configurations may be set using the text syntax as in the advanced config or in a config file used by the command line version of xiSEARCH (see next section)




### Setting up a search on the command line or in the advanced config interface
The whole configuration of the search in the graphical interface may be set up as a configuration file ("config file") containning all the options. This may be accessed by the "advanced config" tab


### FDR calculation (xiFDR)
If the "Do FDR" box is ticked, xiFDR will automatically be run at the end of xiSEARCH. We tend to leave this option off, as we prefer to run xiFDR in a standalone process to have access to more advanced FDR filtering options.

### Config options
Here, we detail the syntax for setting up config options in xiSEARCH, i.e. the backend of all the presets and options present in the graphical interface. This allows far more flexibility and is recommended for advanced users.


### changelog

