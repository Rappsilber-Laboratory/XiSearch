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
import java.io.Reader;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class RunConfigFile extends AbstractRunConfig {
    private static final int crosslinker_hash = "crosslinker:".hashCode();



    // *******************
    // SELECTED PARAMETERS
    // *******************

//    // Set the default value in case there is a problem (TRYPSIN)
//    private Digestion m_digestion_method= Parameters.ENZYME;
//
//    private ArrayList<AminoModification> m_fixed_mods = new ArrayList<AminoModification>();
//    private ArrayList<AminoModification> m_var_mods = new ArrayList<AminoModification>();
//    private ArrayList<AminoLabel> m_label = new ArrayList<AminoLabel>();
//
//    private HashMap<AminoAcid,ArrayList<AminoModification>> m_mapped_fixed_mods;
//    private HashMap<AminoAcid,ArrayList<AminoModification>> m_mapped_var_mods;
//    private HashMap<AminoAcid,ArrayList<AminoLabel>> m_mapped_label;
//    private CrossLinker m_crosslinker;
//    private ToleranceUnit m_PrecoursorTolerance;
//    private ToleranceUnit m_FragmentTolerance;
//
//    // Default value 2
//    private int m_missed_cleavages = Parameters.MISSED_CLEAVAGES;
//
//    private int m_top_mgc = Parameters.TOP_MGC;

    public RunConfigFile() {

    }

    public RunConfigFile(File file) throws FileNotFoundException, IOException, ParseException {
        ReadConfig(new FileReader(file));
    }

    public RunConfigFile(Reader file) throws FileNotFoundException, IOException, ParseException {
        ReadConfig(file);
    }

    
    public RunConfigFile(String file) throws FileNotFoundException, IOException, ParseException {
        this(new File(file));
    }



    public void ReadConfig(Reader conf) throws FileNotFoundException, IOException, ParseException {
        BufferedReader br = new BufferedReader(conf);
        String line = "";
        int lineCount = 0;
        try {
            while ((line = br.readLine()) != null) {
                lineCount++;
                line = line.trim();
                if (line.length() > 0 && !line.startsWith("#")) {
                    //System.err.println(line);
                    if (!evaluateConfigLine(line)) {

                        String[] parts = line.split(":",2);
                        storeObject(parts[0], parts[1]);
                        storeObject(parts[0].toUpperCase(), parts[1]);
                        storeObject(parts[0].toLowerCase(), parts[1]);

                    }
                }

            }
        } catch (ParseException ex) {
            Logger.getLogger(RunConfigFile.class.getName()).log(Level.SEVERE, "Error while parsing the config-file: line " + lineCount , ex);
            throw new ParseException("Error while parsing the config-file: line: " + lineCount + ": " + line, lineCount);
        }

    }



}// end class
