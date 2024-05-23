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

import java.util.HashMap;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedBaseFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class CountMatchGroups extends AbstractStatistic {

    HashMap<String, Integer> GroupCounts;
    int m_topN = 10;
    Spectra m_deisotoped;
    MatchedFragmentCollection m_MatchedFragments;

    final static int SingleLossWithBase = 0;
    final static int MultiLossWithBase = 1;
    final static int SingleLossWithoutBase = 2;
    final static int MultiLossWithoutBase = 3;
    final static int BaseWithoutLoss = 4;
    final static int BaseWithLoss = 5;

    int[] m_matchgroupCount = new int[]{0,0,0,0,0,0};
    int m_countSpectra = 0;
    



    public CountMatchGroups() {
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
                if (mf.getFragment().toString().contentEquals("LKQSQ") &&  mf.getCharge() == 4) {
                    mf.getFragment();
                }
                m_MatchedFragments.add(mf.getFragment(), mf.getCharge(), sp);
            }
        }

        boolean[] thisMatched = new boolean[m_matchgroupCount.length];

        for (MatchedBaseFragment mbf : m_MatchedFragments) {

            if (mbf.isBaseFragmentFound()) { // with base fragment
                HashMap<Loss,SpectraPeak> sl = mbf.getLosses();
                if (sl.size() == 1) {
                    m_matchgroupCount[SingleLossWithBase]++;
                    m_matchgroupCount[BaseWithLoss]++;
                } else if (sl.size() > 1) {
                    m_matchgroupCount[MultiLossWithBase]+=sl.size();
                    m_matchgroupCount[BaseWithLoss]++;
                } else {
                    m_matchgroupCount[BaseWithoutLoss]++;
                }
            } else { // without base fragemnt
                HashMap<Loss,SpectraPeak> sl = mbf.getLosses();
                if (sl.size() == 1) {
                    m_matchgroupCount[SingleLossWithoutBase]++;
                } else {
                    m_matchgroupCount[MultiLossWithoutBase]+=sl.size();
                }
            }
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
               "\n\"Single Loss\"\t" + m_matchgroupCount[SingleLossWithoutBase]+ "\n";
    }


}
