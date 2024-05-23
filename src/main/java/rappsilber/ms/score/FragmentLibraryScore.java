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
package rappsilber.ms.score;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import rappsilber.ms.lookup.fragments.FragmentLookup;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.ions.CrosslinkedFragment;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedBaseFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class FragmentLibraryScore extends AbstractScoreSpectraMatch {
    private FragmentLookup m_FragmentLookup;
    private double           m_countPeptides;
    public static final String Score = "FragmentLibraryScore";
    //public static final String AdaptedScore = "FragmentLibraryScoreMod";
    public static final String ExpScore = "FragmentLibraryScoreExponential";
    public static final String LogScore = "FragmentLibraryScoreLog";
    private final static double factor = Math.exp(100);


    public FragmentLibraryScore(FragmentLookup fl, long peptides) {
        m_FragmentLookup  = fl;
        m_countPeptides = peptides;
    }


    public double score(MatchedXlinkedPeptide match) {

        //MatchedFragmentCollection allMatches = new MatchedFragmentCollection();
        double matchScore = 1;
//        double matchedPeaks = 0;
//        double precMass = match.getSpectrum().getPrecurserMass();
        TreeSet<Double> ps = new TreeSet<Double>();
        MatchedFragmentCollection mfc = match.getMatchedFragments();
        HashMap<SpectraPeak,SpectraPeakMatchedFragment> primaryMatches = match.getSpectrum().getPrimaryMatches(mfc);

        for (Map.Entry<SpectraPeak,SpectraPeakMatchedFragment> e : primaryMatches.entrySet()) {
            SpectraPeakMatchedFragment spmf = e.getValue();
            MatchedBaseFragment mbf = mfc.getMatchedFragmentGroup(spmf.getFragment(), spmf.getCharge());
            if (mbf.isBaseFragmentFound() && mbf.getBaseFragment().getFragmentationSites().length == 1) {
//                ArrayList<Peptide> matchedPeptides = m_FragmentLookup.getForMass(mbf.getBaseFragment().getMZ(mbf.getCharge()));
                Fragment f = mbf.getBaseFragment();
                double peakScore;
                int charge = spmf.getCharge();
                double scoreMass = f.getMass();

                if (f.isClass(CrosslinkedFragment.class)) {
                    double precCalcMass = 0;

                    for (Peptide p : match.getPeptides()) {
                        precCalcMass += p.getMass();
                    }

                    if (match.getCrosslinker() != null) {
                        precCalcMass += match.getCrosslinker().getCrossLinkedMass();
                    }

                    double linMass = precCalcMass - f.getNeutralMass() + Util.PROTON_MASS;
                    scoreMass = linMass;
//                    peakScore = m_FragmentLookup.countPeptides(precMass - mbf.getBaseFragment().getNeutralMass() + Util.PROTON_MASS, precMass)/m_countPeptides;

                } //else
                //    peakScore = m_FragmentLookup.countPeptides(mbf.getBaseFragment().getMZ(1))/m_countPeptides;
                peakScore = m_FragmentLookup.countPeptides(scoreMass)/m_countPeptides;
//                if (peakScore == 0)
//                    System.err.println("found it " + this.getClass().getName());
                ps.add(peakScore);
            }
        }

//        matchScore = -Math.log(matchScore);
        //matchScore = 1/(matchScore * matchScore + 0.01);
        Iterator<Double> it = ps.iterator();
        for (int i =0; i<50 && it.hasNext(); i++) {
            matchScore *= it.next();
        }

        addScore(match, Score, 1.0 - matchScore);
        //addScore(match, AdaptedScore, matchScore*matchScore);
        addScore(match, ExpScore, Math.exp(-1000*matchScore));
        addScore(match, LogScore, -Math.log(matchScore));
//        addScore(match, AdaptedScore, Math.atan(Math.sqrt(Math.sqrt((-Math.log(matchScore)))))/Math.PI*2);

        // return the average
        return matchScore*matchScore;

    }


    public double getOrder() {
        return 20;
    }

    public String[] scoreNames() {
        return new String[]{Score,ExpScore,LogScore};
    }


}
