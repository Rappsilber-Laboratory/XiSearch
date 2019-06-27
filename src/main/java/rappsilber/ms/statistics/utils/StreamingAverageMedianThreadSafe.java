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
package rappsilber.ms.statistics.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * calculates average and standard deviation over streaming data (e.g. seen only ones and not stored) </br>
 Found an algorithm online, that was for awk:
 awk '{delta = $1 - avg; avg += delta / NR; mean2 += delta * ($1 - avg); } END { print sqrt(mean2 / NR); }'
 and transfered this into a class.

 source:
 http://www.commandlinefu.com/commands/view/3442/display-the-standard-deviation-of-a-column-of-numbers-with-awk

 And also calculates an estimate of the histogramMedian based on a histogram of the data
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class StreamingAverageMedianThreadSafe extends StreamingMedianEstimator {
    
    HashMap<Thread,values> threadedValues = new HashMap<Thread, values>();

    public StreamingAverageMedianThreadSafe() {
    }

    public StreamingAverageMedianThreadSafe(double medianResolution) {
        super(medianResolution);
    }

    public StreamingAverageMedianThreadSafe(double medianResolution, int maxMedianBins) {
        super(medianResolution,maxMedianBins);
    }


    /**
     * changes the average and  mean2 value to accompany the given value.</br>
     * Therfore taking the value into account for the average and standard deviation.
     * @param v
     */
    @Override
    public void addValue(double v) {
        super.addValue(v);
        values threadValues = threadedValues.get(Thread.currentThread());
        if (threadValues == null ) {
            threadValues = new values();
            threadedValues.put(Thread.currentThread(), threadValues);
        }
        threadValues.addValue(v);
    }

    /**
     * returns the average over all seen values
     * @return
     */
    public double average() {
        double count = 0;
        double sum = 0;
        for (values v : threadedValues.values()) {
            
            count+= v.m_count;
            sum  += v.m_count*v.m_avg;
        }
        return sum/count;
    }

    public double getMax() {
        double max = -Double.MAX_VALUE;
        for (values v : threadedValues.values()) {
            double m = v.getMax();
            if (m>max)
                max = m;
        }
        
        return max;
    }

    public double getMin() {
        double min = Double.MAX_VALUE;
        for (values v : threadedValues.values()) {
            double m = v.getMin();
            if (m<min)
                min = m;
        }

        return min;
    }

    /**
     * returns the standard deviation over all seen values
     * @return
     */
    public double stdDev() {
        double u=0;
        double l=0;
        double mean = average();
        for (values v : threadedValues.values()) {
            double s = v.stdDev();
            double c = v.m_count;
            double m = v.average();
            u+=c*s*s+c*(m-mean)*(m-mean);
            l+=c;
        }
        return Math.sqrt(u/l);
    }

    /**
     * a private class to calculate the average and stdev for each calling thread
     * separately
     */
    private class values {

        double m_avg = 0;
        int    m_count = 0;
        double m_max = Double.MIN_VALUE;
        double m_mean2 = 0;
        double m_min = Double.MAX_VALUE;

        /**
         * changes the average and  mean2 value to accompany the given value.</br>
         * Therfore taking the value into account for the average and standard deviation.
         * @param v
         */
        public void addValue(double v) {
            m_count++;
            double delta = v - m_avg;
            m_avg += delta / m_count;
            m_mean2 += delta * (v - m_avg);
            if (m_min > v)
                m_min = v;
            else if (m_max < v)
                m_max = v;
            
        }
        
        /**
         * returns the average over all seen values
         * @return
         */
        public double average() {
            return m_avg;
        }
        
        public double getMax() {
            return m_max;
        }
        
        public double getMin() {
            return m_min;
        }
        
        /**
         * returns the standard deviation over all seen values
         * @return
         */
        public double stdDev() {
            return Math.sqrt(m_mean2 / m_count);
        }
    }

}
