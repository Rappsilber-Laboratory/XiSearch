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
public class UpdateableDouble extends Number implements Comparable<UpdateableDouble>{
    private static final long serialVersionUID = -9199441316689581338L;
    public double value;

    public UpdateableDouble(double value) {
        this.value = value;
    }

    @Override
    public int intValue() {
        return (int)value;
    }

    @Override
    public long longValue() {
        return (long)value;
    }

    @Override
    public float floatValue() {
        return (float)value;
    }

    @Override
    public double doubleValue() {
        return value;
    }

    public int compareTo(UpdateableDouble o) {
        double diff = value - o.value;
        if (diff == 0) {
            return 0;
        } else {
            return diff < 0 ? -1 : 1 ;
        }
    }

    @Override
    public String toString() {
        return Double.toString(value);
    }

}
