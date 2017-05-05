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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ms.sequence.ions.Fragment;


public class FragmentTreeSlimedMT implements FragmentLookup, FragmentCollection{
    private static final long serialVersionUID = -119704332506558616L;
    int m_FragmentCount = 0;
    ToleranceUnit m_Tolerance;
    private SequenceList  m_list;
    Integer m_RunningThreads = 0;
    int m_nextTree = 0;

    double  m_MinimumMass = 0;
    double  m_MaximumPeptideMass = Double.MAX_VALUE;

    int m_PeptidesPerThread = 10;

    /**
     * @return the m_list
     */
    public SequenceList getSequeneList() {
        return m_list;
    }

    @Override
    public Peptide lastFragmentedPeptide() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PeptideIterator getPeptideIterator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }



    private class InnerTreeMap extends TreeMap<Double, FragmentTreeSlimedElement> {
        private static final long serialVersionUID = -4038223287995095272L;
    }
    

    InnerTreeMap[] m_threadTrees;
    int[]          m_perTreeCount;
    RunConfig      m_config = null;


    private class adding extends Thread{
        Iterator<Peptide> m_it;
        TreeMap<Double,FragmentTreeSlimedElement> m_targetTree;
        int tree;

        public adding(Iterator<Peptide> it, int tree) {
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
                ArrayList<Peptide> peps = new ArrayList<Peptide>(m_PeptidesPerThread);
                Peptide pep;
                do {
                    peps.clear();
                    synchronized (m_it) {
                        while (peps.size() < m_PeptidesPerThread && m_it.hasNext()) {
                            pep = m_it.next();
                            if (pep != null) {
                                peps.add(pep);
                            }
                        }
                    }
                    for (int p = 0; p < peps.size(); p++) {

                        try {
                            pep = peps.get(p);
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
                } while (m_it.hasNext());
            } catch (Exception error) {
                Logger.getLogger(FragmentTreeSlimedMT.class.getName()).log(Level.INFO, "error while building fragment tree",error);

            }
            synchronized(m_RunningThreads) {
                m_RunningThreads--;
            }

        }

    }




    public FragmentTreeSlimedMT(SequenceList list, double tolerance, int threads){
        this(list, new ToleranceUnit(tolerance, "da"), threads, Double.MAX_VALUE, null);
    }

    public FragmentTreeSlimedMT(SequenceList list, double tolerance, int threads, RunConfig config){
        this(list, new ToleranceUnit(tolerance, "da"), threads, Double.MAX_VALUE, config);
    }


    public FragmentTreeSlimedMT(SequenceList list, ToleranceUnit Tolerance, int threads){
        this(list, Tolerance, threads, Double.MAX_VALUE,null);
    }

    public FragmentTreeSlimedMT(SequenceList list, ToleranceUnit Tolerance, int threads, RunConfig config){
        this(list, Tolerance, threads, Double.MAX_VALUE,config);
    }

    public FragmentTreeSlimedMT(SequenceList list, int threads, RunConfig config){
        this(list, config.getFragmentTolerance(), threads, Double.MAX_VALUE,config);
    }

    public FragmentTreeSlimedMT(SequenceList sequences, PeptideLookup peptides, int threads, RunConfig config){
        this(sequences, peptides, config.getFragmentTolerance(), threads, Double.MAX_VALUE,config);
    }

    public FragmentTreeSlimedMT(SequenceList list, ToleranceUnit Tolerance, int threads, double MaximumPeptideMass, RunConfig config){
        m_MaximumPeptideMass = MaximumPeptideMass;
        m_list = list;
        m_Tolerance = Tolerance;
        m_FragmentCount  = 0;
        m_threadTrees = new InnerTreeMap[threads];
        m_perTreeCount = new int[threads];
        m_config = config;

        //PeptideIterator pIt = list.peptides();
        insertFragements(list.peptides());
//        PeptideIterator it = list.peptides();
//        for (int i=1; i<threads; i++) {
//            m_threadTrees[i] = new InnerTreeMap();
//            new adding(it, i).start();
//        }
//        m_threadTrees[0] = new InnerTreeMap();
//        new adding(it, 0).run();
//        while (m_RunningThreads > 0) {
//            try {
//                Thread.sleep(500);
//            } catch (InterruptedException ex) {}
//        }

    }

    public void insertFragements(Iterator<Peptide> peptides) {
        int threads = m_threadTrees.length;
        //PeptideIterator pIt = list.peptides();
        for (int i=1; i<threads; i++) {
            m_threadTrees[i] = new InnerTreeMap();
            new adding(peptides, i).start();
        }
        m_threadTrees[0] = new InnerTreeMap();
        new adding(peptides, 0).run();
        while (m_RunningThreads > 0) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {}
        }
    }

    public FragmentTreeSlimedMT(SequenceList sl, PeptideLookup list, ToleranceUnit Tolerance, int threads, double MaximumPeptideMass, RunConfig config){
        m_MaximumPeptideMass = MaximumPeptideMass;
        m_list = sl;
        m_Tolerance = Tolerance;
        m_FragmentCount  = 0;
        m_threadTrees = new InnerTreeMap[threads];
        m_perTreeCount = new int[threads];
        m_config = config;

        insertFragements(list.iterator());

        //PeptideIterator pIt = list.peptides();
//        PeptideIterator it = list.getPeptideIterator();
//        for (int i=1; i<threads; i++) {
//            m_threadTrees[i] = new InnerTreeMap();
//            new adding(it, i).start();
//        }
//        m_threadTrees[0] = new InnerTreeMap();
//        new adding(it, 0).run();
//        while (m_RunningThreads > 0) {
//            try {
//                Thread.sleep(500);
//            } catch (InterruptedException ex) {}
//        }

    }


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
            Collection<FragmentTreeSlimedElement> entries =  m_threadTrees[t].subMap(m_Tolerance.getMinRange(mass,referenceMass), m_Tolerance.getMaxRange(mass, referenceMass)).values();
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
//               Collection<FragmentTreeSlimedElement> entries =  m_threadTrees[t].subMap(m_Tolerance.getMinRange(mass, referenceMass), m_Tolerance.getMaxRange(mass, referenceMass)).values();
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
    
    public int countPeptides(double mass) {
        int count = 0;
//        ArrayList<Peptide> ret = new ArrayList<Peptide>();
        for (int t = 0; t<m_threadTrees.length;t++) {
//            if ((int)mass == 173)
//                        System.err.println("found it");
            for (FragmentTreeSlimedElement e : m_threadTrees[t].subMap(m_Tolerance.getMinRange(mass), m_Tolerance.getMaxRange(mass)).values())
                count +=  e.m_countPeptides;
        }
        return count;
    }

    public int countPeptides(double mass, double referenceMass) {
        int count = 0;
//        ArrayList<Peptide> ret = new ArrayList<Peptide>();
        for (int t = 0; t<m_threadTrees.length;t++) {
//            if ((int)mass == 173)
//                        System.err.println("found it");
            for (FragmentTreeSlimedElement e : m_threadTrees[t].subMap(m_Tolerance.getMinRange(mass,referenceMass), m_Tolerance.getMaxRange(mass, referenceMass)).values())
                count +=  e.m_countPeptides;
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

//    @Override
//    public boolean nextRound() {
//        return false;
//    }

}
