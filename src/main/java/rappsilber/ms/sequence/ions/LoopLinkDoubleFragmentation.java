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
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.ions.loss.CrosslinkerModified;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.match.MatchedBaseFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;

public class LoopLinkDoubleFragmentation  extends CrosslinkedFragment {

    protected static int s_IonTypeID = -1;
    protected CrossLinker m_crosslinker;
    protected Fragment m_first;
    protected Fragment m_second;
    

    
    public LoopLinkDoubleFragmentation(Fragment first, Fragment second, CrossLinker crosslinker) {
        super(first, second, crosslinker);
        m_crosslinker = crosslinker;
        m_first = first;
        m_second = second;
        //setBaseFragments();
    }    

     public static ArrayList<Fragment> createCrosslinkedFragments(Collection<Fragment> fragments, Fragment Crosslinked, CrossLinker crosslinker, boolean noPeptideIons) {
                              //         createLossyFragments(java.util.ArrayList, boolean)
        boolean prec=false;
        ArrayList<Fragment> ret = new ArrayList<Fragment>(fragments.size());
        for (Fragment f : fragments) {
            if (!f.isClass(CrosslinkerModified.class))
                try {
                    if (noPeptideIons && f instanceof PeptideIon)
                        continue;
                    if (DoubleFragmentation.isDisabled() && !(f instanceof PeptideIon))
                        continue;
                    ret.add(new CrosslinkedFragment(f,Crosslinked, crosslinker));
                } catch (Exception e) {
                    throw new Error(e);
                }
        }
        return ret;
    }


//    public static Class getMyClass() {
//        return new BIon().getClass();
//    }

    public static ArrayList<Fragment> fragment(Peptide p) {
        int plenght = p.length();
        ArrayList<Fragment> f = new ArrayList<Fragment>(p.length());
        for (short start = 1; start < p.length() - 2; start++) {
            for (short length = (short)(plenght - start - 1); length > 1; length--)
                f.add(new BLikeDoubleFragmentation(p, start, length));
        }
        return f;
    }

    @Override
    public String name() {
        return "loop_" + (m_first.name()) + "_" + m_second.name();
    }


    public static boolean register() {
        try {
            Fragment.registerFragmentClass(LoopLinkDoubleFragmentation.class);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(LoopLinkDoubleFragmentation.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    // takes previously defined fragments and joins them up as loop-linked fragments
    public static ArrayList<Fragment> fragment(Peptide p, ArrayList<Fragment> prevFragmenst, CrossLinker crosslinker) {
        int plenght = p.length();
        ArrayList<Fragment> f = new ArrayList<Fragment>(p.length());
        return f;
    }

    public static void registerIonTypeID(int id) {
        LoopLinkDoubleFragmentation.s_IonTypeID = id;
    }

    @Override
    public int getIonTypeID() {
        return LoopLinkDoubleFragmentation.s_IonTypeID;
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
                if (f.getStart() == this.getStart() || this.getEnd() == this.getEnd())
                for (int c = mfc.getMaxChargeState();c >= charge; c--) {
                    MatchedBaseFragment mbf =  mfc.getMatchedFragmentGroup(f, c);
                    if (mbf != null && mbf.isBaseFragmentFound())
                        return SUPPORT_DOUBLE_SUPPORTED;
                }
            }
        }
        return SUPPORT_DOUBLE_FRAGMENATION;
    }

}

