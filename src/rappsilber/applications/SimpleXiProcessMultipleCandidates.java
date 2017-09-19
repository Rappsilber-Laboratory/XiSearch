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
import rappsilber.ms.dataAccess.AbstractSpectraAccess;
import rappsilber.ms.dataAccess.output.ResultWriter;
import rappsilber.ms.dataAccess.SpectraAccess;
import rappsilber.ms.dataAccess.StackedSpectraAccess;
import rappsilber.ms.dataAccess.output.BufferedResultWriter;
import rappsilber.ms.dataAccess.output.MinimumRequirementsFilter;
import rappsilber.ms.score.AutoValidation;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.utils.ArithmeticScoredOccurence;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class SimpleXiProcessMultipleCandidates extends SimpleXiProcessLinearIncluded{

    public SimpleXiProcessMultipleCandidates(SequenceList fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        super(fasta, input, output, config, filter);
    }

    public SimpleXiProcessMultipleCandidates(File fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        super(fasta, input, output, config, filter);
    }


    public SimpleXiProcessMultipleCandidates(File[] fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        super(fasta, input, output, config, filter);
    }


    protected class MGXMatchSpectrum extends MGXMatch  {
        Spectra spectrum;

        public MGXMatchSpectrum(Peptide[] Peptides, CrossLinker cl, int countBeta,Spectra spectrum) {
            super(Peptides, cl, countBeta);
            this.spectrum = spectrum;
        }
        
    }

 
    
    
    public void process(SpectraAccess input, ResultWriter output) {
        SpectraAccess unbufInput = input;
//        BufferedSpectraAccess bsa = new BufferedSpectraAccess(input, 100);
//        input = bsa;
        BufferedResultWriter brw = new BufferedResultWriter(output, 100);
        output = brw;

        
        try {
            // m_sequences.a


            long allfragments = m_Fragments.getFragmentCount();
            int maxMgcHits = getConfig().getTopMGCHits();
            int maxMgxHits = getConfig().getTopMGXHits();

            
            if (getConfig().retrieveObject("MINIMUM_REQUIREMENT", true)) {
                MinimumRequirementsFilter mrf = new MinimumRequirementsFilter(output);
                mrf.setMaxRank(maxMgxHits);
                output = mrf;
            }

            boolean evaluateSingles = getConfig().isEvaluateLinears();

            int countSpectra = 0;
            int processed = 0;
            // go through each spectra
            while (delayedHasNext(input, unbufInput)) {
                if (input.countReadSpectra() % 100 ==  0) {
                    System.err.println("("+Thread.currentThread().getName()+")Spectra Read " + unbufInput.countReadSpectra() + "\n");
                }

                if (m_doStop)
                    break;
                // ScoredLinkedList<Peptide,Double> scoredPeptides = new ScoredLinkedList<Peptide, Double>();
                Spectra spectraAllchargeStatess = input.next();
//                int sn = spectraAllchargeStatess.getScanNumber();
                if (spectraAllchargeStatess == null) {
                    Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "({0}) did not get a spectra", Thread.currentThread().getName());
                    continue;
                }
                processed ++;
                countSpectra ++;

                ArrayList<MatchedXlinkedPeptide> scanMatches = new ArrayList<MatchedXlinkedPeptide>();

                // for some spectra we are not sure of the charge state
                // so we have to considere every possible one for these
                // spectraAllchargeStatess
                
                Collection<Spectra> specs;
                if (isRelaxedPrecursorMatching())
                    specs = spectraAllchargeStatess.getRelaxedAlternativeSpectra();
                else
                    specs = spectraAllchargeStatess.getAlternativeSpectra();
                
                ArithmeticScoredOccurence<MGXMatchSpectrum> mgxScoreMatches = new ArithmeticScoredOccurence<MGXMatchSpectrum>();

                ArithmeticScoredOccurence<Peptide> mgcMatchScoresAll = new ArithmeticScoredOccurence<>();
                boolean multipleAlphaCandidates = false;
                HashMap<String, Integer> mgcListAll = new HashMap<String,Integer>(maxMgcHits);
                
                for (Spectra spectra : specs) {
                    HashMap<Peptide,HashSet<Peptide>> alphaPeptides = new HashMap<Peptide, HashSet<Peptide>>();

                    
                    
                    Spectra mgc = getMGCSpectrum(spectra);
                    if (mgc == null)
                        continue;

                    // the actuall mass of the precursors
                    double precMass = spectra.getPrecurserMass();

                    double matchcount = 0;

                    Spectra mgx = getMGXSpectra(mgc, spectra);
                    
                    spectra.getIsotopeClusters().clear();

                    if (!m_config.isLowResolution())
                        getConfig().getIsotopAnnotation().anotate(spectra);

                    double precoursorMass = spectra.getPrecurserMass();

                    double maxPrecoursorMass = m_PrecoursorTolerance.getMaxRange(precoursorMass);
                    ArithmeticScoredOccurence<Peptide> mgcMatchScores = m_Fragments.getAlphaCandidates(mgx, maxPrecoursorMass);
                    mgcMatchScoresAll.addAllNew(mgcMatchScores);


                    // do a lookup on a shifted spectra - to give an idea, as to where random mathches would score
                    double ShiftedPrecoursorMass = precoursorMass + 2*Util.PROTON_MASS;
                    double maxShiftedPrecoursorMass = m_PrecoursorTolerance.getMaxRange(ShiftedPrecoursorMass);


                    double topShiftedMGCScore = 1;


                    // try to give back some memory
                    mgc.free();
                    mgc = null;

                    // we get 10 times the accepted alpha candidates to be able to hanlde different modification states as single entries
                    // quite the hack
                    ArrayList<Peptide> scoreSortedAlphaPeptides = mgcMatchScores.getLowestNEntries(maxMgcHits*10, maxMgcHits*100);
                    HashMap<String, Integer> mgcList = new HashMap<String,Integer>(maxMgcHits);
                    if (scoreSortedAlphaPeptides.size()>1)
                        multipleAlphaCandidates=true;
                    
                    double oldAlphaScore  = 2;
                    
                    Integer mgcRank = 0;
                    int mgcRankCount = 0;
                    
                    MgcLoop:
                    for (Peptide ap : scoreSortedAlphaPeptides) {
                        
                        // if we already found this peptide with different modifications
                        // we just keep the previous
                        String baseSeq = ap.toStringBaseSequence();
                        double alphaScore = mgcMatchScores.Score(ap, 1);
                        mgcRank = mgcList.get(baseSeq);
                        
                        // if we haven't see this peptide before we see if need to give it a new rank
                        if (mgcRank == null) {
                            if (alphaScore != oldAlphaScore) {
                                mgcRankCount++;
                                oldAlphaScore = alphaScore;
                            }
                            mgcRank=mgcRankCount;
                            mgcList.put(baseSeq,mgcRank);
                            mgcListAll.put(baseSeq,mgcRank);                            
                        }
                        // only accept peptides where at least a modification state had an mgc-rank smaller or equal to the accepted one.
                        if (mgcRank > maxMgcHits)
                            continue;

                        HashSet<Peptide> betaList = new HashSet<>();
                        alphaPeptides.put(ap, betaList);

                        // not a linear match?
                        if (m_PrecoursorTolerance.compare(ap.getMass(),precoursorMass) != 0) {

                            double gapMass = mgx.getPrecurserMass() - ap.getMass();
                            
                            // for each cross-linker get all the beta - peptide canidates
                            for (CrossLinker cl : m_Crosslinker) {
                                double betaMass = gapMass - cl.getCrossLinkedMass();
                                  
                                if (betaMass > AminoAcid.MINIMUM_MASS) {

                                    ArrayList<Peptide> betaCandidates = m_peptides.getForMass(betaMass, precMass);
                                    int betaCount = betaCandidates.size();
                                    for (Peptide beta : betaCandidates) {
                                        // beta already seen as alpha before?
                                        HashSet<Peptide> prevBeta = alphaPeptides.get(beta);
                                        
                                        // we only want to have every peptide pair only ones
                                        if (cl.canCrossLink(ap,beta) && 
                                                (prevBeta == null || !prevBeta.contains(ap)) && 
                                                cl.canCrossLink(ap, beta)) {
                                            betaList.add(beta);
                                            

                                            double mgxscore = getMGXMatchScores(mgx, ap, beta, cl, allfragments);

                                            mgxScoreMatches.add(new MGXMatchSpectrum(new Peptide[]{ap, beta}, cl, betaCount,spectra), mgxscore);

//                                            mgxscore = - Math.log(mgxscore);
                                        }
                                    }
                                }
                            }

                            // we have done everything with this peptide - no need to look again at it
//                            alphaPeptides.put(ap, ap);
                        }
                    } //mgxloop



                    if (evaluateSingles) {
                        // if we were suppos
                        for (Peptide p: m_peptidesLinear.getForMass(precMass)) {
                            mgxScoreMatches.add(new MGXMatchSpectrum(new Peptide[]{p}, null, 0,spectra), getMGXMatchLinearScores(mgx, p, allfragments));
                        }
                        
                        for (Peptide p: m_peptides.getForMass(precMass)) {
                            mgxScoreMatches.add(new MGXMatchSpectrum(new Peptide[]{p}, null, 0,spectra), getMGXMatchLinearScores(mgx, p, allfragments));
                        }
                        
                    }

                    mgx.free();

                    
                }



                //MGXMatch[] mgxResults = mgxScoreMatches.getScoredSortedArray(new MGXMatch[0]);
                ArrayList<MGXMatchSpectrum> mgxResults = mgxScoreMatches.getLowestNEntries(maxMgxHits, maxMgxHits*maxMgxHits);

                if (mgxResults.size() > 0) {

                    HashMap<String,Integer> mgxList = new HashMap<String, Integer>(maxMgxHits);
                    int mgxRank = 0;

                    double oldMGXScore = 2;
                    // the second best matches are taken as reference - the bigger
                    // the distance between the top and the second the more likely
                    // the top one is right
                    double secondMGX = mgxResults.size() >1 ?  - Math.log(mgxScoreMatches.Score(mgxResults.get(1), 1)) : 0;
                    
                    double secondMGC = 0;
                    if (multipleAlphaCandidates) {
                        ArrayList<Peptide> apeps = mgcMatchScoresAll.getLowestNEntries(2, maxMgcHits*100*specs.size());
                        if (apeps.size() >1) {
                            secondMGC=mgcMatchScoresAll.Score(apeps.get(1),1);
                        }
                    }

                    //int mgxID = 0;
                    for (MGXMatchSpectrum cmgx : mgxResults) {
                        double mgxScore = mgxScoreMatches.Score(cmgx, 0);

                        if (oldMGXScore != mgxScore)
                            mgxRank ++;

                        oldMGXScore=mgxScore;

//                            String baseSeq1 = cmgx.Peptides[0].toStringBaseSequence();
//                            String baseSeq2 = cmgx.Peptides.length >1 ? cmgx.Peptides[1].toStringBaseSequence() : "";
//
//                            String key1 = baseSeq1 + " xl " + baseSeq2;
//                            if (!mgxList.containsKey(key1)) {
//
//                                mgxList.put(baseSeq1 + " xl " + baseSeq2, mgxRank);
//                                mgxList.put(baseSeq2 + " xl " + baseSeq1, mgxRank);
//
//                            }

                        MGXMatch matched = cmgx;
                        Peptide ap = matched.Peptides[0];
                        Peptide bp = (matched.Peptides.length>1? matched.Peptides[1]:null);
                        CrossLinker cl = matched.cl;
                        int betaCount = matched.countBeta;
                        Integer mgcRankAp = mgcListAll.get(ap);
                        if (mgcRankAp == null) {
                            mgcRankAp = 0;
                        }


                        double pa = mgcMatchScoresAll.Score(ap, 1);


                        double alphaMGC = -Math.log(pa);


                        double pb = mgcMatchScoresAll.Score(bp, 1);

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

                        mgxScore = - Math.log(mgxScore);

                        double mgxDelta =  mgxScore - secondMGX;

                        // if we have no mgc for the alpha peptide (came from
                        // the linear suplement)
                        // take the mgx-score as an estimate of the mgc-score
                        if (bp == null && pa == 1)
                            mgcScore = mgxScore;

                        double mgcShiftedDelta =  0;//mgcScore - topShiftedCrosslinkedScoreMGCScore;

                        evaluateMatch(cmgx.spectrum, ap, bp, cl, betaCount, scanMatches, mgcScore, mgcDelta, mgcShiftedDelta, alphaMGC, betaMGC, mgxScore, mgxDelta, mgxRank, mgcRankAp, false);
                    }
                }



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
                                    return Double.compare(o2.getScore(getMatchScore()), o1.getScore(getMatchScore()));
                                } else
                                    return -1;
                            } else if (o2.passesAutoValidation()) {
                                return 1;
                            }

                            return Double.compare(o2.getScore(getMatchScore()), o1.getScore(getMatchScore()));
                        }

                    });
                } catch(Exception e) {
                    setStatus(String.format("Error while sorting the results for scan %s/%s", spectraAllchargeStatess.getScanNumber(), spectraAllchargeStatess.getRun()));
                    Logger.getLogger(this.getClass().getName()).log(Level.OFF, MessageFormat.format("Error while sorting the results for scan {0}/{1}", spectraAllchargeStatess.getScanNumber(), spectraAllchargeStatess.getRun()), e);
                    throw e;
                }

                // test wether a linear match should be top-ranked
                checkLinearPostEvaluation(scanMatches);

                // is there any match to this spectra left over?
                if (countMatches>0) {
                    MatchedXlinkedPeptide[] matches = scanMatches.toArray(new MatchedXlinkedPeptide[0]);
                    MatchedXlinkedPeptide top = matches[0];
//                    if (matches[0].getScore(automatic_evaluation_score) >= automatic_evaluation_value)
//                        matches[0].setValidated(true);
                    
                    // if the topranking one passes the two autovalidation criterias
                    // and its unlikely to be a linear (like e.g two consequtive peptides with no efidence, that they are not just modified)
                    // than flag this one up as autovalidated
                    //if (top.getScore("J48ModeledManual001") >= automatic_evaluation_value && top.getScore("RandomTreeModeledManual") >= automatic_evaluation_value)
                    //    if (top.getScore("mgxDelta") > 0 && top.getScore(MatchScore) > 7 && top.isCrossLinked())
                    if (top.getScore(AutoValidation.scorename) == 1.0 && top.isCrossLinked()) {
                        top.setValidated(true);
                    }
                    outputScanMatches(matches, output);
                }
                scanMatches.clear();
                if (processed >= 50) {
                    increaseProcessedScans(processed);
                    processed=0;
                }
            }
            
            increaseProcessedScans(processed);
            //System.err.println("Spectras processed here: " + countSpectra);
        } catch (Exception e) {
            Logger.getLogger(SimpleXiProcessMultipleCandidates.class.getName()).log(Level.SEVERE, "("+Thread.currentThread().getName()+") Error while processing spectra", e);
            System.err.println(e);
            e.printStackTrace(System.err);
            System.exit(1);
        }
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Search Thread {0} finished", Thread.currentThread().getName());

    }

    
    
}
