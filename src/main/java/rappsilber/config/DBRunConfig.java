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
package rappsilber.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashSet;
import rappsilber.db.ConnectionPool;
import rappsilber.ui.DBStatusInterface;
import rappsilber.utils.XiVersion;

/**
 *
 * @author stahir
 */
public class DBRunConfig extends AbstractRunConfig{

    private ConnectionPool m_connectionPool;
    private Connection m_connection;
    private String m_configQuery;
    private PreparedStatement m_updateExec;
    private PreparedStatement m_getConfig;
    private DBStatusInterface m_statuspublisher;

    // settings we will use
    private String m_base_directory;
    
    protected int[] m_search_id;
    protected String[] m_random_id;
    
 

    // constructor
    public DBRunConfig(ConnectionPool cp) throws SQLException{
        this(cp.getConnection());
        this.m_connectionPool = cp;
        
    }// end constructor

    // constructor
    public DBRunConfig(Connection c) throws SQLException{
        m_connection = c;
        m_configQuery = "SELECT description from xi_config WHERE search_id = ? OR search_id = '-1'";
        m_updateExec = m_connection.prepareStatement("UPDATE search SET is_executing = 'true', status='executing', notes = CASE WHEN notes is null THEN  'xi-version: "+ XiVersion.getVersionString() +"' ELSE notes || '\nxi-version: "+ XiVersion.getVersionString() +"' END    WHERE id = ?");
        m_getConfig = m_connection.prepareStatement(m_configQuery);

        // initialize settings
        m_base_directory = "";

        
    }// end constructor
    
    public void readConfig(int searchID){
        readConfig(new int[]{searchID});
    }

    
    public void readConfig(int[] searchIDs){
        Connection con = null;
        try {
            if (m_connection == null) {
                con =  m_connectionPool.getConnection();
            } else
                con = m_connection;
            StringBuffer sbsids = new StringBuffer();
            for (int s :searchIDs) {
                sbsids.append(s);
                sbsids.append(",");
            }
            String sids = sbsids.substring(0, sbsids.length()-1);

            m_configQuery = "SELECT description from xi_config WHERE search_id = -1 OR search_id in ("+ sids +")";
            m_updateExec = con.prepareStatement("UPDATE search SET is_executing = 'true', status='executing' , notes = CASE WHEN notes is null THEN  'xi-version: "+ XiVersion.getVersionString() +"' ELSE notes || '\nxi-version: "+ XiVersion.getVersionString() +"' END    WHERE id in ("+ sids +")");
            m_getConfig = con.prepareStatement(m_configQuery);

            
            m_search_id = searchIDs;
            // Read the parameters/config-lines associated with this searchId
            //m_getConfig.setInt(1, searchIDs[0]);
            ResultSet rs = m_getConfig.executeQuery();
            // Call evaluateConfigLine() - from class you extend - to accept parameters
            HashSet<String> readlines = new HashSet<>();
            while (rs.next()) {
                String c_line = rs.getString(1);
                if (readlines.contains(c_line)) {
                    continue;
                } else 
                    readlines.add(c_line);
                // System.out.println(c_line);
                if (!evaluateConfigLine(c_line)) {
                    String[] parts = c_line.split(":", 2);
                    storeObject(parts[0].toUpperCase(), parts[1]);
                }
            }
            rs = con.createStatement().executeQuery("Select id, random_id from search where id in ("+sids+");");
            m_random_id= new String[getSearchIDs().length];
            while (rs.next()) {
                String rid = rs.getString(2);
                int sid = rs.getInt(1);
                for (int i=0;i<getSearchIDs().length; i++) {
                    if (sid == getSearchIDs()[i]) {
                        m_random_id[i]=rid;
                        break;
                    }
                }
            }
            

            if (m_connectionPool!= null) 
                m_connectionPool.free(con);
            
        } catch (ParseException ex) {
            String message = "XiDB: problem with evaluating config line: " + ex.getMessage();
            System.err.println(message);
            getStatusInterface().setStatus(message);
            if (con != null && m_connectionPool!= null) 
                m_connectionPool.free(con);
            ex.printStackTrace();
            System.exit(1);
        } catch (SQLException ex) {
            String message = "XiDB: accessing DB for config view: " + ex.getMessage();
            System.err.println(message);
            getStatusInterface().setStatus(message);
            ex.printStackTrace();
            if (con != null && m_connectionPool!= null) 
                m_connectionPool.free(con);
            System.exit(1);
        }

        
    }// end method readConfig
    
    public void setExecuting() {
        try {
            //m_updateExec.setInt(1, m_search_id);
            m_updateExec.execute();
        } catch (SQLException ex) {
            String message = "XiDB: accessing DB for config view: " + ex.getMessage();
            System.err.println(message);
            getStatusInterface().setStatus(message);
            ex.printStackTrace();
            m_connectionPool.free(m_connection);
            System.exit(1);
        }
        
    }


    public String getBaseDirectory(){
        // Note that these settings, are not evalutaing by method evaluate() and require further investigation

       m_base_directory = (String)super.retrieveObject("BASE_DIRECTORY_PATH");
       return m_base_directory;
        
    }// end method setBaseSettings
    /**
     * @return the m_base_directory
     */
    public String getbase_directory() {
        return m_base_directory;
    }

    /**
     * @param m_base_directory the m_base_directory to set
     */
    public void setBaseDirectory(String m_base_directory) {
        this.m_base_directory = m_base_directory;
    }

    /**
     * @return the m_search_id
     */
    public int[] getSearchIDs() {
        return m_search_id;
    }

    /**
     * @return the m_random_id
     */
    public String[] getRandomIDs() {
        return m_random_id;
    }

    /**
     * @return the m_connectionPool
     */
    public ConnectionPool getConnectionPool() {
        return m_connectionPool;
    }

    /**
     * @return the m_connection
     */
    public Connection getConnection() {
        return m_connection;
    }

}// end class
