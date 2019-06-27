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

import rappsilber.ms.crosslinker.NonCovalentBound;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptideWeighted;


/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class LinkSiteDelta extends AbstractScoreSpectraMatch{
    public final static String NAME = "LinkSiteDelta";

    public double getOrder() {
        return 400;
    }

    public double score(MatchedXlinkedPeptide match) {
        // only work if we have weights and then only if we actually have a cross-link
        if (match instanceof MatchedXlinkedPeptideWeighted && match.getPeptides().length >1 && !(match.getCrosslinker() == null || match.getCrosslinker() instanceof NonCovalentBound)) {
            MatchedXlinkedPeptideWeighted wm = (MatchedXlinkedPeptideWeighted) match;
            double delta = Double.MAX_VALUE;
            for (int pid = 0; pid<2 ; pid++) {
                Peptide p = wm.getPeptide(pid);
                double[] w = wm.getLinkageWeights(pid);
                int site = wm.getLinkSites(p)[0];
                if (site < 0) {
                    System.err.println("Link site is < 0");
                    for (int epid = 0; epid<2 ; epid++) {
                        Peptide ep = wm.getPeptide(epid);
                        int esite = wm.getLinkSites(ep)[0]; 
                        System.err.println(ep + " - " + esite);
                    }
                    System.err.println(match.getSpectrum().getRun() + " - " + match.getSpectrum().getScanNumber());                   
                }
                double weight = w[site];
                for (int s=0;s<site;s++) {
                    double wd = weight - w[s];
                    if (wd < delta) {
                        delta = wd;
                    }
                }
                if (delta >0) {
                    for (int s=site+1;s<p.length();s++) {
                        double wd = weight - wm.getLinkageWeight(pid, s);
                        if (wd < delta) {
                            delta = wd;
                        }
                    }
                }
            }
            addScore(match, NAME, delta);
            return 0;
        } else {
            addScore(match, NAME, 0);
            return 0;
        }
        
    }

    
    
}
