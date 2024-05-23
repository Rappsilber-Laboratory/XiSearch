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
import rappsilber.ms.sequence.ions.loss.CleavableCrossLinkerPeptide;
import rappsilber.ms.sequence.ions.loss.CrossLinkerRestrictedLoss;
import rappsilber.ms.statistics.utils.UpdateableDouble;
import rappsilber.utils.ObjectWrapper;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class SymetricSingleAminoAcidRestrictedCrossLinker extends AminoAcidRestrictedCrossLinker {
    

    /**
     * creates and registers a new crosslinker (- definition)
     * @param Name
     * @param BaseMass
     * @param CrossLinkedMass
     * @param linkableAminoAcids
     */
    public SymetricSingleAminoAcidRestrictedCrossLinker(ConfigEntity conf) {
        //String Name, double BaseMass, double CrossLinkedMass, HashSet<AminoAcid> linkableAminoAcids
        this(conf.getConfigValue("name"), getBaseMass(conf), getCrossLinkedMass(conf), getLinkedAminoAcids(conf));
    }

    private static double getBaseMass(ConfigEntity conf) {
        String bm = conf.getConfigValue("BaseMass");
        if (bm==null) {
            bm = conf.getConfigValue("CrossLinkedMass");
        }
        if (bm==null) {
            return Double.POSITIVE_INFINITY;
        } else {
            return Double.parseDouble(bm);
        }
    }

    private static double getCrossLinkedMass(ConfigEntity conf) {
        String cm = conf.getConfigValue("CrossLinkedMass");
        if (cm==null) {
            cm = conf.getConfigValue("BaseMass");
        }
        if (cm==null) {
            return Double.POSITIVE_INFINITY;
        } else {
            return Double.parseDouble(cm);
        }
    }

    private static HashSet<AminoAcid> getLinkedAminoAcids(ConfigEntity conf) {
        String[] aa = conf.getConfigValues("LinkedAminoAcids");
        if (aa==null) {
            return new HashSet<AminoAcid>();
        }

        HashSet<AminoAcid> laa= new HashSet<AminoAcid>();
        for (String a : aa) {
            AminoAcid aminoacid = AminoAcid.getAminoAcid(a);
            if (aminoacid  == null) {
                Logger.getLogger(SymetricSingleAminoAcidRestrictedCrossLinker.class.getName()).log(Level.SEVERE,"Unknown aminoacid: " + a + " will be ignored", new Exception("Unknown aminoacid: " + a));
            } else {
                laa.add(aminoacid);
            }
        }

        if (laa.isEmpty()) {
            Logger.getLogger(SymetricSingleAminoAcidRestrictedCrossLinker.class.getName()).log(Level.SEVERE,"No aminoacids specified, that can be linked.", new Exception(""));
        }

        return laa;

    }

    /**
     * creates and registers a new cross-linker (- definition)
     * @param Name
     * @param BaseMass
     * @param CrossLinkedMass
     * @param linkableAminoAcids
     */
    public SymetricSingleAminoAcidRestrictedCrossLinker(String Name, double BaseMass, double CrossLinkedMass, HashSet<AminoAcid> linkableAminoAcids) {
        super(Name, BaseMass, CrossLinkedMass, linkableAminoAcids);
    }

    /**
     * creates and registers a new crosslinker (- definition)
     * @param Name
     * @param BaseMass
     * @param CrossLinkedMass
     * @param linkableAminoAcids
     */
    public SymetricSingleAminoAcidRestrictedCrossLinker(String Name, double BaseMass, double CrossLinkedMass, HashMap<AminoAcid, Double> linkableAminoAcids) {
        super(Name, BaseMass, CrossLinkedMass, linkableAminoAcids);
    }

    /**
     * creates and registers a new crosslinker (- definition)
     * @param Name
     * @param BaseMass
     * @param CrossLinkedMass
     * @param linkableAminoAcids
     */
    public SymetricSingleAminoAcidRestrictedCrossLinker(String Name, double BaseMass, double CrossLinkedMass, AminoAcid[] linkableAminoAcids) {
        super(Name, BaseMass, CrossLinkedMass, linkableAminoAcids);
    }

//    public static final HashMap<Thread,long[]> times = new HashMap<>();
//    public static String calcTimes () {
//        long[] summed=new long[4];
//        for (long[] t : times.values()) {
//            for (int i=0;i<4;i++) {
//                summed[i]+=t[i];
//            }
//        }
//        StringBuilder sb = new StringBuilder();
//        for (int i=0;i<4;i++) {
//            sb.append(i).append(":").append(summed[i]).append("\n");
//        }
//        return sb.toString();
//    }
    
    @Override
    public boolean canCrossLink(AminoAcidSequence p, int linkSide) {
        // TODO: we make here an assumption, that the digestion would not work
        // after a cross-linekd amino-acid. 
        if (m_linkable.isEmpty() && (linkSide < p.length() - 1 || p.isCTerminal())) {
            return true;
        }                
        if (m_linkable.containsKey(p.nonLabeledAminoAcidAt(linkSide)) &&
                (linkSide < p.length() - 1 || p.isCTerminal())) {
            return true;
        }
        if (m_NTerminal && p.isProteinNTerminal() && linkSide == 0) {
            return true;
        }
        return (m_CTerminal && p.isProteinCTerminal() && linkSide == p.length() - 1);
    }

    @Override
    public boolean canCrossLink(Fragment p, int linkSide) {
        if (m_linkable.isEmpty()) {
            return true;
        }                
        
        if (m_linkable.containsKey(p.nonLabeledAminoAcidAt(linkSide))) {
            return true;
        }
        if (m_NTerminal && p.isProteinNTerminal() && linkSide == 0) {
            return true;
        }
        return (m_CTerminal && p.isProteinCTerminal() && linkSide == p.length() - 1);
    }
    
    
    @Override
    public boolean canCrossLink(AminoAcidSequence p1, int linkSide1, AminoAcidSequence p2, int linkSide2) {
        return canCrossLink(p1, linkSide1) && canCrossLink(p2, linkSide2);
    }

    @Override
    public boolean canCrossLink(Fragment p1, int linkSide1, Fragment p2, int linkSide2) {
        return canCrossLink(p1, linkSide1) && canCrossLink(p2, linkSide2);
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
    public static SymetricSingleAminoAcidRestrictedCrossLinker parseArgs(String args, RunConfig config) throws ConfigurationParserException, ParseException {
        String Name = null;
        ObjectWrapper<Boolean> nTerm = new  ObjectWrapper<Boolean>(false);
        ObjectWrapper<Boolean> cTerm = new  ObjectWrapper<Boolean>(false);
        UpdateableDouble nTermWeight = new UpdateableDouble(Double.POSITIVE_INFINITY);
        UpdateableDouble cTermWeight = new UpdateableDouble(Double.POSITIVE_INFINITY);
        double BaseMass = Double.NEGATIVE_INFINITY;
        double CrossLinkedMass = Double.NEGATIVE_INFINITY;;
        double CrossLinkedMinMass = Double.NEGATIVE_INFINITY;;
        double CrossLinkedMaxMass = Double.NEGATIVE_INFINITY;;
        String[] linearmodifications = null;
        String[] modifications = null;
        String[] losses = null;
        String[] stubs = null;
        boolean isDecoy = false;
        int dbid = -1;
        
//        System.err.println("register crosslinker in config: " + args);

//        HashSet<AminoAcid> linkableAminoAcids = new HashSet<AminoAcid>();
        HashMap<AminoAcid,Double> linkableAminoAcids = new HashMap<AminoAcid,Double>();

        for (String arg : args.split(";")) {
            String[] argParts = arg.split(":");
            String argName = argParts[0].toUpperCase();
            if (argName.contentEquals("NAME")) {
                Name = argParts[1];
            } else if (argName.contentEquals("LINKEDAMINOACIDS")) {
                parseSpecificity(argParts[1], linkableAminoAcids, nTerm, nTermWeight, cTerm, cTermWeight, config);
            } else if (argName.contentEquals("MASS")) {
                BaseMass = CrossLinkedMass = CrossLinkedMinMass = CrossLinkedMaxMass = Double.parseDouble(argParts[1].trim());
            }else if (argName.contentEquals("BASEMASS")) {
                BaseMass = Double.parseDouble(argParts[1].trim());
                if (CrossLinkedMass == Double.NEGATIVE_INFINITY) {
                    CrossLinkedMass = BaseMass;
                }
            } else if (argName.contentEquals("CROSSLINKEDMASS")) {
                CrossLinkedMass = Double.parseDouble(argParts[1].trim());
                if (BaseMass == Double.NEGATIVE_INFINITY) {
                    BaseMass = CrossLinkedMass;
                }
            } else if (argName.contentEquals("CROSSLINKEDMINMASS") || argName.contentEquals("MINMASS")) {
                CrossLinkedMinMass = Double.parseDouble(argParts[1].trim());
            } else if (argName.contentEquals("CROSSLINKEDMAXMASS") || argName.contentEquals("MAXMASS")) {
                CrossLinkedMinMass = Double.parseDouble(argParts[1].trim());
            } else if (argName.contentEquals("MODIFICATIONS")) {
                modifications = argParts[1].split(",");
            } else if (argName.contentEquals("LINEARMODIFICATIONS")) {
                linearmodifications = argParts[1].split(",");
            } else if (argName.contentEquals("LOSSES")) {
                losses = argParts[1].split(",");
            } else if (argName.contentEquals("STUBS")) {
                stubs = argParts[1].split(",");
            } else if (argName.contentEquals("DECOY")) {
                isDecoy = true;
            } else if (argName.contentEquals("ID")) {
                dbid = Integer.parseInt(argParts[1]);
            }
        }
        ArrayList<AminoModification> mods= new ArrayList<>();
        if (modifications != null) {
            for (int mod =0; mod < modifications.length;mod++ ) {
                String mName = modifications[mod++];
                double mMass = Double.parseDouble(modifications[mod].trim());
                for (AminoAcid aa : linkableAminoAcids.keySet()) {
                    AminoModification am = new AminoModification(aa.toString() + Name.toLowerCase() + mName.toLowerCase(), aa, aa.mass + BaseMass + mMass);
//                    System.err.println("new Modification " + am.toString());
                    config.addVariableModification(am);
                    mods.add(am);
                }
            }
        }
        if (linearmodifications != null) {
            for (int mod =0; mod < linearmodifications.length;mod++ ) {
                String mName = linearmodifications[mod++];
                double mMass = Double.parseDouble(linearmodifications[mod].trim());
                for (AminoAcid aa : linkableAminoAcids.keySet()) {
                    AminoModification am = new AminoModification(aa.toString() + Name.toLowerCase() + mName.toLowerCase(), aa, aa.mass + BaseMass + mMass);
//                    System.err.println("new Modification " + am.toString());
                    config.addLinearModification(am);
                    mods.add(am);
                }
            }
        }

        if (losses != null) {
            for (int l =0; l < losses.length;l++ ) {
                String lName = losses[l++];
                double lMass = Double.parseDouble(losses[l].trim());
                for (AminoModification am : mods) {
                    String largs ="NAME:"+lName+";aminoacids:"+ am.SequenceID + ";MASS:" +(lMass);
                    AminoAcidRestrictedLoss.parseArgs(largs, config);
                }
                CrossLinkerRestrictedLoss.parseArgs("NAME:" + lName  + ";MASS:"+lMass, config);
            }
        }

        if (stubs != null) {
            for (int l =0; l < stubs.length;l++ ) {
                String sName = stubs[l++];
                double sMass = Double.parseDouble(stubs[l].trim());
                for (AminoModification am : mods) {
                    double diff = am.mass - (am.BaseAminoAcid.mass+sMass);
                    String largs ="NAME:"+sName+"mod;aminoacids:"+ am.SequenceID + ";MASS:" +diff;
                    AminoAcidRestrictedLoss.parseArgs(largs, config);
                }
                CleavableCrossLinkerPeptide.parseArgs("MASS:"+ sMass + ";NAME:" + sName, config);
            }
        }
        
        if (Name == null || BaseMass == Double.NEGATIVE_INFINITY ||
                CrossLinkedMass == Double.NEGATIVE_INFINITY) { // || linkableAminoAcids.size() == 0)  {
            throw new ConfigurationParserException("Config line does not describe a valid " + SymetricSingleAminoAcidRestrictedCrossLinker.class.getName());
        }
//        if (linkableAminoAcids.isEmpty()){
//            Logger.getLogger(SymetricSingleAminoAcidRestrictedCrossLinker.class.getName()).log(Level.WARNING, "Linker does not define linked amino-acids -> this will be a linear search ");
//        }
        SymetricSingleAminoAcidRestrictedCrossLinker cl =  new SymetricSingleAminoAcidRestrictedCrossLinker(Name, BaseMass, CrossLinkedMass, linkableAminoAcids);
        cl.setlinksNTerm(nTerm.value);
        cl.setNTermWeight(nTermWeight.value);
        cl.setlinksCTerm(cTerm.value);
        cl.setCTermWeight(cTermWeight.value);
        cl.setDecoy(isDecoy);
        cl.setDBid(dbid);
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
     * can the crosslinker link something from one peptide to the other
     * @param p1 first peptide to be crosslink
     * @param p2 peptide 2 to be crosslink
     * @return
     */
    public boolean canCrossLink(AminoAcidSequence p1, AminoAcidSequence p2) {
        return canCrossLink(p1) && canCrossLink(p2);
    }

}
