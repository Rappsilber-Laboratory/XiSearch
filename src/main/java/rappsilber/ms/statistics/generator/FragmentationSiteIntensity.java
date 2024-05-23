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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.AminoModification;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.ions.CrosslinkedFragment;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.utils.FragmentFilter;
import rappsilber.ms.sequence.utils.GenericFragmentFilter;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedBaseFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.ms.statistics.utils.CountAAPair;
import rappsilber.ms.statistics.utils.GroupPeaksByTopPeaks;
import rappsilber.ms.statistics.utils.SpectraPeakGroups;
import rappsilber.utils.CountOccurence;

/**
 * counts the occurrence of fragmentation between
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class FragmentationSiteIntensity extends AbstractStatistic {

//    double[] percentagePeaks  = new double[] {0.1,0.5,0.9,1};

    HashMap<String, Integer> GroupCounts;
    Spectra m_deisotoped;
    int[] m_matchgroupCount = new int[]{0,0,0,0,0,0};
    private int m_countSpectra = 0;
    private int m_minSupportingPeaks = 3;
    private boolean m_canHaveBase = false;
    private boolean m_MustHaveBase = false;
    private double[] m_IntesityClasses = new double[]{0.25, 0.5, 0.75, 1};
    private SpectraPeakGroups m_PeakGrouping = new GroupPeaksByTopPeaks(m_IntesityClasses);

    int m_PossibleEventsPerSite = 2;
    
//    CountAAPair m_PossibleFragmentation = new CountAAPair();
//    CountAAPair m_FoundFragmentation  = new CountAAPair();
    HashMap<AminoAcid,AminoAcid> m_CrosslinkedAminoAcid = new HashMap<AminoAcid, AminoAcid>();
    CountOccurence<AminoAcid> m_AminoCount = new CountOccurence<AminoAcid>();


    CountAAPair[] m_FragmentationGroups = new CountAAPair[getIntesityClasses().length];
    CountAAPair   m_unmatched = new CountAAPair();
//    private Class m_IncludeFragmentClass = Fragment.class;
//    private Class m_ExcludeFragmentClass = Loss.class;

//    private int m_maxLoss = 0;
//    private int m_minLoss = 0;
    private boolean m_countEvents = true;

    /** Filter, that defines, what is counted as a possible fragmentation */
    private FragmentFilter m_PossibleFragmentsFilter = new GenericFragmentFilter();

    /** Filter, that defines, which fragmentation events to count */
    private FragmentFilter m_FoundFragmentsFilter = new GenericFragmentFilter().cloneNonLossy();

    protected boolean checkCrosslinked(Fragment f, int linkSide) {
        if (f.getStart() <= linkSide && linkSide <= f.getEnd() && f.isClass(CrosslinkedFragment.class)) {
            return true;
        } else if ((f.getStart() > linkSide || linkSide > f.getEnd()) && !f.isClass(CrosslinkedFragment.class)) {
            return true;
        }
        return false;
    }

    /**
     * @return the m_countSpectra
     */
    public int getSeenSpectra() {
        return m_countSpectra;
    }

    /**
     * @return the m_minSupportingPeaks
     */
    public int getMinSupportingPeaks() {
        return m_minSupportingPeaks;
    }

    /**
     * @param m_minSupportingPeaks the m_minSupportingPeaks to set
     */
    public void setMinSupportingPeaks(int m_minSupportingPeaks) {
        this.m_minSupportingPeaks = m_minSupportingPeaks;
    }

    /**
     * @return the m_canHaveBase
     */
    public boolean canHaveBase() {
        return m_canHaveBase;
    }

    /**
     * @param m_canHaveBase the m_canHaveBase to set
     */
    public void canHaveBase(boolean m_canHaveBase) {
        this.m_canHaveBase = m_canHaveBase;
    }

    /**
     * @return the m_MustHaveBase
     */
    public boolean mustHaveBase() {
        return m_MustHaveBase;
    }

    /**
     * @param m_MustHaveBase the m_MustHaveBase to set
     */
    public void mustHaveBase(boolean m_MustHaveBase) {
        this.m_MustHaveBase = m_MustHaveBase;
    }

    /**
     * @return the m_IntesityClasses
     */
    public double[] getIntesityClasses() {
        return m_IntesityClasses;
    }

    /**
     * @param m_IntesityClasses the m_IntesityClasses to set
     */
    public void setIntesityClasses(double[] m_IntesityClasses) {
        this.m_IntesityClasses = m_IntesityClasses;
    }

    /**
     * @return the m_PeakGrouping
     */
    public SpectraPeakGroups getPeakGrouping() {
        return m_PeakGrouping;
    }

    /**
     * @param m_PeakGrouping the m_PeakGrouping to set
     */
    public void setPeakGrouping(SpectraPeakGroups m_PeakGrouping) {
        this.m_PeakGrouping = m_PeakGrouping;
    }

//    /**
//     * @return the m_maxLoss
//     */
//    public int getMaxLoss() {
//        return m_maxLoss;
//    }

//    /**
//     * @param m_maxLoss the m_maxLoss to set
//     */
//    public void setMaxLoss(int m_maxLoss) {
//        this.m_maxLoss = m_maxLoss;
//    }

//    /**
//     * @return the m_minLoss
//     */
//    public int getMinLoss() {
//        return m_minLoss;
//    }

//    /**
//     * @param m_minLoss the m_minLoss to set
//     */
//    public void setMinLoss(int m_minLoss) {
//        this.m_minLoss = m_minLoss;
//    }

    /**
     * @return the m_countEvents
     */
    public boolean CountEvents() {
        return m_countEvents;
    }

    /**
     * @param m_countEvents the m_countEvents to set
     */
    public void CountEvents(boolean m_countEvents) {
        this.m_countEvents = m_countEvents;
    }

//    /**
//     * @return the m_IncludeFragmentClass
//     */
//    public Class getIncludeFragmentClass() {
//        return m_IncludeFragmentClass;
//    }
//
//    /**
//     * @param m_IncludeFragmentClass the m_IncludeFragmentClass to set
//     */
//    public void setIncludeFragmentClass(Class m_IncludeFragmentClass) {
//        this.m_IncludeFragmentClass = m_IncludeFragmentClass;
//    }
//
//    /**
//     * @return the m_ExcludeFragmentClass
//     */
//    public Class getExcludeFragmentClass() {
//        return m_ExcludeFragmentClass;
//    }
//
//    /**
//     * @param m_ExcludeFragmentClass the m_ExcludeFragmentClass to set
//     */
//    public void setExcludeFragmentClass(Class m_ExcludeFragmentClass) {
//        this.m_ExcludeFragmentClass = m_ExcludeFragmentClass;
//    }



    

    public void init() {

        // create for each aminoacid an C and N terminal version of it

        Collection<AminoAcid> aas = AminoAcid.getRegisteredAminoAcids();
        for (int i = 0; i<m_FragmentationGroups.length;i++) {
            m_FragmentationGroups[i] = new CountAAPair();
        }

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

    public FragmentationSiteIntensity(FragmentFilter PossibleFragments, FragmentFilter FoundFragments, boolean countSitesOnly) {
        this(countSitesOnly);
        m_PossibleFragmentsFilter = PossibleFragments;
        m_FoundFragmentsFilter = FoundFragments;
    }

    public FragmentationSiteIntensity(FragmentFilter PossibleFragments, FragmentFilter FoundFragments, boolean countSitesOnly, boolean MustHaveBaseFragment, boolean CanHaveBaseFragment) {
        this(PossibleFragments, FoundFragments, countSitesOnly);
        m_MustHaveBase = MustHaveBaseFragment;
        m_canHaveBase = CanHaveBaseFragment;
    }

    public FragmentationSiteIntensity(boolean countSitesOnly) {
        init();
        m_countEvents = !countSitesOnly;
    }

    // </editor-fold>
    

    protected Peptide preparePeptide(Peptide pep) {
        Peptide ret = new Peptide(pep);
        if (!ret.aminoAcidAt(0).SequenceID.contains("nt")) {
            ret.setAminoAcidAt(0, AminoAcid.getAminoAcid(ret.aminoAcidAt(0).SequenceID + "nt"));
        }
        if (!ret.aminoAcidAt(ret.length() - 1).SequenceID.contains("ct")) {
            ret.setAminoAcidAt(ret.length() - 1, AminoAcid.getAminoAcid(ret.aminoAcidAt(pep.length() - 1).SequenceID + "ct"));
        }
        return ret;
    }


    private AminoAcid getCrossLinkedAminoAcid (AminoAcid a) {
        if (m_CrosslinkedAminoAcid.containsKey(a)) {
            return m_CrosslinkedAminoAcid.get(a);
        } else {
            AminoAcid ca = new AminoAcid(a.SequenceID + "xl", a.mass);
            m_CrosslinkedAminoAcid.put(a, ca);
            return ca;
        }

    }

    
//    private SpectraPeak[][] groupPeaksByClass(Spectra s){
//        SpectraPeak[][] ret = new SpectraPeak[getIntesityClasses().length + 1][];
//        SortedLinkedList<SpectraPeak> list = s.getPeaksByIntensity();
//        int beginIndex = 0;
//
//        for (int i = 0; i < getIntesityClasses().length; i++) {
//            double classBorder = getIntesityClasses()[i];
//            int endIndex = (int)(classBorder * s.getPeaks().size() -1 );
//            ret[i] = list.subList(beginIndex, endIndex).toArray(ret[i]);
//            beginIndex = endIndex + 1;
//        }
//
//        return ret;
//
//    }

    private void increaseFragmentationSite(Fragment f, HashMap<Peptide,boolean[]> fragmentationSideFound, MatchedXlinkedPeptide match, CountAAPair count) {
        int site = -1;
        AminoAcid aaN = null;
        AminoAcid aaC = null;

//        if (f.length() == p.length())
//            return; // this is a precoursor-fragmentPrimary - at the moment ignored
//
//        if (f.getStart() == 0 ) {
//            site = f.getEnd();
////            aaN = p.aminoAcidAt(f.getEnd());
////            aaC = p.aminoAcidAt(f.getEnd() + 1);
//        } else if (f.getEnd() == p.length() - 1) {
//            site = f.getStart() - 1;
////            aaN = p.aminoAcidAt(f.getStart() - 1);
////            aaC = p.aminoAcidAt(0);
//        }
//
//        aaN = p.aminoAcidAt(site);
//        aaC = p.aminoAcidAt(site + 1);

        for (rappsilber.ms.sequence.ions.FragmentationSite fs : f.getFragmentationSites()) {

            Peptide p = fs.peptide;

            if (CountEvents() || ! fragmentationSideFound.get(p)[fs.site]) {

                int linkSite = (p == match.getPeptide(0) ?
                    match.getLinkingSite(0) :
                    match.getLinkingSite(1));

                if (fs.site == linkSite) {
                    aaN = getCrossLinkedAminoAcid(fs.NTerm);
                    aaC = fs.CTerm;
                } else if (fs.site+1 == linkSite) {
                    aaN = fs.NTerm;
                    aaC = getCrossLinkedAminoAcid(fs.CTerm);
                } else {
                    aaN = fs.NTerm;
                    aaC = fs.CTerm;
                }


                m_AminoCount.add(aaN);

                m_AminoCount.add(aaC);

                count.add(aaN, aaC);
                //m_PossibleFragmentation.add(aaN,aaC);

                fragmentationSideFound.get(p)[fs.site] = true;
            }
        }

    }


    public void countSpectraMatch(MatchedXlinkedPeptide match) {

        HashMap<Peptide,boolean[]> fragmentationSideFound = new HashMap<Peptide, boolean[]>();
        HashMap<Double, ArrayList<SpectraPeak>> peakGroups = getPeakGrouping().getPeakGroubs(match.getSpectrum());
        MatchedFragmentCollection[] groupedFragmentGroups = new MatchedFragmentCollection[getIntesityClasses().length];
        MatchedFragmentCollection   allFragments = new MatchedFragmentCollection(match.getSpectrum().getPrecurserCharge());

        for (int i = 0; i < groupedFragmentGroups.length; i++) {
            groupedFragmentGroups[i] = new MatchedFragmentCollection(match.getSpectrum().getPrecurserCharge());
        }





        // mark c and n terminal aminoacids
        Peptide pep1 = preparePeptide( match.getPeptide(0));
        {
            boolean[] fragSite = new boolean[pep1.length()];
            java.util.Arrays.fill(fragSite, false);
            fragmentationSideFound.put(pep1, fragSite);
        }

        Peptide pep2 = preparePeptide(match.getPeptide(1));
        {
            boolean[] fragSite = new boolean[pep2.length()];
            java.util.Arrays.fill(fragSite, false);
            fragmentationSideFound.put(pep2, fragSite);
        }



        // go through the possible fragmentation sites
        Collection<Fragment> pep1Frags = Fragment.fragment(pep1,false);
        Collection<Fragment> pep2Frags = Fragment.fragment(pep2,false);

//        pep1Frags.addAll(CrosslinkedFragment.createCrosslinkedFragments(pep1Frags, pep1, match.getCrosslinker(), match.getLinkingSitePeptide1()));
        pep1Frags.addAll(CrosslinkedFragment.createCrosslinkedFragments(pep1Frags, pep2Frags, match.getCrosslinker(), match.getLinkingSite(0), match.getLinkingSite(1)));

//        pep2Frags.addAll(CrosslinkedFragment.createCrosslinkedFragments(pep2Frags, pep2, match.getCrosslinker(), match.getLinkingSitePeptide2()));
        Collection<Fragment> pepFrags = new ArrayList<Fragment>(pep1Frags.size() + pep2Frags.size());

        int linkSide = match.getLinkingSite(0);
        for (Fragment f: pep1Frags) {
            if (checkCrosslinked(f, linkSide)) {
                pepFrags.add(f);
            }
        }
        linkSide = match.getLinkingSite(1);
        for (Fragment f: pep2Frags) {
            if (checkCrosslinked(f, linkSide)) {
                pepFrags.add(f);
            }
        }


        Double[] groups = peakGroups.keySet().toArray(new Double[0]);
        java.util.Arrays.sort(groups);

        // for each group get the matched peaks
        for (int i = 0; i < groupedFragmentGroups.length; i++) {
            MatchedFragmentCollection mfc = groupedFragmentGroups[i];
            CountAAPair countAAPair = m_FragmentationGroups[i];
            // build up a list of matched fragments
            for (SpectraPeak sp: peakGroups.get(groups[i])) {
//                if ((int)sp.getMZ() == 464)
//                    System.out.println("found");
                for (SpectraPeakMatchedFragment mf : sp.getMatchedAnnotation()) {
                    //  - within the group
                    mfc.add(mf.getFragment(),mf.getCharge());
                    //  - overall
                    allFragments.add(mf.getFragment(),mf.getCharge());
                }
            }
             
            // for each matched fragmentPrimary group
            for (MatchedBaseFragment mbf : mfc) {
                
                if (mbf.isBaseFragmentFound()) {
                    if (!canHaveBase()) {
                        continue;
                    }
                } else
                    if (mustHaveBase()) {
                        continue;
                }

                //  for each matched fragmentPrimary-ion
                for (Fragment f: mbf.getFragments()) {

                    if (getFoundFragmentsFilter().isValid(f)) { // does it need to be a lossy peak

                        //no
                        try {
                            increaseFragmentationSite(f, fragmentationSideFound, match, countAAPair);
                        } catch (Exception e) {
                            throw new Error(e);
                        }

                    }
                }
            }

        }

        // count the nonematched fragments
        for (Fragment f: pepFrags){
            if (!allFragments.hasMatchedFragmentGroup(f)) {
                if (getPossibleFragmentsFilter().isValid(f)) { // does it need to be a lossy peak
                    increaseFragmentationSite(f, fragmentationSideFound, match, m_unmatched);
                }
            }
        }

        m_countSpectra++;

        

    }

//    private String toTable(CountAAPair values) {
//        StringBuilder sb = new StringBuilder();
//        Collection<AminoAcid> aaCol = m_AminoCount.getCountedObjects();
//
//        AminoAcid[]  aaa = new AminoAcid[aaCol.size()];
//
//        aaa = aaCol.toArray(aaa);
//
//        int[] sumY = new int[aaa.length];
//        java.util.Arrays.fill(sumY,0);
//
//        java.util.Arrays.sort(aaa);
//
//
//
//
//        for (AminoAcid aa : aaa) {
//            sb.append("\t");
//            sb.append(aa.toString());
//        }
//        sb.append("\tsum\n");
//
//        for (int i = 0 ; i < aaa.length; i++ ) {
//            AminoAcid aan = aaa[i];
//            sb.append(aan.toString());
//
//            int sumx = 0;
//
//            for (int c = 0;c < aaa.length; c++) {
//                AminoAcid aac = aaa[c];
//                Integer v = values.get(aan,aac);
//                sb.append("\t");
//                sb.append(v);
//                sumx += v;
//                sumY[c] += v;
//
//            }
//            sb.append("\t" + sumx + "\n");
//        }
//        sb.append("sum");
//        int sumGlobal = 0;
//        for (int c =0 ; c < aaa.length; c++) {
//            sb.append("\t" + sumY[c]);
//            sumGlobal += sumY[c];
//        }
//        sb.append("\t" + sumGlobal + "\n");
//
//        return sb.toString();
//
//    }
//
//    private String toTable(CountAAPair found, CountAAPair possible) {
//        StringBuilder sb = new StringBuilder();
//        Collection<AminoAcid> aaCol = m_AminoCount.getCountedObjects();
//        AminoAcid[]  aaa = new AminoAcid[aaCol.size()];
//        int[]  cSumPossible = new int[aaCol.size()];
//        int[]  cSumFound = new int[aaCol.size()];
//        aaa = aaCol.toArray(aaa);
//
//        java.util.Arrays.sort(aaa);
//
//        for (int c = 0; c< aaa.length ; c++ ) {
//            sb.append("\t");
//            sb.append(aaa[c].toString());
//        }
//        sb.append("\n");
//
//        for (int n =0; n<aaa.length; n++) {
//            AminoAcid aan = aaa[n];
//            sb.append(aan.toString());
//            int nSumFound = 0;
//            int nSumPossible = 0;
//            for (int c = 0; c< aaa.length ; c++ ) {
//                AminoAcid aac = aaa[c];
//                Integer f = found.get(aan, aac);
//                Integer p = possible.get(aan, aac);
//                sb.append("\t");
//                sb.append(f/(double)p);
//
//                nSumFound    += f;
//                cSumFound[c] += f;
//
//                nSumPossible    += p;
//                cSumPossible[c] += p;
//            }
//            sb.append("\t" + (nSumFound/(double)nSumPossible) + "\n");
//        }
//        for (int c = 0; c< aaa.length ; c++ ) {
//            sb.append("\t");
//            sb.append(cSumFound[c]/(double)cSumPossible[c]);
//        }
//
//        return sb.toString();
//
//    }
//
//    public String getAbsoluteTable() {
//        return toTable(m_FoundFragmentation);
//    }
//
//    public String getPossibleTable() {
//        return toTable(m_PossibleFragmentation);
//    }
//
//    public String getRelativeTable() {
//        return toTable(m_FoundFragmentation,m_PossibleFragmentation);
//    }


    public String getTable() {

        StringBuffer ret = new StringBuffer("#intesity based fragmentation events of aminoacid pairs\n");
        for (AminoAcid aaN : m_AminoCount.getCountedObjects()) {
            for (AminoAcid aaC : m_AminoCount.getCountedObjects()) {
                int s = 0;
                StringBuffer line = new StringBuffer(aaN.SequenceID + "\t" +aaC.SequenceID);
                for (int g = 0; g < m_FragmentationGroups.length; g++) {
                    int c = m_FragmentationGroups[g].get(aaN, aaC);
                    s+=c;
                    line.append("\t" + c);
                }
                int c = m_unmatched.get(aaN, aaC);
                line.append("\t" + c);
                s+=c;
                if (s>0) {
                    ret.append(line);
                    ret.append("\n");
                }
            }
        }
        return ret.toString();
    }

    /**
     * @return the PossibleFragmentsFilter
     */
    public FragmentFilter getPossibleFragmentsFilter() {
        return m_PossibleFragmentsFilter;
    }

    /**
     * @param PossibleFragmentsFilter the PossibleFragmentsFilter to set
     */
    public void setPossibleFragmentsFilter(FragmentFilter PossibleFragmentsFilter) {
        this.m_PossibleFragmentsFilter = PossibleFragmentsFilter;
    }

    /**
     * @return the FoundFragmentsFilter
     */
    public FragmentFilter getFoundFragmentsFilter() {
        return m_FoundFragmentsFilter;
    }

    /**
     * @param FoundFragmentsFilter the FoundFragmentsFilter to set
     */
    public void setFoundFragmentsFilter(FragmentFilter FoundFragmentsFilter) {
        this.m_FoundFragmentsFilter = FoundFragmentsFilter;
    }


//    @Override
//    public void writeFile(OutputStream output) {
//        PrintStream out = new PrintStream(output);
//        Collection<AminoAcid> aaCol = m_AminoCount.getCountedObjects();
//
//        AminoAcid[]  aaa = new AminoAcid[aaCol.size()];
//
//        aaa = aaCol.toArray(aaa);
//
//        java.util.Arrays.sort(aaa);
//
//
//        for (AminoAcid N : aaa) {
//            for (AminoAcid C : aaa) {
//                if (m_PossibleFragmentation.get(N, C) > 0)
//                    out.println(N + "\t" + C + "\t"
//                            + m_FoundFragmentation.get(N, C) + "\t" +
//                            + m_PossibleFragmentation.get(N, C));
//            }
//        }
//        out.flush();
//    }

}
