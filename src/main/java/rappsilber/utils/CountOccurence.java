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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import rappsilber.ms.statistics.utils.UpdateableInteger;

/**
 * Class used to count how often an object was found.
 * @param <T> type of object that should be counted
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class CountOccurence<T> implements Iterable<T> {
    private static final long serialVersionUID = -793161475888181285L;
    private int totalCount = 0;
   
    /** the HashMap that links the seen objects to the counter */
    private HashMap<T, UpdateableInteger> m_count = new HashMap<T, UpdateableInteger>();

    /**
     * increments the counter, on how often the given element was seen
     * @param o the object to count
     * @return
     */
    public int add(T o) {
        UpdateableInteger count;
        if (m_count.containsKey(o)) {
            count = m_count.get(o);
            count.value++;
        } else {
            count = new UpdateableInteger(1);
            m_count.put(o, count);
        }
        totalCount++;
        return count.value;
    }
    
    public ArrayList<T> getSortedList(boolean descending) {
        ArrayList<T> ret = new ArrayList<T>(m_count.keySet());
        
        if (descending)
            java.util.Collections.sort(ret, new Comparator<T>() {

                public int compare(T o1, T o2) {
                    return - m_count.get(o1).compareTo(m_count.get(o2));
                }
            });
        else 
            java.util.Collections.sort(ret, new Comparator<T>() {

                public int compare(T o1, T o2) {
                    return m_count.get(o1).compareTo(m_count.get(o2));
                }
            });
        
        return ret;
        
    }

    /**
     * increments the counter, on how often the given element was seen.<br/>
     * just calls add(o)
     * @param o the object to count
     * @return
     * @see add(T o)
     */
    public int increment(T o){
        return add(o);
    }

    /**
     * returns, how often an object was seen.<br/>
     * if it wasen't seen, then it returns 0;
     * @param o the object in question
     * @return how often was it seen (added/incremented)
     */
    public int count(T o) {
        if (m_count.containsKey(o))
            return m_count.get(o).value;
        else {
            return 0;
        }
    }

    /**
     * returns a list of all seen objects
     * @return
     */
    public Collection<T> getCountedObjects() {
        return m_count.keySet();
    }

    public void clear() {
        m_count.clear();

    }
    
    public int total() {
        return totalCount;
    }

    public Iterator<T> iterator() {
        return m_count.keySet().iterator();
    }
}
