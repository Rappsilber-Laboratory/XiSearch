/*
 * Copyright 2018 Lutz Fischer <lfischer@staffmail.ed.ac.uk>.
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

import java.math.BigDecimal;
import java.math.MathContext;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.utils.Util;
import static rappsilber.utils.Util.factorial;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class AndromedaScore extends AbstractScoreSpectraMatch {

    public static double[] score(int matched_ions, int total_ions, ToleranceUnit msms_tol,
            int totalPeaks, double min, double max) {
        // need for cumulative score
        double[] score_and_p = new double[2];
        /* Parameter list:
		 * 1. 'matched_ions' = the number of ions that match within the MS2 spectrum
		 * 2. 'total_ions' = the total number of ions that fall within the range of
		 *    the masses in the MS2 spectrum
		 * 3. 'msms_tol' = the tolerance unit, used to caluculate the 'p' - the
		 *    probability of a random match.
         */
        int n = total_ions;
        //double double_n = total_ions;
        int k = matched_ions;
        //double double_k = matched_ions;
        // System.out.println("Total peaks = " + totalPeaks);

        // double p = 0.04");
// 		System.out.println("-------");
// 		System.out.println("hits = " + k);
// 		System.out.println("in range = " + n);
// 		System.out.println("total peaks = " + totalPeaks);
// 		System.out.println("min = " + min_max[0]);
// 		System.out.println("max = " + min_max[1]);
// 		System.out.println("-------");
        // Using tolerance window
        double p;
        if (msms_tol.getUnit() == "ppm") {
            // parts per million
            p = (double) (Util.ONE_PPM * msms_tol.getValue());
        } else {
            // we have a dalton unit
            p = (double) msms_tol.getValue();
        }
        p = p * 2;
        // old
        // p = p.multiply( new double(4) );
        p = p * totalPeaks;
        // p = p.divide( new double(100), Util.mc);
        p = p / (max - min);

        // need for Cumulative score
        score_and_p[1] = p;
// 		System.out.println("small p value = " + score_and_p[1]);
        // end use tolerance window

        double score = 0; // initialize the score
        //score = score.setScale(6, double.ROUND_HALF_UP);
        if (k == 0) {
            // if there are no matching ions the score is 0
            // return "";
            // need for cumulative score
            score_and_p[0] = Double.NaN;
        } // end if

        // .setScale(4, )
        /* Probability
		 * -----------
         */
        // Part 1: (n k) permutations
        try {
            double permutations;
            if (n>Util.LAST_DOUBLE_FACTORIAL||k>Util.LAST_DOUBLE_FACTORIAL){
                BigDecimal bdperm =  Util.factorialBig(n);
                BigDecimal divisor = Util.factorialBig(k);
                divisor = divisor.multiply(Util.factorialBig(n - k));
                permutations = bdperm.divide(divisor,MathContext.DECIMAL128).doubleValue();
            } else {
                // recursive component for probs
                permutations = factorial(n);
                double divisor = factorial(k);
                divisor = divisor * factorial(n - k);
                permutations = permutations / divisor;
            }

            // Part 2: p^k
            double part2 = (double) Math.pow(p, k);

            // Part 3: (1-p)^(n-k)
            double part3 = 1;
            part3 = part3 - p;
            part3 = (double) Math.pow(part3, n - k);

            double probability = (permutations * part2 * part3);
            //  end recursive component

            if (probability <= 0) {
                // we do not allow 0 or neagtive probability
                // return "0";
                score_and_p[0] = 0;
            } else {
                // log for score
                double log_probability = probability;
                score = (double) (-10d * Math.log10(log_probability));
                //score = Math.round(score*100.0d) / 100.0d;
                // System.out.println( "SCORE = "  + score.toString() );
                // return score.toString();
                score_and_p[0] = score;
            }
        } catch (IllegalArgumentException ie) {
            String error = "n = " + n + " k = " + k;
            System.err.println("Problem with factorial calculation: " + error);
            // return error;
            score_and_p[0] = Double.NaN;
        }

        //return "";
        return score_and_p;
    }// end method mannScore()    
    
    @Override
    public double score(MatchedXlinkedPeptide match) {
        Spectra s = match.getSpectrum();
        
        ToleranceUnit tu = match.getFragmentTolerance();
        int totalPeaks = match.getSpectrum().getPeaks().size();
        double min = s.getMinMz();
        double max = s.getMaxMz();

        // non lossy
        int matched_ions = (int) match.getScore(FragmentCoverage.mAll);
        int total_ions = match.getPossibleFragments().size(); 
        double[] score = score(matched_ions,total_ions,  tu, totalPeaks, min, max);
        matched_ions = (int) match.getScore(FragmentCoverage.peptide+"1 "+ FragmentCoverage.mAll);
        total_ions = match.getPossiblePeptideFragments().get(match.getPeptide1()).size(); 
        double[] peptide1Score = score(matched_ions,total_ions,  tu, totalPeaks, min, max);
        matched_ions = (int) match.getScore(FragmentCoverage.peptide+"2 "+ FragmentCoverage.mAll);
        total_ions = match.getPossiblePeptideFragments().get(match.getPeptide2()).size(); 
        double[] peptide2Score = score(matched_ions,total_ions,  tu, totalPeaks, min, max);
 
       matched_ions = (int) match.getScore(FragmentCoverage.mAllLossy);
        total_ions = match.getPossibleFragments().size(); 
        double[] scoreLossy = score(matched_ions,total_ions,  tu, totalPeaks, min, max);
        matched_ions = (int) match.getScore(FragmentCoverage.peptide+"1 "+ FragmentCoverage.mAllLossy);
        total_ions = match.getPossiblePeptideFragments().get(match.getPeptide1()).size(); 
        double[] peptide1ScoreLossy = score(matched_ions,total_ions,  tu, totalPeaks, min, max);
        matched_ions = (int) match.getScore(FragmentCoverage.peptide+"2 "+ FragmentCoverage.mAllLossy);
        total_ions = match.getPossiblePeptideFragments().get(match.getPeptide2()).size(); 
        double[] peptide2ScoreLossy = score(matched_ions,total_ions,  tu, totalPeaks, min, max);
        throw new UnsupportedOperationException("Not there yet");
        
    }// end method mannScore()    }

    @Override
    public double getOrder() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    
}
