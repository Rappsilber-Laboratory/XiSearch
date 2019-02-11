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
import java.util.Comparator;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.dataAccess.BufferedSpectraAccess;
import rappsilber.ms.dataAccess.SpectraAccess;
import rappsilber.ms.dataAccess.msm.AbstractMSMAccess;
import rappsilber.ms.dataAccess.msm.MSMIterator;
import rappsilber.ms.dataAccess.output.MSMWriter;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.utils.SortedLinkedList;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class TopY1R1 {

    public static void main(String[] args) throws FileNotFoundException, IOException, ParseException {


        SpectraAccess msmit = AbstractMSMAccess.getMSMIterator(new File("/home/lfischer/Projects/IndicatorPeaks/Dundee/mgf/hcd50.msmlist"), new ToleranceUnit("10ppm"), 0, null);
        BufferedSpectraAccess bfa = new BufferedSpectraAccess(msmit, 10);
        msmit = bfa;
        
        MSMWriter mWriter = new MSMWriter(
                new File("/home/lfischer/Projects/IndicatorPeaks/Dundee/mgf/hcd50_Top800Y1R.msm")
                , null, null, null);
         
        final double targetmz = 175.119495287;
        int topN = 800;

        double targetIntensity = 3046;
        Comparator<Spectra> spectraCompare = new Comparator<Spectra>(){

            @Override
            public int compare(Spectra o1, Spectra o2) {
                SpectraPeak sp1 = o1.getPeakAt(targetmz);
                SpectraPeak sp2 = o2.getPeakAt(targetmz);
                if (sp1 == null) {
                    if (sp2 == null)
                        return 0;
                    return -1;
                }
                if (sp2 == null)
                    return 1;
                double int1 = sp1.getIntensity();
                double int2 = sp2.getIntensity();

                if (int1 == int2)
                    return 0;

                if (int1> int2)
                    return 1;

                return -1;
            }

        };

        SortedLinkedList<Spectra> topSpectra = new SortedLinkedList<Spectra>(spectraCompare);

        Spectra s = msmit.next();
        topSpectra.add(s);
                  
        while (msmit.hasNext()) {
            s = msmit.next();
            if (topSpectra.size()< topN)
                topSpectra.add(s);
            else
                if (spectraCompare.compare(s, topSpectra.getLast()) >0 ) {
                    topSpectra.add(s);
                    topSpectra.removeLast();
                }
        }

        mWriter.writeHeader();
        for (Spectra ts : topSpectra)
            mWriter.writeSpectra(ts);
        mWriter.close();

    }

}
