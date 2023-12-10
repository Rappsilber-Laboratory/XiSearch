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

import rappsilber.config.RunConfig;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.SpectraPeakCluster;
import rappsilber.utils.Util;

/**
 * in opposition to the "normal" averagin/isotopcluster algorithm here we only
 * consider the highest charged matching isotope-cluster and therefor reducing 
 * the total number of possible cluster
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class IsotopeExtendedDeconvolution extends Averagin{

    public IsotopeExtendedDeconvolution(RunConfig conf) {
        super(conf);
    }

    @Override
    public void deConvoluteIsotops(Spectra spectra, int MaxCharge) {
        super.deConvoluteIsotops(spectra, MaxCharge); //To change body of generated methods, choose Tools | Templates.
        for (SpectraPeakCluster spc : spectra.getIsotopeClusters()) {
            double mz = spc.getMZ();
            SpectraPeak sp = spectra.getPeakAt(mz-Util.C13_MASS_DIFFERENCE);
            if (sp != null) {
                SpectraPeakCluster nspc = new SpectraPeakCluster(spectra.getTolearance());
                nspc.addAll(spc);
                super.error(spc, 0, 0);
            }
        }
    }
//    private static final double m_averagin_max_dist = 10;

    
}
