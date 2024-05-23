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
package rappsilber.ms.dataAccess.msm;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.DBRunConfig;
import rappsilber.db.ConnectionPool;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.utils.MyArrayUtils;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class DBPeakList extends AbstractMSMAccess {



    private ArrayList<Integer> m_searchID = null;
    private ArrayList<Integer> m_spectrumIds = null;
    ConnectionPool  m_connectionpool;
    Connection      m_connection;
    ResultSet       m_spectra;
    ResultSet       m_peaks;
    Spectra         m_current = null;
    Spectra         m_next = null;
    boolean         m_hasnext = false;
    DBRunConfig     m_conf;
    int             m_countRead = 0;
//    private ToleranceUnit   m_FragmentTolerance;
    

    public DBPeakList() {
        setInputPath("DB");
    }
    
    public void openSearch(int searchID, DBRunConfig conf) throws SQLException {
        setInputPath("DB:"+searchID);
        openSearch(MyArrayUtils.toCollection(new Integer[]{searchID}),conf);
    }
    
    public void openSearch(Collection<Integer> searchIDs, DBRunConfig conf) throws SQLException {
        this.m_conf = conf;
        this.setSearchID(new ArrayList<Integer>());
        getSearchID().addAll(searchIDs);
        m_connectionpool = conf.getConnectionPool();
        if (m_connectionpool == null) {
            m_connection = conf.getConnection();
        } else {
            m_connection = m_connectionpool.getConnection();
        }
        m_spectra = m_connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
                .executeQuery("SELECT sm.spectrum_id,scan_number,precursor_charge,precursor_intensity,precursor_mz, ss.name  FROM "
                        + "(SELECT spectrum_id FROM spectrum_match WHERE search_id in (" +  MyArrayUtils.toString(getSearchID(), ",") +") and dynamic_rank) sm INNER JOIN "
                        + "spectrum s on sm.spectrum_id = s.id INNER JOIN spectrum_source ss on s.source_id = ss.id   ORDER BY spectrum_id");
        m_peaks = m_connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
                .executeQuery("SELECT s.id, mz,intensity  FROM "
                        + "(SELECT spectrum_id FROM spectrum_match WHERE search_id in (" +  MyArrayUtils.toString(getSearchID(), ",") +") and dynamic_rank) sm INNER JOIN "
                        + "spectrum s on sm.spectrum_id = s.id INNER JOIN "
                        + "spectrum_peak sp on s.id = sp.spectrum_id ORDER BY sm.spectrum_id, mz");
        m_spectra.next();
        m_peaks.next();
        m_next = readSpectra();
        m_hasnext = m_next != null;
    }

    public void open(Collection<Integer> spectrumIDs, DBRunConfig conf) throws SQLException {
        this.m_conf = conf;
        this.setSpectrumIds(new ArrayList<Integer>());
        getSpectrumIds().addAll(spectrumIDs);
        m_connectionpool = conf.getConnectionPool();
        if (m_connectionpool == null) {
            m_connection = conf.getConnection();
        } else {
            m_connection = m_connectionpool.getConnection();
        }
        m_spectra = m_connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
                .executeQuery("SELECT s.id,scan_number,precursor_charge,precursor_intensity,precursor_mz, "
                        + " ss.name  FROM "
                        + "(SELECT * FROM spectrum WHERE id in ("+MyArrayUtils.toString(getSpectrumIds(), ",") + "))s "
                        + " INNER JOIN spectrum_source ss on s.source_id = ss.id ORDER BY s.id");
        m_peaks = m_connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
                .executeQuery("SELECT s.id, mz,intensity  FROM "
                        + "(SELECT * FROM spectrum WHERE id in ("+MyArrayUtils.toString(getSpectrumIds(), ",") + "))s INNER JOIN "
                        + "spectrum_peak sp on s.id = sp.spectrum_id ORDER BY spectrum_id, mz");
        m_spectra.next();
        m_peaks.next();
        m_next = readSpectra();
        m_hasnext = m_next != null;
    }
    
    protected Spectra readSpectra() throws SQLException {
        
        if (!m_spectra.isAfterLast()) {
            int specID = m_spectra.getInt(1);
            int scanNumber = m_spectra.getInt(2);
            int precCharge =  m_spectra.getInt(3);
            double precInt =  m_spectra.getDouble(4);
            double precMz =  m_spectra.getDouble(5);
            String run =  m_spectra.getString(6);
            Spectra s = new Spectra(-1, precInt, precMz, precCharge, run, scanNumber);
            
            while ((!m_peaks.isAfterLast()) && m_peaks.getInt(1) == specID ){
                double mz = m_peaks.getDouble(2);
                double inten = m_peaks.getDouble(3);
                SpectraPeak sp = new SpectraPeak(mz, inten);
                s.addPeak(sp);
                if (!m_peaks.next()) {
                    break;
                }
            }
            m_spectra.next();
            return s;
        }
        
        return null;
        
    }
    
    /**
     * @return the m_searchID
     */
    public ArrayList<Integer> getSearchID() {
        return m_searchID;
    }

    /**
     * @param m_searchID the m_searchID to set
     */
    public void setSearchID(ArrayList<Integer> m_searchID) {
        this.m_searchID = m_searchID;
    }

    /**
     * @return the m_spectrumIds
     */
    public ArrayList<Integer> getSpectrumIds() {
        return m_spectrumIds;
    }

    /**
     * @param m_spectrumIds the m_spectrumIds to set
     */
    public void setSpectrumIds(ArrayList<Integer> m_spectrumIds) {
        this.m_spectrumIds = m_spectrumIds;
    }

//    /**
//     * @return the m_FragmentTolerance
//     */
//    public ToleranceUnit getFragmentTolerance() {
//        return m_FragmentTolerance;
//    }

//    /**
//     * @param m_FragmentTolerance the m_FragmentTolerance to set
//     */
//    public void setFragmentTolerance(ToleranceUnit m_FragmentTolerance) {
//        this.m_FragmentTolerance = m_FragmentTolerance;
//    }    
    
    @Override
    public Spectra current() {
        return m_current;
    }

    @Override
    public int getSpectraCount() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void gatherData() throws FileNotFoundException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int countReadSpectra() {
        return m_countRead;
    }

    @Override
    public boolean canRestart() {
        return true;
    }

    @Override
    public void restart() throws IOException {
        close();
        if (getSpectrumIds() != null) {
            try {
                open(getSpectrumIds(), m_conf);
            } catch (SQLException ex) {
                Logger.getLogger(DBPeakList.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (getSearchID() != null){
            try {
                open(getSearchID(), m_conf);
            } catch (SQLException ex) {
                Logger.getLogger(DBPeakList.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void close() {
        try {
            m_spectra.close();
            if (m_connectionpool != null ){
                m_connectionpool.free(m_connection);
            }
        } catch (SQLException ex) {
            Logger.getLogger(DBPeakList.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    @Override
    public boolean hasNext() {
        return m_hasnext;
    }

    @Override
    public Spectra next() {
        try {
            m_current = m_next;
            m_next = readSpectra();
            m_hasnext = m_next != null;
            if (m_current != null) {
                m_countRead++;
                m_current.setTolearance(getToleranceUnit());
                if (m_current.getAdditionalMZ() == null && m_conf!=null) {
                    m_current.setAdditionalMZ(m_conf.getAdditionalPrecursorMZOffsets());
                    if (m_current.getPrecoursorChargeAlternatives().length >1) {
                        HashSet<Double> mz = new HashSet<>();
                        if (m_conf.getAdditionalPrecursorMZOffsets() != null) {
                            mz.addAll(m_conf.getAdditionalPrecursorMZOffsets());
                        }
                        mz.addAll(m_conf.getAdditionalPrecursorMZOffsetsUnknowChargeStates());
                        m_current.setAdditionalMZ(mz);
                    }
                }  
            }
            return m_current;
        } catch (SQLException ex) {
            Logger.getLogger(DBPeakList.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
}
