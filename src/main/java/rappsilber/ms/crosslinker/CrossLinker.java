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
package rappsilber.ms.crosslinker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.ConfigurationParserException;
import rappsilber.config.RunConfig;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.AminoAcidSequence;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.digest.Digestion;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.statistics.utils.UpdateableDouble;
import rappsilber.utils.ObjectWrapper;

/**
 * provides the base class and methodes for all crosslinker
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public abstract class CrossLinker {

    /** the name of the crosslinker */
    private String m_name;
    /** what is the non-crosslinked mass of the crosslinker- currently not used */
    private double m_baseMass;
    /** what is minimal mass of the cross-linked cross-linker */
    private double m_crossLinkedMinMass;
    /** what is maximal mass of the cross-linked cross-linker */
    private double m_crossLinkedMaxMass;
    /** is this cross-linker used to describe decoy-matches*/
    private boolean m_isDecoy;

    private int m_dbID;

    /** a list of all know instances of crosslinker */
    protected static HashMap<String,CrossLinker> m_AllCrossLinker = new HashMap<String, CrossLinker>();

    public CrossLinker() {
    }



    /** 
     * creates a new crosslinker, and registers it with in the list
     * @param name
     * @param BaseMass
     * @param CrossLinkedMass
     */
    public CrossLinker(String name, double BaseMass, double CrossLinkedMass) {
        m_name = name;
        m_baseMass = BaseMass;
        m_crossLinkedMinMass = m_crossLinkedMaxMass = CrossLinkedMass;
        m_AllCrossLinker.put(name, this); // and register it
    }

    /**
     * retrives a crosslinker with the given name
     * @param name the name of the crosslinker
     * @return the crosslinker of the given name or null if non where found
     */
    public static CrossLinker getCrossLinker(String name){
        return m_AllCrossLinker.get(name);
    }


    /**
     * tests whether it can crosslink and digested
     * for now it jsut makes sure that a reduced peptide is linkable
     * but should be send back to the digestion method
     * @param p
     * @param digest
     * @return
     */
    public boolean canCrosslinkDigestable(Peptide p, Digestion digest) {
        // TODO: ask the digestion, whether th modified peptide could have been digested
        if (!p.isCTerminal()) {
            p = new Peptide(p, 0, p.length() - 1);
        }

        return canCrossLink(p);
    }

    /**
     * tests whether it can crosslink and digested
     * for now it jsut makes sure that a reduced peptide is linkable
     * but should be send back to the digestion method
     * @param p
     * @param digest
     * @return
     */
    public static boolean canCrosslinkDigestable(ArrayList<CrossLinker> crosslinker, Peptide p, Digestion digest) {
        for (CrossLinker cl : crosslinker ) {
            if (cl.canCrosslinkDigestable(p, digest)) {
                return true;
            }
        }
        return false;
    }


    /**
     * can the crosslinker link to the given amino acid
     * @param p peptide to be crosslink
     * @param linkSide which amino acid to crosslink
     * @return
     */
    public abstract boolean canCrossLink(AminoAcidSequence p, int linkSide);

    public abstract boolean canCrossLink(Fragment p, int linkSide);
    
    /**
     * Can the specified cross-linker-site (moiety-site) react with the
     * given peptide.
     * @param p peptide to be crosslink
     * @param linkSide which amino acid to crosslink
     * @return
     */
    public abstract boolean canCrossLinkMoietySite(AminoAcidSequence p, int moietySite);

    public abstract boolean canCrossLinkMoietySite(Fragment p, int moietySite);
    
    
    /**
     * can the crosslinker link to the given amino acid
     * @param p peptide to be crosslink
     * @param linkSide which amino acid to crosslink
     * @return
     */
    public static boolean canCrossLink(ArrayList<CrossLinker> crosslinker, AminoAcidSequence p, int linkSide) {
        for (CrossLinker cl : crosslinker ) {
            if (cl.canCrossLink(p,linkSide)) {
                return true;
            }
        }
        return false;
    }

    /**
     * can the crosslinker link the given amino acids
     * @param p1 first peptide to be crosslink
     * @param linkSide1 which amino acid of peptide 1 to crosslink
     * @param p2 peptide 2 to be crosslink
     * @param linkSide2 which amino acid of peptide 2 to crosslink
     * @return
     */
    public abstract boolean canCrossLink(AminoAcidSequence p1, int linkSide1, AminoAcidSequence p2, int linkSide2);

    public abstract boolean canCrossLink(Fragment p1, int linkSide1, Fragment p2, int linkSide2);

    /**
     * can the crosslinker link to the given amino acid
     * @param p1 peptide to be crosslink
     * @param linkSide1 which amino acid to crosslink
     * @return
     */
    public static boolean canCrossLink(ArrayList<CrossLinker> crosslinker, AminoAcidSequence p1, int linkSide1, AminoAcidSequence p2, int linkSide2) {
        for (CrossLinker cl : crosslinker ) {
            if (cl.canCrossLink(p1,linkSide1,p2,linkSide2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * can the crosslinker link something in the given peptide
     * @param p peptide to be crosslinked
     * @return
     */
    public boolean canCrossLink(AminoAcidSequence p) {
        for (int i = p.length(); --i >= 0;) {
            if (canCrossLink(p,i)) {
                return true;
            }
        }
        return false;
    }

    public boolean canCrossLink(Fragment p) {
        for (int i = p.length(); --i >= 0;) {
            if (canCrossLink(p,i)) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean canCrossLink(ArrayList<CrossLinker> crosslinker, AminoAcidSequence p) {
        for (CrossLinker cl : crosslinker ) {
            if (cl.canCrossLink(p)) {
                return true;
            }
        }
        return false;
    }


    /**
     * can the crosslinker link something from one peptide to the other
     * @param p1 first peptide to be crosslink
     * @param p2 peptide 2 to be crosslink
     * @return
     */
    public boolean canCrossLink(AminoAcidSequence p1, AminoAcidSequence p2) {

        for (int i1 = p1.length(); --i1 >= 0;) {
            if (canCrossLink(p1,i1)) {
                for (int i2 = p2.length(); --i2 >= 0;) {
                    if (canCrossLink(p1,i1,p2,i2)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    
    public boolean canCrossLink(Fragment p1, Fragment p2) {

        for (int i1 = p1.length(); --i1 >= 0;) {
            if (canCrossLink(p1,i1)) {
                for (int i2 = p2.length(); --i2 >= 0;) {
                    if (canCrossLink(p1,i1,p2,i2)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    public static boolean canCrossLink(ArrayList<CrossLinker> crosslinker, AminoAcidSequence p1, AminoAcidSequence p2) {
        for (CrossLinker cl : crosslinker ) {
            if (cl.canCrossLink(p1, p2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the m_name
     */
    public String getName() {
        return m_name;
    }


    /**
     * @return the base (non-crosslinked) mass
     */
    public double getBaseMass() {
        return m_baseMass;
    }

    /**
     * @return the mass of the linker, if it was linked to only one fragmentPrimary
     */
    public double getSinglyLinekdMass() {
        return m_baseMass; //TODO : does the crosslinker containing fragmentPrimary hold a proton
    }

    /**
     * The mass of the unbound cross linker
     * @param baseMass
     */
    public void setBaseMass(double baseMass) {
        this.m_baseMass = baseMass;
    }

    /**
     * @return the mass of the bound cross linker
     */
    public double getCrossLinkedMass() {
        return m_crossLinkedMinMass;
    }

    
    /**
     * @return the mass of the bound cross linker
     */
    public double getMinCrossLinkedMass() {
        return m_crossLinkedMinMass;
    }

    /**
     * @return the mass of the bound cross linker
     */
    public double getMaxCrossLinkedMass() {
        return m_crossLinkedMinMass;
    }

    /**
     * @return the mass of the bound cross linker
     */
    public double getMaxCrossLinkedMaxMass() {
        return m_crossLinkedMaxMass;
    }

    /**
     * @return the mass of the bound cross linker
     */
    public double getMaxCrossLinkedMinMass() {
        return m_crossLinkedMinMass;
    }
    
    /**
     * defines the mass of the bound cross linker
     * @param crossLinkedMass 
     */
    public void setCrossLinkedMass(double crossLinkedMass) {
        this.m_crossLinkedMinMass = this.m_crossLinkedMaxMass = crossLinkedMass;
        this.m_crossLinkedMaxMass = this.m_crossLinkedMaxMass = crossLinkedMass;
    }

    public static CrossLinker getCrossLinker(String className, String Options) {
        Class d= null;
        try {

            d = Class.forName("rappsilber.ms.crosslinker." + className);
            Method m = d.getMethod("parseArgs", String.class);
            return (CrossLinker) m.invoke(null, Options);


        } catch (ClassNotFoundException|IllegalAccessException|IllegalArgumentException|
                InvocationTargetException|NoSuchMethodException|SecurityException ex) {
            Logger.getLogger(CrossLinker.class.getName()).log(Level.SEVERE, null, ex);
            if (ex.getCause() != null)
                Logger.getLogger(CrossLinker.class.getName()).log(Level.SEVERE, "caused by " , ex.getCause());
            throw new Error(ex);
        }
    }


    public static CrossLinker getCrossLinker(String className, String Options, RunConfig config) {
        Class d= null;
        try {

            d = Class.forName("rappsilber.ms.crosslinker." + className);
            Method m = d.getMethod("parseArgs", String.class, RunConfig.class);
            return (CrossLinker) m.invoke(null, Options, config);


        } catch (ClassNotFoundException|IllegalAccessException|IllegalArgumentException|
                InvocationTargetException|NoSuchMethodException|SecurityException ex) {
            Logger.getLogger(CrossLinker.class.getName()).log(Level.SEVERE, null, ex);
            if (ex.getCause() != null)
                Logger.getLogger(CrossLinker.class.getName()).log(Level.SEVERE, "caused by " , ex.getCause());
            System.exit(-1);
        }
        return null;

    }

    /** defines whether a crosslinker is considered to be decoy-crosslinker */
    public void setDecoy(boolean decoy) {
        m_isDecoy = decoy;
    }

    /** returns whether this is a decoy cross-linker */
    public boolean isDecoy() {
        return m_isDecoy;
    }

    /**
     * @return the m_dbID
     */
    public int getDBid() {
        return m_dbID;
    }

    /**
     * @param m_dbID the m_dbID to set
     */
    public void setDBid(int DBid) {
        this.m_dbID = DBid;
    }

    public double getAminoAcidWeight(AminoAcid AA) {
        return 1;
    }

    public abstract double getWeight(Peptide pep, int position);

    public int getSites() {
        return 2;
    }
    
    public abstract boolean linksCTerminal(int site);

    public abstract boolean linksNTerminal(int site);
    
    
    protected static void parseSpecificity(String arg, HashMap<AminoAcid, Double> linkableAminoAcids,
            ObjectWrapper<Boolean> nTerm, UpdateableDouble nTermWeight,
            ObjectWrapper<Boolean> cTerm, UpdateableDouble cTermWeight,
            RunConfig config) throws ConfigurationParserException {
        boolean hasAAspeci = false;
        boolean linksEverything = false;
        for (String aaName : arg.split(",")) {
            aaName = aaName.trim();
            String[] aw = aaName.split("[\\(\\)]", 3);
            double w = 0;
            if (aw.length > 1) {
                aaName = aw[0].trim();
                w = Double.parseDouble(aw[1].trim());
            }
            // if we have an X or ANY defined don't define a specificity
            if (aaName.contentEquals("*") || aaName.contentEquals("ANY") || aaName.contentEquals("X") || aaName.contentEquals("XAA")) {
                linkableAminoAcids.clear();
                linksEverything = true;
                break;
            }
            if (aaName.toLowerCase().replaceAll("-", "").contentEquals("nterm")) {
                nTerm.value = true;
                nTermWeight.value = w;
                linkableAminoAcids.put(AminoAcid.DUMMY, 0d);
            } else if (aaName.toLowerCase().replaceAll("-", "").contentEquals("cterm")) {
                cTerm.value = true;
                cTermWeight.value = w;
                linkableAminoAcids.put(AminoAcid.DUMMY, 0d);
            } else {
                AminoAcid aa = config.getAminoAcid(aaName);
                if (aa != null) {
                    linkableAminoAcids.put(aa, w);
                }
                hasAAspeci = true;
            }
        }
        if (linkableAminoAcids.size()>1) {
            linkableAminoAcids.remove(AminoAcid.DUMMY);
        }
        if (hasAAspeci && linkableAminoAcids.isEmpty() && !linksEverything) {
            throw new ConfigurationParserException("None of the second linked aminoacids in " + arg + " are recognised. " + AsymetricSingleAminoAcidRestrictedCrossLinker.class.getName());
        }
        // if we have at least 20 amino acids I assume it means it means anything  
        if (linkableAminoAcids.size() >= 20) {
            linkableAminoAcids.clear();
        }
    }    
}
