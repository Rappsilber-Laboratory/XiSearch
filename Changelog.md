Version 1.8.6
  * xiSEARCH version gets forwarded to xiFDR (if >= 2.3.1)
  * BugFix: linear peptide matches where exported with two columns missing (from the empty peptide2 columns)

Version 1.8.5
  * better detection java version
  * better detection of the used java binary
  * restored compatibilty with java 1.8

Version 1.8.4
  * bundled with xiFDR 2.2.3
  * some code cleanup
  * BugFix CSV export did forward ; and \" from the fasta header into the exported CSV-file resutling in troubles for xiFDR
  * BugFix for Version-parsing

Version 1.8.3
  * bundeled with xiFDR 2.2.2

Version 1.8.2
  * code cleanups
  * dependencies updated

Version 1.8.1
  * support for deflate64 compressed zip files

Version 1.8
  * new sub-score precursor observed in MS2
  * new watchdog that warns if to much time is spend in garbage collection -> not enough memory
  * modification to MS2PD to optionally include charge state
  * template system for configuration with 4 templates provided (2 for BS3/DSS and  2 for DSSO)
  * xiSEARCH GUI has memory setting
  * xiSEARCH GUI moved non-covalent from crosslinker to new checkbox
  * mzIdentML owner info directly editable in the xiSEARCH GUI
  * for each search a log-file is generated
  * number format locale correctly forwarded to xiFDR
  * output of annotated peaks can be enabled in the GUI
  * when stubs where configured as par of an asymmetric crosslinker definition, these where ignored
  * protein name and description are reported (if parsable from the fasta-file)
  * scan number are now reported without a 1000 digit separator (e.g 1345 instead of 1,345)
  * file names get quoted to prevent troubles with commas in file names
  * Customisable Search Templates
  * Search runs in separate process
  * Memory for search definable in GUI
  * regularly check how much time is spend in Garbage collection and warn if that gets to large
  * as this indicates that not enough memory is available
  * BugFix: csv-output did not quote file names correctly - meaning commas in filenames can mess up the columns

Version 1.7.6.8
  * depending on java version xiFDR gets called with an additional argument (introduced in 1.7.6.5) but seems like this is not always reliable. So if xiFDR crashes on start - it gets restarted without that option.
  * some adaptions for next xiFDR Version 2.2.

Version 1.7.6.7
  * The GUI should not just disapear on a search crash but display some info about the crash and offers to send it to developer
  * BugFix: feedback and version check updated to new server and working
  * BugFix: Doubledigestion could cause crashes for some FASTA-files

Version 1.7.6.6
  * BugFix: loading config with multiple crosslinker
  * BugFix: call to xifdr failed for earlier java versions

Version 1.7.6.5
  * call to xiFDR adapted to newer jdk versions (tested on jdk 17)
  * modification specificity: expanded the terminal names accepted
    * [nc]term
    * prot[nc]term
    * protein[nc]term
    * pep[nc]term
    * peptide[nc]term
    * dashes in the name are ignored

Version 1.7.6.4
  * ms2 missing monoisotopic matching threshold reduced back to 1000Da
  * some minore bug in matching isotope cluster

Version 1.7.6.3
  * new MS2PC algoritm
  * Error when trying to define more then one peptide or protein position per modification

Version 1.7.6.2
  * BugFix: wrong mgcAlpha scores where written out
  * New set of scores \"Unique Crosslinker containing\"
  * remove \";\" from description on writing out CSV-files
  * ErrorStatus contains now also the previous status

Version 1.7.6.1
  * BugFix: BoostLNAPS factor was ignored
  * BugFix: peptide ordering for alpha-peptide = beta-peptide

Version 1.7.6
  * a new digestion unaffected crosslinker implementation
  * MSMWriter writes mgf files with the original title
  * Ability to boost linear and NAPS scores by a given factor
  * BugFix: skyline library generation did not show the modifications to be defined
  * BugFix: in 1.7.5 the generic mgf-title parser was always applied
  * BugFix: losses had more impact on link-site assignment then expected

Version 1.7.5.1
  * BUGFIX: in1.7.5 the generic mgf-title parser was always applied

Version 1.7.5
  * Improved loading of config from DB
  * Modifications can now be defined as applicable before or after digest
  * BugFix: Corrected parsing of file source for MSMListIterator
  * BugFix Regular Expression mgf-title parser
  * BugFix: corrected some settings in the default BasicConfigEntries.conf
  * BugFix: Crosslinker-selection
  * BugFix: decoy generation ignored FASTA-modifications

Version 1.7.4.1
  * Restored compatibility with java 1.8

Version 1.7.4
  * Offline search can load config from DB
  * Bugfix:LoadButton not enabled
  * Bugfix:error after reading last spectrum 

Version 1.7.3
  * Improved Loading of configs.

Version 1.7.2
  * Number of search threads can be adjusted during runtime (if a gui is used).
  * alpha counts writen out.
  * changed format of the estimed time till search finished
  * some more sub-scores - not yet used in final scoring
  * Report intensities of peaks as part of the result (e.g. for TMT)
  * Bugfix: DSSO related problems

Version 1.7.1
  * BugFix for crosslinker that exclusively link protein-terminal to something.
  * changed format of the estimed time till search finished
  * Light speedup (10%-20%)
    * some code resturcturing
    * changed the way fragments are matched to spectra
  * hopefully more robust parsing of scan and run from mgf-title
    * if no known pattern is found it will take the first number longer then 3 digits after leaving space for a run-name
  * expanded matching of the RTINSECONDS tag in MGF files
  * scan-number is not required anymore for offline search
  * As pinpoint is no longer supported renamed the Skyline export class
  * BugFix for Skyline SSL conversion independent of DB
  * BugFix for removing annotation of same loss from different loss-defintions
  * losses that especially prevalent (e.g. phospho losses in an enriched sample) can be flaged to be considered during candidate selection

Version 1.7.0
  * new gui for parameter-selection
  * if xiFDR.jar is found or selected then xiFDR will automatically be called
  * for decoy generations N-terminal Methionines are kept in place
  * new optional decoy generation schema (random_directed: random based on pairwise aminoacid sequences in the original protein
  * example configuration for linear only modifications in default config
  * Crosslinker-Stubs can be defined as part of the cross-linker
  * Crosslinker based losses can be definable as part of the cross-linker
  * made the memory efficient fragmenttree the default
  * restored compatibility with java 1.7
  * Some heuristcs for defining maximum number of threads in a memory constrained environment
  * Hardcoded certificate for the response server
  * BugFix: Modification with zero mass where not permited
  * BugFix: empty proteins in fasta crashed xiSEARCH
  * BugFix: not all sub-scores written top DB

Version 1.6.753
  * BugFix: transfer loss to base did not set the charge stae correcly for the new isotope cluster

Version V1.6.752
  * BugFix: calculation for fragment-mass tolerance was slightly off
  * Feature: aminoacid restricted losses from precursor
  * FastaTool slightly improved

Version 1.6.751
  * BugFix: multiple losses annotated
  * BugFix: previous bugfix disabled photo-crosslinker
  * display estimated time remaining in status

Version 1.6.750
  * BugFix: splitting clusters potentially results in wrong charge state assignment for the cluster
  * BugFix crosslinker defined exclusively with unknown aminoacids for specificites
  * BugFix for variable modifications in FASTA-file

Version 1.6.749
  * BugFix for FASTA-defined variable modifications
  * Bugfix for no aminoacids in cross-linker specificity recognised

Version 1.6.748
  * BugFix wrong masses for U and O
  * Bugfix for NoDigestion throwing an NullPointerException
  * New peak-list iterator that tries to read all fiels in a folder
  * Ensure some buffers are flushed before closing a search-thread
  * DB2Pinpoint no longer supports pinpoint but only skyline
  * changed to maven project
  * Several BugFixes to get Open-and Targetd modifications up to speed with recent canges
  * CleavableCrossLinkerFragments no longer increase the memory size of teh searchtree 
  * experimental: filter candidate pairs to expected protein interactions 
  * experimental: array-based fragmenttree
  * BugFix for delta-score

Version 1.6.747
  * try to protect against intermitten disconnects on the filesystem-site
    * Use a wrapper for FileInputStream (RobustFileInputStream)  that tries to reopen the underlying file on Errors
    * ZipStreamIterator - a zip-fiel access that uses the RobustFileInputStream to acces the zip-file

Version 1.6.746
  * all scores to the DB
  * changed to maven project

Version 1.6.745
  * BugFix decoy proteins not assigned a source

Version 1.6.744
  * AminoAcidRestrictedImmoniumIons maybe usable
  * some output try to finish cleaning on CTRL+C
  * new sub score CCPepFragmentIntensity
  * BugFix: bugfix RUN_RE and SCAN_RE was ignored for first spectrum
  * BugFix: PeaklistWriter not closing down correctly
  * Bugfix: Commandline xi now has a default local set (en)
  * Bugfix: PeakListWriter did not properly close the output
  * Bugfix: Cloned Spectra did not forward the info of additional charges or m/z values to be considered
  * Bugfix: spelling error for CleavableCrossLinkerPeptideFragment
  * BugFix: if one peak has several lossy annotations to only one was removed if it when they did not fit to linkage site if losses are twice defined it will only ones be considered
  * maybe BugFix: DecimalFormat output odd character for Double.NaN

Version 1.6.743
 * new modification type - besides \"fixed\", \"variable\", and \"known\" one 
	can now define \"linear\" modification that are only to be found on 
	peptides matched as linear peptides 
 * some scores get the RunConfig as part of the initialisation
 * renamed \"searchStoped\" to searchStopped in SimpleXiProcess
 * when threads stop they report the reason they stop
 * reordered some initialisations in AbstractRunConfig
 * storeObject and retrieveObject are caseinsensetive if a String is 
	used as key
 * updated filter readout from config
 * new Filter \"RemoveSinglePeaks\" as an alternative denoising method
 * new Filter \"ScanFilteredSpectrumAccess\" to only search specific scans
	actually that filter already exists but was not selectable via the 
	config
 * new filter \"Rebase\": the main use is to write out the filtered 
	spectrum to the database by declaring the up to that point processed 
	spectrum as the \"original\" spectrum 
 * GenericTextPopUpMenu insalles now also a simple search function on 
	textareas
 * the peakfile table the database is now filed and referenced
 * peakFileName will be recorded for each spectrum.
 * CSVExportMatches can change the delimiter and quote charater that is 
	used 
 * CSVExportMatches automatically switches the delimiter to simicolon if 
	\",\" is found to be the decimal separator in the current local
 * saving the result-file as .txt, .txt.gz, .tsv, or .tsv.gz automatically 
	switches to a tab-separated output.
 * if for some reason CSVExportMatches is set to write out CSV with \",\" 
	as delimiter under a german local then all floating-point values get 
	surounded by quotes.
 * new \"peaklist reader\" that forwards a single spectrum from memory.
	mainly used in the annotator project
 * new supscores:
	CCPepFragmentCount : how many unique cross-linker stub modified 
		peptideIon fragments where found
	CCPepFragmentError: the smallest error for cross-linker stub modified 
		peptideIon fragments  
 * BugFix in crosslinkerrestrictedLoss
 * new setting \"TransferLossToBase:true|false\"
	if set to true and isotope cluster is matched to a loss xi will try 
	to find a isotope-cluster for the basic (non-lossy) fragment even
	if the isotope cluster is contained in another isotope cluster
 * BugFix in Spectra.cloneTopPeaksRolling to ensure the right number of 
	peaks are considered 
 * the origin of a spectrum can be overwriten (used in the rebase filter)
 * Error is now calculated for both MS1 and MS2 as (observed - calculated)/calculated*1000000
 * Digestion defintions can have now a minimum peptide length defined
 * modified the retention-time tool to transfer a lot of other metadata.
 * bugfix for asymteric (heterobifuctional) cross-linker assigning wrong 
    penalities for linkable aminoacids
 * BugFix for empty zip-files crashing xi
 * new  Filter that tries to provide new candidate m/z values and charge 
    states based on precursor peaks found in the MS2 spectra
 * NormalizerML can now be instructed to replace missing subscore with a 
    default score via:normalizerml_defaultsubscorevalue:score
    The score would be the normalized version of the score, so the most 
    sensible replacement would be 1.    
 * GenericTextPopUpMenu now also implements a primitive search function for 
	JTextAreas. It also installs a text-handler that triggers this with 
	ctrl+F. Mainly usfull for finding something in the config
 * Context-menu has now two separators (looks nicer)
 * XiSearch is now by default using the english number format on output
 * NumberFormat for the xiRersult can be switched to a different one

Version 1.6.741
 * BugFix for LowResolution mode being the always switched on
 * BugFix for definition for X-ions
 * Bugfix for recognising settings related to variable modifications per peptide
 * Default xi-flavour is now SimpleXiProcessMultipleCandidates
 * Fasta files are now IDed to enable a more correct mzIdentML export
 * StatusWrites to the database are enabled sooner to be able to report more errors to the user
 * Spectra can define candidate masses for peptides 
 * CSV-Output reports if peptide matched to a predefined mass for the spectrum
 * CSV-Output reports if peptides are found as with cross-linker stubs
 * CSV-Output also report the average non-absolute MS2 error
 * experimental mz and charge are now also keeped independent of spectrum as these masses can be changed as a result of missing monoisotopic peak detection
 * Index of the scan in the original file is reported
 * MGF-files can have a new entry XLPEPMASSES to provide a list of masses as candidates for the peptides that make up the spectrum
 * moved some settings from  genericly stored values to proper fields in the config
 * InputFilter can be applied before a search-thread gets to see a spectrum
 * if xi spends more then 80% percent of its time in GC it will be stopping individual search threads as each thread will require memory and freeing the memory for one make the rest work without constantly running into a gc
   * this is part of the watchdog running and will be tested ones every 10 minutes
 * some different approaches to sorting the results for a spectrum are implemented
   * by exceeding vs non-exceeding certain average MS2 error limits
   * prioretising peptides to predefined peptide masses
 * in standard xi make sure progress report happens (if there was a progress) at least ones every 10 seconds per thread
 * BugFix for Spectra.getTopPeaks
 * linksitescore is written to the database
 * write the original (unfiltered unmodified) spectrum to the database

Version 1.6.739
 * Bugfix in watchog  
 * shifted the database ping from the watchdo to the status interface to prevent concurrent updates on the same databse row 
 * undid a workaround to the database problems (sending interrupts) as these are hopefully not needed anymore 

Version 1.6.738
 * some workarounds some postgresql problems of not coming back from a query. 
 *    Unluckely there still cases where I don't have a workaround  

Version 1.6.737
 * bugfix in connectionpool for not creating new connections 

Version 1.6.736
 * collecting custom config files in a separate list - mainly for forwarding them to the spectrumviewer 
 * XiVersion now has two commandline arguments -- gui to show a window with the version information and -v to just print out the version 

Version 1.6.735
 * Annotated peaklist will now automatically compressed if the file ends in .gz 
 * Simple Xi Gui now appends the xi version to the selected file name 
 * Check for non-stoped thread at the end ignores daemon tasks 
 * SimpleXiGui starts search in own ThreadGroup - interface gets not killed at the end 
 * status of restarted searches should be more informative 

Version 1.6.734
 * config containing spaces around numeric values caused crashes 
 * search pings the database regularly to show that it is still running 
 * Search should stop if the search gets flaged as delted in the database 
 * further improvments for handling of databse disconnects 

Version 1.6.733
 * BugFix fragments matched to  missing monoisotopic peaks where using a slightly wrong mass 
 * fixed a java 9 related problem of reading the default config from within the jar-file 
 * Watchdog that kills xi if no progress hs happened in a while(30 minutes) 
 * BugFix related to cleanup after xi trying to daemonise threads - that does not work 
 * Options MAX_MODIFIED_PEPTIDES_PER_PEPTIDE and MAX_MODIFICATION_PER_PEPTIDE got promoted to proper config options
 * Two new optional config options MAX_MODIFIED_PEPTIDES_PER_PEPTIDE_FASTA, MAX_MODIFICATION_PER_PEPTIDE_FASTA. if not set explicitly then the non-fasta values are used.
 * the new options are now aplied when digesting FASTA-files that contain expected variable modifications 
 * Improved handling of databse disconnects 

Version 1.6.732
 * removed unused \"aminoacids\" 
 * bugfix for reading DBConfig if it was on network-path 
 * average MSError subscores for each peptide 
 * BugFix elution time not forwared correctly 
 * Spectra write the original unprocessed spectra out
 * BugFix for randomized dataabse 
 * Spectra can now be cloned with top peaks in a roling window 
 * Denoise filter on spectra prior matching 
 * BugFix for spectra with unknown charge state in conectionn with additional defined charge states 
 * Maximum average MS2 error can be enforced for results writen out 
 * XiSearch should no longer keep running even so nonrecoverable errors have occured 

Version 1.6.731
 * BugFix: Cleavable Cross-linekr stubs having the wrong fragment mass

Version 1.6.730
 * BugWorkaround: Not using FUArithmeticScored ocurence as it seem to have a bug

Version 1.6.729
 * Feature: Use FUArithmeticScoredOccurence hwne the fastutil fragmentree is used
 * Bugfix for permuted peptides
 * New setting Tolerance:candidate: for setting using a different fragmenttolerance during candidate selection
 * Per default priority is now given to linear modified peptides

Version 1.6.728
 * First peptide is always the peptide with more fragment matches
 * Support for cross-linker stub containing fragments
 * cross-linker containing fragments can become part of the fragmenttree
 * Soem rework on the thread cleanup post search
 * Permutated peptides are assigned to the same decoy protein as the originating peptide belonged to
 * Removed some java finalaze methods as these seem to have lead to memory leaks
 * Linear Peptides can now prioretizeed if they have the same number of fragments matched and optionally if they have the same sequence as both peptides combined of teh top-scoring match

Version 1.6.724
 * Alpha candidate selection is pulling in more modification states for peptides
 * BugFix - peptides for a match get now ordered by number of fragments matching to them
 * Using a the FastUtil library to reduce memory consumption
 * Some setting handled by RunConfig.StoreObject and restoreObject are now proper config settings (more need to be cleaned here
 * remove Annotations from spectra after scoring and form all non-topranknign ones also the peaks
 * Spectra can now also searched with additional m/yz offsets to account for miss-assignements of mono-isotopic peaks in the massspec
 * FragmentLookup can now do the candidate selection on it's own. First step to different candidate selection methods

Version 1.6.723
 * Alpha candidate selection is pulling in more modification states for peptides

Version 1.6.722
 * BugFix for custom run and scan number information
 * Memory optimizations

Version 1.6.721
 * BugFix for custom run and scan number information

Version 1.6.720
 * support for defining additional charge states and m/z values in mgf files

Version 1.6.719
 * First try of mzML support
 * ResultWriters throw IOExceptions

Version 1.6.718
 * BugFix for multiple acquisitions searched

Version 1.6.717
 * new option MAXPEAKCANDIDATES to restrict the maximum ambiguity a peak can have to be considered for candidate selection

Version 1.6.716
 * new integer based fragment tree

Version 1.6.714
 * Speed up for candidate selection

Version 1.6.713
 * BugFix: manual start of database searches failed

Version 1.6.712
 * BugFix: comma \",\" in raw-file names
 * BugFix: Multisptep digestion
 * BugFix: Missing remove() in PermArray

Version 1.6.711
 * WorkAround - some buffer threads appear not to close down - this is now just a workaround

Version 1.6.693
 * BugFix: optional setting \"ConservativeLosses\" was ignored
 * BugFix: LinearCrosslinker now does not declare peptides as linkable

Version 1.6.688
 * Command-line support for xi

Version 1.6.683
 * bugfix for writing out proteins to the xi3 database that have no name but only accession numbers 

Version 1.6.681
 * bugfix missing new line when writing out spectrum_sources 

Version 1.6.679
 * Still bugfix for duplicat ids writen to database 

Version 1.6.676
 * bugfix for duplicat ids writen to database again

Version 1.6.675
 * bugfix for duplicat ids writen to database

Version 1.6.671
 * xi3 database output
 * csv-output - updted preceding and succeding amino-acids
 * possibility to define arbitrary regular expressions for parsing mgf-files
 * disabling the debug-window now also does not load it anymore (no graphical system needed anymore)
 * update for restarting searches

Version 1.6.658
 * BugFix for duplicated keys trying to be writen into database (has_protein-table)

Version 1.6.656
 * Bugfix for multistepdigest

Version 1.6.655
 * support for stacking of multistep digestion and parallel digestion

Version 1.6.653
 * support for multistep digestion and parallel digestion

Version 1.6.650
 * bugfix for protein haveing null name not writen out to database

Version 1.6.649
 * limited suport for another openms converted mgf files

Version 1.6.646
 * BugFix for annotation of overlapping isotope clusters (extension of isotope cluster to lighter peaks)
 * BugFix for generation of reversed sequences could lead to protein names of just \"REV_\"
 * BugFix Version.toString() failing when extension is null\"
 * Updated default config for offline Xi

Version 1.6.643
 * BugFix for MSM-iterator expecting FinneganScanNumber to be lower case
 * Some changes to cross-linker needed for mzIdentML export from xiFDR

Version 1.6.642
 * small bugfix in no search related functions

Version 1.6.641
 * Reduced Memory consumtion
 * OpenMS-Style mgf files can now be read in

Version 1.5.636
 * bugfix ZipMSMListIteraotor now opens the zip file as read only
 * Decoys can now be forced to be considered even if they are also found as targets

Version 1.5.632
 * bugfix DBRunConfig

Version 1.5.628
 * bugfix DBRunConfig

Version 1.5.627
 * SLight bugfix for memory-display in debug-window

Version 1.5.626
 * Possibility for automatic gc when memory falls below 10mb - mainly for debug-purposes to log how much memory is actually use/available 
 * detection of possible linears now uses unique peak-matches instead of primary explanations - to make it more robust in not flagging likely linears as autovalidated

Version 1.5.624
 * BugFixes for the debug-window not showning memory informations
 * enabled modification to be c- or n-terminal

Version 1.5.619
 * BugFixes in aminoacid iterator for peptides and sequences
 * ToleranceUnit now provides functions to calculate the error as defined by the tolerance unit - meaning as Da-unit or ppm unit

Version 1.5.618
 * Several BugFixes and some adaptions needed for the on demand annotator

Version 1.5.609
 * Bugfix+: hopefully a \"bugfix\" for searches not finish writing - actually more of a workaround 

Version 1.5.594
 * Bugfix+: for randomly generated peptides 

Version 1.5.592
 * Bugfix: Randomly generated decoy peptides where not correctly flaged up as beeing the same N- and C- terminal wise as the original peptide

Version 1.5.591
 * Bugfix: Likely linear matches where auto-validated

Version 1.5.588
 * Bugfix: unrestricted crosslinker was screwed by the narry cross-linker

Version 1.5.587
 * Bugfix: sometimes a match of two peptides that are not really cross-linkable made it through and then crashed in the new LinkSiteDelta score.
   * now peptides get tested for linkability before doing a full matching.

Version 1.5.584
 * Bugfix: sometimes a match of two peptides that are not really cross-linkable made it through and then crashed in the new LinkSiteDelta score.
   * now peptides get tested for linkability before doing a full matching.

Version 1.4.582 
 * minimum requirementsfilter is back
 * Link-Site weight ignores losses of more then 3 things
 * Link-Site weight losses have less impact
 * xi-version used during search is written into search notes

Version 1.4.581 
 * new xi version with updated scores and autovalidati

Version 1.3.560 
 * Adam HSA research 

Version 1.3.553 
 * Version for Marta Mendes ETD corrected

Version 1.3.600
 * Added flushing of result writers.
 * Multithreaded gather data for chacking maximum peptide-sizes
 * restructured filter into sub-folders

Version 1.3.400
 * Deadlock detectionz
 * empty files in zips are ignored
 * Targeted modifciations can be enabled by dummy crosslinker with name TargetModification
 * Compatible to mgf files of newer versions of ProteoWizard MSConvert
 * Support for low-resolution ms2
 * OpenModification output can be restricted to only modified peptides

Version 1.3.355
 * AutoValidation only validates with score >7 and possible linears must have some support to show, that they are cross-linked
 * Better process-information
 * Database-output changed to ad some new fields (e.g. Peptide-length)
 * Digestion: AminoAcids defined for specificity and restriction that are not known will be ignored
 * Support for ms-convert generated mgf-files
 * Support for MassMatrix-generated mgf-files
 * Fasta-files can be gziped
 * MGF-Files can be gziped or zip-compressed
 * Fasta-header are parsed and split into name accession and description
 * Some changes, to make the non-guiparts compatible with jdk 1.5
 * ScanFilter tries to ignore the extension
 * BugFix for spectra, that are empty or would create an empty mgc-spectra
 * BugFix for ProteinDiscoverer generated mgf-files
 * BugFix in AsymetricAminoAcidRestricted Cross-linker to actually be Asymetric
 * BugFix - Format of the CSV-export
 * BugFix ToleranceUnit to handle \"0 da\"
 * BugFix for some APL-files
 * BugFix to the msm-iterator
 * BugFix for weighted cross-linker sites
 * BugFix in some places where a maximum charge state of 10 defined - removed
 * BugFix to the isotop-detection

Version 1.3.316
 * BugFix release
 * second-last aminoacid being crosslinkable not beeing regocnised
 * exported precursor error was wrong
 * APL-input had an error in the first run of gathering information about the APL-file
 * additionally Lysin got some hardcoded precedence for the crosslinking-site

Version 1.2.258
 * Label working now - but schemes are ignored - so only Heavy and light no medium
 * Bugfix for the cutoff heuristics

Version 1.2.259
 * BugFix for digestion and crosslinker did not handle labeled amino-acids

Version 1.2.315
 * BugFix for second-last aminoacid being crosslinkable not beeing regocnised
 * Hardcoded preference for lysins (until the weighted implementaions is up and running) 
 * A lot of code-cleanup\n" ;
