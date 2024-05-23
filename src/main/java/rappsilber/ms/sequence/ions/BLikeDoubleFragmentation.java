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
import rappsilber.ms.spectra.match.MatchedBaseFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;

public class BLikeDoubleFragmentation extends DoubleFragmentation implements SecondaryFragment {

    protected static int s_IonTypeID = -1;
    

    public BLikeDoubleFragmentation (Peptide peptide1,short pep1Start, short pep1Lentgh) {
        super(peptide1, pep1Start, pep1Lentgh, 0);
    }

    protected BLikeDoubleFragmentation() {}

//    public static Class getMyClass() {
//        return new BIon().getClass();
//    }


    @Override
    public String name() {
        return "dbl_" + (getStart() +1) + "_" + length();
    }


    public static boolean register() {
        try {
            Fragment.registerFragmentClass(BLikeDoubleFragmentation.class);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(BLikeDoubleFragmentation.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    public static ArrayList<Fragment> fragment(Peptide p) {
        int plenght = p.length();
        ArrayList<Fragment> f = new ArrayList<Fragment>(p.length());
        for (short start = 1; start < p.length() - 2; start++) {
            for (short length = (short)(plenght - start - 1); length > 1; length--) {
                f.add(new BLikeDoubleFragmentation(p, start, length));
            }
        }
        return f;
    }

    public static void registerIonTypeID(int id) {
        BLikeDoubleFragmentation.s_IonTypeID = id;
    }

    @Override
    public int getIonTypeID() {
        return BLikeDoubleFragmentation.s_IonTypeID;
    }

    @Override
    public ArrayList<Loss.LossCount> getLossIDs() {
        return new ArrayList<Loss.LossCount>();
    }

    public static int getIonTypeStatic() {
        return s_IonTypeID;
    }

    public double getSupportLevel(MatchedFragmentCollection mfc, int charge) {
        for (Fragment f : mfc.getFragments()) {
            if ((!f.isClass(SecondaryFragment.class)) && !f.isClass(Loss.class)) {
                if (f.getStart() == this.getStart() || this.getEnd() == this.getEnd()) {
                    for (int c = mfc.getMaxChargeState(); c >= charge; c--) {
                        MatchedBaseFragment mbf =  mfc.getMatchedFragmentGroup(f, c);
                        if (mbf != null && mbf.isBaseFragmentFound()) {
                            return SUPPORT_DOUBLE_SUPPORTED;
                        }
                    }
                }
            }
        }
        return SUPPORT_DOUBLE_FRAGMENATION;
    }

}

