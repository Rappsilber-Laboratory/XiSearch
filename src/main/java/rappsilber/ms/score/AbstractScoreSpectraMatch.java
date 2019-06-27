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
package rappsilber.ms.score;

import java.util.HashMap;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.ms.statistics.utils.StreamingAverageMedianStdDev;
import rappsilber.ms.statistics.utils.StreamingAverageMedianThreadSafe;

/**
 * a simple abstract class, that forwards the class name as score name
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public abstract class AbstractScoreSpectraMatch implements ScoreSpectraMatch {

    public static boolean DO_STATS = false;

    private HashMap<String,StreamingAverageMedianThreadSafe> m_calcs;

    AbstractScoreSpectraMatch(){
        initCalculations(scoreNames());
    }

    AbstractScoreSpectraMatch(String[] ScoreNames){
        initCalculations(ScoreNames);
    }
    
    protected void initCalculations(String[] ScoreNames) {
        m_calcs = new HashMap<String, StreamingAverageMedianThreadSafe>();
        for (String score : ScoreNames) {
            m_calcs.put(score, new StreamingAverageMedianThreadSafe());
        }
    }

    protected void addScore(MatchedXlinkedPeptide match, String name, double value) {
        match.setScore(name,value);
        if (DO_STATS && !Double.isInfinite(value) && !Double.isNaN(value)) {
            StreamingAverageMedianThreadSafe ssd = m_calcs.get(name);
            if (ssd == null) {
                ssd = new StreamingAverageMedianThreadSafe();
                m_calcs.put(name, ssd);
            }
            ssd.addValue(value);
        }
    }

    @Override
    public double getAverage(String name){
        if (DO_STATS)
            return m_calcs.get(name).average();
        return Double.NaN;
    }

//    public double getMedian(String name){
//        return m_calcs.get(name).getMedianEstimation();
//    }

    @Override
    public double getStdDev(String name) {
        if (DO_STATS)
            return m_calcs.get(name).stdDev();
        return Double.NaN;
    }

    public double getMAD(String name) {
        if (DO_STATS)
            return m_calcs.get(name).getMADEstimation();
        return Double.NaN;
    }
    
    public double getMedian(String name) {
        if (DO_STATS)
            return m_calcs.get(name).getMedianEstimation();
        return Double.NaN;
    }
    
    @Override
    public double getMin(String name){
        if (DO_STATS)
            return m_calcs.get(name).getMin();
        return Double.NaN;
    }

    @Override
    public double getMax(String name) {
        if (DO_STATS)
            return m_calcs.get(name).getMax();
        return Double.NaN;
    }
    
    
    @Override
    public String name() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String[] scoreNames() {
        return new String[]{ this.getClass().getSimpleName()};
    }

    @Override
    public int compareTo(ScoreSpectraMatch o) {
        return Double.compare(getOrder(),o.getOrder());
    }




}
