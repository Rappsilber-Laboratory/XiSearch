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
package rappsilber.applications;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.dataAccess.BufferedSpectraAccess;
import rappsilber.ms.dataAccess.SpectraAccess;
import rappsilber.ms.dataAccess.msm.AbstractMSMAccess;
import rappsilber.ms.dataAccess.msm.MSMIterator;
import rappsilber.ms.dataAccess.output.MSMWriter;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class MinimumY1R {

    public static void main(String[] args) throws FileNotFoundException, IOException, ParseException {

        SpectraAccess msmit = AbstractMSMAccess.getMSMIterator(new File("/home/lfischer/Projects/IndicatorPeaks/Dundee/mgf/hcd50.msmlist"), new ToleranceUnit("10ppm"), 0, null);
        BufferedSpectraAccess bfa = new BufferedSpectraAccess(msmit, 10);
        msmit = bfa;
        
        MSMWriter mWriter = new MSMWriter(
                new File("/home/lfischer/Projects/IndicatorPeaks/Dundee/mgf/hcd50_min3046Y1R.msm")
                , null, null, null);
         
        double targetmz = 175.119495287;
        double targetIntensity = 3046;

        mWriter.writeHeader();
                  
        while (msmit.hasNext()) {
            Spectra s = msmit.next();
            SpectraPeak tp = s.getPeakAt(targetmz);
            if (tp != null && tp.getIntensity() > targetIntensity) {
                mWriter.writeSpectra(s);
            }
        }
        mWriter.close();

    }

}
