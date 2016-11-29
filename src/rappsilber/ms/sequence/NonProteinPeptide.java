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

import java.util.ArrayList;
import java.util.Collection;
import rappsilber.config.RunConfig;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.sequence.fasta.FastaHeader;


// <editor-fold defaultstate="collapsed" desc=" UML Marker "> 
// #[regen=yes,id=DCE.3BFAEC1E-6771-9AF9-184F-46CAA052FBD4]
// </editor-fold> 
/**
 * Represents a Peptide after digestion
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class NonProteinPeptide extends Peptide{

    ArrayList<AminoAcid>aminoacids;
    private boolean isNTerminal = false;
    private boolean isCTerminal = false;
    private static final Sequence random = new Sequence(new AminoAcid[0]);
    static {
        random.setFastaHeader("REV_RAN");
        random.setDecoy(true);
    }
    
    public NonProteinPeptide() {
        super();
    }
    
    
    
    public NonProteinPeptide(AminoAcid[] aa, PeptidePositions[] pos, boolean decoy ) {
        super(random.addSequence(aa),random.length()-aa.length,aa.length);
        this.getSequence().setDecoy(decoy);
        this.getSequence().getPeptides().add(this);
//        setPositions(pos);
//        setNterminalModification(nterm);
//        setCTerminalModification(cterm);
    }
    
    public NonProteinPeptide(Peptide p) {
        // creatre a new sequence, as the base for this peptide
        this(p.toArray(), p.getPositions(), p.isDecoy());
        FastaHeader fh = p.getSequence().getSplitFastaHeader();
        //this.getSequence().setFastaHeader(new FastaHeader(fh.getAccession(), fh.getAccession(), fh.getName(),"Randomized peptide" ));
        this.getSequence().getPeptides().add(this);
        this.setCTerminal(p.isCTerminal());
        this.setNTerminal(p.isNTerminal());
    }
    
    public static Peptide randomPeptide(Peptide p, RunConfig conf, boolean basicAminoAcids) {
        AminoAcid[] aaa;
        Collection<AminoAcid> allaa = conf.getAllAminoAcids();
        if (basicAminoAcids) {
            ArrayList<AminoAcid> baa = new ArrayList<AminoAcid>();
            for (AminoAcid aa : allaa) {
                if ((!(aa instanceof AminoLabel || aa instanceof AminoModification)) && aa.mass>0 && !Double.isInfinite(aa.mass))
                    baa.add(aa);
            }
            aaa = new AminoAcid[baa.size()];
            aaa = baa.toArray(aaa);
        } else {
            aaa = new AminoAcid[allaa.size()];
            aaa = allaa.toArray(aaa);
        }
        
        AminoAcid[] pepaa = new AminoAcid[p.length()];
        int c = p.length();
        for (int l = 0; l<c; l++) {
            int res = (int) (Math.random()*(aaa.length-1));
            pepaa[l] = aaa[res];
        }
        NonProteinPeptide npp =  new NonProteinPeptide(pepaa, p.getPositions(), p.isDecoy());
        FastaHeader fh=p.getSequence().getSplitFastaHeader();
        npp.getSequence().setFastaHeader(new FastaHeader(fh.getAccession() + " Randomized peptide", fh.getAccession(), fh.getName(),"Randomized peptide" ));
        npp.setCTerminal(p.isCTerminal());
        npp.setNTerminal(p.isNTerminal());
        return npp;
    }
//
//    public static Peptide randomMassPeptide(Peptide p, RunConfig conf, boolean basicAminoAcids,ToleranceUnit t, List<CrossLinker> linkers) {
//        AminoAcid[] aaa;
//        Collection<AminoAcid> allaa = conf.getAllAminoAcids();
//        if (basicAminoAcids) {
//            ArrayList<AminoAcid> baa = new ArrayList<AminoAcid>();
//            for (AminoAcid aa : allaa) {
//                if ((!(aa instanceof AminoLabel || aa instanceof AminoModification)) && aa.mass>0 && !Double.isInfinite(aa.mass))
//                    baa.add(aa);
//            }
//            aaa = new AminoAcid[baa.size()];
//            aaa = baa.toArray(aaa);
//        } else {
//            aaa = new AminoAcid[allaa.size()];
//            aaa = allaa.toArray(aaa);
//        }
//        double newmass = Sequence.EMPTY_PEPTIDE.getMass();
//        double pepmass=p.getMass();
//        ArrayList<AminoAcid> aas = new ArrayList<AminoAcid>();
//        int tries =100;
//        while (t.compareDoubleError(pepmass, newmass) >0) {
//            int aatries=10;
//            AminoAcid aa = aaa[(int) (Math.random()*(aaa.length-1))];
//            while (t.compareDoubleError(pepmass, newmass+aa.mass) <0 && (--aatries)>0) {
//                aa = aaa[(int) (Math.random()*(aaa.length-1))];
//            }
//            if (aatries==0) {
//                q
//            }
//        }
//        AminoAcid[] pepaa = new AminoAcid[p.length()];
//        int c = p.length();
//        for (int l = 0; l<c; l++) {
//            int res = (int) (Math.random()*(aaa.length-1));
//            pepaa[l] = aaa[res];
//        }
//        NonProteinPeptide npp =  new NonProteinPeptide(pepaa, p.getPositions(), p.getNTerminalModification(), p.getCTerminalModification(), p.isDecoy());
//        FastaHeader fh=p.getSequence().getSplitFastaHeader();
//        npp.getSequence().setFastaHeader(new FastaHeader(fh.getAccession() + " Randomized peptide", fh.getAccession(), fh.getName(),"Randomized peptide" ));
//        npp.setCTerminal(p.isCTerminal());
//        npp.setNTerminal(p.isNTerminal());
//        return npp;
//    }
    
    @Override
    public boolean isCTerminal() {
        return isCTerminal; 
    }

    @Override
    public boolean isNTerminal() {
        return isNTerminal;//To change body of generated methods, choose Tools | Templates.
    }

    
    public void setCTerminal(boolean isCT) {
        isCTerminal = isCT; 
    }

    
    public void setNTerminal(boolean isNT) {
        isNTerminal = isNT; 
    }
    
    

}

