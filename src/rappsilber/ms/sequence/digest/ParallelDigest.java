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

import java.text.ParseException;
import java.util.ArrayList;
import rappsilber.config.RunConfig;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.AminoAcidSequence;
import rappsilber.ms.sequence.Peptide;

/**
 * Implements a digestion with several enzymes at the same time.
 * Basically the protein gets digested at every point where any of the enzymes could cut.
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class ParallelDigest extends Digestion{
    Digestion[] steps;
    
    public ParallelDigest(Digestion step1, Digestion step2) {
        super(step1.m_CTermAminoAcids.toArray(new AminoAcid[0]), step1.m_NTermAminoAcids.toArray(new AminoAcid[0]));
        this.steps = new Digestion[] {step1,step2};
    }

    public ParallelDigest(Digestion[] steps) {
        super(steps[0].m_CTermAminoAcids.toArray(new AminoAcid[0]), steps[0].m_NTermAminoAcids.toArray(new AminoAcid[0]));
        this.steps = steps;
    }


    @Override
    protected boolean isCleavageSite(AminoAcidSequence seq, int AAPos) {
        for (Digestion d :steps) 
            if (d.isCleavageSite(seq, AAPos))
                return true;
        return false;
    }


    public static Digestion parseArgs(String args, RunConfig config) throws ParseException {
        ArrayList<Digestion> steps = new ArrayList<>();
        // Complete this and return a Digestion object
        String[] digests = args.split("\\|P\\|");
        
        for (String ds : digests) {
            String[] c = ds.split(":",2);
            c[1] = c[1].replaceAll("\\S|", "S|");
            c[1] = c[1].replaceAll("\\P|", "P|");
            
            Digestion d = Digestion.getDigestion(c[0], c[1], config);
            steps.add(d);
        }


        return new ParallelDigest(steps.toArray(new Digestion[steps.size()]));
    }
    
}
