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
package rappsilber.ms.score;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.ions.CrosslinkedFragment;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.PeptideIon;
import rappsilber.ms.sequence.ions.loss.CleavableCrossLinkerPeptide;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.ms.statistics.utils.UpdateableDouble;
import rappsilber.utils.MyArrayUtils;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class FragmentCoverage extends AbstractScoreSpectraMatch {

    public static final String mAll = "total fragment matches";
    public static final String mAllLossy = "lossy fragment matches";
    public static final String m = "matched";
    public static final String mCount = "fragment matches";
//    public static final String  mVSu = "fragment fragmentsMatched/unmatched";
    public static final String mp = "coverage";
    public static final String mpb = "b-coverage";
    public static final String mpy = "y-coverage";
    public static final String mNL = "non lossy matched";
    public static final String mNLp = "non lossy coverage";
    public static final String mL = "lossy matched";
    public static final String mLp = "lossy coverage";
    public static final String mC = "matched conservative";
    public static final String mCp = "conservative coverage";
    public static final String mpU = "unique matched";
    public static final String mpUNL = "unique matched non lossy";
    public static final String mpUNLc = "unique matched non lossy coverage";
    public static final String mpUL = "unique matched lossy";
    public static final String mpULc = "unique matched lossy coverage";
    public static final String mpUC = "unique matched conservative";
    public static final String mpUCp = "unique matched conservative coverage";
    public static final String mpUxl = "unique crosslinked matched";
    public static final String mpUNLxl = "unique crosslinked matched non lossy";
    public static final String mpUNLxlc = "unique crosslinked matched non lossy coverage";
    public static final String mpULxl = "unique crosslinked matched lossy";
    public static final String mpULxlc = "unique crosslinked matched lossy coverage";
    public static final String mpUCxl = "unique crosslinked matched conservative";
    public static final String mpUCxlp = "unique crosslinked matched conservative coverage";
    public static final String mmp = "multimatched%";
    public static final String stc = "sequencetag coverage%";
    public static final String ccPepFrag = "CCPepFragment";
    public static final String ccPepDoubletCount = "CCPepDoubletCount";
    public static final String ccPepDoubletFound = "CCPepDoubletFound";
    public static final String ccPepFragCount = "CCPepFragmentCount";
    public static final String ccPepFragError = "CCPepFragmentError";
    public static final String ccPepFragIntens = "CCPepFragmentIntensity";
    public static final String peptide = "peptide";
    public static final String whole = "fragment ";

    private int minToConservative = 3;

    public static final int MAX_PEPTIDES = 2;
    //private RunConfig   m_config;

    public FragmentCoverage(int ConservativeLosses) {
        minToConservative = ConservativeLosses;
    }

    private class FragCounts {

        int[] count;
        int[] countLossy;
        int[] countLossyPrimary;
        int[] countNonLossy;
        int[] countNonLossyPrimary;
        int[] countPrimary;
        int[] countLossyPrimaryXL;
        int[] countNonLossyPrimaryXL;
        int[] countPrimaryXL;
        int maxfrag;

        public FragCounts(Peptide p) {
            maxfrag = p.length() - 1;
            count = new int[maxfrag];
            countPrimary = new int[maxfrag];
            countNonLossy = new int[maxfrag];
            countNonLossyPrimary = new int[maxfrag];
            countLossy = new int[maxfrag];
            countLossyPrimary = new int[maxfrag];
            countLossyPrimaryXL = new int[maxfrag];
            countNonLossyPrimaryXL = new int[maxfrag];
            countPrimaryXL = new int[maxfrag];
        }

        public FragCounts(int maxFrags) {

            this.maxfrag = maxFrags;
            count = new int[maxfrag];
            countPrimary = new int[maxfrag];
            countNonLossy = new int[maxfrag];
            countNonLossyPrimary = new int[maxfrag];
            countLossy = new int[maxfrag];
            countLossyPrimary = new int[maxfrag];
            countLossyPrimaryXL = new int[maxfrag];
            countNonLossyPrimaryXL = new int[maxfrag];
            countPrimaryXL = new int[maxfrag];

        }

        public void add(SpectraPeakMatchedFragment mf) {
            Fragment f = mf.getFragment();
            int fs = f.length() - 1;
            count[fs]++;
            if (f.isClass(Loss.class)) {
                countLossy[fs]++;
                if (mf.isPrimary()) {
                    countLossyPrimary[fs]++;
                    countPrimary[fs]++;
                    if (f.isClass(CrosslinkedFragment.class))
                        countLossyPrimaryXL[fs]++;
                        countPrimaryXL[fs]++;
                }

            } else {
                countNonLossy[fs]++;
                if (mf.isPrimary()) {
                    countNonLossyPrimary[fs]++;
                    countPrimary[fs]++;
                    if (f.isClass(CrosslinkedFragment.class))
                        countNonLossyPrimaryXL[fs]++;
                        countPrimaryXL[fs]++;
                }
            }
        }

        public void add(FragCounts fc) {
            for (int f = 0; f < maxfrag; f++) {
                count[f] += fc.count[f];
                countPrimary[f] += fc.countPrimary[f];
                countNonLossy[f] += fc.countNonLossy[f];
                countLossy[f] += fc.countLossy[f];
                countNonLossyPrimary[f] += fc.countNonLossyPrimary[f];
                countLossyPrimary[f] += fc.countLossyPrimary[f];
                countLossyPrimaryXL[f] +=  fc.countLossyPrimaryXL[f];
                countNonLossyPrimaryXL[f] += fc.countNonLossyPrimaryXL[f];
                countPrimaryXL[f] += fc.countPrimaryXL[f];
            }
        }

        public void add(FragCounts[] fcs) {
            for (FragCounts fc : fcs) {
                add(fc);
            }
        }

        public FragCounts total() {
            FragCounts fc = new FragCounts(1);
            for (int f = 0; f < maxfrag; f++) {
                fc.count[0] += count[f];
                fc.countPrimary[0] += countPrimary[f];
                fc.countNonLossy[0] += countNonLossy[f];
                fc.countLossy[0] += countLossy[f];
                fc.countNonLossyPrimary[0] += countNonLossyPrimary[f];
                fc.countLossyPrimary[0] += countLossyPrimary[f];
                fc.countPrimaryXL[0] += countPrimaryXL[f];
                fc.countNonLossyPrimaryXL[0] += countNonLossyPrimaryXL[f];
                fc.countLossyPrimaryXL[0] += countLossyPrimaryXL[f];
            }
            return fc;
        }

        //counts each site only ones
        public FragCounts totalMatched() {
            FragCounts fc = new FragCounts(1);
            for (int f = 0; f < maxfrag; f++) {
                if (count[f] > 0) {
                    fc.count[0]++;
                    if (countPrimary[f] > 0) {
                        fc.countPrimary[0]++;
                        if (countPrimaryXL[f] > 0) {
                            fc.countPrimaryXL[0]++;
                        }
                    }
                    if (countNonLossy[f] > 0) {
                        fc.countNonLossy[0]++;
                        if (countNonLossyPrimary[f] > 0) {
                            fc.countNonLossyPrimary[0]++;
                            if (countNonLossyPrimaryXL[f] > 0) {
                                fc.countNonLossyPrimaryXL[0]++;
                            }
                        }
                    }
                    if (countLossy[f] > 0) {
                        fc.countLossy[0]++;
                        if (countLossyPrimary[f] > 0) {
                            fc.countLossyPrimary[0]++;
                            if (countLossyPrimaryXL[f] > 0) {
                                fc.countLossyPrimaryXL[0]++;
                            }
                        }
                    }
                }
            }
            return fc;
        }

        public FragCounts conservative(int minLoss) {
            FragCounts fc = new FragCounts(maxfrag);
            for (int f = 0; f < maxfrag; f++) {
                if (countNonLossy[f] > 0) {
                    fc.count[f] = 1;
                    fc.countNonLossy[f] = 1;
                    if (countNonLossyPrimary[f] > 0) {
                        fc.countPrimary[f] = 1;
                        if (countNonLossyPrimaryXL[f] > 0) {
                            fc.countPrimaryXL[f] = 1;
                        }
                    }
                } else if (countLossy[f] >= minLoss) {
                    fc.count[f] = 1;
                    fc.countLossy[f] = 1;
                    if (countLossyPrimary[f] >= minLoss) {
                        fc.countLossyPrimary[f] = 1;
                        if (countLossyPrimaryXL[f] >= minLoss) {
                            fc.countLossyPrimaryXL[f] = 1;
                        }
                    }
                }
            }
            return fc;
        }

        public FragCounts sequenceTag() {

            FragCounts t = new FragCounts(maxfrag);

            if (maxfrag > 3) {

                for (int f = 1; f < maxfrag - 1; f++) {

                    if ((countNonLossy[f - 1] > 0
                            && countNonLossy[f] > 0
                            && countNonLossy[f + 1] > 0)) {

                        t.count[f - 1] = 1;
                        t.count[f] = 1;
                        t.count[f + 1] = 1;

                    }

                }

            }

            return t;
        }

    }

    public double score(MatchedXlinkedPeptide match) {
        Peptide[] peps = match.getPeptides();
        int pepsCount = peps.length;
        MatchedFragmentCollection mfc = match.getMatchedFragments();

        int fragmentsMatchesNonLossy = 0;
        int fragmentsMatchesLossy = 0;
        int[] peptideMatchesNonLossy = new int[pepsCount];
        int[] peptideMatchesLossy = new int[pepsCount];
        HashMap<Peptide, UpdateableDouble> ccPeptideFragmentFound = new HashMap<>(2);
        HashMap<Peptide, HashSet<Fragment>> ccPeptideFragmentFoundFrags = new HashMap<>(2);
        HashMap<Peptide, HashMap<Integer, HashSet<Fragment>>> ccPeptideFragmentChargeFoundFrags = new HashMap<>(2);
        HashMap<Peptide, UpdateableDouble> ccPeptideFragmentIntensity = new HashMap<>(2);
        int pepID = 0;
        //for (Peptide p : peptideFragments.keySet()) {

        HashMap<Peptide, Integer> pepIds = new HashMap<Peptide, Integer>(pepsCount);

        int precCharge = match.getSpectrum().getPrecurserCharge();

        FragCounts[][] nTerminalFrags = new FragCounts[pepsCount][precCharge + 1];
        FragCounts[][] cTerminalFrags = new FragCounts[pepsCount][precCharge + 1];
        FragCounts[] Frags = new FragCounts[pepsCount];

        int all = 0;
        for (int i = 0; i < pepsCount; i++) {
            nTerminalFrags[i] = new FragCounts[precCharge + 1];
            cTerminalFrags[i] = new FragCounts[precCharge + 1];
            int maxfrag = peps[i].length() - 1;
            pepIds.put(peps[i], i);

            for (int c = 1; c <= precCharge; c++) {
                nTerminalFrags[i][c] = new FragCounts(peps[i]);
                cTerminalFrags[i][c] = new FragCounts(peps[i]);
            }
        }

        // collect all matches
        for (SpectraPeak sp : match.getSpectrum()) {
            for (SpectraPeakMatchedFragment mf : sp.getMatchedAnnotation()) {
                Fragment f = mf.getFragment();
                // ignore everything, that is not basic fragment (y or b-ions or losses of theese but e.g. no double fragmentation)
                if (!f.isBasicFragmentation()) {
                    if (f instanceof CleavableCrossLinkerPeptide.CleavableCrossLinkerPeptideFragment) {
                        Fragment p = ((CleavableCrossLinkerPeptide.CleavableCrossLinkerPeptideFragment) f).getParent();
                        if (p instanceof PeptideIon) {
                            Peptide pep = p.getPeptide();
                            UpdateableDouble error = ccPeptideFragmentFound.get(pep);
                            double e = sp.getMZ() - f.getMZ(mf.getCharge());
                            if (Math.abs(e) > Math.abs(e - Util.C13_MASS_DIFFERENCE)) {
                                e = e - Util.C13_MASS_DIFFERENCE;
                            }
                            if (match.getFragmentTolerance().isRelative()) {
                                e = e / p.getMZ(mf.getCharge()) * 1000000;
                            }
                            
                            if (error == null) { // we have not seen that fragment ye
                                error = new UpdateableDouble(e);
                                ccPeptideFragmentFound.put(pep, error);
                                HashSet<Fragment> frags = new HashSet<Fragment>();
                                frags.add(f);
                                ccPeptideFragmentFoundFrags.put(pep, frags);
                                ccPeptideFragmentIntensity.put(pep,
                                        new UpdateableDouble(
                                                sp.getIntensity()
                                                / match.getSpectrum().getMaxIntensity()));
                            } else {
                                // seen before 
                                error.value = Math.min(error.value, e);
                                HashSet<Fragment> frags = ccPeptideFragmentFoundFrags.get(pep);
                                frags.add(f);
                                double relIntens = sp.getIntensity()
                                        / match.getSpectrum().getMaxIntensity();
                                UpdateableDouble intensity = ccPeptideFragmentIntensity.get(pep);
                                if (intensity.value < relIntens) {
                                    intensity.value = relIntens;
                                }
                            }
                            // record all in charges - to detect doublets
                            HashMap<Integer, HashSet<Fragment>> pepChargeFrags = ccPeptideFragmentChargeFoundFrags.get(pep);
                            if (pepChargeFrags == null) {
                                pepChargeFrags = new HashMap<>();
                                ccPeptideFragmentChargeFoundFrags.put(pep, pepChargeFrags);
                                pepChargeFrags.put(mf.getCharge(), new HashSet<Fragment>(MyArrayUtils.toCollection(new Fragment[]{f})));
                            } else {
                                HashSet<Fragment> chargeFrags = pepChargeFrags.get(mf.getCharge());
                                if (chargeFrags == null) {
                                    chargeFrags = new HashSet<Fragment>(MyArrayUtils.toCollection(new Fragment[]{f}));
                                    pepChargeFrags.put(mf.getCharge(), chargeFrags);
                                } else {
                                    chargeFrags.add(f);
                                }
                            }
                            
                        }
                    }
                    continue;
                }

                int pep = pepIds.get(f.getPeptide());
                int mc = mf.getCharge();

                if (f.isClass(Loss.class)) {
                    fragmentsMatchesLossy++;
                    peptideMatchesLossy[pep]++;
                } else {
                    fragmentsMatchesNonLossy++;
                    peptideMatchesNonLossy[pep]++;
                }

                boolean isNterminal = f.isNTerminal();
                boolean isCTerminal = f.isCTerminal();

                if (isNterminal) {
                    if (nTerminalFrags[pep] == null || nTerminalFrags[pep][mc] == null) {
                        System.err.println("Something strange is here n ");
                    }
                    nTerminalFrags[pep][mc].add(mf);
                }
                if (isCTerminal) {
                    if (cTerminalFrags[pep] == null || cTerminalFrags[pep][mc] == null) {
                        System.err.println("Something strange is here c ");
                    }
                    cTerminalFrags[pep][mc].add(mf);
                }
            }
        }

        // do some summaries
        FragCounts matchCons = new FragCounts(1);
        FragCounts matchAll = new FragCounts(1);
        FragCounts matchTag = new FragCounts(1);
        int matchMulti = 0;
        double matchPossible = 0;

        int wholeCCPepFrag = 0;
        Double wholeCCPepFragError = Double.NaN;
        double wholeCCPepFragIntens = 0;
        int wholeCCPepFragCount = 0;
        int wholeCCPepDoubletCount = 0;
        int wholeCCPepDoubletFound = 0;

        for (int p = 0; p < pepsCount; p++) {
            // how many unique n or cterminal fragmenst do we expect
            // here x,  y and z are seen as the same and a,b and c as well. 
            int maxTermFrags = peps[p].length() - 1;
            Peptide cp = peps[p];
            if (maxTermFrags > 0) {
                double pepAll = maxTermFrags * 2;
                matchPossible += pepAll;
                FragCounts fnCons = new FragCounts(peps[p]);
                FragCounts fcCons = new FragCounts(peps[p]);
                FragCounts fnAll = new FragCounts(peps[p]);
                FragCounts fcAll = new FragCounts(peps[p]);

                for (int c = 1; c <= precCharge; c++) {

                    FragCounts fn = nTerminalFrags[p][c];
                    fnCons.add(fn.conservative(minToConservative));
                    fnAll.add(fn);

                    FragCounts fc = cTerminalFrags[p][c];
                    fcCons.add(fc.conservative(minToConservative));
                    fcAll.add(fc);

                }

                FragCounts tag = fnCons.sequenceTag().total();
                tag.add(fcCons.sequenceTag().total());

                FragCounts consTotal = fnCons.totalMatched();
                consTotal.add(fcCons.totalMatched());
                FragCounts pepUniqueFragMatches = fcAll.totalMatched();
                pepUniqueFragMatches.add(fnAll.totalMatched());
                fnAll = fnAll.total();
                fnAll.add(fcAll.total());
                int pepMulti = 0;
                for (int c = 0; c < fcCons.count.length; c++) {
                    if (fnCons.count[c] > 1) {
                        pepMulti++;
                    }
                    if (fcCons.count[c] > 1) {
                        pepMulti++;
                    }
                }

                matchMulti += pepMulti;
                matchAll.add(pepUniqueFragMatches);
                matchCons.add(consTotal);
                matchTag.add(tag);

                // setup peptide scores
                addScore(match, peptide + (p + 1) + " " + mAll, peptideMatchesNonLossy[p]);
                addScore(match, peptide + (p + 1) + " " + mAllLossy, peptideMatchesLossy[p]);
                addScore(match, peptide + (p + 1) + " " + m, pepUniqueFragMatches.count[0]);
                addScore(match, peptide + (p + 1) + " " + mp, pepUniqueFragMatches.count[0] / pepAll);
                addScore(match, peptide + (p + 1) + " " + mNL, pepUniqueFragMatches.countNonLossy[0]);
                addScore(match, peptide + (p + 1) + " " + mNLp, pepUniqueFragMatches.countNonLossy[0] / pepAll);
                addScore(match, peptide + (p + 1) + " " + mL, pepUniqueFragMatches.countLossy[0]);
                addScore(match, peptide + (p + 1) + " " + mLp, pepUniqueFragMatches.countLossy[0] / pepAll);
                addScore(match, peptide + (p + 1) + " " + mC, consTotal.count[0]);
                addScore(match, peptide + (p + 1) + " " + mCp, consTotal.count[0] / pepAll);
                addScore(match, peptide + (p + 1) + " " + mmp, pepMulti / pepAll);
                addScore(match, peptide + (p + 1) + " " + mpU, pepUniqueFragMatches.countPrimary[0]);
                addScore(match, peptide + (p + 1) + " " + mpUNL, pepUniqueFragMatches.countNonLossy[0]);
                addScore(match, peptide + (p + 1) + " " + mpUNLc, pepUniqueFragMatches.countNonLossy[0] / pepAll);
                addScore(match, peptide + (p + 1) + " " + mpUL, pepUniqueFragMatches.countLossy[0]);
                addScore(match, peptide + (p + 1) + " " + mpULc, pepUniqueFragMatches.countLossy[0] / pepAll);
                addScore(match, peptide + (p + 1) + " " + mpUC, consTotal.countPrimary[0]);
                addScore(match, peptide + (p + 1) + " " + mpUCp, consTotal.countPrimary[0] / pepAll);
        
                addScore(match, peptide + (p + 1) + " " + mpUxl, pepUniqueFragMatches.countPrimaryXL[0]);
                addScore(match, peptide + (p + 1) + " " + mpUNLxl, pepUniqueFragMatches.countNonLossyPrimaryXL[0]);
                addScore(match, peptide + (p + 1) + " " + mpUNLxlc, pepUniqueFragMatches.countNonLossyPrimaryXL[0] / matchPossible);
                addScore(match, peptide + (p + 1) + " " + mpULxl, pepUniqueFragMatches.countLossyPrimaryXL[0]);
                addScore(match, peptide + (p + 1) + " " + mpULxlc, pepUniqueFragMatches.countLossyPrimaryXL[0] / matchPossible);
                addScore(match, peptide + (p + 1) + " " + mpUCxl, consTotal.countPrimaryXL[0]);
                addScore(match, peptide + (p + 1) + " " + mpUCxlp, consTotal.countPrimaryXL[0] / matchPossible);
        
                addScore(match, peptide + (p + 1) + " " + stc, tag.count[0] / pepAll);
                UpdateableDouble e = ccPeptideFragmentFound.get(cp);
                if (e != null) {
                    wholeCCPepFrag += 1;
                    addScore(match, peptide + (p + 1) + " " + ccPepFrag, 1d);
                    wholeCCPepFragError = Double.isNaN(wholeCCPepFragError)? e.value : Math.max(e.value, wholeCCPepFragError);
                    addScore(match, peptide + (p + 1) + " " + ccPepFragError, e.value);
                    wholeCCPepFragIntens = Math.min(e.value, ccPeptideFragmentIntensity.get(cp).value);
                    addScore(match, peptide + (p + 1) + " " + ccPepFragIntens, ccPeptideFragmentIntensity.get(cp).value);
                    wholeCCPepFragCount += ccPeptideFragmentFoundFrags.get(cp).size();
                    addScore(match, peptide + (p + 1) + " " + ccPepFragCount, ccPeptideFragmentFoundFrags.get(cp).size());
                    int doublets = 0;                    
                    for (Map.Entry<Integer, HashSet<Fragment>> chargeEntries : ccPeptideFragmentChargeFoundFrags.get(cp).entrySet()) {
                        if (chargeEntries.getValue().size()>1)
                            doublets++;
                    }
                    addScore(match, peptide + (p + 1) + " " + ccPepDoubletCount, doublets);
                    wholeCCPepDoubletCount += doublets;
                    addScore(match, peptide + (p + 1) + " " + ccPepDoubletFound, doublets>0?1:0);
                    wholeCCPepDoubletFound += doublets>0?1:0;
                } else {
                    addScore(match, peptide + (p + 1) + " " + ccPepFrag, 0);
                    addScore(match, peptide + (p + 1) + " " + ccPepFragIntens, 0);
                    addScore(match, peptide + (p + 1) + " " + ccPepFragError, Double.NaN);
                    addScore(match, peptide + (p + 1) + " " + ccPepFragCount, 0);
                    addScore(match, peptide + (p + 1) + " " + ccPepDoubletCount, 0);
                    addScore(match, peptide + (p + 1) + " " + ccPepDoubletFound, 0);
                }

            } else {
                addScore(match, peptide + (p + 1) + " " + mAll, 0);
                addScore(match, peptide + (p + 1) + " " + mAllLossy, 0);
                addScore(match, peptide + (p + 1) + " " + m, 0);
                addScore(match, peptide + (p + 1) + " " + mp, 0);
                addScore(match, peptide + (p + 1) + " " + mNL, 0);
                addScore(match, peptide + (p + 1) + " " + mNLp, 0);
                addScore(match, peptide + (p + 1) + " " + mL, 0);
                addScore(match, peptide + (p + 1) + " " + mLp, 0);
                addScore(match, peptide + (p + 1) + " " + mC, 0);
                addScore(match, peptide + (p + 1) + " " + mCp, 0);
                addScore(match, peptide + (p + 1) + " " + mmp, 0);
                addScore(match, peptide + (p + 1) + " " + mpU, 0);
                addScore(match, peptide + (p + 1) + " " + mpUNL, 0);
                addScore(match, peptide + (p + 1) + " " + mpUNLc, 0);
                addScore(match, peptide + (p + 1) + " " + mpUL, 0);
                addScore(match, peptide + (p + 1) + " " + mpULc, 0);
                addScore(match, peptide + (p + 1) + " " + mpUC, 0);
                addScore(match, peptide + (p + 1) + " " + mpUCp, 0);
        
                addScore(match, peptide + (p + 1) + " " + mpUxl, 0);
                addScore(match, peptide + (p + 1) + " " + mpUNLxl, 0);
                addScore(match, peptide + (p + 1) + " " + mpUNLxlc, 0);
                addScore(match, peptide + (p + 1) + " " + mpULxl, 0);
                addScore(match, peptide + (p + 1) + " " + mpULxlc, 0);
                addScore(match, peptide + (p + 1) + " " + mpUCxl, 0);
                addScore(match, peptide + (p + 1) + " " + mpUCxlp, 0);
        
                addScore(match, peptide + (p + 1) + " " + stc, 0);
                addScore(match, peptide + (p + 1) + " " + ccPepFrag, 0);
                addScore(match, peptide + (p + 1) + " " + ccPepFragError, Double.NaN);
                addScore(match, peptide + (p + 1) + " " + ccPepFragIntens, 0);
                addScore(match, peptide + (p + 1) + " " + ccPepFragCount, 0);
                addScore(match, peptide + (p + 1) + " " + ccPepDoubletCount, 0);
                addScore(match, peptide + (p + 1) + " " + ccPepDoubletFound, 0);
            }

        }

        //all = fragmentsMatched + unmatched;
        addScore(match, mAll, fragmentsMatchesNonLossy);
        addScore(match, mAllLossy, fragmentsMatchesLossy);
        addScore(match, whole + m, matchAll.count[0]);
        addScore(match, whole + mp, matchAll.count[0] / matchPossible);
        addScore(match, whole + mNL, matchAll.countNonLossy[0]);
        addScore(match, whole + mNLp, matchAll.countNonLossy[0] / matchPossible);
        addScore(match, whole + mL, matchAll.countLossy[0]);
        addScore(match, whole + mLp, matchAll.countLossy[0] / matchPossible);
        addScore(match, whole + mC, matchCons.count[0]);
        addScore(match, whole + mCp, matchCons.count[0] / matchPossible);
        addScore(match, whole + mmp, matchMulti / matchPossible);
        addScore(match, whole + mpU, matchAll.countPrimary[0]);
        addScore(match, whole + mpUNL, matchAll.countNonLossyPrimary[0]);
        addScore(match, whole + mpUNLc, matchAll.countNonLossyPrimary[0] / matchPossible);
        addScore(match, whole + mpUL, matchAll.countLossyPrimary[0]);
        addScore(match, whole + mpULc, matchAll.countLossyPrimary[0] / matchPossible);
        addScore(match, whole + mpUC, matchCons.countPrimary[0]);
        addScore(match, whole + mpUCp, matchCons.countPrimary[0] / matchPossible);
        
        addScore(match, whole + mpUxl, matchAll.countPrimaryXL[0]);
        addScore(match, whole + mpUNLxl, matchAll.countNonLossyPrimaryXL[0]);
        addScore(match, whole + mpUNLxlc, matchAll.countNonLossyPrimaryXL[0] / matchPossible);
        addScore(match, whole + mpULxl, matchAll.countLossyPrimaryXL[0]);
        addScore(match, whole + mpULxlc, matchAll.countLossyPrimaryXL[0] / matchPossible);
        addScore(match, whole + mpUCxl, matchCons.countPrimaryXL[0]);
        addScore(match, whole + mpUCxlp, matchCons.countPrimaryXL[0] / matchPossible);
        
        addScore(match, whole + stc, matchTag.count[0] / matchPossible);
        addScore(match, whole + ccPepFrag, wholeCCPepFrag);
        addScore(match, whole + ccPepFragError, wholeCCPepFragError);
        addScore(match, whole + ccPepFragIntens, wholeCCPepFragIntens);
        addScore(match, whole + ccPepFragCount, wholeCCPepFragCount);
        addScore(match, whole + ccPepDoubletCount, wholeCCPepDoubletCount);
        addScore(match, whole + ccPepDoubletFound, wholeCCPepDoubletFound);

        return all;

    }

    @Override
    public String[] scoreNames() {
        ArrayList<String> scoreNames = new ArrayList<String>();
        scoreNames.add(mAll);
        scoreNames.add(mAllLossy);
        scoreNames.add(whole + m);
//        scoreNames.add(mVSu);
//        addScore(match, mVSu, fragmentsMatched/(double)unmatched);
        scoreNames.add(whole + mp);
        scoreNames.add(whole + mNL);
        scoreNames.add(whole + mNLp);
        scoreNames.add(whole + mL);
        scoreNames.add(whole + mLp);
        scoreNames.add(whole + mC);
        scoreNames.add(whole + mCp);
        scoreNames.add(whole + mpU);
        scoreNames.add(whole + mpUNL);
        scoreNames.add(whole + mpUNLc);
        scoreNames.add(whole + mpUL);
        scoreNames.add(whole + mpULc);
        scoreNames.add(whole + mpUC);
        
        scoreNames.add(whole + mpUxl);
        scoreNames.add(whole + mpUNLxl);
        scoreNames.add(whole + mpUNLxlc);
        scoreNames.add(whole + mpULxl);
        scoreNames.add(whole + mpULxlc);
        scoreNames.add(whole + mpUCxl);
        
        scoreNames.add(whole + mmp);
        scoreNames.add(whole + stc);
        scoreNames.add(whole + mpUCp);
        scoreNames.add(whole + ccPepFrag);
        scoreNames.add(whole + ccPepFragError);
        scoreNames.add(whole + ccPepFragCount);
        scoreNames.add(whole + ccPepFragIntens);
        scoreNames.add(whole + ccPepDoubletCount);
        scoreNames.add(whole + ccPepDoubletFound);

        for (int pepID = 1; pepID <= 2; pepID++) {
            scoreNames.add(peptide + pepID + " " + mAll);
            scoreNames.add(peptide + pepID + " " + mAllLossy);
            scoreNames.add(peptide + pepID + " " + m);
//            scoreNames.add(peptide + pepID + "_" + mVSu);
            scoreNames.add(peptide + pepID + " " + mp);
            scoreNames.add(peptide + pepID + " " + mNL);
            scoreNames.add(peptide + pepID + " " + mNLp);
            scoreNames.add(peptide + pepID + " " + mL);
            scoreNames.add(peptide + pepID + " " + mLp);
            scoreNames.add(peptide + pepID + " " + mC);
            scoreNames.add(peptide + pepID + " " + mCp);
            scoreNames.add(peptide + pepID + " " + mpU);
            scoreNames.add(peptide + pepID + " " + mpUNL);
            scoreNames.add(peptide + pepID + " " + mpUNLc);
            scoreNames.add(peptide + pepID + " " + mpUL);
            scoreNames.add(peptide + pepID + " " + mpULc);
            scoreNames.add(peptide + pepID + " " + mpUC);
            scoreNames.add(peptide + pepID + " " + mpUCp);
            scoreNames.add(peptide + pepID + " " + mpUxl);
            scoreNames.add(peptide + pepID + " " + mpUNLxl);
            scoreNames.add(peptide + pepID + " " + mpUNLxlc);
            scoreNames.add(peptide + pepID + " " + mpULxl);
            scoreNames.add(peptide + pepID + " " + mpULxlc);
            scoreNames.add(peptide + pepID + " " + mpUCxl);
            scoreNames.add(peptide + pepID + " " + mmp);
            scoreNames.add(peptide + pepID + " " + stc);
            scoreNames.add(peptide + pepID + " " + ccPepFrag);
            scoreNames.add(peptide + pepID + " " + ccPepFragError);
            scoreNames.add(peptide + pepID + " " + ccPepFragCount);
            scoreNames.add(peptide + pepID + " " + ccPepFragIntens);
            scoreNames.add(peptide + pepID + " " + ccPepDoubletCount);
            scoreNames.add(peptide + pepID + " " + ccPepDoubletFound);

        }

        return scoreNames.toArray(new String[0]);
    }

    public double getOrder() {
        return 10;
    }

}
