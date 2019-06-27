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
package rappsilber.ms.score;

import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class PredefinedPeptideMassFit extends AbstractScoreSpectraMatch {

    @Override
    public double score(MatchedXlinkedPeptide match) {
        int c=0;
        double d = match.getPeptide1Weight()>0? 1d:0d;
        if (match.getPeptide2Weight()>0)
            d++;
        return d;
    }

    @Override
    public double getOrder() {
        return 1;
    }
    
}
