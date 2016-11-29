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

//import rappsilber.ms.sequence.ions.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.RunConfig;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.ions.CrosslinkedFragment;
import rappsilber.ms.sequence.ions.CrosslinkerContaining;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.DoubleFragmentation;
import rappsilber.ms.sequence.ions.PeptideIon;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class CrosslinkerModified extends Loss implements CrosslinkerContaining {

    private static int m_LossID = -1;
    private CrossLinker m_crosslinker;
    private Fragment m_fragment;

    public static class CrosslinkerModifiedRest extends Loss {
        private Fragment rest;
        protected CrosslinkerModifiedRest(Fragment basefragment, Fragment rest, int LossID, String name) {
            super(basefragment, rest.getMass() - basefragment.getMass(), LossID);
            m_lossCount = 1;
            m_name = name;
            this.rest = rest;
        }
        
        @Override
        public String toString() {
            return rest.toString();
        }
        
        
        @Override
        public boolean canFullfillXlink(HashMap<Peptide, Integer> sites) {
            for (Peptide p: sites.keySet()) {
                if (p == getPeptide()) {
                    int x = sites.get(p);
                    if (getStart() > x || getEnd() < x)
                        return false;
                }
            }
            return true;
        }

        public boolean canFullfillXlink(Peptide p, int site) {
            if (p == getPeptide()) {
                if (getStart() > site || getEnd() < site)
                    return false;
            }
            return true;
        }

        public boolean canFullfillXlink(Peptide pep1, int site1, Peptide pep2, int site2) {
            if (pep1 == getPeptide()) {
                if (getStart() > site1 || getEnd() < site1)
                    return false;
            } else  if (pep2 == getPeptide()) {
                if (getStart() > site2 || getEnd() < site2)
                    return false;
            }
            return true;
        }
    
        @Override
        public String name() {
            return m_name;
        }

    }

    protected CrosslinkerModified(Fragment baseFragment, Fragment fragment, CrossLinker crosslinker, Fragment losingFragment, String name) {
        super(baseFragment, (fragment.getMass() + crosslinker.getCrossLinkedMass())-baseFragment.getMass(), m_LossID);
//        m_LossyMass = -crosslinker.getSinglyLinekdMass();
//        m_massDiff = -losingFragment.getNeutralMass();
//        m_massDiff = -(fragment.getMass() + crosslinker.getCrossLinkedMass());
        m_lossCount = 1;
        m_name = name;
        m_crosslinker = crosslinker;
        this.m_fragment = fragment;
    }

    protected CrosslinkerModified(Fragment baseFragment, Fragment fragment, CrossLinker crosslinker, Fragment losingFragment, String name, double immoniumMass) {
        this(baseFragment, fragment, crosslinker, losingFragment, name);
        m_massDiff-=immoniumMass;
        m_mass += immoniumMass;
    }
    
    protected CrosslinkerModified() {};

    
    @Override
    public String toString() {
        return m_fragment.toString() + " + (P)";
    }    

//    public static Class getMyClass() {
//        return new CrosslinkerModified().getClass();
//    }

    public static ArrayList<Fragment> createLossyFragments(ArrayList<Fragment> fragments, CrossLinker crosslinker, boolean insert) {
                              //         createLossyFragments(java.util.ArrayList, boolean)
        ArrayList<Fragment> ret = new ArrayList<Fragment>(fragments.size());
        if (crosslinker == null)
            return ret;
        ArrayList<Fragment> base = fragments;
        if (DoubleFragmentation.isEnabled()) {
            for (Fragment f : base) {
//                ret.add(new CrosslinkerModified(f, crosslinker));
                if (f instanceof CrosslinkedFragment) {
                    CrosslinkedFragment cf = (CrosslinkedFragment) f;
                    Fragment b = cf.getBaseFragment();
                    Fragment x = cf.getCrossLinkedFragment();

                    if (b instanceof PeptideIon && x instanceof PeptideIon) {
                            ret.add(new CrosslinkerModified(f,b, crosslinker, x, b.name() + "+(" + x.name() + ")"));
                            ret.add(new CrosslinkerModifiedRest(f,b, CrosslinkerModified.m_LossID, b.name() + "(+" + x.name() + ")"));
                            ret.add(new CrosslinkerModified(f,x, crosslinker, b, x.name() + "+(" + b.name() + ")"));
                            ret.add(new CrosslinkerModifiedRest(f,x, CrosslinkerModified.m_LossID, x.name() + "(+" + b.name() + ")"));
                    } else {
                        if ((x instanceof PeptideIon)) {
                            ret.add(new CrosslinkerModified(f,b, crosslinker, x, b.name() + "+(" + x.name() + ")"));
                            ret.add(new CrosslinkerModifiedRest(f,b, CrosslinkerModified.m_LossID, b.name() + "(+" + x.name() + ")"));
                        } else if ((b instanceof PeptideIon)) {
                            ret.add(new CrosslinkerModified(f,x, crosslinker, b,x.name() + "+(" + b.name() + ")"));
                            ret.add(new CrosslinkerModifiedRest(f,x, CrosslinkerModified.m_LossID,x.name() + "(+" + b.name() + ")"));
                        }
                    }

                } 
            }
        } else
            for (Fragment f : base) {
                if (f instanceof CrosslinkedFragment) {
                    CrosslinkedFragment cf = (CrosslinkedFragment) f;
                    Fragment b = cf.getBaseFragment();
                    Fragment x = cf.getCrossLinkedFragment();

                    if (b instanceof PeptideIon && x instanceof PeptideIon) {
                            ret.add(new CrosslinkerModified(f,b, crosslinker, x, b.name() + "+(" + x.name() + ")"));
                            ret.add(new CrosslinkerModifiedRest(f,b, CrosslinkerModified.m_LossID, b.name() + "(+" + x.name() + ")"));
                            ret.add(new CrosslinkerModified(f,x, crosslinker, b, x.name() + "+(" + b.name() + ")"));
                            ret.add(new CrosslinkerModifiedRest(f,x, CrosslinkerModified.m_LossID, x.name() + "(+" + b.name() + ")"));
                            
                            // dirty hack
                            ret.add(new CrosslinkerModified(f,b, crosslinker, x, b.name() + "+i(" + x.name() + ")",81.05784922929));
                            ret.add(new CrosslinkerModified(f,x, crosslinker, b, x.name() + "+i(" + b.name() + ")",81.05784922929));
                    }
                }
            }
        if (insert)
            fragments.addAll(ret);
        return ret;
    }
    public static ArrayList<Fragment> createLossyFragments(ArrayList<Fragment> fragments, CrossLinker crosslinker, boolean insert    , RunConfig conf) {
                              //         createLossyFragments(java.util.ArrayList, boolean)
        return createLossyFragments(fragments, crosslinker, insert);
    }


    public static boolean register() {
        try {
            Loss.registerLossClass(CrosslinkerModified.class);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(CrosslinkerModified.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }



    @Override
    public String name() {
        return m_name;
    }

    public boolean canFullfillXlink(HashMap<Peptide, Integer> sites) {
        for (Peptide p: sites.keySet()) {
            if (p == getPeptide()) {
                int x = sites.get(p);
                if (getStart() > x || getEnd() < x)
                    return false;
            }
        }
        return true;
    }

    public boolean canFullfillXlink(Peptide pep1, int site1, Peptide pep2, int site2) {
        if (pep1 == getPeptide()) {
            if (getStart() <= site1 && getEnd() >= site1)
                return false;
        } else  if (pep2 == getPeptide()) {
            if (getStart() <= site2 && getEnd() >= site2)
                return false;
        }
        return true;
    }
    
    public boolean canFullfillXlink(Peptide p, int site) {
        if (p == getPeptide()) {
            if (getStart() > site || getEnd() < site)
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
