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
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.RunConfig;
import rappsilber.ms.Range;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.dataAccess.AbstractSpectraAccess;
import rappsilber.ms.dataAccess.output.ResultWriter;
import rappsilber.ms.dataAccess.SpectraAccess;
import rappsilber.ms.dataAccess.StackedSpectraAccess;
import rappsilber.ms.dataAccess.filter.candidates.CandidatePairFilter;
import rappsilber.ms.dataAccess.output.BufferedResultWriter;
import rappsilber.ms.dataAccess.output.MinimumRequirementsFilter;
import rappsilber.ms.score.AutoValidation;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.utils.ArithmeticScoredOccurence;
import rappsilber.utils.ScoredOccurence;
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
        double[] weights =  new double[]{0,0};

        public MGXMatchSpectrum(Peptide[] Peptides, CrossLinker cl, int countBeta,Spectra spectrum) {
            super(Peptides, cl, countBeta);
            this.spectrum = spectrum;
        }
        
        public void setWeights(double[] w) {
            weights = w;
        }

        
    }

 
    
    
    @Override
    public void process(SpectraAccess input, ResultWriter output, AtomicBoolean threadStop) {
        SpectraAccess unbufInput = input;
//        BufferedSpectraAccess bsa = new BufferedSpectraAccess(input, 100);
//        input = bsa;
        BufferedResultWriter brw = new BufferedResultWriter(output, 100);
        output = brw;
        String quitReason="";

        try {
            // m_sequences.a


            long allfragments = m_Fragments.getFragmentCount();
            int globalMaxMgcHits = getConfig().getTopMGCHits();
            int maxMgxHits = getConfig().getTopMGXHits();
            double lastProgressReport=Calendar.getInstance().getTimeInMillis();


            if (getConfig().retrieveObject("MINIMUM_REQUIREMENT", true)) {
                double ms2limit = m_config.retrieveObject("MS2ERROR_LIMIT", Double.NaN);
                MinimumRequirementsFilter mrf=null;
                if (Double.isNaN(ms2limit))
                    mrf = new MinimumRequirementsFilter(output);
                else {
                    mrf = new MinimumRequirementsFilter(output,ms2limit);
                }                
                mrf.setMaxRank(maxMgxHits);
                output = mrf;
            }

            boolean evaluateSingles = getConfig().isEvaluateLinears();

            int countSpectra = 0;
            int processed = 0;
            // go through each spectra
            while (true) {
                if (m_config.searchStopped()) {
                    quitReason="Search got stoped through config";
                    break;
                }
                if (!(delayedHasNext(input, unbufInput) && ! m_config.searchStopped())) {
                    quitReason="no new spectra";
                    break;
                }
                if (input.countReadSpectra() % 100 ==  0) {
                    Logger.getLogger(this.getClass().getName()).log(Level.INFO,"("+Thread.currentThread().getName()+")Spectra Read " + unbufInput.countReadSpectra() + "\n");
                    //System.err.println("("+Thread.currentThread().getName()+")Spectra Read " + unbufInput.countReadSpectra() + "\n");
                }

                if (m_doStop) {
                    quitReason="search got stop";
                    break;
                }
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

                ScoredOccurence<MGXMatchSpectrum> mgxScoreMatches = new ArithmeticScoredOccurence<MGXMatchSpectrum>();

                ScoredOccurence<Peptide> mgcMatchScoresAll = new ArithmeticScoredOccurence<>();

                boolean multipleAlphaCandidates = false;
                int alphaConsidered = 0;
                int alphaCount = 0;
                HashMap<String, Integer> mgcListAll = new HashMap<String,Integer>(globalMaxMgcHits);
                int maxMgcHits = globalMaxMgcHits;
                boolean hasMasses = spectraAllchargeStatess.getPeptideCandidateMasses() != null && 
                            spectraAllchargeStatess.getPeptideCandidateMasses().size() > 0;
                
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
                    final ScoredOccurence<Peptide> mgcMatchScores = m_Fragments.getAlphaCandidates(mgx, maxPrecoursorMass);

                    mgcMatchScoresAll.addAllLowest(mgcMatchScores);


                    // try to give back some memory
                    mgc.free();
                    mgc = null;

                    // we get 10 times the accepted alpha candidates to be able to hanlde different modification states as single entries
                    // quite the hack
                    ArrayList<Peptide> scoreSortedAlphaPeptides = mgcMatchScores.getLowestNEntries(maxMgcHits*10, maxMgcHits*100);
                    HashMap<String, Integer> mgcList = new HashMap<String,Integer>(maxMgcHits);

                    double oldAlphaScore  = 2;

                    Integer mgcRank = 0;
                    int mgcRankCount = 0;

                    final HashMap<Peptide,Double> masscandidateWeights = new HashMap<Peptide,Double>();
                    HashSet<Peptide> mgcPeptides = new HashSet<>();

                    ArrayList<Peptide> masscandidatePeptides = new ArrayList<>();

                    // did we get some hints as to what mass the peptides should be?
                    if (hasMasses) {
                        // yes - collect all peptides that fitt these masses
                        // does the spectrum come with an attached tolerance?
                        ToleranceUnit ctu = spectra.getPeptideCandidateTolerance();

                        if (ctu == null) {
                            // no  so look in the config
                            ctu = m_config.getSpectraPeptideMassTollerance();
                            
                        }

                        if (ctu == null) {
                            String message= "Found candidate masses for the peptides but have no information of the assigned tolerance";
                            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,message);
                            m_config.getStatusInterface().setStatus(message);
                            System.exit(-1);
                        }

                        Iterator<Double> masses = spectra.getPeptideCandidateMasses().iterator();
                        Iterator<Double> weights = spectra.getPeptideCandidateMassWeights().iterator();
                        while (masses.hasNext()) {
                            Double w = weights.next();
                            Range range = ctu.getRange(masses.next());
                            ArrayList<Peptide> peps= m_peptides.getForExactMassRange(range.min, range.max);
                            masscandidatePeptides.addAll(peps);
                            for (Peptide p : peps) {
                                masscandidateWeights.put(p, w);
                                String baseSeq = p.toStringBaseSequence();
                                mgcList.put(baseSeq,0);
                                mgcListAll.put(baseSeq,0);    
                            }
                        }

                        // sort the peptides by mgc score
                        java.util.Collections.sort(masscandidatePeptides,new Comparator<Peptide>() {
                            @Override
                            public int compare(Peptide o1, Peptide o2) {
                                double w1 = masscandidateWeights.get(o1);
                                double w2 = masscandidateWeights.get(o2);
                                int ret = Double.compare(w1, w2);
                                if (ret == 0) {
                                    return Double.compare(mgcMatchScores.Score(o1, 1),mgcMatchScores.Score(o2, 1));
                                } else 
                                    return ret;
                            }
                        });
                        
                        // make sure all candidates get considered
                        maxMgcHits = Math.max(masscandidatePeptides.size(), globalMaxMgcHits);
                        
                        //
                        if (m_config.getXLPeptideMassCandidatesExclusive()) {
                            // replace the candidate peptides with the new candidates
                            scoreSortedAlphaPeptides = masscandidatePeptides;

                        }else {
                            // and add them as the first peptides in our list
                            scoreSortedAlphaPeptides.addAll(0, masscandidatePeptides);
                        }
                    }

                    if (scoreSortedAlphaPeptides.size()>1)
                        multipleAlphaCandidates=true;

                    alphaCount += mgcMatchScores.size();
                    
                    MgcLoop:
                    for (Peptide ap : scoreSortedAlphaPeptides) {
                        // make sure we never considere a peptide twice as alpha
                        if (mgcPeptides.contains(ap))
                            continue;
                        mgcPeptides.add(ap);
                        
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

                        alphaConsidered ++;
                        
                        HashSet<Peptide> betaList = new HashSet<>();
                        alphaPeptides.put(ap, betaList);
                        Double alphaWeight = 0.0;
                        if (hasMasses) {
                            alphaWeight = masscandidateWeights.get(ap);
                            if (alphaWeight == null)
                                alphaWeight = 0d;
                        }

                        // not a linear match?
                        if (m_PrecoursorTolerance.compare(ap.getMass(),precoursorMass) != 0) {

                            double gapMass = mgx.getPrecurserMass() - ap.getMass();

                            // for each cross-linker get all the beta - peptide canidates
                            for (CrossLinker cl : m_Crosslinker) {
                                double betaMass = gapMass - cl.getCrossLinkedMass();

                                if (betaMass > AminoAcid.MINIMUM_MASS) {

                                    ArrayList<Peptide> betaCandidates = m_peptides.getForMass(betaMass, precMass);
                                    int betaCount = betaCandidates.size();
                                    betaloop: for (Peptide beta : betaCandidates) {
                                        // beta already seen as alpha before?
                                        HashSet<Peptide> prevBeta = alphaPeptides.get(beta);    
                                        
                                        // we only want to have every peptide pair only ones
                                        if (cl.canCrossLink(ap,beta) && 
                                                (prevBeta == null || !prevBeta.contains(ap)) && 
                                                cl.canCrossLink(ap, beta)) {
                                            for (CandidatePairFilter cf : getCadidatePairFilter()) {
                                                if (!cf.passes(spectra, cl, ap, beta)) {
                                                    continue betaloop;
                                                }
                                            }
                                            betaList.add(beta);
                                            Double betaWeight = 0.0;
                                            if (hasMasses) {
                                                betaWeight =  masscandidateWeights.get(ap);
                                                if (betaWeight == null)
                                                    betaWeight = 0d;
                                            }

                                            double mgxscore = getMGXMatchScores(mgx, ap, beta, cl, allfragments);
                                            MGXMatchSpectrum mms = new MGXMatchSpectrum(new Peptide[]{ap, beta}, cl, betaCount,spectra);
                                            
                                            // did we find these via predefined peptide masses? if so flag the associated weigths
                                            mms.setWeights(new double[]{alphaWeight,betaWeight});
                                            
                                            
                                            mgxScoreMatches.add(mms, mgxscore);

                                        }
                                    }
                                }
                            }

                        }
                    } //mgxloop



                    if (evaluateSingles) {
                        // if we were suppossed to score linears as well
                        for (Peptide p: m_peptidesLinear.getForMass(precMass)) {
                            mgxScoreMatches.add(new MGXMatchSpectrum(new Peptide[]{p}, null, 0,spectra), getMGXMatchLinearScores(mgx, p, allfragments));
                        }
                        
                        for (Peptide p: m_peptides.getForMass(precMass)) {
                            mgxScoreMatches.add(new MGXMatchSpectrum(new Peptide[]{p}, null, 0,spectra), getMGXMatchLinearScores(mgx, p, allfragments));
                        }

                    }

                    mgx.free();


                }
                alphaCount/=specs.size();
                alphaConsidered/=specs.size();

                //MGXMatch[] mgxResults = mgxScoreMatches.getScoredSortedArray(new MGXMatch[0]);
                ArrayList<MGXMatchSpectrum> mgxResults = null;
                if (hasMasses) {
                    mgxResults = mgxScoreMatches.getLowestNEntries(maxMgxHits, maxMgxHits*maxMgxHits, new Comparator<MGXMatchSpectrum>(){
                        @Override
                        public int compare(MGXMatchSpectrum o1, MGXMatchSpectrum o2) {
                            return Double.compare(o2.weights[0]+o2.weights[1], o1.weights[0]+o2.weights[1]);
                        }
                    });
                } else {
                    mgxResults = mgxScoreMatches.getLowestNEntries(maxMgxHits, maxMgxHits*maxMgxHits);
                }

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
                            double firstScore = mgcMatchScoresAll.Score(apeps.get(0),1);
                            int apepsSize = apeps.size();
                            int i = 0;
                            while (++i<apepsSize) {
                                if ((secondMGC=mgcMatchScoresAll.Score(apeps.get(i),1))<firstScore)
                                    break;
                            }
                            if (secondMGC==firstScore)
                                secondMGC = 0;
                            else 
                                secondMGC = -Math.log(secondMGC);
                        }
                    }

                    //int mgxID = 0;
                    for (MGXMatchSpectrum cmgx : mgxResults) {
                        double mgxScore = mgxScoreMatches.Score(cmgx, 0);

                        if (oldMGXScore != mgxScore)
                            mgxRank ++;

                        oldMGXScore=mgxScore;

                        MGXMatch matched = cmgx;
                        Peptide ap = matched.Peptides[0];
                        Peptide bp = (matched.Peptides.length>1? matched.Peptides[1]:null);
                        CrossLinker cl = matched.cl;
                        int betaCount = matched.countBeta;
                        Integer mgcRankAp = mgcListAll.get(ap.toStringBaseSequence());
                        Integer mgcRankBp = null;
                        if (bp != null) {
                            mgcRankBp = mgcListAll.get(bp.toStringBaseSequence());

                            if (mgcRankAp == null) {
                                if (mgcRankBp != null) {
                                    mgcRankAp = mgcRankBp;
                                    mgcRankBp = null;
                                } else {
                                    mgcRankAp = maxMgcHits*2;
                                }
                            } else if (mgcRankBp != null && mgcRankBp<mgcRankAp) {
                                Integer t = mgcRankAp;
                                mgcRankAp = mgcRankBp;
                                mgcRankBp = t;
                            }
                        } else if (mgcRankAp == null) {
                            mgcRankAp = maxMgcHits*2;
                        }


                        double pa = mgcMatchScoresAll.Score(ap, 1);


                        double alphaMGC = -Math.log(pa);


                        double pb = mgcMatchScoresAll.Score(bp, 1);

                        double mgcDelta = 0;
                        double betaMGC = 0;
                        double mgcScore = alphaMGC;
                        double mgcScoreProb = 0;

                        if (bp == null) {
                            mgcDelta = alphaMGC - secondMGC;
                            mgcScoreProb = pa;
                        } else {
                            mgcDelta = alphaMGC - secondMGC;
                            betaMGC = -Math.log(pb);                                
                            mgcScoreProb = 1-(1-pa)*(1-pb);
                            if (pb == 1) {
                                mgcScoreProb = 1 - (1 - pa) * (0.00000001);
                            }
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

                        ArrayList<MatchedXlinkedPeptide> sr = new ArrayList<>(1);
                        evaluateMatch(cmgx.spectrum, ap, bp, cl, alphaCount, alphaConsidered, betaCount, sr, mgcScore, mgcDelta, mgcShiftedDelta, alphaMGC, betaMGC, mgxScore, mgxDelta, mgxRank, mgcRankAp, false);
                        if (cmgx.Peptides.length>1) {
                            for (MatchedXlinkedPeptide mp : sr) {
                                Peptide p = mp.getPeptide1();
                                if (p == cmgx.Peptides[0]) {
                                    mp.setPeptide1Weight(cmgx.weights[0]);
                                } else if (p == cmgx.Peptides[1]) {
                                    mp.setPeptide1Weight(cmgx.weights[1]);
                                }
                                p = mp.getPeptide2();
                                if (p == cmgx.Peptides[0]) {
                                    mp.setPeptide2Weight(cmgx.weights[0]);
                                } else if (p == cmgx.Peptides[1]) {
                                    mp.setPeptide2Weight(cmgx.weights[1]);
                                }
                            }
                        }
                        scanMatches.addAll(sr);
                    }
                }



                int countMatches = scanMatches.size();
                try {
                    sortResultMatches(scanMatches);
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
                if (processed >= 100 || Calendar.getInstance().getTimeInMillis() - lastProgressReport > 10000) {
                    increaseProcessedScans(processed);
                    lastProgressReport=Calendar.getInstance().getTimeInMillis();
                    processed=0;
                }
                if (threadStop.get()) {
                    quitReason="thread stop";
                    System.err.println("Closing down search thread " + Thread.currentThread().getName());
                    break;
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

        // make sure stuff gets writen out before leafing the thread
        brw.selfFinished();
        brw.flush();
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Search Thread {0} finished ("+quitReason +")", Thread.currentThread().getName());

    }

    
    
}
