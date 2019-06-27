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
package rappsilber.ms.spectra.annotation;

import java.util.Collection;
import java.util.Iterator;
import rappsilber.config.RunConfig;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.SpectraPeakCluster;
import rappsilber.utils.SortedLinkedList;
import rappsilber.utils.Util;

/**
 * As opposed to the "normal" averagin/isotopcluster algorithm here we only
 * consider the highest charged matching isotope-cluster and therefor reduce 
 * the total number of possible cluster
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class XaminatrixIsotopAnnotation extends Averagin{

    public XaminatrixIsotopAnnotation(RunConfig conf) {
        super(conf);
    }
//    private static final double m_averagin_max_dist = 10;

    @Override
    public void findIsotopClusters(Spectra spectra, int MaxCharge) {
        SortedLinkedList<SpectraPeakCluster> isotopClusters = spectra.getIsotopeClusters();

        // don't exced the precurser charge
        MaxCharge = Math.min(MaxCharge, (int)spectra.getPrecurserCharge());

        // for faster handling get the peaks as array
        SpectraPeak[] peaks = spectra.getPeaksArray();

        int peakCount = peaks.length;

        // check each peak, whether it's a start of a isotop cluster
        for (int i = 0;i< peakCount - 1;i++) {
            if (peaks[i].hasAnnotation(SpectraPeakAnnotation.isotop))
                continue;

            // only consider peaks, that have no more then a distance of 1 (cluster for singly charged ions)
            if (spectra.getTolearance().minDiff(peaks[i].getMZ(), peaks[i + 1].getMZ()) <= 1.0){

                int lastPeak = Math.min(peakCount, i + Util.IsotopClusterMaxPeaks);





                // check for each charge
                charge: for (int charge = MaxCharge; charge > 0; charge--) {
                    SpectraPeakCluster spc = new SpectraPeakCluster(spectra.getTolearance());
                    spc.add(peaks[i]);
                    spc.setMZ(peaks[i].getMZ());
                    double diff = Util.C13_MASS_DIFFERENCE/charge;
                    SpectraPeak monoPeak = peaks[i];
                    double pMZ = monoPeak.getMZ();
                    double next = pMZ + diff;
                    double prev = pMZ - diff;
                    SpectraPeak sp = spectra.getPeakAt(next);
                    //double ratio = sp.getIntensity()/peaks[i].getIntensity();
                    if (sp != null && sp.getIntensity()/peaks[i].getIntensity() < getMaxMono2FirstPeakRatio()) {
                        do {
                            spc.add(sp);
                            sp.annotate(SpectraPeakAnnotation.isotop);
                            sp.setCharge(charge);

                            next+=diff;
                        } while ((sp = spectra.getPeakAt(next)) != null);


                        monoPeak.setCharge(charge);
                        monoPeak.annotate(SpectraPeakAnnotation.isotop);
                        monoPeak.annotate(SpectraPeakAnnotation.monoisotop);

                        spc.setMonoIsotopic(monoPeak);
                        spc.setCharge(charge);
                        spc.setMZ(pMZ);

                        isotopClusters.add(spc);

                        // maybe some of the previuos peaks were masked by a previous cluster
                        if ((sp = spectra.getPeakAt(prev)) != null) {
                            spc = (SpectraPeakCluster) spc.clone();

                            do {
                                spc.add(sp);
                                sp.annotate(SpectraPeakAnnotation.isotop);
                                sp.setCharge(charge);
                                monoPeak = sp;
                                prev-=diff;
                            } while ((sp = spectra.getPeakAt(prev)) != null);

                            monoPeak.setCharge(charge);
                            monoPeak.annotate(SpectraPeakAnnotation.isotop);
                            monoPeak.annotate(SpectraPeakAnnotation.monoisotop);

                            spc.setMonoIsotopic(monoPeak);
                            spc.setCharge(charge);
                            spc.setMZ(pMZ);

                            isotopClusters.add(spc);

                        }
                        
                        break charge; // we only considere the highest charge
                    }


                }
            }
            

        }
       // javax.swing.JOptionPane.showMessageDialog(null, isotopClusters.size());
    }

}
