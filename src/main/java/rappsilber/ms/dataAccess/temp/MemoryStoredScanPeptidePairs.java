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
import rappsilber.utils.HashMapList;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class MemoryStoredScanPeptidePairs implements StoreScanPeptidePairs {
    
    private class MatchPair {
        ArrayList<Peptide> peps;
        HashMap<String,Double> scores;

        public MatchPair(ArrayList<Peptide> peps, HashMap<String, Double> scores) {
            this.peps = peps;
            this.scores = scores;
        }
    }


    HashMapList<Spectra, ArrayList<PeptideScanPairs>> store;


    @Override
    public void add(Spectra spectrum, ArrayList<Peptide> peptides, HashMap<String, Double> scores) {
        ArrayList<PeptideScanPairs> pairs = store.get(spectrum);
        if (pairs == null) {
            pairs = new ArrayList<PeptideScanPairs>();
            store.put(spectrum, pairs);
        }
        pairs.add(new PeptideScanPairs(spectrum, peptides, scores));
    }

    @Override
    public ArrayList<PeptideScanPairs> getAll(Spectra spectrum) {
        return store.get(spectrum);
    }

}
