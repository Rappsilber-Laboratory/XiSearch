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
package rappsilber.ui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.db.ConnectionPool;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class DBStatusInterfacfe implements StatusInterface{
    private ConnectionPool m_connection_pool;
    private Connection m_connection;
    private PreparedStatement m_SQLSetStatus;
    private String             m_SQLSetStatusString;
    private String m_status = "";
    private AtomicBoolean m_statusIsBeingSet=new AtomicBoolean(false);


    public DBStatusInterfacfe(ConnectionPool connection_pool, String SQL) throws SQLException {
        m_connection_pool=connection_pool;
        setConnection(m_connection_pool.getConnection(), SQL);
   }

    public DBStatusInterfacfe(Connection connection, String SQL) throws SQLException {
        setConnection(connection, SQL);
   }

    protected void setConnection(Connection connection, String SQL) throws SQLException {
        m_connection = connection;
        m_SQLSetStatusString = SQL;
        m_SQLSetStatus = m_connection.prepareStatement(SQL);
    }
    
    private boolean ensureConnection()  {
        boolean usable = false;
        try {
            m_connection.createStatement().execute("SELECT 1+1");
            usable=true;
        }catch(SQLException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "error using connection", ex);
            m_SQLSetStatus = null;
        }
        if (!usable) {
            int timeout = 10;
            while (!usable && timeout-- >0) {
                if (m_connection_pool != null) {
                    Connection tofree = m_connection;
                    m_connection = null;
                    Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Trying to open new one");
                    m_connection_pool.free(tofree);
                    try {
                        Connection newCon = m_connection_pool.getConnection();
                        setConnection(newCon, m_SQLSetStatusString);
                        usable = true;
                    } catch (SQLException ex) {
                        Logger.getLogger(DBStatusInterfacfe.class.getName()).log(Level.SEVERE, "Error getting new connection", ex);
                        try {
                            Thread.currentThread().sleep(500);
                        } catch (InterruptedException ex1) {
                        }
                    }
                }
            }
        }
        return usable;
    }
    
    @Override
    public synchronized  void setStatus(String status) {
        try {
            m_statusIsBeingSet.set(true);
            if (ensureConnection()) {
                m_status = status;
                m_SQLSetStatus.setString(1, status);
                m_SQLSetStatus.executeUpdate();
            }
            m_statusIsBeingSet.set(false);
        } catch (SQLException ex) {
            Logger.getLogger(DBStatusInterfacfe.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public String getStatus() {
        return m_status;
    }

}
