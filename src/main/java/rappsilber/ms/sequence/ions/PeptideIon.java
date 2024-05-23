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

// <editor-fold defaultstate="collapsed" desc=" UML Marker "> 
// #[regen=yes,id=DCE.805085A4-99DD-F36C-4CBB-62E60BF50F60]
// </editor-fold> 
public class PeptideIon extends Fragment {

    protected static int s_IonTypeID = -1;


    // <editor-fold defaultstate="collapsed" desc=" UML Marker "> 
    // #[regen=yes,id=DCE.19DFF192-F8F0-452F-604D-EF3F521BB265]
    // </editor-fold> 
    public PeptideIon (Peptide peptide) {
        super(peptide, 
                (short)0,
                peptide.length(),
                Util.WATER_MASS);
    }
    
    protected PeptideIon(Peptide peptide, double massDiff) {
        super(peptide, 
                0,
                peptide.length(),
                Util.WATER_MASS+massDiff);
    }

    protected PeptideIon() {}

//    public static Class getMyClass() {
//        return new PeptideIon().getClass();
//    }

    public static boolean register() {
        try {
            Fragment.registerFragmentClass(PeptideIon.class);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(PeptideIon.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    public static ArrayList<Fragment> fragment(Peptide p) {
        ArrayList<Fragment> f = new ArrayList<Fragment>(1);
        f.add(new PeptideIon(p));
        return f;
    }

    @Override
    public String name() {
        return "P";
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

