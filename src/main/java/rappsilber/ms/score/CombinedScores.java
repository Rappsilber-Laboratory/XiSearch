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

import java.util.HashMap;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 * currently hardcoded
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class CombinedScores  extends AbstractScoreSpectraMatch{
    public static final String pep1 = "Pep1Score";
    public static final String pep2 = "Pep2Score";
    public static final String matchscore = "MatchScore";
    public static final String spectrum = "spectrum quality score";
    public static final String all = "AllScore";
    public static final String allLib = "AllScoreLib";
    public static final String norm = "NormScore";

    public double score(MatchedXlinkedPeptide match) {
        double sum = 0;
        double weight = 0;
        HashMap<String,Double> scores = match.getScores();

        //<editor-fold desc="All">
        //all fragments
        double s = match.getScore("fragment " + FragmentCoverage.mCp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 1.5*s;
            weight += 1.5;
        }
        s = match.getScore("fragment " + FragmentCoverage.mp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 1*s;
            weight += 1;
        }
        s = match.getScore("fragment " + FragmentCoverage.mLp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 0.5*s;
            weight += 0.5;
        }
        s = match.getScore("fragment " + FragmentCoverage.mNLp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 2*s;
            weight += 2;
        }
        s = match.getScore("fragment " + FragmentCoverage.mp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 1*s;
            weight += 1;
        }
        // peptide 1 fragments
        s = match.getScore("peptide1 " + FragmentCoverage.mCp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 1.5*s;
            weight += 1.5;
        }
        s = match.getScore("peptide1 " + FragmentCoverage.mp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 1*s;
            weight += 1;
        }
        s = match.getScore("peptide1 " + FragmentCoverage.mLp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 0.5*s;
            weight += 0.5;
        }
        s = match.getScore("peptide1 " + FragmentCoverage.mNLp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 2*s;
            weight += 2;
        }
        s = match.getScore("peptide1 " + FragmentCoverage.mp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 1*s;
            weight += 1;
        }

        //peptide 2 fragments
        s = match.getScore("peptide2 " + FragmentCoverage.mCp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 1.5*s;
            weight += 1.5;
        }
        s = match.getScore("peptide2 " + FragmentCoverage.mp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 1*s;
            weight += 1;
        }
        s = match.getScore("peptide2 " + FragmentCoverage.mLp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 0.5*s;
            weight += 0.5;
        }
        s = match.getScore("peptide2 " + FragmentCoverage.mNLp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 2*s;
            weight += 2;
        }
        s = match.getScore("peptide2 " + FragmentCoverage.mp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 1*s;
            weight += 1;
        }
        // spectra
        s = match.getScore(SpectraCoverage.mp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 1*s;
            weight += 1;
        }
        s = match.getScore(SpectraCoverage.imp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 1.5*s;
            weight += 1.5;
        }
        s = match.getScore(SpectraCoverage.smp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 0.5*s;
            weight += 0.5;
        }
        s = match.getScore(SpectraCoverage.ip);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 1*s;
            weight += 1;
        }
        s = match.getScore(SpectraCoverageConservative.class.getSimpleName());
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 1*s;
            weight += 1;
        }

        s = match.getScore(FragmentLibraryScore.class.getSimpleName());
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            s=s/180;
            sum    += 0.5*s;
            weight += 0.5;
        }
        double ret = sum/weight;
        addScore(match,all, ret);

//        s = match.getScore(FragmentLibraryScore.AdaptedScore);
//        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
//            sum    += 3*s;
//            weight += 3;
//        }
        ret = sum/weight;
        addScore(match,allLib, ret);

        //</editor-fold>

        //<editor-fold desc="match">
        sum = 0;
        weight = 0;
        s = match.getScore(FragmentLibraryScore.class.getSimpleName());
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            s=s/180;
            sum    += 1*s;
            weight += 1;
        }
        s = match.getScore("peptide1 " + FragmentCoverage.mCp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 1*s;
            weight += 1;
        }
        s = match.getScore("peptide2 " + FragmentCoverage.mCp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 1.5*s;
            weight += 1.5;
        }
        s = match.getScore("fragment " + FragmentCoverage.mCp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 1.5*s;
            weight += 1.5;
        }
        s = match.getScore(SpectraCoverageConservative.class.getSimpleName());
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 2*s;
            weight += 2;
        }
        s = match.getScore(SpectraCoverage.imp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 0.5*s;
            weight += 0.5;
        }
        ret = sum/weight;
        addScore(match,matchscore, ret);
        //</editor-fold>

        //<editor-fold desc="spectrum">
        sum = 0;
        weight = 0;
        s = match.getScore(SpectraCoverageConservative.class.getSimpleName());
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 2*s;
            weight += 2;
        }
        s = match.getScore(SpectraCoverage.imp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 1.5*s;
            weight += 1.5;
        }
        s = match.getScore(SpectraCoverage.smp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 0.5*s;
            weight += 0.5;
        }
        s = match.getScore(SpectraCoverage.ip);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 1*s;
            weight += 1;
        }
        ret = sum/weight;
        addScore(match,spectrum, ret);
        //</editor-fold>

        //<editor-fold desc="peptide1">
        // peptide 1 fragments
        sum = 0;
        weight = 0;
        s = match.getScore("peptide1 " + FragmentCoverage.mCp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 2*s;
            weight += 2;
        }
        s = match.getScore("peptide1 " + FragmentCoverage.mp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 1*s;
            weight += 1;
        }
        s = match.getScore("peptide1 " + FragmentCoverage.mLp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 0.5*s;
            weight += 0.5;
        }
        s = match.getScore("peptide1 " + FragmentCoverage.mNLp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 2*s;
            weight += 2;
        }
        s = match.getScore("peptide1 " + FragmentCoverage.mp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 1*s;
            weight += 1;
        }
        ret = sum/weight;
        addScore(match,pep1, ret);
        //</editor-fold>

        //<editor-fold desc="peptide 2">
        // peptide 2 fragments
        sum = 0;
        weight = 0;
        s = match.getScore("peptide2 " + FragmentCoverage.mCp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 2*s;
            weight += 2;
        }
        s = match.getScore("peptide2 " + FragmentCoverage.mp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 1*s;
            weight += 1;
        }
        s = match.getScore("peptide2 " + FragmentCoverage.mLp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 0.5*s;
            weight += 0.5;
        }
        s = match.getScore("peptide2 " + FragmentCoverage.mNLp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 2*s;
            weight += 2;
        }
        s = match.getScore("peptide2 " + FragmentCoverage.mp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 1*s;
            weight += 1;
        }
        if (weight != 0) {
            ret = sum/weight;
            addScore(match,pep2, ret);
        }
        //</editor-fold>

        //<editor-fold desc="norm">
        //nonlossy%
        sum = 0;
        weight = 0;
        s = match.getScore("fragment " + FragmentCoverage.mLp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 2.7*(s/0.94);
            weight += 2.7;
        }

        s = match.getScore("fragment " + FragmentCoverage.mp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 2.5*(s/0.97);
            weight += 2.5;
        }

        s = match.getScore("fragment " + FragmentCoverage.mCp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 2.5*(s/0.97);
            weight += 2.5;
        }

        s = match.getScore("peptide2 " + FragmentCoverage.mNLp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 2*(s/0.9);
            weight += 2;
        }

        s = match.getScore("peptide1 " + FragmentCoverage.mNLp);
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 1.5*s;
            weight += 1.5;
        }

        s = match.getScore(SpectraCoverageConservative.class.getSimpleName());
        if (!(Double.isInfinite(s) || Double.isNaN(s))) {
            sum    += 1*s;
            weight += 1;
        }
        ret = sum/weight;
        addScore(match,norm, ret);

        //</editor-fold>

        return ret;

    }



    public double getOrder() {
        return 90000;
    }

    public String[] scoreNames() {
        return new String[]{ pep1,pep2,spectrum,all,allLib,matchscore,norm};
    }

    

}
