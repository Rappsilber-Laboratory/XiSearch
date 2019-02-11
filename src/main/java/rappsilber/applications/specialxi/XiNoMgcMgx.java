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
public class XiNoMgcMgx extends SimpleXiProcessLinearIncluded {
    
    public XiNoMgcMgx(File fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        super(new File[]{fasta}, input, output, config, filter);
        System.out.println(this.getClass().getSimpleName());
    }


    public XiNoMgcMgx(File[] fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        super(fasta, input, output, config, filter);
        System.out.println(this.getClass().getSimpleName());

    }

    public XiNoMgcMgx(SequenceList fasta, AbstractSpectraAccess input, ResultWriter output, RunConfig config, StackedSpectraAccess filter) {
        super(fasta, input, output, config, filter);
        System.out.println(this.getClass().getSimpleName());

    }    
    

    
    protected Spectra getMGCSpectrum(Spectra full) {
        return  full.cloneComplete();
    }

    protected Spectra getMGXSpectra(Spectra mgc, Spectra full) {
        return  full.cloneComplete();
    }

    
}
