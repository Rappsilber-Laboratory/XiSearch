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
package rappsilber.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.AbstractRunConfig;
import rappsilber.config.LocalProperties;
import rappsilber.config.RunConfig;
import rappsilber.config.RunConfigFile;
import rappsilber.gui.GetFile;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.dataAccess.msm.AbstractMSMAccess;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.utils.SortedLinkedList;
/** Class used to test reading the MSM file and generating the corresponding MGC
 * ans MGX components
 * @author stahir
 */
public class ReadMSM {

    public static void main(String args[]) throws ParseException, IOException{
        ToleranceUnit peakTollerance = new ToleranceUnit(0.035d, "da");
//       ToleranceUnit peakTollerance = new ToleranceUnit(20, "ppm");
        try {
            
            RunConfig conf = null;
            String filename;
            // System.out.println("Enter msm file to parse: ");
            // filename = args[0];
            if (args.length > 0) {
                filename = args[0];
            } else {
                filename = GetFile.getFile(".msm", "MSM-File (*.msm)", LocalProperties.getLastMSMFolder().getAbsolutePath(),null);
            }
            File f = new File(filename);

            if (args.length > 1) {
                conf = new RunConfigFile(args[1]);
            } else if (args.length == 0) {
                String confFile = GetFile.getFile(".conf", "config file", LocalProperties.getLastMSMFolder().getAbsolutePath(),null);
                if (confFile != null && !confFile.isEmpty()) {
                    conf = new RunConfigFile(confFile);
                } else {
                    conf = new AbstractRunConfig() {};
                }
            }
            AbstractMSMAccess it = AbstractMSMAccess.getMSMIterator(f.getAbsolutePath(), peakTollerance, 0, conf);
//            MSMIterator it = new MSMIterator(f,peakTollerance, 3, conf);

            System.out.println(f.getName());
            System.out.println(it.hasNext());
            while(true){
                Spectra msm=null;
                try {
                    if (!it.hasNext()) {
                        break;
                    }

                    msm = it.next();
                } catch (Exception e) {
                    System.err.println(" could an error :" +e + "\n try to continue!"); 
                    continue;
                }
                
                msm.setTolearance(peakTollerance);
                // Read in the relevant information about the spectrum
                System.out.println(msm.getPrecurserCharge() + " " + msm.getPrecurserMZ());


               Spectra mgc = msm.getMgcSpectra();
               Spectra mgx = msm.getMgxSpectra();

               System.out.println("***MSM***:\n" + msm.toString());
               System.out.println("***MGC***:\n" + mgc.toString());
               

               // In ascending MZ values
               ArrayList<SpectraPeak> top10 = new ArrayList(mgc.getTopPeaks(10));
               // In ascending MZ values
               SortedLinkedList<SpectraPeak> mgx_peaks = new SortedLinkedList<SpectraPeak>(mgx.getPeaks());


                System.out.println("top 10 peaks");

                Collections.sort(top10);
//                for(int i = 0; i < top10.size(); i++){
////                    SpectraPeak sp = top10.get(i);
////                    System.out.println(sp.toString());
//                }

//                for(int j = 0; j < mgx_peaks.size(); j++){
//                    SpectraPeak sp = mgx_peaks.get(j);
//                    System.out.println(sp.toString());
//                }
                break;
            }






            // Must iterate through the entire set of spectra - generate duplicates
            // (similar to the way it is already done - but without peaks - and count total spectra
            // with duplication of charge states

            // At the same time, keep the largest exp_mass too so we can build the lin-frag tree a
            // accordingly

            // Must check MGC spectra for corectness also




        } catch (FileNotFoundException ex) {
            Logger.getLogger(ReadMSM.class.getName()).log(Level.SEVERE, null, ex);
        }


    }// end method


}// end class
