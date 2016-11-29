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
package rappsilber.ms.spectra.match.filter;

import java.util.Comparator;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.SpectraPeakCluster;
import rappsilber.ms.spectra.SpectraPeakClusterList;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 * a filter, to find them all and in the darkness bind them.
 * at least the cluster, that are not matched and completely
 * covered by another cluster
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class CleanUpIsotopCluster implements MatchFilter{

    @Override
    public void filter(MatchedXlinkedPeptide match) {
        SpectraPeakClusterList spcl = match.getSpectrum().getIsotopeClusters();

        SpectraPeakCluster[] origClusters = spcl.toArray(new SpectraPeakCluster[spcl.size()]);

        // go through all cluster, begining with the smallest one and delete any cluster without matches, that is completly covered by other clusters
        java.util.Arrays.sort(origClusters, new Comparator<SpectraPeakCluster>() {

                @Override
                public int compare(SpectraPeakCluster o1, SpectraPeakCluster o2) {
                    return o1.size() - o2.size();
                }
            });

        spcloop:
        for (SpectraPeakCluster spc : origClusters) {
            if (spc.get(0).getMatchedFragments().isEmpty()) {
                for (SpectraPeak sp : spc) {
                    if (spcl.countClusterHavingPeak(sp.getMZ()) == 1) {
                        continue spcloop;
                    }
                }
                // if we reach this place, than each peak appears in at least one other cluster
                // so we can delete it, without losing any information
                spcl.remove(spc);
            }
        }

    }

}
