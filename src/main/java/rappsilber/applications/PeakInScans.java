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
import java.util.Collection;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.dataAccess.SpectraAccess;
import rappsilber.ms.dataAccess.filter.spectrafilter.ScanFilteredSpectrumAccess;
import rappsilber.ms.dataAccess.msm.AbstractMSMAccess;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class PeakInScans {
    static final ToleranceUnit default_t = new ToleranceUnit("10ppm");
    //peaklist
    private TreeSet<Double> TargetPeaks;
    private ToleranceUnit tolerance = default_t;
    private PrintStream   out = System.out;
    ScanFilteredSpectrumAccess spectrumInformation = new ScanFilteredSpectrumAccess();

    double[] m_targetMz;


    public void run(SpectraAccess msm) {
        SpectraAccess sa = msm;
        int i =0;
        m_targetMz = new double[getTargetPeaks().size()];
        for (Double d : getTargetPeaks()) {
            m_targetMz[i++] = d;
        }
        getOut().print("Run,Scan");
        for (int id2 = 0 ; id2 < m_targetMz.length; id2++) {
            getOut().print(", F" + m_targetMz[id2] + ", Error" + m_targetMz[id2] + ", MZ" + m_targetMz[id2] + ", BI" + m_targetMz[id2] + ", MI" + m_targetMz[id2]);
        }
        if (spectrumInformation.getSelectedScanCount() != 0) {
            getOut().println ("," + spectrumInformation.getExtraHeader());
        } else {
            getOut().println();
        }
        
        if (spectrumInformation.getSelectedScanCount() == 0) {
            while (sa.hasNext()) {
                Spectra s = sa.next();
                //System.err.println("include : run: " + s.getRun() + "  Scan : " + s.getScanNumber());
                scanToPeaks(s, "");
                s.free();
            }
        } else {
            while (sa.hasNext()) {
                Spectra s = sa.next();
                String extra = spectrumInformation.getInformation(s);
                //System.err.println("include : run: " + s.getRun() + "  Scan : " + s.getScanNumber());
                if (extra != null) {
                    scanToPeaks(s, extra);
                }
                s.free();
            }
        }
        System.err.println("Spectra read:" + sa.countReadSpectra());
    }

    private void scanToPeaks(Spectra s, String extra) {
        getOut().print(s.getRun() + ", " + s.getScanNumber());
        double sMaxIntens = s.getMaxIntensity();
        double sMedianIntens = s.getMedianIntensity();
        for (int id2 = 0 ; id2 < m_targetMz.length; id2++) {
            SpectraPeak sp = s.getPeakAt(m_targetMz[id2]);
            if (sp != null) {
                getOut().print(", 1," + ((sp.getMZ()-m_targetMz[id2])/m_targetMz[id2]*1000000) + "," + (sp.getMZ()) + "," + (sp.getIntensity()/sMaxIntens) + "," + (sp.getIntensity()/sMedianIntens) );
            } else {
                getOut().print(", 0 , 0 , 0 , 0 , 0 ");
            }
        }
        getOut().println("," + extra);
    }


    public void setUpPeakList(SpectraAccess sa) {
        ConsistentPeaks cp = new ConsistentPeaks(false, 100, 500,0, getTolerance());
        TreeMap<Double,ConsistentPeaks.Counter> peaks = cp.scanPeaks(sa);
        System.err.println("Peaks : " + peaks.size());
        setTargetPeaks(new TreeSet<Double>(peaks.keySet()));
        //TargetPeaks.addAll(peaks.keySet());
    }


    public static void printUsage(PrintStream out) {
        out.println("--peak-msm=|-p:        msm-file, that is used to define the peaks - to look for in the target-msm");
        out.println("--peak-filter=|-pf:    a csv-file defining which scans in the peak file to consider");
        out.println("--peak-list=|-pl :     read list of considered peaks from file");
        out.println("--target-msm=|-t:      the file that should be checked for coocurence of peaks");
        out.println("--target-filter=|-tf:  a csv-file defining which scans in the target file to consider");
        out.println("--tolerance=|-T :      the tolerance used for matching peaks (default is " + default_t.toString()+")");
        out.println("--output=|-o :         where to write the results (default is STDOUT)");
        out.println("--verbose|-v :         be more verbose");
    }


    public static void main(String[] args) throws ParseException {
        PeakInScans po = new PeakInScans();
        String PeakMSM = null;
        String PeakFilter = null;
        String TargetMSM = null;
        String TargetFilter = null;
        String PeakList = null;
        ToleranceUnit Tolerance = default_t;
        PrintStream out = System.out;
        boolean verbose = false;


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
                } else if (arg.equals("--verbose") || arg.equals("-v")) {
                    verbose = true;
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
                        po.setOut(new PrintStream(args[i]));
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
            if (verbose) {
                System.err.println("defining target peaks from: " + PeakList);
            }
            
            // create the spectra-access instances to read the data
            try {
                peaks = AbstractMSMAccess.getMSMIterator(PeakMSM, Tolerance, 1, null);
                //peaks = new MSMIterator(new File(PeakMSM), Tolerance);
                
                if (PeakFilter != null) {
                    if (verbose) {
                        System.err.println("setup filter for the peaks -mgf-file  ");
                    }
                    ScanFilteredSpectrumAccess pf = new ScanFilteredSpectrumAccess();
                    pf.readFilter(new File(PeakFilter));
                    pf.setReader(peaks);
                    System.err.println(pf.getSelectedScanCount() + " scans selected for exrraction of peaks");
                    peaks = pf;
                } else {
                    
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
        ScanFilteredSpectrumAccess tf = po.spectrumInformation;
        
        SpectraAccess target;
        try {
            if (verbose) {
                System.err.println("will look for peaks in: " + TargetMSM);
            }
            target = AbstractMSMAccess.getMSMIterator(TargetMSM, Tolerance, 1, null);

            //target = new MSMIterator(new File(TargetMSM), Tolerance);
            if (TargetFilter != null) {
                if (verbose) {
                    System.err.println("filtering target mgf by: " + TargetFilter );
                }
//                ScanFilteredSpectrumAccess tf = new ScanFilteredSpectrumAccess();
                tf.readFilter(new File(TargetFilter));
//                tf.setReader(target);
//                target = tf;
                if (verbose) {
                    System.err.println("selected " + tf.getSelectedScanCount() + " scans");
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PeakCoOccurence.class.getName()).log(Level.SEVERE, "File not found", ex);
            return;
        } catch (IOException ex) {
            Logger.getLogger(PeakCoOccurence.class.getName()).log(Level.SEVERE, "Error accessing file", ex);
            return;
        }

        po.setTolerance(Tolerance);
        if (peaks != null) {
            po.setUpPeakList(peaks);
        } else {
            po.setTargetPeaks(new TreeSet<Double>());
            // read in the peaklist from csv
            try {
                if (verbose) {
                    System.err.println("reading target peaks from: " + PeakList);
                }
                BufferedReader br = new BufferedReader(new FileReader(PeakList));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] fields = line.split(",",2);
                    if (fields[0].matches("\"?[0-9\\.]+\"?")) {
                        po.getTargetPeaks().add(Double.valueOf(fields[0].replace("\"","")));
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
        //po.run();
        //out.println(po.m_CoOccurence.toString());
        po.run(target);

    }

    /**
     * @return the TargetPeaks
     */
    public TreeSet<Double> getTargetPeaks() {
        return TargetPeaks;
    }

    /**
     * @param TargetPeaks the TargetPeaks to set
     */
    public void setTargetPeaks(TreeSet<Double> TargetPeaks) {
        this.TargetPeaks = TargetPeaks;
    }

    /**
     * @param TargetPeaks the TargetPeaks to set
     */
    public void setTargetPeaks(Collection<Double> TargetPeaks) {
        this.TargetPeaks = new TreeSet<Double>(TargetPeaks);
    }
    
    /**
     * @return the tolerance
     */
    public ToleranceUnit getTolerance() {
        return tolerance;
    }

    /**
     * @param tolerance the tolerance to set
     */
    public void setTolerance(ToleranceUnit tolerance) {
        this.tolerance = tolerance;
    }

    /**
     * @return the out
     */
    public PrintStream getOut() {
        return out;
    }

    /**
     * @param out the out to set
     */
    public void setOut(PrintStream out) {
        this.out = out;
    }

}
