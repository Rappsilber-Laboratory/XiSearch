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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.db.ConnectionPool;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.dataAccess.AbstractSpectraAccess;
import rappsilber.ms.spectra.Spectra;

/**
 * an iterator for access spectras from a database
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public abstract class DBSpectraAccess extends AbstractSpectraAccess{

    /** how to connect to the database */
    protected ConnectionPool m_connectionPool;

    /** one connection to use */
    protected Connection     m_dbConnection;

    /** the next spectra to be delivered */
    protected Spectra        m_nextSpectra = null;

    /** the currently active spectra */
    protected Spectra        m_currentSpectra = null;

    /** should be set from the opentable function */
    protected boolean        m_tableOpen = false;

    /** default tolerance unit used for any pending comparisons */
    private ToleranceUnit  m_tolerance = null;
    
    /** new database based spectra iterator
     * @param connection  the connectionpool to use for accesing the spectra
     * @throws SQLException
     */
    public DBSpectraAccess(ConnectionPool connection) throws SQLException{
        m_connectionPool = connection;
        m_dbConnection  =    m_connectionPool.getConnection();
    }

    /** 
     * should open the table
     * @throws SQLException
     */
    public abstract void    openTable() throws SQLException;

    /**
     * reads a single spectra from database and returns it
     * @return the spectra
     */
    public abstract Spectra readSpectra();

    /**
     * is there another spectra to have
     * @return the spectra
     */
    public boolean hasNext() {
        if (!m_tableOpen){
            try {
                openTable();
                m_nextSpectra = readSpectra();
            } catch (SQLException ex) {
                Logger.getLogger(DBSpectraAccess.class.getName()).log(Level.SEVERE, "could not open the database table", ex);
                return false;
            }
        }
        return m_nextSpectra != null;
    }

    /**
     * make the next spectra the current one and read the next one from the
     * database
     * @return the spectra
     */
    public Spectra next() {
        Spectra current = m_nextSpectra;
        m_nextSpectra = readSpectra();
        return current;
    }

    /**
     * return the currently active spectra
     * @return the currently spectra
     */
    public Spectra current() {
        return m_currentSpectra;
    }

    /**
     * @return the tolerance
     */
    public ToleranceUnit getTolerance() {
        return m_tolerance;
    }

    /**
     * @param tolerance the tolerance to set
     */
    public void setTolerance(ToleranceUnit tolerance) {
        this.m_tolerance = tolerance;
    }




}
