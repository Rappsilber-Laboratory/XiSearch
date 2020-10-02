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
import java.util.TreeMap;

/**
 * provides a way to dynamically add or multiply values that are somehow
 * connected to an object
 * @param <T> The type of object that should be scored
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class ArithmeticScoredOccurence<T> implements ScoredOccurence<T> {
    private static final long serialVersionUID = -793161475888181285L;

    /** class, that stores the result for a given object, and how often it was seen */
    protected class Result{
        /** the result of the arithmetic operations */
        public double result = 1;
        /** how often this object was seen */
        public int occured = 0;
    }


    private  class comp implements Comparable<comp>{
        double score;
        T compElement;
        Comparator<T> firstCompare;

        comp(double score, T compElement, Comparator<T> firstCompare) {
            this.score = score;
            this.compElement = compElement;
            this.firstCompare = firstCompare;
        }

        @Override
        public int compareTo(comp o) {
            int ret = firstCompare.compare(compElement, o.compElement);
            if (ret == 0) {
                return Double.compare(score, o.score);
            } else
                return ret;
        }

        @Override
        public boolean equals(Object obj) {
            return score == ((comp) obj).score && firstCompare.compare(compElement, ((comp) obj).compElement) == 0;
        }
    }

    private Double min = null;
    private Double max = null;

    /** connects the object with the result of the operations */
    private HashMap<T, Result> m_Results = new HashMap<T, Result>();

    /**
     * multiply the previously calculated value for this object, with the given
     * number. if the object wasn't seen jet store the value itself. (e.g.
     * assume the previous value was 1)
     * @param o the object in question
     * @param score the value that should be multiplied with the previous value
     * @return the new value (score) for the object
     */
    public double multiply(T o, double score) {
        Result r = m_Results.get(o);
        if (r != null) {
            r.result *= score;
        } else {
            r = new Result();
            m_Results.put(o, r);
            r.result = score;
        }
        r.occured++;
        return r.result;
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
        Result r;
        if (m_Results.containsKey(o)) {
            r = m_Results.get(o);
            r.result += score;
        } else {
            r = new Result();
            m_Results.put(o, r);
            r.result = score;
        }
        r.occured++;
        return r.result;
    }

    public void addAllNew(ArithmeticScoredOccurence<T> list ) {
        for (Map.Entry<T,Result> e : list.m_Results.entrySet())
            if (!this.seen(e.getKey())) {
                m_Results.put(e.getKey(), e.getValue());
            }
    }
    
    
    public void addAllNew(ScoredOccurence<T> list ) {
        if (!(list instanceof ArithmeticScoredOccurence)) {
            throw new UnsupportedOperationException("Currently cant mix these classes for addAllNew");
        }
        
        for (Map.Entry<T,Result> e : ((ArithmeticScoredOccurence<T>)list).m_Results.entrySet())
            if (!this.seen(e.getKey())) {
                m_Results.put(e.getKey(), e.getValue());
            }
    }

    public void addAllHighest(ScoredOccurence<T> list ) {
        if (!(list instanceof ArithmeticScoredOccurence)) {
            throw new UnsupportedOperationException("Currently cant mix these classes for addAllNew");
        }
        
        for (Map.Entry<T,Result> e : ((ArithmeticScoredOccurence<T>)list).m_Results.entrySet()) {
            Result r = this.m_Results.get(e.getKey());
            if (r == null) {
                m_Results.put(e.getKey(), e.getValue());
            } else {
                if (e.getValue().result > r.result)
                    r.result = e.getValue().result;
            }
        }
    }

    public void addAllLowest(ScoredOccurence<T> list ) {
        if (!(list instanceof ArithmeticScoredOccurence)) {
            throw new UnsupportedOperationException("Currently cant mix these classes for addAllNew");
        }
        
        for (Map.Entry<T,Result> e : ((ArithmeticScoredOccurence<T>)list).m_Results.entrySet()) {
            Result r = this.m_Results.get(e.getKey());
            if (r == null) {
                m_Results.put(e.getKey(), e.getValue());
            } else {
                if (e.getValue().result < r.result)
                    r.result = e.getValue().result;
            }
        }
    }
    
    public void addAllNew(Collection<Map.Entry<T,Result>> elements ) {
        for (Map.Entry<T,Result> e : elements)
            if (!this.seen(e.getKey())) {
                m_Results.put(e.getKey(), e.getValue());
            }
    }

    public void addNew(Map.Entry<T,Result> e ) {
        if (!this.seen(e.getKey())) {
            m_Results.put(e.getKey(), e.getValue());
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
            return m_Results.get(o).result;
        else {
            return defaultScore;
        }
    }

    /**
     * how often was an object seen
     * @param o the object in question
     * @return
     */
    public double Count(T o) {
        if (m_Results.containsKey(o))
            return m_Results.get(o).occured;
        else {
            return 0;
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
        TreeMap<Double,ArrayList<T>> map = new TreeMap<Double,ArrayList<T>>();
        Iterator<Map.Entry<T,Result>> i = m_Results.entrySet().iterator();
        
        // get the first n scores
        while (i.hasNext() && map.size()<ranks) {
            Map.Entry<T,Result> e = i.next();
            double d = e.getValue().result;
            ArrayList<T> v = map.get(d);
            if (v==null) {
                v= new ArrayList<T>();
                map.put(d, v);
            }
            v.add(e.getKey());
        }
        
        // now go through the rest
        while (i.hasNext()) {
            Map.Entry<T,Result> e = i.next();
            double d = e.getValue().result;
            ArrayList<T> v = map.get(d);
            if (v==null) {
                if (d>map.firstKey()) {
                    v= new ArrayList<T>();
                    map.put(d, v);
                    v.add(e.getKey());
                    map.remove(map.firstKey());
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
            for (Double r : map.descendingKeySet()) {
                ArrayList<T> s = map.get(r);
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
    public ArithmeticScoredOccurence<T> getHighestNMappings(int ranks, int maxTotal) {
        TreeMap<Double,ArrayList<Map.Entry<T,Result>>> map = new TreeMap<Double,ArrayList<Map.Entry<T,Result>>>();
        Iterator<Map.Entry<T,Result>> i = m_Results.entrySet().iterator();
        
        // get the first n scores
        while (i.hasNext() && map.size()<ranks) {
            Map.Entry<T,Result> e = i.next();
            double d = e.getValue().result;
            ArrayList<Map.Entry<T,Result>> v = map.get(d);
            if (v==null) {
                v= new ArrayList<>();
                map.put(d, v);
            }
            v.add(e);
        }
        
        // now go through the rest
        while (i.hasNext()) {
            Map.Entry<T,Result> e = i.next();
            double d = e.getValue().result;
            ArrayList<Map.Entry<T,Result>> v = map.get(d);
            if (v==null) {
                if (d>map.firstKey()) {
                    v= new ArrayList<>();
                    map.put(d, v);
                    v.add(e);
                    map.remove(map.firstKey());
                }
            } else {
                v.add(e);
            }
        }
        ArithmeticScoredOccurence<T> ret = new ArithmeticScoredOccurence<T>();
        if (maxTotal <0) {
            for (ArrayList<Map.Entry<T,Result>> s : map.values()) {
                ret.addAllNew(s);
            }            
        } else {
            for (Double r : map.descendingKeySet()) {
                ArrayList<Map.Entry<T,Result>> s = map.get(r);
                if (ret.size()+s.size()<=maxTotal)
                    ret.addAllNew(s);
                else
                    break;
            }
        }
        return ret;
    }
    
    /**
     * returns an ArrayList of elements the [ranks] lowest associated values
     * @param ranks how many unique scores to return
     * @param maxTotal return at most this number of results
     * @return 
     */
    public ArrayList<T> getLowestNEntries(int ranks, int maxTotal) {
        TreeMap<Double,ArrayList<T>> map = new TreeMap<Double,ArrayList<T>>();
        Iterator<Map.Entry<T,Result>> i = m_Results.entrySet().iterator();
        
        // get the first n scores
        while (i.hasNext() && map.size()<ranks) {
            Map.Entry<T,Result> e = i.next();
            double d = e.getValue().result;
            ArrayList<T> v = map.get(d);
            if (v==null) {
                v= new ArrayList<T>();
                map.put(d, v);
            }
            v.add(e.getKey());
        }
        
        // now go through the rest
        while (i.hasNext()) {
            Map.Entry<T,Result> e = i.next();
            double d = e.getValue().result;
            if (d<=map.lastKey()) {
                ArrayList<T> v = map.get(d);
                if (v==null) {
                    v= new ArrayList<T>();
                    map.put(d, v);
                    v.add(e.getKey());
                    map.remove(map.lastKey());
                } else {
                    v.add(e.getKey());
                }
            }
        }
        ArrayList<T> ret = new ArrayList<T>(ranks);
        if (maxTotal <0) {
            for (ArrayList<T> s : map.values()) {
                ret.addAll(s);
            }            
        } else {
            for (Double r : map.navigableKeySet()) {
                ArrayList<T> s = map.get(r);
                if (ret.size()+s.size()<=maxTotal)
                    ret.addAll(s);
                else
                    break;
            }
        }
        
        //this.min = map.firstKey();
        
        return ret;
    }

        /**
     * returns an ArrayList of elements the [ranks] lowest associated values
     * @param ranks how many unique scores to return
     * @param maxTotal return at most this number of results
     * @return 
     */
    public ArithmeticScoredOccurence<T> getLowestNMappings(int ranks, int maxTotal) {
        TreeMap<Double,ArrayList<Map.Entry<T,Result>>> map = new TreeMap<>();
        Iterator<Map.Entry<T,Result>> i = m_Results.entrySet().iterator();
        
        // get the first n scores
        while (i.hasNext() && map.size()<ranks) {
            Map.Entry<T,Result> e = i.next();
            double d = e.getValue().result;
            ArrayList<Map.Entry<T,Result>> v = map.get(d);
            if (v==null) {
                v= new ArrayList<Map.Entry<T,Result>>();
                map.put(d, v);
            }
            v.add(e);
        }
        
        // now go through the rest
        while (i.hasNext()) {
            Map.Entry<T,Result> e = i.next();
            double d = e.getValue().result;
            if (d<=map.lastKey()) {
                ArrayList<Map.Entry<T,Result>> v = map.get(d);
                if (v==null) {
                    v= new ArrayList<>();
                    map.put(d, v);
                    v.add(e);
                    map.remove(map.lastKey());
                } else {
                    v.add(e);
                }
            }
        }
        ArithmeticScoredOccurence<T> ret = new ArithmeticScoredOccurence<T> ();
        if (maxTotal <0) {
            for (ArrayList<Map.Entry<T,Result>> s : map.values()) {
                    ret.addAllNew(s);
            }            
        } else {
            for (Double r : map.navigableKeySet()) {
                ArrayList<Map.Entry<T,Result>> s = map.get(r);
                if (ret.size()+s.size()<=maxTotal)
                    ret.addAllNew(s);
                else
                    break;
            }
        }
        return ret;
    }
    
    /**
     * returns an ArrayList of elements the [ranks] lowest associated values
     * @param ranks how many unique scores to return
     * @param maxTotal return at most this number of results
     * @return 
     */
    public ArrayList<T> getLowestNEntries(int ranks, int maxTotal, final Comparator<T> firstCompare) {
        
        TreeMap<comp,ArrayList<T>> map = new TreeMap<comp,ArrayList<T>>();
        Iterator<Map.Entry<T,Result>> i = m_Results.entrySet().iterator();
        
        // get the first n scores
        while (i.hasNext() && map.size()<ranks) {
            Map.Entry<T,Result> e = i.next();
            comp d = new comp(e.getValue().result, e.getKey(), firstCompare);
            ArrayList<T> v = map.get(d);
            if (v==null) {
                v= new ArrayList<T>();
                map.put(d, v);
            }
            v.add(e.getKey());
        }
        
        // now go through the rest
        while (i.hasNext()) {
            Map.Entry<T,Result> e = i.next();
            comp d = new comp(e.getValue().result, e.getKey(), firstCompare);
            if (d.compareTo(map.lastKey())<=0) {
                ArrayList<T> v = map.get(d);
                if (v==null) {
                    v= new ArrayList<T>();
                    map.put(d, v);
                    v.add(e.getKey());
                    map.remove(map.lastKey());
                } else {
                    v.add(e.getKey());
                }
            }
        }
        ArrayList<T> ret = new ArrayList<T>(ranks);
        if (maxTotal <0) {
            for (ArrayList<T> s : map.values()) {
                ret.addAll(s);
            }            
        } else {
            for (comp r : map.navigableKeySet()) {
                ArrayList<T> s = map.get(r);
                if (ret.size()+s.size()<=maxTotal)
                    ret.addAll(s);
                else
                    break;
            }
        }
        
        //this.min = map.firstKey();
        
        return ret;
    }
    
    
    /**
     * returns an ArrayList of elements the [ranks] lowest associated values
     * @param ranks how many unique scores to return
     * @param maxTotal return at most this number of results
     * @param compare
     * @return 
     */
    public ArithmeticScoredOccurence<T> getLowestNMappings(int ranks, int maxTotal, final Comparator<T> firstCompare) {
        
        TreeMap<comp,ArrayList<Map.Entry<T,Result>>> map = new TreeMap<>();
        Iterator<Map.Entry<T,Result>> i = m_Results.entrySet().iterator();
        
        // get the first n scores
        while (i.hasNext() && map.size()<ranks) {
            Map.Entry<T,Result> e = i.next();
            comp c = new comp(e.getValue().result,e.getKey(), firstCompare);
            ArrayList<Map.Entry<T,Result>> v = map.get(c);
            if (v==null) {
                v= new ArrayList<Map.Entry<T,Result>>();
                map.put(c, v);
            }
            v.add(e);
        }
        
        // now go through the rest
        while (i.hasNext()) {
            Map.Entry<T,Result> e = i.next();
            comp c = new comp(e.getValue().result,e.getKey(), firstCompare);
            if (c.compareTo(map.lastKey())<=0) {
                ArrayList<Map.Entry<T,Result>> v = map.get(c);
                if (v==null) {
                    v= new ArrayList<>();
                    map.put(c, v);
                    v.add(e);
                    map.remove(map.lastKey());
                } else {
                    v.add(e);
                }
            }
        }
        ArithmeticScoredOccurence<T> ret = new ArithmeticScoredOccurence<T> ();
        if (maxTotal <0) {
            for (ArrayList<Map.Entry<T,Result>> s : map.values()) {
                    ret.addAllNew(s);
            }            
        } else {
            for (comp r : map.navigableKeySet()) {
                ArrayList<Map.Entry<T,Result>> s = map.get(r);
                if (ret.size()+s.size()<=maxTotal)
                    ret.addAllNew(s);
                else
                    break;
            }
        }

        this.min = map.firstKey().score;

        return ret;
    }

    
    
    public T[] getScoredSortedArray(T[] a) {
        return getSortedEntries().toArray(a);
    }

    public ArrayList<T> getSortedEntries() {

        ArrayList<ScoredObject<T,Double>> list = new ArrayList<ScoredObject<T, Double>>(m_Results.size());

        for (T o : m_Results.keySet())
            list.add(new ScoredObject<T, Double>(o, m_Results.get(o).result));
        java.util.Collections.sort(list);
        ArrayList<T> retDummy = new ArrayList<T>(list.size());
        for (ScoredObject<T, Double> o : list)
            retDummy.add(o.getStore());

        return retDummy;

    }

    public int size() {
        return m_Results.size();
    }
    
    @Override
    public Iterator<T> iterator() {
        return m_Results.keySet().iterator();
    }
    
    
    public double getLowestScore() {
        if (min  !=null) {
            return min;
        }
        Iterator<Result> it = m_Results.values().iterator();
        min = it.next().result;
        while (it.hasNext()) {
            double v = it.next().result;
            if (v<min) {
                min = v;
            }
        }
        return min;
    }

    public double getHighestScore() {
        if (max  !=null) {
            return max;
        }
        Iterator<Result> it = m_Results.values().iterator();
        max = it.next().result;
        while (it.hasNext()) {
            double v = it.next().result;
            if (v>max) {
                max = v;
            }
        }
        return max;
    }

}
