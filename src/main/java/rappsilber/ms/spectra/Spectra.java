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
package rappsilber.ms.spectra;

import java.util.Iterator;
import rappsilber.ms.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import rappsilber.config.AbstractRunConfig;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.SecondaryFragment;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.annotation.IsotopPattern;
import rappsilber.ms.spectra.annotation.SpectraPeakAnnotation;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.annotation.XaminatrixIsotopAnnotation;
import rappsilber.ms.spectra.match.MatchedBaseFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.ms.spectra.match.PreliminaryMatch;
import rappsilber.ms.statistics.utils.UpdateableLong;
import rappsilber.utils.CountOccurence;
import rappsilber.utils.SortedLinkedList;
import rappsilber.utils.Util;

/**
 * Represents an MS2 spectra in form of a peak list
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class Spectra implements PeakList {

    
    private final static SpectraPeakAnnotation MGC_MATCHED = new SpectraPeakAnnotation("mgc_matched");
    private final static SpectraPeakAnnotation MGC_MATCHED_COMPLEMENT = new SpectraPeakAnnotation("mgc_matched_complement");

    /** the physical file that the spectrum was read from */
    private String m_source = "";
    /** the physical file that the spectrum was read from */
    private String m_peakFileName = "";
    /** database id  for the spectrum */
    private UpdateableLong m_id = new UpdateableLong(-1);
    /** an id identifying the run */
    private long m_RunId = -1;
    /** an id identifying the database acquisition */
    private long m_AcqId = -1;
    /**  some arbitrary ID usually a running id of the read spectra*/
    private int m_readid = -1;
    
    /**  the maximal observed intensity */
    private double m_maxIntensity = 0;
    /**  the peak with the maximal intensity */
    private SpectraPeak m_maxPeak;

    /**
     * his is the Isotope annotation method that is used when the spectra is
     * deisotoped without beforehand annotating the isotopes
     */
    public static IsotopPattern DEFAULT_ISOTOP_DETECTION;

    /**
     * time at which the spectra was recorded - or in other words the time when
     * a matched peptide came of the column
     */
    private double m_ElutionTimeStart = -1;

    /**
     * time at which the spectra was recorded - or in other words the time when
     * a matched peptide came of the column
     */
    private double m_ElutionTimeEnd = -1;

    /**
     * The intensity of the MS1 peak that was selected for MS2 fragmentation
     */
    private double m_PrecurserIntesity = -1;
    /**
     * The charge of the MS1 peak that was selected for MS2 fragmentation
     */
    private int    m_PrecurserCharge = -1;
    /**
     * The m/z value of the MS1 peak that was selected for MS2 fragmentation.
     * This value can be changed, to an assumed mass.
     */
    private double m_PrecurserMZ = -1;

    /**
     * The m/z value of the MS1 peak that was selected for MS2 fragmentation as read from the spectrum
     */
    private double m_PrecurserMZExp = -1;
    
    /**
     * The charge of the MS1 peak that was selected for MS2 fragmentation
     */
    private int[]  m_PrecurserChargeAlternatives = new int[0];


    //    private double m_PrecurserMass = -1;
    /**
     * stores all peaks in a sorted list
     */
    //private SortedLinkedList<SpectraPeak> m_Peaks = new SortedLinkedList<SpectraPeak>();
    //private SortedLinkedList<SpectraPeak> m_Peaks = new SortedLinkedList<SpectraPeak>();
    /**
     * makes the peaks accessible via the m/z value
     */
    private TreeMap<Double,SpectraPeak> m_PeakTree = new TreeMap<Double, SpectraPeak>();
    /**
     * when querying any m/z value (like looking up a peak at position n) this
     * tolerance is used
     */
    private ToleranceUnit m_Tolerance = null;

    /** the run of the spectra */
    private String m_run;
    /** the scan number of the spectra */
    private Integer m_scan_number = null;

//    private String m_source;

    /**
     * what lossy masses will be considered for detecting lossy peaks while
     * MGC and MGX generation
     */
    public static ArrayList<Double> SPECTRALOSSSES = new ArrayList<Double>();

    //private ArrayList<SpectraPeakCluster> m_isotopClusters = null;
    /**
     * a list of detected isotope clusters
     */
    private SpectraPeakClusterList m_isotopClusters;

    /**
     * used for the two-step-approach - stores the matched alpha-peptides
     */
    private ArrayList<PreliminaryMatch> m_matches = new ArrayList<PreliminaryMatch>();
    
    
    /**
     * the spectrum can come with instructions on what m/z vales are to be searched
     */
    private ArrayList<Double> m_additional_mz = null;
    
    /**
     * the spectrum can come with instructions on what charge states are to be searched
     */
    private ArrayList<Integer> m_additional_charge = null;
    
    /** if we derive spectra (e.g. clone) then this should point to the original spectrum */
    private Spectra origin = this;

    /** if given then this contains a list of masses that are likely masses fro the peptide candidates*/
    private ArrayList<Double> m_peptideCandidateMasses;
    
    private ArrayList<Double> m_peptideCandidateMassWeights;
    /** the tolerance to use with the peptide candidate masses */
    private ToleranceUnit m_peptideCandidateTolerance;
    
    /**
     * E.g. the TITLE tag from an mgf-file
     */
    private String scanTitle;
    
    /**
     * some initialisation for static members of the class
     */
    static {
        DEFAULT_ISOTOP_DETECTION = new XaminatrixIsotopAnnotation(new AbstractRunConfig() {});
        ((XaminatrixIsotopAnnotation)DEFAULT_ISOTOP_DETECTION).setAveraginBreakUp(4);
        DEFAULT_ISOTOP_DETECTION.setMaxMono2FirstPeakRatio(11);
        DEFAULT_ISOTOP_DETECTION.setMaxPeakToPeakRation(11);

        SPECTRALOSSSES.add(Util.AMMONIA_MASS);
        SPECTRALOSSSES.add(Util.WATER_MASS);
    }


    //----------------------------------
    //constructors
    //<editor-fold desc="constructors">

    /**
     * creates a new spectra with all the information from the spectra s
     * @param s base spectra to be copied
     */
    public Spectra(Spectra s) {

        m_ElutionTimeStart = s.m_ElutionTimeStart;
        m_PrecurserIntesity = s.m_PrecurserIntesity;
        m_PrecurserMZ = s.m_PrecurserMZ;
        m_PrecurserMZExp = s.m_PrecurserMZExp;
        m_PrecurserCharge = s.m_PrecurserCharge;
        setPeaks(s.getPeaks());
        setTolearance(s.getTolearance());
        
        cloneIsotopeClusters(s);
        
        m_run = s.getRun();
        m_scan_number = s.getScanNumber();
    }


    /**
     * creates an empty spectra
     */
    public Spectra() {
    }



    /**
     * creates a spectra with some parameters preset
     * @param ElutionTime
     * @param PrecurserIntesity
     * @param PrecurserMZ
     */
    public Spectra(double ElutionTime,
            double PrecurserIntesity,
            double PrecurserMZ) {
        m_ElutionTimeStart = ElutionTime;
        m_PrecurserIntesity = PrecurserIntesity;
        m_PrecurserMZ = PrecurserMZ;
        m_PrecurserMZExp = PrecurserMZ;
    }

    /**
     * creates a spectra with some parameters preset
     * @param ElutionTime
     * @param PrecurserIntesity
     * @param PrecurserMZ
     * @param PrecurserCharge
     */
    public Spectra(double ElutionTime,
            double PrecurserIntesity,
            double PrecurserMZ,
            int PrecurserCharge) {
        this(ElutionTime, PrecurserIntesity, PrecurserMZ);
        m_PrecurserCharge = PrecurserCharge;
        //m_PrecurserMass = m_PrecurserMZ * PrecurserCharge;
    }

    /**
     * creates a spectra with some parameters preset
     * @param ElutionTime
     * @param PrecurserIntesity
     * @param PrecurserMZ
     * @param PrecurserCharge
     * @param run
     * @param scan_number
     */
    public Spectra(double ElutionTime,
            double PrecurserIntesity,
            double PrecurserMZ,
            int PrecurserCharge,
            String run,
            int scan_number) {
        this(ElutionTime, PrecurserIntesity, PrecurserMZ, PrecurserCharge);
        m_run = run;
        m_scan_number = scan_number;
    }

    public static void free(Spectra s) {
        s.m_PeakTree.clear();
        s.m_PrecurserChargeAlternatives = null;
        s.m_isotopClusters.clear();
        if (s.m_matches != null)
            s.m_matches.clear();
        s.m_source = "";
        s.m_Tolerance = null;
        s.m_run = null;
        s.m_scan_number = -1;

    }

    /**
     * creates a spectra with some parameters preset
     * @param ElutionTime
     * @param PrecurserIntesity
     * @param PrecurserMZ
     * @param PrecurserMass
     */
    public Spectra(double ElutionTime,
            double PrecurserIntesity,
            double PrecurserMZ,
            double PrecurserMass) {
        this(ElutionTime, PrecurserIntesity, PrecurserMZ);
        //m_PrecurserMass = PrecurserMass;
        m_PrecurserCharge = (int) (PrecurserMass / PrecurserMZ);
    }

    /**
     * creates a spectra with some parameters preset
     * @param ElutionTime
     * @param PrecurserIntesity
     * @param PrecurserMZ
     * @param PrecurserMass
     * @param Peaks
     */
    public Spectra(double ElutionTime,
            double PrecurserIntesity,
            double PrecurserMZ,
            double PrecurserMass,
            ArrayList<SpectraPeak> Peaks) {
        this(ElutionTime, PrecurserIntesity, PrecurserMZ, PrecurserMass);
        setPeaks(Peaks);
    }

    /**
     * creates a spectra with some parameters preset and copies over the peak list
     * @param ElutionTime
     * @param PrecurserIntesity
     * @param PrecurserMZ
     * @param Peaks
     */
    public Spectra(double ElutionTime,
            double PrecurserIntesity,
            double PrecurserMZ,
            ArrayList<SpectraPeak> Peaks) {
        this(ElutionTime, PrecurserIntesity, PrecurserMZ);
        setPeaks(Peaks);
    }

    /**
     * creates a spectra with some parameters preset
     * @param ElutionTime
     * @param PrecurserIntesity
     * @param PrecurserMZ
     * @param PrecurserCharge
     * @param Peaks 
     */
    public Spectra(double ElutionTime,
            double PrecurserIntesity,
            double PrecurserMZ,
            int PrecurserCharge,
            ArrayList<SpectraPeak> Peaks) {
        this(ElutionTime, PrecurserIntesity, PrecurserMZ, PrecurserCharge);
        setPeaks(Peaks);
    }


    //</editor-fold > constructors
    //----------------------------------
    //Adding peaks to the spektra
    //<editor-fold desc="add peak data">

    /**
     * Copies all the peaks from the list into the spectra.<br/>
     * Previous peak data get lost.<br/>
     * Each peak gets cloned and then added to the spectra - to create an
     * independent peak list
     * @param Peaks
     */
    public void setPeaks(Collection<SpectraPeak> Peaks) {
        m_PeakTree = new TreeMap<Double, SpectraPeak>();
        for (SpectraPeak p : Peaks) {
            addPeak(p.clone());
        }
//        m_Peaks = new SortedLinkedList<SpectraPeak>();
//        for (SpectraPeak p : Peaks) {
//            SpectraPeak pc = p.clone();
//            m_Peaks.add(pc);
//            m_PeakTree.put(pc.getMZ(), pc);
//        }
    }

    /**
     * Copies all the peaks from the list into the spectra.<br/>
     * Previous peak data get lost.<br/>
     * Each peak gets cloned and then added to the spectra - to create an
     * independent peak list
     * @param Peaks
     */
    public void setPeaks(SpectraPeak[] Peaks) {
        m_PeakTree = new TreeMap<Double, SpectraPeak>();
        for (SpectraPeak p : Peaks) {
            addPeak(p.clone());
        }

    }

    /**
     * adds a new peak to the spectra
     * @param peak
     */
    public void addPeak(SpectraPeak peak) {
        if (this.m_PeakTree == null) {
            //m_Peaks = new SortedLinkedList<SpectraPeak>();
            m_PeakTree = new TreeMap<Double, SpectraPeak>();
        }
        
        if (peak.getIntensity() > m_maxIntensity)  {
            m_maxIntensity=peak.getIntensity();
            m_maxPeak = peak;
        }
        //this.m_Peaks.add(peak);
        this.m_PeakTree.put(peak.getMZ(), peak);
    }

    public void rebuildPeakTree() {
        TreeMap<Double, SpectraPeak> t = new TreeMap<Double, SpectraPeak>();
        for (SpectraPeak sp : m_PeakTree.values())    {
            t.put(sp.getMZ(), sp);
        }
        
        m_PeakTree = t;
    }
    protected void removePeakUnsafe(SpectraPeak peak) {
        this.m_PeakTree.remove(peak.getMZ());
    }

    public void removePeak(double mz) {
        SpectraPeak peak = getPeakAt(mz);
        if (peak != null) {
            this.m_PeakTree.remove(peak.getMZ());
        }
    }


    /**
     * adds a new peak to the spectra
     * @param peak
     */
    public void addPeakIntesity(SpectraPeak peak) {


        if (this.m_PeakTree == null) {
            //m_Peaks = new SortedLinkedList<SpectraPeak>();
            m_PeakTree = new TreeMap<Double, SpectraPeak>();
        }
        SpectraPeak exists = getPeakAt(peak.getMZ());
        if (exists == null) {
            //this.m_Peaks.add(peak);
            this.m_PeakTree.put(peak.getMZ(), peak);
            if (peak.getIntensity() > m_maxIntensity)  {
                m_maxIntensity=peak.getIntensity();
                m_maxPeak = peak;                
            }
        } else {
            double i = exists.getIntensity() + peak.getIntensity();
            exists.setIntensity(i);
            for (SpectraPeakAnnotation a : peak.getAllAnnotations())
                exists.annotate(a);
            if (i > m_maxIntensity) {
                m_maxIntensity=i;
                m_maxPeak = exists;
            }
        }
    }

    /**
     * adds a peak to the spectra, but only if this specific peak hasn't been added before
     * @param peak
     */
    public void addPeakUniqe(SpectraPeak peak) {
        if (this.m_PeakTree == null) {
            //m_Peaks = new SortedLinkedList<SpectraPeak>();
            m_PeakTree = new TreeMap<Double, SpectraPeak>();
        }
        if (!m_PeakTree.containsValue(peak)) {
            //this.m_Peaks.add(peak);
            this.m_PeakTree.put(peak.getMZ(), peak);
            if (peak.getIntensity() > m_maxIntensity) {
                m_maxIntensity=peak.getIntensity();
                m_maxPeak = peak;                
            }
        }
    }

    /**
     * adds a new peak to the spectra
     * @param mz    the m/z value of the peak
     * @param intesity  the intensity of the peak
     */
    public void addPeak(double mz, double intesity) {
        addPeak(new SpectraPeak(mz, intesity, m_Tolerance));
    }

    //</editor-fold> peakdata




    // <editor-fold desc="clone a spectra"> peakdata
    /**
     * creates a copy of the current Spectra
     * @return
     */
    @Override
    public Spectra clone() {
        Spectra s = cloneEmpty();
        for (SpectraPeak p : getPeaks())
            s.addPeak(p.clone());
        return s;
    }

    /**
     * creates a copy of the current Spectra without the actual peaks
     * @return
     */
    public Spectra cloneEmpty() {
        //Spectra s = getSpectra();
        Spectra s = new Spectra();
        s.origin = this.origin;
        s.setElutionTimeStart(m_ElutionTimeStart);
        s.setElutionTimeEnd(m_ElutionTimeEnd);
        s.setPrecurserIntensity(m_PrecurserIntesity);
        s.setPrecurserMZ(m_PrecurserMZ);
        s.setPrecurserCharge(m_PrecurserCharge);
        s.setScanNumber(getScanNumber());
        s.setRun(getRun());
        s.setTolearance(getTolearance());
        s.setSource(getSource());
        s.setPrecoursorChargeAlternatives(m_PrecurserChargeAlternatives);
        s.setAdditionalCharge(m_additional_charge);
        s.setAdditionalMZ(m_additional_mz);
        if (m_peptideCandidateMasses !=null)
            s.setPeptideCandidateMasses(m_peptideCandidateMasses);


        // make sure we propaget the id
        s.m_id = m_id;
        s.m_RunId = m_RunId;
        s.m_readid = m_readid;
        s.m_AcqId = m_AcqId;


        s.m_source = m_source;
        s.m_peakFileName = m_peakFileName;
        s.setScanTitle(scanTitle);
    
        
        return s;
    }

    /**
     * clones all isotope clusters of the given spectrum into this spectrum
     * @param s
     */
    public void cloneIsotopeClusters(Spectra s) {
        for (SpectraPeakCluster c : s.getIsotopeClusters()) {
            SpectraPeakCluster cn = c.clone(this);
            this.m_isotopClusters.add(cn);
        }
    }

    /**
     * clones the spectrum including the isotopeclusters
     * @return a deep clone of the current spectrum
     */
    public Spectra cloneComplete() {
        // prepare an empty spectra
        Spectra s = this.cloneEmpty();
        
        HashMap<SpectraPeak,SpectraPeak> peakMap = new HashMap<SpectraPeak, SpectraPeak>(m_PeakTree.size());
        // clone the peaks
        for (SpectraPeak p : getPeaks()) {
            SpectraPeak pn = p.cloneComplete();
            peakMap.put(p, pn);
            s.addPeak(pn);
        }

        // rebuild the spectra cluster
        for (SpectraPeakCluster c : getIsotopeClusters()) {
            SpectraPeakCluster cn = c.clone(s,peakMap);
            s.m_isotopClusters.add(cn);
        }
        return s;
    }
    
    /**
     * clones all isotope clusters of the given spectrum into this spectrum
     * @return a deep clone of the current spectrum
     */
    public Spectra cloneShifted(double mass) {
        Spectra s = cloneComplete();
        Spectra sRet = cloneEmpty();

        Collection<SpectraPeak> peaks = s.getPeaks();

        for (SpectraPeak p : s.getPeaks()) {
            p.setMZ(p.getMZ() + mass);
            sRet.addPeak(p);
        }

        for (SpectraPeakCluster c : s.getIsotopeClusters()) {
            c.setMZ(c.getMZ() + mass);
            sRet.m_isotopClusters.add(c);
        }

        sRet.setPrecurserMZ(s.getPrecurserMZ() + 2*mass);
        sRet.setRun("DECOY_" + getRun() + "_decoy");
        sRet.setScanNumber(-getScanNumber());

        return sRet;
    }

    /**
     * Returns a reduced spectra, that only contains the given number of peaks in every (consecutive) m/z window
     * @param peaks     number of peaks
     * @param windowSize    size of the windows to check
     * @return
     */
    public Spectra cloneTopPeaks(int peaks, double windowSize) {
        Spectra s = cloneEmpty();
        
        if (m_PeakTree.isEmpty())
            return s;
        
        double minMZ = m_PeakTree.firstKey();
        double maxMZ =  m_PeakTree.lastKey();

        int windows = (int) ((maxMZ - minMZ) / windowSize + 1);

        ArrayList<ArrayList<SpectraPeak>> peaklists = new ArrayList<ArrayList<SpectraPeak>>(windows);
        for (int i = 0; i < windows; i++ )
            peaklists.add(new ArrayList<SpectraPeak>());

        // collect the top peaks
        for (SpectraPeak sp : this.getTopPeaks(-1)){
            int wid = (int) ((sp.getMZ() - minMZ) / windowSize);
            ArrayList<SpectraPeak> windowPeaks = peaklists.get(wid);
            if (windowPeaks.size() < peaks) {
                windowPeaks.add(sp);
            }
        }

        for (ArrayList<SpectraPeak> windowPeaks : peaklists)
            for (SpectraPeak sp : windowPeaks)
                s.addPeak(sp.cloneComplete());
        //fillUpIsotopeCluster(s);
        //s.getPeakAt(window, m_Tolerance)
        return s;
    }

    /**
     * Returns a reduced spectra, that only contains the given number of peaks in every (consecutive) m/z window
     * @param peaks     number of peaks
     * @param windowSize    size of the windows to check
     * @return
     */
    public Spectra cloneTopPeaksRolling(int peaks, double windowSize,double minMZ,double maxMZ, Collection<Double> peaksToKeep) {
        Spectra s = cloneComplete();
        
        HashSet<Double> keepPeaks=new HashSet<>();
        for (Double d : peaksToKeep) {
            SpectraPeak sp = s.getPeakAt(d);
            if (sp != null) {
                keepPeaks.add(sp.getMZ());
            }
        }
        if (m_PeakTree.isEmpty())
            return s;
        SortedMap<Double,SpectraPeak> sm =s.m_PeakTree.headMap(minMZ);
        for (Double mz : sm.keySet().toArray(new Double[sm.size()])) {
            if (!keepPeaks.contains(mz))
                s.m_PeakTree.remove(mz);
        }
        sm =s.m_PeakTree.tailMap(maxMZ);
        for (Double mz : sm.keySet().toArray(new Double[sm.size()])) {
            if (!keepPeaks.contains(mz))
                s.m_PeakTree.remove(mz);
        }
        
        //s.getPeakAt(window, m_Tolerance)
        return s.cloneTopPeaksRolling(peaks,windowSize, peaksToKeep);
    }
    
    protected void fillUpIsotopeCluster(Spectra s) {
        Spectra is = this;
        if (this.m_isotopClusters==null || this.m_isotopClusters.isEmpty()) {
            is = cloneComplete();
            DEFAULT_ISOTOP_DETECTION.anotate(is);
            if (is.m_isotopClusters!=null && !is.m_isotopClusters.isEmpty())
                is.fillUpIsotopeCluster(s);
            return;
        }
        HashSet<SpectraPeak> toAdd =new HashSet<>();
        // go through all isotope cluster and see if a peak of that one exist in s
        for (SpectraPeakCluster spc : is.m_isotopClusters) {
            for (SpectraPeak sp : spc)
                if (s.hasPeakAt(sp.getMZ()))
                    toAdd.addAll(spc);
        }
        
        // now add all the missing isotope peaks onto s
        for (SpectraPeak sp :toAdd)
            s.addPeak(sp.clone());
    }

   /**
     * Returns a reduced spectra, that only contains the given number of peaks in every (consecutive) m/z window
     * @param peaks     number of peaks
     * @param windowSize    size of the windows to check
     * @return
     */
    public Spectra cloneTopPeaksRollingOld(int peaks, double windowSize) {
        Spectra s = cloneEmpty();
        
        
        if (m_PeakTree.isEmpty())
            return s;
        
        ArrayList<SpectraPeak> topPeaks = this.deIsotop().getTopPeaks(-1);

        TreeMap<Double,SpectraPeak> reducedPeaks = new TreeMap<>();
        
        // collect the top peaks
        for (SpectraPeak sp : topPeaks){
            double mz =sp.getMZ();
            double lower1=mz-windowSize/2;
            double lower2=mz-windowSize;
            double higher1=mz+windowSize/2;
            double higher2=mz+windowSize;
            if (s.m_PeakTree.subMap(lower1,higher1).size()<peaks &&
                    s.m_PeakTree.subMap(lower2,mz).size()<peaks &&
                    s.m_PeakTree.subMap(mz,higher2).size()<peaks) {
                s.addPeak(sp);
            }
        }
        fillUpIsotopeCluster(s);

        //s.getPeakAt(window, m_Tolerance)
        return s;
    }    
    
    /**
     * Returns a reduced spectra, that only contains the given number of peaks in every (consecutive) m/z window
     * @param peaks     number of peaks
     * @param windowSize    size of the windows to check
     * @return
     */
    public Spectra cloneTopPeaksRolling(int peaks, double windowSize, Collection<Double> peaksToKeep) {
        // we star with a deisotoped (not charge reduced) spectra
        Spectra s = this.deIsotop();
        
        
        if (m_PeakTree.isEmpty())
            return s;
        
        
        ArrayList<SpectraPeak> topPeaks = s.getTopPeaks(-1);
        LinkedList<SpectraPeak> topPeaksList = new LinkedList<>(topPeaks);
        

        TreeMap<Double,SpectraPeak> reducedPeaksMap = new TreeMap<>();
        HashSet<SpectraPeak> removedPeaks=new HashSet<>();
        for (Double d : peaksToKeep) {
            SpectraPeak sp = s.getPeakAt(d);
            if (sp != null) {
                removedPeaks.add(sp);
            }
        }
        

        // for each peak 
        for (SpectraPeak sp : topPeaks){
            sp.getAllAnnotations().clear();
            if (!removedPeaks.contains(sp)) {
                double mz =sp.getMZ();
                // check how many peaks are in the window around it
                SortedMap<Double,SpectraPeak> sm = s.m_PeakTree.subMap(mz-windowSize/2,mz+windowSize/2);
                if (sm.size()>peaks)  {
                    // to many we need to delte some
                    ArrayList<Map.Entry<Double,SpectraPeak>> as = new ArrayList<>(sm.entrySet());
                    java.util.Collections.sort(as, new Comparator<Map.Entry<Double,SpectraPeak>>() {
                            @Override
                            public int compare(Map.Entry<Double, SpectraPeak> o1, Map.Entry<Double, SpectraPeak> o2) {
                                return Double.compare(o1.getValue().getIntensity(),o2.getValue().getIntensity());
                            }
                        }
                    );

                    // delete the lowest intens peaks in the window around it
                    for (int i=sm.size()-peaks-1;i>=0; i--) {
                        SpectraPeak toDelete= as.get(i).getValue();
                        s.removePeak(toDelete.getMZ());
                        removedPeaks.add(toDelete);
                    }

                }
            }
        }
        fillUpIsotopeCluster(s);
        
        s.m_isotopClusters.clear();
        
        return s;
    }
    
    // </editor-fold >


    
    //<editor-fold desc="field access">

    /**
     * @return the m_ElutionTimeStart
     */
    public double getElutionTimeStart() {
        return m_ElutionTimeStart;
    }

    /**
     * @return the m_ElutionTimeStart
     */
    public double getElutionTimeEnd() {
        return m_ElutionTimeEnd;
    }

    /**
     * @param m_ElutionTimeStart the m_ElutionTimeStart to set
     */
    public void setElutionTimeStart(double ElutionTime) {
        this.m_ElutionTimeStart = ElutionTime;
    }

    /**
     * @param ElutionTime the m_ElutionTimeStart to set
     */
    public void setElutionTimeEnd(double ElutionTime) {
        this.m_ElutionTimeEnd = ElutionTime;
    }

    /**
     * @return the m_PrecurserIntesity
     */
    public double getPrecurserIntensity() {
        return m_PrecurserIntesity;
    }

    /**
     * @param m_PrecurserIntesity the m_PrecurserIntesity to set
     */
    public void setPrecurserIntensity(double m_PrecurserIntesity) {
        this.m_PrecurserIntesity = m_PrecurserIntesity;
    }

    /**
     * @return the m_PrecurserCharge
     */
    public int getPrecurserCharge() {
        return m_PrecurserCharge;

    }

    /**
     * @param m_PrecurserCharge the m_PrecurserCharge to set
     */
    public void setPrecurserCharge(int m_PrecurserCharge) {
        this.m_PrecurserCharge = m_PrecurserCharge;
    }

    /**
     * @return the m_PrecurserMZ
     */
    public double getPrecurserMZ() {
        return m_PrecurserMZ;
    }

    /**
     * @return the m_PrecurserMZ
     */
    public double getPrecurserMZExp() {
        return m_PrecurserMZExp;
    }
    
    /**
     * @param m_PrecurserMZ the m_PrecurserMZ to set
     */
    public void setPrecurserMZ(double m_PrecurserMZ) {
        this.m_PrecurserMZ = m_PrecurserMZ;
        if (m_PrecurserMZExp == -1)
            this.m_PrecurserMZExp = m_PrecurserMZ;
            
    }

    /**
     * @param m_PrecurserMZ the m_PrecurserMZ to set
     */
    public void setPrecurserMZExp(double PrecurserMZ) {
        this.m_PrecurserMZExp = PrecurserMZ;
        if (m_PrecurserMZ == -1)
            this.m_PrecurserMZ = PrecurserMZ;
    }
    
    /**
     * @return the m_PrecurserMass
     */
    public double getPrecurserMass() {
        return (m_PrecurserMZ - Util.PROTON_MASS)* m_PrecurserCharge;
    }

    /**
     * @return the m_PrecurserMass
     */
    public void setPrecurserMass(double mass) {
        m_PrecurserMZ = mass/m_PrecurserCharge + Util.PROTON_MASS;
    }

    public int[] getPrecoursorChargeAlternatives()  {
        return m_PrecurserChargeAlternatives;
    }

    public void setPrecoursorChargeAlternatives(int[] alternativeChargeStates)  {
        m_PrecurserChargeAlternatives = alternativeChargeStates;
    }

//    /**
//     * @param m_PrecurserMass the m_PrecurserMass to set
//     */
//    public void setPrecurserMass(double m_PrecurserMass) {
//        this.m_PrecurserMass = m_PrecurserMass;
//    }

//    /**
//     * @return the m_Peaks
//     */
//    public SortedLinkedList<SpectraPeak> getPeaks() {
//        return m_Peaks;
//    }

    /**
     * @return the m_Peaks
     */
    public Collection<SpectraPeak> getPeaks() {
        return m_PeakTree.values();
    }

    /**
     * returns the peaks with in the given range
     * @param minMZ
     * @param maxMZ
     * @return the Peaks
     */
    public Collection<SpectraPeak> getPeaks(double minMZ, double maxMZ) {
        return m_PeakTree.subMap(minMZ, maxMZ).values();
    }


    /**
     * @return the m_Peaks
     */
    public SpectraPeak[] getPeaksArray() {
        ArrayList<SpectraPeak> ret = new ArrayList<SpectraPeak>(m_PeakTree.values());
        return ret.toArray(new SpectraPeak[ret.size()]);
    }

    /**
     * @return the tolerance
     */
    public ToleranceUnit getTolearance() {
        return m_Tolerance;
    }

    /**
     * @param tolerance the Tolerance for comparing values
     */
    public void setTolearance(ToleranceUnit tolerance) {
        this.m_Tolerance = tolerance;
        if (m_isotopClusters == null)
            m_isotopClusters = new SpectraPeakClusterList(m_Tolerance);
        else
            m_isotopClusters.setTolerance(tolerance);
    }

    /**
     * @return the m_isotopClusters
     */
    public SpectraPeakClusterList getIsotopeClusters() {
        if (m_isotopClusters == null) {
            m_isotopClusters = new SpectraPeakClusterList(m_Tolerance);
        }
        return m_isotopClusters;
    }

    /**
     * clones all isotop cluster in the list and replaces the currently defined
     * ones with these ones
     * @param isotopClusters the isotopClusters to set
     */
    public void setIsotopClusters(Collection<SpectraPeakCluster> isotopClusters) {
        m_isotopClusters = new SpectraPeakClusterList(m_Tolerance);
        for (SpectraPeakCluster c : isotopClusters)
            m_isotopClusters.add(c.clone(this));
    }

    public String getSource() {
        return m_source;
    }

    public void setSource(String source) {
        if (source != null)
            m_source = source;
        else
            m_source = "";

    }

    
    
    //</editor-fold>

    
    /**
     * Returns a peak, that is at a given m/z value.
     * @param mz
     * @return the peak, or null if there is non at that point
     */
    public SpectraPeak getPeakExactlyAt(double mz) {
        return m_PeakTree.get(mz);
    }
    
    /**
     * Returns a peak, that is at a given m/z value.
     * @param mz
     * @return the peak, or null if there is non at that point
     */
    public SpectraPeak getPeakAt(double mz) {
//        Double key = m_PeakTree.ceilingKey(m_Tolerance.getMinRange(mz));
//        if (key == null)
//            return null;
//        if (key <= m_Tolerance.getMaxRange(mz))
//            return m_PeakTree.get(key);
//        return null;
        SortedMap<Double, SpectraPeak> sm = null;
        Range r = m_Tolerance.getRange(mz);
        
        sm = m_PeakTree.subMap(r.min, r.max);
        if (sm.size() == 1)
            return sm.get(sm.firstKey());
        else if (sm.size() > 1) {
            SpectraPeak center = null;
            double middle = r.min+(r.max-r.min) /2;
            SortedMap<Double, SpectraPeak> lower = sm.subMap(r.min, middle);
            SortedMap<Double, SpectraPeak>  upper = sm.subMap(middle,r.max);
            if (lower.isEmpty()) {
                return upper.get(upper.firstKey());
            } else {
                double lk = lower.lastKey();
                if (upper.isEmpty())
                    return lower.get(lk);
                double uk = upper.firstKey();
                if (uk - middle < middle- lk)
                    return upper.get(uk);
                return lower.get(lk);
            }
            
        }
        return null;

    }

    /**
     * Returns a peak, that is at a given m/z value.
     * @param mz
     * @return the peak, or null if there is non at that point
     */
    public SpectraPeak getPeakAt(double mz, ToleranceUnit t) {
        Range r = t.getRange(mz);
        SortedMap<Double, SpectraPeak> sm = m_PeakTree.subMap(r.min,r.max);
        SpectraPeak center = null;
        double diff = Double.MAX_VALUE;
        for (SpectraPeak sp : sm.values()) {
            double newdiff = Math.abs(sp.getMZ() - mz);
            if (newdiff < diff) {
                diff = newdiff;
                center = sp;
            } else if (newdiff > diff) {
                return center;
            }
        }
        return center;
//        Double key = m_PeakTree.ceilingKey(t.getMinRange(mz));
//        if (key == null)
//            return null;
//        if (key <= t.getMaxRange(mz))
//            return m_PeakTree.get(key);
//        return null;
    }

    /**
     * Returns a peak, that is at a given m/z value.
     * @param peak
     * @param deltaMZ
     * @return the peak, or null if there is non at that point
     */
    public SpectraPeak getPeakAtDistance(SpectraPeak peak, double deltaMZ) {
        double peakMZ = peak.getMZ();
        double minMz = m_Tolerance.getMinRange(m_Tolerance.getMinRange(peakMZ)+deltaMZ);
        double maxMz = m_Tolerance.getMaxRange(m_Tolerance.getMaxRange(peakMZ)+deltaMZ);
        double mz = minMz + (maxMz- minMz) /2;
        SortedMap<Double, SpectraPeak> sm = m_PeakTree.subMap(minMz, maxMz);
        SpectraPeak center = null;
        double diff = Double.MAX_VALUE;
        for (SpectraPeak sp : sm.values()) {
            double newdiff = Math.abs(sp.getMZ() - mz);
            if (newdiff < diff) {
                diff = newdiff;
                center = sp;
            } else if (newdiff > diff) {
                return center;
            }
        }
        return center;
    }
    

    /**
     * Returns a peak, that is at a given m/z value.
     * @param mz
     * @return the peak, or null if there is non at that point
     */
    public SpectraPeak getPeakAtDistance(SpectraPeak peak, double deltaMZ, ToleranceUnit t) {
        double peakMZ = peak.getMZ();
        double minMz = t.getMinRange(t.getMinRange(peakMZ)+deltaMZ);
        double maxMz = t.getMaxRange(t.getMaxRange(peakMZ)+deltaMZ);
        double mz = minMz + (maxMz- minMz) /2;
        SortedMap<Double, SpectraPeak> sm = m_PeakTree.subMap(minMz, maxMz);
        SpectraPeak center = null;
        double diff = Double.MAX_VALUE;
        for (SpectraPeak sp : sm.values()) {
            double newdiff = Math.abs(sp.getMZ() - mz);
            if (newdiff < diff) {
                diff = newdiff;
                center = sp;
            } else if (newdiff > diff) {
                return center;
            }
        }
        return center;
    }


    /**
     * returns the standard deviation of the intensity
     * @return
     */
    public double getStdDevIntensity() {
        double mean = getMeanIntensity();
        double squaredDeviation=0;
        Collection<SpectraPeak> peaks = getPeaks();
        for (SpectraPeak p : peaks) {
            double deviation = p.getIntensity() - mean;
            squaredDeviation += deviation*deviation;
        }
        return Math.sqrt(squaredDeviation/(peaks.size() - 1));
    }

    /**
     * @return the run of the spectra
     */
    public String getRun() {
        return m_run;
    }

    /**
     * @param run the run to of the spectra
     */
    public void setRun(String run) {
        this.m_run = run;
    }

    /**
     * @return the scan number of the spectra
     */
    public Integer getScanNumber() {
        return m_scan_number;
    }

    /**
     * @param scan_number the scan number of the spectra
     */
    public void setScanNumber(Integer scan_number) {
        this.m_scan_number = scan_number;
    }


    /**
     * calculate the median over all peak intensities
     * @return
     */
    public double getMedianIntensity() {
        //@TODO  lined list is suboptimal
        TreeSet<Double> intensities = new TreeSet<>();
//        SortedLinkedList<Double> intensities = new SortedLinkedList<Double>();
        for (SpectraPeak sp: getPeaks()) {
            intensities.add(new Double(sp.getIntensity()));
        }
        double pos = intensities.size() / 2.0;
        double pos2 = Math.ceil(pos);
        double median=0;
        
        int p=0;
        Iterator<Double> i = intensities.iterator();
        for (p=0;p<=pos;p++) {
            median=i.next();
        }
        if (pos2 != pos) {
            median += i.next();
            median=median/2.0;
        }
       
        return median;
    }

    /**
     * calculates the mean intensity over all peak intensities
     * @return
     */
    public double getMeanIntensity() {
        double sum = 0;

        Collection<SpectraPeak> peaks = getPeaks();

        for (SpectraPeak sp: peaks) {
            sum += sp.getIntensity();
        }

        return sum/peaks.size();
    }


    /**
     * returns the highest observed intensity within that spectra
     * @return
     */
    public double getMaxIntensity() {
        return m_maxIntensity;
//        double max = 0;
//
//        Collection<SpectraPeak> peaks = getPeaks();
//
//        for (SpectraPeak sp: peaks) {
//            if (max < sp.getIntensity())
//                max = sp.getIntensity();
//        }
//
//        return max;
    }

    /**
     * returns the peak index of the highest observed intensity within that spectra
     * @return
     */
    public double getMaxIntensityIndex() {
        return m_maxPeak.getMZ();
//        double max = 0;
//        int maxPos = -1;
//        int pos = 0;
//        for (SpectraPeak p : getPeaks()) {
//            if (max < p.getIntensity()) {
//                max = p.getIntensity();
//                maxPos = pos;
//            }
//            pos++;
//        }
//        return maxPos;
    }

    /**
     * returns the peak with the highest observed intensity within that spectra
     * @return
     */
    public SpectraPeak getMaxIntensityPeak() {
        return m_maxPeak;
//        double max = 0;
//        SpectraPeak maxPeak = null;
//        for (SpectraPeak p : getPeaks()) {
//            if (max < p.getIntensity()) {
//                max = p.getIntensity();
//                maxPeak = p;
//            }
//        }
//        return maxPeak;
    }

    
    /**
     * returns a deloss copy of this spectra
     * @return
     */
    public Spectra deLoss(double deltamass) {
        if (m_isotopClusters.isEmpty())
            DEFAULT_ISOTOP_DETECTION.anotate(this);
        if (m_isotopClusters.isEmpty())
            return this;
        double mz = 0;
        Spectra di = this.cloneEmpty();
        //di.m_Peaks = new SortedLinkedList<SpectraPeak>();
        SpectraPeakCluster foundSPC = new SpectraPeakCluster(m_Tolerance);
//        foundSPC = m_isotopClusters.getFirst();
//        mz = foundSPC.getMZ();
        // go through the clusters and add a single peak for each one
        for (SpectraPeakCluster spc : m_isotopClusters) {
//            if (spc.getMZ() != mz) {
                mz = spc.getMZ();
                // check whether we have an isotop-cluster in the right distance
                if (!m_isotopClusters.hasCluster(mz+deltamass/spc.getCharge(), spc.getCharge())) {
                    for (SpectraPeak sp : spc) {
                        if (!di.hasPeakAt(sp.getMZ()));
                            di.addPeak(sp.clone());
                    }
                }
                
//                foundSPC = spc;
//            } else if (spc.size() > foundSPC.size() ) {
//
//                foundSPC = spc;
//            }
        }

        // add the non-isotopic peaks/non-lossy peaks
        sploop: for (SpectraPeak p : getPeaks()) {
            if (! p.hasAnnotation(SpectraPeakAnnotation.isotop)) {
                for (int c = 1; c<= getPrecurserCharge();c++) {
                    if (m_isotopClusters.hasCluster(p.getMZ()+deltamass/c, c)) {
                        continue sploop;
                    }
                }
                di.addPeak(p.clone());
            }
        }
        return di;


    }
    
    /**
     * returns a deisotoped copy of this spectra
     * @return
     */
    public Spectra deIsotop() {
        double mz = 0;
        if (m_isotopClusters.isEmpty())
            DEFAULT_ISOTOP_DETECTION.anotate(this);
        if (m_isotopClusters.isEmpty())
            return this.cloneComplete();
        Spectra di = this.cloneEmpty();
        //di.m_Peaks = new SortedLinkedList<SpectraPeak>();
        SpectraPeakCluster foundSPC;// = new SpectraPeakCluster(m_Tolerance);
        foundSPC = m_isotopClusters.getFirst();
        mz = foundSPC.getMZ();
        // go through the clusters and add a single peak for each one
        for (SpectraPeakCluster spc : m_isotopClusters) {
            if (spc.getMZ() != mz) {
                di.addPeak(foundSPC.toPeak(true));
                mz = spc.getMZ();
                foundSPC = spc;
            } else if (spc.size() > foundSPC.size() ) {

                foundSPC = spc;
            }
        }

        // add the non-isotopic peaks
        for (SpectraPeak p : getPeaks()) {
            if (! p.hasAnnotation(SpectraPeakAnnotation.isotop)) {
                di.addPeak(p.clone());
            }
        }
        return di;


    }

        /**
     * returns a deisotoped copy of this spectra
     * @return
     */
    public Spectra deChargeDeisotop() {
        if (m_isotopClusters.isEmpty())
            DEFAULT_ISOTOP_DETECTION.anotate(this);
        if (m_isotopClusters.isEmpty())
            return this;

        double mz = 0;
        Spectra di = this.cloneEmpty();
        //di.m_Peaks = new SortedLinkedList<SpectraPeak>();
        SpectraPeakCluster foundSPC;// = new SpectraPeakCluster(m_Tolerance);
        if (m_isotopClusters.isEmpty())
            DEFAULT_ISOTOP_DETECTION.anotate(this);
        
        if (!m_isotopClusters.isEmpty()) {
            foundSPC = m_isotopClusters.getFirst();
            mz = foundSPC.getMZ();
            // go through the clusters and add a single peak for each one
            for (SpectraPeakCluster spc : m_isotopClusters) {
                if (spc.getMZ() != mz) {
                    SpectraPeak sp = foundSPC.toPeak(true);
                    sp.setMZ((spc.getMZ()-Util.PROTON_MASS)*spc.getCharge() + Util.PROTON_MASS);
                    di.addPeakIntesity(sp);
                    mz = spc.getMZ();
                    foundSPC = spc;
                } else if (spc.size() > foundSPC.size() ) {

                    foundSPC = spc;
                }
            }
        }

        // add the non-isotopic peaks
        for (SpectraPeak p : getPeaks()) {
            if (! p.hasAnnotation(SpectraPeakAnnotation.isotop)) {
                di.addPeakIntesity(p.clone());
            }
        }
        di.setPrecurserCharge(1);
        di.setPrecurserMZ((getPrecurserMZ() - Util.PROTON_MASS) * getPrecurserCharge() + Util.PROTON_MASS);
        return di;
    }


        /**
     * returns a decharged copy of this spectra
     * @return
     */
    public Spectra deCharge() {
        if (m_isotopClusters.isEmpty())
            DEFAULT_ISOTOP_DETECTION.anotate(this);
        if (m_isotopClusters.isEmpty())
            return this;
        double mz = 0;
        Spectra di = this.cloneEmpty();
        //di.m_Peaks = new SortedLinkedList<SpectraPeak>();
        SpectraPeakCluster foundSPC;// = new SpectraPeakCluster(m_Tolerance);
        if (m_isotopClusters.isEmpty())
            DEFAULT_ISOTOP_DETECTION.anotate(this);
        
        if (!m_isotopClusters.isEmpty()) {
            foundSPC = m_isotopClusters.getFirst();
            mz = foundSPC.getMZ();
            // go through the clusters and add a single peak for each one
            for (SpectraPeakCluster spc : m_isotopClusters) {
                if (spc.getMZ() != mz) {
                    for (SpectraPeak csp : spc) {
                        SpectraPeak sp = csp.clone();
                        sp.setMZ((sp.getMZ()-Util.PROTON_MASS)*spc.getCharge() + Util.PROTON_MASS);
                        di.addPeakIntesity(sp);
                    }
                    mz = spc.getMZ();
                    foundSPC = spc;
                } else if (spc.size() > foundSPC.size() ) {

                    foundSPC = spc;
                }
            }
        }

        // add the non-isotopic peaks
        for (SpectraPeak p : getPeaks()) {
            if (! p.hasAnnotation(SpectraPeakAnnotation.isotop)) {
                di.addPeakIntesity(p);
            }
        }
        di.setPrecurserCharge(1);
        di.setPrecurserMZ((getPrecurserMZ() - Util.PROTON_MASS) * getPrecurserCharge() + Util.PROTON_MASS);
        return di;
    }
    
    

    /**
     * returns a list of peaks, that have a higher intensity the minIntensity
     * @param minIntensity the minimal intensity of peaks to be returned
     * @return peak list
     */
    public Collection<SpectraPeak> getPeaks(double minIntensity) {
        ArrayList<SpectraPeak> ret = new ArrayList<SpectraPeak>();
        // add the non-isotopic peaks
        for (SpectraPeak p : getPeaks()) {
            if (p.getIntensity() > minIntensity)
                ret.add(p);
        }
        return ret;

    }
//
//    /**
//     * returns the highest intents peaks with precedence to isotope clusters
//     * @param number how many peaks to return
//     * @return peak list
//     */
//    public Collection<SpectraPeak> getTopPeaksIsotopAware(int number, double Window) {
//        
//        
//        
//        ArrayList<SpectraPeak> ret = new ArrayList<SpectraPeak>(m_PeakTree.values());
//        java.util.Collections.sort(ret, new Comparator() {
//
//            @Override
//            public int compare(Object o1, Object o2) {
//                return Double.compare(((SpectraPeak)o1).getMZ(), ((SpectraPeak)o1).getMZ());
//            }
//        });
//
//        if (number >= 0 && number < ret.size())
//            return ret.subList(0, number);
//        else
//            return ret;
//
//    }
    

    /**
     * returns the highest intents peaks
     * @param number how many peaks to return
     * @return peak list
     */
    public ArrayList<SpectraPeak> getTopPeaks(int number) {

        ArrayList<SpectraPeak> ret = new ArrayList<SpectraPeak>(m_PeakTree.values());
        java.util.Collections.sort(ret, new Comparator() {

            @Override
            public int compare(Object o1, Object o2) {
                return Double.compare(((SpectraPeak)o2).getIntensity(), ((SpectraPeak)o1).getIntensity());
            }
        });

        if (number >= 0 && number < ret.size())
            ret = new ArrayList<SpectraPeak>(ret.subList(0, number));

        return ret;

    }

    /**
     * returns a list of peaks, that is sorted by intensity
     * 
     * @return
     */
    public SortedLinkedList<SpectraPeak> getPeaksByIntensity() {
        SortedLinkedList<SpectraPeak> ret =  new SortedLinkedList<SpectraPeak>(SpectraPeak.INTENSITY_COMPARATOR);
        for (SpectraPeak p: this) {
            ret.add(p);
        }
        return ret;
    }

    /**
     * sums up all intensities, that are not matched to anything and do not 
     * belong to a matched isotop cluster
     * @return
     */
    public double getUnexplainedIntensity() {
        double unexplained = 0;

        // sum up unmatched clusters
        for (SpectraPeakCluster c : getIsotopeClusters()) {
            if (c.get(0).getMatchedFragments().size() == 0) {
                for (SpectraPeak sp : getPeaks())
                    unexplained += sp.getIntensity();
            }
        }

        // sum up remaining unmatched peaks
        for (SpectraPeak sp : getPeaks()) {
            if ((! sp.hasAnnotation(SpectraPeakAnnotation.isotop))
                 && sp.getMatchedFragments().size() == 0) {
                    unexplained += sp.getIntensity();
            }
        }
        return unexplained;
    }



    /**
     * to ease the work of the garbage collector we can unlink everything
     */
    public void free() {
            m_PeakTree.clear();
            m_PrecurserChargeAlternatives = null;
            m_isotopClusters.clear();
            if (m_matches != null)
                m_matches.clear();
            m_source = "";
            m_Tolerance = null;
            m_run = null;
            m_scan_number = -1;
//        if (m_PeakTree != null) {
//            for (SpectraPeak p : m_PeakTree.values())
//                p.free();
//            m_PeakTree.clear();
//            m_PeakTree = null;
//        }
//        if (m_isotopClusters != null) {
//            m_isotopClusters.clear();
//            m_isotopClusters = null;
//        }
//        if (m_matches != null) {
//            m_matches.clear();
//            m_matches = null;
//        }
    }


    /**
     * used internally to decide whether a cluster should be included in the mgc/mgx Spectra
     * @param cluster
     * @return
     */
    private boolean mgcSkipCluster(SpectraPeakCluster cluster) {

        // mass has to be bigger than the smalles aminoacid
        if (cluster.getMZ() * cluster.getCharge() < AminoAcid.MINIMUM_MASS)
            return true;

        // mass has to be smaller than the precursor minus the smallest aminoacid
        if (cluster.getMZ() * cluster.getCharge() > getPrecurserMass() - AminoAcid.MINIMUM_MASS)
            return true;

        // not a loss
        for (Double da : SPECTRALOSSSES) {
            double mz = da / cluster.getCharge() + cluster.getMZ();
            // ignore clusters that match to a loss of another cluster with defined minimal intesity
            //if (m_isotopClusters.hasCluster(mz, cluster.getCharge(), minLossCheck))
            if (m_isotopClusters.hasCluster(mz, cluster.getCharge()))
                return true; // ignore lossy peaks
        }

        return false;
    }

    /**
     * used internally to decide whether a peak should be included in the mgc/mgx Spectra
     * @param cluster
     * @return
     */
    private boolean mgcSkipPeak(SpectraPeak peak) {

        // mass has to be bigger than the smalles aminoacid
        if (peak.getMZ() < AminoAcid.MINIMUM_MASS)
            return true;

        // mass has to be smaller than the precursor minus the smallest aminoacid
        if (peak.getMZ() > getPrecurserMass() - AminoAcid.MINIMUM_MASS)
            return true;

        return false;
    }

    /**
     * returns a copy of this spectra, in which all isotop-clusters are
     * collapsed, dechraged and linearised.
     * returns only the specified number of most intents peaks
     * @param topPeaks
     * @return
     */
    public Spectra getMgcSpectra(int topPeaks) {
        Spectra AllMGC = getMgcSpectra();
        if (topPeaks > 0) {
            Spectra topMGC = AllMGC.cloneEmpty();
            HashSet<SpectraPeak> foundOnes = new HashSet<SpectraPeak>(topPeaks);
            int c=0;
            for (SpectraPeak p : AllMGC.getTopPeaks(-1)) {
                if (c < topPeaks || ((!foundOnes.contains(p)) && p.hasAnnotation(MGC_MATCHED) && p.hasAnnotation(MGC_MATCHED_COMPLEMENT))) {
                    topMGC.addPeak(p);
                    foundOnes.add(p);
                }
            }

            free(AllMGC);

            return topMGC;
        } else {
            return AllMGC;
        }
    }

    /**
     * returns a copy of this spectra, in which all isotop-clusters are
     * collapsed, dechraged and linearised.
     * returns only the specified number of most intents peaks
     * @param topPeaks
     * @return
     */
    public Spectra getMgcSpectra(int topPeaks, double window) {
        Spectra AllMGC = getMgcSpectra();
        if (topPeaks > 0) {
            Spectra topMGC = AllMGC.cloneEmpty();
            HashSet<SpectraPeak> foundOnes = new HashSet<SpectraPeak>(topPeaks);
            TreeMap<Double,SpectraPeak> windowedPeaks = new TreeMap<Double, SpectraPeak>();
            int c=0;
            for (SpectraPeak p : AllMGC.getTopPeaks(-1)) {
                c = topMGC.m_PeakTree.subMap(p.getMZ()-window, p.getMZ()).size();
                if (c < topPeaks || ((!foundOnes.contains(p)) && p.hasAnnotation(MGC_MATCHED) && p.hasAnnotation(MGC_MATCHED_COMPLEMENT))) {
                    topMGC.addPeak(p);
                    foundOnes.add(p);
                }
            }

            free(AllMGC);

            return topMGC;
        } else {
            return AllMGC;
        }
    }

    /**
     * returns an deisotoped and decharged spectra with linearised peaks
     * Isotop clusters  with charge state > 2 are assumed to be crosslinker containing
     * and get converted into the linear one. <br/>
     * These with charge state 2 get duplicated as possible linear and
     * possible crossliniked ones.
     * @return simplified spectra
     */
    public Spectra getMgcSpectra() {
        Spectra mgcSpectra = this.cloneEmpty();
//        double median = getMedianIntensity();
//        double stdev  = getStdDevIntensity();
//        double minLossCheck = median - stdev;

        if (getIsotopeClusters().size() == 0) {
            DEFAULT_ISOTOP_DETECTION.anotate(this);
        }


        // transfer the cluster-peaks
        clusters: for (SpectraPeakCluster cluster : getIsotopeClusters()) {

            if (mgcSkipCluster(cluster))
                continue clusters; // ignore lossy peaks

            SpectraPeak p = cluster.toPeak(true,false).clone();
            if (cluster.getCharge() <= 2) {
                p.annotate(MGC_MATCHED);
                mgcSpectra.addPeakIntesity(p);
            }
            if (cluster.getCharge() >= 2) {
                p = p.clone();
                p.setMZ(getPrecurserMass() - (p.getMZ() - Util.PROTON_MASS) + Util.PROTON_MASS); // convert to (possible) linear
                p.annotate(MGC_MATCHED_COMPLEMENT);
                mgcSpectra.addPeakIntesity(p); // add
            }
        }

        // transfer single peaks
        for (SpectraPeak p: getPeaks()) {
            if (mgcSkipPeak(p))
                continue;
//            if ((int)p.getMZ() == 761 || (int)p.getMZ() == 762)
//                p.getMZ();
            boolean isIsotop = p.hasAnnotation(SpectraPeakAnnotation.isotop);
            if (isIsotop == false) {
                p=p.clone();
                p.annotate(MGC_MATCHED);
                mgcSpectra.addPeakIntesity(p);
            }
        }
        this.getIsotopeClusters().clear();

        return mgcSpectra;
    }

    /**
     * returns an deisotoped and decharged spectra with linearised peaks
     * Isotop clusters  with charge state > 2 are assumed to be crosslinker containing
     * and get converted into the linear one. <br/>
     * These with charge state 2 get duplicated as possible linear and
     * possible crossliniked ones.
     * @return simplified spectra
     */
    public Spectra getOMSpectra() {
        Spectra mgcSpectra = this.cloneEmpty();
//        double median = getMedianIntensity();
//        double stdev  = getStdDevIntensity();
//        double minLossCheck = median - stdev;

        if (getIsotopeClusters().size() == 0) {
            DEFAULT_ISOTOP_DETECTION.anotate(this);
        }


        // transfer the cluster-peaks
        clusters: for (SpectraPeakCluster cluster : getIsotopeClusters()) {

            if (mgcSkipCluster(cluster))
                continue clusters; // ignore lossy peaks

            SpectraPeak p = cluster.toPeak(true,false).clone();
            // if it does not contain the modification it should match directly
            p.annotate(MGC_MATCHED);
            mgcSpectra.addPeakIntesity(p);
            p = p.clone();

            p = cluster.toPeak(true,false).clone();
            // if it does contain the modification the invers peak should not
            p.setMZ(getPrecurserMass() - (p.getMZ() - Util.PROTON_MASS) + Util.PROTON_MASS); // convert to (possible) linear
            p.annotate(MGC_MATCHED_COMPLEMENT);
            mgcSpectra.addPeakIntesity(p); // add
        }

        // transfer single peaks
        for (SpectraPeak p: getPeaks()) {
            if (mgcSkipPeak(p))
                continue;
//            if ((int)p.getMZ() == 761 || (int)p.getMZ() == 762)
//                p.getMZ();
            boolean isIsotop = p.hasAnnotation(SpectraPeakAnnotation.isotop);
            if (isIsotop == false) {
                p=p.clone();
                p.annotate(MGC_MATCHED);
                mgcSpectra.addPeakIntesity(p);

                p=p.clone();
                p.setMZ(getPrecurserMass() - (p.getMZ() - Util.PROTON_MASS) + Util.PROTON_MASS); // convert to (possible) linear
                p.annotate(MGC_MATCHED_COMPLEMENT);
                mgcSpectra.addPeakIntesity(p);
            }
        }
        this.getIsotopeClusters().clear();

        return mgcSpectra;
    }


    /**
     * returns an de-isotoped and de-charged spectra.
     * @return simplified spectra
     */
    public Spectra getMgxSpectra() {
        Spectra mgcSpectra = this.cloneEmpty();

        // if till now no isotop-peaks where anotated
        if (getIsotopeClusters().size() == 0)  // anotate them now
            DEFAULT_ISOTOP_DETECTION.anotate(this);

        // transfer the cluster-peaks
        clusters: for (SpectraPeakCluster cluster : getIsotopeClusters()) {
            if (mgcSkipCluster(cluster))
                continue clusters; // ignore lossy peaks

            mgcSpectra.addPeakIntesity(cluster.toPeak(true,false));
        }

        // transfer single peaks
        for (SpectraPeak p: getPeaks()) {
            if (mgcSkipPeak(p))
                continue;
            if (!p.hasAnnotation(SpectraPeakAnnotation.isotop)) {
                mgcSpectra.addPeakIntesity(p.clone());
            }
        }
        return mgcSpectra;
    }

    public Spectra getMgxSpectra(int topPeaks) {
        Spectra AllMGX = getMgxSpectra();
        if (topPeaks > 0) {
            Spectra topMGX = AllMGX.cloneEmpty();
            HashSet<SpectraPeak> foundOnes = new HashSet<SpectraPeak>(topPeaks);
            for (SpectraPeak p : AllMGX.getTopPeaks(topPeaks)) {
                topMGX.addPeak(p);
                foundOnes.add(p);
            }

            for (SpectraPeak p : AllMGX) {
                if ((!foundOnes.contains(p)) && p.hasAnnotation(MGC_MATCHED) && p.hasAnnotation(MGC_MATCHED_COMPLEMENT)) {
                    topMGX.addPeak(p);
//                    System.out.println("extended top peaks");
                }
            }
            AllMGX.free();
            return topMGX;
        } else {
            return AllMGX;
        }
    }


    /**
     * returns an iterator over the peaks
     * @return
     */
    public Iterator<SpectraPeak> iterator() {
        return getPeaks().iterator();
    }

    /**
     * Returns a trying representation of a spectrum
     * @return
     */
    @Override
    public String toString(){

        String rep = "";

        rep += "Run\tScan\tPrecursor MZ\t\tCharge\tIntensity\tElution Time\n";
        rep += this.m_run + "\t" + this.m_scan_number + "\t" + this.m_PrecurserMZ + "\t\t" + this.m_PrecurserCharge + "\t" +
                this.m_PrecurserIntesity + "\t\t" + this.m_ElutionTimeStart + "\n";

        rep+= "IONS\nMZ\t\tIntensity\n";

        //int peak_count = this.m_Peaks.size();
        for(SpectraPeak sp : getPeaks()){
            
            rep += Util.fourDigits.format(sp.getMZ())
                    + "\t" + Util.fourDigits.format(sp.getIntensity())
                    + "\t" +
                    (sp.hasAnnotation(SpectraPeakAnnotation.monoisotop)? "M" + sp.getCharge():(
                     sp.hasAnnotation(SpectraPeakAnnotation.isotop) ? "I" + sp.getCharge():"")) + "\n";
        }


        return rep;
    }

    /**
     * returns a list of matched fragments - grouped by the non-lossy peak and
     * the charge
     * @return
     */
    public MatchedFragmentCollection getMatchedFragmentGroups() {
        MatchedFragmentCollection mfc = new MatchedFragmentCollection(getPrecurserCharge());
        //m_deisotoped = s.deIsotop();
        // retrive all the matched fragments
        for (SpectraPeak sp : getPeaks()) {
            for (SpectraPeakMatchedFragment mf : sp.getMatchedAnnotation()) {
                Fragment f = mf.getFragment();
                mfc.add(f, mf.getCharge(), sp);
            }
        }
        return mfc;
    }

    public HashSet<Fragment> getMatchedFragments() {
        HashSet<Fragment> ret = new HashSet<Fragment>();
        for (SpectraPeak sp: this) {
            for (Fragment f : sp.getMatchedFragments()) {
                ret.add(f);
            }
        }
        return ret;
    }

    public ArrayList<SpectraPeak> getMatchedPeaks() {
        ArrayList<SpectraPeak> ret = new ArrayList<SpectraPeak>();
        for (SpectraPeak sp: this) {
            if (sp.getMatchedAnnotation().size() > 0)
                ret.add(sp);
        }
        return ret;
    }


    /**
     * Returns a list of matched annotations, reduced, so that each peak has at most one annotation.
     * It first takes all peaks with only one annotation, and uses these and the
     * ppm error of annotations as a guide line for deciding about other
     * annotations.
     * @return
     *
     */
    public MatchedFragmentCollection getUniqueMatches() {
        ArrayList<SpectraPeak> nonUniqe = new ArrayList<SpectraPeak>();
        Spectra s = this.clone();
        MatchedFragmentCollection mfc = new MatchedFragmentCollection(getPrecurserCharge());
        CountOccurence<Peptide> countMatches = new CountOccurence<Peptide>();
        // first transfer all singel peak matches
        for (SpectraPeak sp : this) {
            ArrayList<SpectraPeakMatchedFragment> tm = sp.getMatchedAnnotation();

            if (tm.size() >1)
                nonUniqe.add(sp);
            else if (tm.size() == 1) {
                SpectraPeakMatchedFragment spmf = tm.get(0);
                countMatches.increment(spmf.getFragment().getPeptide());
                mfc.add(spmf, sp);
            }
        }

        
        for (SpectraPeak sp : nonUniqe) {
            ArrayList<SpectraPeakMatchedFragment> num = sp.getMatchedAnnotation();
            SpectraPeakMatchedFragment mf = null;
            Peptide p;
            double mz = sp.getMZ();
            int type = 0;
            double error = Double.MAX_VALUE;
            double halfError = m_Tolerance.getMinRange(sp.getMZ()) + (m_Tolerance.getMaxRange(sp.getMZ()) - m_Tolerance.getMinRange(sp.getMZ()))/4;
            halfError = Math.abs(halfError - sp.getMZ());

            for (SpectraPeakMatchedFragment spmf: num) {
                int fClass = 0;

                Fragment f = spmf.getFragment();

                if (f.isClass(Loss.class)) {
                    fClass = 2;
                } else if (f.isClass(SecondaryFragment.class)) {
                    fClass = 1;
                } else  {
                    fClass = 3;
                }
                double ferror = Math.abs(spmf.getFragment().getMZ(spmf.getCharge())-mz);

                // if the error is to big assume the
                if (ferror < error - halfError) {
                    mf = spmf;
                    error = ferror;
                } else if(ferror > error - halfError && ferror < error + halfError)  {
                    // we are with in the permited error window
                    // take the most "informative" class
                    if (fClass > type) {
                        mf = spmf;
                        error = Math.abs(spmf.getFragment().getMZ(spmf.getCharge())-mz);
                    } else if (fClass == type) { // or if the same the one explanation with the least error

                        if (ferror < error) {

                            mf = spmf;
                            error = ferror;

                        } else if (ferror == error) {
                            // in case we have the same error count it to the dominant peptide

                            if (countMatches.count(spmf.getFragment().getPeptide()) >
                                countMatches.count(mf.getFragment().getPeptide())) {
                                mf = spmf;
                                error = ferror;
                            }

                        }
                    }
                }

            }

            countMatches.increment(mf.getFragment().getPeptide());
            mfc.add(mf, sp);

            
        }


        // now look all the multible matches and try to identifie the most likely/meaningfull one

        return null;



    }

    /**
     * @return the IDs of matched alpha-peptides
     */
    public ArrayList<PreliminaryMatch> getPreliminaryMatch() {
        return m_matches;
    }

    public void addPreliminaryMatch(PreliminaryMatch pm) {
        this.m_matches.add(pm);
    }

    public void addPreliminaryMatch(ArrayList<PreliminaryMatch> pm) {
        this.m_matches.addAll(pm);
    }

    public void addPreliminaryMatches(PreliminaryMatch[] preliminaryMatchs) {
        for (PreliminaryMatch pm : preliminaryMatchs) {
            addPreliminaryMatch(pm);
        }
    }

    
    public int getReadID() {
        return m_readid;
    }

    public void setReadID(int id) {
        m_readid = id;
    }



    public long getID() {
        return m_id.value;
    }

    public void setID(long id) {
        m_id.value = id;
    }

    public void setID(UpdateableLong id) {
        m_id = id;
    }
    
    public long getRunID() {
        return m_RunId;
    }

    public void setRunID(long id) {
        m_RunId = id;
    }


    public long getAcqID() {
        return m_AcqId;
    }

    public void setAcqID(long id) {
        m_AcqId = id;
    }
    
    public Spectra[] getChargeStateSpectra() {
        if (m_PrecurserChargeAlternatives.length == 1) {
            return new Spectra[]{this};
        } else{
            Spectra[] ret = new Spectra[m_PrecurserChargeAlternatives.length];
            for (int i = 0; i< ret.length; i++) {
                Spectra s = cloneComplete();
                s.setPrecurserCharge(m_PrecurserChargeAlternatives[i]);
                ret[i] = s;
            }
            return ret;
        }
    }

    /**
     * if the spectra is of undefined charge state return one spectra for each charge state and -1, and -2 da precursor m/z
     * @return 
     */
    public ArrayList<Spectra> getAlternativeSpectra() {
        ArrayList<Spectra> ret = new ArrayList<Spectra>();

        HashSet<Integer> charges = new HashSet<>();
        HashSet<Double> mzs = new HashSet<>();
        
        if (getAdditionalCharge() != null) {
            charges.addAll(getAdditionalCharge());
        } else if (m_PrecurserChargeAlternatives.length >1) {
            // the precoursor carries no reliable charge information
            setAdditionalCharge(m_PrecurserChargeAlternatives);
            charges.addAll(getAdditionalCharge());
        }
        
        if (m_PrecurserCharge > 0) {
            charges.add(m_PrecurserCharge);
        }
        
        if (getAdditionalMZ() != null) {
            mzs.addAll(getAdditionalMZ());
        }
        
        mzs.add(this.getPrecurserMZ());
        
        if (m_PrecurserChargeAlternatives.length == 1) {
            charges.add(this.getPrecurserCharge());
        }
        
        for (int charge : charges) {
            for (double mz :mzs) {
                Spectra s = cloneComplete();
                s.setPrecurserCharge(charge);
                if (mz<100) {
                    double mass = s.getPrecurserMass()+mz;
                    
                    s.setPrecurserMass(mass);
                } else
                    s.setPrecurserMZ(mz);
                ret.add(s);                        
            }
        }

        return ret;
    }

    
    /**
     * if the spectra is of undefined charge state return one spectra for each charge state and -1, and -2 da precursor m/z
     * @return 
     */
    public ArrayList<Spectra> getAlternativeSpectraOld() {
        ArrayList<Spectra> ret = new ArrayList<Spectra>();
        
        if (m_PrecurserChargeAlternatives.length == 1) {
            ret.add(this);
        } else{
            
            // the precoursor carries no reliable charge information
            for (int i = 0; i< m_PrecurserChargeAlternatives.length; i++) {
                Spectra s = cloneComplete();
                s.setPrecurserCharge(m_PrecurserChargeAlternatives[i]);
                ret.add(s);
//                // as no charge state was there we can assume that the observed 
//                // peak has a good chance of not beeing the monoisotopic one
//                if (s.getPrecurserMass() > 2000) {
//                    Spectra precCor = s.cloneComplete();
//                    precCor.setPrecurserMZ(precCor.getPrecurserMZ() - 1.00335/m_PrecurserChargeAlternatives[i]);
//                    ret.add(precCor);
//                }
//                if (s.getPrecurserMass() > 4500) {
//                    Spectra precCor = s.cloneComplete();
//                    precCor.setPrecurserMZ(precCor.getPrecurserMZ() - 2*1.00335/m_PrecurserChargeAlternatives[i]);
//                    ret.add(precCor);
//                }
//                if (s.getPrecurserMass() > 7500) {
//                    Spectra precCor = s.cloneComplete();
//                    precCor.setPrecurserMZ(precCor.getPrecurserMZ() - 3*1.00335/m_PrecurserChargeAlternatives[i]);
//                    ret.add(precCor);
//                }
                
            }
        }
        return ret;
    }

    /**
     * assumes, that there is a spectra
     * @return 
     */
    public ArrayList<Spectra> getRelaxedAlternativeSpectra() {
        ArrayList<Spectra> ret = new ArrayList<Spectra>();
        Spectra[]  specs = getChargeStateSpectra();
        boolean unknowCharge = specs.length>1;
        if (!unknowCharge) {
            Spectra s = specs[0];
            ret.add(s);
            // and possibly another
            if (s.getPrecurserMass() > 7500) {
                s = s.cloneComplete();
                s.setPrecurserMZ(s.getPrecurserMZ() - Util.C13_MASS_DIFFERENCE/s.getPrecurserCharge());
                ret.add(s);
            }
            
        } else {
            for (Spectra s : specs) {
                
                // if it is a rather large mass assume that we don't see the mono-isotopic peak
                if (s.getPrecurserMass()<=7500)
                    ret.add(s);

                // as no charge state was there we can assume that the observed 
                // peak has a good chance of not beeing the monoisotopic one
                if ( s.getPrecurserMass() > 2000) {
                    s = s.cloneComplete();
                    s.setPrecurserMZ(s.getPrecurserMZ() - Util.C13_MASS_DIFFERENCE/s.getPrecurserCharge());
                    ret.add(s);
                }
                
                // if it was larger we assume that maybe another isotope peak was missing
                if (s.getPrecurserMass() > 4500) {
                    s = s.cloneComplete();
                    s.setPrecurserMZ(s.getPrecurserMZ() - Util.C13_MASS_DIFFERENCE/s.getPrecurserCharge());
                    ret.add(s);
                }
                
//                // and possibly another
//                if (s.getPrecurserMass() > 7500) {
//                    s = s.cloneComplete();
//                    s.setPrecurserMZ(s.getPrecurserMZ() - 1.00335/s.getPrecurserCharge());
//                    ret.add(s);
//                }

            }
        }
        return ret;
    }
    
    
    public Spectra[] getChargeStateSpectra(boolean missingMonoIsotopic) {
        if (missingMonoIsotopic) {
             
            if (m_PrecurserChargeAlternatives.length == 1) { 
                Spectra mm = this.cloneComplete();
                mm.setPrecurserMZ(mm.getPrecurserMZ()-1/mm.getPrecurserCharge());
                return new Spectra[]{this,mm};
            } else {
                Spectra[] ret = new Spectra[m_PrecurserChargeAlternatives.length*2];
                for (int i = 0; i< ret.length*2; i++) {
                    Spectra s = cloneComplete();
                    s.setPrecurserCharge(m_PrecurserChargeAlternatives[i]);
                    ret[i] = s;
                    s = s.cloneComplete();
                    s.setPrecurserMZ(s.getPrecurserMZ()-1/m_PrecurserChargeAlternatives[i]);
                    ret[i++] = s;
                }
                return ret;
            }
        } else {
            if (m_PrecurserChargeAlternatives.length == 1) {
                return new Spectra[]{this};
            } else{
                Spectra[] ret = new Spectra[m_PrecurserChargeAlternatives.length];
                for (int i = 0; i< ret.length; i++) {
                    Spectra s = cloneComplete();
                    s.setPrecurserCharge(m_PrecurserChargeAlternatives[i]);
                    ret[i] = s;
                }
                return ret;
            }
        }
    }
    
    


    public HashMap<SpectraPeak, SpectraPeakMatchedFragment> getPrimaryMatches(MatchedFragmentCollection mfc) {
        HashMap<SpectraPeak, SpectraPeakMatchedFragment> ret = new HashMap<SpectraPeak, SpectraPeakMatchedFragment>();
        for (SpectraPeak sp : this) {
            ArrayList<SpectraPeakMatchedFragment> matches = sp.getMatchedAnnotation();
            if (matches.size() > 0) {
                int mc = 0;
                double minErr = Double.MAX_VALUE;
                SpectraPeakMatchedFragment pm = null;
                for (SpectraPeakMatchedFragment mf : matches) {
                    Fragment f = mf.getFragment();
                    MatchedBaseFragment mbf = mfc.getMatchedFragmentGroup(f, mf.getCharge());
                    int c;
                    if (f.getFragmentationSites().length>1)
                        c = 1;
                    else if (f.isClass(Loss.class)){
                        if (mbf.isBaseFragmentFound())
                            c = 3;
                        else if (mbf.getLosses().size() > 2)
                            c = 2;
                        else
                            c = 1;
                    } else
                        c = 4;

                    double err = Math.abs(mf.getMZ() - sp.getMZ());

                    if (c > mc || (c == mc && err < minErr)) {
                        mc = c;
                        pm = mf;
                        minErr = err;
                    }  else if (c == mc && err == minErr) {
                        if (pm.getFragment().toString().compareTo(mf.getFragment().toString()) < 0) {
                            pm = mf;
                        }
                    }

                }
                ret.put(sp, pm);
            }
        }
        return ret;

    }
    
    
    public int getHighestFragmentCharge() {
        Spectra s = this;
        if (this.m_isotopClusters == null || this.m_isotopClusters.isEmpty()) {
            s = this.clone();
            DEFAULT_ISOTOP_DETECTION.anotate(this);
        }
        
        SpectraPeakClusterList spcl = s.getIsotopeClusters();
        int charge = 0;
        for (SpectraPeakCluster spc : spcl) {
            int spcCharge = spc.getCharge();
            if (spcCharge > charge)
                charge = spcCharge;
        }
        
        if (charge == 0)
            return 2;
        
        return charge;
    }

    @Override
    public boolean hasPeakAt(double mz) {
        return m_PeakTree.containsKey(mz);
    }

    @Override
    public int hashCode() {
        return getRun().hashCode() + getScanNumber() + getPeaks().size();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Spectra) {
            Spectra s = (Spectra) o;
            if (getRun().contentEquals(s.getRun()) && getScanNumber() == s.getScanNumber() && getPeaks().size() == s.getPeaks().size()) {
                return true;
            } else
                return false;
        } else
            return false;
    }

    /**
     * @return the m_additional_mz
     */
    public ArrayList<Double> getAdditionalMZ() {
        return m_additional_mz;
    }

    /**
     * @param m_additional_mz the m_additional_mz to set
     */
    public void setAdditionalMZ(Collection<Double> additional_mz) {
        if (additional_mz == null || additional_mz.isEmpty())
            this.m_additional_mz = null;
        else
            this.m_additional_mz = new ArrayList<>(additional_mz);        
        
    }

    /**
     * @return the m_additional_charge
     */
    public ArrayList<Integer> getAdditionalCharge() {
        return m_additional_charge;
    }

    /**
     * @param m_additional_charge the m_additional_charge to set
     */
    public void setAdditionalCharge(Collection<Integer> additional_charge) {
        if (additional_charge == null || additional_charge.isEmpty())
            this.m_additional_charge = null;
        else
            this.m_additional_charge = new ArrayList<>(additional_charge);
    }

    /**
     * @param m_additional_charge the m_additional_charge to set
     */
    public void setAdditionalCharge(int[] additional_charge) {
        if (additional_charge == null || additional_charge.length==0)
            this.m_additional_charge = null;
        else {
            this.m_additional_charge = new ArrayList<>(additional_charge.length);
            for (int c : additional_charge) {
                this.m_additional_charge.add(c);
            }
        }
    }

    /**
     * if this spectrum is based upon a different spectrum (i.e. cloned) then this should return the original one
     * otherwise the spectrum itself is returned
     * @return 
     */
    public Spectra getOrigin() {
        return this.origin;
    }

    public void setOrigin(Spectra s) {
        this.origin = s;
    }
    
    public double getMinMz(){
        return m_PeakTree.firstKey();
    }

    public double getMaxMz(){
        return m_PeakTree.lastKey();
    }

    /**
     * if given then this contains a list of masses that are likely masses fro the peptide candidates
     * @return the PeptideCandidateMasses
     */
    public ArrayList<Double> getPeptideCandidateMasses() {
        return m_peptideCandidateMasses;
    }

    /**
     * if given then this contains a list of masses that are likely masses fro the peptide candidates
     * @return the PeptideCandidateMasses
     */
    public ArrayList<Double> getPeptideCandidateMassWeights() {
        return m_peptideCandidateMassWeights;
    }
    
    /**
     * if given then this contains a list of masses that are likely masses fro the peptide candidates
     * @param PeptideCandidateMasses the PeptideCandidateMasses to set
     */
    public void setPeptideCandidateMasses(ArrayList<Double> PeptideCandidateMasses) {
        if (PeptideCandidateMasses == null) {
            this.m_peptideCandidateMasses = null;
            this.m_peptideCandidateMassWeights = null;
        } else {
            this.m_peptideCandidateMasses = PeptideCandidateMasses;
            this.m_peptideCandidateMassWeights = new ArrayList<>(java.util.Collections.nCopies(PeptideCandidateMasses.size(), 1d));
        }
    }

    /**
     * if given then this contains a list of masses that are likely masses fro the peptide candidates
     * @param PeptideCandidateMasses the PeptideCandidateMasses to set
     */
    public void setPeptideCandidateMasses(String PeptideCandidateMasses) {
        String[] smasses = PeptideCandidateMasses.split(";");
        ArrayList<Double> masses = new ArrayList<Double>(smasses.length);
        ArrayList<Double> weights = new ArrayList<Double>(smasses.length);
        for (String sm : smasses) {
            int c = sm.indexOf("(");
            if (c>=0) {
                double m = Double.parseDouble(sm.substring(0,c).trim());
                double w = Double.parseDouble(sm.substring(c+1, sm.indexOf(")")).trim());
                masses.add(m);
                weights.add(w);
            } else {
                double m = Double.parseDouble(sm.trim());
                masses.add(m);
                weights.add(1d);
            }
        }
        this.m_peptideCandidateMasses = masses;
        this.m_peptideCandidateMassWeights = weights;
    }
    
    
    public void addPeptideCandidateMass(Double mass) {
        if (this.m_peptideCandidateMasses == null) {
            this.m_peptideCandidateMasses = new ArrayList<>();
            this.m_peptideCandidateMassWeights = new ArrayList<>();
        }
        this.m_peptideCandidateMasses.add(mass);
        this.m_peptideCandidateMassWeights.add(1d);
    }

    public void addPeptideCandidateMass(Double mass, Double weight) {
        if (this.m_peptideCandidateMasses == null) {
            this.m_peptideCandidateMasses = new ArrayList<>();
            this.m_peptideCandidateMassWeights = new ArrayList<>();
        }
        this.m_peptideCandidateMasses.add(mass);
        this.m_peptideCandidateMassWeights.add(weight);
    }
    
    /**
     * the tolerance to use with the peptide candidate masses
     * @return the PeptideCandidateTolerance
     */
    public ToleranceUnit getPeptideCandidateTolerance() {
        return m_peptideCandidateTolerance;
    }

    /**
     * the tolerance to use with the peptide candidate masses
     * @param PeptideCandidateTolerance the PeptideCandidateTolerance to set
     */
    public void setPeptideCandidateTolerance(ToleranceUnit PeptideCandidateTolerance) {
        this.m_peptideCandidateTolerance = PeptideCandidateTolerance;
    }

    /**
     * the physical file that the spectrum was read from
     * @return the m_peakFileName
     */
    public String getPeakFileName() {
        return m_peakFileName;
    }

    /**
     * the physical file that the spectrum was read from
     * @param m_peakFileName the m_peakFileName to set
     */
    public void setPeakFileName(String m_peakFileName) {        
        this.m_peakFileName = m_peakFileName;
    }

    /**
     * E.g. the TITLE tag from an mgf-file
     * @return the scanTitle
     */
    public String getScanTitle() {
        return scanTitle;
    }

    /**
     * E.g. the TITLE tag from an mgf-file
     * @param scanTitle the scanTitle to set
     */
    public void setScanTitle(String scanTitle) {
        this.scanTitle = scanTitle;
    }
    
}
