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
package rappsilber.ms.statistics.generator;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.AminoModification;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.ions.CrosslinkedFragment;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedBaseFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.ms.statistics.utils.CountAAPair;
import rappsilber.utils.CountOccurence;

/**
 * counts the occurrence of fragmentation between
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class FragmentationSite extends AbstractStatistic {

    HashMap<String, Integer> GroupCounts;
    Spectra m_deisotoped;
    MatchedFragmentCollection m_MatchedFragments;
    int[] m_matchgroupCount = new int[]{0,0,0,0,0,0};
    int m_countSpectra = 0;
    int m_minSupportingPeaks = 3;
    boolean m_canHaveBase = false;
    boolean m_MustHaveBase = false;
    
    CountAAPair m_PossibleFragmentation = new CountAAPair();
    CountAAPair m_FoundFragmentation  = new CountAAPair();
    HashMap<AminoAcid,AminoAcid> m_CrosslinkedAminoAcid = new HashMap<AminoAcid, AminoAcid>();
    CountOccurence<AminoAcid> m_AminoCount = new CountOccurence<AminoAcid>();

    Class m_IncludeFragmentClass = Fragment.class;
    Class m_ExcludeFragmentClass = Loss.class;

    int m_maxLoss = 0;

    boolean m_countEvents = true;

   
    /**
     * initialises some variables
     */
    public void init() {

        // create for each aminoacid an C and N terminal version of it
        Collection<AminoAcid> aas = AminoAcid.getRegisteredAminoAcids();

        ArrayList<AminoAcid> cnterm = new ArrayList<AminoAcid>(aas.size());
        cnterm.addAll(aas);
        for (AminoAcid aa : cnterm) {
            if (AminoAcid.getAminoAcid(aa.SequenceID + "ct") == null
                    && !aa.SequenceID.contains("nt")
                    && !aa.SequenceID.contains("ct")) {
                new AminoModification(aa.SequenceID + "ct", aa, aa.mass).register();
                new AminoModification(aa.SequenceID + "nt", aa, aa.mass).register();
            }
        }
    }

    // <editor-fold desc="construtors">
    /**
     * constructor
     * @param IncludeFragmentClass only count fragments of this class
     * @param countSitesOnly only count whether there was a fragmentation event
     * at a the amino
     * @param maxLoss
     */
    public FragmentationSite(Class IncludeFragmentClass, boolean countSitesOnly, int maxLoss) {
        this(countSitesOnly);
        m_IncludeFragmentClass = IncludeFragmentClass;
    }

    public FragmentationSite(Class IncludeFragmentClass, Class ExcludeFragments, boolean countSitesOnly, boolean MustHaveBaseFragment, boolean CanHaveBaseFragment, int maxLossCount) {
        this(IncludeFragmentClass, ExcludeFragments, countSitesOnly);
        m_MustHaveBase = MustHaveBaseFragment;
        m_canHaveBase = CanHaveBaseFragment;
        m_maxLoss = maxLossCount;
    }

    public FragmentationSite(Class IncludeFragmentClass, Class ExcludeFragments, boolean countSitesOnly) {
        this(IncludeFragmentClass, countSitesOnly,0);
        m_IncludeFragmentClass = IncludeFragmentClass;
    }

    public FragmentationSite(boolean countSitesOnly) {
        init();
        m_countEvents = !countSitesOnly;
    }

    // </editor-fold>
    

    protected void preparePeptide(Peptide pep) {
        if (!pep.aminoAcidAt(0).SequenceID.contains("nt")) {
            pep.setAminoAcidAt(0, AminoAcid.getAminoAcid(pep.aminoAcidAt(0).SequenceID + "nt"));
        }
        if (!pep.aminoAcidAt(pep.length() - 1).SequenceID.contains("ct")) {
            pep.setAminoAcidAt(pep.length() - 1, AminoAcid.getAminoAcid(pep.aminoAcidAt(pep.length() - 1).SequenceID + "ct"));
        }
    }


    private AminoAcid getCrossLinkedAminoAcid (AminoAcid a) {
        if (m_CrosslinkedAminoAcid.containsKey(a))
            return m_CrosslinkedAminoAcid.get(a);
        else {
            AminoAcid ca = new AminoAcid(a.SequenceID + "xl", a.mass);
            m_CrosslinkedAminoAcid.put(a, ca);
            return ca;
        }

    }





    public void countSpectraMatch(MatchedXlinkedPeptide match) {
        m_MatchedFragments = new MatchedFragmentCollection(match.getSpectrum().getPrecurserCharge());
        HashMap<Peptide,boolean[]> fragmentationSideFound = new HashMap<Peptide, boolean[]>();

        // mark c and n terminal aminoacids
        Peptide pep1 = match.getPeptide(0);
        preparePeptide(pep1);

        Peptide pep2 = match.getPeptide(1);
        preparePeptide(pep2);

        m_countSpectra++;
        m_MatchedFragments.free();
        
        boolean[] p1FragmentationSite = new boolean[pep1.length() - 1];
        java.util.Arrays.fill(p1FragmentationSite, false);
        boolean[] p2FragmentationSite = new boolean[pep2.length() - 1];
        java.util.Arrays.fill(p2FragmentationSite, false);

        fragmentationSideFound.put(pep1, p1FragmentationSite);
        fragmentationSideFound.put(pep2, p2FragmentationSite);

        //m_deisotoped = s.deIsotop();
        // retrive all the matched fragments
        for (SpectraPeak sp : match.getSpectrum().getPeaks()) {
            for (SpectraPeakMatchedFragment mf : sp.getMatchedAnnotation()) {
                Fragment f = mf.getFragment();
                
                // if not filtered for losses  then only permited ions
                if (!m_IncludeFragmentClass.isAssignableFrom(Loss.class)
                        && !f.isClass(m_IncludeFragmentClass) )
                    continue;

                // don't count possibilities for excluded ions
                if ((f.isClass(m_ExcludeFragmentClass)))
                    continue;
                // don't belive in every loss
                if (f instanceof Loss && ((Loss) f).getTotalLossCount() > m_maxLoss)
                    continue;

                m_MatchedFragments.add(f, mf.getCharge(), sp);
            }
        }

        // go through the possible fragmentation sites
        Collection<Fragment> pep1Frags = Fragment.fragment(pep1,false);
        Collection<Fragment> pep2Frags = Fragment.fragment(pep2,false);

        pep1Frags.addAll(CrosslinkedFragment.createCrosslinkedFragments(pep1Frags, pep2Frags, match.getCrosslinker(), match.getLinkingSite(0), match.getLinkingSite(1)));

        //pep2Frags.addAll(CrosslinkedFragment.createCrosslinkedFragments(pep2Frags, pep2, match.getCrosslinker(), match.getLinkingSitePeptide2()));
        Collection<Fragment> pepFrags = new ArrayList<Fragment>(pep1Frags.size() + pep2Frags.size());

        int linkSide = match.getLinkingSite(0);

        for (Fragment f: pep1Frags) {
            if (f.getStart() <= linkSide && linkSide <= f.getEnd() && f.isClass(CrosslinkedFragment.class)){
                pepFrags.add(f);
            } else if ((f.getStart() > linkSide || linkSide > f.getEnd()) && !f.isClass(CrosslinkedFragment.class)){
                pepFrags.add(f);
            }
        }
        linkSide = match.getLinkingSite(1);
        for (Fragment f: pep2Frags) {
            if (f.getStart() <= linkSide && linkSide <= f.getEnd() && f.isClass(CrosslinkedFragment.class)){
                pepFrags.add(f);
            } else if ((f.getStart() > linkSide || linkSide > f.getEnd()) && !f.isClass(CrosslinkedFragment.class)){
                pepFrags.add(f);
            }
        }

        HashSet<Fragment> foundFragments = new HashSet<Fragment>(pepFrags.size());


        for (Fragment f: pepFrags) {
            AminoAcid aaC = null;
            AminoAcid aaN = null;
            Peptide p = f.getPeptide();


            if (f.length() < p.length()) {
                int site = 0;

                // don't considere losses
                if ((!f.isClass(m_IncludeFragmentClass)) && (!m_IncludeFragmentClass.isAssignableFrom(Loss.class)))
                    continue;
                if ((f.isClass(m_ExcludeFragmentClass)))
                    continue;

                if (f instanceof Loss) {
                    return;
                }

                if (foundFragments.contains(f))
                    continue;

                foundFragments.add(f);


                if (f.getStart() == 0 ) {
                    site = f.getEnd();
                    aaN = f.aminoAcidAt(f.getEnd());
                    aaC = p.aminoAcidAt(f.getEnd() + 1);
                } else if (f.getEnd() == p.length() - 1) {
                    site = f.getStart() - 1;
                    aaN = p.aminoAcidAt(f.getStart() - 1);
                    aaC = f.aminoAcidAt(0);
                }

                if (m_countEvents || ! fragmentationSideFound.get(p)[site]) { // count each site only once
                    int linkSite = (p == match.getPeptide(0) ?
                        match.getLinkingSite(0) :
                        match.getLinkingSite(1));

                    if (site == linkSite)
                        aaN = getCrossLinkedAminoAcid(aaN);
                    else if (site+1 == linkSite)
                        aaC = getCrossLinkedAminoAcid(aaC);

                    
                    m_AminoCount.add(aaN);

                    m_AminoCount.add(aaC);

                    m_PossibleFragmentation.add(aaN,aaC);

                    fragmentationSideFound.get(p)[site] = true;
                }
            }
        }


        java.util.Arrays.fill(p1FragmentationSite, false);
        java.util.Arrays.fill(p2FragmentationSite, false);

        foundFragments.clear();
        // go through the existing fragmentation sites
        for (MatchedBaseFragment mbf : m_MatchedFragments) {
            if (m_MustHaveBase && !mbf.isBaseFragmentFound())
                continue;

            if (!m_canHaveBase && mbf.isBaseFragmentFound())
                continue;

            Fragment f = mbf.getBaseFragment();

            // don't considere losses
//                if ((!f.isClass(m_IncludeFragmentClass)) || (f.isClass(m_ExcludeFragmentClass)))
//                    continue;


            if (foundFragments.contains(f)) // count
                continue;

            foundFragments.add(f);

            AminoAcid aaC = null;
            AminoAcid aaN = null;
            Peptide p = f.getPeptide();


            if (f.length() < p.length()) {
                int site = 0;
                if (f.getStart() == 0 ) {
                    site = f.getEnd();
                    aaN = f.aminoAcidAt(f.getEnd());
                    aaC = p.aminoAcidAt(f.getEnd() + 1);
                } else if (f.getEnd() == p.length() - 1) {
                    site = f.getStart() - 1;
                    aaN = p.aminoAcidAt(f.getStart() - 1);
                    aaC = f.aminoAcidAt(0);
                }

                try {
                    if (m_countEvents || ! fragmentationSideFound.get(p)[site]) { // count each site only once
                        int linkSite = (p == match.getPeptide(0)?
                            match.getLinkingSite(0) :
                            match.getLinkingSite(1));

                        if (site == linkSite)
                            aaN = getCrossLinkedAminoAcid(aaN);
                        else if (site+1 == linkSite)
                            aaC = getCrossLinkedAminoAcid(aaC);

                        m_AminoCount.add(aaN);
                        m_AminoCount.add(aaC);

                        //
                        m_FoundFragmentation.get(aaN).add(aaC);
                        fragmentationSideFound.get(p)[site] = true;
                    }
                } catch( Exception e) {
                    e.printStackTrace(System.err);
                    throw new Error (e);
                }
            }


        }

    }

    private String toTable(CountAAPair values) {
        StringBuilder sb = new StringBuilder();
        Collection<AminoAcid> aaCol = m_AminoCount.getCountedObjects();

        AminoAcid[]  aaa = new AminoAcid[aaCol.size()];

        aaa = aaCol.toArray(aaa);

        int[] sumY = new int[aaa.length];
        java.util.Arrays.fill(sumY,0);

        java.util.Arrays.sort(aaa);




        for (AminoAcid aa : aaa) {
            sb.append("\t");
            sb.append(aa.toString());
        }
        sb.append("\tsum\n");

        for (int i = 0 ; i < aaa.length; i++ ) {
            AminoAcid aan = aaa[i];
            sb.append(aan.toString());

            int sumx = 0;

            for (int c = 0;c < aaa.length; c++) {
                AminoAcid aac = aaa[c];
                Integer v = values.get(aan,aac);
                sb.append("\t");
                sb.append(v);
                sumx += v;
                sumY[c] += v;

            }
            sb.append("\t" + sumx + "\n");
        }
        sb.append("sum");
        int sumGlobal = 0;
        for (int c =0 ; c < aaa.length; c++) {
            sb.append("\t" + sumY[c]);
            sumGlobal += sumY[c];
        }
        sb.append("\t" + sumGlobal + "\n");

        return sb.toString();

    }

    private String toTable(CountAAPair found, CountAAPair possible) {
        StringBuilder sb = new StringBuilder();
        Collection<AminoAcid> aaCol = m_AminoCount.getCountedObjects();
        AminoAcid[]  aaa = new AminoAcid[aaCol.size()];
        int[]  cSumPossible = new int[aaCol.size()];
        int[]  cSumFound = new int[aaCol.size()];
        aaa = aaCol.toArray(aaa);

        java.util.Arrays.sort(aaa);

        for (int c = 0; c< aaa.length ; c++ ) {
            sb.append("\t");
            sb.append(aaa[c].toString());
        }
        sb.append("\n");

        for (int n =0; n<aaa.length; n++) {
            AminoAcid aan = aaa[n];
            sb.append(aan.toString());
            int nSumFound = 0;
            int nSumPossible = 0;
            for (int c = 0; c< aaa.length ; c++ ) {
                AminoAcid aac = aaa[c];
                Integer f = found.get(aan, aac);
                Integer p = possible.get(aan, aac);
                sb.append("\t");
                sb.append(f/(double)p);

                nSumFound    += f;
                cSumFound[c] += f;

                nSumPossible    += p;
                cSumPossible[c] += p;
            }
            sb.append("\t" + (nSumFound/(double)nSumPossible) + "\n");
        }
        for (int c = 0; c< aaa.length ; c++ ) {
            sb.append("\t");
            sb.append(cSumFound[c]/(double)cSumPossible[c]);
        }

        return sb.toString();

    }

    public String getAbsoluteTable() {
        return toTable(m_FoundFragmentation);
    }

    public String getPossibleTable() {
        return toTable(m_PossibleFragmentation);
    }

    public String getRelativeTable() {
        return toTable(m_FoundFragmentation,m_PossibleFragmentation);
    }


    public String getTable() {
        String ret = "Fragmentation in realtion to surounding aminoacid pairs\n";
        return ret +getAbsoluteTable() + "\n"  +
                getPossibleTable() + "\n" +
                getRelativeTable();
    }


    @Override
    public void writeFile(OutputStream output) {
        PrintStream out = new PrintStream(output);
        Collection<AminoAcid> aaCol = m_AminoCount.getCountedObjects();

        AminoAcid[]  aaa = new AminoAcid[aaCol.size()];

        aaa = aaCol.toArray(aaa);

        java.util.Arrays.sort(aaa);


        for (AminoAcid N : aaa) {
            for (AminoAcid C : aaa) {
                if (m_PossibleFragmentation.get(N, C) > 0)
                    out.println(N + "\t" + C + "\t"
                            + m_FoundFragmentation.get(N, C) + "\t" +
                            + m_PossibleFragmentation.get(N, C));
            }
        }
        out.flush();
    }

}
