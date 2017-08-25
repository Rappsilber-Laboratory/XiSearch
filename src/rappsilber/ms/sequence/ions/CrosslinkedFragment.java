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

import java.util.HashMap;
import rappsilber.ms.sequence.ions.loss.*;
import rappsilber.ms.sequence.ions.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.SequenceUtils;
import rappsilber.ms.spectra.match.MatchedBaseFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class CrosslinkedFragment extends Fragment implements CrosslinkerContaining{
    private Fragment    m_crosslinkedFragment;
    private Fragment    m_baseFragment;
    private CrossLinker m_crosslinker;
    

    public CrosslinkedFragment(Fragment base, Peptide Crosslinked, CrossLinker crosslinker) {
        super(base.getPeptide(), base.getStart(), base.length(), base.getMassDifference() + Crosslinked.getMass() + crosslinker.getCrossLinkedMass());
        m_crosslinkedFragment = new PeptideIon(Crosslinked);
        m_crosslinker = crosslinker;
        m_baseFragment = base;
    }

    public CrosslinkedFragment(Fragment base, Fragment Crosslinked, CrossLinker crosslinker) {
        super(CrosslinkedFragment.getBaseFragments(base, Crosslinked), base.getNeutralMass() + Crosslinked.getNeutralMass() + crosslinker.getCrossLinkedMass());
        m_crosslinker = crosslinker;
        m_baseFragment = CrosslinkedFragment.getBaseFragments(base, Crosslinked);
        m_crosslinkedFragment = (Crosslinked == m_baseFragment? base: Crosslinked);
        //setBaseFragments();
    }


    protected static Fragment getBaseFragments(Fragment f1, Fragment f2) {
        double rLength1 = f1.length() / (double)f1.getPeptide().length();
        double rLength2 = f2.length() / (double)f2.getPeptide().length();

        if (rLength1 <= rLength2 ) {
            return f1;
        } else {
            return f2;
        }

    }

    private void setBaseFragments() {
        double rLength1 = m_crosslinkedFragment.length() / (double)m_crosslinkedFragment.getPeptide().length();
        double rLength2 = m_baseFragment.length() / (double)m_baseFragment.getPeptide().length();

        if (rLength1 <= rLength2 ) {
            Fragment temp  = m_crosslinkedFragment;
            m_crosslinkedFragment = m_baseFragment;
            m_baseFragment = temp;
        } 
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

    public static ArrayList<Fragment> createCrosslinkedFragments(Collection<Fragment> fragments, Collection<Fragment> Crosslinked, CrossLinker crosslinker, boolean noPeptideIons) {
        ArrayList<Fragment> ret = new ArrayList<Fragment>(fragments.size());
        for (Fragment f : fragments) {
            for (Fragment c : Crosslinked) {
                if (!f.isClass(CrosslinkerModified.class) && !c.isClass(CrosslinkerModified.class)) {
                    if (noPeptideIons && f instanceof PeptideIon)
                        continue;
                    if (DoubleFragmentation.isDisabled() && !(f instanceof PeptideIon || c instanceof PeptideIon))
                        continue;
                    try {
                        if (crosslinker.canCrossLink(f, c))
                            ret.add(new CrosslinkedFragment(f, c, crosslinker));
                    } catch (Exception e) {
                        throw new Error(e);
                    }
            }
            }
        }
        
        return ret;
    }


    public static ArrayList<Fragment> createCrosslinkedFragments(Collection<Fragment> fragments, Collection<Fragment> Crosslinked, CrossLinker crosslinker, int linkSite1, int linkSite2) {
                              //         createLossyFragments(java.util.ArrayList, boolean)
        ArrayList<Fragment> ret = new ArrayList<Fragment>(fragments.size());
        for (Fragment f : fragments) {
            for (Fragment c : Crosslinked) {
                if (f.getStart() <= linkSite1 && linkSite1 <= f.getEnd() && !f.isClass(CrosslinkerModified.class))
                     if (c.getStart() <= linkSite2 && linkSite2 <= c.getEnd() && !c.isClass(CrosslinkerModified.class))
                        if (!f.isClass(CrosslinkerModified.class) && !c.isClass(CrosslinkerModified.class))
                            if (DoubleFragmentation.isEnabled() || (f instanceof PeptideIon || c instanceof PeptideIon))
                                if (crosslinker.canCrossLink(f, c))
                                    ret.add(new CrosslinkedFragment(f, c, crosslinker));
            }
        }
        return ret;
    }


    @Override
    public int countAminoAcid(HashSet<AminoAcid> aas) {
        return SequenceUtils.countAminoAcid(getBaseFragment(), aas) + SequenceUtils.countAminoAcid(m_crosslinkedFragment, aas);
    }


    @Override
    public FragmentationSite[] getFragmentationSites() {
        FragmentationSite[] fsBase = m_baseFragment.getFragmentationSites();
        FragmentationSite[] fsCross = m_crosslinkedFragment.getFragmentationSites();
        FragmentationSite[] fs = new FragmentationSite[fsBase.length + fsCross.length];
        System.arraycopy(fsBase, 0, fs, 0, fsBase.length);
        System.arraycopy(fsCross, 0, fs, fsBase.length, fsCross.length);

        return fs;
    }

    /**
     * returns whether the fragment has only one fragmentation site
     * @return
     */
    public boolean isBasicFragmentation() {
        return (m_baseFragment.isBasicFragmentation() && m_crosslinkedFragment.isClass(PeptideIon.class)) || (m_crosslinkedFragment.isBasicFragmentation() && m_baseFragment.isClass(PeptideIon.class));
//        if (m_start > 0) {
//            return m_start + m_length == m_peptide.length();
//        } else 
//            return m_length>0 && m_length < m_peptide.length();
        
    }
    

    @Override
    public String name() {
        return m_baseFragment.name() + "+" + m_crosslinkedFragment.name();
    }

    @Override
    public boolean isClass(Class c) {
        return c.isInstance(this) || m_baseFragment.isClass(c) || m_crosslinkedFragment.isClass(c);
    }

    @Override
    public String toString() {
        return m_baseFragment.toString() + " + " + m_crosslinkedFragment.toString();
    }

    public Fragment getCrossLinkedFragment() {
        return m_crosslinkedFragment;
    }

    public Fragment[] getFragments() {
        return new Fragment[]{m_baseFragment, m_crosslinkedFragment};
    }


    public Fragment getBaseFragment() {
        return m_baseFragment;
    }

    @Override
    public boolean canFullfillXlink(HashMap<Peptide, Integer> sites) {
        for (Peptide p: sites.keySet()) {
            for (Fragment f : getFragments()) {
                int x = sites.get(p);
                if (f.getPeptide() == p) {
                    if (f.getStart() > x || f.getEnd() < x)
                        return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean canFullfillXlink(Peptide p, int site) {
        for (Fragment f : getFragments()) {
            if (p == f.getPeptide()) {
                if (f.getStart() > site || f.getEnd() < site)
                    return false;
            }
        }
        return true;
    }

    @Override
    public boolean canFullfillXlink(Peptide p1, int site1, Peptide p2, int site2) {
        for (Fragment f : getFragments()) {
            if (p1 == f.getPeptide()) {
                if (f.getStart() > site1 || f.getEnd() < site1)
                    return false;
            } else
            if (p2 == f.getPeptide()) {
                if (f.getStart() > site2 || f.getEnd() < site2)
                    return false;
            }
        }
        return true;
    }


//    public void registerIonTypeID(int id) {
//        this.getBaseFragment().get
//    }

    @Override
    public int getIonTypeID() {
        if (m_crosslinkedFragment instanceof PeptideIon)
            return getBaseFragment().getIonTypeID();
        else
            return BLikeDoubleFragmentation.getIonTypeStatic();
    }

    @Override
    public ArrayList<Loss.LossCount> getLossIDs() {
        return new ArrayList<Loss.LossCount>();
    }

    public double getSupportLevel(MatchedFragmentCollection mfc, int charge) {
        if (getFragmentationSites().length <= 1)
            return super.getSupportLevel(mfc, charge);
//        for (Fragment f : mfc.getFragments()) {
//            if ((!f.isClass(SecondaryFragment.class)) && !f.isClass(Loss.class)) {
//                if (f.getStart() == this.getStart() || this.getEnd() == this.getEnd())
//                for (int c = mfc.getMaxChargeState();c >= charge; c--) {
//                    MatchedBaseFragment mbf =  mfc.getMatchedFragmentGroup(f, c);
//                    if (mbf != null && mbf.isBaseFragmentFound())
//                        return SUPPORT_DOUBLE_SUPPORTED;
//                }
//            }
//        }
        return SUPPORT_DOUBLE_FRAGMENATION;
    }

}
