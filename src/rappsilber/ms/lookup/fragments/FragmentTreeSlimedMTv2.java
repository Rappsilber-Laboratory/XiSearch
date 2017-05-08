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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
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
import rappsilber.ms.statistics.utils.UpdateableInteger;
import rappsilber.utils.Util;


public class FragmentTreeSlimedMTv2 implements FragmentLookup, FragmentCollection{
    private static final long serialVersionUID = -119704332506558616L;

    private int     m_FragmentCount = 0;
    private ToleranceUnit m_Tolerance;
    private SequenceList  m_list;
    private Integer m_RunningThreads = 0;
    private int     m_nextTree = 0;
    private long    m_maxPeptides = Long.MAX_VALUE;

    private double  m_MinimumMass = 0;
    private double  m_MaximumPeptideMass = Double.MAX_VALUE;

    private int     m_PeptidesPerThread = 10;
    private UpdateableInteger m_processedSequences = new UpdateableInteger(0);

    private PeptideIterator m_peptides = null;
    private int[]   peptides_perTree;
    

    /**
     * @return the m_list
     */
    public SequenceList getSequeneList() {
        return m_list;
    }




    private class InnerTreeMap extends TreeMap<Double, FragmentTreeSlimedElement> {
        private static final long serialVersionUID = -4038223287995095272L;
    }
    

    InnerTreeMap[] m_threadTrees;
    int[]          m_perTreeCount;
    RunConfig      m_config = null;
    int            m_total_Peptides;



    private class adding extends Thread{
        Iterator<Sequence> m_it;
        TreeMap<Double,FragmentTreeSlimedElement> m_targetTree;
        int tree;

        public adding(Iterator<Sequence> it, int tree) {
            m_it=it;
            m_targetTree = m_threadTrees[tree];
            this.tree = tree;
        }


        public void run() {
            int m_localFragmentCount = 0;
            synchronized(m_RunningThreads) {
                m_RunningThreads++;
            }
            try {
                //Peptide pep;
                do {
                    Sequence seq;
                    synchronized (m_it) {
                        if (m_it.hasNext()) {
                            seq = m_it.next();
                        } else
                            seq = null;
                    }
                    if (seq != null) {
                        for (Peptide pep : seq.getPeptides()) {

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
    //                                if (m_FragmentCount % 500000 == 0) {
    //                                    // System.err.println("--" + this.getId() + " -- current peptide " + pep.getPeptideIndex());
    //                                    // System.err.println("--" + this.getId() + " -- fragments registered " + m_FragmentCount);
    //
    //                                    if (m_FragmentCount % 5000000 == 0) {
    //                                        printStatistic(System.err);
    //
    //                                        // System.err.println("--" + this.getId() + " -- Free Memory  " + (Runtime.getRuntime().freeMemory() / 1024 / 1024) + " MB");
    //
    //                                    }
    //                                }
                                    }
                                }
                            } catch (Exception e) {
                                throw new Error(e);
                            }
                        }
                        synchronized(m_processedSequences) {
                            if ((++m_processedSequences.value) % 100 == 0)
                                Logger.getLogger(FragmentTreeSlimedMTv2.class.getName()).log(Level.INFO, "Fragmentation: " + m_processedSequences + " sequences");

                        }
                    }
                } while (m_it.hasNext());
            } catch (Exception error) {
                Logger.getLogger(FragmentTreeSlimedMTv2.class.getName()).log(Level.SEVERE, "error while building fragment tree",error);

            }
            synchronized(m_RunningThreads) {
                m_RunningThreads--;
            }

        }

    }

    private class addingPeptideTree extends Thread{
        Iterator<Peptide> m_it;
        TreeMap<Double,FragmentTreeSlimedElement> m_targetTree;
        int tree;
        long max_peps = Long.MAX_VALUE;

        public addingPeptideTree(Iterator<Peptide> it, int tree) {
            m_it=it;
            m_targetTree = m_threadTrees[tree];
            this.tree = tree;
        }

        public addingPeptideTree(Iterator<Peptide> it, int tree, long max_peps) {
            m_it=it;
            m_targetTree = m_threadTrees[tree];
            this.tree = tree;
            this.max_peps = max_peps;
        }


        public void run() {
            long pep_count=0;
            int m_localFragmentCount = 0;
            synchronized(m_RunningThreads) {
                m_RunningThreads++;
            }
            try {
                //Peptide pep;
                do {
                    Peptide pep;
                    synchronized (m_it) {
                        if (m_it.hasNext()) {
                            pep = m_it.next();
                        } else
                            pep = null;
                    }
                    if (pep != null) {
                        if (++pep_count > max_peps)
                            break;

//                        for (Peptide pep : seq.getForMass()) {

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
//                        }
                        synchronized(m_processedSequences) {
                            if ((++m_processedSequences.value) % 1000 == 0) {
                                Logger.getLogger(FragmentTreeSlimedMTv2.class.getName()).log(Level.INFO, "Fragmentation: " + m_processedSequences + " Peptides");
                                m_config.getStatusInterface().setStatus("Fragmentation: " + ((int)(m_processedSequences.value/(double)m_total_Peptides * 100)) + "% of  Peptides" );

                            }

                        }
                    }
                } while (m_it.hasNext());
            } catch (Exception error) {
                Logger.getLogger(FragmentTreeSlimedMTv2.class.getName()).log(Level.SEVERE, "error while building fragment tree",error);

            }
            synchronized(m_RunningThreads) {
                m_RunningThreads--;
            }

        }

    }



    public FragmentTreeSlimedMTv2(SequenceList list, double tolerance, int threads){
        this(list, new ToleranceUnit(tolerance, "da"), threads, Double.MAX_VALUE, null);
    }

    public FragmentTreeSlimedMTv2(PeptideLookup peptideList, SequenceList list, double tolerance, int threads){
        this(peptideList, list, new ToleranceUnit(tolerance, "da"), threads, Double.MAX_VALUE, null);
    }

    public FragmentTreeSlimedMTv2(SequenceList list, double tolerance, int threads, RunConfig config){
        this(list, new ToleranceUnit(tolerance, "da"), threads, Double.MAX_VALUE, config);
    }

    public FragmentTreeSlimedMTv2(PeptideLookup peptideList, SequenceList list, double tolerance, int threads, RunConfig config){
        this(peptideList, list, new ToleranceUnit(tolerance, "da"), threads, Double.MAX_VALUE, config);
    }

    public FragmentTreeSlimedMTv2(SequenceList list, ToleranceUnit Tolerance, int threads){
        this(list, Tolerance, threads, Double.MAX_VALUE,null);
    }

    public FragmentTreeSlimedMTv2(PeptideLookup peptideList, SequenceList list, ToleranceUnit Tolerance, int threads){
        this(peptideList, list, Tolerance, threads, Double.MAX_VALUE,null);
    }


    public FragmentTreeSlimedMTv2(SequenceList list, ToleranceUnit Tolerance, int threads, RunConfig config){
        this(list, Tolerance, threads, Double.MAX_VALUE,config);
    }

    public FragmentTreeSlimedMTv2(PeptideLookup peptideList, SequenceList list, ToleranceUnit Tolerance, int threads, RunConfig config){
        this(peptideList, list, Tolerance, threads, Double.MAX_VALUE,config);
    }

    public FragmentTreeSlimedMTv2(SequenceList list, int threads, RunConfig config){
        this(list, config.getFragmentTolerance(), threads, Double.MAX_VALUE,config);
    }

    public FragmentTreeSlimedMTv2(PeptideLookup peptideList, SequenceList list, int threads, RunConfig config){
        this(peptideList, list, config.getFragmentTolerance(), threads, Double.MAX_VALUE,config);
    }

    public FragmentTreeSlimedMTv2(PeptideLookup peptideList, long maxPeptides, Peptide lastPeptide, SequenceList list, int threads, RunConfig config){
        this(peptideList, maxPeptides, lastPeptide, list, config.getFragmentTolerance(), threads, Double.MAX_VALUE,config);
    }

//    public FragmentTreeSlimedMTv2(SequenceList sequences, PeptideLookup peptides, int threads, RunConfig config){
//        this(sequences, peptides, config.getFragmentTolerance(), threads, Double.MAX_VALUE,config);
//    }

    public FragmentTreeSlimedMTv2(SequenceList list, ToleranceUnit Tolerance, int threads, double MaximumPeptideMass, RunConfig config){
        m_MaximumPeptideMass = MaximumPeptideMass;
        m_list = list;
        m_Tolerance = Tolerance;
        m_FragmentCount  = 0;
        m_threadTrees = new InnerTreeMap[threads];
        m_perTreeCount = new int[threads];
        m_config = config;

        insertFragements(list.iterator());
    }

    public FragmentTreeSlimedMTv2(PeptideLookup PeptideList, SequenceList list, ToleranceUnit Tolerance, int threads, double MaximumPeptideMass, RunConfig config){
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

        insertFragementsFromPeptides(m_peptides);
    }

    public FragmentTreeSlimedMTv2(PeptideLookup PeptideList, long maxPeptides, Peptide lastPeptide, SequenceList list, ToleranceUnit Tolerance, int threads, double MaximumPeptideMass, RunConfig config){
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

        insertFragementsFromPeptides(m_peptides);
    }

    public void insertFragements(Iterator<Sequence> sequences) {
        int threads = m_threadTrees.length;
        //PeptideIterator pIt = PeptideList.peptides();
        for (int i=1; i<threads; i++) {
            m_threadTrees[i] = new InnerTreeMap();
            new adding(sequences, i).start();
        }
        m_threadTrees[0] = new InnerTreeMap();
        new adding(sequences, 0).run();
        while (m_RunningThreads > 0) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {}
        }
    }


    public void insertFragementsFromPeptides(Iterator<Peptide> peptides) {
//        int FragmentCountLast = 0;
        int threads = m_threadTrees.length;
        long maxPepPerTree = m_maxPeptides/threads; // how many peptides per tree
        long rest = m_maxPeptides % threads; // how many would we miss
        Thread[] addingThreads = new Thread[threads];
        //PeptideIterator pIt = PeptideList.peptides();
        for (int i=0; i<threads; i++) {

            long maxPeps = maxPepPerTree;
            if (i == 0)
                maxPeps += rest; // the first tree does the missing number of peptides as well
            
            m_threadTrees[i] = new InnerTreeMap();
            addingThreads[i] = new addingPeptideTree(peptides, i, maxPeps);
            addingThreads[i].start();
        }
//        m_threadTrees[0] = new InnerTreeMap();
//        new addingPeptideTree(peptides, 0).run();
        while (true) {
            boolean finished = true;
            for (int i=0; i<threads; i++) {
                try {
                    addingThreads[i].join(500);
                } catch (InterruptedException ex) {}
                if (addingThreads[i].isAlive())
                        finished = false;
            }
            if (finished) break;

        }

        this.shrink();
    }


//    @Override
//    public boolean nextRound() {
//        for (InnerTreeMap t: m_threadTrees)
//            t.clear();
//        if (m_peptides.hasNext()) {
//            insertFragementsFromPeptides(m_peptides);
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
        ArrayList<Peptide> ret = new ArrayList<Peptide>();
        for (int t = 0; t<m_threadTrees.length;t++) {
//            if ((int)mass == 173)
//                        System.err.println("found it");
            Collection<FragmentTreeSlimedElement> entries =  m_threadTrees[t].subMap(m_Tolerance.getMinRange(mass), m_Tolerance.getMaxRange(mass)).values();
            Peptide[] allPeptides = m_list.getAllPeptideIDs();
            Iterator<FragmentTreeSlimedElement> it = entries.iterator();
            while (it.hasNext()) {
                FragmentTreeSlimedElement ids = it.next();
                for (int i = 0; i < ids.m_countPeptides; i++) {
//                    if (allPeptides[ids.m_peptideIds[i]] == null)
//                        System.err.println("found it");
                    ret.add(allPeptides[ids.m_peptideIds[i]]);
                }
            }
        }
        return ret;
    }

    public ArrayList<Peptide> getForMass(double mass, double referenceMass) {
        ArrayList<Peptide> ret = new ArrayList<Peptide>();
        for (int t = 0; t<m_threadTrees.length;t++) {
//            if ((int)mass == 173)
//                        System.err.println("found it");
            Collection<FragmentTreeSlimedElement> entries =  m_threadTrees[t].subMap(m_Tolerance.getMinRange(mass, referenceMass), m_Tolerance.getMaxRange(mass, referenceMass)).values();
            Peptide[] allPeptides = m_list.getAllPeptideIDs();
            Iterator<FragmentTreeSlimedElement> it = entries.iterator();
            while (it.hasNext()) {
                FragmentTreeSlimedElement ids = it.next();
                for (int i = 0; i < ids.m_countPeptides; i++) {
//                    if (allPeptides[ids.m_peptideIds[i]] == null)
//                        System.err.println("found it");
                    ret.add(allPeptides[ids.m_peptideIds[i]]);
                }
            }
        }
        return ret;
    }
    
    
    @Override
    public ArrayList<Peptide> getForMass(double mass, double referenceMass, double maxMass) {
        ArrayList<Peptide> ret = new ArrayList<Peptide>();
        for (int t = 0; t<m_threadTrees.length;t++) {
//            if ((int)mass == 173)
//                        System.err.println("found it");
            Collection<FragmentTreeSlimedElement> entries =  m_threadTrees[t].subMap(m_Tolerance.getMinRange(mass, referenceMass), m_Tolerance.getMaxRange(mass, referenceMass)).values();
            Peptide[] allPeptides = m_list.getAllPeptideIDs();
            Iterator<FragmentTreeSlimedElement> it = entries.iterator();
            while (it.hasNext()) {
                FragmentTreeSlimedElement ids = it.next();
                for (int i = 0; i < ids.m_countPeptides; i++) {
                    Peptide p = allPeptides[ids.m_peptideIds[i]];
                    if (p.getMass() <= maxMass)
                        ret.add(p);
                }
            }
        }
        return ret;
    }

    @Override
    public ArrayList<Peptide> getForMass(double mass, double referenceMass, double maxMass,int maxPeptides) {
        ArrayList<Peptide> ret = new ArrayList<Peptide>();
        Collection[] allEntries = new Collection[m_threadTrees.length];
        int count =0;
        for (int t = 0; t<m_threadTrees.length;t++) {
            allEntries[t] =  m_threadTrees[t].subMap(m_Tolerance.getMinRange(mass, referenceMass), m_Tolerance.getMaxRange(mass, referenceMass)).values();
            count+=allEntries[t].size();
        }
        
        if (count<= maxPeptides) {
            for (int t = 0; t<allEntries.length;t++) {
                Collection<FragmentTreeSlimedElement> entries =allEntries[t];
                Peptide[] allPeptides = m_list.getAllPeptideIDs();
                Iterator<FragmentTreeSlimedElement> it = entries.iterator();
                while (it.hasNext()) {
                    FragmentTreeSlimedElement ids = it.next();
                    for (int i = 0; i < ids.m_countPeptides; i++) {
                        Peptide p = allPeptides[ids.m_peptideIds[i]];
                        if (p.getMass() <= maxMass)
                            ret.add(p);
                    }
                }
            }
        }
        return ret;
    }
    
    public ArrayList<Peptide> getPeptidesExactFragmentMass(double mass) {
        ArrayList<Peptide> ret = new ArrayList<Peptide>();
        Peptide[] allPeptides = m_list.getAllPeptideIDs();
        for (int t = 0; t<m_threadTrees.length;t++) {
//            if ((int)mass == 173)
//                        System.err.println("found it");
            FragmentTreeSlimedElement ids =  m_threadTrees[t].get(mass);
            if (ids != null)
                for (int i = 0; i < ids.m_countPeptides; i++) {
    //                    if (allPeptides[ids.m_peptideIds[i]] == null)
    //                        System.err.println("found it");
                    ret.add(allPeptides[ids.m_peptideIds[i]]);
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
            double from = m_Tolerance.getMinRange(mass);
            double to = m_Tolerance.getMaxRange(mass);

//            if (from > to)
//                    System.err.println("count peptides From > to");
            for (FragmentTreeSlimedElement e : m_threadTrees[t].subMap(from, to).values()) {
                count +=  e.m_countPeptides;
            }
        }
        return count;
    }

    public int countPeptides(double mass, double referenceMass) {
        int count = 0;
//        ArrayList<Peptide> ret = new ArrayList<Peptide>();
        for (int t = 0; t<m_threadTrees.length;t++) {
//            if ((int)mass == 173)
//                        System.err.println("found it");
            count +=  m_threadTrees[t].subMap(m_Tolerance.getMinRange(mass,referenceMass), m_Tolerance.getMaxRange(mass, referenceMass)).size();
        }
        return count;
    }


    public Map<Peptide, Double> getPeptidesForMasses(double mass) {
        HashMap<Peptide, Double> ret = new HashMap<Peptide, Double>();
        for (int t = 0; t<m_threadTrees.length;t++) {
            Collection<FragmentTreeSlimedElement> entries =  m_threadTrees[t].subMap(m_Tolerance.getMinRange(mass), m_Tolerance.getMaxRange(mass)).values();
            Peptide[] allPeptides = m_list.getAllPeptideIDs();
            Iterator<FragmentTreeSlimedElement> it = entries.iterator();
            while (it.hasNext()) {
                FragmentTreeSlimedElement ids = it.next();
                for (int i = 0; i < ids.m_countPeptides; i++)
                    ret.put(allPeptides[ids.m_peptideIds[i]], mass);
            }
        }
        return ret;
    }

    public void setTolerance(ToleranceUnit tolerance) {
        m_Tolerance = tolerance;
    }




    private void addFragment(Peptide pep, double mass, TreeMap<Double, FragmentTreeSlimedElement> targetTree, int tree) {


        FragmentTreeSlimedElement src=null;
        if (targetTree.containsKey(mass))
            src  = targetTree.get(mass);
        if (src != null) {
            src.add(pep.getPeptideIndex());
        } else {

            targetTree.put(mass, new FragmentTreeSlimedElement(pep.getPeptideIndex()));
        }
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
            java.util.Set<Double> s = m_threadTrees[i].keySet();
            Iterator<Double> it =  s.iterator();
            while (it.hasNext()) {
                Double k = it.next();
                FragmentTreeSlimedElement v = m_threadTrees[i].get(k);
                minSize = Math.min(minSize, v.m_countPeptides);
                maxSize = Math.max(maxSize, v.m_countPeptides);
                count += v.m_countPeptides;
            }
        }



        out.println("==============================");
        out.println("==        Statistic        ===");
        out.println("==============================");
        //out.println("==distinct masses: " + this.size());
        out.println("==min peptides per mass: " + minSize);
        out.println("==max peptides per mass: " + maxSize);
        //out.println("==avarage peptides per mass: " + count/(double)this.size());
        out.println("==============================");
    }

    public void dump(String path) throws IOException {
        dump(new FileWriter(path));
    }

    public void dump(Writer out) throws IOException {
        BufferedWriter bw = new BufferedWriter(out);
        Peptide[] allPeptides = m_list.getAllPeptideIDs();
        HashSet<Double> masses = new HashSet<Double>();
        for (int i = 0; i < m_threadTrees.length; i++) {
            java.util.Set<Double> s = m_threadTrees[i].keySet();
            masses.addAll(s);
        }
        
        Double[] ma = masses.toArray(new Double[0]);
        
        java.util.Arrays.sort(ma);
        
        for (double m : ma) {
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



//        for (int i = 0; i < m_threadTrees.length; i++) {
//            java.util.Set<Double> s = m_threadTrees[i].keySet();
//            Iterator<Double> it =  s.iterator();
//            while (it.hasNext()) {
//                Double k = it.next();
//                FragmentTreeSlimedElement v = m_threadTrees[i].get(k);
//                bw.append(i + "," + k.toString());
//                for (int pepID : v.m_peptideIds) {
//                    bw.append("," + pepID + "," + allPeptides[pepID]);
//                }
//                bw.newLine();
//            }
//        }

    }
    
    public void shrink() {
//        Thread[] waitThreads = new Thread[m_threadTrees.length];
        for (int i = 0; i < m_threadTrees.length; i++) {
            final InnerTreeMap it  = m_threadTrees[i];

//            Runnable runnable = new Runnable() {
//
//                public void run() {
                    for (FragmentTreeSlimedElement e : it.values().toArray(new FragmentTreeSlimedElement[0])) {
                        e.shrink();
                    }
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


}
