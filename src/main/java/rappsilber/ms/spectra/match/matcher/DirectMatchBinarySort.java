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
import java.util.Collections;
import java.util.Comparator;
import rappsilber.config.RunConfig;
import rappsilber.ms.Range;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.SpectraPeakCluster;
import rappsilber.ms.spectra.annotation.SpectraPeakAnnotation;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.utils.Util;

/**
 * Non-fancy matching of possible fragments against the spectra first all
 * detected isotope cluster gets checked, whether there m/z and charge state
 * result in a mass -
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class DirectMatchBinarySort implements Match {

    private boolean m_MatchMissingMonoIsotopic = true;
    private boolean m_TransferLossToBase = false;

    public DirectMatchBinarySort(RunConfig config) {
        m_MatchMissingMonoIsotopic = config.retrieveObject("MATCH_MISSING_MONOISOTOPIC", m_MatchMissingMonoIsotopic);
        m_TransferLossToBase = config.retrieveObject("TransferLossToBase", m_TransferLossToBase);
        // System.out.println("MatchMissingMonoIsotopic: " + m_MatchMissingMonoIsotopic);
    }

    private void MFNG_Cluster(Collection<SpectraPeakCluster> clusters, ArrayList<Fragment> frags, ToleranceUnit tolerance, MatchedFragmentCollection matchedFragments, Spectra s) {
        // first go through the peaks/peak clusters, match them to the first peptide
        // and store the none matched ones in a new spectra
        // first the cluster
        ArrayList<SpectraPeakCluster> sorted_clusters = new ArrayList<>(clusters);
        java.util.Collections.sort(sorted_clusters, new Comparator<SpectraPeakCluster>() {
            @Override
            public int compare(SpectraPeakCluster o1, SpectraPeakCluster o2) {
                return Double.compare(o2.getMass() - Util.PROTON_MASS*o2.getCharge(), 
                        o1.getMass()- Util.PROTON_MASS*o1.getCharge());
            }
        });
        int maxIndex = frags.size() - 1;
        int maxIndexMissingMono = frags.size() - 1;
        
        ArrayList<SpectraPeakCluster> added = new ArrayList<>();
        for (SpectraPeakCluster c : sorted_clusters) {
            SpectraPeak m = c.getMonoIsotopic();

            double cmz = c.getMZ();
            int cCharge = c.getCharge();

            double missingMZ = cmz - (Util.C13_MASS_DIFFERENCE / cCharge);

            double monoNeutral = (cmz - Util.PROTON_MASS) * cCharge;
            double massCharged = cmz * cCharge;
            double missingNeutral = monoNeutral - Util.C13_MASS_DIFFERENCE;
            double missingCharged = massCharged - Util.C13_MASS_DIFFERENCE;

            Range rMonoNeutral = tolerance.getRange(monoNeutral, massCharged);
            maxIndex = getMaxIndexForMass(frags, rMonoNeutral.max, maxIndex);
            int nextIndex = maxIndex;
            boolean matched = false;
            boolean matchedPrimary = false;
            
            Fragment f;
            while (nextIndex >=0 && (f = frags.get(nextIndex)).getNeutralMass()>=rMonoNeutral.min) {
                nextIndex--;
                m.annotate(new SpectraPeakMatchedFragment(f, cCharge, c));

                // was the same fragment with the same charge matched previously ?
                if (matchedFragments.hasMatchedFragment(f, cCharge)) {
                    // if yes it must have been an missing-peak so we should delete that
                    SpectraPeak prevPeak = matchedFragments.getMatchedPeak(f, cCharge);
                    for (SpectraPeakMatchedFragment mf : prevPeak.getMatchedAnnotation()) {
                        if (mf.getFragment() == f) {
                            prevPeak.deleteAnnotation(mf);
                            break;
                        }
                    }
                    matchedFragments.remove(f, cCharge);
                }
                if (f.isClass(Loss.class)) {
                    // if this is a lossy fragment and we have not matched the base frgment yet
                    if (m_TransferLossToBase && !matchedFragments.hasMatchedFragment(((Loss) f).getBaseFragment(), cCharge)) {
                        // we could try to recover the base fragment
                        Fragment bf = ((Loss) f).getBaseFragment();
                        Double mz = bf.getMZ(cCharge);
                        SpectraPeak basepeak = s.getPeakAt(mz, tolerance);
                        // try to detect a cluster from that point with the given charge
                        if (basepeak != null && basepeak.hasAnnotation(SpectraPeakAnnotation.isotop)) {
                            SpectraPeakCluster spc = new SpectraPeakCluster(tolerance);
                            spc.add(basepeak);
                            double diff = Util.C13_MASS_DIFFERENCE / cCharge;
                            int pc = 1;
                            SpectraPeak n = s.getPeakAt(mz + diff * pc++);
                            double intensity = 0;
                            // do we already go down with the itenasity
                            boolean down = false;
                            while (n != null) {
                                if (intensity * 0.95 > n.getIntensity()) {
                                    down = true;
                                } else if (down && intensity < n.getIntensity() * 0.95) {
                                    break;
                                }
                                spc.add(n);
                                intensity = n.getIntensity();
                                n = s.getPeakAt(mz + diff * pc++);
                            }
                            if (spc.size() > 1) {
                                spc.setCharge(cCharge);
                                added.add(spc);
                                basepeak.annotate(new SpectraPeakMatchedFragment(bf, cCharge, spc));
                                matchedFragments.add(bf, cCharge, basepeak);
                            }
                        }
                    }
                } else {
                    matchedPrimary = true;
                }
                
                matched = true;
                matchedFragments.add(f, cCharge, m);
            }
            // match missing monoisotpoic only if we don't have a primary explanation
            if (m_MatchMissingMonoIsotopic && !matchedPrimary && missingNeutral > 1000) {                
                if (matched) {
                    // we have a loss annotation without missing monoisotopic
                    Range r = tolerance.getRange(missingNeutral, missingCharged);
                    maxIndexMissingMono = getMaxIndexForMass(frags, r.max, maxIndex);
                    int nextIndexMM = maxIndexMissingMono;

                    while (nextIndexMM >=0 && (f = frags.get(nextIndexMM)).getNeutralMass()>=r.min) {
                        if (!matchedFragments.hasMatchedFragment(f, cCharge) && !f.isClass(Loss.class)) {
                            m.annotate(new SpectraPeakMatchedFragment(f, cCharge, missingMZ, c));
                            matchedFragments.add(f, cCharge, m);
                        }
                        nextIndexMM--;
                    }
                } else {
                    Range r = tolerance.getRange(missingNeutral, missingCharged);
                    maxIndexMissingMono = getMaxIndexForMass(frags, r.max, maxIndex);
                    int nextIndexMM = maxIndexMissingMono;

                    while (nextIndexMM >=0 && (f = frags.get(nextIndexMM)).getNeutralMass()>=r.min) {
                        if (!matchedFragments.hasMatchedFragment(f, cCharge)) {
                            m.annotate(new SpectraPeakMatchedFragment(f, cCharge, missingMZ, c));
                            matchedFragments.add(f, cCharge, m);
                        }
                        nextIndexMM--;
                    }
                }
            }
        }

        s.getIsotopeClusters().addAll(added);
    }

    private void MFNG_Peak(Spectra s, ArrayList<Fragment> frags, ToleranceUnit tolerance, MatchedFragmentCollection matchedFragments) {
        int maxCharge = s.getPrecurserCharge();
        int[] maxIndex = new int[maxCharge+1];
        int lastIndex = frags.size() - 1;
        for (int charge = maxCharge; charge > 0; charge--) {
            maxIndex[charge] = lastIndex;
        }
        
        // next single peaks
        ArrayList<SpectraPeak> sps  = new ArrayList<>(s.getPeaks());

        java.util.Collections.sort(sps, new Comparator<SpectraPeak>() {
            @Override
            public int compare(SpectraPeak o1, SpectraPeak o2) {
                return Double.compare(o2.getMZ(), o1.getMZ());
            }
        });
        
        int pc = 0;
        for (SpectraPeak p : sps) {
            pc++;
            if (p.hasAnnotation(SpectraPeakAnnotation.isotop) || p.hasAnnotation(SpectraPeakAnnotation.monoisotop)) {
                continue;
            }

            double peakMZ = p.getMZ();

            boolean matched = false;
            boolean matchedPrimary = false;
            
            for (int charge = maxCharge; charge > 0; charge--) {

                double monoNeutral = (peakMZ - Util.PROTON_MASS) * charge;
                double massCharged = peakMZ * charge;

                Range r = tolerance.getRange(monoNeutral, massCharged);
                Collection<ArrayList<Fragment>> subSet = null;
                maxIndex[charge] = getMaxIndexForMass(frags, r.max, maxIndex[charge]);
                int nextIndex = maxIndex[charge];
                
                Fragment f;
                while (nextIndex >=0 && (f = frags.get(nextIndex)).getNeutralMass()>=r.min) {
                    // if it has been mathed somewhere before
                    if (matchedFragments.hasMatchedFragment(f, charge)) {
                        // if yes it must have been an missing-peak so we should delete that
                        SpectraPeak prevPeak = matchedFragments.getMatchedPeak(f, charge);
                        for (SpectraPeakMatchedFragment mf : (ArrayList<SpectraPeakMatchedFragment>) prevPeak.getMatchedAnnotation().clone()) {
                            if (mf.getFragment() == f) {
                                prevPeak.deleteAnnotation(mf);
                            }
                        }
                        matchedFragments.remove(f, charge);
                    }
                    p.annotate(new SpectraPeakMatchedFragment(f, charge));
                    matchedFragments.add(f, charge, p);
                    if (!f.isClass(Loss.class)) {
                        matchedPrimary = true;
                    }
                    matched = true;
                    nextIndex--;
                }

            }
            if (m_MatchMissingMonoIsotopic && !matchedPrimary) {                
                if (!matched) {
                    for (int charge = maxCharge; charge > 0; charge--) {

                        double monoNeutral = (peakMZ - Util.PROTON_MASS) * charge;
                        if (monoNeutral > 1000) {
                            double missingNeutral = monoNeutral - Util.C13_MASS_DIFFERENCE;
                            double missingMZ = missingNeutral / charge + Util.PROTON_MASS;
                            double missingCharged = missingMZ * charge;

                            Range r = tolerance.getRange(missingNeutral, missingCharged);
                            int maxIndexMissingMono = getMaxIndexForMass(frags, r.max, maxIndex[charge]);
                            int nextIndexMM = maxIndexMissingMono;

                            Fragment f;
                            while (nextIndexMM >=0 && (f = frags.get(nextIndexMM)).getNeutralMass()>=r.min) {
                                if (!matchedFragments.hasMatchedFragment(f, charge)) {
                                    p.annotate(new SpectraPeakMatchedFragment(f, charge, missingMZ));
                                    matchedFragments.add(f, charge, p);
                                }
                                nextIndexMM--;
                            }
                        }
                    }
                } else {
                    for (int charge = maxCharge; charge > 0; charge--) {

                        double monoNeutral = (peakMZ - Util.PROTON_MASS) * charge;
                        if (monoNeutral > 2000) {
                            double missingNeutral = monoNeutral - Util.C13_MASS_DIFFERENCE;
                            double missingMZ = missingNeutral / charge + Util.PROTON_MASS;
                            double missingCharged = missingMZ * charge;

                            Range r = tolerance.getRange(missingNeutral, missingCharged);
                            int maxIndexMissingMono = getMaxIndexForMass(frags, r.max, maxIndex[charge]);
                            int nextIndexMM = maxIndexMissingMono;

                            Fragment f;
                            while (nextIndexMM >=0 && (f = frags.get(nextIndexMM)).getNeutralMass()>=r.min) {
                                if (!matchedFragments.hasMatchedFragment(f, charge) && !f.isClass(Loss.class)) {
                                    p.annotate(new SpectraPeakMatchedFragment(f, charge, missingMZ));
                                    matchedFragments.add(f, charge, p);
                                }
                                nextIndexMM--;
                            }
                        }
                    }
                    
                }
            }

        }
    }


    // returns the index of the fist fragment that has a larger mass then the requested
    int getMaxIndexForMass(ArrayList<Fragment> frags, double maxMass, int lastMaxIndex) {
        int maxIndex = lastMaxIndex;
        int first = 0;
        int last = frags.size()-1;

        while (maxIndex >= first) {
            int mid = first + (maxIndex-first)/2;
            double fmass = frags.get(mid).getNeutralMass();
            if (fmass > maxMass) {
               maxIndex = mid-1;
            } else {
               first = mid+1;
           }
        }
        return maxIndex;
    }

    
    public void matchFragmentsNonGreedy(Spectra s, ArrayList<Fragment> frags, ToleranceUnit tolerance, MatchedFragmentCollection matchedFragments) {

        Collections.sort(frags, new Comparator<Fragment>() {
            @Override
            public int compare(Fragment t, Fragment t1) {
                return Double.compare(t.getNeutralMass(), t1.getNeutralMass());
            }
        });

        Collection<SpectraPeakCluster> clusters = s.getIsotopeClusters();
        MFNG_Cluster(clusters, frags, tolerance, matchedFragments,s);
        MFNG_Peak(s, frags, tolerance, matchedFragments);

    }
    
}
