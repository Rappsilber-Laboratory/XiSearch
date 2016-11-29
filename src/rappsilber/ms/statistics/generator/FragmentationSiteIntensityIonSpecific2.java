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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.ions.BIon;
import rappsilber.ms.sequence.ions.CrosslinkedFragment;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.FragmentationSite;
import rappsilber.ms.sequence.ions.YIon;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedBaseFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.ms.statistics.utils.UpdateableInteger;

/**
 * counts the occurrence of fragmentation between
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class FragmentationSiteIntensityIonSpecific2 extends AbstractStatistic {


    //private HashMap<String,HashMap<String,int[]>> m_FragmentationSiteOccurence = new HashMap<String, HashMap<String, int[]>>();
    private HashMap<Class,HashMap<String,HashMap<String,int[]>>> m_FragmentationSiteOccurence = new HashMap<Class,HashMap<String, HashMap<String, int[]>>>();
    private HashMap<Integer,HashMap<Class,HashMap<String,HashMap<String,int[]>>>> m_PeptideFragmentationSiteOccurence = new HashMap<Integer,HashMap<Class,HashMap<String, HashMap<String, int[]>>>>();
    private int m_countSpectra = 0;
    private int m_groups = 10;
//    private int m_bcount = 0;
//    private int m_ycount = 0;
    private int m_bMatchedCount = 0;
    private int m_yMatchedCount = 0;
    private int m_bMissedCount = 0;
    private int m_yMissedCount = 0;
//    private int m_otherMatchedCount = 0;
//    private int m_otherMissedCount = 0;
//    private int m_otherCount = 0;

    private class FragmentBooleanMap extends TreeMap<Fragment, Boolean>{
        public FragmentBooleanMap() {
            super(new Comparator<Fragment>() {
                    public int compare(Fragment o1, Fragment o2) {
                        Peptide p1 = o1.getPeptide();
                        Peptide p2 = o2.getPeptide();
                        if (p1.length() > p2.length())
                            return 1;
                        if (p1.length() < p2.length())
                            return -1;
                        int ret = p1.toString().compareTo(p2.toString());
                        if (ret != 0)
                            return ret;
                        ret = (o1.name() + o1.toString()).compareTo(o2.name() + o2.toString());
                        if (ret != 0)
                            return ret;
                        return 0;
                    }
                }
            );
        }
    }


    private String[] convertFragmentationSite(MatchedXlinkedPeptide match, Fragment f) {
        FragmentationSite fs = f.getFragmentationSites()[0];
        String[] siteNames = new String[]{fs.NTerm.SequenceID,fs.CTerm.SequenceID};


        if (fs.site==0) {
            siteNames[0] += "nt";
        }
        if (fs.site == fs.peptide.length() - 2) {
            siteNames[1] += "ct";
        }

        if (fs.site == match.getLinkingSite(fs.peptide)) {
            siteNames[0] += "xl";
        }

        if (fs.site + 1 == match.getLinkingSite(fs.peptide)) {
            siteNames[1] += "xl";
        }

        return siteNames;

    }

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


    // <editor-fold desc="construtors">

    public FragmentationSiteIntensityIonSpecific2() {
    }

    // </editor-fold>
    


    public boolean validFragment(Fragment f) {
        // no loss and no double fragmentation
        return (! (f instanceof Loss)) && f.getFragmentationSites().length == 1;

    }

    public boolean canCountFragment(Fragment f, int charge, MatchedFragmentCollection mfc) {
        if (f.getFragmentationSites().length != 1) {
            return false;
        }
        if (f instanceof Loss) {
            MatchedBaseFragment mbf = mfc.getMatchedFragmentGroup(f,charge);
            return mbf.isBaseFragmentFound();
        } else
            return true;
    }

    private void incrementSite(MatchedXlinkedPeptide match, Fragment f, int group) {
        String[] siteNames = convertFragmentationSite(match, f);
        String n = siteNames[0];
        String c = siteNames[1];
        if (f.isClass(Loss.class)) {
            f = ((Loss)f).getBaseFragment();
        }
        if (f.isClass(CrosslinkedFragment.class)) {
            Fragment f1 = ((CrosslinkedFragment)f).getBaseFragment();
            if (f1.length() == f1.getPeptide().length())
                f1 = ((CrosslinkedFragment)f).getCrossLinkedFragment();
            f = f1;
        }

        HashMap<String, HashMap<String, int[]>> sites = m_FragmentationSiteOccurence.get(f.getClass());
        if (sites == null) {
            sites = new HashMap<String, HashMap<String, int[]>>();
            m_FragmentationSiteOccurence.put(f.getClass(), sites);
        }

        HashMap<String,int[]> nGroup = sites.get(n);

        if (nGroup == null) {
            nGroup = new HashMap<String, int[]>();
            int[] cGroup = new int[m_groups + 1];
            java.util.Arrays.fill(cGroup, 0);
            cGroup[group] = 1;
            nGroup.put(c, cGroup);
            sites.put(n,nGroup);
        } else {
            int[] cGroup = nGroup.get(c);
            if (cGroup == null) {
                cGroup = new int[m_groups + 1];
                java.util.Arrays.fill(cGroup, 0);
                cGroup[group] = 1;
                nGroup.put(c, cGroup);
            } else
                cGroup[group]++;

        }
    }


    private void incrementPeptideSite(MatchedXlinkedPeptide match, Fragment f, int group) {
        String[] siteNames = convertFragmentationSite(match, f);
        String n = siteNames[0];
        String c = siteNames[1];
        if (f.isClass(Loss.class)) {
            f = ((Loss)f).getBaseFragment();
        }
        if (f.isClass(CrosslinkedFragment.class)) {
            f = ((CrosslinkedFragment)f).getBaseFragment();
//            if (f1.length() == f1.getPeptide().length())
//                f1 = ((CrosslinkedFragment)f).getCrossLinkedFragment();
//            f = f1;
        }

        int pid = match.getPeptideID(f);
        
        HashMap<Class,HashMap<String, HashMap<String, int[]>>> classSites = m_PeptideFragmentationSiteOccurence.get(pid);
        if (classSites == null) {
            classSites = new HashMap<Class, HashMap<String, HashMap<String, int[]>>>();
            m_PeptideFragmentationSiteOccurence.put(new Integer(pid), classSites);
        }

        HashMap<String, HashMap<String, int[]>> sites = classSites.get(f.getClass());
        if (sites == null) {
            sites = new HashMap<String, HashMap<String, int[]>>();
            classSites.put(f.getClass(), sites);
        }

        HashMap<String,int[]> nGroup = sites.get(n);

        if (nGroup == null) {
            nGroup = new HashMap<String, int[]>();
            int[] cGroup = new int[m_groups + 1];
            java.util.Arrays.fill(cGroup, 0);
            cGroup[group] = 1;
            nGroup.put(c, cGroup);
            sites.put(n,nGroup);
        } else {
            int[] cGroup = nGroup.get(c);
            if (cGroup == null) {
                cGroup = new int[m_groups + 1];
                java.util.Arrays.fill(cGroup, 0);
                cGroup[group] = 1;
                nGroup.put(c, cGroup);
            } else
                cGroup[group]++;

        }
    }



    public void countSpectraMatch(MatchedXlinkedPeptide match) {

        MatchedFragmentCollection   mfc = match.getMatchedFragments();

        Spectra deisotoped = match.getSpectrum().deIsotop();


        //HashMap<String,HashMap<String,Boolean>> foundSites = new HashMap<String, HashMap<String, Boolean>>();

        //HashMap<Fragment,Boolean> foundFragments = new HashMap<Fragment, Boolean>();
        FragmentBooleanMap foundFragments = new FragmentBooleanMap();
        HashMap<Peptide,FragmentBooleanMap> foundPeptideFragments = new HashMap<Peptide, FragmentBooleanMap>();

        HashMap<Peptide,UpdateableInteger> matchcount = new HashMap<Peptide, UpdateableInteger>(2);
        for (Peptide p : match.getPeptides()) {
            matchcount.put(p, new UpdateableInteger(0));
        }

        for (MatchedBaseFragment mbf : mfc) {
            if (mbf.isBaseFragmentFound()) {
                matchcount.get(mbf.getBaseFragment().getPeptide()).value++;
            }
        }
        
        Peptide alpha;
        Peptide beta;

        int count1 = matchcount.get(match.getPeptide(0)).value;
        int count2 = matchcount.get(match.getPeptide(1)).value;

        if (count1 > count2) {
            alpha = match.getPeptide(0);
            beta = match.getPeptide(1);
        }


        // read out all possible fragmentation sites
        for (Fragment f: match.getPossibleFragments()) {
            if (validFragment(f)) {
                foundFragments.put(f, Boolean.FALSE);
//              System.err.println(f.name() + " -> " + f.toString());
            }
        }



        // now look if and where we find them
        // among all peaks sorted by intensity
        Collection<SpectraPeak> peaks = deisotoped.getTopPeaks(-1);

        int group = 0;
        int peakCount = peaks.size();
        double groupSize = peakCount/(double)m_groups;
        int currentPeak = -1;

        for (SpectraPeak sp : peaks) {
            currentPeak++;
            group = (int) (currentPeak / groupSize);

            for (SpectraPeakMatchedFragment mf : sp.getMatchedAnnotation()) {
                Fragment f = mf.getFragment();
                MatchedBaseFragment mbf = mfc.getMatchedFragmentGroup(f, mf.getCharge());
                Fragment nonLossy = mbf.getBaseFragment();
                
                // only if the fragment matches the conditions and was not found yet ---------------------
                Boolean found = foundFragments.get(nonLossy);
                if (canCountFragment(f,mf.getCharge(),mfc) && (found != null) && ! found) {

                    // only if we didn't count that site combination already
                    foundFragments.put(nonLossy, Boolean.TRUE);
                    if (nonLossy.isClass(BIon.class))
                        m_bMatchedCount++;
                    else if (nonLossy.isClass(YIon.class))
                        m_yMatchedCount++;
                    incrementPeptideSite(match, f, group);
                    incrementSite(match, f, group);
 
                }
            }
        }

        // now look at all non matched fragmentation sites
        for (Fragment f : foundFragments.keySet()) {
            if (! foundFragments.get(f)) {
//                missed ++;
                if (f.isClass(BIon.class))
                    m_bMissedCount++;
                else if (f.isClass(YIon.class))
                    m_yMissedCount++;
//                else
//                    m_otherMissedCount++;

                incrementPeptideSite(match, f,m_groups);
                incrementSite(match, f,m_groups);
//            } else {
//                found++;
//                //System.err.print('.');
            }
       }
 //      System.err.println("counted : " + counted + ",  found : " + found + ",  missed : " + missed + " possible fragments: " + foundFragments.size());
//       if (counted != found || found + missed != foundFragments.size())
//           System.err.println("here");

//        if (m_bMatchedCount + m_bMissedCount != m_yMatchedCount + m_yMissedCount)
//           System.err.println("here");


        m_countSpectra++;

        HashMap<Integer,HashMap<Class,UpdateableInteger>> checkMissing = new HashMap<Integer, HashMap<Class, UpdateableInteger>>();
        HashMap<Class, UpdateableInteger> classMap = new HashMap<Class, UpdateableInteger>();
        classMap.put(BIon.class, new UpdateableInteger(0));
        classMap.put(YIon.class, new UpdateableInteger(0));
        checkMissing.put(0, classMap);
        classMap = new HashMap<Class, UpdateableInteger>();
        classMap.put(BIon.class, new UpdateableInteger(0));
        classMap.put(YIon.class, new UpdateableInteger(0));
        checkMissing.put(1, classMap);

        for (Integer p : m_PeptideFragmentationSiteOccurence.keySet()) {
            HashMap<Class,HashMap<String,HashMap<String,int[]>>> cl = m_PeptideFragmentationSiteOccurence.get(p);

            for (Class c : cl.keySet()) {

                HashMap<String, HashMap<String, int[]>> AA1 = cl.get(c);

                for (String N : AA1.keySet()) {

                    HashMap<String, int[]> AA2 = AA1.get(N);
                    for (String C : AA2.keySet()) {
                        int groups[] = AA2.get(C);
                        for (int g=0; g<groups.length ; g++) {
                            checkMissing.get(p).get(c).value+=groups[g];
                        }
                    }
                }

            }

        }

        for (Integer p : checkMissing.keySet()) {
            HashMap<Class, UpdateableInteger> cl = checkMissing.get(p);
            UpdateableInteger[] counts = cl.values().toArray(new UpdateableInteger[0]);
            for (int i = 1; i < counts.length; i++) {
                if (counts[0].value != counts[i].value) {
                    System.err.println("found it " + this.getClass().getName());
                }
            }
        }

//        for (Class c : m_FragmentationSiteOccurence.keySet()) {
//            HashMap<String, HashMap<String, int[]>> AA1 = m_FragmentationSiteOccurence.get(c);
//            for (String N : AA1.keySet()) {
//                HashMap<String, int[]> AA2 = AA1.get(N);
//                for (String C : AA2.keySet()) {
//                    int[] peps = AA2.get(C);
//
//                    for (int p = 0; p < peps.length; p++) {
//                        checkMissing.get(p).get(c).value++;
//                    }
//
//                }
//            }
//        }

        

    }

    @Override
    public void writeFile(OutputStream output) {
        PrintStream out = new PrintStream(output);
        out.println("#detailed table");
        out.println(getTable());
        out.println("#ion table");
        out.println(getIonTable());
        out.println("#summed table");
        out.println(getSumTable());
        out.flush();
    }


    public String getTable() {
        //StringBuffer ret = new StringBuffer("#intesity based fragmentation events of aminoacid pairs\n");
        StringBuffer ret = new StringBuffer();

        ret.append("PID,Class,N,C");
        for (int i=1;i<=m_groups;i++) {
            ret.append(","+ (100.0 * i/(double)m_groups));
        }
        ret.append(", notObserved\n");

        for (Integer pid : m_PeptideFragmentationSiteOccurence.keySet()) {
            HashMap<Class,HashMap<String, HashMap<String, int[]>>> pepSites = m_PeptideFragmentationSiteOccurence.get(pid);

            for (Class fClass : pepSites.keySet()) {
                HashMap<String, HashMap<String, int[]>> sites = pepSites.get(fClass);
                String className = fClass.getSimpleName();

                String[] nList =  sites.keySet().toArray(new String[0]);
                java.util.Arrays.sort(nList);

                for (String n : nList) {
                    HashMap<String,int[]> cGroups = sites.get(n);
                    String[] cList = cGroups.keySet().toArray(new String[0]);
                    java.util.Arrays.sort(cList);
                    for (String c : cList) {
                        int[] groups = cGroups.get(c);
                        ret.append(pid + "," + className + "," + n + "," + c);
                        for (int count : groups) {
                            ret.append("," + count);
                        }
                        ret.append("\n");
                    }
                }
            }
        }
        return ret.toString();
    }

    public String getIonTable() {
        //StringBuffer ret = new StringBuffer("#intesity based fragmentation events of aminoacid pairs\n");
        StringBuffer ret = new StringBuffer();

        ret.append("Class,N,C");
        for (int i=1;i<=m_groups;i++) {
            ret.append(","+ (100.0 * i/(double)m_groups));
        }
        ret.append(", notObserved\n");

//        for (Integer pid : m_PeptideFragmentationSiteOccurence.keySet()) {
//            HashMap<Class,HashMap<String, HashMap<String, int[]>>> pepSites = m_PeptideFragmentationSiteOccurence.get(pid);

            for (Class fClass : m_FragmentationSiteOccurence.keySet()) {
                HashMap<String, HashMap<String, int[]>> sites = m_FragmentationSiteOccurence.get(fClass);
                String className = fClass.getSimpleName();

                String[] nList =  sites.keySet().toArray(new String[0]);
                java.util.Arrays.sort(nList);

                for (String n : nList) {
                    HashMap<String,int[]> cGroups = sites.get(n);
                    String[] cList = cGroups.keySet().toArray(new String[0]);
                    java.util.Arrays.sort(cList);
                    for (String c : cList) {
                        int[] groups = cGroups.get(c);
                        ret.append(className + "," + n + "," + c);
                        for (int count : groups) {
                            ret.append("," + count);
                        }
                        ret.append("\n");
                    }
                }
            }
//        }
        return ret.toString();
    }

    public String getSumTable() {
        //StringBuffer ret = new StringBuffer("#intesity based fragmentation events of aminoacid pairs\n");
        StringBuffer ret = new StringBuffer();

        ret.append("N,C");
        for (int i=1;i<=m_groups;i++) {
            ret.append(","+ (100.0 * i/(double)m_groups));
        }
        ret.append(", notObserved\n");

//        for (Integer pid : m_PeptideFragmentationSiteOccurence.keySet()) {
//            HashMap<Class,HashMap<String, HashMap<String, int[]>>> pepSites = m_PeptideFragmentationSiteOccurence.get(pid);

        Class[] fClasses = m_FragmentationSiteOccurence.keySet().toArray(new Class[0]);

        HashMap<String, HashMap<String, int[]>> sites = m_FragmentationSiteOccurence.get(fClasses[0]);

        String[] nList =  sites.keySet().toArray(new String[0]);
        java.util.Arrays.sort(nList);

        for (String n : nList) {
            HashMap<String,int[]> cGroups = sites.get(n);
            String[] cList = cGroups.keySet().toArray(new String[0]);
            java.util.Arrays.sort(cList);
            for (String c : cList) {
                int[] groups = cGroups.get(c);
                ret.append(n + "," + c);
                for (int i = 0; i <= m_groups; i++) {
                    int count = 0;
                    for (Class fClass : fClasses) {
                        count += m_FragmentationSiteOccurence.get(fClass).get(n).get(c)[i];
                    }
                    ret.append("," + count);
                }
                ret.append("\n");
            }
        }
//        }
        return ret.toString();
    }

}
