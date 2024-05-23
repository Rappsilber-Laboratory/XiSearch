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
import rappsilber.ms.lookup.peptides.PeptideLookup;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.AminoAcidSequence;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.Sequence;

/**
 * Implements a digestion with several enzymes in a step-wise manner.
 * Basically the protein gets digested by the first enzyme and the resulting peptides by the second. the then resulting peptides get digested by the third and so on.
 * The final result would be the list of peptides from the last enzyme.
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class MultiStepDigest extends Digestion{
    Digestion[] steps;
    /**
     * how do the enzyme digest the protein and peptides of previous enzymes
     */
    public static enum StepMethod{
        /**
         * every enzyme digests the protein plus the peptides of previous enzymes
         */
        full,
        /**
         * every enzyme only digests the peptides of the previous enzyme. 
         */
        consecutive,
        /**
         * every enzyme only digests the original sequences. 
         */
        independent,

        
    }
    
    StepMethod method;
    
    public MultiStepDigest(Digestion step1, Digestion step2, StepMethod method, RunConfig config) {
        super(step1.m_CTermAminoAcids.toArray(new AminoAcid[0]), step1.m_NTermAminoAcids.toArray(new AminoAcid[0]), config);
        this.steps = new Digestion[]{step1,step2};
        this.method=method;
    }

    public MultiStepDigest(Digestion[] steps, StepMethod method,RunConfig config) {
        super(steps[0].m_CTermAminoAcids.toArray(new AminoAcid[0]), steps[0].m_NTermAminoAcids.toArray(new AminoAcid[0]), config);
        this.steps = steps;
        this.method = method;
    }

    public void setPeptideLookup(PeptideLookup crosslinked, PeptideLookup linear) {
        super.setPeptideLookup(crosslinked, linear);
        for (Digestion d :steps) {
            d.setPeptideLookup(crosslinked, linear);
        }
    }    

    @Override
    public ArrayList<Peptide> digest(Sequence seq, double MaxMass, ArrayList<CrossLinker> cl) {
        ArrayList<Peptide> ret;
        if (method == StepMethod.consecutive) {
            ret = steps[0].digest(seq, MaxMass, cl); 
            for (int s = 1; s<steps.length;s++) {
                ArrayList<Peptide> pepret = new ArrayList<>();
                for (Peptide p: ret) {
                    pepret.addAll(steps[s].digest(p, MaxMass, cl));
                }
                ret = pepret;
            }
        } else if (method == StepMethod.full) {
            ret = steps[0].digest(seq, MaxMass, cl); 
            for (int s = 1; s<steps.length;s++) {
                ArrayList<Peptide> pepret = steps[s].digest(seq,MaxMass, cl);
                for (Peptide p: ret) {
                    pepret.addAll(steps[s].digest(p, MaxMass, cl));
                }
                ret.addAll(pepret);
            }
        } else {
            ret = steps[0].digest(seq, MaxMass, cl); 
            for (int s = 1; s<steps.length;s++) {
                ArrayList<Peptide> pepret = steps[s].digest(seq,MaxMass, cl);
                ret.addAll(pepret);
            }
        }

        return ret;
    }

    @Override
    protected boolean isCleavageSite(AminoAcidSequence seq, int AAPos) {
        return steps[0].isCleavageSite(seq, AAPos); //To change body of generated methods, choose Tools | Templates.
    }

    public ArrayList<Peptide> digest(ArrayList<Peptide> peps, double MaxMass, ArrayList<CrossLinker> cl) {
        ArrayList<Peptide> ret = new ArrayList<>();
        for (Peptide p: peps) {
            ret.addAll(digest(p, MaxMass, cl));
        }
        return ret;
    }

    @Override
    public ArrayList<Peptide> digest(Peptide seq, double MaxMass, ArrayList<CrossLinker> cl) {
        ArrayList<Peptide> ret = null;
        if (method == StepMethod.consecutive) {
            ret = steps[0].digest(seq, cl);
            for (int s = 1;s<steps.length;s++) {
                ArrayList<Peptide> rs = new ArrayList<>();
                for (Peptide p: ret) {
                    rs.addAll(steps[s].digest(p, MaxMass, cl));
                }
                ret = rs;
            }
        } else {
            ret = steps[0].digest(seq, cl);
            for (int s = 1;s<steps.length;s++) {
                ArrayList<Peptide> rs = steps[s].digest(seq, cl);
                for (Peptide p: ret) {
                    rs.addAll(steps[s].digest(p, MaxMass, cl));
                }
                ret.addAll(rs);
            }
        }
        return ret;
    }
    

    public static Digestion parseArgs(String args, RunConfig config) throws ParseException {
        ArrayList<Digestion> steps = new ArrayList<>();
        // Complete this and return a Digestion object
        String[] digests = args.split("\\|S\\|");
        String sMethod = digests[0].toLowerCase().trim();
        StepMethod m = null;

        for (StepMethod sm : StepMethod.values()) {
            if (sMethod.contentEquals(sm.name()) || (sMethod.length()<sm.name().length() && sm.name().substring(0, sMethod.length()).contentEquals(sMethod))) {
                m = sm;
            }
        }

        
        for (int i = 1;i<digests.length;i++) {
            String ds = digests[i];
            String[] c = ds.split(":",2);
            c[1] = c[1].replaceAll("\\\\S\\|", "S|");
            c[1] = c[1].replaceAll("\\\\P\\|", "P|");
            c[1] = c[1].replaceAll("\\\\\\\\", "\\\\");
            Digestion d = Digestion.getDigestion(c[0], c[1], config);
            steps.add(d);
        }

        return new MultiStepDigest(steps.toArray(new Digestion[steps.size()]),m,config);
    }

    
    /**
     * @param maxMissCleavages the m_maxMissCleavages to set
     */
    public void setMaxMissCleavages(int max) {
        super.setMaxMissCleavages(max);
        for (Digestion d : steps) {
            if (d.getMaxMissCleavages() <= 0) {
                d.setMaxMissCleavages(max);
            }
        }
    }    
    
}
