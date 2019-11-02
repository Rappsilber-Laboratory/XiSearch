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

import java.util.ArrayList;
import java.util.regex.Pattern;
import rappsilber.ms.sequence.Peptide;
import rappsilber.utils.Util;

/**
 * a generic class that can then produce primary ions (akin to abc/xyz ions)
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class PrimaryIonProducer {
    boolean cterminal;
    boolean nterminal;
    double deltamass;
    Pattern restrictedTo;
    static ArrayList<PrimaryIonProducer> producers = new ArrayList<>();
    
    protected static int s_IonTypeID = -1;
    
    public class PrimaryIon extends Fragment{
        public PrimaryIon (Peptide peptide,short fromResidue,short toResidue, double deltamass) {
            super(peptide, 
                    (short)fromResidue,
                    (short)toResidue,
                    deltamass);
        }
        
    }
    


    public static ArrayList<Fragment> fragment(Peptide p) {
        ArrayList<Fragment> f = new ArrayList<Fragment>(p.length());
        for (int i = 1; i < p.length(); i++) {
            f.add(new AIon(p, i));
        }
        return f;
    }

    public static void registerIonTypeID(int id) {
        PrimaryIonProducer.s_IonTypeID = id;
    }
    
}
