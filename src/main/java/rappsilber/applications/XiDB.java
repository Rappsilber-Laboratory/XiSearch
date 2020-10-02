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
package rappsilber.applications;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.DBRunConfig;
import rappsilber.db.ConnectionPool;
import rappsilber.gui.components.DebugFrame;
import rappsilber.utils.MyLoggingUtils;

/**
 *
 * @author stahir
 * Class to load the configuration
 *  - read sequence files and,
 *  - msmfiles selected and start the search
 *
 */
public class XiDB {
    // m_connectionPool for our search
    private ConnectionPool m_connection_pool;
    // variable that holds the search id of this object
    private int m_search_id;
    private String m_search_name;
    public static String m_db_connection =  "jdbc:postgresql:xi";
    public static String m_db_user =  "xi";
    public static String m_db_passwd =  "";
    // Deals with config
    private DBRunConfig m_dbconfig;

    //Sets up and proceeds to search
    private XiDBSearch m_xi_search;

    public XiDB(String s_id, String s_name){
        this(s_id,s_name, m_db_connection);

    }

    public XiDB(String s_id, String s_name, String db_connection) {
        m_db_connection = db_connection;

        m_db_connection = System.getProperty("XI_DB_CONNECTION", m_db_connection);
        m_db_user = System.getProperty("XI_DB_USER", m_db_user);
        m_db_passwd = System.getProperty("XI_DB_PASSWD", m_db_passwd);

        // 1. Set the search ID and name
        this.m_search_id = Integer.parseInt(s_id);
        this.m_search_name = s_name;

        // 2. Set the conneciton pool
        try {
            m_connection_pool = new ConnectionPool("org.postgresql.Driver", m_db_connection, m_db_user, m_db_passwd);
            m_dbconfig = new DBRunConfig(m_connection_pool);

            String DBOutput = System.getProperty("XI_DB_OUTPUT", "YES");

            if (!DBOutput.contentEquals("YES"))
                m_dbconfig.clearStatusInterface();
            
            m_xi_search = new XiDBSearch(m_connection_pool);

            
            

        } catch (SQLException e) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,"XiDB: Error with connection pool: ", e);
                m_connection_pool.closeAllConnections();
                System.exit(1);
        }
    }




    


    public static void main(String args[]){
        // We launch XiDB with the search ID as a command-line argument

        DebugFrame  df = null;
        Thread dfThread = null;
        XiDB xi = null;
        try {
            String debugframe = System.getProperty("XI_SHOW_DEBUG", "1").toLowerCase();
            MyLoggingUtils.SystemPropertyToLoggerLevel();
            if (debugframe.equals("1") || debugframe.equals("t") || debugframe.equals("true") || debugframe.equals("yes") || debugframe.equals("y")) {
                df = new DebugFrame(args[0] + " - "+ args[1], null);
                final DebugFrame mdf = df;
                System.err.println("Showing debug window!");
                dfThread = new Thread(new Runnable() {

                    public void run() {
                         mdf.setVisible(true);
                    }
                });
                dfThread.setDaemon(true);
                dfThread.start();
            } else {
                System.err.println("No debug window is shown!");
            }


            // reset this later
            xi = new XiDB(args[0], args[1]);
    //       XiDB xi = new XiDB("3", "test search 2");

            // Only print severe messages to System.err
            Logger.getLogger("rappsilber").setLevel(Level.SEVERE);
            Logger.getLogger("org.rappsilber").setLevel(Level.SEVERE);

            // 1. Read the configuration/parameters for search
            xi.m_dbconfig.readConfig(xi.m_search_id);
            String DBOutput = System.getProperty("XI_DB_OUTPUT", "YES");

            if (!DBOutput.contentEquals("YES"))
                xi.m_dbconfig.clearStatusInterface();
            else 
                // 1.5 set is_executing to true;
                xi.m_dbconfig.setExecuting();

            if (df != null)
                xi.m_dbconfig.addStatusInterface(df);

            String extraconfig = System.getProperty("XI_EXTRA_CONFIG", "");

            if (!extraconfig.isEmpty()) {
                try {
                    xi.m_dbconfig.evaluateConfigLine("custom:" + extraconfig);
                } catch (ParseException ex) {
                    Logger.getLogger(XiDB.class.getName()).log(Level.SEVERE, "Extra configuration contained error:", ex);
                    df.setVisible(false);
                    df.dispose();                
                    return;
                }
            }


            // 2. Read base-directory 
            String base_dir = xi.m_dbconfig.getBaseDirectory();

            // 3. setup the search process
            xi.m_xi_search.initSearch(xi.m_search_id, base_dir, xi.m_search_name ,xi.m_dbconfig);

            // 4.Prepare for reading multiple MSM files
            xi.m_xi_search.setupMSMIterator();

            // 5. Read the sequence files from the DB
            xi.m_xi_search.getSequenceFiles();

            // 6. Set the results output correctly i.e. can be multiple result writers
            xi.m_xi_search.setupResultWriter();



            if (df != null) {
                final Timer t = new Timer("update xi process", true);
                final XiDB xif =xi;
                final DebugFrame f = df;
                t.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (xif.m_xi_search.getXiProcess() != null) {
                            f.setXiProcess(xif.m_xi_search.getXiProcess());
                            t.cancel();
                        }
                    }
                }, 1000, 1000);
            }
            // 7. Search!
            xi.m_xi_search.search();
        }   catch (Exception ex ) {
            Logger.getLogger(XiDB.class.getName()).log(Level.SEVERE,"Error occured",ex);
            if (xi!= null && xi.m_dbconfig!=null) {
                xi.m_dbconfig.getStatusInterface().setStatus("{%o%} Error occured:" + ex);
            }
        } finally {
            
        
            if (df != null) {
                df.setVisible(false);
                df.dispose();
            }
        }

    }// end mehtod main()



}
