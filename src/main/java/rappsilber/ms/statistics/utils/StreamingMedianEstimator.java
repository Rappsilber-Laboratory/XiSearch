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
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

/**
 * estimates a Median by splitting the assumed observed range into windows and count the occurrences within these windows.
 * Basically making a histogram and then calculating the histogramMedian for this histogram.
 * Which is hopefully a close enough approximation of the real median.
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class StreamingMedianEstimator {
    protected HashMap<Double, UpdateableInteger> m_values = new HashMap<Double, UpdateableInteger>();
    protected double m_resolution = 0.001;
    protected int    m_maxWindows = 1000;
    protected final static int semcount = 10000;
    
    private final Semaphore m_resolutionChange = new Semaphore(semcount, true);

    public StreamingMedianEstimator(double resolution) {
        this.m_resolution = resolution;
    }

    public StreamingMedianEstimator() {
    }
    
    public StreamingMedianEstimator(double resolution, int maxBins) {
        this(resolution);
        m_maxWindows = maxBins;
    }


    protected synchronized void reduceResolution() {
        // make sure nobody else is currently doing anything to the values
        // semcount - 1 as we already acquired one permit
        
        
        // now we should be the only ones doing anything to the values
        int ws = m_values.size();
        // we should only get in here if we exceded the number of possible windows
        // but as we are in a multithreaded enviroment we could have ended up in here 
        // several times in parrallel
        // so a previous call might have already taken care of it.
        if (ws >= m_maxWindows) { 
            m_resolutionChange.acquireUninterruptibly(semcount);
            try {
                // we would exceed the maximum number of keys
                // so we are the first to tread the current limit
                double newResolution = m_resolution * 2;
                HashMap<Double, UpdateableInteger> old_values = m_values;
                HashMap<Double, UpdateableInteger> new_values = new HashMap<Double, UpdateableInteger>(ws/2);

                // go through all bins of old values and transfer the data to the new bins
                for (Double oldbin : old_values.keySet()) {
                    Double newbin = Math.round(oldbin/newResolution)*newResolution;
                    UpdateableInteger newBinCount = new_values.get(newbin);
                    UpdateableInteger oldBinCount = old_values.get(oldbin);
                    if (newBinCount != null) {
                        newBinCount.value += oldBinCount.value;
                    } else {
                        newBinCount = new UpdateableInteger(oldBinCount);
                        new_values.put(newbin, newBinCount);
                    }
                }
            } finally {
                m_resolutionChange.release(semcount);
            }
        }
    }

    public void addValue(double d) {
        Double key = Math.round(d/m_resolution)*m_resolution;
        UpdateableInteger i = m_values.get(key);
        
        // if nobody is currently changing the resolution we should just
        // be able to acquire without wait
        m_resolutionChange.acquireUninterruptibly();
        try {
            if (i == null) {

                int ws = m_values.size();

                if (ws >= m_maxWindows) { 
                    // we would exceed the maximum number of keys
                    // so we have to reduce the resolution
                    
                    // we release the hold here, so we can get all holds within 
                    // the reduceResolution function
                    m_resolutionChange.release();
                    reduceResolution();
                    // and now we need the hold again to be able to work
                    // thread safely
                    m_resolutionChange.acquireUninterruptibly();
                } else {
                    synchronized(m_values) {
                        // somebody could have added this already
                        i = m_values.get(key);

                        if (i == null) {
                            // nobody has done so yet
                            i= new UpdateableInteger(1);
                            m_values.put(key, i);
                        } else {
                            // oh somebody was faster - so we just use that one
                            synchronized(i) {
                                i.value++;
                            }
                        }
                    }
                }
            } else {
                synchronized(i) {
                    i.value++;
                }
            }
        } finally {
            m_resolutionChange.release();
        }
    }

    public double getMedianEstimation() {
        // count how many we have
        HashMap<Double, UpdateableInteger> v = m_values;
        return histogramMedian(v);
    }

    public static double histogramMedian(HashMap<Double, UpdateableInteger> v) {
        TreeMap<Integer,Double> countTobin = new TreeMap<Integer, Double>();
        
        // now we need the sorted array of bins
        Set<Double> binSet = v.keySet();
        Double[] bins = binSet.toArray(new Double[binSet.size()]);
        java.util.Arrays.sort(bins);
        
        int all = 0;
        
        for (Double b : bins) {
            
            UpdateableInteger c = v.get(b);
            countTobin.put(all, b);
            all += c.value;
            
        }
        
        Double center = all/2.0;
        int ci = center.intValue();
        
        if (Double.valueOf(ci).equals(center)) {
            
            Map.Entry<Integer,Double> bin = countTobin.floorEntry(ci);
            
            return bin.getValue();
        }
        
        Map.Entry<Integer,Double> bin0 = countTobin.floorEntry(ci);
        Map.Entry<Integer,Double> bin1 = countTobin.floorEntry(ci+1);
        return (bin0.getValue() + bin1.getValue()) / 2.0;
    }

    public double getMADEstimation() {
        HashMap<Double, UpdateableInteger> v = m_values;
        double median = histogramMedian(v);
        HashMap<Double, UpdateableInteger> absolutdeviation = new HashMap<Double, UpdateableInteger>(v.size()/2);
        for (Map.Entry<Double,UpdateableInteger> bin : v.entrySet()) {
            Double deviation = Math.abs(median - bin.getKey());
            UpdateableInteger dCount = absolutdeviation.get(deviation);
            if (dCount == null) {
                dCount = new UpdateableInteger(bin.getValue());
                absolutdeviation.put(deviation, dCount);
            } else {
                dCount.value+=bin.getValue().value;
            }
        }
        return histogramMedian(absolutdeviation);
    }
    
}
