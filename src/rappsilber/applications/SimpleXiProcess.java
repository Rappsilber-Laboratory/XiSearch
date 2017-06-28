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
import java.io.IOException;
import java.lang.management.LockInfo;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.AbstractRunConfig;
import rappsilber.config.RunConfig;
import rappsilber.gui.components.DebugFrame;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.crosslinker.SymetricNarrySingleAminoAcidRestrictedCrossLinker;
import rappsilber.ms.dataAccess.AbstractSpectraAccess;
import rappsilber.ms.dataAccess.BufferedSpectraAccess;
import rappsilber.ms.dataAccess.SpectraAccess;
import rappsilber.ms.dataAccess.StackedSpectraAccess;
import rappsilber.ms.dataAccess.output.AbstractStackedResultWriter;
import rappsilber.ms.dataAccess.output.BufferedResultWriter;
import rappsilber.ms.dataAccess.output.PreFilterResultWriter;
import rappsilber.ms.dataAccess.output.ResultWriter;
import rappsilber.ms.lookup.fragments.FragmentLookup;
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
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ms.sequence.digest.AAConstrained;
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
import rappsilber.utils.StringUtils;
import rappsilber.utils.Util;
import rappsilber.utils.XiVersion;
//import rappsilber.utils.ScoredLinkedList2;


public class SimpleXiProcess implements XiProcess {// implements ScoreSpectraMatch{

    protected AbstractSpectraAccess m_msmInput;
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
    private int m_useCPUs = Runtime.getRuntime().availableProcessors() - 1;
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


    
    /**
     * filters, that should be applied after the matching but before the scoring
     */
    private MatchFilter  m_matchfilter = new BatchFilter(new MatchFilter[]{new CleanUpMatchedPeaksFilter(), new DefinePrimaryFragmentMatches()});


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
     * @return the m_useCPUs
     */
    @Override
    public int getCPUs() {
        return m_useCPUs;
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
    }

    protected void matchToBaseLookup(Peptide[] topPeps, HashMap<String, HashSet<String>> topMatchHash) {
        String[] topPepsBase = new String[topPeps.length];
        for (int p = 0 ; p<topPeps.length;p++) {
            topPepsBase[p] = topPeps[p].toStringBaseSequence();
        }
        
        for (int p1 = 0 ; p1<topPeps.length;p1++) {
            HashSet<String> p1HashSet=new HashSet<String>();
            for (int p2 = 0 ; p2<topPeps.length;p2++) {
                if (p1 == p2)
                    break;
                p1HashSet.add(topPepsBase[p2]);
            }
            topMatchHash.put(topPepsBase[p1],p1HashSet);
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


    protected class SearchRunner implements Runnable {

        SpectraAccess m_input;
        ResultWriter m_output;

        public SearchRunner(SpectraAccess input, ResultWriter output) {
            m_input = input;
            m_output = output;
        }

        public void run() {
            process(m_input, m_output);
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
        Object cpu = m_config.retrieveObject("USECPUS");
        if (cpu != null) {
            m_useCPUs = Integer.valueOf(cpu.toString());
        }
        if (m_useCPUs < 0) {
            m_useCPUs = Math.max(Runtime.getRuntime().availableProcessors() + m_useCPUs,1);
        }
        m_AUTODECOY = m_config.retrieveObject("AUTODECOY", true);



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

        setOutputTopOnly(getConfig().retrieveObject("TOPMATCHESONLY", OutputTopOnly()));


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
        if (m_FragmentTolerance.getUnit().toLowerCase().contentEquals("da") && m_FragmentTolerance.getValue() > 0.08)
            m_config.setLowResolution();
        if (m_config.retrieveObject("LOWRESOLUTION", false)) {
            m_config.setLowResolution();
        }
        m_min_pep_length = m_config.retrieveObject("MINIMUM_PEPTIDE_LENGTH", m_min_pep_length);
        if (readSequences()) {
            return true;
        }
        applyLabel();
        fixedModifications();
        digest();
        variableModifications();
        peptideTreeFinalizations();
        Logger.getLogger(this.getClass().getName()).log(Level.INFO,"PeptideTree size: "+m_peptides.size());
//        for (Peptide p : m_peptides) {
//            System.out.println(p.toString());
//        }
        return false;
    }

    protected void applyLabel() {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Apply Label");
        setStatus("prepare: Apply Label");
        m_sequences.applyLabel(getConfig());
    }

    protected void digest() {
        m_maxPeptideMass = m_msmInput.getMaxPrecursorMass();
        m_maxPeptideMass = m_config.retrieveObject("MAXPEPTIDEMASS", m_maxPeptideMass);
        
        boolean forceSameDecoys = m_config.retrieveObject("FORCESAMEDECOYS", false);
        
        //m_maxPeptideMass = Math.min(m_maxPeptideMass, m_msmInput.getMaxPrecursorMass());
        
        //Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Build Peptide Tree");
        //        m_peptides = new PeptideTree(m_FragmentTolerance);
        m_peptides = new PeptideTree(m_PrecoursorTolerance);
        m_peptidesLinear = new PeptideTree(m_PrecoursorTolerance);
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
            ((PeptideTree)m_peptides).forceAddDiscarded();
            ((PeptideTree)m_peptidesLinear).forceAddDiscarded();
        } else {
            ArrayList<Peptide> discardedPeptides = ((PeptideTree)m_peptides).addDiscaredPermut(m_config);
            if (discardedPeptides.size() >0) {
                Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Some decoy peptides where not considered in the database");
                for (Peptide p : discardedPeptides) {
                    Logger.getLogger(this.getClass().getName()).log(Level.FINE, p.toString());
                }
            }
            discardedPeptides = ((PeptideTree)m_peptidesLinear).addDiscaredPermut(m_config);
            if (discardedPeptides.size() >0) {
                Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Some linear only decoy  peptides where not considered in the database");
                for (Peptide p : discardedPeptides) {
                    Logger.getLogger(this.getClass().getName()).log(Level.FINE, p.toString());
                }
            }
        }
        
        //        System.err.println("Peptides now stored ion the tree : " + m_peptides.size());
        //        try {
        //            ((PeptideTree) m_peptides).dump("/tmp/PeptideTreeDumpBefore.csv");
        //        } catch (IOException ex) {
        //            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        //        }
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
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Build Fragment Tree (should use " + m_useCPUs + " threads)" );
//        m_Fragments = new rappsilber.ms.lookup.fragments.FragmentMapDB(m_peptides, m_sequences, m_useCPUs, getConfig());
        m_Fragments = new rappsilber.ms.lookup.fragments.FragmentTreeSlimedMTvArrayOnly(m_peptides, m_sequences, m_useCPUs, getConfig());
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
                            m_sequences.includeShuffled(((AASpecificity) dig).getAminoAcidSpecificity());

                        } else {

                            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Including random sequences");
                            m_sequences.includeShuffled();
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
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "FragmentLibrary Coverage");
        getConfig().getScores().add(new FragmentLibraryScore(m_Fragments, m_sequences.getCountPeptides()));
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Spectar Coverage");
        getConfig().getScores().add(new SpectraCoverage());
        getConfig().getScores().add(new FragmentChargeState());
        getConfig().getScores().add(new SpectraCoverageConservative(minConservativeLosses));
        getConfig().getScores().add(new CombinedScores());
        getConfig().getScores().add(new rappsilber.ms.score.Error(getConfig()));
        getConfig().getScores().add(new BS3ReporterIonScore());
//        getConfig().getScores().add(new Normalizer());
        //getConfig().getScores().add(new LinkSiteDelta());
        getConfig().getScores().add(new NormalizerML());
        // add dummy score for feeding in the delta score
        getConfig().getScores().add(m_deltaScore);
    }

    protected void variableModifications() {
        rappsilber.utils.Util.MaxModifiedPeptidesPerPeptide = m_config.retrieveObject("MAX_MODIFIED_PEPTIDES_PER_PEPTIDE", rappsilber.utils.Util.MaxModifiedPeptidesPerPeptide);
        rappsilber.utils.Util.MaxModificationPerPeptide = m_config.retrieveObject("MAX_MODIFICATION_PER_PEPTIDE", rappsilber.utils.Util.MaxModificationPerPeptide);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Apply Variable Modifications");
        // apply variable modification
        m_config.getStatusInterface().setStatus("Applying variable modification to non-cross-linkable peptides");
        m_peptidesLinear.applyVariableModificationsLinear(m_config,m_peptides);
        m_config.getStatusInterface().setStatus("Applying variable modification to cross-linkable peptides");
        m_peptides.applyVariableModifications(m_config, m_peptidesLinear);
        //        m_sequences.applyVariableModifications(getConfig(),m_peptides, m_Crosslinker, digest);
        //        m_sequences.applyVariableModifications(getConfig(),m_peptidesLinear, m_Crosslinker, digest);

    }



    @Override
    public void startSearch() {
        startSearch(m_useCPUs);
    }


    @Override
    public void addFilter(StackedSpectraAccess f) {
        m_filters.add(f);
    }

    public void startSearch(int numberOfThreads) {
        SpectraAccess sa = m_msmInput;


        Object bufferIn = getConfig().retrieveObject("BUFFERINPUT");
        if (bufferIn != null && (Integer.valueOf((String) bufferIn) > 0)) {
            sa = new BufferedSpectraAccess(m_msmInput, Integer.valueOf((String) bufferIn));
        }

        for (StackedSpectraAccess f : m_filters) {
            f.setReader(sa);
            sa = f;
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
        m_processedInput = sa;
        setSearchThreads(new Thread[numberOfThreads]);
//        if (numberOfThreads == 1) {
//            getSearchThreads()[0] = new Thread(new SearchRunner(sa, m_output));
//            getSearchThreads()[0].setName("Search");
//            getSearchThreads()[0].run();
//        } else {
            for (int i = 0; i < numberOfThreads; i++) {
                getSearchThreads()[i] = new Thread(new SearchRunner(sa, m_output));
                getSearchThreads()[i].setName("Search_" + i);
                getSearchThreads()[i].start();
            }
//        }

    }


    @Override
    public void waitEnd() {
        Thread.currentThread().setName("WaitForEnd_" +  Thread.currentThread().getId());
        boolean running = true;
        int gc = 0;
        long oldProc = -100;
        long noChange = 0;
        long testDeadLock = 20;
        int noChangeDetected = 0;
        Calendar changeDate = Calendar.getInstance();
        
        while (running) {
            for (int i = 0; i < getSearchThreads().length; i++) {
                if (getSearchThreads()[i].isAlive()) {
                    try {
                        getSearchThreads()[i].join(500);


                    } catch (InterruptedException ex) {
                        Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Interrupt while wating for thread to finish", ex);
                    }
                }
            }
            long proc = getProcessedSpectra();
            if (oldProc != proc) {
                if (m_msmInput.getSpectraCount() >0) {
                    int procPerc = (int)(getProcessedSpectra()*100/m_msmInput.getSpectraCount());
                    m_config.getStatusInterface().setStatus(procPerc +"% processed (" + proc + ") " + m_msmInput.countReadSpectra() + " read of " + m_msmInput.getSpectraCount());
                } else {
                    m_config.getStatusInterface().setStatus(proc + " spectra processed " + m_msmInput.countReadSpectra() + " read of " + m_msmInput.getSpectraCount());
                }
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
                        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "\n"
                                + "================================\n"
                                + "== long time without activity ==\n"
                                + "================================\n"
                                + "\nno change for at least : " + delay + " seconds (" + (delay/60) + " minutes)\n");
                        if (m_auto_stacktrace) {

                            logStackTraces(Level.INFO);
                            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "\n"
                                    + "================================\n"
                                    + "== stacktraces finished ==\n"
                                    + "================================");
                        }
                    }
                }
                
            }
//            if (Spectra.SPECTRACOUNT > 2000)
//            System.gc();
            running = false;
            for (int i = 0; i < getSearchThreads().length; i++) {
                if (getSearchThreads()[i].isAlive()) {
                    running = true;
                    break;
                }
            }
            
            
        }

        long proc = getProcessedSpectra();
        if (m_msmInput.getSpectraCount() >0) {
            int procPerc = (int)(getProcessedSpectra()*100/m_msmInput.getSpectraCount());
            m_config.getStatusInterface().setStatus(procPerc +"% processed (" + proc + ") " + m_msmInput.countReadSpectra() + " read of " + m_msmInput.getSpectraCount());
        } else {
            m_config.getStatusInterface().setStatus(proc + " spectra processed " + m_msmInput.countReadSpectra() + " read of " + m_msmInput.getSpectraCount());
        }
        

        if (m_msmInput.hasNext()) {
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
        
        if (m_msmInput.hasNext()) {
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
        m_config.getStatusInterface().setStatus("Finished");
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Open Threads:");
        System.err.flush();
        System.out.flush();
        logStackTraces(Level.FINE);
        
        if (AbstractScoreSpectraMatch.DO_STATS) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, ScoreStatistic());
        }
        
        if (m_debugFrame != null) {
            m_debugFrame.setVisible(false);
            m_debugFrame.dispose();
        }
        
        if (countActiveThreads()>1){
            int delay = 10000;
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING,"There seem to be some open threads that have not finished yet. Will kill them after {0} seconds.", delay/1000  );
            new Timer("kill tasks", true).schedule(new TimerTask() {
                @Override
                public void run() {
                    if (countActiveThreads()>1) {
                        Logger.getLogger(this.getClass().getName()).log(Level.WARNING,"Forcefully closing the search"  );
                        logStackTraces(Level.WARNING);
                        killOtherActiveThreads();
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
            if (t != null) {
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
        Thread[] active = new Thread[tg.activeCount()*100];
        tg.enumerate(active, true);
        int c =0;
        for (Thread t : active) {
            if (t != null) {
                if (t != Thread.currentThread() && !t.isDaemon()) {
                    Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Try to daemonise {0}", t.getName());
                    try {
                        t.setDaemon(true);
                    } catch (Exception ex) {
                        Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "could not daemonise {0}, killing it", t.getName());
                        t.stop();
                    }
                }
            }
        }
        return c;
    }

    
    protected void logStackTraces(Level level) {
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        Thread[] active = new Thread[tg.activeCount()*100];
        tg.enumerate(active, true);
        StringBuilder sb = new StringBuilder();
        for (Thread t : active) {
            if (t != null) {
                try {
                    sb.append("\n--------------------------\n");
                    sb.append("--- Thread stack-trace ---\n");
                    sb.append("--------------------------\n");
                    sb.append("--- " + t.getId() + " : " + t.getName()+"\n");
                    sb.append(MyArrayUtils.toString(t.getStackTrace(), "\n"));
                    sb.append("\n");

                } catch (SecurityException se) {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, m_status);
                    System.err.println("could not get a stacktrace");
                }
            }
        }
        Logger.getLogger(this.getClass().getName()).log(level, sb.toString());
    }

    @Override
    public void stop() {
        m_doStop = true;
    }

    public void process(SpectraAccess input, ResultWriter output) {    
        try {

            // m_sequences.a


            long allfragments = m_Fragments.getFragmentCount();

            boolean evaluateSingles = getConfig().retrieveObject("EVALUATELINEARS", false);

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


//                SortedLinkedList<MatchedXlinkedPeptide> scanMatches = new SortedLinkedList<MatchedXlinkedPeptide>(new Comparator<MatchedXlinkedPeptide>() {
//
//                    public int compare(MatchedXlinkedPeptide o1, MatchedXlinkedPeptide o2) {
//                        return Double.compare(o2.getScore(CombinedScores.all), o1.getScore(CombinedScores.all));
//                    }
//                });

                ArrayList<MatchedXlinkedPeptide> scanMatches = new ArrayList<MatchedXlinkedPeptide>();



                for (Spectra spectra : spectraAllchargeStatess.getChargeStateSpectra()) {

                    Spectra mgc = spectra.getMgcSpectra(getConfig().getNumberMgcPeaks());
                    spectra.getIsotopeClusters().clear();

    //                Spectra mgx = spectra.getMgxSpectra();

                    getConfig().getIsotopAnnotation().anotate(spectra);

                    double precoursorMass = spectra.getPrecurserMass();

    //                if (spectra.getScanNumber() == 1966 &&
    //                        spectra.getRun().contentEquals("100220_04_Orbi1_JB_IN_180_XlinkedPYKaf01.raw")) {
    //                    System.err.println("found 1966");
    //                    System.err.println("look further");
    //                    System.out.println(mgc);
    //                }


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
    //                ScoredTreeSet<Peptide, Double> scoredPeptides = mgcMatchScores.getScoredList();

    //                System.err.print(spectra.getRun() + "," + spectra.getScanNumber());
    //                for (Peptide p: scoreSortedAlphaPeptides) {
    //                    System.err.print("," + p.toString() + "," + mgcMatchScores.Score(p, -1));
    //                }
    //                System.err.println();

    //                if (spectra.getScanNumber() == 1966 &&
    //                        spectra.getRun().contentEquals("100220_04_Orbi1_JB_IN_180_XlinkedPYKaf01.raw")) {
    //                    System.err.println(mgcMatchScores.size() + " -> " + scoreSortedAlphaPeptides.length);
    //                }


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
    //                    System.err.print("," + alphaFirst.toString() + "," + mgcMatchScores.Score(alphaFirst, -1));

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

    //                        if (cls.size() == 0 && ! matched)
    //                            lastIndex++;


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
                                for (Peptide beta : betaPeptides) {
                                    //alpha
                                    //beta = beta.clone();
                                    String betaID = beta.getSequence().getFastaHeader() + "_" + beta.toString();

                                    // don't search alpha and beta reveresed
                                    if (foundPeptideSequences.contains(betaID + "_" + alphaId)) {
                                        continue betaSearch;
                                    }

                                    // remember we foudn these
                                    foundPeptideSequences.add(alphaId + "_" + betaID);

                                    evaluateMatch(spectra.cloneComplete(), alphaFirst, beta, cl, betaCount, scanMatches, false);

                                    //match.free();
                                    //beta.free();
                                    //s.free();
                                }
                                //alpha.free();

                            }


                        }
    //                    if (i == lastIndex && i<lastPossibleIndex) {
    //                        if (mgcMatchScores.Score(alphaFirst, 0) == mgcMatchScores.Score(scoreSortedAlphaPeptides[i+1], 0) )
    //                            lastIndex++;
    //                    }

                    }
    //                System.err.println();
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

//                    MatchedXlinkedPeptide top = scanMatches.get(0);
//                    //double topScore = top.getScore(CombinedScores.all);
//                    double topScore = top.getScore(MatchScore);
//                    if (countMatches == 1) {
//                        m_deltaScore.setScore(top, "delta", topScore);
//                        m_deltaScore.setScore(top, "combinedDelta", topScore);
//
////                        System.out.println(top.getSpectrum().getRun() +"," + top.getSpectrum().getScanNumber() +"," + top.getPeptide1() + "," + top.getPeptide2() );
//
//
//                        output.writeResult(top);
//                    } else {
//                        // collect all top scoring matches
//                        ArrayList<MatchedXlinkedPeptide> tops = new ArrayList<MatchedXlinkedPeptide>(countMatches);
//                        tops.add(top);
//                        Iterator<MatchedXlinkedPeptide> mi = scanMatches.iterator();
//                        MatchedXlinkedPeptide m = mi.next();
//                        double delta = topScore;
//                        while (mi.hasNext()) {
//                            m = mi.next();
//                            if (m.getScore(MatchScore) == topScore) {
//                                tops.add(m);
//                            } else {
//                                delta = topScore - m.getScore(MatchScore);
//                                break;
//                            }
//                        }
//                        if (delta < 0)
//                            System.err.println("Delta < 0");
//                        for (MatchedXlinkedPeptide topM : tops) {
//                            //topM.setScore("delta", delta);
//                            m_deltaScore.setScore(topM, "delta", delta);
//                            m_deltaScore.setScore(topM, "combinedDelta", (topM.getScore(MatchScore) + delta)/2);
////                            System.out.println(topM.getSpectrum().getRun() +"," + topM.getSpectrum().getScanNumber() +"," + topM.getPeptide1() + "," + topM.getPeptide2() );
//                            output.writeResult(topM);
//                        }
//                        m_deltaScore.setScore(m,"delta", 0);
//                        m_deltaScore.setScore(m, "combinedDelta", (m.getScore(MatchScore))/2);
//
////                        if (m.getSpectra() != null)
////                            output.writeResult(m);
////                        if (m.getSpectrum() != null) {
////                            m.getSpectrum().free();
////                            m.free();
////                        }
//
//                        while (mi.hasNext()) {
//                            MatchedXlinkedPeptide mfree = mi.next();
////                            mfree.getSpectrum().free();
////                            mfree.free();
////                            m = mi.next();
////                            m_deltaScore.setScore(m,"delta", 0);
////                            m_deltaScore.setScore(m, "combinedDelta", (m.getScore(CombinedScores.all))/2);
////                            output.writeResult(m);
//                        }
//                    }
//
                }
                scanMatches.clear();


                //      find matching candidat peptides
                //      for each match
                //          score match
                //

                if (processed >= 10) {
                    increaseProcessedScans(processed);
                    processed=0;
                }

            }
            
            //System.err.println("Spectras processed here: " + countSpectra);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error while processing spectra", e);
            System.err.println(e);
            e.printStackTrace(System.err);
            setStatus("Error while processing spectra" + e);
            System.exit(0);
        }

    }

    public String ScoreStatistic() {
        StringBuilder sb = new StringBuilder("scorer ,score,min,max,average,stdDev,median,mad\n");
        for (ScoreSpectraMatch ssm : getConfig().getScores()) {
            String scorer = ssm.name();
            for (String score : ssm.scoreNames()) {
                double min = ssm.getMin(score);
                double max = ssm.getMax(score);
                double average = ssm.getAverage(score);
                double stdDev = ssm.getStdDev(score);
                double median = ssm.getMedian(score);
                double mad = ssm.getMAD(score);
                sb.append(scorer +"," + score + ","+ "," + min +"," + max +"," + average + "," + stdDev + "," + median +"," + mad + "\n");

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
        for (double score : match.getScores().values())
            if(Double.isNaN(score)) {
                //System.err.println("found it " + this.getClass().getName());
                for (ScoreSpectraMatch ssm : getConfig().getScores()) {
                    ssm.score(match);

                }
            }

//        if (match.getScore(FragmentCoverage.mAll) > 1) {
            scanMatches.add(match);
            return match;
//        } else {
//            match.free();
//            return null;
//        }
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
                    matchToBaseLookup(topPeps, topMatchHash);
                }
            }

            double noModSecond = -Double.MAX_VALUE;
            // now find the second best distinct match (meaning different peptide sequences)
            for (int m = 0; m< matches.length ; m++) {
                if (matches[m].getScore(MatchScore) < topScore) {
                    Peptide[] peps = matches[m].getPeptides();
                    String fpep = peps[0].toStringBaseSequence();
                    HashSet<String> baseLookup =  topMatchHash.get(fpep);
                    if (baseLookup == null) { // first peptide i non of the top matches
                        double matchScore  = matches[m].getScore(MatchScore);
                        if (noModSecond<matchScore) {
                            noModSecond = matchScore;
                        }
                    } else {
                        // go through the secondary peptides
                        if (peps.length == 1)
                            continue;
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


//
//    public double score(MatchedXlinkedPeptide match) {
//        return 0;
//    }
//
//    public String name() {
//        return "combinedDelta";
//    }
//
//    public String[] scoreNames() {
//        return new String[]{"delta", "combinedDelta"};
//    }

//    public double getAverage(String name) {
//        return 0;
//    }
//
//
//    public double getStdDev(String name) {
//        return 0;
//    }

//    public double getMin(String name) {
//        return 0;
//    }
//
//    public double getMax(String name) {
//        return 0;
//    }
//
//    public double getOrder() {
//        return 1000000;
//    }

//    public int compareTo(ScoreSpectraMatch o) {
//        return Double.compare(getOrder(), o.getOrder());
//    }


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
    
    public synchronized long increaseProcessedScans(int count) {
        m_processedSpectra+=count;
        return m_processedSpectra;
    }
    
    @Override
    public long getProcessedSpectra() {
        return m_processedSpectra;
    }
    
    
}
