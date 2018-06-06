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
package rappsilber.ms.sequence.digest;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.RunConfig;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.AminoAcidSequence;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.Sequence;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class AAConstrainedDigestion extends Digestion implements AAConstrained, AASpecificity{

    /**
     * before which amino acids can not be cleaved
     */
    protected HashSet<AminoAcid> m_NTermConstrainingAminoAcids;
    protected HashSet<AminoAcid> m_CTermConstrainingAminoAcids;

    /**
     * creates a new enzyme
     * @param DigestedAminoAcids after which amino acid can the enzyme cleave
     * @param ConstrainingAminoAcids before which it can't
     * @param misscleavages consider how many miss-cleavages
     */
    public AAConstrainedDigestion (AminoAcid[] NTermDigestedAminoAcids, AminoAcid[] CTermDigestedAminoAcids,
                                        AminoAcid[] NTermConstrainingAminoAcids, AminoAcid[] CTermConstrainingAminoAcids, RunConfig config) {
        super(CTermDigestedAminoAcids, NTermDigestedAminoAcids, config);
        
        m_NTermConstrainingAminoAcids = new HashSet<AminoAcid>(NTermConstrainingAminoAcids.length);
        m_NTermConstrainingAminoAcids.addAll(Arrays.asList(NTermConstrainingAminoAcids));

        m_CTermConstrainingAminoAcids = new HashSet<AminoAcid>(CTermConstrainingAminoAcids.length);
        m_CTermConstrainingAminoAcids.addAll(Arrays.asList(CTermConstrainingAminoAcids));
        
    }

    @Override
    protected boolean isCleavageSite(AminoAcidSequence seq, int AAPos) {
        boolean ret = false;
        try {
            if (m_CTermAminoAcids.contains(seq.nonLabeledAminoAcidAt(AAPos))) {
                ret = !m_CTermConstrainingAminoAcids.contains(seq.nonLabeledAminoAcidAt(AAPos+1));
            }
            if (m_NTermAminoAcids.contains(seq.nonLabeledAminoAcidAt(AAPos+1))) {
                ret |= !m_NTermConstrainingAminoAcids.contains(seq.nonLabeledAminoAcidAt(AAPos));
            }
            return ret;
        } catch (ArrayIndexOutOfBoundsException ex) {
            return true;
        }
    }

    @Override
    protected boolean isCleavagePair(Sequence seq, int AAPos1, int AAPos2) {
        boolean ret = false;
        try {
            if (m_CTermAminoAcids.contains(seq.nonLabeledAminoAcidAt(AAPos1))) {
                ret = !m_CTermConstrainingAminoAcids.contains(seq.nonLabeledAminoAcidAt(AAPos1+1));
                
            }
            
            if ((!ret) && m_NTermAminoAcids.contains(seq.nonLabeledAminoAcidAt(AAPos1+1))) {
                ret = !m_NTermConstrainingAminoAcids.contains(seq.nonLabeledAminoAcidAt(AAPos1));
            }
            
            if (ret) {
                if (m_CTermAminoAcids.contains(seq.nonLabeledAminoAcidAt(AAPos2))) {
                    ret = !m_CTermConstrainingAminoAcids.contains(seq.nonLabeledAminoAcidAt(AAPos2+1));

                }

                if ((!ret) && m_NTermAminoAcids.contains(seq.nonLabeledAminoAcidAt(AAPos2+1))) {
                    ret = !m_NTermConstrainingAminoAcids.contains(seq.nonLabeledAminoAcidAt(AAPos2));
                }
            }
            
            return ret;
            
        } catch (ArrayIndexOutOfBoundsException ex) {
            return true;
        }
    }

    public HashSet<AminoAcid> getConstrainingAminoAcids() {
        HashSet<AminoAcid> aa = new HashSet<AminoAcid>(m_CTermConstrainingAminoAcids);
        aa.addAll(m_NTermConstrainingAminoAcids);
        return aa;
    }
    
    public HashSet<AminoAcid> getAminoAcidSpecificity() {
        HashSet<AminoAcid> aa = new HashSet<AminoAcid>(m_CTermAminoAcids);
        aa.addAll(m_NTermAminoAcids);
        return aa;
    }
    
    /**
     * test whether a peptide could have been be the result of this digestion
     * @param p
     * @return
     */
    @Override
    public boolean isDigestedPeptide(Peptide p) {
        
        Sequence s = p.getSequence();
        int sl = s.length();
        int start = p.getStart();
        
        AminoAcid startAA = p.aminoAcidAt(0);
        
        int prev = p.getStart() - 1;
        int next = p.getStart() + p.length();
        int end = next -1;
        AminoAcid endAA = p.aminoAcidAt(p.length() - 1);
        boolean nret = false;
        
        if (prev>=0) {
            AminoAcid prevAA = s.aminoAcidAt(prev);
            
            if (m_CTermAminoAcids.contains(prevAA) && !m_CTermConstrainingAminoAcids.contains(startAA)) {
                nret = true;
            } else  {
                if (m_NTermAminoAcids.contains(startAA) && !m_NTermConstrainingAminoAcids.contains(prevAA)) {
                    nret = true;
                }
            }
        } else {
            nret = true;
        }
        
        if (nret) {
            boolean cret = false;

            if (next < sl) {
                AminoAcid nextAA = s.aminoAcidAt(next);

                if (m_CTermAminoAcids.contains(endAA) && !m_CTermConstrainingAminoAcids.contains(nextAA)) {
                    return true;
                } else  {
                    
                    if (m_NTermAminoAcids.contains(nextAA) && !m_NTermConstrainingAminoAcids.contains(endAA)) {
                        return true;
                    }
                    
                }
                return false;
                
            }
            return true;
        }
        return false;
    }

//    public static Digestion parseArgs(String args) throws ParseException {
//        
//        // Complete this and return a PostAAConstrainedDigestion
//        ArrayList<AminoAcid> NTermDigestedAminoAcids = new ArrayList<AminoAcid>();
//        ArrayList<AminoAcid> NTermConstrainingAminoAcids = new ArrayList<AminoAcid>();
//        ArrayList<AminoAcid> CTermDigestedAminoAcids = new ArrayList<AminoAcid>();
//        ArrayList<AminoAcid> CTermConstrainingAminoAcids = new ArrayList<AminoAcid>();
//
//        // parses something like: DigestedAminoAcids:R,K;ConstrainingAminoAcids:P
//        String[] options = args.split(";");
//        int mc = -1;
//        for (String a : options) {
//            // Strip the string of whitespace and make it uppercase for comparison
//            String x = (a.trim()).toUpperCase();
//            // the amino acid substring
//            String aa_substring = x.substring(x.indexOf(":") + 1);
//
//            String[] amino_acids = aa_substring.split(",");
//            if( x.startsWith("NTERMDIGEST")) {
//                for(String b : amino_acids)
//                    NTermDigestedAminoAcids.add(AminoAcid.getAminoAcid(b));
//            } else if( x.startsWith("CTERMDIGEST") ){
//                for(String b : amino_acids)
//                    CTermDigestedAminoAcids.add(AminoAcid.getAminoAcid(b));
//            } else if( x.startsWith("NTERMDIGESTCONSTRAINT")) {
//                // Deal with the restricting AAs
//                for(String b : amino_acids)
//                    NTermConstrainingAminoAcids.add(AminoAcid.getAminoAcid(b));
//            } else if( x.startsWith("CTERMDIGESTCONSTRAINT")) {
//                // Deal with the restricting AAs
//                for(String b : amino_acids)
//                    CTermConstrainingAminoAcids.add(AminoAcid.getAminoAcid(b));
//            } else if( x.startsWith("MISSEDCLEAVAGES")) {
//                mc=Integer.parseInt(aa_substring);
//            } else {
//                throw new ParseException("Could not read type of Digested AA's from config file, " +
//                        " read: '" + args +"'", 0);
//            }
//        }
//        AminoAcid aas[]= new AminoAcid[0];
//        AAConstrainedDigestion ret = new AAConstrainedDigestion(NTermDigestedAminoAcids.toArray(aas), CTermDigestedAminoAcids.toArray(aas), NTermConstrainingAminoAcids.toArray(aas), CTermConstrainingAminoAcids.toArray(aas));
//        if (mc>=0)
//            ret.setMaxMissCleavages(mc);
//        return ret;
//    }

    public static Digestion parseArgs(String args, RunConfig conf) throws ParseException {
        
        // Complete this and return a PostAAConstrainedDigestion
        ArrayList<AminoAcid> NTermDigestedAminoAcids = new ArrayList<AminoAcid>();
        ArrayList<AminoAcid> NTermConstrainingAminoAcids = new ArrayList<AminoAcid>();
        ArrayList<AminoAcid> CTermDigestedAminoAcids = new ArrayList<AminoAcid>();
        ArrayList<AminoAcid> CTermConstrainingAminoAcids = new ArrayList<AminoAcid>();
        String name = "enzyme";

        // parses something like: DigestedAminoAcids:R,K;ConstrainingAminoAcids:P
        String[] options = args.split(";");
        for (String a : options) {
            // Strip the string of whitespace and make it uppercase for comparison
            String x = (a.trim()).toUpperCase();
            // the amino acid substring
            String aa_substring = x.substring(x.indexOf(":") + 1);

            String[] amino_acids = aa_substring.split(",");
            if( x.startsWith("NTERMDIGEST")) {
                for(String b : amino_acids)
                    NTermDigestedAminoAcids.add(conf.getAminoAcid(b));
            } else if( x.startsWith("CTERMDIGEST") ){
                for(String b : amino_acids)
                    CTermDigestedAminoAcids.add(conf.getAminoAcid(b));
            } else if( x.startsWith("NTERMDIGESTCONSTRAINT")) {
                // Deal with the restricting AAs
                for(String b : amino_acids)
                    NTermConstrainingAminoAcids.add(conf.getAminoAcid(b));
            } else if( x.startsWith("CTERMDIGESTCONSTRAINT")) {
                // Deal with the restricting AAs
                for(String b : amino_acids)
                    CTermConstrainingAminoAcids.add(conf.getAminoAcid(b));
            }else if (x.startsWith("NAME")){
                name = aa_substring;
            } else {
                throw new ParseException("Could not read type of Digested AA's from config file, " +
                        " read: '" + args +"'", 0);
            }
        }
        AminoAcid aas[]= new AminoAcid[0];
        
        AAConstrainedDigestion d =  new AAConstrainedDigestion(NTermDigestedAminoAcids.toArray(aas), CTermDigestedAminoAcids.toArray(aas), NTermConstrainingAminoAcids.toArray(aas), CTermConstrainingAminoAcids.toArray(aas),conf);


        d.setName(name);

        return d;
    }


}
