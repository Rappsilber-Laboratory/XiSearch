/* 
 * Copyright 2016 Lutz Fischer <l.fischer@ed.ac.uk>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rappsilber.utils;


/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class XiVersion {
    public static final String m_revstring="$Rev: 720 $";
    public static final String m_extension="";
// overlap  
//    public static final int m_major = 1;
//    public static final int m_minor = 2;
//    public static int m_build = -1;
    public static Version version = new Version(1, 6, m_revstring,m_extension);
     
    
    public final String changes = "v1.6.720\n"+
                                  "     support for defining additional charge states and m/z values in mgf files\n" +
                                  "v1.6.719\n"+
                                  "     First try of mzML support\n" +
                                  "     ResultWriters throw IOExceptions"+ 
                                  "v1.6.718\n"+
                                  "     BugFix for multiple acquisitions searched\n"+ 
                                  "v1.6.717\n"+
                                  "     new option MAXPEAKCANDIDATES to restrict the maximum ambiguity a peak can have to be considered for candidate selection\n"+ 
                                  "v1.6.716\n"+
                                  "     new integer based fragment tree\n"+ 
                                  "v1.6.714\n"+
                                  "     Speed up for candidate selection\n"+ 
                                  "v1.6.713\n"+
                                  "     BugFix: manual start of database searches failed\n"+ 
                                  "v1.6.712\n"+
                                  "     BugFix: comma \",\" in raw-file names\n"+ 
                                  "     BugFix: Multisptep digestion\n" + 
                                  "     BugFix: Missing remove() in PermArray\n" +
                                  "v1.6.711\n"+
                                  "     WorkAround - some buffer threads appear not to close down - this is now just a workaround" + 
                                  "v1.6.693\n" + 
                                  "     BugFix: optional setting \"ConservativeLosses\" was ignored\n" +
                                  "     BugFix: LinearCrosslinker now does not declare peptides as linkable"+
                                  "v1.6.688\n" + 
                                  "     Command-line support for xi\n"+
                                  "v1.6.683\n" + 
                                  "     bugfix for writing out proteins to the xi3 database that have no name but only accession numbers \n"+
                                  "v1.6.681\n" + 
                                  "     bugfix missing new line when writing out spectrum_sources \n"+
                                  "v1.6.679\n" + 
                                  "     Still bugfix for duplicat ids writen to database \n"+
                                  "v1.6.676\n" + 
                                  "     bugfix for duplicat ids writen to database again\n"+
                                  "v1.6.675\n" + 
                                  "     bugfix for duplicat ids writen to database\n"+
                                  "v1.6.671\n" + 
                                  "     xi3 database output\n"+
                                  "     csv-output - updted preceding and succeding amino-acids\n"+
                                  "     possibility to define arbitrary regular expressions for parsing mgf-files\n"+
                                  "     disabling the debug-window now also does not load it anymore (no graphical system needed anymore)\n"+
                                  "     update for restarting searches\n"+
                                  "v1.6.658\n" + 
                                  "     BugFix for duplicated keys trying to be writen into database (has_protein-table)\n"+
                                  "v1.6.656\n" + 
                                  "     Bugfix for multistepdigest\n"+
                                  "v1.6.655\n" + 
                                  "     support for stacking of multistep digestion and parallel digestion\n"+
                                  "v1.6.653\n" + 
                                  "     support for multistep digestion and parallel digestion\n"+
                                  "v1.6.650\n" + 
                                  "     bugfix for protein haveing null name not writen out to database\n"+
                                  "v1.6.649\n" + 
                                  "     limited suport for another openms converted mgf files\n"+
                                  "v1.6.646\n" + 
                                  "     BugFix for annotation of overlapping isotope clusters (extension of isotope cluster to lighter peaks)\n"+
                                  "     BugFix for generation of reversed sequences could lead to protein names of just \"REV_\"\n"+
                                  "     BugFix Version.toString() failing when extension is null\"\n"+
                                  "     Updated default config for offline Xi\n" +
                                  "v1.6.643\n" + 
                                  "     BugFix for MSM-iterator expecting FinneganScanNumber to be lower case\n"+
                                  "     Some changes to cross-linker needed for mzIdentML export from xiFDR\n"+
                                  "v1.6.642\n" + 
                                  "     small bugfix in no search related functions\n"+
                                  "v1.6.641\n" + 
                                  "     Reduced Memory consumtion\n" +
                                  "     OpenMS-Style mgf files can now be read in\n"+
                                  "v1.5.636\n" + 
                                  "     bugfix ZipMSMListIteraotor now opens the zip file as read only\n" +
                                  "     Decoys can now be forced to be considered even if they are also found as targets\n"+
                                  "v1.5.632\n" + 
                                  "     bugfix DBRunConfig\n" +
                                  "v1.5.628\n" + 
                                  "     bugfix DBRunConfig\n" +
                                  "v1.5.627\n" + 
                                  "     SLight bugfix for memory-display in debug-window\n"+
                                  "v1.5.626\n" + 
                                  "     Possibility for automatic gc when memory falls below 10mb - mainly for debug-purposes to log how much memory is actually use/available \n" +
                                  "     detection of possible linears now uses unique peak-matches instead of primary explanations - to make it more robust in not flagging likely linears as autovalidated\n"+
                                  "v1.5.624\n" + 
                                  "     BugFixes for the debug-window not showning memory informations" +
                                  "     enabled modification to be c- or n-terminal\n"+
                                  "v1.5.619\n" + 
                                  "     BugFixes in aminoacid iterator for peptides and sequences" +
                                  "     ToleranceUnit now provides functions to calculate the error as defined by the tolerance unit - meaning as Da-unit or ppm unit\n"+
                                  "v1.5.618\n" + 
                                  "     Several BugFixes and some adaptions needed for the on demand annotator" +
                                  "v1.5.609\n" +
                                  "     Bugfix+: hopefully a \"bugfix\" for searches not finish writing - actually more of a workaround \n" +
                                  "v1.5.594\n" +
                                  "     Bugfix+: for randomly generated peptides \n" +
                                  "v1.5.592\n" +
                                  "     Bugfix: Randomly generated decoy peptides where not correctly flaged up as beeing the same N- and C- terminal wise as the original peptide\n" +
                                  "v1.5.591\n" +
                                  "     Bugfix: Likely linear matches where auto-validated\n" +
                                  "v1.5.588\n" +
                                  "     Bugfix: unrestricted crosslinker was screwed by the narry cross-linker\n" +
                                  "v1.5.587\n" +
                                  "Bugfix: sometimes a match of two peptides that are not really cross-linkable made it through and then crashed in the new LinkSiteDelta score.\n" +
                                  "	now peptides get tested for linkability before doing a full matching.\n" +
                                  "v1.5.584\n" +
                                  "Bugfix: sometimes a match of two peptides that are not really cross-linkable made it through and then crashed in the new LinkSiteDelta score.\n" +
                                  "	now peptides get tested for linkability before doing a full matching.\n" +
                                  "v1.4.582 \n" +
                                  "minimum requirementsfilter is back\n"+
                                  "Link-Site weight ignores losses of more then 3 things\n"+
                                  "Link-Site weight losses have less impact\n"+
                                  "xi-version used during search is written into search notes\n"+
                                  "v1.4.581 \n" +
                                  "new xi version with updated scores and autovalidation" +
                                  "v1.3.560 \n" +
                                  "Adam HSA research \n"+
                                  "v1.3.553 \n" +
                                  "Version for Marta Mendes ETD corrected\n" +
                                  "v1.3.600\n" +
                                  "Added flushing of result writers.\n"+
                                  "Multithreaded gather data for chacking maximum peptide-sizes\n" + 
                                  "restructured filter into sub-folders\n"+ 
                                  "v1.3.400\n" +
                                  "Deadlock detectionz\n" +
                                  "empty files in zips are ignored\n" +
                                  "Targeted modifciations can be enabled by dummy crosslinker with name TargetModification\n" +
                                  "Compatible to mgf files of newer versions of ProteoWizard MSConvert\n"+ 
                                  "Support for low-resolution ms2\n" +
                                  "OpenModification output can be restricted to only modified peptides" +
                                  "v1.3.355\n" +
                                  "AutoValidation only validates with score >7 and possible linears must have some support to show, that they are cross-linked\n" +
                                  "Better process-information\n" +
                                  "Database-output changed to ad some new fields (e.g. Peptide-length)\n"+
                                  "Digestion: AminoAcids defined for specificity and restriction that are not known will be ignored\n" +
                                  "Support for ms-convert generated mgf-files\n" +
                                  "Support for MassMatrix-generated mgf-files\n" +
                                  "Fasta-files can be gziped\n"+
                                  "MGF-Files can be gziped or zip-compressed\n" +
                                  "Fasta-header are parsed and split into name accession and description\n" +
                                  "Some changes, to make the non-guiparts compatible with jdk 1.5\n"+
                                  "ScanFilter tries to ignore the extension\n" +
                                  "BugFix for spectra, that are empty or would create an empty mgc-spectra\n" +
                                  "BugFix for ProteinDiscoverer generated mgf-files\n" +
                                  "BugFix in AsymetricAminoAcidRestricted Cross-linker to actually be Asymetric\n" +
                                  "BugFix - Format of the CSV-export\n" +
                                  "BugFix ToleranceUnit to handle \"0 da\"\n" +
                                  "BugFix for some APL-files\n"+
                                  "BugFix to the msm-iterator\n" +
                                  "BugFix for weighted cross-linker sites\n" +
                                  "BugFix in some places where a maximum charge state of 10 defined - removed\n" +
                                  "BugFix to the isotop-detection\n" +
                                  "v1.3.316\n" +
                                  "BugFix release\n" +
                                  "second-last aminoacid being crosslinkable not beeing regocnised\n" +
                                  "exported precursor error was wrong\n" +
                                  "APL-input had an error in the first run of gathering information about the APL-file\n"+
                                  "additionally Lysin got some hardcoded precedence for the crosslinking-site"+
                                  "1.2.258\n" +
                                  "Label working now - but schemes are ignored - so only Heavy and light no medium\n" +
                                  "Bugfix for the cutoff heuristics" +
                                  "1.2.259\n" +
                                  "BugFix for digestion and crosslinker did not handle labeled amino-acids\n" + 
                                  "1.2.315\n" + 
                                  "BugFix for second-last aminoacid being crosslinkable not beeing regocnised\n" +
                                  "Hardcoded preference for lysins (until the weighted implementaions is up and running) \n"+
                                  "A lot of code-cleanup" ;


    public static String getVersionString() {
        return version.toString();
    }


    public static void main(String[] args) {
        if (args.length >0) {
            javax.swing.JOptionPane.showMessageDialog(null, getVersionString(), "Version", javax.swing.JOptionPane.PLAIN_MESSAGE);
        }
        System.err.println(getVersionString());
 
    }


}
