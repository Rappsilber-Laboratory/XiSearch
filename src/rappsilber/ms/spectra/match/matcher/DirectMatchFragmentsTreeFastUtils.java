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

import it.unimi.dsi.fastutil.doubles.Double2ObjectRBTreeMap;
import java.util.ArrayList;
import java.util.Collection;
import rappsilber.config.RunConfig;
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
 * state result in a mass - 
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class DirectMatchFragmentsTreeFastUtils implements Match{
    
    private boolean m_MatchMissingMonoIsotopic = true;

    public DirectMatchFragmentsTreeFastUtils(RunConfig config) {
        m_MatchMissingMonoIsotopic = config.retrieveObject("MATCH_MISSING_MONOISOTOPIC", m_MatchMissingMonoIsotopic);
       // System.out.println("MatchMissingMonoIsotopic: " + m_MatchMissingMonoIsotopic);
    }
    
    
    

    private void MFNG_Cluster_Match_Missing_Monoisotopic(Collection<SpectraPeakCluster> clusters, Double2ObjectRBTreeMap<ArrayList<Fragment>> ftree, ToleranceUnit tolerance, MatchedFragmentCollection matchedFragments) {
        // first go through the peaks/peak clusters, match them to the first peptide
        // and store the none matched ones in a new spectra
        // first the cluster
        for (SpectraPeakCluster c : clusters) {
            SpectraPeak m = c.getMonoIsotopic();

            double cmz = c.getMZ();
            int cCharge = c.getCharge();


            double monoNeutral = (cmz - Util.PROTON_MASS) * cCharge;

            Collection<ArrayList<Fragment>> subSet = ftree.subMap(tolerance.getMinRange(monoNeutral), tolerance.getMaxRange(monoNeutral)).values();

            boolean matched = false;
            for (ArrayList<Fragment> af : subSet) {
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
                        matchedFragments.add(f, cCharge, m);
                        matched = true;
                }
            }
            
            if (!matched) {
                double missingMZ = cmz - (Util.PROTON_MASS/cCharge);
                double missingNeutral = (missingMZ  - Util.PROTON_MASS) * cCharge;
                subSet = ftree.subMap(tolerance.getMinRange(missingNeutral), tolerance.getMaxRange(missingNeutral)).values();

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
    }


    private void MFNG_Cluster(Collection<SpectraPeakCluster> clusters, Double2ObjectRBTreeMap<ArrayList<Fragment>> ftree, ToleranceUnit tolerance, MatchedFragmentCollection matchedFragments) {
        // first go through the peaks/peak clusters, match them to the first peptide
        // and store the none matched ones in a new spectra
        // first the cluster
        for (SpectraPeakCluster c : clusters) {
            SpectraPeak m = c.getMonoIsotopic();

            double cmz = c.getMZ();
            int cCharge = c.getCharge();


            double monoNeutral = (cmz - Util.PROTON_MASS) * cCharge;

            Collection<ArrayList<Fragment>> subSet = ftree.subMap(tolerance.getMinRange(monoNeutral), tolerance.getMaxRange(monoNeutral)).values();

            for (ArrayList<Fragment> af : subSet) {
                for (Fragment f : af) {
                        m.annotate(new SpectraPeakMatchedFragment(f, cCharge, c));

                        matchedFragments.add(f, cCharge, m);
                }
            }
        }
    }

    
    private void MFNG_Peak_Match_Missing_Monoisotopic(Spectra s, Double2ObjectRBTreeMap<ArrayList<Fragment>> ftree, ToleranceUnit tolerance, MatchedFragmentCollection matchedFragments) {
        int maxCharge = s.getPrecurserCharge();
        // next single peaks
        for (SpectraPeak p : s.getPeaks()) {
            if (p.hasAnnotation(SpectraPeakAnnotation.isotop) || p.hasAnnotation(SpectraPeakAnnotation.monoisotop))
                continue;

            double peakMZ = p.getMZ();

            boolean matched = false;
            // peaks get potentially annotated with all charge states
            for (int charge = maxCharge;charge >0 ; charge --) {
                
                double monoNeutral = (peakMZ - Util.PROTON_MASS) * charge;
                
                double minMass  = tolerance.getMinRange(monoNeutral);
                double maxMass  = tolerance.getMaxRange(monoNeutral);
                // get a list of fragments matching to this peak in this charge state
                Collection<ArrayList<Fragment>> subSet = ftree.subMap(minMass, maxMass).values();
                // annotate the peak with all fragment
                for (ArrayList<Fragment> af : subSet) {
                    for (Fragment f : af) {
                            // if it has been matched somewhere before
                            
                            if (matchedFragments.hasMatchedFragment(f, charge)) {
                                // if yes it must have been an missing-peak so we should delete that
                                SpectraPeak prevPeak = matchedFragments.getMatchedPeak(f, charge);
                                for (SpectraPeakMatchedFragment mf : prevPeak.getMatchedAnnotation())
                                    if (mf.getFragment() == f) {
                                        prevPeak.deleteAnnotation(mf);
                                        break;
                                    }
                                matchedFragments.remove(f, charge);
                            }
                            // do the actuall annotation
                            p.annotate(new SpectraPeakMatchedFragment(f, charge));
                            matchedFragments.add(f, charge, p);
                            matched =true;
                    }
                }
            }
            
            // if we are supposed to also annotate fragments with missing monoisotopic masses
            // do that now - but only if that peak was not matched already
            if (!matched) {
                for (int charge = maxCharge;charge >0 ; charge --) {

                    double missingNeutral = (peakMZ - Util.PROTON_MASS) * charge  - Util.C13_MASS_DIFFERENCE;
                    double missingMZ = missingNeutral/charge  + Util.PROTON_MASS;

                    Collection<ArrayList<Fragment>> subSet = ftree.subMap(tolerance.getMinRange(missingNeutral), tolerance.getMaxRange(missingNeutral)).values();

                    for (ArrayList<Fragment> af : subSet)
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

    private void MFNG_Peak(Spectra s, Double2ObjectRBTreeMap<ArrayList<Fragment>> ftree, ToleranceUnit tolerance, MatchedFragmentCollection matchedFragments) {
        int maxCharge = s.getPrecurserCharge();
        // next single peaks
        for (SpectraPeak p : s.getPeaks()) {
            if (p.hasAnnotation(SpectraPeakAnnotation.isotop) || p.hasAnnotation(SpectraPeakAnnotation.monoisotop))
                continue;

            double peakMZ = p.getMZ();

            // peaks get potentially annotated with all charge states
            for (int charge = maxCharge;charge >0 ; charge --) {
                
                double monoNeutral = (peakMZ - Util.PROTON_MASS) * charge;
                
                double minMass  = tolerance.getMinRange(monoNeutral);
                double maxMass  = tolerance.getMaxRange(monoNeutral);
                // get a list of fragments matching to this peak in this charge state
                Collection<ArrayList<Fragment>> subSet = ftree.subMap(minMass, maxMass).values();
                // annotate the peak with all fragment
                for (ArrayList<Fragment> af : subSet) {
                    for (Fragment f : af) {
                        // do the actuall annotation
                        p.annotate(new SpectraPeakMatchedFragment(f, charge));
                        matchedFragments.add(f, charge, p);
                    }
                }
            }
        }  
    }
    
    /**
     * Creates a lookup tree for masses to fragments.
     * This is used to speed up the lookup of peaks to explaining fragments
     * @param frags
     * @return 
     */
    private Double2ObjectRBTreeMap<ArrayList<Fragment>> makeTree(ArrayList<Fragment> frags) {
        Double2ObjectRBTreeMap<ArrayList<Fragment>> tree = new Double2ObjectRBTreeMap<ArrayList<Fragment>>();
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

    @Override
    public Spectra matchFragments(Spectra s, ArrayList<Fragment> frags, ToleranceUnit tolerance, MatchedFragmentCollection matchedFragments) {

        Double2ObjectRBTreeMap<ArrayList<Fragment>> ftree = makeTree(frags);

        Spectra unmatched = matchIsotopeCluster(s, ftree, tolerance, matchedFragments);

        matchSinglePeaks(s, ftree, tolerance, matchedFragments, unmatched);
        
        for (ArrayList<Fragment> v: ftree.values()) {
            v.clear();
        }
        ftree.clear();

        return unmatched;
    }

    /**
     * match all fragments against all peaks not part of an isotope cluster.
     * @param s the spectrum to match
     * @param ftree a tree representation of masses to all fragments
     * @param tolerance the tollerance used for matching
     * @param matchedFragments collection that will hold all matched fragments
     * @param unmatched spectrum with all unmatched peaks - used for greedy matching
     */
    protected void matchSinglePeaks(Spectra s, Double2ObjectRBTreeMap<ArrayList<Fragment>> ftree, ToleranceUnit tolerance, MatchedFragmentCollection matchedFragments, Spectra unmatched) {
        int maxCharge = s.getPrecurserCharge();
        // next single peaks
        for (SpectraPeak p : s.getPeaks()) {
            if (p.hasAnnotation(SpectraPeakAnnotation.isotop) || p.hasAnnotation(SpectraPeakAnnotation.monoisotop))
                continue;

            boolean matched = false;
            double peakMZ = p.getMZ();

            for (int charge = maxCharge;charge >0 ; charge --) {

                double monoNeutral = (peakMZ - Util.PROTON_MASS) * charge;
                double missingNeutral = monoNeutral  - Util.PROTON_MASS;
                double missingMZ = missingNeutral/charge  + Util.PROTON_MASS;

                Collection<ArrayList<Fragment>> subSet = ftree.subMap(tolerance.getMinRange(monoNeutral), tolerance.getMaxRange(monoNeutral)).values();

                for (ArrayList<Fragment> af : subSet)
                    for (Fragment f : af) {
                        p.annotate(new SpectraPeakMatchedFragment(f, charge));
                        matchedFragments.add(f, charge, p);
                        matched = true;
                    }

                subSet = ftree.subMap(tolerance.getMinRange(missingNeutral), tolerance.getMaxRange(missingNeutral)).values();

                for (ArrayList<Fragment> af : subSet)
                    for (Fragment f : af) {
                        p.annotate(new SpectraPeakMatchedFragment(f, charge, missingMZ));
                        matchedFragments.add(f, charge, p);
                        matched = true;
                    }
            }

            if (!matched) {
                //unmatched.getIsotopClusters().add(c);
                unmatched.addPeak(p);
            }
        }
    }

    /**
     * match all fragments against all isotope clusters.
     * @param s the spectrum to match
     * @param ftree a tree representation of masses to all fragments
     * @param tolerance the tollerance used for matching
     * @param matchedFragments collection that will hold all matched fragments
     * @return a spectra conttaining all isotope cluster not yet explained
     * 
     */
    protected Spectra matchIsotopeCluster(Spectra s, Double2ObjectRBTreeMap<ArrayList<Fragment>> ftree, ToleranceUnit tolerance, MatchedFragmentCollection matchedFragments) {
        Collection<SpectraPeakCluster> clusters = s.getIsotopeClusters();
        Spectra unmatched = s.cloneEmpty();
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

            boolean matched = false;
            double cmz = c.getMZ();
            int cCharge = c.getCharge();
//            double monoMZ = cmz;

            double missingMZ = cmz - (Util.PROTON_MASS / cCharge);

            double monoNeutral = (cmz - Util.PROTON_MASS) * cCharge;
            double missingNeutral = (missingMZ - Util.PROTON_MASS) * cCharge;

            Collection<ArrayList<Fragment>> subSet = ftree.subMap(tolerance.getMinRange(monoNeutral), tolerance.getMaxRange(monoNeutral)).values();

            for (ArrayList<Fragment> af : subSet) {
                for (Fragment f : af) {
                    m.annotate(new SpectraPeakMatchedFragment(f, cCharge, c));
                    matchedFragments.add(f, cCharge, m);
                    matched = true;
                }
            }

            subSet = ftree.subMap(tolerance.getMinRange(missingNeutral), tolerance.getMaxRange(missingNeutral)).values();

            for (ArrayList<Fragment> af : subSet) {
                for (Fragment f : af) {
                    m.annotate(new SpectraPeakMatchedFragment(f, cCharge, missingMZ, c));
                    matchedFragments.add(f, cCharge, m);
                    matched = true;
                }
            }

            if (!matched) {
                unmatched.getIsotopeClusters().add(c);
                unmatched.addPeak(c.getMonoIsotopic());
            }

        }
        return unmatched;
    }


    @Override
    public void matchFragmentsNonGreedy(Spectra s, ArrayList<Fragment> frags, ToleranceUnit tolerance, MatchedFragmentCollection matchedFragments) {

        Double2ObjectRBTreeMap<ArrayList<Fragment>> ftree = makeTree(frags);

        Collection<SpectraPeakCluster> clusters = s.getIsotopeClusters();
        if (m_MatchMissingMonoIsotopic) {
            MFNG_Cluster_Match_Missing_Monoisotopic(clusters, ftree, tolerance, matchedFragments);
            MFNG_Peak_Match_Missing_Monoisotopic(s, ftree, tolerance, matchedFragments);
        } else {
            MFNG_Cluster(clusters, ftree, tolerance, matchedFragments);
            MFNG_Peak(s, ftree, tolerance, matchedFragments);
        }
        
        for (ArrayList<Fragment> v: ftree.values()) {
            v.clear();
        }
        ftree.clear();

    }

}
