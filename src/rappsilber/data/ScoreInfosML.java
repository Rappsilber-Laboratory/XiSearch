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
import rappsilber.data.csv.CsvParser;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class ScoreInfosML  {

    public static class ScoreInfoStruct{
        public String group;
        public String name;
        public double average;
        public double stdev;
        public double min;
        public double max;
        public double weigth;
        public double split;
        public double normalizedSplit;
        public int factor;

        public ScoreInfoStruct(String group, String name, double average,
                                double stdev, double min, double max, 
                                double weigth,double split, int factor) {
            this.group = group;
            this.name = name;
            this.max = max;
            this.min = min;
            this.name = name;
            this.stdev = stdev;
            this.average = average;
            this.weigth = weigth;
            this.split = split;
            this.factor = factor;
            this.normalizedSplit = (split - average)/stdev*factor;
        }
        
        public double scaleScore(double score) {
            double normscore = (score - average)/stdev*factor;
            return normscore / normalizedSplit;
        }

    }

    static HashMap<String,ScoreInfoStruct> m_scoreInfos = new HashMap<String, ScoreInfoStruct>();
//    private static String m_scoreInfoSource=".rappsilber.data.Scores2.csv";
    private static String m_scoreInfoSource=".rappsilber.data.ScoresML.csv";

    static {
        try {
            read(m_scoreInfoSource);
        } catch (IOException ex) {
            Logger.getLogger(ScoreInfosML.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @param filePath      name of file to open. The file can reside
     *                      anywhere in the classpath
     */
    public static void read(String filePath) throws java.io.IOException {

        BufferedReader reader = Util.readFromClassPath(filePath);
        int numRead = 0;
        m_scoreInfos.clear();
        CsvParser csv = new CsvParser(',', '"');
        csv.setAlternative("direction", "factor");
        csv.setAlternative("group", "scorer");
        csv.openFile(reader,true);
        while (csv.next()) {
            ScoreInfoStruct si = null;
            //System.out.println(csv.getCurrentLine());
            si = new ScoreInfoStruct(csv.getValue("group"),
                    csv.getValue("score"),csv.getDouble("average"),
                    csv.getDouble("stdev"),
                    csv.getDouble("min"), csv.getDouble("max"),
                    csv.getDouble("weight",0d),csv.getDouble("split",0),
                    csv.getInteger("factor",0)
            );
            m_scoreInfos.put(si.name, si);
        }
        reader.close();
    }


    public static HashMap<String,ScoreInfoStruct> getScoreInfos() {
        return m_scoreInfos;
    }


}
