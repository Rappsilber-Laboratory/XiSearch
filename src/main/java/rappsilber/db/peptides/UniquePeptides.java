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
package rappsilber.db.peptides;

import rappsilber.applications.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.DBRunConfig;
import rappsilber.db.ConnectionPool;
import rappsilber.ms.dataAccess.DBSequenceList;
import rappsilber.ms.dataAccess.filter.spectrafilter.DBScanFilteredSpectrumAccess;
import rappsilber.ms.dataAccess.msm.DBMSMListIterator;
import rappsilber.ms.dataAccess.output.ResultMultiplexer;
import rappsilber.ms.dataAccess.output.ResultWriter;
import rappsilber.utils.XiProvider;

/**
 *
 * @author stahir
 */
public class UniquePeptides {

    // settings needed for DB search
    private int m_search_id;
    private String m_base_directory;
    private String m_search_name;
    private DBRunConfig m_config;
    private DBMSMListIterator m_db_msm;
    //private AbstractMSMAccess m_db_msm;
    private File[] m_sequenceFiles;
    private DBSequenceList m_sequences;
    private ResultMultiplexer m_result_multiplexer;


    private ConnectionPool m_connectionPool;
    private Connection m_con;
    private PreparedStatement m_getSeqID;
    private PreparedStatement m_getPaths;
    private PreparedStatement m_getSpecID;
    private PreparedStatement m_updateSpecID;
    private ResultWriter    m_resultWriter;


    UniquePeptides(ConnectionPool cp) {
        try {

            this.m_connectionPool = cp;
            m_con = m_connectionPool.getConnection();
            m_getSeqID = m_con.prepareStatement("SELECT seqdb_id FROM search_sequencedb WHERE search_id = ?");
            m_getPaths = m_con.prepareStatement("SELECT file_path || '/' || file_name FROM sequence_file WHERE id = ?");
            m_getSpecID = m_con.prepareStatement("SELECT id_value FROM storage_ids WHERE name = 'spectrum_id'");
            m_updateSpecID = m_con.prepareStatement("UPDATE storage_ids SET id_value = id_value + ? WHERE name = 'spectrum_id'");

            m_result_multiplexer = new ResultMultiplexer();
//            m_result_multiplexer.setFreeMatch(true);
            m_sequenceFiles = null;
            m_db_msm = null;

        } catch (SQLException ex) {
//             System.err.println("XiDB: problem with gettign connection for reading sequence files: " + ex.getMessage());
//             m_connectionPool.closeAllConnections();
//             System.exit(1);
            String message = "XiDB: problem with gettign connection for reading sequence files: " + ex.getMessage();

            System.err.println(message);
            ex.printStackTrace();
            m_config.getStatusInterface().setStatus(message);
            m_connectionPool.closeAllConnections();
            System.exit(0);
        }
    }

    
    public void initSearch(int search_id, String base_directory, String search_name, DBRunConfig dbrc) {
        this.m_search_id = search_id;
        this.m_base_directory = base_directory;
        this.m_search_name = search_name;
        this.m_config = dbrc;


        // For settings that couldn't be evaluated, we just pass the config these can be read as foollows e.g.
        // m_buffer_output = Integer.parseInt((String)super.retrieveObject("BUFFEROUTPUT"));
        // m_use_cpus = Integer.parseInt((String)super.retrieveObject("USECPUS"));
        // m_evaluate_linears = Boolean.parseBoolean((String)super.retrieveObject("EVALUATELINEARS"));
    }

    public void setupMSMIterator() {
        // Setup the reader for MSM files
        try {
            int minCharge = Integer.parseInt((String) this.m_config.retrieveObject("MINCHARGE"));
            minCharge = 1;
            
            m_db_msm = new DBMSMListIterator(
                    this.m_search_id,
                    this.m_base_directory,
                    this.m_config.getFragmentTolerance(),
                    minCharge,
                    this.m_connectionPool, m_config);

            // enables filtering by max peptide mass in db
            //m_db_msm.gatherData();
            int cpus = m_config.getSearchThreads();

            
            String message = "detect maximum precursor mass ("  + cpus +")";
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, message);
            System.err.println(message);
            m_config.getStatusInterface().setStatus(message);
            m_db_msm.gatherData(cpus);
            //m_db_msm.gatherData();
            
            message = "Maximum mass : " + m_db_msm.getMaxPrecursorMass();
            System.err.println(message);
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, message);

            
        } catch (FileNotFoundException ex) {
//             System.err.println("XiDB: problem when setting up MSMIterator: " + ex.getMessage());
//             m_connectionPool.closeAllConnections();
//             System.exit(1);
            String message = "XiDB: problem when setting up MSMIterator: " + ex.getMessage();

            System.err.println(message);
            ex.printStackTrace();
            m_config.getStatusInterface().setStatus(message);
            m_connectionPool.closeAllConnections();
            System.exit(0);
        } catch (IOException ex) {
//             System.err.println("XiDB: problem when setting up MSMIterator: " + ex.getMessage());
//             m_connectionPool.closeAllConnections();
//             System.exit(1);
            String message = "XiDB: problem when setting up MSMIterator: " + ex.getMessage();

            System.err.println(message);
            ex.printStackTrace();
            m_config.getStatusInterface().setStatus(message);
            m_connectionPool.closeAllConnections();
            System.exit(0);
        } catch (ParseException ex) {
            String message = "XiDB: problem when setting up MSMIterator: " + ex.getMessage();

            System.err.println(message);
            ex.printStackTrace();
            m_config.getStatusInterface().setStatus(message);
            m_connectionPool.closeAllConnections();
            System.exit(0);
        }

        
    }


    // Read FASTA files, as an array of Files[] then result multiplexer, then filter i.e set to null we don;t filetr the runs just yet - basically work through params for simpleXi and then search
    public void getSequenceFiles(){

        try {
            m_sequences = new DBSequenceList(m_connectionPool, m_search_id, this.m_base_directory, m_config);
//            ArrayList<File> r = new ArrayList<File>();
//            m_getSeqID.setInt(1, this.m_search_id);
//            ResultSet rs = m_getSeqID.executeQuery();
//            while (rs.next()) {
//                m_getPaths.setInt(1, rs.getInt(1));
//                ResultSet rs2 = m_getPaths.executeQuery();
//                while (rs2.next()) {
//                    r.add(new File(this.m_base_directory + rs2.getString(1)));
//                }
//            }
//
//
//            this.m_sequenceFiles = new File[r.size()];
//            r.toArray(this.m_sequenceFiles);
            // System.out.println("sequence files "  + r.toString() + " size = " + r.size());

            

        } catch (Exception ex) {
            String message = "XiDB: problem when reading sequence files from the database: " + ex.getMessage();

            System.err.println(message);
            ex.printStackTrace();
            m_config.getStatusInterface().setStatus(message);
            m_connectionPool.closeAllConnections();
            System.exit(0);
        }

        
    }// end method get base

    public void setupResultWriter(){
        // Here we will write to multiple databases: for now
        // 1. The old Xi database, from which xaminatrix can read from
        // 2. The new Xi databse

//       // ResultWriter xamaintrix_writer = new XmassDB(this.m_config, this.m_search_name);
//        m_resultWriter = new XiDBWriterCopySqlIndividualBatchDs(this.m_config, this.m_connectionPool, this.m_search_id, this.m_db_msm.getAcqID());
////        m_result_multiplexer.setFreeMatch(true);

        String DBOutput = System.getProperty("XI_DB_OUTPUT", "YES");
        
//        if (DBOutput.contentEquals("YES"))
//            m_result_multiplexer.addResultWriter(m_resultWriter);

//        String csvOutPut = System.getProperty("XI_CSV_OUTPUT", null);
//        if (csvOutPut != null) {
//            try {
//                m_result_multiplexer.addResultWriter(new CSVExportMatches(new FileOutputStream(csvOutPut), m_config));
//    //            m_result_multiplexer.addResultWriter(new  CSVExportMatches(new FileOutputStream("/tmp/test_results.csv"), m_config));
//
//            } catch (FileNotFoundException ex) {
//                Logger.getLogger(UniquePeptides.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
        
        //   m_result_multiplexer.addResultWriter(xamaintrix_writer);
        //        try {
        //          //  m_result_multiplexer.addResultWriter(new ResultStreamWriter(new FileOutputStream("/tmp/test_results_peaks.csv")));
        //        } catch (FileNotFoundException ex) {
        //            Logger.getLogger(XiDBSearch.class.getName()).log(Level.SEVERE, null, ex);
        //        }


    }// end method setupResultWriter



    public SimpleXiProcess buildTress() {
        // SimpleXiProcess(File[] fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter)

        // For now 'StackedSpectraAccess filter' is set to null because we don't want to filter the spectra we search.
        // iIn the future we may want to (e.g get spectra with reporter ions only)
//        SimpleXiProcess xi = new SimpleXiProcess(m_sequenceFiles, m_db_msm, m_result_multiplexer, m_config, null);
//        SimpleXiProcess xi = new SimpleXiProcess(m_sequenceFiles, m_db_msm, m_result_multiplexer, m_config, null);
//        ResultWriter writer = new XiDBSingleSQLWriter(this.m_config, this.m_connectionPool, this.m_search_id, this.m_db_msm.getAcqID());
//        ResultWriter writer = new XiDBWriter(this.m_config, this.m_connectionPool, this.m_search_id, this.m_db_msm.getAcqID());
        // ResultWriter copy_writer = new XiDBWriterCopySql(this.m_config, this.m_connectionPool, this.m_search_id, this.m_db_msm.getAcqID());


//        SimpleXiProcess xi = new SimpleXiProcessDevMGX(m_sequences, m_db_msm,
//                        m_result_multiplexer , m_config, null);
        // settup a filter for already found results
        
//        System.err.println("\n BLA BLA BLA BLA BLA BLA BLA BLA BLA BLA BLA BLA BLA BLA BLA BLA \n");
        
        XiProcess xi = null;
        DBScanFilteredSpectrumAccess scanfilter = new DBScanFilteredSpectrumAccess(false);
        try {
            scanfilter.readFromSearch(m_con, m_search_id);
            scanfilter.setReader(m_db_msm);
            if (scanfilter.scansRegistered() ==0)
                xi = XiProvider.getXiSearch(m_sequences, m_db_msm, m_result_multiplexer, null, m_config, SimpleXiProcessLinearIncluded.class);
            else
                xi = XiProvider.getXiSearch(m_sequences, scanfilter, m_result_multiplexer, null, m_config, SimpleXiProcessLinearIncluded.class);
        } catch (SQLException ex) {
            Logger.getLogger(UniquePeptides.class.getName()).log(Level.SEVERE, null, ex);
            xi = XiProvider.getXiSearch(m_sequences, m_db_msm, m_result_multiplexer, null, m_config, SimpleXiProcessLinearIncluded.class);
        }
        System.out.println("Xi:" + xi.getClass().getName());
        
        xi.preparePreFragmentation();
        return (SimpleXiProcess) xi;
    }// end seacrh
    

    
}
