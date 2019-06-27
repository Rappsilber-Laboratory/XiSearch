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
import rappsilber.utils.Util;

public class AIon extends Fragment {

    protected static int s_IonTypeID = -1;

    public AIon (Peptide peptide,int residue) {
        super(peptide, 
                (short)0,
                (short)residue,
                -(Util.OXYGEN_MASS+ Util.CARBON_MASS));
    }

    /** dummy constructor to be able to use getClass in static functions */
    protected AIon(){}

//    public static Class getMyClass() {
//        return new AIon().getClass();
//    }


    @Override
    public String name() {
        return "a" + length();
    }

    public static boolean register() {
        try {
            Fragment.registerFragmentClass(AIon.class);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(AIon.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    public static ArrayList<Fragment> fragment(Peptide p) {
        ArrayList<Fragment> f = new ArrayList<Fragment>(p.length());
        for (int i = 1; i < p.length(); i++) {
            f.add(new AIon(p, i));
        }
        return f;
    }

    public static void registerIonTypeID(int id) {
        AIon.s_IonTypeID = id;
    }

    @Override
    public int getIonTypeID() {
        return AIon.s_IonTypeID;
    }

    @Override
    public ArrayList<LossCount> getLossIDs() {
        return new ArrayList<LossCount>();
    }




}

