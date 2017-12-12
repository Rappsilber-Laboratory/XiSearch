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

import rappsilber.ms.dataAccess.AbstractStackedSpectraAccess;
import rappsilber.ms.spectra.Spectra;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class Denoise  extends AbstractStackedSpectraAccess {
    double minMZ=Double.MIN_VALUE;
    double maxMZ=Double.MAX_VALUE;
    double window=100d;
    int peaks=20;

    public Denoise() {
    }

    
    public Denoise(double minMZ,double maxMZ, double window,int peaks) {
        this(window, peaks);
        this.minMZ=minMZ;
        this.maxMZ = maxMZ;
    }

    public Denoise(double window,int peaks) {
        this.window = window;
        this.peaks = peaks;
    }
    
    
    public Spectra next() {
        if (minMZ > Double.MIN_VALUE || maxMZ<Double.MAX_VALUE ) {
            return m_InnerAcces.next().cloneTopPeaksRolling(peaks, window, minMZ, maxMZ);
        }
        return m_InnerAcces.next().cloneTopPeaksRolling(peaks, window);
    }

 
    
}
