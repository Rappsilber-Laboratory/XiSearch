/*
 * Copyright 2018 Lutz Fischer <lfischer@staffmail.ed.ac.uk>.
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
package rappsilber.ms.sequence.ions;

import java.util.HashSet;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.sequence.AminoAcid;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class ImmoniumIonProducer {
    private CrossLinker cl;
    private HashSet<AminoAcid> residues;
    private boolean nTerm;
    private boolean cTerm;

    public ImmoniumIonProducer(CrossLinker cl, HashSet<AminoAcid> residues, boolean nTerm, boolean cTerm) {
        this.cl = cl;
        this.residues = residues;
        this.nTerm = nTerm;
        this.cTerm = cTerm;
    }
    
    public void fragment(){
        
    }
    
    
}
