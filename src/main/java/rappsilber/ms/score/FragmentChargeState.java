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

import rappsilber.ms.spectra.SpectraPeakCluster;
import rappsilber.ms.spectra.SpectraPeakClusterList;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.utils.IntArrayList;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class FragmentChargeState  extends AbstractScoreSpectraMatch {
    private String maxName = "MaxCharge";
    private String averageName = "AverageCharge";
    private String medianName = "MedianCharge";
    private String relative = "Relative";
    private String relativeMaxName = relative + maxName;
    private String relativeAverageName = relative + averageName;
    private String relativeMedianName = relative + medianName;
    
    public double score(MatchedXlinkedPeptide match) {
        
        SpectraPeakClusterList spcl = match.getSpectrum().getIsotopeClusters();
        int clustercount = spcl.size();
        double precCharge = match.getSpectrum().getPrecurserCharge();
        IntArrayList charges = new IntArrayList(clustercount);
        double sumcharge = 0;
        int maxCharge = 0;
        
        for (SpectraPeakCluster spc : spcl) {
            int c = spc.getCharge();
            sumcharge += c;
            charges.add(c);
            if (c>maxCharge)
                maxCharge = c;
        }
        
        double averagecharge = 0;
        java.util.Collections.sort(charges);
        double median = 0;
        if (clustercount >1) {
            averagecharge = sumcharge /clustercount;
            int mid = clustercount/2;
            if (clustercount%2 == 0) {
                median = (charges.get(mid) + charges.get(mid-1)) /2.0;
            } else {
                median = charges.get(mid);
            }
        } else if (clustercount == 1) {
            median = charges.get(0);
            averagecharge = sumcharge;
        }
        
        addScore(match,averageName, averagecharge);
        addScore(match,maxName, maxCharge);
        addScore(match,relativeMaxName, maxCharge/precCharge);
        addScore(match,relativeAverageName, averagecharge/precCharge);
        addScore(match,medianName, median);
        addScore(match,relativeMedianName, median/precCharge);
        
        return maxCharge/precCharge;
    }

    public String[] scoreNames() {
        return new String[]{maxName,averageName,relativeMaxName, relativeAverageName};
    }
    
    public double getOrder() {
        return 10;
    }
    
}
