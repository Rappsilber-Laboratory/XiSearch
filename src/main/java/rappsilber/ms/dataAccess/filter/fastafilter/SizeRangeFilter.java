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
package rappsilber.ms.dataAccess.filter.fastafilter;

import java.util.ArrayList;
import java.util.Collection;
import rappsilber.ms.sequence.Sequence;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class SizeRangeFilter implements FastaFilter {

    public static enum filtermode {INCLUDE, EXCLUDE};
    
    public static filtermode INCLUDE = filtermode.INCLUDE;
    public static filtermode EXCLUDE = filtermode.EXCLUDE;
    private filtermode m_mode = filtermode.INCLUDE;
    
    int minSize;
    int maxSize;

    public SizeRangeFilter(int minSize, int maxSize) {
        this.minSize = minSize;
        this.maxSize = maxSize;
    }
    
    public SizeRangeFilter(int minSize, int maxSize, filtermode mode) {
        this(minSize, maxSize);
        m_mode = mode;
    }
    
    public void setFilterMode(filtermode mode) {
        m_mode = mode;
    }
    
    boolean passes(Sequence s) {
        boolean inrange = s.length()>=minSize && s.length()<=maxSize;
        if (m_mode == filtermode.INCLUDE) {
            return inrange;
        } else {
            return !inrange;
        }
        
    }
    
    public Sequence[] getSequences(Sequence s) {
        if (passes(s)) {
            return new Sequence[] {s};
        } else {
            return new Sequence[0];
        }
    }

    public Collection<Sequence> getSequences(Collection<Sequence> sequences) {
        ArrayList<Sequence> ret = new ArrayList<Sequence>(sequences.size());
        for (Sequence s: sequences) {
            if (passes(s)) {
                ret.add(s);
            }
        }
        return ret;
    }
    
    
}
