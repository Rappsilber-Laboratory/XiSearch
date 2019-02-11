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
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.ms.dataAccess.AbstractStackedSpectraAccess;
import rappsilber.ms.spectra.Spectra;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class CompareScanMemory  extends ScanMemory {

    int diff=0;
    ScanMemory compareto;
    public CompareScanMemory(ScanMemory compareTo) {
        this.compareto = compareTo;
    }

    
    public CompareScanMemory(String settings) {
    }
    
    
    
    public Spectra next() {
        Spectra n = super.next();
        String r = n.getRun();
        Integer sn = n.getScanNumber();
        int prev = compareto.scansSeen.get(r).get(sn);
        if (prev - scanCount-1 != diff) {
            int lost = prev - scanCount - diff-1;
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING,"Lost " + lost +" scans.\n  Compare has read " +compareto.scanCount +" scans while I read " + scanCount + " scans");
            diff = prev - scanCount-1;
        }
        scanCount++;
        return n;
    }

    
    
}
