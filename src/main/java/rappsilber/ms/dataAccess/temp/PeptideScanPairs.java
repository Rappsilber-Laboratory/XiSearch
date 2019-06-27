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
package rappsilber.ms.dataAccess.temp;

import java.util.ArrayList;
import java.util.HashMap;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.spectra.Spectra;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class PeptideScanPairs {
    public Spectra spectrum;
    public ArrayList<Peptide> matchedPeptides;
    public HashMap<String,Double> scores;

    public PeptideScanPairs(Spectra spectrum, ArrayList<Peptide> matchedPeptides, HashMap<String, Double> scores) {
        this.spectrum = spectrum;
        this.matchedPeptides = matchedPeptides;
        this.scores = scores;
    }

    

}
