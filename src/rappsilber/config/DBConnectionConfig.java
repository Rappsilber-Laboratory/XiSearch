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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * provides a way to read potential database connections from a config file.
 * By default the file that is read is XiDBConfig.conf. It will  first searched where ever the jar-file is.
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class DBConnectionConfig {
    
    public class DBServer {
        public String server;
        public String port;
        public String user;
        public String password;
        public String database;
        public String connectionString;
        public String name = "";

        public DBServer(String connection, String user, String password) {

            this.user = user;
            this.password = password;
            this.connectionString = connection;
        }
        
        public DBServer(String server, String port, String user, String password, String database) {
            this.server = server;
            this.port = port;
            this.user = user;
            this.password = password;
            this.database = database;
            this.connectionString="jdbc:postgresql://"+server+":"+port+"/"+database;
        }
        
        
        

        public DBServer() {
        }
        
        public String toString() {
            if (name != null && !name.isEmpty())
                return name;

            return connectionString;
        }
        
        
    }
    
    private ArrayList<DBServer> servers = new ArrayList<>();
    
    protected File getConfigFile(String name) {
        try {
            File dbconf = null;
            // try to get the config from PD
            URL urlPD = this.getClass().getProtectionDomain().getCodeSource().getLocation();
            if (urlPD != null) {
                try {
                    URI confuri = urlPD.toURI();
                    if (confuri.toString().startsWith("file://") && System.getProperty("os.name").toLowerCase().contains("windows") && confuri.getAuthority() != null) {
                        System.out.println("need to rewrite the uri from:" + confuri);

                        String url = confuri.toString().replace("file://","\\\\").replaceAll("/", "\\\\");
                        
                        System.out.println("to :" + confuri);
                        File filePD = new File(url);
                        dbconf = new File(filePD.getParent()+File.separator+name);
                    } else {
                        File filePD = new File(confuri);
                        dbconf = new File(filePD.getParent()+File.separator+name);
                    }
                } catch (Exception ex) {
                    Logger.getLogger(DBConnectionConfig.class.getName()).log(Level.SEVERE, "error reading the db config from: "+urlPD.toURI(), ex);
                    
                }
            }
            
            if (dbconf == null || !dbconf.exists()) {
                URL urlR = this.getClass().getResource(this.getClass().getSimpleName() + ".class");
                if (urlR != null) {
                    try {
                        File fileR= new File(urlR.toURI());
                        dbconf = new File(fileR.getParent()+File.separator+name);
                    } catch (Exception e) {}
                }
            }
            
            if (dbconf == null || !dbconf.exists()) {
                dbconf = new File(System.getProperty("user.home")+File.separator+".config"+File.separator+name);
            }
            
            if (dbconf.exists())
                return dbconf;
            
        } catch (Exception ex) {
            Logger.getLogger(DBConnectionConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    
    public boolean readConfig() throws FileNotFoundException, ParseException {
        return readConfig("XiDBConfig.conf");
    }
    
    public boolean readConfig(String name) throws FileNotFoundException, ParseException {
        int c = getServers().size();
        File cf = getConfigFile(name);
        if (cf == null)
            return false;
        BufferedReader bf = new BufferedReader(new FileReader(cf));
        String line;
        int ln = 0;
        try {
            DBServer server = null;
            while ((line = bf.readLine())!= null) {
                ln++;
                 line = line.trim();
                 if (line.isEmpty() || line.startsWith("#"))
                     continue;
                 
                 String lineLC = line.toLowerCase();
                 
                 if (lineLC.contentEquals("[database]")) {
                     server = new DBServer();
                     getServers().add(server);
                 } else if (line.contains("=")) {
                     String[] arg = line.split("=",2);
                     arg[0] = arg[0].trim().toLowerCase();
                     arg[1] = arg[1].trim();
                     if (arg[0].contentEquals("server")) {
                         server.server = arg[1];
                     } else if (arg[0].contentEquals("port")) {
                         server.port = arg[1];
                     } else if (arg[0].contentEquals("database")) {
                         server.database = arg[1];
                     } else if (arg[0].contentEquals("connection")) {
                         server.connectionString = arg[1];
                     } else if (arg[0].contentEquals("name")) {
                         server.name = arg[1];
                     } else if (arg[0].contentEquals("user")) {
                         server.user = arg[1];
                     } else if (arg[0].contentEquals("password")) {
                         server.password = arg[1];
                     } else {
                         throw new ParseException("Unexpected entry in the file:" +line, ln);
                     }
                 } else {
                     throw new ParseException("Unexpected entry in the file:" +line, ln);
                 }
                 
            }
        } catch (IOException ex) {
            Logger.getLogger(DBConnectionConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        return getServers().size() != c;
        
    }

    /**
     * @return the servers
     */
    public ArrayList<DBServer> getServers() {
        return servers;
    }

    /**
     * @return the servers
     */
    public DBServer[] getServerArray() {
        return servers.toArray(new DBServer[servers.size()]);
    } 
    /**
     * @param servers the servers to set
     */
    public void setServers(ArrayList<DBServer> servers) {
        this.servers = servers;
    }
    
    
}
