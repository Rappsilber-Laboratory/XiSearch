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

import java.util.ArrayList;
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
public class PostAAConstrainedDigestion extends Digestion  implements AAConstrained, AASpecificity{

    /**
     * before which amino acids can not be cleaved
     */
    protected HashSet<AminoAcid> m_ConstrainingAminoAcids;

    /**
     * creates a new enzyme
     * @param DigestedAminoAcids after which amino acid can the enzym cleave
     * @param ConstrainingAminoAcids before which it can't
     * @param misscleavages considere how many misscleaviges
     */
    public PostAAConstrainedDigestion (AminoAcid[] DigestedAminoAcids,
                                        AminoAcid[] ConstrainingAminoAcids) {
        super(DigestedAminoAcids, new AminoAcid[0]);
        m_ConstrainingAminoAcids = new HashSet<AminoAcid>(ConstrainingAminoAcids.length);
        for (int i = 0; i< ConstrainingAminoAcids.length; i++) {
            m_ConstrainingAminoAcids.add(ConstrainingAminoAcids[i]);
        }
    }

    @Override
    protected boolean isCleavageSite(Sequence seq, int AAPos) {
        try {
            return super.isCleavageSite(seq, AAPos) &&
                    ! m_ConstrainingAminoAcids.contains(seq.nonLabeledAminoAcidAt(AAPos + 1));
        } catch (ArrayIndexOutOfBoundsException ex) {
            return true;
        }
    }

    @Override
    protected boolean isCleavageSite(AminoAcidSequence seq, int AAPos) {
        try {
            return super.isCleavageSite(seq, AAPos) &&
                    ! m_ConstrainingAminoAcids.contains(seq.nonLabeledAminoAcidAt(AAPos + 1));
        } catch (ArrayIndexOutOfBoundsException ex) {
            return true;
        }
    }
    
    public HashSet<AminoAcid> getConstrainingAminoAcids() {
        return new HashSet<AminoAcid>(m_ConstrainingAminoAcids);
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
        int start = p.getStart();
        int end = p.getStart() + p.length() - 1;

        return (start == 0 || (                // n-terminal
                m_CTermAminoAcids.contains(      // or specified aminoacid
                s.aminoAcidAt(start - 1))   // before the peptide
                &&
                !m_ConstrainingAminoAcids.contains(      // or specified aminoacid
                p.aminoAcidAt(0))   // before the peptide
                )
                &&
                (end ==  s.length() - 1 ||(  // c-terminal
                m_CTermAminoAcids.contains(      // or ends
                p.aminoAcidAt(p.length() - 1)) &&
                !m_ConstrainingAminoAcids.contains(      // or specified aminoacid
                s.aminoAcidAt(end + 1))   // before the peptide
                ))
                );       // with specified aminoacid
    }

    public static Digestion parseArgs(String args) {
        
        // Complete this and return a PostAAConstrainedDigestion
        ArrayList<AminoAcid> DigestedAminoAcids = new ArrayList<AminoAcid>();
        ArrayList<AminoAcid> ConstrainingAminoAcids = new ArrayList<AminoAcid>();

        // parses something like: DigestedAminoAcids:R,K;ConstrainingAminoAcids:P
        String[] options = args.split(";");
        for (String a : options) {
            // Strip the string of whitespace and make it uppercase for comparison
            String x = (a.trim()).toUpperCase();
            // the amino acid substring
            String aa_substring = x.substring(x.indexOf(":") + 1);

            String[] amino_acids = aa_substring.split(",");
            if( x.startsWith("DIGESTED") ){
                for(String b : amino_acids)
                    DigestedAminoAcids.add(AminoAcid.getAminoAcid(b));
            }else{
                // Deal with the restricting AAs
                for(String b : amino_acids)
                    ConstrainingAminoAcids.add(AminoAcid.getAminoAcid(b));
            }
        }
        return new PostAAConstrainedDigestion(DigestedAminoAcids.toArray(new AminoAcid[0]),
                ConstrainingAminoAcids.toArray(new AminoAcid[0]));
    }

    public static Digestion parseArgs(String args, RunConfig conf) {

        // Complete this and return a PostAAConstrainedDigestion
        ArrayList<AminoAcid> DigestedAminoAcids = new ArrayList<AminoAcid>();
        ArrayList<AminoAcid> ConstrainingAminoAcids = new ArrayList<AminoAcid>();
        String name = null;

        // parses something like: DigestedAminoAcids:R,K;ConstrainingAminoAcids:P
        String[] options = args.split(";");
        for (String a : options) {
            // Strip the string of whitespace and make it uppercase for comparison
            String x = (a.trim()).toUpperCase();
            // the amino acid substring
            String aa_substring = x.substring(x.indexOf(":") + 1);

            if( x.startsWith("DIGESTED") ){
                String[] amino_acids = aa_substring.split(",");
                for(String b : amino_acids)
                    try {
                        DigestedAminoAcids.add(conf.getAminoAcid(b));
                    } catch(Exception e) {
                        Logger.getLogger(PostAAConstrainedDigestion.class.getName()).log(Level.SEVERE, "Error while defining AminoAcids for digestion - will ignore \"" + b + "\" for digestion", e);
                    }
            }else if (x.startsWith("CONSTRAINING")){
                String[] amino_acids = aa_substring.split(",");
                // Deal with the restricting AAs
                for(String b : amino_acids)
                    try {
                        ConstrainingAminoAcids.add(conf.getAminoAcid(b));
                    } catch(Exception e) {
                        Logger.getLogger(PostAAConstrainedDigestion.class.getName()).log(Level.SEVERE, "Error while defining constraining AminoAcids for digestion - will ignore \"" + b + "\" as constrain", e);
                    }
            }else if (x.startsWith("NAME")){
                name = aa_substring;
            }
        }

        PostAAConstrainedDigestion d = new PostAAConstrainedDigestion(DigestedAminoAcids.toArray(new AminoAcid[0]),
                ConstrainingAminoAcids.toArray(new AminoAcid[0]));

        d.setName(name);

        return d;
    }

}
