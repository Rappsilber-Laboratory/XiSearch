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
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.crosslinker.CrossLinker;
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
    ArrayList<AminoModification> getFixedModifications();

    /**
     * list of modifications, that should be assumed as fixed modifications
     * ( each occurrence of the base-aminoacid is replaced with the modified version
     * @return
     */
    ArrayList<AminoModification> getFixedModifications(AminoAcid base);

    /**
     * list of all variable modification considered during the search
     * @return
     */
    ArrayList<AminoModification> getVariableModifications();

    /**
     * list of all variable modification that can be applied to the given aminoacid
     * @return
     */
    ArrayList<AminoModification> getVariableModifications(AminoAcid aa);

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
    
}
