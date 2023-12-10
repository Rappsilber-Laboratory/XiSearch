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
package rappsilber.ms.spectra.annotation;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;
import rappsilber.config.RunConfig;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.SpectraPeakCluster;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class X2Isotop extends Averagin{

    public X2Isotop(RunConfig conf) {
        super(conf);
    }

   @Override
    public void AnnotateIsotops (Spectra spectra, int MaxCharge) {

       if (spectra.getIsotopeClusters().size() == 0)
            // first annotate the isotop-clusters
            super.AnnotateIsotops(spectra, MaxCharge);

        // take a look at the annotated isotop peaks
        Collection<SpectraPeakCluster> IsoptopClusters = spectra.getIsotopeClusters();
        TreeMap<Double,SpectraPeakCluster> ScoredCluster = new TreeMap<Double, SpectraPeakCluster>();
        double mz = 0;

        for (SpectraPeakCluster cluster : IsoptopClusters) {
            
            double X2 = 0;
            
            if (cluster.getMZ() != mz) {
                ScoredCluster = new TreeMap<Double, SpectraPeakCluster>();
            }

            double mass = cluster.getMass();

            // should be determined based on the highest intens peak
            // but does not really make sends with the curretn implementation of averagine
            double factor = relativeHight(mass, cluster.getCharge());

            Iterator<SpectraPeak> peaks = cluster.iterator();

            int p = 0;
            while (peaks.hasNext()) {
                SpectraPeak peak = peaks.next();
                double o = peak.getIntensity();
                double e = relativeHight(mass, p) * factor;
                X2 += (o-e)*(o-e)/e;

                p++;
            }
            cluster.setAveraginScore(X2/p);
        }


    }


}
