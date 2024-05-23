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

import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 * Tries to emulate the manual validation made by angel
 *
 * Things to consider:
 *  - Spectra coverage
 *  - coverage of alpha peptide
 *  - coverage of beta peptide
 *
 * Cases:
 *  case one A) good spectra alpha and beta coverage
 *    - good spectra coverage
 *       - top peaks covered (top 2(?) or 5 out of top 10?)
 *       - overall Coverage: at least 50% total intensity explained
 *    - good alpha peptide coverage
 *       - at least 70% explained and at least 8 matches
 *    - reasonable beta coverage
 *       - 4 unique matches
 *       - or 80% coverage
 *  case two A) very good spectra coverage reasonable alpha and beta coverage
 *    - good spectra coverage
 *       - top peaks covered (top 2(?) or 5 out of top 10?)
 *       - overall Coverage: at least 80% total intensity explained
 *    - good alpha peptide coverage
 *       - at least 40% explained and at least 8 matches
 *    - reasonable beta coverage
 *       - 3 unique matches
 *       - 60% coverage
 *  case two B) good spectra and alpha coverage, reasonable beta coverage
 *    - good spectra coverage
 *       - top peaks covered (top 2(?) or 5 out of top 10?)
 *       - overall Coverage: at least 50% total intensity explained
 *    - good alpha peptide coverage
 *       - at least 70% explained and at least 8 matches
 *    - reasonable beta coverage
 *       - 2 unique matches
 *       - 30% coverage
 *  case two C) good spectra and alpha coverage, low beta coverage
 *    - good spectra coverage
 *       - top peaks covered (top 2(?) or 5 out of top 10?)
 *       - overall Coverage: at least 50% total intensity explained
 *    - good alpha peptide coverage
 *       - at least 70% explained and at least 8 matches
 *    - reasonable beta coverage
 *       - 1 unique match
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class PassesManualValidation extends AbstractScoreSpectraMatch{
    private class Validation {
        double pepACoverage;
        double PepAMinPeaks;
        double pepBcoverage;
        double PepBMinPeaks;
        double spectraCoverage;
        double spectraTop10;
        double spectraTop5;
        double Score;

        public Validation(double pepACoverage, double PepAMinPeaks, double pepBcoverage, double PepBMinPeaks, double spectraCoverage, double spectraTop10, double spectraTop5, double Score) {
            this.pepACoverage = pepACoverage;
            this.PepAMinPeaks = PepAMinPeaks;
            this.pepBcoverage = pepBcoverage;
            this.PepBMinPeaks = PepBMinPeaks;
            this.spectraCoverage = spectraCoverage;
            this.spectraTop10 = spectraTop10;
            this.spectraTop5 = spectraTop5;
            this.Score = Score;
        }

        

    }

    Validation[] checkValidation = new Validation[]{
        new Validation(0.7, 8, 0.7, 4, 0.5, 6, 3, 1),
        new Validation(0.4, -1, 0.6, 3, 0.8, 8, 4, 9),
        new Validation(0.7, 8, 0.3, 2, 0.5, 6, 3, 0.5),
        new Validation(0.7, 8, 0.0, 1, 0.5, 6, 3, 0.2),
    };


    @Override
    public double score(MatchedXlinkedPeptide match) {
        Spectra s = match.getSpectrum();
        double score = 0;


        // make sure we are talking about unique peptides
        for (Peptide p : match.getPeptides()) {
            if (p.getProteinCount() > 1) {
                super.addScore(match, name(), 0);
                return 0;
            }
        }


        if (match.getScore("fragment matched conservative%") >= 0.306287031 &&
            (match.getScore("peptide1 coverage") >= 0.4285714286 || match.getScore("peptide2 coverage") >= 0.4285714286 ) &&
            (match.getScore("peptide1_non lossy matched%") >= 0.3333333333 || match.getScore("peptide2_non lossy matched%") >= 0.3333333333) &&
            (match.getScore("peptide2_fragment unique matched conservative%") >= 0.3181818182 || match.getScore("peptide2_fragment unique matched conservative%") >= 0.3181818182) &&
            (match.getScore("peptide1 coverage") >= 0.3846153846 && match.getScore("peptide2 coverage") >= 0.3846153846 ) &&
            (match.getScore("peptide1_non lossy matched%") >= 0.25 && match.getScore("peptide2_non lossy matched%") >= 0.25) &&
            (match.getScore("peptide2_fragment unique matched conservative%") >= 0.2352941176 && match.getScore("peptide2_fragment unique matched conservative%") >= 0.2352941176) &&
            (match.getScore("spectra intensity nonlossy coverage") >= 0.4789530623)) {
            super.addScore(match, name(), 1);
            score = 1;
        } else {
            super.addScore(match, name(), 0);
        }

        if (match.getScore("mgcDelta")>=2.8873868224016 &&
                match.getScore("mgcShiftedDelta")>=46.3364285803362 &&
                match.getScore("mgcAlpha")>27.289526437529 &&
                match.getScore("mgcBeta")>11.8965915736964 &&
                match.getScore("mgxScore")>=69.9744434956799 &&
                match.getScore("mgxDelta")>=8.28329532666743 &&
                match.getScore("1-ErrorRelative")>=0.467323348955666 &&
                match.getScore("Average1-RelativeMS2Error")>=0.658081855283972 &&
                match.getScore("total fragment matches")>=12 &&
                match.getScore("fragment coverage")>=0.290218758545256 &&
                match.getScore("fragment non lossy coverage")>=0.203754679978606 &&
                match.getScore("fragment matched conservative")>=11 &&
                match.getScore("fragment conservative coverage")>=0.21169014084507 &&
                match.getScore("fragment unique matched")>=12 &&
                match.getScore("fragment unique matched conservative coverage")>=0.21169014084507 &&
                match.getScore("fragment multimatched%")>=0 &&
                match.getScore("peptide1 coverage")>=0.144069264069264 &&
                match.getScore("peptide1 non lossy matched")>=3 &&
                match.getScore("peptide1 non lossy coverage")>=0.0891717171717172 &&
                match.getScore("peptide1 conservative coverage")>=0.0909090909090909 &&
                match.getScore("peptide1 unique matched non lossy coverage")>=0.0891717171717172 &&
                match.getScore("peptide1 unique matched conservative coverage")>=0.0891717171717172 &&
                (match.getScore("peptide1 matched")>=7 || match.getScore("peptide2 matched")>=7 )&&
                (match.getScore("peptide1 matched")>=4 & match.getScore("peptide2 matched")>=4 )&&
                match.getScore("peptide2 coverage")>=0.266760233918129 &&
                match.getScore("peptide2 non lossy coverage")>=0.191103448275862 &&
                match.getScore("peptide2 conservative coverage")>=0.192270114942529 &&
                match.getScore("peptide2 unique matched non lossy coverage")>=0.230961538461538 &&
                match.getScore("peptide2 unique matched conservative coverage")>=0.191103448275862 &&
                match.getScore("FragmentLibraryScoreLog")>=22.232979612601 &&
                match.getScore("spectrum intensity coverage")>=0.510098626031891 &&
                match.getScore("spectra intensity nonlossy coverage")>=0.314516762772543 &&
                match.getScore("spectra isotop%")>=0.6999609121508 &&
                match.getScore("spectra matched isotop%")>=0.543311322164814 &&
                match.getScore("spectra matched single%")>=0.187060724680967 &&
                match.getScore("spectra top10 matched%")>=0 &&
                match.getScore("spectra top20 matched%")>=0.15 &&
                match.getScore("spectra top40 matched%")>=0.225 &&
                match.getScore("spectra top100 matched%")>=0.2656 &&
                match.getScore("spectrum peaks coverage")>=0.284052515394446 &&
                match.getScore("SpectraCoverageConservative")>=0.465074688879871) {
            super.addScore(match, "MV", 1);
            score = 1;
        } else {
            super.addScore(match, "MV", 0);
        }




        if (match.getScore("fragment matched conservative%") >= 0.1442857143 &&
            (match.getScore("peptide1 coverage") >= 0.1635185185 && match.getScore("peptide2 coverage") >= 0.1635185185) &&
            (match.getScore("peptide1 coverage") >= 0.30 || match.getScore("peptide2 coverage") >= 30) &&
            (match.getScore("peptide2_non lossy matched") >= 4 && match.getScore("peptide2_non lossy matched") >= 4) &&
            match.getScore("SpectraCoverageConservative") >= 0.1317285889 &&
            match.getScore("spectrum peaks coverage") >= 0.1603773585 &&
            match.getScore("spectra top100 matched%") >= 0.13 &&
            match.getScore("spectra top40 matched%") >= 0.125 &&
            match.getScore("spectra top20 matched%") >= 0.05 &&
            match.getScore("spectra matched isotop%") >= 0.1515094345 &&
            match.getScore("spectra isotop%") >= 0.3787572839 &&
            match.getScore("spectra intensity nonlossy coverage") >= 0.1184469137
        ) {
            super.addScore(match, name() + "V2", 1);
            score = 1;
        } else {
            super.addScore(match, name() + "V2", 0);
        }

        return score;

    }


    @Override
    public String[] scoreNames() {
        return new String[]{ name(), name() + "V2", "MV"};
    }

    @Override
    public double getOrder() {
        Normalizer n = new Normalizer();
        return n.getOrder() - 1;
    }

}
