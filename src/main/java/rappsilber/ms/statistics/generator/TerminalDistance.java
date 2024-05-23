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

import java.util.HashSet;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class TerminalDistance extends AbstractStatistic{
    int m_minSize = 6;
    int m_maxDist = 2;
    int m_countSpectra = 0;
    Class m_IncludeFragmentClass = Fragment.class;
    Class m_ExcludeFragmentClass = Loss.class;
    int[] m_NTermDistance;
    int[] m_CTermDistance;


    public TerminalDistance(int minSize, int maxDist) {
        m_minSize = minSize;
        m_maxDist = maxDist;

        m_NTermDistance = new int[m_maxDist + 1];
        m_CTermDistance = new int[m_maxDist + 1];
    }

    public TerminalDistance(int minSize) {
        m_minSize = minSize;
        m_maxDist = (minSize) / 2;

        m_NTermDistance = new int[m_maxDist + 1];
        m_CTermDistance = new int[m_maxDist + 1];
    }

    public TerminalDistance(int minSize, Class IncludeClass) {
        this(minSize);
        m_IncludeFragmentClass = IncludeClass;
    }

    public TerminalDistance(int minSize, Class IncludeClass, Class ExcludeClass) {
        this(minSize);
        m_IncludeFragmentClass = IncludeClass;
        m_ExcludeFragmentClass = ExcludeClass;
    }

    public void countSpectraMatch(MatchedXlinkedPeptide match) {
        // go through all matched fragments and count how far from the terminal sides these are
        Peptide p1 = match.getPeptide(0);
        Peptide p2 = match.getPeptide(1);

//        if (p1.length() <= m_minSize || p2.length() <= m_minSize)
//           return; // ignore matches to peask to small to be considered

        m_countSpectra++;

        HashSet<Fragment> matchedFragments = new HashSet<Fragment>();
        for (SpectraPeak sp : match.getSpectrum().getPeaks()) {

            for (SpectraPeakMatchedFragment mf : sp.getMatchedAnnotation()) {
                Fragment f = mf.getFragment();

                // only considere the basic ION (B/Y) and mybe filtered down
                if ((!f.isClass(m_IncludeFragmentClass)) || (f.isClass(m_ExcludeFragmentClass))) {
                    continue;
                }

                if (matchedFragments.contains(f)) {
                    continue;
                }

                matchedFragments.add(f);

                if (f.getStart() > 0 && f.getStart() <= m_maxDist) {
                    m_NTermDistance[f.getStart()]++;
                }

                int pepLast = f.getPeptide().length() -1;

                if (f.getPeptide().length()<= m_minSize && f.getEnd() < pepLast && pepLast - f.getEnd() <= m_maxDist) {
                    m_CTermDistance[pepLast - f.getEnd()]++;
                }

            }

        }


    }

    public String getTable() {
        StringBuffer sb = new StringBuffer();
        sb.append("checked " + m_countSpectra + "Spectra\n");
        sb.append("Distance\tNTerminal\tCTerminal\n");
        for (int i = 1; i < m_NTermDistance.length;i++) {
            sb.append(i + "\t" + m_NTermDistance[i]/(double)m_countSpectra + "\t" + m_CTermDistance[i]/(double)m_countSpectra + "\n");
        }

        return sb.toString();

    }

}
