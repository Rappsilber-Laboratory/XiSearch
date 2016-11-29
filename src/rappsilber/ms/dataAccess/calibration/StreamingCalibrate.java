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
package rappsilber.ms.dataAccess.calibration;

import java.util.Iterator;
import rappsilber.ms.dataAccess.*;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ms.spectra.Spectra;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public abstract class StreamingCalibrate extends AbstractStackedSpectraAccess implements  Calibration{

 //   SpectraAccess m_innerReader = null;

//    @Override
//    public void setReader(SpectraAccess innerAccess) {
//        m_innerReader = innerAccess;
//    }

    @Override
    public Spectra next() {
        Spectra s = m_InnerAcces.next();
        calibrate(s);
        return s;
    }

//    @Override
//    public Spectra current() {
//        return m_innerReader.current();
//    }

//    @Override
//    public int countReadSpectra() {
//        return m_innerReader.countReadSpectra();
//    }

//    public boolean hasNext() {
//         return m_innerReader.hasNext();
//    }


}
