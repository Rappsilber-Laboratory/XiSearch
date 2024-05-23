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
package rappsilber.ms.dataAccess.filter.spectrafilter;

import java.util.HashMap;
import rappsilber.ms.dataAccess.AbstractStackedSpectraAccess;
import rappsilber.ms.spectra.Spectra;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class ScanMemory  extends AbstractStackedSpectraAccess {

    int scanCount=0;
    HashMap<String,HashMap<Integer,Integer>> scansSeen= new HashMap<String, HashMap<Integer,Integer>>();
    public ScanMemory() {
    }

    
    public ScanMemory(String settings) {
    }
    
    
    
    public Spectra next() {
        Spectra n = m_InnerAcces.next();
        String r = n.getRun();
        Integer sn = n.getScanNumber();
        HashMap<Integer,Integer> e = scansSeen.get(r);
        if (e==null) {
            e = new HashMap<Integer,Integer>();
            scansSeen.put(r, e);
        }
        e.put(sn,scanCount++);
        return n;
    }

    
    
}
