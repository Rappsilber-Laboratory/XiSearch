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

import java.util.HashSet;
import rappsilber.config.RunConfig;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.dataAccess.AbstractStackedSpectraAccess;
import rappsilber.ms.spectra.Spectra;
import static rappsilber.ms.spectra.Spectra.DEFAULT_ISOTOP_DETECTION;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.SpectraPeakCluster;
import rappsilber.ms.spectra.SpectraPeakClusterList;
import rappsilber.utils.Util;

/**
 * Correct precursor based on MS2 peaks.
 * 
 * This filter will look up if there is a a peak in the MS2 that could 
 * correspond to the precursor. If it finds one it will detect any isoptope 
 * cluster that contains that peak and add these to the searched precursor 
 * information for that spectrum.
 * 
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class MS2PrecursorDetection  extends AbstractStackedSpectraAccess {
    private double window=3;
    private double countCorrectedMZ=0;
    private double countCorrectedCharge=0;
    private double countFound=0;
    private double count=0;

    public MS2PrecursorDetection(RunConfig conf) {
    }

    
    public MS2PrecursorDetection(RunConfig conf,String settings) {
        String[] set = settings.split(";");
        for (String s: set) {
            String[] args = s.split(":");
            String a = args[0].toLowerCase().trim();
            String v = args[1].toLowerCase().trim();
            if (a.contentEquals("window")) {
                window = Double.parseDouble(v);
            }
        }
    }
    public MS2PrecursorDetection(RunConfig conf,double window) {
        this.window = window;
    }
    
    
    public Spectra next() {
        Spectra  i =m_InnerAcces.next(); 
        // do we have a precursor peak?
        double precMZ =i.getPrecurserMZ();
        
        SpectraPeak precPeak =  i.getPeakAt(precMZ);
        count++;
        
        if (precPeak != null) {
            countFound++;
            double precPeakMZ = precPeak.getMZ();
            Spectra s = i.cloneEmpty();
            for (SpectraPeak sp : i.getPeaks(i.getPrecurserMZ()-window, i.getPrecurserMZ()+window)) {
                s.addPeak(sp.clone());
            }
            DEFAULT_ISOTOP_DETECTION.anotate(s);
            SpectraPeakClusterList spcl = s.getIsotopeClusters();
            HashSet<Double> pmz = new HashSet<>(spcl.size());
            HashSet<Integer> pc = new HashSet<>(spcl.size());
            for (SpectraPeakCluster  spc : spcl) {
                // only consider cluster that actually cover the precusor
                if (spc.hasPeakAt(precPeakMZ)) {
                    if (s.getTolearance().compare(i.getPrecurserMZ(), spc.getMZ()) != 0 ) {
                        // assume that the precursor defined before actually belongs to 
                        // the peptide fragmented then the actual monoisotopic peak 
                        // should be n*(C13-C2) Da away
                        int c = spc.getCharge();
                        double diff = (spc.getMZ()-i.getPrecurserMZ())*c/Util.C13_MASS_DIFFERENCE;
                        long C13_count = Math.round(diff);
                        pmz.add(precMZ+Util.C13_MASS_DIFFERENCE*C13_count);
                    }
                    if (i.getPrecurserCharge() != spc.getCharge() ) {
                        pc.add(spc.getCharge());
                    }
                }
            }
            if (pmz.size() >0) {
                countCorrectedMZ++;
                i.setAdditionalMZ(pmz);
            }
            if (pc.size() >0) {
                countCorrectedCharge++;
                i.setAdditionalCharge(pc);
            }

        }

        if (count >0 && count % 1000 == 0) {
            System.err.println("Spectra seen:"+count+"\nPrecursor seen:"+ 
                    countFound +"\nm/z corrected:"+countCorrectedMZ
                    +"\ncharge corrected:"+countCorrectedCharge);
        }

        return i;
    }

 
}
