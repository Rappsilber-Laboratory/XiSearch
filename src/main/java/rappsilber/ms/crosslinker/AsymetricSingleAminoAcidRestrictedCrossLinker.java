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

import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import rappsilber.config.ConfigurationParserException;
import rappsilber.config.RunConfig;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.AminoAcidSequence;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.loss.CleavableCrossLinkerPeptide;
import rappsilber.ms.sequence.ions.loss.CrossLinkerRestrictedLoss;
import rappsilber.ms.statistics.utils.UpdateableDouble;
import rappsilber.utils.ObjectWrapper;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class AsymetricSingleAminoAcidRestrictedCrossLinker extends AminoAcidRestrictedCrossLinker {

    protected HashMap<AminoAcid,Double> m_linkableSecondary;
    private boolean m_NTerminalSecondary = false;
    private double m_NTerminalWeightSecondary = Double.POSITIVE_INFINITY;
    private boolean m_CTerminalSecondary = false;
    private double m_CTerminalWeightSecondary = Double.POSITIVE_INFINITY;
    

    /**
     * creates and registers a new crosslinker (- definition)
     * @param Name
     * @param BaseMass
     * @param CrossLinkedMass
     * @param primaryLinkableAminoAcids
     */
    public AsymetricSingleAminoAcidRestrictedCrossLinker(String Name, double BaseMass, double CrossLinkedMass, HashSet<AminoAcid> PrimaryLinkableAminoAcids, HashSet<AminoAcid> SecondaryLinkableAminoAcids) {
        super(Name, BaseMass, CrossLinkedMass, PrimaryLinkableAminoAcids);
        m_linkableSecondary = new HashMap<AminoAcid, Double>(SecondaryLinkableAminoAcids.size());
        for (AminoAcid aa : SecondaryLinkableAminoAcids) {
            m_linkableSecondary.put(aa, 1.0);
        }
        
        // I use this as part of the workaround for nary cross-links
        //m_linkableSecondary.put(AminoAcid.X, 1.0);
        
    }

    /**
     * creates and registers a new crosslinker (- definition)
     * @param Name
     * @param BaseMass
     * @param CrossLinkedMass
     * @param primaryLinkableAminoAcids
     */
    public AsymetricSingleAminoAcidRestrictedCrossLinker(String Name, double BaseMass, double CrossLinkedMass, HashMap<AminoAcid,Double> PrimaryLinkableAminoAcids, HashMap<AminoAcid,Double> SecondaryLinkableAminoAcids) {
        super(Name, BaseMass, CrossLinkedMass, PrimaryLinkableAminoAcids);
        m_linkableSecondary = SecondaryLinkableAminoAcids;
        
        // I use this as part of the workaround for nary cross-links
        //m_linkableSecondary.put(AminoAcid.X, 1.0);
        
    }
    
    
    /**
     * creates and registers a new crosslinker (- definition)
     * @param Name
     * @param BaseMass
     * @param CrossLinkedMass
     * @param primaryLinkableAminoAcids
     */
    public AsymetricSingleAminoAcidRestrictedCrossLinker(String Name, double BaseMass, double CrossLinkedMass, AminoAcid[] PrimaryLinkableAminoAcids, AminoAcid[] SecondaryLinkableAminoAcids) {
        super(Name, BaseMass, CrossLinkedMass, PrimaryLinkableAminoAcids);
        m_linkableSecondary = new HashMap<AminoAcid,Double>(SecondaryLinkableAminoAcids.length);
        for (AminoAcid aa : SecondaryLinkableAminoAcids)
            m_linkableSecondary.put(aa,1.0);
    }

    @Override
    public boolean canCrossLink(AminoAcidSequence p, int linkSide) {
        if ((m_linkableSecondary.isEmpty() || m_linkable.isEmpty()) && (linkSide < p.length() - 1 || p.isCTerminal())) {
            return true;
        }                
        AminoAcid aa = p.nonLabeledAminoAcidAt(linkSide);
        return (m_linkable.containsKey(aa) || m_linkableSecondary.containsKey(aa) ||
                ((m_NTerminal || m_NTerminalSecondary) && p.isProteinNTerminal() && linkSide == 0) ||
                ((m_CTerminal || m_CTerminalSecondary) && p.isProteinCTerminal() && linkSide == p.length() - 1));
    }

    @Override
    public boolean canCrossLink(Fragment p, int linkSide) {
        if ((m_linkableSecondary.isEmpty() || m_linkable.isEmpty())) {
            return true;
        }                
        AminoAcid aa = p.nonLabeledAminoAcidAt(linkSide);
        return (m_linkable.containsKey(aa) || m_linkableSecondary.containsKey(aa) ||
                ((m_NTerminal || m_NTerminalSecondary) && p.isProteinNTerminal() && linkSide == 0) ||
                ((m_CTerminal || m_CTerminalSecondary) && p.isProteinCTerminal() && linkSide == p.length() - 1));
    }

    public boolean canCrossLinkSecondary(AminoAcidSequence p, int linkSide) {
        // if no specificity is given asume it can link anything
        if (m_linkableSecondary.isEmpty() && (linkSide < p.length() - 1 || p.isCTerminal())) {
            return true;
        }
        return ((m_linkableSecondary.containsKey(p.nonLabeledAminoAcidAt(linkSide)) &&
                (linkSide < p.length() - 1 || p.isCTerminal()))
                || (m_NTerminalSecondary && p.isNTerminal() && linkSide == 0)
                || (m_CTerminalSecondary && p.isCTerminal() && linkSide == p.length() - 1));
    }

    public boolean canCrossLinkSecondary(Fragment p, int linkSide) {
        // if no specificity is given asume it can link anything
        if (m_linkableSecondary.isEmpty()) {
            return true;
        }
        return ((m_linkableSecondary.containsKey(p.nonLabeledAminoAcidAt(linkSide)))
                || (m_NTerminalSecondary && p.isNTerminal() && linkSide == 0)
                || (m_CTerminalSecondary && p.isCTerminal() && linkSide == p.length() - 1));
    }

    public boolean canCrossLinkPrimary(AminoAcidSequence p, int linkSide) {
//        return (m_linkable.containsKey(p.nonLabeledAminoAcidAt(linkSide))
//                || (m_NTerminal && p.isNTerminal() && linkSide == 0)
//                || (m_CTerminal && p.isCTerminal() && linkSide == p.length() - 1));
        // TODO: we make here an assumption, that the digestion would not work
        // after a cross-linekd amino-acid. 
        // if no specificity is given asume it can link anything
        if (m_linkable.isEmpty() && (linkSide < p.length() - 1 || p.isCTerminal())) {
            return true;
        }        
        return ((m_linkable.containsKey(p.nonLabeledAminoAcidAt(linkSide)) &&
                (linkSide < p.length() - 1 || p.isCTerminal())) ||
                (m_NTerminal && p.isNTerminal() && linkSide == 0) ||
                (m_CTerminal && p.isCTerminal() && linkSide == p.length() - 1));
    }

    public boolean canCrossLinkPrimary(Fragment p, int linkSide) {
        if (m_linkable.isEmpty()) {
            return true;
        }        
        return ((m_linkable.containsKey(p.nonLabeledAminoAcidAt(linkSide))) ||
                (m_NTerminal && p.isNTerminal() && linkSide == 0) ||
                (m_CTerminal && p.isCTerminal() && linkSide == p.length() - 1));
    }

    
    @Override
    public boolean canCrossLinkMoietySite(AminoAcidSequence p, int moietySite) {
        if (moietySite==0) {
            if (m_linkable.isEmpty()) {
                return true;
            }  
            for (int site = p.length()-1; site >=0; site--) {
                if (canCrossLinkPrimary(p, site)) {
                    return true;
                }
            }
            return false;
        }
        if (m_linkableSecondary.isEmpty()) {
            return true;
        }  
        for (int site = p.length()-1; site >=0; site--) {
            if (canCrossLinkSecondary(p, site)) {
                return true;
            }
        }
        return false;
    }    

    public boolean canCrossLinkMoietySite(Fragment p, int moietySite) {
        if (moietySite==0) {
            if (m_linkable.isEmpty()) {
                return true;
            }  
            for (int site = p.length()-1; site >=0; site--) {
                if (canCrossLinkPrimary(p, site)) {
                    return true;
                }
            }
            return false;
        }
        if (m_linkableSecondary.isEmpty()) {
            return true;
        }  
        for (int site = p.length()-1; site >=0; site--) {
            if (canCrossLinkSecondary(p, site)) {
                return true;
            }
        }
        return false;
    }    
    
    @Override
    public boolean canCrossLink(AminoAcidSequence p1, int linkSide1, AminoAcidSequence p2, int linkSide2) {
        return (canCrossLinkPrimary(p1, linkSide1) && canCrossLinkSecondary(p2, linkSide2)) ||
                (canCrossLinkSecondary(p1, linkSide1) && canCrossLinkPrimary(p2, linkSide2));
    }

    @Override
    public boolean canCrossLink(Fragment p1, int linkSide1, Fragment p2, int linkSide2) {
        return (canCrossLinkPrimary(p1, linkSide1) && canCrossLinkSecondary(p2, linkSide2)) ||
                (canCrossLinkSecondary(p1, linkSide1) && canCrossLinkPrimary(p2, linkSide2));
    }    

    /**
     * Returns a link weight (actually a penalty) for the amino-acid
     * @param AA the amino acid to be tested
     * @return
     */
    @Override
    public double getAminoAcidWeight(AminoAcid AA) {
        return Math.min(super.getAminoAcidWeight(AA), getAminoAcidWeightSecondary(AA));
    }

    public double getAminoAcidWeightSecondary(AminoAcid AA) {
        Double w;
        if (!m_linkableSecondary.isEmpty())
            w = m_linkableSecondary.get(AA);
        else 
            w = 0d;
        return w == null ? Double.POSITIVE_INFINITY : w;
    }
    

    /**
     * parses an argument string to generate a new cross-linker object.<br/>
     * the argument should have the format
     * Name:BS2G; LinkedAminoAcids:R,K; Mass: 193.232;
     * or
     * Name:BS2G; LinkedAminoAcids:R,K; BassMass: 193.232; CrosslinkedMass:194.232
     * @param args
     * @return
     */
    public static AsymetricSingleAminoAcidRestrictedCrossLinker parseArgs(String args, RunConfig config) throws ConfigurationParserException, ParseException {
        String Name = null;
        double BaseMass = Double.NEGATIVE_INFINITY;
        double CrossLinkedMass = Double.NEGATIVE_INFINITY;
        ObjectWrapper<Boolean> NTerm1 = new  ObjectWrapper<Boolean>(false);
        ObjectWrapper<Boolean> CTerm1 = new  ObjectWrapper<Boolean>(false);
        ObjectWrapper<Boolean> NTerm2 = new  ObjectWrapper<Boolean>(false);
        ObjectWrapper<Boolean> CTerm2 = new  ObjectWrapper<Boolean>(false);
        UpdateableDouble NTermWeight1 = new UpdateableDouble(Double.POSITIVE_INFINITY);
        UpdateableDouble CTermWeight1 = new UpdateableDouble(Double.POSITIVE_INFINITY);
        UpdateableDouble NTermWeight2 = new UpdateableDouble(Double.POSITIVE_INFINITY);
        UpdateableDouble CTermWeight2 = new UpdateableDouble(Double.POSITIVE_INFINITY);
        String[] losses = null;
        String[] stubs = null;
        boolean isDecoy = false;
        int dbid = 0;

        HashMap<AminoAcid,Double> primaryLinkableAminoAcids = new HashMap<AminoAcid,Double>();
        HashMap<AminoAcid,Double> secondaryLinkableAminoAcids = new HashMap<AminoAcid,Double>();

        for (String arg : args.split(";")) {
            String[] argParts = arg.split(":");
            String argName = argParts[0].toUpperCase();
            if (argName.contentEquals("NAME"))
                    Name = argParts[1];
            else if (argName.contentEquals("FIRSTLINKEDAMINOACIDS")) {
                parseSpecificity(argParts[1], primaryLinkableAminoAcids, NTerm1, NTermWeight1, CTerm1, CTermWeight1, config);
            } else if (argName.contentEquals("SECONDLINKEDAMINOACIDS")) {
                parseSpecificity(argParts[1], secondaryLinkableAminoAcids, NTerm2, NTermWeight2, CTerm2, CTermWeight2, config);
            } else if (argName.contentEquals("MASS"))
                BaseMass = CrossLinkedMass = Double.parseDouble(argParts[1].trim());
            else if (argName.contentEquals("BASEMASS"))
                BaseMass = CrossLinkedMass = Double.parseDouble(argParts[1].trim());
            else if (argName.contentEquals("CROSSLINKEDMASS"))
                BaseMass = CrossLinkedMass = Double.parseDouble(argParts[1].trim());
            else if (argName.contentEquals("DECOY")) {
                isDecoy = true;
            } else if (argName.contentEquals("LOSSES")) {
                losses = argParts[1].split(",");
            } else if (argName.contentEquals("STUBS")) {
                losses = argParts[1].split(",");
            } else if (argName.contentEquals("ID")) {
                dbid = Integer.parseInt(argParts[1].trim());
            }
        }
        if (Name == null || BaseMass == Double.NEGATIVE_INFINITY ||
                CrossLinkedMass == Double.NEGATIVE_INFINITY)  {
            throw new ConfigurationParserException("Config line does not describe a valid " + AsymetricSingleAminoAcidRestrictedCrossLinker.class.getName());
        }
        if (losses != null) {
            for (int l =0; l < losses.length;l++ ) {
                String lName = losses[l++];
                double lMass = Double.parseDouble(losses[l].trim());
                CrossLinkerRestrictedLoss.parseArgs("NAME:" + lName  + ";MASS:"+lMass, config);
            }
        }

        if (stubs != null) {
            for (int l =0; l < stubs.length;l++ ) {
                String sName = stubs[l++];
                double sMass = Double.parseDouble(stubs[l].trim());
                CleavableCrossLinkerPeptide.parseArgs("MASS:"+ sMass + ";NAME:" + sName, config);
            }
        }
        
        AsymetricSingleAminoAcidRestrictedCrossLinker cl = new AsymetricSingleAminoAcidRestrictedCrossLinker(Name, BaseMass, CrossLinkedMass, primaryLinkableAminoAcids, secondaryLinkableAminoAcids);
        cl.setlinksCTerm(CTerm1.value);
        cl.setlinksNTerm(NTerm1.value);
        cl.setLinksCTermSecondary(CTerm2.value);
        cl.setLinksNTermSecondary(NTerm2.value);
        cl.setCTermWeight(CTermWeight1.value);
        cl.setNTermWeight(NTermWeight1.value);
        cl.setCTermWeightSecondary(CTermWeight2.value);
        cl.setNTermWeightSecondary(NTermWeight2.value);
        cl.setDecoy(isDecoy);
        cl.setDBid(dbid);
        return cl;
    }

    
    /**
     * @return the m_NTerminalSecondary
     */
    public boolean linksNTerminalSecondary() {
        return m_NTerminalSecondary;
    }

    /**
     * @param m_NTerminalSecondary the m_NTerminalSecondary to set
     */
    public void setLinksNTermSecondary(boolean m_NTerminalSecondary) {
        this.m_NTerminalSecondary = m_NTerminalSecondary;
    }

    /**
     * @return the m_CTerminalSecondary
     */
    public boolean linksCTerminalSecondary() {
        return m_CTerminalSecondary;
    }

    /**
     * @param m_CTerminalSecondary the m_CTerminalSecondary to set
     */
    public void setLinksCTermSecondary(boolean m_CTerminalSecondary) {
        this.m_CTerminalSecondary = m_CTerminalSecondary;
    }


    public double getSecondaryAminoAcidWeight(AminoAcid AA) {
        Double w = m_linkableSecondary.get(AA);
        return w == null ? Double.NEGATIVE_INFINITY : w;
    }

    public double getWeight(Peptide pep, int position) {
        double aaw = super.getWeight(pep, position);
        
        if (position==0 && m_NTerminalSecondary && pep.isNTerminal())
            return Math.min(m_NTerminalWeightSecondary, aaw);

        if (position==pep.length()-1 && m_CTerminalSecondary && pep.isCTerminal())
            return Math.min(m_CTerminalWeightSecondary, aaw);
       
        return aaw;
    }

    public Set<AminoAcid> getAASpecificity(int site) {
        if (site == 0) {
            return m_linkable.keySet();
        } 
        return m_linkableSecondary.keySet();
    }

    public boolean linksCTerminal(int site) {
        if (site == 0) 
            return m_CTerminal;
           
        return m_CTerminalSecondary;
    }

    public boolean linksNTerminal(int site) {
        if (site == 0) 
            return m_NTerminal;
           
        return m_NTerminalSecondary;
    }    

    private void setCTermWeightSecondary(double CTermWeight) {
        m_CTerminalWeightSecondary=CTermWeight;
    }

    private void setNTermWeightSecondary(double NTermWeight) {
        m_NTerminalWeightSecondary=NTermWeight;
    }
    
}
