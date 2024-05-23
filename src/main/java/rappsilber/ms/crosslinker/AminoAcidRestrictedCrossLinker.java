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
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.Peptide;

/**
 * base class for crosslinker, that link to a set of given amino acids
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public abstract class AminoAcidRestrictedCrossLinker extends CrossLinker{

    /** all amino-acids, that can be cross-linked */
    protected HashMap<AminoAcid,Double> m_linkable;
    /** can the cross-linker react with the N-terminal */
    protected boolean            m_NTerminal = true;
    protected double             m_NTerminalWeight = 1;
    /** can the cross-linker react with the C-terminal */
    protected boolean            m_CTerminal = false;
    protected double             m_CTerminalWeight = 1;

    /**
     * creates and registers a new crosslinker (- definition)
     * @param Name
     * @param BaseMass
     * @param CrossLinkedMass
     * @param linkableAminoAcids
     */
    public AminoAcidRestrictedCrossLinker (String Name, double BaseMass, double CrossLinkedMass, HashSet<AminoAcid> linkableAminoAcids) {
        super(Name, BaseMass, CrossLinkedMass);
        m_linkable = new HashMap<AminoAcid, Double>();
        for (AminoAcid aa : linkableAminoAcids) {
            m_linkable.put(aa, 1.0);
            
            // I use this as part of the workaround for nary cross-links
            //m_linkable.put(AminoAcid.X, 1.0);
        }
    }    

    /**
     * creates and registers a new crosslinker (- definition)
     * @param Name
     * @param BaseMass
     * @param CrossLinkedMass
     * @param linkableAminoAcids
     */
    public AminoAcidRestrictedCrossLinker (String Name, double BaseMass, double CrossLinkedMass, HashMap<AminoAcid,Double> linkableAminoAcids) {
        super(Name, BaseMass, CrossLinkedMass);
        m_linkable = linkableAminoAcids;
    }

    /**
     * creates and registers a new crosslinker (- definition)
     * @param Name
     * @param BaseMass
     * @param CrossLinkedMass
     * @param linkableAminoAcids
     */
    public AminoAcidRestrictedCrossLinker (String Name, double BaseMass, double CrossLinkedMass, AminoAcid[] linkableAminoAcids) {
        super(Name, BaseMass, CrossLinkedMass);
        HashMap<AminoAcid,Double> linkable = new HashMap<AminoAcid,Double>(linkableAminoAcids.length);

        for (AminoAcid aa : linkableAminoAcids) {
            linkable.put(aa, 1.0);
        }

        m_linkable = linkable;
    }


    public double getAminoAcidWeight(AminoAcid AA) {
        Double w = 0d;
        if (!m_linkable.isEmpty()) {
            w = m_linkable.get(AA);
        }      
        return w == null ? Double.POSITIVE_INFINITY : w;
    }


    public void setlinksNTerm(boolean linksNTerm) {
        m_NTerminal = linksNTerm;
    }
    
    public void setNTermWeight(double w) {
        m_NTerminalWeight = w;
    }

    public void setlinksCTerm(boolean linksCTerm) {
        m_CTerminal = linksCTerm;
    }

    public void setCTermWeight(double w) {
        m_CTerminalWeight = w;
    }

    @Override
    public double getWeight(Peptide pep, int position) {

        double aaw = getAminoAcidWeight(pep.nonLabeledAminoAcidAt(position));

        if (position==0 && m_NTerminal && pep.isNTerminal()) {
            return Math.min(m_NTerminalWeight, aaw);
        }

        if (position==pep.length()-1 && m_CTerminal && pep.isCTerminal()) {
            return Math.min(m_CTerminalWeight, aaw);
        }

        return aaw;

    }
    
    
    public Set<AminoAcid> getAASpecificity(int site) {
        return m_linkable.keySet();
    }

    @Override
    public boolean linksCTerminal(int site) {
        return m_CTerminal;
    }

    @Override
    public boolean linksNTerminal(int site) {
        return m_NTerminal;
    }

}
