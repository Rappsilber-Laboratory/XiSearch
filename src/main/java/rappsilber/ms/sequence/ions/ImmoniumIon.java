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
package rappsilber.ms.sequence.ions;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.ions.loss.Loss.LossCount;

public class ImmoniumIon extends Fragment {

    protected static int s_IonTypeID = -1;

    public ImmoniumIon (Peptide peptide,int residue, double mass) {
        super(peptide, 
                (short)residue,
                (short)1,0);
    }

    
    protected ImmoniumIon() {}


    @Override
    public String name() {
        return "b" + length();
    }


    public static boolean register() {
        try {
            Fragment.registerFragmentClass(ImmoniumIon.class);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(ImmoniumIon.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    public static ArrayList<Fragment> fragment(Peptide p) {
        ArrayList<Fragment> f = new ArrayList<Fragment>(p.length());
        for (int i = 1; i < p.length(); i++) {
            f.add(new ImmoniumIon(p, i,0));
        }
        return f;
    }

    public static void registerIonTypeID(int id) {
        s_IonTypeID = id;
    }

    @Override
    public int getIonTypeID() {
        return s_IonTypeID;
    }

    @Override
    public ArrayList<LossCount> getLossIDs() {
        return new ArrayList<LossCount>();
    }

}

