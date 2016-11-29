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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import rappsilber.config.ConfigurationParserException;
import rappsilber.config.RunConfig;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.AminoAcidSequence;
import rappsilber.ms.sequence.Peptide;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class AsymetricSingleAminoAcidRestrictedCrossLinker extends AminoAcidRestrictedCrossLinker {

    protected HashMap<AminoAcid,Double> m_linkableSecondary;
    private boolean m_NTerminalSecondary = false;
    private boolean m_CTerminalSecondary = false;
    

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
                ((m_NTerminal || m_NTerminalSecondary) && p.isNTerminal() && linkSide == 0) ||
                ((m_CTerminal || m_CTerminalSecondary) && p.isCTerminal() && linkSide == p.length() - 1));
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

    
    @Override
    public boolean canCrossLink(AminoAcidSequence p1, int linkSide1, AminoAcidSequence p2, int linkSide2) {
        return (canCrossLinkPrimary(p1, linkSide1) && canCrossLinkSecondary(p2, linkSide2)) ||
                (canCrossLinkSecondary(p1, linkSide1) && canCrossLinkPrimary(p2, linkSide2));
    }

    public double getAminoAcidWeight(AminoAcid AA) {
        Double w = 1d;
        
        if (!m_linkable.isEmpty()) {
            w = m_linkable.get(AA);
            if (w == null) 
                if (m_linkableSecondary.isEmpty())
                    w=1d;
                else 
                    w = m_linkableSecondary.get(AA);
        } else {
            if (!m_linkableSecondary.isEmpty())
                w = m_linkableSecondary.get(AA);
            if (w == null)
                w = 1d;
        }
        
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
    public static AsymetricSingleAminoAcidRestrictedCrossLinker parseArgs(String args) throws ConfigurationParserException {
        String Name = null;
        double BaseMass = Double.NEGATIVE_INFINITY;
        double CrossLinkedMass = Double.NEGATIVE_INFINITY;;

        HashSet<AminoAcid> primaryLinkableAminoAcids = new HashSet<AminoAcid>();
        HashSet<AminoAcid> secondaryLinkableAminoAcids = new HashSet<AminoAcid>();

        for (String arg : args.split(";")) {
            String[] argParts = arg.split(":");
            String argName = argParts[0].toUpperCase();
            if (argName.contentEquals("NAME"))
                    Name = argParts[1];
            else if (argName.contentEquals("FIRSTLINKEDAMINOACIDS")) {
                for (String aaName : argParts[1].split(",")) {
                    // if we have an X or ANY defined don't define a specificity
                    if (aaName.contentEquals("ANY") || aaName.contentEquals("X") || aaName.contentEquals("XAA")) {
                        primaryLinkableAminoAcids.clear();
                        break;
                    }
                    primaryLinkableAminoAcids.add(AminoAcid.getAminoAcid(aaName));
                }
                // if we have at least 20 amino acids I assume it means it means anything  
                if (primaryLinkableAminoAcids.size() >= 20)
                    primaryLinkableAminoAcids.clear();
            
            } else if (argName.contentEquals("SECONDLINKEDAMINOACIDS")) {
                for (String aaName : argParts[1].split(",")) {
                    // if we have an X or ANY defined don't define a specificity
                    if (aaName.contentEquals("ANY") || aaName.contentEquals("X") || aaName.contentEquals("XAA")) {
                        secondaryLinkableAminoAcids.clear();
                        break;
                    }
                    secondaryLinkableAminoAcids.add(AminoAcid.getAminoAcid(aaName));
                }
                // if we have at least 20 amino acids I assume it means it means anything  
                if (secondaryLinkableAminoAcids.size() >= 20)
                    secondaryLinkableAminoAcids.clear();
            } else if (argName.contentEquals("MASS"))
                BaseMass = CrossLinkedMass = Double.parseDouble(argParts[1]);
            else if (argName.contentEquals("BASEMASS"))
                BaseMass = CrossLinkedMass = Double.parseDouble(argParts[1]);
            else if (argName.contentEquals("CROSSLINKEDMASS"))
                BaseMass = CrossLinkedMass = Double.parseDouble(argParts[1]);
        }
        if (Name == null || BaseMass == Double.NEGATIVE_INFINITY || 
                CrossLinkedMass == Double.NEGATIVE_INFINITY || primaryLinkableAminoAcids.size() == 0)  {
            throw new ConfigurationParserException("Config line does not describe a valid " + AsymetricSingleAminoAcidRestrictedCrossLinker.class.getName());
        }
        return new AsymetricSingleAminoAcidRestrictedCrossLinker(Name, BaseMass, CrossLinkedMass, primaryLinkableAminoAcids, secondaryLinkableAminoAcids);
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
    public static AsymetricSingleAminoAcidRestrictedCrossLinker parseArgs(String args, RunConfig config) throws ConfigurationParserException {
        String Name = null;
        double BaseMass = Double.NEGATIVE_INFINITY;
        double CrossLinkedMass = Double.NEGATIVE_INFINITY;
        boolean NTerm1 = false;
        boolean CTerm1 = false;
        boolean NTerm2 = false;
        boolean CTerm2 = false;
        boolean isDecoy = false;
        int dbid = 0;

        HashSet<AminoAcid> primaryLinkableAminoAcids = new HashSet<AminoAcid>();
        HashSet<AminoAcid> secondaryLinkableAminoAcids = new HashSet<AminoAcid>();

        for (String arg : args.split(";")) {
            String[] argParts = arg.split(":");
            String argName = argParts[0].toUpperCase();
            if (argName.contentEquals("NAME"))
                    Name = argParts[1];
            else if (argName.contentEquals("FIRSTLINKEDAMINOACIDS")) {
                for (String aaName : argParts[1].split(",")) {
                    if (aaName.contentEquals("ANY") || aaName.contentEquals("X") || aaName.contentEquals("XAA")) {
                        primaryLinkableAminoAcids.clear();
                        break;
                    }
                    if (aaName.toLowerCase().contentEquals("nterm"))
                        NTerm1 = true;
                    else if (aaName.toLowerCase().contentEquals("cterm"))
                        CTerm1 = true;
                    else {
                        AminoAcid aa =config.getAminoAcid(aaName);
                        if (aa!= null)
                            primaryLinkableAminoAcids.add(aa);
                    }
                }
                // if we have at least 20 amino acids I assume it means it means anything  
                if (primaryLinkableAminoAcids.size() >= 20)
                    primaryLinkableAminoAcids.clear();
            
            } else if (argName.contentEquals("SECONDLINKEDAMINOACIDS")) {
                for (String aaName : argParts[1].split(",")) {
                    // if we have an X or ANY defined don't define a specificity
                    if (aaName.contentEquals("ANY") || aaName.contentEquals("X") || aaName.contentEquals("XAA")) {
                        secondaryLinkableAminoAcids.clear();
                        break;
                    }
                    if (aaName.toLowerCase().contentEquals("nterm"))
                        NTerm2 = true;
                    else if (aaName.toLowerCase().contentEquals("cterm"))
                        CTerm2 = true;
                    else {
                        AminoAcid aa =config.getAminoAcid(aaName);
                        if (aa!= null)
                            secondaryLinkableAminoAcids.add(aa);
                    }
                }
                // if we have at least 20 amino acids I assume it means it means anything  
                if (secondaryLinkableAminoAcids.size() >= 20)
                    secondaryLinkableAminoAcids.clear();
            } else if (argName.contentEquals("MASS"))
                BaseMass = CrossLinkedMass = Double.parseDouble(argParts[1]);
            else if (argName.contentEquals("BASEMASS"))
                BaseMass = CrossLinkedMass = Double.parseDouble(argParts[1]);
            else if (argName.contentEquals("CROSSLINKEDMASS"))
                BaseMass = CrossLinkedMass = Double.parseDouble(argParts[1]);
            else if (argName.contentEquals("DECOY")) {
                isDecoy = true;
            } else if (argName.contentEquals("ID")) {
                dbid = Integer.parseInt(argParts[1]);
            }
        }
        if (Name == null || BaseMass == Double.NEGATIVE_INFINITY ||
                CrossLinkedMass == Double.NEGATIVE_INFINITY)  {
            throw new ConfigurationParserException("Config line does not describe a valid " + AsymetricSingleAminoAcidRestrictedCrossLinker.class.getName());
        }
        AsymetricSingleAminoAcidRestrictedCrossLinker cl = new AsymetricSingleAminoAcidRestrictedCrossLinker(Name, BaseMass, CrossLinkedMass, primaryLinkableAminoAcids, secondaryLinkableAminoAcids);
        cl.setlinksCTerm(CTerm1);
        cl.setlinksNTerm(NTerm1);
        cl.setLinksCTermSecondary(CTerm2);
        cl.setLinksNTermSecondary(NTerm2);
        cl.setDecoy(isDecoy);
        cl.setDBid(dbid);
        return cl;
    }

    
//    public double getAminoAcidWeight(AminoAcid AA) {
//        
//        Double wp = m_linkable.get(AA);
//        Double ws = m_linkableSecondary.get(AA);
//        if (wp == null) {
//            if (ws == null)
//                return Double.POSITIVE_INFINITY;
//            return ws;
//        }
//        return wp;
//    }
    
    
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
            return Math.min(m_NTerminalWeight, aaw);

        if (position==pep.length()-1 && m_CTerminalSecondary && pep.isCTerminal())
            return Math.min(m_CTerminalWeight, aaw);
       
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
    
}
