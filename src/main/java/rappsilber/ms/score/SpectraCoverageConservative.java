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
import java.util.HashSet;
import rappsilber.ms.sequence.ions.DoubleFragmentation;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.SpectraPeakCluster;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedBaseFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class SpectraCoverageConservative extends AbstractScoreSpectraMatch{

    private int minConservativeLosses = 3;

    public SpectraCoverageConservative(int minConservativeLosses) {
        this.minConservativeLosses = minConservativeLosses;
    }
    
    
    public double score(MatchedXlinkedPeptide match) {
        MatchedFragmentCollection mfc = match.getMatchedFragments();
        double matched = 0;
        double unmatched = 0;
        HashSet<SpectraPeak> peaks = new HashSet<SpectraPeak>();

//        for (SpectraPeak sp : match.getSpectra()) {
//            for (SpectraPeakMatchedFragment spmf: sp.getMatchedAnnotation()) {
//                mfc.add(spmf.getFragment(), spmf.getCharge(), sp);
//            }
//        }

        SPC: for (SpectraPeakCluster spc : match.getSpectrum().getIsotopeClusters()) {
            for (SpectraPeakMatchedFragment spmf: spc.getMonoIsotopic().getMatchedAnnotation()) {
                if (spmf.getFragment().isClass(DoubleFragmentation.class))
                    continue;

                if (spmf.getCharge() == spc.getCharge()) {
                    MatchedBaseFragment mbf = mfc.getMatchedFragmentGroup(spmf.getFragment(), spmf.getCharge());
                    if (mbf.isBaseFragmentFound() || mbf.getLosses().size() >= minConservativeLosses ) {
                        for (SpectraPeak sp : spc) {
                            peaks.add(sp);
                        }
                        continue SPC;
                    }
                }
            }
        }

        for (SpectraPeak sp : match.getSpectrum()) {
            for (SpectraPeakMatchedFragment spmf : sp.getMatchedAnnotation()) {
                if (spmf.getFragment().isClass(DoubleFragmentation.class))
                    continue;
                MatchedBaseFragment mbf = mfc.getMatchedFragmentGroup(spmf.getFragment(), spmf.getCharge());
                if (mbf.isBaseFragmentFound() || mbf.getLosses().size() >= minConservativeLosses ) {
                    peaks.add(sp);
                }
            }
        }

        for (SpectraPeak sp : match.getSpectrum()) {
            if (peaks.contains(sp))
                matched += sp.getIntensity();
            else
                unmatched += sp.getIntensity();
        }

        double score = matched/(matched + unmatched);
        addScore(match, this.getClass().getSimpleName(), score);
        return score;
    }

    public double getOrder() {
        return 100;
    }

}
