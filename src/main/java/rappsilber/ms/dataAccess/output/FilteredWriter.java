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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class FilteredWriter  extends AbstractStackedResultWriter{
    FilterEntry[] filters;
    int m_writen = 0;

    @Override
    public void selfFinished() {
        
    }

    @Override
    public boolean selfWaitForFinished() {
        return true;
    }
    
    private class FilterEntry {
        String run;
        int scan;
        String p1;
        String p2;

        public FilterEntry(String run, int scan, String p1, String p2) {
            this.run = run;
            this.scan = scan;
            this.p1 = p1;
            this.p2 = p2;
        }
        
        public boolean isEntry(MatchedXlinkedPeptide match) {
            Spectra s = match.getSpectrum();
            if (s.getScanNumber() == scan) {
                if (run.length() == 0 || s.getRun().contentEquals(run)) {
                    String mp1 = match.getPeptides()[1].toString();
                    String mp2 = (match.getPeptides().length<2 ? "" :match.getPeptides()[1].toString());
                    if ((p1.contentEquals(mp1) && p2.contentEquals(mp2))
                            || (p1.contentEquals(mp2) && p2.contentEquals(mp1))) {
                        return true;
                    }
                }
            }
            return false;
        }
        
    }


    {
        filters = new FilterEntry[] {
            new FilterEntry("E090105_05.raw",1780,"KAAPSPISHVAEIR","SALEKGSR"),
            new FilterEntry("E090105_05.raw",2927,"VKVIPYSTFR","KFLR"),
            new FilterEntry("E090110_12.raw",1965,"LTQSKIWDVVEK","KNK"),
            new FilterEntry("E090110_12.raw",1969,"YISKYELDKAFSDR","KSLK"),
            new FilterEntry("E090110_12.raw",5394,"KVEGTAFVIFGIQDGEQR","HQQTVTIPPKSSLSVPYVIVPLK"),
            new FilterEntry("E090110_13.raw",1528,"SDDKVTLEER","QNQELKVR"),
            new FilterEntry("E090403_03.raw",760,"KSETSGR","QLVKDRK"),
            new FilterEntry("E090403_06.raw",2564,"KVVENEYVFTEQNLKK","INKDGSK"),
            new FilterEntry("E090403_06.raw",879,"GLKER","AKSNIK"),
            new FilterEntry("E090404_01.raw",1029,"AKIGGLNDPR","EKAK"),
            new FilterEntry("E090404_01.raw",910,"KDDPEYAEER","LNNKER"),
            new FilterEntry("E090404_02.raw",1803,"SDNSNFLKVGR","AKSNIK"),
            new FilterEntry("E090404_03.raw",3057,"KGDINPEVSMIR","NIVKLNNK"),
            new FilterEntry("E090404_03.raw",3285,"ILPTDGSAEFNVKYR","EKAIPK"),
            new FilterEntry("E090404_04.raw",4019,"VLDVTKEAQANLLTAK","KKFNHR"),
            new FilterEntry("E090404_06.raw",1977,"AKIGGLNDPR","SNIKSIR"),
            new FilterEntry("E090404_07.raw",2770,"DGETTDANGKTIVTGGNGPEDFQQHEQIR","LKESQLPR"),
            new FilterEntry("E081213_03.raw",2299,"QGALELIKK","KLVLSSEK"),
            new FilterEntry("E090105_05.raw",1097,"TTKIPQIGDK","TAYHSKR"),
            new FilterEntry("E090105_05.raw",1429,"SYMDQEKK","GNLMGKR"),
            new FilterEntry("E090105_05.raw",2520,"GNLMGKR","MKHGTYDKLDDDGLIAPGVR"),
            new FilterEntry("E090105_05.raw",4502,"LKIDPDTKAPNAVVITFEK","EKGPQVCcmAKLFGNIQK"),
            new FilterEntry("E090105_06.raw",3060,"VSGEDVIIGKTTPISPDEEELGQR","YSKR"),
            new FilterEntry("E090110_12.raw",1831,"GVFVLNKK","NKLTQSK"),
            new FilterEntry("E090110_12.raw",2723,"SPMoxYSIITPNILR","AHEAKIR"),
            new FilterEntry("E090110_13.raw",2331,"DSCcmVGSLVVKSGQSEDR","QPVPGQQMoxTLKIEGDHGAR"),
            new FilterEntry("E090403_01.raw",231,"SDRECcmPKCcmHSR","NKR"),
            new FilterEntry("E090403_04.raw",6471,"MILTHVDLIEKFLR","AMoxEYLKFR"),
            new FilterEntry("E090403_08.raw",840,"QAYLKK","KYQQR"),
            new FilterEntry("E090404_02.raw",5726,"KVEEEENAATLQLGQEFQLK","GCcmDNKIDIYQIHIPYAAK"),
            new FilterEntry("",4624,"AMEYLKFR","GPYAFKYTLRPEYK")
        };
    }

    public FilteredWriter() {
    }

    public FilteredWriter(ResultWriter m_innerWriter) {
        setInnerWriter(m_innerWriter);
    }

    public FilteredWriter(ResultWriter m_innerWriter, FilterEntry[] filters) {
        this(m_innerWriter);
        this.filters = filters;
    }

    public FilteredWriter(ResultWriter m_innerWriter, File CSVfile) throws FileNotFoundException, IOException {
        this(m_innerWriter, new FileInputStream(CSVfile));
    }


    public FilteredWriter(ResultWriter m_innerWriter, InputStream CSVfile) throws IOException {
        this(m_innerWriter);
        readFilterEntries(CSVfile);
    }

    public int readFilterEntries(InputStream CSVfile) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(CSVfile));
        String line = null;
        int countLines = 0;
        while ((line = in.readLine()) != null) {
            countLines ++;
        }
        return countLines;
    }



    @Override
    public void writeHeader() {
        try {
            setInnerWriter(new PeakListWriter(new FileOutputStream("/tmp/filteredPeakList.csv")));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FilteredWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
        super.writeHeader();
    }

    @Override
    public void writeResult(MatchedXlinkedPeptide match) throws IOException {
        for (FilterEntry fe :filters) {
            if (fe.isEntry(match)) {
                innerWriteResult(match);
                m_writen++;
                return;
            }
        }
        if (m_doFreeMatch) {
            match.free();
        }
    }

    @Override
    public int getResultCount() {
        return m_writen;
    }




}
