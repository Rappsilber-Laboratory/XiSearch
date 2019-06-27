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
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.utils.Util;

public class CIon extends Fragment {

    protected static int s_IonTypeID = -1;

    public CIon (Peptide peptide,int residue) {
        super(peptide, 
                (short)0,
                (short)residue,
                Util.NITROGEN_MASS + 3*Util.HYDROGEN_MASS);
    }

    /** dummy constructor to be able to use getClass in static functions */
    protected CIon(){}

//    public static Class getMyClass() {
//        return new CIon().getClass();
//    }


    @Override
    public String name() {
        return "c" + length();
    }

    public static boolean register() {
        try {
            Fragment.registerFragmentClass(CIon.class);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(CIon.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    public static ArrayList<Fragment> fragment(Peptide p) {
        ArrayList<Fragment> f = new ArrayList<Fragment>(p.length());
        for (int i = 1; i < p.length(); i++) {
            f.add(new CIon(p, i));
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
    public ArrayList<Loss.LossCount> getLossIDs() {
        return new ArrayList<Loss.LossCount>();
    }

}

