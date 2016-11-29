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

import java.util.HashSet;

/**
 * defines a standart set of functions to be provided by all sequence-list elements
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public interface AminoAcidSequence extends Iterable<AminoAcid>{

    /**
     * queries the aminoacid at the given position
     * @param pos position of the residue (0 to size - 1)
     * @return the AminoAcid or Modification at this position
     */
    AminoAcid aminoAcidAt(int pos);

    /**
     * queries the aminoacid at the given position
     * @param pos position of the residue (0 to size - 1)
     * @return the AminoAcid or Modification at this position
     */
    AminoAcid nonLabeledAminoAcidAt(int pos);



    /**
     * changes the aminoacid at the given position
     * @param pos position of the residue (0 to size - 1)
     * @param aa the new amino acid
     * @return the old AminoAcid or Modification at this position
     */
    AminoAcid setAminoAcidAt(int pos, AminoAcid aa);

    /**
     * replaces all aminoacids of type oldAA with newAA
     * @param oldAA
     * @param newAA
     * @return
     */
    int replace(AminoAcid oldAA, AminoAcid newAA);

    int replace(AminoModification AAM);

    /**
     * returns the length of the sequence in aminoacids
     * @return length
     */
    int length();

    /**
     * if the sequence is a subsequence of another one, return the start position
     * or 0 otherwise
     * @return start of the sequence
     */
    int getStart();

    /**
     * returns whther a certain type of amino-acid is part of the sequence.
     * a modification is not counted as the base-aminoacid
     * @param aa the amino acid in question
     * @return true: contains the amino acid; false : does not
     */
    boolean containsAminoAcid(AminoAcid aa);

    /**
     * returns an part of the sequence
     * @param from position of the first residue of the subsequence
     * @param length length opf the subsequence
     * @return
     */
    AminoAcidSequence subSequence(short from, short length);

    /**
     * counts how many aminoacids of the secuence are of any of the given types
     * @param aas a HashSet of all aminoacids to be counted
     * @return numbner of occurence
     */
    int countAminoAcid(HashSet<AminoAcid> aas);

    /**
     * returns the sequence as an array of amino acids
     * @return Array of amino acids that make up the sequence
     */
    public AminoAcid[] toArray();

    boolean isNTerminal();

    boolean isCTerminal();

    /**
     * The most parent sequence, from which this sequence was derived
     * Typically a protein.
     * @return the SourceSequence
     */
    public AminoAcidSequence getSourceSequence();
}
