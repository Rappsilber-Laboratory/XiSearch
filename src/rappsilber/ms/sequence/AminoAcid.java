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
package rappsilber.ms.sequence;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class AminoAcid implements Comparable{

    public String SequenceID;
    public double mass;

    public static double  MINIMUM_MASS = Double.MAX_VALUE;

    //default aminoacids
    /** Alanin */
	public static final AminoAcid A = new AminoAcid("A", 71.037114).register();
    /** Cysteine */
	public static final AminoAcid C = new AminoAcid("C", 103.009184).register();
    /** Aspartic acid */
	public static final AminoAcid D = new AminoAcid("D", 115.026943).register();
    /** Glutamic acid */
	public static final AminoAcid E = new AminoAcid("E", 129.042593).register();
    /** Phenylalanine */
	public static final AminoAcid F = new AminoAcid("F", 147.068414).register();
    /** Glycine */
	public static final AminoAcid G = new AminoAcid("G", 57.021464).register();
    /** Histidine */
	public static final AminoAcid H = new AminoAcid("H", 137.058912).register();
    /** Isoleucine (leucin with a twist :)*/
	public static final AminoAcid I = new AminoAcid("I", 113.084064).register();
    /** Lysin */
	public static final AminoAcid K = new AminoAcid("K", 128.094963).register();
    /** Leucine */
	public static final AminoAcid L = new AminoAcid("L", 113.084064).register();
    /** Methionine */
	public static final AminoAcid M = new AminoAcid("M", 131.040485).register();
    /** Asparagine */
	public static final AminoAcid N = new AminoAcid("N", 114.042927).register();
    /** Proline */
	public static final AminoAcid P = new AminoAcid("P", 97.052764).register();
    /** Glutamine */
	public static final AminoAcid Q = new AminoAcid("Q", 128.058578).register();
    /** Argenine */
	public static final AminoAcid R = new AminoAcid("R", 156.101111).register();
    /** Serine */
	public static final AminoAcid S = new AminoAcid("S", 87.032028).register();
    /** Threonine */
	public static final AminoAcid T = new AminoAcid("T", 101.047678).register();
    /** Valine */
	public static final AminoAcid V = new AminoAcid("V", 99.068414).register();
    /** Tryptophan */
	public static final AminoAcid W = new AminoAcid("W", 186.079313).register();
    /** Tyrosine */
	public static final AminoAcid Y = new AminoAcid("Y", 163.063329).register();
//    /** C- or N-Terminal */
//	public static final AminoAcid CNTERMINAL = new AminoAcid(".", 0).register();


//	public static final AminoAcid A = new AminoAcid("A", 71.03711).register();
//	public static final AminoAcid C = new AminoAcid("C", 103.00919).register();
//	public static final AminoAcid D = new AminoAcid("D", 115.02694).register();
//	public static final AminoAcid E = new AminoAcid("E", 129.04259).register();
//	public static final AminoAcid F = new AminoAcid("F", 147.06841).register();
//	public static final AminoAcid G = new AminoAcid("G", 57.02146).register();
//	public static final AminoAcid H = new AminoAcid("H", 137.05891).register();
//	public static final AminoAcid I = new AminoAcid("I", 113.08406).register();
//	public static final AminoAcid K = new AminoAcid("K", 128.09496).register();
//	public static final AminoAcid L = new AminoAcid("L", 113.08406).register();
//	public static final AminoAcid M = new AminoAcid("M", 131.04049).register();
//	public static final AminoAcid N = new AminoAcid("N", 114.04293).register();
//	public static final AminoAcid P = new AminoAcid("P", 97.05276).register();
//	public static final AminoAcid Q = new AminoAcid("Q", 128.05858).register();
//	public static final AminoAcid R = new AminoAcid("R", 156.10111).register();
//	public static final AminoAcid S = new AminoAcid("S", 87.03203).register();
//	public static final AminoAcid T = new AminoAcid("T", 101.04768).register();
//	public static final AminoAcid V = new AminoAcid("V", 99.06841).register();
//	public static final AminoAcid W = new AminoAcid("W", 186.07931).register();
//	public static final AminoAcid Y = new AminoAcid("Y", 163.06333).register();

    //"strange" aminoacids
    /** Selenocysteine */
	public static final AminoAcid U = new AminoAcid("U", 168.053).register();

    /** Pyrrolysine */
	public static final AminoAcid O = new AminoAcid("O", 255.31).register();

    //aminoacids aliases - only usefull with modification
    /** asparagine or aspartic acid */
	public static final AminoAcid B = new AminoAcid("B", N.mass).register();
    /** glutamine or glutamic acid */
	public static final AminoAcid Z = new AminoAcid("Z", G.mass).register();

    // TODO sequnecees with X are rather hard to implement
    // for now peptides containing X get ignored
	public static final AminoAcid X = new AminoAcid("X", Double.POSITIVE_INFINITY).register();



    /** the list of registered AminoAcids - and aminoacids derivates */
    private static HashMap<String,AminoAcid> m_RegisteredAminoAcids;

    /** 
     * creates a new aminoacid of the given mass
     * @param ID id of the new amino acid
     * @param Mass mass of the new amino acid
     */
    public AminoAcid(String ID, double Mass) {
        SequenceID = ID;
        mass = Mass;
    }


    /**
     * registers the current amino acid
     * @return the aminoacid itself
     */
    public AminoAcid register() {
        if (m_RegisteredAminoAcids == null)
            m_RegisteredAminoAcids = new HashMap(23);

        m_RegisteredAminoAcids.put(SequenceID, this);
        
        if (MINIMUM_MASS > this.mass)
            MINIMUM_MASS = this.mass;
        
        return this;
    }

    /**
     * The id of the AminoAcid
     * @return id
     */
    @Override
    public String toString() {
        return SequenceID;
    }


    /**
     * returns the amino acid with the given sequence id
     * @param sequenceID id of the requested aminoacid
     * @return the Amino Acid or null
     */
    public static AminoAcid getAminoAcid(String sequenceID) {
        return m_RegisteredAminoAcids.get(sequenceID);
    }



    /**
     * @return the list of all registered AminoAcids
     */
    public static Collection<AminoAcid> getRegisteredAminoAcids() {
        return m_RegisteredAminoAcids.values();
    }

    public static Comparator<AminoAcid> getSequenceIdComparator() {
        return new Comparator<AminoAcid>() {

            public int compare(AminoAcid o1, AminoAcid o2) {
                try {
                    return o1.toString().compareTo(o2.toString());
                } catch( Exception e) {
                    throw new Error(e);
                }
            }
        };
    }

    public static Comparator<AminoAcid> getSizeComparator() {
        return new Comparator<AminoAcid>() {

            public int compare(AminoAcid o1, AminoAcid o2) {
                try {
                    return Double.compare(o1.mass, o2.mass);
                } catch( Exception e) {
                    throw new Error(e);
                }
            }
        };
    }

    public int compareTo(Object o) {
        return this.toString().compareTo(o.toString());
    }
    
    @Override
    public int hashCode() {
        return SequenceID.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AminoAcid other = (AminoAcid) obj;
        if ((this.SequenceID == null) ? (other.SequenceID != null) : !this.SequenceID.equals(other.SequenceID)) {
            return false;
        }
        if (Double.doubleToLongBits(this.mass) != Double.doubleToLongBits(other.mass)) {
            return false;
        }
        return true;
    }

}
