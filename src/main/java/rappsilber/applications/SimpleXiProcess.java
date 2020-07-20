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
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.RunConfig;
import rappsilber.gui.components.DebugFrame;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.crosslinker.SymetricNarrySingleAminoAcidRestrictedCrossLinker;
import rappsilber.ms.dataAccess.AbstractSpectraAccess;
import rappsilber.ms.dataAccess.AbstractStackedSpectraAccess;
import rappsilber.ms.dataAccess.BufferedSpectraAccess;
import rappsilber.ms.dataAccess.SpectraAccess;
import rappsilber.ms.dataAccess.StackedSpectraAccess;
import rappsilber.ms.dataAccess.filter.candidates.CandidatePairFilter;
import rappsilber.ms.dataAccess.filter.candidates.CandidatePairFromGroups;
import rappsilber.ms.dataAccess.output.AbstractStackedResultWriter;
import rappsilber.ms.dataAccess.output.BufferedResultWriter;
import rappsilber.ms.dataAccess.output.PreFilterResultWriter;
import rappsilber.ms.dataAccess.output.ResultWriter;
import rappsilber.ms.lookup.fragments.FragmentLookup;
import rappsilber.ms.lookup.peptides.FUPeptideTree;
import rappsilber.ms.lookup.peptides.PeptideLookup;
import rappsilber.ms.lookup.peptides.PeptideTree;
import rappsilber.ms.score.AbstractScoreSpectraMatch;
import rappsilber.ms.score.BS3ReporterIonScore;
import rappsilber.ms.score.CombinedScores;
import rappsilber.ms.score.DummyScore;
import rappsilber.ms.score.FragmentChargeState;
import rappsilber.ms.score.FragmentCoverage;
import rappsilber.ms.score.FragmentLibraryScore;
import rappsilber.ms.score.LinkSiteDelta;
import rappsilber.ms.score.Normalizer;
import rappsilber.ms.score.NormalizerML;
import rappsilber.ms.score.ScoreSpectraMatch;
import rappsilber.ms.score.SpectraCoverage;
import rappsilber.ms.score.SpectraCoverageConservative;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.ModificationType;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.Sequence;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ms.sequence.digest.AASpecificity;
import rappsilber.ms.sequence.digest.Digestion;
import rappsilber.ms.sequence.ions.DoubleFragmentation;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.match.MatchedBaseFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptideWeightedNnary;
import rappsilber.ms.spectra.match.filter.BatchFilter;
import rappsilber.ms.spectra.match.filter.CleanUpIsotopCluster;
import rappsilber.ms.spectra.match.filter.CleanUpMatchedPeaksFilter;
import rappsilber.ms.spectra.match.filter.DefinePrimaryFragmentMatches;
import rappsilber.ms.spectra.match.filter.MatchFilter;
import rappsilber.ms.statistics.utils.UpdateableInteger;
import rappsilber.utils.ArithmeticScoredOccurence;
import rappsilber.utils.MyArrayUtils;
import rappsilber.utils.Util;
import rappsilber.utils.XiVersion;
//import rappsilber.utils.ScoredLinkedList2;


public class SimpleXiProcess implements XiProcess {// implements ScoreSpectraMatch{

    protected AbstractSpectraAccess m_msmInput;
    protected SpectraAccess m_ThreadInput;
    protected SpectraAccess m_processedInput;
    private File[] m_fasta;
    private SequenceList m_sequences;
    protected PeptideLookup m_peptides;
    protected PeptideLookup m_peptidesLinear;
    protected FragmentLookup m_Fragments;
    protected ArrayList<CrossLinker> m_Crosslinker;
    protected ToleranceUnit m_PrecoursorTolerance;
    protected ToleranceUnit m_FragmentTolerance;
    private ResultWriter m_output;
    protected RunConfig m_config;
    private Thread[] m_searchThreads;
    private AtomicBoolean[] m_threadStop;
//    private int m_useCPUs = Runtime.getRuntime().availableProcessors() - 1;
    protected boolean m_running = false;
    //private StackedSpectraAccess m_filter = null;
    private DummyScore m_deltaScore  = new DummyScore(0, new String[] {"delta", "deltaMod", "combinedDelta"});
    private final String MatchScore = Normalizer.NAME;
    protected boolean m_doStop = false;

    protected ArrayList<StackedSpectraAccess> m_filters = new ArrayList<StackedSpectraAccess>();

    private double m_smallestCrosslinkedPeptideMass;

    private boolean m_outputTopOnly = false;

    private String m_status = "executing";

    protected int    m_min_pep_length = 0;
    
    /** just for providing some sensible status - how many spectra were actually processed - not only read.*/
    private long m_processedSpectra = 0;
    /** The maximum mass of a peptide to be considered */
    private double m_maxPeptideMass = Double.MAX_VALUE;
    /** Minimum score of the top match for a scan to be reported */
    private double m_minTopScore = -10;
    
    private DebugFrame m_debugFrame;
    
    private boolean m_AUTODECOY = true;
    
    private boolean m_auto_stacktrace = false;
    
    protected LinkSiteDelta linksitedelta = new LinkSiteDelta();

    protected boolean m_prioritizelinears = false;
    protected boolean m_testforlinearmod = true;
    protected boolean m_testlinear = true;
    
    /**
     * filters, that should be applied after the matching but before the scoring
     */
    private MatchFilter  m_matchfilter = new BatchFilter(new MatchFilter[]{new CleanUpMatchedPeaksFilter(), new DefinePrimaryFragmentMatches()});
    /**
     * the last time we checked for the amount of time spend in gc.
     * used to test if we spend overly much time in gc and if so we will just try to stop one of the search-threads
     */
    private long lastGCTestTime;
    /**
     * accumulated amount of time spend in gc at the last time we checked.
     * used to test if we spend overly much time in gc and if so we will just try to stop one of the search-threads
     */
    private long lastGCCollectedTime;
    
    private ArrayList<CandidatePairFilter> candidatePairFilters = new ArrayList<>();

    protected void filterProteins() {
        if (m_config.getProteinGroups().size() >0) {
            HashSet<String> accessions = new HashSet<>();
            for (HashSet<String> set : m_config.getProteinGroups().values()) {
                accessions.addAll(set);
            }
            ArrayList<Sequence> remove = new ArrayList<>();
            for (Sequence s : m_sequences) {
                if (!accessions.contains(s.getSplitFastaHeader().getAccession().trim()))
                    remove.add(s);
            }
            m_sequences.removeAll(remove);
            if (m_config.getProteinGroups().size() >1)
                candidatePairFilters.add(new CandidatePairFromGroups(
                    new ArrayList<HashSet<String>>(m_config.getProteinGroups().values())));
        }
        
    }


    public ArrayList<CandidatePairFilter> getCadidatePairFilter() {
        return candidatePairFilters;
    }

    protected class SearchRunner implements Runnable {

        SpectraAccess m_input;
        ResultWriter m_output;
        AtomicBoolean threadStop;

        public SearchRunner(SpectraAccess input, ResultWriter output, AtomicBoolean threadStop) {
            m_input = input;
            m_output = output;
            this.threadStop = threadStop;
        }

        public void run() {
            process(m_input, m_output, threadStop);
        }
    }

    protected SimpleXiProcess() {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Xi version: " + XiVersion.getVersionString());
    }

    public SimpleXiProcess(File fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        this(new File[]{fasta}, input, output, config, filter);
    }


    public SimpleXiProcess(File[] fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        this(null, fasta, input, output, config, filter);
    }

    public SimpleXiProcess(SequenceList fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        this(fasta, null, input, output, config, filter);
    }


    private SimpleXiProcess(SequenceList sl, File[] fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        this();
        
        m_msmInput = input;
        //m_output = new BufferedResultWriter(output);
        m_output = output;
        m_fasta = fasta;
        m_sequences = sl;
        m_config = config;
        if (filter != null)
            m_filters.add(filter);
        m_Crosslinker = m_config.getCrossLinker();
        m_PrecoursorTolerance = m_config.getPrecousorTolerance();
        m_FragmentTolerance = m_config.getFragmentTolerance();
//        m_output.setFreeMatch(true);
//        m_useCPUs = m_config.getSearchThreads();

        m_AUTODECOY = m_config.autoGenerateDecoys();

        m_prioritizelinears=m_config.retrieveObject("prioritizelinears", false);
        m_testforlinearmod=m_config.retrieveObject("testforlinearmod", true);
        m_testlinear = m_prioritizelinears || m_testforlinearmod;
        
        for (AbstractStackedSpectraAccess ssa :  m_config.getInputFilter()) {
            ssa.setReader(m_msmInput);
            m_msmInput = ssa;
        }

    }

    
    /**
     * @return the m_running
     */
    public boolean isRunning() {
        return m_running;
    }

    /**
     * @return the m_config
     */
    @Override
    public RunConfig getConfig() {
        return m_config;
    }

    @Override
    public void setStatus(String status) {
        m_config.getStatusInterface().setStatus(status);
    }

    @Override
    public String getStatus() {
        return m_config.getStatusInterface().getStatus();
    }

    /**
     * @return the m_outputTopOnly
     */
    @Override
    public boolean OutputTopOnly() {
        return m_outputTopOnly;
    }

    /**
     * @param outputTopOnly the m_outputTopOnly to set
     */
    @Override
    public void setOutputTopOnly(boolean outputTopOnly) {
        this.m_outputTopOnly = outputTopOnly;
    }

    protected void filterMatch(MatchedXlinkedPeptide match) {
        m_matchfilter.filter(match);
    }


    /**
     * @return the m_output
     */
    @Override
    public ResultWriter getOutput() {
        return m_output;
    }

    /**
     * @return the m_msmInput
     */
    @Override
    public AbstractSpectraAccess getMSMInput() {
        return m_msmInput;
    }

    /**
     * @param m_output the m_output to set
     */
    public void setOutput(ResultWriter m_output) {
        this.m_output = m_output;
    }

    /**
     * @return the m_searchThreads
     */
    protected Thread[] getSearchThreads() {
        return m_searchThreads;
    }

    /**
     * @param m_searchThreads the m_searchThreads to set
     */
    protected void setSearchThreads(Thread[] m_searchThreads) {
        this.m_searchThreads = m_searchThreads;
        
        this.m_threadStop = new AtomicBoolean[m_searchThreads.length];
        for (int i =0; i < m_searchThreads.length; i++)
            m_threadStop[i] = new AtomicBoolean(true);
    }

    protected void matchToBaseLookup(Peptide[] topPeps, HashMap<String, HashSet<String>> topMatchHash) {
        if (topPeps.length > 2)
            throw new UnsupportedOperationException("Can't handle more then two peptides");
        String[] topPepsBase = new String[topPeps.length];
        for (int p = 0 ; p<topPeps.length;p++) {
            topPepsBase[p] = topPeps[p].toStringBaseSequence();
        }
        
        HashSet<String> secondPepHashSet=topMatchHash.get(topPepsBase[0]);
        if (secondPepHashSet == null) {
            secondPepHashSet=new HashSet<>();
            topMatchHash.put(topPepsBase[0],secondPepHashSet);
        }
        
        if (topPeps.length > 1) {
            secondPepHashSet.add(topPepsBase[1]);
            secondPepHashSet=topMatchHash.get(topPepsBase[1]);
            if (secondPepHashSet == null) {
                secondPepHashSet=new HashSet<>();
                topMatchHash.put(topPepsBase[1],secondPepHashSet);
            }
            secondPepHashSet.add(topPepsBase[0]);
            
        }
        
    }

    /**
     * @return the m_peptides
     */
    public PeptideLookup getXLPeptideLookup() {
        return m_peptides;
    }

    /**
     * @return the m_peptidesLinear
     */
    public PeptideLookup getLinearPeptideLookup() {
        return m_peptidesLinear;
    }


    /**
     * @return the m_Fragments
     */
    public FragmentLookup getFragments() {
        return m_Fragments;
    }

    

    
    @Override
    public void prepareSearch() {

        if (preparePreFragmentation())
            return;
        fragmentTree();
        
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Before GC:" + Util.memoryToString());
        rappsilber.utils.Util.forceGC();
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "After gc:"  + Util.memoryToString());
        setupScores();

        setOutputTopOnly(getConfig().getTopMatchesOnly());


        m_smallestCrosslinkedPeptideMass = Double.MAX_VALUE;
        for (CrossLinker cl : m_Crosslinker) {
            if (cl.getCrossLinkedMass() < m_smallestCrosslinkedPeptideMass) {
                m_smallestCrosslinkedPeptideMass = cl.getCrossLinkedMass();
            }
        }

        m_smallestCrosslinkedPeptideMass += AminoAcid.G.mass;

        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Finished preperation - can go on with the search now");


        m_minTopScore = m_config.retrieveObject("MINIMUM_TOP_SCORE", m_minTopScore);

        
    }

    public boolean preparePreFragmentation() {
//        Object AminoAcidDecoy = getConfig().retrieveObject("AMINOACIDDECOY");
//        if (AminoAcidDecoy != null &&
//                (AminoAcidDecoy.toString().toLowerCase().contentEquals("true") ||
//                AminoAcidDecoy.toString().toLowerCase().contentEquals("1") ||
//                AminoAcidDecoy.toString().toLowerCase().contentEquals("-1"))) {
//            for (AminoAcid aa : AminoAcid.getRegisteredAminoAcids()) {
//                aa.mass += Util.PROTON_MASS;
//            }
//        }
//        if (m_FragmentTolerance.getUnit().toLowerCase().contentEquals("da") && m_FragmentTolerance.getValue() > 0.08)
//            m_config.setLowResolution();
//        if (m_config.retrieveObject("LOWRESOLUTION", false)) {
//            m_config.setLowResolution();
//        }
        m_min_pep_length = m_config.retrieveObject("MINIMUM_PEPTIDE_LENGTH", m_min_pep_length);
        if (readSequences()) {
            return true;
        }
        applyLabel();
        fixedModifications();
        digest();
        variableModifications();
        peptideTreeFinalizations();
        if (m_config.retrieveObject("PRINT_PEPTIDE_STATS") != null) {
            boolean print = true;
            String  s = m_config.retrieveObject("PRINT_PEPTIDE_STATS").toString();
            PrintStream out = System.out;
            if (s.toLowerCase().matches("(true|false|1|0|y|n)")) {
                print = m_config.retrieveObject("PRINT_PEPTIDE_STATS", false);
            } else {
                try {
                    out = new PrintStream(new File(s));
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(SimpleXiProcess.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (print) {
                printPeptideStats(out);
                if (m_config.retrieveObject("QUIT_AFTER_PEPTIDE_STATS", true)) {
                    System.exit(0);
                }
            }
        }

        if (m_config.retrieveObject("PRINT_PEPTIDES") != null) {
            boolean print = true;
            String  s = m_config.retrieveObject("PRINT_PEPTIDES").toString();
            PrintStream out = System.out;
            if (s.toLowerCase().matches("(true|false|1|0|y|n)")) {
                print = m_config.retrieveObject("PRINT_PEPTIDES", false);
            } else {
                try {
                    out = new PrintStream(new File(s));
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(SimpleXiProcess.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (print) {
                printPeptides(out);
                if (m_config.retrieveObject("QUIT_AFTER_PEPTIDES", true)) {
                    System.exit(0);
                }
            }
        }
        
        Logger.getLogger(this.getClass().getName()).log(Level.INFO,"PeptideTree size: "+m_peptides.size());
//        for (Peptide p : m_peptides) {
//            System.out.println(p.toString());
//        }
        return false;
    }

    /**
     * just dumps all peptides to the stream
     * @param ps
     */
    protected void printPeptides(PrintStream ps) {
        System.out.println("Proteins: " + m_sequences.size());
        int AAs = 0;
        int tp = 0;
        ps.println("XL-Peps");
        ps.println("Peptide, isDecoy");
        for (Peptide p : m_peptides) {
            ps.println(p + ", " + p.isDecoy());
        }
        ps.println("L-Peps");
        ps.println("Peptide, isDecoy");
        for (Peptide p : m_peptidesLinear) {
            ps.println(p + ", " + p.isDecoy());
        }
    }

    protected void printPeptideStats(PrintStream ps) {
        TreeMap<Integer,UpdateableInteger> targetCounts  =new TreeMap<>();
        TreeMap<Integer,UpdateableInteger> targetDecoyComplementCount  =new TreeMap<>();
        TreeMap<Integer,UpdateableInteger> decoyCounts = new TreeMap<>();

        TreeMap<Integer,UpdateableInteger> ltargetCounts  =new TreeMap<>();
        TreeMap<Integer,UpdateableInteger> ldecoyCounts = new TreeMap<>();

        TreeMap<Integer,UpdateableInteger> atargetCounts  =new TreeMap<>();
        TreeMap<Integer,UpdateableInteger> adecoyCounts = new TreeMap<>();
        TreeMap<Integer,UpdateableInteger> allcounts = new TreeMap<>();
        
        TreeMap<Integer,UpdateableInteger> counts;
        TreeMap<Integer,UpdateableInteger> acounts;

        TreeMap<Integer,UpdateableInteger> complement = new TreeMap<>();
        TreeMap<Integer,UpdateableInteger> tComplement = new TreeMap<>();
        TreeMap<Integer,UpdateableInteger> dComplement = new TreeMap<>();
        
        ps.println("Proteins: " + m_sequences.size());
        int AAs = 0;
        int tp = 0;
        for (Sequence s : m_sequences) {
            if (!s.isDecoy()) {
                tp++;
                AAs+=s.length();
            }
        }
        ps.println("Target proteins, " + tp);
        ps.println("AminoAcids, " + AAs);
        
        int maxComplement = 0;
        int xlTargetPeps = 0;
        int xlDecoyPeps = 0;
        
        for (Peptide p: m_peptides) {
            if (p.isDecoy()) {
                xlDecoyPeps++;
                counts=decoyCounts;
                acounts = adecoyCounts;
            } else {
                xlTargetPeps++;
                counts = targetCounts;
                acounts = atargetCounts;
            }
            
            UpdateableInteger i = counts.get(p.length());
            if (i == null) {
                i = new UpdateableInteger(1);
                counts.put(p.length(), i);
            } else {
                i.value++;
            }
            i = acounts.get(p.length());
            if (i == null) {
                i = new UpdateableInteger(1);
                acounts.put(p.length(), i);
            } else {
                i.value++;
            }

            i = allcounts.get(p.length());
            if (i == null) {
                i = new UpdateableInteger(1);
                allcounts.put(p.length(), i);
            } else {
                i.value++;
            }
            if (!p.isDecoy()) {
                double complementmass = 4000;
                if (p.getMass() > complementmass) {
                    complementmass =p.getMass()*1.5;
                }
        
                ArrayList<Peptide> peps = m_peptides.getForMass(p.getMass(), complementmass);
                int t=0;
                int d=0;
                for (Peptide pc : peps) {
                    if (pc.isDecoy())
                        d++;
                    else
                        t++;
                }
                
                UpdateableInteger lbdc = targetDecoyComplementCount.get(p.length());
                if (lbdc == null) {
                    lbdc = new UpdateableInteger(d);
                    targetDecoyComplementCount.put(p.length(), lbdc);
                } else {
                    lbdc.value+=d;
                }
                
                UpdateableInteger tc = tComplement.get(t);
                if (tc==null) {
                    tc = new UpdateableInteger(1);
                    tComplement.put(t, tc);
                } else {
                    tc.value ++;
                }

                UpdateableInteger dc = dComplement.get(d);
                if (dc==null) {
                    dc = new UpdateableInteger(1);
                    dComplement.put(d, dc);
                } else {
                    dc.value ++;
                }

                UpdateableInteger c = complement.get(t+d);
                if (c==null) {
                    c = new UpdateableInteger(1);
                    complement.put(t+d, c);
                } else {
                    c.value ++;
                }
                
            }
        }
        ps.println("XL Target Peptide, " + xlTargetPeps);
        ps.println("XL Target Peptide, " + xlDecoyPeps);

        for (Peptide p: m_peptidesLinear) {
            if (p.isDecoy()) {
                counts=ldecoyCounts;
                acounts = adecoyCounts;
            } else {
                counts = ltargetCounts;
                acounts = atargetCounts;
            }
            
            UpdateableInteger i = counts.get(p.length());
            if (i == null) {
                i = new UpdateableInteger(1);
                counts.put(p.length(), i);
            } else {
                i.value++;
            }
            i = acounts.get(p.length());
            if (i == null) {
                i = new UpdateableInteger(1);
                acounts.put(p.length(), i);
            } else {
                i.value++;
            }

            i = allcounts.get(p.length());
            if (i == null) {
                i = new UpdateableInteger(1);
                allcounts.put(p.length(), i);
            } else {
                i.value++;
            }
        }
        
        ps.println("Peptide length,  XLTarget, XLDecoy, Linear Target, Linear Decoy, All Target, All Decoy, All,Average XLDecoyComplement");
        
        UpdateableInteger zero = new UpdateableInteger(0);
        for (Integer l : allcounts.keySet()) {
            UpdateableInteger lbdc = targetDecoyComplementCount.getOrDefault(l,zero);
            UpdateableInteger t = targetCounts.getOrDefault(l,zero);
            UpdateableInteger d = decoyCounts.getOrDefault(l,zero);
            UpdateableInteger lt = ltargetCounts.getOrDefault(l,zero);
            UpdateableInteger ld = ldecoyCounts.getOrDefault(l,zero);
            UpdateableInteger at = atargetCounts.getOrDefault(l,zero);
            UpdateableInteger ad = adecoyCounts.getOrDefault(l,zero);
            UpdateableInteger a = allcounts.getOrDefault(l+1,zero);
            double averageDecoyComplement = 0; 
            if (t.value >0) 
                averageDecoyComplement = lbdc.value/(double)t.value;
            
            ps.println(l + ", " +t +", " + d + ", " + lt + ", " + ld +
                    ", " + at + ", " + ad + ", " + a + ", "+ averageDecoyComplement);
        }
        ps.println();
        ps.println();
        ps.println("complement count, decoy, target, total");
        for (int c = 0; c<=complement.lastKey();c++) {
            UpdateableInteger tc = tComplement.getOrDefault(c,zero);
            UpdateableInteger dc = dComplement.getOrDefault(c,zero);
            UpdateableInteger cc = complement.getOrDefault(c,zero);
            

            ps.println(c + ", "+ dc + ", " + tc +", " + cc );
        }
    }

    protected void applyLabel() {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Apply Label");
        setStatus("prepare: Apply Label");
        m_sequences.applyLabel(getConfig());
    }

    protected void digest() {
//        m_maxPeptideMass = m_msmInput.getMaxPrecursorMass();
        m_maxPeptideMass = m_config.getMaxPeptideMass()<0?m_msmInput.getMaxPrecursorMass(): m_config.getMaxPeptideMass();
        
        boolean forceSameDecoys = m_config.retrieveObject("FORCESAMEDECOYS", false);
        
        //m_maxPeptideMass = Math.min(m_maxPeptideMass, m_msmInput.getMaxPrecursorMass());
        
        //Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Build Peptide Tree");
        //        m_peptides = new PeptideTree(m_FragmentTolerance);
        String tree = getConfig().retrieveObject("FRAGMENTTREE", "default").toLowerCase();
        if (tree.contentEquals("fu")) {
            m_peptides = new FUPeptideTree(m_PrecoursorTolerance);
            m_peptidesLinear = new FUPeptideTree(m_PrecoursorTolerance);
        } else {
            m_peptides = new PeptideTree(m_PrecoursorTolerance);
            m_peptidesLinear = new PeptideTree(m_PrecoursorTolerance);
        }
//        m_peptides = new PeptideMapDB(m_PrecoursorTolerance);
//        m_peptidesLinear = new PeptideTree(m_PrecoursorTolerance);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Digest Sequences");
        setStatus("prepare: Digest sequence");
        // digest
        Digestion digest = getConfig().getDigestion_method();
        digest.setMaxMissCleavages(getConfig().getMaxMissCleavages());
        digest.setPeptideLookup(m_peptides, m_peptidesLinear);
        m_sequences.digest(getConfig().getDigestion_method(), m_maxPeptideMass, m_Crosslinker);
        if (forceSameDecoys) {
            m_peptides.forceAddDiscarded();
            m_peptidesLinear.forceAddDiscarded();
        } else {
            ArrayList<Peptide> discardedPeptides = m_peptides.addDiscaredPermut(m_config);
            if (discardedPeptides.size() >0) {
                Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Some decoy peptides where not considered in the database");
                for (Peptide p : discardedPeptides) {
                    Logger.getLogger(this.getClass().getName()).log(Level.FINE, p.toString());
                }
            }
            discardedPeptides = m_peptidesLinear.addDiscaredPermut(m_config);
            if (discardedPeptides.size() >0) {
                Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Some linear only decoy  peptides where not considered in the database");
                for (Peptide p : discardedPeptides) {
                    Logger.getLogger(this.getClass().getName()).log(Level.FINE, p.toString());
                }
            }
        }
        
    }

    protected void fixedModifications() {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Apply Fixed Modifications");
        // apply fixed modification
        m_sequences.applyFixedModifications(getConfig());
        setStatus("prepare: Apply Fixed Modifications");
    }

    protected void fragmentTree() {
        //m_sequences.dumpPeptides();
        //        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Build Peptide Tree");
        //        m_peptides = new PeptideTree(m_sequences, m_FragmentTolerance);
        setStatus("Build Fragmenttree");
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Build Fragment Tree (should use " + m_config.getPreSearchThreads() + " threads)" );
//        m_Fragments = new rappsilber.ms.lookup.fragments.FragmentMapDB(m_peptides, m_sequences, m_useCPUs, getConfig());
        m_Fragments = new rappsilber.ms.lookup.fragments.FragmentTreeSlimedMTvArrayOnly(m_peptides, m_sequences, m_config.getPreSearchThreads(), getConfig());
        //m_Fragments = new rappsilber.ms.lookup.fragments.FragmentTreeSlimedMTvArrayOnly(m_sequences, m_useCPUs, getConfig());
    }

    protected void peptideTreeFinalizations() {
        //        System.err.println("Peptides now stored ion the tree : " + m_peptides.size());
        //        try {
        //            ((PeptideTree) m_peptides).dump("/tmp/PeptideTreeDumpAfter.csv");
        //        } catch (IOException ex) {
        //            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        //        }
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Peptides:" + m_peptides.size());
        m_peptides.cleanup(m_min_pep_length);
        m_peptidesLinear.cleanup(m_min_pep_length);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Peptides after cleanup:" + m_peptides.size());
        //        setStatus("total number of peptides:" +  m_peptides.size());
        m_sequences.buildIndex();
    }

    protected boolean readSequences() {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Setup the search environment");
        // read sequences
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Reading sequence file");
        try {
            
            if (m_fasta != null && m_sequences == null) {
                
                m_sequences = new SequenceList(m_fasta, getConfig());
                
            } else if (m_fasta != null) {
                
                for (File f : m_fasta) {
                    m_sequences.addFasta(f);
                }
                
            }
            filterProteins();
            
            if (m_AUTODECOY) {
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Decoys should be auto-generated");
                
                if (!m_sequences.hasDecoy()) {
                
                    Digestion dig = m_config.getDigestion_method();

                    boolean decoyDigestionAware = m_config.retrieveObject("DECOY_DIGESTION_AWARE", true);
                    String decoyGeneration = m_config.retrieveObject("DECOY_GENERATION", "reverse").trim().toLowerCase();
                    
                    if (decoyGeneration.contentEquals("reverse")) {
                        if (dig instanceof AASpecificity && decoyDigestionAware) {

                            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Including reversed sequences with swapped amino-acids");
                            m_sequences.includeReverseAndSwap(((AASpecificity) dig).getAminoAcidSpecificity());

                        } else {

                            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Including reversed sequences");
                            m_sequences.includeReverse();

                        }
                    } else if (decoyGeneration.contentEquals("shuffle")){
                        if (dig instanceof AASpecificity && decoyDigestionAware) {

                            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Including shuffled sequences with fixed amino-acids");
                            m_sequences.includeShuffled(((AASpecificity) dig).getAminoAcidSpecificity());

                        } else {

                            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Including shuffled sequences");
                            m_sequences.includeShuffled();
                        }
                    } else if (decoyGeneration.contentEquals("random")){
                        if (dig instanceof AASpecificity && decoyDigestionAware) {

                            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Including randomized sequences with fixed amino-acids");
                            m_sequences.includeRandomizedN(((AASpecificity) dig).getAminoAcidSpecificity(),100);

                        } else {

                            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Including random sequences");
                            m_sequences.includeRandomizedN(new HashSet<AminoAcid>(),100);
                        }
                    } else if (decoyGeneration.contentEquals("random_directed")){
                        if (dig instanceof AASpecificity && decoyDigestionAware) {

                            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Including randomized sequences with fixed amino-acids");
                            m_sequences.includeRandomizedDirectedN(((AASpecificity) dig).getAminoAcidSpecificity(),100);

                        } else {

                            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Including random sequences");
                            m_sequences.includeRandomizedDirectedN(new HashSet<AminoAcid>(),100);
                        }
                    }
                    
                } else {
                    Logger.getLogger(this.getClass().getName()).log(Level.INFO, "A decoy-database was configured, so not generating additional decoys");
                    
                }
                
            }
            
        } catch (IOException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error while reading sequences (FASTA-files)", ex);
            return true;
        }
        return false;
    }

    protected void setupScores() {
        int minConservativeLosses = m_config.retrieveObject("ConservativeLosses", 3);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Settup the scores");
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Frament Coverage");
        getConfig().getScores().add(new FragmentCoverage(minConservativeLosses));
        //        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Loss Coverage");
        //        m_config.getScores().add(new LossCoverage());
        //Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Frament Coverage");
        //        CDRIntensityScore cdr = new CDRIntensityScore();
        //        try {
        //            cdr.readStatistic(m_config);
        //            m_config.getScores().add(cdr);
        //        } catch (FileNotFoundException ex) {
        //            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Error reading statistics", ex);
        //        } catch (IOException ex) {
        //            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Error reading statistics", ex);
        //        }
//        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "FragmentLibrary Coverage");
        getConfig().getScores().add(new FragmentLibraryScore(m_Fragments, m_sequences.getCountPeptides()));
//        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Spectar Coverage");
        getConfig().getScores().add(new SpectraCoverage());
        getConfig().getScores().add(new FragmentChargeState());
        getConfig().getScores().add(new SpectraCoverageConservative(minConservativeLosses));
        getConfig().getScores().add(new CombinedScores());
        getConfig().getScores().add(new rappsilber.ms.score.Error(getConfig()));
        getConfig().getScores().add(new BS3ReporterIonScore());
//        getConfig().getScores().add(new Normalizer());
        //getConfig().getScores().add(new LinkSiteDelta());
        getConfig().getScores().add(new NormalizerML(getConfig()));
        // add dummy score for feeding in the delta score
        getConfig().getScores().add(m_deltaScore);
    }

    protected void variableModifications() {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Apply Post Digest FixedMods");
        m_peptidesLinear = m_peptidesLinear.applyFixedModificationsPostDigestLinear(m_config, m_peptides);
        m_peptides = m_peptides.applyFixedModificationsPostDigest(m_config, m_peptidesLinear);
        
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Apply Variable Modifications");
        // apply variable modification
        m_config.getStatusInterface().setStatus("Applying variable modification to non-cross-linkable peptides");
        m_peptidesLinear.applyVariableModificationsLinear(m_config,m_peptides);
        m_config.getStatusInterface().setStatus("Applying variable modification to cross-linkable peptides");
        m_peptides.applyVariableModifications(m_config, m_peptidesLinear);
        m_config.getStatusInterface().setStatus("Applying linear modification to cross-linkable peptides");
        ArrayList<Peptide> linearMods =  new ArrayList<>();
        for (Peptide p: m_peptides) {
            linearMods.addAll(p.modify(m_config,ModificationType.linear));
        }
        for (Peptide p: m_peptidesLinear) {
            linearMods.addAll(p.modify(m_config,ModificationType.linear));
        }
        
        for (Peptide p: linearMods) {
            m_peptidesLinear.addPeptide(p);
        }
    }



    @Override
    public void startSearch() {
        startSearch(m_config.getSearchThreads());
    }


    @Override
    public void addFilter(StackedSpectraAccess f) {
        m_filters.add(f);
    }

    public void startSearch(int numberOfThreads) {
        m_ThreadInput = m_msmInput;


        Object bufferIn = getConfig().retrieveObject("BUFFERINPUT");
        if (bufferIn != null && (Integer.valueOf((String) bufferIn) > 0)) {
            m_ThreadInput = new BufferedSpectraAccess(m_msmInput, Integer.valueOf((String) bufferIn));
        }

        for (StackedSpectraAccess f : m_filters) {
            f.setReader(m_ThreadInput);
            m_ThreadInput = f;
        }

        // should redundant cluster be deleted?
        boolean filterCluster = getConfig().retrieveObject("DELETE_REDUNDANT_CLUSTER", true);
        if (filterCluster) {
            setOutput(new PreFilterResultWriter(m_output, new CleanUpIsotopCluster()));
        }
        
        Object bufferOut = getConfig().retrieveObject("BUFFEROUTPUT");
        if (bufferOut != null && (Integer.valueOf((String) bufferOut) > 0)) {
            BufferedResultWriter bout =  new BufferedResultWriter(m_output, Integer.valueOf((String) bufferOut));
            setOutput(bout);
        }




        m_running = true;

        m_output.writeHeader();
        // fire up the threads
        m_processedInput = m_ThreadInput;
        // just add some more threads so we could dynamically add and remove some
        setSearchThreads(new Thread[numberOfThreads*10+10]);
//        if (numberOfThreads == 1) {
//            getSearchThreads()[0] = new Thread(new SearchRunner(sa, m_output));
//            getSearchThreads()[0].setName("Search");
//            getSearchThreads()[0].run();
//        } else {
            for (int i = 0; i < numberOfThreads; i++) {
                getSearchThreads()[i] = new Thread(new SearchRunner(m_ThreadInput, m_output, m_threadStop[i]));
                m_threadStop[i].set(false);
                getSearchThreads()[i].setName("Search_" + i);
                getSearchThreads()[i].start();
            }
//        }

    }
    
    public void decreaseSearchThread() {
        if (countSelectedSearchThread() > 1) {
            if (m_searchThreads != null)
                for (int i = m_threadStop.length-1; i>=0; i--) {
                    if (!m_threadStop[i].get()) {
                        m_threadStop[i].set(true);
                        break;
                    }
                }
        }
    }

    public void increaseSearchThread() {
        boolean started = false;
        if (m_searchThreads != null)
            for (int i = 0; i<m_threadStop.length; i++) {
                if (m_threadStop[i].get()) {

                    if (m_searchThreads[i] == null || !m_searchThreads[i].isAlive()) {
                        // found a dead thread
                        m_threadStop[i].set(false);
                        getSearchThreads()[i] = new Thread(new SearchRunner(m_ThreadInput, m_output, m_threadStop[i]));
                        getSearchThreads()[i].setName("Search_" + i);
                        getSearchThreads()[i].start();
                        started = true;
                        break;
                    }
                }
            }
        if (!started) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING,"Could not increase the number of threads");
        }
    }

    public int countActiveSearchThread() {
        int c=0;
        if (m_searchThreads != null)
            for (int i = 0; i<m_threadStop.length; i++) {
                if (m_searchThreads[i] !=null && m_searchThreads[i].isAlive()) {
                    c++;
                }
            }
        return c;
    }

    public int countSelectedSearchThread() {
        int c=0;
        if (m_searchThreads != null)
            for (int i = 0; i<m_threadStop.length; i++) {
                if (!m_threadStop[i].get()) {
                    c++;
                }
            }
        return c;
    }
    
    /**
     * Returns the approximate accumulated collection elapsed time
     * in milliseconds for the garbage collection.
     * @return the approximate accumulated collection elapsed time
     * in milliseconds.     
     */
    
    private static long getGarbageCollectionTime() {
        long collectionTime = 0;
        for (GarbageCollectorMXBean garbageCollectorMXBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            collectionTime += garbageCollectorMXBean.getCollectionTime();
        }
        return collectionTime;
    }    
 
    /**
     * checks how much time is spend in gc and if it is deemed to much will 
     * instruct a search thread to shutdown
     */
    protected void checkGC() {
        long gctime = getGarbageCollectionTime();
        // scale 
        long totalTime = (System.nanoTime() - lastGCTestTime)/1000000;
        boolean oneRunning = false;
        if ((lastGCCollectedTime-gctime) / totalTime> 0.8) {
            for (AtomicBoolean ab : m_threadStop) {
                if (!ab.get()) {
                    if (oneRunning) {
                        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, 
                                "\n=================================================\n"+
                                "*** stopping one thread to reduce memory load ***\n"+
                                "=================================================\n");
                        ab.set(m_doStop);
                        lastGCCollectedTime=getGarbageCollectionTime();
                        lastGCTestTime = System.nanoTime();
                        break;
                    } else {
                        oneRunning = true;
                    }
                }
            }
        }
    }

    @Override
    public void waitEnd() {
        lastGCTestTime=System.nanoTime();
        lastGCCollectedTime=getGarbageCollectionTime();
        Thread.currentThread().setName("WaitForEnd(" +  Thread.currentThread().getId()+")");
        boolean running = true;
        int gc = 0;
        long oldProc = -100;
        long noChange = 0;
        long testDeadLock = 20;
        int noChangeDetected = 0;
        Calendar changeDate = Calendar.getInstance();
        long startTime = Calendar.getInstance().getTimeInMillis();
        LinkedList<Long> times = new LinkedList();
        LinkedList<Long> counts = new LinkedList();
        times.add(startTime);
        counts.add(0l);
        
        Timer watchdog =  null;
        // setup a watchdog that kills off the search f no change happen for a long time
        if (m_config.retrieveObject("WATCHDOG", true)) {
            watchdog = new Timer("Watchdog", true);
            TimerTask watchdogTask = new TimerTask() {
                int maxCountDown=30;
                {
                    try {
                        maxCountDown=m_config.retrieveObject("WATCHDOG", 30);
                    } catch(Exception e){};
                }
                int tickCountDown=maxCountDown;
                long lastProcessesd=0;
                int checkGC = 10;
                boolean first = true;
                @Override
                public void run() {
                    try {
                        long proc = getProcessedSpectra();
                        if (lastProcessesd !=proc) {
                            lastProcessesd=proc;
                            tickCountDown=maxCountDown;
                            sendPing();
                        } else {
                            // if we are on the first one double the countdown time
                            if ((proc > 0 &&tickCountDown--==0) || (tickCountDown<-maxCountDown)) {
                                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "\n"
                                        + "================================\n"
                                        + "==       Watch Dog Kill       ==\n"
                                        + "==        Stacktraces         ==\n"
                                        + "================================\n");

                                Util.logStackTraces(Level.SEVERE);
                                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "\n"
                                        + "================================\n"
                                        + "== stacktraces finished ==\n"
                                        + "================================");
                                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Long time no change - assuming something is wrong -> exiting");
                                System.exit(1000);
                            } else {
                                if (first) {
                                    first = false;
                                    return;
                                }
                                System.out.println("****WATCHDOG**** countdown " + tickCountDown);
                                if (tickCountDown%5 == 0) {
                                    Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Long time no change - count down to kill : " + tickCountDown + " minutes");
                                }
                                // we haven't given up yet so lets ping that we are still alive
                                sendPing();
                            }
                        }       
                        if (--checkGC==0) {
                            checkGC();
                            checkGC=10;
                        }
                    } catch (Exception e) {
                        Logger.getLogger(this.getClass().getName()).log(Level.WARNING,"Error im watchdog : ", e);
                    }
                }

                /**
                 * starts the ping in its own thread so as not to interfere with the watchdog
                 */
                public void sendPing() {
                    // ping the world to say we are still alive
    //                Runnable runnablePing = new Runnable() {
    //                    public void run() {
    //                        m_output.ping();
    //                    }
    //                };
    //                Thread t = new Thread(runnablePing, "ping");
    //                t.setDaemon(true);
    //                t.start();
                }
            };
            watchdog.scheduleAtFixedRate(watchdogTask, 10, 60000);
        }
        
        
        while (running && !m_config.searchStopped()) {
            for (int i = 0; i < getSearchThreads().length; i++) {
                if (getSearchThreads()[i] != null && getSearchThreads()[i].isAlive()) {
                    try {
                        getSearchThreads()[i].join(500);


                    } catch (InterruptedException ex) {
                        Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Interrupt while waiting for thread to finish", ex);
                    }
                }
            }
            long proc = getProcessedSpectra();
            if (oldProc != proc) {
                
                String status = "";
                int allSpectra = m_msmInput.getSpectraCount();
                if (allSpectra>0) {
                    long filteredOut = m_msmInput.getDiscardedSpectra();
                    long procTotal = proc+m_msmInput.getDiscardedSpectra();
                    int procPerc = (int)(procTotal*100/m_msmInput.getSpectraCount());
                    long remaining = allSpectra - procTotal;
                    times.add(Calendar.getInstance().getTimeInMillis());
                    counts.add(proc);
                    if (times.size()> 10) {
                        times.removeFirst();
                        counts.removeFirst();
                    }
                    
                    double timePerSpectrumLast100 = (times.getLast()-times.getFirst())/ (double)(counts.getLast()-counts.getFirst());

                    double timePerSpectrumTotal = (times.getLast()-startTime) / (double)counts.getLast();
                    
                    if (filteredOut >0) {
                        status = procPerc +"% processed (" + proc + " + " + filteredOut + " fitlered out" + ") " + m_msmInput.countReadSpectra() + " read of " + m_msmInput.getSpectraCount();
                    } else {
                        status = procPerc +"% processed (" + proc + ") " + m_msmInput.countReadSpectra() + " read of " + m_msmInput.getSpectraCount();
                    }
                    if (proc > 0) {
                        if (timePerSpectrumLast100 != timePerSpectrumTotal) {
                            long r1 =  (long)(timePerSpectrumLast100 * remaining);
                            long r2 =  (long)(timePerSpectrumTotal * remaining);
                            String from  = Util.millisToTime(Math.min(r1,r2), Math.max(r1,r2));
                            String to  = Util.millisToTime(Math.max(r1,r2));
                            if (from.contentEquals(to)) {
                                status += " Estimated remaining: " + from;
                            } else {
                                status += " Estimated remaining: " + from +" to " + to;
                            }
                        } else {
                            long r1 =  (long)(timePerSpectrumLast100 * remaining);

                            status += " Estimated time remaining: " + Util.millisToTime(r1);

                        }
                    }
                    
                } else {
                    status = proc + " spectra processed " + m_msmInput.countReadSpectra() + " read of " + m_msmInput.getSpectraCount();
                }
                m_config.getStatusInterface().setStatus(status);
                oldProc = proc;
                noChange = 0;
                changeDate = Calendar.getInstance();
                noChangeDetected = 0;
            } else if (++noChange > testDeadLock) {
                
                // we might be in a deadlock
                
                ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                long[] threadIds = bean.findDeadlockedThreads(); // Returns null if no threads are deadlocked.

                if (threadIds != null) {
                    StringBuilder sb = new StringBuilder();
                    ThreadInfo[] infos = bean.getThreadInfo(threadIds);
                    for (ThreadInfo info : infos) {
                        StackTraceElement[] stack = info.getStackTrace();
                        // Log or store stack trace information.
                        sb.append("--------------------------\n");
                        sb.append("--- Thread stack-trace ---\n");
                        sb.append(MyArrayUtils.toString(stack, "\n"));
                    }

                    // we are in a deadlock - ouch
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "\n"
                            + "=======================\n"
                            + "== deadlock detected ==\n"
                            + "=======================\n{0}", sb.toString());

                    System.exit(-2);
                }   else {
                    
                    testDeadLock*=10;
                    
                    // we are supposedly not in a deadlock
                    if (noChange > 10 && (++noChangeDetected)<2) {
                        
                        Calendar now = Calendar.getInstance();
                        double delay = (now.getTimeInMillis() - changeDate.getTimeInMillis())/ 1000.0;
                        // but nothing has happend for quite a while - so what is going on
                        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "\n"
                                + "================================\n"
                                + "== long time without activity ==\n"
                                + "================================\n"
                                + "\nno change for at least : " + delay + " seconds (" + (delay/60) + " minutes)\n");
                        if (m_auto_stacktrace) {

                            Util.logStackTraces(Level.INFO);
                            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "\n"
                                    + "================================\n"
                                    + "==    stacktraces finished    ==\n"
                                    + "================================");
                        }
                    }
                }
                
            }
            running = false;
            for (int i = 0; i < getSearchThreads().length; i++) {
                if (getSearchThreads()[i] != null && getSearchThreads()[i].isAlive()) {
                    running = true;
                    break;
                }
            }
            
            
        }

        long proc = getProcessedSpectra();
        if (m_msmInput.getSpectraCount() >0) {
            long filteredOut = m_msmInput.getDiscardedSpectra();
            long procTotal = proc+m_msmInput.getDiscardedSpectra();
            int procPerc = (int)(procTotal*100/m_msmInput.getSpectraCount());
                        
            if (filteredOut >0)
                m_config.getStatusInterface().setStatus(procPerc +"% processed (" + proc + " + " + filteredOut + " fitlered out" + ") " + m_msmInput.countReadSpectra() + " read of " + m_msmInput.getSpectraCount());
            else
                m_config.getStatusInterface().setStatus(procPerc +"% processed (" + proc + ") " + m_msmInput.countReadSpectra() + " read of " + m_msmInput.getSpectraCount());
//            int procPerc = (int)(getProcessedSpectra()*100/m_msmInput.getSpectraCount());
//            m_config.getStatusInterface().setStatus(procPerc +"% processed (" + proc + ") " + m_msmInput.countReadSpectra() + " read of " + m_msmInput.getSpectraCount());
        } else if (proc==0) {
            m_config.getStatusInterface().setStatus("Error: no Spectra found to process");
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,"Error: no Spectra found to process");
            
            System.exit(-1);
        }else {
            m_config.getStatusInterface().setStatus(proc + " spectra processed " + m_msmInput.countReadSpectra() + " read of " + m_msmInput.getSpectraCount());
        }
        

        if (m_msmInput.hasNext() && !m_config.searchStopped()) {
            emptyBufferedWriters();
            for (BufferedResultWriter brw : (LinkedList<BufferedResultWriter>)BufferedResultWriter.allActiveWriters.clone()) {
                if (brw.getInnerWriter() instanceof BufferedResultWriter) {
                    brw.flush();
                    brw.finished();
                    brw.waitForFinished();
                    brw.selfFinished();
                }
            }
            String msg = " Looks like for some reason peaklists are not completely read in yet - restarting search ...";
            m_config.getStatusInterface().setStatus(msg);
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, msg);
            startSearch();
            waitEnd();
            return;
        }
        
        // make sure all the results are write out
        if (!BufferedResultWriter.allActiveWriters.isEmpty()) {
            Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Search Finished but buffers still filled. Will wait for buffers to flush" );

            int empty = emptyBufferedWriters();
            
            if (empty==0) {
                Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Buffer not empty - might have lost some data");
            }
            
            for (BufferedResultWriter brw : (LinkedList<BufferedResultWriter>)BufferedResultWriter.allActiveWriters.clone()) {
                brw.flush();
            }
            
            for (BufferedResultWriter brw : (LinkedList<BufferedResultWriter>)BufferedResultWriter.allActiveWriters.clone()) {
                brw.flush();
                brw.finished();
                brw.waitForFinished();
            }
            BufferedResultWriter.allActiveWriters.clear();
            Logger.getLogger(this.getClass().getName()).log(Level.FINE, "All write through buffers are empty. flushing the output" );
            if (m_output instanceof AbstractStackedResultWriter) {
                ResultWriter rw = ((AbstractStackedResultWriter) m_output).getInnerWriter();
                while (rw instanceof AbstractStackedResultWriter) {
                    rw.flush();
                    rw.finished();
                    rw.waitForFinished();
                    rw = ((AbstractStackedResultWriter) rw).getInnerWriter();
                }
                rw.flush();
                rw.finished();
                rw.waitForFinished();
            }
            m_output.flush();
            m_output.finished();
            m_output.waitForFinished();
            Logger.getLogger(this.getClass().getName()).log(Level.FINE, "It should be save to say good by now." );
        }
        
        if (m_msmInput.hasNext() && !m_config.searchStopped()) {
            startSearch();
            waitEnd();
            return;
        }

        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "\n==============================\nCounts from all buffered writer\n==============================");
        for (BufferedResultWriter brw : BufferedResultWriter.allWriters) {
            Logger.getLogger(this.getClass().getName()).log(Level.FINE, brw.getBufferThread().getName() +"\n\t All Results : " + brw.getResultCount() + "\n\t" 
                    + " Top Results : " + brw.getTopResultCount() + "\n\t" 
                    + " Forwarded Results : " + brw.getForwardedMatchesCount());
        }
        BufferedResultWriter.allWriters.clear();
        
        proc = getProcessedSpectra();
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Search Finished  (" + proc + ") " + m_msmInput.countReadSpectra() + " read of " + m_msmInput.getSpectraCount());
        m_config.getStatusInterface().setStatus("Finished (" + proc + ") " + m_msmInput.countReadSpectra() + " read of " + m_msmInput.getSpectraCount() + " waiting for writing out results");

        m_output.finished();
        m_output.waitForFinished();
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "\nAll Results : " + m_output.getResultCount() + "\n" 
                + " Top Matches : " + m_output.getTopResultCount());
        

        m_running = false;
//        m_config.getStatusInterface().setStatus("Finished");
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Open Threads:");
        System.err.flush();
        System.out.flush();
        Util.logStackTraces(Level.FINE);
        
        if (AbstractScoreSpectraMatch.DO_STATS) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, ScoreStatistic());
        }
        
        if (m_debugFrame != null) {
            m_debugFrame.setVisible(false);
            m_debugFrame.dispose();
        }
        if (watchdog != null)
            watchdog.cancel();

        m_config.getStatusInterface().setStatus("completed");
        
        if (countActiveThreads()>1){
            int delay = 60000;
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING,"There seem to be some open threads that have not finished yet. Will kill them after {0} seconds.", delay/1000  );
            new Timer("kill tasks", true).schedule(new TimerTask() {
                @Override
                public void run() {
                    if (countActiveThreads()>1) {
                        Logger.getLogger(this.getClass().getName()).log(Level.WARNING,"Forcefully closing the search"  );
                        Util.logStackTraces(Level.WARNING);
                        Logger.getLogger(this.getClass().getName()).log(Level.WARNING,"Still open threads - Forcefully closing xi"  );
                        for (Handler h : Logger.getGlobal().getHandlers()) {
                            h.flush();
                        }
                        m_config.getStatusInterface().setStatus("completed");
                        System.exit(-1);
                    } else {
                        Logger.getLogger(this.getClass().getName()).log(Level.WARNING,"No Warning: Threads did shut down by themself"  );
                    }
                }
            }, 5000);
        }
        

    }

    protected int emptyBufferedWriters() {
        long c = Calendar.getInstance().getTimeInMillis();
        int empty = 0;
        while (empty <2 && Calendar.getInstance().getTimeInMillis() - c < 1000*60*10) {
            
            boolean notEmpty = false;
            for (BufferedResultWriter brw : (LinkedList<BufferedResultWriter>)BufferedResultWriter.allActiveWriters.clone()) {
                brw.selfFlush();
                if (!brw.isBufferEmpty()) {
                    notEmpty = true;
                    if (!brw.isAlive()) {
                        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Buffer is not empty but the writer is dead - remaining: {0} - try to restart", brw.bufferedMatches());
                        brw.startProcessing();
                    }
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            }
            if (notEmpty)
                empty=0;
            else
                empty++;
        }
        return empty;
    }

    /**
     * counts active threads in the current group
     **/
    protected int countActiveThreads() {
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        Thread[] active = new Thread[tg.activeCount()*100];
        tg.enumerate(active, true);
        int c =0;
        for (Thread t : active) {
            if (t != null && !t.isDaemon() && t.isAlive()) {
                c++;
            }
        }
        return c;
    }
    
    /**
     * kills all threads in the current group except or the current thread.
     * Actually it defines all other threads as daemons and by that stopping 
     * them from prolonging the life of the application.
     **/
    protected int killOtherActiveThreads() {
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        boolean killed = false;
        int tries = 10;
        int c =0;
        HashSet<Thread> threadNonDemonizable = new HashSet<>();
        
        do {
            try {
                Thread.currentThread().sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(SimpleXiProcess.class.getName()).log(Level.SEVERE, null, ex);
            }
            tries --;
            Thread[] active = new Thread[tg.activeCount()*100];
            tg.enumerate(active, true);
            killed = false;
            for (Thread t : active) {
                if (t != null) {
                    if (t.isAlive() && t != Thread.currentThread() && (!t.isDaemon()) && (!t.getName().contains("DestroyJavaVM")) && (!t.getName().contains("AWT-EventQueue-0") && !threadNonDemonizable.contains(t))) {
                        Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Try to daemonise {0}", t.getName());
                        try {
                            killed = true;
                            t.setDaemon(true);
                        } catch (Exception ex) {
                            threadNonDemonizable.add(t);
                            
                            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "could not daemonise {0}, will be ignored for now", t.getName());
                            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, MyArrayUtils.toString(t.getStackTrace(),"\n"));
                        }
                    }
                }
            }
        } while (killed == true || tries >0);
        if (killed && tries == 0) {
            Util.logStackTraces(Level.WARNING);
        }
        return c;
    }

    


    @Override
    public void stop() {
        m_doStop = true;
        m_config.stopSearch();
    }

    public void process(SpectraAccess input, ResultWriter output, AtomicBoolean threadStop) {    
        try {

            // m_sequences.a


            long allfragments = m_Fragments.getFragmentCount();

            boolean evaluateSingles = getConfig().isEvaluateLinears();

            int countSpectra = 0;
            // go through each spectra
            int processed=0;
            msmloop: while (input.hasNext()) {
                processed ++;

                if (input.countReadSpectra() % 100 ==  0) {
                    System.err.println("Spectra Read " + input.countReadSpectra() + "\n");
                }

                if (m_doStop)
                    break;
                // ScoredLinkedList<Peptide,Double> scoredPeptides = new ScoredLinkedList<Peptide, Double>();
                Spectra spectraAllchargeStatess = input.next();

                if (spectraAllchargeStatess == null) {
                    System.err.println("warning here - did not get a spectra");
                    new Exception().printStackTrace();

                    continue;
                }
                countSpectra ++;


                ArrayList<MatchedXlinkedPeptide> scanMatches = new ArrayList<MatchedXlinkedPeptide>();



                for (Spectra spectra : spectraAllchargeStatess.getChargeStateSpectra()) {

                    Spectra mgc = spectra.getMgcSpectra(getConfig().getNumberMgcPeaks());
                    spectra.getIsotopeClusters().clear();

                    getConfig().getIsotopAnnotation().anotate(spectra);

                    double precoursorMass = spectra.getPrecurserMass();

                    ArithmeticScoredOccurence<Peptide> mgcMatchScores = new ArithmeticScoredOccurence<Peptide>();
                    double maxMass = m_PrecoursorTolerance.getMaxRange(precoursorMass);
                    // if we ignore singles then the maxImal mass can even be reduced by crosslinker and smallest Aminoacid
                    if (!evaluateSingles) {
                        maxMass -= m_smallestCrosslinkedPeptideMass;
                    }

                    //   go through mgc spectra
                    for (SpectraPeak sp : mgc) {
                        //      for each peak
                        //           count found peptides
                        ArrayList<Peptide> matchedPeptides = m_Fragments.getForMass(sp.getMZ()); // - Util.PROTON_MASS);
                        double peakScore = (double) matchedPeptides.size() / allfragments;


                        for (Peptide p : matchedPeptides) {
                            // don't look at peptides to large for the spectra
                            if (p.getMass() <= maxMass)
                                mgcMatchScores.multiply(p, peakScore);

                        }

                    }
//                    mgc.free();

                    Peptide[] scoreSortedAlphaPeptides = mgcMatchScores.getScoredSortedArray(new Peptide[mgcMatchScores.size()]);


                    HashSet<String> foundPeptideSequences = new HashSet<String>();


                    int lastIndex = Math.min(scoreSortedAlphaPeptides.length, getConfig().getTopMGCHits()) - 1;
                    int indexBookmark = lastIndex;

                    //Object[] alphaList = scoredPeptides.toArray();
                    int lastPossibleIndex = scoreSortedAlphaPeptides.length - 1;

                    while (lastIndex < lastPossibleIndex) {
                        if (mgcMatchScores.Score(scoreSortedAlphaPeptides[lastIndex], 1) != mgcMatchScores.Score(scoreSortedAlphaPeptides[lastIndex + 1], 1))
                            break;
                        lastIndex ++;
                    }

                    if (lastIndex > indexBookmark * indexBookmark + 10) {

                        lastIndex = indexBookmark;

                        while (lastIndex > 1) {
                            if (mgcMatchScores.Score(scoreSortedAlphaPeptides[lastIndex], 1) != mgcMatchScores.Score(scoreSortedAlphaPeptides[lastIndex - 1], 1))
                                break;
                            lastIndex --;
                        }
                        
                    }



                    alphaLoop: // go through selected aplha candidates
                    for (int i = 0; i <= lastIndex; i++) {

                        
                        Peptide alphaFirst = scoreSortedAlphaPeptides[i];
                        double pepMass = alphaFirst.getMass();

                        // is this a linear match?
                        if (m_PrecoursorTolerance.compare(precoursorMass, pepMass) == 0) {
                            //linear fragment match
                            if (evaluateSingles)
                                evaluateMatch(spectra.cloneComplete(), alphaFirst, null, null, 0, scanMatches, false);
                        
                        } else { // no could be cross-linked

                            ArrayList<CrossLinker> cls = new ArrayList<CrossLinker>(m_Crosslinker.size());
                            boolean matched = false;
                            for (CrossLinker cl : m_Crosslinker) {
                                double crosslinkerMass = cl.getCrossLinkedMass();
                                double crosslinkerContaining = pepMass + crosslinkerMass;
                                if (m_PrecoursorTolerance.compare(precoursorMass, crosslinkerContaining) > 0) {
                                    cls.add(cl);
                                }
                            }

                            for (CrossLinker cl : cls) {
                                double crosslinkerMass = cl.getCrossLinkedMass();
                                double crosslinkerContaining = pepMass + crosslinkerMass;

                                double queryMass = precoursorMass - crosslinkerContaining;
                                ArrayList<Peptide> betaPeptides = null;
                                try {
                                    betaPeptides = m_peptides.getForMass(queryMass);
                                } catch (Exception ex) {
                                    System.err.println(" found it");
                                }

                                String alphaId = alphaFirst.getSequence().getFastaHeader() + "_" + alphaFirst.toString();
                                int betaCount = betaPeptides.size();
                                betaSearch:
                                betaloop: for (Peptide beta : betaPeptides) {
                                    
                                    //alpha
                                    //beta = beta.clone();
                                    for (CandidatePairFilter cf : getCadidatePairFilter()) {
                                        if (!cf.passes(spectra, cl, alphaFirst, beta)) {
                                            continue betaloop;
                                        }
                                    }
                                    
                                    String betaID = beta.getSequence().getFastaHeader() + "_" + beta.toString();

                                    // don't search alpha and beta reveresed
                                    if (foundPeptideSequences.contains(betaID + "_" + alphaId)) {
                                        continue betaloop;
                                    }

                                    // remember we foudn these
                                    foundPeptideSequences.add(alphaId + "_" + betaID);

                                    evaluateMatch(spectra.cloneComplete(), alphaFirst, beta, cl, betaCount, scanMatches, false);

                                }

                            }


                        }

                    }
                }

                java.util.Collections.sort(scanMatches, new Comparator<MatchedXlinkedPeptide>() {
                    @Override
                    public int compare(MatchedXlinkedPeptide o1, MatchedXlinkedPeptide o2) {
                        return Double.compare(o2.getScore(MatchScore), o1.getScore(MatchScore));
                    }
                });


                int countMatches = scanMatches.size();
                if (countMatches>0) {
                    MatchedXlinkedPeptide[] matches = scanMatches.toArray(new MatchedXlinkedPeptide[0]);
                    outputScanMatches(matches, output);

                }
                scanMatches.clear();

                if (processed >= 10) {
                    increaseProcessedScans(processed);
                    processed=0;
                }
                if (threadStop.get()) {
                    System.err.println("Closing down search thread " + 
                            Thread.currentThread().getName());
                    break;
                }

            }
            
            //System.err.println("Spectras processed here: " + countSpectra);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, 
                    "Error while processing spectra", e);
            System.err.println(e);
            e.printStackTrace(System.err);
            setStatus("Error while processing spectra" + e);
            System.exit(0);
        }

    }

    public String ScoreStatistic() {
        StringBuilder sb = new StringBuilder("scorer ,score,min,max,average,"
                + "stdDev,median,mad\n");
        for (ScoreSpectraMatch ssm : getConfig().getScores()) {
            String scorer = ssm.name();
            for (String score : ssm.scoreNames()) {
                double min = ssm.getMin(score);
                double max = ssm.getMax(score);
                double average = ssm.getAverage(score);
                double stdDev = ssm.getStdDev(score);
                double median = ssm.getMedian(score);
                double mad = ssm.getMAD(score);
                sb.append(scorer).append(",").append(score).append(",,").
                        append(min).append(",").append(max).append(",").
                        append(average).append(",").append(stdDev).
                        append(",").append(median).append(",").append(mad).
                        append("\n");

            }
        }
        return sb.toString();

    }




    protected MatchedXlinkedPeptide getMatch(Spectra s, Peptide alphaFirst, Peptide beta, CrossLinker cl, boolean primaryOnly) {
        Spectra sClone = s.cloneComplete();
        MatchedXlinkedPeptide match = new MatchedXlinkedPeptide(s, alphaFirst, beta, cl,getConfig(), primaryOnly);
        return match;
    }

    
    protected MatchedXlinkedPeptide getMatch(Spectra s, Peptide[] peptides, CrossLinker cl, boolean primaryOnly) {
        Spectra sClone = s.cloneComplete();
        MatchedXlinkedPeptide match;
        if (cl instanceof SymetricNarrySingleAminoAcidRestrictedCrossLinker) {
             match = new MatchedXlinkedPeptideWeightedNnary(s, peptides, cl,getConfig(), primaryOnly);
        } else {
            match = new MatchedXlinkedPeptide(s, peptides[0], peptides.length >1? peptides[1] : null, cl,getConfig(), primaryOnly);
        }
        return match;
    }
    
    protected MatchedXlinkedPeptide evaluateMatch(Spectra s, Peptide alphaFirst, Peptide beta, CrossLinker cl, int betaCount, Collection<MatchedXlinkedPeptide> scanMatches, boolean primaryOnly) {
        return evaluateMatch(getMatch(s, alphaFirst, beta, cl, primaryOnly), betaCount, scanMatches, primaryOnly);

    }

    protected MatchedXlinkedPeptide evaluateMatch(MatchedXlinkedPeptide match, int betaCount, Collection<MatchedXlinkedPeptide> scanMatches, boolean primaryOnly) {
        match.setCountPossibleBeta(betaCount);
        match.matchPeptides();
        
        filterMatch(match);

        if (match.getMatchedFragments().isEmpty())
            return null;

        for (ScoreSpectraMatch ssm : getConfig().getScores()) {
            ssm.score(match);
        }
//        for (double score : match.getScores().values())
//            if(Double.isNaN(score)) {
//                System.err.println("found it " + this.getClass().getName());
//                for (ScoreSpectraMatch ssm : getConfig().getScores()) {
//                    ssm.score(match);
//
//                }
//            }

        scanMatches.add(match);
        return match;
    }



    public void outputScanMatches(MatchedXlinkedPeptide[] matches, ResultWriter output)  throws IOException {

        if (matches.length == 1) {
            MatchedXlinkedPeptide m = matches[0];
            double score = m.getScore(MatchScore);
            if (score >= m_minTopScore)  {
                m_deltaScore.setScore(m,"delta", score);
                m_deltaScore.setScore(m,"deltaMod", score);
                m_deltaScore.setScore(m, "combinedDelta", score);
                m.setMatchrank(1);
                linksitedelta.score(m);
                output.writeResult(m);
            }
        } else {
            MatchedXlinkedPeptide topMatch = matches[0];
            double topScore = topMatch.getScore(MatchScore);
            // find the top score
            for (int m=1;m<matches.length;m++) {
                MatchedXlinkedPeptide match = matches[m];
                double s = match.getScore(MatchScore);

                if (s > topScore)  {
                    topScore = s;
                    topMatch = match;
                }

            }

            // make modification independend lookup for top-match
            HashMap<String,HashSet<String>> topMatchHash = new HashMap<String,HashSet<String>>();
            Peptide[] topPeps = topMatch.getPeptides();
            matchToBaseLookup(topPeps, topMatchHash);

            // and all others of the same score
            for (int m = 0; m< matches.length ; m++) {
                if (matches[m].getScore(MatchScore) == topScore) {
                    Peptide[] peps = matches[m].getPeptides();
                    matchToBaseLookup(peps, topMatchHash);
                }
            }

            double noModSecond = -Double.MAX_VALUE;
            // now find the second best distinct match (meaning different peptide sequences)
            for (int m = 0; m< matches.length ; m++) {
                if (matches[m].getScore(MatchScore) < topScore) {
                    Peptide[] peps = matches[m].getPeptides();
                    String fpep = peps[0].toStringBaseSequence();
                    boolean linear = peps.length == 1;
                    
                    HashSet<String> baseLookup =  topMatchHash.get(fpep);
                    if (baseLookup == null) { // first peptide i non of the top matches
                        double matchScore  = matches[m].getScore(MatchScore);
                        if (noModSecond<matchScore) {
                            noModSecond = matchScore;
                        }
                    } else {
                        // go through the secondary peptides
                        if (peps.length == 1)
                            // its linear and we found this already
                            continue;
                        //
                        double matchScore  = matches[m].getScore(MatchScore);
                        for (int p = 1; p<peps.length; p++) {
                            if (!baseLookup.contains(peps[p].toStringBaseSequence())) {
                                if (noModSecond<matchScore) {
                                    noModSecond = matchScore;
                                }
                                break;
                            }
                        }
                    }
                }
            }


            double secondScore = (topScore < 0 ? topScore : 0);
            // ignore matches smaller then ten

            if (topScore < m_minTopScore) 
                return;



            for (int m=1;m<matches.length;m++) {
                MatchedXlinkedPeptide match = matches[m];
                double s = match.getScore(MatchScore);

                if (s > topScore)  {
                    secondScore = topScore;
                    topScore = s;
                } else if (s > secondScore) {
                    secondScore = s;
                }

            }


            int rank = 1;

            MatchedXlinkedPeptide m = matches[0];
            m.setMatchrank(rank);

            if (secondScore == -Double.MAX_VALUE) 
                secondScore = 0;
            if (noModSecond == -Double.MAX_VALUE) 
                noModSecond = 0;

            double s = m.getScore(MatchScore);
            double delta = s - noModSecond;
            double combined = (delta + topScore)/2;
            double deltaMod = s - secondScore;

    //        double delta = s - secondScore;
            m_deltaScore.setScore(m,"delta", delta);
            m_deltaScore.setScore(m,"deltaMod", deltaMod);
            m_deltaScore.setScore(m, "combinedDelta", combined);

            linksitedelta.score(m);
            output.writeResult(m);
            
            double lastS = s;
            int i;
            for (i = 1; i < matches.length ; i++) {
                m = matches[i];
                s = m.getScore(MatchScore);
                if (s != lastS)
                    break;
                delta = s - noModSecond;
                combined = (delta + topScore)/2;
                deltaMod = s - secondScore;

                m_deltaScore.setScore(m,"delta", delta);
                m_deltaScore.setScore(m,"deltaMod", deltaMod);
                m_deltaScore.setScore(m, "combinedDelta", combined);
                m.setMatchrank(rank);
                linksitedelta.score(m);
                //only the first writen spectrum needs to have peaks
                if ((!BufferedResultWriter.m_ForceNoClearAnnotationsOnBuffer) && BufferedResultWriter.m_clearAnnotationsOnBuffer) {
                    m.clearAnnotations();
                    m.setSpectrum(m.getSpectrum().getOrigin());
                }
                output.writeResult(m);
            }
            
            if (OutputTopOnly())
                return;

            for (; i < matches.length ; i++) {
                m = matches[i];
                s = m.getScore(MatchScore);
                if (s != lastS)
                    rank++;
                delta = s - noModSecond;
                combined = (delta + topScore)/2;
                deltaMod = s - secondScore;

                m_deltaScore.setScore(m,"delta", delta);
                m_deltaScore.setScore(m,"deltaMod", deltaMod);
                m_deltaScore.setScore(m, "combinedDelta", combined);
                m.setMatchrank(rank);
                output.writeResult(m);
                lastS = s;
            }
        }

    }



    @Override
    public SequenceList getSequenceList() {
        return m_sequences;
    }


    private double getIntensitySupport(MatchedBaseFragment mbf, Fragment f, Peptide pep1, UpdateableInteger countNonLossy) {
        double supportingIntensity= 0;
        if (mbf.isBaseFragmentFound()) {
            if (f.getPeptide() == pep1) {
                supportingIntensity+= mbf.getBasePeak().getIntensity();
                countNonLossy.value ++;
            }
        }
        // lossy peaks are only counted with 1/10 of their intensity
        for (SpectraPeak lp : mbf.getLosses().values()) {
            supportingIntensity += lp.getIntensity()/10;
        }
        return supportingIntensity;
    }

    private double getIntensitySupportCrosslinked(MatchedFragmentCollection mfc, Peptide pep1, Peptide pep2, int link1, int link2, UpdateableInteger countNonLossy) {
        double supportingIntensity = 0;
        countNonLossy.value = 0;
        // find non-crosslinked y ion of pep1 or crosslinked b-yons of pep1
        // and non-crosslinked b ions of peptide 2 or crosslinked large y-ions
        for (MatchedBaseFragment mbf : mfc) {
            Fragment f = mbf.getBaseFragment();
            // ignore double fragmentation
            if (f.isClass(DoubleFragmentation.class))
                continue;
            
            if (f.getPeptide() == pep1) {
            // linear fragments after the linkage site
                if (f.getStart() > link1 && f.isCTerminal()) {
                    supportingIntensity = getIntensitySupport(mbf, f, pep1, countNonLossy);
                } else if (f.isNTerminal() && f.getEnd() >= link1) {
                    supportingIntensity = getIntensitySupport(mbf, f, pep1, countNonLossy);
                }
            } else {
                // linear fragments before the linkage site
                if (f.getEnd() < link2 && f.isNTerminal()) {
                    supportingIntensity = getIntensitySupport(mbf, f, pep2, countNonLossy);
                } else if (f.isCTerminal() && f.getStart() <= link2) {
                    supportingIntensity = getIntensitySupport(mbf, f, pep2, countNonLossy);
                }
            }
        }
        return supportingIntensity;
    }    
    
    /**
     *  checks whether a peptide pair could be a linear match
     * if it is found as a possible linear, check, whether there are unique 
     * @return 
     */
    protected boolean isCrosslinked(MatchedXlinkedPeptide match) {
        // if we have only one peptide, then there is no question
        if (match.getPeptides().length == 1)
            return false;
        
        if (match.getMightBeLinear()) {
//            MatchedFragmentCollection mfc = match.getMatchedFragments();
            MatchedFragmentCollection mfc = match.getUniquelyMatchedFragments();
            // find the protein, where they could be linear
            Peptide pep1 = match.getPeptides()[0];
            int link1 = match.getLinkingSite(0);
            Peptide pep2 = match.getPeptides()[1];
            int link2 = match.getLinkingSite(1);
            for (Peptide.PeptidePositions pp1 : pep1.getPositions()) {
                for (Peptide.PeptidePositions pp2 : pep2.getPositions()) {
                    if (pp1.base == pp2.base) {
                        UpdateableInteger countNonLossySupport = new UpdateableInteger(0);
                        if (pp1.start+pep1.getLength() == pp2.start) {
                            double supportingIntensity = getIntensitySupportCrosslinked(mfc, pep1, pep2, link1, link2, countNonLossySupport);

                            // we assume, that it is still a cross-linked peptide, if we have at least 10% base-intensity explained
                            if (supportingIntensity >= match.getSpectrum().getMaxIntensity()/20 || countNonLossySupport.value >= 3)
                                return true;
                            else 
                                return false;
                        
                        } else if (pp2.start+pep2.getLength() == pp1.start) {
                            double supportingIntensity = getIntensitySupportCrosslinked(mfc, pep2, pep1, link2, link1, countNonLossySupport);

                            // we assume, that it is still a cross-linked peptide, if we have at least 10% base-intensity explained
                            if (supportingIntensity >= match.getSpectrum().getMaxIntensity()/20  || countNonLossySupport.value >= 3)
                                return true;
                            else 
                                return false;
                             

                        }
                    }
                }
                
            }
        }
        return true;
        
    }

    /**
     * assumes a score sorted list of matches and potentially pulls a linear 
     * match to the front of the list.
     * <br/>Two settings are used:
     * <ul><li>prioritizelinears : generally try to give linear matches a better 
     * chance at coming through as our score potentially counter selects for them</li>
     * <li>testforlinearmod  : if a cross-link of consecutive peptides was also 
     * matched as a linear peptide (I.e. cross-linker modified) and does as linear 
     * peptide not have less fragments matched then we would consider this the 
     * more likely explanation at the moment</li></ul>
     * @param scanMatches sorted list of matches.
     */
    protected void checkLinearPostEvaluation(ArrayList<MatchedXlinkedPeptide> scanMatches) {
        // is a cross-link
        if (m_testlinear && scanMatches.size()>0 && scanMatches.get(0).getPeptides().length > 1) {
            
            MatchedXlinkedPeptide xl = scanMatches.get(0);
            // should we generally prioritize linears
            if (m_prioritizelinears) {
                // yes so we look for the best linear match
                for (MatchedXlinkedPeptide m : scanMatches) {
                    // is this a linear match
                    if (m.getPeptides().length == 1) {
                        MatchedXlinkedPeptide l= m;
                        
                        double frags = l.getScore("fragment "+FragmentCoverage.mpUNL)/xl.getScore("fragment "+FragmentCoverage.mpUNL);
                        double intensity = l.getScore(SpectraCoverageConservative.class.getSimpleName())/xl.getScore(SpectraCoverageConservative.class.getSimpleName());
                        double ratio = (frags+1.5*intensity)/2.5;
                        // does it explain more of the spectrum and has more fragments?
                        if (ratio >0) {
                            // switch to top
                            scanMatches.remove(l);
                            scanMatches.add(0, l);
                        }
                        // we only need to look at the top-matching linear
                        break;
                    }
                    
                }
                
            }
            
            if (m_testforlinearmod) {
                // even if we don't generaly prioritize linears
                // if the top-match is of sequence consecutive peptides
                // we need to check if the linear would be just as ok.
                if (xl.getMightBeLinear()) {
                    // what would be the linear peptide
                    String pep1 = xl.getPeptide1().toStringBaseSequence();
                    String pep2 = xl.getPeptide2().toStringBaseSequence();
                    for (MatchedXlinkedPeptide m : scanMatches) {
                        // is this a linear version of the top-match?
                        if (m.getPeptides().length == 1 &&
                                (m.getPeptide1().toStringBaseSequence().contentEquals(pep1 +pep2) ||
                                m.getPeptide1().toStringBaseSequence().contentEquals(pep2 +pep1))) {
                            MatchedXlinkedPeptide l= m;
                            if (l.getScore("fragment "+FragmentCoverage.mpUNL)>=xl.getScore("fragment "+FragmentCoverage.mpUNL)-1 ) {
                                scanMatches.remove(l);
                                scanMatches.add(0, l);
                                break;
                            }
                        }
                        
                    }
                }
            }
        }
    }
    
    public synchronized long increaseProcessedScans(int count) {
        m_processedSpectra+=count;
        return m_processedSpectra;
    }
    
    @Override
    public long getProcessedSpectra() {
        return m_processedSpectra;
    }
    
    
}
