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

import rappsilber.config.RunConfig;
import rappsilber.ms.sequence.AminoAcidSequence;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.digest.Digestion;
import rappsilber.ms.sequence.ions.Fragment;

/**
 * Dummy cross-linker cross-links everything to everything but with infinite mass
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class LinearCrosslinker extends CrossLinker{
    private static final String defualtname = "LinearCrosslinker";

    public LinearCrosslinker() {
        this(defualtname);
    }

    public LinearCrosslinker(String name) {
        super(name, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    @Override
    public boolean canCrossLink(AminoAcidSequence p, int linkSide) {
        return false;
    }

    @Override
    public boolean canCrossLink(AminoAcidSequence p1, int linkSide1, AminoAcidSequence p2, int linkSide2) {
        return false;
    }

    @Override
    public boolean canCrossLink(AminoAcidSequence p1, AminoAcidSequence p2) {
        return false;
    }
    
    
    

    /**
     * tests whether it can crosslink and digested
     * for now it just makes sure that a reduced peptide is linkable
     * but should be send back to the digestion method
     * @param p
     * @param digest
     * @return
     */
    @Override
    public boolean canCrosslinkDigestable(Peptide p, Digestion digest) {
        return digest.isDigestedPeptide(p);
    }


    @Override
    public boolean canCrossLink(AminoAcidSequence p) {
        return false;
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
    public static LinearCrosslinker parseArgs(String args) {
        return new LinearCrosslinker();
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
    public static LinearCrosslinker parseArgs(String args, RunConfig config) {
        String name = defualtname;
        int dbid =1;
        for (String arg : args.split(";")) {
            String[] argParts = arg.split(":");
            String argName = argParts[0].toUpperCase();
            if (argName.contentEquals("NAME")) {
                name = argParts[1];
            } else if (argName.contentEquals("ID")) {
                dbid = Integer.parseInt(argParts[1]);
            }
        }

        LinearCrosslinker dxl =  new LinearCrosslinker(name);
        dxl.setDBid(dbid);
        return dxl;
    }

    @Override
    public double getWeight(Peptide pep, int position) {
        return 0;
    }

    @Override
    public boolean canCrossLinkMoietySite(AminoAcidSequence p, int moietySite) {
        return false;
    }
    
    @Override
    public boolean canCrossLinkMoietySite(Fragment p, int moietySite) {
        return false;
    }

    @Override
    public boolean canCrossLink(Fragment p, int linkSide) {
        return false;
    }

    @Override
    public boolean canCrossLink(Fragment p1, int linkSide1, Fragment p2, int linkSide2) {
        return false;
    }



    @Override
    public boolean linksCTerminal(int site) {
        return false;
    }

    @Override
    public boolean linksNTerminal(int site) {
        return false;
    }



}
