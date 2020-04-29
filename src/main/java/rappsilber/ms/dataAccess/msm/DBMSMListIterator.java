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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.RunConfig;
import rappsilber.db.ConnectionPool;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.spectra.Spectra;
import rappsilber.utils.MyArrayUtils;

/**
 *
 * @author stahir
 */
public class DBMSMListIterator extends MSMListIterator{

    private ConnectionPool m_connection_pool;
    private Connection m_connection;
    private PreparedStatement m_getAcqID;
    private PreparedStatement m_getPaths;
    private PreparedStatement m_getAcqPath;

    
    private HashMap<String,Integer> m_runids = new HashMap<String,Integer>();

    private int m_defaultRunID = -1;

    private HashMap<String,Integer> m_acqids = new HashMap<String,Integer>();

    private int m_defaultAcqID = -1;
    
//    private int m_acqID;

    public DBMSMListIterator(int search_id, String base_path, ToleranceUnit t , int minCharge, ConnectionPool cp, RunConfig config) throws FileNotFoundException, IOException, ParseException {
        super(t,minCharge,config);
        try {
//            m_acqID = -1;
            m_inputPath = base_path;
            m_connection_pool = cp;
            m_connection = m_connection_pool.getConnection();
            m_getAcqID = m_connection.prepareStatement("SELECT acq_id, run_id FROM search_acquisition WHERE search_id = ?",ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            m_getPaths = m_connection.prepareStatement("SELECT file_path FROM run WHERE acq_id = ? AND run_id = ?");
            m_getAcqPath = m_connection.prepareStatement("SELECT file_path, r.run_id, r.acq_id FROM search_acquisition sa INNER JOIN  run r ON sa.acq_id = r.acq_id AND sa.run_id = r.run_id WHERE search_id = ?",ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            PreparedStatement getAcqCount = m_connection.prepareStatement("SELECT count(*) FROM search_acquisition sa INNER JOIN  run r ON sa.acq_id = r.acq_id AND sa.run_id = r.run_id WHERE search_id = ?",ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);


            getAcqCount.setInt(1, search_id);
            ResultSet rsCount = getAcqCount.executeQuery();

            while (rsCount.next()) {
                System.err.println("Should get " + rsCount.getInt(1) + " files");
            }
            rsCount.close();

            ResultSet rs;
            m_getAcqPath.setInt(1, search_id);
            rs = m_getAcqPath.executeQuery();

            int inputfilescount = 0;

            while(rs.next()){

//                m_acqID = rs.getInt(3);
                String absolutePath = addFile(rs.getString(1), base_path, t);
                // some windows oddety
                if (absolutePath.matches("[a-zA-Z]:[\\/][\\/].*")) {
                    absolutePath = absolutePath.substring(0,3) + absolutePath.substring(4);
                }
                String fileName = absolutePath.substring(absolutePath.lastIndexOf(File.pathSeparator) + 1);

                String absolutePath_s1 = absolutePath.replace("/", "\\");
                String fileName1 = absolutePath.substring(absolutePath.lastIndexOf("/") + 1);
                String absolutePath_s2 = absolutePath.replace("\\", "/");
                String fileName2 = absolutePath.substring(absolutePath.lastIndexOf("\\") + 1);
                
                m_runids.put(absolutePath, rs.getInt(2));
                m_runids.put(absolutePath_s1, rs.getInt(2));
                m_runids.put(absolutePath_s2, rs.getInt(2));

                m_acqids.put(absolutePath, rs.getInt(3));
                m_acqids.put(absolutePath_s1, rs.getInt(3));
                m_acqids.put(absolutePath_s2, rs.getInt(3));
                
                m_runids.put(fileName, rs.getInt(2));
                m_runids.put(fileName1, rs.getInt(2));
                m_runids.put(fileName2, rs.getInt(2));
                m_acqids.put(fileName, rs.getInt(3));
                m_acqids.put(fileName1, rs.getInt(3));
                m_acqids.put(fileName2, rs.getInt(3));
                m_defaultRunID = rs.getInt(2);
                

                System.err.println("added msm file :" + absolutePath);
                inputfilescount++;

            }
            rs.close();

            System.err.println("got " + inputfilescount);
            
            setNext();

        } catch (SQLException ex) {
             System.err.println("XiDB: problem when setting up MSMIterator: " + ex.getMessage());
             Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,"XiDB: problem when setting up MSMIterator",ex);
             this.m_connection_pool.closeAllConnections();
             System.exit(1);
        }

    

    }

    @Override
    protected void publishNextSpectra(Spectra s){
        String file = s.getSource();
        Integer acqid = m_acqids.get(file);
        Integer runid = m_runids.get(file);
        ArrayList<String> searched = new ArrayList<>();
        searched.add(file);
        
        if (runid == null && file.contains("->")) {
            for (String f : file.split("\\s*->\\s*")) {
                
                searched.add(f);
                
                acqid = m_acqids.get(f);
                runid = m_runids.get(f);
                
                if (acqid != null && runid != null) {
                    file=f;
                    break;
                } else {
                    if (runid == null && f.contains(File.separator)) {
                        f=f.substring(f.lastIndexOf(File.separator)+1).trim();
                        searched.add(f);
                        acqid = m_acqids.get(f);
                        runid = m_runids.get(f);
                    }
                    if (acqid != null && runid != null) {
                        file=f;
                        break;
                    } else if (f.contains("/")) {
                        f=f.substring(f.lastIndexOf("/")+1).trim();
                        searched.add(f);
                        acqid = m_acqids.get(f);
                        runid = m_runids.get(f);
                    }
                    if (acqid != null && runid != null) {
                        file=f;
                        break;
                    } else if (f.contains("\\")) {
                        f=f.substring(f.lastIndexOf("\\") + 1).trim();
                        searched.add(f);
                        acqid = m_acqids.get(f);
                        runid = m_runids.get(f);
                    }
                    
                }
            }
        }

        if (runid == null && file.contains(File.separator)) {
            file=file.substring(file.lastIndexOf(File.separator)+1).trim();
            searched.add(file);
            acqid = m_acqids.get(file);
            runid = m_runids.get(file);
        }
        
        if (runid == null)
            s.setRunID(m_defaultRunID);
        else
            s.setRunID(runid);

        if (acqid == null) {
            s.setAcqID(m_defaultAcqID);
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,
                    "Could not find the database acquisition.\n"
                            + "Current source is: " + s.getSource() +"\n"
                            + "searched for as: \n\"" + MyArrayUtils.toString(searched, "\" , \"") + "\"\n"
                            + "\n\nAmong: \"" + MyArrayUtils.toString(m_acqids.keySet(), "\" , \"") + "\"\n" 
                            + "\n\nand: \"" + MyArrayUtils.toString(m_runids.keySet(), "\" , \"") + "\"");
            System.exit(-1);
        } else
            s.setAcqID(acqid);

    }

//    /**
//     * @return the m_acqID
//     */
//    public int getAcqID() {
//        return m_acqID;
//    }
//
//    /**
//     * @param m_acqID the m_acqID to set
//     */
//    public void setAcqID(int m_acqID) {
//        this.m_acqID = m_acqID;
//    }

}
