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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class PeakFilteredSpectrumAccess extends AbstractSpectraFilter{
    private class PeakInfo {
        double mz;
        double minAbsIntensity;
        double minrelativeIntensity;

        public PeakInfo(double mz, double minAbsIntensity, double minrelativeIntensity) {
            this.mz = mz;
            this.minAbsIntensity = minAbsIntensity;
            this.minrelativeIntensity = minrelativeIntensity;
        }
    }

    private ToleranceUnit m_tolerance;
    private int m_minFoundPeaks = 1;


    ArrayList<PeakInfo> m_peaks = new ArrayList<PeakInfo>();

    public PeakFilteredSpectrumAccess(ToleranceUnit tolerance) {
        this.m_tolerance = tolerance;
    }

    public PeakFilteredSpectrumAccess() {
    }


    

    public void addPeak(double mz) {
        m_peaks.add(new PeakInfo(mz, 0, 0));
    }

    public void addPeak(double mz, double minAbsIntens, double minRelativeIntens) {
        m_peaks.add(new PeakInfo(mz, minAbsIntens, minRelativeIntens));
    }


    public void setFindAll() {
        m_minFoundPeaks = -1;
    }

    public void setFindAny() {
        m_minFoundPeaks = 1;
    }

    public void setTolerance(ToleranceUnit tu) {
        m_tolerance = tu;
    }


    public void setMinimumFoundPeaks(int minFoundPeaks) {
        m_minFoundPeaks = minFoundPeaks;
    }
    

    @Override
    public boolean passScan(Spectra s) {
        int count = 0;
        double maxInt = s.getMaxIntensity();
        for (PeakInfo pi : m_peaks) {
            SpectraPeak sp = s.getPeakAt(pi.mz,m_tolerance);
            if (sp != null) {
                double intens = sp.getIntensity();
                double relInte = intens/maxInt;
                if (intens >=  pi.minAbsIntensity && relInte >= pi.minrelativeIntensity) {
                    count++;
                }
            }
        }
        
        if (m_minFoundPeaks < 0) {
            return count == m_peaks.size();
        } else {
            return count >= m_minFoundPeaks;
        }
    }
    
    public Collection<Double> getPeaks(){
        HashSet<Double> peaks = new HashSet<>(m_peaks.size());
        for (PeakInfo p : m_peaks) {
            peaks.add(p.mz);
        }
        return peaks;
    }

}
