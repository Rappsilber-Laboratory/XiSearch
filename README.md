# XiSearch
Actually a library containing several tools - most prominently a cross-link MS search engine.

XiSearch is implemented as a Java Application. Therefore it requires that java is installed. We recommend to use the the latest update of JAVA version 8.


The latest version as binary can be downloaded from
http://xi3.bio.ed.ac.uk/downloads/xiSearch.zip

This includes a startup script for the XiSearch gui.


the Gui provides several tabs to configure your search. 
In the first tab (Run) the result file (result) and optionally a for writing out full annotations of the spectra (peak annotations) can be defined.

In the second tab (Peak Lists) the peak lists to be searched can be defined. Supported formats are mgf-files as generated from msconvert or apl-files as produced by MaxQuant. For mgf-files of other sources the config might need to be adapted to define how to read out a run-name and a scan-number from the TITLE= tags in the files.

The third tab defines the FASTA files to be searched. Several files can be defined. One also define one of the fasta files as custom decoy database. If no decoy database is given one is automatically generated based on the target database.

The fourth tab provides the search configuration. Here all parameters are defined as a in form of a text file. A default config is provided as an example that defines a search with BS3 as cross-linker and Carboxyamidomethylation on Cystein as fixed modification and oxidation of methionine as variable modification. Each option contains a short description of what it does and how to change it.

When everything is configured as desired then start-button on the first tab can be pressed to start the search.

Depending on the size of the database the start-script might need to be adapted to permit xi to use a larger amount of memory (-Xmx option). This should not exceed the amount of free memory available without running xi. E.g. if a computer has 8GB of RAM but per default 5 are used by other programs then Xi should get at most 3GB as otherwise part of the programm will be swapped out to disk and in effect run extremely slow.




