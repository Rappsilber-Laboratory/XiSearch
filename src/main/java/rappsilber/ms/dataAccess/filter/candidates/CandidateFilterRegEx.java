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

import java.util.regex.Pattern;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.spectra.Spectra;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class CandidateFilterRegEx implements CandidateFilter{
    Pattern pat;


    public CandidateFilterRegEx(Pattern p) {
        this.pat = p;
    }

    
    @Override
    public boolean passes(Spectra s, Peptide p) {
         return pat.matcher(p.toString()).matches();
    }
    
}
