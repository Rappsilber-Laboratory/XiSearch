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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.utils.HashSetList;

/**
 * A collection of fragment groups.<br/>
 * A fragment group consists of a base fragment (A/B/C/X/Y/Z-ion) and all lossy
 * fragments of it
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class MatchedFragmentCollection implements Iterable<MatchedBaseFragment> {
    // curretnly hardcoded maximal charge state to be considered
    private int m_maxChargeState = 10;

 
    
    


    /**
     * map, that maps a fragment to the fragment group
     */
    private class MatchedBaseFragmentLookup extends HashMap<Fragment,MatchedBaseFragment> {
        private static final long serialVersionUID = -8068772341550345106L;
        public void free() {
            for (MatchedBaseFragment mbf : values()) 
                mbf.free();
            this.clear();
        }
    }

    ///**
    // * a list of all fragments - independent of charge state
    // */
    //private HashSet<Fragment> m_fragments = new HashSet<Fragment>();
    //private HashMap<Peptide,HashSet<Fragment>> m_peptideFragments = new HashMap<Peptide, HashSet<Fragment>>(2);

    private int m_matchedNonLossy = 0;
    private int m_matchedLossy = 0;


//    private void addMatchedFragment(Fragment f) {
//        Peptide p = f.getPeptide();
//        HashSet<Fragment> fragments = m_peptideFragments.get(p);
//        if (fragments == null) {
//            fragments = new HashSet<Fragment>();
//            m_peptideFragments.put(p, fragments);
//        }
//        fragments.add(f);
//        //m_fragments.add(f);
//    }

    /**
     * for each charge state create a lookup table for the fragments
     */
    MatchedBaseFragmentLookup[] m_list;


    public MatchedFragmentCollection(int MaxChargeState) {
        m_maxChargeState = MaxChargeState;
        m_list = new MatchedBaseFragmentLookup[MaxChargeState+1];
        
        {
            for (int i = 0; i< m_list.length; i++) {
                m_list[i] = new MatchedBaseFragmentLookup();
            }
        }
    }
    

    /**
     * returns the number of fragments in the list (each fragment is counted the
     * number of charge states it was seen in).
     * @return
     */
    public int size() {
        int s = 0;
        for (int i = 0; i < m_list.length; i++)
            s += m_list[i].size();
        return s;
    }



    /**
     * @return true: no fragments where registered; false otherwise
     */
    public boolean isEmpty() {
        for (int i = 0; i < m_list.length; i++)
            if (!m_list[i].isEmpty())
                return false;
        return true;
    }

    public SpectraPeak getMatchedPeak(Fragment f, int charge)  {

        Fragment search;

        if (f.isClass(Loss.class)) {

            search = ((Loss)f).getBaseFragment();

            if (m_list[charge].containsKey(search)) {
                return m_list[charge].get(search).getLosses().get(f);
            }
        } else {
            search = f;
            if (m_list[charge].containsKey(search)) {
                return m_list[charge].get(search).getBasePeak();
            }
        }
        return null;
    }

    /**
     * returns the group of fragments that belong to the same base ion (B/Y-ion)
     * @param f
     * @param charge
     * @return
     */
    public MatchedBaseFragment getMatchedFragmentGroup(Fragment f, int charge) {
        if (f.isClass(Loss.class))
            return m_list[charge].get(((Loss)f).getBaseFragment());
        else
            return m_list[charge].get(f);
    }

    /**
     * returns the group of fragments that belong to the same base ion (B/Y-ion)
     * @param f
     * @param charge
     * @return
     */
    public ArrayList<MatchedBaseFragment> getMatchedFragmentGroup(Fragment f) {
        Fragment search;
        ArrayList<MatchedBaseFragment> ret = new ArrayList<MatchedBaseFragment>();
        if (f.isClass(Loss.class))
            search = ((Loss)f).getBaseFragment();
        else
            search = f;

        for (int charge = 1; charge< m_list.length; charge++)
            if (m_list[charge].containsKey(search))
                ret.add(m_list[charge].get(search));

        return ret;
    }


    /**
     * tests whether there was a group of fragments observed that belong to the same base ion (B/Y-ion)
     * @param f
     * @param charge
     * @return
     */
    public boolean hasMatchedFragmentGroup(Fragment f, int charge) {
        if (f.isClass(Loss.class))
            return m_list[charge].containsKey(((Loss)f).getBaseFragment());
        else
            return m_list[charge].containsKey(f);
    }

    /**
     * tests whether there was a group of fragments observed that belong to the same base ion (B/Y-ion)
     * @param f
     * @return
     */
    public boolean hasMatchedFragment(Fragment f) {

        Fragment search;

        if (f.isClass(Loss.class)) {

            search = ((Loss)f).getBaseFragment();

            for (int charge = 1; charge< m_list.length; charge++) {
                if (m_list[charge].containsKey(search)) {
                    if (m_list[charge].get(search).getLosses().containsKey(f)) {
                        return true;
                    }
                }
            }

        } else {
            search = f;

            for (int charge = 1; charge< m_list.length; charge++) {
                if (m_list[charge].containsKey(search)) {
                    if (m_list[charge].get(search).isBaseFragmentFound()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * tests whether there was a group of fragments observed that belong to the same base ion (B/Y-ion)
     * @param f
     * @param charge 
     * @return
     */
    public boolean hasMatchedFragment(Fragment f, int charge) {

        Fragment search;

        if (f.isClass(Loss.class)) {

            search = ((Loss)f).getBaseFragment();

            if (m_list[charge].containsKey(search)) {
                if (m_list[charge].get(search).getLosses().containsKey(f)) {
                    return true;
                }
            }
        } else {
            search = f;
            if (m_list[charge].containsKey(search)) {
                if (m_list[charge].get(search).isBaseFragmentFound()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * tests whether there was a group of fragments observed that belong to the same base ion (B/Y-ion)
     * @param f
     * @return
     */
    public boolean hasMatchedFragmentGroup(Fragment f) {
        Fragment search;
        if (f.isClass(Loss.class))
            search = ((Loss)f).getBaseFragment();
        else
            search = f;

        for (int charge = 1; charge< m_list.length; charge++)
            if (m_list[charge].containsKey(search))
                return true;

        return false;
    }

    /**
     * tests whether there was a non-lossy fragment found that would be either
     * the given fragment or that the given fragment would be loss of.
     * @param f
     * @return
     */
    public boolean hasMatchedNonLossyFragment(Fragment f) {
        Fragment search;
        if (f.isClass(Loss.class))
            search = ((Loss)f).getBaseFragment();
        else
            search = f;

        for (int charge = 1; charge< m_list.length; charge++)
            if (m_list[charge].containsKey(search) && m_list[charge].get(search).isBaseFragmentFound())
                return true;

        return false;
    }



    /**
     * returns an iterator over all matched fragment-groups
     * @return
     */
    public Iterator<MatchedBaseFragment> iterator() {
        return new Iterator<MatchedBaseFragment>() {
            Iterator<MatchedBaseFragment> currentIterator;
            int nextList = 0;
            MatchedBaseFragment next = null;

            {
                do  {
                    currentIterator = m_list[nextList].values().iterator();
                    if (currentIterator.hasNext())
                        next = currentIterator.next();
                    nextList++;
                } while (next == null && nextList <m_list.length);

            }

            public boolean hasNext() {
                return next != null;
            }

            public MatchedBaseFragment next() {
                MatchedBaseFragment ret = next;
                if (currentIterator.hasNext()) {
                    next = currentIterator.next();
                } else {
                    if (nextList < m_list.length) {
                        do  {
                            next = null;
                            currentIterator = m_list[nextList].values().iterator();
                            if (currentIterator.hasNext())
                                next = currentIterator.next();
                            nextList++;
                        } while (next == null && nextList < m_list.length);
                    } else {
                        next = null;
                    }
                }
                return ret;

            }

            public void remove() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
    }

    /**
     * add all fragments from e
     * @param e
     * @return
     */
    public boolean add(MatchedBaseFragment e) {
        int charge = e.getCharge();
        return add(e, charge);
    }

    /**
     * add the matched fragments
     * @param e
     * @return
     */
    public boolean add(SpectraPeakMatchedFragment mf) {
        int charge = mf.getCharge();
        return add(mf.getFragment(), charge);
    }


    /**
     * add the matched fragments
     * @param e
     * @return
     */
    public boolean add(SpectraPeakMatchedFragment mf, SpectraPeak sp) {
        int charge = mf.getCharge();
        return add(mf.getFragment(), charge, sp);
    }

    protected boolean add(MatchedBaseFragment e, int charge) {
        if (charge>=m_list.length) {
            MatchedBaseFragmentLookup[] newlist = new MatchedBaseFragmentLookup[charge+1];
            System.arraycopy(m_list, 0, newlist, 0, m_list.length);
            for (int i = m_list.length; i<=charge; i++) {
                newlist[i]=new MatchedBaseFragmentLookup();
            }
            m_list=newlist;
            m_maxChargeState = charge;
            if (e.isBaseFragmentFound()) 
               m_matchedNonLossy++;

            m_list[charge].put(e.getBaseFragment(), e);
            m_matchedLossy+=e.getLosses().size();

            return true;
        } else if (m_list[charge].containsKey(e.getBaseFragment())) {
            MatchedBaseFragment ex = m_list[charge].get(e.getBaseFragment());
            Set<Loss> exLosses = ex.getLosses().keySet();
            for (Loss l : e.getLosses().keySet()) {
                if (!exLosses.contains(l)) {
                    ex.getLosses().put(l, e.getLosses().get(l));
//                    addMatchedFragment(l);
//                    m_fragments.add(e.getBaseFragment());
//                    m_peptideFragments
                }
            }
            if (e.isBaseFragmentFound()) {
                m_matchedNonLossy++;
                ex.setBaseFragmentFound(true);
//                addMatchedFragment(e.getBaseFragment());
            }
            m_matchedLossy+=e.getLosses().size();
            return true;
        } else {
            if (e.isBaseFragmentFound()) 
               m_matchedNonLossy++;

            m_list[charge].put(e.getBaseFragment(), e);
            m_matchedLossy+=e.getLosses().size();

//            for (Fragment f : e.getFragments()) {
//                addMatchedFragment(f);
//            }
            return true;
        }
    }


    /**
     * adds a lossy fragment to the list
     * @param l
     * @param charge
     * @param peak 
     * @return
     */
    public boolean add(Loss l, int charge, SpectraPeak peak) {
//        addMatchedFragment(l);
        m_matchedLossy++;

        if (m_list[charge].containsKey(l.getBaseFragment())) {
            MatchedBaseFragment ex = m_list[charge].get(l.getBaseFragment());
            ex.getLosses().put(l,peak);
            return true;
        } else {
            m_list[charge].put(l.getBaseFragment(), new MatchedBaseFragment(l.getBaseFragment(), charge, l, peak));
            return true;
        }
    }

    /**
     * adds a fragment to the list
     * @param f
     * @param charge
     * @param peak the peak it was matched to
     * @return
     */
    public boolean add(Fragment f, int charge, SpectraPeak peak) {
//        addMatchedFragment(f);
        if (f.isClass(Loss.class))
            return add((Loss) f, charge, peak);
        else  {
            m_matchedNonLossy++;
            if (m_list[charge].containsKey(f)) {
                if (m_list[charge].get(f).isBaseFragmentFound()) {
                    System.err.println("error here! tried to add the same fragment twice.");
                    new Exception().printStackTrace();
                }

                m_list[charge].get(f).setBaseFragmentFound(peak);
            } else {
                MatchedBaseFragment m = new MatchedBaseFragment(f, charge, peak);
                m_list[charge].put(f, m);
            }
            return true;
        }
    }

    /**
     * adds a fragment to the list
     * @param f
     * @param charge
     * @return
     */
    public boolean add(Fragment f, int charge) {
        return add(f,charge,null);
//        if (f instanceof Loss)
//            return add((Loss) f, charge, null);
//        else  {
//            if (m_list[charge].containsKey(f)) {
//                m_list[charge].get(f).setBaseFragmentFound();
//            } else {
//                MatchedBaseFragment m = new MatchedBaseFragment(f, charge);
//                m.setBaseFragmentFound();
//                m_list[charge].put(f, m);
//            }
//            return true;
//        }
    }

    /**
     * removes a fragment
     * @param o
     * @param charge
     * @return true, if the fragment was removed
     */
    public boolean remove(Object o, int charge) {
        if (o instanceof Loss) {

            MatchedBaseFragment m = m_list[charge].get(((Loss) o).getBaseFragment());
            if (m != null) {
                boolean ret = m.getLosses().remove((Loss)o) != null;
                if (m.getLosses().isEmpty() && !m.isBaseFragmentFound())
                    m_list[charge].remove(((Loss) o).getBaseFragment());
                m_matchedLossy--;
                return ret;
            } else {
                return false;
            }
        } else if(o instanceof Fragment) {
            m_matchedNonLossy--;
            MatchedBaseFragment mbf =  m_list[charge].get(o);
            if (mbf.getLosses().size()>0) {
                mbf.unsetBaseFragmentFound();
                return true;
            } else {
                return m_list[charge].remove((Fragment)o) != null;
            }
        } else if(o instanceof MatchedBaseFragment) {
            MatchedBaseFragment mbf = (MatchedBaseFragment) o;
            boolean ret = m_list[charge].remove(mbf.getBaseFragment()) != null;
            if (mbf.isBaseFragmentFound())
                m_matchedNonLossy--;
            m_matchedLossy -= mbf.getLosses().size();
            return ret;
        } else {
            throw new UnsupportedOperationException("can't objects of type " + o.getClass().getName());
        }
    }

    /**
     * removes a fragment
     * @param o
     * @param charge
     * @return true, if a fragment was removed
     */
    public boolean remove(Object o) {
//        if (o instanceof Fragment)
//            addMatchedFragment((Fragment) o);

        boolean ret = false;
        for (int charge = 1; charge<m_list.length; charge++)
            ret = (remove(o, charge)) || ret;
        return ret;
    }


    /**
     * add all matched fragment groups in the collection
     * @param c
     * @return
     */
    public boolean addAll(Collection<? extends MatchedBaseFragment> c) {
        boolean ret = false;
        for (MatchedBaseFragment o : c) {
            ret = add(o) || ret;
        }
        return ret;
    }

    /**
     * add all matched fragment groups in the collection
     * @param c
     * @return
     */
    public boolean addAll(Iterable<MatchedBaseFragment> c) {
        boolean ret = false;
        for (MatchedBaseFragment o : c) {
            ret = add(o) || ret;
        }
        return ret;
    }    
    
//    /**
//     * a list of all fragments - independent of charge state
//     * @return the fragments
//     */
//    public HashSet<Fragment> getFragments() {
//        return m_fragments;
//    }

//    /**
//     * a list of all fragments - independent of charge state
//     * @return the fragments
//     */
//    public HashMap<Peptide,HashSet<Fragment>> getPeptideFragments() {
//        return m_peptideFragments;
//    }

    public MatchedFragmentCollection toSinglyCharged() {
        MatchedFragmentCollection mfc = new MatchedFragmentCollection(1);

        for (MatchedBaseFragment mbf : this) {
            MatchedBaseFragment cmbf = mbf.clone();
            cmbf.setCharge(1);
            mfc.add(cmbf);
        }
        return mfc;
    }

    /**
     * creates a list with all matches merged into singly charged fragment ions
     * @param minLosses only merge back match-groups with a found basefragment or at least these amount of losses
     * @return
     */
    public MatchedFragmentCollection toSinglyCharged(int minLosses) {
        MatchedFragmentCollection mfc = new MatchedFragmentCollection(1);

        for (MatchedBaseFragment mbf : this) 
            if (mbf.isBaseFragmentFound() || mbf.getLosses().size() >= minLosses) {

                MatchedBaseFragment cmbf = mbf.clone();
                cmbf.setCharge(1);
                mfc.add(cmbf);
            }
        return mfc;
    }

    public Fragment[] getFragments() {
        HashSetList<Fragment> frags = new HashSetList<Fragment>();
        for (MatchedBaseFragment mbf : this)  {
            frags.addAll(mbf.getFragments());
        }
        return frags.toArray(new Fragment[0]);

    }




    /**
     * the number of nonlossy fragments matched
     * @return count of non lossy fragments
     */
    public int getMatchedNonLossy() {
        return m_matchedNonLossy;
    }

    /**
     * the number of lossy fragments matched
     * @return count of lossy fragments
     */
    public int getMatchedLossy() {
        return m_matchedLossy;
    }

    //    public boolean removeAll(Collection<?> c) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }

//    public boolean retainAll(Collection<?> c) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }

    /**
     * small function to ease the live of the garbage collector
     */
    public void free() {
        //m_fragments.clear();

//        for (HashSet<Fragment> f : m_peptideFragments.values())
//            f.clear();
//
//        m_peptideFragments.clear();

        for (int i = 0; i< m_list.length; i++) {
            m_list[i].free();
        }
    }

    public int getMaxChargeState(){
        return m_maxChargeState;
    }
    
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (MatchedBaseFragment mbf : this) {
            if (mbf.isBaseFragmentFound()) {
                sb.append("|");
                sb.append(mbf.getBaseFragment().name());
            }
            for (Loss l : mbf.getLosses().keySet()) {
                sb.append("|");
                sb.append(l.name());
            }
        }
        return sb.substring(1);
    }
    
    public void clear() {
        for (MatchedBaseFragmentLookup mbfl : m_list) {
            mbfl.clear();
        }
    }

}
