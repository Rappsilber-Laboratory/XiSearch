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
package rappsilber.ms.sequence.ions;

import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.Peptide;

/**
 *  "struct" for storing the fragmentation residues
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class FragmentationSite {
    public AminoAcid NTerm;
    public AminoAcid CTerm;
    public Peptide  peptide;
    public int      site;

    public FragmentationSite(AminoAcid n, AminoAcid c, Peptide p, int site) {
        NTerm = n;
        CTerm = c;
        peptide = p;
        this.site = site;
    }

    public FragmentationSite(Peptide p, int site) {
        NTerm = p.aminoAcidAt(site);
        CTerm = p.aminoAcidAt(site + 1);
        peptide = p;
        this.site = site;
    }

//    public FragmentationSite clone(int linkSite) {
//        AminoAcid NTerm_new = NTerm;
//        AminoAcid CTerm_new = CTerm;
//        if (site == 0 && !NTerm_new.SequenceID.contains("nt")) {
//            NTerm_new = AminoAcid.getAminoAcid(NTerm.SequenceID + "nt");
//            if (NTerm_new == null) {
//                NTerm_new = new AminoAcid(NTerm.SequenceID + "nt", NTerm.mass);
//                NTerm_new.register();
//            }
//        }
//        if (site == peptide.length() - 2 && !CTerm_new.SequenceID.contains("ct")) {
//            NTerm_new = AminoAcid.getAminoAcid(CTerm.SequenceID + "ct");
//            if (CTerm_new == null) {
//                CTerm_new = new AminoAcid(CTerm.SequenceID + "ct", CTerm.mass);
//                CTerm_new.register();
//            }
//        }
//
//        if (site == peptide.length() - 2 && !CTerm_new.SequenceID.contains("ct")) {
//            NTerm_new = AminoAcid.getAminoAcid(CTerm.SequenceID + "ct");
//            if (CTerm_new == null) {
//                CTerm_new = new AminoAcid(CTerm.SequenceID + "ct", CTerm.mass);
//                CTerm_new.register();
//            }
//        }
//
//        if (site == linkSite && !NTerm_new.SequenceID.contains("xl")) {
//            AminoAcid xl = AminoAcid.getAminoAcid(NTerm.SequenceID + "xl");
//            if (xl == null) {
//                xl = new AminoAcid(NTerm.SequenceID + "xl", NTerm.mass);
//                xl.register();
//            }
//            NTerm_new = xl;
//        }
//
//        if (site == linkSite - 1 && !CTerm_new.SequenceID.contains("xl")) {
//            AminoAcid xl = AminoAcid.getAminoAcid(CTerm.SequenceID + "xl");
//            if (xl == null) {
//                xl = new AminoAcid(CTerm.SequenceID + "xl", CTerm.mass);
//                xl.register();
//            }
//            CTerm_new = xl;
//        }
//
//        return new FragmentationSite(NTerm_new, CTerm_new, peptide, site);
//
//    }
//
//    public FragmentationSite clone(int linkSite, RunConfig config) {
//        AminoAcid NTerm_new = NTerm;
//        AminoAcid CTerm_new = CTerm;
//        if (site == 0 && !NTerm_new.SequenceID.contains("nt")) {
//            NTerm_new = config.getAminoAcid(NTerm.SequenceID + "nt");
//            if (NTerm_new == null) {
//                NTerm_new = new AminoAcid(NTerm.SequenceID + "nt", NTerm.mass);
//                config.register(NTerm_new);
//            }
//        }
//        if (site == peptide.length() - 2 && !CTerm_new.SequenceID.contains("ct")) {
//            NTerm_new = config.getAminoAcid(CTerm.SequenceID + "ct");
//            if (CTerm_new == null) {
//                CTerm_new = new AminoAcid(CTerm.SequenceID + "ct", CTerm.mass);
//                config.register(CTerm_new);
//            }
//        }
//
//        if (site == peptide.length() - 2 && !CTerm_new.SequenceID.contains("ct")) {
//            NTerm_new = config.getAminoAcid(CTerm.SequenceID + "ct");
//            if (CTerm_new == null) {
//                CTerm_new = new AminoAcid(CTerm.SequenceID + "ct", CTerm.mass);
//                config.register(CTerm_new);
//            }
//        }
//
//        if (site == linkSite && !NTerm_new.SequenceID.contains("xl")) {
//            AminoAcid xl = config.getAminoAcid(NTerm.SequenceID + "xl");
//            if (xl == null) {
//                xl = new AminoAcid(NTerm.SequenceID + "xl", NTerm.mass);
//                config.register(xl);
//            }
//            NTerm_new = xl;
//        }
//
//        if (site == linkSite - 1 && !CTerm_new.SequenceID.contains("xl")) {
//            AminoAcid xl = config.getAminoAcid(CTerm.SequenceID + "xl");
//            if (xl == null) {
//                xl = new AminoAcid(CTerm.SequenceID + "xl", CTerm.mass);
//                config.register(xl);
//            }
//            CTerm_new = xl;
//        }
//
//        return new FragmentationSite(NTerm_new, CTerm_new, peptide, site);
//
//    }

}
