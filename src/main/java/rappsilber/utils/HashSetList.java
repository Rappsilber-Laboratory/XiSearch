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

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class HashSetList<T> implements Set<T>{

    HashMapList<T, T> m_innerMap = new HashMapList<T, T>();



    @Override
    public int size() {
        return m_innerMap.size();
    }

    @Override
    public boolean isEmpty() {
        return m_innerMap.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return m_innerMap.containsKey(o);
    }

    @Override
    public Iterator<T> iterator() {
        return m_innerMap.keySet().iterator();
    }

    @Override
    public Object[] toArray() {
        return m_innerMap.keySet().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return m_innerMap.keySet().toArray(a);
    }

    @Override
    public boolean add(T e) {
        T r = m_innerMap.put(e, e);
        return r == null;
    }

    @Override
    public boolean remove(Object o) {
        return m_innerMap.remove(o) != null;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object k : c) {
            if (!m_innerMap.containsKey(k)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean ret = false;
        for (T k : c) {
            T i = m_innerMap.put(k, k);
            ret = ret || k != i;
        }
        return ret;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean ret = false;
        for (Object k : c) {
            ret = m_innerMap.remove(k) != null || ret;
        }
        return true;
    }

    @Override
    public void clear() {
        m_innerMap.clear();
    }

}
