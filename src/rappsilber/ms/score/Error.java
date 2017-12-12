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
    public static final String  mAverageAbsolutePep1MS2 = "AverageMS2ErrorPeptide1";
    public static final String  mAverageAbsolutePep2MS2 = "AverageMS2ErrorPeptide2";
    public static final String  mAverageAbsoluteXLMS2 = "AverageMS2ErrorCrossLinked";
    public static final String  mMeanSquareError = "MeanSquareError";
    public static final String  mMeanSquareRootError = "MeanSquareRootError";
    public static final String  mAverageAbsoluteRelativeMS2 = "AverageRelativeMS2Error";
    public static final String  mAverageInvertedAbsoluteRelativeMS2 = "Average1-RelativeMS2Error";
    public static final double NOMATCH_ERROR =9999999;

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
        ArrayList<Double> errorsPep1 = new ArrayList<Double>(s.getPeaks().size()*3);
        ArrayList<Double> errorsPep2 = new ArrayList<Double>(s.getPeaks().size()*3);
        ArrayList<Double> errorsXL = new ArrayList<Double>(s.getPeaks().size()*3);
        ArrayList<Double> relativeErrors = new ArrayList<Double>(s.getPeaks().size()*3);
        for (SpectraPeak sp : match.getSpectrum()) {
            ArrayList<SpectraPeakMatchedFragment> amf =  AnnotationUtil.getReducedAnnotation(sp, mfc);
            for (SpectraPeakMatchedFragment mf : amf) {
                double peakMZ = sp.getMZ();
                double calcMZ = mf.getFragment().getMZ(mf.getCharge());
                if (mf.matchedMissing()) {
                    //how many peaks missing 
                    long m = Math.round(((peakMZ-calcMZ)*mf.getCharge())/Util.C13_MASS_DIFFERENCE);
                    peakMZ = peakMZ - m*Util.C13_MASS_DIFFERENCE/mf.getCharge();
                }
                
//                if (!mf.matchedMissing()) {
//                    double calcMZ = mf.getFragment().getMZ(mf.getCharge());
                double peakError = peakMZ - calcMZ;
                double maxPeakError = m_fracTolerance.getAbsoluteError(peakMZ);
                if (Math.abs(peakError) > maxPeakError){
                    System.err.println("missmatch?");
                }else {
                    relativeErrors.add(Math.abs(peakError)/maxPeakError);
                    peakError /= peakMZ;
                    peakError *= 1000000;
                    errors.add(peakError);
                    if (mf.isLinear()) {
                        if (mf.getFragment().getPeptide() == match.getPeptide1()) {
                            errorsPep1.add(peakError);
                        } else {
                            errorsPep2.add(peakError);
                        }
                    } else {
                        errorsXL.add(peakError);
                    }
                }
//                }
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
            addScore(match, mMeanSquareError, NOMATCH_ERROR);
            addScore(match, mAverageAbsoluteMS2, NOMATCH_ERROR);
            addScore(match, mAverageAbsoluteRelativeMS2, 2);
            addScore(match, mAverageInvertedAbsoluteRelativeMS2, -1);
            addScore(match, mMeanSquareRootError, Double.POSITIVE_INFINITY);
        }
        Double errorXL = null;
        Double errorPep1 = null;
        Double errorPep2 = null;
        if (errorsXL.size() > 0) {
            double average = 0;

            for (double e : errorsXL) {
                average += Math.abs(e);
            }
            errorXL=average/errorsXL.size();
            addScore(match, mAverageAbsoluteXLMS2, errorXL);
        }
        if (errorsPep1.size() > 0) {
            double average = 0;

            for (double e : errorsPep1) {
                average += Math.abs(e);
            }
            errorPep1=average/errorsPep1.size();
            addScore(match, mAverageAbsolutePep1MS2, errorPep1);
        } 
        if (errorsPep2.size() > 0) {
            double average = 0;

            for (double e : errorsPep2) {
                average += Math.abs(e);
            }
            errorPep2 =  average/errorsPep2.size();
            addScore(match, mAverageAbsolutePep2MS2, errorPep2);
        } 
        if (match.getPeptides().length==1) {
            if (errorPep1 == null)
                errorPep1 = NOMATCH_ERROR;
            errorXL = errorPep1;
            errorPep2=errorPep1;
            addScore(match, mAverageAbsoluteXLMS2, errorXL);
            addScore(match, mAverageAbsolutePep2MS2, errorPep2);
        } else {
            if (errorXL == null) {
                
                addScore(match, mAverageAbsoluteXLMS2, NOMATCH_ERROR);
                errorXL=NOMATCH_ERROR;
            } 
            // if we have no peaks for a indiviual peptide take the cross-linked error
            if (errorPep1 == null) {
                addScore(match, mAverageAbsolutePep1MS2, errorXL);
            }
            if (errorPep2 == null) {
                addScore(match, mAverageAbsolutePep2MS2, errorXL);
            }
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
        scoreNames.add(mAverageAbsolutePep1MS2);
        scoreNames.add(mAverageAbsolutePep2MS2);
        scoreNames.add(mAverageAbsoluteXLMS2);

        return scoreNames.toArray(new String[0]);
    }

    public double getOrder() {
        return 10;
    }

}
