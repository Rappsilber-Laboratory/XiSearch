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
package rappsilber.ms.spectra.match.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.ms.statistics.utils.UpdateableInteger;

/**
 * for each peak with more then one match - the fragment match with the highest
 * rank will get annotated as the primary match.
 * if there is only one match, this one get the primary.
 * of two matches have the same rank the match with the lower error get precedence
 * if two matches with same rank and same error the one belonging to the peptide
 * with the most matches becomes primary
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class DefinePrimaryFragmentMatches implements MatchFilter{

    @Override
    public void filter(MatchedXlinkedPeptide match) {
        MatchedFragmentCollection mfc = match.getMatchedFragments();
        MatchedFragmentCollection primaryFragments = new MatchedFragmentCollection(match.getSpectrum().getPrecurserCharge());
        MatchedFragmentCollection uniqueFragments = new MatchedFragmentCollection(match.getSpectrum().getPrecurserCharge());
        HashMap<Peptide, UpdateableInteger> peptideCount = new HashMap<Peptide, UpdateableInteger>();
        for (Peptide p :match.getPeptides()) {
            peptideCount.put(p, new UpdateableInteger(0));
        }


        for (SpectraPeak sp : match.getSpectrum()) {

            SpectraPeakMatchedFragment primaryAnnotation = null;
            double                     primarySupport = Double.NEGATIVE_INFINITY;
            double                     primaryError   = Double.POSITIVE_INFINITY;
            int                        primaryPeptideCount = 0;
            ArrayList<SpectraPeakMatchedFragment> mfs = sp.getMatchedAnnotation();
            if (mfs.size() > 1) {
                for (SpectraPeakMatchedFragment mf : sp.getMatchedAnnotation()) {
                    int charge = mf.getCharge();
                    Fragment f = mf.getFragment();
                    double supportLevel = f.getSupportLevel(mfc, charge);
                    if (mf.matchedMissing())
                        supportLevel/=10;
                    double error = Math.abs(f.getMZ(charge) - sp.getMZ());
                    int fPeptideCount = peptideCount.get(f.getPeptide()).value;

                    if (supportLevel > primarySupport) {
                        primaryAnnotation = mf;
                        primarySupport = supportLevel;
                        primaryError = error;
                        primaryPeptideCount = fPeptideCount;
                    } else if (supportLevel == primarySupport) {
                        if (error < primaryError) {
                            primaryAnnotation = mf;
                            primarySupport = supportLevel;
                            primaryError = error;
                            primaryPeptideCount = fPeptideCount;
                        } else if (error == primaryError && primaryPeptideCount < fPeptideCount) {
                            primaryAnnotation = mf;
                            primarySupport = supportLevel;
                            primaryError = error;
                            primaryPeptideCount = fPeptideCount;
                        }
                    }
                    mf.setSupportLevel(supportLevel);
                }
            }else if (mfs.size() == 1) {
                primaryAnnotation =mfs.get(0);
                uniqueFragments.add(mfs.get(0));
            }

            if (primaryAnnotation != null) {
                primaryAnnotation.setPrimary();
                primaryFragments.add(primaryAnnotation.getFragment(), primaryAnnotation.getCharge(), sp);
            } else if(sp.getMatchedAnnotation().size() > 0) {
                Logger.getLogger(this.getClass().getName()).log(Level.WARNING,"!!Error here!! " + this.getClass().getSimpleName() + "\n" + match.getSpectrum().getRun() + " " +  " -> " + match.getSpectrum().getScanNumber() + "\n peptides ");
                Logger.getLogger(this.getClass().getName()).log(Level.WARNING," " + match.getPeptide(0)  );
                if (match.getPeptide(1) != null)
                    Logger.getLogger(this.getClass().getName()).log(Level.WARNING," - " + match.getPeptide(1));
            }

        }
        match.setUniqueFragments(mfc);
        match.setPrimaryMatchedFragments(primaryFragments);
        if (mfc.size()>0 && primaryFragments.isEmpty()) {
                Logger.getLogger(this.getClass().getName()).log(Level.WARNING,"!!Error here!! no primary matches " + match.getSpectrum().getRun() + " " +  " -> " + match.getSpectrum().getScanNumber() + "\n peptides ");
                Logger.getLogger(this.getClass().getName()).log(Level.WARNING," " + match.getPeptide(0)  );
                if (match.getPeptide(1) != null)
                    Logger.getLogger(this.getClass().getName()).log(Level.WARNING," - " + match.getPeptide(0));
        }
        
    }


}
