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



import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.RunConfig;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.AminoAcidSequence;
import rappsilber.ms.sequence.AminoModification;
import rappsilber.ms.sequence.NonAminoAcidModification;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.SequenceUtils;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.match.MatchedBaseFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.utils.Util;


// <editor-fold defaultstate="collapsed" desc=" UML Marker "> 
// #[regen=yes,id=DCE.6329B271-A4CA-8A7A-42EE-C9E08C54DCD9]
// </editor-fold> 
public abstract class Fragment implements AminoAcidSequence {

    public static final double SUPPORT_NONLOSSY = 500;
    public static final double SUPPORT_SUPPORTING_NONLOSSY = 50;
    public static final double SUPPORT_LOSSY = 20;
    public static final double SUPPORT_LOSSY_SUPPORTED_NONLOSSY = 50;
    public static final double SUPPORT_LOSSY_SUPPORTED_LOSSY = 40;
    public static final double SUPPORT_DOUBLE_FRAGMENATION = 15;
    public static final double SUPPORT_DOUBLE_SUPPORTED = 30;
    public static final double SUPPORT_OTHERS = 0;

    private NonAminoAcidModification m_nterminal_modification = NonAminoAcidModification.NO_MODIFICATION;
    private NonAminoAcidModification m_cterminal_modification = NonAminoAcidModification.NO_MODIFICATION;


    /** the neutral mass of the fragmentPrimary */
    protected double m_mass;

    private double m_massDifference;

    /** the length og the fragmentPrimary */
    private short m_length;

    /** where in the peptide sequence does the fragmentPrimary start */
    private int m_start;

    protected String m_name;

    /** fragmentPrimary belongs to what peptide */
    private Peptide m_peptide;

    private long m_id = -1;

    private static ArrayList<Method> m_fragments = new ArrayList<Method>();


    public static ArrayList<Fragment> fragment(Peptide p, boolean includeSecondary) {
        return fragment(p, m_fragments, includeSecondary);
    }


    public static ArrayList<Fragment> fragment(Peptide p, RunConfig conf, boolean includeSecondary) {
        ArrayList<Method> fragments = conf.getFragmentMethods();
        return fragment(p, fragments, includeSecondary);
    }

    private static ArrayList<Fragment> fragment(Peptide p, ArrayList<Method> fragments, boolean includeSecondary) {
        ArrayList<Fragment> returnList = new ArrayList<Fragment>();
        // call each registered fragmentation function
        for (Method m : fragments) {
            Object ret = null;
            try {
                if (includeSecondary || !SecondaryFragment.class.isAssignableFrom(m.getDeclaringClass())) {
                    ret = m.invoke(null, p);
                }

            } catch (IllegalAccessException ex) { //<editor-fold desc="and some other" defaultstate="collapsed">
                Logger.getLogger(Fragment.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(Fragment.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvocationTargetException ex) {
                Logger.getLogger(Fragment.class.getName()).log(Level.SEVERE, null, ex);//</editor-fold>
            }
            if (ret != null && ret instanceof ArrayList) {
                returnList.addAll((ArrayList<Fragment>)ret);
            }
        }
        return returnList;
    }

    public static void registerFragmentClass(Class<? extends Fragment> c) throws NoSuchMethodException {
            Method m = c.getMethod("fragment", Peptide.class);
            if (!m_fragments.contains(m))
                m_fragments.add(m);
    }

    public static void registerFragmentClass(Class<? extends Fragment> c, RunConfig conf) throws NoSuchMethodException {
        ArrayList<Method> fragments = conf.getFragmentMethods();
        Method m = c.getMethod("fragment", Peptide.class);
        if (!fragments.contains(m))
            fragments.add(m);
    }


    public static void registerSecondaryFragmentClass(Class<? extends Fragment> c, RunConfig conf) throws NoSuchMethodException {
        ArrayList<Method> fragments = conf.getFragmentMethods();
        Method m = c.getMethod("fragment", Peptide.class);
        if (!fragments.contains(m))
            fragments.add(m);
    }

    public static void includeSecondaryFragmentations(RunConfig conf) {
        conf.getFragmentMethods().addAll(conf.getSecondaryFragmentMethods());
    }



    protected Fragment() {
        
    }
    /**
     * Creates a fragment from the peptide with the defined part of the peptide.
     * The mass difference is calculated as difference to a B-Ion
     * @param peptide
     * @param Start
     * @param length
     * @param weightDiff 
     */
    public Fragment (Peptide peptide, int Start, int length, double weightDiff) {
        double weight = 0;
        m_peptide = peptide;

        this.m_length = (short)length;
        this.m_start = Start;

        for (int i = Start + length; --i >= Start; ) {
            weight += peptide.aminoAcidAt(i).mass;
        }

//        if (Start == 0) {
//            m_nterminal_modification = peptide.getNTerminalModification();
//            weight += m_nterminal_modification.Mass;
//        }
//
//        if (Start+length == peptide.length()) {
//            m_cterminal_modification = peptide.getCTerminalModification();
//            weight += m_cterminal_modification.Mass;
//        }

        // include the difference do to fragmentation
        weight += weightDiff;
        m_mass = weight;
        m_massDifference = weightDiff;
        if (Double.isNaN(m_mass))
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING,"Mass is NaN");

    }

    public Fragment (Fragment f, double totalWeight) {
        double weight = 0;
        m_peptide = f.getPeptide();

        this.m_length = (short) f.length();
        this.m_start = f.getStart();

        for (int i = m_start + m_length; --i >= m_start; ) {
            weight += m_peptide.aminoAcidAt(i).mass;
        }
        // include the difference do to fragmentation
        m_mass = totalWeight;
        m_massDifference = totalWeight - weight;
        if (Double.isNaN(m_mass))
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING,"Mass is NaN");

    }


    /**
     * returns a list of fragmentation sites
     * @return
     */
    public FragmentationSite[] getFragmentationSites() {
        FragmentationSite[] fs;
        if (m_start > 0) {
            if (m_start + m_length < m_peptide.length()) {
                fs = new FragmentationSite[2];
                fs[0] = new FragmentationSite(m_peptide,
                        m_start - 1);
                fs[1] = new FragmentationSite(m_peptide,
                        m_start + m_length - 1);
            } else {
                fs = new FragmentationSite[1];
                fs[0] = new FragmentationSite(m_peptide,
                        m_start - 1);
            }

        } else if (m_length < m_peptide.length()) {
                fs = new FragmentationSite[1];
                fs[0] = new FragmentationSite(m_peptide,
                        m_start + m_length - 1);
        } else
            return new FragmentationSite[0];
        
        return fs;
    }

    /**
     * returns whether the fragment has only one fragmentation site
     * @return
     */
    public boolean isBasicFragmentation() {
        if (m_start > 0) {
            return m_start + m_length == m_peptide.length();
        } else 
            return m_length>0 && m_length < m_peptide.length();
        
    }
    
    /** 
     * returns the mass the fragmentPrimary would have in the given charge state
     * @param charge
     * @return
     */
    public double getMass (int charge) {
        return m_mass + (Util.PROTON_MASS * charge);
    }

    /** 
     * returns the mass the fragmentPrimary would have in the given charge state
     * @param charge
     * @return
     */
    public double getMZ (int charge) {
        return getMass(charge)/ (double)charge;
    }

    /** returns the mass the fragmentPrimary would have singlely charged */
    public double getMass () {
        return m_mass + Util.PROTON_MASS;
    }

    /** returns the neutral mass the fragmentPrimary */
    public double getNeutralMass () {
        return m_mass;
    }

    /** defines the neutral mass the fragmentPrimary */
    public void setMass (int val) {
        this.m_mass = val;
    }

    public Peptide getPeptide() {
        return m_peptide;
    }

    public int getStart() {
        return m_start;
    }

    public short getEnd() {
        return (short)(m_start + m_length - 1);
    }

    public double getMassDifference() {
        return m_massDifference;
    }


    @Override
    public String  toString() {

        StringBuilder out = new StringBuilder(m_length);
        out.append(m_nterminal_modification.toString());
        for (int i = 0 ; i< m_length; i++)
            out.append(m_peptide.aminoAcidAt(m_start + i));
        out.append(m_cterminal_modification.toString());
        return out.toString();
    }


    public AminoAcid aminoAcidAt(int pos) {
        try {
            return m_peptide.aminoAcidAt(pos + m_start);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public AminoAcid nonLabeledAminoAcidAt(int pos) {
        try {
            return m_peptide.nonLabeledAminoAcidAt(pos + m_start);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public boolean containsAminoAcid(AminoAcid aa) {
        return SequenceUtils.containsAminoAcid(this, aa);
    }

    public Fragment subSequence(short from, short length) {
        BLikeDoubleFragmentation blf = new BLikeDoubleFragmentation(m_peptide, (short) (m_start + from), length);
        return blf;
    }


    public boolean isNTerminal() {
        return m_start == 0;
    }

    public boolean isCTerminal() {
        return m_start + m_length == m_peptide.length();
    }


    public int length() {
        return m_length;
    }

    public String name() {
        return m_name + m_length;
    }

    /**
     * returns whether the fragmentPrimary is of the given class
     * @param c
     * @return
     */
    public boolean isClass(Class c) {
        return c.isInstance(this);
    }


    public int countAminoAcid(HashSet<AminoAcid> aas) {
        return SequenceUtils.countAminoAcid(this, aas);
    }
    
    public boolean containsAminoAcids(HashSet<AminoAcid> aas) {
        return SequenceUtils.containsAminoAcid(this, aas);
    }

    public void free() {

        m_name = null;

        m_peptide=null;


    }

    public AminoAcid[] toArray() {
        AminoAcid[] aas = new AminoAcid[m_length];
        for (int i = m_length - 1 ; --i >= 0;)
            aas[i] = m_peptide.aminoAcidAt(m_start + i);

        return aas;
    }

    public AminoAcid setAminoAcidAt(int pos, AminoAcid aa) {
        throw new UnsupportedOperationException("the sequence of fragment ion can't be modified");
    }

    public int replace(AminoAcid oldAA, AminoAcid newAA) {
        int found = 0;
        int length = length();
        for (int i=0; i < length; i++ ) {
            if (aminoAcidAt(i) == oldAA) {
                setAminoAcidAt(i, newAA);
                found++;
            }
        }
        m_mass += (newAA.mass - oldAA.mass) * found;
        return found;
    }

    public int replace(AminoModification AAM) {
        return replace(AAM.BaseAminoAcid, AAM);
    }


//    public Class[] getBaseFragmentsClases() {
//        return new Class[] {AIon.class, BIon.class, CIon.class,XIon.class, YIon.class, ZIon.class};
//    }

    @Override
    public int hashCode() {
        int hash = getStart();
        hash = 13 * hash + this.name().hashCode();
        hash = 13 * hash + this.toString().hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (o.getClass() == this.getClass()) {
            if (this.getNeutralMass() == ((Fragment)o).getNeutralMass() && this.getPeptide() == ((Fragment)o).getPeptide() && this.toString().contentEquals(o.toString())) {
                return true;
            }
        }
        return false;
    }

    public static void parseArgs(String args, RunConfig conf) throws ParseException {



            // Strip the string of whitespace and make it uppercase for comparison
            String x = (args.trim());
            String[] xargs = x.split(";");
            String classname = xargs[0];
            HashMap<String,String> OptionalArgs = new HashMap<String, String>();
            for (int a = 1; a<xargs.length;a++) {
                String[] aa = xargs[a].split(":",2);
                String key = aa[0].trim().toLowerCase();
                String v = null;
                if (aa.length>1)
                    v=aa[1];
                OptionalArgs.put(key, v);
            }

            try {
                Class c;
                if (classname.contains("."))
                    c = Class.forName(classname);
                else
                    c = Class.forName("rappsilber.ms.sequence.ions."+classname);

                if (DoubleFragmentation.class.isAssignableFrom(c)) {
                    DoubleFragmentation.setEnable(true);
                }

                if (c.isAssignableFrom(SecondaryFragment.class))
                    registerSecondaryFragmentClass(c.asSubclass(Fragment.class), conf);
                else
                    registerFragmentClass(c.asSubclass(Fragment.class), conf);

                try {
                    if (OptionalArgs.containsKey("id")) {
                        Method m  =c.getMethod("registerIonTypeID", int.class);
                        m.invoke(null, Integer.parseInt(OptionalArgs.get("id")));
                    }
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(Fragment.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(Fragment.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(Fragment.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NoSuchMethodException ex) {
                    Logger.getLogger(Fragment.class.getName()).log(Level.SEVERE, "Fragment-class \"" + classname + "\" does has no register-method", ex);
                    throw new ParseException("Fragment-class \"" + classname + "\" does has no register-method", 0);
                }

            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Fragment.class.getName()).log(Level.SEVERE, "Fragment-class not found:" + classname, ex);
                throw new ParseException("Fragment-class not found:" + classname, 0);
            } catch (NoSuchMethodException ex) {
                Logger.getLogger(Fragment.class.getName()).log(Level.SEVERE, "Fragment-class \"" + classname + "\" does has no register-method", ex);
                throw new ParseException("Fragment-class \"" + classname + "\" does has no register-method", 0);
            }
    }

    public boolean canFullfillXlink(HashMap<Peptide, Integer> sites) {
        for (Peptide p: sites.keySet()) {
            if (p == getPeptide()) {
                int site = sites.get(p);
                if (getStart() <= site && getEnd() >= site)
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
            if (getStart() <= site && getEnd() >= site)
                return false;
        }
        return true;
    }

    public AminoAcidSequence getSourceSequence() {
        return getPeptide().getSourceSequence();
    }


    public long getID() {
        return m_id;
    }

    public void setID(long id) {
        m_id = id;
    }

    public int getIonTypeID() 
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ArrayList<Loss.LossCount> getLossIDs()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
//    public double[] complementaryLinearMasses() {
//
//    }

//    public abstract String getArgValue(String name);
    /**
     * Returns some measure of support for a match within th set of matches
     * @return a numeric value representing the support the smaller the lower
     *          standard values:
     */
    public double getSupportLevel(MatchedFragmentCollection mfc, int charge) {
        MatchedBaseFragment mbf = mfc.getMatchedFragmentGroup(this, charge);
//        if (mbf == null) {
//            System.err.println("error here! no matched base fragment");
//        }
        HashMap<Loss,SpectraPeak> losses = mbf.getLosses();
        int lossCount = losses.size();
        if (this.isClass(Loss.class)) {
            if (mbf.isBaseFragmentFound())
                return SUPPORT_LOSSY_SUPPORTED_NONLOSSY;
            else if (lossCount > 2) {
                return SUPPORT_LOSSY_SUPPORTED_LOSSY;
            } else
                return SUPPORT_LOSSY;
        } else if (this.isClass(SecondaryFragment.class)) {
            return SUPPORT_OTHERS;
        } else if (lossCount > 0) {
            return SUPPORT_NONLOSSY + lossCount * SUPPORT_LOSSY;
        } else
            return SUPPORT_NONLOSSY;
    }

    public double getBaseSupportLevel() {
        return SUPPORT_NONLOSSY;
    }

    @Override
    public Iterator<AminoAcid> iterator() {
        return new Iterator<AminoAcid>() {
            int current = 0;
            @Override
            public boolean hasNext() {
                return current < length();
            }

            @Override
            public AminoAcid next() {
                if (current < length())
                    return aminoAcidAt(current++);
                return null;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }

}

