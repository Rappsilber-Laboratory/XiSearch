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
import java.util.HashMap;
import rappsilber.ms.sequence.ions.CrosslinkedFragment;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.FragmentationSite;
import rappsilber.ms.sequence.ions.loss.Loss;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedBaseFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 * counts the occurrence of fragmentation between
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class FragmentationSiteIntensityIonSpecific extends AbstractStatistic {


    private HashMap<String,HashMap<String,int[]>> m_FragmentationSiteOccurence = new HashMap<String, HashMap<String, int[]>>();
    private int m_countSpectra = 0;
    private int m_groups = 10;


    private String[] convertFragmentationSite(MatchedXlinkedPeptide match, Fragment f) {
        FragmentationSite fs = f.getFragmentationSites()[0];
        String[] siteNames = new String[]{fs.NTerm.SequenceID,fs.CTerm.SequenceID};


        if (fs.site==0) {
            siteNames[0] += "nt";
        }
        if (fs.site == fs.peptide.length() - 1) {
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

    public FragmentationSiteIntensityIonSpecific() {
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

    private void incrementSite(String n, String c, int group) {
       HashMap<String,int[]> nGroup = m_FragmentationSiteOccurence.get(n);
        if (nGroup == null) {
            nGroup = new HashMap<String, int[]>();
            int[] cGroup = new int[m_groups + 1];
            java.util.Arrays.fill(cGroup, 0);
            cGroup[group] = 1;
            nGroup.put(c, cGroup);
            m_FragmentationSiteOccurence.put(n,nGroup);
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


        HashMap<String,HashMap<String,Boolean>> foundSites = new HashMap<String, HashMap<String, Boolean>>();

        HashMap<Fragment,Boolean> foundFragments = new HashMap<Fragment, Boolean>();

        // read out all possible fragmentation sites
        for (Fragment f: match.getPossibleFragments()) {
            if (validFragment(f)) {
                foundFragments.put(f, Boolean.FALSE);
            }
        }

        // now look if and where we find them
        // among all peaks sorted by intensity
        Collection<SpectraPeak> peaks = deisotoped.getTopPeaks(-1);

        int group = 0;
        int peakCount = peaks.size();
        double groupSize = peakCount/(double)m_groups;
        int currentPeak = -1;
        int counted = 0;
        for (SpectraPeak sp : peaks) {
            currentPeak++;
            group = (int) (currentPeak / groupSize);
//            if (group > 9) {
//                System.err.println("found it");
//            }

            for (SpectraPeakMatchedFragment mf : sp.getMatchedAnnotation()) {
                Fragment f = mf.getFragment();
                MatchedBaseFragment mbf = mfc.getMatchedFragmentGroup(f, mf.getCharge());
                
                // only if the fragment matches the conditions and was not found yet
                if (canCountFragment(f,mf.getCharge(),mfc) && ! foundFragments.get(mbf.getBaseFragment())) {

                    String[] siteNames = convertFragmentationSite(match, f);
                    String n = siteNames[0];
                    String c = siteNames[1];

                    // only if we didn't count that site combination already
                    foundFragments.put(mbf.getBaseFragment(), Boolean.TRUE);
                    incrementSite(n,c,group);
                    counted++;
 
                }
            }
        }

        int found = 0;
        int missed = 0;
        // now look at all non matched fragmentation sites
        for (Fragment f : foundFragments.keySet()) {
            if (! foundFragments.get(f)) {
                missed ++;

                String[] siteNames = convertFragmentationSite(match, f);
                String n = siteNames[0];
                String c = siteNames[1];

                incrementSite(n,c,m_groups);
            } else {
                found++;
                //System.err.print('.');
            }
       }
       System.err.println("counted : " + counted + ",  found : " + found + ",  missed : " + missed);


        m_countSpectra++;

        

    }

    @Override
    public void writeFile(OutputStream output) {
        PrintStream out = new PrintStream(output);
        out.print("C,N");
        for (int i=1;i<=m_groups;i++) {
            out.print(","+ (100.0 * i/(double)m_groups));
        }
        out.print(", notFound");
        out.println(getTable());
        out.flush();
    }


    public String getTable() {
        //StringBuffer ret = new StringBuffer("#intesity based fragmentation events of aminoacid pairs\n");
        StringBuffer ret = new StringBuffer();
        String[] nList =  m_FragmentationSiteOccurence.keySet().toArray(new String[0]);
        java.util.Arrays.sort(nList);

        for (String n : nList) {
            HashMap<String,int[]> cGroups = m_FragmentationSiteOccurence.get(n);
            String[] cList = cGroups.keySet().toArray(new String[0]);
            java.util.Arrays.sort(cList);
            for (String c : cList) {
                int[] groups = cGroups.get(c);
                ret.append(n + "," + c);
                for (int count : groups) {
                    ret.append("," + count);
                }
                ret.append("\n");
            }
        }
        return ret.toString();
    }


}
