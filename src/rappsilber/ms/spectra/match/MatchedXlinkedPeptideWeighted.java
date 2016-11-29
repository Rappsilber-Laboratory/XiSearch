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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import rappsilber.config.RunConfig;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.statistics.utils.UpdateableDouble;
import rappsilber.utils.Util;

// TODO: expand to an arbitrary number of peptides
/**
 * represents the match between a spectrum and a list of peptides (for now just 2)
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class MatchedXlinkedPeptideWeighted extends MatchedXlinkedPeptide {

    HashMap<int[],Double> m_linkWeights = new HashMap<int[], Double>();
    double[][] m_PolWeights;

    private static final double NON_CROSSLINKABLE_WEIGHT = Double.POSITIVE_INFINITY;

    private static final double WEIGHT_OFFSET = 0.0001;


//    protected void initializeWeights(Peptide peptide1, Peptide peptide2) {
//        double[] w = new double[peptide1.length()];
//        java.util.Arrays.fill(w, 0.0);
//        if (peptide2 == null) {
//            m_linkWeights = new ArrayList<double[]>(1);
//            m_linkWeights.add(w);
//        } else {
//            m_linkWeights = new ArrayList<double[]>(2);
//            m_linkWeights.add(w);
//            w = new double[peptide2.length()];
//            java.util.Arrays.fill(w, 0.0);
//            m_linkWeights.add(w);
//        }
//    }



    private class MatchPeakPair {
        public Fragment f;
        public int charge;
        public SpectraPeak                p;

        public MatchPeakPair(Fragment f, int charge, SpectraPeak p){
            this.f = f;
            this.p = p;
            this.charge = charge;
        }

    }



    /**
     * creates a new match
     * @param spectra the spectrum, that should be matched
     * @param peptide1 the first peptide, that should be matched
     * @param peptide2 the second peptide, that should be matched
     * @param fragmentTolerance the tolerance for the search for fragmentPrimary-matches (MS2 tolerance)
     * @param crosslinker the used crosslinker
     */
    public MatchedXlinkedPeptideWeighted(Spectra spectra,
                                Peptide peptide1,
                                Peptide peptide2,
                                ToleranceUnit fragmentTolerance,
                                CrossLinker crosslinker) {
        super(spectra, peptide1, peptide2, fragmentTolerance, crosslinker);
//        initializeWeights(peptide1, peptide2);
        if (peptide2 == null) {
            m_PolWeights = new double[1][peptide1.length()];
        } else {
            m_PolWeights = new double[2][Math.max(peptide1.length(),peptide2.length())];
        }
    }


    /**
     * creates a new match
     * @param spectra the spectrum, that should be matched
     * @param peptide1 the first peptide, that should be matched
     * @param peptide2 the second peptide, that should be matched
     * @param config the config describing the run
     */
    public MatchedXlinkedPeptideWeighted(Spectra spectra,
                                Peptide peptide1,
                                Peptide peptide2,
                                CrossLinker crosslinker,
                                RunConfig config) {
        this(spectra, peptide1, peptide2, crosslinker, config,false);
    }

    public MatchedXlinkedPeptideWeighted(Spectra spectra,
                                Peptide peptide1,
                                Peptide peptide2,
                                CrossLinker crosslinker,
                                RunConfig config, boolean primaryOnly) {
        super(spectra, peptide1, peptide2, crosslinker, config, primaryOnly);
//        initializeWeights(peptide1, peptide2);

        if (peptide2 == null) {
            m_PolWeights = new double[1][peptide1.length()];
        } else {
            m_PolWeights = new double[2][Math.max(peptide1.length(),peptide2.length())];
        }

    }



    /**
     * finds the linkage side for the peptide by minimising the number of
     * miss-matched fragments
     * @param pep the crosslinked peptide
     * @param miss the miss-matched annotations for this linkage site
     * @return the linkage site, that produces the fewest miss-matches
     */
    private int findCrossLinkedResiduesWeighted(Peptide pep, ArrayList<MatchPeakPair> miss, double[] weights) {
        int missCount=Integer.MAX_VALUE;
        double minWeight = Double.MAX_VALUE;
        CrossLinker cl = getCrosslinker();
        ArrayList<MatchPeakPair> minWeightMiss = new ArrayList<MatchPeakPair>();

        
        ArrayList<MatchPeakPair> missMatch = null;
        HashMap<Integer,Double> siteWeights = new HashMap<Integer, Double>(pep.length());
        int pos = -1;
        for (int i=0 ; i< pep.length(); i++) { // for each residue
            if (cl.canCrossLink(pep, i)) { // if the crosslinker can act there
                double w = cl.getWeight(pep, i);
                ArrayList<MatchPeakPair> weightMiss = new ArrayList<MatchPeakPair>();
                // find all missmatched entries
                for (MatchedBaseFragment mbf : getMatchedFragments()) {
                    Fragment f = mbf.getBaseFragment();
                    // it would be a missmatch  miss
                    if (!f.canFullfillXlink(pep, i)) {

                        // add the missmatch weights
                        if (mbf.isBaseFragmentFound()) {
                            SpectraPeak sp = mbf.getBasePeak();
                            w+=getMissMatchWeight(f, sp);
                            weightMiss.add(new MatchPeakPair(f, mbf.getCharge(), sp));
                        }

                        for (Loss l :  mbf.getLosses().keySet()) {
                            SpectraPeak sp = mbf.getLosses().get(l);
                            w+=getMissMatchWeight(l, sp);
                            weightMiss.add(new MatchPeakPair(l, mbf.getCharge(), sp));
                        }
                    }
                }

                if (w<minWeight) {
                    minWeight = w;
                    minWeightMiss = weightMiss;
                    pos = i;
                }
                weights[i] = w;

                siteWeights.put(i, w);

            } else {
                weights[i] = NON_CROSSLINKABLE_WEIGHT;
            }
        }
        if (minWeightMiss.size() > 0)
            miss.addAll(minWeightMiss); // add the missmatches to the list of missmatches

        // recalculate the weights, so that the best match has the highest score and the sum of scores is 1
        double ws = 0;
        for (int i = 0 ; i<weights.length; i++) {
            weights[i]=1/(weights[i] + WEIGHT_OFFSET);
            ws += weights[i];
        }

        for (int i = 0 ; i<weights.length; i++) {
            weights[i]/=ws;
        }

        return pos;
    }

    
    /**
     * finds the linkage side for the peptide by minimising the number of
     * miss-matched fragments
     * @param pep the crosslinked peptide
     * @param miss the miss-matched annotations for this linkage site
     * @return the linkage site, that produces the fewest miss-matches
     */
    private HashMap<int[],UpdateableDouble> findCrossLinkedResiduesWeighted(Peptide pep1, Peptide pep2, ArrayList<MatchPeakPair> miss) {
        int missCount=Integer.MAX_VALUE;
        double minWeight = Double.MAX_VALUE;
        CrossLinker cl = getCrosslinker();
        HashMap<int[],UpdateableDouble> weights = new HashMap<int[], UpdateableDouble>();
        ArrayList<MatchPeakPair> minWeightMiss = new ArrayList<MatchPeakPair>();

        
        ArrayList<MatchPeakPair> missMatch = null;
//        HashMap<Integer,Double> siteWeights = new HashMap<Integer, Double>(pep1.length());
        double[][] siteWeights = new double[2][Math.max(pep1.length(), pep2.length())];
        int pos1 = -1;
        int pos2 = -1;
        for (int p1=pep1.length()-1 ; p1>=0; p1--) { // for each residue
            if (cl.canCrossLink(pep1,p1))
            for (int p2=pep2.length() - 1; p2>=0; p2--) { // for each residue
                if (cl.canCrossLink(pep1, p1, pep2, p2)) { // if the crosslinker can act there
                    double w2 = cl.getWeight(pep1, p1) + cl.getWeight(pep2, p2);
                    ArrayList<MatchPeakPair> weightMiss = new ArrayList<MatchPeakPair>();
                    // find all missmatched entries
                    for (MatchedBaseFragment mbf : getMatchedFragments()) {
                        Fragment f = mbf.getBaseFragment();
                        // it would be a missmatch  miss
                        if (!f.canFullfillXlink(pep1, p1, pep2, p2)) {

                        // add the missmatch weights
                            if (mbf.isBaseFragmentFound()) {
                                SpectraPeak sp = mbf.getBasePeak();
                                w2+=getMissMatchWeight(f, sp);
                                weightMiss.add(new MatchPeakPair(f, mbf.getCharge(), sp));
                            }

                            for (Loss l :  mbf.getLosses().keySet()) {
                                SpectraPeak sp = mbf.getLosses().get(l);
                                w2+=getMissMatchWeight(l, sp);
                                weightMiss.add(new MatchPeakPair(l, mbf.getCharge(), sp));
                            }
                        }
                    }
                    if (w2<minWeight) {
                        minWeight = w2;
                        minWeightMiss = weightMiss;
                        pos1 = p1;
                        pos2 = p2;
                    }
                    weights.put(new int[] {p1,p2}, new UpdateableDouble(w2));

                } else {
                    weights.put(new int[] {p1,p2}, new UpdateableDouble(NON_CROSSLINKABLE_WEIGHT));
                }
            }
        }
        if (minWeightMiss.size() > 0)
            miss.addAll(minWeightMiss); // add the missmatches to the list of missmatches

        // recalculate the weights, so that the best match has the highest score and the sum of scores is 1
        double ws = 0;
        for (UpdateableDouble ud : weights.values()) {
            ud.value = 1/ (ud.value + WEIGHT_OFFSET);
            ws += ud.value;
        }

        for (UpdateableDouble ud : weights.values()) {
            ud.value /= ws;
        }

        setLinkingSitePeptide1(pos1);
        setLinkingSitePeptide2(pos2);

        return weights;
    }
    
    protected double getMissMatchWeight(Fragment f, SpectraPeak sp  ) {
        double w = sp.getIntensity()/getSpectrum().getMaxIntensity();
        if (f.isClass(Loss.class)) {
            Loss l = (Loss) f;
            int lc = l.getTotalLossCount();
            if (lc < 3)
                w/=10*lc;
            else  
                w = 0;
            
        } else if (f.getFragmentationSites().length>0) {
            w/=10;
        }
        return w;
    }




    /**
     * defines the cross-linked residues in all peptides and deletes the
     * fragmentPrimary-matches, that do conflict with these
     */
    @Override
    public void findCrossLinkedResidues() {
        ArrayList<MatchPeakPair> missMatches = new ArrayList<MatchPeakPair>();
        m_PolWeights = new double[2][Math.max(getPeptide1().length(), getPeptide2().length())];
        
        HashMap<int[], UpdateableDouble> lw = findCrossLinkedResiduesWeighted(getPeptide1(), getPeptide2(), missMatches);

        for (MatchPeakPair mf : missMatches) {
            mf.p.deleteAnnotation(mf.f);
            getMatchedFragments().remove(mf.f, mf.charge);
        }
        missMatches.clear();
        
        for (int[] pos: lw.keySet()) {
            double w = lw.get(pos).value;
            m_linkWeights.put(pos, lw.get(pos).value);
            for (int p=0; p<2; p++) {
                m_PolWeights[p][pos[p]] += w;
            }
        }

        //deleteCrossLinkedMissMatches(m_Peptide2, m_LinkingSitePeptide2);
    }

    
    
    /**
     * defines the cross-linked residues in all peptides and deletes the
     * fragmentPrimary-matches, that do conflict with these
     */
    public void setCrossLinkedResidues(int site1 , int site2) {
        ArrayList<MatchPeakPair> missMatches = new ArrayList<MatchPeakPair>();
        m_PolWeights = new double[2][Math.max(getPeptide1().length(), getPeptide2().length())];
        
        HashMap<int[], UpdateableDouble> lw = findCrossLinkedResiduesWeighted(getPeptide1(), getPeptide2(), missMatches);
        MatchedFragmentCollection mfc = getMatchedFragments();
        ArrayList<MatchedBaseFragment> toDelete = new ArrayList<MatchedBaseFragment>();
        for (MatchedBaseFragment mbf : mfc ){
            if (!mbf.getBaseFragment().canFullfillXlink(getPeptide1(), site1, getPeptide2(), site2)) {
                toDelete.add(mbf);
            }
        }
        
        for (MatchedBaseFragment mbf : toDelete) {
            if (mbf.isBaseFragmentFound())
                mbf.getBasePeak().deleteAnnotation(mbf.getBaseFragment());
            for (Map.Entry<Loss,SpectraPeak> e:mbf.getLosses().entrySet()){
                e.getValue().deleteAnnotation(e.getKey());
            }
            mfc.remove(mbf);
        }
        
        missMatches.clear();
        
        for (int[] pos: lw.keySet()) {
            double w = lw.get(pos).value;
            m_linkWeights.put(pos, lw.get(pos).value);
            for (int p=0; p<2; p++) {
                m_PolWeights[p][pos[p]] += w;
            }
        }
        setLinkingSitePeptide1(site1);
        setLinkingSitePeptide2(site2);
        //deleteCrossLinkedMissMatches(m_Peptide2, m_LinkingSitePeptide2);
    }

    public double getLinkageWeight(int pepID, int position) {
        return m_PolWeights[pepID][position];
        
//        double w = 0;
//        ArrayList<int[]> positions = getSortedLinkPositions();
//        for (int[] pos : positions) {
//            if (pos[pepID] == position) {
//                w+= m_linkWeights.get(pos);
//            }
//        }
//        return w;
    }

    public double[] getLinkageWeights(int pepID) {
        return m_PolWeights[pepID];
//        int pl = getPeptides()[pepID].length();
//        double[] ret = new double[pl];
//        for (int p = 0; p< pl;p++) {
//            ret[p] = getLinkageWeight(pepID, p);
//        }
//        return ret;
    }
    
    protected ArrayList<int[]> getSortedLinkPositions() {
        ArrayList<int[]> positions = new ArrayList<int[]>();
        for (int[] pos : m_linkWeights.keySet()) {
            positions.add(pos);
        }
        java.util.Collections.sort(positions, new Comparator<int[]>(){

            public int compare(int[] o1, int[] o2) {
                return Double.compare(m_linkWeights.get(o2), m_linkWeights.get(o1));
            }
            
        });
        return positions;
    }
    
    
    public String LinkageWeigthsToString() {
        // sort linkage sites by score
        ArrayList<int[]> positions = new ArrayList<int[]>();
        for (int[] pos : m_linkWeights.keySet()) {
            positions.add(pos);
        }
        java.util.Collections.sort(positions, new Comparator<int[]>(){

            public int compare(int[] o1, int[] o2) {
                return Double.compare(m_linkWeights.get(o2), m_linkWeights.get(o2));
            }
            
        });
        StringBuffer ret = new StringBuffer();
        for (int[] pos: positions) {
            ret.append(pos[0]+1);
            ret.append("-");
            ret.append(pos[1]+1);
            ret.append(":");
            ret.append(Util.twoDigits.format(m_linkWeights.get(pos)));
            ret.append(";");
        }
        return ret.substring(0, ret.length()-1);
    }
}
