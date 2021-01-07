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
import java.util.Comparator;
import java.util.HashMap;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedBaseFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 * This class is meant to delete all supposedly meaningless annotations.
 * E.g. a total loss count of 10 on one fragment, if no lower number of
 * losses on the same fragment where observed
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class CleanUpMatchedPeaksFilter implements MatchFilter{

    private static int MAX_LOSS_DISTANCE = 2;

    /**
     * 
     * @param match
     */
    @Override
    /**
     * go through all peak annotations, and delete unbeliveable ones
     */
    public void filter(MatchedXlinkedPeptide match) {
        
        final MatchedFragmentCollection omfc = match.getMatchedFragments();

        // find all "obviously" false annotations
        HashMap<SpectraPeakMatchedFragment, SpectraPeak> toDelete = new HashMap<SpectraPeakMatchedFragment, SpectraPeak>();
        for (SpectraPeak sp : match.getSpectrum()) {


            HashMap<SpectraPeakMatchedFragment, SpectraPeak> conditionalDelete = new HashMap<SpectraPeakMatchedFragment, SpectraPeak>();
            ArrayList<SpectraPeakMatchedFragment> annotations =  sp.getMatchedAnnotation();
            int ca = annotations.size();
            for (SpectraPeakMatchedFragment mf : annotations) {
                Fragment f = mf.getFragment();
                int charge = mf.getCharge();
                MatchedBaseFragment mbf = omfc.getMatchedFragmentGroup(f, charge);
                // currently IO only delete fragment matches to losses, that have unreasonable high numbers of losses without having enough other
                // loss matches, to support the possibility
                // all just made up/ guesswork - no real knowledge behind it
                if (f.isClass(Loss.class)) {
                    // don't delete precoursor losses but anything else, that has not enough support
                    if (((Loss)f).getFragmentationSites().length != 0 &&  // no precursor loss
                            ((Loss)f).getTotalLossCount()> MAX_LOSS_DISTANCE &&  
                            mbf.getLosses().size() <= MAX_LOSS_DISTANCE) {
                        toDelete.put(mf,sp);
                    }
                    
                } else if (mf.matchedMissing() && (ca>toDelete.size() + 1))
                    conditionalDelete.put(mf,sp);
            }

            // if there is an "bad" explanation (conditionalDelete) and a good explanation, delte the bad one
            if (conditionalDelete.size()>0 && toDelete.size() + conditionalDelete.size() < ca) {
                toDelete.putAll(conditionalDelete);
            }

            
        }

        // and delete them
        for (SpectraPeakMatchedFragment spmf : toDelete.keySet()) {
            SpectraPeak sp = toDelete.get(spmf);
            omfc.remove(spmf.getFragment(), spmf.getCharge());
            sp.deleteAnnotation(spmf);
        }


    }

}
