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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.RunConfig;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.sequence.Peptide;
import rappsilber.utils.Util;

/**
 * represents the creation of AIons as a Loss of CO from BIon (12 + 15.99491463  = 27.99491463)
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class CleavableCrossLinkerPeptide extends Loss implements CrossLinkedFragmentProducer{

    private static int m_LossID = -1;
    private static int s_IonTypeID = -1;
    String name;
    double deltamass;


    
    
    public class CleavableCrossLinkerPeptideFragment extends Fragment implements CrosslinkerContaining{
        private Fragment parent;
        double deltamass;
        int id = -1;
        
        public CleavableCrossLinkerPeptideFragment (Fragment f, double deltaMass) {
            super(f, f.getNeutralMass()+deltaMass);
            this.parent = f;
            this.deltamass = deltaMass;
            
        }        
        
        @Override
        public String name() {
            return parent.name()+"_"+name;
        }        
        
        @Override
        public int getIonTypeID() {
            return id;
        }

        @Override
        public ArrayList<LossCount> getLossIDs() {
            return new ArrayList<LossCount>();
        }    
        
        
        @Override
        public boolean canFullfillXlink(HashMap<Peptide, Integer> sites) {
            for (Map.Entry<Peptide,Integer> p: sites.entrySet()) {
                if (!canFullfillXlink(p.getKey(), p.getValue()))
                    return false;
            }
            return true;
        }

        @Override
        public boolean canFullfillXlink(Peptide p, int site) {
            if (parent.getPeptide() == p) {
                    return (site >=getStart() && site <= getEnd());
            }
            return true;
        }

        @Override
        public boolean canFullfillXlink(Peptide p1, int site1, Peptide p2, int site2) {
            return canFullfillXlink(p1, site1) && canFullfillXlink(p2, site2);
        }

        /**
         * @return the parent
         */
        public Fragment getParent() {
            return parent;
        }
        
    }
    

    /** need this for getting the class in a static function */
    protected CleavableCrossLinkerPeptide() {

    }
    
    /**
     * creates a new fragment, that represents a loss from a b-ion to an a-ion
     *
     * @param f a fragment that is a b-ion or a loss to a b-ion
     */
    public CleavableCrossLinkerPeptide(double deltaMass) {
        this.deltamass = deltaMass;
    }

    /**
     * creates a new fragment, that represents a loss from a b-ion to an a-ion
     *
     * @param f a fragment that is a b-ion or a loss to a b-ion
     */
    public CleavableCrossLinkerPeptide(double deltaMass, String name) {
        this(deltaMass);
        this.name = name;
    }
    
    
    @Override
    public ArrayList<Fragment> createCrosslinkedFragments(Collection<Fragment> fragments, Fragment Crosslinked, CrossLinker crosslinker, boolean noPeptideIons) {
        ArrayList<Fragment> ret = new ArrayList<Fragment>(fragments.size());
        for (Fragment f : fragments) {
            if (!(f instanceof CrosslinkerContaining))
                ret.add(new CleavableCrossLinkerPeptideFragment(f, deltamass));
        }
        return ret;
                
    }

    @Override
    public ArrayList<Fragment> createCrosslinkedFragments(Collection<Fragment> fragments, Collection<Fragment> Crosslinked, CrossLinker crosslinker, boolean noPeptideIons) {
        ArrayList<Fragment> ret = new ArrayList<Fragment>(fragments.size()+Crosslinked.size());
        for (Fragment f : fragments) {
            if (!(f instanceof CrosslinkerContaining))
                ret.add(new CleavableCrossLinkerPeptideFragment(f, deltamass));
        }
        for (Fragment f : Crosslinked) {
            if (!(f instanceof CrosslinkerContaining))
                ret.add(new CleavableCrossLinkerPeptideFragment(f, deltamass));
        }
        return ret;
    }

    @Override
    public ArrayList<Fragment> createCrosslinkedFragments(Collection<Fragment> fragments, Collection<Fragment> Crosslinked, CrossLinker crosslinker, int linkSite1, int linkSite2) {
        ArrayList<Fragment> ret = new ArrayList<Fragment>(fragments.size()+Crosslinked.size());
        Peptide p = Crosslinked.iterator().next().getPeptide();
        PeptideIon pi = new PeptideIon(p);
        for (Fragment f : fragments) {
            if (f.getStart() <= linkSite1 && linkSite1 <= f.getEnd() && !f.isClass(CrosslinkerContaining.class)
                && crosslinker.canCrossLink(pi, linkSite2, f, linkSite1-f.getStart()))
                ret.add(new CleavableCrossLinkerPeptideFragment(f, deltamass));
        }
        p = fragments.iterator().next().getPeptide();
        pi = new PeptideIon(p);
        for (Fragment f : Crosslinked) {
            if (f.getStart() <= linkSite2 && linkSite2 <= f.getEnd() && !f.isClass(CrosslinkerContaining.class)
                && crosslinker.canCrossLink(pi, linkSite1, f, linkSite2-f.getStart()))
                ret.add(new CleavableCrossLinkerPeptideFragment(f, deltamass));
        }
        return ret;
    }    

    

    public static ArrayList<Fragment> createLossyFragments(ArrayList<Fragment> frags, CrossLinker crosslinker, boolean insert) {
        return new ArrayList<Fragment>(0);
    }

    public static ArrayList<Fragment> createLossyFragments(ArrayList<Fragment> frags, CrossLinker crosslinker, boolean insert, RunConfig conf) {
        return new ArrayList<Fragment>(0);
    }

    public static boolean register() {
        try {
            Loss.registerLossClass(CleavableCrossLinkerPeptide.class);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(CleavableCrossLinkerPeptide.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    public static void parseArgs(String args, RunConfig conf) throws ParseException {
        String[] argParts = args.split(";");
        Double mass = null;
        String name = null;
        Integer id=null;

        // parses something like: deltamass:x;name:y
        for (String arg : argParts) {
            String[] ap = arg.split(":");
            String aName = ap[0].trim().toLowerCase();
            if (aName.contentEquals("id")) {
                id = Integer.parseInt(ap[1].trim());
            } else if (aName.contentEquals("name")) {
                name=ap[1].trim();
            } else if (aName.contentEquals("mass")) {
                mass =Double.parseDouble(ap[1].trim());
                if (name == null) {
                    name = Util.twoDigits.format(mass);
                }
            }
        }
        if (mass == null) {
            throw new ParseException("CleavableCrossLinkerPeptide defined without mass:" +args, 0);
        }
        conf.getAlphaCandidateDeltaMasses().add(mass);
        CleavableCrossLinkerPeptide p = new CleavableCrossLinkerPeptide(mass, name);
        if (id != null)
            p.setID(id);
        conf.addCrossLinkedFragmentProducer(p,true);
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
