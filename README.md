
xiSEARCH can be downloaded **[here](https://www.rappsilberlab.org/software/xisearch/)**. The application can then be run by clicking on "startXiWindows", "startXiUnix" or "startMacOS".

xiSEARCH is a search engine for the identification of crosslinked spectra matches in crosslinking mass spectrometry experiments. It is implemented as a Java Application. We recommend to use the latest update of JAVA version 8 or above.


For questions regarding usage of xiSEARCH, you can open a discussion [here](https://github.com/Rappsilber-Laboratory/XiSearch/discussions).

When using xiSEARCH, please cite [Mendez, Fischer *et al.* Mol. Sys. Bio. 2019](https://www.embopress.org/doi/full/10.15252/msb.20198994).


Table of contents

<!-- TOC start (generated with https://github.com/derlin/bitdowntoc) -->

- [Background](#background)
- [Getting started](#getting-started)
  * [Setting up a search in the interface](#setting-up-a-search-in-the-interface)
    + [The interface](#the-interface)
      - [Allocating memory](#allocating-memory)
    + [The files tab](#the-files-tab)
    + [The parameters tab](#the-parameters-tab)
      - [Basic Config](#basic-config)
        * [crosslinker selection](#crosslinker-selection)
        * [crosslinker selection - presets](#crosslinker-selection-presets)
        * [Tolerance ](#tolerance)
        * [Enzyme](#enzyme)
        * [Miscleavages](#miscleavages)
        * [Number of Threads](#number-of-threads)
        * [Modifications](#modifications)
        * [Ions](#ions)
        * [Losses](#losses)
        * [Custom config](#custom-config)
    + [Do FDR setting ](#do-fdr-setting)
    + [Start search](#start-search)
- [Setting up a search in the advanced interface and editing config files](#setting-up-a-search-in-the-advanced-interface-and-editing-config-files)
    + [Full options for configuration in text config](#full-options-for-configuration-in-text-config)
      - [Search settings ](#search-settings)
      - [Scoring settings](#scoring-settings)
      - [Protease settings](#protease-settings)
      - [Crosslinker settings](#crosslinker-settings)
      - [Modification settings](#modification-settings)
        * [Legacy modification nomenclature](#legacy-modification-nomenclature)
      - [Losses settings](#losses-settings)
      - [Changing or adding new entries to the graphical config interface](#changing-or-adding-new-entries-to-the-graphical-config-interface)
  * [running xiSEARCH from command line and on a high performance computing (HPC) cluster](#running-xisearch-from-command-line-and-on-a-high-performance-computing-hpc-cluster)
  * [Additional utilities](#additional-utilities)
      - [mgf file filtering](#mgf-file-filtering)
      - [theoretical spectra of crosslinked peptides](#theoretical-spectra-of-crosslinked-peptides)
      - [Diagnostic ion mining and MS1 features](#diagnostic-ion-mining-and-ms1-features)
      - [Sequence tools](#sequence-tools)
      - [Skyline spectral library generation](#skyline-spectral-library-generation)

<!-- TOC end -->

### Background
xiSEARCH is a search engine for crosslinking mass spectrometry (crosslinking MS). It is mainly tested with data acquired with ThermoFisher Orbitrap instruments (.raw format) that have been converted to peak files (.mgf format), for example with [ProteoWizard MsConvert](https://proteowizard.sourceforge.io/) and recalibrated using our [preprocessing pipeline](https://github.com/Rappsilber-Laboratory/preprocessing)- but any high-resolution data in MGF format or MaxQuant APL format are likely to be usable. It then searches the peakfiles against a sequence database in .fasta format to identify crosslinked peptide pairs from mass spectra. 

The search algorithm uses a target-decoy approach outlined in  [Fischer _et al._ 2017](https://doi.org/10.1021/acs.analchem.6b03745) and [Fischer _et al._ 2018]([url](https://doi.org/10.1371%2Fjournal.pone.0196672)), which enables false-discovery rate (FDR) estimation. The FDR calculation on the xiSEARCH result is performed by [xiFDR](https://github.com/Rappsilber-Laboratory/xiFDR).

xiSEARCH is a flexible search engine that allows for extensive configuration of the search options and of the search scoring methods in crosslink identification. Nevertheless, its design suits best data acquired at high resolution in MS1 and MS2 - in the Rappsilber lab, we acquire with 120k resolution in ms1 and 60k resolution ms2. Currently, xiSEARCH does not support MS3 approaches.

The xiSEARCH algorithm is described in detail in [Mendez, Fischer *et al.* Mol. Sys. Bio. 2019](https://www.embopress.org/doi/full/10.15252/msb.20198994). The xiSEARCH scoring function is made up of several terms accounting for the goodness of fit of the spectra to the peptide pair selected from the database, including fragment mass error, percentage intensity explained, number of fragments, number of crosslinked fragments.

![xi_search_strategy](https://user-images.githubusercontent.com/6330440/202421641-69fa3021-f53e-4eb1-9923-c9c740aca0e2.jpg)

Scoring happens in three stages: 

1. alpha candidates are selected and scored
2. top n alpha candidates are taken and all matching beta-candidates (according to the precursor mass) will be selected and prescored as pairs
3. the top X of these are then fully matched and scored

The scoring function is applied to explain each spectrum without considering if a peptide is target or decoy. The resulting chances for a false positive match to to be a target-target, target-decoy or a decoy-decoy match should be 1:2:1.  Error control by false discovery rate estimation is then performed in a separate step with xiFDR.

# Getting started

## Setting up a search in the interface

1. Upload files in the "files" tab
2. Edit options in "parameters" tab
3. Press "Start" in "parameters" tab to start search.


### The interface

Launch the xiSEARCH interface using "startXiWindows.bat", "startMacOS.command", or "startXiUnix.sh", depending on your operating system. If you need to allocate more memory to xiSEARCH, open the launch file as text and edit the -XmX parameter (see below).

The interface provides several tabs. The first two tabs are the main ones for configuring the search. The first one (Files) defines the input and output, i.e. the peaklist and fasta files to be search and where to write the result. The second one (Parameters) configures the actual search. The third one (Feedback) provides the log of the current search and provides a means to contact the developers. The fourth tab contains the change log/version history. 

#### Allocating memory
Depending on the size of the sequence database and the number of search threads the start script ("startXiWindows", "startXiUnix" or "startMacOS") might need to be adapted to permit xiSEARCH to use a larger amount of memory (-Xmx option). This should not exceed the amount of free memory available without running xiSEARCH. E.g. if a computer has 8GB of RAM but per default 5 are used by other programs then xiSEARCH should get at most 3GB as otherwise part of the program will be swapped out to disk and in effect run extremely slow. For searches involving dozens of peak files and hundreds of proteins, we recommend running xiSEARCH on an HPC node with large Xmx values or on a server. This is because the RAM requirements increase with the square of the size of the database. As an example, we ran searches for [this publication](https://pubs.acs.org/doi/full/10.1021/acs.jproteome.9b00541) with the -Xmx option in the launch script edited to:

    -Xmx256G

specifying 256Gb of RAM.

### The files tab
The "files" tab allows for the upload of the mass spec data in .mgf format and the database in .fasta format. The path to where the results of the search are written also needs to be set. The decoy database is automatically generated from the uploaded .fasta files. 

The user has the liberty to define a custom decoy database by marking one of the FASTA files as the decoy database instead. If a FASTA file is marked as decoy - no additional decoys will be auto-generated. For the correct estimate of FDR for self links and heteromeric links proteins in the target and the decoy database  need to  match each other by having the same accession - just prepending a REV_ for the decoy proteins

### The parameters tab
#### Basic Config
In this view, the user selects the crosslinker, protease, error tolerances, miscleavages, modifications to be considered and number of threads to be assigned for the search.

This section covers setting up a search in the graphical interface in "basic config" mode, with selection of options.
The advanced config and editing of config files is covered [below](#Setting up a search in the advanced interface and editing config files).

##### crosslinker selection
Normally, all searches are performed with 2 crosslinkers selected: the crosslinker used in the sample (be it BS3, DSS, SDA or other) and "NonCovalent", which allows the search engine to match spectra with a pair of co-eluting and co-fragmenting linear peptides that are not actually crosslinked. This is a common source of misinterpretation of crosslinking MS spectra [ref](https://pubs.acs.org/doi/10.1021/acs.analchem.8b04037). Thus, the "multiple" crosslinker box should be ticked and then both the crosslinker of interest and "nonCovalent" (near the bottom) should be selected.

##### crosslinker selection - presets
"Large scale" presets refer to search parameters optimised for searches with lots (>100) of proteins in the database. They search for crosslinker modifications (amidated, hydrolysed, crosslinks within a peptide) only on linear peptides rather than peptide pairs. For crosslinkers using NHS-ester chemistry (DSS, BS3, DSBU, BS2G), S/T/Y is considered a side reaction and a score penalty is applied to the match relative to matching spectra crosslinked to K or Nterm.

| Preset      | Description                      | 
| ----------- |--------------------------------------------------------------------------------------------------| 
| BS2G (Large Scale)      | BS2G crosslinker, K/S/T/Y/Nterm to  K/S/T/Y/Nterm                |
| BS2G (Small Scale)    | BS2G crosslinker, K/S/T/Y/Nterm to  K/S/T/Y/Nterm                |
| SDA    | SDA crosslinker, K/S/T/Y/Nterm to any amino acid                 |
| BS3 (Large Scale)    | BS3 crosslinker, K/S/T/Y/Nterm to  K/S/T/Y/Nterm. Also for DSS.  |
| BS3 (Small Scale)    | BS3 crosslinker, K/S/T/Y/Nterm to  K/S/T/Y/Nterm. Also for DSS.  |
| DSSO (Large Scale)    | DSSO crosslinker,  K/S/T/Y/Nterm to  K/S/T/Y/Nterm  with cleavable stub fragment identification  |
| DSSO (Small Scale)    | DSSO crosslinker, K/S/T/Y/Nterm to  K/S/T/Y/Nterm with cleavable stub fragment                   |
| EDC    | EDC crosslinker, K/S/T/Y, Nterm to E/D/Cterm. No modifications defined.   |
| DSBU (Large Scale)    | DSBU crosslinker, K/S/T/Y/Nterm with cleavable stub fragment     |
| DSBU (Small Scale)    | DSBU crosslinker, K/S/T/Y/Nterm with cleavable stub fragment        |
| NonCovalent    | Include noncovalent identification in search                     |
| Linear Search    | perform linear peptide search ONLY (overrides all other options) |


##### Tolerance 
Set the MS1 and MS2 search tolerances. If you are working with high-resolution orbitrap (120K MS1 and 60k MS2) data that has been previously recalibrated with msfragger or a linear search, we suggest very tight tolerances such as 3ppm MS1 and 5ppm MS2. Non-recalibrated data is usually searched with looser tolerances such as 6ppm MS1 and 10ppm MS2, but this depends on your average mass error, which you can check with a regular proteomic search. Notice that xiSEARCH does not perform recalibration by itself, but spectra may be recalibrated prior to xiSEARCH with our [preprocessing pipeilne](https://github.com/Rappsilber-Laboratory/preprocessing). Thus, some information on MS1 and MS2 error from quality control runs or linear proteomic searches of the same samples is necessary to set sensible tolerances.


##### Enzyme
Select an enzyme or multiple enzyme used to digest the sample. 

| Preset                   | Description      | 
|--------------------------|-----------------------------------------| 
| Trypsin                  |         |
| Trypsin/P                | trypsin not restricted by proline       |
| V8|         |
| Lys-C                    |         |
| Lys-C/P                  |         |
| trypsin/P + V8           |         |
| proteinaseK              |         |
| proteinaseK & trypsin\P  |         |
| Chymotrypsin             |         |
| Trypsin+Chymotrypsin     |         |
| Trypsin/P + ASP-N (D)    |         |
| Asp-N(DE)                |         |
| Trypsin/P+ASP-N(DE)      |         |
| Trypsin/P+ASP-N(E)       |         |
| Elastase                 |         |
| Elastase & Trypsin       |         |
| Trypsin/P & Exopeptidase |         |
| Tryp-N                   |         |
| No digestion             | for example used for synthetic peptides |



##### Miscleavages
The number of miscleavages to consider in the search. Given that crosslinked peptides generate spectra that are similar to spectra of long, miscleaved linear peptides, we suggest setting this number to 3, or even 4 if the database and set of modifications included in the search is small. This allows for alternative explanations of crosslinked spectra with miscleaved peptides.

##### Number of Threads
Number of threads to be used for the search. The memory usage scales with the number of threads. If the program runs out of memory, consider re-launching xiSEARCH with increased memory via the -Xmx option (see above) and/or reduce the number of threads.

##### Modifications
Modifications are considered in 3 flavours:

1) Fixed: occurring on every instance of a residue
2) Variable: may or may not be present on a residue
3) Variable - linear peptides: may or may not be present on a residue, but will only be considered to explain spectra of non-crosslinked peptides. This option is used to simplify the search problem in searches with large (hundreds of proteins) databases.

In the modifications tab, it is important to select the appropriate crosslinker modifications for your sample. In particular, loop modifications and hydrolysed crosslinkers are very common for both NHS-ester and diazirine crosslinkers. If a modification is selected in variable or variable-linear, it should not be selected in the other tab. Remember that the search problem (and therefore the memory and time necessary for the search) scales exponentially with the number of proteins in the database and their modifications.

##### Ions
Which fragment ion types to consider in the search.

##### Losses
Which fragment losses to consider in the search.

##### Custom config
Here, additional configurations may be set using the text syntax as in the advanced config or in a config file used by the command line version of xiSEARCH (see next section)

### Do FDR setting 
If the "Do FDR" box is ticked, xiFDR will automatically be run at the end of xiSEARCH. We tend to leave this option off, as we prefer to run [xiFDR](https://github.com/Rappsilber-Laboratory/xiFDR) in a stand-alone process to have access to more advanced FDR filtering options. Alternative is to tick both the "Do FDR" and the "GUI" checkbox. This will start xiFDR for the generated output directly - but as an independent GUI, where you can then define all parameters. It will be preconfigured with the right input files and these can then be read in by pressing the "read" button.

### Start search

Press "Start" to start the search.

# Setting up a search in the advanced interface and editing config files
The whole configuration of the search in the graphical interface may be set up as a configuration file ("config file") containing all the options. This may be accessed by the "advanced config" tab. Saving the config file allows then to search loading a config file in the interface or via the command line.

### Full options for configuration in text config
Here, we detail the syntax for setting up config options in xiSEARCH, i.e. the backend of all the presets and options present in the graphical interface. This allows far more flexibility and is recommended for advanced users.

To see an example config, click on the "advanced" tab in the search interface.

All configs are in the format 

    settingName:Setting;Options

multiple options or settings are separated with a comma.

Below is a list of settings that can be configured in a text config and their description.

All possible options and their default values are also found in the BasicConfigEntries.conf file.

#### Search settings 

| Setting                                | Description                                                                                                                                                                                                             | Normally included | 
|----------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------| 
| tolerance:precursor:6ppm               | MS1 tolerance                                                                                                                                                                                                           | Yes               | |
| tolerance:fragment:20ppm               | MS2 tolearnace                                                                                                                                                                                                          | Yes               |
| missedcleavages:4                      | how many missed cleavages are considered                                                                                                                                                                                | Yes               | |
| UseCPUs:-1                             | How many threads to use. -1 uses all available                                                                                                                                                                          | Yes               |     |
| fragment:BIon                          | Ion fragment to consider. One line per fragment. Options: BIon, YIon,PeptideIon,CIon,ZIon,AIon,XIon. PeptideIon Should always be included.                                                                              | Yes               |  
| Fragment:BLikeDoubleFragmentation;ID:4 | enables secondary fragmentation within one fragment but also fragmentation events on both peptides - consider secondary fragmentation for HCD                                                                           | No                |  
| EVALUATELINEARS:true                   | Include linear matches to allow matching spectra with linears as well as crosslinks                                                                                                                                     | Yes               |
| MATCH_MISSING_MONOISOTOPIC:true        | Compensate for misidentification of monoisotopic peak in precursor. Allow matches that are off by 1 or 2 daltons                                                                                                        | Yes               | |
| missing_isotope_peaks:2                | Consider matches that are up to n Da lighter in the missing monoisotopic peak correction                                                                                                                                | Yes               |
| mgcpeaks:10                            | how many peaks to consider for alpha peptide search (the search of the bigger candidate peptide)                                                                                                                        | Yes               |
| topmgcpeaks:150                        | how many alpha peptide candidates will be considered to find beta peptide.                                                                                                                                              | Yes               |
| topmgxhits:10                          | how many combinations of alpha and beta peptides will be considered for final scoring                                                                                                                                   | Yes               |
| MAX_MODIFICATION_PER_PEPTIDE:3         | limit on how many modifications to consider per peptide. Only variable modifications count against the limit                                                                                                            | Yes               |
| maxpeakcandidates:10000                | when looking for candidate peptides only consider peaks in a spectrum that result in less then this number of candidate peptides. Default unlimited. Useful for memory otimization.                                     | No                |
| MAX_MODIFIED_PEPTIDES_PER_PEPTIDE:20   | After the initial match, how many modified versions of the peptide are considered per peptide. 20 default. Increase in searches with large number of modifications.                                                     | Yes               |
| MAX_PEPTIDES_PER_PEPTIDE:20            | How many peptides are generated from a single peptide with combinations of variable and/or linear modifications at the database stage. Consider increasing for searches with large number of modifications. 20 default. | Yes               |
| FRAGMENTTREE:FU                        | FU: uses a fastutil based implementation of the fragmenttree and conservea lot of memory doing so.  default: the default tree. FU should be chosen.                                                                     | Yes               |
| normalizerml_defaultsubscorevalue:1    | Normally, the scoring ignores subscores that are not defined. With this enabled, missing scores are set to a fixed value.                                                                                               | No                |
| MAXTOTALLOSSES:                        | for a fragment up to how many neutral losses for that fragment are considered                                                                                                                                           | No                |
| MAXLOSSES:                             | for each type of loss up to how often is that considered for a single fragment                                                                                                                                          | No                |
| MINIMUM_PEPTIDE_LENGTH:6               | Define a custom minimum peptide length in the search of alpha and beta candidates (the default value is 2)                                                                                                              | No                |
| BufferInput:100                        | IO setting improving parallel processing                                                                                                                                                                                | Yes               |
| BufferOutput:100                       | IO setting improving parallel processing                                                                                                                                                                                | Yes               |
| WATCHDOG:10000                         | How many seconds the program allows with nothing going on before shutting down. (default 1800 seconds).                                                                                                                 | Yes               |


#### Scoring settings

| Setting      | Description          | Normally included              | 
|----------------------------------------|----------------------------------------------------------------------------------------------------------------------|--------------------------------| 
| boostlnasp:overwrite:true;factor:1.3   | in the scoring, boost linear matches by a factor of X to remove crosslinked spectra that may be explained by linears | No, but useful in SDA searches |
| ConservativeLosses:3                   | How many lossy fragments are needed to define a fragment as observed. This applies to subscores denoted as "conservative" in the output csv. These count a fragment as observed if at least this number of lossy fragments are detected, even if the non-lossy fragment is missing. Default 3. | Yes               |
| MINIMUM_TOP_SCORE:0   |  If the top-match for a spectra has a score lower than this, the spectra and all of its matches are not reported| No |

#### Protease settings
Proteases are configured with their rules. Users may define their own custom proteases.

Here are a few definitions, to give an idea of the syntax:

definition of trypsin

    digestion:PostAAConstrainedDigestion:DIGESTED:K,R;ConstrainingAminoAcids:P;NAME=Trypsin

definition of trypsin\P, which also cleaves at K/R if a proline follows
    
    digestion:PostAAConstrainedDigestion:DIGESTED:K,R;ConstrainingAminoAcids:;NAME=Trypsin\P

Asp-N:

    digestion:AAConstrainedDigestion:CTERMDIGEST:;NTERMDIGEST:D,E;NAME=ASP-N

#### Crosslinker settings
Crosslinkers Should be defined with their mass and reaction chemistry:

General syntax for crosslinker definition:

     crosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:[name];MASS:[cross-linker mass];LINKEDAMINOACIDS:[list of possible cross-link targts];MODIFICATIONS:[list of associated modifications];decoy

with:

| Term                 | Description          | 
|----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| 
| Name:                | A name of the cross-linker- ALL UPERCASE             |
| MASS:                | The mass of the cross-linker as  the difference between the mass of the two peptides and the mass of the mass of the two peptides when reacted with the cross-linker        |
| LINKEDAMINOACIDS:    | A comma separated list of amino-acids that the cross-linker can react with. Additionaly nterm andcterm are accepted Also amino-acids can get a ranking by defining a penelty (between 0 and 1) for them. E.g. K(0),S(0.2),T(0.2),Y(0.2),nterm(0) means that K and the protein n-terminal are more likely to be cross-linked then S, T, or Y |
| MODIFICATIONS:       | a comma-separeted list defining related modifications E.g. NH3,17.026549105,OH2,18.0105647 defines NH3: that adds 17.026549105 to the mass of the cross-linker and OH2: that adds 18.0105647 to the mass of the cross-linker                |
| LINEARMODIFICATIONS: | same as MODIFICATIONS but will only be applied to linear peptides           |
| LOSSES:              | a comma-separeted list defining crosslinker related losses E.g. X,10,Y120 defines two losses- X: a loss of 10Da from the cross-linker Y: a loss of 120Da from the cross-linker       |
| STUBS:               | a comma-separeted list defining crosslinker stubs for MS-cleavable cross-linker  defines three cross-linker stubs: A: with mass 54.0105647 S: with mass 103.9932001 T: with mass 85.9826354                 |

For example, definition of BS3


    crosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:BS3;MASS:138.06807;LINKEDAMINOACIDS:K(0),S(0.2),T(0.2),Y(0.2),nterm(0)

The numbers next to the LINKEDAMINOACIDS refer to score penalties to account for the fact that S,T and Y are less likely to be crosslinked than K or N-terminus. 

Heterobifunctional crosslinkers like sulfo-SDA may be defined as follows:

    crosslinker:AsymetricSingleAminoAcidRestrictedCrossLinker:Name:SDA;MASS:82.0413162600906;FIRSTLINKEDAMINOACIDS:*;SECONDLINKEDAMINOACIDS:K,S,Y,T,nterm

MS-cleavable crosslinkers need to be defined with losses corresponding to their crosslinker stubs:

    crosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:DSSO;MASS:158.0037648;LINKEDAMINOACIDS:K(0),S(0.2),T(0.2),Y(0.2),nterm(0);STUBS:A,54.0105647,S,103.9932001,T,85.9826354

Additionally, crosslinker-related modifications may be defined in the crosslinker definition. It is however 
recommended to define them separately as variable or linear modifications (see next section)

Multiple crosslinkers may be defined by adding more than one line. Normally, this is done for accounting for 
noncovalent associations, including the additional "NonCovalent" crosslinker with 0 mass.

    crosslinker:NonCovalentBound:Name:NonCovalent

#### Modification settings
Modifications are possible to be defined as four types:
1. fixed: every aminoacid is modified
2. variable: peptides containing the aminoacids will be searched with and without modification
3. known: not automatically searched - but enables to defined modification as part of the FASTA file as fixed or 
   variable modification (e.g. defined histone modification only on histones without having to search them everywhere).
4. linear: peptides with that modification will only be searched as linear peptides (not part of an cross-link)

In generating the database, the software first generates all peptide variants with a single modifications, then all variants with 2 modifications, then 3 and so on until it has reached the value specified in MAX_PEPTIDES_PER_PEPTIDE (default 20). Similarly, to perform a search with a lot of modifications on a peptide, the value MAX_MODIFICATION_PER_PEPTIDE (default 3) also needs to be adjusted in order to consider combinations of more than 3 modifications. Fixed modifications don't count against either limit. Both of these variables reduce the search space. Increasing them leads to a computational cost in terms of memory and search time.

Modifications can be defined as 

        modification:variable::SYMBOLEXT:[extension];MODIFIED:[amino-acids];DELTAMASS:[mass-difference];PROTEINPOSITION:[position];PEPTIDEPOSITION:[position]

| Preset           | Description                                                                                                                                       | 
|------------------|---------------------------------------------------------------------------------------------------------------------------------------------------| 
| SYMBOLEXT:       | string of lowercase text used as modification name                                                                                                |
| MODIFIED:        | A list of amino acids that can have this modification                                                                                             |
| DELTAMASS:       | the mass diference between the modified and the undmodified version of the amino acid. Unimod mass definitions are commonly used.                 |
| PROTEINPOSITION: | The position in the protein sequence. Can only be "nterm", "nterminal", "cterm", "cterminal" or "any" (which is default, also when not specified) |
| PEPTIDEPOSITION: | The position of the modification at the peptide level. Can be "nterminal" or "cterminal" if it is specified.                                      |
| POSTDIGEST:      | the modification occurred after digestion or does not affect digestion (e.g. for mass tags). Can be set to "true" or "false"                       |



For example, a modification defining a loop link for SDA to be searched on all peptides:

    modification:variable::SYMBOLEXT:sda-loop;MODIFIED:K,S,T,Y;DELTAMASS:82.04186484

In defining modifications, "X" in the MODIFIED field denotes any amino acid. "nterm" or "cterm" cannot be used in the MODIFIED field, which only takes amino acid letters. Modifications at n- or c- terminus of proteins or peptides should be specified by the PROTEINPOSITION or PEPTIDEPOSITION field. For example, reaction of amidated bs3 on protein n-termini, searched on all peptides:

    modification:variable::SYMBOLEXT:bs3nhn;MODIFIED:X;DELTAMASS:155.094619105;PROTEINPOSITION:nterm

A modification defining pyroglutamate on n-terminal glutamates of peptides, searched on linear peptides only:

    modification:linear::SYMBOLEXT:pgu;MODIFIED:E;DELTAMASS:-18.0153;PEPTIDEPOSITION:nterminal;POSTDIGEST:true

Site-specific modifications that are always on (site-specific and fixed) can be defined by editing the .fasta database of the search. For example, a phosphorylation at a specific serine (e.g. serine 340) can be introduced by editing the sequence of the protein in the database. Thus, the protein sequence GRSKMLN becomes

    GRSphKMLN

For a site-specific modification that is not always on (site-specific and variable), the modification is introduced in brackets in the sequence

    GRS(ph)KMLN

In the .config file for the search, the associated known modification for phospho is then defined

    modification:known::SYMBOLEXT:ph;MODIFIED:S;DELTAMASS:79.966331

Known modification are registered but not applied as either fixed, variable or linear. The only use is to enable xiSEARCH to understand modifications defined in a FASTA file.


##### Legacy modification nomenclature
Legacy versions of Xi defined modifications for specific amino acids as extensions of the  amino acid name with the total mass of the amino acid plus the modification as the definition. This nomenclature is deprecated.

    modification:variable::SYMBOL:Mox;MODIFIED:M;MASS:147.035395


#### Losses settings
The losses to be considered. The syntax is similar to modifications.

    loss:AminoAcidRestrictedLoss:NAME:H20;aminoacids:S,T,D,E;MASS:18.01056027;cterm

defines a loss of water to be considered on S,T,D,E, and Cterm when assigning fragments.

Losses associated with MS-cleavable crosslinkers may also be defined here. For example, cleavage of a diazirine crosslinker results in 2 stubs (here for SDA):
    
     loss:CleavableCrosslinkerPeptide:MASS:0;Name:0
     loss:CleavableCrosslinkerPeptide:MASS:82.04186;NAME:S

#### Changing or adding new entries to the graphical config interface
The options in the drop-down menus and lists of the interface may be edited according to your needs. 

The "BasicConfigEntries.conf" contains all the selectable config values. In this file new entries for crosslinker, enzymes, modifications and losses can be freely defined. The file contains sections for crosslinker, modifications, losses, ions,enzymes and custom settings. Each section has a short description on how to add new entries.

Editing this file changes the options in the dropdown menu of the interface.

There is also the "BasicConfig.conf" file containing default values for settings not exposed in the interface. But all of these can also be overwritten in the custom settings.

## running xiSEARCH from command line and on a high performance computing (HPC) cluster

xiSEARCH may be launched from the command line specifying database and config file. Often, a config file is created in the interface and then used in launching searches from command line, for example as cluster jobs.

    java -Xmx60G -cp /path/to/xiSearch.jar rappsilber.applications.Xi --config=./MyConfig.config --peaks=./peakfile.mgf --fasta=./database.fasta -output=./MyOutput.csv --locale=en

will launch a search on peakfile.mgf with database.fasta and MyConfig.conf and 60Gb of RAM. Command line options are available

    java -cp /path/to/xiSearch.jar rappsilber.applications.Xi --help

If there is more than one peaklist to be searched, the .mgf files can either be zipped together and the zip file be given as the option of --peaks= or several --peaks= options can be given.

Multiple fasta files can be given, by providing  a --fasta= argument per fasta file.

Relative paths pointing to files in the current directory have to be preceded by ./

For HPC jobs, the directory "HPC scripts" contains an example SLURM submission scripts for single searches ("jobscript_example.sh"). Make sure to edit the number of threads in the xiSEARCH config file to match what is requested in the job file.

However, it is often desirable to run one job per peak file and combine the results at the end by concatenating the output csv files prior to FDR calculation, basically running many searches in parallel.  

Here is an example for the SLURM job scheduler. This is done with the submission scripts 1_search_template.sh, 2_create_search_calls.sh and 3_start_jobs.sh. To take advantage of this:

1. set up a directory with the sequence database (database.fasta), the config file for the search (myconfig.conf) and a directory containing all the .mgf files. In 1_search_template.sh, these are called database.fasta, myconfig.conf and peakfiles , respectively. The directory should also contain the .jar file for xiSEARCH.
2. Edit the 1_search template as required by your job scheduler and the .jar file ox xiSEARCH, keeping the capital variables in the xiSEARCH command intact (these will be changed by the second script to create a job file per mgf file)
3. execute

       ./2_create_search_calls.sh

You should now have 1 job file per .mgf file
4. launch all jobs by executing

       ./3_start_jobs.sh

5. Once all searches are completed, combine all results found in all subdirectories of the newly created "searches" directory with

        python combine_searches.py

from inside the "searches" directory.

## Additional utilities

xiSEARCH comes with a few additional utilities to convert, filter and analyze mass spectra. All these utilities have a graphical user interface. They can be launched from command line in linux/maxOS, or by editing a launcher in windows to include the line below, rather than launching the main xiSEARCH application.

#### mgf file filtering

A small application for filtering .mgf files by run and scan number  - you can start it with

    java -cp /path/to/xiSearch.jar rappsilber.gui.localapplication.ScanFilter

This is particularly useful to trim runs or perform any filtering prior to the search step. This utility can filter .mgf file by charge, perform de-noising, de-isotoping, de-charging and remove loss peaks. It can also extract spectra with a given precursor mass range, or with particular peaks present (e.g. crosslinker stub doublets). Upload  as a single peak list or .mgf files in the MSM files window.


#### theoretical spectra of crosslinked peptides

Simulate fragmentation patterns of single peptides or crosslinked peptide pairs. Launch with

    java -cp /path/to/xiSearch.jar rappsilber.gui.localapplication.peptide2ions.PeptideToIonWindow

Can define precursor charge state, ions, crosslinker, losses and enzymes in the config window of the tool.

#### Diagnostic ion mining and MS1 features

Looks for how often specific peaks appear - either as diagnostic ions or in form of neutral losses. Upload  as a single peak list or .mgf files in the MSM files window. Run with

    java -cp /path/to/xiSearch.jar rappsilber.gui.localapplication.ConsistentPeaks

#### Sequence tools

Filter fasta files for specific proteins or generate decoys explicitly

    java -cp /path/to/xiSearch.jar rappsilber.gui.localapplication.FastaTools

#### Skyline spectral library generation

Generate a skyline .ssl spectral library file from a xiSEARCH result. Upload the search config file and the .csv file of the search result.

    java -cp /path/to/xiSearch.jar rappsilber.gui.skyline.Xi2Skyline
