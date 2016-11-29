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
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class FragmentCoverage extends AbstractScoreSpectraMatch{
    public static final String  mAll = "total fragment matches";
    public static final String  mAllLossy = "lossy fragment matches";
    public static final String  m = "matched";
//    public static final String  mVSu = "fragment fragmentsMatched/unmatched";
    public static final String  mp = "coverage";
    public static final String  mpb = "b-coverage";
    public static final String  mpy = "y-coverage";
    public static final String  mNL = "non lossy matched";
    public static final String  mNLp = "non lossy coverage";
    public static final String  mL = "lossy matched";
    public static final String  mLp = "lossy coverage";
    public static final String  mC = "matched conservative";
    public static final String  mCp = "conservative coverage";
    public static final String  mpU = "unique matched";
    public static final String  mpUNL = "unique matched non lossy";
    public static final String  mpUNLc = "unique matched non lossy coverage";
    public static final String  mpUL = "unique matched lossy";
    public static final String  mpULc = "unique matched lossy coverage";
    public static final String  mpUC = "unique matched conservative";
    public static final String  mpUCp = "unique matched conservative coverage";
    public static final String  mmp = "multimatched%";
    public static final String  stc = "sequencetag coverage%";
    
    private int minToConservative = 3;


    public static final int      MAX_PEPTIDES = 2;
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
        int maxfrag;

        public FragCounts(Peptide p) { 
            maxfrag=p.length()-1;
            count = new int[maxfrag];
            countPrimary = new int[maxfrag];
            countNonLossy = new int[maxfrag];
            countNonLossyPrimary = new int[maxfrag];
            countLossy = new int[maxfrag];
            countLossyPrimary = new int[maxfrag];
        }

        public FragCounts(int maxFrags) { 
            
            this.maxfrag=maxFrags;
            count = new int[maxfrag];
            countPrimary = new int[maxfrag];
            countNonLossy = new int[maxfrag];
            countNonLossyPrimary = new int[maxfrag];
            countLossy = new int[maxfrag];
            countLossyPrimary = new int[maxfrag];
            
        }
        
        public void add(SpectraPeakMatchedFragment mf) {
            Fragment f = mf.getFragment();
            int fs =  f.length()-1;
            count[fs]++;
            if (f.isClass(Loss.class)) {
                countLossy[fs]++;
                if (mf.isPrimary()) {
                    countLossyPrimary[fs]++;
                    countPrimary[fs]++;
                }
                
            } else {
                countNonLossy[fs]++;
                if (mf.isPrimary()) {
                    countNonLossyPrimary[fs]++;
                    countPrimary[fs]++;
                }
            }
        }
        
        public void add(FragCounts fc) { 
            for (int f =0 ; f<maxfrag; f++) {
                count[f] += fc.count[f];
                countPrimary[f] += fc.countPrimary[f];
                countNonLossy[f] += fc.countNonLossy[f];
                countLossy[f] += fc.countLossy[f];
                countNonLossyPrimary[f] += fc.countNonLossyPrimary[f];
                countLossyPrimary[f] += fc.countLossyPrimary[f];
            }
        }

        public void add(FragCounts[] fcs) { 
            for (FragCounts fc :fcs) {
                add(fc);
            }
        }

        public FragCounts total() {
            FragCounts fc = new FragCounts(1);
            for (int f=0;f<maxfrag;f++) {
                fc.count[0]+=count[f];
                fc.countPrimary[0]+=countPrimary[f];
                fc.countNonLossy[0]+=countNonLossy[f];
                fc.countLossy[0]+=countLossy[f];
                fc.countNonLossyPrimary[0]+=countNonLossyPrimary[f];
                fc.countLossyPrimary[0]+=countLossyPrimary[f];
            }
            return fc;
        }
        
        //counts each site only ones
        public FragCounts totalMatched() {
            FragCounts fc = new FragCounts(1);
            for (int f=0;f<maxfrag;f++) {
                if (count[f]>0) {
                    fc.count[0]++;
                    if (countPrimary[f]>0) {
                        fc.countPrimary[0]++;
                    }
                    if (countNonLossy[f]>0) {
                        fc.countNonLossy[0]++;
                        if (countNonLossyPrimary[f]>0)
                            fc.countNonLossyPrimary[0]++;
                    }
                    if (countLossy[f] > 0 ) {
                        fc.countLossy[0]++;
                        if (countLossyPrimary[f]>0)
                            fc.countLossyPrimary[0]++;
                    }
                }
            }
            return fc;
        }
        
        public FragCounts conservative(int minLoss) {
            FragCounts fc = new FragCounts(maxfrag);
            for (int f=0;f<maxfrag;f++) {
                if (countNonLossy[f] >0) {
                    fc.count[f]=1;
                    fc.countNonLossy[f]=1;
                    if (countNonLossyPrimary[f] >0) {
                        fc.countPrimary[f]=1;
                    }
                } else if (countLossy[f]>=minLoss) {
                    fc.count[f]=1;
                    fc.countLossy[f]=1;
                    if (countLossyPrimary[f]>=minLoss) {
                        fc.countLossyPrimary[f]=1;
                    }
                }
            }
            return fc;
        }
        
        public FragCounts sequenceTag() {
                        
            FragCounts t = new FragCounts(maxfrag);
            
            if (maxfrag>3) {
                
                
                for (int f=1;f<maxfrag-1;f++) {
                    
                    if ((countNonLossy[f-1] > 0 &&
                         countNonLossy[f] > 0 &&
                        countNonLossy[f+1] > 0)) {
                        
                        t.count[f-1] = 1;
                        t.count[f] = 1;
                        t.count[f+1] = 1;
                        
                    }
                    
                }
                
            }
            
            return t;
        }
        
    }
    
    
    public double score(MatchedXlinkedPeptide match) {
        MatchedFragmentCollection mfc = match.getMatchedFragments();
        
        int fragmentsMatched = 0;
        int fragmentsMatchesNonLossy = 0;
        int fragmentsMatchesLossy = 0;
        int uniqueMatched = 0;
        int uniqueMatches = 0;
        int uniqueMatchesLossy = 0;
        int uniqueMatchesNonLossy = 0;
//        int all = 0;
        int LossyMatched = 0;    // count matches to lossy fragmnets
        int NonLossyMatched = 0; // count matches to non-lossy fragments
        int MultiMatched = 0; // count matches to non-lossy fragments
        int ConservativeMatched = 0; // count matches with either non lossy or n supporting lossy fragments
        int uniqueConservativeMatched = 0; // count matches with either non lossy or n supporting lossy fragments

        int pepID = 0;
        //for (Peptide p : peptideFragments.keySet()) {
        Peptide[] peps = match.getPeptides();
        int pepsCount = peps.length;
        
        HashMap<Peptide,Integer> pepIds = new HashMap<Peptide, Integer>(pepsCount);

        int precCharge = match.getSpectrum().getPrecurserCharge();
        
        FragCounts[][] nTerminalFrags = new FragCounts[pepsCount][precCharge + 1];
        FragCounts[][] cTerminalFrags = new FragCounts[pepsCount][precCharge + 1];
        FragCounts[] Frags = new FragCounts[pepsCount];
        
        

        int all = 0;
        for (int i=0; i < pepsCount; i++) {
            nTerminalFrags[i] = new FragCounts[precCharge + 1];
            cTerminalFrags[i] = new FragCounts[precCharge + 1];
            int maxfrag = peps[i].length()-1;
            pepIds.put(peps[i],i);
            
            for (int c = 1; c<=precCharge; c++)  {
                nTerminalFrags[i][c]=new FragCounts(peps[i]);
                cTerminalFrags[i][c]=new FragCounts(peps[i]);
            }
        }

        // collect all matches
        for (SpectraPeakMatchedFragment mf : match.getSpectraPeakMatchedFragment()) {
            Fragment f = mf.getFragment();
            // ignore everything, that is not basic fragment (y or b-ions or losses of theese but e.g. no double fragmentation)
            if (!f.isBasicFragmentation())
                continue;
            
            int mc = mf.getCharge();
            if (f.isClass(Loss.class))
                fragmentsMatchesLossy++;
            else 
                fragmentsMatchesNonLossy++;
            
            
            
           
            int pep = pepIds.get(f.getPeptide());
            int l = f.length();
            boolean isNterminal = f.isNTerminal();
            boolean isCTerminal = f.isCTerminal();
            
            if (isNterminal) {
                if (nTerminalFrags[pep] == null || nTerminalFrags[pep][mc] == null)
                    System.err.println("Something strange is here n ");
                nTerminalFrags[pep][mc].add(mf);
            } if (isCTerminal) {
                if (cTerminalFrags[pep] == null || cTerminalFrags[pep][mc] == null)
                    System.err.println("Something strange is here c ");
                cTerminalFrags[pep][mc].add(mf);
            }
        }
        
        // do some summaries
        FragCounts matchCons = new FragCounts(1);
        FragCounts matchAll = new FragCounts(1);
        FragCounts matchTag = new FragCounts(1);
        int matchMulti =0;
        double matchPossible=0;

        for (int p = 0; p < pepsCount;p++) {
            // how many unique n or cterminal fragmenst do we expect
            // here x,  y and z are seen as the same and a,b and c as well. 
            int maxTermFrags = peps[p].length() -1;
            
            if (maxTermFrags>0) {
                double pepAll = maxTermFrags*2;
                matchPossible += pepAll;
                FragCounts fnCons = new FragCounts(peps[p]);
                FragCounts fcCons = new FragCounts(peps[p]);
                FragCounts fnAll = new FragCounts(peps[p]);
                FragCounts fcAll = new FragCounts(peps[p]);
                
                for (int c = 1; c<=precCharge; c++)  {
                    
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
                fnAll= fnAll.total();
                fnAll.add(fcAll.total());
                int pepMulti = 0;
                for (int c =0; c< fcCons.count.length; c++) {
                    if ( fnCons.count[c]>1)
                        pepMulti ++;                
                    if ( fcCons.count[c]>1)
                        pepMulti ++;                
                }
                
                matchMulti+=pepMulti;
                matchAll.add(pepUniqueFragMatches);
                matchCons.add(consTotal);
                matchTag.add(tag);
                
                // setup peptide scores
                addScore(match, "peptide" + (p + 1) + " " + m, pepUniqueFragMatches.count[0]);
                addScore(match, "peptide" + (p + 1) + " " + mp, pepUniqueFragMatches.count[0]/pepAll);
                addScore(match, "peptide" + (p + 1) + " " + mNL, pepUniqueFragMatches.countNonLossy[0]);
                addScore(match, "peptide" + (p + 1) + " " + mNLp, pepUniqueFragMatches.countNonLossy[0]/pepAll);
                addScore(match, "peptide" + (p + 1) + " " + mL, pepUniqueFragMatches.countLossy[0]);
                addScore(match, "peptide" + (p + 1) + " " + mLp, pepUniqueFragMatches.countLossy[0]/pepAll);
                addScore(match, "peptide" + (p + 1) + " " + mC, consTotal.count[0]);
                addScore(match, "peptide" + (p + 1) + " " + mCp, consTotal.count[0]/pepAll);
                addScore(match, "peptide" + (p + 1) + " " + mmp, pepMulti/pepAll);
                addScore(match, "peptide" + (p + 1) + " " + mpU, pepUniqueFragMatches.countPrimary[0]);
                addScore(match, "peptide" + (p + 1) + " " + mpUNL, pepUniqueFragMatches.countNonLossy[0]);
                addScore(match, "peptide" + (p + 1) + " " + mpUNLc, pepUniqueFragMatches.countNonLossy[0]/pepAll);
                addScore(match, "peptide" + (p + 1) + " " + mpUL, pepUniqueFragMatches.countLossy[0]);
                addScore(match, "peptide" + (p + 1) + " " + mpULc, pepUniqueFragMatches.countLossy[0]/pepAll);
                addScore(match, "peptide" + (p + 1) + " " + mpUC, consTotal.countPrimary[0]);
                addScore(match, "peptide" + (p + 1) + " " + mpUCp, consTotal.countPrimary[0]/pepAll);                
                addScore(match, "peptide" + (p + 1) + " " + stc, tag.count[0]/pepAll);
                
            } else {
                addScore(match, "peptide" + (p + 1) + " " + m,  0);
                addScore(match, "peptide" + (p + 1) + " " + mp,  0);
                addScore(match, "peptide" + (p + 1) + " " + mNL,  0);
                addScore(match, "peptide" + (p + 1) + " " + mNLp,  0);
                addScore(match, "peptide" + (p + 1) + " " + mL,  0);
                addScore(match, "peptide" + (p + 1) + " " + mLp,  0);
                addScore(match, "peptide" + (p + 1) + " " + mC,  0);
                addScore(match, "peptide" + (p + 1) + " " + mCp,  0);
                addScore(match, "peptide" + (p + 1) + " " + mmp,  0);
                addScore(match, "peptide" + (p + 1) + " " + mpU,  0);
                addScore(match, "peptide" + (p + 1) + " " + mpUNL,  0);
                addScore(match, "peptide" + (p + 1) + " " + mpUNLc,  0);
                addScore(match, "peptide" + (p + 1) + " " + mpUL,  0);
                addScore(match, "peptide" + (p + 1) + " " + mpULc,  0);
                addScore(match, "peptide" + (p + 1) + " " + mpUC,  0);
                addScore(match, "peptide" + (p + 1) + " " + mpUCp,  0);                
                addScore(match, "peptide" + (p + 1) + " " + stc, 0);
                
            }
            
        }

        //all = fragmentsMatched + unmatched;
        addScore(match, mAll, fragmentsMatchesNonLossy);
        addScore(match, mAllLossy, fragmentsMatchesLossy);
        addScore(match, "fragment"  + " " + m, matchAll.count[0]);
        addScore(match, "fragment"  + " " + mp, matchAll.count[0]/matchPossible);
        addScore(match, "fragment"  + " " + mNL, matchAll.countNonLossy[0]);
        addScore(match, "fragment"  + " " + mNLp, matchAll.countNonLossy[0]/matchPossible);
        addScore(match, "fragment"  + " " + mL, matchAll.countLossy[0]);
        addScore(match, "fragment"  + " " + mLp, matchAll.countLossy[0]/matchPossible);
        addScore(match, "fragment"  + " " + mC, matchCons.count[0]);
        addScore(match, "fragment"  + " " + mCp, matchCons.count[0]/matchPossible);
        addScore(match, "fragment"  + " " + mmp, matchMulti/matchPossible);
        addScore(match, "fragment"  + " " + mpU, matchAll.countPrimary[0]);
        addScore(match, "fragment"  + " " + mpUNL, matchAll.countNonLossy[0]);
        addScore(match, "fragment"  + " " + mpUNLc, matchAll.countNonLossy[0]/matchPossible);
        addScore(match, "fragment"  + " " + mpUL, matchAll.countLossy[0]);
        addScore(match, "fragment"  + " " + mpULc, matchAll.countLossy[0]/matchPossible);
        addScore(match, "fragment"  + " " + mpUC, matchCons.countPrimary[0]);
        addScore(match, "fragment"  + " " + mpUCp, matchCons.countPrimary[0]/matchPossible);                
        addScore(match, "fragment"  + " " + stc, matchTag.count[0]/matchPossible);

        return all;

    }
    
    @Override
    public String[] scoreNames() {
        ArrayList<String> scoreNames = new ArrayList<String>();
        scoreNames.add(mAll);
        scoreNames.add(mAllLossy);
        scoreNames.add("fragment " + m);
//        scoreNames.add(mVSu);
//        addScore(match, mVSu, fragmentsMatched/(double)unmatched);
        scoreNames.add( "fragment " + mp);
        scoreNames.add( "fragment " + mNL);
        scoreNames.add( "fragment " + mNLp);
        scoreNames.add( "fragment " + mL);
        scoreNames.add( "fragment " + mLp);
        scoreNames.add( "fragment " + mC);
        scoreNames.add( "fragment " + mCp);
        scoreNames.add( "fragment " + mpU);
        scoreNames.add( "fragment " + mpUNL);
        scoreNames.add( "fragment " + mpUNLc);
        scoreNames.add( "fragment " + mpUL);
        scoreNames.add( "fragment " + mpULc);
        scoreNames.add( "fragment " + mpUC);
        scoreNames.add( "fragment " + mpUCp);
        scoreNames.add( "fragment " + mmp);
        scoreNames.add( "fragment " + stc);

        for (int pepID = 1; pepID <= 2; pepID++) {
            scoreNames.add("peptide" + pepID + " " + m);
//            scoreNames.add("peptide" + pepID + "_" + mVSu);
            scoreNames.add("peptide" + pepID + " " + mp);
            scoreNames.add("peptide" + pepID + " " + mNL);
            scoreNames.add("peptide" + pepID + " " + mNLp);
            scoreNames.add("peptide" + pepID + " " + mL);
            scoreNames.add("peptide" + pepID + " " + mLp);
            scoreNames.add("peptide" + pepID + " " + mC);
            scoreNames.add("peptide" + pepID + " " + mCp);
            scoreNames.add("peptide" + pepID + " " + mpU);
            scoreNames.add("peptide" + pepID + " " + mpUNL);
            scoreNames.add("peptide" + pepID + " " + mpUNLc);
            scoreNames.add("peptide" + pepID + " " + mpUL);
            scoreNames.add("peptide" + pepID + " " + mpULc);
            scoreNames.add("peptide" + pepID + " " + mpUC);
            scoreNames.add("peptide" + pepID + " " + mpUCp);
            scoreNames.add("peptide" + pepID + " " + mmp);
            scoreNames.add("peptide" + pepID + " " + stc);

        }

        return scoreNames.toArray(new String[0]);
    }

    public double getOrder() {
        return 10;
    }

}
