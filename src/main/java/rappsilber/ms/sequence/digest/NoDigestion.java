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
package rappsilber.ms.sequence.digest;

import java.util.ArrayList;
import rappsilber.config.AbstractRunConfig;
import rappsilber.config.RunConfig;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.Sequence;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class NoDigestion extends Digestion{

    public NoDigestion() {
        super(new AminoAcid[0], new AminoAcid[0], new AbstractRunConfig() {
        });
    }

    public boolean  isDigestedPeptide(Peptide p) {
        return true;
    }



    @Override
    public ArrayList<Peptide> digest(Sequence seq, double MaxMass, ArrayList<CrossLinker> cl) {
        ArrayList<Peptide> peptide = new ArrayList<Peptide>(1);
        addPeptide(new Peptide(seq, 0, seq.length()), seq, peptide);
        //peptide.add(new Peptide(seq, 0, seq.length()));
        return peptide;
    }

    public static Digestion parseArgs(String args, RunConfig conf) {
        return new NoDigestion();
    }

    public static Digestion parseArgs(String args) {
        return new NoDigestion();
    }

}
