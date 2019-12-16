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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public  class LocalProperties {
    private static final long serialVersionUID = -2872695155848651883L;
    
    static String homeDir = System.getProperty("user.home");
    static String userPropertiesFile = homeDir + "/.Xlink.properties";
    static Properties localProperties = new Properties();
    public static final String LAST_MSM_FOLDER = "LastMSMFolder";
    public static final String LAST_SEQUNECE_FOLDER = "LastSequenceFolder";
    static {
        try {
            localProperties.load(new FileInputStream(userPropertiesFile));
        } catch (IOException ex) {
        }
    }

    public static File getLastMSMFolder() {
        return getFolder(LAST_MSM_FOLDER);
    }

    public static Object setLastMSMFolder(String path) {
        return setFolder(LAST_MSM_FOLDER, path);
    }
    
    public static Object setLastMSMFolder(File path) {
        return setFolder(LAST_MSM_FOLDER, path);
    }


    public static File getFolder(String key) {
        if (key != null)
            return new File(localProperties.getProperty(key,homeDir));
        else
            return new File(homeDir);
    }

    public static Object setFolder(String key, File path) {
        return setProperty(key, path.getAbsolutePath());
    }

    public static Object setFolder(String key, String path) {
        return setProperty(key, path);
    }


    public static synchronized Object setProperty(String key, String value)  {
        String old = localProperties.getProperty(key);
        if (old == null || !old.contentEquals(value)) {
            Object ret = localProperties.setProperty(key, value);
            try {
                    localProperties.store(new FileOutputStream(userPropertiesFile), "XLink local properties file");
            } catch (FileNotFoundException ex) {
                Logger.getLogger(LocalProperties.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(LocalProperties.class.getName()).log(Level.SEVERE, null, ex);
            }
            return ret;
        } else
            return localProperties.setProperty(key, value);

    }


    public static String getProperty(String key) {
        return localProperties.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return localProperties.getProperty(key, defaultValue);
    }

}
