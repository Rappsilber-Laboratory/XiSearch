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
package rappsilber.ms.statistics.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.utils.SortedLinkedList;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class GroupPeaksByTopPeaks implements SpectraPeakGroups{
    private double[] m_groupPercentages;

    public GroupPeaksByTopPeaks(double[] groups) {
        setGroupBorders(groups);
    }

    /**
     * defines how many peaks (relative to the overal number) goes into each peak
     * e.g.
     * setGroupBorders(new double[]{0.3, 0.4, 0.6, 0.8, 1);
     * @param percent
     */
    public void setGroupBorders(double[] percent) {
        double sum = 0;
        m_groupPercentages = percent.clone();
        for (int i = 0;i< percent.length; i++) {
            sum += percent[i];
        }



        if (sum <= 1) {

            for (int i=1; i< m_groupPercentages.length;i++) {
                m_groupPercentages[i] += m_groupPercentages[i-1];
            }
        }

        if (m_groupPercentages[m_groupPercentages.length - 1] < 0.99999) {
            double[] dummy = new double[m_groupPercentages.length + 1];
            System.arraycopy(m_groupPercentages, 0, dummy, 0, m_groupPercentages.length);
            dummy[m_groupPercentages.length] = 1;
            m_groupPercentages = dummy;
        }


    }

    public double[] getGroupBorders() {
        return m_groupPercentages;
    }


    public HashMap<Double, ArrayList<SpectraPeak>> getPeakGroubs(Spectra s) {
        double sum = 0;

        HashMap<Double, ArrayList<SpectraPeak>> retList = new HashMap<Double, ArrayList<SpectraPeak>>(m_groupPercentages.length);

        SortedLinkedList<SpectraPeak> peaks = s.getPeaksByIntensity();
        int[] lastPeakIndex = new int[m_groupPercentages.length];

        for (int i =0 ; i< m_groupPercentages.length; i++) {
            lastPeakIndex[i] = (int)(m_groupPercentages[i] * peaks.size());
        }

        Iterator<SpectraPeak> peakIterator = peaks.descendingIterator();
        int peakId = 0;
        for (int gid = 0; gid < m_groupPercentages.length; gid++) {
            int groupEnd = lastPeakIndex[gid];
            Double groupName = m_groupPercentages[gid];
            ArrayList<SpectraPeak> peakGroup = new ArrayList<SpectraPeak>();
            retList.put(groupName, peakGroup);
            for (;peakId < groupEnd; peakId++) {
                SpectraPeak p = peakIterator.next();
//                if ((int)p.getMZ() == 464)
//                    System.out.println("found");
                peakGroup.add(p);
            }

        }
        return retList;

    }

}
