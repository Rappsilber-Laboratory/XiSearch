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
package rappsilber.ms.dataAccess.msm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import rappsilber.config.RunConfig;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.dataAccess.AbstractSpectraAccess;
import rappsilber.ms.dataAccess.SpectraAccess;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public abstract class AbstractMSMAccess extends AbstractSpectraAccess {
    protected double m_MaxPrecursorMass = Double.POSITIVE_INFINITY;
    protected int    m_scanCount    = -1;
//    protected int    m_countSpectra = -1;
    protected ToleranceUnit   m_ToleranceUnit = ToleranceUnit.ZEROTOLERANCE;
    protected String          m_inputPath = null;
    protected int               countTotelSpectra = 0;


    


    public static AbstractMSMAccess getMSMIterator(String path, ToleranceUnit t, int minCharge, RunConfig config) throws FileNotFoundException, IOException, ParseException {
        return getMSMIterator(new File(path), t, minCharge,config);
    }

    public static AbstractMSMAccess getMSMIterator(File path, ToleranceUnit t, int minCharge, RunConfig config) throws FileNotFoundException, IOException, ParseException {
        if (path.getName().toLowerCase().endsWith(".list") || path.getName().toLowerCase().endsWith(".msmlist")) {
            return new MSMListIterator(path, t, minCharge,config);
        } else if (path.getName().toLowerCase().endsWith(".zip")) {
            return new ZipMSMListIterator(path, t, minCharge,config);
        } else if (path.getName().toLowerCase().endsWith(".apl"))  {
            return new APLIterator(path, t, minCharge, config);
        } else if (path.getName().toLowerCase().endsWith(".mzml"))  {
            return new MzMLIterator(path, t, minCharge, config);
        } else
            return new MSMIterator(path, t, minCharge, config);
    }

    public static AbstractMSMAccess getMSMIterator(String name, InputStream input, ToleranceUnit t, int minCharge, RunConfig config) throws FileNotFoundException, IOException, ParseException {
        if (name.toLowerCase().endsWith(".apl"))  {
            return new APLIterator(input, name, t, minCharge, config);
        } else
            return new MSMIterator(input, name, t, minCharge, config);
    }
    
    
    
    /**
     * @return the toleranceUnit that gets assigned to each read spectrum
     */
    public ToleranceUnit getToleranceUnit() {
        return m_ToleranceUnit;
    }

    /**
     * @param toleranceUnit the toleranceUnit to that assigned to each read spectrum
     */
    public void setToleranceUnit(ToleranceUnit toleranceUnit) {
        this.m_ToleranceUnit = toleranceUnit;
    }


    /**
     * @return the MaxPrecursorMass returns the highest observed precursor mass
     */
    public double getMaxPrecursorMass() {
        return m_MaxPrecursorMass;
    }

//    /**
//     * @return the number of entries (without regard for unsure charge-state)
//     */
//    public int getEntriesCount() {
//        return m_scanCount;
//    }
//
//    /**
//     * @return the number of entries including duplicate entries for unsure
//     *          charge-states
//     */
    public int getSpectraCount() {
        return m_scanCount;
    }

    /**
     * @return the m_inputPath
     */
    public String getInputPath() {
        return m_inputPath;
    }

    /**
     * @return the m_inputPath
     */
    public void setInputPath(String path) {
        m_inputPath = path;
    }

}
