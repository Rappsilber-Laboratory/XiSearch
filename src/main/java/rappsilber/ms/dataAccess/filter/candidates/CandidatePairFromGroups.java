/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rappsilber.ms.dataAccess.filter.candidates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.spectra.Spectra;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class CandidatePairFromGroups implements CandidatePairFilter  {
    HashMap<String,ArrayList<HashSet<String>>> proteinToGroups = new HashMap<>();

    public CandidatePairFromGroups(ArrayList<HashSet<String>> groups) {
        for (HashSet<String> group : groups) {
            HashSet<String> tdgroup = new HashSet<>(group);
            for (String a : group) {
                tdgroup.add("REV_"+a);
                tdgroup.add("RAN_"+a);
                ArrayList<HashSet<String>> maps = proteinToGroups.get(a);
                if (maps == null) {
                    maps = new ArrayList<>();
                    maps.add(tdgroup);
                    proteinToGroups.put(a,maps);
                    proteinToGroups.put("REV_" + a, maps);
                    proteinToGroups.put("RAN_" + a, maps);
                } else {
                    maps.add(tdgroup);
                }
            }
        }
    }

    @Override
    public boolean passes(Spectra s, CrossLinker xl, Peptide a, Peptide b) {
        for (Peptide.PeptidePositions pp1 : a.getPositions()) {
            for (Peptide.PeptidePositions pp2 : b.getPositions()) {
                String aAcc = pp1.base.getSplitFastaHeader().getAccession();
                String bAcc = pp2.base.getSplitFastaHeader().getAccession();
                ArrayList<HashSet<String>> maps  = proteinToGroups.get(aAcc.trim());
                for (HashSet<String> group : maps)
                    if (group.contains(bAcc.trim()))
                        return true;
            }
            
        }
        return false;
    }
    
    
            
}
