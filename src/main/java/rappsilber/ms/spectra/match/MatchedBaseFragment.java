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
package rappsilber.ms.spectra.match;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;

/**
 * Represents a group of fragments that belongs to the same base fragment
 * (A/B/C/X/Y/Z-ion)
 * Meaning a fragment group consists of a base fragment (A/B/C/X/Y/Z-ion)
 * and all lossy fragments of it
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class MatchedBaseFragment {
    private Fragment m_BaseFragment;
    private int m_Charge;
    private boolean m_BaseFragmentFound;
    private SpectraPeak m_basePeak;
    private Spectra     m_spectra;
    private HashMap<Loss, SpectraPeak> m_Losses = new HashMap<Loss, SpectraPeak>();


//    public static double CORROBORATION_BASE_FRAGMENT = 1;
//    public static double CORROBORATION_SAME_LOSS_MASS = 0.3;
//    public static double CORROBORATION_LOSS = 0.15;
//
//    public double getCorroborationLevel() {
//        double coroboration = 0;
//        // first check whether we have a base fragment
//
//        if (isBaseFragmentFound())
//            coroboration += CORROBORATION_BASE_FRAGMENT;
//
//        Set<Loss> losses = m_Losses.keySet();
//        for (Loss l : losses) {
//
//        }
//
//    }
    /**
     * initialises a new fragment group
     * @param base
     * @param charge
     * @param l
     * @param peak
     */
    public MatchedBaseFragment(Fragment base, int charge, Loss l, SpectraPeak peak) {
        this(base, charge);
        m_Losses.put(l,peak);
    }

    /**
     * initialises a new fragment group
     * @param base
     * @param charge
     */
    public MatchedBaseFragment(Fragment base, int charge) {
        m_BaseFragment = base;
        m_Charge = charge;
    }

    /**
     * initialises a new fragment group
     * @param base
     * @param charge
     * @param peak
     */
    public MatchedBaseFragment(Fragment base, int charge, SpectraPeak peak) {
        m_BaseFragment = base;
        m_Charge = charge;
        m_basePeak = peak;
        m_BaseFragmentFound = true;
    }

    /**
     * @return the m_BaseFragment
     */
    public Fragment getBaseFragment() {
        return m_BaseFragment;
    }


    /**
     * @return the charge state of the matched fragments
     */
    public int getCharge() {
        return m_Charge;
    }

    /**
     * @return whether the base fragment (b/y ion) for the fragment group was found
     */
    public boolean isBaseFragmentFound() {
        return m_BaseFragmentFound;
    }

    /**
     * defines whether the base fragment was found or not
     * @param found
     */
    public void setBaseFragmentFound(boolean found) {
        m_BaseFragmentFound = found;
    }

    /**
     * defines, that the base fragment was found
     */
    public void setBaseFragmentFound() {
        m_BaseFragmentFound = true;
    }

    /**
     * defines, that the base fragment was found
     */
    public void setBaseFragmentFound(SpectraPeak peak) {
        m_BaseFragmentFound = true;
        m_basePeak = peak;
    }

    /**
     * defines, that the base fragment was not found
     */
    public void unsetBaseFragmentFound() {
        m_BaseFragmentFound = false;
        m_basePeak = null;
    }


    /**
     * @return the peak that the non lossy fragment was matched to
     */
    public SpectraPeak getBasePeak() {
        return m_basePeak;
    }

    /**
     * @return the lossy fragments
     */
    public HashMap<Loss, SpectraPeak> getLosses() {
        return m_Losses;
    }

    /**
     * 
     * @return a list of all found fragments
     */
    public Set<Fragment> getFragments() {
        Set<Fragment> f = new HashSet<Fragment>(m_Losses.size() + 1);
        f.addAll(m_Losses.keySet());
        if (isBaseFragmentFound()) {
            f.add(m_BaseFragment);
        }
        return f;
    }

    /**
     * cleans up the references
     */
    public void free() {
        m_BaseFragment = null;
        m_Losses.clear();
        m_basePeak = null;
    }

    @Override
    public MatchedBaseFragment clone() {
        MatchedBaseFragment ret = new MatchedBaseFragment(m_BaseFragment, m_Charge);
        ret.m_BaseFragmentFound = m_BaseFragmentFound;
        ret.m_Losses = (HashMap<Loss, SpectraPeak>) m_Losses.clone();
        ret.m_basePeak = m_basePeak;
        ret.m_spectra = m_spectra;
        return ret;
    }

    public void setCharge(int c) {
        m_Charge = c;
    }
}
