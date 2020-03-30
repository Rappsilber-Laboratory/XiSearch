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
package rappsilber.ms.lookup.peptides;

import java.util.ArrayList;
import java.util.Iterator;
import rappsilber.config.RunConfig;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.lookup.Lookup;
import rappsilber.ms.sequence.Iterators.PeptideIterator;
import rappsilber.ms.sequence.Peptide;

public interface PeptideLookup extends Lookup<Peptide>, Iterable<Peptide> {
    

    /**
     * return all peptides, that have exactly this mass
     * @param mass
     * @return 
     */
    ArrayList<Peptide> getPeptidesByExactMass(double mass);

    
    /**
     * add a peptide to the tree
     * @param p 
     */
    public void addPeptide(Peptide p);

    /**
     * apply variable modifications.
     * meaning all peptides, that can have a variable modification are duplicated with the variable modification.
     * Modified peptides, that would not be cross-linkable are discarded
     * @param config 
     */
    public void applyVariableModifications(RunConfig config);

    /**
     * apply variable modifications.
     * meaning all peptides, that can have a variable modification are duplicated with the variable modification
     * all modified peptides, that would not be cross-linkable, would be added to the linear tree
     * @param config 
     * @param linear 
     */
    public void applyVariableModifications(RunConfig config, PeptideLookup linear);
    
    
    /**
     * apply variable modifications.
     * This function assumes that this tree contains the linear-tree
     * meaning all peptides, that can have a variable modification are duplicated with the variable modification
     * and all modified peptides, that would now be cross-linkable, would be added to the cross-link tree
     * 
     * @param conf
     * @param Crosslinked  
     */
    public void applyVariableModificationsLinear(RunConfig conf, PeptideLookup Crosslinked);

    public PeptideLookup applyFixedModificationsPostDigestLinear(RunConfig conf, PeptideLookup Crosslinked);

    public PeptideLookup applyFixedModificationsPostDigest(RunConfig conf, PeptideLookup linear);
    
    /**
     * how many peptides are stored
     * @return 
     */
    public int size();

    /**
     * delete peptides, that should not be considered
     */
    public void cleanup();
    
    /**
     * delete peptides, that should not be considered
     */
    public void cleanup(int minLength);

    /**
     * delete peptides, that should not be considered
     */
    public void cleanup(int minLength, int maxAmbiguity);

    public void cleanup(int minLength, int maxAmbiguity, int maxProtAmbiguity);
   
    /**
     * an iterator, over all peptides
     * @return 
     */
    public PeptideIterator iterator();

    /**
     * an iterator, over all peptides, starting at after the given peptide
     * @return 
     */
    public PeptideIterator iteratorAfter(Peptide p);
    
    /**
     * what is the smallest stored peptide in the tree
     * @return 
     */
    public double getMinimumMass();

    /**
     * what is the largest stored peptide in the tree
     * @return 
     */
    public double getMaximumMass();

    
    
    ArrayList<Peptide> getForMassRange(double minmass, double maxmass, double referenceMass);

    ArrayList<Peptide> getForExactMassRange(double minmass, double maxmass);
    
    public double countOffset(double offset);
    
    
    /**
     * tries to replace decoy-peptides that where also found as target peptides
     * with randomly generated decoy-peptides. And thereby providing more equal 
     * decoy database.
     * @param conf the configuration - needed to get a list of amino-acids
     * @return the list of peptides, that could not be replaced by an unique
     * random peptide of same length
     */
    public ArrayList<Peptide> addDiscared(RunConfig conf);

    /**
     * tries to replace decoy-peptides that where also found as target peptides
     * with permuted decoy-peptides. And thereby providing more equal 
     * decoy database.<br>
     * The advantage against using random peptide is, that the system stays 
     * reproducible. (The permutation here used does not do a random permutation)
     * @param conf the configuration - needed to get a list of amino-acids
     * @return the list of peptides, that could not be replaced by an unique
     * permuted peptide
     */
    public ArrayList<Peptide> addDiscaredPermut(RunConfig conf);

    public void forceAddDiscarded();
    
    
}
