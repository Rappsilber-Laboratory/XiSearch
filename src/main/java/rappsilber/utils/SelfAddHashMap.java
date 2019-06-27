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
import java.util.HashMap;
import java.util.Iterator;

/**
 * A hashmap, to automatically collate links and protein interactions that are supported by different peptide pairs/links.
 * @param <T>
 */
public class SelfAddHashMap<T extends SelfAdd<T>> extends HashMap<T, T> implements Iterable<T> {
    /**
     * if selfAdd is false then the actual add is not called but only the previously stored instance is returned.
     */
    public boolean selfAdd = true;

    /**
     * Add a instance of T to the hashmap.
     * If there is already an instance, that equals the current one, then the current one is "added" to the already existing one and the existing one is returned.
     * @param value
     * @return the (possibly newly) stored instance of T, that equals value
     */
    public T add(T value) {
        T cont = this.get(value);
        if (cont == null) {
            this.put(value, value);
            return value;
        } else {
            if (selfAdd) {
                cont.add(value);
            }
            return cont;
        }
    }

    /**
     * calls add on each element of the collection
     * @param values
     * @return
     */
    public ArrayList<T> addAll(Collection<T> values) {
        ArrayList<T> ret = new ArrayList<T>(values.size());
        for (T v : values) {
            ret.add(add(v));
        }
        return ret;
    }

    /**
     * returns an iterator over all stored instances
     * @return
     */
    public Iterator<T> iterator() {
        return this.keySet().iterator();
    }

    /**
     * check, whether it contains the given object
     * @param o
     * @return
     */
    public boolean contains(Object o) {
        return super.containsKey(o);
    }

    /**
     * returns an array of all stored instances
     * @return
     */
    public Object[] toArray() {
        return keySet().toArray();
    }

    public <T> T[] toArray(T[] a) {
        return keySet().toArray(a);
    }

    public boolean containsAll(Collection<?> c) {
        return super.keySet().containsAll(c);
    }

    public boolean removeAll(Collection<?> c) {
        boolean del = false;
        for (Object o : c) {
            del |= (super.remove(o) != null);
        }
        return del;
    }

    public boolean retainAll(Collection<?> c) {
        ArrayList<T> del = new ArrayList<T>();
        for (T k : keySet()) {
            if (!c.contains(k)) {
                del.add(k);
            }
        }
        for (T k : del) {
            remove(k);
        }
        return !del.isEmpty();
    }
    
    
    public SelfAddHashMap<T> clone() {
        SelfAddHashMap<T> ret = new SelfAddHashMap<T>();
        ret.addAll(this.keySet());
        return ret;
    }
    
}
