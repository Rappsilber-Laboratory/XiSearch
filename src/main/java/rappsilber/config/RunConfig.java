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
package rappsilber.config;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.dataAccess.AbstractStackedSpectraAccess;
import rappsilber.ms.dataAccess.filter.spectrafilter.AbstractSpectraFilter;
import rappsilber.ms.score.ScoreSpectraMatch;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.AminoLabel;
import rappsilber.ms.sequence.AminoModification;
import rappsilber.ms.sequence.NonAminoAcidModification;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ms.sequence.digest.Digestion;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.spectra.annotation.IsotopPattern;
import rappsilber.ui.StatusInterface;
import rappsilber.utils.SortedLinkedList;
import rappsilber.ms.sequence.ions.CrossLinkedFragmentProducer;
import rappsilber.ms.spectra.Spectra;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public interface RunConfig {

    /**
     * The digestion-method used for splitting the proteins into peptides
     * @return the digestion_method
     */
    Digestion getDigestion_method();


    /**
     * list of used labels
     * @return
     */
    ArrayList<AminoLabel> getLabel();

    /**
     * list of label that can be applied to the given amino acid
     * @param aa
     * @return
     */
    ArrayList<AminoLabel> getLabel(AminoAcid aa);

    /**
     * list of modifications, that should be assumed as fixed modifications
     * ( each occurrence of the base-aminoacid is replaced with the modified version
     * @return
     */
    ArrayList<AminoModification> getFixedModificationsPreDigest();

    /**
     * list of modifications, that should be assumed as fixed modifications
     * ( each occurrence of the base-aminoacid is replaced with the modified version
     * @return
     */
    ArrayList<AminoModification> getFixedModificationsPostDigest();

    ArrayList<AminoModification> getFixedModifications();
    /**
     * list of modifications, that should be assumed as fixed modifications
     * ( each occurrence of the base-aminoacid is replaced with the modified version
     * @return
     */
    ArrayList<AminoModification> getFixedModificationsPreDigest(AminoAcid base);

    /**
     * list of modifications, that should be assumed as fixed modifications
     * ( each occurrence of the base-aminoacid is replaced with the modified version
     * @return
     */
    ArrayList<AminoModification> getFixedModificationsPostDigest(AminoAcid base);
    
    /**
     * list of all variable modification considered during the search
     * @return
     */
    ArrayList<AminoModification> getVariableModifications();
    /**
     * list of all variable modification considered during the search
     * for linear peptides only
     * @return
     */
    ArrayList<AminoModification> getLinearModifications();

    
    /**
     * list of all variable modification that can be applied to the given aminoacid
     * @return
     */
    ArrayList<AminoModification> getVariableModifications(AminoAcid aa);

    /**
     * list of all variable modification that can be applied to the given aminoacid
     * @return
     */
    ArrayList<AminoModification> getVariableModificationsPostDigest(AminoAcid aa);
    
    /**
     * list of all variable modification that can be applied to the given aminoacid
     * on linear peptides only
     * @return
     */
    ArrayList<AminoModification> getLinearModifications(AminoAcid aa);
    ArrayList<AminoModification> getLinearModificationsPostDigest(AminoAcid aa);
    
    /**
     * list of modifications, that are known but not automatically used.
     * Main use is to define modifications as part of the fasta file.
     * @return
     */
    ArrayList<AminoModification> getKnownModifications();

    /**
     * list of modifications, that are known for the given amino acid but not automatically used.
     * Main use is to define modifications as part of the fasta file.
     * @return
     */
    ArrayList<AminoModification> getKnownModifications(AminoAcid base);
    
    /**
     * list of all variable modification considered during the search
     * @return
     */
    ArrayList<NonAminoAcidModification> getVariableNterminalPeptideModifications();
    /**
     * list of all variable modification considered during the search
     * @return
     */
    ArrayList<NonAminoAcidModification> getVariableCterminalPeptideModifications();
    
    /**
     * returns all known aminoacids, modified aminoacids and labeled aminoacids
     * @return 
     */
    Collection<AminoAcid> getAllAminoAcids();

    /**
     * @return list of cross-linker to be searched 
     */
    ArrayList<CrossLinker> getCrossLinker();

    /**
     * @return MS1 tolerance
     */
    ToleranceUnit getPrecousorTolerance();

    /**
     * @return MS2 tolerance
     */
    ToleranceUnit getFragmentTolerance();

    /**
     * @return The fragment error considered during candidate selection
     */
    ToleranceUnit getFragmentToleranceCandidate();
    
    /**
     * @return the maximum number peaks for the spectrum used to generate alpha 
     * peptide candidates
     */
    int getNumberMgcPeaks();

    /**
     * @return the maximum number of miss-cleavages
     */
    int getMaxMissCleavages();

    /**
     * Return a list of all possible losses to the given list of fragments
     * @param comulative if true also the fragments themself are returned
     * @return list of lossy fragments
     */
    public ArrayList<Fragment> includeLosses(ArrayList<Fragment> fragments,  boolean comulative);

    /**
     * A sorted list of scoring methods for evaluating PSMs
     * @return 
     */
    public SortedLinkedList<ScoreSpectraMatch> getScores();


    /**
     * set the maximum number of peptides in a cross-linked PSM.
     * Currently not really in use
     * @return maximum number of peptides per PSM;
     */
    public int getMaxCrosslinkedPeptides();
    
    /**
     * set the maximum number of peptides in a cross-linked PSM.
     * Currently not really in use
     * @param maxpeps 
     */
    public void setMaxCrosslinkedPeptides(int maxpeps);

    /**
     * Get the method used to annotate isotope patterns
     * @return The method for annotating Isotope patterns
     */
    public IsotopPattern getIsotopAnnotation();


    /**
     * Get the amino acid/modification/label represented by this sequence id
     * @param SequenceID
     * @return 
     */
    public AminoAcid getAminoAcid(String SequenceID);

    /**
     * List of methods used to generate fragments 
     * @TODO this is bad
     * @return 
     */
    public ArrayList<Method> getFragmentMethods();

    /**
     * List of methods used to generate secondary fragments (e.g. neutral losses)
     * @TODO this is bad
     * @return 
     */
    public ArrayList<Method> getSecondaryFragmentMethods();
    
    /**
     * List of methods used to generate losses
     * @TODO this is bad
     * @return 
     */
    public ArrayList<Method> getLossMethods();

    /**
     * returns the number of top - mgc hits to consider for mgx-matching
     */
    public int getTopMGCHits();

    /**
     * returns the number of top - mgx hits to consider for full matching
     */
    public int getTopMGXHits();
    
    /**
     * @deprecated 
     * @param aa 
     */
    public void register(AminoAcid aa);

    /**
     * Stores an arbitrary object in the config.
     * This can be used to carry information through the search, as most objects 
     * involved with a search will have access to the config.
     * Currently abused a lot for storing arbritrary config options that have no
     * defined entry in here.
     * @param key the name of the config entry
     */
    public void storeObject(Object key, Object value);

    /**
     * Try to get some arbitrary defined object from the Config.
     * This can be used to carry information through the search, as most objects 
     * involved with a search will have access to the config.
     * Currently abused a lot for storing arbritrary config options that have no
     * defined entry in here.
     * @param key the name of the config entry
     * @return null if no object is stored under this key; other the stored object
     */
    public Object retrieveObject(Object key);

    /**
     * Try to get some arbitrary defined object from the Config.
     * This can be used to carry information through the search, as most objects 
     * involved with a search will have access to the config.
     * Currently abused a lot for storing arbritrary config options that have no
     * defined entry in here.
     * <p> This tries to interpret the value as of type boolean</p>
     * @param key the name of the config entry
     * @param defaultValue default value
     * @return defaultValue if no object is stored under this key; other the stored object interpreted as boolean
     */
    public boolean retrieveObject(Object key, boolean defaultValue);

    /**
     * Try to get some arbitrary defined object from the Config.
     * This can be used to carry information through the search, as most objects 
     * involved with a search will have access to the config.
     * Currently abused a lot for storing arbritrary config options that have no
     * defined entry in here.
     * <p> This tries to interpret the value as of type String</p>
     * @param key the name of the config entry
     * @param defaultValue default value
     * @return defaultValue if no object is stored under this key; other the stored object interpreted as String
     */
    public String retrieveObject(Object key, String defaultValue);

    /**
     * Try to get some arbitrary defined object from the Config.
     * This can be used to carry information through the search, as most objects 
     * involved with a search will have access to the config.
     * Currently abused a lot for storing arbritrary config options that have no
     * defined entry in here.
     * <p> This tries to interpret the value as of type double</p>
     * @param key the name of the config entry
     * @param defaultValue default value
     * @return defaultValue if no object is stored under this key; other the stored object interpreted as double
     */
    public double retrieveObject(Object key, double defaultValue);

    /**
     * Try to get some arbitrary defined object from the Config.
     * This can be used to carry information through the search, as most objects 
     * involved with a search will have access to the config.
     * Currently abused a lot for storing arbritrary config options that have no
     * defined entry in here.
     * <p> This tries to interpret the value as of type int</p>
     * @param key the name of the config entry
     * @param defaultValue default value
     * @return defaultValue if no object is stored under this key; other the stored object interpreted as int
     */
    public int retrieveObject(Object key, int defaultValue);

    /**
     * Try to get some arbitrary defined object from the Config.
     * This can be used to carry information through the search, as most objects 
     * involved with a search will have access to the config.
     * Currently abused a lot for storing arbritrary config options that have no
     * defined entry in here.
     * <p> This tries to interpret the value as of type long</p>
     * @param key the name of the config entry
     * @param defaultValue default value
     * @return defaultValue if no object is stored under this key; other the stored object interpreted as long
     */
    public long retrieveObject(Object key, long defaultValue);

    /**
     * Register a modification as fixed modification.
     * Fixed modification arte applied on the protein level - before digest.
     * Every amino-acid that can be modified will be modified
     * @param mod the modification
     */
    void addFixedModification(AminoModification am);

    /**
     * register a modification as variable modification.
     * Peptides containing amino-acids that can be modified by this  will be 
     * searched both with and without this modification
     * @param mod the modification
     */
    void addVariableModification(AminoModification am);

    /**
     * register a modification as variable modification on linear peptides.
     * Peptides containing amino-acids that can be modified by this  will be 
     * searched both with and without this modification if they are not part of 
     * a crosslink.
     * @param mod the modification
     */
    void addLinearModification(AminoModification am);
    /**
     * register a modification as known modification.
     * Xi is aware of these modifications but does not automatically makes use 
     * of these. Currently the only use is to predefine modifications as part of 
     * the FASTA file. 
     * @param mod the modification
     */
    void addKnownModification(AminoModification am);

    /**
     * register a modification as variable n-terminal modification
     * @param mod the modification
     * @deprecated n-terminal modifications are now also represented via 
     * standard modifications
     */
    void addVariableNterminalPeptideModifications(NonAminoAcidModification mod);

    /**
     * register a modification as variable modification
     * @param mod 
     */
    void addVariableCterminalPeptideModifications(NonAminoAcidModification mod);

    /**
     * get the currently defined status interface. Per default this is a 
     * multiplexer that writes out the status to all registered 
     * status-interfaces.
     * @see #addStatusInterface(rappsilber.ui.StatusInterface) 
     * @param i 
     */
    public StatusInterface getStatusInterface();

    /**
     * define where to send status messages to
     * @deprecated use {@link #addStatusInterface} instead
     * @param i 
     */
    public void setStatusInterface(StatusInterface i);


    /**
     * Define the current search run as low resolution search.
     * Low resolution means fewer preprocessing steps are taken for candidate 
     * selection. E.g. no de-isotoping, de-charging, and no linearisation
     * @return whether current search is a low resolution search
     */
    public void setLowResolution();

    /**
     * Is the current search run as low resolution search.
     * Low resolution means fewer preprocessing steps are taken for candidate 
     * selection. E.g. no de-isotoping, de-charging, and no linearisation
     * @return whether current search is a low resolution search
     */
    public boolean isLowResolution();

    /**
     * some duplication here - AUTODECOY, DECOY_GENERATION, and 
     * DECOY_DIGESTION_AWARE seem to supersede this now
     * @return 
     */
    public SequenceList.DECOY_GENERATION getDecoyTreatment();

    /**
     * Defines if and how decoys are generated.
     * @see rappsilber.ms.sequence.SequenceList.DECOY_GENERATION
     * @param dt 
     */
    public void setDecoyTreatment(SequenceList.DECOY_GENERATION dt);

    /**
     * during alpha peptide candidate selection only consider peaks that do not
     * return more then this number of peaks.
     * @return -1 for unlimited; otherwise the maximum number of candidates
     */
    public int getMaximumPeptideCandidatesPerPeak();

    
    /**
     * add a listener for status messages
     * @param si status listener
     */
    void addStatusInterface(StatusInterface si);
    
    /**
     * remove a listener for status messages
     * @param si status listener
     */
    void removeStatusInterface(StatusInterface si);
 
    /**
     * what is the maximum mass a peptide can have that we consider
     * @return 
     */
    double getMaxPeptideMass();
    void setMaxPeptideMass(double mass);
    
    /**
     * should linear peptides be considered for matching spectra
     * @return 
     */
    boolean isEvaluateLinears();

    /**
     * how many threads for spectrum matching should be started
     * @return 
     */
    int getPreSearchThreads();
    
    /**
     * how many threads for spectrum matching should be started
     * @return 
     */
    int getSearchThreads();
    
    /**
     * Should only he top-ranking matches be writen out?
     * @return 
     */
    boolean getTopMatchesOnly();
    
    
    /**
     * registers an object that can produce cross-linked fragments based on 
     * linear fragments
     * @param producer the producer
     * @param isprimary should these fragments be considered during candidate selection
     */
    void addCrossLinkedFragmentProducer(CrossLinkedFragmentProducer producer, boolean isprimary);

    /**
     * get a list of all registered objects that can produce cross-linked 
     * fragments based on linear fragments
     */
    ArrayList<CrossLinkedFragmentProducer> getCrossLinkedFragmentProducers();


    /**
     * get a list of all registered objects that can produce cross-linked 
     * fragments based on linear fragments and that should be considered during 
     * primary candidate selection
     */
    ArrayList<CrossLinkedFragmentProducer> getPrimaryCrossLinkedFragmentProducers();

    /**
     * A list of precursor m/z offset that each spectrum should be searched with
     */
    ArrayList<Double> getAdditionalPrecursorMZOffsets();

    /**
     * A list of precursor m/z offset that each spectrum with unknown precursor 
     * charge state should be searched with
     */
    ArrayList<Double> getAdditionalPrecursorMZOffsetsUnknowChargeStates();
 
    
    //rappsilber.utils.Util.MaxModificationPerPeptide = m_config.retrieveObject("MAX_MODIFICATION_PER_PEPTIDE", rappsilber.utils.Util.MaxModificationPerPeptide);
    /**
     * maximum number of variable modification on a peptide
     * @return 
     */
    int getMaximumModificationPerPeptide();
    //rappsilber.utils.Util.MaxModifiedPeptidesPerPeptide = m_config.retrieveObject("MAX_MODIFIED_PEPTIDES_PER_PEPTIDE", rappsilber.utils.Util.MaxModifiedPeptidesPerPeptide);
    /**
     * maximum number of modified peptides derived from a single peptide
     * @return 
     */
    int getMaximumModifiedPeptidesPerPeptide();

    /**
     * maximum number of modified peptides derived from a single peptide by modifications defined in the fasta file
     * @return 
     */
    int getMaximumModificationPerFASTAPeptide();
    //rappsilber.utils.Util.MaxModifiedPeptidesPerPeptide = m_config.retrieveObject("MAX_MODIFIED_PEPTIDES_PER_PEPTIDE", rappsilber.utils.Util.MaxModifiedPeptidesPerPeptide);
    /**
     * maximum number of modified peptides derived from a single peptide by modifications defined in the fasta file
     * @return 
     */
    int getMaximumModifiedPeptidesPerFASTAPeptide();
    //rappsilber.utils.Util.MaxModificationPerPeptide = m_config.retrieveObject("MAX_MODIFICATION_PER_PEPTIDE", rappsilber.utils.Util.MaxModificationPerPeptide);

    /**
     * Should the current search be stopped.
     * This could be some external event that triggers setting of this flag 
     * (e.g. the search was deleted from the db before i was finished searching).
     * All search threads should regularly query this and shutdown if this falg is set to true
     * @return 
     */
    boolean searchStopped();
    /**
     * Should the current search be stopped.
     * This could be some external event that triggers setting of this flag 
     * (e.g. the search was deleted from the db before i was finished searching).
     * All search threads should regularly query this and shutdown if this falg is set to true
     * @return 
     */
    void stopSearch();

    /**
     * if masses for individual peptides are given for a spectrum assume the 
     * following tolerance for these.
     * @return 
     */
    ToleranceUnit getSpectraPeptideMassTollerance();
    
    /**
     * if masses for individual peptides are given should these be treated as 
     * the only candidates or as prioritised candidates (i.e. make sure they are 
     * among the tested ones)
     * @return 
     */
    boolean getXLPeptideMassCandidatesExclusive();


    /**
     * Should decoys be automatically be generated
     */
    boolean autoGenerateDecoys();
    
    /**
     * get a list of configured input filter
     * @return 
     */
    ArrayList<AbstractStackedSpectraAccess> getInputFilter();
    
//    /**
//     * if matches have some associated weight then this defines how they affect the sorting of match-candidates
//     * @return the matchWeightMultiplication
//     */
//    public boolean isMatchWeightMultiplication();
//
//    /**
//     * if matches have some associated weight then this defines how they affect the sorting of match-candidates
//     * @param matchWeightMultiplication the matchWeightMultiplication to set
//     */
//    public void setMatchWeightMultiplication(boolean matchWeightMultiplication);
//
//    /**
//     * if matches have some associated weight then this defines how they affect the sorting of match-candidates
//     * @return the matchWeightAddition
//     */
//    public boolean isMatchWeightAddition();
//
//    /**
//     * if matches have some associated weight then this defines how they affect the sorting of match-candidates
//     * @param matchWeightAddition the matchWeightAddition to set
//     */
//    public void setMatchWeightAddition(boolean matchWeightAddition);
 
    
    /**
     * Returns the defined protein groups.
     * if protein groups are defined then matches are only considered possible when they are within one protein group.
     * e.g.given two protein groups:
     * 0 : A,B,C
     * 1 : A,C,D
     * acceptable matches:
     *  A:A A:B A:C A:D B:C A:D
     * not acceptable matches:
     *  B:D as these would cross the border of groups
     * @param groupName the name of the group that it should be added to 
     * @param accession the accession number of the protein
     */
    HashMap<String,HashSet<String>> getProteinGroups();
    
    
    /**
     * when looking at fragments also look for fragments with the given mass 
     * deltas for each fragment.
     * @return 
     */
    Collection<Double> getAlphaCandidateDeltaMasses();
 
    
    /**
     * Intensities of peaks at these masses should be reported in the result file
     * @return 
     */
    Collection<Double> getReporterPeaks();
    
    ArrayList<String> getConfigLines();
    
    /**
     * If a search fails do to an error - this provides a means to flag the error 
     * @param message
     * @param ex 
     */
    void flagError(String message, Exception ex, Spectra spectrum, boolean stopSearch);
    
    
    boolean hasError();
    
    String errorMessage();

    Spectra errorSpectrum();
    
    Exception errorException();
}
