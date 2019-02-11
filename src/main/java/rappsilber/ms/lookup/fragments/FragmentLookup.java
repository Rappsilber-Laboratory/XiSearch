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
package rappsilber.ms.lookup.fragments;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.lookup.Lookup;
import rappsilber.ms.lookup.Lookup;
import rappsilber.ms.sequence.Iterators.PeptideIterator;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.spectra.Spectra;
import rappsilber.utils.ArithmeticScoredOccurence;
import rappsilber.utils.ScoredOccurence;

public interface FragmentLookup extends Lookup<Peptide> {
    /**
     * return all peptides that could throw a fragment of the given mass
     * @param mass the fragment mass
     * @param referenceMass reference mass
     * @param maxMass what is the maximal mass of a peptide to be returned
     * @return 
     */
    ArrayList<Peptide> getForMass(double mass, double referenceMass, double maxMass);
    /**
     * return all peptides that could throw a fragment of the given mass
     * @param mass the fragment mass
     * @param referenceMass reference mass
     * @param maxMass what is the maximal mass of a peptide to be returned
     * @param maxPeptides only return something if not more then this number of peptides would be returned
     * @return 
     */
    ArrayList<Peptide> getForMass(double mass, double referenceMass, double maxMass, int maxPeptides);

    Map<Peptide,Double> getPeptidesForMasses(double mass);
    /**
     * 
     * @return how many fragments are registered in the tree
     */
    int getFragmentCount();
    /**
     * 
     * @param mass fragment mass
     * @return how many peptides could throw a fragment of the given mass
     */
    int countPeptides(double mass);
    /**
     * 
     * @param mass fragment mass
     * @param targetMass the size of the tolerance window is based on target mass
     * @return how many peptides could throw a fragment of the given mass
     */
    public int countPeptides(double mass, double targetMass);

    Peptide lastFragmentedPeptide();
    PeptideIterator getPeptideIterator();
//    public boolean nextRound();
    ScoredOccurence<Peptide> getAlphaCandidates(Spectra s, ToleranceUnit precursorTolerance);
    ScoredOccurence<Peptide> getAlphaCandidates(Spectra s, double maxPeptideMass);
    
    public void writeOutTree(File out) throws IOException;
    
}
