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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.RunConfig;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.crosslinker.DummyCrosslinker;
import rappsilber.ms.dataAccess.AbstractSpectraAccess;
import rappsilber.ms.dataAccess.BufferedSpectraAccess;
import rappsilber.ms.dataAccess.output.ResultWriter;
import rappsilber.ms.dataAccess.SpectraAccess;
import rappsilber.ms.dataAccess.StackedSpectraAccess;
import rappsilber.ms.dataAccess.output.BufferedResultWriter;
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
import rappsilber.utils.FloatArrayList;
import rappsilber.utils.HashMapList;
import rappsilber.utils.PermArray;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class SimpleXiProcessTargetModificationXlinkMulti extends SimpleXiProcessLinearIncluded{


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


    private double m_minModMass = 10;
    private double m_maxModMass = 800;

    private boolean m_LowResolution = false;

    private DummyScore m_deltaScore  = new DummyScore(0, new String[] {"delta", "combinedDelta"});

    private Sequence m_OpenModSequence = new Sequence(new AminoAcid[]{AminoAcid.X});
    {
        m_OpenModSequence.setFastaHeader("OpenModification");
    }

    double m_targetModifications[];
    /**
     * fake peptides to denote the modification
     */
    Peptide[] m_targetModificationsPeptides;

    private ArrayList<TreeMap<Double, Peptide>> m_Xmodifications;


//    private ModificationLookup m_modifications = new ModificationLookup();
//    private HashMap<AminoModification,UpdateableInteger> m_modCounts = new HashMap<AminoModification, UpdateableInteger>();

    private HashMap<AminoAcid,ModificationLookup> m_OpenModifications;

//    private HashMap<AminoAcid,ModificationLookup> m_KnownModifications;
    private ModificationLookup m_AllKnownModifications = new ModificationLookup();

    
    // all combinations of targeted modifcations we are going to search
    HashMap<Float,FloatArrayList> modCombs = new HashMap<Float,FloatArrayList>();


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

    public SimpleXiProcessTargetModificationXlinkMulti(File fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        this(new File[]{fasta}, input, output, config, filter);
    }


    public SimpleXiProcessTargetModificationXlinkMulti(File[] fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        super(fasta, input, output, config, filter);
    }

    public SimpleXiProcessTargetModificationXlinkMulti(SequenceList fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        super(fasta, input, output, config, filter);

    }

    /**
     *
     */
    @Override
    public void prepareSearch() {
//        try {
//            rappsilber.data.ScoreInfos.read(".rappsilber.data.ScoresOpenMod.csv");
//        } catch (IOException ex) {
//            Logger.getLogger(SimpleXiProcessTargetModificationXlink.class.getName()).log(Level.SEVERE, null, ex);
//        }
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
  
        m_minModMass = m_config.retrieveObject("OM_MIN_MASS",m_minModMass);
        m_maxModMass = m_config.retrieveObject("OM_MAX_MASS",m_maxModMass);
        
        String sTargetModifications[] = m_config.retrieveObject("TM_MASSES").toString().split(";");
        m_targetModifications = new double[sTargetModifications.length];
        m_targetModificationsPeptides = new Peptide[sTargetModifications.length];
        for (int i = 0 ; i< m_targetModifications.length;i++) {
            m_targetModifications[i] = Double.parseDouble(sTargetModifications[i]);
            AminoModification mod = new AminoModification("X" + Util.fiveDigits.format(m_targetModifications[i]), AminoAcid.X, m_targetModifications[i] - Util.WATER_MASS);
            Peptide beta = new Peptide(m_OpenModSequence, 0, 1);
            beta.modify(0, mod);
            m_targetModificationsPeptides[i] = beta;
        }
        

        
        if (m_FragmentTolerance.getUnit().contentEquals("da") && m_FragmentTolerance.getValue() > 0.06)
            m_LowResolution = true;
        
        m_LowResolution = m_config.isLowResolution();

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

        int maxModComb = 1000;
        
        for (double  d : m_targetModifications) {
            FloatArrayList c = new FloatArrayList(1);
            c.add((float) d);
            modCombs.put((float)d, c);
        }
        
        int combiCount = 0;
        while (modCombs.size() < maxModComb && combiCount++<4) {
            for (double d : m_targetModifications) {
                for (float f : modCombs.keySet()) {
                    float mm = (float) (f+d);
                    FloatArrayList c = modCombs.get(f);
                    FloatArrayList cn = new FloatArrayList(c.size()+1);
                    cn.addAll(c);
                    cn.add((float) d);
                    modCombs.put(mm, cn);
                }
            }
            
        }

    }




    public void process(SpectraAccess input, ResultWriter output) {
        try {
            // m_sequences.a

            SpectraAccess unbufInput = input;
            BufferedSpectraAccess bsa = new BufferedSpectraAccess(input, 100);
            input = bsa;
            BufferedResultWriter brw = new BufferedResultWriter(output, 100);
            output = brw;

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
            while (input.hasNext()&& ! m_config.searchStoped()) {
                processed ++;

                if (input.countReadSpectra() % 100 ==  0) {
                    System.err.println("Spectra Read " + input.countReadSpectra() + "\n");
                }

                if (m_doStop)
                    break;
                // ScoredLinkedList<Peptide,Double> scoredPeptides = new ScoredLinkedList<Peptide, Double>();
                Spectra spectraAllchargeStatess = input.next();

                if (spectraAllchargeStatess == null) {
                    System.err.println("warning here - did not get a spectra");
                    new Exception().printStackTrace();
                    continue;
                }
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
                for (Spectra spectra : spectraAllchargeStatess.getChargeStateSpectra()) {
                    HashMapList<Peptide,Peptide> alphaPeptides = new HashMapList<Peptide, Peptide>();


                    // the actuall mass of the precursors
                    double precMass = spectra.getPrecurserMass();

                    double matchcount = 0;

                    Spectra om = null;
                    if (m_LowResolution)
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



                    if (!m_LowResolution)
                        getConfig().getIsotopAnnotation().anotate(spectra);

                    double precoursorMass = spectra.getPrecurserMass();

                    double maxPrecoursorMass = m_PrecoursorTolerance.getMaxRange(precoursorMass);
                    ArithmeticScoredOccurence<Peptide> mgcMatchScores = getMGCMatchScores(om, allfragments, maxPrecoursorMass);



                    Peptide[] scoreSortedAlphaPeptides = mgcMatchScores.getScoredSortedArray(new Peptide[mgcMatchScores.size()]);



                    int lastPossibleIndex = scoreSortedAlphaPeptides.length - 1;

                    int lastAlphaIndex = maxMgcHits  - 1;
                    if (lastAlphaIndex < 0 || lastAlphaIndex > lastPossibleIndex)
                        lastAlphaIndex = lastPossibleIndex;


                    // find the last alpha index
                    while (lastAlphaIndex < lastPossibleIndex &&
                            mgcMatchScores.Score(scoreSortedAlphaPeptides[lastAlphaIndex], 0) == mgcMatchScores.Score(scoreSortedAlphaPeptides[lastAlphaIndex+1], 0) )
                                lastAlphaIndex++;

//                    System.out.println("\nLast Alpha: "+ lastAlphaIndex);



                    ArithmeticScoredOccurence<MGXMatch> mgxScoreMatches = new ArithmeticScoredOccurence<MGXMatch>();

                    //mgc-level
                    // go through the list of sorted alpha peptides until lastindex
                    // if by then we found exactly one mgx-hit go on, to and try to find a second mgx-hit
                    // This is done, so we can get meaningfull delta-values
                    MgcLoop:
                    for (int a = 0; (!(a >= lastAlphaIndex && (mgxScoreMatches.size() == 0 || mgxScoreMatches.size() > 1))) && a < lastPossibleIndex; a++) {
//                    for (int a = 0; a <= lastAlphaIndex; a++) {


                        Peptide ap = scoreSortedAlphaPeptides[a];
                        double alphaScore = mgcMatchScores.Score(ap, 1);

                        double gapMass = om.getPrecurserMass() - ap.getMass();

                        double mgxscore = mgcMatchScores.Score(ap, 0);
                        if (m_PrecoursorTolerance.compare(ap.getMass(),precoursorMass) == 0) {


                            mgxScoreMatches.add(new MGXMatch(new Peptide[]{ap}, null, 0), mgxscore);

                        } else { //if (gapMass >= m_minModMass && gapMass <= m_maxModMass) {
                            
                            Double modmass = null;
                            Peptide modPep = null;
                            for (int i = 0; i<m_targetModifications.length;i++) {
                                double m = m_targetModifications[i];
                                if (m_PrecoursorTolerance.compare(ap.getMass()+m ,precoursorMass) == 0) {
//                                if (gapMass >= m_PrecoursorTolerance.getMinRange(m, precMass) && 
//                                    gapMass <= m_PrecoursorTolerance.getMaxRange(m, precMass)) {
                                    modmass = m;
                                    modPep= m_targetModificationsPeptides[i];
                                    mgxScoreMatches.add(new MGXMatch(new Peptide[]{ap,modPep}, dcl, 0), mgxscore);
                                    break;
                                    
                                }
                            }
                            for (Float combmass : modCombs.keySet()) {
                                FloatArrayList mods = modCombs.get(combmass);
                                
                                if (mods.size() == 0)
                                    continue;
                                
                                if (m_PrecoursorTolerance.compare(ap.getMass()+combmass ,precoursorMass) == 0) {
                                    if (mods.size() <= ap.length()) {
                                        Float[] ma = mods.toArray(new Float[ap.length()]);
                                        
                                        for (Float[] pepmods : new PermArray<Float>(ma)) {
                                            Peptide apClone = ap.clone();
                                            
                                            for (int p = 0; p<ap.length(); p++) {
                                                if (pepmods[p] != 0) {
                                                    AminoAcid aa = apClone.aminoAcidAt(p);
                                                    AminoModification am = new AminoModification(aa.SequenceID+"|"+pepmods[p], aa, aa.mass+pepmods[p]);
                                                    apClone.setAminoAcidAt(p, am);
                                                }
                                            }
                                            mgxScoreMatches.add(new MGXMatch(new Peptide[]{apClone}, null, 0), mgxscore);
                                        }
                                    }
                                }
                                    
                            }

                        }

                    } //mgxloop



                    
                    MGXMatch[] mgxResults = mgxScoreMatches.getScoredSortedArray(new MGXMatch[0]);



                    int lastPossibleMGXIndex = mgxResults.length - 1;


                    int lastMGXIndex = maxMgxHits  - 1;
                    if (lastMGXIndex < 0 || lastMGXIndex > lastPossibleMGXIndex)
                        lastMGXIndex = lastPossibleMGXIndex;

                    int mgxIndexMarker = lastMGXIndex;

                    // find the last alpha index
                    while (lastMGXIndex < lastPossibleMGXIndex &&
                            mgxScoreMatches.Score(mgxResults[lastMGXIndex], 0) == mgxScoreMatches.Score(mgxResults[lastMGXIndex + 1], 0) )
                                lastMGXIndex++;


                    // just some "heuristic" assuming , that we have to many
                    // mgx-hits, that might lead to trouble with to many scans
                    // to be evaluated -> out of memory
                    // so we try to cut away the lowest scoring element
                    if (lastMGXIndex > maxMgxHits*maxMgxHits) {
                        // reset to the start point
                        lastMGXIndex = mgxIndexMarker - 1;
                        // and count backward until we found a better score
                        while (lastMGXIndex >= 0 &&
                                mgxScoreMatches.Score(mgxResults[lastMGXIndex], 0) == mgxScoreMatches.Score(mgxResults[lastMGXIndex + 1], 0) )
                                    lastMGXIndex--;

//                        System.out.println("reduced to Last MGX index : " + lastMGXIndex);
                    }

                    // the second best matches are taken as reference - the bigger
                    // the distance between the top and the second the more likely
                    // the top one is right
                    double secondMGX = mgxResults.length >1 ?  - Math.log(mgxScoreMatches.Score(mgxResults[1], 1)) : 0;
                    double secondMGC = scoreSortedAlphaPeptides.length >1 ?  - Math.log(mgcMatchScores.Score(scoreSortedAlphaPeptides[1], 1)):0;
                    
                    for (int mgxID = 0; mgxID <= lastMGXIndex;mgxID++) {
                        MGXMatch matched = mgxResults[mgxID];
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

                        evaluateMatch(spectra.cloneComplete(), ap, bp, cl, 0, scanMatches, mgcScore, mgcDelta, mgcShiftedDelta, alphaMGC, 0, mgxScore, mgxDelta, mgxID, 0, false);
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
                if (processed >= 10) {
                    increaseProcessedScans(processed);
                    processed=0;
                }
            }
            // empty out the buffer - non-propagating.
            brw.selfFlush();
            // close the attached thread
            brw.selfFinished();
//            brw.waitForFinished();

            //System.err.println("Spectras processed here: " + countSpectra);
        } catch (Exception e) {
            Logger.getLogger(SimpleXiProcessTargetModificationXlinkMulti.class.getName()).log(Level.SEVERE, "Error while processing spectra", e);
            System.err.println(e);
            e.printStackTrace(System.err);
            System.exit(1);
        }

    }






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
