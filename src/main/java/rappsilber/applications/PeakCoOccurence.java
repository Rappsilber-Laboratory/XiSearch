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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.dataAccess.SpectraAccess;
import rappsilber.ms.dataAccess.filter.spectrafilter.ScanFilteredSpectrumAccess;
import rappsilber.ms.dataAccess.msm.AbstractMSMAccess;
import rappsilber.ms.spectra.Spectra;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class PeakCoOccurence {
    private static class OccurenceEntry {
        int found = 0;
        int missed = 0;

        public OccurenceEntry(int found, int missed) {
            this.found = found;
            this.missed = missed;
        }

    }

    private static class CoOccurence extends HashMap<Double,HashMap<Double, OccurenceEntry>>{
        private static final long serialVersionUID = -3581921788051247030L;
        public void found(double mz1, double mz2) {
            HashMap<Double, OccurenceEntry> pc = this.get(mz1);
            if (pc == null) {
                pc = new HashMap<Double, OccurenceEntry>();
                this.put(mz1, pc);
                pc.put(mz2, new OccurenceEntry(1, 0));
            } else {
                OccurenceEntry i = pc.get(mz2);
                if (i == null) {
                    i = new OccurenceEntry(1,0);
                    pc.put(mz2, i);
                } else {
                    i.found++;
                }
            }

        }

        public void missed(double mz1, double mz2) {
            HashMap<Double, OccurenceEntry> pc = this.get(mz1);
            if (pc == null) {
                pc = new HashMap<Double, OccurenceEntry>();
                this.put(mz1, pc);
                pc.put(mz2, new OccurenceEntry(0, 1));
            } else {
                OccurenceEntry i = pc.get(mz2);
                if (i == null) {
                    i = new OccurenceEntry(0,1);
                    pc.put(mz2, i);
                } else {
                    i.missed++;
                }
            }

        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            Double[] mz = this.keySet().toArray(new Double[0]);
            java.util.Arrays.sort(mz);
            // header line
            for (Double d : mz) {
                sb.append(",");
                sb.append(d.toString());
            }
            // datatable
            for (Double mz1 : mz) {
                sb.append(mz1.toString());
                HashMap<Double, OccurenceEntry> line = this.get(mz1);
                for (Double mz2 : mz) {
                    sb.append(",");
                    OccurenceEntry o = line.get(mz2);
                    sb.append(o.found / (o.found + o.missed));
                }
            }
            return sb.toString();
        }

    }

    //peaklist
    TreeSet<Double> TargetPeaks;
    //peak coocurence
    CoOccurence m_CoOccurence = new CoOccurence();
    // tollerance for peak-matching
    ToleranceUnit t = default_t;
    SpectraAccess msm;
    ScanFilteredSpectrumAccess filter = null;
    boolean m_peakCentredBins = true;
    static final ToleranceUnit default_t = new ToleranceUnit("20ppm");

    public void setUpPeakList(SpectraAccess sa) {
        ConsistentPeaks cp = new ConsistentPeaks(false, 100, 500,0, t);
        TreeMap<Double,ConsistentPeaks.Counter> peaks = cp.scanPeaks(sa);
        System.err.println("Peaks : " + peaks.size());
        TargetPeaks = new TreeSet<Double>(peaks.keySet());
        //TargetPeaks.addAll(peaks.keySet());
    }


    public void run() {
        SpectraAccess sa = msm;
        if (filter != null) {
            filter.setReader(msm);
            sa = filter;
        }

        while (sa.hasNext()) {
            Spectra s = sa.next();
            System.err.println("include : run: " + s.getRun() + "  Scan : " + s.getScanNumber());
            for (double mz1 : TargetPeaks) {
                if (s.getPeakAt(mz1) != null) {
                    for (double mz2 : TargetPeaks) {
                        if (s.getPeakAt(mz2) != null) {
                            m_CoOccurence.found(mz1, mz2);
                        } else {
                            m_CoOccurence.missed(mz1, mz2);
                        }
                    }
                }
            }
        }
    }

    int[][] m_foundPeaks;
    int[][] m_missedPeaks;
    double[] m_targetMz;
    public void run2() {
        SpectraAccess sa = msm;
        if (filter != null) {
            filter.setReader(msm);
            sa = filter;
        }
        int i =0;
        m_targetMz = new double[TargetPeaks.size()];
        for (Double d : TargetPeaks) {
            m_targetMz[i++] = d;
        }
        m_foundPeaks = new int[TargetPeaks.size()][TargetPeaks.size()];
        m_missedPeaks = new int[TargetPeaks.size()][TargetPeaks.size()];

        while (sa.hasNext()) {
            Spectra s = sa.next();
            System.err.println("include : run: " + s.getRun() + "  Scan : " + s.getScanNumber());
            for (int id1 = 0 ; id1 < m_missedPeaks.length; id1++) {
                if (s.getPeakAt(m_targetMz[id1]) != null) {
                    for (int id2 = 0 ; id2 < m_missedPeaks.length; id2++) {
                        if (s.getPeakAt(m_targetMz[id2]) != null) {
                            m_foundPeaks[id1][id2]++;
                        } else {
                            m_missedPeaks[id1][id2]++;
                        }
                    }
                }
            }
            s.free();
        }
    }

    public void resultToStream(PrintStream out) {
            //StringBuffer sb = new StringBuffer();
            // header line
            out.print("Relative CoOccurence\n");
            for (Double d : m_targetMz) {
                out.print(",");
                out.print(d.toString());
            }
            out.print("\n");
            // datatable
            for (int mz1 = 0; mz1 < m_targetMz.length;mz1++) {
                out.print(m_targetMz[mz1]);
                for (int mz2 = 0; mz2 < m_targetMz.length;mz2++) {
                    out.print(",");
                    out.print(m_foundPeaks[mz1][mz2] / (m_foundPeaks[mz1][mz2] + (double)m_missedPeaks[mz1][mz2]));
                }
                out.print("\n");
            }
            out.print("\n");
            out.print("\n");
            out.print("Absolute CoOccurence\n");

            for (Double d : m_targetMz) {
                out.print(",");
                out.print(d.toString());
            }
            out.print("\n");
            // datatable
            for (int mz1 = 0; mz1 < m_targetMz.length;mz1++) {
                out.print(m_targetMz[mz1]);
                for (int mz2 = 0; mz2 < m_targetMz.length;mz2++) {
                    out.print(",");
                    out.print(m_foundPeaks[mz1][mz2] + m_missedPeaks[mz1][mz2]);
                }
                out.print("\n");
            }
            out.print("\n");
            out.print("\n");
            out.print("Realative and Absolute CoOccurence\n");

            for (Double d : m_targetMz) {
                out.print(",,");
                out.print(d.toString());
            }
            out.print("\n");
            // datatable
            for (int mz1 = 0; mz1 < m_targetMz.length;mz1++) {
                out.print(m_targetMz[mz1]);
                for (int mz2 = 0; mz2 < m_targetMz.length;mz2++) {
                    out.print(",");
                    out.print(m_foundPeaks[mz1][mz2] / (m_foundPeaks[mz1][mz2] + (double)m_missedPeaks[mz1][mz2]));
                    out.print(",");
                    out.print(m_foundPeaks[mz1][mz2] + m_missedPeaks[mz1][mz2]);
                }
                out.print("\n");
            }

    }



    public static void printUsage(PrintStream out) {
        out.println("--peak-msm=|-p:        msm-file, that is used to define the peaks - to look for in the target-msm");
        out.println("--peak-filter=|-pf:    a csv-file defining which scans in the peak file to consider");
        out.println("--peak-list=|-l :      read list of considered peaks from file");
        out.println("--target-msm=|-t:      the file that should be checked for coocurence of peaks");
        out.println("--target-filter=|-tf:  a csv-file defining which scans in the target file to consider");
        out.println("--tolerance=|-T :      the tolerance used for matching peaks (default is " + default_t.toString()+")");
        out.println("--output=|-o :          where to write the results (default is STDOUT)");
    }

    public static void main(String[] args) throws ParseException {
        PeakCoOccurence po = new PeakCoOccurence();
        String PeakMSM = null;
        String PeakFilter = null;
        String TargetMSM = null;
        String TargetFilter = null;
        String PeakList = null;
        ToleranceUnit Tolerance = new ToleranceUnit(20.0,"ppm");
        PrintStream out = System.out;

        //<editor-fold defaultstate="collapsed" desc="argument parsing">
        {
            boolean argerror = false;
            for (int i = 0 ; i< args.length; i++) {
                String arg = args[i];
                if (arg.startsWith("--tolerance=")){
                    String[] a = arg.split("=",2);
                    Tolerance = new ToleranceUnit(a[1]);
                } else if (arg.startsWith("--target-msm=")){
                    String[] a = arg.split("=",2);
                    TargetMSM = a[1];
                } else if (arg.startsWith("--target-filter=")) {
                    String[] a = arg.split("=",2);
                    TargetFilter = a[1];
                } else if (arg.startsWith("--peeak-msm=")){
                    String[] a = arg.split("=",2);
                    TargetMSM = a[1];
                } else if (arg.startsWith("--peak-filter=")) {
                    String[] a = arg.split("=",2);
                    PeakFilter = a[1];
                } else if (arg.startsWith("--peak-list=")) {
                    String[] a = arg.split("=",2);
                    PeakList = a[1];
                } else if (arg.startsWith("--output=")) {
                    String[] a = arg.split("=",2);
                    try {
                        out = new PrintStream(a[1]);
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(PeakCoOccurence.class.getName()).log(Level.SEVERE, "could not open output-file " + a[1], ex);
                        printUsage(System.err);
                        return;
                    }
                } else if (arg.contentEquals("-T")){
                    i++;
                    Tolerance = new ToleranceUnit(args[i]);
                } else if (arg.contentEquals("-t")){
                    i++;
                    TargetMSM = args[i];
                } else if (arg.contentEquals("-tf")) {
                    i++;
                    TargetFilter = args[i];
                } else if (arg.contentEquals("-p")){
                    i++;
                    PeakMSM = args[i];
                } else if (arg.contentEquals("-pf")) {
                    i++;
                    PeakFilter = args[i];
                } else if (arg.contentEquals("-pl")) {
                    i++;
                    PeakList = args[i];
                } else if (arg.contentEquals("-o")){
                    i++;
                    try {
                        out = new PrintStream(args[i]);
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(PeakCoOccurence.class.getName()).log(Level.SEVERE, "could not open output-file " + args[i], ex);
                        printUsage(System.err);
                        return;
                    }
                } else if (arg.contentEquals("-h") || arg.contentEquals("--help")) {
                    printUsage(System.out);
                } else {
                    System.err.println("Unknown argument : " + arg);
                    argerror = true;
                }

            }
            if (argerror) {
                printUsage(System.err);
                return;
            }
        }

        //</editor-fold>

        if (PeakMSM == null && PeakList == null) {
            System.err.println("Neither msm-file given for the target peaks (-p) nor a list of peaks given (-pl)");
        }
        if (TargetMSM == null) {
            System.err.println("No msm-file given for the target peaks (-t)");
        }

        if ((PeakMSM == null  && PeakList == null) || TargetMSM == null) {
            printUsage(System.err);
            return;
        }

        SpectraAccess peaks = null;
        if (PeakList == null) {
            // create the spectra-access instances to read the data
            try {
                peaks = AbstractMSMAccess.getMSMIterator(PeakMSM, Tolerance,1, null);
                //peaks = new MSMIterator(new File(PeakMSM), Tolerance);
                if (PeakFilter != null) {
                    ScanFilteredSpectrumAccess pf = new ScanFilteredSpectrumAccess();
                    pf.readFilter(new File(PeakFilter));
                    pf.setReader(peaks);
                    peaks = pf;
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(PeakCoOccurence.class.getName()).log(Level.SEVERE, "File not found", ex);
                printUsage(System.err);
                return;
            } catch (IOException ex) {
                Logger.getLogger(PeakCoOccurence.class.getName()).log(Level.SEVERE, "Error accessing file", ex);
                printUsage(System.err);
                return;
            }
        }

        SpectraAccess target;
        try {
            target = AbstractMSMAccess.getMSMIterator(TargetMSM, Tolerance,1, null);
            //target = new MSMIterator(new File(TargetMSM), Tolerance);
            if (TargetFilter != null) {
                ScanFilteredSpectrumAccess tf = new ScanFilteredSpectrumAccess();
                tf.readFilter(new File(TargetFilter));
                tf.setReader(target);
                target = tf;
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PeakCoOccurence.class.getName()).log(Level.SEVERE, "File not found", ex);
            return;
        } catch (IOException ex) {
            Logger.getLogger(PeakCoOccurence.class.getName()).log(Level.SEVERE, "Error accessing file", ex);
            return;
        }

        // setup the run
        po.t = Tolerance;
        if (peaks != null) {
            po.setUpPeakList(peaks);
        } else {
            po.TargetPeaks = new TreeSet<Double>();
            // read in the peaklist from csv
            try {
                BufferedReader br = new BufferedReader(new FileReader(PeakList));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] fields = line.split(",",2);
                    if (fields[0].matches("[0-9\\.]+")) {
                        po.TargetPeaks.add(new Double(fields[0]));
                    }
                }
                br.close();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(PeakCoOccurence.class.getName()).log(Level.SEVERE, null, ex);
                return;
            } catch (IOException ex) {
                Logger.getLogger(PeakCoOccurence.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
        }
        po.msm = target;
        //po.run();
        //out.println(po.m_CoOccurence.toString());
        po.run2();
        //out.println(po.resultToString());
        po.resultToStream(out);
    }
}
