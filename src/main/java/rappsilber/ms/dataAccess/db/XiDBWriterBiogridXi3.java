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
import rappsilber.utils.InterruptSender;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.postgresql.PGConnection;
import rappsilber.config.RunConfig;
import rappsilber.db.ConnectionPool;
import rappsilber.ms.dataAccess.output.AbstractResultWriter;
import rappsilber.ms.dataAccess.output.BufferedResultWriter;
import rappsilber.ms.score.FragmentCoverage;
import rappsilber.ms.score.ScoreSpectraMatch;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.Sequence;
import rappsilber.ms.sequence.fasta.FastaHeader;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptideWeighted;
import rappsilber.ui.DBStatusInterfacfe;
import rappsilber.ui.StatusInterface;
import rappsilber.utils.MyArrayUtils;

/**
 *
 * @author stahir
 */
public class XiDBWriterBiogridXi3 extends AbstractResultWriter {

    private int m_search_id;
    private RunConfig m_config;
    // these values are the same throughout the class so just set there values on init
    private int alpha_id;
    private int beta_id;
    protected ConnectionPool m_connectionPool;
    protected Connection m_conn;

//    private PreparedStatement m_search_complete;
//    private PreparedStatement m_match_type;
//    protected PreparedStatement m_getIDsSingle;
    private PreparedStatement m_check_score;
    private PreparedStatement m_updateDB;
    private PreparedStatement m_insert_score;

    // ArrayList for materialized view EXPORT
    private int sqlBatchCount = 0;
    private int sqlBufferSize = 1000;
    private int results_processed = 0;
    private int top_results_processed = 0;
    private IDs ids;
    private HashMap<String,Long> runIds= new HashMap<>();
    private HashMap<String,Long> peakFileIds= new HashMap<>();
    
    private StringBuffer m_copySpectrumSource = new StringBuffer();
    private StringBuffer m_copySpectrumPeakFile = new StringBuffer();
    
    private HashMap<String,Long> proteinIDs = new HashMap<>();
    
    private boolean storePeaksAsArray = false;
    private final Object pingsnyc = new Object();
    
    private boolean stopped = false;
    
    private String[] scorenames= null;
    
    /** Through this we publish status infos into the database */
    private DBStatusInterfacfe m_statuspublisher;

    
    // holds the start Ids for each result to save
    protected class IDs {
        class id {
            long last;
            long next;
            long inc;

            public id(long last, long next, long inc) {
                this.last = last;
                this.next = next;
                this.inc = inc;
            }
            public id(long inc) {
                this(-1, 0, inc);
            }
            
        }
        
        private id run =  new id(10);
        private id peakfile =  new id(10);
        private id spec =  new id(2000);
        private id peak =  new id(100000);
        private id specMatch =  new id(10000);
        private id prot =  new id(10);
        private id pep =  new id(400);
        
        
        Connection dbconection;
        
        public IDs() {}
        
        public long reserveID(String name,long ids)  {
            try {
                Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Reserving {0} IDs for {1}", new Object[]{ids, name});
                Connection con = m_connectionPool.getConnection();
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery("SELECT reserve_ids('"+name+"',"+ids+");");
                rs.next();
                long ret = rs.getLong(1);
                rs.close();
                st.close();
                m_connectionPool.free(con);
                return ret;
            } catch (SQLException ex) {
                Logger.getLogger(XiDBWriterBiogridXi3.class.getName()).log(Level.SEVERE, null, ex);
                throw new Error(ex);
            }
        }


        public long nextRunId() {
            if (run.next <= run.last) 
                return run.next++;
            
            run.next=reserveID("run_id",run.inc);
            run.last = run.next + run.inc -1;
            return run.next++;
        }        

        public long nextPeakFileId() {
            if (peakfile.next <= peakfile.last) 
                return peakfile.next++;
            
            peakfile.next=reserveID("peakfile_id",run.inc);
            peakfile.last = peakfile.next + peakfile.inc -1;
            return peakfile.next++;
        }        

        public long nextSpectrumId() {
            if (spec.next <= spec.last)
                return spec.next++;
            
            spec.next=reserveID("spectrum_id",spec.inc);
            spec.last = spec.next + spec.inc -1;
            return spec.next++;
        }

        public long nextPeakId() {
            if (peak.next <= peak.last)
                return peak.next++;
            
            peak.next=reserveID("peak_id",peak.inc);
            peak.last = peak.next + peak.inc -1;
            return peak.next++;
        }
        
        public long nextPeptideId() {
            if (pep.next <= pep.last)
                return pep.next++;
            
            pep.next=reserveID("peptide_id",pep.inc);
            pep.last = pep.next+ pep.inc -1;
            return pep.next++;
        }

        public long nextProteinId() {
            if (prot.next <= prot.last)
                return prot.next++;
            
            prot.next=reserveID("protein_id",prot.inc);
            prot.last += prot.next + prot.inc -1;
            return prot.next++;

        }

        public long nextSpectrumMatchId() {
            if (specMatch.next <= specMatch.last)
                return specMatch.next++;
            
            specMatch.next=reserveID("spectrum_match_id",specMatch.inc);
            specMatch.last = specMatch.next + specMatch.inc - 1;
            return specMatch.next++;
        }
        
    }
    
    public XiDBWriterBiogridXi3(RunConfig config, ConnectionPool cp, int searchID) {
        // if we write here more likely then not we don't need the annotations anymore
        BufferedResultWriter.m_clearAnnotationsOnBuffer=true;
        try {
            m_config = config;
            sqlBufferSize =  m_config.retrieveObject("SQLBUFFER", sqlBufferSize); // after reading how many spectra do we batch

            m_connectionPool = cp;
            m_search_id = searchID;
            
            ids = new IDs();
            
//            setupMetaConnection();


            // Just get this value once and set them in the class
            alpha_id = -1;
            beta_id = -1;
            
            Connection con = m_connectionPool.getConnection();
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT id FROM match_type WHERE name = 'alpha'");
            rs.next();
            alpha_id = rs.getInt(1);
            rs.close();
            st.close();

            st = con.createStatement();
            rs = st.executeQuery("SELECT id FROM match_type WHERE name = 'beta'");
            rs.next();
            beta_id = rs.getInt(1);
            rs.close();
            st.close();
            
            ResultSet sm = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).executeQuery("SELECT id from spectrum_match where search_id = " + m_search_id + " limit 1");
            // do we have already results for this search?
            if (sm.next()) {
                String protein_query = "select id, accession_number, is_decoy , protein_length from protein where id in (select distinct protein_id  from (select *  from matched_peptide where search_id = "+m_search_id + ") mp inner join has_protein hp on mp.peptide_id = hp.peptide_id inner join protein p on hp.protein_id = p.id);";
                ResultSet proteins = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).executeQuery(protein_query);
                while (proteins.next()) {
                    proteinIDs.put(proteins.getString(2) +proteins.getBoolean(3)  +proteins.getInt(4),  proteins.getLong(1));
                }
                proteins.close();
            }
            sm.close();
            
            m_connectionPool.free(con);
            
            m_statuspublisher = new DBStatusInterfacfe(m_connectionPool, "UPDATE search SET status = ?, ping=now() WHERE id = " + m_search_id);
            m_config.addStatusInterface(m_statuspublisher);
            

        } catch (SQLException ex) {
            System.err.println("XiDB: problem when setting up XiDBWriter: " + ex.getMessage());
            m_connectionPool.closeAllConnections();
            System.exit(1);
        }
        
    }

    
    public void setProteinIDIncrement(int count) {
        if (count>0)
            ids.prot.inc = count;
    }
    

    public void setPepetideIDIncrement(int count) {
        if (count>0)
            ids.pep.inc = count;
    }

    public void setSpectrumIDIncrement(int count) {
        if (count>0)
            ids.spec.inc = count;
    }

    public void setSpectrumMatchIDIncrement(int count) {
        if (count>0)
            ids.specMatch.inc = count;
    }

    public void setRunIDIncrement(int count) {
        if (count>0)
            ids.run.inc = count;
    }

    public void setPeakIDIncrement(int count) {
        if (count>0)
            ids.peak.inc = count;
    }
    

    
    private StringBuffer m_copySpectrum = new StringBuffer();

    
    public void addSpectrum(long acq_id, long run_id, Spectra s) {

        m_copySpectrum.append(acq_id);
        m_copySpectrum.append(",");
        m_copySpectrum.append(run_id);
        m_copySpectrum.append(",");
        m_copySpectrum.append(s.getScanNumber() == null ? "": s.getScanNumber());
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
        m_copySpectrum.append(getSpectrumSourceID(s));
        m_copySpectrum.append(",");
        m_copySpectrum.append(getSpectrumPeakFileID(s));
        m_copySpectrum.append(",");
        m_copySpectrum.append(s.getReadID());
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
        m_spectrum_peakSql.append("\n");
    }

    private StringBuffer m_spectrum_allpeaksSql = new StringBuffer();

    public void addAllSpectrumPeak(Spectra s) {
        ArrayList<Double> mz = new ArrayList<>() ;
        ArrayList<Double> intensity = new ArrayList<>();
        for (SpectraPeak sp : s) {
            mz.add(sp.getMZ());
            intensity.add(sp.getIntensity());
        }
        
        m_spectrum_peakSql.append(s.getID());
        m_spectrum_peakSql.append(",\"{")
                .append(MyArrayUtils.toString(mz, ","))
                .append("}\",\"{")
                .append(MyArrayUtils.toString(intensity, ","))
                .append("}\"\n");
        
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
//        m_peptideSql.append(",");
//        m_peptideSql.append(m_search_id);
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

        String name  = fh.getName();
        if (name == null)
            name = fh.getAccession();
        if (name == null)
            name = "";
        
        String accession = fh.getAccession();
        if (accession == null)
            if (name.isEmpty())
                accession=""+p.getID();
            else
                accession = name;
        
        String description = fh.getDescription();
        if (description == null)
            description = name;
        
        m_proteinSql.append("\"");
        m_proteinSql.append(x);
        m_proteinSql.append("\",\"");
        m_proteinSql.append(name.replace("'", " ").replace(",", " "));
        m_proteinSql.append("\",\"");
        m_proteinSql.append(accession.replace("'", " ").replace(",", " "));
        m_proteinSql.append("\",\"");
        m_proteinSql.append(description.replace("\"", "\"\"").replace(",", " "));
//        m_proteinSql.append("',");
        m_proteinSql.append("\",\"");
        m_proteinSql.append(p.toString().replace("\"", "\"\""));
        m_proteinSql.append("\",");
        m_proteinSql.append(p.getID());
        m_proteinSql.append(",");
        m_proteinSql.append(p.isDecoy());
        m_proteinSql.append(",");
        m_proteinSql.append(p.length());
        m_proteinSql.append(",").append(p.target.getSource().getId());
//        m_proteinSql.append(",");
//        m_proteinSql.append(m_search_id);
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
//            m_hasProteinSql.append(",");
//            m_hasProteinSql.append(m_search_id);
            m_hasProteinSql.append("\n");

        }

    }

   
    private StringBuffer m_SpectrumMatchSql = new StringBuffer();

    public void addSpectrumMatch(long searchID, double score, long spectrumID, long id, boolean is_decoy, MatchedXlinkedPeptide match) {
        // COPY spectrum_match(search_id, score, spectrum_id, id, is_decoy, rank, autovalidated, precursor_charge, calc_mass, dynamic_rank, scorepeptide1matchedconservative, scorepeptide2matchedconservative, scorefragmentsmatchedconservative, scorespectrumpeaksexplained, scorespectrumintensityexplained, scorelinksitedelta, scoredelta, scoremoddelta)
        m_SpectrumMatchSql.append(searchID); // search_id
        m_SpectrumMatchSql.append(","); 
        m_SpectrumMatchSql.append(score);// score
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append(spectrumID);// spectrum_id
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append(id);//id
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append(is_decoy);//is_decoy
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append(match.getMatchrank()); //rank
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append(match.isValidated());//autovalidated
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append(match.getSpectrum().getPrecurserCharge()); //precursor_charge
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append(match.getCalcMass());//calc_mass
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append(match.getMatchrank() == 1 ? true : false); // dynamic_rank
        // scorepeptide1matchedconservative, scorepeptide2matchedconservative, scorefragmentsmatchedconservative, scorespectrumpeaksexplained, scorespectrumintensityexplained, scorelinksitedelta
        m_SpectrumMatchSql.append(",");
        //scorepeptide1matchedconservative, scorepeptide2matchedconservative, scorefragmentsmatchedconservative, scorespectrumpeaksexplained, scorespectrumintensityexplained, scorelinksitedelta, scoredelta, scoremoddelta,scoreMGCAlpha,ScoreMGCBeta,ScoreMGC,ScoreMGXRank, ScoreMGX, ScoreMGXDelta,scorecleavclpep1fragmatched, scorecleavclpep2fragmatched, assumed_precursor_mz,scores
        m_SpectrumMatchSql.append((int)match.getScore("peptide1 unique matched conservative"));
        //scorepeptide2matchedconservative
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append((int)match.getScore("peptide2 unique matched conservative"));
        //scorefragmentsmatchedconservative
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append((int)match.getScore("fragment unique matched conservative"));
        // scorespectrumpeaksexplained
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append(match.getScore("spectrum peaks coverage"));
        //scorespectrumintensityexplained, 
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append(match.getScore("spectrum intensity coverage"));
        //scorelinksitedelta
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append(match.getScore("LinkSiteDelta"));
        //scoredelta
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append(match.getScore("delta"));
        m_SpectrumMatchSql.append(",");
        //scoremoddelta
        m_SpectrumMatchSql.append(match.getScore("deltaMod"));
        
        m_SpectrumMatchSql.append(",");
        //scoreMGCAlpha
        m_SpectrumMatchSql.append(match.getScore("mgcAlpha"));
        m_SpectrumMatchSql.append(",");
        //ScoreMGCBeta
        m_SpectrumMatchSql.append(match.getScore("mgcBeta"));
        m_SpectrumMatchSql.append(",");
        //ScoreMGC
        m_SpectrumMatchSql.append(match.getScore("mgcScore"));
        m_SpectrumMatchSql.append(",");
        //ScoreMGXRank
        m_SpectrumMatchSql.append((int)match.getScore("mgxRank"));
        m_SpectrumMatchSql.append(",");
        //ScoreMGX
        m_SpectrumMatchSql.append(match.getScore("mgxScore"));
        m_SpectrumMatchSql.append(",");
        //ScoreMGXDelta
        m_SpectrumMatchSql.append(match.getScore("mgxDelta"));
        m_SpectrumMatchSql.append(",");
        //scorecleavclpep1fragmatched
        m_SpectrumMatchSql.append(match.getScore(FragmentCoverage.peptide+"1 " + FragmentCoverage.ccPepFrag) == 1);
        m_SpectrumMatchSql.append(",");
        //scorecleavclpep2fragmatched
        m_SpectrumMatchSql.append(match.getScore(FragmentCoverage.peptide+"2 " + FragmentCoverage.ccPepFrag) == 1);
        m_SpectrumMatchSql.append(",");
        //assumed_precursor_mz,scores
        m_SpectrumMatchSql.append(match.getSpectrum().getPrecurserMZ());
        Float[] scores = new Float[scorenames.length];
        for (int i = 0; i<scorenames.length; i++) 
            scores[i] = (float) match.getScore(scorenames[i]);
        m_SpectrumMatchSql.append(",");
        m_SpectrumMatchSql.append("\"{").append(MyArrayUtils.toString(scores, ",")).append("}\"");
        m_SpectrumMatchSql.append("\n");

        
    }
    private StringBuffer m_MatchedPeptideSql = new StringBuffer();

    public void addMatchedPeptide(Peptide p, long matchid, long matchtype, long link_position, boolean display_positon, Integer crosslinker_id, Integer crosslinker_number, double[] linkWeights) {
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
        m_MatchedPeptideSql.append(",");
        for (double d : linkWeights) {
            if (d !=0) {
                m_MatchedPeptideSql.append("\"{").append(MyArrayUtils.toString(linkWeights, ",")).append("}\"");
                break;
            }
        }
//        m_MatchedPeptideSql.append(",");
//        m_MatchedPeptideSql.append(pepweight == null ? "" : pepweight);
        m_MatchedPeptideSql.append("\n");
    }


    
    /**
     * tries up to ten times to execute the write out
     * @throws IOException
     * @throws SQLException 
     */
    private synchronized void executeCopy() throws IOException, SQLException {
        int countup = 0;
        boolean done = false;
        while (!done) {
            countup++;
            try {
                executeCopySingle();
                done = true;
            }catch (IOException| SQLException ex) {
                if (countup >= 10) {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "some database operation failed - several times - giving up", ex);
                    throw ex;
                }
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "some database operation failed - retrying", ex);
                try {
                    Thread.currentThread().sleep(1000*countup*countup);
                } catch (InterruptedException exI) {
                }
            }
        }
        
    }

    private synchronized void executeCopySingle() throws IOException, SQLException {
        //try {
        if (!stopped) {
            PGConnection postgres_con = null;
            Connection con = null;
            try {
                // Cast to a postgres connection
                con = m_connectionPool.getConnection();
                postgres_con = (PGConnection) con;
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                throw new Error(ex);
            }
            
            // check if the hiddenflag was set
            {
                InterruptSender isHidden = new InterruptSender(Thread.currentThread(), 5000, "hidden check").setCheckMethod(true);
                isHidden.start();
                try {
                    Statement st = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                    ResultSet rs = st.executeQuery("SELECT hidden FROM search WHERE id = " + m_search_id);
                    rs.next();
                    stopped=rs.getBoolean(1);
                    rs.close();
                } finally {
                    isHidden.cancel();
                }
            }
            if (stopped) {
                m_config.stopSearch();
                m_connectionPool.free(con);
            }


            try {
                con.setAutoCommit(false);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                m_connectionPool.free(con);
                throw ex;
            }
            
            {
                copySourceTable(postgres_con, con);
                copyPeakFileTable(postgres_con, con);
                
                //
                String spectrumCopy = m_copySpectrum.toString();
                byte[] sByte= spectrumCopy.getBytes();
                InputStream is = new ByteArrayInputStream(sByte);
    //            System.out.println("spectrum " + postgres_con.getCopyAPI().copyIn(
    //                    "COPY spectrum (acq_id, run_id, scan_number, elution_time_start, elution_time_end, id) " +
    //                    "FROM STDIN WITH CSV", is));

                InterruptSender isQuerry = new InterruptSender(Thread.currentThread(), 600000, "spectrum check").setCheckMethod(true);
                isQuerry.start();
                try {
                    postgres_con.getCopyAPI().copyIn(
                            "COPY spectrum (acq_id, run_id, scan_number, elution_time_start, elution_time_end, id, precursor_charge, precursor_intensity, precursor_mz, source_id, peaklist_id, scan_index) "
                            + "FROM STDIN WITH CSV", is);
                            //              1982,         1,       3182,               -1.0,       -1.0,148797622,               -1,                -1.0, 335.4323873,10001,2000080
                } catch (SQLException ex) {
                    String message = "error writing the spectra informations";
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, message, ex);
                    try {
                        PrintWriter pw = new PrintWriter(new FileOutputStream("/tmp/XiCopySqlError.csv", true));
                        pw.println("\n------------------------------------------------\n" + new Date() + " " + message);
                        ex.printStackTrace(pw);
                        pw.println("->");
                        pw.println(spectrumCopy);
                        pw.close();
                    } catch(Exception pwex) {
                        Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Error writing error log", ex);
                    }
                    try {
                        testConnectionAndRollBack(con);
                    } catch (SQLException ex1) {
                        Logger.getLogger(XiDBWriterBiogridXi3.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                    m_connectionPool.free(con);
                    throw ex;
                } finally{
                    isQuerry.cancel();
                }
            }
            
            {
                // Spectrum Peak
                final String spectrumPeakCopy = m_spectrum_peakSql.toString();
                byte spByte[] = spectrumPeakCopy.getBytes();
                InputStream isp = new ByteArrayInputStream(spByte);
    //            System.out.println("spectrum_peak " + postgres_con.getCopyAPI().copyIn(
    //                    "COPY spectrum_peak (spectrum_id, mz, intensity, id)" +
    //                    "FROM STDIN WITH CSV", isp));
                InterruptSender isQuerry = new InterruptSender(Thread.currentThread(), 600000, "spectrum_peak check").setCheckMethod(true);
                isQuerry.start();
                try {
                    postgres_con.getCopyAPI().copyIn(
                            "COPY spectrum_peak (spectrum_id, mz, intensity, id)"
                            + "FROM STDIN WITH CSV", isp);
                } catch (SQLException ex) {
                    String message = "error writing the spectra peak informations";
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, message, ex);
                    try {
                        PrintWriter pw = new PrintWriter(new FileOutputStream("/tmp/XiCopySqlError.csv", true));
                        pw.println("\n------------------------------------------------\n" + new Date() + " " + message);
                        ex.printStackTrace(pw);
                        pw.println("->");
                        pw.println(spectrumPeakCopy);
                    } catch(Exception pwex) {
                        Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Error writing error log", ex);
                    }
                    try {
                        testConnectionAndRollBack(con);
                    } catch (SQLException ex1) {
                        Logger.getLogger(XiDBWriterBiogridXi3.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                    m_connectionPool.free(con);
                    throw ex;
                } finally{
                    isQuerry.cancel();
                }
            }
            // Peptide
            {
                String peptideCopy = m_peptideSql.toString();
                byte peptideByte[] = peptideCopy.getBytes();
                InputStream pis = new ByteArrayInputStream(peptideByte);
    //             System.out.println("peptide " + postgres_con.getCopyAPI().copyIn(
    //                    "COPY peptide(sequence, mass, id) " +
    //                    "FROM STDIN WITH CSV", pis));
                InterruptSender isQuerry = new InterruptSender(Thread.currentThread(), 600000, "peptide check").setCheckMethod(true);
                isQuerry.start();
                try {
                    postgres_con.getCopyAPI().copyIn(
                            "COPY peptide(sequence, mass, id, peptide_length) "
                            + "FROM STDIN WITH CSV", pis);
                } catch (SQLException ex) {
                    String message = "error writing the peptide informations";
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, message, ex);
                    try {
                        PrintWriter pw = new PrintWriter(new FileOutputStream("/tmp/XiCopySqlError.csv", true));
                        pw.println("\n------------------------------------------------\n" + new Date() + " " + message);
                        ex.printStackTrace(pw);
                        pw.println("->");
                        pw.println(peptideCopy);
                        pw.flush();
                    } catch(Exception pwex) {
                        Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Error writing error log", ex);
                    }
                    try {
                        testConnectionAndRollBack(con);
                    } catch (SQLException ex1) {
                        Logger.getLogger(XiDBWriterBiogridXi3.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                    m_connectionPool.free(con);
                    throw ex;
                } finally{
                    isQuerry.interrupt();
                }
                peptideCopy = null;
            }

            // Protein
            {
                String proteinCopy = m_proteinSql.toString();
                //System.err.println("to save>> " + m_proteinSql.toString());
                byte protByte[] = proteinCopy.getBytes();
                InputStream protis = new ByteArrayInputStream(protByte);
    //             System.out.println("protein " + postgres_con.getCopyAPI().copyIn(
    //                    "COPY protein(name, sequence, id) " +
    //                    "FROM STDIN WITH CSV", protis));
                // System.err.println(protis);
                InterruptSender isQuerry = new InterruptSender(Thread.currentThread(), 600000, "protein check").setCheckMethod(true);
                isQuerry.start();
                try {
                    postgres_con.getCopyAPI().copyIn(
                            "COPY protein(header,name, accession_number, description, sequence, id, is_decoy, protein_length, seq_id) "
                            + "FROM STDIN WITH CSV", protis);
                } catch (SQLException ex) {
                    String message = "error writing the protein informations";
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, message, ex);
                    try {
                        PrintWriter pw = new PrintWriter(new FileOutputStream("/tmp/XiCopySqlError.csv", true));
                        pw.println("\n------------------------------------------------\n" + new Date() + " " + message);
                        ex.printStackTrace(pw);
                        pw.println("->");
                        pw.println(proteinCopy);
                    } catch(Exception pwex) {
                        Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Error writing error log", ex);
                    }
                    try {
                        testConnectionAndRollBack(con);
                    } catch (SQLException ex1) {
                        Logger.getLogger(XiDBWriterBiogridXi3.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                    m_connectionPool.free(con);
                    throw ex;
                } finally{
                    isQuerry.cancel();
                }
                proteinCopy=null;
            }

            // has_protein
            {
                String hpCopy = m_hasProteinSql.toString();
                byte hpByte[] = hpCopy.getBytes();
                // System.err.println(hpCopy);
                InputStream hpis = new ByteArrayInputStream(hpByte);
    //             System.out.println("has_protein " + postgres_con.getCopyAPI().copyIn(
    //                    "COPY has_protein(peptide_id, protein_id, peptide_position, display_site) " +
    //                    "FROM STDIN WITH CSV", hpis));
                InterruptSender isQuerry = new InterruptSender(Thread.currentThread(), 600000, "has_protein check").setCheckMethod(true);
                isQuerry.start();
                try {
                    postgres_con.getCopyAPI().copyIn(
                            "COPY has_protein(peptide_id, protein_id, peptide_position, display_site) "
                            + "FROM STDIN WITH CSV", hpis);
                } catch (SQLException ex) {
                    String message = "error writing the hasprotein table";
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, message, ex);
                    try {
                        PrintWriter pw = new PrintWriter(new FileOutputStream("/tmp/XiCopySqlErrorHasProtein.csv", true));
                        pw.println("\n------------------------------------------------\n" + new Date() + " " + message);
                        ex.printStackTrace(pw);
                        pw.println("->");
                        pw.println(hpCopy);
                        pw.flush();
                    } catch(Exception pwex) {
                        Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Error writing error log", ex);
                    }
                    try {
                        testConnectionAndRollBack(con);
                    } catch (SQLException ex1) {
                        Logger.getLogger(XiDBWriterBiogridXi3.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                    m_connectionPool.free(con);
                    throw ex;
                } finally{
                    isQuerry.cancel();
                }
                hpCopy = null;
            }




            
            // spetcrum_match
            {
                final String specCopy = m_SpectrumMatchSql.toString();
                byte specByte[] = specCopy.getBytes();
                InputStream specis = new ByteArrayInputStream(specByte);
    //             System.out.println("spectrum_match " + postgres_con.getCopyAPI().copyIn(
    //                    "COPY spectrum_match(search_id, score, spectrum_id, id) " +
    //                    "FROM STDIN WITH CSV", specis));
                InterruptSender isQuerry = new InterruptSender(Thread.currentThread(), 600000, "spectrum_match check").setCheckMethod(true);
                isQuerry.start();
                try {
                    postgres_con.getCopyAPI().copyIn(
                            "COPY spectrum_match(search_id, score, spectrum_id, id, is_decoy, rank, autovalidated, precursor_charge, calc_mass, dynamic_rank, scorepeptide1matchedconservative, scorepeptide2matchedconservative, scorefragmentsmatchedconservative, scorespectrumpeaksexplained, scorespectrumintensityexplained, scorelinksitedelta, scoredelta, scoremoddelta,scoreMGCAlpha,ScoreMGCBeta,ScoreMGC,ScoreMGXRank, ScoreMGX, ScoreMGXDelta,scorecleavclpep1fragmatched, scorecleavclpep2fragmatched, assumed_precursor_mz,scores) "
                            + "FROM STDIN WITH CSV", specis);
                } catch (SQLException ex) {
                    String message = "error writing the spectrum_match table";
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, message, ex);
                    try {
                        PrintWriter pw = new PrintWriter(new FileOutputStream("/tmp/XiCopySqlError.csv", true));
                        pw.println("\n------------------------------------------------\n" + new Date() + " " + message);
                        ex.printStackTrace(pw);
                        pw.println("->");
                        pw.println(specCopy);
                        pw.close();
                    } catch(Exception pwex) {
                        Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Error writing error log", ex);
                    }
                    try {
                        testConnectionAndRollBack(con);
                    } catch (SQLException ex1) {
                        Logger.getLogger(XiDBWriterBiogridXi3.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                    m_connectionPool.free(con);
                    throw ex;
                } finally{
                    isQuerry.cancel();
                }
            }
            

            {
                // matched_peptide
                final String mpCopy = m_MatchedPeptideSql.toString();
                byte mpByte[] = mpCopy.getBytes();
                InputStream mpis = new ByteArrayInputStream(mpByte);
    //             System.out.println("matched_peptide " + postgres_con.getCopyAPI().copyIn(
    //                    "COPY matched_peptide(peptide_id, match_id, match_type, link_position, display_positon) " +
    //                    "FROM STDIN WITH CSV", mpis));
                InterruptSender isQuerry = new InterruptSender(Thread.currentThread(), 600000, "matched_peptide check").setCheckMethod(true);
                isQuerry.start();
                try {
                    postgres_con.getCopyAPI().copyIn(
                            "COPY matched_peptide(peptide_id, match_id, match_type, link_position, display_positon, crosslinker_id, crosslinker_number, search_id,link_site_score) "
                            + "FROM STDIN WITH CSV", mpis);
                } catch (SQLException ex) {
                    String message = "error writing the matched_peptide table";
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, message, ex);
                    try { 
                        PrintWriter pw = new PrintWriter(new FileOutputStream("/tmp/XiCopySqlError.csv", true));
                        pw.println("\n------------------------------------------------\n" + new Date() + " " + message);
                        ex.printStackTrace(pw);
                        pw.println("->");
                        pw.println(mpCopy);
                    } catch(Exception pwex) {
                        Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Error writing error log", ex);
                    }
                    try {
                        testConnectionAndRollBack(con);
                    } catch (SQLException ex1) {
                        Logger.getLogger(XiDBWriterBiogridXi3.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                    m_connectionPool.free(con);
                    throw ex;
                } finally{
                    isQuerry.cancel();
                }
            }
            
            
            // join up with the threads, that write the materialized views
//            try {
//                waitSync.await();
//            } catch (InterruptedException ex) {
//                Logger.getLogger(XiDBWriterBiogridXi3.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (BrokenBarrierException ex) {
//                Logger.getLogger(XiDBWriterBiogridXi3.class.getName()).log(Level.SEVERE, null, ex);
//            }


            m_copySpectrumSource.setLength(0);
            m_copySpectrum.setLength(0);
            m_spectrum_peakSql.setLength(0);
            m_peptideSql.setLength(0);
            m_proteinSql.setLength(0);
            m_hasProteinSql.setLength(0);
            m_SpectrumMatchSql.setLength(0);
            m_MatchedPeptideSql.setLength(0);
            m_copySpectrumPeakFile.setLength(0);

            try {
                con.commit();
            } catch (SQLException ex1) {
                Logger.getLogger(XiDBWriterBiogridXi3.class.getName()).log(Level.SEVERE, null, ex1);
            }
            
            // free the connection
            m_connectionPool.free(con);
//            m_connectionPool.free(conExportMat);
//            m_connectionPool.free(conSpecViewerMat);




//        } catch (IOException ex) {
//            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
//        }
        }



    }// end method

    /**
     * Defines the scores to be written to the database. 
     * First it will try to recover scoresnames from the database (e.g. if a 
     * search crashed) to make sure we write them in the same order.
     * If no scores are defined in the search table then the scores names of the 
     * current match are taken.
     * @param match
     * @throws SQLException 
     */
    protected void setScoreNames(MatchedXlinkedPeptide match) throws SQLException {        
        if (scorenames != null) {
            return;
        }
        
        Connection con;
        try {
            // Cast to a postgres connection
            con = m_connectionPool.getConnection();
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            throw new Error(ex);
        }
        try {
            // try to retrive the scornames from the database
            Statement st = con.createStatement(ResultSet.TYPE_FORWARD_ONLY,  ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT);
            ResultSet rs = st.executeQuery("Select scorenames, id from search where id = " + m_search_id);
            rs.next();
            Array jdbcscorenames = rs.getArray(1);
            if (jdbcscorenames != null) {
                String[] values = (String[]) jdbcscorenames.getArray();
                if (values.length>0) {
                    scorenames = values;
                }
            }
            if (scorenames == null) {
                
                HashSet<String> sn = new HashSet<>();
                for (ScoreSpectraMatch ssm : m_config.getScores()) {
                    sn.addAll(MyArrayUtils.toCollection(ssm.scoreNames()));
                }
                String[] toStore = sn.toArray(new String[sn.size()]);
                Statement stOut = con.createStatement(ResultSet.TYPE_FORWARD_ONLY,  ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT);
                st.executeUpdate("UPDATE search SET scorenames = '{\""+ MyArrayUtils.toString(toStore, "\",\"") +"\"}' where id = " + m_search_id);
                scorenames = toStore;
            }
        } finally {
            m_connectionPool.free(con);
        }
    }

    protected void copySourceTable(PGConnection postgres_con, Connection con) throws SQLException, IOException {
        if (m_copySpectrumSource.length() >0) {
            String spectrumSourceCopy = m_copySpectrumSource.toString();
            byte sByte[] = spectrumSourceCopy.getBytes();
            InputStream is = new ByteArrayInputStream(sByte);
            //            System.out.println("spectrum " + postgres_con.getCopyAPI().copyIn(
            //                    "COPY spectrum (acq_id, run_id, scan_number, elution_time_start, elution_time_end, id) " +
            //                    "FROM STDIN WITH CSV", is));
            
            InterruptSender isQuerry = new InterruptSender(Thread.currentThread(), 600000, "spectrum_source check").setCheckMethod(true);
            isQuerry.start();
            try {
                postgres_con.getCopyAPI().copyIn(
                        "COPY spectrum_source (id, name) "
                                + "FROM STDIN WITH CSV", is);
            } catch (SQLException ex) {
                String message = "error writing the spectra informations";
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, message, ex);
                try {
                    PrintWriter pw = new PrintWriter(new FileOutputStream("/tmp/XiCopySqlError.csv", true));
                    pw.println("\n------------------------------------------------\n" + new Date() + " " + message);
                    ex.printStackTrace(pw);
                    pw.println("->");
                    pw.println(spectrumSourceCopy);
                    pw.close();
                } catch(Exception pwex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Error writing error log", ex);
                }
                try {
                    testConnectionAndRollBack(con);
                } catch (SQLException ex1) {
                    Logger.getLogger(XiDBWriterBiogridXi3.class.getName()).log(Level.SEVERE, null, ex1);
                }
                m_connectionPool.free(con);
                throw ex;
            } finally{
                isQuerry.cancel();
            }
            
        } else if (runIds.size() == 0) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "trying to store something but have no spectrum_source data");
        }
    }

    
    
    protected void copyPeakFileTable(PGConnection postgres_con, Connection con) throws SQLException, IOException {
        if (m_copySpectrumPeakFile.length() >0) {
            String spectrumPeakListCopy = m_copySpectrumPeakFile.toString();
            byte sByte[] = spectrumPeakListCopy.getBytes();
            InputStream is = new ByteArrayInputStream(sByte);
            //            System.out.println("spectrum " + postgres_con.getCopyAPI().copyIn(
            //                    "COPY spectrum (acq_id, run_id, scan_number, elution_time_start, elution_time_end, id) " +
            //                    "FROM STDIN WITH CSV", is));
            
            InterruptSender isQuerry = new InterruptSender(Thread.currentThread(), 600000, "spectrum_source check").setCheckMethod(true);
            isQuerry.start();
            try {
                postgres_con.getCopyAPI().copyIn(
                        "COPY PeakListFile (id, name) "
                                + "FROM STDIN WITH CSV", is);
            } catch (SQLException ex) {
                String message = "error writing the spectra informations";
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, message, ex);
                try {
                    PrintWriter pw = new PrintWriter(new FileOutputStream("/tmp/XiCopySqlError.csv", true));
                    pw.println("\n------------------------------------------------\n" + new Date() + " " + message);
                    ex.printStackTrace(pw);
                    pw.println("->");
                    pw.println(spectrumPeakListCopy);
                    pw.close();
                } catch(Exception pwex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Error writing error log", ex);
                }
                try {
                    testConnectionAndRollBack(con);
                } catch (SQLException ex1) {
                    Logger.getLogger(XiDBWriterBiogridXi3.class.getName()).log(Level.SEVERE, null, ex1);
                }
                m_connectionPool.free(con);
                throw ex;
            } finally{
                isQuerry.cancel();
            }
            
        } else if (peakFileIds.size() == 0) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "trying to store something but have no peakfile data");
        }
    }
    
    /**
     * Had a case where the roll back on a unusable connection simply ended in a endless wait for a lock
     * So prepend the roll back with a "select 1+1" to test if the connection is actually usable.
     * @param con
     * @throws SQLException 
     */
    protected void testConnectionAndRollBack(Connection con) throws SQLException {
        Statement st = con.createStatement();
        st.execute("Select 1+1;");
        st.close();
        con.rollback();
    }

    @Override
    public void writeHeader() {
        // updating the search table to 'executing' if not already done - check
    }


    @Override
    public synchronized void writeResult(MatchedXlinkedPeptide match) {
        if (stopped)
            return;
        ++results_processed;
        
        {
            int countdown = 10;
            
            while (countdown-- >0 && scorenames == null) {
                try {
                    setScoreNames(match);
                } catch (SQLException ex){}
            }

        }
        
        if (match.getMatchrank() == 1)
            top_results_processed++;

        sqlBatchCount++;
        if (sqlBatchCount > sqlBufferSize) {  
            try {
                executeCopy();
            } catch (IOException | SQLException ex) {
                throw new Error(ex);
            }
            sqlBatchCount = 0;
        }


        // 1. Check spectrum info spectrum spectrum peak
        Spectra matched_spectrum = match.getSpectrum();

        if (matched_spectrum.getOrigin().getID() == -1) {
            saveSpectrum(matched_spectrum.getOrigin(), ids);
        }




        // 2. Save spectrum_match info
        double score = match.getScore("match score");
        if (Double.isNaN(score) || Double.isNaN(score)) {
            score = 0;
        }
        long spectrum_match_id = saveSpectrumMatch(match.getScore("match score"), matched_spectrum.getOrigin().getID(), ids, match.isDecoy(), match);

        // 3. Save the protein/peptide sequences
//        mon3 = MonitorFactory.start("savePeptide1()");
        boolean alpha = true;
        Peptide[] peps = match.getPeptides();
        for (int p = 0; p<peps.length; p++) {
            double[][] lw;
            lw=new double[1][];
            if (match instanceof MatchedXlinkedPeptideWeighted){
                lw[0] = ((MatchedXlinkedPeptideWeighted)match).getLinkageWeights(p);
            } else {
                lw[0] = new double[peps[p].length()];
            }
            savePeptide(peps[p], spectrum_match_id, alpha, match.getLinkSites(peps[p]), ids, match.getCrosslinker() == null? null : match.getCrosslinker().getDBid(),0,lw);
            alpha = false;
        }




    }// write result

    private long getSpectrumSourceID(Spectra matched_spectrum){
        String run = matched_spectrum.getRun();
        Long id = runIds.get(run);
        if (id == null) {
            id = ids.nextRunId();
            runIds.put(run, id);
            m_copySpectrumSource.append(id).append(",\"").append(run.replaceAll("\"", "\\\"")).append("\"\n");
        }
        return id;
    }

    private long getSpectrumPeakFileID(Spectra matched_spectrum){
        String peakfile = matched_spectrum.getPeakFileName();
        Long id = peakFileIds.get(peakfile);
        if (id == null) {
            id = ids.nextPeakFileId();
            peakFileIds.put(peakfile, id);
            m_copySpectrumPeakFile.append(id).append(",\"").append(peakfile.replaceAll("\"", "\\\"")).append("\"\n");
        }
        return id;
    }

    private long getPeakFileID(Spectra matched_spectrum){
        String run = matched_spectrum.getRun();
        Long id = runIds.get(run);
        if (id == null) {
            id = ids.nextRunId();
            runIds.put(run, id);
            m_copySpectrumSource.append(id).append(",\"").append(run.replaceAll("\"", "\\\"")).append("\"\n");
        }
        return id;
    }

    private void saveSpectrum(Spectra matched_spectrum, IDs ids) {

        
        
        matched_spectrum.setID(ids.nextSpectrumId());
        addSpectrum(matched_spectrum.getAcqID(), matched_spectrum.getRunID(), matched_spectrum);

        if (storePeaksAsArray) {
            addAllSpectrumPeak(matched_spectrum);
        } else {
            for (SpectraPeak sp : matched_spectrum.getPeaksArray()) {
                sp.setID(ids.nextPeakId());
                addSpectrumPeak(matched_spectrum, sp);

            }
        }
        



    }//



    private void savePeptide(Peptide peptide, long match_id, boolean alpha, int[] linkSites, IDs result_ids, Integer crosslinker_id, Integer crosslinker_number,double[][] linkWeight) {

        // if this is the first time you see a peptide, then save it to the DB, and set the ID
        // Likewise do the same with the Protein
//        try {
        if (peptide.getID() == -1) {

            //             id       | bigint  | not null default nextval('peptide_id_seq'::regclass)
            //             sequence | text    |
            //             mass     | numeric |
            peptide.setID(result_ids.nextPeptideId());
            addPeptide(peptide);


            // Now check if there is a problem with the Proteins i.e. have we seen them before

            int first = 0;
            for (Peptide.PeptidePositions pp : peptide.getPositions()) {
                ++first;
                Sequence protein = pp.base;
                if (protein.getID() == -1) {

                    // check if we recovered the id from the database
                    Long id = proteinIDs.get(protein.getSplitFastaHeader().getAccession()+protein.isDecoy()+protein.length());
                    if (id == null) {
                        protein.setID(result_ids.nextProteinId());
                        addProtein(protein);

                    } else {
                        protein.setID(id);
                    }


                }


            }
            addHasProtein(peptide);





        }// end if peptide

        // Save matched_peptide information

        // For each link position , save the match in matched peptides
        // initially this we are forwardede only 1, in future more will come
        for (int i = 0; i < linkSites.length; i++) {
            addMatchedPeptide(peptide, match_id, (alpha ? alpha_id : beta_id), linkSites[i], true, crosslinker_id, crosslinker_number, linkWeight[i]);
        }// end for


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
            try {
                executeCopy();
            } catch (IOException | SQLException ex) {
                throw new Error(ex);
            }
        }

    }
    
    

    @Override
    public void finished() {

        InterruptSender isQuerry = new InterruptSender(Thread.currentThread(), 5000, "spectrum_source check").setCheckMethod(true);
        try {
            flush();
//                executeSQL();
//                  writeInserts();
            // our search is done
            
            isQuerry.start();
            m_statuspublisher.executeSql("UPDATE search "
                + "SET is_executing = 'false', status = 'completed', completed = 'true', percent_complete = 100 "
                + "WHERE id = "+m_search_id + "; ");
            // runtime stats
            Logger.getLogger(this.getClass().getName()).log(Level.INFO,"XiDBWriterCopySql - Total results: " + getResultCount() + "\n-------------");

        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,"XiDB: problem when writing last results " + ex.getMessage());
            m_connectionPool.closeAllConnections();
            System.exit(1);
        } finally {
            isQuerry.cancel();
        }

        super.finished();
    }

    private long saveSpectrumMatch(double match_score, long spec_id, IDs result_ids, boolean is_decoy, MatchedXlinkedPeptide match) {
        long spec_match_id = result_ids.nextSpectrumMatchId();
//        try{
        addSpectrumMatch(m_search_id, match_score, spec_id, spec_match_id, is_decoy, match);

        return spec_match_id;
    }

    
    static boolean pinging  =false;
    static long pingTime  = 0;

    /**
     * ping writes the current time into the ping column of the database. 
     * This should be used to automatically detect dead searches.
     */
    public void  ping() {
        
        // I run this in a separate thread, to prevent the thread calling this
        // gets hung up on trying to ping the db
        Runnable runnable = new Runnable() {
            public void run() {
                long now=Calendar.getInstance().getTimeInMillis();
                boolean wasPinging = pinging;
                if ((wasPinging) || (now - pingTime > 10000)) {
                    synchronized (pingsnyc) {
                        pinging=true;
                        try {
                            Connection c = m_connectionPool.getConnection();
                            try {
                                Statement st = c.createStatement();
                                st.executeUpdate("UPDATE search set ping = now() where id = " + m_search_id);
                            } catch (SQLException ex) {
                                Logger.getLogger(XiDBWriterBiogridXi3.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            m_connectionPool.free(c);
                        } catch (SQLException ex) {
                            Logger.getLogger(XiDBWriterBiogridXi3.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        if (!wasPinging)
                            pinging=false;
                    }
                }
            }
        };
        Thread p = new Thread(runnable,"DB-ping");
        p.setDaemon(true);
        p.start();
        
    }
    

}
