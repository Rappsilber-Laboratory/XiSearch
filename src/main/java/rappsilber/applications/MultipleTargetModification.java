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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.RunConfig;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.crosslinker.LinearCrosslinker;
import rappsilber.ms.dataAccess.AbstractSpectraAccess;
import rappsilber.ms.dataAccess.output.ResultWriter;
import rappsilber.ms.dataAccess.SpectraAccess;
import rappsilber.ms.dataAccess.StackedSpectraAccess;
import rappsilber.ms.dataAccess.output.BufferedResultWriter;
import rappsilber.ms.dataAccess.output.MinimumRequirementsFilter;
import rappsilber.ms.score.AutoValidation;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.AminoModification;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.utils.ArithmeticScoredOccurence;
import rappsilber.utils.FloatArrayList;
import rappsilber.utils.PermArray;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class MultipleTargetModification extends SimpleXiProcessLinearIncluded{



    double[] m_targetModifications;

    // all combinations of targeted modifcations we are going to search
    HashMap<Float,FloatArrayList> modCombs = new HashMap<Float,FloatArrayList>();
    

    public MultipleTargetModification(File fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        this(new File[]{fasta}, input, output, config, filter);
    }


    public MultipleTargetModification(File[] fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        super(fasta, input, output, config, filter);
        this.check_noncovalent = config.retrieveObject("CHECK_NON_COVALENT", this.check_noncovalent);

    }

    public MultipleTargetModification(SequenceList fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        super(fasta, input, output, config, filter);
        this.check_noncovalent = config.retrieveObject("CHECK_NON_COVALENT", this.check_noncovalent);

    }

    public void prepareSearch() {
        ArrayList<CrossLinker> scl = m_config.getCrossLinker();
        scl.clear();
        scl.add(new LinearCrosslinker("TargetMod"));
        
        String sTargetModifications[] = m_config.retrieveObject("TM_MASSES").toString().split(";");
        m_targetModifications = new double[sTargetModifications.length];
        for (int i = 0 ; i< m_targetModifications.length;i++) {
            m_targetModifications[i] = Double.parseDouble(sTargetModifications[i]);
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
        
        super.prepareSearch();
    }


    
    
    public void process(SpectraAccess input, ResultWriter output, AtomicBoolean threadStop) {
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

            boolean evaluateSingles = getConfig().isEvaluateLinears() ;

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

                if (spectraAllchargeStatess == null) {
                    System.err.println("warning here - did not get a spectra");
                    new Exception().printStackTrace();
                    continue;
                }
                processed ++;
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

                    // the actuall mass of the precursors
                    double precMass = spectra.getPrecurserMass();


                    if (!m_config.isLowResolution())
                        getConfig().getIsotopAnnotation().anotate(spectra);


                    double maxPrecoursorMass = m_PrecoursorTolerance.getMaxRange(precMass);



                    ArithmeticScoredOccurence<MGXMatch> mgxScoreMatches = new ArithmeticScoredOccurence<MGXMatch>();

                    if (evaluateSingles) {
                        // if we were suppos
                        for (Peptide p: m_peptidesLinear.getForMass(precMass)) {
                            mgxScoreMatches.add(new MGXMatch(new Peptide[]{p}, null, 0), getMGXMatchLinearScores(spectra, p, allfragments));
                        }
                    }
                    
                    for (Float combmass : modCombs.keySet()) {
                        FloatArrayList mods = modCombs.get(combmass);
                        // if we were suppos
                        for (Peptide pep: m_peptidesLinear.getForMass(precMass-combmass)) {
                            if (mods.size() <= pep.length()) {
                                Float[] ma = mods.toArray(new Float[pep.length()]);
                                
                                // go through all possible permutations
                                for (Float[] pepmods : new PermArray<Float>(ma)) {
                                    Peptide apClone = pep.clone();
                                    // create a copy of the peptide with the modifications applied
                                    for (int p = 0; p<pep.length(); p++) {
                                        if (pepmods[p] != 0) {
                                            AminoAcid aa = apClone.aminoAcidAt(p);
                                            AminoModification am = new AminoModification(aa.SequenceID+"|"+pepmods[p], aa, aa.mass+pepmods[p]);
                                            apClone.setAminoAcidAt(p, am);
                                        }
                                    }
                                    // and add it to the list
                                    mgxScoreMatches.add(new MGXMatch(new Peptide[]{apClone}, null, 0), 0);
                                }
                            }
                        }
                    }
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

                        for (int mgxID = 0; mgxID <= lastMGXIndex;mgxID++) {
                            MGXMatch matched = mgxResults[mgxID];
                            Peptide ap = matched.Peptides[0];
                            Peptide bp = (matched.Peptides.length>1? matched.Peptides[1]:null);
                            CrossLinker cl = matched.cl;
                            int betaCount = matched.countBeta;


                            double pa = 1;


                            double alphaMGC = 0;


                            double pb = 1;

                            double mgcDelta = 0;
                            double betaMGC = 0;
                            double mgcScore = 0;

                            mgcDelta = 0;

                            double mgxScore = 0;

                            double mgxDelta =  0;

                            // if we have no mgc for the alpha peptide (came from
                            // the linear suplement)
                            // take the mgx-score as an estimate of the mgc-score
                            if (bp == null && pa == 1)
                                mgcScore = mgxScore;

                            double mgcShiftedDelta =  0;//mgcScore - topShiftedCrosslinkedScoreMGCScore;

                            evaluateMatch(spectra, ap, bp, cl, 0, 0, betaCount, scanMatches, mgcScore, mgcDelta, mgcShiftedDelta, alphaMGC, betaMGC, mgxScore, mgxDelta, mgxID,0, false);
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
//            // empty out the buffer - non-propagating.
//            brw.selfFlush();
//            // close the attached thread
//            brw.selfFinished();
////            brw.waitForFinished();
            
            
            //System.err.println("Spectras processed here: " + countSpectra);
        } catch (Exception e) {
            Logger.getLogger(MultipleTargetModification.class.getName()).log(Level.SEVERE, "Error while processing spectra", e);
            System.err.println(e);
            e.printStackTrace(System.err);
            System.exit(1);
        }
        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Search Thread " + Thread.currentThread().getName() + " finished");

    }


    
    
}
