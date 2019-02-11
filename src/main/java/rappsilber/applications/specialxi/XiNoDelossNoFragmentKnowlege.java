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
package rappsilber.applications.specialxi;

import java.io.File;
import rappsilber.applications.SimpleXiProcessLinearIncluded;
import rappsilber.config.RunConfig;
import rappsilber.ms.dataAccess.AbstractSpectraAccess;
import rappsilber.ms.dataAccess.StackedSpectraAccess;
import rappsilber.ms.dataAccess.output.ResultWriter;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ms.spectra.Spectra;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class XiNoDelossNoFragmentKnowlege extends SimpleXiProcessLinearIncluded {
    
    public XiNoDelossNoFragmentKnowlege(File fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        super(new File[]{fasta}, input, output, config, filter);
        System.out.println(this.getClass().getSimpleName());
    }


    public XiNoDelossNoFragmentKnowlege(File[] fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        super(fasta, input, output, config, filter);
        System.out.println(this.getClass().getSimpleName());

    }

    public XiNoDelossNoFragmentKnowlege(SequenceList fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        super(fasta, input, output, config, filter);
        System.out.println(this.getClass().getSimpleName());

    }    
    


    
    @Override
    protected Spectra getMGCSpectrum(Spectra full) {
        Spectra mgcFull = full.cloneComplete();
//                        // deloss
//                        mgcFull.DEFAULT_ISOTOP_DETECTION.anotate(mgcFull);
//                        mgcFull = mgcFull.deLoss(18.01056027);
//                        mgcFull.DEFAULT_ISOTOP_DETECTION.anotate(mgcFull);
//                        mgcFull = mgcFull.deLoss(17.02654493);
//                        
//                        //decharge
        
        mgcFull.DEFAULT_ISOTOP_DETECTION.anotate(mgcFull);
        mgcFull = mgcFull.deCharge();
//                        
//                        //deisotop
        mgcFull.DEFAULT_ISOTOP_DETECTION.anotate(mgcFull);
        mgcFull = mgcFull.deIsotop();
        Spectra mgc = mgcFull.cloneTopPeaks(getConfig().getNumberMgcPeaks(), 100);
        
        full.free();
        
        return  mgc;
    }

    @Override
    protected Spectra getMGXSpectra(Spectra mgc, Spectra full) {
        return  getMGCSpectrum(full);
    }

    
}
