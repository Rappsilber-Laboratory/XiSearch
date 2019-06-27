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
import java.util.Map;

/**
 * Just some function to ease the comparison of maps
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public abstract class MapUtils {
    
    /**
     * Checks whether two maps contain the same mappings. <br/>
     * this function specifically works on maps, where the value is a map.
     * It compares whether both maps contain the same  keys and each key map to a collection that contains the same values.
     * @param <K> Any type of key
     * @param <V> A Map-type as Value for the maps
     * @param m1 first map to compare
     * @param m2 second map to compare
     * @return whether both maps are the same
     */
    public static <K, V extends Collection> boolean sameMapCollection (Map<K,V> m1, Map<K,V> m2) {
        
        // check for each entry in m1 wether it is contained also in m2
        for (Map.Entry<K,V> e1 : m1.entrySet()) {
            K k1 = e1.getKey();
            V v1 = e1.getValue();
            V v2 = m2.get(k1);
            if (v1!= null || v2 != null) {
                if (v1 !=null && v2 != null) {
                    if (!v1.containsAll(v2))
                        return false;
                } else {
                    return false;
                }
            }
        }
        
        return true;
    
    }

    /**
     * Checks whether two maps contain the same entries. <br/>
     * 
     * @param m1 first map to compare
     * @param m2 second map to compare
     * @return whether both maps are the same
     */
    public static <K, V> boolean same (Map<K,V> m1, Map<K,V> m2) {
        
        // check for each entry in m1 wether it is contained also in m2
        for (Map.Entry<K,V> e1 : m1.entrySet()) {
            K k1 = e1.getKey();
            V v1 = e1.getValue();
            V v2 = m2.get(k1);
            if (v1 != null) {
                if (!v1.equals(v2))
                    return false;
                
            } else if (v2!= null)
                return false;
        }
        
        return true;
    
    }
    
    
}
