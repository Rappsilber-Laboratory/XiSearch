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

import java.util.Comparator;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
/**
 * class used for storing an object and the score of that object
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 * @param <Store>  the object type that should be sorted
 * @param <Score>  the type of score to be used for sorting the elements
 */
public class ScoredObject<Store,Score extends Comparable> implements Comparable, Comparator {
    Store  m_Store;
    Score m_Score;

    /**
     * creates a new scored storage-element
     * @param store
     * @param score
     */
    public ScoredObject(Store store,Score score) {
        m_Score = score;
        m_Store = store;
    }

    public int compareTo(Object o) {
        int res = m_Score.compareTo(((ScoredObject<Store,Score>)o).getScore());
        if (res == 0) {
            res = m_Store.toString().compareTo(((ScoredObject<Store,Score>)o).getStore().toString());
        }
        return res;
        //return m_Score.compareTo(((ScoredObject<Store,Score>)o).getScore());
    }

    public int compareTo(Score o) {
        return m_Score.compareTo(o);
    }

    /**
     * the score of the stored element
     * @return
     */
    public Score getScore() {
        return m_Score;
    }

    /**
     * the actual stored element
     * @return
     */
    public Store getStore() {
        return m_Store;
    }



    /**
     * compares whether two stored objects are equal
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (o.getClass() == this.getClass())
            return  (((ScoredObject<Store,Score>) o).m_Store.equals(m_Store));
        else if (o.getClass() == m_Store.getClass())
            return ((Store) o).equals(m_Store);

        return false;
    }

    /**
     * creates a hashcode based on the stored object and the score
     * @return
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + (this.m_Store != null ? this.m_Store.hashCode() : 0);
        hash = 13 * hash + (this.m_Score != null ? this.m_Score.hashCode() : 0);
        return hash;
    }

    public int compare(Object o1, Object o2) {
        return ((Comparable) o1).compareTo(o2);
    }



}
