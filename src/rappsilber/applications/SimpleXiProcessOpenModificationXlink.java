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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.RunConfig;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.crosslinker.DummyCrosslinker;
import rappsilber.ms.dataAccess.AbstractSpectraAccess;
import rappsilber.ms.dataAccess.output.ResultWriter;
import rappsilber.ms.dataAccess.SpectraAccess;
import rappsilber.ms.dataAccess.StackedSpectraAccess;
import rappsilber.ms.dataAccess.output.MinimumRequirementsFilter;
import rappsilber.ms.lookup.ModificationLookup;
import rappsilber.ms.score.DummyScore;
import rappsilber.ms.score.Normalizer;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.AminoModification;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.Sequence;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.utils.ArithmeticScoredOccurence;
import rappsilber.utils.HashMapList;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class SimpleXiProcessOpenModificationXlink extends SimpleXiProcessLinearIncluded{


    private class MGXMatch {
        Peptide[] Peptides;
        CrossLinker cl;
        int         countBeta;
        public MGXMatch(Peptide[] Peptides, CrossLinker cl, int countBeta) {
            this.Peptides = Peptides;
            this.cl = cl;
            this.countBeta = countBeta;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(Peptides[0].toString());
            for (int p = 1; p< Peptides.length; p++)
                sb.append(", " + Peptides[p].toString());
            if (cl != null)
                sb.append(", " + cl.toString());
            return sb.toString();
        }
    }

    /** the smallest mass, that is accepted as unknown modification*/
    private double m_minModMass = 10;
    /** the largest mass, that is accepted as unknown modification*/
    private double m_maxModMass = 800;
    /** in this context means, whether non-modified peptides should be considered */
    private boolean m_evaluateSingles = true;
    /** This flags up, if we do a  low-resolution search, meaning de-isotoping is ignored */
//    private boolean m_LowResolution = false;
    /** Minimum score of the top match for a scan to be reported */
    private double m_minTopScore = Double.NEGATIVE_INFINITY;

    /** a generic score, that is used to provide the delta score */
    private DummyScore m_deltaScore  = new DummyScore(0, new String[] {"delta", "combinedDelta"});

    /** we need a generic sequence, that can provide the "peptide" representing the modification */
    private Sequence m_OpenModSequence = new Sequence(new AminoAcid[]{AminoAcid.X});
    {
        m_OpenModSequence.setFastaHeader("OpenModification");
    }

    /** if this is false only spectra where the top-match has a unknown modification will be reported*/
    private boolean m_output_nonmodified_top = true;
    
    
    /** a list of found modifications */
    private ArrayList<TreeMap<Double, Peptide>> m_Xmodifications;


//    private ModificationLookup m_modifications = new ModificationLookup();
//    private HashMap<AminoModification,UpdateableInteger> m_modCounts = new HashMap<AminoModification, UpdateableInteger>();

    private HashMap<AminoAcid,ModificationLookup> m_OpenModifications;

//    private HashMap<AminoAcid,ModificationLookup> m_KnownModifications;
    private ModificationLookup m_AllKnownModifications = new ModificationLookup();


    {
        m_OpenModifications = new HashMap<AminoAcid, ModificationLookup>();
        for (AminoAcid aa : AminoAcid.getRegisteredAminoAcids()) {
            m_OpenModifications.put(aa, new ModificationLookup());
        }
    }



    private final String MatchScore = Normalizer.NAME;



    //private boolean m_doStop = false;

    {
        m_min_pep_length = 6;
    }

    public SimpleXiProcessOpenModificationXlink(File fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        this(new File[]{fasta}, input, output, config, filter);
    }


    public SimpleXiProcessOpenModificationXlink(File[] fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        super(fasta, input, output, config, filter);
    }

    public SimpleXiProcessOpenModificationXlink(SequenceList fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        super(fasta, input, output, config, filter);

    }

    public void prepareSearch() {
        try {
            rappsilber.data.ScoreInfos.read(".rappsilber.data.ScoresOpenMod.csv");
        } catch (IOException ex) {
            Logger.getLogger(SimpleXiProcessOpenModificationXlink.class.getName()).log(Level.SEVERE, null, ex);
        }
        ArrayList<CrossLinker> scl = m_config.getCrossLinker();
        boolean dummy = false;
        for (CrossLinker cl : scl)
            if (cl instanceof DummyCrosslinker) {
                dummy = true;
                break;
            }
        if (!dummy)
            m_config.getCrossLinker().add(new DummyCrosslinker());


        if (readSequences()) {
            return;
        }
        applyLabel();
        fixedModifications();
        digest();

        variableModifications();

        peptideTreeFinalizations();
        fragmentTree();
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Memory now:" + Runtime.getRuntime().freeMemory());
        rappsilber.utils.Util.forceGC();
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "after gc:" + Runtime.getRuntime().freeMemory());
        setupScores();

        setOutputTopOnly(getConfig().getTopMatchesOnly());
  
//        super.prepareSearch();
        m_minModMass = m_config.retrieveObject("OM_MIN_MASS",m_minModMass);
        m_maxModMass = m_config.retrieveObject("OM_MAX_MASS",m_maxModMass);

        m_output_nonmodified_top = m_config.retrieveObject("OM_MODIFIED_ONLY",true);
        
        if (m_FragmentTolerance.getUnit().contentEquals("da") && m_FragmentTolerance.getValue() > 0.06)
            m_config.setLowResolution();
        
        if (m_config.retrieveObject("LOWRESOLUTION", false)) {
            m_config.setLowResolution();
        }
//        m_LowResolution = m_config.retrieveObject("LOWRESOLUTION",m_LowResolution);

//        m_modifications.setTolerance(m_PrecoursorTolerance);
        for (ModificationLookup ml : m_OpenModifications.values())
            ml.setTolerance(m_PrecoursorTolerance);

        for (AminoModification am : m_config.getVariableModifications()) {
            m_AllKnownModifications.add(am);
//            AminoAcid baa = am.BaseAminoAcid;
//            ModificationLookup ml = m_KnownModifications.get(baa);
//            if (ml == null) {
//                ml = new ModificationLookup();
//                ml.add(am);
//                m_KnownModifications.put(baa, ml);
//            }

        }

        for (Sequence s : getSequenceList()) {
            if (s.getFastaHeader().contains("OpenModification"))
                m_OpenModSequence = s;
        }

        m_Xmodifications = new ArrayList<TreeMap<Double, Peptide>>((int)m_maxModMass);
        for (int i = 0 ; i<(int)m_maxModMass; i++) {
            m_Xmodifications.add(new TreeMap<Double, Peptide>());
        }

        m_evaluateSingles = getConfig().isEvaluateLinears() ;        
        m_minTopScore = m_config.retrieveObject("MINIMUM_TOP_SCORE", m_minTopScore);
    }
    
    
   

    public void process(SpectraAccess input, ResultWriter output) {
        try {
            // m_sequences.a

            SpectraAccess unbufInput = input;
//            BufferedSpectraAccess bsa = new BufferedSpectraAccess(input, 100);
//            input = bsa;
//            BufferedResultWriter brw = new BufferedResultWriter(output, 500);
//            output = brw;

            long allfragments = m_Fragments.getFragmentCount();
            int maxMgcHits = getConfig().getTopMGCHits();
            int maxMgxHits = getConfig().getTopMGXHits();
            DummyCrosslinker dcl = null;
            for (CrossLinker cl : getConfig().getCrossLinker()) {
                if (cl instanceof DummyCrosslinker)
                    dcl = (DummyCrosslinker) cl;
            }
            if (dcl == null)
                dcl = new DummyCrosslinker();
            dcl.setBaseMass(0);
            dcl.setCrossLinkedMass(0);

            MinimumRequirementsFilter mrf = new MinimumRequirementsFilter(output);
            mrf.setMaxRank(maxMgxHits);
            output = mrf;


//            boolean evaluateSingles = true ;

            int countSpectra = 0;
            int processed = 0;
            // go through each spectra
            while (input.hasNext()) {
                processed ++;

                if (input.countReadSpectra() % 100 ==  0) {
                    System.err.println("Spectra Read " + unbufInput.countReadSpectra() + "\n");
                }

                if (m_doStop)
                    break;
                Spectra spectraAllchargeStatess = input.next();
                // if we are looking at the last few available spectra, it can happen, that hasNext returns true
                // but betwwen asking, whether ther is a spectra and retriving that spectra,
                // another thread got already the last one. And then we get null.
                if (spectraAllchargeStatess == null) {
                    System.err.println("warning here - did not get a spectra if it is close to the end this is expected");
                    new Exception().printStackTrace();
                    continue;
                }
//                int sn = spectraAllchargeStatess.getScanNumber();
                countSpectra ++;

                ArrayList<MatchedXlinkedPeptide> scanMatches = new ArrayList<MatchedXlinkedPeptide>();
//                SortedLinkedList<MatchedXlinkedPeptide> scanMatches = new SortedLinkedList<MatchedXlinkedPeptide>(new Comparator<MatchedXlinkedPeptide>() {
//
//                    public int compare(MatchedXlinkedPeptide o1, MatchedXlinkedPeptide o2) {
//                        return Double.compare(o2.getScore(MatchScore), o1.getScore(MatchScore));
//                    }
//                });

                // for some spectra we are not sure of the charge state
                // so we have to considere every possible one for these
                // spectraAllchargeStatess

                Collection<Spectra> specs;
                if (isRelaxedPrecursorMatching())
                    specs = spectraAllchargeStatess.getRelaxedAlternativeSpectra();
                else
                    specs = spectraAllchargeStatess.getAlternativeSpectra();
                
                for (Spectra spectra : specs) {
                    HashMapList<Peptide,Peptide> alphaPeptides = new HashMapList<Peptide, Peptide>();


                    // the actuall mass of the precursors
                    double precMass = spectra.getPrecurserMass();

                    double matchcount = 0;

                    Spectra om = null;
                    if (m_config.isLowResolution())
                        om =  spectra.cloneTopPeaks(getConfig().getNumberMgcPeaks(), 100);
                    else {
                        Spectra omFull = null;
                        omFull = spectra.getOMSpectra();
                        om  =  omFull.cloneTopPeaks(getConfig().getNumberMgcPeaks(), 100);
                        omFull.free();
                    }

                    //Spectra om =  omFull.cloneTopPeaks(getConfig().getNumberMgcPeaks(), 100);
                    // invert each and every peak - so we also have candidates for

                    spectra.getIsotopeClusters().clear();



                    if (!m_config.isLowResolution())
                        getConfig().getIsotopAnnotation().anotate(spectra);

                    double precoursorMass = spectra.getPrecurserMass();

                    double maxPrecoursorMass = m_PrecoursorTolerance.getMaxRange(precoursorMass);
                    if (m_minModMass <0)
                        maxPrecoursorMass-=m_minModMass;
                    ArithmeticScoredOccurence<Peptide> mgcMatchScores = getMGCMatchScores(om, allfragments, maxPrecoursorMass);



                    ArrayList<Peptide> scoreSortedAlphaPeptides = mgcMatchScores.getLowestNEntries(maxMgcHits, maxMgcHits*100);

//                    System.out.println("\nLast Alpha: "+ lastAlphaIndex);



                    ArithmeticScoredOccurence<MGXMatch> mgxScoreMatches = new ArithmeticScoredOccurence<MGXMatch>();

                    //mgc-level
                    // go through the list of sorted alpha peptides until lastindex
                    // if by then we found exactly one mgx-hit go on, to and try to find a second mgx-hit
                    // This is done, so we can get meaningfull delta-values
                    MgcLoop:
                    for (int a = 0; a<scoreSortedAlphaPeptides.size(); a++ ) {
//                    for (int a = 0; a <= lastAlphaIndex; a++) {


                        Peptide ap = scoreSortedAlphaPeptides.get(a);
                        double alphaScore = mgcMatchScores.Score(ap, 1);

                        double gapMass = om.getPrecurserMass() - ap.getMass();

                        double mgxscore = mgcMatchScores.Score(ap, 0);
                        if (m_PrecoursorTolerance.compare(ap.getMass(),precoursorMass) == 0) {


                            mgxScoreMatches.add(new MGXMatch(new Peptide[]{ap}, null, 0), mgxscore);

                        } else if (gapMass >= m_minModMass && gapMass <= m_maxModMass) {
                            // modified candidate
                            Peptide beta = null;

                            SortedMap<Double,Peptide> m = null;
                                // find prefious modifications that would fit to the current gap-mass

                                AminoModification openmod = null;
                                    try {
                                        openmod = new AminoModification("X" + Util.fiveDigits.format(gapMass), AminoAcid.X, gapMass - Util.WATER_MASS);
                                    } catch (NullPointerException ne) {
                                        throw ne;
                                    }
                                     beta = new Peptide(m_OpenModSequence, 0, 1);
                                     beta.modify(0, openmod);

                            mgxScoreMatches.add(new MGXMatch(new Peptide[]{ap,beta}, dcl, 0), mgxscore);


                        }

                    } //mgxloop



                    
                    ArrayList<MGXMatch> mgxResults = mgxScoreMatches.getLowestNEntries(maxMgxHits, maxMgxHits*maxMgxHits);




                    // the second best matches are taken as reference - the bigger
                    // the distance between the top and the second the more likely
                    // the top one is right
                    double secondMGX = mgxResults.size() >1 ?  - Math.log(mgxScoreMatches.Score(mgxResults.get(1), 1)) : 0;
                    double secondMGC = scoreSortedAlphaPeptides.size() >1 ?  - Math.log(mgcMatchScores.Score(scoreSortedAlphaPeptides.get(1), 1)):0;
                    
                    for (int mgxID = 0; mgxID <mgxResults.size();mgxID++) {
                        MGXMatch matched = mgxResults.get(mgxID);
                        Peptide ap = matched.Peptides[0];
                        Peptide bp = null;
                        CrossLinker cl = null;
                        if (matched.Peptides.length >1) {
                            bp = matched.Peptides[1];
                            cl = matched.cl;
                        }

                        double pa = mgcMatchScores.Score(ap, 1);

                        double alphaMGC = -Math.log(pa);

                        double mgcDelta = alphaMGC - secondMGC;
                        double mgcScore = alphaMGC;

                        double mgxScore = - Math.log(mgxScoreMatches.Score(matched, 1));

                        double mgxDelta =  mgxScore - secondMGX;

                        // if we have no mgc for the alpha peptide (came from
                        // the linear suplement)
                        // take the mgx-score as an estimate of the mgc-score
                        mgcScore = mgxScore;

                        double mgcShiftedDelta =  -mgcScore;

                        evaluateMatch(spectra, ap, bp, cl, 0, scanMatches, mgcScore, mgcDelta, mgcShiftedDelta, alphaMGC, 0, mgxScore, mgxDelta, mgxID,0, false);
                    }
                    spectra.free();

                }
                spectraAllchargeStatess.free();






                int countMatches = scanMatches.size();
//                for (int i = 0; i< countMatches; i++) {
//                    MatchedXlinkedPeptide m= scanMatches.get(i);
//                    double omPenality = (m.getPeptides().length-1) / 5.0;
//                }
//                
                java.util.Collections.sort(scanMatches, new Comparator<MatchedXlinkedPeptide>() {


                    @Override
                    public int compare(MatchedXlinkedPeptide o1, MatchedXlinkedPeptide o2) {
                        //int m = Double.compare(o2.getScore("RandomTreeModeledManual"), o1.getScore("RandomTreeModeledManual"));
                        //int m = Double.compare(o2.getScore("J48ModeledManual001"), o1.getScore("J48ModeledManual001"));
                        int m = Double.compare(Math.min(o2.getScore("J48ModeledManual001"),o2.getScore("RandomTreeModeledManual")),
                                Math.min(o1.getScore("J48ModeledManual001"),o1.getScore("RandomTreeModeledManual")));
                        if (m == 0) {
                            return Double.compare(o2.getScore(MatchScore), o1.getScore(MatchScore));
                        }
                        return m;
                    }

                });

                if (countMatches>0) {
                    MatchedXlinkedPeptide[] matches = scanMatches.toArray(new MatchedXlinkedPeptide[0]);
                    outputScanMatches(matches, output);
                    

                }
                scanMatches.clear();
                if (processed >= 20) {
                    increaseProcessedScans(processed);
                    processed=0;
                }
            }
//            // empty out the buffer - non-propagating.
//            brw.selfFlush();
//            // close the attached thread
//            brw.selfFinished();
//            brw.selfWaitForFinished();
//            if (!brw.isFinished()) {
//                Logger.getLogger(SimpleXiProcessOpenModificationXlink.class.getName()).log(Level.WARNING, "There are still spectra in the buffer " + Thread.currentThread().getName());
//                while (brw.isAlive() && !brw.isBufferEmpty()) {
//                    try {
//                        Thread.sleep(100);
//                    }catch (InterruptedException e) {
//                        
//                    }
//                }
//                if (!brw.isBufferEmpty()) {
//                    Logger.getLogger(SimpleXiProcessOpenModificationXlink.class.getName()).log(Level.SEVERE, "There are still spectra in the buffer but buffer-thread died" + Thread.currentThread().getName());
//                }
//            }

            //System.err.println("Spectras processed here: " + countSpectra);
        } catch (Exception e) {
            Logger.getLogger(SimpleXiProcessOpenModificationXlink.class.getName()).log(Level.SEVERE, "Error while processing spectra", e);
            System.err.println(e);
            e.printStackTrace(System.err);
            System.exit(1);
        }

    }




    @Override
    public void outputScanMatches(MatchedXlinkedPeptide[] matches, ResultWriter output) throws IOException {

        MatchedXlinkedPeptide topMatch = matches[0];
        // if the top match is un-modified return
        if (topMatch.getPeptide2() == null && !m_evaluateSingles)
            return;
        
        double topScore = topMatch.getScore(MatchScore);
        
        if (!m_output_nonmodified_top && topMatch.getPeptides().length == 1)
            return;
        
        // ignore matches smaller then ten
        if (topScore < m_minTopScore) 
            return;
        
        super.outputScanMatches(matches, output);
        
//        String topPeptide = topMatch.getPeptide1().toStringBaseSequence();
//        double secondScore = 0;
//        
////        if (topScore < m_minTopScore) 
////            return;
//
//        // define top and second best match based on base-peptides
//        // to reduce the influence of uncertain placemnet of the modification
//        for (int m=1;m<matches.length;m++) {
//            double s = matches[m].getScore(MatchScore);
//            String mPep = matches[m].getPeptide1().toStringBaseSequence();
//            if (s > topScore)  {
//                if (topPeptide.contentEquals(mPep))
//                    topScore = s;
//                else {
//                    secondScore = topScore;
//                    topPeptide = mPep;
//                    topScore = s;
//                }
//            } else if (s > secondScore && !mPep.contentEquals(topPeptide)) {
//                secondScore = s;
//            }
//        }
//
//
//
//        double delta = topScore - secondScore;
//        double combined = (delta + topScore)/2;
//        int rank = 1;
//
//        MatchedXlinkedPeptide m = matches[0];
//        m.setMatchrank(rank);
//
//        double s = m.getScore(MatchScore);
//        double d = s - secondScore;
//        m_deltaScore.setScore(m,"delta", d);
//        m_deltaScore.setScore(m, "combinedDelta", (s + d) / 2.0);
//        output.writeResult(m);
//        double lastS = s;
//        int i;
//        for (i = 1; i < matches.length ; i++) {
//            m = matches[i];
//            s = m.getScore(MatchScore);
//            if (s != lastS)
//                break;
//            d = s - secondScore;
//            m_deltaScore.setScore(m,"delta", d);
//            m_deltaScore.setScore(m, "combinedDelta", (s + d) / 2.0);
//            m.setMatchrank(rank);
//            output.writeResult(m);
//        }
//        if (OutputTopOnly())
//            return;
//
//        for (; i < matches.length ; i++) {
//            m = matches[i];
//            s = m.getScore(MatchScore);
//            if (s != lastS)
//                rank++;
//            d = s - secondScore;
//            m_deltaScore.setScore(m,"delta", d);
//            m_deltaScore.setScore(m, "combinedDelta", (s + d) / 2.0);
//            m.setMatchrank(rank);
//            output.writeResult(m);
//            lastS = s;
//        }
//
    }





//    public void waitEnd() {
//        super.waitEnd();
//        for (AminoModification am: m_modCounts.keySet()) {
//                System.err.println(am.SequenceID + "," + am.weightDiff + "," + m_modCounts.get(am).value);
//        }
//    }


    private boolean couldBeKnownModification(double PrecMass, Peptide ap, int aaPos, double gapMass) {
        ArrayList<AminoModification> ams = m_AllKnownModifications.getForMass(gapMass, PrecMass);
        if (ams != null) {

            HashSet<AminoAcid> baseAAs = new HashSet<AminoAcid>(ams.size());

            for (AminoModification am : ams)
                baseAAs.add(am.BaseAminoAcid);

            int minPos = Math.max(0,aaPos - 1);
            int maxPos = Math.min(ap.length() - 1,aaPos + 1);
            for (int i = minPos; i<= maxPos; i++ ) {
                if (baseAAs.contains(ap.aminoAcidAt(i)))
                    return true;
            }
        };
        return false;
    }


}
