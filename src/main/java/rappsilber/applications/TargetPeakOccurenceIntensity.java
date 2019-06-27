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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.util.TreeSet;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.dataAccess.SpectraAccess;
import rappsilber.ms.dataAccess.msm.AbstractMSMAccess;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class TargetPeakOccurenceIntensity {

//    static double[] m_TargetPeaks = new double[]{156.14,
//184.13,
//269.22,
//297.22,
//267.21,
//305.22,
//307.24,
//255.21,
//240.16,
//270.23,
//222.15,
//268.21,
//340.22,
//150.11,
//201.16,
//185.14,
//215.18,
//224.17,
//380.26,
//335.24,
//305.23,
//139.08,
//241.19,
//170.12,
//335.23,
//269.23,
//256.21,
//134.1,
//352.26,
//165.06,
//189.13,
//201.19,
//253.19,
//123.09,
//283.2,
//363.75,
//153.62,
//139.07,
//240.18,
//207.15,
//184.14,
//364.85,
//294.82,
//239.18,
//254.2,
//342.7
//};

    //static double[] m_TargetPeaks = new double[]{165.06,201.19,267.17,305.22,306.23,326.25,333.22,378.24,385.14,176.11}
    static double[] m_TargetPeaks = new double[]{
120.0875,
120.578,
134.084,
139.075,
153.1145,
156.102,
157.086,
165.06,
170.138,
175.622,
176.11,
184.1355,
198.1354125175,
201.19,
222.149,
239.175,
240.156,
267.168,
305.22,
305.229,
306.23,
326.25,
333.22,
339.276,
350.244,
367.271,
378.24,
385.14,
395.263,};


    static TreeSet<Double> m_TargetSet;

    static {
        m_TargetSet = new TreeSet<Double>();
        for (double d: m_TargetPeaks) 
            m_TargetSet.add(d);
    }
    
    public double[] getPeaks(Spectra s, TreeSet<Double> TargetPeaks) {
        double[] peaks = new double[TargetPeaks.size()];
        double maxIntensity = s.getMaxIntensity();
        int p = 0;
        for (double d : TargetPeaks) {
            SpectraPeak sp = s.getPeakAt(d);
            if (sp == null) {
                peaks[p++] = 0;
            } else
                peaks[p++] = sp.getIntensity()/maxIntensity;
        }
        return peaks;
    }


    public void run(PrintStream out, SpectraAccess sa, TreeSet<Double> TargetPeaks) {
        out.print("Run, Scan");
        for (Double d : TargetPeaks) {
            out.print("," + d);
        }
        out.println();

        while (sa.hasNext()) {
            Spectra s = sa.next();
            out.print(s.getRun() + "," + s.getScanNumber());
            for (double d : getPeaks(s, TargetPeaks)) {
                out.print("," + d);
            }
            out.println();
        }

    }


    public static void main(String[] argv) throws FileNotFoundException, IOException, ParseException {
        ToleranceUnit t = new ToleranceUnit(0.005, "da");
        //SpectraAccess sa = AbstractMSMAccess.getMSMIterator("/home/lfischer/people/Helena/M090625_crosslinked_matched.msm", t, 1);
        SpectraAccess sa = AbstractMSMAccess.getMSMIterator("/home/lfischer/people/Juri/IndicatorPeaks/all.msm", t, 1, null);
        //SpectraAccess sa = AbstractMSMAccess.getMSMIterator("/home/lfischer/people/Juri/IndicatorPeaks/XLinkedSynthetic_and_Yeast.msm", t, 1);

        //PrintStream out = new PrintStream("/home/lfischer/people/Juri/IndicatorPeaks/XLinkdSynth.csv");
        PrintStream out = new PrintStream("/home/lfischer/people/Juri/IndicatorPeaks/Synthetic_Ribosom_Yeast_peaks_intensity.csv");
        //PrintStream out = new PrintStream("/home/lfischer/people/Juri/IndicatorPeaks/XLinkedSynthetic_and_Yeast_peaks_intensity.csv");
        new TargetPeakOccurenceIntensity().run(out, sa, m_TargetSet);
    }
}

