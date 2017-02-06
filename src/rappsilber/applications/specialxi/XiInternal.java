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
import rappsilber.ms.crosslinker.NonCovalentBound;
import rappsilber.ms.crosslinker.SymetricNarrySingleAminoAcidRestrictedCrossLinker;
import rappsilber.ms.dataAccess.AbstractSpectraAccess;
import rappsilber.ms.dataAccess.output.ResultWriter;
import rappsilber.ms.dataAccess.SpectraAccess;
import rappsilber.ms.dataAccess.StackedSpectraAccess;
import rappsilber.ms.dataAccess.output.BufferedResultWriter;
import rappsilber.ms.dataAccess.output.MinimumRequirementsFilter;
import rappsilber.ms.score.AutoValidation;
import rappsilber.ms.score.DummyScore;
import rappsilber.ms.score.FragmentCoverage;
import rappsilber.ms.score.J48ModeledManual001;
import rappsilber.ms.score.NormalizerML;
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
import rappsilber.utils.HashMapList;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class XiInternal extends SimpleXiProcessLinearIncluded{



 
    
   
    public XiInternal(File fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        super(new File[]{fasta}, input, output, config, filter);
    }


    public XiInternal(File[] fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        super(fasta, input, output, config, filter);
    }

    public XiInternal(SequenceList fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        super(fasta, input, output, config, filter);
    }

    protected void peptideTreeFinalizations() {
        //        System.err.println("Peptides now stored ion the tree : " + m_peptides.size());
        //        try {
        //            ((PeptideTree) m_peptides).dump("/tmp/PeptideTreeDumpAfter.csv");
        //        } catch (IOException ex) {
        //            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        //        }
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Peptides:" + m_peptides.size()); 
        m_peptides.cleanup(m_min_pep_length,1,1);
        m_peptidesLinear.cleanup(m_min_pep_length,1,1);
        // remove ambigious peptides

        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Peptides after cleanup:" + m_peptides.size());
        //        setStatus("total number of peptides:" +  m_peptides.size());
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

            MinimumRequirementsFilter mrf = new MinimumRequirementsFilter(output);
            mrf.setMaxRank(maxMgxHits);
            output = mrf;

            boolean evaluateSingles = getConfig().retrieveObject("EVALUATELINEARS", false) ;

            int countSpectra = 0;
            int processed = 0;
            // go through each spectra
            while (input.hasNext()) {
                if (input.countReadSpectra() % 100 ==  0) {
                    System.err.println("Spectra Read " + unbufInput.countReadSpectra() + "\n");
                }

                if (m_doStop)
                    break;
                // ScoredLinkedList<Peptide,Double> scoredPeptides = new ScoredLinkedList<Peptide, Double>();
                Spectra spectraAllchargeStatess = input.next();
//                int sn = spectraAllchargeStatess.getScanNumber();
                if (spectraAllchargeStatess == null) {
                    System.err.println("warning here - did not get a spectra");
                    new Exception().printStackTrace();
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
                
                
                for (Spectra spectra : specs) {
                    HashMapList<Peptide,Peptide> alphaPeptides = new HashMapList<Peptide, Peptide>();

                    
                    
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
                    ArithmeticScoredOccurence<Peptide> mgcMatchScores = getMGCMatchScores(mgc, allfragments, maxPrecoursorMass);



                    // do a lookup on a shifted spectra - to give an idea, as to where random mathches would score
                    double ShiftedPrecoursorMass = precoursorMass + 2*Util.PROTON_MASS;
                    double maxShiftedPrecoursorMass = m_PrecoursorTolerance.getMaxRange(ShiftedPrecoursorMass);


                    double topShiftedMGCScore = 1;


                    // try to give back some memory
                    mgc.free();
                    mgc = null;

                    ArrayList<Peptide> scoreSortedAlphaPeptides = mgcMatchScores.getLowestNEntries(maxMgcHits, maxMgcHits*100);
                    HashMap<Peptide, Integer> mgcList = new HashMap<Peptide,Integer>(maxMgcHits);
                    double oldAlphaScore  = 2;
                    int mgcRank = 0;
                    ArithmeticScoredOccurence<MGXMatch> mgxScoreMatches = new ArithmeticScoredOccurence<MGXMatch>();
                    MgcLoop:
                    for (Peptide ap : scoreSortedAlphaPeptides) {
                        
                        
                        String baseSeq = ap.toStringBaseSequence();
                        double alphaScore = mgcMatchScores.Score(ap, 1);
                        if (alphaScore != oldAlphaScore) {
                            mgcRank++;
                            oldAlphaScore = alphaScore;
                        }
                        mgcList.put(ap,mgcRank);

                        
                        if (m_PrecoursorTolerance.compare(ap.getMass(),precoursorMass) != 0) {

                            double gapMass = mgx.getPrecurserMass() - ap.getMass();

                            for (CrossLinker cl : m_Crosslinker) {
                                double betaMass = gapMass - cl.getCrossLinkedMass();
                                  
                                if (betaMass > AminoAcid.MINIMUM_MASS) {

                                    ArrayList<Peptide> betaCandidates = m_peptides.getForMass(betaMass, precMass);
                                    int betaCount = betaCandidates.size();
                                    for (Peptide beta : betaCandidates) {
                                        //only internal peptides 
                                        if (ap.getPositions()[0].base.target != beta.getPositions()[0].base.target)
                                            continue;
                                        
                                        
                                        // we only want to have every peptide pair only ones
                                        if (cl.canCrossLink(ap,beta) && (!alphaPeptides.containsKey(beta)) && cl.canCrossLink(ap, beta)) {
                                            

                                            double mgxscore = getMGXMatchScores(mgx, ap, beta, cl, allfragments);

                                            mgxScoreMatches.add(new MGXMatch(new Peptide[]{ap, beta}, cl, betaCount), mgxscore);

//                                            mgxscore = - Math.log(mgxscore);
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
                        
                        for (Peptide p: m_peptides.getForMass(precMass)) {
                            mgxScoreMatches.add(new MGXMatch(new Peptide[]{p}, null, 0), getMGXMatchLinearScores(mgx, p, allfragments));
                        }
                        
                    }

                    mgx.free();

                    
                    ArrayList<MGXMatch> mgxResults = mgxScoreMatches.getLowestNEntries(maxMgxHits, maxMgxHits*maxMgxHits);

                    if (mgxResults.size() > 0) {
  
                        HashMap<String,Integer> mgxList = new HashMap<String, Integer>(maxMgxHits);
                        int mgxRank = 0;
                       
                        double oldMGXScore = 2;
                        // the second best matches are taken as reference - the bigger
                        // the distance between the top and the second the more likely
                        // the top one is right
                        double secondMGX = mgxResults.size() >1 ?  - Math.log(mgxScoreMatches.Score(mgxResults.get(1), 1)) : 0;
                        double secondMGC = scoreSortedAlphaPeptides.size() >1 ?  - Math.log(mgcMatchScores.Score(scoreSortedAlphaPeptides.get(1), 1)):0;
                        //int mgxID = 0;
                        for (MGXMatch cmgx : mgxResults) {
                            double mgxScore = mgxScoreMatches.Score(cmgx, 0);

                            if (oldMGXScore != mgxScore)
                                mgxRank ++;

                            oldMGXScore=mgxScore;
                            
                            MGXMatch matched = cmgx;
                            Peptide ap = matched.Peptides[0];
                            Peptide bp = (matched.Peptides.length>1? matched.Peptides[1]:null);
                            CrossLinker cl = matched.cl;
                            int betaCount = matched.countBeta;
                            Integer mgcRankAp = mgcList.get(ap);
                            if (mgcRankAp == null) {
                                mgcRankAp = 0;
                            }


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

                            mgxScore = - Math.log(mgxScore);

                            double mgxDelta =  mgxScore - secondMGX;

                            // if we have no mgc for the alpha peptide (came from
                            // the linear suplement)
                            // take the mgx-score as an estimate of the mgc-score
                            if (bp == null && pa == 1)
                                mgcScore = mgxScore;

                            double mgcShiftedDelta =  0;//mgcScore - topShiftedCrosslinkedScoreMGCScore;

                            evaluateMatch(spectra, ap, bp, cl, betaCount, scanMatches, mgcScore, mgcDelta, mgcShiftedDelta, alphaMGC, betaMGC, mgxScore, mgxDelta, mgxRank, mgcRankAp, false);
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

                // is there any match to this spectra left over?
                if (countMatches>0) {
                    MatchedXlinkedPeptide[] matches = scanMatches.toArray(new MatchedXlinkedPeptide[0]);
                    MatchedXlinkedPeptide top = matches[0];
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
            Logger.getLogger(XiInternal.class.getName()).log(Level.SEVERE, "Error while processing spectra", e);
            System.err.println(e);
            e.printStackTrace(System.err);
            System.exit(1);
        }
        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Search Thread " + Thread.currentThread().getName() + " finished");

    }


    
    
}
