/*
 * Copyright 2017 Lutz Fischer <lfischer@staffmail.ed.ac.uk>.
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
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedBaseFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class RemoveAnnotationFilter implements MatchFilter{

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
        if (!omfc.isEmpty()) {
            omfc.clear();

            for (SpectraPeak sp : match.getSpectrum()) {
                sp.clearAnnotations();
            }
        }
        


    }
    
}
