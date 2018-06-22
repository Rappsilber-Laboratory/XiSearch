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
import rappsilber.ms.dataAccess.filter.spectrafilter.ScanFilteredSpectrumAccess;
import rappsilber.ms.dataAccess.SpectraAccess;
import rappsilber.ms.dataAccess.msm.AbstractMSMAccess;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.statistics.utils.StreamingAverageStdDev;
import rappsilber.utils.CountOccurence;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class NeutralLostFixedBins {

    public static void run(File msm, ToleranceUnit t, ScanFilteredSpectrumAccess filter, PrintStream out) throws FileNotFoundException, IOException, ParseException {
        //, boolean splitByCharge
        AbstractMSMAccess msmiter = AbstractMSMAccess.getMSMIterator(msm, t, 1, null);
        SpectraAccess sa = msmiter;
        if (filter != null) {
            filter.setReader(msmiter);
            sa = filter;
        }
        run(sa, t, out, 120, 1000000.0, 0, null);//, splitByCharge);
    }

    public static void run(SpectraAccess sa, ToleranceUnit t, PrintStream out, double minMZ, double maxMZ, int minCharge, ArrayList<Double> TargetPeaks) throws FileNotFoundException, IOException {
        //, boolean splitByCharge
        TreeMap<Double, counter> peaks = new TreeMap<Double, counter>();
        TreeMap<Integer, TreeMap<Double, counter>> chargePeaks = new TreeMap<Integer, TreeMap<Double, counter>>();
        TreeMap<Long, counter> longIndex = new TreeMap<Long, counter>();
        CountOccurence<Integer> countTheCount = new CountOccurence<Integer>();

        TreeMap<Integer, TreeMap<Long, counter>> longIndexCharge = new TreeMap<Integer, TreeMap<Long, counter>>();

        if (TargetPeaks == null) {
            initilizeBins(peaks, minMZ, maxMZ, t);
        } else {
            for (Double mz : TargetPeaks) {
                peaks.put(mz, new counter());
            }
            int c = 0;
            for (Double mz : peaks.keySet()) {
                peaks.get(mz).id = c++;
            }
        }

        while (sa.hasNext()) {
            Spectra is = sa.next();
            if (minCharge > 0) {
                if (is.getPrecoursorChargeAlternatives().length >1 || is.getPrecurserCharge()< minCharge)
                    continue;
            }

            
            Spectra s = is.deChargeDeisotop();
            double sMax = s.getMaxIntensity();
//            System.err.println("include : run: " + s.getRun() + "  Scan : " + s.getScanNumber() + "  Max Intes: " + sMax);
            int charge = is.getPrecurserCharge();
            if (is.getPrecoursorChargeAlternatives().length != 1) {
                charge = -1;
            }


            long peakIndex = 0;
            int foundPeaks = 0;
            double precmz = s.getPrecurserMZ();
            double precZ = s.getPrecurserCharge();
            for (SpectraPeak sp : s) {
                double mzdiff = precmz - sp.getMZ();
                if (mzdiff >= minMZ &&  mzdiff <= maxMZ) {



                    Double mz = sp.getMZ();
                    //System.err.println(sp.toString());
                    Range r = t.getRange(mz, precmz);
                    SortedMap<Double, counter> sm = peaks.subMap(precmz - r.max, precmz - r.min);

                    for (double pmz : sm.keySet()) {
                        counter c = sm.get(pmz);
                        counter cc = null;
//                        if (cpeaks != null) {
//                            cc = cpeaks.get(pmz);
//                        }

                        if (!c.flaged) {

                            c.flaged = true;
                            foundPeaks++;

                            if (TargetPeaks != null) {
                                peakIndex += Math.pow(2, c.id);
                            }

                            c.add(sp.getIntensity(), sMax,mzdiff);
//                            if (cpeaks != null) {
//                                cc.add(sp.getIntensity(), maxMZ);
//                            }
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
//                if (splitByCharge) {
//                    TreeMap<Long, counter> cli = longIndexCharge.get(charge);
//                    if (cli == null) {
//                        cli = new TreeMap<Long, counter>();
//                        longIndexCharge.put(charge, cli);
//                    }
//                    c = cli.get(peakIndex);
//                    if (c == null) {
//                        c = new counter();
//                        cli.put(peakIndex, c);
//                    }
//                    c.count++;
//                }

            }
            s.free();
        }
        
        double cc=0;
        int peakCount = 0;
        for (Double mz : peaks.keySet()) {
            counter c = peaks.get(mz);
            cc+=c.count;
            peakCount++;
        }

        int minCount = Math.max((int) (cc/peakCount/10),2);
        
        out.println("dM/Z , Spectra Found,Average dm/z,StDev dm/z,  Average Absolute Intensity,StDev absolute, Average relative intensity,StDev relative");
        for (Double mz : peaks.keySet()) {            
            counter c = peaks.get(mz);
            if (c.count>=minCount)
                out.println("" + mz + "," + c.count  + "," + c.getMZ() + "," + c.getMZStDev()+ "," + c.getIntensity() + "," + c.getStDev() + "," + c.getRelativeIntensity() + "," + c.getRelativeStDev());
        }


//        if (splitByCharge) {
//
//            for (Integer charge : chargePeaks.keySet()) {
//                out.println("Charge :" + charge);
//
//                TreeMap<Double, counter> cpeaks = chargePeaks.get(charge);
//
//                out.println("M/Z , Peaks Found, Average Absolute Intensity,StDev absolute, Average relative intensity,StDev relative");
//                for (Double mz : cpeaks.keySet()) {
//                    counter c = cpeaks.get(mz);
//                    out.println("" + mz + "," + c.count + "," + c.getIntensity() + "," + c.getStDev() + "," + c.getRelativeIntensity() + "," + c.getRelativeStDev());
//                }
//            }
//        }



        if (TargetPeaks != null) {
            long max = 0;
            out.print("peakIndex, count");
//            if (splitByCharge) {
//                for (Integer id : longIndexCharge.keySet()) {
//                    out.print("," + id);
//                }
//            }
            out.println();
            HashMap<Long, Double> intToMz = new HashMap<Long, Double>();
            for (Double mz : peaks.keySet()) {
                counter c = peaks.get(mz);
                intToMz.put(c.id, mz);
                System.err.println(c.id + " -> " + ((long) Math.pow(2, c.id)) + " -> " + mz);
                if (c.id > max) {
                    max = c.id;
                }
            }

            for (Long l : longIndex.keySet()) {
                if (longIndex.get(l).count > 0) {
                    String k = "";
                    for (long i = 0; i <= max; i++) {
                        if ((l.longValue() & (long) Math.pow(2, i)) != 0) {
                            k += intToMz.get(i) + "|";
                        }
                    }
                    out.print("" + l + "," + k + "," + longIndex.get(l).count);
                    for (Integer id : longIndexCharge.keySet()) {
                        counter c = longIndexCharge.get(id).get(l);
                        if (c == null) {
                            out.print(",");
                        } else {
                            out.print("," + c.count);
                        }
                    }
                    out.println();
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
        StreamingAverageStdDev StDevIntensityRelative = new StreamingAverageStdDev();
        StreamingAverageStdDev StDevIntensityAbsolute = new StreamingAverageStdDev();
        StreamingAverageStdDev StDevMZ = new StreamingAverageStdDev();

        public counter() {
            count = 0;
            Intensity = 0;
            relativeIntensity = 0;
            id = counterInitialized++;
        }

        public counter(double intens, double maxIntens,double mz) {
            count = 1;
            Intensity = intens;
            relativeIntensity = (intens / maxIntens);
            StDevIntensityAbsolute.addValue(intens);
            StDevIntensityRelative.addValue(intens / maxIntens);
            StDevMZ.addValue(mz);
        }

        public void add(double intens, double maxIntens,double mz) {
            Intensity += intens;
            relativeIntensity += (intens / maxIntens);
            count++;
            StDevIntensityAbsolute.addValue(intens);
            StDevIntensityRelative.addValue(intens / maxIntens);
            StDevMZ.addValue(mz);
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

        public double getMZStDev() {
            if (count == 0) {
                return 0;
            }
            return StDevMZ.stdDev();
        }
        public double getMZ() {
            if (count == 0) {
                return 0;
            }
            return StDevMZ.average();
        }
    }

    public static void initilizeBins(TreeMap<Double, counter> peaks, double minMZ, double maxMZ, ToleranceUnit t) {
        if (t.isRelative()) {
//            for (double i = minMZ;i<maxMZ; i+=2*(t.getMaxRange(i)-i)) {
            for (double i = minMZ; i < maxMZ; i += (Math.min(0.0001,t.getMaxRange(i)-i))) {
                counter c = new counter();
                peaks.put(i, c);
            }
        } else {
            double d = 1;
            // we are talking about floating point values, therefore adding something like 0.1 is not like realy adding 0.1
            // the to work a bit around that trouble I round the values to the next decimal after the dot.
            // this typically results in a value displayed as the wanted one :)

            // so we have to get the number of digits to round
            double tInv = 1 / t.getValue();
            for (d = 1; d < tInv; d = d * 10);

            for (double i = minMZ; i < maxMZ; i += 2 * (t.getMaxRange(i) - i)) {
                // round the values
                i = Math.round(i * d) / d;
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
        conf = new AbstractRunConfig() {

            {
                setFragmentTolerance(new ToleranceUnit("30ppm"));
            }
        };

        String msmFile = args[0];
        File msm = new File(msmFile);
        String csvOut = csvOut = args[1];
        if (args.length == 3) {
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
        run(msm, t, filter, out);//, false);

    }
}
