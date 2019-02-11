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
package rappsilber.ms.spectra.match;

import java.util.ArrayList;
import java.util.HashMap;
import rappsilber.config.RunConfig;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.AminoModification;
import rappsilber.ms.sequence.Sequence;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.DoubleFragmentation;

/**
 * represents the match between a spectrum and a list of peptides.
 * This 
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class MatchedXlinkedPeptideWeightedNnary  extends MatchedXlinkedPeptideWeighted {

    Peptide[] peptides;
    Peptide[] modpeptide_complement;

    Peptide[] matchedMasses;
    
    Boolean m_isInternal = null;
    
    boolean m_isDecoy = false;
    
    MatchedXlinkedPeptideWeighted[] subMatches;

    String key = null;
    
    




//    /**
//     * creates a new match
//     * @param spectra the spectrum, that should be matched
//     * @param peptide1 the first peptide, that should be matched
//     * @param peptide2 the second peptide, that should be matched
//     * @param fragmentTolerance the tolerance for the search for fragmentPrimary-matches (MS2 tolerance)
//     * @param crosslinker the used crosslinker
//     */
//    public MatchedXlinkedPeptideWeightedNnary(Spectra spectra,
//                                Peptide[] peptides,
//                                ToleranceUnit fragmentTolerance,
//                                CrossLinker crosslinker) {
//        super(spectra, peptides[0], getPeptideMassComplements(peptides, 0),fragmentTolerance, crosslinker);
//        this.peptides = peptides;
//        subMatches= new MatchedXlinkedPeptideWeighted[peptides.length];
//        for (int i = 0; i< peptides.length;i++) {
//            subMatches[i] = new MatchedXlinkedPeptideWeighted(spectra, peptides[i], modpeptide_complement[i], fragmentTolerance, crosslinker);
//            if (peptides[i].isDecoy())
//                m_isDecoy=true;
//        }
//        modpeptide_complement = getPeptideMassComplements(peptides);
//        
//    }

    
    /**
     * creates a new match
     * @param spectra the spectrum, that should be matched
     * @param peptide1 the first peptide, that should be matched
     * @param peptide2 the second peptide, that should be matched
     * @param config the config describing the run
     */
    public MatchedXlinkedPeptideWeightedNnary(Spectra spectra,
                                Peptide[] peptides,
                                CrossLinker crosslinker,
                                RunConfig config) {
        this(spectra, peptides, crosslinker, config,false);
    }

    public MatchedXlinkedPeptideWeightedNnary(Spectra spectra,
                                Peptide[] peptides,
                                CrossLinker crosslinker,
                                RunConfig config, boolean primaryOnly) {
        super(spectra, peptides[0], getPeptideMassComplements(peptides, 0), crosslinker, config, primaryOnly);
        setPeptides(peptides);
        subMatches= new MatchedXlinkedPeptideWeighted[peptides.length];
        modpeptide_complement = getPeptideMassComplements(this.peptides);
        for (int i = 0; i< peptides.length;i++) {
            subMatches[i] = new MatchedXlinkedPeptideWeighted(spectra, this.peptides[i], modpeptide_complement[i], crosslinker,config, primaryOnly);
            if (peptides[i].isDecoy())
                m_isDecoy=true;
        }


    }
    
    public void setPeptides(Peptide[] peps) {
        this.peptides = new Peptide[peps.length];
        for (int p =0; p < peps.length;p++) {
            Peptide n = peps[p];
            for (int pp = 0; pp< p; pp++) {
                if (n==this.peptides[pp]) {
                    n = n.clone();
                    break;
                }
            }
            this.peptides[p] = n;
        }
    }

    
    public static  Peptide getPeptideMassComplements(Peptide[] peptides, int pep) {
        Peptide[] ret = new Peptide[peptides.length];
        double ModMass = 0;
        String ModName = "";
        for (int p=0;p<pep;p++) {
            ModMass+=peptides[p].getMass();
            ModName+="_" +peptides[p].toString();
        }
         
            
        for (int p = pep+1; p< peptides.length; p++) {
            ModMass+=peptides[p].getMass();
            ModName+="_" +peptides[p].toString();
        }
        AminoModification am = new AminoModification("X"+ModName, AminoAcid.X, ModMass);
        Sequence s = new Sequence(new AminoAcid[]{am});
        Peptide mp = new Peptide(s, 0, 1);
        return mp;
    }   
    
    protected Peptide[] getPeptideMassComplements(Peptide[] peptides) {
        Peptide[] ret = new Peptide[peptides.length];
        for (int i = 0; i< peptides.length; i++) {
 
            ret[i]=getPeptideMassComplements(peptides, i);
        }
        return ret;
    }
    



    public Peptide[] getPeptides() {
        return peptides;
    }
    
    public Peptide getPeptide(int pep) {
        return pep>=peptides.length? null: peptides[pep];
    }
    public double getScore() {
        double s =0;
        for (int m=0;m<subMatches.length;m++)
            s+=subMatches[m].getScore();
        return s/subMatches.length;
    }



    public boolean isDecoy() {
        return m_isDecoy;
    }



    int matchedNonLossy = 0;
    int matchedLossy = 0;
    public void matchPeptides() {
        Spectra s = this.getSpectrum();
        for (int m =0; m<subMatches.length;m++ ) {
            subMatches[m].matchPeptides();
            super.getMatchedFragments().addAll(subMatches[m].getMatchedFragments());
            matchedNonLossy+=subMatches[m].getMatchedFragments().getMatchedNonLossy();
            matchedLossy+=subMatches[m].getMatchedFragments().getMatchedLossy();
        }
        if (super.getMatchedFragments().getMatchedLossy() != matchedLossy || 
                super.getMatchedFragments().getMatchedNonLossy() != matchedNonLossy) {
            System.err.println("Something is wrong");
        }
    }

    public void free() {
        for (int m =0; m<subMatches.length;m++ )
            subMatches[m].free();
    }



    public int[] getLinkSites(Peptide peptide) {
        for (int i =0; i<peptides.length;i++ ) {
            if (peptide == peptides[i])
                return new int[] {getLinkingSite(i)};
        }
        throw new UnsupportedOperationException("Not a peptide of this match"); 
    }

    public int getLinkingSite(int pep) {
        return subMatches[pep].getLinkingSite(0);
    }

    public int getLinkingSite(Peptide peptide) {
        for (int i =0; i<peptides.length;i++ ) {
            if (peptide == peptides[i])
                return subMatches[i].getLinkingSite(0);
        }
        throw new UnsupportedOperationException("Not a peptide of this match");     }

//    public MatchedFragmentCollection getMatchedFragments() {
//        MatchedFragmentCollection mfc = new MatchedFragmentCollection(20);
//        for (MatchedXlinkedPeptideWeighted match : subMatches) {
//            mfc.addAll(match.getMatchedFragments());
//        }
//        return mfc;
//    }



    public Boolean getMightBeLinear() {
        return false;
    }


    @Override
    public ArrayList<Fragment> getFragments() {
        ArrayList<Fragment> frags = new ArrayList<Fragment>();
        for (MatchedXlinkedPeptideWeighted match : subMatches)
            frags.addAll(match.getFragments());
        return frags;
    }
 

    /**
     * returns a list of possible fragments - so returning all fragments
     * for the peptide, that are not in contradiction to the cross linking site.
     * @return
     */
    public HashMap<Peptide,ArrayList<Fragment>> getPossiblePeptideFragments() {
        HashMap<Peptide,ArrayList<Fragment>> ret = new HashMap<Peptide, ArrayList<Fragment>>(2);
        for (int sm = 0; sm < subMatches.length; sm++) {
            MatchedXlinkedPeptideWeighted match = subMatches[sm];
            Peptide p = match.getPeptide(0);
            ArrayList<Fragment> frags = match.getPeptide1Fragments();
            ArrayList<Fragment> PepFrags = new ArrayList<Fragment>(frags.size());
            for (Fragment f : frags)
                if (!f.isClass(DoubleFragmentation.class))
                    if (f.canFullfillXlink(p, getLinkingSite(sm)))
                        PepFrags.add(f);
            ret.put(p, PepFrags);
        }
        return ret;
    }
    
    
}
