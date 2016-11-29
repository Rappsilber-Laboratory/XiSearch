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

import java.util.Comparator;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class UpdatableString implements java.io.Serializable, Comparable<UpdatableString>, Comparator<UpdatableString>, CharSequence{
    public String value;

    public UpdatableString(String v) {
        value = v;
    }

    public int compareTo(String o) {
        return value.compareTo(o);
    }

    public int compareTo(UpdatableString o) {
        return value.compareTo(o.value);
    }
    
    public int length() {
        return value.length();
    }

    public char charAt(int index) {
        return value.charAt(index);
    }

    public CharSequence subSequence(int start, int end) {
        return value.subSequence(start, end);
    }

    public int compare(UpdatableString o1, UpdatableString o2) {
        return o1.value.compareTo(o2.value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
