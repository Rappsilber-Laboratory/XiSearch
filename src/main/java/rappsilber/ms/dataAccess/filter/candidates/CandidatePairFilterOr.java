/*
 * Copyright 2018 Lutz Fischer <lfischer@staffmail.ed.ac.uk>.
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
package rappsilber.ms.dataAccess.filter.candidates;

import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.spectra.Spectra;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class CandidatePairFilterOr implements CandidatePairFilter{
    CandidateFilter inner;

    public CandidatePairFilterOr(CandidateFilter f) {
        inner = f;
    }

    @Override
    public boolean passes(Spectra s, CrossLinker xl, Peptide a, Peptide b) {
        return inner.passes(s, a) || inner.passes(s, b);
    }
    
    
    
}
