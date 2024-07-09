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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.TreeSet;
import rappsilber.ms.Range;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.dataAccess.*;
import rappsilber.ms.spectra.Spectra;


/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class MassFilteredSpectrumAccess extends AbstractSpectraFilter{
    TreeSet<Double> m_filteredMasses = new TreeSet<Double>();
    
//    int m_readSpectra = 0;

    SpectraAccess m_reader;

//    Spectra m_current = null;
//    Spectra m_next = null;
    ToleranceUnit m_tolerance = new ToleranceUnit(20, "ppm");


    private Spectra m_spectrum;


    public void readFilter(File f) throws FileNotFoundException, IOException {
        readFilter(new FileInputStream(f));
    }

    public void readFilter(InputStream filter) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(filter));
        String line;
        while ((line = br.readLine()) != null) {

            String[] parts = line.split(",");

            if (parts.length > 0 && parts[0].length() > 0 &&  parts[0].matches("[0-9\\.]+")) {
                m_filteredMasses.add(Double.valueOf(parts[0]));
            }

        }

    }


    public MassFilteredSpectrumAccess(ToleranceUnit t) {
        m_tolerance = t;
    }

    public MassFilteredSpectrumAccess(SpectraAccess innerAccess, ToleranceUnit t) {
        this(t);
        setReader(innerAccess);
//        m_reader = innerAccess;
//        next();
    }


    public void SelectMass(Double mass) {
        m_filteredMasses.add(mass);
    }

    @Override
    public boolean passScan(Spectra s) {
        double precMass = s.getPrecurserMass();
        Range r = m_tolerance.getRange(precMass);
        return (m_filteredMasses.subSet(r.min,r.max).size() > 0);
    }

}
