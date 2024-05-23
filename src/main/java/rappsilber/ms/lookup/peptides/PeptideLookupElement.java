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
import rappsilber.ms.sequence.AminoAcidSequence;
import rappsilber.ms.sequence.Peptide;

public class PeptideLookupElement extends ArrayList<Peptide>{
    private static final long serialVersionUID = -8152408378677201222L;
    private double m_mass;

    public PeptideLookupElement(double mass) {
        super(1);
        this.m_mass = mass;
    }



    public Peptide getElementBySequence(AminoAcidSequence aas) {

        for (Peptide p : this) {
            if (p.equalSequence(aas)) {
                return p;
            }
        }
        return null;
    }

    public Peptide getElementBySequenceAAMass(AminoAcidSequence aas) {

        for (Peptide p : this) {
            if (p.equalSequenceAAMass(aas)) {
                return p;
            }
        }
        return null;
    }

    public Peptide getElementBySequenceAAMass(AminoAcidSequence aas, boolean decoy) {

        for (Peptide p : this) {
            if (p.equalSequenceAAMass(aas) && decoy == p.isDecoy()) {
                return p;
            }
        }
        return null;
    }    
    public double getMass() {
        return m_mass;
    }

}
