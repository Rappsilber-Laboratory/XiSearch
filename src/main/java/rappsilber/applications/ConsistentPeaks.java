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
package rappsilber.applications;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.AbstractRunConfig;
import rappsilber.config.RunConfig;
import rappsilber.config.RunConfigFile;
import rappsilber.ms.Range;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.dataAccess.SpectraAccess;
import rappsilber.ms.dataAccess.filter.spectrafilter.ScanFilteredSpectrumAccess;
import rappsilber.ms.dataAccess.msm.MSMIterator;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class ConsistentPeaks {


    boolean m_FixedBins = false;
    double  m_minRange = 100;
    double  m_maxRange = 4000;
    int     m_minCharge = 0;

    ToleranceUnit m_binTolerance = new ToleranceUnit("30ppm");
    ScanFilteredSpectrumAccess m_filter = null;
    TreeMap<Double,Counter> m_foundPeaks = new TreeMap<Double, Counter>();

    public ConsistentPeaks() {}

    public ConsistentPeaks(boolean FixedBines, double minRange, double maxRange, int minCharge, ToleranceUnit tu) {
        m_FixedBins = FixedBines;
        m_minRange = minRange;
        m_maxRange = maxRange;
        m_binTolerance = tu;
        m_minCharge = minCharge;
    }

    public TreeMap<Double,Counter> scanPeaks(SpectraAccess sa) {
        TreeMap<Double,Counter> peaks = new TreeMap<Double, Counter>();
        if (m_FixedBins) {
            for (double d = m_minRange;d<m_maxRange;) {
                peaks.put(d, new Counter(0, 0));
                d += m_binTolerance.getRangeSize(d);
            }
        }

        while (sa.hasNext()) {
            Spectra s = sa.next();
            if (m_minCharge != 0) {
                if (s.getPrecoursorChargeAlternatives().length >1 || s.getPrecurserCharge() < m_minCharge) {
                    continue;
                }
            }
            double sMax = s.getMaxIntensity();
            for (SpectraPeak sp : s) { 
                if (sp.getMZ() <= m_maxRange) {
                    //System.err.println(sp.toString());
                    Range r = m_binTolerance.getRange(sp.getMZ());
                    SortedMap<Double, Counter> sm = peaks.subMap(r.min,r.max);
                    Counter c;
                    if (sm.size() == 0) {
                        c = new Counter(sp.getIntensity(), sMax);
                        peaks.put(sp.getMZ(), c);
                    } else {
                        c = sm.get(sm.firstKey());
                        c.add(sp.getIntensity(), sMax);
                    }
                }
            }
            s.free();
        }

        return peaks;
    }


    public static class Counter {
        int count;
        double Intensity;
        double relativeIntensity;

        public Counter(double intens, double maxIntens) {
            count = 1;
            Intensity = intens;
            relativeIntensity = (intens/maxIntens);
        }

        public void add(double intens, double maxIntens) {
            Intensity += intens;
            relativeIntensity += (intens/maxIntens);
            count++;
        }

        public double getIntensity() {
            return Intensity / count;
        }

        public double getRelativeIntensity() {
            return relativeIntensity / count;
        }
        
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, ParseException {
        TreeMap<Double,Counter> peaks;
        RunConfig conf;
        ScanFilteredSpectrumAccess filter = null;
        conf = new AbstractRunConfig() { {setFragmentTolerance(new ToleranceUnit("30ppm")); } };

        String msmFile = args[0];
        File msm = new File(msmFile);
        String csvOut = csvOut = args[1];
        if (args.length == 3)  {
            if (args[1].matches(".*\\.[cC][oO][nN][fF]$")) {

                conf = new RunConfigFile(args[1]);
                Logger.getLogger(ConsistentPeaks.class.getName()).log(Level.INFO, "Read config from {0}", args[1]);
            } else {
                filter = new ScanFilteredSpectrumAccess(true);
                filter.readFilter(new File(args[1]));
                Logger.getLogger(ConsistentPeaks.class.getName()).log(Level.INFO, "Read filter from {0}", args[1]);
            }
            csvOut = args[2];
        } else if (args.length == 4) {
            conf = new RunConfigFile(args[1]);
            Logger.getLogger(ConsistentPeaks.class.getName()).log(Level.INFO, "Read config from {0}", args[1]);
            filter = new ScanFilteredSpectrumAccess(true);
            filter.readFilter(new File(args[2]));
            Logger.getLogger(ConsistentPeaks.class.getName()).log(Level.INFO, "Read filter from {0}", args[2]);
            csvOut = args[3];
        }

        PrintStream out = new PrintStream(csvOut);


        ToleranceUnit t = conf.getFragmentTolerance();
        System.err.println(t.toString());
        MSMIterator msmiter = new MSMIterator(msm, t, 1, conf);
        SpectraAccess sa = msmiter;
        if (filter != null) {
            filter.setReader(msmiter);
            sa = filter;
        }
        peaks = new ConsistentPeaks().scanPeaks(sa);
        out.println("M/Z , Peaks Found, Average Absolute Intensity, Average relative intensity");
        for (Double mz : peaks.keySet()) {
            Counter c = peaks.get(mz);
            out.println("" + mz + ", " + c.count + ", " + c.getIntensity() + "," + c.getRelativeIntensity());
        }



    }
}
