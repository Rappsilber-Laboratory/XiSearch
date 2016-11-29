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

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;
import rappsilber.ms.ToleranceUnit;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class ToleranceTreeSet extends TreeSet<Double> {
    ToleranceUnit m_tolerance;

    public ToleranceTreeSet(Collection<? extends Double> c, ToleranceUnit t) {
        super(c);
        setTolerance(t);
    }
    public ToleranceTreeSet(ToleranceUnit t) {
        super();
        setTolerance(t);
    }
    public ToleranceTreeSet(SortedSet<Double> c, ToleranceUnit t) {
        super(c);
        setTolerance(t);
    }

    public Double get(double t) {
        SortedSet<Double> s = super.subSet(m_tolerance.getMinRange(t), m_tolerance.getMaxRange(t));
        if (s.size() == 0)
            return null;
        else
            return s.first();
    }

    public void setTolerance(ToleranceUnit t) {
        m_tolerance = t;
    }

}
