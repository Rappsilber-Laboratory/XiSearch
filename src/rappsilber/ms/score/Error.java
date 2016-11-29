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
import rappsilber.config.RunConfig;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.annotation.AnnotationUtil;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class Error extends AbstractScoreSpectraMatch{
    public static final String  mPrecoursor = "Precoursor Error";
    public static final String  mPrecoursorAbsolute = "Precoursor Absolute Error";
    public static final String  mPrecoursorAbsoluteRelative = "PrecoursorAbsoluteErrorRelative";
    public static final String  mPrecoursorAbsoluteRelativeInverted = "1-ErrorRelative";
    public static final String  mAverageAbsoluteMS2 = "AverageMS2Error";
    public static final String  mMeanSquareError = "MeanSquareError";
    public static final String  mMeanSquareRootError = "MeanSquareRootError";
    public static final String  mAverageAbsoluteRelativeMS2 = "AverageRelativeMS2Error";
    public static final String  mAverageInvertedAbsoluteRelativeMS2 = "Average1-RelativeMS2Error";

    public static final int      MAX_PEPTIDES = 2;
    private RunConfig   m_config;
    private ToleranceUnit m_precTolerance;
    private ToleranceUnit m_fracTolerance;

    public Error(RunConfig config) {
        this.m_config = config;
        m_precTolerance = m_config.getPrecousorTolerance();
        m_fracTolerance = m_config.getFragmentTolerance();
    }




    public double score(MatchedXlinkedPeptide match) {
        Spectra s = match.getSpectrum();
        MatchedFragmentCollection mfc = match.getMatchedFragments();
        double calcPrecMZ = 0;
        if (match.getCrosslinker() != null)
            calcPrecMZ+=match.getCrosslinker().getCrossLinkedMass();
        
        for (Peptide p : match.getPeptides())
            calcPrecMZ += p.getMass();
        
        calcPrecMZ = calcPrecMZ / ((double)s.getPrecurserCharge()) + Util.PROTON_MASS;
        double precError = calcPrecMZ - s.getPrecurserMZ();
        double precPPMError = precError / s.getPrecurserMZ();
        precPPMError *= 1000000;
        double maxPrecError = m_precTolerance.getAbsoluteError(s.getPrecurserMZ());
        if (Math.abs(precError) > maxPrecError) {
            System.err.println("missmatch?");
        }

        addScore(match, mPrecoursor, precPPMError);
        addScore(match, mPrecoursorAbsolute, Math.abs(precPPMError));
        addScore(match, mPrecoursorAbsoluteRelative, Math.abs(precError)/maxPrecError);
        addScore(match, mPrecoursorAbsoluteRelativeInverted, 1 - Math.abs(precError)/maxPrecError);
                

        ArrayList<Double> errors = new ArrayList<Double>(s.getPeaks().size()*3);
        ArrayList<Double> relativeErrors = new ArrayList<Double>(s.getPeaks().size()*3);
        for (SpectraPeak sp : match.getSpectrum()) {
            ArrayList<SpectraPeakMatchedFragment> amf =  AnnotationUtil.getReducedAnnotation(sp, mfc);
            for (SpectraPeakMatchedFragment mf : amf) {
                if (!mf.matchedMissing()) {
                    double calcMZ = mf.getFragment().getMZ(mf.getCharge());
                    double peakError = sp.getMZ() - calcMZ;
                    double maxPeakError = m_fracTolerance.getAbsoluteError(sp.getMZ());
                    if (Math.abs(peakError) > maxPeakError){
                        System.err.println("missmatch?");
                    }else {
                        relativeErrors.add(Math.abs(peakError)/maxPeakError);
                        peakError /= sp.getMZ();
                        peakError *= 1000000;
                        errors.add(peakError);
                    }
                }
            }
        }
        double mse = -1;
        if (errors.size() > 0) {
            double average = 0;
            double averageRelative = 0;
            double sqr = 0;

            for (double e : errors) {
                average += Math.abs(e);
                sqr += e*e;
            }
            for (double e : relativeErrors) {
                averageRelative += e;
            }
            mse = sqr/errors.size();

            addScore(match, mMeanSquareError, mse);
            addScore(match, mAverageAbsoluteMS2, average/errors.size());
            addScore(match, mAverageAbsoluteRelativeMS2, averageRelative/relativeErrors.size());
            addScore(match, mAverageInvertedAbsoluteRelativeMS2, (relativeErrors.size() - averageRelative)/relativeErrors.size());
            addScore(match, mMeanSquareRootError, Math.sqrt(mse));
        } else {
            addScore(match, mMeanSquareError, 9999999);
            addScore(match, mAverageAbsoluteMS2, 9999999);
            addScore(match, mAverageAbsoluteRelativeMS2, 2);
            addScore(match, mAverageInvertedAbsoluteRelativeMS2, -1);
            addScore(match, mMeanSquareRootError, Double.POSITIVE_INFINITY);
        }

        return mse;

    }

    @Override
    public String[] scoreNames() {
        ArrayList<String> scoreNames = new ArrayList<String>();
        scoreNames.add(mPrecoursor);
        scoreNames.add(mPrecoursorAbsolute);
        scoreNames.add(mPrecoursorAbsoluteRelative);
        scoreNames.add(mPrecoursorAbsoluteRelativeInverted);
        scoreNames.add(mAverageAbsoluteMS2);
        scoreNames.add(mMeanSquareError);
        scoreNames.add(mMeanSquareRootError);
        scoreNames.add(mAverageAbsoluteRelativeMS2);
        scoreNames.add(mAverageInvertedAbsoluteRelativeMS2);

        return scoreNames.toArray(new String[0]);
    }

    public double getOrder() {
        return 10;
    }

}
