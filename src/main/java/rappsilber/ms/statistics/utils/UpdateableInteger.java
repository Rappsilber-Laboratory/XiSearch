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

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class UpdateableInteger extends Number implements Comparable<UpdateableInteger>{
    private static final long serialVersionUID = -9199441316689581338L;
    public int value;

    public UpdateableInteger(int value) {
        this.value = value;
    }

    public UpdateableInteger(Number value) {
        this.value = value.intValue();
    }

    public UpdateableInteger(UpdateableInteger value) {
        this.value = value.value;
    }
    
    @Override
    public int intValue() {
        return value;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }

    public int compareTo(UpdateableInteger o) {
        return value - o.value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UpdateableInteger) {
            return value == ((UpdateableInteger) obj).value;
        } else if (obj instanceof Integer) {
            Integer i = (Integer) obj;
            return value == i;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value;
    }
    
    
    

}
