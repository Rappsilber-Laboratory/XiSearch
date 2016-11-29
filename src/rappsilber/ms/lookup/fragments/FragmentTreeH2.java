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
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.RunConfig;
import rappsilber.db.ConnectionPool;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.sequence.Iterators.PeptideIterator;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.Sequence;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.statistics.utils.UpdateableInteger;


public class FragmentTreeH2 implements FragmentLookup, FragmentCollection{
    private static final long serialVersionUID = -119704332506558616L;
    int m_FragmentCount = 0;
    ToleranceUnit m_Tolerance;
    private SequenceList  m_list;
    Integer m_RunningThreads = 0;
    int m_nextTree = 0;

    double  m_MinimumMass = 0;
    double  m_MaximumPeptideMass = Double.MAX_VALUE;
    private String m_dbPath = null;
    private ArrayList<Connection> m_connections =  new ArrayList<Connection>();


    int m_PeptidesPerThread = 10;
    private UpdateableInteger m_processedSequences = new UpdateableInteger(0);
    private PreparedStatement m_InsertFragement;
    private PreparedStatement m_GetPeptides;
    private PreparedStatement m_GetPeptidesWithMass;
    private PreparedStatement m_CountPeptides;
    private PreparedStatement m_CountFragments;
    private ConnectionPool    m_ConnectionPool;
    private int               m_countFraments=0;
    private double            m_MinMass = 0;
    private double            m_MaxMass = 0;
    private double            m_MassWindow = 20;
    private CachedFragmentTree m_CachedFragments;



    private int m_ThreadCount = 1;

    @Override
    public Peptide lastFragmentedPeptide() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PeptideIterator getPeptideIterator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

//    @Override
//    public boolean nextRound() {
//        return false;
//    }


    private class CachedFragmentTree extends TreeMap<Double, FragmentTreeSlimedElement> {
        private static final long serialVersionUID = -4263269823540053713L;

        public void add(Peptide pep, double mass) {
            add(pep.getPeptideIndex(),mass);
        }

        public void add(int pepID, double mass) {
            FragmentTreeSlimedElement e;
            if (containsKey(mass)) {
                e = get(mass);
                e.add(pepID);
            } else {
                e = new FragmentTreeSlimedElement(pepID);
                this.put(mass, e);
            }
        }

    }


    /**
     * @return the m_list
     */
    public SequenceList getSequeneList() {
        return m_list;
    }






    RunConfig      m_config = null;


    private class adding extends Thread{
        Iterator<Sequence> m_it;
        Connection m_con = getNewConnection();
        PreparedStatement m_FragInsert;
        int tree;

        public adding(Iterator<Sequence> it, int tree) {
            m_it=it;
            this.tree = tree;
            try {
                m_FragInsert = m_con.prepareStatement("INSERT INTO F (ID, M) VALUES (?,?)");
                m_GetPeptidesWithMass = m_con.prepareStatement("SELECT ID, M FROM F WHERE M >= ? AND M <= ? ORDER BY M");
                m_GetPeptides = m_con.prepareStatement("SELECT ID FROM F WHERE M >= ? AND M <= ?");
                m_CountPeptides = m_con.prepareStatement("SELECT count(*) FROM F WHERE M >= ? AND M <= ?");
                m_CountFragments = m_con.prepareStatement("SELECT count(*) FROM F");
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            }
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
                                        addFragment(pep, f.getMass(),m_FragInsert);
                                    }
                                }
                            } catch (Exception e) {
                                throw new Error(e);
                            }
                        }
                        synchronized(m_processedSequences) {
                            if ((++m_processedSequences.value) % 100 == 0)
                                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Fragmentation: " + m_processedSequences + " sequences");

                        }
                    }
                } while (m_it.hasNext());
            } catch (Exception error) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "error while building fragment tree",error);

            }
            synchronized(m_RunningThreads) {
                m_RunningThreads--;
            }

        }

    }




    public FragmentTreeH2(SequenceList list, double tolerance, int threads){
        this(list, new ToleranceUnit(tolerance, "da"), threads, Double.MAX_VALUE, null);
    }

    public FragmentTreeH2(SequenceList list, double tolerance, int threads, RunConfig config){
        this(list, new ToleranceUnit(tolerance, "da"), threads, Double.MAX_VALUE, config);
    }


    public FragmentTreeH2(SequenceList list, ToleranceUnit Tolerance, int threads){
        this(list, Tolerance, threads, Double.MAX_VALUE,null);
    }

    public FragmentTreeH2(SequenceList list, ToleranceUnit Tolerance, int threads, RunConfig config){
        this(list, Tolerance, threads, Double.MAX_VALUE,config);
    }

    public FragmentTreeH2(SequenceList list, int threads, RunConfig config){
        this(list, config.getFragmentTolerance(), threads, Double.MAX_VALUE,config);
    }

//    public FragmentTreeSlimedMT_v2(SequenceList sequences, PeptideLookup peptides, int threads, RunConfig config){
//        this(sequences, peptides, config.getFragmentTolerance(), threads, Double.MAX_VALUE,config);
//    }

    protected Connection getNewConnection() {

        try {
            if (m_dbPath == null) {
                // get a random file name
                File tempDir = File.createTempFile("XlinkFTH2", "dir");
                tempDir.delete();
                tempDir.mkdir();
                m_dbPath = tempDir.getAbsolutePath();
                m_ConnectionPool = new ConnectionPool("org.h2.Driver", "jdbc:h2:" + m_dbPath, "sa","");

            }

            return m_ConnectionPool.getConnection();
        } catch (IOException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Could not create a path for the database;", ex);
            System.exit(1);
            return null;
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "could not access the database", ex);
            System.exit(1);
            return null;
        }

    }

    protected void closeConnection(Connection con) {
        m_ConnectionPool.free(con);
    }



    public FragmentTreeH2(SequenceList list, ToleranceUnit Tolerance, int threads, double MaximumPeptideMass, RunConfig config){
        m_MaximumPeptideMass = MaximumPeptideMass;
        m_list = list;
        m_Tolerance = Tolerance;
        m_FragmentCount  = 0;
        m_config = config;
        m_ThreadCount = threads;
        try {
            Class.forName("org.h2.Driver");
            Connection con = getNewConnection();
            // create the fragment table
            con.createStatement().execute("CREATE CACHED TEMP TABLE F (ID INT, M DOUBLE); CREATE INDEX FIDX ON F(M)");
            m_InsertFragement = con.prepareStatement("INSERT INTO F (ID, M) VALUES (?,?)");

            // add application code here
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }

        // add application code here

        //PeptideIterator pIt = list.peptides();
        insertFragements(list.iterator());
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

    public void insertFragements(Iterator<Sequence> sequences) {
        //PeptideIterator pIt = list.peptides();
        for (int i=1; i<m_ThreadCount; i++) {
            new adding(sequences, i).start();
        }
        new adding(sequences, 0).run();
        while (m_RunningThreads > 0) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {}
        }


        ResultSet rs;
        try {
            rs = m_CountFragments.executeQuery();
            rs.next();
            m_countFraments = rs.getInt(1);
        } catch (SQLException ex) {
            Logger.getLogger(FragmentTreeH2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }



    public void clear() {
        m_ConnectionPool.closeAllConnections();
        new File(m_dbPath).delete();
    }


    public ArrayList<Peptide> getForMass(double mass) {
        try {
            double min = m_Tolerance.getMinRange(mass);
            double max = m_Tolerance.getMaxRange(mass);

            ArrayList<Peptide> ret = new ArrayList<Peptide>();
            Peptide[] allPeptides = m_list.getAllPeptideIDs();
            if (min < m_MinMass || max > m_MaxMass) {
                m_CachedFragments = new CachedFragmentTree();
                m_MinMass = min;
                m_MaxMass = Math.max(max, min + m_MassWindow);
                ResultSet rs = null;
                synchronized (m_GetPeptidesWithMass) {
                    try {
                        m_GetPeptidesWithMass.setDouble(1, m_MinMass);
                        m_GetPeptidesWithMass.setDouble(2, m_MaxMass);
                        rs = m_GetPeptidesWithMass.executeQuery();
                    } catch (SQLException ex) {
                        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                        System.exit(1);
                    }

                    while (rs.next()) {
                        m_CachedFragments.add(rs.getInt(1), rs.getDouble(2));
                    }
                }
            }


            for (FragmentTreeSlimedElement e : m_CachedFragments.subMap(min, max).values()) {
                for (int pid : e.m_peptideIds)
                    ret.add(allPeptides[pid]);
            }

            return ret;
            
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
            return null;
        }
    }

    public ArrayList<Peptide> getForMass(double mass, double referenceMass) {
        try {
            double min = m_Tolerance.getMinRange(mass, referenceMass);
            double max = m_Tolerance.getMaxRange(mass, referenceMass);

            ArrayList<Peptide> ret = new ArrayList<Peptide>();
            Peptide[] allPeptides = m_list.getAllPeptideIDs();
            if (min < m_MinMass || max > m_MaxMass) {
                m_CachedFragments = new CachedFragmentTree();
                m_MinMass = min;
                m_MaxMass = Math.max(max, min + m_MassWindow);
                ResultSet rs = null;
                synchronized (m_GetPeptidesWithMass) {
                    try {
                        m_GetPeptidesWithMass.setDouble(1, m_MinMass);
                        m_GetPeptidesWithMass.setDouble(2, m_MaxMass);
                        rs = m_GetPeptidesWithMass.executeQuery();
                    } catch (SQLException ex) {
                        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                        System.exit(1);
                    }

                    while (rs.next()) {
                        m_CachedFragments.add(rs.getInt(1), rs.getDouble(2));
                    }
                }
            }


            for (FragmentTreeSlimedElement e : m_CachedFragments.subMap(min, max).values()) {
                for (int pid : e.m_peptideIds)
                    ret.add(allPeptides[pid]);
            }

            return ret;

        } catch (SQLException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
            return null;
        }
    }

    public int countPeptides(double mass, double referenceMass) {
        int count = 0;
//        ArrayList<Peptide> ret = new ArrayList<Peptide>();
        ResultSet rs = null;
        synchronized (m_CountPeptides) {
            try {
                m_CountPeptides.setDouble(1, m_Tolerance.getMinRange(mass, referenceMass));
                m_CountPeptides.setDouble(2, m_Tolerance.getMaxRange(mass, referenceMass));
                rs = m_CountPeptides.executeQuery();
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }

        try {
            while (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException ex) {
            Logger.getLogger(FragmentTreeH2.class.getName()).log(Level.SEVERE, null, ex);
        }

        return count;
    }

    public int countPeptides(double mass) {
        return countPeptides(mass, mass);
    }


    public Map<Peptide, Double> getPeptidesForMasses(double mass) {
        HashMap<Peptide, Double> ret = new HashMap<Peptide, Double>();
        try {
            double min = m_Tolerance.getMinRange(mass);
            double max = m_Tolerance.getMaxRange(mass);

            Peptide[] allPeptides = m_list.getAllPeptideIDs();
            if (min < m_MinMass || max > m_MaxMass) {
                m_CachedFragments = new CachedFragmentTree();
                m_MinMass = min;
                m_MaxMass = Math.max(max, min + m_MassWindow);
                ResultSet rs = null;
                synchronized (m_GetPeptidesWithMass) {
                    try {
                        m_GetPeptidesWithMass.setDouble(1, m_MinMass);
                        m_GetPeptidesWithMass.setDouble(2, m_MaxMass);
                        rs = m_GetPeptidesWithMass.executeQuery();
                    } catch (SQLException ex) {
                        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                        System.exit(1);
                    }

                    while (rs.next()) {
                        m_CachedFragments.add(rs.getInt(1), rs.getDouble(2));
                    }
                }
            }

            SortedMap<Double,FragmentTreeSlimedElement> m  = m_CachedFragments.subMap(min, max);
            for (Double d : m.keySet()) {
                FragmentTreeSlimedElement e = m.get(ret);
                for (int pid : e.m_peptideIds)
                    ret.put(allPeptides[pid],d);
            }
            return ret;
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
            return null;
        }
    }

    public void setTolerance(ToleranceUnit tolerance) {
        m_Tolerance = tolerance;
    }




    private void addFragment(Peptide pep, double mass, PreparedStatement tree) {
        try {
            if (mass > 120) {
                tree.setInt(1, pep.getPeptideIndex());
                tree.setDouble(2, mass);
                tree.execute();
                m_FragmentCount ++;
            }
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }


    }

    public void addFragment(Peptide pep, double mass) {
        synchronized (m_InsertFragement) {
            addFragment(pep, mass, m_InsertFragement);
        }
    }

    public int getFragmentCount() {
        return m_countFraments;
    }

    public void printStatistic(PrintStream out) {


        out.println("==============================");
        out.println("==        Statistic        ===");
        out.println("==============================");
        //out.println("==distinct masses: " + this.size());
        //out.println("==avarage peptides per mass: " + count/(double)this.size());
        out.println("==============================");
    }

    public void dump(String path) throws IOException {
        dump(new FileWriter(path));
    }

    public void dump(Writer out) throws IOException {

    }


}
