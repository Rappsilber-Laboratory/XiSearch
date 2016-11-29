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
package rappsilber.applications;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.crosslinker.SymetricSingleAminoAcidRestrictedCrossLinker;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.AminoAcidSequence;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ms.sequence.digest.PostAAConstrainedDigestion;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class CountPeptides {

    public static void main(String[] argv) throws IOException{
        SequenceList sl = new SequenceList(SequenceList.DECOY_GENERATION.ISTARGET, new File("MyFastaFiel"), null);
        PostAAConstrainedDigestion trypsin = new PostAAConstrainedDigestion(new AminoAcid[]{AminoAcid.K,AminoAcid.R}, new AminoAcid[]{AminoAcid.P});
        SymetricSingleAminoAcidRestrictedCrossLinker CrosslinkAny =
                new SymetricSingleAminoAcidRestrictedCrossLinker("", 0, 0, new AminoAcid[]{
                            AminoAcid.A,
                            AminoAcid.C,
                            AminoAcid.D,
                            AminoAcid.E,
                            AminoAcid.F,
                            AminoAcid.G,
                            AminoAcid.H,
                            AminoAcid.I,
                            AminoAcid.K,
                            AminoAcid.L,
                            AminoAcid.M,
                            AminoAcid.N,
                            AminoAcid.O,
                            AminoAcid.P,
                            AminoAcid.Q,
                            AminoAcid.R,
                            AminoAcid.S,
                            AminoAcid.T,
                            AminoAcid.U,
                            AminoAcid.V,
                            AminoAcid.W,
                            AminoAcid.Y,
                            AminoAcid.Z});
        ArrayList<CrossLinker> cl = new ArrayList<CrossLinker>(1);
        cl.add(CrosslinkAny);
        sl.digest(trypsin, cl);
        Iterator<Peptide> pit = sl.peptides();
        int ccount = 0;
        while (pit.hasNext()) {
            Peptide p = pit.next();
            if (p.containsAminoAcid(AminoAcid.C)) {
                ccount++;
            }
        }
        System.out.println(ccount);
    }


}
