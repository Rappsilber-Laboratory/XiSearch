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
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class LossCoverage extends AbstractScoreSpectraMatch{
    private static final String  mAll = "loss matches";
    private static final String  m = "loss matched";
    private static final String  mVSu = "loss matched/unmatched";
    private static final String  mp = "loss matched%";
    private static final String  up = "loss unmatched%";

    public double score(MatchedXlinkedPeptide match) {
        int matched = 0;
        int unmatched = 0;
        int all =0;
        Peptide[] peps = match.getPeptides();

        HashMap<Peptide,boolean[]> foundSites =  new HashMap<Peptide, boolean[]>(peps.length);
        HashMap<Peptide,HashMap<Fragment,Boolean>> foundFragments = new HashMap<Peptide, HashMap<Fragment, Boolean>>();

        for (Peptide p: peps) {
            HashMap<Fragment,Boolean> frags = new HashMap<Fragment, Boolean>();
            for (Fragment f : p.getPrimaryFragments()) {
                if (!(f instanceof Loss)) {
                    frags.put(f, Boolean.FALSE);
                }
            }
            foundFragments.put(p, frags);
        }


        //MatchedFragmentCollection allMatches = new MatchedFragmentCollection();
        for (SpectraPeak sp : match.getSpectrum() ){
            for (Fragment f : sp.getMatchedFragments()) {
                if ((f instanceof Loss)) {
                    foundFragments.get(f.getPeptide()).put(f, Boolean.TRUE);
                    all++;
                }
            }
        }

        for (Peptide pep :match.getPeptides()) {
            HashMap<Fragment,Boolean> frags = foundFragments.get(pep);
            for (boolean b: frags.values()) {
                if (b) {
                    matched ++;
                } else {
                    unmatched ++;
                }
            }
        }
        
        addScore(match,mAll, all);
        addScore(match,m, matched);
        addScore(match,mVSu, matched/(double)unmatched);
        addScore(match,mp, matched/(double)(matched + unmatched));
        addScore(match,up, unmatched/(double)(matched + unmatched));

        return all;

    }

    public String[] scoreNames() {
        return new String[]{mAll,m,mVSu,mp,up};
    }

    public double getOrder() {
        return 10;
    }

}
