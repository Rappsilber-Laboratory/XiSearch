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
import java.util.Comparator;
import java.util.TreeMap;
import rappsilber.config.RunConfig;
import rappsilber.ms.Range;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.PeptideIon;
import rappsilber.ms.sequence.ions.loss.CrosslinkerModified;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeakCluster;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.annotation.SpectraPeakAnnotation;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedBaseFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.utils.Util;

/**
 * Non-fancy matching of possible fragments against the spectra
 * first all detected isotope cluster gets checked, whether there m/z and charge
 * state result in a mass - 
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class DirectMatchFragmentsTree implements Match{
    
    private boolean m_MatchMissingMonoIsotopic = true;
    private boolean m_TransferLossToBase = false;

    public DirectMatchFragmentsTree(RunConfig config) {
        m_MatchMissingMonoIsotopic = config.retrieveObject("MATCH_MISSING_MONOISOTOPIC", m_MatchMissingMonoIsotopic);
        m_TransferLossToBase = config.retrieveObject("TransferLossToBase", m_TransferLossToBase);
       // System.out.println("MatchMissingMonoIsotopic: " + m_MatchMissingMonoIsotopic);
    }
    
    
    

    private void MFNG_Cluster(Collection<SpectraPeakCluster> clusters, TreeMap<Double, ArrayList<Fragment>> ftree, ToleranceUnit tolerance, MatchedFragmentCollection matchedFragments, Spectra s) {
        // first go through the peaks/peak clusters, match them to the first peptide
        // and store the none matched ones in a new spectra
        // first the cluster
        ArrayList<SpectraPeakCluster> sorted_clusters = new ArrayList<>(clusters);
        java.util.Collections.sort(sorted_clusters, new Comparator<SpectraPeakCluster>() {
            @Override
            public int compare(SpectraPeakCluster o1, SpectraPeakCluster o2) {
                return Double.compare(o2.getMZ(), o1.getMZ());
            }
        });
        
        ArrayList<SpectraPeakCluster> added = new ArrayList<>();
        for (SpectraPeakCluster c : sorted_clusters) {
            SpectraPeak m = c.getMonoIsotopic();

            double cmz = c.getMZ();
            int cCharge = c.getCharge();


            double missingMZ = cmz - (Util.C13_MASS_DIFFERENCE/cCharge);



            double monoNeutral = (cmz - Util.PROTON_MASS) * cCharge;
            double massCharged = cmz * cCharge;
            double missingNeutral = monoNeutral - Util.C13_MASS_DIFFERENCE;
            double missingCharged = massCharged - Util.C13_MASS_DIFFERENCE;
            
            Range rMonoNeutral = tolerance.getRange(monoNeutral, massCharged);
            Collection<ArrayList<Fragment>> subSet = ftree.subMap(rMonoNeutral.min,rMonoNeutral.max).values();

            boolean matched = false;
            for (ArrayList<Fragment> af : subSet)
                for (Fragment f : af) {
                        m.annotate(new SpectraPeakMatchedFragment(f, cCharge, c));

                        // was the same fragment with the same charge matched previously ?
                        if (matchedFragments.hasMatchedFragment(f, cCharge)) { 
                            // if yes it must have been an missing-peak so we should delete that
                            SpectraPeak prevPeak = matchedFragments.getMatchedPeak(f, cCharge);
                            for (SpectraPeakMatchedFragment mf : prevPeak.getMatchedAnnotation())
                                if (mf.getFragment() == f) {
                                    prevPeak.deleteAnnotation(mf);
                                    break;
                                }
                            matchedFragments.remove(f, cCharge);
                        }
                        // if this is a lossy fragment and we have not matched the base frgment yet
                        if (m_TransferLossToBase && f.isClass(Loss.class) && !matchedFragments.hasMatchedFragment(((Loss)f).getBaseFragment(), cCharge)) { 
                            // we could try to recover the base fragment
                            Fragment bf=((Loss)f).getBaseFragment();
                            Double mz = bf.getMZ(cCharge);
                            SpectraPeak basepeak = s.getPeakAt(mz, tolerance);
                            // try to detect a cluster from that point with the given charge
                            if (basepeak != null && basepeak.hasAnnotation(SpectraPeakAnnotation.isotop)) {
                                SpectraPeakCluster spc = new SpectraPeakCluster(tolerance);
                                spc.add(basepeak);
                                double diff= Util.C13_MASS_DIFFERENCE/cCharge;
                                int pc = 1;
                                SpectraPeak n = s.getPeakAt(mz+diff*pc++);
                                double intensity = 0;
                                // do we already go down with the itenasity
                                boolean down=false;
                                while (n!= null) {
                                    if (intensity*0.95>n.getIntensity()) {
                                        down=true;
                                    } else if (down && intensity<n.getIntensity()*0.95) {
                                        break;
                                    }
                                    spc.add(n);
                                    intensity=n.getIntensity();
                                    n = s.getPeakAt(mz+diff*pc++);
                                }
                                if (spc.size()>1) {
                                    spc.setCharge(cCharge);
                                    added.add(spc);
                                    basepeak.annotate(new SpectraPeakMatchedFragment(bf, cCharge, spc));
                                    matchedFragments.add(bf, cCharge, basepeak);
                                }
                            }
                        }
                        matchedFragments.add(f, cCharge, m);
                        matched = true;
                }

            if (m_MatchMissingMonoIsotopic && !matched && missingNeutral >1000) {
                Range r = tolerance.getRange(missingNeutral, missingCharged);
                subSet = ftree.subMap(r.min,r.max).values();

                // if something was matched
                for (ArrayList<Fragment> af : subSet)
                    for (Fragment f : af) {
                        if (!matchedFragments.hasMatchedFragment(f, cCharge)) {                            
                            m.annotate(new SpectraPeakMatchedFragment(f, cCharge, missingMZ, c));
                            matchedFragments.add(f, cCharge, m);
                        }
                    }
            }
        }

        s.getIsotopeClusters().addAll(added);
    }

    private void MFNG_Peak(Spectra s, TreeMap<Double, ArrayList<Fragment>> ftree, ToleranceUnit tolerance, MatchedFragmentCollection matchedFragments) {
        int maxCharge = s.getPrecurserCharge();
        // next single peaks
        for (SpectraPeak p : s.getPeaks()) {
            if (p.hasAnnotation(SpectraPeakAnnotation.isotop) || p.hasAnnotation(SpectraPeakAnnotation.monoisotop))
                continue;


            double peakMZ = p.getMZ();




            boolean matched = false;

            for (int charge = maxCharge;charge >0 ; charge --) {

//                double monoNeutral = (peakMZ - Util.PROTON_MASS) * charge;
//                double missingNeutral = monoNeutral  - Util.PROTON_MASS;
//                double missingMZ = missingNeutral/charge  + Util.PROTON_MASS;

                double monoNeutral = (peakMZ - Util.PROTON_MASS) * charge;
                double massCharged = peakMZ * charge;
                
                
                Range r = tolerance.getRange(monoNeutral, massCharged);
//                double minMass  = tolerance.getMinRange(monoNeutral);
//                double maxMass  = tolerance.getMaxRange(monoNeutral);
                Collection<ArrayList<Fragment>> subSet = null;
                subSet = ftree.subMap(r.min, r.max).values();
                for (ArrayList<Fragment> af : subSet)
                    for (Fragment f : af) {
                            // if it has been mathed somewhere before
                            if (matchedFragments.hasMatchedFragment(f, charge)) {
                                // if yes it must have been an missing-peak so we should delete that
                                SpectraPeak prevPeak = matchedFragments.getMatchedPeak(f, charge);
                                for (SpectraPeakMatchedFragment mf : (ArrayList<SpectraPeakMatchedFragment>)prevPeak.getMatchedAnnotation().clone())
                                    if (mf.getFragment() == f) {
                                        prevPeak.deleteAnnotation(mf);
                                    }
                                matchedFragments.remove(f, charge);
                            }
                            p.annotate(new SpectraPeakMatchedFragment(f, charge));
//                            if (f instanceof CrosslinkerModified) {
//                                System.out.println("found it" + f.name());
//                            }
//                            if (f instanceof CrosslinkerModified.CrosslinkerModifiedRest) {
//                                System.out.println("found it" + f.name());
//                            }
                            matchedFragments.add(f, charge, p);
                            matched =true;
                    }

            }
            if (m_MatchMissingMonoIsotopic && !matched) {
                for (int charge = maxCharge;charge >0 ; charge --) {
                    
                    double monoNeutral = (peakMZ - Util.PROTON_MASS) * charge;
                    if (monoNeutral > 1000) {
                        double missingNeutral = monoNeutral  - Util.C13_MASS_DIFFERENCE;
                        double missingMZ = missingNeutral/charge  + Util.PROTON_MASS;
                        double missingCharged = missingMZ * charge;
                        
                        Range r = tolerance.getRange(missingNeutral, missingCharged);
                        Collection<ArrayList<Fragment>> subSet = ftree.subMap(r.min,r.max).values();

                        for (ArrayList<Fragment> af : subSet) {
                            for (Fragment f : af) {
                                if (!matchedFragments.hasMatchedFragment(f, charge)) {
                                    p.annotate(new SpectraPeakMatchedFragment(f, charge, missingMZ));
                                    matchedFragments.add(f, charge, p);
                                }
                            }
                        }
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
        MFNG_Cluster(clusters, ftree, tolerance, matchedFragments,s);
        MFNG_Peak(s, ftree, tolerance, matchedFragments);

        for (ArrayList<Fragment> v: ftree.values()) {
            v.clear();
        }
        ftree.clear();

    }

}
