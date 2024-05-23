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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class TreeMapList<K,V> extends AbstractMap<K,V>
    implements NavigableMap<K,V>, Cloneable, java.io.Serializable {
    private static final long serialVersionUID = 5859017242927142851L;

    @Override
    public Entry<K, V> lowerEntry(K key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public K lowerKey(K key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Entry<K, V> floorEntry(K key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public K floorKey(K key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Entry<K, V> ceilingEntry(K key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public K ceilingKey(K key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Entry<K, V> higherEntry(K key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public K higherKey(K key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Entry<K, V> firstEntry() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Entry<K, V> lastEntry() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Entry<K, V> pollFirstEntry() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Entry<K, V> pollLastEntry() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SortedMap<K, V> headMap(K toKey) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Comparator<? super K> comparator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public K firstKey() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public K lastKey() {
        throw new UnsupportedOperationException("Not supported yet.");
    }




    private class ListEntry implements Map.Entry<K,V>{

        K key;
        V value;

        public ListEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V ov = value;
            this.value = value;
            return ov;
        }

    }

    private class ArraySet<T> extends ArrayList<T> implements Set<T>{
        private static final long serialVersionUID = -6166124106388533671L;

        public ArraySet(int size) {
            super(size);
        }

    }

    int m_size = 0;

    TreeMap<K,ArrayList<ListEntry>> m_InnerMap = new TreeMap<K, ArrayList<ListEntry>>();

    @Override
    public int size() {
        return m_size;
    }

    @Override
    public boolean isEmpty() {
        return m_size == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        ArrayList<ListEntry> pairs = m_InnerMap.get(key);
        if (pairs != null) {
            for (ListEntry le : pairs) {
                if (le.key == key) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        for (ArrayList<ListEntry> keyList : m_InnerMap.values()) {
            for (ListEntry le : keyList) {
                if (le.value == value) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        ArrayList<ListEntry> pairs = m_InnerMap.get(key);
        if (pairs != null) {
            for (ListEntry le : pairs) {
                if (le.key.equals(key)) {
                    return le.value;
                }
            }
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        ArrayList<ListEntry> pairs = m_InnerMap.get(key);
        if (pairs != null) {
            for (ListEntry le : pairs) {
                if (le.key == key) {
                    V ov = le.value;
                    le.value = value;
                    return ov;
                }
            }
            m_size++;
            pairs.add(new ListEntry(key, value));
            return null;
        }
        pairs = new ArrayList<ListEntry>();
        pairs.add(new ListEntry(key, value));
        m_InnerMap.put(key, pairs);
        m_size++;
        return null;
    }

    @Override
    public V remove(Object key) {
        ArrayList<ListEntry> pairs = m_InnerMap.get(key);
        if (pairs != null) {
            int dIndex = -1;
            ListEntry le = null;
            for (int de = 0; de < pairs.size(); de++) {
                le = pairs.get(de);
                if (le.key == key) {
                    dIndex = de;
                    break;
                }
            }
            if (dIndex >=0) {
                V ov = le.value;
                pairs.remove(dIndex);
                m_size--;
                if (pairs.size() == 0) {
                    m_InnerMap.remove(key);
                }
                return ov;
            }
        }
        return  null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e :  m.entrySet()) {
            put(e.getKey(),e.getValue());
        }
    }

    @Override
    public void clear() {
        m_InnerMap.clear();
        m_size=0;
    }

    @Override
    public Set<K> keySet() {
        ArraySet<K> ks = new ArraySet<K>(m_size);
        for (ArrayList<ListEntry> pairs : m_InnerMap.values()) {
            for (ListEntry le : pairs) {
                ks.add(le.key);
            }
        }
        return ks;
    }

    @Override
    public Collection<V> values() {
        ArraySet<V> vs = new ArraySet<V>(m_size);
        for (ArrayList<ListEntry> pairs : m_InnerMap.values()) {
            for (ListEntry le : pairs) {
                vs.add(le.value);
            }
        }
        return vs;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        ArraySet<Entry<K, V>> es = new ArraySet<Entry<K, V>>(m_size);
        for (ArrayList<ListEntry> pairs : m_InnerMap.values()) {
            es.addAll(pairs);
        }
        return es;
    }

}

