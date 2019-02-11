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
package rappsilber.ms.dataAccess.filter.spectrafilter;

import java.util.TreeSet;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.spectra.Spectra;


/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class BS3ReporterIonPeaksFilteredSpectrumAccess extends AbstractSpectraFilter { // extends AbstractStackedSpectraAccess {
    TreeSet<Double> m_filteredMasses = new TreeSet<Double>();
    
    int m_readSpectra = 0;

//    SpectraAccess m_reader;

    Spectra m_current = null;
    Spectra m_next = null;
    ToleranceUnit m_tolerance = new ToleranceUnit(20, "ppm");


    private class peakScoreInfo {
        double mz;
        double score;

        public peakScoreInfo(double mz, double score) {
            this.mz = mz;
            this.score = score;
        }

    }




    /*
 138.09134,0.18108651911469,+
139.07536,0.00010060362173,+
156.10191,0.06740442655936,+
192.13829,0.0010060362173,+
194.15394,0.00010060362173,+
221.16484,0.00704225352113,+
222.14886,0.0010060362173,+
239.1754,0.10160965794769,+
240.15942,0.00402414486922,+
267.17032,0.00503018108652,+
285.18088,0.00201207243461,+
305.22235,0.00201207243461,++
350.24382,0.0010060362173,++
157.08592,0.00201207243461,-
257.18597,0.0010060362173,-
*/
    peakScoreInfo[] positivePeaks = new peakScoreInfo[] {
        new peakScoreInfo(138.09134,0.18108651911469),
        new peakScoreInfo(139.07536,0.00010060362173),
        new peakScoreInfo(156.10191,0.06740442655936),
        new peakScoreInfo(192.13829,0.0010060362173),
        new peakScoreInfo(194.15394,0.00010060362173),
        new peakScoreInfo(221.16484,0.00704225352113),
        new peakScoreInfo(222.14886,0.0010060362173),
        new peakScoreInfo(239.1754,0.10160965794769),
        new peakScoreInfo(240.15942,0.00402414486922),
        new peakScoreInfo(267.17032,0.00503018108652),
        new peakScoreInfo(285.18088,0.00201207243461),
        new peakScoreInfo(305.22235,0.00201207243461),
        new peakScoreInfo(350.24382,0.0010060362173),
    };

    double[] positiveMZ;

    peakScoreInfo[] negativePeaks = new peakScoreInfo[] {
        new peakScoreInfo(157.08592,0.00201207243461),
        new peakScoreInfo(257.18597,0.0010060362173),
    };

    double[] negativeMZ;

    {
        double[] posMZ =new double[positivePeaks.length];
        int p=0;
        for (peakScoreInfo mz : positivePeaks)
            if (mz.score <0.01)
                posMZ[p++] = mz.mz;
        positiveMZ = new double[p];
        System.arraycopy(posMZ, 0, positiveMZ, 0 , p );
                    
        negativeMZ = new double[negativePeaks.length];
        for (int i = 0 ; i< negativePeaks.length;i++)
            negativeMZ[i] = negativePeaks[i].mz;
        
    }


//    public void setReader(SpectraAccess innerAccess){
//        m_InnerAcces = innerAccess;
//        next();
//    }
//

//    @Override
//    public Spectra current() {
//        return m_InnerAcces.current();
//    }
//
//    @Override
//    public int countReadSpectra() {
//        return m_readSpectra;
//    }
//
//    public boolean hasNext() {
//        synchronized (m_sync) {
//            if (m_next != null)
//                return true;
//            else
//                return false;
//        }
//   //     return m_next != null;
//    }
//
//    public  Spectra next() {
//        synchronized (m_sync) {
//            m_current = m_next;
//            m_next = null;
//            Spectra n = null;
//
//            iterator: while (m_InnerAcces.hasNext()) {
//                n = m_InnerAcces.next();
//
//                for (double mz : positiveMZ) {
//                    if (n.getPeakAt(mz) != null) { // positive case
//                        for (double negMZ : negativeMZ)
//                            if (n.getPeakAt(negMZ, m_tolerance) != null) // negative case
//                                continue iterator;
//                        m_next = n;
//                        break iterator;
//                    }
//                }
//
//            }
//        }
//
//        return m_current;
//
//    }

    @Override
    public boolean passScan(Spectra s) {
        for (double mz : positiveMZ) {
            if (s.getPeakAt(mz) != null) { // positive case
                for (double negMZ : negativeMZ)
                    if (s.getPeakAt(negMZ, m_tolerance) != null) // negative case
                        return false;
                return true;
            }
        }
        return false;
    }

}
