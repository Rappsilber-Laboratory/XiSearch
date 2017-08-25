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
package rappsilber.ms.lookup.fragments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.RunConfig;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.lookup.peptides.PeptideLookup;
import rappsilber.ms.sequence.Iterators.PeptideIterator;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.Sequence;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.utils.ArithmeticScoredOccurence;
import rappsilber.utils.Util;


public class FragmentTreeSlimedIntArray implements FragmentLookup, FragmentCollection{
    private static final long serialVersionUID = -119704332506558616L;

    private int     m_FragmentCount = 0;
    private ToleranceUnit m_Tolerance;
    private SequenceList  m_list;
    private Integer m_RunningThreads = 0;
    private int     m_nextTree = 0;
    private long    m_maxPeptides = Long.MAX_VALUE;

    public int divide = 20000;
    public double MAX_FRAGMENT_MASS = Integer.MAX_VALUE/(double)divide;
    
    private double  m_MinimumMass = 0;
    private double  m_MaximumPeptideMass = Double.MAX_VALUE;

    private int     m_PeptidesPerThread = 10;
    private AtomicInteger m_processedSequences = new AtomicInteger(0);

    private PeptideIterator m_peptides = null;
    private int[]   peptides_perTree;
    
    private int     m_maxPeakCandidates;
    

    /**
     * @return the m_list
     */
    public SequenceList getSequeneList() {
        return m_list;
    }




    private class InnerTreeMap extends TreeMap<Integer, int[]> {
        private static final long serialVersionUID = -4038223287995095272L;
    }
    

    InnerTreeMap[] m_threadTrees;
    int[]          m_perTreeCount;
    RunConfig      m_config = null;
    int            m_total_Peptides;




    private class addingPeptideTreeByMass implements Runnable{
        PeptideLookup m_peptides;
//        Iterator<Peptide> m_it;
        InnerTreeMap m_targetTree;
        int tree;
        long max_peps = Long.MAX_VALUE;
        private double minMass = 0;
        private double maxMass = Double.MAX_VALUE;
        

        public addingPeptideTreeByMass(PeptideLookup peptides, int tree) {
            m_peptides = peptides;
            m_targetTree = m_threadTrees[tree];
            this.tree = tree;
        }

        public addingPeptideTreeByMass(Iterator<Peptide> it, int tree) {
            m_targetTree = m_threadTrees[tree];
            this.tree = tree;
            this.max_peps = max_peps;
        }
        
        public void setMassRange(double min, double max) {
            setMinMass(min);
            setMaxMass(max);
        }
        
        public void run() {
            long pep_count=0;
            long lastPepCount = 0;
            int countRounds=0;
            int m_localFragmentCount = 0;
            synchronized(m_RunningThreads) {
                m_RunningThreads++;
            }
            try {
                //Peptide pep;
                ArrayList<Peptide> peps = m_peptides.getForExactMassRange(getMinMass(), getMaxMass());
                int count = 0;
                for (Peptide pep : peps ) {
                    if (++pep_count > max_peps)
                        break;

                    try {
                        if (pep.getMass() < m_MaximumPeptideMass && pep.getMass() > m_MinimumMass) {
                            ArrayList<Fragment> frags;
                            if (m_config == null) {
                                frags = pep.getPrimaryFragments();

                            } else {
                                frags = pep.getPrimaryFragments(m_config);


                            }
                            for (int i = 0; i < frags.size(); i++) {
                                Fragment f = frags.get(i);
                                addFragment(pep, f.getMass(), m_targetTree, tree);
                            }
                        }
                    } catch (Exception e) {
                        throw new Error(e);
                    }
                    
                    if (++count == 5000) {
                        int total = m_processedSequences.addAndGet(count);
                        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Fragmentation ("+tree + "): " + total + " Peptides"+ ((int)(total/(double)m_total_Peptides * 100)) + "%");
                        m_config.getStatusInterface().setStatus("Fragmentation: " + ((int)(total/(double)m_total_Peptides * 100)) + "% of  Peptides" );
                        count = 0;
                        
                    }

                }            
                
                double total = m_processedSequences.addAndGet(count);
                
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Fragmentation: " + total + " Peptides");
                m_config.getStatusInterface().setStatus("Fragmentation: " + ((int)(total/(double)m_total_Peptides * 100)) + "% of  Peptides" );

                
//                synchronized(m_processedSequences) {
//                    m_processedSequences.value+=(pep_count-lastPepCount);
//                }
                
            } catch (Exception error) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "error while building fragment tree",error);
                System.err.println(error);
                error.printStackTrace(System.err);
                System.exit(0);

            }
            synchronized(m_RunningThreads) {
                m_RunningThreads--;
            }

        }

        /**
         * @return the minMass
         */
        public double getMinMass() {
            return minMass;
        }

        /**
         * @param minMass the minMass to set
         */
        public void setMinMass(double minMass) {
            this.minMass = minMass;
        }

        /**
         * @return the maxMass
         */
        public double getMaxMass() {
            return maxMass;
        }

        /**
         * @param maxMass the maxMass to set
         */
        public void setMaxMass(double maxMass) {
            this.maxMass = maxMass;
        }

    }



    public FragmentTreeSlimedIntArray(SequenceList list, double tolerance, int threads){
        this(list, new ToleranceUnit(tolerance, "da"), threads, Double.MAX_VALUE, null);
    }

    public FragmentTreeSlimedIntArray(PeptideLookup peptideList, SequenceList list, double tolerance, int threads){
        this(peptideList, list, new ToleranceUnit(tolerance, "da"), threads, Double.MAX_VALUE, null);
    }

    public FragmentTreeSlimedIntArray(SequenceList list, double tolerance, int threads, RunConfig config){
        this(list, new ToleranceUnit(tolerance, "da"), threads, Double.MAX_VALUE, config);
    }

    public FragmentTreeSlimedIntArray(PeptideLookup peptideList, SequenceList list, double tolerance, int threads, RunConfig config){
        this(peptideList, list, new ToleranceUnit(tolerance, "da"), threads, Double.MAX_VALUE, config);
    }

    public FragmentTreeSlimedIntArray(SequenceList list, ToleranceUnit Tolerance, int threads){
        this(list, Tolerance, threads, Double.MAX_VALUE,null);
    }

    public FragmentTreeSlimedIntArray(PeptideLookup peptideList, SequenceList list, ToleranceUnit Tolerance, int threads){
        this(peptideList, list, Tolerance, threads, Double.MAX_VALUE,null);
    }


    public FragmentTreeSlimedIntArray(SequenceList list, ToleranceUnit Tolerance, int threads, RunConfig config){
        this(list, Tolerance, threads, Double.MAX_VALUE,config);
    }

    public FragmentTreeSlimedIntArray(PeptideLookup peptideList, SequenceList list, ToleranceUnit Tolerance, int threads, RunConfig config){
        this(peptideList, list, Tolerance, threads, Double.MAX_VALUE,config);
    }

    public FragmentTreeSlimedIntArray(SequenceList list, int threads, RunConfig config){
        this(list, config.getFragmentTolerance(), threads, Double.MAX_VALUE,config);
    }

    public FragmentTreeSlimedIntArray(PeptideLookup peptideList, SequenceList list, int threads, RunConfig config){
        this(peptideList, list, config.getFragmentTolerance(), threads, Double.MAX_VALUE,config);
    }

    public FragmentTreeSlimedIntArray(PeptideLookup peptideList, long maxPeptides, Peptide lastPeptide, SequenceList list, int threads, RunConfig config){
        this(peptideList, maxPeptides, lastPeptide, list, config.getFragmentTolerance(), threads, Double.MAX_VALUE,config);
    }

//    public FragmentTreeSlimedMTv2(SequenceList sequences, PeptideLookup peptides, int threads, RunConfig config){
//        this(sequences, peptides, config.getFragmentTolerance(), threads, Double.MAX_VALUE,config);
//    }

    public FragmentTreeSlimedIntArray(SequenceList list, ToleranceUnit Tolerance, int threads, double MaximumPeptideMass, RunConfig config){
        m_MaximumPeptideMass = MaximumPeptideMass;
        m_list = list;
        m_Tolerance = Tolerance;
        m_FragmentCount  = 0;
        m_threadTrees = new InnerTreeMap[threads];
        m_perTreeCount = new int[threads];
        m_config = config;
        m_maxPeakCandidates = m_config.getMaximumPeptideCandidatesPerPeak();

        insertFragements(list.iterator());
    }

    public FragmentTreeSlimedIntArray(PeptideLookup PeptideList, SequenceList list, ToleranceUnit Tolerance, int threads, double MaximumPeptideMass, RunConfig config){
//        threads = 3;
        m_MaximumPeptideMass = MaximumPeptideMass;
        m_list = list;
        m_Tolerance = Tolerance;
        m_FragmentCount  = 0;
        m_threadTrees = new InnerTreeMap[threads];
        m_perTreeCount = new int[threads];
        m_config = config;
        m_total_Peptides = PeptideList.size();
        m_peptides = PeptideList.iterator();
        m_maxPeakCandidates = m_config.getMaximumPeptideCandidatesPerPeak();

        insertFragementsFromPeptides(PeptideList);
    }

    public FragmentTreeSlimedIntArray(PeptideLookup PeptideList, long maxPeptides, Peptide lastPeptide, SequenceList list, ToleranceUnit Tolerance, int threads, double MaximumPeptideMass, RunConfig config){
//        threads = 3;
        m_MaximumPeptideMass = MaximumPeptideMass;
        m_list = list;
        m_Tolerance = Tolerance;
        m_FragmentCount  = 0;
        m_threadTrees = new InnerTreeMap[threads];
        m_perTreeCount = new int[threads];
        m_config = config;
        m_total_Peptides = PeptideList.size();
        m_peptides = PeptideList.iteratorAfter(lastPeptide);
        m_maxPeptides = maxPeptides;
        m_maxPeakCandidates = m_config.getMaximumPeptideCandidatesPerPeak();

        insertFragementsFromPeptides(PeptideList);
    }

    public void insertFragements(Iterator<Sequence> sequences) {
        throw new UnsupportedOperationException("Sorry this FragmentLookup only works on PeptideTree");
    }


    public void insertFragementsFromPeptides(PeptideLookup peptides) {
//        int FragmentCountLast = 0;
        int threads = m_threadTrees.length;
        long rest = m_maxPeptides % threads; // how many would we miss
        addingPeptideTreeByMass[] addingRuannables = new addingPeptideTreeByMass[threads];
        Thread[] addingThreads = new Thread[threads];
        //PeptideIterator pIt = PeptideList.peptides();
        double massStep = (peptides.getMaximumMass()+1)/threads/10;
        double endmass = peptides.getMaximumMass();
        double startmass = 0;

        
        for (int i=0; i<threads; i++) {
            m_threadTrees[i] = new InnerTreeMap();
            addingRuannables[i] = new addingPeptideTreeByMass(peptides, i);
            addingThreads[i] = new Thread(addingRuannables[i]);
        }


        while (startmass < endmass) {
            for (int i=0; i<threads/2+1; i++) {
                if (startmass < endmass && !addingThreads[i].isAlive()) {
                    double stepend = startmass + massStep;
                    addingRuannables[i].setMassRange(startmass, stepend);
                    String threadname = "fragementing Peptides " + startmass + " to " + stepend;
                    startmass = stepend;
                    
                    addingThreads[i] = new Thread(addingRuannables[i]);
                    addingThreads[i].setName(threadname);
                    addingThreads[i].start();
                    
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(FragmentTreeSlimedIntArray.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
//        m_threadTrees[0] = new InnerTreeMap();
//        new addingPeptideTree(peptides, 0).run();

        Util.joinAllThread(addingThreads);

        //        while (true) {
//            boolean finished = true;
//            for (int i=0; i<threads; i++) {
//                try {
//                    addingThreads[i].join(500);
//                } catch (InterruptedException ex) {}
//                if (addingThreads[i].isAlive())
//                        finished = false;
//            }
//            if (finished) break;
//
//        }

        this.shrink();
    }


//    @Override
//    public boolean nextRound() {
//        for (InnerTreeMap t: m_threadTrees)
//            t.clear();
//        if (m_peptides.hasNext()) {
//            insertFragementsFromPeptides(PeptideList);
//            return true;
//        } else
//            return false;
//    }


    public void clear() {
        for (InnerTreeMap t: m_threadTrees)
            t.clear();
        m_threadTrees = null;
    }


    public ArrayList<Peptide> getForMass(double mass) {
        int min = (int) (m_Tolerance.getMinRange(mass)*divide);
        int max = (int) (m_Tolerance.getMaxRange(mass)*divide);
        
        ArrayList<Peptide> ret = new ArrayList<Peptide>();
        Peptide[] allPeptides = m_list.getAllPeptideIDs();
        for (int t = 0; t<m_threadTrees.length;t++) {
//            if ((int)mass == 173)
//                        System.err.println("found it");
            Collection<int[]> entries;
            entries = m_threadTrees[t].subMap(min,max).values();
            Iterator<int[]> it = entries.iterator();
            while (it.hasNext()) {
                int[] ids = it.next();
                for (int i = 0; i < ids.length; i++) {
//                    if (allPeptides[ids.m_peptideIds[i]] == null)
//                        System.err.println("found it");
                    ret.add(allPeptides[ids[i]]);
                }
            }
        }
        return ret;
    }

    public ArrayList<Peptide> getForMass(double mass, double referenceMass) {
        ArrayList<Peptide> ret = new ArrayList<Peptide>();
        int min = (int) (m_Tolerance.getMinRange(mass, referenceMass)*divide);
        int max = (int) (m_Tolerance.getMaxRange(mass, referenceMass)*divide);
        for (int t = 0; t<m_threadTrees.length;t++) {
            Collection<int[]> entries =  m_threadTrees[t].subMap(min,max).values();
            Peptide[] allPeptides = m_list.getAllPeptideIDs();
            Iterator<int[]> it = entries.iterator();
            while (it.hasNext()) {
                int[] ids = it.next();
                for (int i = 0; i < ids.length; i++) {
                    ret.add(allPeptides[ids[i]]);
                }
            }
        }
        return ret;
    }

    @Override
    public ArrayList<Peptide> getForMass(double mass, double referenceMass, double maxPepMass) {
        ArrayList<Peptide> ret = new ArrayList<Peptide>();
        int min = (int) (m_Tolerance.getMinRange(mass, referenceMass)*divide);
        int max = (int) (m_Tolerance.getMaxRange(mass, referenceMass)*divide);
        for (int t = 0; t<m_threadTrees.length;t++) {
            Collection<int[]> entries =  m_threadTrees[t].subMap(min,max).values();
            Peptide[] allPeptides = m_list.getAllPeptideIDs();
            Iterator<int[]> it = entries.iterator();
            while (it.hasNext()) {
                int[] ids = it.next();
                for (int i = 0; i < ids.length; i++) {
                    Peptide p = allPeptides[ids[i]];
                    if (p.getMass()<maxPepMass)
                        ret.add(p);
                }
            }
        }
        return ret;
    }


    @Override
    public ArrayList<Peptide> getForMass(double mass, double referenceMass, double maxPepMass,int maxPeptides) {
        ArrayList<Peptide> ret = new ArrayList<Peptide>();
        int min = (int) (m_Tolerance.getMinRange(mass, referenceMass)*divide);
        int max = (int) (m_Tolerance.getMaxRange(mass, referenceMass)*divide);

        Collection[] allEntries = new Collection[m_threadTrees.length];
        int count =0;

        for (int t = 0; t<m_threadTrees.length;t++) {
            allEntries[t] =  m_threadTrees[t].subMap(min, max).values();
            count+=allEntries[t].size();
        }

        if (count<= maxPeptides) {
            for (int t = 0; t<allEntries.length;t++) {
                Collection<int[]> entries =allEntries[t];
                Peptide[] allPeptides = m_list.getAllPeptideIDs();
                Iterator<int[]> it = entries.iterator();
                while (it.hasNext()) {
                    int[] ids = it.next();
                    for (int i = 0; i < ids.length; i++) {
                        Peptide p = allPeptides[ids[i]];
                        if (p.getMass()<maxPepMass)
                            ret.add(p);
                    }
                }
            }
        }
        return ret;
    }
    
    public ArrayList<Peptide> getPeptidesExactFragmentMass(double mass) {
        return getPeptidesExactFragmentMass(mass*divide);
    }

    protected ArrayList<Peptide> getPeptidesExactFragmentMass(int mass) {
        ArrayList<Peptide> ret = new ArrayList<Peptide>();
        Peptide[] allPeptides = m_list.getAllPeptideIDs();
        for (int t = 0; t<m_threadTrees.length;t++) {
//            if ((int)mass == 173)
//                        System.err.println("found it");
            int[] ids =  m_threadTrees[t].get(mass);
            if (ids != null)
                for (int i = 0; i < ids.length; i++) {
    //                    if (allPeptides[ids.m_peptideIds[i]] == null)
    //                        System.err.println("found it");
                    ret.add(allPeptides[ids[i]]);
                }
        }
        return ret;
    }
    
    public int countPeptides(double mass) {
        int count = 0;
//        ArrayList<Peptide> ret = new ArrayList<Peptide>();
        for (int t = 0; t<m_threadTrees.length;t++) {
//            if ((int)mass == 173)
//                        System.err.println("found it");
            int min = (int) (m_Tolerance.getMinRange(mass)*divide);
            int max = (int) (m_Tolerance.getMaxRange(mass)*divide);

//            if (from > to)
//                    System.err.println("count peptides From > to");
            for (int[] e : m_threadTrees[t].subMap(min,max).values()) {
                count +=  e.length;
            }
        }
        return count;
    }

    public int countPeptides(double mass, double referenceMass) {
        int count = 0;
        int min = (int) (m_Tolerance.getMinRange(mass, referenceMass)*divide);
        int max = (int) (m_Tolerance.getMaxRange(mass, referenceMass)*divide);
//        ArrayList<Peptide> ret = new ArrayList<Peptide>();
        for (int t = 0; t<m_threadTrees.length;t++) {
//            if ((int)mass == 173)
//                        System.err.println("found it");
            count +=  m_threadTrees[t].subMap(min,max).size();
        }
        return count;
    }


    public Map<Peptide, Double> getPeptidesForMasses(double mass) {
        HashMap<Peptide, Double> ret = new HashMap<Peptide, Double>();
        int min = (int) (m_Tolerance.getMinRange(mass)*divide);
        int max = (int) (m_Tolerance.getMaxRange(mass)*divide);
        for (int t = 0; t<m_threadTrees.length;t++) {
            Collection<int[]> entries =  m_threadTrees[t].subMap(min, max).values();
            Peptide[] allPeptides = m_list.getAllPeptideIDs();
            Iterator<int[]> it = entries.iterator();
            while (it.hasNext()) {
                int[] ids = it.next();
                for (int i = 0; i < ids.length; i++)
                    ret.put(allPeptides[ids[i]], mass);
            }
        }
        return ret;
    }

    public void setTolerance(ToleranceUnit tolerance) {
        m_Tolerance = tolerance;
    }




    private void addFragment(Peptide pep, double mass, InnerTreeMap targetTree, int tree) {
        // fragments exceding maxvalue can not be represented and are therefore ignored
        if (mass > MAX_FRAGMENT_MASS) {
            return;
        }
        int[] src=null;
        int[] newEntry;
        src  = targetTree.get((int)(mass*divide));
        if (src != null) {
            newEntry = java.util.Arrays.copyOf(src, src.length + 1);
            newEntry[src.length] = pep.getPeptideIndex();
        } else {
            newEntry = new int[] {pep.getPeptideIndex()};
        }
        targetTree.put((int)(mass*divide), newEntry);
        m_FragmentCount ++;
        m_perTreeCount[tree] ++;
    }

    public void addFragment(Peptide pep, double mass) {
        synchronized (m_threadTrees) {
            addFragment(pep, mass, m_threadTrees[m_nextTree], m_nextTree);
        }
    }

    public int getFragmentCount() {
        int count = 0;
        for (int i = 0 ; i < m_threadTrees.length; i++)
            count += m_perTreeCount[i];
        return count;
    }

    public void printStatistic(PrintStream out) {

        int minSize = Integer.MAX_VALUE;
        int maxSize = Integer.MIN_VALUE;
        int count = 0;

        for (int i = 0; i < m_threadTrees.length; i++) {
            java.util.Set<Integer> s = m_threadTrees[i].keySet();
            Iterator<Integer> it =  s.iterator();
            while (it.hasNext()) {
                Integer k = it.next();
                int[] v = m_threadTrees[i].get(k);
                minSize = Math.min(minSize, v.length);
                maxSize = Math.max(maxSize, v.length);
                count += v.length;
            }
        }



        out.println("==============================");
        out.println("==        Statistic        ===");
        out.println("==============================");
        //out.println("==distinct masses: " + this.size());
        out.println("==min peptides per mass: " + minSize/(double)divide);
        out.println("==max peptides per mass: " + maxSize/(double)divide);
        //out.println("==avarage peptides per mass: " + count/(double)this.size());
        out.println("==============================");
    }

    public void dump(String path) throws IOException {
        dump(new FileWriter(path));
    }

    public void dump(Writer out) throws IOException {
        BufferedWriter bw = new BufferedWriter(out);
        Peptide[] allPeptides = m_list.getAllPeptideIDs();
        HashSet<Integer> masses = new HashSet<Integer>();
        for (int i = 0; i < m_threadTrees.length; i++) {
            java.util.Set<Integer> s = m_threadTrees[i].keySet();
            masses.addAll(s);
        }
        
        Integer[] ma = masses.toArray(new Integer[0]);
        
        java.util.Arrays.sort(ma);
        
        for (Integer m : ma) {
            ArrayList<Peptide> peps = this.getPeptidesExactFragmentMass(m);
            java.util.Collections.sort(peps, new Comparator<Peptide>() {

                @Override
                public int compare(Peptide o1, Peptide o2) {
                    int ret = Double.compare(o1.length(), o2.length());
                    if (ret == 0) 
                        ret = o1.toString().compareTo(o2.toString());
                    return ret;
                }
            });

            bw.append(Double.toString(m));
            for (Peptide p : peps)
                bw.append(", " + p.getSequence().getFastaHeader().substring(0, Math.min(40,p.getSequence().getFastaHeader().length())) + ":" + p.toString());

            bw.newLine();
        }




    }
    
    public void shrink() {
//        Thread[] waitThreads = new Thread[m_threadTrees.length];
        for (int i = 0; i < m_threadTrees.length; i++) {
            final InnerTreeMap it  = m_threadTrees[i];

//            Runnable runnable = new Runnable() {
//
//                public void run() {
//                    for (FragmentTreeSlimedElement e : it.values().toArray(new FragmentTreeSlimedElement[0])) {
//                        e.shrink();
//                    }
//                }
//            };
//            waitThreads[i] = new Thread(runnable);
//            waitThreads[i].start();
        }

//        boolean finished = false;
//        while (!finished ) {
//            finished =true;
//            for (int i = 0; i < m_threadTrees.length; i++) {
//                try {
//                    waitThreads[i].join();
//                } catch (InterruptedException ex) {
//                    finished = false;
//                }
//            }
//        }

    }

    @Override
    public Peptide lastFragmentedPeptide() {
        return m_peptides.current();
    }

    public PeptideIterator getPeptideIterator() {
        return m_peptides;
    }

        @Override
    public ArithmeticScoredOccurence<Peptide> getAlphaCandidates(Spectra s, ToleranceUnit precursorTolerance) {
        double maxPeptideMass=precursorTolerance.getMaxRange(s.getPrecurserMass());
        int maxcandidates = m_config.getMaximumPeptideCandidatesPerPeak();
        return this.getAlphaCandidates(s, maxPeptideMass);
    }    
    
    @Override
    public ArithmeticScoredOccurence<Peptide> getAlphaCandidates(Spectra s, double maxPeptideMass) {
        ArithmeticScoredOccurence<Peptide> peakMatchScores = new ArithmeticScoredOccurence<Peptide>();

        if (m_maxPeakCandidates == -1) {
            //   go through mgc spectra
            for (SpectraPeak sp : s) {
                //      for each peak
                //           count found peptides
                ArrayList<Peptide> matchedPeptides = this.getForMass(sp.getMZ(),sp.getMZ(),maxPeptideMass); // - Util.PROTON_MASS);
                double peakScore = (double) matchedPeptides.size() / getFragmentCount();
                for (Peptide p : matchedPeptides) {
                    peakMatchScores.multiply(p, peakScore);
                }
            }
        } else {
            //   go through mgc spectra
            for (SpectraPeak sp : s) {
                //      for each peak
                //           count found peptides
                ArrayList<Peptide> matchedPeptides = getForMass(sp.getMZ(),sp.getMZ(),maxPeptideMass,m_maxPeakCandidates);
                double peakScore = (double) matchedPeptides.size() / getFragmentCount();
                for (Peptide p : matchedPeptides) {
                    peakMatchScores.multiply(p, peakScore);
                }
            }
        }
        return peakMatchScores;
    }
    
    @Override
    public void writeOutTree(File out) throws IOException{
        PrintWriter o = new PrintWriter(out);
        for (InnerTreeMap itm : m_threadTrees) {
            for (Map.Entry<Integer,int[]>  e: itm.entrySet()) {
                for (int id : e.getValue()) {
                    o.println(e.getKey() + " , " + id);
                }
            }
        }
        o.close();
    }  
}
