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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.ConfigEntity;
import rappsilber.config.ConfigurationParserException;
import rappsilber.config.RunConfig;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.AminoAcidSequence;
import rappsilber.ms.sequence.AminoModification;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.loss.AminoAcidRestrictedLoss;
import rappsilber.ms.sequence.ions.loss.CrossLinkerRestrictedLoss;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class SymetricNarrySingleAminoAcidRestrictedCrossLinker extends AminoAcidRestrictedCrossLinker {
    private int sites;

//    /**
//     * creates and registers a new crosslinker (- definition)
//     * @param Name
//     * @param BaseMass
//     * @param CrossLinkedMass
//     * @param linkableAminoAcids
//     */
//    public SymetricNarrySingleAminoAcidRestrictedCrossLinker(ConfigEntity conf) {
//        //String Name, double BaseMass, double CrossLinkedMass, HashSet<AminoAcid> linkableAminoAcids
//        this(conf.getConfigValue("name"), getBaseMass(conf), getCrossLinkedMass(conf), getLinkedAminoAcids(conf));
//    }
//
//    private static double getBaseMass(ConfigEntity conf) {
//        String bm = conf.getConfigValue("BaseMass");
//        if (bm==null) {
//            bm = conf.getConfigValue("CrossLinkedMass");
//        }
//        if (bm==null)
//            return Double.POSITIVE_INFINITY;
//        else
//            return Double.parseDouble(bm);
//    }
//
//    private static double getCrossLinkedMass(ConfigEntity conf) {
//        String cm = conf.getConfigValue("CrossLinkedMass");
//        if (cm==null) {
//            cm = conf.getConfigValue("BaseMass");
//        }
//        if (cm==null)
//            return Double.POSITIVE_INFINITY;
//        else
//            return Double.parseDouble(cm);
//    }

    private static HashSet<AminoAcid> getLinkedAminoAcids(ConfigEntity conf) {
        String[] aa = conf.getConfigValues("LinkedAminoAcids");
        if (aa==null)
            return new HashSet<AminoAcid>();

        HashSet<AminoAcid> laa= new HashSet<AminoAcid>();
        for (String a : aa) {
            AminoAcid aminoacid = AminoAcid.getAminoAcid(a);
            if (aminoacid  == null) {
                Logger.getLogger(SymetricNarrySingleAminoAcidRestrictedCrossLinker.class.getName()).log(Level.SEVERE,"Unknown aminoacid: " + a + " will be ignored", new Exception("Unknown aminoacid: " + a));
            } else
                laa.add(aminoacid);
        }

        if (laa.isEmpty())
            Logger.getLogger(SymetricNarrySingleAminoAcidRestrictedCrossLinker.class.getName()).log(Level.SEVERE,"No aminoacids specified, that can be linked.", new Exception(""));

        return laa;

    }

    /**
     * creates and registers a new cross-linker (- definition)
     * @param Name
     * @param BaseMass
     * @param CrossLinkedMass
     * @param linkableAminoAcids
     */
    public SymetricNarrySingleAminoAcidRestrictedCrossLinker(String Name, double BaseMass, double CrossLinkedMass, HashSet<AminoAcid> linkableAminoAcids, int sites) {
        super(Name, BaseMass, CrossLinkedMass, linkableAminoAcids);
        this.sites= sites;
    }

    /**
     * creates and registers a new crosslinker (- definition)
     * @param Name
     * @param BaseMass
     * @param CrossLinkedMass
     * @param linkableAminoAcids
     */
    public SymetricNarrySingleAminoAcidRestrictedCrossLinker(String Name, double BaseMass, double CrossLinkedMass, HashMap<AminoAcid, Double> linkableAminoAcids, int sites) {
        super(Name, BaseMass, CrossLinkedMass, linkableAminoAcids);
        this.sites= sites;
    }

    /**
     * creates and registers a new crosslinker (- definition)
     * @param Name
     * @param BaseMass
     * @param CrossLinkedMass
     * @param linkableAminoAcids
     */
    public SymetricNarrySingleAminoAcidRestrictedCrossLinker(String Name, double BaseMass, double CrossLinkedMass, AminoAcid[] linkableAminoAcids) {
        super(Name, BaseMass, CrossLinkedMass, linkableAminoAcids);
    }

    @Override
    public boolean canCrossLink(AminoAcidSequence p, int linkSide) {
        // TODO: we make here an assumption, that the digestion would not work
        // after a cross-linekd amino-acid. 
        if (m_linkable.isEmpty() && (linkSide < p.length() - 1 || p.isCTerminal())) {
            return true;
        }                

        return ((m_linkable.containsKey(p.nonLabeledAminoAcidAt(linkSide)) &&
                (linkSide < p.length() - 1 || p.isCTerminal())) ||
                (m_NTerminal && p.isNTerminal() && linkSide == 0) ||
                (m_CTerminal && p.isCTerminal() && linkSide == p.length() - 1));
    }

    public boolean canCrossLink(Fragment p, int linkSide) {
        // TODO: we make here an assumption, that the digestion would not work
        // after a cross-linekd amino-acid. 
        if (m_linkable.isEmpty()) {
            return true;
        }                

        return ((m_linkable.containsKey(p.nonLabeledAminoAcidAt(linkSide))) ||
                (m_NTerminal && p.isNTerminal() && linkSide == 0) ||
                (m_CTerminal && p.isCTerminal() && linkSide == p.length() - 1));
    }
    
    @Override
    public boolean canCrossLink(AminoAcidSequence p1, int linkSide1, AminoAcidSequence p2, int linkSide2) {
        return canCrossLink(p1, linkSide1) && canCrossLink(p2, linkSide2);
    }

    @Override
    public boolean canCrossLink(Fragment p1, int linkSide1, Fragment p2, int linkSide2) {
        return canCrossLink(p1, linkSide1) && canCrossLink(p2, linkSide2);
    }
    
    public boolean canCrossLink(AminoAcidSequence[] peps, int[] linkSides) {
        for (int p = 0; p < peps.length; p++) {
            if (!canCrossLink(peps[p], linkSides[p]))
                return false;
        }
        return true;
    }

    /**
     * parses an argument string to generate a new crosslinker object.<br/>
     * the argument should have the format
     * Name:BS2G; LinkedAminoAcids:R,K; Mass: 193.232;
     * or
     * Name:BS2G; LinkedAminoAcids:R,K; BassMass: 193.232; CrosslinkedMass:194.232
     * @param args
     * @return
     */
    public static SymetricNarrySingleAminoAcidRestrictedCrossLinker parseArgs(String args) throws ConfigurationParserException {
        String Name = null;
        double BaseMass = Double.NEGATIVE_INFINITY;
        double CrossLinkedMass = Double.NEGATIVE_INFINITY;;
        int MOIETIES =0;

        HashMap<AminoAcid,Double> linkableAminoAcids = new HashMap<AminoAcid,Double>();

//        System.err.println("register crosslinker globaly: " + args);

        for (String arg : args.split(";")) {
            String[] argParts = arg.split(":");
            String argName = argParts[0].toUpperCase();
            if (argName.contentEquals("NAME"))
                    Name = argParts[1];
            else if (argName.contentEquals("LINKEDAMINOACIDS")) {
                String[] aas = argParts[1].split(",");
                double aacount = aas.length;
                for ( int i = 0 ; i< aas.length; i++) {
                    String aaName =aas[i];
                    String[] aw = aaName.split("[\\(\\)]",2);
                    double w = i;
                    if (aw.length == 1) {
                        w=i/aacount;
                    } else {
                        w = Double.parseDouble(aw[1]);
                    }
                    if (aaName.contentEquals("ANY") || aaName.contentEquals("*") || aaName.contentEquals("X") || aaName.contentEquals("XAA")) {
                        linkableAminoAcids.clear();
                        break;
                    }
                    linkableAminoAcids.put(AminoAcid.getAminoAcid(aaName), w);
                }
            } else if (argName.contentEquals("MASS"))
                BaseMass = CrossLinkedMass = Double.parseDouble(argParts[1]);
            else if (argName.contentEquals("BASEMASS"))
                BaseMass = CrossLinkedMass = Double.parseDouble(argParts[1]);
            else if (argName.contentEquals("CROSSLINKEDMASS"))
                BaseMass = CrossLinkedMass = Double.parseDouble(argParts[1]);
            else if (argName.contentEquals("MOIETIES"))
                MOIETIES = Integer.parseInt(argParts[1]);
            else if (argName.contentEquals("SITES"))
                MOIETIES = Integer.parseInt(argParts[1]);        }
        if (Name == null || BaseMass == Double.NEGATIVE_INFINITY || 
                CrossLinkedMass == Double.NEGATIVE_INFINITY ) { // || linkableAminoAcids.size() == 0)  {
            throw new ConfigurationParserException("Config line does not describe a valid " + SymetricNarrySingleAminoAcidRestrictedCrossLinker.class.getName());
        }
        return new SymetricNarrySingleAminoAcidRestrictedCrossLinker(Name, BaseMass, CrossLinkedMass, linkableAminoAcids,MOIETIES);
    }


    
    /**
     * parses an argument string to generate a new crosslinker object.<br/>
     * the argument should have the format
     * Name:BS2G; LinkedAminoAcids:R,K; Mass: 193.232;
     * or
     * Name:BS2G; LinkedAminoAcids:R,K; BassMass: 193.232; CrosslinkedMass:194.232
     * @param args
     * @return
     */
    public static SymetricNarrySingleAminoAcidRestrictedCrossLinker parseArgs(String args, RunConfig config) throws ConfigurationParserException, ParseException {
        String Name = null;
        boolean NTerm = false;
        double NTermWeight = Double.POSITIVE_INFINITY;
        double CTermWeight = Double.POSITIVE_INFINITY;
        boolean CTerm = false;
        double BaseMass = Double.NEGATIVE_INFINITY;
        double CrossLinkedMass = Double.NEGATIVE_INFINITY;;
        double CrossLinkedMinMass = Double.NEGATIVE_INFINITY;;
        double CrossLinkedMaxMass = Double.NEGATIVE_INFINITY;;
        String[] modifications = null;
        String[] losses = null;
        boolean isDecoy = false;
        int dbid = -1;
        int MOIETIES =0;        
//        System.err.println("register crosslinker in config: " + args);

//        HashSet<AminoAcid> linkableAminoAcids = new HashSet<AminoAcid>();
        HashMap<AminoAcid,Double> linkableAminoAcids = new HashMap<AminoAcid,Double>();

        for (String arg : args.split(";")) {
            String[] argParts = arg.split(":");
            String argName = argParts[0].toUpperCase();
            if (argName.contentEquals("NAME"))
                    Name = argParts[1];
            else if (argName.contentEquals("LINKEDAMINOACIDS")) {
                if (argParts.length>1) {
                    String[] aas = argParts[1].split(",");
                    double aacount = aas.length;
                    for ( int i = 0 ; i< aacount; i++) {
                        String aaName =aas[i];
                        String[] aw = aaName.split("[\\(\\)]",3);
                        double w = i;
                        if (aw.length == 1) {
                            w= 0;
                        } else {
                            aaName = aw[0];
                            w = Double.parseDouble(aw[1]);
                        }
                        if (aaName.toLowerCase().contentEquals("nterm")) {
                            NTerm = true;
                            NTermWeight = w;
                        } else if (aaName.toLowerCase().contentEquals("cterm")) {
                            CTerm = true;
                            CTermWeight = w;
                        } else { 

                            AminoAcid AA = config.getAminoAcid(aaName);
                            if (AA != null)
                                linkableAminoAcids.put(config.getAminoAcid(aaName), w);
                        }
                    }
                }

//                for (String aaName : argParts[1].split(",")) {
//                    if (aaName.toLowerCase().contentEquals("nterm"))
//                        NTerm = true;
//                    else if (aaName.toLowerCase().contentEquals("cterm"))
//                        CTerm = true;
//                    else
//                        linkableAminoAcids.add(config.getAminoAcid(aaName));
//                }
            } else if (argName.contentEquals("MASS")) {
                BaseMass = CrossLinkedMass = CrossLinkedMinMass = CrossLinkedMaxMass = Double.parseDouble(argParts[1]);
            }else if (argName.contentEquals("BASEMASS")) {
                BaseMass = Double.parseDouble(argParts[1]);
                if (CrossLinkedMass == Double.NEGATIVE_INFINITY)
                    CrossLinkedMass = BaseMass;
            } else if (argName.contentEquals("CROSSLINKEDMASS")) {
                CrossLinkedMass = Double.parseDouble(argParts[1]);
                if (BaseMass == Double.NEGATIVE_INFINITY)
                    BaseMass = CrossLinkedMass;
            } else if (argName.contentEquals("CROSSLINKEDMINMASS") || argName.contentEquals("MINMASS"))
                CrossLinkedMinMass = Double.parseDouble(argParts[1]);
            else if (argName.contentEquals("CROSSLINKEDMAXMASS") || argName.contentEquals("MAXMASS"))
                CrossLinkedMinMass = Double.parseDouble(argParts[1]);
            else if (argName.contentEquals("MODIFICATIONS")) {
                modifications = argParts[1].split(",");
            } else if (argName.contentEquals("LOSSES")) {
                losses = argParts[1].split(",");
            } else if (argName.contentEquals("DECOY")) {
                isDecoy = true;
            } else if (argName.contentEquals("ID")) {
                dbid = Integer.parseInt(argParts[1]);
            } else if (argName.contentEquals("MOIETIES"))
                MOIETIES = Integer.parseInt(argParts[1]);
            else if (argName.contentEquals("SITES"))
                MOIETIES = Integer.parseInt(argParts[1]);        
        
        }



        if (losses != null) {
            for (int l =0; l < losses.length;l++ ) {
                String lName = losses[l++];
                double lMass = Double.parseDouble(losses[l]);
                for (int mod =0; mod < modifications.length;mod++ ) {
                    String mName = modifications[mod++];
                    double mMass = Double.parseDouble(modifications[mod]);
                    for (AminoAcid aa : linkableAminoAcids.keySet()) {
                        AminoModification am = new AminoModification(aa.toString() + Name.toLowerCase() + lName.toLowerCase(), aa, aa.mass + BaseMass + lMass);
                        String largs ="NAME:"+lName+";aminoacids:"+ aa.toString() + Name.toLowerCase() + mName.toLowerCase() + ";MASS:" +(aa.mass + BaseMass + mMass - lMass);
                        AminoAcidRestrictedLoss.parseArgs(largs, config);
//    //                    System.err.println("new Modification " + am.toString());
//                        config.addVariableModification(am);
                    }
                }
                CrossLinkerRestrictedLoss.parseArgs("NAME:" + lName  + ";MASS:"+lMass, config);
            }
        }
        
        if (Name == null || BaseMass == Double.NEGATIVE_INFINITY ||
                CrossLinkedMass == Double.NEGATIVE_INFINITY) { // || linkableAminoAcids.size() == 0)  {
            throw new ConfigurationParserException("Config line does not describe a valid " + SymetricNarrySingleAminoAcidRestrictedCrossLinker.class.getName());
        }
        if (linkableAminoAcids.size() == 0){
            Logger.getLogger(SymetricNarrySingleAminoAcidRestrictedCrossLinker.class.getName()).log(Level.WARNING, "Linker does not define linked amino-acids -> this will be a linear search ");
        }
        
        // setup sitecount cross-linker and dead end cross-linker
        if (modifications != null) {
            for (int mod =0; mod < modifications.length;mod++ ) {
                String mName = modifications[mod++];
                String modExt ="";
                double mMass = Double.parseDouble(modifications[mod]);
                // lower site-count cross-linker
                for (int i =1; i<MOIETIES-1; i++) {
                    modExt += "_"+ mName;
                    SymetricNarrySingleAminoAcidRestrictedCrossLinker cl =  new SymetricNarrySingleAminoAcidRestrictedCrossLinker(Name+ modExt, BaseMass+mMass*i, CrossLinkedMass+mMass*i, linkableAminoAcids, MOIETIES-i);
                    cl.setlinksNTerm(NTerm);
                    cl.setNTermWeight(NTermWeight);
                    cl.setlinksCTerm(CTerm);
                    cl.setCTermWeight(CTermWeight);
                    cl.setDecoy(isDecoy);
                    cl.setDBid(dbid); 
                    config.getCrossLinker().add(cl);
                }
                // dead end
                for (AminoAcid aa : linkableAminoAcids.keySet()) {
                    AminoModification am = new AminoModification(aa.toString() + Name.toLowerCase() + modExt.toLowerCase(), aa, aa.mass + BaseMass + mMass*(MOIETIES-1));
                    config.addVariableModification(am);
                }                
            }

            // setup dead end modifications
            HashMap<ArrayList<String>,Double> allMods = new HashMap((modifications.length*modifications.length+modifications.length)/2);
        }

        // n sites to the same peptide
        for (int i =1; i<MOIETIES-1; i++) {
            SymetricNarrySingleAminoAcidRestrictedCrossLinker cl =  new SymetricNarrySingleAminoAcidRestrictedCrossLinker(Name+ "_partLoop", BaseMass, CrossLinkedMass, linkableAminoAcids, MOIETIES-i);
            cl.setlinksNTerm(NTerm);
            cl.setNTermWeight(NTermWeight);
            cl.setlinksCTerm(CTerm);
            cl.setCTermWeight(CTermWeight);
            cl.setDecoy(isDecoy);
            cl.setDBid(dbid); 
            config.getCrossLinker().add(cl);
        }
        
        SymetricNarrySingleAminoAcidRestrictedCrossLinker cl =  new SymetricNarrySingleAminoAcidRestrictedCrossLinker(Name, BaseMass, CrossLinkedMass, linkableAminoAcids, MOIETIES);
        cl.setlinksNTerm(NTerm);
        cl.setNTermWeight(NTermWeight);
        cl.setlinksCTerm(CTerm);
        cl.setCTermWeight(CTermWeight);
        cl.setDecoy(isDecoy);
        cl.setDBid(dbid);
        if (config.getMaxCrosslinkedPeptides() < MOIETIES)
            config.setMaxCrosslinkedPeptides(MOIETIES);
        return cl;
        
    }

    @Override
    public boolean canCrossLinkMoietySite(AminoAcidSequence p, int moietySite) {
        return canCrossLink(p);
    }
    
    @Override
    public boolean canCrossLinkMoietySite(Fragment p, int moietySite) {
        return canCrossLink(p);
    }

    /**
     * @return the sites
     */
    public int getSites() {
        return sites;
    }

    /**
     * @param sites the sites to set
     */
    public void setSites(int sites) {
        this.sites = sites;
    }



}
