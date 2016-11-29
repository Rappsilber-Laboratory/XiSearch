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
import java.util.Map;
import rappsilber.config.RunConfig;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.crosslinker.NonCovalentBound;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.spectra.match.matcher.DirectMatchFragmentsTree;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.spectra.match.matcher.Match;
import rappsilber.ms.sequence.ions.CrosslinkedFragment;
import rappsilber.ms.sequence.ions.DoubleFragmentation;
import rappsilber.ms.sequence.ions.SecondaryFragment;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.SpectraPeakCluster;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.matcher.DirectMatchFragmentsTreeLowRes;
import rappsilber.ms.statistics.utils.UpdateableInteger;

// TODO: expand to an arbitrary number of peptides
/**
 * represents the match between a spectrum and a list of peptides (for now just 2)
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class MatchedXlinkedPeptide implements ScoredPeptideMatch {
    /** site the linker attaches to peptide 1 */
    private int m_LinkingSitePeptide1 = -1;
    /** site the linker attaches to peptide 2 */
    private int m_LinkingSitePeptide2 = -1;
    /** peptide 1 */
    private Peptide m_Peptide1;
    /** the possible fragments to be matched originating from peptide 1 */
    private ArrayList<Fragment> m_Peptide1Fragments;
    /** the second peptide to match */
    private Peptide m_Peptide2 = null;
    /** the possible fragments to be matched originating from peptide 2 */
    private ArrayList<Fragment> m_Peptide2Fragments;
    /** the actual spectrum */
    private Spectra m_Spectra;
    /** MS2 tolerance */
    private ToleranceUnit m_FragmentTolerance;

    //private Spectra m_Unmatched;
    /** the crosslinker, that links the peptides */
    private CrossLinker m_crosslinker;
    /** method used for matching fragments to spectra */
    private Match m_matcher = null;// new DirectMatchFragmentsTree();
    /** a collection of all matched fragments */
    private MatchedFragmentCollection m_matchedFragments = new MatchedFragmentCollection(0);

    /** a collection of all matched fragments that are declared to be the 
     * primary explanation for a peak*/
    private MatchedFragmentCollection m_primaryMatchedFragments;
    /** a collection of all matched fragments that are a unique explanation 
     * for a peak*/
    private MatchedFragmentCollection m_uniqueMatchedFragments;

    private int m_countPossibleBeta = 0;
    /**
     * keeps the peptides in an ordered list (by number of matches)
     */
    private Peptide[] m_orderedPeptides = null;


    /** defines, whether this match is a decoy match */
    private boolean m_isDecoy = false;

    /**
     * the rank of the match. Best rank is 0 then comes 1 and so on
     */
    private int  m_matchrank = 0;

    /**
     * defines whether a scan is supposed to be believable true (automatically validated)
     */
    private boolean m_isValidated=false;

    /**
     * are the two peptides consecutive
     */
    private Boolean m_mightBeLinear = null;
    
    /**
     * either the two peptides are non-consecutive or there is some evidence for them to be non-consecutive
     */
    private Boolean m_isCrosslinked = null;
    
    /**
     * would this match pass the auto-validation
     */
    protected boolean m_passesAutoValidation = false;
    

    /**
     * some "measure" as to what should be the noise level.
     * currently that is defined along the lines of median by 3/4 of unmatched peaks
     * in a box-plot, that would be the upper end of the box
     */
    private double m_noiceIntensity=0;
    
    /**
     * Dirty trick - for disabling the Lys-preference for linkage sites
     */
    private boolean m_isopenmod = false;
    
    /**
     * flags up, whether the matched peptides could be from the same protein.
     */
    private Boolean m_isInternal  = null;
    
    /**
     * a key, that uniquely represents the current peptide match
     */
    private String key;
    

    /**
     * @return whether this is a decoy-match
     */
    public boolean isDecoy() {
        return m_isDecoy;
    }

    /**
     * @param isDecoy defines whether the match is a decoy match
     */
    public void setDecoy(boolean isDecoy) {
        this.m_isDecoy = isDecoy;
    }

    /**
     * @return isValidated
     */
    public boolean isValidated() {
        return m_isValidated;
    }

    /**
     * set whether this match is believed to be true
     * @param isValidated true: for true match
     */
    public void setValidated(boolean isValidated) {
        this.m_isValidated = isValidated;
    }

    /**
     * @return the m_matchrank
     */
    public int getMatchrank() {
        return m_matchrank;
    }

    /**
     * @param m_matchrank the m_matchrank to set
     */
    public void setMatchrank(int m_matchrank) {
        this.m_matchrank = m_matchrank;
    }


    public double getCalcMass(){

        double mass = 0.0d;

        if(this.getCrosslinker() != null){
            mass += this.getCrosslinker().getCrossLinkedMass();
        }

        for(Peptide p: this.getPeptides()){
            mass += p.getMass();
        }

        return mass;
    }
    /**
     * Returns, whether peptide one and two could be consecutive Peptides in a sequence.
     * Therefore indicates, whether a cross-linked match could actually be a linear modified one
     * @return the m_mightBeLinear
     */
    public Boolean getMightBeLinear() {
        if (m_mightBeLinear == null) {
            Peptide[] peps = getPeptides();
            if (peps.length == 1)
                m_mightBeLinear = Boolean.TRUE;
            else {
                Peptide p1 = peps[0];
                Peptide p2 = peps[1];

                pos1:for (Peptide.PeptidePositions pos1 : p1.getPositions())
                    pos2:for (Peptide.PeptidePositions pos2 : p2.getPositions()) {
                        if (pos1.base == pos2.base &&
                                (pos1.start == pos2.start + p2.getLength() || pos2.start == pos1.start + p1.getLength()) ) {
                            m_mightBeLinear = Boolean.TRUE;
                            return m_mightBeLinear;
                        }
                    }
                m_mightBeLinear = Boolean.FALSE;
            }
        }
        return m_mightBeLinear;
    }

    /**
     * @param m_mightBeLinear the m_mightBeLinear to set
     */
    public void setMightBeLinear(boolean mightBeLinear) {
        this.m_mightBeLinear = mightBeLinear;
    }


    private static final double NON_CROSSLINKABLE_WEIGHT = Double.POSITIVE_INFINITY;

    /**
     * site the linker attaches to peptide 1
     * @param m_LinkingSitePeptide1 the m_LinkingSitePeptide1 to set
     */
    protected void setLinkingSitePeptide1(int m_LinkingSitePeptide1) {
        this.m_LinkingSitePeptide1 = m_LinkingSitePeptide1;
    }

    /**
     * site the linker attaches to peptide 2
     * @param m_LinkingSitePeptide2 the m_LinkingSitePeptide2 to set
     */
    protected void setLinkingSitePeptide2(int m_LinkingSitePeptide2) {
        this.m_LinkingSitePeptide2 = m_LinkingSitePeptide2;
    }

    /**
     * method used for matching fragments to spectra
     * @return the m_matcher
     */
    protected Match getMatcher() {
        return m_matcher;
    }

    /**
     * method used for matching fragments to spectra
     * @param m_matcher the m_matcher to set
     */
    protected void setMatcher(Match m_matcher) {
        this.m_matcher = m_matcher;
    }

    /**
     * a collection of all matched fragments
     * @param m_matchedFragments the m_matchedFragments to set
     */
    protected void setMatchedFragments(MatchedFragmentCollection m_matchedFragments) {
        this.m_matchedFragments = m_matchedFragments;
    }

    public double getScore() {
        return getScore("match score");
    }

    public boolean isInternal() {
        if (m_isInternal != null)
            return m_isInternal;
        Peptide[] peps = getPeptides();
        int s = peps.length;
        if (s == 1) {
            m_isInternal = true;
            return true;
        }
        
        p1loop: for (int p1 = 0; p1<s;p1++) {
            for (int p2 = p1+1 ; p2<s;p2++) {
                for (Peptide.PeptidePositions pos1 : peps[p1].getPositions())  { // as long as we 
                    for (Peptide.PeptidePositions pos2 : peps[p2].getPositions())  {
                        if (pos1.base.getSourceSequence() == pos2.base.getSourceSequence()) {
                            continue p1loop;
                        }
                            
                    }
                }
                m_isInternal = false;
                return false;
            }
        }
        m_isInternal = true;
        
        return m_isInternal;
    }

    public boolean isAmbigious() {
        for (Peptide p : getPeptides()) {
            if (p.getPositions().length > 1)
                return true;
        }
        return false;
    }

    public boolean isDecoy(int site) {
        return getPeptides()[site].isDecoy();
    }

    public int sites() {
        return getPeptides().length;
    }

    public int length(int site) {
        return getPeptides()[site].length();
    }

    public String key() {
        // if 
        if (key != null)
            return key;
        
        synchronized(this) {
            if (key != null)
                return key;
        
            Peptide[] peps = getPeptides();
            String key = null;
            int pc = peps.length;
            if (pc == 1) {
                key = peps[0].getPeptideIndex() + " c" + getSpectrum().getChargeStateSpectra();
            } else if (pc == 2) {
                int p1 = Math.min(peps[0].getPeptideIndex(), peps[1].getPeptideIndex());
                int p2 = Math.max(peps[0].getPeptideIndex(), peps[1].getPeptideIndex());

                key = p1 + " " + p2 + " c" + getSpectrum().getChargeStateSpectra();
            } else {

                int pids[] = new int[pc];
                for (int p = 0; p< pc; p++) {
                    pids[p]=peps[p].getPeptideIndex();
                }
                java.util.Arrays.sort(pids);
                StringBuffer sb = new StringBuffer(pc*7+3);
                sb.append(pids[0]);
                for (int p = 1; p<pc;p++) {
                    sb.append("_");
                    sb.append(pids[p]);
                }
                sb.append(" c");
                sb.append(getSpectrum().getChargeStateSpectra());
                key = sb.toString();
            }

            return key;
        }
        
    }
    

    public String getPepKey() {
        // if 
        if (key != null)
            return key;
        
        synchronized(this) {
            if (key != null)
                return key;
        
            Peptide[] peps = getPeptides();
            String key = null;
            int pc = peps.length;
            if (pc == 1) {
                key = Integer.toString(peps[0].getPeptideIndex());
            } else if (pc == 2) {
                int p1 = Math.min(peps[0].getPeptideIndex(), peps[1].getPeptideIndex());
                int p2 = Math.max(peps[0].getPeptideIndex(), peps[1].getPeptideIndex());

                key = p1 + " " + p2;
            } else {

                int pids[] = new int[pc];
                for (int p = 0; p< pc; p++) {
                    pids[p]=peps[p].getPeptideIndex();
                }
                java.util.Arrays.sort(pids);
                StringBuffer sb = new StringBuffer(pc*7+3);
                sb.append(pids[0]);
                for (int p = 1; p<pc;p++) {
                    sb.append("_");
                    sb.append(pids[p]);
                }
//                sb.append(" c");
//                sb.append(getSpectrum().getChargeStateSpectra());
                key = sb.toString();
            }

            return key;
        }
        
    }
    
//    public String getLinkKey() {
//        final Peptide[] peps = getPeptides();
//        Integer[] pepID = new Integer[peps.length];
//        for (int p = 0; p<peps.length;p++) {
//            pepID[p] = p;
//        }
//        
//        java.util.Arrays.sort(pepID,new Comparator<Integer>() {
//
//            public int compare(Integer o1, Integer o2) {
//                Sequence s1 = ((Sequence)peps[o1].getSequence().getSourceSequence());
//                Sequence s2 = ((Sequence)peps[o2].getSequence().getSourceSequence());
//                if (s1 == s2) {
//                    return Integer.compare(getLinkingSite(o1), getLinkingSite(o2));
//                }
//                return Integer.compare(s1.getUniqueID(), s2.getUniqueID());
//            }
//        });
//        StringBuffer sb = new StringBuffer();
//        for (int i=0; i<peps.length; i++) {
//            sb.append(Integer.toString(peps[pepID[i]].getSequence().getSourceSequence().getUniqueID()));
//            sb.append("_");
//            sb.append(peps[pepID[i]].getStart() + getLinkingSite(pepID[i]));
//            sb.append(" ");
//        }
//        
//        return sb.substring(0,sb.length()-1);
//        
//    }

    /**
     * @return the m_passesAutoValidation
     */
    public boolean passesAutoValidation() {
        return m_passesAutoValidation;
    }

    /**
     * @param m_passesAutoValidation the m_passesAutoValidation to set
     */
    public void setPassesAutoValidation(boolean m_passesAutoValidation) {
        this.m_passesAutoValidation = m_passesAutoValidation;
    }

    
//    public String getPPIKey() {
//        final Peptide[] peps = getPeptides();
//        Integer[] pepID = new Integer[peps.length];
//        for (int p = 0; p<peps.length;p++) {
//            pepID[p] = p;
//        }
//        
//        java.util.Arrays.sort(pepID,new Comparator<Integer>() {
//
//            public int compare(Integer o1, Integer o2) {
//                Sequence s1 = ((Sequence)peps[o1].getSequence().getSourceSequence());
//                Sequence s2 = ((Sequence)peps[o2].getSequence().getSourceSequence());
//                return Integer.compare(s1.getUniqueID(), s2.getUniqueID());
//            }
//        });
//        StringBuffer sb = new StringBuffer();
//        for (int i=0; i<peps.length; i++) {
//            sb.append(Integer.toString(peps[pepID[i]].getSequence().getSourceSequence().getUniqueID()));
//            sb.append(" ");
//        }
//        
//        return sb.substring(0,sb.length()-1);
//        
//    }
    
//@TODO
//    /**
//     * Returns some "measure" as to what should be the noise level.
//     * currently that is defined along the lines of median by 3/4 of unmatched peaks
//     * in a box-plot, that would be the upper end of the box
//     * @return the m_noiceIntensity
//     */
//    public double getNoiceIntensity() {
//        if (m_noiceIntensity == 0 && m_matchedFragments.size() > 0) {
//            for (SpectraPeak sp : getSpectrum().getTopPeaks(-1)) {
//                ()
//            }
//        }
//        return m_noiceIntensity;
//    }

    protected class MatchPeakPair {
        public SpectraPeakMatchedFragment f;
        public SpectraPeak                p;

        public MatchPeakPair(SpectraPeakMatchedFragment f, SpectraPeak p){
            this.f = f;
            this.p = p;
        }
    }



    private class MatchPeakList extends ArrayList<MatchPeakPair> {
        int nonLossyCount = 0;
        double missIntensity = 0;
        double nonLossyMissIntensity = 0;

        @Override
        public boolean add(MatchPeakPair mp) {
            return false;
        }


        public boolean add(MatchPeakPair mp, double intensity){
            boolean ret = super.add(mp);
            missIntensity += intensity;
            if (!(mp.f.getFragment().isClass(Loss.class) || mp.f.getFragment().isClass(SecondaryFragment.class) || mp.f.getFragment().getFragmentationSites().length>1)) {
                    nonLossyCount++;
                    nonLossyMissIntensity += intensity;
            }

            return ret;
        }

    }


    
    private int mgc_matches;
    private double mgc_score;
    private int partners;
    private double molecular_weight;
    private double gap_mass;


    private RunConfig  m_config = null;


    private HashMap<String,Double> m_scores = new HashMap<String, Double>();




    /**
     * creates a new match
     * @param spectra the spectrum, that should be matched
     * @param peptide1 the first peptide, that should be matched
     * @param peptide2 the second peptide, that should be matched
     * @param fragmentTolerance the tolerance for the search for fragmentPrimary-matches (MS2 tolerance)
     * @param crosslinker the used crosslinker
     */
    public MatchedXlinkedPeptide(Spectra spectra,
                                Peptide peptide1,
                                Peptide peptide2,
                                ToleranceUnit fragmentTolerance,
                                CrossLinker crosslinker) {
        m_Peptide1 = peptide1;
//        if (peptide1 == peptide2) {
//            m_Peptide2 = peptide2.clone();
//        } else {
            m_Peptide2 = peptide2;
//        }
        m_Peptide1Fragments = m_Peptide1.getFragments();
        m_Peptide2Fragments = m_Peptide2.getFragments();
        m_Spectra  = spectra;
        m_FragmentTolerance = fragmentTolerance;
        m_crosslinker = crosslinker;
        boolean decoy = peptide1.getSequence().isDecoy() ||
                (peptide2 != null && peptide2.getSequence().isDecoy()) ||
                (crosslinker != null && crosslinker.isDecoy());

        setDecoy(decoy);
        if (m_crosslinker != null && m_crosslinker.getName().contentEquals("OpenModification"))
                m_isopenmod = true;
        
        m_primaryMatchedFragments = new MatchedFragmentCollection(m_Spectra.getPrecurserCharge());
    }


    /**
     * creates a new match
     * @param spectra the spectrum, that should be matched
     * @param peptide1 the first peptide, that should be matched
     * @param peptide2 the second peptide, that should be matched
     * @param config the config describing the run
     */
    public MatchedXlinkedPeptide(Spectra spectra,
                                Peptide peptide1,
                                Peptide peptide2,
                                CrossLinker crosslinker,
                                RunConfig config) {
        this(spectra, peptide1, peptide2, crosslinker, config,false);
    }

    public MatchedXlinkedPeptide(Spectra spectra,
                                Peptide peptide1,
                                Peptide peptide2,
                                CrossLinker crosslinker,
                                RunConfig config, boolean primaryOnly) {
        m_config = config;
        if (m_config.isLowResolution())
            m_matcher = new DirectMatchFragmentsTreeLowRes();
        else
            m_matcher = new DirectMatchFragmentsTree(config);

        m_Peptide1 = peptide1;
        if (peptide1 == peptide2) {
            m_Peptide2 = peptide2.clone();
        } else {
            m_Peptide2 = peptide2;
        }
        m_FragmentTolerance = config.getFragmentTolerance();
        m_crosslinker = crosslinker;

        if (primaryOnly){

            m_Peptide1Fragments = (ArrayList<Fragment>) m_Peptide1.getPrimaryFragments(config).clone();
            if (m_Peptide2 != null)
                m_Peptide2Fragments = (ArrayList<Fragment>) m_Peptide2.getPrimaryFragments(config).clone();
            else
                m_Peptide2Fragments = new ArrayList<Fragment>();
            if (m_crosslinker != null && !(m_crosslinker instanceof NonCovalentBound))
                m_Peptide1Fragments.addAll(CrosslinkedFragment.createCrosslinkedFragments(m_Peptide1Fragments, m_Peptide2Fragments, m_crosslinker, false));
        } else {

            m_Peptide1Fragments = (ArrayList<Fragment>) m_Peptide1.getFragments(config).clone();
            if (m_Peptide2 != null)
                m_Peptide2Fragments = (ArrayList<Fragment>) m_Peptide2.getFragments(config).clone();
            else
                m_Peptide2Fragments = new ArrayList<Fragment>();

            if (m_crosslinker != null && !(m_crosslinker instanceof NonCovalentBound))
                m_Peptide1Fragments.addAll(CrosslinkedFragment.createCrosslinkedFragments(m_Peptide1Fragments, m_Peptide2Fragments, m_crosslinker, false));
            Loss.includeLosses(m_Peptide1Fragments, m_crosslinker, true,config);

    //        m_Peptide2Fragments.addAll(CrosslinkedFragment.createCrosslinkedFragments(m_Peptide2Fragments, peptide1, m_crosslinker, true));
            Loss.includeLosses(m_Peptide2Fragments, m_crosslinker, true,config);
        }

        m_Spectra  = spectra;

        boolean decoy = peptide1.getSequence().isDecoy() ||
                (peptide2 != null && peptide2.getSequence().isDecoy()) ||
                (crosslinker != null && crosslinker.isDecoy());

        setDecoy(decoy);

        if (m_crosslinker != null && m_crosslinker.getName().contentEquals("OpenModification"))
                m_isopenmod = true;
        
        m_primaryMatchedFragments = new MatchedFragmentCollection(m_Spectra.getPrecurserCharge());

    }

    private boolean isMissCrossLinkMatched(Peptide p, int linkSite, Fragment f) {
        return !f.canFullfillXlink(p, linkSite);
//        return ( (f.getStart() <= linkSite && linkSite <= f.getEnd() &&
//                  ! (f.isClass(CrosslinkerContaining.class))) ||
//             ((f.getStart() > linkSite || linkSite > f.getEnd()) &&
//                            (f.isClass(CrosslinkerContaining.class))));

    }

    /**
     * returns the miss-matched fragments for the peptide under the assumption,
     * that the crosslinker links to the residue at pos
     * @param p the crosslinked peptide
     * @param pos the linkage position
     * @return list of miss-matched fragmentPrimary
     */
    private MatchPeakList getCrosslinkMissMatches(Peptide p, int pos ){
        MatchPeakList missMatch = new MatchPeakList();
        for (SpectraPeak sp : m_Spectra.getPeaks()) {
             for (SpectraPeakMatchedFragment mf : sp.getMatchedAnnotation()) {
                 Fragment f = mf.getFragment();
                 if (!f.canFullfillXlink(p, pos)) {
                     SpectraPeakCluster spc;
                     if ((spc = mf.getCluster()) != null) 
                        missMatch.add(new MatchPeakPair(mf, sp),spc.getSummedIntensity()); // we have a missmatch
                     else
                        missMatch.add(new MatchPeakPair(mf, sp),sp.getIntensity()); // we have a missmatch
                 }
//                 if (f.getPeptide() == p)
//                     // if the match is covering the crosslink side
//                     //and the the matched fragmentPrimary is not crosslinked
//                     if (isMissCrossLinkMatched(p, pos, f))
//                        missMatch.add(mf); // we have a missmatch
             }
        }
        return missMatch;
    }

    private MatchPeakList getCrosslinkMissMatches(int pos1, int pos2 ){
        MatchPeakList missMatch = new MatchPeakList();
        for (SpectraPeak sp : m_Spectra.getPeaks()) {
             for (SpectraPeakMatchedFragment mf : sp.getMatchedAnnotation()) {
                 Fragment f = mf.getFragment();
                 if (!f.canFullfillXlink(m_Peptide1, pos1) || !f.canFullfillXlink(m_Peptide2, pos2)) {
                     SpectraPeakCluster spc;
                     if ((spc = mf.getCluster()) != null)
                        missMatch.add(new MatchPeakPair(mf, sp),spc.getSummedIntensity()); // we have a missmatch
                     else
                        missMatch.add(new MatchPeakPair(mf, sp),sp.getIntensity()); // we have a missmatch
                 }
//                 if (f.getPeptide() == p)
//                     // if the match is covering the crosslink side
//                     //and the the matched fragmentPrimary is not crosslinked
//                     if (isMissCrossLinkMatched(p, pos, f))
//                        missMatch.add(mf); // we have a missmatch
             }
        }
        return missMatch;
    }





    protected void findCrossLinkedResidues(ArrayList<MatchPeakPair> miss) {
        int missCount=Integer.MAX_VALUE;
        int nlMissCount=Integer.MAX_VALUE;
        double missMatchedIntensity =Double.MAX_VALUE;
        double missNonLossyMatchedIntensity =Double.MAX_VALUE;
        AminoAcid aa1 = AminoAcid.G;
        AminoAcid aa2 = AminoAcid.G;
        ArrayList<MatchPeakPair> missMatch = null;
        int pos1 = -1;
        int pos2 = -1;
        for (int i1=0 ; i1< m_Peptide1.length(); i1++)  // for each residue
        //if (getCrosslinker().canCrossLink(m_Peptide1, i1))
            for (int i2=0 ; i2< m_Peptide2.length(); i2++) { // for each residue
                if (getCrosslinker().canCrossLink(m_Peptide1, i1,m_Peptide2, i2)) { // if the crosslinker can act there
                    MatchPeakList mm = getCrosslinkMissMatches(i1,i2); // count the missmatched fragments
                    if (mm.nonLossyCount < nlMissCount) { // do we have the (untill now) lowest number of missmatches?
                        pos1 = i1;
                        pos2 = i2;
                        aa1 = m_Peptide1.aminoAcidAt(i1);
                        aa2 = m_Peptide2.aminoAcidAt(i2);
                        missCount = mm.size();
                        nlMissCount = mm.nonLossyCount;
                        missMatch = mm;
                        missMatchedIntensity = mm.missIntensity;
                        missNonLossyMatchedIntensity = mm.nonLossyMissIntensity;
                    } else if (mm.nonLossyCount == nlMissCount)  {
                        AminoAcid paa1 = m_Peptide1.aminoAcidAt(i1);
                        AminoAcid paa2 = m_Peptide2.aminoAcidAt(i2);
                        if (m_isopenmod ||  (aa1 != AminoAcid.K || (aa1 == AminoAcid.K &&  paa1 == AminoAcid.K)) ||
                                (aa2 != AminoAcid.K || (aa2 == AminoAcid.K && paa2 == AminoAcid.K)))
                        if (    (mm.nonLossyMissIntensity < missNonLossyMatchedIntensity ||
                                (mm.nonLossyMissIntensity == missNonLossyMatchedIntensity && mm.missIntensity < missMatchedIntensity)
                                || (!m_isopenmod && (mm.nonLossyMissIntensity == missNonLossyMatchedIntensity && mm.nonLossyMissIntensity == missNonLossyMatchedIntensity && mm.missIntensity < missMatchedIntensity))
                                || (!m_isopenmod && ((aa1 != AminoAcid.K && paa1 == AminoAcid.K) || (aa2 != AminoAcid.K && paa2 == AminoAcid.K)))
                            )) {
                            pos1 = i1;
                            pos2 = i2;
                            aa1 = m_Peptide1.aminoAcidAt(i1);
                            aa2 = m_Peptide2.aminoAcidAt(i2);
                            missCount = mm.size();
                            nlMissCount = mm.nonLossyCount;
                            missMatch = mm;
                            missMatchedIntensity = mm.missIntensity;
                            missNonLossyMatchedIntensity = mm.nonLossyMissIntensity;
                        }
                    }
                }
            }
        if (missCount > 0 && missCount < Integer.MAX_VALUE)
            miss.addAll(missMatch); // add the missmatches to the list of missmatches

        setLinkingSitePeptide1(pos1);
        setLinkingSitePeptide2(pos2);
    }



    /**
     * defines the crosslinked residues in all peptides and deletes the
     * fragmentPrimary-matches, that do conflict with these
     */
    public void findCrossLinkedResidues() {
        //deleteCrossLinkedMissMatches(m_Peptide2, m_LinkingSitePeptide2);
        ArrayList<MatchPeakPair> missMatches = new ArrayList<MatchPeakPair>();
        findCrossLinkedResidues(missMatches);
        for (MatchPeakPair mf : missMatches) {
            mf.p.deleteAnnotation(mf.f);
            m_matchedFragments.remove(mf.f.getFragment(), mf.f.getCharge());
        }
    }

       /**
     * defines the crosslinked residues in all peptides and deletes the
     * fragmentPrimary-matches, that do conflict with these
     */
    public void setCrossLinkedResidues(int site1, int site2) {
        //deleteCrossLinkedMissMatches(m_Peptide2, m_LinkingSitePeptide2);
        ArrayList<MatchPeakPair> missMatches = new ArrayList<MatchPeakPair>();
        findCrossLinkedResidues(missMatches);
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
        setLinkingSitePeptide1(site1);
        setLinkingSitePeptide2(site2);
    }
 

    /**
     * tries to find all possible explanation for a each spectrum peak by
     * finding all fragments, that could produce that peak.
     */
    public void matchPeptides() {
        setMatchedFragments(new MatchedFragmentCollection(m_Spectra.getPrecurserCharge()));
        // match everything
        if (m_Peptide2 != null) {
            getMatcher().matchFragmentsNonGreedy(m_Spectra, m_Peptide1Fragments, m_FragmentTolerance, m_matchedFragments);
            getMatcher().matchFragmentsNonGreedy(m_Spectra, m_Peptide2Fragments, m_FragmentTolerance, m_matchedFragments);
            if (m_crosslinker != null && !(m_crosslinker instanceof NonCovalentBound))
                findCrossLinkedResidues();
        } else
            getMatcher().matchFragmentsNonGreedy(m_Spectra, m_Peptide1Fragments, m_FragmentTolerance, m_matchedFragments);
    }
    
    /**
     * tries to find all possible explanation for a each spectrum peak by
     * finding all fragments, that could produce that peak.
     * will remove all peak matches, that conflict with the given linksites
     */
    public void matchPeptides(int site1, int site2) {
        setMatchedFragments(new MatchedFragmentCollection(m_Spectra.getPrecurserCharge()));
        // match everything
        if (m_Peptide2 != null) {
            getMatcher().matchFragmentsNonGreedy(m_Spectra, m_Peptide1Fragments, m_FragmentTolerance, m_matchedFragments);
            getMatcher().matchFragmentsNonGreedy(m_Spectra, m_Peptide2Fragments, m_FragmentTolerance, m_matchedFragments);
            if (m_crosslinker != null && !(m_crosslinker instanceof NonCovalentBound))
                setCrossLinkedResidues(site1,site2);
        } else
            getMatcher().matchFragmentsNonGreedy(m_Spectra, m_Peptide1Fragments, m_FragmentTolerance, m_matchedFragments);
    }    

    

    /**
     * tries to find all possible explanation for a each spectrum peak by
     * finding all fragments, that could produce that peak. <br/><br/>
     * if a peak was matched by one fragmentPrimary it will not be searched again for a
     * another peptide.
     */
    public void matchPeptidesGreedy() {
        setMatchedFragments(new MatchedFragmentCollection(m_Spectra.getPrecurserCharge()));
        
        if (m_Peptide2 != null) {
            Spectra rest = getMatcher().matchFragments(m_Spectra, m_Peptide1Fragments,  m_FragmentTolerance, m_matchedFragments);
            //m_Unmatched =
            getMatcher().matchFragments(rest, m_Peptide2Fragments, m_FragmentTolerance, m_matchedFragments);
            if (m_crosslinker != null && !(m_crosslinker instanceof NonCovalentBound))
                findCrossLinkedResidues();
        } else
            getMatcher().matchFragmentsNonGreedy(m_Spectra, m_Peptide1Fragments, m_FragmentTolerance, m_matchedFragments);

    }


    /**
     * Deletes matches, that I prefer not to belive in
     */
    public void postProcessMatch() {
        MatchedFragmentCollection mfc = getMatchedFragments();
        for (SpectraPeak p : m_Spectra) {
            for (SpectraPeakMatchedFragment mf : p.getMatchedAnnotation()) {
                Fragment f = mf.getFragment();
                int      c = mf.getCharge();
                if (f instanceof DoubleFragmentation) { // only belive in double fragmentation,

                }
            }
        }
    }




    /**
     * @return the linkage side on the first peptide
     */
    public int getLinkingSitePeptide1() {
        return m_LinkingSitePeptide1;
    }


    /**
     * @return the linkage side on the second peptide
     */
    public int getLinkingSitePeptide2() {
        return m_LinkingSitePeptide2;
    }


    public int[] getLinkSites(Peptide p){
        if (p==getPeptide1())
            return new int[]{getLinkingSitePeptide1()};
        else
            return new int[]{getLinkingSitePeptide2()};
    }



//    /**
//     * @param LinkingSidePeptide2 the m_LinkingSitePeptide2 to set
//     */
//    public void setLinkingSidePeptide2(int LinkingSidePeptide2) {
//        this.m_LinkingSitePeptide2 = LinkingSidePeptide2;
//    }

    public int getLinkingSite(int peptide) {
        if (peptide == 0)
            return getLinkingSitePeptide1();
        else
            return getLinkingSitePeptide2();
    }

    public int getLinkingSite(Peptide peptide) {
        if (peptide == getPeptide1())
            return getLinkingSitePeptide1();
        else
            return getLinkingSitePeptide2();
    }

    /**
     * @return the first matched peptide
     */
    public Peptide getPeptide(int p) {
        if (p >1) return null;
        return p==0?m_Peptide1:m_Peptide2;
    }

    
    /**
     * @return the first matched peptide
     */
    public Peptide getPeptide1() {
        return m_Peptide1;
    }

    /**
     * @return the second  matched peptide
     */
    public Peptide getPeptide2() {
        return m_Peptide2;
    }

    /**
     * @return list of peptides
     */
    public Peptide[] getPeptides() {
        if (m_Peptide2 == null) {
            return new Peptide[]{m_Peptide1};
        } else
            return new Peptide[]{m_Peptide1, m_Peptide2};
    }

    public void setOrderedPeptides() {
        Peptide[] peps = getPeptides();
        if (peps.length == 1) {
            m_orderedPeptides = peps.clone();
        } else {
            HashMap<Peptide,UpdateableInteger> matchcount = new HashMap<Peptide, UpdateableInteger>(2);
            for (Peptide p : peps) {
                matchcount.put(p, new UpdateableInteger(0));
            }

            for (MatchedBaseFragment mbf : m_matchedFragments) {
                Fragment f = mbf.getBaseFragment();
                if (mbf.isBaseFragmentFound() && f.getFragmentationSites().length == 1) {
                    matchcount.get(f.getPeptide()).value++;
                }
            }

            for (int pid = 0; pid < peps.length ; pid ++) {
                matchcount.get(peps[pid]).value *= 100;
                matchcount.get(peps[pid]).value += pid;
            }

            UpdateableInteger[] ids = matchcount.values().toArray(new UpdateableInteger[0]);
            java.util.Arrays.sort(ids);
            m_orderedPeptides = new Peptide[ids.length];

            for (int i = 0; i < ids.length; i++) {
                int pid = ids[i].value % 100;
                m_orderedPeptides[i] = peps[pid];
            }

        }

    }

    /**
     * @return list of peptides
     */
    public Peptide[] getOrderedPeptides() {
        if (m_orderedPeptides == null) {
            setOrderedPeptides();
        }
        return m_orderedPeptides;
    }

    public int getPeptideID(Fragment f) {
        Peptide p = f.getPeptide();
        Peptide[] op = getPeptides();
        for (int pid = 0; pid< op.length; pid++)
            if (p == op[pid])
                return pid;
        throw new Error("Fragment does not belong to this match");
    }

    /**
     * @return the spectrum
     */
    public Spectra getSpectrum() {
        return m_Spectra;
    }

    /**
     * @return the fragmentPrimary tolerance
     */
    public ToleranceUnit getFragmentTolerance() {
        return m_FragmentTolerance;
    }

    /**
     * @param FragmentTolerance the m_FragmentTolerance to set
     */
    public void setFragmentTolerance(ToleranceUnit FragmentTolerance) {
        this.m_FragmentTolerance = FragmentTolerance;
    }

    /**
     * @return the m_Peptide1Fragments
     */
    public ArrayList<Fragment> getPeptide1Fragments() {
        return m_Peptide1Fragments;
    }

    /**
     * @return the m_Peptide2Fragments
     */
    public ArrayList<Fragment> getPeptide2Fragments() {
        return m_Peptide2Fragments;
    }

    public ArrayList<Fragment> getFragments() {
        ArrayList<Fragment> ret = new ArrayList<Fragment>(m_Peptide1Fragments);
        ret.addAll(m_Peptide2Fragments);
        return ret;
    }

    /**
     * returns a list of possible fragments - so returning all fragments
     * for the peptide, that are not in contradiction to the cross linking site.
     * @return
     */
    public ArrayList<Fragment> getPossibleFragments() {
        ArrayList<Fragment> ret = new ArrayList<Fragment>(m_Peptide1Fragments.size() + m_Peptide2Fragments.size());
        for (Fragment f : getFragments()) {
            if (f.canFullfillXlink(m_Peptide1, m_LinkingSitePeptide1) && f.canFullfillXlink(m_Peptide2, m_LinkingSitePeptide2))
                ret.add(f);
//            else if (f.canFullfillXlink(m_Peptide2, m_LinkingSitePeptide2))
//                System.err.println("here");
        }

//        for (Fragment f : getPeptide1Fragments())
//            if (!isMissCrossLinkMatched(m_Peptide1, m_LinkingSitePeptide1, f))
//                ret.add(f);
//
//        for (Fragment f : getPeptide2Fragments())
//            if (!isMissCrossLinkMatched(m_Peptide2, m_LinkingSitePeptide2, f))
//                ret.add(f);

        return ret;
    }

    /**
     * returns a list of possible fragments - so returning all fragments
     * for the peptide, that are not in contradiction to the cross linking site.
     * @return
     */
    public HashMap<Peptide,ArrayList<Fragment>> getPossiblePeptideFragments() {
        HashMap<Peptide,ArrayList<Fragment>> ret = new HashMap<Peptide, ArrayList<Fragment>>(2);
        ArrayList<Fragment> PepFrags = new ArrayList<Fragment>(m_Peptide1Fragments.size());

        for (Fragment f : getPeptide1Fragments())
            if (!f.isClass(DoubleFragmentation.class))
                if (f.canFullfillXlink(m_Peptide1, m_LinkingSitePeptide1))
                    PepFrags.add(f);
        ret.put(m_Peptide1, PepFrags);

        PepFrags = new ArrayList<Fragment>(m_Peptide2Fragments.size());

        for (Fragment f : getPeptide2Fragments())
            if (!f.isClass(DoubleFragmentation.class))
                if (f.canFullfillXlink(m_Peptide2, m_LinkingSitePeptide2))
                    PepFrags.add(f);
        ret.put(m_Peptide2, PepFrags);

        return ret;
    }

    StackTraceElement[] stack = null;

    /**
     * a small function to ease the live of th system.gc();
     */
    public void free() {
        if (stack == null) {
            stack =Thread.currentThread().getStackTrace();
        } else {
            System.err.println("----------------------");
            System.err.println("---- Repeated Free ---");
            System.err.println("free last:");
            for (StackTraceElement ste : stack)
                System.err.println(ste.toString());
            System.err.println("----------------------");
            System.err.println("this free:");
            for (StackTraceElement ste : Thread.currentThread().getStackTrace())
                System.err.println(ste.toString());

        }
        m_Peptide1 = null;
        m_Peptide2 = null;
        if (m_Spectra != null) {
            m_Spectra.free();
            m_Spectra = null;
        }
        if (m_Peptide1Fragments != null) {
            for (Fragment f: m_Peptide1Fragments)
                f.free();
            m_Peptide1Fragments.clear();
            m_Peptide1Fragments = null;
        }
        if (m_Peptide2Fragments != null) {
            for (Fragment f: m_Peptide2Fragments)
                f.free();
            m_Peptide2Fragments.clear();
            m_Peptide2Fragments = null;
        }
//        m_Unmatched.free();
//        m_Unmatched = null;
        m_crosslinker = null;
        setMatcher(null);
        if (m_matchedFragments != null) {
            m_matchedFragments.free();
            setMatchedFragments(null);
        }
    }

    /**
     * @return the crosslinker
     */
    public CrossLinker getCrosslinker() {
        return m_crosslinker;
    }


    /**
     * adds a new score
     * @param name name of the score
     * @param value value of the score
     */
    public void setScore(String name, double value) {
        m_scores.put(name, new Double(value));
    }
    
    /**
     * returns a named score
     * @param name of the score
     * @return
     */
    public double getScore(String name) {
        Double ret = m_scores.get(name);
        return  ret == null ? Double.NaN : ret.doubleValue();
    }


    /**
     * returns a named score
     * @param name of the score
     * @return
     */
    public HashMap<String,Double> getScores() {
        return  m_scores;
    }

    /**
     * @return the m_countPossibleBeta
     */
    public int getCountPossibleBeta() {
        return m_countPossibleBeta;
    }

    /**
     * @param m_countPossibleBeta the m_countPossibleBeta to set
     */
    public void setCountPossibleBeta(int m_countPossibleBeta) {
        this.m_countPossibleBeta = m_countPossibleBeta;
    }

    /**
     * provides a list of the matched fragments. <br/>
     * @return
     */
    public MatchedFragmentCollection getMatchedFragments() {
        return m_matchedFragments;
    }  


    /**
     * provides a list of the matched fragments. <br/>
     * @return
     */
    public MatchedFragmentCollection getPrimaryMatchedFragments() {
        return m_primaryMatchedFragments;
    }
    /**
     * provides a list of the matched fragments. <br/>
     * @return
     */
    public MatchedFragmentCollection getUniquelyMatchedFragments() {
        return m_uniqueMatchedFragments;
    }

    
    
    ArrayList<SpectraPeakMatchedFragment> m_anotations;
    /**
     * provides a list of the matched fragments. <br/>
     * @return
     */
    public ArrayList<SpectraPeakMatchedFragment> getSpectraPeakMatchedFragment() {
        if (m_anotations != null)
            return m_anotations;
        
        m_anotations = new ArrayList<SpectraPeakMatchedFragment>();
        for (SpectraPeak sp : m_Spectra) {
            
            m_anotations.addAll(sp.getMatchedAnnotation());
        }
        return m_anotations;
    }
    
    /**
     * provides a list of the matched fragments. <br/>
     * @return
     */
    public void setPrimaryMatchedFragments(MatchedFragmentCollection mfc) {
        m_primaryMatchedFragments = mfc;
    }

    /**
     * provides a list of the matched fragments. <br/>
     * @return
     */
    public void setUniqueFragments(MatchedFragmentCollection mfc) {
        m_uniqueMatchedFragments = mfc;
    }

    
//    /**
//     * provides a list of the matched fragments that where declared as primary matches for a given peak. <br/>
//     * @return
//     */
//    public MatchedFragmentCollection getMatchedFragments() {
//        return m_matchedFragments;
//    }

    @Override
    protected void finalize() throws Throwable {
        this.m_FragmentTolerance = null;
        this.m_Peptide1 = null;
        this.m_Peptide2 = null;
        if (m_Peptide1Fragments != null)  {
            this.m_Peptide1Fragments.clear();
            this.m_Peptide1Fragments=null;
        }
        if (m_Peptide2Fragments != null)  {
            this.m_Peptide2Fragments.clear();
            this.m_Peptide2Fragments=null;
        }
        if (m_Spectra != null) {
            this.m_Spectra.free();
            this.m_Spectra = null;

        }
        this.m_config = null;
        this.setMatcher(null);
        super.finalize();
    }
    
    public boolean isCrossLinked() {
        if (m_isCrosslinked != null)
            return m_isCrosslinked;
        
        // if we have only one peptide, then there is no question
        if (getPeptides().length == 1) {
            m_isCrosslinked = false;
            return false;
        }
        
        if (getMightBeLinear()) {
//            MatchedFragmentCollection mfc = match.getMatchedFragments();
            MatchedFragmentCollection mfc = getPrimaryMatchedFragments();
            // find the protein, where they could be linear
            Peptide pep1 = getPeptide1();
            int link1 = getLinkingSitePeptide1();
            Peptide pep2 = getPeptide2();
            int link2 = getLinkingSitePeptide2();
            for (Peptide.PeptidePositions pp1 : pep1.getPositions()) {
                for (Peptide.PeptidePositions pp2 : pep2.getPositions()) {
                    if (pp1.base == pp2.base) {
                        UpdateableInteger countNonLossySupport = new UpdateableInteger(0);
                        if (pp1.start+pep1.getLength() == pp2.start) {
                            double supportingIntensity = getIntensitySupportCrosslinked(mfc, pep1, pep2, link1, link2, countNonLossySupport);

                            // we assume, that it is still a cross-linked peptide, if we have at least 10% base-intensity explained
                            if (supportingIntensity >= getSpectrum().getMaxIntensity()/20 || countNonLossySupport.value >= 3) {
                                m_isCrosslinked = true;
                                return true;
                            }else {
                                m_isCrosslinked = false;
                                return false;
                            }
                        
                        } else if (pp2.start+pep2.getLength() == pp1.start) {
                            double supportingIntensity = getIntensitySupportCrosslinked(mfc, pep2, pep1, link2, link1, countNonLossySupport);

                            // we assume, that it is still a cross-linked peptide, if we have at least 10% base-intensity explained
                            if (supportingIntensity >= getSpectrum().getMaxIntensity()/20  || countNonLossySupport.value >= 3) {
                                m_isCrosslinked = true;
                                return true;
                            }else {
                                m_isCrosslinked = false;
                                return false;
                            }
                             

                        }
                    }
                }
                
            }
        }
        m_isCrosslinked = true;
        return true;
        
    }
    
    
    
    private double getIntensitySupportCrosslinked(MatchedFragmentCollection mfc, Peptide pep1, Peptide pep2, int link1, int link2, UpdateableInteger countNonLossy) {
        double supportingIntensity = 0;
        countNonLossy.value = 0;
        // find non-crosslinked y ion of pep1 or crosslinked b-yons of pep1
        // and non-crosslinked b ions of peptide 2 or crosslinked large y-ions
        for (MatchedBaseFragment mbf : mfc) {
            Fragment f = mbf.getBaseFragment();
            // ignore double fragmentation
            if (f.isClass(DoubleFragmentation.class))
                continue;
            
            if (f.getPeptide() == pep1) {
            // linear fragments after the linkage site
                if (f.getStart() > link1 && f.isCTerminal()) {
                    supportingIntensity = getIntensitySupport(mbf, f, pep1, countNonLossy);
                } else if (f.isNTerminal() && f.getEnd() >= link1) {
                    supportingIntensity = getIntensitySupport(mbf, f, pep1, countNonLossy);
                }
            } else {
                // linear fragments before the linkage site
                if (f.getEnd() < link2 && f.isNTerminal()) {
                    supportingIntensity = getIntensitySupport(mbf, f, pep2, countNonLossy);
                } else if (f.isCTerminal() && f.getStart() <= link2) {
                    supportingIntensity = getIntensitySupport(mbf, f, pep2, countNonLossy);
                }
            }
        }
        return supportingIntensity;
    }    
    
    
    private double getIntensitySupport(MatchedBaseFragment mbf, Fragment f, Peptide pep1, UpdateableInteger countNonLossy) {
        double supportingIntensity= 0;
        if (mbf.isBaseFragmentFound()) {
            if (f.getPeptide() == pep1) {
                supportingIntensity+= mbf.getBasePeak().getIntensity();
                countNonLossy.value ++;
            }
        }
        // lossy peaks are only counted with 1/10 of their intensity
        for (SpectraPeak lp : mbf.getLosses().values()) {
            supportingIntensity += lp.getIntensity()/10;
        }
        return supportingIntensity;
    }    
}
