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
package rappsilber.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class ScoreInfos {

    public static class ScoreInfoStruct{
        public String group;
        public String name;
        public double average;
        public double stdev;
        public double min;
        public double max;
        public double weigth;

        public ScoreInfoStruct(String group, String name, double average,
                                double stdev, double min, double max, double weigth) {
            this.group = group;
            this.name = name;
            this.max = max;
            this.min = min;
            this.name = name;
            this.stdev = stdev;
            this.weigth = weigth;
        }

    }

    static HashMap<String,ScoreInfoStruct> m_scoreInfos = new HashMap<String, ScoreInfoStruct>();
//    private static String m_scoreInfoSource=".rappsilber.data.Scores2.csv";
    private static String m_scoreInfoSource=".rappsilber.data.ScoresMAD.csv";

    static {
        try {
            read(m_scoreInfoSource);
        } catch (IOException ex) {
            Logger.getLogger(ScoreInfos.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @param filePath      name of file to open. The file can reside
     *                      anywhere in the classpath
     */
    public static void read(String filePath) throws java.io.IOException {

        BufferedReader reader = Util.readFromClassPath(filePath);
        String line;
        int numRead = 0;
        m_scoreInfos.clear();
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.length() >0 && ! line.startsWith("#")) {
                String[] parts = line .split("\\s*,\\s*");
                ScoreInfoStruct si = null;
                if (parts.length == 7)
                    si = new ScoreInfoStruct(parts[0], parts[1], Double.valueOf(parts[2]), Double.valueOf(parts[3]), Double.valueOf(parts[4]), Double.valueOf(parts[5]),Double.valueOf(parts[6]));
                else
                    si = new ScoreInfoStruct(parts[0], parts[1], Double.valueOf(parts[2]), Double.valueOf(parts[3]), Double.valueOf(parts[4]), Double.valueOf(parts[5]), 0d);
                

                m_scoreInfos.put(si.name, si);
            }
        }
        reader.close();
    }


    public static HashMap<String,ScoreInfoStruct> getScoreInfos() {
        return m_scoreInfos;
    }


}
