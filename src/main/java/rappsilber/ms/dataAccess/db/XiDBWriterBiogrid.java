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

/**
 *
 * @author stahir
 */
//import com.jamonapi.Monitor;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.postgresql.PGConnection;
import rappsilber.config.RunConfig;
import rappsilber.db.ConnectionPool;
import rappsilber.ms.dataAccess.output.AbstractResultWriter;
import rappsilber.ms.dataAccess.output.BufferedResultWriter;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.Sequence;
import rappsilber.ms.sequence.fasta.FastaHeader;
import rappsilber.ms.sequence.ions.CrosslinkedFragment;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.loss.Loss.LossCount;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.SpectraPeakCluster;
import rappsilber.ms.spectra.annotation.SpectraPeakAnnotation;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.ms.statistics.utils.UpdateableInteger;
import rappsilber.utils.CountOccurence;
import rappsilber.utils.SortedLinkedList;

/**
 *
 * @author stahir
 */
public class XiDBWriterBiogrid extends AbstractResultWriter {

    private int m_search_id;
    private RunConfig m_config;
    // these values are the same throughout the class so just set there values on init
    private int alpha_id;
    private int beta_id;
    protected ConnectionPool m_connectionPool;
    protected Connection m_conn;
    private final UpdateableInteger m_SQLThreadCount = new UpdateableInteger(0);
    private final java.util.concurrent.Semaphore m_ThreadMonitor = new java.util.concurrent.Semaphore(4);
    private final Object m_semaphoreSynchronization = new Object();
    // TODO: that is just a dirty hack, to fight an error I have no time to fix correctly just now
    private HashMap<Long, HashSet<Long>> m_BF_ClusterPeakAssignments = new HashMap<Long, HashSet<Long>>();
    private PreparedStatement m_check_score;
    private PreparedStatement m_updateDB;
    private PreparedStatement m_insert_score;

    private PreparedStatement m_search_complete;
    private PreparedStatement m_match_type;
    protected PreparedStatement m_getIDs;
    private HashMap<String, Integer> m_scores;



    // ArrayList for materialized view EXPORT
    private HashMap mat_export_entries = new HashMap(41);
    private int sqlBatchCount = 0;
    private int sqlBufferSize = 0;
    private int results_processed = 0;
    private int top_results_processed = 0;
    private IDs ids;


    // ID counts requested per spectrum - updated on each store


    // holds the start Ids for each result to save
    protected class IDs {


        private long last_reserved_spectrum_id = -1;
        private long last_reserved_peak_id = -1;
        private long last_reserved_peak_annotations_id = -1;
        private long last_reserved_fragment_id = -1;
        private long last_reserved_peptide_id = -1;
        private long last_reserved_protein_id = -1;
        private long last_reserved_peak_cluster_id = -1;
        private long last_reserved_spectrum_match_id = -1;
        
        private long next_spectrum_id = 0;
        private long next_peak_id = 0;
        private long next_peak_annotations_id = 0;
        private long next_fragment_id = 0;
        private long next_peptide_id = 0;
        private long next_protein_id = 0;
        private long next_peak_cluster_id = 0;
        private long next_spectrum_match_id = 0;

        private long increment_spectrum_id = 2000;
        private long increment_peak_id = increment_spectrum_id*50;
        private long increment_peak_annotations_id = increment_spectrum_id*200;
        private long increment_fragment_id = increment_spectrum_id*30;
        private long increment_peak_cluster_id = increment_spectrum_id*20;
        private long increment_spectrum_match_id = increment_spectrum_id*5;
        private long increment_protein_id = 10;
        private long increment_peptide_id = increment_protein_id*40;
        
        Connection dbconection;
        
        
        public IDs() {}

        public void reserve_IDs(long spectrum_ids, 
                        long peak_ids, 
                        long peak_annotations_ids, 
                        long fragment_ids,
                        long peptide_ids,
                        long protein_ids,
                        long peak_cluster_ids,
                        long spectrum_match_ids) {
            try {
                m_getIDs.setLong(1, spectrum_ids);
                m_getIDs.setLong(2, peak_ids);
                m_getIDs.setLong(3, peptide_ids);
                m_getIDs.setLong(4, protein_ids);
                m_getIDs.setLong(5, spectrum_match_ids);
                m_getIDs.setLong(6, peak_annotations_ids);
                m_getIDs.setLong(7, fragment_ids);        
                m_getIDs.setLong(8, peak_cluster_ids);
                ResultSet rs = m_getIDs.executeQuery();
                while (rs.next()) {
                    if (spectrum_ids >0) {
                        next_spectrum_id = rs.getLong(1);
                        last_reserved_spectrum_id = next_spectrum_id +spectrum_ids - 1; 
                    }
                    if (peak_ids >0) {
                        next_peak_id = rs.getLong(2);
                        last_reserved_peak_id = next_peak_id + peak_ids - 1;
                    }
                    if (peptide_ids > 0 ) { 
                        next_peptide_id = rs.getLong(3);
                        last_reserved_peptide_id = next_peptide_id + peptide_ids - 1;
                    }
                    if (protein_ids > 0) {
                        next_protein_id = rs.getLong(4);
                        last_reserved_protein_id = next_protein_id + protein_ids - 1;
                    }
                    if (spectrum_match_ids > 0) {
                        next_spectrum_match_id = rs.getLong(5);
                        last_reserved_spectrum_match_id = next_spectrum_match_id + spectrum_match_ids - 1;
                    }
                    if (peak_annotations_ids > 0) {
                        next_peak_annotations_id = rs.getLong(6);
                        last_reserved_peak_annotations_id = next_peak_annotations_id + peak_annotations_ids - 1;
                    }
                    if (fragment_ids > 0) {
                        next_fragment_id = rs.getLong(7);
                        last_reserved_fragment_id = next_fragment_id + fragment_ids - 1;
                    }
                    if (peak_cluster_ids > 0) {
                        next_peak_cluster_id = rs.getLong(8);                
                        last_reserved_peak_cluster_id = next_peak_cluster_id + peak_cluster_ids - 1;
                    }
                }
                    
            } catch (SQLException ex) {
                Logger.getLogger(XiDBWriterBiogrid.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        

        public long nextSpectrumId() {
            if (next_spectrum_id <= last_reserved_spectrum_id)
                return next_spectrum_id ++;
            reserve_IDs(increment_spectrum_id, 0, 0, 0, 0, 0, 0, 0);
            return next_spectrum_id ++;
        }

        public long nextPeakId() {
            if (next_peak_id <= last_reserved_peak_id)
                return next_peak_id ++;
            reserve_IDs(0, increment_peak_id, 0, 0, 0, 0, 0, 0);
            return next_peak_id ++;
        }
        
        public long nextPeptideId() {
            if (next_peptide_id <= last_reserved_peptide_id)
                return next_peptide_id ++;
            reserve_IDs(0, 0, 0, 0, increment_peptide_id, 0, 0, 0);
            return next_peptide_id ++;
        }

        public long nextProteinId() {
            if (next_protein_id <= last_reserved_protein_id)
                return next_protein_id ++;
            reserve_IDs(0, 0, 0, 0, 0, increment_protein_id, 0, 0);
            return next_protein_id ++;
        }

        public long nextSpectrumMatchId() {
            if (next_spectrum_match_id <= last_reserved_spectrum_match_id)
                return next_spectrum_match_id ++;
            reserve_IDs(0, 0, 0, 0, 0, 0, 0, increment_spectrum_match_id);
            return next_spectrum_match_id ++;
        }

        public long nextPeakAnnotationsId() {
            if (next_peak_annotations_id <= last_reserved_peak_annotations_id)
                return next_peak_annotations_id ++;
            reserve_IDs(0, 0, increment_peak_annotations_id, 0, 0, 0, 0, 0);
            return next_peak_annotations_id ++;
        }

        public long nextFragmentId() {
            if (next_fragment_id <= last_reserved_fragment_id)
                return next_fragment_id ++;
            reserve_IDs(0, 0, 0, increment_fragment_id, 0, 0, 0, 0);
            return next_fragment_id ++;
        }
        
        public long nextPeakClusterId() {
            if (next_peak_cluster_id <= last_reserved_peak_cluster_id)
                return next_peak_cluster_id ++;
            reserve_IDs(0, 0, 0, 0, 0, 0, increment_peak_cluster_id, 0);
            return next_peak_cluster_id ++;
        }
        
        
    }

    private StringBuffer m_spectrumSql = new StringBuffer();
    private StringBuffer m_copySpectrum = new StringBuffer();

    public void addSpectrum(long acq_id, long run_id, Spectra s) {

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
        m_copySpectrum.append(s.getID());
        m_copySpectrum.append(",");
        m_copySpectrum.append(s.getPrecoursorChargeAlternatives().length <= 1 ? s.getPrecurserCharge() : -1);
        m_copySpectrum.append(",");
        m_copySpectrum.append(s.getPrecurserIntensity());
        m_copySpectrum.append(",");
        m_copySpectrum.append(s.getPrecurserMZ());
        m_copySpectrum.append(",");
        m_copySpectrum.append(m_search_id);
        m_copySpectrum.append("\n");

    }
    private StringBuffer m_spectrum_peakSql = new StringBuffer();

    public void addSpectrumPeak(Spectra s, SpectraPeak sp) {

        m_spectrum_peakSql.append(s.getID());
        m_spectrum_peakSql.append(",");
        m_spectrum_peakSql.append(sp.getMZ());
        m_spectrum_peakSql.append(",");
        m_spectrum_peakSql.append(sp.getIntensity());
        m_spectrum_peakSql.append(",");
        m_spectrum_peakSql.append(sp.getID());
        m_spectrum_peakSql.append(",");
        m_spectrum_peakSql.append(sp.hasAnnotation(SpectraPeakAnnotation.isotop));
        m_spectrum_peakSql.append(",");
        m_spectrum_peakSql.append(sp.hasAnnotation(SpectraPeakAnnotation.monoisotop));
        m_spectrum_peakSql.append("\n");


    }
    private StringBuffer m_peptideSql = new StringBuffer();

    public void addPeptide(Peptide p) {

        m_peptideSql.append("\"" + p.toString().replace("\"", "\"\"") + "\"");
        // m_peptideSql.append("',");
        m_peptideSql.append(",");
        m_peptideSql.append(p.getMass());
        m_peptideSql.append(",");
        m_peptideSql.append(p.getID());
        m_peptideSql.append(",");
        m_peptideSql.append(p.length());
        m_peptideSql.append(",");
        m_peptideSql.append(m_search_id);
        m_peptideSql.append("\n");
    }
    private StringBuffer m_proteinSql = new StringBuffer();

    public void addProtein(Sequence p) {
//                 postgres_con.getCopyAPI().copyIn(
//                        "COPY protein(header, name, accession_number, description, sequence, id, is_decoy, protein_length) " +
//                        "FROM STDIN WITH CSV", protis);

        String x = "";

        if (p.isDecoy()) {
            x = "DECOY";
        } else if (p.getFastaHeader() != null) {
            x = p.getFastaHeader().replace("\"", "\"\"").replace(",", " ");
        }//.replace("\\","\\\\").replace(",","\\,");
        FastaHeader fh = p.getSplitFastaHeader();
        //System.out.println("Fasta Header :" + x);

        m_proteinSql.append("\"");
        m_proteinSql.append(x);
        m_proteinSql.append("\",\"");
        m_proteinSql.append(fh.getName().replace("'", " ").replace(",", " "));
        m_proteinSql.append("\",\"");
        m_proteinSql.append(fh.getAccession().replace("'", " ").replace(",", " "));
        m_proteinSql.append("\",\"");
        m_proteinSql.append(fh.getDescription().replace("\"", "\"\"").replace(",", " "));
//        m_proteinSql.append("',");
        m_proteinSql.append("\",\"");
        m_proteinSql.append(p.toString().replace("\"", "\"\""));
        m_proteinSql.append("\",");
        m_proteinSql.append(p.getID());
        m_proteinSql.append(",");
        m_proteinSql.append(p.isDecoy());
        m_proteinSql.append(",");
        m_proteinSql.append(p.length());
        m_proteinSql.append(",");
        m_proteinSql.append(m_search_id);
        m_proteinSql.append("\n");

    }
    private StringBuffer m_hasProteinSql = new StringBuffer();

    public void addHasProtein(Peptide p) {
        long pepid = p.getID();
        HashMap<Long, HashSet<Integer>> postions = new HashMap<Long, HashSet<Integer>>();

        boolean first = true;
        for (Peptide.PeptidePositions pp : p.getPositions()) {
            Long protID = pp.base.getID();
            Integer pepStart = pp.start;
            HashSet<Integer> protPos = postions.get(pp.base.getID());
            if (protPos == null) {
                protPos = new HashSet<Integer>();
                postions.put(pp.base.getID(), protPos);
            } else if (protPos.contains(pepStart)) {
                continue;
            }



            m_hasProteinSql.append(pepid);
            m_hasProteinSql.append(",");
            m_hasProteinSql.append(protID);
            m_hasProteinSql.append(",");
            m_hasProteinSql.append(pepStart);
            m_hasProteinSql.append(",");
            if (first) {
                m_hasProteinSql.append("true");
                first = false;
            } else {
                m_hasProteinSql.append("false");
            }
            m_hasProteinSql.append(",");
            m_hasProteinSql.append(m_search_id);
            m_hasProteinSql.append("\n");

        }

    }

   
    private StringBuffer m_SpectrumMatchSql = new StringBuffer();

    public void addSpectrumMatch(long searchID, double score, long spectrumID, long id, boolean is_decoy, MatchedXlinkedPeptide match) {
        m_SpectrumMatchSql.append(searchID);
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append(score);
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append(spectrumID);
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append(id);
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append(is_decoy);
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append(match.getMatchrank());
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append(match.isValidated());
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append(match.getSpectrum().getPrecurserCharge());
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append(match.getCalcMass());
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append(match.getMatchrank() == 1 ? true : false);
        m_SpectrumMatchSql.append("\n");

    }
    private StringBuffer m_MatchedPeptideSql = new StringBuffer();

    public void addMatchedPeptide(Peptide p, long matchid, long matchtype, long link_position, boolean display_positon, Integer crosslinker_id, Integer crosslinker_number) {
        m_MatchedPeptideSql.append(p.getID());
        m_MatchedPeptideSql.append(",");
        m_MatchedPeptideSql.append(matchid);
        m_MatchedPeptideSql.append(",");
        m_MatchedPeptideSql.append(matchtype);
        m_MatchedPeptideSql.append(",");
        m_MatchedPeptideSql.append(link_position);
        m_MatchedPeptideSql.append(",");
        m_MatchedPeptideSql.append(display_positon);
        m_MatchedPeptideSql.append(",");
        m_MatchedPeptideSql.append(crosslinker_id == null ? "" : crosslinker_id);
        m_MatchedPeptideSql.append(",");
        m_MatchedPeptideSql.append(crosslinker_number == null ? "" : crosslinker_number);
        m_MatchedPeptideSql.append(",");
        m_MatchedPeptideSql.append(m_search_id);
        m_MatchedPeptideSql.append("\n");
    }

    public XiDBWriterBiogrid(RunConfig config, ConnectionPool cp, int searchID) {
        BufferedResultWriter.m_clearAnnotationsOnBuffer=true;

        try {
            m_config = config;
            sqlBufferSize = Integer.parseInt((String) m_config.retrieveObject("SQLBUFFER")); // after reading how many spectra do we batch

            m_connectionPool = cp;
            m_search_id = searchID;
            m_conn = m_connectionPool.getConnection();
            ids = new IDs();

            // will hold scores
            m_scores = new HashMap<String, Integer>();



            // score related information
            m_check_score = m_conn.prepareStatement("SELECT id, display_order, description FROM score WHERE name = ?");
            m_search_complete = m_conn.prepareStatement("UPDATE search "
                    + "SET is_executing = 'false', status = 'completed', completed = 'true', percent_complete = 100 "
                    + "WHERE id = ?; ");
            m_insert_score = m_conn.prepareStatement(
                    "INSERT INTO score(name, display_order, description) "
                    + "VALUES(?,?,?) RETURNING id");


            m_match_type = m_conn.prepareStatement("SELECT id FROM match_type WHERE name = ?");


            // Used to get IDs add pass in 0
            m_getIDs = m_connectionPool.getConnection().prepareStatement("SELECT spectrumid, peakid, pepid, protid, specmatchid, paid, fragid, pcid "
                    + "FROM reserve_ids(?,?,?,?,?,?,?,?);");


            // Just get this value once and set them in the class
            alpha_id = -1;
            beta_id = -1;
            m_match_type.setString(1, "alpha");
            ResultSet rs = m_match_type.executeQuery();
            while (rs.next()) {
                alpha_id = rs.getInt(1);
            }

            m_match_type.setString(1, "beta");
            rs = m_match_type.executeQuery();
            while (rs.next()) {
                beta_id = rs.getInt(1);
            }
            rs.close();

        } catch (SQLException ex) {
            System.err.println("XiDB: problem when setting up XiDBWriter: " + ex.getMessage());
            m_connectionPool.closeAllConnections();
            System.exit(1);
        }




    }

    private void executeCopy() {

        try {
            PGConnection postgres_con = null;
            Connection con = null;
            try {
                // Cast to a postgres connection
                con = m_connectionPool.getConnection();
                postgres_con = (PGConnection) con;
            } catch (SQLException ex) {
                Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }

            Connection conExportMat = null;
            try {
                // Cast to a postgres connection
                conExportMat = m_connectionPool.getConnection();
            } catch (SQLException ex) {
                Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
            final PGConnection postgresConExportMat = (PGConnection) conExportMat;

            Connection conSpecViewerMat = null;
            try {
                // Cast to a postgres connection
                conSpecViewerMat = m_connectionPool.getConnection();
            } catch (SQLException ex) {
                Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
            final PGConnection postgresConSpecViewerMat = (PGConnection) conSpecViewerMat;

            final CyclicBarrier waitSync = new CyclicBarrier(3);




            Runnable runExportMaterialized = new Runnable() {
                public void run() {
                    //materialized export entry
                    String matExpCopy = m_v_export_mat_SQL.toString();
                    byte mat_expByte[] = matExpCopy.getBytes();
                    InputStream exp_mat_is = new ByteArrayInputStream(mat_expByte);
                    m_v_export_mat_SQL.setLength(0);
                    try {

                        postgresConExportMat.getCopyAPI().copyIn("COPY v_export_materialized( "
                                + "search_id, spectrum_match_id, spectrum_id, autovalidated, rank, run_name, scan_number, "
                                + "match_score, total_fragment_matches, fragment_coverage, delta, peptide1_coverage, peptide2_coverage, "
                                + "spectrum_peaks_coverage, spectrum_intensity_coverage, spectrum_quality_score, peptide1_id, peptide1, "
                                + "display_protein1, peptide_position1, site_count1, protein_count1, peptide2_id, peptide2, display_protein2, "
                                + "peptide_position2, site_count2, protein_count2, pep1_link_pos, pep2_link_pos, crosslinker, is_decoy, "
                                + "precursor_charge, precursor_intensity, precursor_mz, exp_mass, calc_mass, error, dynamic_rank, "
                                + "display_protein1_id, display_protein1_length, display_protein2_id, display_protein2_length, peptide1_length, peptide2_length) "
                                + "FROM STDIN WITH CSV", exp_mat_is);

                    } catch (SQLException ex) {
                        String message = "error writing v_export_materialized";
                        Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, message, ex);
                        PrintWriter pw;
                        try {
                            pw = new PrintWriter(new FileOutputStream("/tmp/XiCopySqlError.csv", true));
                            pw.println("\n------------------------------------------------\n" + new Date() + " " + message);
                            ex.printStackTrace(pw);
                            pw.println("->");
                            pw.println(matExpCopy);
                        } catch (FileNotFoundException ex1) {
                            Logger.getLogger(XiDBWriterBiogrid.class.getName()).log(Level.SEVERE, null, ex1);
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(XiDBWriterBiogrid.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    try {
                        waitSync.await();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(XiDBWriterBiogrid.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (BrokenBarrierException ex) {
                        Logger.getLogger(XiDBWriterBiogrid.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            };
            
            new Thread(runExportMaterialized).start();

            Runnable runSpecViewer = new Runnable() {
                public void run() {
                    // materialized spectrum view
                    final String matSpecViewCopy = m_v_spectra_view_ad.toString();
                    byte mat_specByte[] = matSpecViewCopy.getBytes();
                    InputStream exp_spec_is = new ByteArrayInputStream(mat_specByte);
                    m_v_spectra_view_ad.setLength(0);
                    
                    try {
                        
                        postgresConSpecViewerMat.getCopyAPI().copyIn("COPY v_spec_viewer_advanced_materialized( "
                                + "spectrum_match_id, spectrum_id, scan_number, run, spectrum_peak_id, expmz, absoluteintesity, unmatched, "
                                + "isotope_peak_info, fragment_name, fragment_id, sequence, mass, peptide_id, matchedpeptide, charge, "
                                + "isprimarymatch, description) "
                                + "FROM STDIN WITH CSV", exp_spec_is);
                        
                        
                        
                    } catch (SQLException ex) {
                        PrintWriter pw = null;
                        try {
                            String message = "error writing v_spec_viewer_advanced_materialized";
                            Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, message, ex);
                            pw = new PrintWriter(new FileOutputStream("/tmp/XiCopySqlError.csv", true));
                            pw.println("\n------------------------------------------------\n" + new Date() + " " + message);
                            ex.printStackTrace(pw);
                            pw.println("->");
                            pw.println(matSpecViewCopy);
                        } catch (FileNotFoundException ex1) {
                            Logger.getLogger(XiDBWriterBiogrid.class.getName()).log(Level.SEVERE, null, ex1);
                        } finally {
                            pw.close();
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(XiDBWriterBiogrid.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    try {
                        waitSync.await();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(XiDBWriterBiogrid.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (BrokenBarrierException ex) {
                        Logger.getLogger(XiDBWriterBiogrid.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                }
            };

            new Thread(runSpecViewer).start();


            {
                //
                String spectrumCopy = m_copySpectrum.toString();
                byte sByte[] = spectrumCopy.getBytes();
                InputStream is = new ByteArrayInputStream(sByte);
                m_copySpectrum.setLength(0);
    //            System.out.println("spectrum " + postgres_con.getCopyAPI().copyIn(
    //                    "COPY spectrum (acq_id, run_id, scan_number, elution_time_start, elution_time_end, id) " +
    //                    "FROM STDIN WITH CSV", is));


                try {
                    postgres_con.getCopyAPI().copyIn(
                            "COPY spectrum (acq_id, run_id, scan_number, elution_time_start, elution_time_end, id, precursor_charge, precursor_intensity, precursor_mz, search_id) "
                            + "FROM STDIN WITH CSV", is);
                } catch (SQLException ex) {
                    String message = "error writing the spectra informations";
                    Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, message, ex);
                    PrintWriter pw = new PrintWriter(new FileOutputStream("/tmp/XiCopySqlError.csv", true));
                    pw.println("\n------------------------------------------------\n" + new Date() + " " + message);
                    ex.printStackTrace(pw);
                    pw.println("->");
                    pw.println(spectrumCopy);
                    pw.close();
                    return;
                }
            }
            
            {
                // Spectrum Peak
                final String spectrumPeakCopy = m_spectrum_peakSql.toString();
                byte spByte[] = spectrumPeakCopy.getBytes();
                InputStream isp = new ByteArrayInputStream(spByte);
                m_spectrum_peakSql.setLength(0);
    //            System.out.println("spectrum_peak " + postgres_con.getCopyAPI().copyIn(
    //                    "COPY spectrum_peak (spectrum_id, mz, intensity, id)" +
    //                    "FROM STDIN WITH CSV", isp));
                try {
                    postgres_con.getCopyAPI().copyIn(
                            "COPY spectrum_peak (spectrum_id, mz, intensity, id, isIsotope, isMonoIsotope)"
                            + "FROM STDIN WITH CSV", isp);
                } catch (SQLException ex) {
                    String message = "error writing the spectra peak informations";
                    Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, message, ex);
                    PrintWriter pw = new PrintWriter(new FileOutputStream("/tmp/XiCopySqlError.csv", true));
                    pw.println("\n------------------------------------------------\n" + new Date() + " " + message);
                    ex.printStackTrace(pw);
                    pw.println("->");
                    pw.println(spectrumPeakCopy);
                    return;
                }
            }
            // Peptide
            {
                String peptideCopy = m_peptideSql.toString();
                byte peptideByte[] = peptideCopy.getBytes();
                InputStream pis = new ByteArrayInputStream(peptideByte);
                m_peptideSql.setLength(0);
    //             System.out.println("peptide " + postgres_con.getCopyAPI().copyIn(
    //                    "COPY peptide(sequence, mass, id) " +
    //                    "FROM STDIN WITH CSV", pis));
                try {
                    postgres_con.getCopyAPI().copyIn(
                            "COPY peptide(sequence, mass, id, peptide_length, search_id) "
                            + "FROM STDIN WITH CSV", pis);
                } catch (SQLException ex) {
                    String message = "error writing the peptide informations";
                    Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, message, ex);
                    PrintWriter pw = new PrintWriter(new FileOutputStream("/tmp/XiCopySqlError.csv", true));
                    pw.println("\n------------------------------------------------\n" + new Date() + " " + message);
                    ex.printStackTrace(pw);
                    pw.println("->");
                    pw.println(peptideCopy);
                    pw.flush();
                    return;
                }
                peptideCopy = null;
            }

            // Protein
            {
                String proteinCopy = m_proteinSql.toString();
                //System.err.println("to save>> " + m_proteinSql.toString());
                byte protByte[] = proteinCopy.getBytes();
                InputStream protis = new ByteArrayInputStream(protByte);
                m_proteinSql.setLength(0);
    //             System.out.println("protein " + postgres_con.getCopyAPI().copyIn(
    //                    "COPY protein(name, sequence, id) " +
    //                    "FROM STDIN WITH CSV", protis));
                // System.err.println(protis);
                try {
                    postgres_con.getCopyAPI().copyIn(
                            "COPY protein(header,name, accession_number, description, sequence, id, is_decoy, protein_length, search_id) "
                            + "FROM STDIN WITH CSV", protis);
                } catch (SQLException ex) {
                    String message = "error writing the protein informations";
                    Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, message, ex);
                    PrintWriter pw = new PrintWriter(new FileOutputStream("/tmp/XiCopySqlError.csv", true));
                    pw.println("\n------------------------------------------------\n" + new Date() + " " + message);
                    ex.printStackTrace(pw);
                    pw.println("->");
                    pw.println(proteinCopy);
                    return;
                }
                proteinCopy=null;
            }

            // has_protein
            {
                String hpCopy = m_hasProteinSql.toString();
                byte hpByte[] = hpCopy.getBytes();
                // System.err.println(hpCopy);
                InputStream hpis = new ByteArrayInputStream(hpByte);
                m_hasProteinSql.setLength(0);
    //             System.out.println("has_protein " + postgres_con.getCopyAPI().copyIn(
    //                    "COPY has_protein(peptide_id, protein_id, peptide_position, display_site) " +
    //                    "FROM STDIN WITH CSV", hpis));
                try {
                    postgres_con.getCopyAPI().copyIn(
                            "COPY has_protein(peptide_id, protein_id, peptide_position, display_site, search_id) "
                            + "FROM STDIN WITH CSV", hpis);
                } catch (SQLException ex) {
                    String message = "error writing the hasprotein table";
                    Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, message, ex);
                    PrintWriter pw = new PrintWriter(new FileOutputStream("/tmp/XiCopySqlErrorHasProtein.csv", true));
                    pw.println("\n------------------------------------------------\n" + new Date() + " " + message);
                    ex.printStackTrace(pw);
                    pw.println("->");
                    pw.println(hpCopy);
                    pw.flush();
                    System.exit(1);
                    return;
                }
                hpCopy = null;
            }




            
            // spetcrum_match
            {
                final String specCopy = m_SpectrumMatchSql.toString();
                byte specByte[] = specCopy.getBytes();
                InputStream specis = new ByteArrayInputStream(specByte);
                m_SpectrumMatchSql.setLength(0);
    //             System.out.println("spectrum_match " + postgres_con.getCopyAPI().copyIn(
    //                    "COPY spectrum_match(search_id, score, spectrum_id, id) " +
    //                    "FROM STDIN WITH CSV", specis));
                try {
                    postgres_con.getCopyAPI().copyIn(
                            "COPY spectrum_match(search_id, score, spectrum_id, id, is_decoy, rank, autovalidated, precursor_charge, calc_mass, dynamic_rank) "
                            + "FROM STDIN WITH CSV", specis);
                } catch (SQLException ex) {
                    String message = "error writing the spectrum_match table";
                    Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, message, ex);
                    PrintWriter pw = new PrintWriter(new FileOutputStream("/tmp/XiCopySqlError.csv", true));
                    pw.println("\n------------------------------------------------\n" + new Date() + " " + message);
                    ex.printStackTrace(pw);
                    pw.println("->");
                    pw.println(specCopy);
                    return;
                }
            }
            

            {
                // matched_peptide
                final String mpCopy = m_MatchedPeptideSql.toString();
                byte mpByte[] = mpCopy.getBytes();
                InputStream mpis = new ByteArrayInputStream(mpByte);
                m_MatchedPeptideSql.setLength(0);
    //             System.out.println("matched_peptide " + postgres_con.getCopyAPI().copyIn(
    //                    "COPY matched_peptide(peptide_id, match_id, match_type, link_position, display_positon) " +
    //                    "FROM STDIN WITH CSV", mpis));
                try {
                    postgres_con.getCopyAPI().copyIn(
                            "COPY matched_peptide(peptide_id, match_id, match_type, link_position, display_positon, crosslinker_id, crosslinker_number, search_id) "
                            + "FROM STDIN WITH CSV", mpis);
                } catch (SQLException ex) {
                    String message = "error writing the matched_peptide table";
                    Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, message, ex);
                    PrintWriter pw = new PrintWriter(new FileOutputStream("/tmp/XiCopySqlError.csv", true));
                    pw.println("\n------------------------------------------------\n" + new Date() + " " + message);
                    ex.printStackTrace(pw);
                    pw.println("->");
                    pw.println(mpCopy);
                }
            }
            
            
            // join up with the threads, that write the materialized views
            try {
                waitSync.await();
            } catch (InterruptedException ex) {
                Logger.getLogger(XiDBWriterBiogrid.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BrokenBarrierException ex) {
                Logger.getLogger(XiDBWriterBiogrid.class.getName()).log(Level.SEVERE, null, ex);
            }


            // free the connection
            m_connectionPool.free(con);
            m_connectionPool.free(conExportMat);
            m_connectionPool.free(conSpecViewerMat);




        } catch (IOException ex) {
            Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, null, ex);
        }




    }// end method

    private void executeSQL() {

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

                    synchronized (m_SQLThreadCount) {
                        m_SQLThreadCount.value--;
                    }


                } catch (SQLException ex) {
                    Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, "error writing batch job", ex);
                    while ((ex = ex.getNextException()) != null) {
                        Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, "next exception", ex);
                    }

                    //System.err.println("XiDB: problem when batching SQL results: " + ex.getMessage());
                    m_connectionPool.closeAllConnections();
                    System.exit(1);
                }
            }
        };

        execSQL1.start();



        final String proteinSql = m_proteinSql.toString();
        m_proteinSql.setLength(0);
//        m_sqlout.write(proteinSql + "\n");


        final String peptideSql = m_peptideSql.toString();
        m_peptideSql.setLength(0);
//        m_sqlout.write(peptideSql + "\n");


        final String hasProteinSql = m_hasProteinSql.toString();
        m_hasProteinSql.setLength(0);
//        m_sqlout.write(hasProteinSql + "\n");


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
                    stm.executeUpdate(proteinSql);
                    stm.executeUpdate(peptideSql);

                    stm.close();

                    conn.commit();
                    conn.setAutoCommit(isAutoCommit);

                    m_connectionPool.free(conn);

                    synchronized (m_SQLThreadCount) {
                        m_SQLThreadCount.value--;
                    }


                } catch (SQLException ex) {
                    Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, "error writing batch job", ex);
                    while ((ex = ex.getNextException()) != null) {
                        Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, "next exception", ex);
                    }

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



        while (execSQL1.isAlive()) {
            try {
                execSQL1.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        while (execSQL2.isAlive()) {
            try {
                execSQL2.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, null, ex);
            }
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
                    stm.executeUpdate(SpectrumMatchSql);

                    stm.executeUpdate(hasProteinSql);
                    stm.executeUpdate(MatchedPeptideSql);

                    stm.close();

                    conn.commit();
                    conn.setAutoCommit(isAutoCommit);

                    m_connectionPool.free(conn);

                    synchronized (m_SQLThreadCount) {
                        m_SQLThreadCount.value--;
                    }


                } catch (SQLException ex) {
                    Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, "error writing batch job", ex);
                    while ((ex = ex.getNextException()) != null) {
                        Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, "next exception", ex);
                    }

                    //System.err.println("XiDB: problem when batching SQL results: " + ex.getMessage());
                    m_connectionPool.closeAllConnections();
                    System.exit(1);
                } finally {
//                    synchronized (m_semaphoreSynchronization) {
                    m_ThreadMonitor.release();
//                    }
                }
            }
        };
        execSQL3.run();

        Runtime r = Runtime.getRuntime();
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

//    /**
//     *
//     * @param match
//     * @return
//     */
//    protected IDs getIds(MatchedXlinkedPeptide match) {
//
//        // will hold the start values of the ids
//        IDs start_ids = new IDs();
//
//        // will hold a count of all ids we want to return
//        IdCounts id_counts = new IdCounts();
//
//        // count of how many IDS we need
//        if (match.getSpectrum().getID() == -1) {
//            ++id_counts.i_spectrum_ids;
//        }
//
//        id_counts.i_peak_ids = match.getSpectrum().getPeaks().size();
//        id_counts.i_peak_annotations_ids = 0;
//        id_counts.i_fragment_ids = 0;
//
//
//
////         for (MatchedBaseFragment mbf : match.getMatchedFragments()) {
////            // save fragments
////            Fragment f = mbf.getBaseFragment();
////            long f_id = f.getID();
////            if (f_id == -1) {
////                f.setID(-2);
////                id_counts.i_fragment_ids++;
////            }
////
////            // save annotated_peak, same as above
////            SpectraPeak sp = mbf.getBasePeak();
////            // we can have null here if there are losses WITHOUT non-lossy fragments
////            if(sp != null){
////                id_counts.i_peak_annotations_ids++;
////            }
////            // Do the same for losses
////            for (Loss l : mbf.getLosses().keySet()) {
////                // same for loss fragments
////                Fragment lf = l;
////                long lf_id = lf.getID();
////                if (lf_id ==-1) {
////                    //saveFragment(lf, result_ids);
////                    id_counts.i_fragment_ids++;
////                    lf.setID(-2);
////                }
////
////                //SpectraPeak lsp = mbf.getLosses().get(lf);
////                id_counts.i_peak_annotations_ids++;
////                //savePeakAnnotation(lsp, lf.getID(), result_ids);
////
////            }
////        }
//
//        for (SpectraPeak sp : match.getSpectrum()) {
//            for (SpectraPeakMatchedFragment spmf : sp.getMatchedAnnotation()) {
//                id_counts.i_peak_annotations_ids++;
//                id_counts.i_fragment_ids++;
//            }
//        }
//
//
//
//
//        // System.out.println("Fragments to store :"  + id_counts.i_fragment_ids);
////        System.out.println("Annotations to store: "  + id_counts.i_peak_annotations_ids++);
//
//
//        int counts[] = countPeptideProteinIDs(match.getPeptides());
//        id_counts.i_peptide_ids = counts[0];
//        id_counts.i_protein_ids = counts[1];
//
//        id_counts.i_peak_cluster_ids = 0;
//        id_counts.i_peak_cluster_ids = match.getSpectrum().getIsotopeClusters().size() * 2;
////        System.out.println("cluster ids requested = " +  id_counts.i_peak_cluster_ids);
//
//
//
//        try {
//            // start commitg
//            boolean isAutoCommit = m_conn.getAutoCommit();
//            m_conn.setAutoCommit(false);
//
//
//
////            m_getIDs = m_conn.prepareStatement("SELECT spectrumid, peakid, pepid, protid, specmatchid, paid, fragid, pcid "
//
//            m_getIDs.setLong(1, id_counts.i_spectrum_ids);
//            m_getIDs.setLong(2, id_counts.i_peak_ids);
//            m_getIDs.setLong(3, id_counts.i_peptide_ids);
//            m_getIDs.setLong(4, id_counts.i_protein_ids);
//            m_getIDs.setLong(5, id_counts.i_spectrum_match_ids);
//            m_getIDs.setLong(6, id_counts.i_peak_annotations_ids);
//            m_getIDs.setLong(7, id_counts.i_fragment_ids);
//            m_getIDs.setLong(8, id_counts.i_peak_cluster_ids);
//
//
//
//            ResultSet rs = m_getIDs.executeQuery();
//
//
//            while (rs.next()) {
//                start_ids.i_spectrum_id = rs.getLong(1);
//                start_ids.i_peak_id = rs.getLong(2);
//                start_ids.i_peptide_id = rs.getLong(3);
//                start_ids.i_protein_id = rs.getLong(4);
//                start_ids.i_spectrum_match_id = rs.getLong(5);
//                start_ids.i_peak_annotations_id = rs.getLong(6);
//                start_ids.i_fragment_id = rs.getLong(7);
//                start_ids.i_peak_cluster_id = rs.getLong(8);
//            }
//            // m_getIDs = m_conn.prepareStatement("SELECT spectrumid, peakid from reserve_ids(?, ?);");
//            // Call the function once at the start of each spectrum
//            // passing in 0 for the arguments you don't need every time you call this
//
//            // m_getIDs.setInt(1, "1");
//
//            // for peptide, check if they are -1 i.e they could be seen before
//
//            //  for protein count check if they are -1 (i.e. not seen before ) and then increment the count
//
//            // also replace checks for -1 (fragments only with < 0 because we set -2)
//
//            // count fragments
//
//
//
//            // end commit
//            m_conn.commit();
//            m_conn.setAutoCommit(isAutoCommit);
//
//        } catch (SQLException ex) {
//            Logger.getLogger(XiDBWriterMultiInsertSql.class.getName()).log(Level.SEVERE, null, ex);
//            System.err.println("XiDB: problem when acquiring IDs for storing results: " + ex.getMessage());
//            m_connectionPool.closeAllConnections();
//            System.exit(1);
//
//        }
//
//
//
////        System.out.println("Cluster :" + (id_counts.i_peak_cluster_ids + start_ids.i_peak_cluster_id));
//
//        return start_ids;
//
//    }// end method getIds()

    @Override
    public synchronized void writeResult(MatchedXlinkedPeptide match) {
        m_BF_ClusterPeakAssignments.clear();

        ++results_processed;
        if (match.getMatchrank() == 1)
            top_results_processed++;

        sqlBatchCount++;
        if (sqlBatchCount > sqlBufferSize) { //sqlBufferSize/10){
            executeCopy();
//          executeSQL();
//            writeInserts();
            sqlBatchCount = 0;
        }

        // 0. Get the ids for saving results
//        mon0 = MonitorFactory.start("getIds()");
//        IDs result_ids = getIds(match);
//        mon0.stop();

        // 1. Check spectrum info spectrum spectrum peak
        Spectra matched_spectrum = match.getSpectrum();

        if (matched_spectrum.getID() == -1) {
            // We need to insert this spectrum AND it's peaks into the database, and set their ids once doing so
            // high level information
            // (acq_id, run_id, scan_number, elution_time_start, elution_time_end, notes)
//                mon1 = MonitorFactory.start("saveSpectrum()");
            saveSpectrum(matched_spectrum, ids);
//               mon1.stop();
        }




        // 2. Save spectrum_match info
//         mon2 = MonitorFactory.start("saveSpectrumMatch()");
        double score = match.getScore("match score");
        if (Double.isNaN(score) || Double.isNaN(score)) {
            score = 0;
        }
        long spectrum_match_id = saveSpectrumMatch(match.getScore("match score"), matched_spectrum.getID(), ids, match.isDecoy(), match);
//        mon2.stop();

        // 3. Save the protein/peptide sequences
//        mon3 = MonitorFactory.start("savePeptide1()");
        boolean alpha = true;
        Peptide[] peps = match.getPeptides();
        for (int p = 0; p<peps.length; p++) {
            savePeptide(peps[p], spectrum_match_id, alpha, match.getLinkSites(peps[p]), ids, match.getCrosslinker() == null? null : match.getCrosslinker().getDBid(),0);
            alpha = false;
        }
//        savePeptide(match.getPeptides()[0], spectrum_match_id, true, match.getLinkSites(match.getPeptides()[0]), ids, match.getCrosslinker() == null? null : match.getCrosslinker().getDBid(),0);

//        mon3.stop();
//        mon4 = MonitorFactory.start("savePeptide2()");
//        if (match.getPeptide(1) != null) {
//            savePeptide(match.getPeptides()[1], spectrum_match_id, false, match.getLinkSites(match.getPeptides()[1]), ids, match.getCrosslinker().getDBid(),0);
//        }
//        mon4.stop();






        // 6. Collate info for materialized view v_export_mat & v_spectra_viewer advanced, then save it
        save_v_export_mat(match, spectrum_match_id);
//        System.out.println(" cluster  writen : " + result_ids.i_peak_cluster_id );


        save_v_spectra_viewer_advanced(match, spectrum_match_id);


        // Done !!!!!!




    }// write result
    private StringBuffer m_v_spectra_view_ad = new StringBuffer();

    private void save_v_spectra_viewer_advanced(MatchedXlinkedPeptide match, long spectrum_match_id) {

        // Almost the same data from PeakListWriter()

        // header information for each peak - matched or unmatched
        Spectra matched_spectrum = match.getSpectrum();;





//        peak.getIntensity(); // absolute intensity
//        peak.getMZ(); // expmz
//        (peak.hasAnnotation(SpectraPeakAnnotation.monoisotop)? "monoisotopic" :(peak.hasAnnotation(SpectraPeakAnnotation.isotop)? "isotope": ""))
//        peak.hasAnnotation(SpectraPeakAnnotation.unmatched);
//

//        f.name();

        for (SpectraPeak peak : matched_spectrum.getPeaks()) {

            boolean unmatched = peak.hasAnnotation(SpectraPeakAnnotation.unmatched);
            int umatched = unmatched ? 1 : 0;
            String isotope_peak_info = "";
            if (peak.hasAnnotation(SpectraPeakAnnotation.monoisotop)) {
                // will be tagged 'monoisotopic' AND 'isotope'
                // but monoistopic supercededs so it takes this value
                isotope_peak_info = "monoisotopic";
            } else if (peak.hasAnnotation(SpectraPeakAnnotation.isotop)) {
                isotope_peak_info = "isotope";
            }

            ArrayList<SpectraPeakMatchedFragment> matches = peak.getMatchedAnnotation();


            if (matches.size() == 0) {
                // Write the header information for each peak
                m_v_spectra_view_ad.append(spectrum_match_id);
                m_v_spectra_view_ad.append(",");
                m_v_spectra_view_ad.append(matched_spectrum.getID());// spectrum_id
                m_v_spectra_view_ad.append(",");
                m_v_spectra_view_ad.append(matched_spectrum.getScanNumber());
                m_v_spectra_view_ad.append(",\"");
                m_v_spectra_view_ad.append(matched_spectrum.getRun());
                m_v_spectra_view_ad.append("\",");
                m_v_spectra_view_ad.append(peak.getID());
                m_v_spectra_view_ad.append(",");
                m_v_spectra_view_ad.append(peak.getMZ());
                m_v_spectra_view_ad.append(",");
                m_v_spectra_view_ad.append(peak.getIntensity());
                m_v_spectra_view_ad.append(",");
                m_v_spectra_view_ad.append(umatched);
                m_v_spectra_view_ad.append(",");
                m_v_spectra_view_ad.append(isotope_peak_info);
                m_v_spectra_view_ad.append(",");

                // fields have null values for unmateched peaks
                //m_v_spectra_view_ad.append("null");
                m_v_spectra_view_ad.append(",");
                //m_v_spectra_view_ad.append("null");
                m_v_spectra_view_ad.append(",");
                // m_v_spectra_view_ad.append("null");
                m_v_spectra_view_ad.append(",");
                //m_v_spectra_view_ad.append("null");
                m_v_spectra_view_ad.append(",");
                //m_v_spectra_view_ad.append("null");
                m_v_spectra_view_ad.append(",");
                //m_v_spectra_view_ad.append("null");
                m_v_spectra_view_ad.append(",");
                //m_v_spectra_view_ad.append("null");
                m_v_spectra_view_ad.append(",");
                // m_v_spectra_view_ad.append("null");
                m_v_spectra_view_ad.append(",");

                m_v_spectra_view_ad.append("\n");

            } else {
                // Write other data when peak has annotation -
                // this could be more than one form of annotation
                for (SpectraPeakMatchedFragment mf : matches) {

                    Fragment f = mf.getFragment();
                    Peptide p = f.getPeptide();

                    // could be crosslinked or linear
                    String match_type = (mf.isLinear()) ? "linear" : "crosslinked";
                    String is_primary = (mf.isPrimary()) ? "1" : "0";

                    m_v_spectra_view_ad.append(spectrum_match_id);
                    m_v_spectra_view_ad.append(",");
                    m_v_spectra_view_ad.append(matched_spectrum.getID());// spectrum_id
                    m_v_spectra_view_ad.append(",");
                    m_v_spectra_view_ad.append(matched_spectrum.getScanNumber());
                    m_v_spectra_view_ad.append(",\"");
                    m_v_spectra_view_ad.append(matched_spectrum.getRun());
                    m_v_spectra_view_ad.append("\",");
                    m_v_spectra_view_ad.append(peak.getID());
                    m_v_spectra_view_ad.append(",");
                    m_v_spectra_view_ad.append(peak.getMZ());
                    m_v_spectra_view_ad.append(",");
                    m_v_spectra_view_ad.append(peak.getIntensity());
                    m_v_spectra_view_ad.append(",");
                    m_v_spectra_view_ad.append(umatched);
                    m_v_spectra_view_ad.append(",");
                    m_v_spectra_view_ad.append(isotope_peak_info);
                    m_v_spectra_view_ad.append(",\"");


                    // columns specific to annotated peaks
                    m_v_spectra_view_ad.append(f.name());
                    m_v_spectra_view_ad.append("\",");
                    m_v_spectra_view_ad.append(f.getID());
                    m_v_spectra_view_ad.append(",\"");
                    m_v_spectra_view_ad.append(f.toString());
                    m_v_spectra_view_ad.append("\",");
                    m_v_spectra_view_ad.append(f.getNeutralMass());
                    m_v_spectra_view_ad.append(",");
                    m_v_spectra_view_ad.append(p.getID());
                    m_v_spectra_view_ad.append(",\"");
                    m_v_spectra_view_ad.append(p.toString());
                    m_v_spectra_view_ad.append("\",");
                    m_v_spectra_view_ad.append(mf.getCharge());
                    m_v_spectra_view_ad.append(",");
                    m_v_spectra_view_ad.append(is_primary);
                    m_v_spectra_view_ad.append(",");
                    m_v_spectra_view_ad.append(match_type);


                    m_v_spectra_view_ad.append("\n");


                }// end for

            }// end else

        }// end for





// spectrum_match_id  | bigint                  |
// spectrum_id        | bigint                  |
// scan_number        | integer                 |
// run                | character varying(1000) |

// spectrum_peak_id   | bigint                  |   
// expmz              | numeric                 |
// absoluteintesity   | numeric                 |
// unmatched          | integer                 |
// isotope_peak_info  | text                    |

// fragment_name      | text                    |
// fragment_id        | bigint                  |
// sequence           | text                    |
// mass               | numeric                 |


// peptide_id         | bigint                  |
// matchedpeptide     | text                    |

// charge             | integer                 |
// isprimarymatch     | text                    |


    }// end method save_v_spectra_viewer()
    // For materialized views
    private StringBuffer m_v_export_mat_SQL = new StringBuffer();

    private void save_v_export_mat(MatchedXlinkedPeptide match, long spectrum_match_id) {

        // Nothe this method only writes ! row to the materialized view
        Spectra matched_spectrum = match.getSpectrum();
        double calc_mass = match.getCalcMass();
        double exp_mass = matched_spectrum.getPrecurserMass();
        Peptide pep1 = match.getPeptides()[0];
        Peptide pep2 = match.getPeptides()[1];
        Peptide.PeptidePositions pp = pep1.getPositions()[0];
        Peptide.PeptidePositions pp2 = null;
        if (pep2 != null) {
            pp2 = pep2.getPositions()[0];
        }

        Peptide.PeptidePositions pps[] = pep1.getPositions();
        CountOccurence<Sequence> ppcount = new CountOccurence<Sequence>();

        for (Peptide.PeptidePositions p : pep1.getPositions()) {
            ppcount.increment(p.base);
        }

        StringBuilder pep1counts = new StringBuilder();
        for (Sequence prot : ppcount.getCountedObjects()) {
            pep1counts.append(prot.getFastaHeader() + " (" + ppcount.count(prot) + ");");
        }
        ppcount.clear();

        if (pep2 != null) {
            for (Peptide.PeptidePositions p : pep2.getPositions()) {
                ppcount.increment(p.base);
            }

            StringBuilder pep2counts = new StringBuilder();
            for (Sequence prot : ppcount.getCountedObjects()) {
                pep2counts.append(prot.getFastaHeader() + " (" + ppcount.count(prot) + ");");
            }

        }


//        "search_id, spectrum_match_id, spectrum_id, autovalidated, rank, run_name, scan_number, "
//                        + "match_score, total_fragment_matches, fragment_coverage, delta, peptide1_coverage, peptide2_coverage, "
//                        + "spectrum_peaks_coverage, spectrum_intensity_coverage, spectrum_quality_score, peptide1_id, peptide1, "
//                        + "display_protein1, peptide_position1, site_count1, protein_count1, peptide2_id, peptide2, display_protein2, "
//                        + "peptide_position2, site_count2, protein_count2, pep1_link_pos, pep2_link_pos, crosslinker, is_decoy, "
//                        + "precursor_charge, precursor_intensity, precursor_mz, exp_mass, calc_mass, error) "
//                        + "FROM STDIN WITH CSV"

        m_v_export_mat_SQL.append(m_search_id);
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(spectrum_match_id);
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(matched_spectrum.getID());
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(match.isValidated());
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(match.getMatchrank());
        m_v_export_mat_SQL.append(",\"");
        m_v_export_mat_SQL.append(matched_spectrum.getRun());
        m_v_export_mat_SQL.append("\",");
        m_v_export_mat_SQL.append(matched_spectrum.getScanNumber());
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(match.getScore("match score"));
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(match.getScore("total fragment matches"));
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(match.getScore("fragment coverage"));
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(match.getScore("delta"));
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(match.getScore("peptide1 coverage"));
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(pep2 == null ? "" : match.getScore("peptide2 coverage"));
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(match.getScore("spectrum peaks coverage"));
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(match.getScore("spectrum intensity coverage"));
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(match.getScore("spectrum quality score"));
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(pep1.getID());
        m_v_export_mat_SQL.append(",\"");
//        m_v_export_mat_SQL.append("'").append(pep1.toString()).append("'");
        m_v_export_mat_SQL.append("\"" + pep1.toString().replace("\"", "\"\"") + "\"");
        m_v_export_mat_SQL.append("\",\"");
        m_v_export_mat_SQL.append((pp.base.isDecoy() ? "DECOY" : pp.base.getFastaHeader().replace("\"", "\"\"").replace(",", " ")));
        m_v_export_mat_SQL.append("\",");
        m_v_export_mat_SQL.append(pp.start);
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(pep1.getPositions().length);
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(pep1.getProteinCount());
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(pep2 == null ? "" : pep2.getID());
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(pep2 == null ? "" : "\"" + pep2.toString() + "\"");
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(pep2 == null ? "" : (pp2.base.isDecoy() ? "\"DECOY\"" : "\"" + pp2.base.getFastaHeader().replace("\"", "\"\"") + "\""));
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(pep2 == null ? "" : pp2.start);
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(pep2 == null ? "" : pep2.getPositions().length);
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(pep2 == null ? "" : pep2.getProteinCount());
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(pep2 == null ? "" : match.getLinkingSite(0));
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(pep2 == null ? "" : match.getLinkingSite(1));
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(pep2 == null ? "" : match.getCrosslinker().getName());
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(match.isDecoy());
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(matched_spectrum.getPrecurserCharge());
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(matched_spectrum.getPrecurserIntensity());
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(matched_spectrum.getPrecurserMZ());
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(exp_mass);
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(calc_mass);
        m_v_export_mat_SQL.append(",");
//        m_v_export_mat_SQL.append(( exp_mass - calc_mass ) / ( calc_mass * 1000000 ));
        m_v_export_mat_SQL.append(1000000 * ((exp_mass - calc_mass) / (calc_mass)));
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(match.getMatchrank() == 1 ? true : false);
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(pp.base.getID());
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(pp.base.length());
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(pep2 == null ? "" : pp2.base.getID());
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(pep2 == null ? "" : pp2.base.length());
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(pep1.length());
        m_v_export_mat_SQL.append(",");
        m_v_export_mat_SQL.append(pep2 == null ? "" : pep2.length());



        //display_protein1_id, display_protein1_length, display_protein2_id, display_protein2_length, peptide1_length, peptide2_length




        m_v_export_mat_SQL.append("\n");

        //mat_export_entries.put("search_id", m_search_id);
        //mat_export_entries.put("spectrum_match_id", spectrum_match_id);
        // mat_export_entries.put("spectrum_id", matched_spectrum.getID());
        //mat_export_entries.put("scan_number", matched_spectrum.getScanNumber());
        //mat_export_entries.put("run_name", matched_spectrum.getRun());
        //mat_export_entries.put("match_score", match.getScore("match score"));
        //mat_export_entries.put("is_decoy", match.isDecoy());
        //mat_export_entries.put("autovalidated", match.isValidated());
        // mat_export_entries.put("rank", match.getMatchrank());
//        mat_export_entries.put("precursor_charge", matched_spectrum.getPrecurserCharge());
//        mat_export_entries.put("precursor_intensity", matched_spectrum.getPrecurserIntensity());
//        mat_export_entries.put("precursor_mz", matched_spectrum.getPrecurserMZ());

//        mat_export_entries.put("exp_mass", exp_mass);
//        mat_export_entries.put("calc_mass", calc_mass);
//        mat_export_entries.put("error", ( exp_mass - calc_mass ) / ( calc_mass * 1000000 ) );


        // mat_export_entries.put("peptide1_id", pep1.getID());
        // mat_export_entries.put("peptide1", pep1.getSequence());
        // mat_export_entries.put("protein_count1", pep1.getProteinCount() );


        //mat_export_entries.put("peptide2_id", pep2.getID());
        //mat_export_entries.put("peptide2", pep2.getSequence());
//        mat_export_entries.put("protein_count2", pep2.getProteinCount() );

        // first is the display header and peptide position

        // We only want the first position (duisplay protein)

        // mat_export_entries.put("display_protein1", pp.base.getFastaHeader().replace("'", "''").replace(",", " ") );
        //mat_export_entries.put("peptide_position1", pp.start );

        // same for peptide2

        // mat_export_entries.put("display_protein2", pp2.base.getFastaHeader().replace("'", "''").replace(",", " ") );
        // mat_export_entries.put("peptide_position2", pp2.start );


        // linker positions for each peptide
        //        mat_export_entries.put("pep1_link_pos", match.getLinkSites(pep1)[0]);
        //        mat_export_entries.put("pep2_link_pos", match.getLinkSites(pep2)[0]);
        // mat_export_entries.put("pep1_link_pos", match.getLinkingSitePeptide1());
        //mat_export_entries.put("pep2_link_pos", match.getLinkingSitePeptide2());


        // sites
        // mat_export_entries.put("site_count1", pep1.getPositions().length);
        // mat_export_entries.put("site_count2", pep2.getPositions().length);

        // mat_export_entries.put("crosslinker", match.getCrosslinker() );

        // remaining scores
//        mat_export_entries.put("total_fragment_matches", match.getScore("total fragment matches"));
//        mat_export_entries.put("fragment_coverage", match.getScore("fragment coverage"));
//        mat_export_entries.put("delta", match.getScore("delta"));
//        mat_export_entries.put("peptide1_coverage", match.getScore("peptide1 coverage"));
//        mat_export_entries.put("peptide2 coverage", match.getScore("peptide2 coverage"));
//        mat_export_entries.put("spectrum_peaks_coverage", match.getScore("spectrum peaks coverage"));
//        mat_export_entries.put("spectrum_intensity_coverage", match.getScore("spectrum intensity coverage"));
//        mat_export_entries.put("spectrum_quality_score", match.getScore("spectrum quality score"));


    }// end method save_v_export_mat()

    private void saveSpectrum(Spectra matched_spectrum, IDs ids) {

//      try{
//         id                 | bigint                | not null default nextval('spectrum_id_seq'::regclass)
//         run_id             | integer               |
//         acq_id             | integer               |
//         scan_number        | integer               |
//         elution_time_start | character varying(50) |
//         elution_time_end   | character varying(50) |
//         notes              | text                  |
        matched_spectrum.setID(ids.nextSpectrumId());
        addSpectrum(matched_spectrum.getAcqID(), matched_spectrum.getRunID(), matched_spectrum);
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


        for (SpectraPeak sp : matched_spectrum.getPeaksArray()) {
            sp.setID(ids.nextPeakId());
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

    protected int[] countPeptideProteinIDs(Peptide[] peptides) {
        // Store peptide ids in ids[0] and protein ids in ids[1]
        int[] ids = new int[2];
        ids[0] = 0;
        ids[1] = 0;

        for (Peptide p : peptides) {
            if (p.getID() == -1) {
                // increment pep id count
                ++ids[0];


                for (Peptide.PeptidePositions pp : p.getPositions()) {
                    Sequence protein = pp.base;
                    if (protein.getID() == -1) {
                        ++ids[1];
                    }

                }
            }// end if we haven't seen a peptide

        }// end for

        return ids;
    }//

    private void savePeptide(Peptide peptide, long match_id, boolean alpha, int[] linkSites, IDs result_ids, Integer crosslinker_id, Integer crosslinker_number) {

        // if this is the first time you see a peptide, then save it to the DB, and set the ID
        // Likewise do the same with the Protein
//        try {
        if (peptide.getID() == -1) {

            //             id       | bigint  | not null default nextval('peptide_id_seq'::regclass)
            //             sequence | text    |
            //             mass     | numeric |
            peptide.setID(result_ids.nextPeptideId());
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
            for (Peptide.PeptidePositions pp : peptide.getPositions()) {
                ++first;
                Sequence protein = pp.base;
                if (protein.getID() == -1) {

                    //                 id       | bigint | not null default nextval('protein_id_seq'::regclass)
                    //                 name     | text   |
                    //                 sequence | text   |
                    protein.setID(result_ids.nextProteinId());
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
        for (int i = 0; i < linkSites.length; i++) {
            addMatchedPeptide(peptide, match_id, (alpha ? alpha_id : beta_id), linkSites[i], (i == 0), crosslinker_id, crosslinker_number);
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
    public synchronized void flush() {
        if (sqlBatchCount > 0) {
            executeCopy();
        }

    }

    @Override
    public void finished() {

        try {
            flush();
//                executeSQL();
//                  writeInserts();


            // our search is done
            m_search_complete.setInt(1, m_search_id);
            m_search_complete.executeUpdate();


            // runtime stats
            System.out.println("XiDBWriterCopySql - Total results: " + getResultCount() + "\n-------------");
//            class M implements Comparable{
//                String label;
//                double avg;
//
//                public M(String l, double a){
//                    this.label = l;
//                    this.avg = a;
//                }
//
//                @Override
//                public int compareTo(Object o) {
//                   if(o instanceof M){
//                    if(this.avg < ((M)o).avg)
//                        return 1;
//                    else if(this.avg > ((M)o).avg)
//                        return -1;
//                   }
//                   return 0;
//                }
//
//            }

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

        super.finished();
    }

    private long saveSpectrumMatch(double match_score, long spec_id, IDs result_ids, boolean is_decoy, MatchedXlinkedPeptide match) {
        long spec_match_id = result_ids.nextSpectrumMatchId();
//        try{
        addSpectrumMatch(m_search_id, match_score, spec_id, spec_match_id, is_decoy, match);


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

   





   

  
}
