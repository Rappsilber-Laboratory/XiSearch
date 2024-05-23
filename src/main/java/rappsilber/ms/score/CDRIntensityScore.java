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
package rappsilber.ms.score;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.RunConfig;
import rappsilber.ms.sequence.AminoAcid;
import rappsilber.ms.sequence.Peptide;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.sequence.ions.FragmentationSite;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.match.MatchedBaseFragment;
import rappsilber.ms.spectra.match.MatchedFragmentCollection;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.ms.statistics.utils.AAPair2Double;
import rappsilber.ms.statistics.utils.GroupPeaksByTopPeaks;
import rappsilber.ms.statistics.utils.SpectraPeakGroups;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class CDRIntensityScore extends AbstractScoreSpectraMatch {
    SpectraPeakGroups m_PeakGrouping = new GroupPeaksByTopPeaks(new double[]{0.25,0.5,0.75,1});

    AAPair2Double[]     m_GroupedProbability;



    public void readStatistic(RunConfig conf) throws FileNotFoundException, IOException {
        String f = (String) conf.retrieveObject("statistic".toUpperCase());
        if (f == null) {
            Logger.getLogger(CDRIntensityScore.class.getName()).log(Level.SEVERE, "CDRScore without defined statistics-file");
            throw new FileNotFoundException("statistic file");
        }
        readStatistic(new FileInputStream(new File(f)));
    }

    public void readStatistic(File f) throws FileNotFoundException, IOException {
        readStatistic(new FileInputStream(f));
    }

    public void readStatistic(InputStream in) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        // read header (C,N,0.25,0.5,0.75,1,unmatched)
        String line = br.readLine();
        String[] header = line.split(",");
        double[] groupborders = new double[header.length - 3];
        // determain the classes
        for (int i= 0; i< groupborders.length; i++) {
            groupborders[i]=Double.parseDouble(header[i+2]);
        }

        m_PeakGrouping = new GroupPeaksByTopPeaks(groupborders);

        m_GroupedProbability = new AAPair2Double[groupborders.length + 1];
        for (int i = 0; i< m_GroupedProbability.length; i++) {
            m_GroupedProbability[i] = new AAPair2Double();
        }

        // read in each line
        while ((line = br.readLine()) != null) {
            String[] row = line.split(",");
            AminoAcid n = AminoAcid.getAminoAcid(row[0]);
            AminoAcid c = AminoAcid.getAminoAcid(row[1]);
            // store
            for (int i = 0; i< m_GroupedProbability.length; i++) {
                m_GroupedProbability[i].set(n, c,Double.parseDouble(row[i+2]));
            }
        }
    }


    public double score(MatchedXlinkedPeptide match) {
        // get the grouped peaks

        HashMap<Double,ArrayList<SpectraPeak>> groups = m_PeakGrouping.getPeakGroubs(match.getSpectrum());
        MatchedFragmentCollection[] mfc = new MatchedFragmentCollection[groups.size()];
        Double[] boarders = groups.keySet().toArray(new Double[0]);
        java.util.Arrays.sort(boarders);
        MatchedFragmentCollection allMatches = new MatchedFragmentCollection(match.getSpectrum().getPrecurserCharge());

        // for each group get
        ArrayList<SpectraPeak> peaks;

        for (int g=0; g < groups.size(); g ++) {
            peaks = groups.get(boarders[g]);
            MatchedFragmentCollection inGroupMatches = (
                    mfc[g] = new MatchedFragmentCollection(match.getSpectrum().getPrecurserCharge()));
            //for each peak
            for (SpectraPeak p : peaks) {
                // collect the matches
                for (Fragment f : p.getMatchedFragments()) {
                    inGroupMatches.add(f, 1);
                    allMatches.add(f,1);
                }
            }
            ArrayList<MatchedBaseFragment> toDelete = new ArrayList<MatchedBaseFragment>();
            //for each matched fragmentPrimary group
            for (MatchedBaseFragment mbf : inGroupMatches) {
                // compare whether that group was found in any of the more intens groups
                for (int pg = 0 ; pg < g; pg++) {
                    MatchedFragmentCollection pgm = mfc[pg];
                    ArrayList<MatchedBaseFragment> bf;
                    if ((bf = pgm.getMatchedFragmentGroup(mbf.getBaseFragment())).size() >0)  {
                        // if found delete the less meaningfull group
                        if (bf.get(0).isBaseFragmentFound() || ! mbf.isBaseFragmentFound() ) { // keep the more intens non-lossy fragmentPrimary
                            toDelete.add(mbf); // mark for deletion
                        } else  {
                            for (MatchedBaseFragment del : bf) {
                                // keep the found base-fragmentPrimary
                                pgm.remove(del);
                            }
                        }
                        // check the probability of finding that fragmentPrimary with the given intensity
                    }
                }
            }

            for (MatchedBaseFragment del : toDelete) {
                inGroupMatches.remove(del);
            }
        }

        Peptide[] peptides = match.getPeptides();

        HashMap<Peptide, boolean[]> sitesFound = new HashMap<Peptide, boolean[]>(peptides.length);

        for (Peptide p: peptides) {
            sitesFound.put(p, new boolean[p.length() - 1]);
        }

        int scoredFragments = 0;
        double scoreAllFragments = 0;
        // score the fragments
        for (int g=0; g < groups.size() ; g ++) {
            AAPair2Double prob = m_GroupedProbability[g];
            MatchedFragmentCollection inGroupMatches = mfc[g];
            for (MatchedBaseFragment mbf : inGroupMatches) {
                double FragmentScore = 0;
                FragmentationSite[] sites = mbf.getBaseFragment().getFragmentationSites();
                for (FragmentationSite fs : sites) {
                    FragmentScore += prob.get(fs.NTerm, fs.CTerm, 0.5);
                    sitesFound.get(fs.peptide)[fs.site] = true;
                }
                FragmentScore /= sites.length;
                scoreAllFragments += FragmentScore;
                scoredFragments ++;
            }
        }
        AAPair2Double prob = m_GroupedProbability[groups.size()];

        //each unmatched non-lossy fragmentPrimary
        for (Peptide p : peptides)  {
            boolean[] found = sitesFound.get(p);
            for (int s = 0 ; s < p.length() - 1; s++ ) {
                if (!found[s]) {
                    // check the probability to find it as an unmatched fragmentPrimary
                    // and score the fragmentPrimary
                    double FragmentScore = prob.get(p.aminoAcidAt(s), 
                                                p.aminoAcidAt(s+1)
                                                , 0.5);
                    scoreAllFragments += FragmentScore;
                    scoredFragments ++;
                }
            }
        
        }

        addScore(match, name(), scoreAllFragments/scoredFragments);

        // return the average
        return scoreAllFragments/scoredFragments;

    }

    public double getOrder() {
        return 30;
    }


}
