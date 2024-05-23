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
import java.util.Comparator;
import rappsilber.ms.sequence.Sequence;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class RandomFilter implements FastaFilter {

    /**
     * do we have a preferred size
     */
    private double preferredSize = Double.NaN;
    private int count;
    private double chance = Double.NaN;


    public RandomFilter(int count) {
        this.count = count;
    }
    
    public RandomFilter(double chance) {
        this.chance = chance;
    }
    
    public RandomFilter(int count, double preferedSize) {
        this(count);
        this.preferredSize = preferedSize;
    }
    
    
    @Override
    public Sequence[] getSequences(Sequence s) {
        if (Math.random() <= chance) {
            return new Sequence[] {s};
        } else {
            return new Sequence[0] ;
        }
    }

    @Override
    public Collection<Sequence> getSequences(Collection<Sequence> sequences) {
        if (Double.isNaN(chance) && count > 0) {
            if (sequences.size()<= count) {
                return sequences;
            } else {
                ArrayList<Sequence> ret = new ArrayList<Sequence>(sequences);
                if (Double.isNaN(preferredSize))  {
                    while (ret.size() > count) {
                        ret.remove((int)(Math.random()*ret.size()));
                    }
                } else {
                    int min = ret.get(0).length();
                    int max = ret.get(0).length();
                    for (Sequence s : sequences) {
                        if (min > s.length()) {
                            min = s.length();
                        } else if (max <s.length()) {
                            max = s.length();
                        }
                    }
                    double norm = Math.max(preferredSize-min,max - preferredSize);
                    
                    double ps = preferredSize;
                    while (ret.size() > count * 2) {
                        int pos = ((int)(Math.random()*ret.size()));
                        Sequence s = ret.get(pos);
                        double sd = Math.abs((s.length()-preferredSize)/norm);
                        // make sure none pass automatically and noe have a nonexistent chance
                        sd = sd/3.0+0.3;
                        
                        if (Math.random()> sd) {
                            ret.remove((int)(Math.random()*ret.size()));
                        }
                    }
                    
                    ret.sort(new Comparator<Sequence>() {
                        @Override
                        public int compare(Sequence o1, Sequence o2) {
                            return Double.compare(Math.abs(o1.length()-preferredSize), Math.abs(o2.length()-preferredSize));
                        }
                    });
                    
                    while (ret.size() >count) {
                        ret.remove(count);
                    }
                }
                return ret;
            }
        } else if (!Double.isNaN(chance) && count == 0) {
            ArrayList<Sequence> ret = new ArrayList<Sequence>(sequences.size());
            for (Sequence r : sequences) {
                if (Math.random()<=chance) {
                    ret.add(r);
                }
            }
            return ret;
        }
        
        throw new UnsupportedOperationException("either chance or count need to be defined");
    }
    
    
}
