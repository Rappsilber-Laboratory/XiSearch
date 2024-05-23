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
import java.util.TreeMap;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.SpectraPeakCluster;
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
public class DirectMatchFragmentsTreeLowRes implements Match{

    private void MFNG_Cluster(Collection<SpectraPeakCluster> clusters, TreeMap<Double, ArrayList<Fragment>> ftree, ToleranceUnit tolerance, MatchedFragmentCollection matchedFragments) {
        // first go through the peaks/peak clusters, match them to the first peptide
        // and store the none matched ones in a new spectra
        // first the cluster
        for (SpectraPeakCluster c : clusters) {
            SpectraPeak m = c.getMonoIsotopic();
//            if (! s.getPeaks().contains(m)) {
//                m.annotate(SpectraPeakAnnotation.virtual);
//                s.addPeak(m);
//                System.out.println("virtual peak");
//            }

            double cmz = c.getMZ();
            int cCharge = c.getCharge();
//            double monoMZ = cmz;


            double missingMZ = cmz - (Util.PROTON_MASS/cCharge);



            double monoNeutral = (cmz - Util.PROTON_MASS) * cCharge;
//            double missingNeutral = (missingMZ  - Util.PROTON_MASS) * cCharge;

            Collection<ArrayList<Fragment>> subSet = ftree.subMap(tolerance.getMinRange(monoNeutral), tolerance.getMaxRange(monoNeutral)).values();

            boolean matched = false;
            for (ArrayList<Fragment> af : subSet) {
                for (Fragment f : af) {
                    m.annotate(new SpectraPeakMatchedFragment(f, cCharge, c));
                    if (matchedFragments.hasMatchedFragment(f, cCharge)) {
                        SpectraPeak prevPeak = matchedFragments.getMatchedPeak(f, cCharge);
                        for (SpectraPeakMatchedFragment mf : prevPeak.getMatchedAnnotation()) {
                            if (mf.getFragment() == f) {
                                prevPeak.deleteAnnotation(mf);
                                break;
                            }
                        }
                        matchedFragments.remove(f, cCharge);
                    }
                    matchedFragments.add(f, cCharge, m);
                    matched = true;
                }
            }



        }
    }

    private void MFNG_Peak(Spectra s, TreeMap<Double, ArrayList<Fragment>> ftree, ToleranceUnit tolerance, MatchedFragmentCollection matchedFragments) {
        int maxCharge = s.getPrecurserCharge();
        // next single peaks
        for (SpectraPeak p : s.getPeaks()) {
            if (p.hasAnnotation(SpectraPeakAnnotation.isotop) || p.hasAnnotation(SpectraPeakAnnotation.monoisotop)) {
                continue;
            }


            double peakMZ = p.getMZ();




            boolean matched = false;

            for (int charge = maxCharge;charge >0 ; charge --) {

                double monoNeutral = (peakMZ - Util.PROTON_MASS) * charge;
//                double missingNeutral = monoNeutral  - Util.PROTON_MASS;
//                double missingMZ = missingNeutral/charge  + Util.PROTON_MASS;

                Collection<ArrayList<Fragment>> subSet = ftree.subMap(tolerance.getMinRange(monoNeutral), tolerance.getMaxRange(monoNeutral)).values();

                for (ArrayList<Fragment> af : subSet) {
                    for (Fragment f : af) {
                        if (matchedFragments.hasMatchedFragment(f, charge)) {
                            SpectraPeak prevPeak = matchedFragments.getMatchedPeak(f, charge);
                            for (SpectraPeakMatchedFragment mf : prevPeak.getMatchedAnnotation()) {
                                if (mf.getFragment() == f) {
                                    prevPeak.deleteAnnotation(mf);
                                    break;
                                }
                            }
                            matchedFragments.remove(f, charge);
                        }
                        p.annotate(new SpectraPeakMatchedFragment(f, charge));
                        matchedFragments.add(f, charge, p);
                        matched =true;
                    }
                }

            }



        }
    }

    private TreeMap<Double,ArrayList<Fragment>> makeTree(ArrayList<Fragment> frags) {
        TreeMap<Double,ArrayList<Fragment>> tree = new TreeMap<Double, ArrayList<Fragment>>();
        for (Fragment f : frags) {
            double mass = f.getNeutralMass();
            ArrayList fl = tree.get(mass);
            if (fl == null) {
                fl = new ArrayList<Fragment>(1);
                tree.put(mass, fl);
            }
            fl.add(f);
        }
        return tree;

    }

    public void matchFragmentsNonGreedy(Spectra s, ArrayList<Fragment> frags, ToleranceUnit tolerance, MatchedFragmentCollection matchedFragments) {

        TreeMap<Double,ArrayList<Fragment>> ftree = makeTree(frags);

        Collection<SpectraPeakCluster> clusters = s.getIsotopeClusters();
        MFNG_Cluster(clusters, ftree, tolerance, matchedFragments);
        MFNG_Peak(s, ftree, tolerance, matchedFragments);

        for (ArrayList<Fragment> v: ftree.values()) {
            v.clear();
        }
        ftree.clear();

    }

}
