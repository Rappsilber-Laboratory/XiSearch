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
package rappsilber.config;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.applications.SimpleXiProcessLinearIncluded;
import rappsilber.applications.XiProcess;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.dataAccess.AbstractSpectraAccess;
import rappsilber.ms.dataAccess.AbstractStackedSpectraAccess;
import rappsilber.ms.dataAccess.StackedSpectraAccess;
import rappsilber.ms.dataAccess.filter.spectrafilter.AbstractSpectraFilter;
import rappsilber.ms.dataAccess.filter.spectrafilter.DeIsotoper;
import rappsilber.ms.dataAccess.filter.spectrafilter.Denoise;
import rappsilber.ms.dataAccess.filter.spectrafilter.MS2PrecursorDetection;
import rappsilber.ms.dataAccess.filter.spectrafilter.PeakFilteredSpectrumAccess;
import rappsilber.ms.dataAccess.filter.spectrafilter.Rebase;
import rappsilber.ms.dataAccess.filter.spectrafilter.RemoveSinglePeaks;
import rappsilber.ms.dataAccess.filter.spectrafilter.ScanFilteredSpectrumAccess;
import rappsilber.ms.dataAccess.output.ResultWriter;
import rappsilber.ms.score.ScoreSpectraMatch;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.AminoLabel;
import rappsilber.ms.sequence.AminoModification;
import rappsilber.ms.sequence.NonAminoAcidModification;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ms.sequence.digest.Digestion;
import rappsilber.ms.sequence.ions.BasicCrossLinkedFragmentProducer;
import rappsilber.ms.sequence.ions.CrosslinkedFragment;
import rappsilber.ms.sequence.ions.DoubleFragmentation;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.annotation.Averagin;
import rappsilber.ms.spectra.annotation.IsotopPattern;
import rappsilber.ui.LoggingStatus;
import rappsilber.ui.StatusInterface;
import rappsilber.utils.SortedLinkedList;
import rappsilber.ms.sequence.ions.CrossLinkedFragmentProducer;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public abstract class AbstractRunConfig implements RunConfig {

    public static final int DEFAULT_MAX_LOSSES  =  4;
    public static final int DEFAULT_MAX_TOTAL_LOSSES = 6;
    
    public static AbstractRunConfig DUMMYCONFIG = new AbstractRunConfig() {
    };

    private ArrayList<Method> m_losses = new ArrayList<Method>();

    private ArrayList<AminoModification> m_fixed_mods = new ArrayList<AminoModification>();
    private ArrayList<AminoModification> m_linear_mods = new ArrayList<AminoModification>();
    private ArrayList<AminoModification> m_var_mods = new ArrayList<AminoModification>();
    private ArrayList<AminoModification> m_known_mods = new ArrayList<AminoModification>();
    private ArrayList<NonAminoAcidModification> m_NTermPepMods = new ArrayList<NonAminoAcidModification>();
    private ArrayList<NonAminoAcidModification> m_CTermPepMods = new ArrayList<NonAminoAcidModification>();
    private ArrayList<NonAminoAcidModification> m_NTermProtMods = new ArrayList<NonAminoAcidModification>();
    private ArrayList<NonAminoAcidModification> m_CTermProtMods = new ArrayList<NonAminoAcidModification>();
    private ArrayList<AminoLabel> m_label = new ArrayList<AminoLabel>();
    private HashMap<Integer,ArrayList<AminoLabel>> m_labelShemes = new HashMap<Integer,ArrayList<AminoLabel>>();

    private HashMap<AminoAcid,ArrayList<AminoModification>> m_mapped_fixed_mods = new HashMap<AminoAcid, ArrayList<AminoModification>>();
    private HashMap<AminoAcid,ArrayList<AminoModification>> m_mapped_linear_mods = new HashMap<AminoAcid, ArrayList<AminoModification>>();
    private HashMap<AminoAcid,ArrayList<AminoModification>> m_mapped_var_mods  = new HashMap<AminoAcid, ArrayList<AminoModification>>();
    private HashMap<AminoAcid,ArrayList<AminoModification>> m_mapped_known_mods  = new HashMap<AminoAcid, ArrayList<AminoModification>>();
    private HashMap<AminoAcid,ArrayList<AminoLabel>> m_mapped_label = new HashMap<AminoAcid, ArrayList<AminoLabel>>();
    private ArrayList<CrossLinker> m_crosslinker = new ArrayList<CrossLinker>();
    private Digestion   m_digestion;
    private ToleranceUnit m_PrecoursorTolerance;
    private ToleranceUnit m_FragmentTolerance;
    private ToleranceUnit m_FragmentToleranceCandidate;
    private IsotopPattern m_isotopAnnotation;
    private int           m_topMGCHits = 10;
    private int           m_topMGXHits = -1;
    private StatusInterface m_status = new status_multiplexer();
    private ArrayList<StatusInterface> m_status_publishers = new ArrayList<StatusInterface>(2);
    private int           m_maxpeps = 2;
    
    private int           m_maxFragmentCandidates = -1;
    
    /**
     * a flag to indicate if the current search should be stopped.
     * I.e. all search threads should close down after this flag is set.
     */
    private volatile boolean       m_searchStoped=false;
    
    private SequenceList.DECOY_GENERATION m_decoyTreatment = SequenceList.DECOY_GENERATION.ISTARGET;
    
    

    /** This flags up, if we do a  low-resolution search, meaning de-isotoping is ignored */
    private Boolean       m_LowResolution = null;
    
    /**
     * the maximum mass of a peptide to be considered.
     * for values &lt;0 this gets defined by the largest precursor in the peak-list
     */
    private double        m_maxPeptideMass = -1;

    /**
     * check for peptides being non-covalently bound
     */
    private boolean       m_CheckNonCovalent= false;

    /**
     * Also match linear peptides to spectra
     */
    private boolean       m_EvaluateLinears = true;
    
    /**
     * the number of concurent search threads
     * values &lt;0 should indicate that a according number of detected cpu-cores 
     * should not be used.
     * E.g. if the computer has 16 cores and the setting is -1 then 15 search 
     * threads should be started
     * otherwise the number of threads defined should be used.
     */
    private int           m_SearchThreads = calculateSearchThreads(-1);
    
    /**
     * Should only the top-ranking matches be written out?
     */
    private boolean       m_topMatchesOnly = false;
    
    /**
     * list of objects that can produce cross-linked fragments based on linear fragments
     */
    private ArrayList<CrossLinkedFragmentProducer>  m_crossLinkedFragmentProducer = new ArrayList<>();   
    
    /**
     * each spectrum can be searched with a list of additional m/z offsets to 
     * the precursor 
     */
    ArrayList<Double> m_additionalPrecursorMZOffsets = null;

    /**
     * spectra with unknown precursor charge state can be searched with a list of 
     * additional m/z offsets to the precursor 
     */
    ArrayList<Double> m_additionalPrecursorMZOffsetsUnknowChargeStates;

    /**
     * list of objects that can produce cross-linked fragments based on 
     * linear fragments
     * that should also be considered for candidate selection
     */
    private ArrayList<CrossLinkedFragmentProducer>  m_primaryCrossLinkedFragmentProducer = new ArrayList<>();   
    
    private int m_maxModificationPerPeptide = 3;

    private int m_maxModifiedPeptidesPerPeptide = 20;
    
    private boolean m_maxModificationPerFASTAPeptideSet  =false;
    private int m_maxModificationPerFASTAPeptide = m_maxModificationPerPeptide;

    private boolean m_maxModifiedPeptidesPerFASTAPeptideSet  =false;
    private int m_maxModifiedPeptidesPerFASTAPeptide = m_maxModifiedPeptidesPerPeptide;
    
    /**
     * tolerance for matching peptides to masses given via extra tag in mgf-files
     */
    private ToleranceUnit m_spectraPeptideMassTollerance;
    /**
     * If a spectrum defines masses for peptide candidates should we use 
     * these exclusively or just make them a priority
     */
    private boolean m_xlPeptideMassCandidatesAreExclusive = false;
    /**
     * should decoys automatically be created from the target proteins
     */
    private boolean m_autodecoys = true;
    
    
//    /**
//     * if matches have some associated weight then this defines how they affect the sorting of match-candidates 
//     */
//    private boolean matchWeightMultiplication = false;
//    /**
//     * if matches have some associated weight then this defines how they affect the sorting of match-candidates 
//     */
//    private boolean matchWeightAddition = false;

    
    /**
     * a list of all configured input filters. These can be either things that 
     * only forward specific spectra
     * or filter that modify spectra
     * Spectra will be applied in the order of definition
     */
    private ArrayList<AbstractStackedSpectraAccess> m_inputFilter = new ArrayList<>();


    SortedLinkedList<ScoreSpectraMatch> m_scores = new SortedLinkedList<ScoreSpectraMatch>();

    private HashMap<Object, Object> m_storedObjects = new HashMap<Object, Object>();

    {
        addStatusInterface(new LoggingStatus());
        m_crossLinkedFragmentProducer.add(new BasicCrossLinkedFragmentProducer());
        m_isotopAnnotation = new Averagin(this);
    }
    
    /**
     * more then one {@link  StatusInterface} can be defined for writing out 
     * status messages. this class is used to write them out to several interfaces
     */
    private class status_multiplexer implements StatusInterface {
        
        String status;
            
        public void setStatus(String status) {
            this.status = status;
            for (StatusInterface si : m_status_publishers) {
                si.setStatus(status);
            }
        }

        public String getStatus() {
            return status;
        }
        
    }



    // laod all default aminoacid-defnitionen
    HashMap<String,AminoAcid> m_AminoAcids = new HashMap<String, AminoAcid>();
    {
        for (AminoAcid aa : AminoAcid.getRegisteredAminoAcids()) {
            m_AminoAcids.put(aa.SequenceID,aa);
        }

        DoubleFragmentation.setEnable(false);
    }


    private int m_NumberMGCPeaks = -1;

    private int m_max_missed_cleavages = 4;

    private ArrayList<Method> m_fragmentMethods = new ArrayList<Method>();
    private ArrayList<Method> m_secondaryFragmentMethods = new ArrayList<Method> ();
    private ArrayList<Method> m_lossMethods = new ArrayList<Method>();


    private ArrayList<String> m_checkedConfigLines = new ArrayList<String>();
    private ArrayList<String> m_checkedCustomConfigLines = new ArrayList<String>();

//    private ArrayList<Method> m_fragments = new ArrayList<Method>();


//    public ArrayList<Fragment> fragment(Peptide p) {
//        ArrayList<Fragment> returnList = new ArrayList<Fragment>();
//        // call each registered fragmentation function
//        for (Method m :m_fragments) {
//            Object ret = null;
//            try {
//
//                ret = m.invoke(null, p);
//
//            } catch (IllegalAccessException ex) { //<editor-fold desc="and some other" defaultstate="collapsed">
//                Logger.getLogger(Fragment.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (IllegalArgumentException ex) {
//                Logger.getLogger(Fragment.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (InvocationTargetException ex) {
//                Logger.getLogger(Fragment.class.getName()).log(Level.SEVERE, null, ex);//</editor-fold>
//            }
//            if (ret != null && ret instanceof ArrayList) {
//                returnList.addAll((ArrayList<Fragment>)ret);
//            }
//        }
//        return returnList;
//    }
//
//    public void registerFragmentClass(Class<? extends Fragment> c) throws NoSuchMethodException {
//            Method m = c.getMethod("fragment", Peptide.class);
//            if (!m_fragments.contains(m))
//                m_fragments.add(m);
//    }


    public void addFixedModification(AminoModification am) {
        AminoAcid base = am.BaseAminoAcid;
        m_fixed_mods.add(am);
        // sync with label
        ArrayList<AminoLabel> ml = m_mapped_label.get(base);
        if (ml != null)
            for (AminoLabel al : ml)
                m_fixed_mods.add(new AminoModification(am.SequenceID + al.labelID, base, am.mass + al.weightDiff));
       m_mapped_fixed_mods = generateMappings(m_fixed_mods);
       addAminoAcid(am);
    }

    public void addVariableModification(AminoModification am) {
        AminoAcid base = am.BaseAminoAcid;
        m_var_mods.add(am);
        // sync with label
        ArrayList<AminoLabel> ml = m_mapped_label.get(base);
        if (ml != null)
            for (AminoLabel al : ml)
                m_var_mods.add(new AminoModification(am.SequenceID + al.labelID, base, am.mass + al.weightDiff));
        m_mapped_var_mods = generateMappings(m_var_mods);
        addAminoAcid(am);
    }

    public void addLinearModification(AminoModification am) {
        AminoAcid base = am.BaseAminoAcid;
        m_linear_mods.add(am);
        // sync with label
        ArrayList<AminoLabel> ml = m_mapped_label.get(base);
        if (ml != null)
            for (AminoLabel al : ml)
                m_linear_mods.add(new AminoModification(am.SequenceID + al.labelID, base, am.mass + al.weightDiff));
        m_mapped_linear_mods = generateMappings(m_linear_mods);
        addAminoAcid(am);
    }
    
    public void addLabel(AminoLabel al) {
        AminoAcid base = al.BaseAminoAcid;
        m_label.add(al);
        ArrayList<AminoModification> dependend = getVariableModifications(al.BaseAminoAcid);
        m_mapped_label = generateLabelMappings(m_label);

        // duplicate modifications into labeled modifications
        ArrayList<AminoModification> cm = getVariableModifications(al.BaseAminoAcid);
        if (cm != null)
        for (AminoModification am : (ArrayList<AminoModification>)cm.clone())
            m_var_mods.add(new AminoModification(am.SequenceID + al.labelID, base, am.mass + al.weightDiff));
        cm = getFixedModifications(al.BaseAminoAcid);
        if (cm != null)
        for (AminoModification am : (ArrayList<AminoModification>)cm.clone())
            m_fixed_mods.add(new AminoModification(am.SequenceID + al.labelID, base, am.mass + al.weightDiff));

        // update mappings
        m_mapped_fixed_mods = generateMappings(m_fixed_mods);
        m_mapped_var_mods = generateMappings(m_var_mods);
        m_AminoAcids.put(al.SequenceID,al);
    }



    protected HashMap<AminoAcid,ArrayList<AminoModification>> generateMappings(ArrayList<AminoModification> modifications){
        HashMap<AminoAcid,ArrayList<AminoModification>> map = new HashMap<AminoAcid, ArrayList<AminoModification>>();
        for (AminoModification am : modifications) {
            ArrayList<AminoModification> list = map.get(am.BaseAminoAcid);
            if (list == null) {
                list = new ArrayList<AminoModification>();
                map.put(am.BaseAminoAcid, list);
            }
            list.add(am);
        }
        return map;
    }

    protected HashMap<AminoAcid,ArrayList<AminoLabel>> generateLabelMappings(ArrayList<AminoLabel> label){
        HashMap<AminoAcid,ArrayList<AminoLabel>> map = new HashMap<AminoAcid, ArrayList<AminoLabel>>();
        for (AminoLabel am : label) {
            ArrayList<AminoLabel> list = map.get(am.BaseAminoAcid);
            if (list == null) {
                list = new ArrayList<AminoLabel>();
                map.put(am.BaseAminoAcid, list);
            }
            list.add(am);
        }
        return map;
    }


    public Digestion getDigestion_method() {
        return m_digestion;
    }


    public ArrayList<AminoLabel> getLabel() {
            return m_label;
    }

    public ArrayList<AminoLabel> getLabel(AminoAcid aa) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ArrayList<AminoModification> getFixedModifications() {
        return m_fixed_mods;
    }

    public ArrayList<AminoModification> getFixedModifications(AminoAcid aa) {
        return m_mapped_fixed_mods.get(aa);
    }

    public ArrayList<AminoModification> getVariableModifications() {
        return m_var_mods;
    }

    public ArrayList<AminoModification> getKnownModifications() {
        return m_known_mods;
    }
    public ArrayList<AminoModification> getKnownModifications(AminoAcid aa) {
        return m_mapped_known_mods.get(aa);
    }

    @Override
    public ArrayList<AminoModification> getLinearModifications() {
        return m_linear_mods;
    }

    @Override
    public ArrayList<AminoModification> getLinearModifications(AminoAcid aa) {
        return m_mapped_linear_mods.get(aa);
    }
    
    
    public ArrayList<NonAminoAcidModification> getVariableNterminalPeptideModifications() {
        return m_NTermPepMods;
    }

    public ArrayList<NonAminoAcidModification> getVariableCterminalPeptideModifications() {
        return m_CTermPepMods;
    }

    public void addVariableNterminalPeptideModifications(NonAminoAcidModification mod) {
        m_NTermPepMods.add(mod);
    }

    public void addVariableCterminalPeptideModifications(NonAminoAcidModification mod) {
        m_CTermPepMods.add(mod);
    }


    public ArrayList<AminoModification> getVariableModifications(AminoAcid aa) {
        return m_mapped_var_mods.get(aa);
    }

    public ArrayList<CrossLinker> getCrossLinker() {
        return m_crosslinker;
    }

    public ToleranceUnit getPrecousorTolerance() {
        return m_PrecoursorTolerance;
    }

    public ToleranceUnit getFragmentTolerance() {
        return m_FragmentTolerance;
    }
    
    /**
     * defaults to getFragmentTolerance
     * @return the m_FragmentToleranceCandidate
     */
    public ToleranceUnit getFragmentToleranceCandidate() {
        return m_FragmentToleranceCandidate == null? m_FragmentTolerance : m_FragmentToleranceCandidate;
    }

    /**
     * @param m_FragmentToleranceCandidate the m_FragmentToleranceCandidate to set
     */
    public void setFragmentToleranceCandidate(ToleranceUnit m_FragmentToleranceCandidate) {
        this.m_FragmentToleranceCandidate = m_FragmentToleranceCandidate;
        //if fragment tolerance is to big - switch to low-resolution mode
        if (getFragmentToleranceCandidate().getUnit().contentEquals("da") && getFragmentToleranceCandidate().getValue() > 0.06)
            m_LowResolution = true;
        else {
            m_LowResolution = false;
        }
    }

    public int getNumberMgcPeaks() {
        return m_NumberMGCPeaks;
    }

    public int getMaxMissCleavages() {
        return m_max_missed_cleavages;
    }

    public ArrayList<Fragment> includeLosses(ArrayList<Fragment> fragments,  boolean comulative) {

        ArrayList<Fragment> returnList = new ArrayList<Fragment>();
        //returnList.addAll(AminoAcidRestrictedLoss.createLossyFragments(fragments));
        //returnList.addAll(AIonLoss.createLossyFragments(Fragments));
        //returnList.addAll( );
        // H20
        if (comulative) {
            for (Method m :m_losses) {
                Object ret = null;
                try {

                    m.invoke(null, fragments, m_crosslinker, true);

                } catch (IllegalAccessException ex) { //<editor-fold desc="and some other" defaultstate="collapsed">
                    Logger.getLogger(Fragment.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(Fragment.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(Fragment.class.getName()).log(Level.SEVERE, null, ex);//</editor-fold>
                }
            }
        } else {
            for (Method m :m_losses) {
                Object ret = null;
                try {

                    ret = m.invoke(null, fragments, false);

                } catch (IllegalAccessException ex) { //<editor-fold desc="and some other" defaultstate="collapsed">
                Logger.getLogger(Fragment.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(Fragment.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvocationTargetException ex) {
                Logger.getLogger(Fragment.class.getName()).log(Level.SEVERE, null, ex);//</editor-fold>
                }
                if (ret != null && ret instanceof ArrayList) {
                    returnList.addAll((ArrayList<Fragment>)ret);
                }
            }
        }
        return returnList;
    }

    public void registerLossClass(Class<? extends Fragment> c) throws NoSuchMethodException {
        //find the createLossyFragments method
        for (Method m : c.getMethods()) {
            if (m.getName().contentEquals("createLossyFragments")) {
                m_losses.add(m);
            }
        }
       // m_losses.add(c.getMethod("createLossyFragments", new Class[]{ArrayList.class, boolean.class}));

    }

    public void addScore(ScoreSpectraMatch score) {
        m_scores.add(score);
    }

    public SortedLinkedList<ScoreSpectraMatch> getScores() {
        return m_scores;
    }

    
    public int getMaxCrosslinkedPeptides() {
        return m_maxpeps;
    }

    public void setMaxCrosslinkedPeptides(int maxpeps) {
        this.m_maxpeps=maxpeps;
    }
    
    public IsotopPattern getIsotopAnnotation() {
        return m_isotopAnnotation;
    }


    public AminoAcid getAminoAcid(String SequenceId) {
        return m_AminoAcids.get(SequenceId);
    }
    
    public Collection<AminoAcid> getAllAminoAcids() {
        return m_AminoAcids.values();
    }

    public void addAminoAcid(AminoAcid aa) {
        m_AminoAcids.put(aa.SequenceID,aa);
    }

    public void addKnownModification(AminoModification am) {
        AminoAcid base = am.BaseAminoAcid;
        m_known_mods.add(am);
        // sync with label
        ArrayList<AminoLabel> ml = m_mapped_label.get(base);
        if (ml != null)
            for (AminoLabel al : ml)
                m_known_mods.add(new AminoModification(am.SequenceID + al.labelID, base, am.mass + al.weightDiff));
        m_mapped_known_mods = generateMappings(m_known_mods);
        addAminoAcid(am);
    }
    
    public ArrayList<Method> getFragmentMethods() {
        return m_fragmentMethods;
    }

    public ArrayList<Method> getSecondaryFragmentMethods() {
        return m_secondaryFragmentMethods;
    }

    public ArrayList<Method> getLossMethods() {
        return m_lossMethods;
    }


    public void register(AminoAcid aa) {
        this.m_AminoAcids.put(aa.SequenceID, aa);
    }

    public void setIsotopAnnotation(IsotopPattern i) {
        m_isotopAnnotation = i;
    }

    public void storeObject(Object key, Object value) {
        m_storedObjects.put(key, value);
        if (key instanceof String)
            m_storedObjects.put(((String) key).toLowerCase(), value);
    }

    public Object retrieveObject(Object key) {
        Object o = m_storedObjects.get(key);
        if (o == null && key instanceof String) {
            o = m_storedObjects.get(((String)key).toLowerCase());
            if (o==null)
                o = m_storedObjects.get(((String)key).toUpperCase());
        }
        return o;
    }


    public boolean retrieveObject(Object key, boolean defaultValue) {
        Object o = retrieveObject(key);
        if (o == null)
            return defaultValue;
        if (o instanceof Boolean)
            return (Boolean) o;

        String s = o.toString();
        return getBoolean(s, defaultValue);
    }

    public String retrieveObject(Object key, String defaultValue) {
        Object o = retrieveObject(key);
        if (o == null)
            return defaultValue;

        return o.toString();
    }

    public double retrieveObject(Object key, double defaultValue) {
        Object o = retrieveObject(key);
        if (o == null)
            return defaultValue;

        if (o instanceof Number)
            return ((Number) o).doubleValue();

        return Double.parseDouble(o.toString().trim());
    }

    public int retrieveObject(Object key, int defaultValue) {
        Object o = retrieveObject(key);
        if (o == null)
            return defaultValue;

        if (o instanceof Number)
            return ((Number) o).intValue();

        return Integer.parseInt(o.toString().trim());
    }

    public long retrieveObject(Object key, long defaultValue) {
        Object o = retrieveObject(key);
        if (o == null)
            return defaultValue;

        if (o instanceof Number)
            return ((Number) o).longValue();

        return Long.parseLong(o.toString().trim());
    }

    /**
     * @param crosslinker the crosslinker to set
     */
    public void setCrosslinker(CrossLinker[] crosslinker) {
        for (CrossLinker cl : crosslinker) {
            addCrossLinker(cl);
        }
    }

    /**
     * adds a crosslinker to the list of searched crosslinkers
     * @param crosslinker
     */
    public void addCrossLinker(CrossLinker crosslinker) {
        m_crosslinker.add(crosslinker);
    }

    /**
     * @param digestion the digestion to set
     */
    public void setDigestion(Digestion digestion) {
        this.m_digestion = digestion;
    }

    /**
     * @param tolerance the tolerance to set
     */
    public void setPrecoursorTolerance(ToleranceUnit tolerance) {
        this.m_PrecoursorTolerance = tolerance;
    }

    /**
     * Defines the tolerance for matching ms2 peaks.
     * <p>if {@link #setLowResolution() } or {@link #setLowResolution(boolean) } 
     * has not been called before this will also this will also automatically 
     * set the lowresolution mode if the Tolerance is lower then 50ppm at 400 
     * m/z  </p>
     * @param tolerance the tolerance to set
     */
    public void setFragmentTolerance(ToleranceUnit tolerance) {
        this.m_FragmentTolerance = tolerance;
        if (m_LowResolution == null) {
            double testmz = 400;
            double testppm  = 50;
            //if fragment tolerance is to big - switch to low-resolution mode
            if (getFragmentTolerance().compare(testmz, testmz+testmz*testppm/1000000) == 0)
                setLowResolution(true);
            else {
                setLowResolution(false);
            }
        }
    }

    /**
     * @param mgcPeaks the mgcPeaks to set
     */
    public void setNumberMGCPeaks(int mgcPeaks) {
        this.m_NumberMGCPeaks = mgcPeaks;
    }

    /**
     * @param max the max to set
     */
    public void setMaxMissedCleavages(int max) {
        this.m_max_missed_cleavages = max;
    }

    /**
     * @param max the max to set
     */
    public void setTopMGCHits(int max) {
        this.m_topMGCHits = max;
    }

    /**
     * @param max the max to set
     */
    public int getTopMGCHits() {
        return this.m_topMGCHits;
    }

    public int getTopMGXHits() {
        return this.m_topMGXHits;
    }

    public void setTopMGXHits(int top) {
        this.m_topMGXHits = top;
    }
    
    public boolean evaluateFilterLine(String filterDef) {
        String[] c = filterDef.split(":",2);
        String type = c[0].toLowerCase();
        
        HashMap<String,String> args = new HashMap<>();
        if (c.length>1 && c[1].length() >0) {
            for (String a : c[1].split(";")) {
                String[] p = a.split(":",2);
                args.put(p[0].toLowerCase(), p[1].trim());
            }
        }
        
        if (type.contentEquals("MS2PrecursorDetection".toLowerCase())) {
            double window = 2;
            if (args.containsKey("window")) {
                window = Double.parseDouble(args.get("window"));
            }
            MS2PrecursorDetection ms2d = new MS2PrecursorDetection(this,window);
            this.m_inputFilter.add(ms2d);
            return true;
        } 
        
        if (type.contentEquals("denoise")) {
            this.m_inputFilter.add(c.length == 2?  new Denoise(this,c[1]): new Denoise(this));
            return true;
        }
        if (type.contentEquals("RemoveSinglePeaks".toLowerCase())) {
            this.m_inputFilter.add(new RemoveSinglePeaks(c[1]));
            return true;
        }
        if (type.contentEquals("rebase")) {
            this.m_inputFilter.add(new Rebase(c[1]));
            return true;
        }
        
        if (type.contentEquals("ScanFilteredSpectrumAccess".toLowerCase())) {
            this.m_inputFilter.add(new ScanFilteredSpectrumAccess(this,c[1]));
        }
        
        if (type.contentEquals("deisotope")) {
            this.m_inputFilter.add(new DeIsotoper(this));
        }
        if (type.contentEquals("containspeaks")) {
            PeakFilteredSpectrumAccess f = new PeakFilteredSpectrumAccess();
            if (args.containsKey("tolerance")) {
                f.setTolerance(new ToleranceUnit(args.get("tolerance")));
            }
            if (args.containsKey("peaks")) {
                for (String p : args.get("peaks").split(",")) {
                    f.addPeak(Double.parseDouble(p.trim()));
                }
            }
            if (getBoolean(args.get("matchall"),false)) {
                f.setFindAll();
            }
            if (getBoolean(args.get("matchany"),false)) {
                f.setFindAny();
            }

            String m = args.get("match");
            if (m != null) {
                m=m.toLowerCase().trim();
                if (m.contentEquals("all")) {
                    f.setFindAll();
                } else if (m.contentEquals("any")) {
                    f.setFindAll();
                } if (m.matches("[0-9]+")) {
                    f.setMinimumFoundPeaks(Integer.parseInt(m));
                } else {
                    Logger.getLogger(this.getClass().getName()).log(Level.WARNING,"unknown match argument in " + filterDef);
                }
            }
        }
        return false;
    }
    

    public boolean evaluateConfigLine(String line) throws ParseException{
        String[] confLine = line.split(":",2);
        String confName = confLine[0].toLowerCase().trim();
        //String className = confLine[1].trim();
        String confArgs = null;
        if (confLine.length >1)
            confArgs = confLine[1].trim();

        if (confName.contentEquals("crosslinker")){
            evaluateCrossLinker(confArgs);
        } else if (confName.contentEquals("filter")){
            evaluateFilterLine(confArgs);
        } else if (confName.contentEquals("digestion")){
            evaluateDigestion(confArgs);

        } else if (confName.contentEquals("modification")) {
            evaluateModification(confArgs, line);
        } else if (confName.contentEquals("cterminalmodification")) {
            evaluateNTerminalModification(confArgs);
        } else if (confName.contentEquals("nterminalmodification")) {
            evaluateCTerminalModification(confArgs);

        } else if (confName.contentEquals("MAX_MODIFIED_PEPTIDES_PER_PEPTIDE".toLowerCase())) {
            setMaxModifiedPeptidesPerPeptide(confArgs);
        } else if (confName.contentEquals("MAX_MODIFIED_PEPTIDES_PER_PEPTIDE_FASTA".toLowerCase())) {
            setMaxModifiedPeptidesPerFASTAPeptide(confArgs);
        } else if (confName.contentEquals("MAX_MODIFICATION_PER_PEPTIDE".toLowerCase())) {
            setMaxModificationPerPeptide(confArgs);
        } else if (confName.contentEquals("MAX_MODIFICATION_PER_PEPTIDE_FASTA".toLowerCase())) {
            setMaxModificationPerFASTAPeptide(confArgs);
        } else if (confName.contentEquals("label")) {
            String[] c = confArgs.split(":",3);
            if (c[1].length()==0) {
                addLabel(AminoLabel.parseArgs(c[2], this));
            } else {
                addLabel(AminoLabel.getLabel(c[1], c[2], this));
            }

        } else if (confName.contentEquals("tolerance")) {
            evaluateTolerance(confArgs);

        } else if (confName.contentEquals("loss")) {
            if (!m_checkedConfigLines.contains(line)) {
                Loss.parseArgs(confArgs, this);
                m_checkedConfigLines.add(line);
                return true;
            }
            

        } else if (confName.contentEquals("fragment")) {
            Fragment.parseArgs(confArgs, this);
//        } else if (confName.contentEquals("score")) {
//            String[] c = confArgs.split(":",2);
//            if (c.length == 1)
//
        } else if (confName.contentEquals("missedcleavages")) {
            setMaxMissedCleavages(Integer.parseInt(confArgs.trim()));
        } else if (confName.contentEquals("topmgchits")) {
            int mgc =Integer.parseInt(confArgs.trim());
            setTopMGCHits(mgc);
            if (getTopMGXHits() < -1)
                setTopMGXHits(mgc);
        } else if (confName.contentEquals("topmgxhits")) {
            setTopMGXHits(Integer.parseInt(confArgs.trim()));
        } else if (confName.contentEquals("isotoppattern")) {
            evaluateIsotopeRecognition(confArgs);

        } else if (confName.contentEquals("mgcpeaks")) {
            int peaks = Integer.valueOf(confArgs);
            setNumberMGCPeaks(peaks);
            
        } else if (confName.contentEquals("custom")) {
            evaluateCustomConfig(confArgs);
        } else if (confName.contentEquals("maxpeakcandidates")) {
            setMaximumPeptideCandidatesPerPeak(Integer.parseInt(confArgs.trim()));
        } else if (confName.contentEquals("targetdecoy")) {
            evaluateTargetDecoy(confArgs);
        }else if (confName.contentEquals("maxpeptidemass")){
            m_maxPeptideMass = Double.parseDouble(confArgs.trim());
        }else if (confName.contentEquals("evaluatelinears")){
            m_EvaluateLinears = getBoolean(confArgs,m_EvaluateLinears);
        }else if (confName.contentEquals("usecpus") || confName.contentEquals("searchthreads")){
            m_SearchThreads = calculateSearchThreads(Integer.parseInt(confArgs.trim()));
        } else if (confName.contentEquals("TOPMATCHESONLY".toLowerCase())) {
            m_topMatchesOnly = getBoolean(confArgs, false);
        } else if (confName.contentEquals("LOWRESOLUTION".toLowerCase())) {
            m_LowResolution = getBoolean(confArgs, false);
        } else if (confName.contentEquals("AUTODECOY".toLowerCase())) {
            m_autodecoys = getBoolean(confArgs, m_autodecoys);
        } else if (confName.contentEquals("XL_Peptide_Mass_Candidates_Exclusive".toLowerCase())) {
            m_xlPeptideMassCandidatesAreExclusive = getBoolean(confArgs, m_xlPeptideMassCandidatesAreExclusive);
        } else if (confName.contentEquals("missing_isotope_peaks".toLowerCase())) {
            evaluateMissingMonoisotopicDetection(confArgs);
        } else if (confName.contentEquals("missing_isotope_peaks_unknown_charge".toLowerCase())) {
            evaluateMissingMonoisotopicDetectionUnknowChargeState(confArgs);
//        } else if (confName.contentEquals("match_weight_score_multiplication")) {
//            matchWeightMultiplication = getBoolean(line, matchWeightMultiplication);
//        } else if (confName.contentEquals("match_weight_score_addition")) {
//            matchWeightMultiplication = getBoolean(line, matchWeightAddition);
        } else {
            m_checkedConfigLines.add(line);
            return false;
        }
    m_checkedConfigLines.add(line);
    return true;
    }

    public void evaluateCrossLinker(String confArgs) {
        String[] c = confArgs.split(":",2);
        CrossLinker cl = CrossLinker.getCrossLinker(c[0], c[1], this);
        if (cl.getName().toLowerCase().contentEquals("wipe")) {
            m_crosslinker.clear();
        } else
            addCrossLinker(cl);
    }

    public void evaluateDigestion(String confArgs) {
        String[] c = confArgs.split(":",2);
        Digestion d = Digestion.getDigestion(c[0], c[1], this);
        setDigestion(d);
    }

    public void evaluateNTerminalModification(String confArgs) throws ParseException {
        String[] c = confArgs.split(":",3);
        String type = c[0].toLowerCase();
        //confLine = classArgs.split(":",2);
        NonAminoAcidModification am = null;
        
        if (c[1].length() == 0)  {
            am = NonAminoAcidModification.parseArgs(c[2],this).get(0);
        }
//            } else {
//                am = AminoModification.getModifictaion(c[1], c[2], this);
//            }

if (c[0].toLowerCase().contentEquals("fixed")) {
//                addFixedModification(am);
} else {
    addVariableCterminalPeptideModifications(am);
}
    }

    public void evaluateCTerminalModification(String confArgs) throws ParseException {
        String[] c = confArgs.split(":",3);
        String type = c[0].toLowerCase();
        //confLine = classArgs.split(":",2);
        NonAminoAcidModification am = null;
        
        if (c[1].length() == 0)  {
            am = NonAminoAcidModification.parseArgs(c[2],this).get(0);
        }
//            } else {
//                am = AminoModification.getModifictaion(c[1], c[2], this);
//            }

if (c[0].toLowerCase().contentEquals("fixed")) {
//                addFixedModification(am);
} else {
    addVariableNterminalPeptideModifications(am);
}
    }

    public void evaluateMissingMonoisotopicDetectionUnknowChargeState(String confArgs) throws NumberFormatException {
        int p = Integer.parseInt(confArgs.trim());
        ArrayList<Double> setting = new ArrayList<Double>(p);
        for (int i =1; i<=p; i++) {
            setting.add(-i*Util.C13_MASS_DIFFERENCE);
        }
        if (setting.size() >0) {
            m_additionalPrecursorMZOffsetsUnknowChargeStates = setting;
            if (m_additionalPrecursorMZOffsets != null) {
                for (Double mz : m_additionalPrecursorMZOffsets) {
                    m_additionalPrecursorMZOffsetsUnknowChargeStates.remove(mz);
                }
            }
        }
    }

    public void evaluateMissingMonoisotopicDetection(String confArgs) throws NumberFormatException {
        int p = Integer.parseInt(confArgs.trim());
        ArrayList<Double> setting = new ArrayList<Double>(p);
        for (int i =1; i<=p; i++) {
            setting.add(-i*Util.C13_MASS_DIFFERENCE);
        }
        if (setting.size() >0) {
            m_additionalPrecursorMZOffsets = setting;
            if (m_additionalPrecursorMZOffsetsUnknowChargeStates != null) {
                for (Double mz : m_additionalPrecursorMZOffsets) {
                    m_additionalPrecursorMZOffsetsUnknowChargeStates.remove(mz);
                }
            }
        }
    }

    public void evaluateTargetDecoy(String confArgs) {
        String cl = confArgs.toLowerCase();
        if (cl.contentEquals("t") || cl.contentEquals("target"))
            m_decoyTreatment = SequenceList.DECOY_GENERATION.ISTARGET;
        else if (cl.contentEquals("rand") || cl.contentEquals("randomize"))
            m_decoyTreatment = SequenceList.DECOY_GENERATION.GENERATE_RANDOMIZED_DECOY;
        else if (cl.contentEquals("rev") || cl.contentEquals("reverse"))
            m_decoyTreatment = SequenceList.DECOY_GENERATION.GENERATE_REVERSED_DECOY;
//            int peaks = Integer.valueOf(confArgs);
//            setNumberMGCPeaks(peaks);
    }

    public void evaluateCustomConfig(String confArgs) throws ParseException {
        String[] customLines = confArgs.split("(\r?\n\r?|\\n)");
        
        for (int cl = 0 ; cl < customLines.length; cl++) {
            
            String tcl = customLines[cl].trim();
            m_checkedCustomConfigLines.add(tcl);
            
            if (!(tcl.startsWith("#") || tcl.isEmpty())) {
                if (!evaluateConfigLine(tcl)) {
                    
                    String[] parts = tcl.split(":", 2);
                    storeObject(parts[0], parts[1]);
                    storeObject(parts[0].toUpperCase(), parts[1]);
                    storeObject(parts[0].toLowerCase(), parts[1]);
                    
                } else {
                    if (m_checkedConfigLines.get(m_checkedConfigLines.size()-1) == tcl);
                        m_checkedConfigLines.remove(m_checkedConfigLines.size()-1);
                }
            }
        }
    }

    public void evaluateIsotopeRecognition(String confArgs) {
        Class i;
        try {
            i = Class.forName("rappsilber.ms.spectra.annotation." + confArgs);
            IsotopPattern ip;
            Constructor c = i.getConstructor(RunConfig.class);
            
            ip = (IsotopPattern) c.newInstance(this);
            setIsotopAnnotation(ip);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
            Logger.getLogger(AbstractRunConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void evaluateTolerance(String confArgs) {
        String[] c = confArgs.split(":",2);
        String tType = c[0].toLowerCase();
        if (tType.contentEquals("precursor"))
            setPrecoursorTolerance(ToleranceUnit.parseArgs(c[1]));
        else if (tType.contentEquals("fragment")) {
            setFragmentTolerance(ToleranceUnit.parseArgs(c[1]));
        } else if (tType.contentEquals("candidate")) {
            setFragmentToleranceCandidate(ToleranceUnit.parseArgs(c[1]));
        } else if (tType.contentEquals("peptidemasses")) {
            setSpectraPeptideMassTollerance(ToleranceUnit.parseArgs(c[1]));
        }
    }

    public void evaluateModification(String confArgs, String line) throws ParseException {
        String[] c = confArgs.split(":",3);
        String type = c[0].toLowerCase();
        //confLine = classArgs.split(":",2);
        List<AminoModification> am;
        
        if (c[1].length() == 0)  {
            am = AminoModification.parseArgs(c[2],this);
        } else {
            am = AminoModification.getModifictaion(c[1], c[2], this);
        }
        
        if (c[0].toLowerCase().contentEquals("fixed")) {
            for (AminoModification a : am)
                addFixedModification(a);
        } else if (c[0].toLowerCase().contentEquals("variable")){
            for (AminoModification a : am)
                addVariableModification(a);
        } else if (c[0].toLowerCase().contentEquals("linear")){
            for (AminoModification a : am)
                addLinearModification(a);
        } else if (c[0].toLowerCase().contentEquals("known")){
            for (AminoModification a : am)
                addKnownModification(a);
        } else {
            throw new ParseException("Unknown modification Type \"" + c[0] + "\" in " + line,0);
        }
    }

    public void setMaxModifiedPeptidesPerPeptide(String confArgs) throws NumberFormatException {
        m_maxModifiedPeptidesPerPeptide = Integer.parseInt(confArgs.trim());
        if (!m_maxModifiedPeptidesPerFASTAPeptideSet)
            m_maxModifiedPeptidesPerFASTAPeptide = m_maxModifiedPeptidesPerPeptide;
    }

    public void setMaxModificationPerPeptide(String confArgs) throws NumberFormatException {
        m_maxModificationPerPeptide = Integer.parseInt(confArgs.trim());
        if (!m_maxModificationPerFASTAPeptideSet)
            m_maxModificationPerFASTAPeptide = m_maxModificationPerPeptide;
    }

    public void setMaxModificationPerFASTAPeptide(String confArgs) throws NumberFormatException {
        m_maxModificationPerFASTAPeptide = Integer.parseInt(confArgs.trim());
        m_maxModificationPerFASTAPeptideSet =true;
    }

    public void setMaxModifiedPeptidesPerFASTAPeptide(String confArgs) throws NumberFormatException {
        m_maxModifiedPeptidesPerFASTAPeptide = Integer.parseInt(confArgs.trim());
        m_maxModifiedPeptidesPerFASTAPeptideSet =true;
    }

    public int calculateSearchThreads(int searchThreads) {
        int realthreads;
        if (searchThreads == 0)
            realthreads = Math.max(Runtime.getRuntime().availableProcessors(),1);
        else if (searchThreads < 0 )
            realthreads = Math.max(Runtime.getRuntime().availableProcessors()+searchThreads,1);
        else
            realthreads = searchThreads;
        return realthreads;
    }

    public static boolean getBoolean(String value, boolean defaultValue) {
        if (value == null)
            return defaultValue;
        String v = value.trim();
        
        return ((String)v).contentEquals("true") ||
                        ((String)v).contentEquals("yes") ||
                        ((String)v).contentEquals("t") ||
                        ((String)v).contentEquals("y") ||
                        ((String)v).contentEquals("1");
    }

    public static boolean getBoolean(RunConfig config, String key, boolean defaultValue) {
        Object value = config.retrieveObject(key.toUpperCase());
        return getBoolean((String) value, defaultValue);
    }

    /**
     * @return the m_storedObjects
     */
    public HashMap<Object, Object> getStoredObjects() {
        return m_storedObjects;
    }

    /**
     * @param m_storedObjects the m_storedObjects to set
     */
    public void setStoredObjects(HashMap<Object, Object> m_storedObjects) {
        this.m_storedObjects = m_storedObjects;
    }


    public StringBuffer dumpConfig() {
        StringBuffer buf = new StringBuffer();
        for (String line : m_checkedConfigLines) {
            buf.append(line);
            buf.append("\n");
        }
        return buf;
    }

    public ArrayList<String> getConfigLines() {
        return m_checkedConfigLines;
    }

    public ArrayList<String> getCustomConfigLines() {
        return m_checkedCustomConfigLines;
    }
    
    
    public StringBuffer dumpCustomConfig() {
        StringBuffer buf = new StringBuffer();
        for (String line : m_checkedCustomConfigLines) {
            buf.append(line);
            buf.append("\n");
        }
        return buf;
    }

    public StatusInterface getStatusInterface() {
        return m_status;
    }

    public void setStatusInterface(StatusInterface i) {
        m_status_publishers.add(i);
    }

    public void addStatusInterface(StatusInterface i) {
        m_status_publishers.add(i);
    }

    public void removeStatusInterface(StatusInterface i) {
        m_status_publishers.remove(i);
    }
    

    public void clearStatusInterface() {
        m_status_publishers.clear();
    }
    
    protected XiProcess getXiSearch(Object sequences, AbstractSpectraAccess msmInput, ResultWriter output, StackedSpectraAccess sc, RunConfig conf, Class defaultClass) {
            //            xi = new SimpleXiProcessDevMGX(new File(fastaFile), sequences, output, conf, sc);
            //            xi = new SimpleXiProcessDev(new File(fastaFile), sequences, output, conf, sc);
            //            xi = new SimpleXiProcess(new File(fastaFile), sequences, output, conf, sc);
            XiProcess xi = null;
            String xiClassName = conf.retrieveObject("XICLASS", defaultClass.getName());
            Class xiclass;
            try {
                xiclass = Class.forName(xiClassName);
            } catch (ClassNotFoundException ex) {
                try {
                    xiclass = Class.forName("rappsilber.applications." + xiClassName);
                } catch (ClassNotFoundException ex2) {
                    xiclass = defaultClass;
                }
            }
            Constructor xiConstructor = null;
            try {
//                xiConstructor = xiclass.getConstructor(File.class, AbstractSpectraAccess.class, ResultWriter.class, RunConfig.class, StackedSpectraAccess.class);
                  xiConstructor = xiclass.getConstructor(sequences.getClass(), AbstractSpectraAccess.class, ResultWriter.class, RunConfig.class, StackedSpectraAccess.class);
            } catch (Exception ex) {
                if (sequences instanceof File) {
                    xi = new SimpleXiProcessLinearIncluded((File)sequences, msmInput, output, conf, sc);
                } else if (sequences instanceof File[]) {
                    xi = new SimpleXiProcessLinearIncluded((File[])sequences, msmInput, output, conf, sc);
                } else if (sequences instanceof SequenceList) {
                    xi = new SimpleXiProcessLinearIncluded((SequenceList)sequences, msmInput, output, conf, sc);
                }
            }

            if (xi == null) {
                try {
                    xi = (XiProcess) xiConstructor.newInstance(sequences, msmInput, output, conf, sc);
                } catch (Exception ex) {
                    if (sequences instanceof File) {
                        xi = new SimpleXiProcessLinearIncluded((File)sequences, msmInput, output, conf, sc);
                    } else if (sequences instanceof File[]) {
                        xi = new SimpleXiProcessLinearIncluded((File[])sequences, msmInput, output, conf, sc);
                    } else if (sequences instanceof SequenceList) {
                        xi = new SimpleXiProcessLinearIncluded((SequenceList)sequences, msmInput, output, conf, sc);
                    }
                }
            }
            return xi;
        }
    

    public void setLowResolution() {
        m_LowResolution = true;
    }
    
    public void setLowResolution(boolean lowresolution) {
        m_LowResolution = lowresolution;
    }
    
    public boolean isLowResolution() {
        if (m_LowResolution == null)
            return false;
        return m_LowResolution;
    }
    
    public SequenceList.DECOY_GENERATION getDecoyTreatment() {
        return m_decoyTreatment;
    }

    @Override
    public int getMaximumPeptideCandidatesPerPeak() {
        return m_maxFragmentCandidates;
    }
    
    public void setMaximumPeptideCandidatesPerPeak(int candidates) {
        m_maxFragmentCandidates = candidates;
    }
    
    
    public void setDecoyTreatment(SequenceList.DECOY_GENERATION dt) {
        m_decoyTreatment = dt;
    }

    /**
     * @return the m_maxPeptideMass
     */
    public double getMaxPeptideMass() {
        return m_maxPeptideMass;
    }

    /**
     * @param m_maxPeptideMass the m_maxPeptideMass to set
     */
    public void setMaxPeptideMass(double m_maxPeptideMass) {
        this.m_maxPeptideMass = m_maxPeptideMass;
    }

    /**
     * Also match linear peptides to spectra
     * @return the m_EvaluateLinears
     */
    public boolean isEvaluateLinears() {
        return m_EvaluateLinears;
    }

    /**
     * Also match linear peptides to spectra
     * @param m_EvaluateLinears the m_EvaluateLinears to set
     */
    public void setEvaluateLinears(boolean m_EvaluateLinears) {
        this.m_EvaluateLinears = m_EvaluateLinears;
    }

    /**
     * the number of concurent search threads
     * values &lt;0 should indicate that a according number of detected cpu-cores
     * should not be used.
     * E.g. if the computer has 16 cores and the setting is -1 then 15 search
     * threads should be started
     * otherwise the number of threads defined should be used.
     * @return the m_SearchThreads
     */
    public int getSearchThreads() {
        return m_SearchThreads;
    }

    /**
     * the number of concurent search threads
     * values &lt;0 should indicate that a according number of detected cpu-cores
     * should not be used.
     * E.g. if the computer has 16 cores and the setting is -1 then 15 search
     * threads should be started
     * otherwise the number of threads defined should be used.
     * @param m_SearchThreads the m_SearchThreads to set
     */
    public void setSearchThreads(int m_SearchThreads) {
        this.m_SearchThreads = calculateSearchThreads(m_SearchThreads);
    }

    /**
     * @return the m_topMatchesOnly
     */
    public boolean getTopMatchesOnly() {
        return m_topMatchesOnly;
    }

    /**
     * @param m_topMatchesOnly the m_topMatchesOnly to set
     */
    public void setTopMatchesOnly(boolean m_topMatchesOnly) {
        this.m_topMatchesOnly = m_topMatchesOnly;
    }
    
//    
//    public boolean getLowResolutionMatching() {
//        return m_lowResolutionMatching;
//    }
//
//    /**
//     * @param m_lowResolutionMatching the m_lowResolutionMatching to set
//     */
//    public void setLowResolutionMatching(boolean m_lowResolutionMatching) {
//        this.m_lowResolutionMatching = m_lowResolutionMatching;
//    }
    @Override
    public void addCrossLinkedFragmentProducer(CrossLinkedFragmentProducer producer, boolean isprimary) {
        m_crossLinkedFragmentProducer.add(producer);
        if (isprimary) {
            m_primaryCrossLinkedFragmentProducer.add(producer);
        }
    } 
    
    @Override
    public ArrayList<CrossLinkedFragmentProducer> getCrossLinkedFragmentProducers() {
        return m_crossLinkedFragmentProducer;
    }

    @Override
    public ArrayList<CrossLinkedFragmentProducer> getPrimaryCrossLinkedFragmentProducers() {
        return m_primaryCrossLinkedFragmentProducer;
    }
    
    
    /**
     * A list of precursor m/z offset that each spectrum should be searched with
     * @return 
     */
    @Override
    public ArrayList<Double> getAdditionalPrecursorMZOffsets() {
        return m_additionalPrecursorMZOffsets;
    }

    /**
     * A list of precursor m/z offset that each spectrum with unknown precursor 
     * charge state should be searched with
     * @return 
     */
    @Override
    public ArrayList<Double> getAdditionalPrecursorMZOffsetsUnknowChargeStates() {
        return m_additionalPrecursorMZOffsetsUnknowChargeStates;
    }


    public int getMaximumModificationPerPeptide() {
        return m_maxModificationPerPeptide;
    }

    public int getMaximumModifiedPeptidesPerPeptide() {
        return m_maxModifiedPeptidesPerPeptide;
    }
    //rappsilber.utils.Util.MaxModificationPerPeptide = m_config.retrieveObject("MAX_MODIFICATION_PER_PEPTIDE", rappsilber.utils.Util.MaxModificationPerPeptide);

    public int getMaximumModificationPerFASTAPeptide() {
        return m_maxModificationPerFASTAPeptide;
    }

    public int getMaximumModifiedPeptidesPerFASTAPeptide() {
        return m_maxModifiedPeptidesPerFASTAPeptide;
    }

    
    @Override
    public boolean searchStopped() {
        return m_searchStoped;
    }

    @Override
    public void stopSearch() {
        m_searchStoped  =true;
    }

    @Override
    public ToleranceUnit getSpectraPeptideMassTollerance() {
        return m_spectraPeptideMassTollerance;
    }


    public void setSpectraPeptideMassTollerance(ToleranceUnit tu) {
        m_spectraPeptideMassTollerance  = tu;
    }

    @Override
    public boolean getXLPeptideMassCandidatesExclusive() {
        return m_xlPeptideMassCandidatesAreExclusive;
    }
    

    @Override
    public boolean autoGenerateDecoys() {
        return m_autodecoys;
    }


    @Override
    public ArrayList<AbstractStackedSpectraAccess> getInputFilter() {
        return m_inputFilter;
    }

//    /**
//     * if matches have some associated weight then this defines how they affect the sorting of match-candidates
//     * @return the matchWeightMultiplication
//     */
//    public boolean isMatchWeightMultiplication() {
//        return matchWeightMultiplication;
//    }
//
//    /**
//     * if matches have some associated weight then this defines how they affect the sorting of match-candidates
//     * @param matchWeightMultiplication the matchWeightMultiplication to set
//     */
//    public void setMatchWeightMultiplication(boolean matchWeightMultiplication) {
//        this.matchWeightMultiplication = matchWeightMultiplication;
//    }
//
//    /**
//     * if matches have some associated weight then this defines how they affect the sorting of match-candidates
//     * @return the matchWeightAddition
//     */
//    public boolean isMatchWeightAddition() {
//        return matchWeightAddition;
//    }
//
//    /**
//     * if matches have some associated weight then this defines how they affect the sorting of match-candidates
//     * @param matchWeightAddition the matchWeightAddition to set
//     */
//    public void setMatchWeightAddition(boolean matchWeightAddition) {
//        this.matchWeightAddition = matchWeightAddition;
//    }

}
