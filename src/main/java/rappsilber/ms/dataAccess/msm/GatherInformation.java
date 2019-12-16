/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rappsilber.ms.dataAccess.msm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.spectra.Spectra;
import org.rappsilber.utils.DoubleArrayList2D;
import org.rappsilber.utils.RArrayUtils;
import rappsilber.config.AbstractRunConfig;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.dataAccess.AbstractSpectraAccess;
import rappsilber.ms.dataAccess.AbstractStackedSpectraAccess;
import rappsilber.ms.dataAccess.BufferedSpectraAccess;
import rappsilber.ms.dataAccess.SpectraAccess;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class GatherInformation extends AbstractStackedSpectraAccess  {

    
    public class Corrector {
        double relativeOffset;
        int count;

        public Corrector(double relativeOffset) {
            this.relativeOffset = relativeOffset;
        }
        
        public void correct(Spectra s) {
            for (SpectraPeak sa : s) {
                sa.setMZ(sa.getMZ()+sa.getMZ()*relativeOffset);
            }
            s.rebuildPeakTree();
            if (isCorrectMS1())
                s.setPrecurserMZ(s.getPrecurserMZ()+s.getPrecurserMZ()*relativeOffset);
        }
    }
    
    protected Corrector noCorrection = new Corrector(0) {
        @Override
        public void correct(Spectra s) {
            
        }
    };

    /** the peaks to use for calibration */
    private double[] peaks;

    /** some weighting for the impact of the peak*/
    private double[] peakWeight;

    /**  optional some name */
    private String[] peakDescription;
    
//    /** where to read from */
//    private SpectraAccess inner;
    
    /** a mz corrector per run */
    private HashMap<String, Corrector> runCalibration;
    
    /** by default precursor values are not touched */
    private boolean correctMS1 = false;
    
    /** only correct errors larger then this. */
    private double cutoff = 2/1000000;
    
    /** tolerance used to look for peaks that are used for calibration */
    private ToleranceUnit preCalibrationTolerance = new ToleranceUnit("50ppm");

    private double m_MaxPrecursorMass;    
    private int m_scanCount;

    
    /**
     * given some >4K human proteins and a fully tryptic insilico digest what 
     * are the expected y1 and y2 ions. y1 and y2 are assigned a weight of 1,
     * while the y2 ions are assigned a weight relative to the expected 
     * frequency of occurence
     */
    public void setupPeaksTryptic() {
        AminoAcid[] y1 = new AminoAcid[]{AminoAcid.R, AminoAcid.K};
        //y2 XK
        AminoAcid[] y2XK = new AminoAcid[]{
            AminoAcid.A,
            AminoAcid.N,
            AminoAcid.C,
            AminoAcid.P,
            AminoAcid.D,
            AminoAcid.Q,
            AminoAcid.R,
            AminoAcid.E,
            AminoAcid.F,
            AminoAcid.S,
            AminoAcid.T,
            AminoAcid.G,
            AminoAcid.H,
            AminoAcid.V,
            AminoAcid.I,
            AminoAcid.W,
            AminoAcid.L,
            AminoAcid.Y,
            AminoAcid.M};
        double[] weighty2XK = new double[]{
            0.0625714856547694,
            0.0404392140655911,
            0.0197452512810215,
            0.0587877278100372,
            0.0475561442485473,
            0.0579112810802622,
            0.0697001784630354,
            0.0996817124671524,
            0.0338637796966405,
            0.0764923754481157,
            0.0468971000032723,
            0.0694025013209751,
            0.0220011533367967,
            0.0619515771425149,
            0.0486964502123307,
            0.015760177012033,
            0.113395630783349,
            0.0266875377509728,
            0.0284587222225827};

        AminoAcid[] y2XR = new AminoAcid[]{
            AminoAcid.N,
            AminoAcid.A,
            AminoAcid.P,
            AminoAcid.C,
            AminoAcid.Q,
            AminoAcid.D,
            AminoAcid.E,
            AminoAcid.S,
            AminoAcid.F,
            AminoAcid.T,
            AminoAcid.G,
            AminoAcid.H,
            AminoAcid.V,
            AminoAcid.I,
            AminoAcid.W,
            AminoAcid.K,
            AminoAcid.L,
            AminoAcid.Y,
            AminoAcid.M
        };

        double[] weighty2XR = new double[]{
            0.036600949365738,
            0.0751304070295788,
            0.0694590131911698,
            0.0249851140879364,
            0.0620580664631866,
            0.0486774072619212,
            0.0792613496705871,
            0.0860095853790308,
            0.0375547666934852,
            0.0497423689365836,
            0.0768655062316789,
            0.0310952421480519,
            0.0594338882136803,
            0.0469015906742538,
            0.015668002712065,
            0.0608885790614174,
            0.12281262225988,
            0.0313802789926058,
            0.0220762109928878
        };
        double[] peakY12 = new double[2+y2XR.length + y2XK.length];
        double[] peakY12Weight = new double[peakY12.length];
        String[] seqY12 =  new String[peakY12.length];

        
        for (int i=0;i<2;i++) {
            peakY12[i] = y1[i].mass +18.01056027 + Util.PROTON_MASS;
            peakY12Weight[i] = 1;
            seqY12[i] = "y1 " + y1[i].SequenceID;
        }
        for (int i=0;i<y2XK.length;i++) {
            peakY12[2+i] = y2XK[i].mass + AminoAcid.K.mass + 18.01056027 + Util.PROTON_MASS;
            peakY12Weight[2+i] = weighty2XK[i]/10;
            seqY12[2+i] = "y2 " + y2XK[i].SequenceID + "K";
        }

        for (int i=0;i<y2XR.length;i++) {
            peakY12[2+ y2XK.length + i] = y2XR[i].mass + AminoAcid.R.mass + 18.01056027 + Util.PROTON_MASS;
            peakY12Weight[2+ y2XK.length + i] = weighty2XR[i]/10;
            seqY12[2+ y2XK.length + i] = "y2 " + y2XR[i].SequenceID + "R";
        }
        
        this.addPeaks(peakY12,peakY12Weight,seqY12);
    }
    
    
    public void setupPeak445(double weight) {
        double[] p = new double[]{445.120024701879};
        double[] pweight = new double[]{weight};
        String[] pname = new String[] {"445"};
        this.addPeaks(p,pweight,pname);
    }
    
    public void readPeakErrors() throws IOException {
        HashMap<String,DoubleArrayList2D> errors =  new HashMap<>();
        Spectra s;
        BufferedSpectraAccess bsa = new BufferedSpectraAccess(m_InnerAcces, 1000);
        ToleranceUnit ta = getPreCalibrationTolerance();
        m_MaxPrecursorMass = 0;
        m_scanCount =0;
        while (bsa.hasNext()) {
            s = bsa.next();
            m_scanCount ++;
            if (s.getPrecurserMass() > m_MaxPrecursorMass) {
                m_MaxPrecursorMass = s.getPrecurserMass();
            }
            String r = s.getRun();
            DoubleArrayList2D e = errors.get(r);
            if (e == null) {
                e = new DoubleArrayList2D(3);
                errors.put(r, e);
            }
            for (int i = 0; i< getPeaks().length;i++) {
                double mz = getPeaks()[i];
                
                SpectraPeak sp = s.getPeakAt(mz,ta);
                if (sp != null) {
                    double[] peakError = new double[3];
                    peakError[0] = (sp.getMZ()-mz)/mz;
                    peakError[1] = getPeakWeight()[i] * sp.getIntensity();
                    peakError[2] = s.getScanNumber();
                    e.add(peakError);
                }
            }
        }
        HashMap<String, Corrector> runCalibration =  new HashMap<String, Corrector>();
        setRunCalibration(runCalibration);
        double errorsumall = 0;
        double weightsumall = 0;
        int countall = 0;
        
        for (Map.Entry<String,DoubleArrayList2D> e:  errors.entrySet()) {
            String run = e.getKey();
            DoubleArrayList2D er = e.getValue();
            ArrayList<Double> runerrors = new ArrayList<>(er.size());
            ArrayList<Double> runweights = new ArrayList<>(er.size());
            double weightsum = 0;
            double errorsum = 0;
            
            for (int i = 0 ; i< er.size(); i++) {
                double pw = er.getDouble(i, 1);
                double pe = er.getDouble(i, 0);
                double we = pe * pw;
                runerrors.add(pe);
                runweights.add(pw);
                errorsumall += pe*pw;
                weightsumall += pw;
                countall++;
            }
            
            Corrector c = new Corrector(Util.weightedMedian(runerrors, runweights));
            runCalibration.put(run + " wm", c);
            c.count = er.size();
            
        }
        Corrector all = new Corrector(errorsumall/weightsumall);
        all.count = countall;
        runCalibration.put("all", all);
//        for (Map.Entry<String,Corrector> e: runCalibration.entrySet()) {
//            if (e.getValue().count<100) {
//                e.setValue(all);
//            }
//        }
        m_InnerAcces.restart();
    }
    
    
    
    

    @Override
    public Iterator<Spectra> iterator() {
        return this;
    }

    @Override
    public Spectra current() {
        return m_InnerAcces.current();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int countReadSpectra() {
        return m_InnerAcces.countReadSpectra();
    }

    @Override
    public SequenceList getSequences() {
        return m_InnerAcces.getSequences();
    }

    @Override
    public void setSequences(SequenceList sequences) {
        m_InnerAcces.setSequences(sequences);
    }

    @Override
    public boolean canRestart() {
        return m_InnerAcces.canRestart();
    }

    @Override
    public void restart() throws IOException {
        m_InnerAcces.restart();
    }

    @Override
    public void close() {
        m_InnerAcces.close();
    }

    @Override
    public boolean hasNext() {
        return m_InnerAcces.hasNext();
    }

    @Override
    public Spectra next() {
        Spectra n = m_InnerAcces.next();
        Corrector c = getRunCalibration().get(n.getRun());
        if (c!= null)
            c.correct(n);
        return n;
    }
    
    public void parseArgs(String args) {
        
    }
    

    /**
     * the peaks to use for calibration
     * @return the peaks
     */
    public double[] getPeaks() {
        return peaks;
    }

    /**
     * the peaks to use for calibration
     * @param peaks the peaks to set
     */
    public void setPeaks(double[] peaks) {
        this.peaks = peaks;
    }

    /**
     * some weighting for the impact of the peak
     * @return the peakWeight
     */
    public double[] getPeakWeight() {
        return peakWeight;
    }

    /**
     * some weighting for the impact of the peak
     * @param peakWeight the peakWeight to set
     */
    public void setPeakWeight(double[] peakWeight) {
        this.peakWeight = peakWeight;
    }

    /**
     * optional some name
     * @return the peakDescription
     */
    public String[] getPeakDescription() {
        return peakDescription;
    }
    
    public void addPeaks(double[] peaks, double[] weights, String[] descritpions) {
        if (this.peaks == null) {
            setPeaks(peaks);
            setPeakWeight(weights);
            setPeakDescription(descritpions);
            return;
        }
        double[] newpeaks = Arrays.copyOf(this.peaks,this.peaks.length+peaks.length);
        System.arraycopy(peaks, 0, newpeaks, this.peaks.length, peaks.length);
        double[] newpeakWeights = Arrays.copyOf(this.peakWeight,this.peaks.length+peaks.length);
        System.arraycopy(weights, 0, newpeakWeights, this.peaks.length, peaks.length);
        this.peaks = newpeaks;
        this.peakWeight  = newpeakWeights;
        if (descritpions != null) {
            String[] newpeakdescriptions;
            if (this.peakDescription == null) {
                newpeakdescriptions=new String[this.peaks.length];
            } else {
                newpeakdescriptions = Arrays.copyOf(this.peakDescription,this.peaks.length);
            }
            System.arraycopy(descritpions, 0, newpeakdescriptions, this.peakDescription.length, descritpions.length);
            this.peakDescription = newpeakdescriptions;
        }
    }

    /**
     * optional some name
     * @param peakDescription the peakDescription to set
     */
    public void setPeakDescription(String[] peakDescription) {
        this.peakDescription = peakDescription;
    }

//    /**
//     * where to read from
//     * @return the inner
//     */
//    public SpectraAccess getInner() {
//        return inner;
//    }
//
//    /**
//     * where to read from
//     * @param inner the inner to set
//     */
//    public void setInner(SpectraAccess inner) {
//        this.inner = inner;
//    }

    /**
     * a mz corrector per run
     * @return the runCalibration
     */
    public HashMap<String, Corrector> getRunCalibration() {
        return runCalibration;
    }

    /**
     * a mz corrector per run
     * @param runCalibration the runCalibration to set
     */
    protected void setRunCalibration(HashMap<String, Corrector> runCalibration) {
        this.runCalibration = runCalibration;
    }

    /**
     * by default precursor values are not touched
     * @return the correctMS1
     */
    public boolean isCorrectMS1() {
        return correctMS1;
    }

    /**
     * by default precursor values are not touched
     * @param correctMS1 the correctMS1 to set
     */
    public void setCorrectMS1(boolean correctMS1) {
        this.correctMS1 = correctMS1;
    }

    /**
     * only correct errors larger then this.
     * @return the cutoff
     */
    public double getCutoff() {
        return cutoff;
    }

    /**
     * only correct errors larger then this.
     * @param cutoff the cutoff to set
     */
    public void setCutoff(double cutoff) {
        this.cutoff = cutoff;
    }

    /**
     * tolerance used to look for peaks that are used for calibration
     * @return the preCalibrationTolerance
     */
    public ToleranceUnit getPreCalibrationTolerance() {
        return preCalibrationTolerance;
    }

    /**
     * tolerance used to look for peaks that are used for calibration
     * @param preCalibrationTolerance the preCalibrationTolerance to set
     */
    public void setPreCalibrationTolerance(ToleranceUnit preCalibrationTolerance) {
        this.preCalibrationTolerance = preCalibrationTolerance;
    }
    

    public static void main(String[] args) throws IOException, FileNotFoundException, ParseException {
        GatherInformation gi = new GatherInformation();
        gi.setPreCalibrationTolerance(new ToleranceUnit("20ppm"));
        gi.setupPeaksTryptic();
        gi.setupPeak445(1);
        for (int i = 0 ; i <gi.getPeaks().length; i++)
            System.out.println(gi.getPeakDescription()[i] + ", " + gi.getPeaks()[i] + ", " + gi.getPeakWeight()[i]);
        
        gi.setReader(AbstractMSMAccess.getMSMIterator(
                new File("/home/lfischer/Projects/ProteinRNA/analysis_ti02_real-20190228T104644Z-001.zip"),
                //new File("/Users/salman_ed_ssh/xi_data/" + "xi/users/kosuke/170825-2C-Lumos-2C-Jurkat-2C-Phospho-2C-SCX-Fr1-12-13_08_57-28_Aug_2017"),
                //+ "xi/users/kosuke/170825-2C-Lumos-2C-Jurkat-2C-Phospho-2C-SCX-Fr1-12-13_08_57-28_Aug_2017"), 
                new ToleranceUnit("20 ppm"), 0, new AbstractRunConfig() { 
                    
                    {
                        storeObject("SCAN_RE", ".*SpectrumID:\\s*\"([0-9]*)\".*");
                        storeObject("RUN_RE", ".*File:\\s*\"([^\"]*)\".*");
                    }
                }));
        gi.readPeakErrors();
        for (String run : gi.getRunCalibration().keySet()) {
            System.out.println(run + " (" +gi.getRunCalibration().get(run).count +") : " + (gi.getRunCalibration().get(run).relativeOffset * 1000000));
        }
    }
    
    @Override
    public void gatherData() throws FileNotFoundException, IOException {
        readPeakErrors();
    }    
    
    public int getSpectraCount() {
        return m_scanCount;
    }    
}
