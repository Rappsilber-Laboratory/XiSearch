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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.DBRunConfig;
import rappsilber.db.ConnectionPool;
import rappsilber.gui.components.DebugFrame;
import rappsilber.ms.lookup.peptides.PeptideLookup;
import rappsilber.ms.sequence.Peptide;

/**
 *
 * @author stahir
 * Class to load the configuration
 *  - read sequence files and,
 *  - msmfiles selected and start the search
 *
 */
public class WriteUniquePeptides {
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
    private UniquePeptides m_xi_search;

    public WriteUniquePeptides(String s_id, String s_name){
        this(s_id,s_name, m_db_connection);

    }

    public WriteUniquePeptides(String s_id, String s_name, String db_connection) {
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
            m_xi_search = new UniquePeptides(m_connection_pool);


        } catch (SQLException e) {
                System.err.println("XiDB: Error with connection pool: " + e.getMessage());
                m_connection_pool.closeAllConnections();
                System.exit(1);
        }
    }




    


    public static void main(String args[]) throws FileNotFoundException{
        // We launch XiDB with the search ID as a command-line argument
        String xlOutFile = args[2];
        String lOutFile = args[3];
        
        PrintWriter pwXL = new PrintWriter(new File(xlOutFile));
        PrintWriter pwL = new PrintWriter(new File(lOutFile));
        
        final DebugFrame  df = new DebugFrame(args[0] + " - "+ args[1], null);
        String debugframe = System.getProperty("XI_SHOW_DEBUG", "1").toLowerCase();
        if (debugframe.equals("1") || debugframe.equals("t") || debugframe.equals("true") || debugframe.equals("yes") || debugframe.equals("y")) {
            System.err.println("Showing debug window!");
            new Thread(new Runnable() {

                public void run() {
                     df.setVisible(true);
                }
            }).start();
        } else {
            System.err.println("No debug window is shown!");
        }
        
        
        // reset this later
        WriteUniquePeptides xi = new WriteUniquePeptides(args[0], args[1]);
//       XiDB xi = new XiDB("3", "test search 2");
       
        // Only print severe messages to System.err
        Logger.getLogger("rappsilber").setLevel(Level.SEVERE);

        // 1. Read the configuration/parameters for search
        xi.m_dbconfig.readConfig(xi.m_search_id);
        
        xi.m_dbconfig.clearStatusInterface();
        
        xi.m_dbconfig.addStatusInterface(df);

        // 1.5 set is_executing to true;
        //xi.m_dbconfig.setExecuting();

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
        
        // 7. Search!
        rappsilber.applications.SimpleXiProcess proc = xi.m_xi_search.buildTress();
        
        PeptideLookup lp = proc.getLinearPeptideLookup();
        PeptideLookup xlp = proc.getXLPeptideLookup();
        
        // we have some trouble with rounding iin floating point values (presumably) so we only do a quasy same mass
        final double quasyExact = 0.0000000001;
        
        pwL.println("Peptide,isDecoy,mass,foundSameMass,foundRelativeMass,foundRelativeDecoy");
        for (Peptide p : lp) {
            double mass = p.getMass();
            if (!Double.isInfinite(mass) && !p.isDecoy()) {
                int exact = lp.getForExactMassRange(mass-quasyExact, mass+quasyExact).size();
                ArrayList<Peptide> rspep  = lp.getForMass(mass);
                int relative  = rspep.size();
                int relativeDecoy = 0;
                for (Peptide rp : rspep) {
                    if (rp.isDecoy()) {
                        relativeDecoy ++;
                    }
                }
                
                pwL.printf("\"%s\",%d,%f,%d,%d,%d\n", 
                        p,
                        p.isDecoy()?1:0,
                        mass,
                        exact,
                        relative,
                        relativeDecoy);
            } 
        }
        pwL.flush();
        pwL.close();

        double maxMass = 0;
        for (Peptide p : xlp) {
            Double pepMass = p.getMass();
            if (pepMass > maxMass && !Double.isInfinite(pepMass)) {
                maxMass = pepMass;
            }
        }
        //double maxMass = xlp.getMaximumMass();
        maxMass=3700;
        
        int targetMassFoundInDecoy = 0;
        int targetMassFoundInDecoyRelative = 0;
        int allPeps = xlp.size();
        
        pwXL.println("Peptide,isDecoy,mass,foundSameMass, foundSameMassTarget, foundSameDecoy,foundRelativeMass, foundRelativeTarget,foundRelativeDecoy");
        for (Peptide p : xlp) {
            double mass = p.getMass();
            if (!Double.isInfinite(mass) && !p.isDecoy()) {
                ArrayList<Peptide> exactPeps = xlp.getForExactMassRange(mass-quasyExact, mass+quasyExact);
                int exact = exactPeps.size();
                int exactTarget = 0;
                int exactDecoy = 0;

                for (Peptide rp : exactPeps) {
                    if (rp.isDecoy()) { 
                        exactDecoy ++;
                    } else {
                        exactTarget++;
                    }
                }

                ArrayList<Peptide> rspep  = xlp.getForMass(mass,mass+maxMass);
                int relative  = rspep.size();
                int relativeDecoy = 0;
                int relativeTarget = 0;

                for (Peptide rp : rspep) {
                    if (rp.isDecoy()) {
                        relativeDecoy ++;
                    } else {
                        relativeTarget++;
                    }

                }
                
                if (exactDecoy > 0) {
                    targetMassFoundInDecoy ++;
                }
                
                if (relativeDecoy >0) {
                    targetMassFoundInDecoyRelative ++;
                }

                pwXL.printf("\"%s\",%d,%f,%d,%d,%d,%d,%d,%d\n", 
                        p,
                        p.isDecoy()?1:0,
                        mass,
                        exact,
                        exactTarget,
                        exactDecoy,
                        relative,
                        relativeTarget,
                        relativeDecoy);
            }             
        }
        pwXL.flush();
        pwXL.close();
        
        
        df.setVisible(false);
        df.dispose();


    }// end mehtod main()



}
