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

import java.util.Collection;
import java.util.HashSet;
import rappsilber.ms.sequence.ions.SecondaryFragment;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.SpectraPeakCluster;
import rappsilber.ms.spectra.annotation.SpectraPeakAnnotation;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class SpectraCoverage extends AbstractScoreSpectraMatch {

//    public static final String mVSu = "spectra matched/unmatched";
    public static final String mp = "spectrum intensity coverage";
    public static final String mnlp = "spectra intensity nonlossy coverage";
    public static final String ip = "spectra isotop%";
    public static final String imp = "spectra matched isotop%";
    public static final String smp = "spectra matched single%";
    public static final String t10mp = "spectra top10 matched%";
    public static final String t20mp = "spectra top20 matched%";
    public static final String t40mp = "spectra top40 matched%";
    public static final String t100mp = "spectra top100 matched%";
    public static final String pmp = "spectrum peaks coverage";
    public static final String mps = "spectra matched peak specifity";
    public int                 topPeaks = 10;
    public int                 topHighPeaks = 100;

    public double score(MatchedXlinkedPeptide match) {
        double matched = 0;
        double matchedNonLossy = 0;
        double unmatched = 0;
        double isotopAllIntensity = 0;
        double isotopMatchedIntensity = 0;
        double singleAllIntensity = 0;
        double singleMatchedIntensity = 0;

        HashSet<SpectraPeak> peaks = new HashSet<SpectraPeak>();
        HashSet<SpectraPeak> peaksNonLossy = new HashSet<SpectraPeak>();
        SPC:
        for (SpectraPeakCluster spc : match.getSpectrum().getIsotopeClusters()) {
            boolean isLossy = false;
            boolean isNonLossy = false;
            for (SpectraPeakMatchedFragment spmf : spc.getMonoIsotopic().getMatchedAnnotation()) {
                if (spmf.getCharge() == spc.getCharge()) {
                    if (spmf.getFragment().isClass(Loss.class) || spmf.getFragment().isClass(SecondaryFragment.class)) {
                        isLossy = true;
//                        continue SPC;
                    } else {
                        isNonLossy = true;
//                        continue SPC;
                    }
                }
            }
            if (isNonLossy) {
                peaksNonLossy.addAll(spc);
                peaks.addAll(spc);
            } else if (isLossy) {
                peaks.addAll(spc);
            }

        }

        for (SpectraPeak sp : match.getSpectrum()) {
            boolean isLossy = false;
            boolean isNonLossy = false;

            if (sp.getMatchedAnnotation().size() > 0) {
                peaks.add(sp);
                for (SpectraPeakMatchedFragment spmf : sp.getMatchedAnnotation()) {
                    if (!spmf.getFragment().isClass(Loss.class) && !spmf.getFragment().isClass(SecondaryFragment.class)) {
                        isNonLossy = true;
                    } else {
                        isLossy = true;
                    }
                }
            }
            if (isNonLossy) {
                peaksNonLossy.add(sp);
                peaks.add(sp);
            } if (isLossy) {
                peaks.add(sp);
            }

        }

        for (SpectraPeak sp : match.getSpectrum()) {
            double peakIntensity = sp.getIntensity();

            if (peaks.contains(sp)) {

                matched += peakIntensity;
                if (peaksNonLossy.contains(sp)) {
                    matchedNonLossy += peakIntensity;
                }

                if (sp.hasAnnotation(SpectraPeakAnnotation.isotop)) {
                    isotopAllIntensity += peakIntensity;
                    isotopMatchedIntensity += peakIntensity;
                } else {
                    singleAllIntensity += peakIntensity;
                    singleMatchedIntensity += peakIntensity;
                }

            } else {

                unmatched += sp.getIntensity();
                sp.annotate(SpectraPeakAnnotation.unmatched);

                if (sp.hasAnnotation(SpectraPeakAnnotation.isotop)) {
                    isotopAllIntensity += sp.getIntensity();
                } else {
                    singleAllIntensity += sp.getIntensity();
                }


            }
        }

        int topMatched=0;
        int top20Matched=0;
        int top40Matched=0;
        int topHighMatched=0;
        int peakMatched = 0;
        int pid=0;
        Collection<SpectraPeak> peaksColl = match.getSpectrum().getTopPeaks(-1);
        for (SpectraPeak top : peaksColl) {
            if (peaks.contains(top)) {
                peakMatched++;
                if (pid < topPeaks) {
                    topMatched ++;
                }
                if (pid < 20) {
                    top20Matched ++;
                }
                if (pid < 40) {
                    top40Matched ++;
                }
                if (pid < topHighPeaks) {
                    topHighMatched ++;
                }
            }
            pid++;
        }



//        addScore(match, mVSu, matched / unmatched);
        addScore(match, t10mp, topMatched/(double)topPeaks);
        addScore(match, t20mp, top20Matched/(double)20);
        addScore(match, t40mp, top40Matched/(double)40);
        addScore(match, t100mp, topHighMatched/(double)topHighPeaks);
        addScore(match, pmp, peakMatched/(double)peaksColl.size());
        addScore(match, mp, matched / (matched + unmatched));
        addScore(match, mnlp, matchedNonLossy / (matched + unmatched));
        addScore(match, ip, isotopAllIntensity / (isotopAllIntensity + singleAllIntensity));
        if (isotopAllIntensity > 0) {
            addScore(match, imp, isotopMatchedIntensity / isotopAllIntensity);
        } else {
            addScore(match, imp, 0);
        }
        addScore(match, smp, singleMatchedIntensity / singleAllIntensity);
        return matched / (matched + unmatched);
    }

    public String[] scoreNames() {
//        return new String[]{mVSu, mp, ip, imp, smp};
        return new String[]{mp, mnlp, ip, imp, smp, t10mp,t20mp,t40mp,t100mp,pmp};
    }

    public double getOrder() {
        return 50;
    }


}
