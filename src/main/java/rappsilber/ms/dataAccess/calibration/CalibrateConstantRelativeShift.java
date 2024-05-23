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

import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.dataAccess.*;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class CalibrateConstantRelativeShift extends StreamingCalibrate {



    protected double[] m_targetMZ;
    protected ToleranceUnit m_initialTolerance;



    public CalibrateConstantRelativeShift(double targetMZ, ToleranceUnit initialTolerance) {
        this.m_targetMZ = new double[]{targetMZ};
        this.m_initialTolerance = initialTolerance;
    }

    public CalibrateConstantRelativeShift(double[] targetMZ, ToleranceUnit initialTolerance) {
        this.m_targetMZ = targetMZ.clone();
        this.m_initialTolerance = initialTolerance;
    }

    public CalibrateConstantRelativeShift(double targetMZ, ToleranceUnit initialTolerance, SpectraAccess reader) {
        this(targetMZ, initialTolerance);
        setReader(reader);
    }

    public CalibrateConstantRelativeShift(double[] targetMZ, ToleranceUnit initialTolerance, SpectraAccess reader) {
        this(targetMZ, initialTolerance);
        setReader(reader);
    }



    @Override
    public void calibrate(Spectra s) {
        double diff = 0;
        int count = 0;
        for (double targetMZ : m_targetMZ) {
            SpectraPeak tp = s.getPeakAt(targetMZ, m_initialTolerance);
            if (tp != null) {
                diff += (targetMZ - tp.getMZ()) / targetMZ;
                count ++;
            }
        }

        if (count > 0) {
            diff /= count;
            for (SpectraPeak sp : s) {
                sp.setMZ(sp.getMZ() + (sp.getMZ()*diff));
            }
        }
    }
}
