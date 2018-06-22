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
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
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

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class PeaksLookup {



    public static void run(File msm, ToleranceUnit t, ScanFilteredSpectrumAccess filter, double[] MZ, PrintStream out) throws FileNotFoundException, IOException, ParseException {
        TreeMap<Double,ArrayList<Object[]>> peaks = new TreeMap<Double, ArrayList<Object[]>>();

        AbstractMSMAccess msmiter = AbstractMSMAccess.getMSMIterator(msm, t, 1, null);
        SpectraAccess sa = msmiter;
        if (filter != null) {
            filter.setReader(msmiter);
            sa = filter;
        }

        for (double mz : MZ) {
            peaks.put(mz, new ArrayList<Object[]>());
        }


        while (sa.hasNext()) {
            Spectra s = sa.next();
            double sMax = s.getMaxIntensity();
            System.err.println("include : run: " + s.getRun() + "  Scan : " + s.getScanNumber() + "  Max Intes: " + sMax);
            for (SpectraPeak sp : s) {
                double mz = sp.getMZ();
                //System.err.println(sp.toString());
                Range r = t.getRange(mz);
                Map<Double, ArrayList<Object[]>> map = peaks.subMap(r.min, r.max);
                if (map.size() > 0) {
                    ArrayList<Object[]> c = map.values().iterator().next();
                    c.add(new Object[]{sp.getMZ(),sp.getIntensity(), s.getRun(), s.getScanNumber()});
                }
            }
        }

        out.println(t);
        out.println("M/Z , minimum looked up, maximum lookup, minimum found, maximumfound, median, average");

        for (Double baseMZ : peaks.keySet()) {
            ArrayList<Object[]> c = peaks.get(baseMZ);

            if ( c.size() > 0) {
                Collections.sort(c, new Comparator<Object[]>(){

                    @Override
                    public int compare(Object[] o1, Object[] o2) {
                        return Double.compare((Double)o1[0], (Double)o2[0]);
                    }
                });
                double median;
                int medianID = (int)Math.floor(c.size()/2.0);

                if ((double)(int)(c.size()/2.0) != c.size()/2.0) {
                    median = (((Double)c.get(medianID)[0]) + ((Double)c.get(medianID)[0]))/2.0;
                } else
                    median = (Double)c.get(medianID)[0];

                double average=0;

                for (Object[] mz : c) {
                    average+=(Double)mz[0];
                }

                average /= c.size();
                Range r = t.getRange(baseMZ);
                out.println("" + baseMZ + ", " + r.min +", " + r.max +
                        ", " + c.get(0) +", " + c.get(c.size()-1) + ", " + median + ", " + average);
            } else {
                out.println("" + baseMZ );
            }

        }

        for (Double baseMZ : peaks.keySet()) {
            out.println();
            out.println(baseMZ);
            ArrayList<Object[]> c = peaks.get(baseMZ);
            if ( c.size() > 0) {
                for (Object mz[]: c) {
                    out.println(baseMZ + "," + mz[0] + "," + mz[1] + "," + mz[2] + "," + mz[3]);
                }
            }
        }

    }


//    public static void main(String[] args) throws FileNotFoundException, IOException, ParseException {
//        if (args.length == 0) {
//            new rappsilber.gui.localapplication.ConsistentPeaks().setVisible(true);
//            return;
//        }
//        RunConfig conf;
//        ScanFilteredSpectrumAccess filter = null;
//        conf = new AbstractRunConfig() { {setFragmentTolerance(new ToleranceUnit("30ppm")); } };
//
//        String msmFile = args[0];
//        File msm = new File(msmFile);
//        String csvOut = csvOut = args[1];
//        if (args.length == 3)  {
//            if (args[1].matches(".*\\.[cC][oO][nN][fF]$")) {
//
//                conf = new RunConfigFile(args[1]);
//                System.err.println("Read config from " + args[1]);
//            } else {
//                filter = new ScanFilteredSpectrumAccess();
//                filter.readFilter(new File(args[1]));
//                System.err.println("Read filter from " + args[1]);
//            }
//            csvOut = args[2];
//        } else if (args.length == 4) {
//            conf = new RunConfigFile(args[1]);
//            System.err.println("Read config from " + args[1]);
//            filter = new ScanFilteredSpectrumAccess();
//            filter.readFilter(new File(args[2]));
//            System.err.println("Read filter from " + args[2]);
//            csvOut = args[3];
//        }
//
//        PrintStream out = new PrintStream(csvOut);
//
//
//        ToleranceUnit t = conf.getFragmentTolerance();
//        System.err.println(t.toString());
//        run(msm, t, filter, out);
//
//    }
}
