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
package rappsilber.ms.dataAccess.db;

//import com.jamonapi.Monitor;
//import com.jamonapi.MonitorFactory;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.postgresql.PGConnection;
import rappsilber.config.RunConfig;
import rappsilber.db.ConnectionPool;
import rappsilber.ms.dataAccess.output.AbstractResultWriter;
import rappsilber.ms.dataAccess.output.ResultWriter;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.Sequence;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.SpectraPeakCluster;
import rappsilber.ms.spectra.match.MatchedBaseFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.ms.statistics.utils.UpdateableInteger;
import rappsilber.utils.SortedLinkedList;

/**
 *
 * @author stahir
 */
public class XiDBWriterMultiInsertSql extends AbstractResultWriter {

    private int m_search_id;
    private int m_acq_id;
    private RunConfig m_config;

    // these values are the same throughout the class so just set there values on init
    private int alpha_id;
    private int beta_id;

    private ConnectionPool m_connectionPool;
    private Connection m_conn;

    private final UpdateableInteger m_SQLThreadCount = new UpdateableInteger(0);


    private final java.util.concurrent.Semaphore m_ThreadMonitor = new java.util.concurrent.Semaphore(4);
    private final Object m_semaphoreSynchronization = new Object();

    private File sqlOutput = new File("/Users/stahir/Documents/workspace/Xi/sql/insert_results.sql");
    private BufferedWriter m_sqlout;



//    private PreparedStatement m_insertSpectrum;
//    private PreparedStatement m_insertSpectrumPeak;
//    private PreparedStatement m_insertPeptide;
//    private PreparedStatement m_insertProtein;
//    private PreparedStatement m_insertHasProtein;
//    private PreparedStatement m_insertFragment;
//    private PreparedStatement m_peakAnnotation;
//    private PreparedStatement m_clusterPeak;
//    private PreparedStatement m_clusterPeak_id;
    private PreparedStatement m_check_score;
    private PreparedStatement m_insert_score;
//    private PreparedStatement m_spectrum_match_score;
//    private PreparedStatement m_spectrum_match;
    private PreparedStatement m_search_complete;
//    private PreparedStatement m_matched_peptide;
    private PreparedStatement m_match_type;
    private PreparedStatement m_getIDs;

    private HashMap<String, Integer> m_scores;


    private int sqlBatchCount = 0;
    private int sqlBufferSize = 0;

    private int results_processed = 0;
    private int top_results_processed = 0;

//    Monitor mon0 = null;
//    Monitor mon1 = null;
//    Monitor mon2 = null;
//    Monitor mon3 = null;
//    Monitor mon4 = null;
//    Monitor mon5 = null;
//    Monitor mon6 = null;
//    Monitor mon7 = null;
//
    // ID counts requested per spectrum - updated on each store
    private class IdCounts{
        long i_spectrum_ids = 0; // might have seen a spectrum before
        long i_peak_ids = 0;
        long i_peak_annotations_ids = 0;
        long i_fragment_ids = 0;
        long i_peptide_ids = 0;
        long i_protein_ids = 0;
        long i_peak_cluster_ids = 0;
        long i_spectrum_match_ids = 1; // will always be 1 for a spectrum result
    }

    // holds the start Ids for each result to save
     private class IDs{
        long i_spectrum_id = 0;
        long i_peak_id = 0;
        long i_peak_annotations_id = 0;
        long i_fragment_id = 0;
        long i_peptide_id = 0;
        long i_protein_id = 0;
        long i_peak_cluster_id = 0;
        long i_spectrum_match_id = 0;
    }

    private StringBuffer m_spectrumSql = new StringBuffer();
    private StringBuffer m_copySpectrum = new StringBuffer();
    public void addSpectrum(int acq_id, long run_id, Spectra s) {
        m_spectrumSql.append(
        "INSERT INTO spectrum (acq_id, run_id, scan_number, elution_time_start, elution_time_end, id) " +
                    "VALUES(");
        m_spectrumSql.append(acq_id);
        m_spectrumSql.append(",");
        m_spectrumSql.append(run_id);
        m_spectrumSql.append(",");
        m_spectrumSql.append(s.getScanNumber());
        m_spectrumSql.append(",");
        m_spectrumSql.append(s.getElutionTimeStart());
        m_spectrumSql.append(",");
        m_spectrumSql.append(s.getElutionTimeEnd());
        m_spectrumSql.append(",");
        m_spectrumSql.append(s.getID());
        m_spectrumSql.append(");");




        m_copySpectrum.append(acq_id);
        m_copySpectrum.append(",");
        m_copySpectrum.append(run_id);
        m_copySpectrum.append(",");
        m_copySpectrum.append(s.getScanNumber());
        m_copySpectrum.append(",");
        m_copySpectrum.append(s.getElutionTimeStart());
        m_copySpectrum.append(",");
        m_copySpectrum.append(s.getElutionTimeEnd());
        m_copySpectrum.append(",");
        m_copySpectrum.append(s.getID() + "\n");



    }

    private StringBuffer m_spectrum_peakSql = new StringBuffer();
    public void addSpectrumPeak(Spectra s, SpectraPeak sp) {
        m_spectrum_peakSql.append("INSERT INTO spectrum_peak(spectrum_id, mz, intensity, id) " +
                    "VALUES(");
        m_spectrum_peakSql.append(s.getID());
        m_spectrum_peakSql.append(",");
        m_spectrum_peakSql.append(sp.getMZ());
        m_spectrum_peakSql.append(",");
        m_spectrum_peakSql.append(sp.getIntensity());
        m_spectrum_peakSql.append(",");
        m_spectrum_peakSql.append(sp.getID());
        m_spectrum_peakSql.append(");");
    }


    private StringBuffer m_peptideSql = new StringBuffer();
    public void addPeptide(Peptide p) {
        m_peptideSql.append("INSERT INTO peptide(sequence, mass, id) " +
                    "VALUES('");
        m_peptideSql.append(p.toString());
        m_peptideSql.append("',");
        m_peptideSql.append(p.getMass());
        m_peptideSql.append(",");
        m_peptideSql.append(p.getID());
        m_peptideSql.append(");");
    }

    private StringBuffer m_proteinSql = new StringBuffer();
    public void addProtein(Sequence p) {
        m_proteinSql.append("INSERT INTO protein(name, sequence, id) " +
                    "VALUES('");
        m_proteinSql.append(p.getFastaHeader());
        m_proteinSql.append("','");
        m_proteinSql.append(p.toString());
        m_proteinSql.append("',");
        m_proteinSql.append(p.getID());
        m_proteinSql.append(");");
    }

    private StringBuffer m_hasProteinSql = new StringBuffer();
    public void addHasProtein(Peptide p) {
        boolean first = true;
        for (Peptide.PeptidePositions pp : p.getPositions()) {
            m_hasProteinSql.append("INSERT INTO has_protein(peptide_id, protein_id, peptide_position, display_site) " +
                        "VALUES(");
            m_hasProteinSql.append(p.getID());
            m_hasProteinSql.append(",");
            m_hasProteinSql.append(pp.base.getID());
            m_hasProteinSql.append(",");
            m_hasProteinSql.append(pp.start);
            m_hasProteinSql.append(",");
            if (first) {
                m_hasProteinSql.append("'true'");
                first = false;
            } else
                m_hasProteinSql.append("'false'");
            m_hasProteinSql.append(");");
        }
    }

    private StringBuffer m_fragmentSql = new StringBuffer();
    public void addFragment(Fragment f) {
        m_fragmentSql.append("INSERT INTO fragment(peptide_id, sequence, mass, id) " +
                "VALUES(");
        m_fragmentSql.append(f.getPeptide().getID());
        m_fragmentSql.append(",'");
        m_fragmentSql.append(f.toString());
        m_fragmentSql.append("',");
        m_fragmentSql.append(f.getNeutralMass());
        m_fragmentSql.append(",");
        m_fragmentSql.append(f.getID());
        m_fragmentSql.append(");");
    }


    private StringBuffer m_spectrumPeakAnnotationSql = new StringBuffer();
    public void addSpectrumPeakAnnotation(SpectraPeak sp, Fragment f, long id) {
        m_spectrumPeakAnnotationSql.append("INSERT INTO spectrum_peak_annotation(peak_id, fragment_id, manual_annotation, false_annotation, notes, id) " +
                    "VALUES(");
        m_spectrumPeakAnnotationSql.append(sp.getID());
        m_spectrumPeakAnnotationSql.append(",");
        m_spectrumPeakAnnotationSql.append(f.getID());
        m_spectrumPeakAnnotationSql.append(",");
        m_spectrumPeakAnnotationSql.append("'false'");
        m_spectrumPeakAnnotationSql.append(",");
        m_spectrumPeakAnnotationSql.append("null");
        m_spectrumPeakAnnotationSql.append(",");
        m_spectrumPeakAnnotationSql.append("''");
        m_spectrumPeakAnnotationSql.append(",");
        m_spectrumPeakAnnotationSql.append(id);
        m_spectrumPeakAnnotationSql.append(");");
    }

    private StringBuffer m_ClusterPeakSql = new StringBuffer();
    public void addClusterPeak(long clusterID, SpectraPeak sp) {
        m_ClusterPeakSql.append("INSERT INTO cluster_peak(id, peak_id) VALUES(");
        m_ClusterPeakSql.append(clusterID);
        m_ClusterPeakSql.append(",");
        m_ClusterPeakSql.append(sp.getID());
        m_ClusterPeakSql.append(");");
    }

    private StringBuffer m_SpectrumMatchScoreSql = new StringBuffer();
    public void addSpectrumMatchScore(long matchid, long scoreId, double score) {
        m_SpectrumMatchScoreSql.append("INSERT INTO spectrum_match_score(spectrum_match_id, score_id, score) VALUES(");
        m_SpectrumMatchScoreSql.append(matchid);
        m_SpectrumMatchScoreSql.append(",");
        m_SpectrumMatchScoreSql.append(scoreId);
        m_SpectrumMatchScoreSql.append(",");
        m_SpectrumMatchScoreSql.append(score);
        m_SpectrumMatchScoreSql.append(");");
    }

    private StringBuffer m_SpectrumMatchSql = new StringBuffer();
    public void addSpectrumMatch(long searchID, double score, long spectrumID, long id) {
        m_SpectrumMatchSql.append("INSERT INTO spectrum_match(search_id, score, spectrum_id, id) VALUES(");
        m_SpectrumMatchSql.append(searchID);
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append(score);
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append(spectrumID);
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append(id);
        m_SpectrumMatchSql.append(");");
    }

    private StringBuffer m_MatchedPeptideSql = new StringBuffer();
    public void addMatchedPeptide(Peptide p, long matchid, long matchtype, long link_position, boolean display_positon) {
        m_MatchedPeptideSql.append("INSERT INTO matched_peptide(peptide_id, match_id, match_type, link_position, display_positon) VALUES(");
        m_MatchedPeptideSql.append(p.getID());
        m_MatchedPeptideSql.append(",");
        m_MatchedPeptideSql.append(matchid);
        m_MatchedPeptideSql.append(",");
        m_MatchedPeptideSql.append(matchtype);
        m_MatchedPeptideSql.append(",");
        m_MatchedPeptideSql.append(link_position);
        m_MatchedPeptideSql.append(",'");
        m_MatchedPeptideSql.append(display_positon);
        m_MatchedPeptideSql.append("');");
    }


    public XiDBWriterMultiInsertSql(RunConfig config, ConnectionPool cp, int searchID, int acq_id) {
        try {
            m_sqlout = new BufferedWriter(new FileWriter(sqlOutput));
        } catch (IOException ex) {
            Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            m_config = config;
            sqlBufferSize = Integer.parseInt((String)m_config.retrieveObject("SQLBUFFER")); // after reading how many spectra do we batch
            
            m_connectionPool = cp;
            m_search_id = searchID;
            m_acq_id = acq_id;
            m_conn = m_connectionPool.getConnection();

            // will hold scores
            m_scores = new HashMap<String, Integer>();
            
            //Compile the prepare stataemnts
//            m_insertSpectrum = m_conn.prepareStatement("INSERT INTO spectrum" +
//                    "(acq_id, run_id, scan_number, elution_time_start, elution_time_end, id) " +
//                    "VALUES(?,?,?,?,?,?)");
//
//
//            // spectrum_id | mz | intensity
//            m_insertSpectrumPeak = m_conn.prepareStatement("INSERT INTO spectrum_peak(spectrum_id, mz, intensity, id) " +
//                    "VALUES(?,?,?,?)");
//
//            m_insertPeptide = m_conn.prepareStatement("INSERT INTO peptide(sequence, mass, id) " +
//                    "VALUES(?,?,?)");
//
//            m_insertProtein = m_conn.prepareStatement("INSERT INTO protein(name, sequence, id) " +
//                    "VALUES(?,?,?)");
//
//            m_insertHasProtein = m_conn.prepareStatement(
//                    "INSERT INTO has_protein(peptide_id, protein_id, peptide_position, display_site) " +
//                    "VALUES(?,?,?,?)");
//
//            m_insertFragment = m_conn.prepareStatement(
//                    "INSERT INTO fragment(peptide_id, sequence, mass, id) " +
//                    "VALUES(?,?,?,?)");
//
//            m_peakAnnotation = m_conn.prepareStatement(
//                    "INSERT INTO spectrum_peak_annotation(peak_id, fragment_id, manual_annotation, false_annotation, notes, id) " +
//                    "VALUES(?,?,?,?,?,?)");
//
//
//            m_clusterPeak = m_conn.prepareStatement(
//                    "INSERT INTO cluster_peak(id, peak_id) " +"VALUES(?,?)");
//
            // m_clusterPeak_id = m_conn.prepareStatement("SELECT nextval('cluster_peak_id_serial')");

            // score related information
            m_check_score = m_conn.prepareStatement("SELECT id, display_order, description FROM score WHERE name = ?");
            m_insert_score = m_conn.prepareStatement(
                    "INSERT INTO score(name, display_order, description) " +
                    "VALUES(?,?,?) RETURNING id");
//            m_spectrum_match_score = m_conn.prepareStatement(
//                    "INSERT INTO spectrum_match_score(spectrum_match_id, score_id, score) VALUES(?,?,?)");
//
//            // overall spectrum info
//            m_spectrum_match = m_conn.prepareStatement(
//                    "INSERT INTO spectrum_match(search_id, score, spectrum_id, id) VALUES(?,?,?,?)");


            m_search_complete = m_conn.prepareStatement(
                    "UPDATE search SET status = 'completed', completed = 'true' where id = ?");

//            m_matched_peptide = m_conn.prepareStatement(
//                    "INSERT INTO matched_peptide(peptide_id, match_id, match_type, link_position, display_positon) VALUES(?,?,?,?,?)");
//
            m_match_type = m_conn.prepareStatement("SELECT id FROM match_type WHERE name = ?");


            // Used to get IDs add pass in 0 
            m_getIDs = m_conn.prepareStatement("SELECT spectrumid, peakid, pepid, protid, specmatchid, paid, fragid, pcid " +
                    "FROM reserve_ids(?,?,?,?,?,?,?,?);");


            // Just get this value once and set them in the class
            alpha_id = -1;
            beta_id = -1;
            m_match_type.setString(1, "alpha");
            ResultSet rs = m_match_type.executeQuery();
            while(rs.next()){alpha_id = rs.getInt(1);}

            m_match_type.setString(1, "beta");
            rs = m_match_type.executeQuery();
            while(rs.next()){beta_id = rs.getInt(1);}
            rs.close();
            
        } catch (SQLException ex) {
           System.err.println("XiDB: problem when setting up XiDBWriter: " + ex.getMessage());
           m_connectionPool.closeAllConnections();
           System.exit(1);
        }




    }


    private void writeInserts(){
        try {

            // Spectrum & Spectrum peaks
            final String spectrumSql = m_spectrumSql.toString();
            m_spectrumSql.setLength(0);
            m_sqlout.write(spectrumSql + "\n");
            final String spectrum_peakSql = m_spectrum_peakSql.toString();
            m_spectrum_peakSql.setLength(0);
            m_sqlout.write(spectrum_peakSql + "\n");

            // Protein, Peptide, hasProtein & fragment
            final String proteinSql =  m_proteinSql.toString();
            m_proteinSql.setLength(0);
            m_sqlout.write(proteinSql + "\n");
            final String peptideSql = m_peptideSql.toString();
            m_peptideSql.setLength(0);
            m_sqlout.write(peptideSql + "\n");
            final String hasProteinSql = m_hasProteinSql.toString();
            m_hasProteinSql.setLength(0);
            m_sqlout.write(hasProteinSql + "\n");
            final String fragmentSql = m_fragmentSql.toString();
            m_fragmentSql.setLength(0);
            m_sqlout.write(fragmentSql + "\n");


            // Spectrum match, MatchedPeptide, SpectrumPeakAnnotation, clusterPeak, spectrumMatchScore
            final String SpectrumMatchSql = m_SpectrumMatchSql.toString();
            m_SpectrumMatchSql.setLength(0);
            m_sqlout.write(SpectrumMatchSql + "\n");
            final String MatchedPeptideSql = m_MatchedPeptideSql.toString();
            m_MatchedPeptideSql.setLength(0);
            m_sqlout.write(MatchedPeptideSql + "\n");
            final String spectrumPeakAnnotationSql = m_spectrumPeakAnnotationSql.toString();
            m_spectrumPeakAnnotationSql.setLength(0);
            m_sqlout.write(spectrumPeakAnnotationSql + "\n");
            final String ClusterPeakSql = m_ClusterPeakSql.toString();
            m_ClusterPeakSql.setLength(0);
            m_sqlout.write(ClusterPeakSql + "\n");
            final String SpectrumMatchScoreSql = m_SpectrumMatchScoreSql.toString();
            m_SpectrumMatchScoreSql.setLength(0);
            m_sqlout.write(SpectrumMatchScoreSql + "\n");


            // Garbage collect if running out of memory
            Runtime r = Runtime.getRuntime();
            if (r.freeMemory()<50000000) {
                    for(int i = 0; i < 10; i++){
                        System.gc();
                    }
            }


           
        } catch (IOException ex) {
           System.err.println("XiDB: problem when writing SQL inserts: " + ex.getMessage());
           m_connectionPool.closeAllConnections();
           System.exit(1);
        }


    }// end method writeInserts()

     private void executeCopy(){

        try {
//            m_copySpectrum.append("\\.");
            final String spectrumCopy = m_copySpectrum.toString();
            byte x[] = spectrumCopy.getBytes();
            InputStream i = new ByteArrayInputStream(x);
            m_copySpectrum.setLength(0);





            Connection con = m_connectionPool.getConnection();
            ((PGConnection) con).getCopyAPI().copyIn(
                    "COPY spectrum (acq_id, run_id, scan_number, elution_time_start, elution_time_end, id) " +
                    "FROM STDIN WITH CSV", i);
            
            
            
            
            
        } catch (IOException ex) {
            Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, null, ex);
        }




    }// end method


    private void executeSQL(){

        final String spectrumSql = m_spectrumSql.toString();
        m_spectrumSql.setLength(0);
//        m_sqlout.write(spectrumSql + "\n");

        final String spectrum_peakSql = m_spectrum_peakSql.toString();
        m_spectrum_peakSql.setLength(0);
//        m_sqlout.write(spectrum_peakSql + "\n");

//        mon_batch = MonitorFactory.start("mon_batch");
        // first store the basic non-dependet spectra informations
        Thread execSQL1 = new Thread() {
            @Override
            public void run() {
                try {
                    Connection conn = m_connectionPool.getConnection();
                    // start commiting
                    boolean isAutoCommit = m_conn.getAutoCommit();
                    conn.setAutoCommit(false);

                    java.sql.Statement stm = conn.createStatement();
                    stm.executeUpdate(spectrumSql);
//                    stm.execute(SpectrumMatchSql);
                    stm.executeUpdate(spectrum_peakSql);
//                    stm.execute(proteinSql);
//                    stm.execute(peptideSql);
//                    stm.execute(hasProteinSql);
//                    stm.execute(MatchedPeptideSql);
//                    stm.execute(fragmentSql);
//                    stm.execute(spectrumPeakAnnotationSql);
//                    stm.execute(ClusterPeakSql);
//                    stm.execute(SpectrumMatchScoreSql);
        //            stm.executeBatch();
                    stm.close();



                    conn.commit();
                    conn.setAutoCommit(isAutoCommit);

                    m_connectionPool.free(conn);

                    synchronized(m_SQLThreadCount) {
                        m_SQLThreadCount.value--;
                    }


                } catch (SQLException ex) {
                   Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, "error writing batch job", ex);
                   while ((ex = ex.getNextException()) !=null)
                        Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, "next exception", ex);

                   //System.err.println("XiDB: problem when batching SQL results: " + ex.getMessage());
                   m_connectionPool.closeAllConnections();
                   System.exit(1);
                } 
            }
        };

        execSQL1.start();



        final String proteinSql =  m_proteinSql.toString();
        m_proteinSql.setLength(0);
//        m_sqlout.write(proteinSql + "\n");


        final String peptideSql = m_peptideSql.toString();
        m_peptideSql.setLength(0);
//        m_sqlout.write(peptideSql + "\n");


        final String hasProteinSql = m_hasProteinSql.toString();
        m_hasProteinSql.setLength(0);
//        m_sqlout.write(hasProteinSql + "\n");


        final String fragmentSql = m_fragmentSql.toString();
        m_fragmentSql.setLength(0);
//        m_sqlout.write(fragmentSql + "\n");


        // in parallel we can store the independent sequence informations (protein, peptide, fragment)
        Thread execSQL2 = new Thread() {
            @Override
            public void run() {
                try {
                    Connection conn = m_connectionPool.getConnection();
                    // start commiting
                    boolean isAutoCommit = m_conn.getAutoCommit();
                    conn.setAutoCommit(false);

                    java.sql.Statement stm = conn.createStatement();
//                    stm.execute(spectrumSql);
//                    stm.execute(SpectrumMatchSql);
//                    stm.execute(spectrum_peakSql);
                    stm.executeUpdate(proteinSql);
                    stm.executeUpdate(peptideSql);
//                    stm.execute(hasProteinSql);
//                    stm.execute(MatchedPeptideSql);
                    stm.executeUpdate(fragmentSql);
//                    stm.execute(spectrumPeakAnnotationSql);
//                    stm.execute(ClusterPeakSql);
//                    stm.execute(SpectrumMatchScoreSql);
        //            stm.executeBatch();
                    stm.close();

                    conn.commit();
                    conn.setAutoCommit(isAutoCommit);

                    m_connectionPool.free(conn);

                    synchronized(m_SQLThreadCount) {
                        m_SQLThreadCount.value--;
                    }


                } catch (SQLException ex) {
                   Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, "error writing batch job", ex);
                   while ((ex = ex.getNextException()) !=null)
                        Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, "next exception", ex);

                   //System.err.println("XiDB: problem when batching SQL results: " + ex.getMessage());
                   m_connectionPool.closeAllConnections();
                   System.exit(1);
                }
//                finally {
//                    m_ThreadMonitor.release();
//                }
            }
        };
        execSQL2.start();



        final String SpectrumMatchSql = m_SpectrumMatchSql.toString();
        m_SpectrumMatchSql.setLength(0);
//        m_sqlout.write(SpectrumMatchSql + "\n");

        final String MatchedPeptideSql = m_MatchedPeptideSql.toString();
        m_MatchedPeptideSql.setLength(0);
//        m_sqlout.write(MatchedPeptideSql + "\n");



        final String spectrumPeakAnnotationSql = m_spectrumPeakAnnotationSql.toString();
        m_spectrumPeakAnnotationSql.setLength(0);
//        m_sqlout.write(spectrumPeakAnnotationSql + "\n");


        final String ClusterPeakSql = m_ClusterPeakSql.toString();
        m_ClusterPeakSql.setLength(0);
//        m_sqlout.write(ClusterPeakSql + "\n");


        final String SpectrumMatchScoreSql = m_SpectrumMatchScoreSql.toString();
        m_SpectrumMatchScoreSql.setLength(0);
//        m_sqlout.write(SpectrumMatchScoreSql + "\n");





        while (execSQL1.isAlive())
            try {
                execSQL1.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, null, ex);
            }

        while (execSQL2.isAlive())
            try {
                execSQL2.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, null, ex);
            }

        // wait until these two threads are finished
//        System.err.println("execSQL1 = " + execSQL1.isAlive());
//        System.err.println("execSQL2 = " + execSQL2.isAlive());
        // free the semaphore - so we don't wait for it in the next cycle

        m_ThreadMonitor.acquireUninterruptibly();

        //System.err.println("permits available " + m_ThreadMonitor.availablePermits());

        // now we can store the dependent informations - AND don't need to wait for the return
        Thread execSQL3 = new Thread() {
            @Override
            public void run() {
                try {
                    Connection conn = m_connectionPool.getConnection();
                    // start commiting
                    boolean isAutoCommit = m_conn.getAutoCommit();
                    conn.setAutoCommit(false);

                    java.sql.Statement stm = conn.createStatement();
//                    stm.execute(spectrumSql);
                    stm.executeUpdate(SpectrumMatchSql);
//                    stm.execute(spectrum_peakSql);
//                    stm.execute(proteinSql);
//                    stm.execute(peptideSql);
                    stm.executeUpdate(hasProteinSql);
                    stm.executeUpdate(MatchedPeptideSql);
//                    stm.execute(fragmentSql);
                    stm.executeUpdate(spectrumPeakAnnotationSql);
                    stm.executeUpdate(ClusterPeakSql);
                    stm.executeUpdate(SpectrumMatchScoreSql);
        //            stm.executeBatch();
                    stm.close();

                    conn.commit();
                    conn.setAutoCommit(isAutoCommit);

                    m_connectionPool.free(conn);

                    synchronized(m_SQLThreadCount) {
                        m_SQLThreadCount.value--;
                    }


                } catch (SQLException ex) {
                   Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, "error writing batch job", ex);
                   while ((ex = ex.getNextException()) !=null)
                        Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, "next exception", ex);

                   //System.err.println("XiDB: problem when batching SQL results: " + ex.getMessage());
                   m_connectionPool.closeAllConnections();
                   System.exit(1);
                }
                finally {
//                    synchronized (m_semaphoreSynchronization) {
                        m_ThreadMonitor.release();
//                    }
                }
            }
        };
        execSQL3.run();

        Runtime r =Runtime.getRuntime();
        //System.err.println("free memory = " +r.freeMemory());

//        if (r.freeMemory()<50000000) {
//            System.gc();
            // System.err.println("free memory now = " +r.freeMemory());

//        }

//         mon_batch.stop();
    }// end method

    protected void init(int maxBatchSize, String score) {

    }



    @Override
    public void writeHeader() {
        // updating the search table to 'executing' if not already done - check
    }

    private IDs getIds(MatchedXlinkedPeptide match) {

        // will hold the start values of the ids
        IDs start_ids = new IDs();

        // will hold a count of all ids we want to return
        IdCounts id_counts = new IdCounts();

        // count of how many IDS we need
        if(match.getSpectrum().getID() == -1){
            ++id_counts.i_spectrum_ids;
        }

        id_counts.i_peak_ids = match.getSpectrum().getPeaks().size();
        id_counts.i_peak_annotations_ids = 0;
        id_counts.i_fragment_ids = 0;


         for (MatchedBaseFragment mbf : match.getMatchedFragments()) {
            // save fragments
            Fragment f = mbf.getBaseFragment();
            long f_id = f.getID();
            if (f_id == -1) {
                f.setID(-2);
                id_counts.i_fragment_ids++;
            }

            // save annotated_peak, same as above
            SpectraPeak sp = mbf.getBasePeak();
            // we can have null here if there are losses WITHOUT non-lossy fragments
            if(sp != null){
                id_counts.i_peak_annotations_ids++;
            }
            // Do the same for losses
            for (Loss l : mbf.getLosses().keySet()) {
                // same for loss fragments
                Fragment lf = l;
                long lf_id = lf.getID();
                if (lf_id ==-1) {
                    //saveFragment(lf, result_ids);
                    id_counts.i_fragment_ids++;
                    lf.setID(-2);
                }

                //SpectraPeak lsp = mbf.getLosses().get(lf);
                id_counts.i_peak_annotations_ids++;
                //savePeakAnnotation(lsp, lf.getID(), result_ids);

            }
        }


        // System.out.println("Fragments to store :"  + id_counts.i_fragment_ids);
//        System.out.println("Annotations to store: "  + id_counts.i_peak_annotations_ids++);


        int counts[] = countPeptideProteinIDs(match.getPeptides());
        id_counts.i_peptide_ids = counts[0];
        id_counts.i_protein_ids = counts[1];

        id_counts.i_peak_cluster_ids = 0;
        id_counts.i_peak_cluster_ids = match.getSpectrum().getIsotopeClusters().size();
//        System.out.println("cluster ids requested = " +  id_counts.i_peak_cluster_ids);
        

        
        try {
            // start commitg
            boolean isAutoCommit = m_conn.getAutoCommit();
            m_conn.setAutoCommit(false);



//            m_getIDs = m_conn.prepareStatement("SELECT spectrumid, peakid, pepid, protid, specmatchid, paid, fragid, pcid "

            m_getIDs.setLong(1, id_counts.i_spectrum_ids);
            m_getIDs.setLong(2, id_counts.i_peak_ids);
            m_getIDs.setLong(3, id_counts.i_peptide_ids);
            m_getIDs.setLong(4, id_counts.i_protein_ids);
            m_getIDs.setLong(5, id_counts.i_spectrum_match_ids);
            m_getIDs.setLong(6, id_counts.i_peak_annotations_ids);
            m_getIDs.setLong(7, id_counts.i_fragment_ids);
            m_getIDs.setLong(8, id_counts.i_peak_cluster_ids);



            ResultSet rs = m_getIDs.executeQuery();


            while(rs.next()){
                start_ids.i_spectrum_id = rs.getLong(1);
                start_ids.i_peak_id = rs.getLong(2);
                start_ids.i_peptide_id = rs.getLong(3);
                start_ids.i_protein_id = rs.getLong(4);
                start_ids.i_spectrum_match_id = rs.getLong(5);
                start_ids.i_peak_annotations_id = rs.getLong(6);
                start_ids.i_fragment_id = rs.getLong(7);
                start_ids.i_peak_cluster_id = rs.getLong(8);
            }
            // m_getIDs = m_conn.prepareStatement("SELECT spectrumid, peakid from reserve_ids(?, ?);");
            // Call the function once at the start of each spectrum
            // passing in 0 for the arguments you don't need every time you call this

            // m_getIDs.setInt(1, "1");

            // for peptide, check if they are -1 i.e they could be seen before

           //  for protein count check if they are -1 (i.e. not seen before ) and then increment the count

            // also replace checks for -1 (fragments only with < 0 because we set -2)

            // count fragments
   


            // end commit
            m_conn.commit();
            m_conn.setAutoCommit(isAutoCommit);
            
        } catch (SQLException ex) {
           Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, null, ex);
           System.err.println("XiDB: problem when acquiring IDs for storing results: " + ex.getMessage());
           m_connectionPool.closeAllConnections();
           System.exit(1);
            
        }



//        System.out.println("Cluster :" + (id_counts.i_peak_cluster_ids + start_ids.i_peak_cluster_id));

        return start_ids;

    }// end method getIds()


    @Override
    public synchronized void writeResult(MatchedXlinkedPeptide match) {


        ++results_processed;
        if (match.getMatchrank() == 1)
            top_results_processed++;
        
        sqlBatchCount++;
        if(sqlBatchCount > sqlBufferSize){ //sqlBufferSize/10){
//            executeCopy();
          executeSQL();
//            writeInserts();
            sqlBatchCount = 0;
        }

        // 0. Get the ids for saving results
//        mon0 = MonitorFactory.start("getIds()");
        IDs result_ids = getIds(match);
//        mon0.stop();

        // 1. Check spectrum info spectrum spectrum peak
        Spectra matched_spectrum = match.getSpectrum();

        if(matched_spectrum.getID() == -1){
               // We need to insert this spectrum AND it's peaks into the database, and set their ids once doing so
               // high level information
               // (acq_id, run_id, scan_number, elution_time_start, elution_time_end, notes)
//                mon1 = MonitorFactory.start("saveSpectrum()");
               saveSpectrum(matched_spectrum, result_ids);
//               mon1.stop();
        }

 
        // 2. Save spectrum_match info
//         mon2 = MonitorFactory.start("saveSpectrumMatch()");
        long spectrum_match_id = saveSpectrumMatch( match.getScore("match score"), matched_spectrum.getID(), result_ids);
//        mon2.stop();

        // 3. Save the protein/peptide sequences
//        mon3 = MonitorFactory.start("savePeptide1()");
        savePeptide(match.getPeptides()[0], spectrum_match_id, true, match.getLinkSites(match.getPeptides()[0]), result_ids);
//        mon3.stop();
//        mon4 = MonitorFactory.start("savePeptide2()");
        savePeptide(match.getPeptides()[1], spectrum_match_id, false, match.getLinkSites(match.getPeptides()[1]), result_ids);
//        mon4.stop();

        // 4. Fragments, peak annotations and clusters

//        mon5 = MonitorFactory.start("saveMatchedFragmentsAndPeaks()");
        saveMatchedFragmentsAndPeaks(match.getMatchedFragments(), result_ids);
//        mon5.stop();

//        mon6 = MonitorFactory.start("savePeakclusters()");
        savePeakclusters(matched_spectrum.getIsotopeClusters(), result_ids);
//        mon6.stop();

        // 5. Save Socres
//        mon7 = MonitorFactory.start("saveScoreInformation()");
        saveScoreInformation(match.getScores(), spectrum_match_id, result_ids);
//        mon7.stop();

//        System.out.println(" cluster  writen : " + result_ids.i_peak_cluster_id );

        // Done !!!!!!


    }// write result

    private void saveSpectrum(Spectra matched_spectrum, IDs result_ids){

//      try{
//         id                 | bigint                | not null default nextval('spectrum_id_seq'::regclass)
//         run_id             | integer               |
//         acq_id             | integer               |
//         scan_number        | integer               |
//         elution_time_start | character varying(50) |
//         elution_time_end   | character varying(50) |
//         notes              | text                  |
        matched_spectrum.setID(result_ids.i_spectrum_id++);
        addSpectrum(m_acq_id, matched_spectrum.getRunID(), matched_spectrum);
//        m_insertSpectrum.setInt(1, m_acq_id);
//        m_insertSpectrum.setInt(2,(int) matched_spectrum.getRunID());
//        m_insertSpectrum.setInt(3, matched_spectrum.getScanNumber());
//        m_insertSpectrum.setString(4, "" + matched_spectrum.getElutionTimeStart());
//        m_insertSpectrum.setString(5, "" + matched_spectrum.getElutionTimeEnd());
//        m_insertSpectrum.setLong(6, result_ids.i_spectrum_id);
//
////        ResultSet rs = m_insertSpectrum.executeQuery();
//        m_insertSpectrum.addBatch();
      //  rs.next();

        // Save the auto-generated spectrum id
//        matched_spectrum.setID(rs.getLong(1));
//        matched_spectrum.setID(result_ids.i_spectrum_id++);

        // Now save the spectrum peak information
//        (spectrum_id, mz, intensity)
//         spectrum_id | bigint  |
//         mz          | numeric |
//         intensity   | numeric |


        for(SpectraPeak sp: matched_spectrum.getPeaksArray()){
             sp.setID(result_ids.i_peak_id++);
             addSpectrumPeak(matched_spectrum, sp);
//             m_insertSpectrumPeak.setLong(1, matched_spectrum.getID());
//             m_insertSpectrumPeak.setString(2, String.valueOf(sp.getMZ()));
//             m_insertSpectrumPeak.setString(3, String.valueOf(sp.getIntensity()));
//             m_insertSpectrumPeak.setLong(4, result_ids.i_peak_id);
//
////             ResultSet peak_rs = m_insertSpectrumPeak.executeQuery();
////             peak_rs.next();
//             m_insertSpectrumPeak.addBatch();

        }


//         }catch(SQLException ex){
//             Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "XiDB: problem when writing results - spectrum info: " + ex.getMessage(),ex);
//             //System.err.println("XiDB: problem when writing results - spectrum info: " + ex.getMessage());
//             m_connectionPool.closeAllConnections();
//             System.exit(1);
//        }

    }//

    private int[] countPeptideProteinIDs(Peptide [] peptides){
        // Store peptide ids in ids[0] and protein ids in ids[1]
        int [] ids  = new int[2];
        ids[0] = 0;
        ids[1] = 0;

        for (Peptide p: peptides){
            if(p.getID() == -1){
                // increment pep id count
                ++ids[0];


                 for (Peptide.PeptidePositions pp: p.getPositions()) {
                    Sequence protein = pp.base;
                    if(protein.getID() == -1){++ids[1];}

                 }
            }// end if we haven't seen a peptide

        }// end for

        return ids;
    }//

    private void savePeptide(Peptide peptide, long match_id, boolean alpha, int[] linkSites, IDs result_ids) {

        // if this is the first time you see a peptide, then save it to the DB, and set the ID
        // Likewise do the same with the Protein
//        try {
            if(peptide.getID() == -1){

    //             id       | bigint  | not null default nextval('peptide_id_seq'::regclass)
    //             sequence | text    |
    //             mass     | numeric |
                peptide.setID(result_ids.i_peptide_id++);
                addPeptide(peptide);
//                m_insertPeptide.setString(1, peptide.toString());
//                m_insertPeptide.setString(2, String.valueOf(peptide.getMass()));
//                m_insertPeptide.setLong(3, result_ids.i_peptide_id);
//
////                ResultSet pep_rs = m_insertPeptide.executeQuery();
////                // set the ID of the peptide
////                pep_rs.next();
//                m_insertPeptide.addBatch();
////                peptide.setID(pep_rs.getLong(1));
//                peptide.setID(result_ids.i_peptide_id++);


                // Now check if there is a problem with the Proteins i.e. have we seen them before

                int first = 0;
                for (Peptide.PeptidePositions pp: peptide.getPositions()) {
                    ++first;
                    Sequence protein = pp.base;
                    if(protein.getID() == -1){

        //                 id       | bigint | not null default nextval('protein_id_seq'::regclass)
        //                 name     | text   |
        //                 sequence | text   |
                            protein.setID(result_ids.i_protein_id++);
                            addProtein(protein);
//                            m_insertProtein.setString(1, protein.getFastaHeader());
//                            m_insertProtein.setString(2, protein.toString());
//                            m_insertProtein.setLong(3, result_ids.i_protein_id);
////                            ResultSet prot_rs = m_insertProtein.executeQuery();
////                            prot_rs.next();
//                            m_insertProtein.addBatch();
//
////                            protein.setID(prot_rs.getLong(1));
//                            protein.setID(result_ids.i_protein_id++);



                            // Now insert into has_protein
//                             peptide_id       | bigint  | not null
//                             protein_id       | bigint  | not null
//                             peptide_position | integer | not null
//                             display_site     | boolean |
                    }

                   // "INSERT INTO has_protein(peptide_id, protein_id, peptide_position, display_site)
//                    m_insertHasProtein.setLong(1, peptide.getID());
//                    m_insertHasProtein.setLong(2, protein.getID());
//                    m_insertHasProtein.setInt(3, pp.start);
//                    if (first == 1){m_insertHasProtein.setBoolean(4, true);}
//                    else{m_insertHasProtein.setBoolean(4, false);}

//                    System.out.println("peptide id = " + peptide.getID());
//                    System.out.println("PROTEIN id = " + protein.getID());
//                    System.out.println("PEP start pos = " + pp.start);

//                    m_insertHasProtein.addBatch();

                }
                addHasProtein(peptide);





            }// end if peptide

            // Save matched_peptide information

            // For each link position , save the match in matched peptides
            // initially this we are forwardede only 1, in future more will come
            for(int i = 0; i < linkSites.length; i++){
              addMatchedPeptide(peptide, match_id, (alpha?alpha_id:beta_id), linkSites[i], (i == 0));
//              m_matched_peptide.setLong(1, peptide.getID());
//              m_matched_peptide.setLong(2, match_id);
//
//              if(alpha){ m_matched_peptide.setInt(3, alpha_id);}
//              else{m_matched_peptide.setInt(3, beta_id);}
//
//              m_matched_peptide.setInt(4, linkSites[i]);
//
//              boolean display_positon = (i == 0) ? true : false;
//              m_matched_peptide.setBoolean(5, display_positon);
//
//              m_matched_peptide.addBatch();
            }// end for



//        } catch (SQLException ex) {
//             Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "XiDB: problem when writing results - peptide/protein info: ", ex);
//             //System.err.println("XiDB: problem when writing results - peptide/protein info: " + ex.getMessage());
//             m_connectionPool.closeAllConnections();
//             System.exit(1);
//        }
    }// end method

    @Override
    public int getResultCount() {
        return results_processed;
    }

    @Override
    public int getTopResultCount() {
        return top_results_processed;
    }
    
    @Override
    public void setFreeMatch(boolean doFree) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void finished() {

        try {
            if (sqlBatchCount >0)

//                executeCopy();
                executeSQL();
//                  writeInserts();
         
            try {
                m_sqlout.close();
            } catch (IOException ex) {
                Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, null, ex);
            }

            // our search is done
            m_search_complete.setInt(1, m_search_id);
            m_search_complete.executeUpdate();


            // runtime stats
            System.out.println("XiDBWriterMultiInsertSql - Total results: " + getResultCount() + "\n-------------");
            class M implements Comparable{
                String label;
                double avg;

                public M(String l, double a){
                    this.label = l;
                    this.avg = a;
                }

                @Override
                public int compareTo(Object o) {
                   if(o instanceof M){
                    if(this.avg < ((M)o).avg)
                        return 1;
                    else if(this.avg > ((M)o).avg)
                        return -1;
                   }
                   return 0;
                }

            }
//            mon0.getLabel();
//            M [] avg_vals = {new M(mon0.getLabel(), mon0.getAvg()),
//                            new M(mon1.getLabel(), mon1.getAvg()),
//                            new M(mon2.getLabel(), mon2.getAvg()),
//                            new M(mon3.getLabel(), mon3.getAvg()),
//                            new M(mon4.getLabel(), mon4.getAvg()),
//                            new M(mon5.getLabel(), mon5.getAvg()),
//                            new M(mon6.getLabel(), mon6.getAvg()),
//                            new M(mon7.getLabel(), mon7.getAvg()),
//                            new M(mon_batch.getLabel(), mon_batch.getAvg())
//            };


//            Arrays.sort(avg_vals);

//            for(M monitor: avg_vals){
//                System.out.println(monitor.label + " avg: " + monitor.avg);
//            }

//            System.out.println("---------------\n" + mon0);
//            System.out.println(mon1);
//            System.out.println(mon2);
//            System.out.println(mon3);
//            System.out.println(mon4);
//            System.out.println(mon5);
//            System.out.println(mon6);
//            System.out.println(mon7);
//            System.out.println(mon_batch);




        } catch (SQLException ex) {
            System.err.println("XiDB: problem when writing results - final search table: " + ex.getMessage());
            m_connectionPool.closeAllConnections();
            System.exit(1);
        }

        m_connectionPool.closeAllConnections();
        super.finished();
    }

    private long saveSpectrumMatch(double match_score, long spec_id, IDs result_ids) {
        long spec_match_id = result_ids.i_spectrum_match_id++;
//        try{
            addSpectrumMatch(m_search_id, match_score, spec_id, spec_match_id);
//            m_spectrum_match.setInt(1, this.m_search_id);
//            m_spectrum_match.setString(2, String.valueOf(match_score));
//            m_spectrum_match.setLong(3, spec_id);
//            m_spectrum_match.setLong(4, result_ids.i_spectrum_match_id);
//
//
//
////            ResultSet rs = m_spectrum_match.executeQuery();
////            while(rs.next()){spec_match_id = rs.getLong(1);}
//
//            m_spectrum_match.addBatch();
//            spec_match_id = result_ids.i_spectrum_match_id;
//            result_ids.i_spectrum_match_id++;

//        }catch(SQLException ex){
//            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "XiDB: problem when writing results - spectrum match: " + ex.getMessage(),ex);
////            System.err.println("XiDB: problem when writing results - spectrum match: " + ex.getMessage());
//            m_connectionPool.closeAllConnections();
//            System.exit(1);
//        }


        return spec_match_id;
    }


    private void saveScoreInformation(HashMap<String, Double> scores, long spec_match_id, IDs result_ids) {
        try {
            // First check if we've encouneterd the first score we come accross in the spectra
            for(String name: scores.keySet()){
               int score_id = -1;
               int display_order = -1;
               String description = "";

               if( !m_scores.containsKey(name) ){

                    // check if in DB, get info insert into Hashmap, otherwise insert into DB
                   // and into Hashmap
                    m_check_score.setString(1, name);

                    // Check if in DB
                    ResultSet rs = m_check_score.executeQuery();
                    while(rs.next()){
                        score_id = rs.getInt(1);
                        display_order = rs.getInt(2);
                        description = rs.getString(3);
                    }

                    // If not then insert and update the hashmap
                    if(score_id == -1){
                        // Not in DB insert first
                        m_insert_score.setString(1, name);
                        // TODO note: these two below  should get proper values from the search HashMap scores
                        m_insert_score.setInt(2, -1);
                        m_insert_score.setString(3, null);

                        // set the score id
                        //System.out.println("score name " + name);
                        ResultSet rs2 = m_insert_score.executeQuery();
                        while(rs2.next()){score_id = rs2.getInt(1);}
                    }

                    m_scores.put(name, score_id);
                   
               }else{
                   score_id = m_scores.get(name);
               }

               // Now save the link table
                // spectrum_match_id | bigint  | not null
                // score_id          | integer | not null
                // score             | numeric |
               addSpectrumMatchScore(spec_match_id, score_id, scores.get(name));
//               m_spectrum_match_score.setLong(1, spec_match_id);
//               m_spectrum_match_score.setInt(2, score_id);
//
//
//               m_spectrum_match_score.setString(3, String.valueOf(scores.get(name)));
//
//
////               m_spectrum_match_score.executeUpdate();
//               m_spectrum_match_score.addBatch();


            }// end for

           }catch (SQLException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "XiDB: problem when writing results - spectraScore: " + ex.getMessage(),ex);
                //System.err.println("XiDB: problem when writing results - spectraScore: " + ex.getMessage());
                m_connectionPool.closeAllConnections();
                System.exit(1);
           }
            
    }// end saveScoreInformation()


    private void savePeakclusters(SortedLinkedList<SpectraPeakCluster> peakClusters, IDs result_ids) {
        //cluster_peak;
//                         Table "public.cluster_peak"
// Column  |  Type  |                         Modifiers
//---------+--------+-----------------------------------------------------------
// id      | bigint | not null default nextval('cluster_peak_id_seq'::regclass)
// peak_id | bigint | not null
//        try {

            for(SpectraPeakCluster spc: peakClusters){
//                    ResultSet rs = m_clusterPeak_id.executeQuery();
//                    rs.next();
                    long cluster_id = result_ids.i_peak_cluster_id++;
                    for (SpectraPeak sp : spc) {
                        //System.out.println(cluster_id + ", " + sp.getID() + "," +sp.getMZ());
                        addClusterPeak(cluster_id, sp);

//                        m_clusterPeak.setLong(1, cluster_id);
//                        m_clusterPeak.setLong(2, sp.getID());
//
//                        m_clusterPeak.addBatch();
                    }

            }


//        } catch (SQLException ex) {
//                System.err.println("XiDB: problem when writing results - spectraPeakCluster: " + ex.getMessage());
//                m_connectionPool.closeAllConnections();
//                System.exit(1);
//        }

    }

    private void saveMatchedFragmentsAndPeaks(MatchedFragmentCollection fragments, IDs result_ids) {

//        --CREATE table fragment(
//--		id			BIGSERIAL,
//--		peptide_id	BIGINT,
//--		sequence	TEXT,
//--		mass		NUMERIC,
//--		PRIMARY KEY(id),
//--		FOREIGN KEY(peptide_id) REFERENCES peptide(id)

      //  int fragsCount = 0;
       // int annoCount = 0;
        for (MatchedBaseFragment mbf : fragments) {
            // save fragments
            Fragment f = mbf.getBaseFragment();
            long f_id = f.getID();
            if (f_id < 0) {
                saveFragment(f, result_ids);
               // fragsCount++;
            }

            // save annotated_peak, same as above
            SpectraPeak sp = mbf.getBasePeak();
            // we can have null here if there are losses WITHOUT non-lossy fragments
            if(sp != null){
               // annoCount++;
                savePeakAnnotation(sp, f, result_ids);
            }
            // Do the same for losses
            for (Loss l : mbf.getLosses().keySet()) {
                // same for loss fragments
                Fragment lf = l;
                long lf_id = lf.getID();
                if (lf_id < 0) {
                    saveFragment(lf, result_ids);
                   // fragsCount++;
                }

                SpectraPeak lsp = mbf.getLosses().get(l);
               // annoCount++;
                savePeakAnnotation(lsp, lf, result_ids);
                
            }
        }
//        System.out.println("fragments for match : " + fragsCount );
//        System.out.println("annontations for match: " + annoCount );


        

    }

    private void saveFragment(Fragment f, IDs result_ids){
         f.setID(result_ids.i_fragment_id++);
         addFragment(f);
//         try {
//                addFragment(f);
//                m_insertFragment.setLong(1, f.getPeptide().getID());
//                m_insertFragment.setString(2, f.toString());
//                m_insertFragment.setString(3, String.valueOf(f.getNeutralMass()));
//                //m_insertFragment.setDouble(3, f.getNeutralMass());
//                m_insertFragment.setLong(4, result_ids.i_fragment_id);
//
//
//                // set the fragment id
////                ResultSet rs = m_insertFragment.executeQuery();
////                while (rs.next()) {
////                    f.setID(rs.getLong(1));
////                }
//                m_insertFragment.addBatch();
//                f.setID(result_ids.i_fragment_id++);
//
//
//
//
//            } catch (SQLException ex) {
//                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "XiDB: problem when writing results - fragment info: " + ex.getMessage(),ex);
////                System.err.println("XiDB: problem when writing results - fragment info: " + ex.getMessage());
//                m_connectionPool.closeAllConnections();
//                System.exit(1);
//            }
    }

    private void savePeakAnnotation(SpectraPeak sp, Fragment f, IDs result_ids){
//    private void savePeakAnnotation(SpectraPeak sp, long f_id, IDs result_ids){
        addSpectrumPeakAnnotation(sp, f, result_ids.i_peak_annotations_id++);
//         try {
//
//        //
//        // id                | bigint  | not null default nextval('spectrum_peak_annotation_id_seq'::regclass)
//        // peak_id           | bigint  |
//        // fragment_id       | bigint  |
//        // manual_annotation | boolean |
//        // false_annotation  | boolean |
//        // notes             | text    |
//
//
//             m_peakAnnotation.setLong(1, sp.getID());
//             m_peakAnnotation.setLong(2, f_id);
//             m_peakAnnotation.setBoolean(3, false);
//             m_peakAnnotation.setBoolean(4, false);
//             m_peakAnnotation.setString(5, "");
//             m_peakAnnotation.setLong(6, result_ids.i_peak_annotations_id);
//
//         // set the peakAnnotationId id
////            ResultSet rs = m_peakAnnotation.executeQuery();
////            while (rs.next()) {
////                System.out.println("return peak_annotation_id = " + rs.getLong(1));
////                sp.setID(rs.getLong(1));
////            }
//
//             m_peakAnnotation.addBatch();
//             //sp.setID(result_ids.i_peak_annotations_id++);
//
//
//            } catch (SQLException ex) {
//                System.err.println("XiDB: problem when writing results - spectrum peak annotation info: " + ex.getMessage());
//                m_connectionPool.closeAllConnections();
//                System.exit(1);
//            }
    }


    
    public synchronized  void flush() {
        if (sqlBatchCount>0) {
            executeSQL();
            sqlBatchCount = 0;
        }
    }



}
