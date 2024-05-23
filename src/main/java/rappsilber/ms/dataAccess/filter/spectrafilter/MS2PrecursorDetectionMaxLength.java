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
import java.util.HashMap;
import java.util.HashSet;
import static rappsilber.config.AbstractRunConfig.getBoolean;
import rappsilber.config.RunConfig;
import rappsilber.ms.dataAccess.AbstractStackedSpectraAccess;
import rappsilber.ms.spectra.Spectra;
import static rappsilber.ms.spectra.Spectra.DEFAULT_ISOTOP_DETECTION;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.SpectraPeakCluster;
import rappsilber.ms.spectra.SpectraPeakClusterList;
import rappsilber.ms.statistics.utils.UpdateableInteger;
import rappsilber.utils.AvergineUtil;
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
public class MS2PrecursorDetectionMaxLength  extends AbstractStackedSpectraAccess {
    private double window=3;
    private double countCorrectedMZ=0;
    private ArrayList<UpdateableInteger> mzDiffCorrected = new ArrayList<>(10);
    private HashMap<Integer,ArrayList<UpdateableInteger>> chargeCorrected = new HashMap<>();
    private double countCorrectedCharge=0;
    private double countFound=0;
    private double count=0;
    private double artifact_peak_detection_ratio = 3;
    private int detect_artifarct_peaks = 1;
    private boolean detectCharge = false;
    private Integer maxCharge = null;

    public MS2PrecursorDetectionMaxLength(RunConfig conf) {
        for (int i=0;i<10;i++) {
            mzDiffCorrected.add(new UpdateableInteger(0));
            ArrayList<UpdateableInteger> targetCharge = new ArrayList<>(10);
            for (int c=0;c<10;c++) {
                targetCharge.add(new UpdateableInteger(0));
            }
            chargeCorrected.put(i,targetCharge);
        }
    }

    
    public MS2PrecursorDetectionMaxLength(RunConfig conf,String settings) {
        this(conf);
        String[] set = settings.split(";");
        for (String s: set) {
            String[] args = s.split(":");
            String a = args[0].toLowerCase().trim();
            String v = args[1].toLowerCase().trim();
            if (a.contentEquals("window")) {
                window = Double.parseDouble(v);
            } else if (a.contentEquals("charge")) {
                detectCharge = getBoolean(v, true);
            } else if (a.contentEquals("maxcharge")) {
                maxCharge = Integer.getInteger(v, maxCharge);
            }

        }
    }
    public MS2PrecursorDetectionMaxLength(RunConfig conf,double window, int artifactpeaks, double artfactratio, boolean detectCharge) {
        this(conf);
        this.window = window;
        this.artifact_peak_detection_ratio = artfactratio;
        this.detect_artifarct_peaks = artifactpeaks;
        this.detectCharge = detectCharge;
        this.detectCharge = detectCharge;
    }
    
    
    public Spectra next() {
        Spectra  i =m_InnerAcces.next(); 
        // do we have a precursor peak?
        double precMZ =i.getPrecurserMZ();
        int precZ =i.getPrecurserCharge();
        
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
            int peaks = 0;
            double mz = Double.MAX_VALUE;
            SpectraPeakCluster  assumed_precursor = null;
            // find longest cluster
            for (SpectraPeakCluster  spc : spcl) {
                if (spc.hasPeakAt(precPeakMZ)) {
                    if ((spc.size() > peaks || 
                            (spc.size() == peaks && spc.getMZ()< mz)) && 
                            ((spc.getCharge()<precZ && detectCharge ) ||
                                spc.getCharge()==precZ)) {
                        peaks = spc.size();
                        mz = spc.getMZ();
                        assumed_precursor = spc;
                    }
                }
                 
            }
            
            // only consider cluster that actually cover the precusor
            if (assumed_precursor != null) {
                // does it indicate the same m/z as the original precursor?
                //if (s.getTolearance().compare(i.getPrecurserMZ(), assumed_precursor.getMZ()) != 0 ) {
                    
                    // assume that the precursor defined before actually belongs to 
                    // the peptide fragmented then the actual monoisotopic peak 
                    // should be n*(C13-C2) Da away
                    int c = assumed_precursor.getCharge();
                    double diff = (assumed_precursor.getMZ()-i.getPrecurserMZ())*c/Util.C13_MASS_DIFFERENCE;
                    long C13_count = Math.round(diff);
                    if (C13_count != 0) {
                        pmz.add(precMZ+(Util.C13_MASS_DIFFERENCE*C13_count)/c);
                    }
                    // see if we should assume that the first peaks are artifarcts of some kind?
                    if (detect_artifarct_peaks > 0) {
                        for (int p = detect_artifarct_peaks - 1; p>=0 ; p--) {
                            // if there are enough peaks left to make a cluster
                            if (assumed_precursor.size()>p+3 && p != -C13_count) {
                                double pi = assumed_precursor.get(p).getIntensity();
                                double pin = assumed_precursor.get(p+1).getIntensity();
                                
                                double avergine_pi = AvergineUtil.relativeHight(assumed_precursor.getMZ(), p);
                                double avergine_pin = AvergineUtil.relativeHight(assumed_precursor.getMZ(), p + 1);
                                double assumed_next_peak = pi/avergine_pi*avergine_pin;
                                // does this look like an artifact
                                if (assumed_next_peak < pin/artifact_peak_detection_ratio) {
                                    // also considere the next peak as a possible precusor
                                    if (C13_count + p != 0) {
                                        pmz.add(precMZ+(Util.C13_MASS_DIFFERENCE*(C13_count+p))/c);
                                    }
                                }
                            }
                        }
                    }
                //}
                // do we see a different charge state?
                if (i.getPrecurserCharge() != assumed_precursor.getCharge() ) {
                    pc.add(assumed_precursor.getCharge());
                }
            }
            
            if (pmz.size() >0) {
                countCorrectedMZ++;
                for (double cmz : pmz) {
                    mzDiffCorrected.get(Math.min((int)(Math.round((precMZ-cmz)*i.getPrecurserCharge())), 9)).value++;
                }
                i.setAdditionalMZ(pmz);
            }
            if (pc.size() >0) {
                countCorrectedCharge++;
                for (int cz : pc) {
                    int origCharge = Math.min(precZ, 9);
                    int correctedCharge = Math.min(cz, 9);
                    chargeCorrected.get(origCharge).get(correctedCharge).value++;
                }
                i.setAdditionalCharge(pc);
            }

        }

        if (count >0 && count % 1000 == 0) {
            report();
        }

        return i;
    }

    protected void report() {
        System.err.println("Spectra seen:"+count+"\nPrecursor seen:"+
                countFound +"\nm/z corrected:"+countCorrectedMZ
                +"\ncharge corrected:"+countCorrectedCharge);
        for (int o=1;o<10;o++) {
            System.err.println("m/z offset "+ o + ":"+mzDiffCorrected.get(o).value);
        }
        System.err.println("rc\tp0\tp1\tp2\tp3\tp4\tp5\tp6\tp7\tp8\tp9");
        for (int c=1;c<10;c++) {
            System.err.print(c);
            ArrayList<UpdateableInteger> predict = chargeCorrected.get(c);
            for (int pc=1;pc<10;pc++) {
                System.err.print("\t "+ predict.get(pc).value);
            }
            System.out.println("");
        }
    }

    @Override
    public void close() {
        super.close(); //To change body of generated methods, choose Tools | Templates.
        report();
    }

    
 
}
