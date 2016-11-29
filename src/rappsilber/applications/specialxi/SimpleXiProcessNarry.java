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
package rappsilber.applications.specialxi;

import rappsilber.applications.*;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.RunConfig;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.crosslinker.SymetricNarrySingleAminoAcidRestrictedCrossLinker;
import rappsilber.ms.dataAccess.AbstractSpectraAccess;
import rappsilber.ms.dataAccess.BufferedSpectraAccess;
import rappsilber.ms.dataAccess.output.ResultWriter;
import rappsilber.ms.dataAccess.SpectraAccess;
import rappsilber.ms.dataAccess.StackedSpectraAccess;
import rappsilber.ms.dataAccess.output.BufferedResultWriter;
//import rappsilber.ms.score.CDRIntensityScore;
import rappsilber.ms.dataAccess.output.MinimumRequirementsFilter;
import rappsilber.ms.score.DummyScore;
import rappsilber.ms.score.J48ModeledManual001;
import rappsilber.ms.score.Normalizer;
import rappsilber.ms.score.RandomTreeModeledManual;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ms.sequence.ions.CrosslinkedFragment;
import rappsilber.ms.sequence.ions.PeptideIon;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptideWeighted;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptideWeightedNnary;
import rappsilber.utils.ArithmeticScoredOccurence;
//import rappsilber.utils.ScoredLinkedList2;
import rappsilber.utils.HashMapList;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class SimpleXiProcessNarry extends SimpleXiProcess{



 
    
    protected class MGXMatch {
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


    protected DummyScore m_mgcmgxDeltaScore  = new DummyScore(0, new String[] {"mgcScore", "mgcDelta", "mgcShiftedDelta", "mgcAlpha", "mgcBeta","mgxScore" , "mgxDelta"});
    protected DummyScore m_alphaBetaRank  = new DummyScore(0, new String[] {"betaCount","betaCountInverse", "mgxRank"});

    private double automatic_evaluation_value = 1;
    
    private boolean relaxedPrecursorMatching = false;

    

    protected class MGCInfo{
        int betaCount;
        int topMGCBeta;

        public MGCInfo(int betaCount, int topMGCBeta) {
            this.betaCount = betaCount;
            this.topMGCBeta = topMGCBeta;
        }

    }



    private final String MatchScore = Normalizer.NAME;



    //private boolean m_doStop = false;


    public SimpleXiProcessNarry(File fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        this(new File[]{fasta}, input, output, config, filter);
    }


    public SimpleXiProcessNarry(File[] fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        super(fasta, input, output, config, filter);

    }

    public SimpleXiProcessNarry(SequenceList fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        super(fasta, input, output, config, filter);

    }

    public void prepareSearch() {
        super.prepareSearch();
        System.err.println("\n\n===========\nprepare search\n==================\n");


        getConfig().getScores().add(m_mgcmgxDeltaScore);
        getConfig().getScores().add(m_alphaBetaRank);
        //getConfig().getScores().add(new PassesManualValidation());
        getConfig().getScores().add(new RandomTreeModeledManual());
        getConfig().getScores().add(new J48ModeledManual001());
        relaxedPrecursorMatching = getConfig().retrieveObject("RELAXEDPRECURSORMATCHING", relaxedPrecursorMatching);
        if (relaxedPrecursorMatching) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "relaxedPrecursorMatching enabled: Matching additional precursor masses");
        }
        

        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Finished - can go on with the search now");
        
//        System.exit(0);
    }




    protected void fragmentTree() {
        //m_sequences.dumpPeptides();
        //        Logger.getLogger(SimpleXiProcess.class.getName()).log(Level.INFO, "Build Peptide Tree");
        //        m_peptides = new PeptideTree(m_sequences, m_FragmentTolerance);
        setStatus("Build Fragmenttree - splitting by peptide mass");
        Logger.getLogger(SimpleXiProcess.class.getName()).log(Level.INFO, "Build Fragment Tree - splitting by peptide mass");
//        m_Fragments = new rappsilber.ms.lookup.fragments.FragmentTreeSlimedMTvArrayOnly(m_peptides, getSequenceList(), getCPUs(), getConfig());
        m_Fragments = new rappsilber.ms.lookup.fragments.FragmentTreeSlimedArrayMassSplitBuild(m_peptides, getSequenceList(), getCPUs(), getConfig());
//        m_Fragments = new rappsilber.ms.lookup.fragments.FragmentMapDB(m_peptides, getSequenceList(), getCPUs(), getConfig());
    }
    
    
    public void process(SpectraAccess input, ResultWriter output) {
        SpectraAccess unbufInput = input;
        BufferedSpectraAccess bsa = new BufferedSpectraAccess(input, 100);
        input = bsa;
        BufferedResultWriter brw = new BufferedResultWriter(output, 100);
        output = brw;
        
        try {
            // m_sequences.a


            long allfragments = m_Fragments.getFragmentCount();
            int maxMgcHits = getConfig().getTopMGCHits();
            int maxMgxHits = getConfig().getTopMGXHits();

            MinimumRequirementsFilter mrf = new MinimumRequirementsFilter(output);
            mrf.setMaxRank(maxMgxHits);
            output = mrf;

            boolean evaluateSingles = getConfig().retrieveObject("EVALUATELINEARS", false) ;

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
                
                Collection<Spectra> specs;
                if (relaxedPrecursorMatching)
                    specs = spectraAllchargeStatess.getRelaxedAlternativeSpectra();
                else
                    specs = spectraAllchargeStatess.getAlternativeSpectra();
                
                
                for (Spectra spectra : specs) {
                    HashMapList<Peptide,Peptide> alphaPeptides = new HashMapList<Peptide, Peptide>();

                    
                    
                    Spectra mgc = getMGCSpectrum(spectra);
                    if (mgc == null)
                        continue;

                    // the actuall mass of the precursors
                    double precMass = spectra.getPrecurserMass();

//                    double topMGC = 0;
//                    double topMGX = 0;
//                    double secondMGC = 0;
//                    double secondMGX = 0;
                    double matchcount = 0;

                    Spectra mgx = getMGXSpectra(mgc, spectra);
                    
//                    if  (mgcFull!= null)  
//                        mgcFull.free();
                    spectra.getIsotopeClusters().clear();

                    if (!m_config.isLowResolution())
                        getConfig().getIsotopAnnotation().anotate(spectra);

                    double precoursorMass = spectra.getPrecurserMass();

                    double maxPrecoursorMass = m_PrecoursorTolerance.getMaxRange(precoursorMass);
                    ArithmeticScoredOccurence<Peptide> mgcMatchScores = getMGCMatchScores(mgc, allfragments, maxPrecoursorMass);



                    // do a lookup on a shifted spectra - to give an idea, as to where random mathches would score
                    double ShiftedPrecoursorMass = precoursorMass + 2*Util.PROTON_MASS;
                    double maxShiftedPrecoursorMass = m_PrecoursorTolerance.getMaxRange(ShiftedPrecoursorMass);


                    double topShiftedMGCScore = 1;
                    double topShiftedCrosslinkedScoreMGCScore = precursorShiftedMGCMatch(topShiftedMGCScore, mgc, allfragments, maxShiftedPrecoursorMass, ShiftedPrecoursorMass);


                    // try to give back some memory
                    mgc.free();
                    mgc = null;

                    Peptide[] scoreSortedAlphaPeptides = mgcMatchScores.getScoredSortedArray(new Peptide[mgcMatchScores.size()]);

//                    System.out.println("\nAlpha,MgcScore");
//                    for (Peptide p : scoreSortedAlphaPeptides) {
//                        System.out.println(p.toString() + "," + mgcMatchScores.Score(p, 2));
//                    }
//
//                    System.out.println();




                    int lastPossibleIndex = Math.min(scoreSortedAlphaPeptides.length - 1, maxMgcHits*100);

//                    int lastAlphaIndex = maxMgcHits  - 1;
//                    if (lastAlphaIndex < 0 || lastAlphaIndex > lastPossibleIndex)
//                        lastAlphaIndex = lastPossibleIndex;


                    // find the last alpha index
//                    while (lastAlphaIndex < lastPossibleIndex &&
//                            mgcMatchScores.Score(scoreSortedAlphaPeptides[lastAlphaIndex], 0) == mgcMatchScores.Score(scoreSortedAlphaPeptides[lastAlphaIndex+1], 0) )
//                                lastAlphaIndex++;

//                    System.out.println("\nLast Alpha: "+ lastAlphaIndex);



                    ArithmeticScoredOccurence<MGXMatch> mgxScoreMatches = new ArithmeticScoredOccurence<MGXMatch>();
                    int mgcRank = 0;
                    HashMap<String, Integer> mgcList = new HashMap<String,Integer>(maxMgcHits);
                    double oldAlphaScore  = 2;

                    //mgc-level
                    // go through the list of sorted alpha peptides until lastindex
                    // if by then we found exactly one mgx-hit go on, to and try to find a second mgx-hit
                    // This is done, so we can get meaningfull delta-values
                    MgcLoop:
//                    for (int a = 0; (!(a >= lastAlphaIndex && (mgxScoreMatches.size() == 0 || mgxScoreMatches.size() > 1))) && a < lastPossibleIndex; a++) {
                    for (int a = 0; a<=lastPossibleIndex && mgcRank < maxMgcHits; a++) {
                        
                        Peptide ap = scoreSortedAlphaPeptides[a];
                        String baseSeq = ap.toStringBaseSequence();
                        double alphaScore = mgcMatchScores.Score(ap, 1);
                        // only count as a "new" mgcRank, if we have not seen the 
                        // unmodified peptide before and if it is 
                        // actually a different score
                        if (!mgcList.containsKey(baseSeq)) {
                            if (alphaScore != oldAlphaScore) {
                                mgcRank++;
                                oldAlphaScore = alphaScore;
                            }
                            mgcList.put(baseSeq,mgcRank);
                        }

                        
                        if (m_PrecoursorTolerance.compare(ap.getMass(),precoursorMass) == 0) {
                            if (evaluateSingles) {
                                double mgxscore = getMGXMatchScores(mgx, ap, null, null, allfragments);


                                mgxScoreMatches.add(new MGXMatch(new Peptide[]{ap}, null, 0), mgxscore);



                            }
                        } else {


                            double gapMass = mgx.getPrecurserMass() - ap.getMass();

                            for (CrossLinker cl : m_Crosslinker) {
                                double betaMass = gapMass - cl.getCrossLinkedMass();
                                
                                if (betaMass > AminoAcid.MINIMUM_MASS) {
                                    
                                    if (cl.getSites() == 2) {
                                    
                                        ArrayList<Peptide> betaCandidates = m_peptides.getForMass(betaMass, precMass);
                                        int betaCount = betaCandidates.size();
                                        for (Peptide beta : betaCandidates) {

                                            // we only want to have every peptide pair only ones
                                            if ((!alphaPeptides.containsKey(beta)) && cl.canCrossLink(ap, beta)) {


                                                double mgxscore = getMGXMatchScores(mgx, ap, beta, cl, allfragments);

                                                mgxScoreMatches.add(new MGXMatch(new Peptide[]{ap, beta}, cl, betaCount), mgxscore);

    //                                            mgxscore = - Math.log(mgxscore);
                                            }

                                        }
                                    } else  {
                                        
                                        double maxBetaMass = gapMass - AminoAcid.MINIMUM_MASS - cl.getCrossLinkedMass();
                                        // look at the lower scoring candidates
                                        for (int b = a; b<=lastPossibleIndex ; b++) {
                                               
                                            Peptide beta = scoreSortedAlphaPeptides[b];
                                            if (beta.getMass() < maxBetaMass) {
                                                double betaScore = mgcMatchScores.Score(beta, 1);          
                                                double gamaMass = gapMass - beta.getMass() - cl.getCrossLinkedMass();
                                                ArrayList<Peptide> gamaCandidates = m_peptides.getForMass(gamaMass, precMass);
                                                int gamaCount = gamaCandidates.size();
                                                for (Peptide gama : gamaCandidates) {

                                                    // we only want to have every peptide pair only ones
                                                    if ((!alphaPeptides.containsKey(gama)) && cl.canCrossLink(ap, gama)) {


                                                        double mgxscore = getMGXMatchScores(mgx, ap, beta, cl, allfragments);

                                                        mgxScoreMatches.add(new MGXMatch(new Peptide[]{ap, beta,gama}, cl, gamaCount), mgxscore);

            //                                            mgxscore = - Math.log(mgxscore);
                                                    }

                                                }
                                                
                                                
                                            }
                                        }
                                    }

                                }

                            }

                            // we have done everything with this peptide - no need to look again at it
                            alphaPeptides.put(ap, ap);


                        }

                    } //mgxloop



                    if (evaluateSingles) {
                        // if we were suppos
                        for (Peptide p: m_peptidesLinear.getForMass(precMass)) {
                            mgxScoreMatches.add(new MGXMatch(new Peptide[]{p}, null, 0), getMGXMatchLinearScores(mgx, p, allfragments));
                        }
                    }

                    mgx.free();
                    
                    MGXMatch[] mgxResults = mgxScoreMatches.getScoredSortedArray(new MGXMatch[0]);


                    if (mgxResults.length > 0) {
                        int lastPossibleMGXIndex = mgxResults.length - 1;


                        int lastMGXIndex = 0;
//                        if ((lastMGXIndex < 0 || lastMGXIndex > lastPossibleMGXIndex))
//                            lastMGXIndex = lastPossibleMGXIndex;


                        HashMap<String,Integer> mgxList = new HashMap<String, Integer>(maxMgxHits);

                        int mgxRank = 0;
                        int oldMGXScore = 2;
                        // find the last alpha index
    //                    while (lastMGXIndex < lastPossibleMGXIndex &&
    //                            mgxScoreMatches.Score(cmgx = mgxResults[lastMGXIndex], 0) == mgxScoreMatches.Score(mgxResults[lastMGXIndex + 1], 0) ) {

                        while (lastMGXIndex < lastPossibleMGXIndex && mgxRank<=maxMgxHits) {
                            MGXMatch cmgx = mgxResults[lastMGXIndex];
                            double mgxScore = mgxScoreMatches.Score(cmgx, 0);
                            String baseSeq1 = cmgx.Peptides[0].toStringBaseSequence();
                            String baseSeq2 = cmgx.Peptides.length >1 ? cmgx.Peptides[1].toStringBaseSequence() : "";

                            String key1 = baseSeq1 + " xl " + baseSeq2;
                            if (!mgxList.containsKey(key1)) {
                                if (oldMGXScore != mgxScore)
                                    mgxRank ++;

                                mgxList.put(baseSeq1 + " xl " + baseSeq2, mgxRank);
                                mgxList.put(baseSeq2 + " xl " + baseSeq1, mgxRank);

                            }
                            lastMGXIndex++;
                        }


                        // just some "heuristic" assuming , that if we have to many
                        // mgx-hits, that might lead to trouble with to many scans
                        // to be evaluated -> out of memory
                        // so we try to cut away the lowest scoring element
                        if (lastMGXIndex > maxMgxHits*maxMgxHits) {
                            // reset to the start point
                            lastMGXIndex = maxMgxHits*maxMgxHits;
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
                            Peptide bp = (matched.Peptides.length>1? matched.Peptides[1]:null);
                            CrossLinker cl = matched.cl;
                            int betaCount = matched.countBeta;


                            double pa = mgcMatchScores.Score(ap, 1);


                            double alphaMGC = -Math.log(pa);


                            double pb = mgcMatchScores.Score(bp, 1);

                            double mgcDelta = 0;
                            double betaMGC = 0;
                            double mgcScore = alphaMGC;

                            if (bp == null) {
                                mgcDelta = alphaMGC - secondMGC;
                            } else {
                                mgcDelta = - Math.log(pa * pb) - secondMGC;
                                betaMGC = -Math.log(pb);
                                mgcScore += betaMGC;
                            }

                            double mgxScore = - Math.log(mgxScoreMatches.Score(matched, 1));

                            double mgxDelta =  mgxScore - secondMGX;

                            // if we have no mgc for the alpha peptide (came from
                            // the linear suplement)
                            // take the mgx-score as an estimate of the mgc-score
                            if (bp == null && pa == 1)
                                mgcScore = mgxScore;

                            double mgcShiftedDelta =  mgcScore - topShiftedCrosslinkedScoreMGCScore;

                            evaluateMatch(spectra, matched.Peptides, cl, betaCount, scanMatches, mgcScore, mgcDelta, mgcShiftedDelta, alphaMGC, betaMGC, mgxScore, mgxDelta, mgxID, false);
                        }
                    }
                    spectra.free();

                }
                spectraAllchargeStatess.free();






                int countMatches = scanMatches.size();
                try {
                    java.util.Collections.sort(scanMatches, new Comparator<MatchedXlinkedPeptide>() {


                        @Override
                        public int compare(MatchedXlinkedPeptide o1, MatchedXlinkedPeptide o2) {
                            //int m = Double.compare(o2.getScore("RandomTreeModeledManual"), o1.getScore("RandomTreeModeledManual"));
                            //int m = Double.compare(o2.getScore("J48ModeledManual001"), o1.getScore("J48ModeledManual001"));
    //                        double o2auto = o2.isCrossLinked()? Math.min(o2.getScore("J48ModeledManual001"),o2.getScore("RandomTreeModeledManual")) : 0;
    //                        double o1auto = o2.isCrossLinked()? Math.min(o1.getScore("J48ModeledManual001"),o1.getScore("RandomTreeModeledManual")) : 0;
    //                        Boolean b;

                            if (o1.passesAutoValidation()) {
                                if (o2.passesAutoValidation()) {
                                    return Double.compare(o2.getScore(MatchScore), o1.getScore(MatchScore));
                                } else
                                    return -1;
                            } else if (o2.passesAutoValidation()) {
                                return 1;
                            }

                            return Double.compare(o2.getScore(MatchScore), o1.getScore(MatchScore));
                        }

                    });
                } catch(Exception e) {
                    setStatus(String.format("Error while sorting the results for scan %s/%s", spectraAllchargeStatess.getScanNumber(), spectraAllchargeStatess.getRun()));
                    Logger.getLogger(this.getClass().getName()).log(Level.OFF, MessageFormat.format("Error while sorting the results for scan {0}/{1}", spectraAllchargeStatess.getScanNumber(), spectraAllchargeStatess.getRun()), e);
                    throw e;
                }

                // is there any match to this spectra left over?
                if (countMatches>0) {
                    MatchedXlinkedPeptide[] matches = scanMatches.toArray(new MatchedXlinkedPeptide[0]);
                    MatchedXlinkedPeptide top = matches[0];
//                    if (matches[0].getScore(automatic_evaluation_score) >= automatic_evaluation_value)
//                        matches[0].setValidated(true);
                    
                    // if the topranking one passes the two autovalidation criterias
                    // and its unlikely to be a linear (like e.g two consequtive peptides with no efidence, that they are not just modified)
                    // than flag this one up as autovalidated
                    if (top.getScore("J48ModeledManual001") >= automatic_evaluation_value && top.getScore("RandomTreeModeledManual") >= automatic_evaluation_value)
                        if (top.getScore("mgxDelta") > 0 && top.getScore(MatchScore) > 7 && top.isCrossLinked())
                            top.setValidated(true);
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
            Logger.getLogger(SimpleXiProcessNarry.class.getName()).log(Level.SEVERE, "Error while processing spectra", e);
            System.err.println(e);
            e.printStackTrace(System.err);
            System.exit(1);
        }

    }


    @Override
    protected MatchedXlinkedPeptide getMatch(Spectra s, Peptide alphaFirst, Peptide beta, CrossLinker cl, boolean primaryOnly) {
        MatchedXlinkedPeptideWeighted match = new MatchedXlinkedPeptideWeighted(s, alphaFirst, beta, cl,getConfig(), primaryOnly);
        //MatchedXlinkedPeptide match       = new MatchedXlinkedPeptide        (s, alphaFirst, beta, cl,getConfig(), primaryOnly);
        return match;
    }

    @Override
    protected MatchedXlinkedPeptide getMatch(Spectra s, Peptide[] peptides, CrossLinker cl, boolean primaryOnly) {
        MatchedXlinkedPeptide match;
        if (cl instanceof SymetricNarrySingleAminoAcidRestrictedCrossLinker) {
             match = new MatchedXlinkedPeptideWeightedNnary(s, peptides, cl,getConfig(), primaryOnly);
        } else {
            match = new MatchedXlinkedPeptideWeighted(s, peptides[0], peptides.length >1? peptides[1] : null, cl,getConfig(), primaryOnly);
        }
        return match;
    }

    protected MatchedXlinkedPeptide evaluateMatch(Spectra sin, Peptide alphaFirst, Peptide beta, CrossLinker cl, int betaCount, Collection<MatchedXlinkedPeptide> scanMatches, double mgcScore, double mgcDeltaScore, double mgcShiftedDeltaScore, double mgcAlphaScore, double mgcBetaScore, double mgxScore, double mgxDelta, int mgxRank, boolean primaryOnly) {
        Spectra s  = sin.cloneComplete();
        MatchedXlinkedPeptide match = getMatch(s, alphaFirst, beta, cl, primaryOnly);
        if (match!= null) {
            m_mgcmgxDeltaScore.setScore(match, "mgcAlpha", mgcAlphaScore);
            m_mgcmgxDeltaScore.setScore(match, "mgcBeta", mgcBetaScore);
            m_mgcmgxDeltaScore.setScore(match, "mgcScore", mgcScore);
            m_mgcmgxDeltaScore.setScore(match, "mgcShiftedDelta", mgcShiftedDeltaScore);
            m_mgcmgxDeltaScore.setScore(match, "mgcDelta", mgcDeltaScore);
            m_mgcmgxDeltaScore.setScore(match, "mgxScore", mgxScore );
            m_mgcmgxDeltaScore.setScore(match, "mgxDelta", mgxDelta );
            m_alphaBetaRank.setScore(match, "mgxRank", mgxRank );
            m_alphaBetaRank.setScore(match, "betaCount", betaCount );
            m_alphaBetaRank.setScore(match, "betaCountInverse", betaCount>0 ? 1.0/betaCount:0);
        }
        match.setPassesAutoValidation(match.getScore("J48ModeledManual001") == 1 && match.getScore("RandomTreeModeledManual") == 1);

        if (super.evaluateMatch(match, betaCount, scanMatches, primaryOnly) == null)
            return null;



//        if ((match.getScore("fragment unique matched non lossy") == 0 && match.getScore("fragment unique matched lossy") ==0) && (match.getScore("fragment non lossy matched") > 0 || match.getScore("fragment lossy matched") >0))
//            new FragmentCoverage().score(match);

        return match;

    }

    //protected MatchedXlinkedPeptide evaluateMatch(Spectra sin, Peptide[] peptides,CrossLinker cl, int betaCount, Collection<MatchedXlinkedPeptide> scanMatches, double[] mgcScore, double[] mgcDeltaScore, double mgxScore, double mgxDelta, int mgxRank, boolean primaryOnly) {
    protected MatchedXlinkedPeptide evaluateMatch(Spectra sin, Peptide[] peptides, CrossLinker cl, int betaCount, Collection<MatchedXlinkedPeptide> scanMatches, double mgcScore, double mgcDeltaScore, double mgcShiftedDeltaScore, double mgcAlphaScore, double mgcBetaScore, double mgxScore, double mgxDelta, int mgxRank, boolean primaryOnly) {
        Spectra s  = sin.cloneComplete();
        MatchedXlinkedPeptide match = getMatch(s, peptides, cl, primaryOnly);
        if (match!= null) {
            m_mgcmgxDeltaScore.setScore(match, "mgcAlpha", mgcAlphaScore);
            m_mgcmgxDeltaScore.setScore(match, "mgcBeta", mgcBetaScore);
            m_mgcmgxDeltaScore.setScore(match, "mgcScore", mgcScore);
            m_mgcmgxDeltaScore.setScore(match, "mgcShiftedDelta", mgcShiftedDeltaScore);
            m_mgcmgxDeltaScore.setScore(match, "mgcDelta", mgcDeltaScore);
            m_mgcmgxDeltaScore.setScore(match, "mgxScore", mgxScore );
            m_mgcmgxDeltaScore.setScore(match, "mgxDelta", mgxDelta );
            m_alphaBetaRank.setScore(match, "mgxRank", mgxRank );
            m_alphaBetaRank.setScore(match, "betaCount", betaCount );
            m_alphaBetaRank.setScore(match, "betaCountInverse", betaCount>0 ? 1.0/betaCount:0);
        }
        //match.setPassesAutoValidation(match.getScore("J48ModeledManual001") == 1 && match.getScore("RandomTreeModeledManual") == 1);

        if (super.evaluateMatch(match, betaCount, scanMatches, primaryOnly) == null)
            return null;



//        if ((match.getScore("fragment unique matched non lossy") == 0 && match.getScore("fragment unique matched lossy") ==0) && (match.getScore("fragment non lossy matched") > 0 || match.getScore("fragment lossy matched") >0))
//            new FragmentCoverage().score(match);

        return match;

    }
   
//    private double getMGXScore(Spectra mgx, ArrayList<Fragment> linearAlphaFragments, ArrayList) {
//        // try to match all linear alpha-fragments
//
//
//
//
//    }



    protected double getMGXMatchScores(Spectra mgx, Peptide alpha, Peptide beta, CrossLinker cl, long allfragments) {
        ArrayList<Fragment> allFragments = alpha.getPrimaryFragments(m_config);
        if (beta != null) {
            ArrayList<Fragment> betaFragments = beta.getPrimaryFragments(m_config);
            allFragments.addAll(rappsilber.ms.sequence.ions.CrosslinkedFragment.createCrosslinkedFragments(allFragments, betaFragments, cl, false));
            allFragments.addAll(betaFragments);
        }
        double score = 1;
        HashSet<SpectraPeak> matchedPeaks = new HashSet<SpectraPeak>();

        for (Fragment f : allFragments) {
            Double fmz = f.getMZ(1);
            SpectraPeak sp = mgx.getPeakAt(fmz);
            if (sp != null && !matchedPeaks.contains(sp)) {
                matchedPeaks.add(sp);
                double spmz = sp.getMZ();
                double scoreMZ = spmz;
//                if (mgx.getScanNumber() == 237 || mgx.getScanNumber() == 238) 
//                    System.out.print("MGX: " + spmz + ", " + f.toString());

                // we try to get scores from the tree of linear fragments, so we have to linearise matched fragments first
                if (f.isClass(CrosslinkedFragment.class)) {
                    CrosslinkedFragment clf = ((CrosslinkedFragment) f);
                    Fragment bf = clf.getBaseFragment();
                    Fragment cf = clf.getCrossLinkedFragment();
                    Fragment subF = null;
                    Fragment pepF = null;
                    
                    if (bf.isClass(PeptideIon.class)) {
                        subF = cf;
                        pepF = bf;
                    } else {
                        subF = bf;
                        pepF = cf;
                    }

                    if (pepF.isClass(PeptideIon.class) && !subF.isClass(PeptideIon.class)) {
                        scoreMZ = subF.getPeptide().getMass() - subF.getMass() + 2*Util.PROTON_MASS;
                        //scoreMZ = mgx.getPrecurserMass() - f.getMass(1) + 2 * Util.PROTON_MASS;
                        score *= ((double) m_Fragments.countPeptides(scoreMZ)) / (double)allfragments;
                        if (mgx.getScanNumber() == 237 || mgx.getScanNumber() == 238)
                            System.out.println(", " + ((double) m_Fragments.countPeptides(scoreMZ)) / (double)allfragments);
                    } else {
                        if (mgx.getScanNumber() == 237 || mgx.getScanNumber() == 238)
                            System.out.println(", ");

                    }
                } else {
                    score *= ((double) m_Fragments.countPeptides(scoreMZ)) / (double)allfragments;
                    if (mgx.getScanNumber() == 237 || mgx.getScanNumber() == 238)
                        System.out.println(", " + ((double) m_Fragments.countPeptides(scoreMZ)) / (double)allfragments);
                }
//                if (mgx.getScanNumber() == 237 || mgx.getScanNumber() == 238) {
//                    System.out.println("mgx," + mgx.getScanNumber() + "," + alpha.toString() +"," + beta.toString() + "," + sp.getMZ() + ", " + score);
//                }
            }
        }

        return score;



        // get all the primary fragments
        // match

//        // get all possible beta
//        // build a fragment tree of the fragments + crosslinked combinations
//        ArithmeticScoredOccurence<Peptide[]> mgcMatchScores = new ArithmeticScoredOccurence<Peptide[]>();
//        //   go through mgc spectra
//        for (SpectraPeak sp : mgx) {
//            //      for each peak
//            //           count found peptides
//            ArrayList<Peptide> matchedPeptides = m_Fragments.getForMass(sp.getMZ()); // - Util.PROTON_MASS);
//            double peakScore = (double) matchedPeptides.size() / allfragments;
//            for (Peptide p : matchedPeptides) {
//                if (p.getMass() <= precoursorMass) {
//                    mgcMatchScores.multiply(p, peakScore);
//                }
//            }
//        }
//        return mgcMatchScores;
    }


    // assume, that the distribution of fragments in the tree is not only representativ for crosslinkable peptides but also for non-crosslinkable ones
    protected double getMGXMatchLinearScores(Spectra mgx, Peptide linear, long allfragments) {
        ArrayList<Fragment> allFragments = linear.getPrimaryFragments(m_config);

        allfragments += allFragments.size();

        double score = 1;
        HashSet<SpectraPeak> matchedPeaks = new HashSet<SpectraPeak>();

        for (Fragment f : allFragments) {
            Double fmz = f.getMZ(1);
            SpectraPeak sp = mgx.getPeakAt(fmz);
            if (sp != null && !matchedPeaks.contains(sp)) {
                matchedPeaks.add(sp);
                double spmz = sp.getMZ();
                double scoreMZ = spmz;
//                if (mgx.getScanNumber() == 237 || mgx.getScanNumber() == 238)
//                    System.out.print("MGX: " + spmz + ", " + f.toString());

                score *= (m_Fragments.countPeptides(scoreMZ)+ 1.0) / (double)allfragments;
//                if (mgx.getScanNumber() == 237 || mgx.getScanNumber() == 238)
//                    System.out.println(", " + ((double) m_Fragments.countPeptides(scoreMZ)) / (double)allfragments);
            }
        }

        return score;

    }


    protected ArithmeticScoredOccurence<Peptide> getMGCMatchScores(Spectra mgc, long allfragments, double precoursorMass) {
        ArithmeticScoredOccurence<Peptide> mgcMatchScores = new ArithmeticScoredOccurence<Peptide>();
        //   go through mgc spectra
        for (SpectraPeak sp : mgc) {
            //      for each peak
            //           count found peptides
            ArrayList<Peptide> matchedPeptides = m_Fragments.getForMass(sp.getMZ()); // - Util.PROTON_MASS);
            double peakScore = (double) matchedPeptides.size() / allfragments;
//            if (mgc.getScanNumber() == 330)
//                System.out.println(sp.getMZ() + ", " + matchedPeptides.size() + ", " + allfragments + "," + peakScore);
            for (Peptide p : matchedPeptides) {
//                if (p.toString().contentEquals("LKIDPDTKAPNAVVITFEK"))
//                    System.out.println("LKIDPDTKAPNAVVITFEK");
//                if (mgc.getScanNumber() == 1475 && p.toString().replace("bs3nh2", "").replace("bs3oh", "").replace("bs3nh", "").contentEquals("KHEQNKQHIINR"))
//                    p = p;
                if (p.getMass() <= m_PrecoursorTolerance.getMaxRange(precoursorMass)) {
//                    System.out.println("Peak: " + sp.getMZ() + ", " + p.toString());
                    mgcMatchScores.multiply(p, peakScore);
                }
            }
        }
//        if (mgc.getScanNumber() == 330)
//            for (Peptide p: mgcMatchScores.getScoredObjects()) {
//                System.out.println(p.toString() + ", " + mgcMatchScores.Score(p, 0) + ", " + mgcMatchScores.Count(p));
//            }
        return mgcMatchScores;
    }

    protected ArithmeticScoredOccurence<Peptide> getMGCMatchScores(Spectra mgc, long allfragments, double precoursorMass, double spectraPeakDeltaMZ) {
        ArithmeticScoredOccurence<Peptide> mgcMatchScores = new ArithmeticScoredOccurence<Peptide>();
        //   go through mgc spectra
        for (SpectraPeak sp : mgc) {
            //      for each peak
            //           count found peptides
            ArrayList<Peptide> matchedPeptides = m_Fragments.getForMass(sp.getMZ()+spectraPeakDeltaMZ); // - Util.PROTON_MASS);
            double peakScore = (double) matchedPeptides.size() / allfragments;
            for (Peptide p : matchedPeptides) {
                if (p.getMass() <= precoursorMass) {
                    mgcMatchScores.multiply(p, peakScore);
                }
            }
        }
        return mgcMatchScores;
    }

    protected double precursorShiftedMGCMatch(double topShiftedMGCScore, Spectra mgc, long allfragments, double maxShiftedPrecoursorMass, double ShiftedPrecoursorMass) {
        topShiftedMGCScore = - Math.log(topShiftedMGCScore);
        double topShiftedCrosslinkedScoreMGCScore = 1;
        double topShiftedLinearScoreMGCScore = 1;
        {
            int charge = 1;
            while (topShiftedCrosslinkedScoreMGCScore == 1 && charge < 6) {
                ArithmeticScoredOccurence<Peptide> shiftedMgcMatchScores = getMGCMatchScores(mgc, allfragments, maxShiftedPrecoursorMass, Util.PROTON_MASS / charge);
                if (shiftedMgcMatchScores.size() > 0)
                    topShiftedMGCScore = shiftedMgcMatchScores.Score(shiftedMgcMatchScores.getScoredSortedArray(new Peptide[0])[0],1);

                Peptide[] shiftedMatchedPeptides =shiftedMgcMatchScores.getSortedEntries().toArray(new Peptide[0]);

                // find the highest scoring shifted crosslinked match
                shiftedmgc: for (int a = 0; a< shiftedMgcMatchScores.size(); a++) {

                    Peptide ap = shiftedMatchedPeptides[a];
                    // score for the Peptide a
                    double apScore = shiftedMgcMatchScores.Score(shiftedMatchedPeptides[a],1);

                    if (apScore < topShiftedLinearScoreMGCScore &&
                            m_PrecoursorTolerance.compare(ap.getMass(), ShiftedPrecoursorMass) == 0)
                        topShiftedLinearScoreMGCScore = apScore;


                    // could we get better then the best crosslinked score so far?
                    if (apScore*apScore > topShiftedCrosslinkedScoreMGCScore) break;

                    // check all the beta
                    shiftedBeta: for (int b = a; a< shiftedMatchedPeptides.length; a++) {
                        Peptide bp = shiftedMatchedPeptides[b];
                        double apbpscore = shiftedMgcMatchScores.Score(bp,1) * apScore;

                        if (apbpscore < topShiftedCrosslinkedScoreMGCScore)
                            for (CrossLinker cl : m_Crosslinker) {

                                if (m_PrecoursorTolerance.compare(ap.getMass() + bp.getMass() + cl.getCrossLinkedMass(), ShiftedPrecoursorMass) == 0) {
                                    topShiftedCrosslinkedScoreMGCScore = shiftedMgcMatchScores.Score(ap,1) * shiftedMgcMatchScores.Score(bp,1);
                                    break shiftedBeta;
                                }
                            }
                    }

                }
                charge++;
            }
        }
        topShiftedCrosslinkedScoreMGCScore = -Math.log(topShiftedCrosslinkedScoreMGCScore);
        if (topShiftedCrosslinkedScoreMGCScore == 0) {
            topShiftedCrosslinkedScoreMGCScore = -Math.log(topShiftedLinearScoreMGCScore);
        }
        return topShiftedCrosslinkedScoreMGCScore;
    }
    
    
    protected Spectra getMGCSpectrum(Spectra full) {
        int topPeakWindow = 100;
        
        Spectra mgc = null;
//                    Spectra mgcFull =  null;
        if (m_config.isLowResolution()) {

            mgc =  full.cloneTopPeaks(getConfig().getNumberMgcPeaks(), topPeakWindow);

        } else {
            //Spectra mgc = spectra.getMgcSpectra(getConfig().getNumberMgcPeaks());
            //Spectra mgcFull = spectra.getMgcSpectra();
//            mgcFull = spectra.cloneComplete();
////                        // deloss
//            mgcFull.DEFAULT_ISOTOP_DETECTION.anotate(mgcFull);
//            mgcFull = mgcFull.deLoss(18.01056027);
//            mgcFull.DEFAULT_ISOTOP_DETECTION.anotate(mgcFull);
//            mgcFull = mgcFull.deLoss(17.02654493);
//
////                        //decharge
//            mgcFull.DEFAULT_ISOTOP_DETECTION.anotate(mgcFull);
//            mgcFull = mgcFull.deCharge();
////                        
////                        //deisotop
//            mgcFull.DEFAULT_ISOTOP_DETECTION.anotate(mgcFull);
//            mgcFull = mgcFull.deIsotop();
                        
            Spectra mgcFull = full.getMgcSpectra();
//                        Spectra mgcFull = spectra.getMgxSpectra();

            if (mgcFull.getPeaks().isEmpty())
                return null;
            mgc = mgcFull.cloneTopPeaks(getConfig().getNumberMgcPeaks(), topPeakWindow);
            mgcFull.free();
        }
        return mgc;
    }

    protected Spectra getMGXSpectra(Spectra mgc, Spectra full) {
        Spectra mgx;
        if (m_config.isLowResolution()) {
            mgx = mgc.cloneComplete();
        } else {
            
//            Spectra mgxFull = spectra.cloneComplete();
//            // deloss
//            mgxFull.DEFAULT_ISOTOP_DETECTION.anotate(mgxFull);
//            mgxFull = mgxFull.deLoss(18.01056027);
//            mgxFull.DEFAULT_ISOTOP_DETECTION.anotate(mgxFull);
//            mgxFull = mgxFull.deLoss(17.02654493);
//
//            //decharge
//            mgxFull.DEFAULT_ISOTOP_DETECTION.anotate(mgxFull);
//            mgxFull = mgxFull.deCharge();
//
//            //deisotop
//            mgxFull.DEFAULT_ISOTOP_DETECTION.anotate(mgxFull);
//            mgxFull = mgxFull.deIsotop();
            
          
     
            
            Spectra mgxFull = full.getMgxSpectra();

//                        mgx =  mgcFull.cloneTopPeaks(getConfig().getNumberMgcPeaks()*2, 100);
            mgx =  mgxFull.cloneTopPeaks(getConfig().getNumberMgcPeaks()*2, 100);
            mgxFull.free();
//                        mgx = mgc;
//                        spectra.getIsotopeClusters().clear();
        }
        return mgx;
    }
    
    
}
