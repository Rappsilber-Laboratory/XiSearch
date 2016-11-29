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
package rappsilber.ms.sequence.ions.loss;

import java.text.ParseException;
import rappsilber.ms.sequence.ions.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.RunConfig;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.utils.Util;

/**
 * represents the creation of AIons as a Loss of CO from BIon (12 + 15.99491463  = 27.99491463)
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class BIonWaterGain extends Loss{

    private static int m_LossID = -1;

    /** need this for getting the class in a static function */
    protected BIonWaterGain() {

    }
    
    /**
     * creates a new fragment, that represents a loss from a b-ion to an a-ion
     *
     * @param f a fragment that is a b-ion or a loss to a b-ion
     */
    public BIonWaterGain(Fragment f) {
        super(f, + Util.AMMONIA_MASS, m_LossID);
    }


    @Override
    public String name() {
        return getParentFragment().name() + "_GAIN_H20";
    }

//    /** a static wrapper since getClass does not work from a static function
//     * @return
//     */
//    public static Class getMyClass() {
//        return new AIonLoss().getClass();
//    }

    public static ArrayList<Fragment> createLossyFragments(ArrayList<Fragment> frags, CrossLinker crosslinker, boolean insert) {
        ArrayList<Fragment> fragments = frags;
        ArrayList<Fragment> ret = new ArrayList<Fragment>(fragments.size());
        ArrayList<Fragment> base = fragments;
        for (Fragment f : base) {
            if (f.isClass(BIon.class) ) {
               ret.add(new BIonWaterGain(f));
            }
        }
        if (insert) {
            fragments.addAll(ret);
        }
        return ret;
    }

    public static boolean register() {
        try {
            Loss.registerLossClass(BIonWaterGain.class);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(BIonWaterGain.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    public static void parseArgs(String args, RunConfig conf) throws ParseException {
        String[] argParts = args.split(";");

        // parses something like: aminoacids:R,K
        for (String arg : argParts) {
            String[] ap = arg.split(":");
            String aName = ap[0].trim().toLowerCase();
            if (aName.contentEquals("id")) {
                m_LossID = Integer.parseInt(ap[1]);
            }
        }
    }

}
