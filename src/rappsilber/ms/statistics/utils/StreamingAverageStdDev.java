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

/**
 * calculates average and standard deviation over streaming data (e.g. seen only ones and not stored) </br>
 * Found an algorithm online, that was for awk:
 * awk '{delta = $1 - avg; avg += delta / NR; mean2 += delta * ($1 - avg); } END { print sqrt(mean2 / NR); }'
 * and transfered this into a class.
 *
 * source:
 * http://www.commandlinefu.com/commands/view/3442/display-the-standard-deviation-of-a-column-of-numbers-with-awk
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class StreamingAverageStdDev {
        double m_avg = 0;
        int    m_count = 0;
        double m_mean2 = 0;
        double m_min = Double.MAX_VALUE;
        double m_max = Double.MIN_VALUE;

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

        /**
         * returns the standard deviation over all seen values
         * @return
         */
        public double stdDev() {
            return Math.sqrt(m_mean2 / m_count);
        }

        public double getMin() {
            return m_min;
        }

        public double getMax() {
            return m_max;
        }

}
