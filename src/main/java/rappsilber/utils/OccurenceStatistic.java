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
package rappsilber.utils;

import java.util.HashMap;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class OccurenceStatistic <T> {
    private static final long serialVersionUID = -793161475888181285L;

    /** class, that stores the result for a given object, and how often it was seen */
    protected class Result{
        /** the result of the arithmetic operations */
        public double result = 1;
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        /** how often this object was seen */
        public int occured = 0;
    }

    /** connects the object with the result of the operations */
    private HashMap<T, Result> m_Results = new HashMap<T, Result>();

    /**
     * returns whether a given object was already seen. (via add or multiply)
     * @param o the object in question
     * @return true: was already seen; false otherwise
     */
    public boolean seen(T o) {
        return m_Results.containsKey(o);
    }


    /**
     * Register a object with associated score
     *
     * @param o the object in question
     * @param score the value that should be added to the previous value
     * @return the new value (score) for the object
     */
    public double register(T o, double score) {
        Result r;
        if (m_Results.containsKey(o)) {
            r = m_Results.get(o);
            r.result += score;
            if (r.min > score) {
                r.min = score;
            } else if (r.max < score) {
                r.max = score;
            }
            r.occured++;
        } else {
            r = new Result();
            m_Results.put(o, r);
            r.result = score;
            r.min = score;
            r.max = score;
            r.occured=1;
        }
        return r.result;
    }

    /**
     * returns the sum of all scores for that object
     * @param o the object in question
     * @param defaultScore if no score was calculated return this score.
     * @return
     */
    public double sum(T o) {
        if (m_Results.containsKey(o)) {
            return m_Results.get(o).result;
        } else {
            return Double.NaN;
        }
    }

    /**
     * returns the minimum value
     * @param o the object in question
     * @param defaultScore if no score was calculated return this score.
     * @return
     */
    public double min(T o) {
        if (m_Results.containsKey(o)) {
            return m_Results.get(o).min;
        } else {
            return Double.NaN;
        }
    }
    
    /**
     * returns the maximum value
     * @param o the object in question
     * @param defaultScore if no score was calculated return this score.
     * @return
     */
    public double max(T o) {
        if (m_Results.containsKey(o)) {
            return m_Results.get(o).max;
        } else {
            return Double.NaN;
        }
    }
    
    /**
     * returns the average
     * @param o the object in question
     * @param defaultScore if no score was calculated return this score.
     * @return
     */
    public double average(T o) {
        if (m_Results.containsKey(o)) {
            Result r = m_Results.get(o);
            return r.result/r.occured;
        }else {
            return Double.NaN;
        }
    }
    
    
    
    /**
     * how often was an object seen
     * @param o the object in question
     * @return
     */
    public double count(T o) {
        if (m_Results.containsKey(o)) {
            return m_Results.get(o).occured;
        } else {
            return 0;
        }
    }


    /**
     * return the number of seen unique objects
     * @return 
     */
    public int size() {
        return m_Results.size();
    }
    
}
