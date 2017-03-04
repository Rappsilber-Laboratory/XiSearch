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
import rappsilber.ms.sequence.Peptide;
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

    ArrayList<CrossLinker> getCrossLinker();

    ToleranceUnit getPrecousorTolerance();

    ToleranceUnit getFragmentTolerance();

    int getNumberMgcPeaks();

    int getMaxMissCleavages();

    public ArrayList<Fragment> includeLosses(ArrayList<Fragment> fragments,  boolean comulative);

    public SortedLinkedList<ScoreSpectraMatch> getScores();


    public int getMaxCrosslinkedPeptides();
    public void setMaxCrosslinkedPeptides(int maxpeps);

    public IsotopPattern getIsotopAnnotation();


    public AminoAcid getAminoAcid(String SequenceID);

    public ArrayList<Method> getFragmentMethods();

    public ArrayList<Method> getSecondaryFragmentMethods();

    public ArrayList<Method> getLossMethods();

    /**
     * returns the number of top - mgc hits to consider for mgx-matching
     */
    public int getTopMGCHits();

    /**
     * returns the number of top - mgx hits to consider for full matching
     */
    public int getTopMGXHits();
    
    public void register(AminoAcid aa);

    public void storeObject(Object key, Object value);

    public Object retrieveObject(Object key);

    public boolean retrieveObject(Object key, boolean defaultValue);

    public String retrieveObject(Object key, String defaultValue);

    public double retrieveObject(Object key, double defaultValue);

    public int retrieveObject(Object key, int defaultValue);

    public long retrieveObject(Object key, long defaultValue);

    void addFixedModification(AminoModification am);
    void addVariableModification(AminoModification am);
    void addKnownModification(AminoModification am);

    void addVariableNterminalPeptideModifications(NonAminoAcidModification mod);

    void addVariableCterminalPeptideModifications(NonAminoAcidModification mod);

    public StatusInterface getStatusInterface();

    public void setStatusInterface(StatusInterface i);


    public void setLowResolution();
    
    public boolean isLowResolution();
    
    public SequenceList.DECOY_GENERATION getDecoyTreatment();

    public void setDecoyTreatment(SequenceList.DECOY_GENERATION dt);
    
    void addStatusInterface(StatusInterface si);
    
    void removeStatusInterface(StatusInterface si);
    
}
