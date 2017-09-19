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

import it.unimi.dsi.fastutil.doubles.AbstractDouble2ObjectMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * provides a way to dynamically add or multiply values that are somehow
 * connected to an object
 * @param <T> The type of object that should be scored
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class FUArithmeticScoredOccurence<T> implements Iterable<T> {
    private static final long serialVersionUID = -793161475888181285L;


    
    Object2DoubleOpenHashMap<T> m_Results = new Object2DoubleOpenHashMap<>();

    public FUArithmeticScoredOccurence() {
        m_Results.defaultReturnValue(Double.NaN);
    }

    

    /**
     * multiply the previously calculated value for this object, with the given
     * number. if the object wasn't seen jet store the value itself. (e.g.
     * assume the previous value was 1)
     * @param o the object in question
     * @param score the value that should be multiplied with the previous value
     * @return the new value (score) for the object
     */
    public double multiply(T o, double score) {
        double r = m_Results.getDouble(o);
        if (!Double.isNaN(r)) {
            r *= score;
            m_Results.put(o, r);
        } else {
            r = score;
            m_Results.put(o, r);
        }
        return r;
    }

    /**
     * returns whether a given object was already seen. (via add or multiply)
     * @param o the object in question
     * @return true: was already seen; false otherwise
     */
    public boolean seen(T o) {
        return m_Results.containsKey(o);
    }


    /**
     * Add the given number to the previously calculated value for this object.
     * if the object wasn't seen jet store the value itself. (e.g.
     * assume the previous value was 0)
     *
     * @param o the object in question
     * @param score the value that should be added to the previuos value
     * @return the new value (score) for the object
     */
    public double add(T o, double score) {
        double r = m_Results.getDouble(o);
        if (!Double.isNaN(r)) {
            r = m_Results.getDouble(o);
            r += score;
            m_Results.put(o, r);
        } else {
            m_Results.put(o, score);
            r = score;
        }
        return r;
    }

    public void addAllNew(FUArithmeticScoredOccurence<T> list ) {
        for (Object2DoubleMap.Entry<T> e : list.m_Results.object2DoubleEntrySet())
            if (!this.seen(e.getKey())) {
                m_Results.put(e.getKey(), e.getDoubleValue());
            }
    }

    public void addAllNew(Object2DoubleMap.FastEntrySet<T> elements ) {
        for (Object2DoubleMap.Entry<T> e : elements)
            if (!this.seen(e.getKey())) {
                m_Results.put(e.getKey(), e.getDoubleValue());
            }
    }

    public void addAllNew(Collection<Object2DoubleMap.Entry<T>> elements ) {
        for (Object2DoubleMap.Entry<T> e : elements)
            if (!this.seen(e.getKey())) {
                m_Results.put(e.getKey(), e.getDoubleValue());
            }
    }
    
    public void addNew(Object2DoubleMap.Entry<T> e ) {
        if (!this.seen(e.getKey())) {
            m_Results.put(e.getKey(), e.getDoubleValue());
        }
    }

    /**
     * returns the score that was calculated for the given object
     * @param o the object in question
     * @param defaultScore if no score was calculated return this score.
     * @return
     */
    public double Score(T o, double defaultScore) {
        if (m_Results.containsKey(o))
            return m_Results.getDouble(o);
        else {
            return defaultScore;
        }
    }

    /**
     * returns a list of all seen objects
     * @return
     */
    public Collection<T> getScoredObjects() {
        return m_Results.keySet();
    }



    /**
     * returns an ArrayList of elements the [ranks] highest associated values
     * @param ranks how many unique scores to return
     * @param maxTotal return at most this number of results
     * @return 
     */
    public ArrayList<T> getHighestNEntries(int ranks, int maxTotal) {
        Double2ObjectRBTreeMap<ArrayList<T>> map = new Double2ObjectRBTreeMap<ArrayList<T>>();
        ObjectIterator<Object2DoubleMap.Entry<T>> i =  m_Results.object2DoubleEntrySet().fastIterator();
//        Iterator<Map.Entry<T,Result>> i = m_Results.entrySet().iterator();
        
        // get the first n scores
        while (i.hasNext() && map.size()<ranks) {
            Object2DoubleMap.Entry<T> e = i.next();
            double d = e.getDoubleValue();
            ArrayList<T> v = map.get(d);
            if (v==null) {
                v= new ArrayList<T>();
                map.put(d, v);
            }
            v.add(e.getKey());
        }
        
        // now go through the rest
        while (i.hasNext()) {
            Object2DoubleMap.Entry<T> e = i.next();
            double d = e.getDoubleValue();
            ArrayList<T> v = map.get(d);
            if (v==null) {
                if (d>map.firstDoubleKey()) {
                    v= new ArrayList<T>();
                    map.put(d, v);
                    v.add(e.getKey());
                    map.remove(map.firstDoubleKey());
                }
            } else {
                v.add(e.getKey());
            }
        }
        ArrayList<T> ret = new ArrayList<T>(ranks);
        if (maxTotal <0) {
            for (ArrayList<T> s : map.values()) {
                ret.addAll(s);
            }            
        } else {

            ObjectBidirectionalIterator ri = map.double2ObjectEntrySet().iterator(map.double2ObjectEntrySet().last());

            while (ri.hasPrevious()) {
                AbstractDouble2ObjectMap.BasicEntry<ArrayList<T>> e = (AbstractDouble2ObjectMap.BasicEntry<ArrayList<T>>) ri.previous();
                ArrayList<T> s = e.getValue();
                if (ret.size()+s.size()<=maxTotal)
                    ret.addAll(s);
                else
                    break;
            }
        }
        return ret;
    }

    /**
     * returns an ArrayList of elements the [ranks] highest associated values
     * @param ranks how many unique scores to return
     * @param maxTotal return at most this number of results
     * @return 
     */
    public FUArithmeticScoredOccurence<T> getHighestNMappings(int ranks, int maxTotal) {
        Double2ObjectRBTreeMap<ArrayList<Object2DoubleMap.Entry<T>>> map = new Double2ObjectRBTreeMap<ArrayList<Object2DoubleMap.Entry<T>>>();
        ObjectIterator<Object2DoubleMap.Entry<T>> i =  m_Results.object2DoubleEntrySet().fastIterator();
//        Iterator<Map.Entry<T,Result>> i = m_Results.entrySet().iterator();
        
        // get the first n scores
        while (i.hasNext() && map.size()<ranks) {
            Object2DoubleMap.Entry<T> e = i.next();
            double d = e.getDoubleValue();
            ArrayList<Object2DoubleMap.Entry<T>> v = map.get(d);
            if (v==null) {
                v= new ArrayList<Object2DoubleMap.Entry<T>>();
                map.put(d, v);
            }
            v.add(e);
        }
        
        // now go through the rest
        while (i.hasNext()) {
            Object2DoubleMap.Entry<T> e = i.next();
            double d = e.getDoubleValue();
            ArrayList<Object2DoubleMap.Entry<T>> v = map.get(d);
            if (v==null) {
                if (d>map.firstDoubleKey()) {
                    v= new ArrayList<Object2DoubleMap.Entry<T>>();
                    map.put(d, v);
                    v.add(e);
                    map.remove(map.firstDoubleKey());
                }
            } else {
                v.add(e);
            }
        }
        
        FUArithmeticScoredOccurence<T> ret = new FUArithmeticScoredOccurence<T>();
        if (maxTotal <0) {
            for (ArrayList<Object2DoubleMap.Entry<T>> s : map.values()) {
                ret.addAllNew(s);
            }            
        } else {

            ObjectBidirectionalIterator ri = map.double2ObjectEntrySet().iterator(map.double2ObjectEntrySet().last());

            while (ri.hasPrevious()) {
                AbstractDouble2ObjectMap.BasicEntry<ArrayList<Object2DoubleMap.Entry<T>>> e = (AbstractDouble2ObjectMap.BasicEntry<ArrayList<Object2DoubleMap.Entry<T>>>) ri.previous();
                ArrayList<Object2DoubleMap.Entry<T>> s = e.getValue();
                if (ret.size()+s.size()<=maxTotal)
                    ret.addAllNew(s);
                else
                    break;
            }
        }
        return ret;
    }
    

    /**
     * returns an ArrayList of elements the [ranks] highest associated values
     * @param ranks how many unique scores to return
     * @param maxTotal return at most this number of results
     * @return 
     */
    public ArrayList<T> getLowestNEntries(int ranks, int maxTotal) {
        Double2ObjectRBTreeMap<ArrayList<T>> map = new Double2ObjectRBTreeMap<ArrayList<T>>();
        ObjectIterator<Object2DoubleMap.Entry<T>> i =  m_Results.object2DoubleEntrySet().fastIterator();
//        Iterator<Map.Entry<T,Result>> i = m_Results.entrySet().iterator();
        
        // get the first n scores
        while (i.hasNext() && map.size()<ranks) {
            Object2DoubleMap.Entry<T> e = i.next();
            double d = e.getDoubleValue();
            ArrayList<T> v = map.get(d);
            if (v==null) {
                v= new ArrayList<T>();
                map.put(d, v);
            }
            v.add(e.getKey());
        }
        
        // now go through the rest
        while (i.hasNext()) {
            Object2DoubleMap.Entry<T> e = i.next();
            double d = e.getDoubleValue();
            ArrayList<T> v = map.get(d);
            if (v==null) {
                if (d<map.firstDoubleKey()) {
                    v= new ArrayList<T>();
                    map.put(d, v);
                    v.add(e.getKey());
                    map.remove(map.firstDoubleKey());
                }
            } else {
                v.add(e.getKey());
            }
        }
        ArrayList<T> ret = new ArrayList<T>(ranks);
        if (maxTotal <0) {
            for (ArrayList<T> s : map.values()) {
                ret.addAll(s);
            }            
        } else {

            ObjectBidirectionalIterator ri = map.double2ObjectEntrySet().iterator();

            while (ri.hasNext()) {
                AbstractDouble2ObjectMap.BasicEntry<ArrayList<T>> e = (AbstractDouble2ObjectMap.BasicEntry<ArrayList<T>>) ri.next();
                ArrayList<T> s = e.getValue();
                if (ret.size()+s.size()<=maxTotal)
                    ret.addAll(s);
                else
                    break;
            }
        }
        return ret;
    }

    /**
     * returns an ArrayList of elements the [ranks] highest associated values
     * @param ranks how many unique scores to return
     * @param maxTotal return at most this number of results
     * @return 
     */
    public FUArithmeticScoredOccurence<T> getLowestNMappings(int ranks, int maxTotal) {
        Double2ObjectRBTreeMap<ArrayList<Object2DoubleMap.Entry<T>>> map = new Double2ObjectRBTreeMap<ArrayList<Object2DoubleMap.Entry<T>>>();
        ObjectIterator<Object2DoubleMap.Entry<T>> i =  m_Results.object2DoubleEntrySet().fastIterator();
//        Iterator<Map.Entry<T,Result>> i = m_Results.entrySet().iterator();
        
        // get the first n scores
        while (i.hasNext() && map.size()<ranks) {
            Object2DoubleMap.Entry<T> e = i.next();
            double d = e.getDoubleValue();
            ArrayList<Object2DoubleMap.Entry<T>> v = map.get(d);
            if (v==null) {
                v= new ArrayList<Object2DoubleMap.Entry<T>>();
                map.put(d, v);
            }
            v.add(e);
        }
        
        // now go through the rest
        while (i.hasNext()) {
            Object2DoubleMap.Entry<T> e = i.next();
            double d = e.getDoubleValue();
            ArrayList<Object2DoubleMap.Entry<T>> v = map.get(d);
            if (v==null) {
                if (d<map.firstDoubleKey()) {
                    v= new ArrayList<Object2DoubleMap.Entry<T>>();
                    map.put(d, v);
                    v.add(e);
                    map.remove(map.firstDoubleKey());
                }
            } else {
                v.add(e);
            }
        }
        
        FUArithmeticScoredOccurence<T> ret = new FUArithmeticScoredOccurence<T>();
        if (maxTotal <0) {
            for (ArrayList<Object2DoubleMap.Entry<T>> s : map.values()) {
                ret.addAllNew(s);
            }            
        } else {

            ObjectBidirectionalIterator ri = map.double2ObjectEntrySet().iterator();

            while (ri.hasNext()) {
                AbstractDouble2ObjectMap.BasicEntry<ArrayList<Object2DoubleMap.Entry<T>>> e = (AbstractDouble2ObjectMap.BasicEntry<ArrayList<Object2DoubleMap.Entry<T>>>) ri.next();
                ArrayList<Object2DoubleMap.Entry<T>> s = e.getValue();
                if (ret.size()+s.size()<=maxTotal)
                    ret.addAllNew(s);
                else
                    break;
            }
        }
        return ret;
    }
    
    
    
    public T[] getScoredSortedArray(T[] a) {
        return getSortedEntries().toArray(a);
    }

    public ArrayList<T> getSortedEntries() {
        ArrayList<T> retDummy = new ArrayList<T>(m_Results.size());
        Double2ObjectRBTreeMap<ArrayList<T>> map = new Double2ObjectRBTreeMap<ArrayList<T>>();
        ObjectIterator<Object2DoubleMap.Entry<T>> i =  m_Results.object2DoubleEntrySet().fastIterator();

        // push everything in a tree map
        while (i.hasNext()) {
            Object2DoubleMap.Entry<T> e = i.next();
            double d = e.getDoubleValue();
            ArrayList<T> v = map.get(d);
            if (v==null) {
                v= new ArrayList<T>();
                map.put(d, v);
            }
            v.add(e.getKey());
        }
        for (ArrayList<T> s : map.values()) {
            retDummy.addAll(s);
        }           

        return retDummy;

    }

    public int size() {
        return m_Results.size();
    }
    
    @Override
    public Iterator<T> iterator() {
        return m_Results.keySet().iterator();
    }
    
}
