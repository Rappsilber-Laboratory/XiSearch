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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.AbstractRunConfig;
import rappsilber.config.RunConfig;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.ions.*;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class AminoAcidRestrictedLoss extends Loss {


    protected HashSet<AminoAcid> m_LossingAminoAcids;

    protected static ArrayList<RegistredLoss> m_RegisteredLosses = new ArrayList<RegistredLoss>();

    /** defines the registered types of (AminoAcidRestricted-) losses */
    public static class RegistredLoss{
        public HashSet<AminoAcid> LossingAminoAcids;
        public double             LossyMass;
        public boolean            LossCTerminal;
        public boolean            LossNTerminal;
        public String             Name;
        public int                LossID;

        /**
         *
         * @param LossingAminoAcids
         * @param LossyMass
         * @param LossCTerminal
         * @param LossNTerminal
         * @param name
         */
        public RegistredLoss(HashSet<AminoAcid> LossingAminoAcids,
                                double LossyMass,
                                boolean LossCTerminal,
                                boolean LossNTerminal,
                                String name, int lossID) {
            this.LossingAminoAcids = (HashSet<AminoAcid>) LossingAminoAcids.clone();
            this.LossyMass = LossyMass;
            this.LossCTerminal = LossCTerminal;
            this.LossNTerminal = LossNTerminal;
            this.Name = name;
            this.LossID = lossID;
        }
        
        public boolean matches(RegistredLoss rl) {
            return (rl.LossyMass == LossyMass && rl.Name.contentEquals(Name));
        }

        public void add(RegistredLoss rl) {
            if (rl.LossyMass == LossyMass && rl.Name.contentEquals(Name)) {
                this.LossCTerminal |= rl.LossCTerminal;
                this.LossNTerminal |= rl.LossNTerminal;
                LossingAminoAcids.addAll(rl.LossingAminoAcids);
            }
        }
    }
    




    public static void registerLoss(HashSet<AminoAcid> LossingAminoAcids,
                                double LossyMass,
                                boolean LossCTerminal,
                                boolean LossNTerminal,
                                String name) {
        RegistredLoss so2 = new RegistredLoss(LossingAminoAcids, LossyMass, LossCTerminal, LossNTerminal, name, -1);
        for (RegistredLoss rl : m_RegisteredLosses) {
            if (rl.matches(so2)) {
                rl.add(so2);
                return;
            }
        }
        m_RegisteredLosses.add(so2);

    }

    public static String[] getRegisteredNames(RunConfig conf) {
        ArrayList<RegistredLoss> losses = (ArrayList<RegistredLoss>) conf.retrieveObject(AminoAcidRestrictedLoss.class);
        if (losses == null) {
            return new String[0];
        }
        String[] lossNames = new String[losses.size()];
        for (int i = 0; i<losses.size(); i++) {
            lossNames[i]=losses.get(i).Name;
        }
        return lossNames;
    }

    public static void registerLoss(HashSet<AminoAcid> LossingAminoAcids,
                                double LossyMass,
                                boolean LossCTerminal,
                                boolean LossNTerminal,
                                String name,
                                RunConfig conf, int lossID) {
        ArrayList<RegistredLoss> losses = (ArrayList<RegistredLoss>) conf.retrieveObject(AminoAcidRestrictedLoss.class);
        if (losses == null) {
            losses = new ArrayList<RegistredLoss>();
            conf.storeObject(AminoAcidRestrictedLoss.class, losses);
        }
        RegistredLoss so2 = new RegistredLoss(LossingAminoAcids, LossyMass, LossCTerminal, LossNTerminal, name, lossID);
        boolean added = false;
        for (RegistredLoss rl : losses) {
            if (rl.matches(so2)) {
                rl.add(so2);
                added  =true;
                break;
            }
        }        
        
        if (!added) {
            losses.add(so2);
        }
        
        try {
            Loss.registerLossClass(AminoAcidRestrictedLoss.class, conf);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(AminoAcidRestrictedLoss.class.getName()).log(Level.SEVERE, "Strange Error: NoSuchMethodException", ex);
        }

    }


    public double getLossMass() {
        return m_massDiff;
    }

    public HashSet<AminoAcid> getLossingAminoAcids() {
        return m_LossingAminoAcids;
    }


    protected AminoAcidRestrictedLoss(Fragment f, double MassDifference, int count, HashSet<AminoAcid> LossingAminoAcids, String name, int LossID ) {
        super(f, MassDifference * count, LossID);
        m_massDiff = MassDifference;
        m_LossingAminoAcids = LossingAminoAcids;
        m_lossCount = count;
        m_name = name;
    }

    protected AminoAcidRestrictedLoss() {};




    /**
     * creates lossy fragments for each registered type of aminoacid restricted losses
     * also creates losses on lossy fragments (e.g. having loss of water and amonia on the same fragment
     * @param fragments
     * @param crosslinker
     * @param insert
     * @param conf
     * @return
     */
    public static ArrayList<Fragment> createLossyFragments(ArrayList fragments, CrossLinker crosslinker,  boolean insert, RunConfig conf) {
                              //         createLossyFragments(java.util.ArrayList, boolean)
        ArrayList<RegistredLoss> losses = (ArrayList<RegistredLoss>) conf.retrieveObject(AminoAcidRestrictedLoss.class);
        ArrayList<Fragment> ret = new ArrayList<Fragment>(fragments.size());
        ArrayList<Fragment> base = (ArrayList<Fragment>) fragments.clone();

        int maxTotalLossCount = (int) conf.retrieveObject("MAXTOTALLOSSES", AbstractRunConfig.DEFAULT_MAX_TOTAL_LOSSES);
        int maxLossCount = (int) conf.retrieveObject("MAXLOSSES", AbstractRunConfig.DEFAULT_MAX_LOSSES);
        
        
        for (RegistredLoss l: losses) {
            ArrayList<Fragment> retLoss = new ArrayList<Fragment>(fragments.size());
            for (Fragment f : base) {
                if (f.getFragmentationSites().length <= 1) {
                    // any fragment, that contains S,T,E or D can throw the according number of water
                    int count = f.countAminoAcid(l.LossingAminoAcids);
                    if (l.LossCTerminal && f.isCTerminal()) {
                        count ++;
                    }
                    if (l.LossNTerminal && f.isNTerminal()) {
                        count ++;
                    }

                    // don't create fragments with to many losses -> appears unrealistically
                    if (f.isClass(Loss.class)) {
                        int prevLossCount = ((Loss)f).getTotalLossCount();
                        if (count+prevLossCount >maxTotalLossCount) {
                            count=maxTotalLossCount-prevLossCount;
                        }
                    }
                    count = Math.min(count, maxLossCount);

                    for (int c = 1; c <= count; c++){
                        retLoss.add(new AminoAcidRestrictedLoss(f, l.LossyMass, c, l.LossingAminoAcids, l.Name, l.LossID));
                    }
                }
            }
            base.addAll(retLoss);
            ret.addAll(retLoss);
        }
        if (insert) {
            fragments.addAll(ret);
        }
        return ret;

    }

    public static boolean register() {
        try {
            Loss.registerLossClass(AminoAcidRestrictedLoss.class);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(AminoAcidRestrictedLoss.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }



    @Override
    public String name() {
        return getParentFragment().name() + "_"+ m_name + "x" + m_lossCount;
    }



    public static void parseArgs(String args, RunConfig conf) throws ParseException {

        HashSet<AminoAcid> LossingAminoAcids = null;
        double LossyMass = Double.NaN;
        boolean LossCTerminal = false;
        boolean LossNTerminal = false;
        String useForCandidates = null;
        int lossID = -1;
        String name = null;

        String[] argParts = args.split(";");

        // parses something like: aminoacids:R,K
        for (String arg : argParts) {
            String[] ap = arg.split(":");
            String aName = ap[0].trim().toLowerCase();
            if (aName.contentEquals("aminoacids")) {
                // Strip the string of whitespace and make it uppercase for comparison
                String[] amino_acids = ap[1].split(",");
                LossingAminoAcids = new HashSet<AminoAcid>(amino_acids.length);
                for(String b : amino_acids) {
                    LossingAminoAcids.add(conf.getAminoAcid(b.trim()));
                }
            } else if (aName.contentEquals("name")) {
                name = ap[1];
            } else if (aName.contentEquals("nterm")) {
                LossNTerminal = true;
            } else if (aName.contentEquals("cterm")) {
                LossCTerminal = true;
            } else if (aName.contentEquals("mass")) {
                LossyMass = - Double.parseDouble(ap[1]);
            } else if (aName.contentEquals("id")) {
                lossID = Integer.parseInt(ap[1].trim());
            } else if (aName.contentEquals("useforcandidates")) {
                if (ap.length>1) {
                    useForCandidates = ap[1].trim().toLowerCase();
                } else {
                    useForCandidates = "unspecific";
                }
            }

        }

        if (name == null) {
            throw new ParseException("AminoAcidRestrictedLoss without Name: " + args, 0);
        }
        if ((LossingAminoAcids == null || LossingAminoAcids.size() == 0) && LossCTerminal == false && LossNTerminal == false) {
            throw new ParseException("AminoAcidRestrictedLoss without Amiono Acids: " + args, 0);
        }
        if (Double.isNaN(LossyMass)) {
            throw new ParseException("AminoAcidRestrictedLoss without mass: " + args, 0);
        }

        if (useForCandidates!=null) {
            if (useForCandidates.contentEquals("unspecific")) {
                conf.getAlphaCandidateDeltaMasses().add(LossyMass);
            } else {
                throw new ParseException("unknown value for UseForCandidates: " + useForCandidates, 0);
            }
        }

        registerLoss(LossingAminoAcids,LossyMass,LossCTerminal,LossNTerminal,name,conf,lossID);

    }




}
