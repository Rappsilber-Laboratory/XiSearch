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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import rappsilber.config.AbstractRunConfig;
import rappsilber.ms.dataAccess.AbstractStackedSpectraAccess;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.SpectraPeakCluster;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class RemoveSinglePeaks  extends AbstractStackedSpectraAccess {



    public RemoveSinglePeaks() {
    }

    
    public RemoveSinglePeaks(String settings) {
    }
    
   
    
    public Spectra next() {
        Spectra n =m_InnerAcces.next();
        Spectra r = n.cloneEmpty();
        HashSet<SpectraPeak> peaks = new HashSet<>();
        for (SpectraPeakCluster spc :n.getIsotopeClusters()) {
            for (SpectraPeak sp : spc) {
                peaks.add(sp);
            }
        }
        for (SpectraPeak sp : peaks) {
            r.addPeak(sp);
        }
                    

        return r;
    }
    
}
