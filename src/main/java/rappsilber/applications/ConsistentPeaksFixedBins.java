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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;
import rappsilber.config.AbstractRunConfig;
import rappsilber.config.RunConfig;
import rappsilber.config.RunConfigFile;
import rappsilber.ms.Range;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.dataAccess.SpectraAccess;
import rappsilber.ms.dataAccess.filter.spectrafilter.ScanFilteredSpectrumAccess;
import rappsilber.ms.dataAccess.msm.AbstractMSMAccess;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.statistics.utils.StreamingAverageStdDev;
import rappsilber.utils.CountOccurence;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class ConsistentPeaksFixedBins {

    public static void run(File msm, ToleranceUnit t, ScanFilteredSpectrumAccess filter, PrintStream out, boolean splitByCharge) throws FileNotFoundException, IOException, ParseException {
        AbstractMSMAccess msmiter = AbstractMSMAccess.getMSMIterator(msm, t, 1, null);
        SpectraAccess sa = msmiter;
        if (filter != null) {
            filter.setReader(msmiter);
            sa = filter;
        }
        run(sa, t, out, 120, 1000000.0,0, null, splitByCharge);
    }

    public static void run(SpectraAccess sa, ToleranceUnit t, PrintStream statsOut, double minMZ, double maxMZ, int minCharge, ArrayList<Double> TargetPeaks, boolean splitByCharge) throws FileNotFoundException, IOException {
        run(sa, t, statsOut, null, minMZ, maxMZ, minCharge, TargetPeaks, splitByCharge);
    }

    public static void run(SpectraAccess sa, ToleranceUnit t, PrintStream statsOut, PrintStream peaksOut, double minMZ, double maxMZ, int minCharge, ArrayList<Double> TargetPeaks, boolean splitByCharge) throws FileNotFoundException, IOException {
        run(sa, t, statsOut, peaksOut, minMZ, maxMZ, minCharge, TargetPeaks, splitByCharge,null);
    }
    
    public static void run(SpectraAccess sa, ToleranceUnit t, PrintStream statsOut, PrintStream peaksOut, double minMZ, double maxMZ, int minCharge, ArrayList<Double> TargetPeaks, boolean splitByCharge, String peakClass) throws FileNotFoundException, IOException {
        TreeMap<Double,counter> peaks = new TreeMap<Double, counter>();
        TreeMap<Integer,TreeMap<Double,counter>> chargePeaks = new TreeMap<Integer,TreeMap<Double, counter>>();
        TreeMap<Long,counter> longIndex = new TreeMap<Long, counter>();
        CountOccurence<Integer> countTheCount = new CountOccurence<Integer>();

        TreeMap<Integer,TreeMap<Long,counter>> longIndexCharge = new TreeMap<Integer,TreeMap<Long, counter>>();

        if (TargetPeaks == null) {
            initilizeBins(peaks, minMZ, maxMZ, t);
        } else {
            for (Double mz: TargetPeaks) {
                peaks.put(mz,new counter());
            }
            int c = 0;
            for (Double mz: peaks.keySet()) {
                peaks.get(mz).id = c++;
            }
        }
        
        if (peaksOut != null) {
            for (Double mz : TargetPeaks) {
                peaksOut.print(", F" + mz + ", Error" + mz + ", MZ" + mz + ", BI" + mz + ", MI" + mz);
            }
            peaksOut.println();
        }
        
        int countSpectra =0;
        while (sa.hasNext()) {
            Spectra s = sa.next();
            countSpectra++;
            if (minCharge > 0) {
                if (s.getPrecoursorChargeAlternatives().length >1 || s.getPrecurserCharge()< minCharge) {
                    continue;
                }
            }

            double sMax = s.getMaxIntensity();
//            System.err.println("include : run: " + s.getRun() + "  Scan : " + s.getScanNumber() + "  Max Intes: " + sMax);
            int charge = s.getPrecurserCharge();
            if (s.getPrecoursorChargeAlternatives().length != 1)  {
                charge = -1;
            }

            if (peaksOut != null) {
                peaksOut.print(s.getRun() + ", " + s.getScanNumber());
                double sMaxIntens = s.getMaxIntensity();
                double sMedianIntens = s.getMedianIntensity();
                for (Double mz : TargetPeaks) {
                    SpectraPeak sp = s.getPeakAt(mz);
                    if (sp != null) {
                        peaksOut.print(", 1," + ((sp.getMZ()-mz)/mz*1000000) + "," + (sp.getMZ()) + "," + (sp.getIntensity()/sMaxIntens) + "," + (sp.getIntensity()/sMedianIntens) );
                    } else {
                        peaksOut.print(", 0 , 0 , 0 , 0 , 0 ");
                    }
                }
                if (peakClass != null) {
                    peaksOut.println(","+peakClass);
                } else {
                    peaksOut.println();
                }                
            }

            long peakIndex = 0;
            int foundPeaks = 0;
            for (SpectraPeak sp : s) {
                if (sp.getMZ() <= maxMZ) {
                    TreeMap<Double,counter> cpeaks = null;
                    if (splitByCharge) {
                        cpeaks = chargePeaks.get(charge);
                        if (cpeaks == null) {
                            cpeaks = new TreeMap<Double, counter>();
                            chargePeaks.put(charge, cpeaks);
                            if (TargetPeaks == null) {
                                initilizeBins(cpeaks, minMZ, maxMZ, t);
                            } else {
                                for (Double mz: TargetPeaks) {
                                    cpeaks.put(mz,new counter());
                                }
                                int c = 0;
                                for (Double mz: cpeaks.keySet()) {
                                    cpeaks.get(mz).id = c++;
                                }
                            }
                        }
                    }
                    Double mz = sp.getMZ();
                    Range r = t.getRange(mz);
                    SortedMap<Double,counter> sm = peaks.subMap(r.min,r.max);
                    for (double pmz : sm.keySet()) {
                        counter c = sm.get(pmz);
                        counter cc = null;
                        if (cpeaks != null ) {
                            cc = cpeaks.get(pmz);
                        }   if (!c.flaged) {
                            c.flaged = true;
                            foundPeaks++;
                            if (TargetPeaks != null) {
                                peakIndex += Math.pow(2, c.id);
                            }   c.add(sp.getMZ(),sp.getIntensity(), sMax);
                            if (cpeaks != null) {
                                cc.add(sp.getMZ(),sp.getIntensity(), maxMZ);
                            }
                        }
                    }
                }
            }
            countTheCount.add(foundPeaks);

            for (counter c : peaks.values()) {
                c.flaged = false;
            }
            if (TargetPeaks != null) {
                counter c = longIndex.get(peakIndex);
                if (c == null) {
                    c = new counter();
                    longIndex.put(peakIndex, c);
                }
                c.count++;
                if (splitByCharge) {
                    TreeMap<Long,counter> cli = longIndexCharge.get(charge);
                    if (cli == null) {
                        cli = new TreeMap<Long,counter>();
                        longIndexCharge.put(charge, cli);
                    }
                    c = cli.get(peakIndex);
                    if (c == null) {
                        c = new counter();
                        cli.put(peakIndex, c);
                    }
                    c.count++;
                }
                
            }
            s.free();
        }
            statsOut.println("M/Z,ObserveM/Z, ppm,M/Z StDev,M/Z StDev(ppm), Peaks Found,Relative Peaks Found, Average Absolute Intensity,StDev absolute, Average relative intensity,StDev relative");
            for (Double mz : peaks.keySet()) {
                counter c = peaks.get(mz);
                
                statsOut.println("" + mz + "," + c.getMZ() + ","  + (mz-c.getMZ())/mz*1000000 + "," + c.getMZStDev()+ "," + c.getMZStDev()/(mz/1000000) + ","+  c.count + "," + (c.count/(double)countSpectra) + "," +c.getIntensity() + "," + c.getStDev() + "," + c.getRelativeIntensity() + "," + c.getRelativeStDev());
            }


            if (splitByCharge) {

                for (Integer charge : chargePeaks.keySet()) {
                    statsOut.println("Charge :" + charge);

                    TreeMap<Double,counter> cpeaks = chargePeaks.get(charge);

                    statsOut.println("M/Z,ObserveM/Z,ppm,M/Z StDev,M/Z StDev(ppm), Peaks Found,Relative Peaks Found, Average Absolute Intensity,StDev absolute, Average relative intensity,StDev relative");
                    for (Double mz : cpeaks.keySet()) {
                        counter c = cpeaks.get(mz);
                        statsOut.println("" + mz + "," + c.getMZ() + ","  + (mz-c.getMZ())/mz*1000000 + "," + c.getMZStDev()+ "," + c.getMZStDev()/(mz/1000000) + ","+  c.count + "," + (c.count/(double)countSpectra) + "," +c.getIntensity() + "," + c.getStDev() + "," + c.getRelativeIntensity() + "," + c.getRelativeStDev());
                    }
                }
            }



            if (TargetPeaks != null) {
                long max = 0;
                statsOut.print("peakgroupIndex,group, count, relative");
                if (splitByCharge) {
                    for (Integer id : longIndexCharge.keySet()) {
                        statsOut.print(","+id);
                    }
                }
                statsOut.println();
                HashMap<Long,Double> intToMz = new HashMap<Long, Double>();
                for (Double mz : peaks.keySet()) {
                    counter c  = peaks.get(mz);
                    intToMz.put(c.id, mz);
                    System.err.println(c.id + " -> " + ((long) Math.pow(2, c.id))  + " -> " + mz);
                    if (c.id>max) {
                        max=c.id;
                    }
                }

                for (Long l : longIndex.keySet()) {
                    if (longIndex.get(l).count > 0 ) {
                        String k = "";
                        for (long i=0; i<=max;i++) {
                            if ((l.longValue() & (long) Math.pow(2, i)) != 0) {
                                k += intToMz.get(i) + "|";
                            }
                        }
                        statsOut.print("" + l + "," + k + "," + longIndex.get(l).count + "," + (longIndex.get(l).count/(double)countSpectra));
                        for (Integer id : longIndexCharge.keySet()) {
                            counter c = longIndexCharge.get(id).get(l);
                            if (c== null) {
                                statsOut.print(",");
                            } else {
                                statsOut.print("," + c.count);
                            }
                        }
                        statsOut.println();
                    }
                }

            }


        }

    private static class counter {
        int count;
        static int counterInitialized = 0;
        long id = 0;
        double Intensity;
        double relativeIntensity;
        boolean flaged = false;
        StreamingAverageStdDev StDevMZ = new StreamingAverageStdDev();
        StreamingAverageStdDev StDevIntensityRelative = new StreamingAverageStdDev();
        StreamingAverageStdDev StDevIntensityAbsolute = new StreamingAverageStdDev();

        public counter() {
            count = 0;
            Intensity = 0;
            relativeIntensity = 0;
            id = counterInitialized++;
        }

        public counter(double mz, double intens, double maxIntens) {
            count = 1;
            Intensity = intens;
            relativeIntensity = (intens/maxIntens);
            StDevMZ.addValue(mz);
            StDevIntensityAbsolute.addValue(intens);
            StDevIntensityRelative.addValue(intens/maxIntens);
        }

        public void add(double mz, double intens, double maxIntens) {
            Intensity += intens;
            relativeIntensity += (intens/maxIntens);
            count++;
            StDevMZ.addValue(mz);
            StDevIntensityAbsolute.addValue(intens);
            StDevIntensityRelative.addValue(intens/maxIntens);
        }

        public double getIntensity() {
            if (count == 0) {
                return 0;
            }
            return Intensity / count;
        }

        public double getStDev() {
            if (count == 0) {
                return 0;
            }
            return StDevIntensityAbsolute.stdDev();
        }

        public double getRelativeIntensity() {
            if (count == 0) {
                return 0;
            }
            return relativeIntensity / count;
        }

        public double getRelativeStDev() {
            if (count == 0) {
                return 0;
            }
            return StDevIntensityRelative.stdDev();
        }

        public double getMZ() {
            if (count == 0) {
                return 0;
            }
            return StDevMZ.average();
        }

        public double getMZStDev() {
            if (count == 0) {
                return 0;
            }
            return StDevMZ.stdDev();
        }
        
    }


    public static void initilizeBins(TreeMap<Double,counter> peaks, double minMZ, double maxMZ, ToleranceUnit t) {
        if (t.isRelative()) {
//            for (double i = minMZ;i<maxMZ; i+=2*(t.getMaxRange(i)-i)) {
            for (double i = minMZ; i<maxMZ; i+=(Math.max(0.0001,t.getMaxRange(i)-i))) {
                counter c = new counter();
                peaks.put(i, c);
            }
        } else {
            double d = 1;
            // we are talking about floating point values, therefore adding something like 0.1 is not like realy adding 0.1
            // the to work a bit around that trouble I round the values to the next decimal after the dot.
            // this typically results in a value displayed as the wanted one :)

            // so we have to get the number of digits to round
            double tInv = 1/t.getValue();
            for (d = 1; d<tInv;d *= 10);

            for (double i = minMZ;i<maxMZ; i+=2*(t.getMaxRange(i)-i)) {
                // round the values
                i= Math.round(i *d)/d;
                counter c = new counter();
                peaks.put(i, c);
            }
        }

//        for (double i = 120;i<maxMZ; i=Math.round(i*100.0+1)/100.0) {
//            counter c = new counter();
//            peaks.put(i, c);
//        }
    }



    public static void main(String[] args) throws FileNotFoundException, IOException, ParseException {
        if (args.length == 0) {
            new rappsilber.gui.localapplication.ConsistentPeaks().setVisible(true);
            return;
        }
        RunConfig conf;
        ScanFilteredSpectrumAccess filter = null;
        conf = new AbstractRunConfig() { {setFragmentTolerance(new ToleranceUnit("30ppm")); } };

        String msmFile = args[0];
        File msm = new File(msmFile);
        String csvOut = csvOut = args[1];
        if (args.length == 3)  {
            if (args[1].matches(".*\\.[cC][oO][nN][fF]$")) {

                conf = new RunConfigFile(args[1]);
                System.err.println("Read config from " + args[1]);
            } else {
                filter = new ScanFilteredSpectrumAccess();
                filter.readFilter(new File(args[1]));
                System.err.println("Read filter from " + args[1]);
            }
            csvOut = args[2];
        } else if (args.length == 4) {
            conf = new RunConfigFile(args[1]);
            System.err.println("Read config from " + args[1]);
            filter = new ScanFilteredSpectrumAccess();
            filter.readFilter(new File(args[2]));
            System.err.println("Read filter from " + args[2]);
            csvOut = args[3];
        }

        PrintStream out = new PrintStream(csvOut);


        ToleranceUnit t = conf.getFragmentTolerance();
        System.err.println(t.toString());
        run(msm, t, filter, out,false);

    }
}
