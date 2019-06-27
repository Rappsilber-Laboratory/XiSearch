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

/**
 * This is a generic object wrapper for numeric types.
 * The main purpose is to enable return basic values via arguments.
 * Equivalent to C-style by reference arguments.
 * <p>Now I am aware that all objects in java are basically handed over by 
 * reference - only that the basic types like Boolean or Double are immutable 
 * or final (Don't quite understand why). Therefore we can't directly use them 
 * for that purpose and we need a wrapper that can be used to hold a then 
 * mutable value.</p>
 * <p>This class provides a set of proxy functions of the {@link Number} and
 * {@link Comparable} interfaces that call the according functions on the 
 * underlying object.</p>
 * <p>For non-numeric values - like Double or Integer - there is a generic wrapper: 
 * {@link ValueClass}.</p>
 * 
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class NumberWrapper<T extends Number & Comparable<T>> extends Number implements Comparable<NumberWrapper<T>>{
    public T value;

    public NumberWrapper() {
    }

    public NumberWrapper(T value) {
        this.value = value;
    }
    
    
    @Override
    public int intValue() {
        return value.intValue();
    }

    @Override
    public long longValue() {
        return value.longValue();
    }

    @Override
    public float floatValue() {
        return value.floatValue();
    }

    @Override
    public double doubleValue() {
        return value.doubleValue();
    }

    public int compareTo(T o) {
        return value.compareTo(o);
    }

    @Override
    public int compareTo(NumberWrapper<T> o) {
        return value.compareTo(o.value);
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
    
}
