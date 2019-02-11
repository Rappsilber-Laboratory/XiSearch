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
package rappsilber.ms.statistics.generator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.annotation.SpectraPeakAnnotation;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedBaseFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class CountMatchPeaks extends AbstractStatistic {

    HashMap<String, Integer> GroupCounts;
    int m_topN = 10;
    Spectra m_deisotoped;
    MatchedFragmentCollection m_MatchedFragments;

    final static int BaseWithLoss = 0;
    final static int BaseWithoutLoss = 1;
    final static int MultiLossWithBase = 2;
    final static int SingleLossWithBase = 3;
    final static int MultiLossWithoutBase = 4;
    final static int SingleLossWithoutBase = 5;
    final static int Unmatched = 6;

    int[] m_matchgroupCount = new int[]{0,0,0,0,0,0,0};
    int m_countSpectra = 0;
    



    public CountMatchPeaks() {
    }

    public void countSpectraMatch(MatchedXlinkedPeptide match) {

        Spectra s = match.getSpectrum();

        m_countSpectra++;
        m_MatchedFragments.free();
        m_MatchedFragments = new MatchedFragmentCollection(match.getSpectrum().getPrecurserCharge());
        //m_deisotoped = s.deIsotop();
        // retrive all the matched fragments
        for (SpectraPeak sp : s.getPeaks()) {
            for (SpectraPeakMatchedFragment mf : sp.getMatchedAnnotation()) {
                m_MatchedFragments.add(mf.getFragment(), mf.getCharge(), sp);
            }
        }

        // count the best match for each peak
        for (SpectraPeak sp : s.getPeaks()) {

            if ((sp.hasAnnotation(SpectraPeakAnnotation.isotop)) && // ignore isotop peaks
                    !sp.hasAnnotation(SpectraPeakAnnotation.monoisotop))
                continue;

            int peakGroup = Unmatched;
            for (SpectraPeakMatchedFragment mf : sp.getMatchedAnnotation()) {
                Fragment f = mf.getFragment();
                MatchedBaseFragment mbf = m_MatchedFragments.getMatchedFragmentGroup(f, mf.getCharge());
                if (mbf.isBaseFragmentFound()) {
                    if (f.isClass(Loss.class)) {
                        // lossy fragment with parent
                        if (mbf.getLosses().size()>1) {
                            if (peakGroup > MultiLossWithBase)
                                peakGroup = MultiLossWithBase;
                        } else {
                            if (peakGroup > SingleLossWithBase)
                                peakGroup = SingleLossWithBase;

                        }
                    } else {
                        if (mbf.getLosses().size()>0) {
                            peakGroup = BaseWithLoss;
                        } else {
                            if (peakGroup > BaseWithoutLoss)
                                peakGroup = BaseWithoutLoss;

                        }
                    }
                } else if (peakGroup>MultiLossWithoutBase) {
                    if (mbf.getLosses().size()>1) {
                        if (peakGroup > MultiLossWithoutBase)
                            peakGroup = MultiLossWithoutBase;
                    } else {
                        if (peakGroup > SingleLossWithoutBase)
                            peakGroup = SingleLossWithoutBase;
                    }
                }
            }
            m_matchgroupCount[peakGroup]++;
        }

    }


    public String getTable() {
       return
               "Group\tCount"+
               "\n\"Parent with Loss\"\t" + m_matchgroupCount[BaseWithLoss]+
               "\n\"Parent without Loss\"\t" + m_matchgroupCount[BaseWithoutLoss] +
               "\n\"Multiple Loss + Parent\"\t" + m_matchgroupCount[MultiLossWithBase]+
               "\n\"Single Loss + Parent\"\t"+ m_matchgroupCount[SingleLossWithBase]+
               "\n\"Multiple Loss\"\t" + m_matchgroupCount[MultiLossWithoutBase]+
               "\n\"Single Loss\"\t" + m_matchgroupCount[SingleLossWithoutBase]+
               "\n\"Unmatched Peaks\"\t" + m_matchgroupCount[Unmatched]+ "\n";
    }


}
