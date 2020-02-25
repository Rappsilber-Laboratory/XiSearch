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
import rappsilber.config.AbstractRunConfig;
import rappsilber.config.RunConfig;
import rappsilber.ms.dataAccess.AbstractStackedSpectraAccess;
import rappsilber.ms.spectra.Spectra;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class Denoise  extends AbstractStackedSpectraAccess {


    private double minMZ=0d;
    private double maxMZ=Double.POSITIVE_INFINITY;
    private double window=100d;
    private int peaks=20;
    private boolean keepPrecursor=true;
    private HashSet<Double> keepPeaks=new HashSet<>();
    

    public Denoise(RunConfig conf) {
    }

    
    public Denoise(RunConfig conf,String settings) {
        String[] set = settings.split(";");
        for (String s: set) {
            String[] args = s.split(":");
            String a = args[0].toLowerCase().trim();
            String v = args[1].toLowerCase().trim();
            if (a.contentEquals("minmz")) {
                minMZ = Double.parseDouble(v);
            } else if (a.contentEquals("maxmz")) {
                maxMZ = Double.parseDouble(v);
            } else if (a.contentEquals("peaks")) {
                peaks = (int)Double.parseDouble(v);
            } else if (a.contentEquals("window")) {
                window = Double.parseDouble(v);
            } else if (a.contentEquals("keepprecursor")) {
                keepPrecursor = AbstractRunConfig.getBoolean(v, keepPrecursor);
            } else if (a.contentEquals("keeppeaks")) {
                String[] peaks = v.split(",");
                for (String p : peaks) {
                    Double d=new Double(p.trim());
                    keepPeaks.add(d);
                }
            }
            
        }
    }
    
    public Denoise(RunConfig conf,double minMZ,double maxMZ, double window,int peaks) {
        this(conf,window, peaks);
        this.minMZ=minMZ;
        this.maxMZ = maxMZ;
    }

    public Denoise(RunConfig conf,double window,int peaks) {
        this.window = window;
        this.peaks = peaks;
    }
    
    
    public Spectra next() {
        Spectra n =m_InnerAcces.next();

        // we try to keep peaks that could be related to the precursor
        double p_mz= n.getPrecurserMZ();
        HashSet<Double> mz = new HashSet<>(7*7);
        
        if (keepPrecursor) {
            // so if we assume that there is a possibility for error 
            // in the MS1 precursor detection then we should keep all peaks that 
            // could belong to the precursor in any charge state and with any number 
            // of missing isotopic peaks
            for (int c=1;c<=7;c++) {
                for (int p =-3;p<0;p++) {
                    mz.add(p_mz+Util.C13_MASS_DIFFERENCE*p/c);
                }
                for (int p =1;p<4;p++) {
                    mz.add(p_mz+Util.C13_MASS_DIFFERENCE*p/c);
                }
            }
        }
        
        if (keepPeaks.size()>0) {
            mz.addAll(keepPeaks);
        }
        
        if (minMZ > 0 || maxMZ<Double.POSITIVE_INFINITY ) {
            return n.cloneTopPeaksRolling(peaks, window, minMZ, maxMZ,mz);
        }
        return n.cloneTopPeaksRolling(peaks, window,mz);
    }

    
    /**
     * @return the minMZ
     */
    public double getMinMZ() {
        return minMZ;
    }

    /**
     * @param minMZ the minMZ to set
     */
    public void setMinMZ(double minMZ) {
        this.minMZ = minMZ;
    }

    /**
     * @return the maxMZ
     */
    public double getMaxMZ() {
        return maxMZ;
    }

    /**
     * @param maxMZ the maxMZ to set
     */
    public void setMaxMZ(double maxMZ) {
        this.maxMZ = maxMZ;
    }

    /**
     * @return the window
     */
    public double getWindow() {
        return window;
    }

    /**
     * @param window the window to set
     */
    public void setWindow(double window) {
        this.window = window;
    }

    /**
     * @return the peaks
     */
    public int getPeaks() {
        return peaks;
    }

    /**
     * @param peaks the peaks to set
     */
    public void setPeaks(int peaks) {
        this.peaks = peaks;
    }
    
 
    /**
     * @return the keepPrecursor
     */
    public boolean KeepPrecursorPeaks() {
        return keepPrecursor;
    }

    /**
     * @param keepPrecursor the keepPrecursor to set
     */
    public void setKeepPrecursorPeaks(boolean keepPrecursor) {
        this.keepPrecursor = keepPrecursor;
    }

    /**
     * @return the keepPeaks
     */
    public HashSet<Double> getKeepPeaks() {
        return keepPeaks;
    }

    /**
     * @param keepPeaks the keepPeaks to set
     */
    public void setKeepPeaks(Collection<Double> keepPeaks) {
        this.keepPeaks = new HashSet<>(keepPeaks);
    }

    /**
     * @param keepPeak refister an m/z value that should not be filtered out by 
     * the noise filter
     */
    public void addKeepPeaks(Double keepPeak) {
        this.keepPeaks.add(keepPeak);
    }

    
}
