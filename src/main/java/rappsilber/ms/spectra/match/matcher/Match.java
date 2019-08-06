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
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeakCluster;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public interface Match {

    /**
     * tries to match the peaks of the Spectra to the peptide-fragments - each matched peak gets annotated
     * Does not return a list of unmatched peaks
     * @param s     the spectra
     * @param frags the list of possibly matching fragments
     * @param crosslinkedMass what is the mass of the crosslinked peptide + crosslinker
     * @param tollerance mass tolerance
     * @param matchedFragments
     */
    void matchFragmentsNonGreedy(Spectra s, ArrayList<Fragment> frags, ToleranceUnit tollerance, MatchedFragmentCollection matchedFragments);

}
