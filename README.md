## xiSEARCH 1.7.6.7 documentation


XiSearch is implemented as a Java Application. Therefore it requires that java is installed. We recommend to use the the latest update of JAVA version 8 or above. 
The latest version as binary can be downloaded from https://rappsilberlab.org/downloads/

### Background
xiSEARCH is a search engine for crosslinking mass spectrometry (crosslinking MS). It supports data acquired with ThermoFisher Orbitrap instruments (.raw format) that have been converted to peak. files (.mgf format), for example with [ProteoWizard MsConvert](https://proteowizard.sourceforge.io/). It then searches the peakfiles against a sequence database in .fasta format to identify crosslinked peptide pairs from mass spectra. The search algorithm uses a target-decoy approach outlined in XXX, which enables false-discovery rate (FDR) estimation. The FDR calculation on the xiSEARCH result is performed by xiFDR.

xiSEARCH is a flexible search engine that allows for extensive configuration of the search options and of the search scoring methods in crosslink identification. Nevertheless, its design suits best data acquired at high resolution in MS1 and MS2- in the Rappsilber lab, we acquire with 120k resolution in ms1 and 60k resolution ms2. Currently, xiSEARCH does not support MS3 approaches.

The xiSEARCH algorithm is described in detail in [Mendez, Fischer *et al.* Mol. Sys. Bio. 2019](https://www.embopress.org/doi/full/10.15252/msb.20198994). The xiSEARCH scoring function is made up of several terms accounting for the goodness of fit of the spectra to the peptide pair selected from the database, including fragment mass error, percentage intensity explained, number of fragments, number of crosslinked fragments.

Scoring happens in three stages: 

1. alpha candidates are selected and scored
2. top n aplpha candidates are taken and all matching beta-candidates will be selected and prescored
3. the top X of these are then fully matched and scored

The scoring function is applied to explain each spectrum with a target-target pair, a target-decoy pair, a decoy-decoy pair, a linear target sequence or a linear decoy sequence. Error control by false discovery rate estimation is then performed in a separate step with xiFDR.



### The interface
the interface provides several tabs to configure your search. In the first tab (Run) the result file (result) and optionally a for writing out full annotations of the spectra (peak annotations) can be defined.

In the second tab (Peak Lists) the peak lists to be searched can be defined. Supported formats are mgf-files as generated from msconvert or apl-files as produced by MaxQuant. For mgf-files of other sources the config might need to be adapted to define how to read out a run-name and a scan-number from the TITLE= tags in the files.

The third tab defines the FASTA files to be searched. Multiple files can be defined. 

The fourth tab provides the search configuration. Here all parameters are defined as a in form of a text file. A default config is provided as an example that defines a search with BS3 as cross-linker and Carboxyamidomethylation on Cystein as fixed modification and oxidation of methionine as variable modification. Each option contains a short description of what it does and how to change it.

When everything is configured as desired then start-button on the first tab can be pressed to start the search.

Depending on the size of the sequence-database and the number of search threads the start-script might need to be adapted to permit xi to use a larger amount of memory (-Xmx option). This should not exceed the amount of free memory available without running xi. E.g. if a computer has 8GB of RAM but per default 5 are used by other programs then Xi should get at most 3GB as otherwise part of the programm will be swapped out to disk and in effect run extremely slow. For searches involving dozens of spectra and hundreds of proteins, we recommend running xiSEARCH on an HPC node with large Xmx values or a server, as the RAM requirements increase with the square of the size of the database. As an example, we run searches for [this publication](https://pubs.acs.org/doi/full/10.1021/acs.jproteome.9b00541) with -Xmx 256G, specifying 256Gb of RAM.

# Setting up a search in the interface

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

| Preset                   | Description                             | 
|--------------------------|-----------------------------------------| 
| Trypsin                  |                                         |
| Trypsin/P                | trypsin not restricted by proline       |
| V8                       |                                         |
| Lys-C                    |                                         |
| Lys-C/P                  |                                         |
| trypsin/P + V8           |                                         |
| proteinaseK              |                                         |
| proteinaseK & trypsin\P  |                                         |
| Chymotrypsin             |                                         |
| Trypsin+Chymotrypsin     |                                         |
| Trypsin/P + ASP-N (D)    |                                         |
| Asp-N(DE)                |                                         |
| Trypsin/P+ASP-N(DE)      |                                         |
| Trypsin/P+ASP-N(E)       |                                         |
| Elastase                 |                                         |
| Elastase & Trypsin       |                                         |
| Trypsin/P & Exopeptidase |                                         |
| Tryp-N                   |                                         |
| No digestion             | for example used for synthetic peptides |



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

### Do FDR setting 
If the "Do FDR" box is ticked, xiFDR will automatically be run at the end of xiSEARCH. We tend to leave this option off, as we prefer to run xiFDR in a standalone process to have access to more advanced FDR filtering options.

### Start search

Press "Start" to start the search.

# Setting up a search on the command line or in the advanced config interface
The whole configuration of the search in the graphical interface may be set up as a configuration file ("config file") containning all the options. This may be accessed by the "advanced config" tab. Saving the config file allows then to search loading a config file in the GUI or via the command line.

### Full options for configuration in text config
Here, we detail the syntax for setting up config options in xiSEARCH, i.e. the backend of all the presets and options present in the graphical interface. This allows far more flexibility and is recommended for advanced users.

To see an example config, click on the "advanced" tab in the search interface.

All configs are in the format 

    settingName:Setting;Options

multiple options or settings are separated with a comma.

Below is a list of settings that can be configured in a text config and their description.

#### SEARCH SETTINGS 

| Setting                                | Description                                                                                                                                                         | Normally included              | 
|----------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------| 
| tolerance:precursor:6ppm               | MS1 tolerance                                                                                                                                                       | Yes                            | |
| tolerance:fragment:20ppm               | MS2 tolearnace                                                                                                                                                      | Yes                            |
| missedcleavages:4              | how many missed cleavages are considered                                                                                                                            | Yes                            | |
| UseCPUs:-1                             | How many threads to use. -1 uses all available                                                                                                                      | Yes                            |                                                                                            |
| fragment:BIon                          | Ion fragment to consider. One line per fragment. Options: BIon, YIon,PeptideIon,CIon,ZIon,AIon,XIon. PeptideIon Should always be included.                          | Yes                            |  
| Fragment:BLikeDoubleFragmentation;ID:4 | enables double fragmentation with in one fragment but also fragmentation events on both peptides                                                                    | No                             |  
| EVALUATELINEARS:true                   | Include linear matches to allow matching spectra with linears as well as crosslinks                                                                                 | Yes                            |
| MATCH_MISSING_MONOISOTOPIC:true        | Compensate for misidentification of monoisotopic peak in precursor. Allow matches that are off by 1 or 2 daltons                                                    | Yes                            | |
| missing_isotope_peaks:2                | Consider matches that are up to n Da lighter in the missing monoisotopic peak correction                                                                            | Yes                            |
| mgcpeaks:10                            | how many peaks to consider for alpha peptide search (the search of the bigger candidate peptide)                                                                    | Yes                            |
| topmgcpeaks:150                        | how many alpha peptide candidates will be considered to find beta peptide.                                                                                          | Yes                            |
| topmgxhits:10                          | how many combinations of alpha and beta peptides will be considered for final scoring                                                                               | Yes                            |
| MAX_MODIFICATION_PER_PEPTIDE:3         | limit on how many modifications to consider per peptide. Both variable and fixed modifications count against the limit                                              | Yes                            |
| MAX_MODIFIED_PEPTIDES_PER_PEPTIDE:20   | After the initial match, how many modified versions of the peptide are considered per peptide. 20 default. Increase in searches with large number of modifications. | Yes                            |
| FRAGMENTTREE:FU                        | FU: uses a fastutil based implementation of the fragmenttree and conservea lot of memory doing so.  default: the default tree. FU should be chosen.                 | Yes                            |
| normalizerml_defaultsubscorevalue:1    | Normally, the scoring ignores subscores that are not defined. With this enabled, missing scores are set to a fixed value.                                           | No                             |
| MAXTOTALLOSSES:                        | for a fragment up to how many neutral losses for that fragment are considered                                                                                       | No                             |
| MAXLOSSES:                             | for each type of loss up to how often is that considered for a single fragment                                                                                      | No                             |
| MINIMUM_PEPTIDE_LENGTH:6               | Define a custom minimum peptide length in the search of alpha and beta candidates (the default value is 2)                                                          | No                             |
| BufferInput:100                        | IO setting improving parallel processing                                                                                                                            | Yes                            |
| BufferOutput:100                       | IO setting improving parallel processing                                                                                                                            | Yes                            |

#### SCORING SETTINGS

| Setting      | Description                                                                                                          | Normally included              | 
|----------------------------------------|----------------------------------------------------------------------------------------------------------------------|--------------------------------| 
| boostlnasp:overwrite:true;factor:1.3   | in the scoring, boost linear matches by a factor of X to remove crosslinked spectra that may be explained by linears | No, but useful in SDA searches |
| MINIMUM_TOP_SCORE:0   |  If the top-match for a spectra has a score lower than this, the spectra and all of its matches are not reported| No |

#### PROTEASE SETTINGS
Proteases are configured with their rules. Users may define their own custom proteases.

Here are a few definitions, to give an idea of the syntax:

definition of trypsin

    digestion:PostAAConstrainedDigestion:DIGESTED:K,R;ConstrainingAminoAcids:P;NAME=Trypsin

definition of trypsin\P, which also cleaves at K/R if a proline follows
    
    digestion:PostAAConstrainedDigestion:DIGESTED:K,R;ConstrainingAminoAcids:;NAME=Trypsin\P

Asp-N:

    digestion:AAConstrainedDigestion:CTERMDIGEST:;NTERMDIGEST:D,E;NAME=ASP-N

#### CROSSLINKER SETTINGS
Crosslinkers Should be defined with their mass and reaction chemistry:
General syntax for crosslinker definition:

     crosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:[name];MASS:[cross-linker mass];LINKEDAMINOACIDS:[list of possible cross-link targts];MODIFICATIONS:[list of associated modifications];decoy

with:

| Term                                                                          | Description                                                                                                                                                                                                                                                                                                                                 | 
|-------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| 
| Name:                                                                         | A name of the cross-linker- ALL UPERCASE                                                                                                                                                                                                                                                                                                    |
| MASS:                                                                         | The mass of the cross-linker as  the difference between the mass of the two peptides and the mass of the mass of the two peptides when reacted with the cross-linker                                                                                                                                                                        |
| LINKEDAMINOACIDS:                                                             | A comma separated list of amino-acids that the cross-linker can react with. Additionaly nterm andcterm are accepted Also amino-acids can get a ranking by defining a penelty (between 0 and 1) for them. E.g. K(0),S(0.2),T(0.2),Y(0.2),nterm(0) means that K and the protein n-terminal are more likely to be cross-linked then S, T, or Y |
| MODIFICATIONS:                                                                | a comma-separeted list defining related modifications E.g. NH3,17.026549105,OH2,18.0105647 defines NH3: that adds 17.026549105 to the mass of the cross-linker and OH2: that adds 18.0105647 to the mass of the cross-linker                                                                                                                |
| LINAERMODIFICATIONS:                                                          | same as MODIFICATIONS but will only be applied to linear peptides                                                                                                                                                                                                                                                                           |
| LOSSES: | a comma-separeted list defining crosslinker related losses E.g. X,10,Y120 defines two losses- X: a loss of 10Da from the cross-linker Y: a loss of 120Da from the cross-linker                                                                                                                                                              |
| STUBS:  | a comma-separeted list defining crosslinker stubs for MS-cleavable cross-linker  defines three cross-linker stubs: A: with mass 54.0105647 S: with mass 103.9932001 T: with mass 85.9826354                                                                                                                                                 |

For example, definition of BS3


    crosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:BS3;MASS:138.06807;LINKEDAMINOACIDS:K(0),S(0.2),T(0.2),Y(0.2),nterm(0)

The numbers next to the LINKEDAMINOACIDS refer to score penalties to account for the fact that S,T and Y are less 
likely to be crosslinked than K or nterminus. 

Heterobifunctional crosslinkers like sulfo-SDA may be defined as follows:

    crosslinker:AsymetricSingleAminoAcidRestrictedCrossLinker:Name:SDA;MASS:82.0413162600906;FIRSTLINKEDAMINOACIDS:*;SECONDLINKEDAMINOACIDS:K,S,Y,T,nterm

MS-cleavable crosslinkers need to be defined with losses corresponding to their crosslinker stubs:

    crosslinker:SymetricSingleAminoAcidRestrictedCrossLinker:Name:DSSO;MASS:158.0037648;LINKEDAMINOACIDS:K(0),S(0.2),T(0.2),Y(0.2),nterm(0);STUBS:A,54.0105647,S,103.9932001,T,85.9826354

Additionally, crosslinker-related modifications may be defined in the crosslinker definition. It is however 
recommended to define them separately as variable or linear modifications (see next section)

Multiple crosslinkers may be defined by adding more than one line. Normally, this is done for accounting for 
noncovalent modifications, including the additional "NonCovalent" crosslinker with 0 mass.

    crosslinker:NonCovalentBound:Name:NonCovalent

#### MODIFICATION SETTINGS
Modifications are possible to be defined as four types:
1. fixed: every aminoacid is modified
2. variable: peptides containing the aminoacids will be searched with and without modification
3. known: not automatically searched - but enables to defined modification as part of the FASTA-file as fixed or 
   variable modification (e.g. defined histone modification only on histones without having to search them everywhere).
4. linear: peptides with that modification will only be searched as linear peptides (not part of an cross-link)

Modifications can be defined as 

        modification:variable::SYMBOLEXT:[extension];MODIFIED:[amino-acids];DELTAMASS:[mass-difference]

| Preset             | Description                                                                                                                       | 
|--------------------|-----------------------------------------------------------------------------------------------------------------------------------| 
| SYMBOLEXT:            | string of lowercase text used as modification name                                                                                |
| MODIFIED: | A list of amino acids that can have this modification                                                                             |
| DELTAMASS: | the mass diference between the modified and the undmodified version of the amino acid. Unimod mass definitions are commonly used. |

For example, a modification defining a loop link for SDA to be searched on all peptides:

    modification:variable::SYMBOLEXT:sda-loop;MODIFIED:K,S,T,Y;DELTAMASS:82.04186484

Legacy versions of Xi defined modifications for specific amino acids as extensions of the  amino acid name with the 
total mass of the amino acid plus the modification as the definition. This nomenclature is deprecated.

    modification:variable::SYMBOL:Mox;MODIFIED:M;MASS:147.035395

#### LOSSES SETTINGS
The losses to be considered. The syntax is similar to modifications.

    loss:AminoAcidRestrictedLoss:NAME:H20;aminoacids:S,T,D,E;MASS:18.01056027;cterm

defines a loss of water to be considered on S,T,D,E, and Ctermm when assigning fragments.

Losses associated with MS-cleavable crosslinkers may also be defined here. For example, cleavage of a diazirine 
crosslinker results in 2 stubs (here for SDA):
    
     

