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
    private String m_status = "";


    public DBStatusInterfacfe(ConnectionPool connection_pool, String SQL) throws SQLException {
        this(connection_pool == null?null :connection_pool.getConnection(), SQL);
        m_connection_pool=connection_pool;
   }

    public DBStatusInterfacfe(Connection connection, String SQL) throws SQLException {
        m_connection = connection;
        m_SQLSetStatus = m_connection.prepareStatement(SQL);
   }
    
    @Override
    public synchronized  void setStatus(String status) {
        try {
            if (!m_connection.isClosed()) {
                m_status = status;
                m_SQLSetStatus.setString(1, status);
                m_SQLSetStatus.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DBStatusInterfacfe.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public String getStatus() {
        return m_status;
    }

}
