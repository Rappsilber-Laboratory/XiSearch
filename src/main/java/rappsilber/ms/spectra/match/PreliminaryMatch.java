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
package rappsilber.ms.spectra.match;

import java.util.ArrayList;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.SequenceList;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class PreliminaryMatch extends ArrayList<Peptide>{
    private static final long serialVersionUID = 960775992864324965L;
    Peptide m_alpha;
    double  m_alphaScore;
    int     m_crosslinkerid = -1;

    public PreliminaryMatch(Peptide alpha, double alphaScore) {
        m_alpha = alpha;
        m_alphaScore = alphaScore;
    }

    public PreliminaryMatch(Peptide alpha, double alphaScore, int crosslinkerID) {
        this(alpha, alphaScore);
        m_crosslinkerid = crosslinkerID;
    }

    public PreliminaryMatch(Peptide alpha, double alphaScore, ArrayList<Peptide> beta) {
        super(beta);
        m_alpha = alpha;
        m_alphaScore = alphaScore;
    }

    public PreliminaryMatch(Peptide alpha, double alphaScore, ArrayList<Peptide> beta, int crosslinkerID) {
        this(alpha, alphaScore, beta);
        m_crosslinkerid = crosslinkerID;
    }


    /**
     * parses a string, that contains a description - of a preliminary match
     * String format should follow:
     * alpha:id;score:beta:id1;id2;id3:crosslinker:id
     * @param l
     * @param matches
     */
    public PreliminaryMatch(SequenceList l, String matches) {
        String[] parts = matches.split(":");
        int alphaID = -1;
        for (int p =0 ; p< parts.length; p++ ) {

            if (parts[p].toLowerCase().contentEquals("alpha")) {
                p++;
                String[] alphaInfo = parts[p].split(";");
                alphaID = Integer.parseInt(alphaInfo[0]);
                m_alpha = l.getPeptide(alphaID);
                m_alphaScore = Double.parseDouble(alphaInfo[1]);

            } else if (parts[p].toLowerCase().contentEquals("beta")) {
                p++;
                String[] sBetaIDs = parts[p].split(";");
                for (int b = 0; b< sBetaIDs.length; b++) {
                    if (sBetaIDs[b].length() > 0) {
                        int betaID = Integer.parseInt(sBetaIDs[b]);
                        this.add(l.getPeptide(betaID));
                    }
                }
            } else if (parts[p].toLowerCase().contentEquals("crosslinker")) {
                p++;
                m_crosslinkerid = Integer.parseInt(parts[p]);
            }
        }
    }
    
    public Peptide getAlpha() {
        return m_alpha;
    }

    public double getAlphaScore() {
        return m_alphaScore;
    }

    public int getCrosslinkerID() {
        return m_crosslinkerid;
    }


    @Override
    public String toString() {
        StringBuffer ret = new StringBuffer("alpha:" + m_alpha.getPeptideIndex() + ";" + m_alphaScore);
        if (this.size() > 0) {
            ret.append(":beta:");
            for (Peptide p : this) {
                ret.append(p.getPeptideIndex() + ";");
            }
        }
        if (m_crosslinkerid >= 0) {
            ret.append(":crosslinker:" + m_crosslinkerid);
        }
        return ret.toString();

    }



}
