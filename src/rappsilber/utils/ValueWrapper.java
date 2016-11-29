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
 * This is a generic object wrapper.
 * The main purpose is to enable return basic values via arguments.
 * Equivalent to C-style by reference arguments.
 * <p>Now I am aware that all objects in java are basically handed over by 
 * reference - only that the basic types like Boolean or Double are immutable 
 * or final (Don't quite understand why). Therefore we can't directly use them 
 * for that purpose and we need a wrapper that can be used to hold a then 
 * mutable value.</p>
 * For numeric values - like Double or Integer - there is a specific wrapper, 
 * {@link NumberWrapper} that provides some more convenience functions.</p>
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class ValueWrapper<T> {
    public T value;

    public ValueWrapper() {
    }
    
    public ValueWrapper(T value) {
        this.value = value;
    }
    
    public String toString() {
        return value.toString();
    }
}
