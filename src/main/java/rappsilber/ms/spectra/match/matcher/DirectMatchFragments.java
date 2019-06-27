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
package rappsilber.ms.spectra.match.matcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeakCluster;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.annotation.SpectraPeakAnnotation;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.utils.Util;

/**
 * Non-fancy matching of possible fragments against the spectra
 * first all detected isotope cluster gets checked, whether there m/z and charge
 * state resuld in a mass - 
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class DirectMatchFragments implements Match{


    public Spectra matchFragments(Spectra s, ArrayList<Fragment> frags, ToleranceUnit tolerance, MatchedFragmentCollection matchedFragments) {
        //matchedFragments = new MatchedFragmentCollection();
        Collection<SpectraPeakCluster> clusters = s.getIsotopeClusters();
        Spectra unmatched = s.cloneEmpty();
        // first go through the peaks/peak clusters, match them to the first peptide
        // and store the none matched ones in a new spectra
        // first the cluster
        for (SpectraPeakCluster c : clusters) {
            SpectraPeak m = c.getMonoIsotopic();
            if (! s.getPeaks().contains(m)) {
                m.annotate(SpectraPeakAnnotation.virtual);
                s.addPeak(m);
                Logger.getLogger(this.getClass().getName()).log(Level.INFO,"virtual peak");
            }

            boolean matched = false;
            double cmz = c.getMZ();
            int cCharge = c.getCharge();
            double monoMZ = cmz;
            double missingMZ = cmz - (Util.PROTON_MASS/cCharge);



            for (Fragment f : frags) {
//                if (Double.isNaN(f.getMass()) )
//                    System.out.println("Strange");
                double mz = f.getMZ(cCharge);
                if (tolerance.compare(monoMZ, mz) == 0 ) {
                    // ok found a matching fragment for the peak
                    m.annotate(new SpectraPeakMatchedFragment(f, cCharge, c));
                    matchedFragments.add(f, cCharge, m);
                    matched = true;
                } else if (tolerance.compare(missingMZ, mz) == 0 ) {
                    // matches a missing peak
                    matched = true;
//                    SpectraPeak p = s.getPeakAt(missingMZ);
//                    if (p) {
//
//                    } else
                    m.annotate(new SpectraPeakMatchedFragment(f, cCharge, missingMZ, c));
                    matchedFragments.add(f, cCharge, m);

                }
            }
            if (!matched) {
                unmatched.getIsotopeClusters().add(c);
                unmatched.addPeak(c.getMonoIsotopic());
            }

        }

        int maxCharge = s.getPrecurserCharge();
        // next single peaks
        for (SpectraPeak p : s.getPeaks()) {
            if (p.hasAnnotation(SpectraPeakAnnotation.isotop) || p.hasAnnotation(SpectraPeakAnnotation.monoisotop))
                continue;

            double peakMZ = p.getMZ();


            boolean matched = false;
            for (Fragment f : frags) {
                double neutralMass = f.getNeutralMass();
                if (tolerance.compare(f.getMZ(1), peakMZ)<0)
                    continue;

                for (int charge = maxCharge;charge >0 ; charge --) {
//                    if ((int)peakMZ == 158 && charge == 1 && f.name().contains("y1"))
//                        System.out.println("found y1");
//                    if ((int)peakMZ ==  317 && charge == 3 && f.name().contains("b2"))
//                        System.out.println("found b2");
//                    if ((int)peakMZ ==  465 && charge == 2 && f.name().contains("b2"))
//                        System.out.println("found b2");
//                    if ((int)peakMZ ==  474 && charge == 2 && f.name().contains("b2"))
//                        System.out.println("found b2");


                    double monoMZ = (neutralMass/charge) + Util.PROTON_MASS;
                    //double monoMZ = f.getMZ(charge);
                    double missingMZ = monoMZ + (Util.PROTON_MASS/charge);

                    if (tolerance.compare(peakMZ, monoMZ) == 0 ) {
                        // ok found a matching fragment for the peak
                        p.annotate(new SpectraPeakMatchedFragment(f, charge));
                        matchedFragments.add(f, charge, p);
                        p.setCharge(charge);
                        matched = true;
                        break;
                    } else if (tolerance.compare(peakMZ, missingMZ) == 0 ) {
                        // ok found a matching fragment for the peak
                        p.annotate(new SpectraPeakMatchedFragment(f, charge, monoMZ));
                        p.setCharge(charge);
                        matchedFragments.add(f, charge, p);
                        matched = true;
                        break;
                    }

                }

            }
            if (!matched) {
                unmatched.addPeak(p);
            }

        }


        return unmatched;
    }

    @Override
    public void matchFragmentsNonGreedy(Spectra s, ArrayList<Fragment> frags, ToleranceUnit tollerance, MatchedFragmentCollection matchedFragments) {
        matchFragments(s, frags, tollerance, matchedFragments);
    }


}
