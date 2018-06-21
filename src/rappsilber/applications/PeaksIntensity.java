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
import java.text.DecimalFormat;
import java.text.ParseException;
import rappsilber.ms.Range;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.dataAccess.AbstractSpectraAccess;
import rappsilber.ms.dataAccess.SpectraAccess;
import rappsilber.ms.dataAccess.calibration.CalibrateConstantRelativeShift;
import rappsilber.ms.dataAccess.calibration.CalibrateConstantShift;
import rappsilber.ms.dataAccess.calibration.CalibrateLinearRegression;
import rappsilber.ms.dataAccess.calibration.CalibrateLinearRegression2PeakGroups;
import rappsilber.ms.dataAccess.calibration.StreamingCalibrate;
import rappsilber.ms.dataAccess.msm.AbstractMSMAccess;
import rappsilber.ms.dataAccess.msm.MSMIterator;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class PeaksIntensity {
    
    
    public static void main(String args[]) throws FileNotFoundException, ParseException, IOException {
       // File msmFile = new File("/home/lfischer/Projects/IndicatorPeaks/XlinkData/Munich_XsynHCD50_XLinkMatch.msm");
       // File msmFile = new File("/home/lfischer/Projects/IndicatorPeaks/Munich/mgf/5PercentFalsePositive_mgf.msm");
        File msmFile = new File("/media/backup/1_MS_Data/Dundee/hcd_091218_15_DU_ZC_100min_IN_yeastnew2ugHCD50_1.mgf");
        double[] targetpeaks = new double[]{
            138.09134,
139.07536,
156.10191,
157.08592,
192.13829,
194.15394,
221.16484,
222.14886,
239.1754,
240.15942,
257.18597,
267.17032,
285.18088,
305.22235,
350.24382,
368.25438,

175.119495287, // Y1 Argenin
355.069933,
429.088735,     // Lock mass
503.107515,
577.126307,
651.145098,
725.163890
        };
        //double[] targetpeaks = new double[]{175.119};

        double[] calibrationpeaks1 = new double[]{
175.119495287,//	R
        };
        double[] calibrationpeaks2 = new double[]{
246.156605287,//	RA
278.128685287,//	RC
290.146435287,//	RD
304.162085287,//	RE
322.187905287,//	RF
232.140955287,//	RG
312.178405287,//	RH
288.203555287,//	RI
303.214455287,//	RK
288.203555287,//	RL
306.159985287,//	RM
289.162425287,//	RN
272.172255287,//	RP
303.178075287,//	RQ
331.220605287,//	RR
262.151525287,//	RS
276.167175287,//	RT
274.187905287,//	RV
361.198805287,//	RW
338.182825287,//	RY
322.154895287,//	RMox
335.150151287,//	RCcis
        };
        ToleranceUnit t = new ToleranceUnit("10ppm");
        
        
        SpectraAccess msm = new MSMIterator(msmFile, new ToleranceUnit("0.1ppm"), 0, null);

        StreamingCalibrate cal = new CalibrateConstantRelativeShift(calibrationpeaks1, t, msm);
//        StreamingCalibrate cal = new CalibrateLinearRegression2PeakGroups(calibrationpeaks1, calibrationpeaks2, t, msm);
//        msm = cal;
        DecimalFormat fiveDigits = new DecimalFormat("0.0#########");

//        while (cal.hasNext()) {
        while (msm.hasNext()) {

            //first calibrate the spectra
//            Spectra s = cal.next();
            Spectra s = msm.next();
            //get the peaks in a 20 ppm range around the target peaks
            for (double p : targetpeaks) {
                Range r = t.getRange(p);
                for (SpectraPeak sp : s.getPeaks(r.min, r.max)) {
                    System.out.println(s.getRun() +" " + s.getScanNumber() + " " + sp.toString(fiveDigits));
                }
            }
        }


    }
}
