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
package rappsilber.ms.dataAccess.output;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.ms.spectra.match.PreliminaryMatch;
import rappsilber.utils.SortedLinkedList;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class MSMWriter extends AbstractResultWriter{
//    File            m_outFile;
    PrintStream     m_out;
    String          m_period;
    String          m_experiment;
    String          m_cycles;
    int             m_countResults = 0;
    int             m_countTopResults = 0;
    private boolean m_writePeptides = false;
    private NumberFormat m_numberformat = NumberFormat.getInstance(Locale.ENGLISH);


    /**
     * constructor to create a new mgc/mgx-file
     * @param out   where to store them
     * @param period
     * @param experiment
     * @param cycles
     * @throws IOException
     */
    public MSMWriter(File out, String period, String experiment, String cycles) throws IOException {
//        m_outFile   = out;
        this(new FileOutputStream(out), period, experiment, cycles);
    }
    
    /**
     * constructor to create a new mgc/mgx-file
     * @param out   where to store them
     * @param period
     * @param experiment
     * @param cycles
     * @throws IOException
     */
    public MSMWriter(OutputStream out, String period, String experiment, String cycles) throws IOException {
        m_out       = new PrintStream(out);
        m_period    = period;
        m_experiment = experiment;
        m_cycles    = cycles;
        m_numberformat.setMaximumFractionDigits(9);
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        finished();
                    }catch (Exception e) {}
                }
            });
        
    }    

    /**
     * writes the file header
     */
    public void writeHeader() {
        m_out.println("SEARCH=MIS");
        m_out.println("REPTYPE=Peptide");
    }

    /**
     * write a single spectra
     * @param s
     */
    public void writeSpectra(Spectra s)  {
        writeSpectra(s, -1);
    }

    /**
     * writes a single spectra - but only the top peaks
     * @param s the spectrum to export
     * @param top how many peaks to export
     * @param peptideIDs ids of matched alpha-peptides
     */
    public synchronized  void writeSpectra(Spectra s, int top)  {
        m_out.println("BEGIN IONS");
        m_out.println("PEPMASS=" + s.getPrecurserMZ());
        if (s.getChargeStateSpectra().length > 1) {
            m_out.println("CHARGE=2+ and 3+");
        } else
            m_out.println("CHARGE=" + s.getPrecurserCharge() + "+");
        if (arePeptidesWriten()) {
            String sMatches = "";
            for (PreliminaryMatch pm : s.getPreliminaryMatch()) {
                sMatches += ":matchgroup:" + pm.toString();
            }
            if (sMatches.length() > 0)
                m_out.println("PEPTIDEMATCHES=" + sMatches);
        }
//        m_out.println("CHARGE=" + s.getPrecurserCharge() + "+");
        m_out.println("TITLE="
                + " Elution from: " + s.getElutionTimeStart()
                + " to " + m_numberformat.format(s.getElutionTimeEnd())
                + " period: "+ m_period
                + " experiment: " +  m_experiment
                + " cycles: "+ m_cycles
                + " precIntensity: " + m_numberformat.format(s.getPrecurserIntensity())
                + " RawFile: " + s.getRun()
                + " FinneganScanNumber: " + m_numberformat.format(s.getScanNumber()));
        if (top > 0) {
            
            SortedLinkedList<SpectraPeak> topPeaks = new SortedLinkedList<SpectraPeak>();
            topPeaks.addAll(s.getTopPeaks(top));

            for (SpectraPeak sp : topPeaks) {
                m_out.println(m_numberformat.format(sp.getMZ()) + " " + m_numberformat.format(sp.getIntensity()));
            }
        } else
            for (SpectraPeak sp : s) {
                m_out.println(m_numberformat.format(sp.getMZ()) + " " + m_numberformat.format(sp.getIntensity()));
            }
        m_out.println("END IONS");
        m_out.println();
        m_countResults++;

    }

    /**
     * close the mgc-file
     */
    public void close() {
        m_out.close();
    }

    public void writeResult(MatchedXlinkedPeptide match) {
        writeSpectra(match.getSpectrum());
        if (match.getMatchrank() == 1)
            m_countTopResults ++;
//        m_countResults++;
        if (m_doFreeMatch)
            match.free();
    }

    public void finished() {
        close();
        super.finished();
    }

    public int getResultCount() {
        return m_countResults;
    }

    public int getTopResultCount() {
        return m_countTopResults;
    }

    /**
     * @return the m_writePeptides
     */
    public boolean arePeptidesWriten() {
        return m_writePeptides;
    }

    /**
     * @param m_writePeptides the m_writePeptides to set
     */
    public void setWritePeptides(boolean m_writePeptides) {
        this.m_writePeptides = m_writePeptides;
    }

    public void flush() {
        m_out.flush();
    }

}
