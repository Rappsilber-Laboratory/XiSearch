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
public class TopPeaks extends AbstractStatistic {

    HashMap<String, Integer> GroupCounts;
    int m_topN = 10;
    Spectra m_deisotoped;
    MatchedFragmentCollection m_MatchedFragments;
    int[] m_matchgroupCount = new int[]{0,0,0,0,0,0};
    int m_countSpectra = 0;
    



    public TopPeaks(int topN) {
        m_topN = topN;
    }

    public void countSpectra(Spectra s) {
        m_countSpectra++;
        m_MatchedFragments = new MatchedFragmentCollection(s.getPrecurserCharge());
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

        // get the top n peaks
        Collection<SpectraPeak> peaks = s.getTopPeaks(m_topN);
        for (SpectraPeak sp : peaks) {
            // and count the
            ArrayList<SpectraPeakMatchedFragment> ma =  sp.getMatchedAnnotation();

            
            if (ma.size() == 0) { // unmatched peak
                m_matchgroupCount[m_matchgroupCount.length -1]++;
                //TODO: cleanup
                continue;
            }

            java.util.Arrays.fill(thisMatched,false);


            for (SpectraPeakMatchedFragment mf : ma){
                if (mf.getFragment().toString().contentEquals("LKQSQ")) {
                    mf.getFragment();
                }
                MatchedBaseFragment mbf = m_MatchedFragments.getMatchedFragmentGroup(
                            mf.getFragment(),
                            mf.getCharge());
                if (mbf == null) {
                    mbf = null;
                }
                if (mf.getFragment().isClass(Loss.class)) {
                    if (mbf.isBaseFragmentFound()) { // loss peak but base fragment was matched as well
                        thisMatched[2] = true;
                    } else if (mbf.getLosses().size()>1) { // loss without base peak - but other losses to the same fragment
                        thisMatched[3] = true;
                    } else {
                        thisMatched[4] = true; // loss without support
                    }

                } else {
                    if (mbf.getLosses().size() > 0) {
                        thisMatched[0] = true; // matched base peak - with supporting losses
                    } else {
                        thisMatched[1] = true; // without supporting losses
                    }
                }

            }
            for (int i = 0; i< thisMatched.length; i ++) {
                if (thisMatched[i]) {
                    m_matchgroupCount[i] ++;
                    break;
                }
            }


        }

    }

    public int[] getGroupCountsAbsolute() {
        return m_matchgroupCount;
    }

    public double[] getGroupCountsRelative() {
        double counts[] = new double[6];
        double spectraCount = m_countSpectra;
        for (int g=0; g< m_matchgroupCount.length; g++) {
            counts[g] = m_matchgroupCount[g] / spectraCount;
        }
        return counts;
    }

    public void countSpectraMatch(MatchedXlinkedPeptide match) {
        countSpectra(match.getSpectrum());
    }

    public String getTable() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
