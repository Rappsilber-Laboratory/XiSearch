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
import rappsilber.ms.sequence.AminoAcid;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.AbstractRunConfig;
import rappsilber.config.RunConfig;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.sequence.AminoModification;
import rappsilber.ms.sequence.Peptide;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class AminoAcidRestrictedImmonium extends Loss {


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
    }





    public static void registerLoss(HashSet<AminoAcid> LossingAminoAcids,
                                double LossyMass,
                                boolean LossCTerminal,
                                boolean LossNTerminal,
                                String name) {
        RegistredLoss so2 = new RegistredLoss(LossingAminoAcids, LossyMass, LossCTerminal, LossNTerminal, name, -1);
        m_RegisteredLosses.add(so2);

    }

    public static String[] getRegisteredNames(RunConfig conf) {
        ArrayList<RegistredLoss> losses = (ArrayList<RegistredLoss>) conf.retrieveObject(AminoAcidRestrictedImmonium.class);
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
        ArrayList<RegistredLoss> losses = (ArrayList<RegistredLoss>) conf.retrieveObject(AminoAcidRestrictedImmonium.class);
        if (losses == null) {
            losses = new ArrayList<RegistredLoss>();
            conf.storeObject(AminoAcidRestrictedImmonium.class, losses);
        }
        RegistredLoss so2 = new RegistredLoss(LossingAminoAcids, LossyMass, LossCTerminal, LossNTerminal, name, lossID);
        losses.add(so2);
        try {
            Loss.registerLossClass(AminoAcidRestrictedImmonium.class, conf);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(AminoAcidRestrictedImmonium.class.getName()).log(Level.SEVERE, "Strange Error: NoSuchMethodException", ex);
        }

    }


    public double getLossMass() {
        return m_massDiff;
    }

    public HashSet<AminoAcid> getLossingAminoAcids() {
        return m_LossingAminoAcids;
    }


    protected AminoAcidRestrictedImmonium(Fragment f, double Mass, int count, HashSet<AminoAcid> LossingAminoAcids, String name, int LossID ) {
        super(f, -f.getMass() + Mass, LossID);
        m_massDiff = Mass;
        m_LossingAminoAcids = LossingAminoAcids;
        m_lossCount = count;
        m_name = name;
    }

    protected AminoAcidRestrictedImmonium() {};


//    public static Class getMyClass() {
//        return new AminoAcidRestrictedLoss().getClass();
//    }

    public static ArrayList<Fragment> createLossyFragments(ArrayList fragments, CrossLinker crosslinker,  boolean insert) {
                              //         createLossyFragments(java.util.ArrayList, boolean)
        ArrayList<Fragment> ret = new ArrayList<Fragment>(fragments.size());
        ArrayList<Fragment> base = fragments;
        for (Fragment f : base) {
            for (RegistredLoss l: m_RegisteredLosses) {
                // any fragment, that contains S,T,E or D can throw the according number of water
                int count = f.countAminoAcid(l.LossingAminoAcids);
                if (l.LossCTerminal && f.isCTerminal()) count ++;
                if (l.LossNTerminal && f.isNTerminal()) count ++;
                for (int c = 1; c <= count; c++)
                    ret.add(new AminoAcidRestrictedImmonium(f, l.LossyMass, c, l.LossingAminoAcids, l.Name,l.LossID));

            }
        }
        if (insert)
            fragments.addAll(ret);
        return ret;
    }

    
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
        ArrayList<RegistredLoss> losses = (ArrayList<RegistredLoss>) conf.retrieveObject(AminoAcidRestrictedImmonium.class);
        ArrayList<Fragment> ret = new ArrayList<Fragment>(fragments.size());
        ArrayList<Fragment> base = fragments;
        if (fragments.isEmpty())
            return ret;

        int maxTotalLossCount = (int) conf.retrieveObject("MAXTOTALLOSSES", AbstractRunConfig.DEFAULT_MAX_TOTAL_LOSSES);
        int maxLossCount = (int) conf.retrieveObject("MAXLOSSES", AbstractRunConfig.DEFAULT_MAX_LOSSES);
        // linear match -> we can just take the first fragment and get the peptide from it
        if (crosslinker == null) {
            Peptide p = base.get(0).getPeptide();

            for (RegistredLoss l: losses) {
                boolean h = p.containsAminoAcids(l.LossingAminoAcids);
                if (l.LossCTerminal) h=true;
                if (l.LossNTerminal) h=true;

                if (h) {
                    ret.add(new AminoAcidRestrictedImmonium(new PeptideIon(p), l.LossyMass, 1, l.LossingAminoAcids, l.Name, l.LossID));
                }
            }
        } else {

            HashSet<RegistredLoss> found = new HashSet<RegistredLoss>();

            for (Fragment f : base) {
                if (f.getFragmentationSites().length == 0)
                    for (RegistredLoss l: losses) if (!found.contains(l)) {

                        // any fragment, that contains S,T,E or D can throw the according number of water
                        boolean h = f.containsAminoAcids(l.LossingAminoAcids);
                        if (l.LossCTerminal) h=true;
                        if (l.LossNTerminal) h=true;

                        if (h) {
                            ret.add(new AminoAcidRestrictedImmonium(f, l.LossyMass, 1, l.LossingAminoAcids, l.Name, l.LossID));
                            found.add(l);
                        }
                    }
            }
        }
        if (insert)
            fragments.addAll(ret);
        return ret;

    }

    public static boolean register() {
        try {
            Loss.registerLossClass(AminoAcidRestrictedImmonium.class);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(AminoAcidRestrictedImmonium.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }



    @Override
    public String name() {
        return m_name;
    }



    public static void parseArgs(String args, RunConfig conf) throws ParseException {

        HashSet<AminoAcid> LossingAminoAcids = null;
        double LossyMass = Double.NaN;
        boolean LossCTerminal = false;
        boolean LossNTerminal = false;
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
                for(String b : amino_acids)
                    LossingAminoAcids.add(conf.getAminoAcid(b));
            } else if (aName.contentEquals("name")) {
                name = ap[1].trim();
            } else if (aName.contentEquals("nterm")) {
                LossNTerminal = true;
            } else if (aName.contentEquals("cterm")) {
                LossCTerminal = true;
            } else if (aName.contentEquals("mass")) {
                LossyMass = Double.parseDouble(ap[1]);
            } else if (aName.contentEquals("id")) {
                lossID = Integer.parseInt(ap[1].trim());
            }

        }

        if (name == null) {
            throw new ParseException("AminoAcidRestrictedImmonium without Name: " + args, 0);
        }
        if (LossingAminoAcids == null || LossingAminoAcids.size() == 0) {
            throw new ParseException("AminoAcidRestrictedImmonium without Amiono Acids: " + args, 0);
        }
        if (Double.isNaN(LossyMass)) {
            throw new ParseException("AminoAcidRestrictedImmonium without mass: " + args, 0);
        }

        registerLoss(LossingAminoAcids,LossyMass,LossCTerminal,LossNTerminal,name,conf,lossID);

    }




}
