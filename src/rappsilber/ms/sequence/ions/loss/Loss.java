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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.RunConfig;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.match.MatchedBaseFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;

public abstract class Loss extends Fragment {


    public class LossCount {
        public int lossID;
        public int count;

        public LossCount(int lossID, int count) {
            this.lossID = lossID;
            this.count = count;
        }

    }



    private Fragment m_baseFragment;
    private Fragment m_parentFragment;

    protected double             m_massDiff;
    protected int                m_lossCount = -1;
    protected int                m_LossID = -1;



    private static ArrayList<Method> m_losses = new ArrayList<Method>();
    

//    public Loss (Peptide peptide,int residue) {
//        super(peptide,
//                0,
//                residue,
//                -28);
//    }




    /** dummy constructor to be able to use getClass in static functions */
    protected Loss(){}

    protected Loss(Fragment parent, double weightDiff, int ID) {
        super(parent.getPeptide(), parent.getStart(), parent.length(), parent.getMassDifference() + weightDiff);
        m_LossID = ID;
        m_massDiff = weightDiff;
        m_lossCount = 1;
        m_parentFragment = parent;
        if (parent instanceof Loss) {
            m_baseFragment = ((Loss) parent).getBaseFragment();
        } else
            m_baseFragment = parent;
    }

//    public static Class getMyClass() {
//        return new Loss().getClass();
//    }

    /**
     * give a name for the fragment
     * @return
     */
    @Override
    public String name() {
        throw new UnsupportedOperationException("Loss is a meta class and does not has a name");
    }


    public static ArrayList<Fragment> includeLosses(ArrayList<Fragment> fragments,CrossLinker crosslinker,  boolean comulative) {
        ArrayList<Method> losses = m_losses;
        ArrayList<Fragment> returnList = new ArrayList<Fragment>();
        for (Method m :losses) {
            Object ret = null;
            try {

                ret = m.invoke(null, fragments, crosslinker, false);

            } catch (IllegalAccessException ex) { //<editor-fold desc="and some other" defaultstate="collapsed">
            Logger.getLogger(Fragment.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(Fragment.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvocationTargetException ex) {
                Logger.getLogger(Fragment.class.getName()).log(Level.SEVERE, null, ex);//</editor-fold>
            }

            if ((!comulative) && ret != null && ret instanceof ArrayList) {
                returnList.addAll((ArrayList<Fragment>)ret);
            }
        }
        return returnList;
    }

    public static ArrayList<Fragment> includeLosses(ArrayList<Fragment> fragments,CrossLinker crosslinker,  boolean comulative, RunConfig conf) {

        ArrayList<Method> losses = conf.getLossMethods();

        ArrayList<Fragment> returnList = new ArrayList<Fragment>();
        for (Method m :losses) {
            Object ret = null;
            try {

                ret = m.invoke(null, fragments, crosslinker, true, conf);

            } catch (IllegalAccessException ex) { //<editor-fold desc="and some other" defaultstate="collapsed">
                Logger.getLogger(Fragment.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(Fragment.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvocationTargetException ex) {
                Logger.getLogger(Fragment.class.getName()).log(Level.SEVERE, null, ex);//</editor-fold>
            }

            if ((!comulative) && ret != null && ret instanceof ArrayList) {
                returnList.addAll((ArrayList<Fragment>)ret);
            }
        }
        return returnList;
    }





    /**
     * @return the fragment that was lost something to generate this fragment
     */
    public Fragment getBaseFragment() {
        return m_baseFragment;
    }

    /**
     * @return the (possibly lossy) fragment that was lost something to generate this fragment
     */
    public Fragment getParentFragment() {
        return m_parentFragment;
    }

//    /**
//     * @param BaseFragment the base fragment to set
//     */
//    public void setBaseFragment(Fragment BaseFragment) {
//        this.m_baseFragment = BaseFragment;
//    }

    /**
     * returns whether the spectra also contains a peak, that could be the according parent-fragment
     * @param s
     * @param charge
     * @param tu
     * @return
     */
    public boolean foundBasePeak(Spectra s, int charge, double modification, ToleranceUnit tu) {
        return (getBasePeak(s, charge, modification, tu) != null);
    }


    /**
     * returns whether the spectra also contains a peak, that could be the according parent-fragment
     * @param s
     * @param charge
     * @param tu
     * @return
     */
    public SpectraPeak getBasePeak(Spectra s, int charge, double modification, ToleranceUnit tu) {
        double base_mz = m_baseFragment.getMZ(charge) + modification/charge;
        SpectraPeak p = s.getPeakAt(base_mz);
        return p;
    }

    public static void registerLossClass(Class<? extends Fragment> c) throws NoSuchMethodException {
        //find the createLossyFragments method
        for (Method m : c.getMethods()) {
            if (m.getName().contentEquals("createLossyFragments")) {
                Class[] params = m.getParameterTypes();
                if (params.length == 3 &&
                        params[0] == ArrayList.class &&
                        params[1] == CrossLinker.class &&
                        params[2] == boolean.class &&
                        !m_losses.contains(m))
                m_losses.add(m);
            }
        }
       // m_losses.add(c.getMethod("createLossyFragments", new Class[]{ArrayList.class, boolean.class}));
        
    }

    public static void registerLossClass(Class<? extends Fragment> c, RunConfig conf) throws NoSuchMethodException {

        ArrayList<Method> losses = conf.getLossMethods();

        //find the createLossyFragments method
        for (Method m : c.getMethods()) {
            if (m.getName().contentEquals("createLossyFragments")) {
                Class[] params = m.getParameterTypes();
                if (params.length == 4 &&
                        params[0] == ArrayList.class &&
                        params[1] == CrossLinker.class &&
                        params[2] == boolean.class &&
                        params[3] == RunConfig.class &&
                        !losses.contains(m))
                losses.add(m);
            }
        }
       // m_losses.add(c.getMethod("createLossyFragments", new Class[]{ArrayList.class, boolean.class}));

    }

    /**
     * returns whether this fragment is of the given class, or was derived from
     * the given class
     * @param c
     * @return
     */
    @Override
    public boolean isClass(Class c) {
        return c.isInstance(this) || getParentFragment().isClass(c);
    }


    public void free(){
        super.free();
        m_baseFragment = null;
        m_parentFragment = null;
    }

    
    public double getLossyMass() {
        return m_massDiff;
    }

    /**
     * returns how often the current loss happened to the parent fragment to produce this fragment
     * @return
     */
    public int getLossCount() {
        return m_lossCount;
    }

    /**
     * returns how often the any loss happened to the parent fragment to 
     * produce this fragment <br/>
     * eg. if this fragment is a 3 times loss of water from a B-ion then
     * this would be three<br/>
     * but if it is 3 times loss of water from a loss of (1x) amonia fragment
     * then it would be 4 (3x water + 1x amonia)
     *
     * @return
     */
    public int getTotalLossCount() {
        Fragment parent = getParentFragment();

        if (parent instanceof Loss)
            return m_lossCount + ((Loss)parent).getTotalLossCount();
        return m_lossCount;
    }


    public static void parseArgs(String args, RunConfig conf) throws ParseException {
        //System.out.println("args>>>> " + args);
        String[] c = args.split("[:;]",2);
        String cName;
        if (c.length == 1) {
            cName = c[0];
            if (!cName.contains("."))
                cName = "rappsilber.ms.sequence.ions.loss." + cName;
        }else {
            cName = c[0];
            if (!c[0].contains("."))
                cName = "rappsilber.ms.sequence.ions.loss." + cName;

            try {
                for (Method m : Class.forName(cName).getMethods()) {
                    if (m.getName().contentEquals("parseArgs") && m.getParameterTypes().length == 2) {
                        try {
                            m.invoke(null, c[1], conf);
                        } catch (IllegalAccessException ex) {
                            Logger.getLogger(Loss.class.getName()).log(Level.SEVERE, null, ex);
                            throw new ParseException(args, 0);
                        } catch (IllegalArgumentException ex) {
                            Logger.getLogger(Loss.class.getName()).log(Level.SEVERE, null, ex);
                            throw new ParseException(args, 0);
                        } catch (InvocationTargetException ex) {
                            Logger.getLogger(Loss.class.getName()).log(Level.SEVERE, null, ex);
                            throw new ParseException(args, 0);
                        }
                    }
                }
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Loss.class.getName()).log(Level.SEVERE, null, ex);
                throw new ParseException(args, 0);
            }

        }

        // parses something like: DigestedAminoAcids:R,K

            try {
                Class cl;
                cl = Class.forName(cName);
                registerLossClass(cl.asSubclass(Fragment.class), conf);

            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Loss.class.getName()).log(Level.SEVERE, "Loss-class not found:" + args, ex);
            } catch (NoSuchMethodException ex) {
                Logger.getLogger(Loss.class.getName()).log(Level.SEVERE, "Loss-class \"" + args + "\" does has no createLossyFragments-method", ex);
            }
    }

    public String toString() {
        return m_baseFragment.toString();
    }

    @Override
    public int getIonTypeID() {
        return getBaseFragment().getIonTypeID();
    }

    @Override
    public ArrayList<LossCount> getLossIDs() {
        ArrayList<LossCount> losses = new ArrayList<LossCount>();
        losses.add(new LossCount(m_LossID, m_lossCount));
        losses.addAll(getParentFragment().getLossIDs());
        return losses;
    }

    @Override
    public double getSupportLevel(MatchedFragmentCollection mfc, int charge) {
        MatchedBaseFragment mbf = mfc.getMatchedFragmentGroup(this, charge);
//        if (mbf == null) {
//            System.err.println("error here!");
//        }
        HashMap<Loss,SpectraPeak> losses = mbf.getLosses();
        int lossCount = losses.size();

        if (mbf.isBaseFragmentFound())
            return getBaseSupportLevel() + SUPPORT_SUPPORTING_NONLOSSY;
        
        return getBaseSupportLevel() + SUPPORT_LOSSY*(lossCount-1);
    }

    @Override
    public double getBaseSupportLevel() {
        return SUPPORT_LOSSY;
    }


    @Override
    public boolean canFullfillXlink(Peptide p, int site) {
        return m_parentFragment.canFullfillXlink(p, site);
    }

    @Override
    public boolean canFullfillXlink(HashMap<Peptide, Integer> sites) {
        return m_parentFragment.canFullfillXlink(sites);
    }
}

