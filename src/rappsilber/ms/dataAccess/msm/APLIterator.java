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
package rappsilber.ms.dataAccess.msm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import rappsilber.config.RunConfig;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.match.PreliminaryMatch;
import rappsilber.ms.statistics.utils.UpdateableInteger;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class APLIterator extends AbstractMSMAccess {

    Spectra         m_current   = null;
    LinkedList<Spectra> m_next  = new LinkedList<Spectra>();
    InputStream     m_inputUnbufferd = null;
    BufferedReader  m_input     = null;
    File            m_inputFile = null;
    String          m_source = "";
//    String          m_inputPath = null;
    int             m_currentLine = 0;
    boolean         m_indexed   = false;
    int             m_countReadSpectra = 0;
    RunConfig       m_config;
    int            m_nextID = 0;

    static Pattern  RE_PEAK_ENTRY = Pattern.compile("\\s*([0-9\\.]+(?:E\\+[0-9]+)?)\\s*([0-9\\.]+(?:[Ee]\\+?[0-9]+)?)\\s*");

    boolean         m_RunEmpty = false;
    int             m_discardedScans = 0;
    int[]           m_UnknowChargeStates = new int[]{2,3,4};
    int             m_defaultChargeState = 4;
    int             m_MaxChargeState = 5;
    private int     m_MinChargeState = 2;
    /** flags up, if it is an peak.apl file 
     * meaning whether the scans should be considered as unknown charge state
     **/
    private boolean m_isUnknownChargeFile = false;

//    int             d_debugCountNext = 0;

    private class PeakOccurence extends TreeMap<Double, UpdateableInteger> {
        public void inc(Double mz) {
            SortedMap<Double, UpdateableInteger> m = subMap(m_ToleranceUnit.getMinRange(mz), m_ToleranceUnit.getMaxRange(mz));
            if (m.size() == 0)
                put(mz, new UpdateableInteger(1));
            else
                m.get(m.firstKey()).value++;
        }
    };
    PeakOccurence m_foundPeaks = new PeakOccurence() ;

//    private ToleranceUnit   m_ToleranceUnit = new ToleranceUnit(0,"da");


    /**
     * provides a new msm-file based SpectraIterator
     * @param msmfile
     * @throws java.io.FileNotFoundException
     */
    public APLIterator(File msmfile, ToleranceUnit t, int minCharge) throws FileNotFoundException  {
        this(msmfile, t, minCharge, 0);
    }

    /**
     * provides a new msm-file based SpectraIterator
     * @param msmfile
     * @throws java.io.FileNotFoundException
     */
    public APLIterator(File msmfile, ToleranceUnit t, int minCharge, RunConfig config) throws FileNotFoundException  {
        this(msmfile, t, minCharge, 0, config);
    }

    /**
     * provides a new msm-file based SpectraIterator
     * @param msmfile
     * @throws java.io.FileNotFoundException
     */
    public APLIterator(File msmfile, ToleranceUnit t, int minCharge, int firstID, RunConfig config) throws FileNotFoundException  {
        m_MinChargeState = minCharge;
        m_nextID = firstID;
        m_config = config;

        m_UnknowChargeStates = new int[m_MaxChargeState - m_MinChargeState+1];
        for (int i = m_MinChargeState;i<=m_MaxChargeState; i++)
            m_UnknowChargeStates[i-m_MinChargeState] = i;

        setToleranceUnit(t);

        inputFromFile(msmfile);

    }

    public APLIterator(File msmfile, ToleranceUnit t, int minCharge, int firstID) throws FileNotFoundException  {
        this(msmfile, t, minCharge, firstID, null);
    }
    
    /**
     * provides a new msm-file based SpectraIterator
     * @param msmfile
     * @throws java.io.FileNotFoundException
     */
    public APLIterator(InputStream msmfile, String source, ToleranceUnit t, int minCharge, RunConfig config) throws FileNotFoundException  {
        this(msmfile, source, t, minCharge, config, 0);
    }
    
    /**
     * provides a new msm-file based SpectraIterator
     * @param msmfile
     * @throws java.io.FileNotFoundException
     */
    public APLIterator(InputStream msmfile, String source, ToleranceUnit t, int minCharge, RunConfig config, int firstID) throws FileNotFoundException  {
        m_nextID = firstID;
        setToleranceUnit(t);
        m_MinChargeState = minCharge;
        m_inputUnbufferd = msmfile;
        m_input = new BufferedReader(new InputStreamReader(msmfile));

        m_UnknowChargeStates = new int[m_MaxChargeState - m_MinChargeState+1];
        for (int i = m_MinChargeState;i<=m_MaxChargeState; i++)
            m_UnknowChargeStates[i-m_MinChargeState] = i;


        m_source = source;
        if (m_source.endsWith("peak.apl")) {
            m_isUnknownChargeFile =true;
        }
        
        m_next.addAll(readScan());// read first scan
        m_config = config;
        setInputPath(source);
    }
    
    
//    /**
//     * provides a new msm-file based SpectraIterator that reads data from
//     * the given stream
//     * @param msmStream
//     */
//    public MSMIterator(InputStream msmStream, int minCharge)  {
//        m_input = new BufferedReader(new InputStreamReader(msmStream));
//
//        m_next.addAll(readScan());// read first scan
//    }

//    protected void inputFromFile(File msmfile) throws FileNotFoundException {
//        m_inputFile = msmfile;
//        m_inputPath = msmfile.getAbsolutePath();
//        m_input = new BufferedReader(new InputStreamReader(new FileInputStream(msmfile)));
//        m_next.addAll(readScan()); // read first scan
//    }

    protected void inputFromFile(File msmfile) throws FileNotFoundException {
        m_inputFile = msmfile;
        m_inputPath = msmfile.getAbsolutePath();
        m_inputUnbufferd = new FileInputStream(msmfile);
        m_source = msmfile.getName();
        if (m_source.endsWith("peak.apl")) {
            m_isUnknownChargeFile =true;
        }

        GZIPInputStream gzipIn = null;
        try {
            gzipIn = new GZIPInputStream(m_inputUnbufferd);
        } catch (IOException ex) {
            try {
                m_inputUnbufferd.close();
            } catch (IOException ex1) {
                Logger.getLogger(MSMIterator.class.getName()).log(Level.SEVERE, null, ex1);
            }
            m_inputUnbufferd = new FileInputStream(msmfile);
        }
        if (gzipIn == null) {
            m_input = new BufferedReader(new InputStreamReader(m_inputUnbufferd));
        } else
            m_input = new BufferedReader(new InputStreamReader(gzipIn));
        
        m_next.addAll(readScan()); // read first scan
    }
    /**
     * returns the current Spectra
     * Every 
     * @return
     */
    public Spectra current() {
        return m_current;
    }

    public boolean hasNext() {
        return m_next.size() > 0;
    }

    public synchronized  Spectra next() {
//        System.err.println("next : " + (++d_debugCountNext));
        if (!m_next.isEmpty()) {
            m_current = m_next.removeFirst();
            m_current.setSource(m_inputPath);
            //m_next.removeFirst();
            if (m_next.isEmpty())
                m_next.addAll(readScan());
            m_countReadSpectra++;
            if (m_current.getTolearance() == null)
                m_current.setTolearance(getToleranceUnit());
//            if (m_next.isEmpty()) {
//                System.err.println("read everything");
//            }
            m_current.setReadID(m_nextID++);
        } else
            m_current = null;

        return m_current;
    }

    /**
     * converts the input string into an int while checking, that it foloows the format [0-9]+\+
     * @param chargeString
     * @return the decoded chargestate
     * @throws java.lang.NumberFormatException
     */
    protected int getCharge(String chargeString) throws NumberFormatException {
        int charge;
        if (!chargeString.endsWith("+")) {
//            Logger.getLogger(APLIterator.class.getName()).log(Level.WARNING, "Unknown charge state (" + chargeString + ")  while reading spectra from " + getInputPath() + ": line : " + m_currentLine + ". Assuming charge state 1");
            charge = Integer.parseInt(chargeString);
        } else {
            charge = Integer.parseInt(chargeString.substring(0, chargeString.length() - 1));
        }
        return charge;
    }

    /**
     * parses the string and takes up all the information, it knows how to 
     * handle (run and scan number) and stores them in the spectra
     * @param Title the title of the spectra as read from the msm-file
     * @param s     the spectra , that is currently build up
     */
    protected void parseTitle(String Title, Spectra s) {

        //TITLE=RawFile: m090623_03.raw FinneganScanNumber: 2995
        int scan = -1;
        String run = "";
        String titleText = Title.trim();
//        double elutionFrom = s.getElutionTimeStart();
//        double elutionTo = s.getElutionTimeEnd();

        if (titleText.startsWith("header=header=")) {
            titleText = titleText.substring(14);
        } else if (titleText.startsWith("header=")) {
            titleText = titleText.substring(7);
        }

//        String[] ta = Title.split("(: | )");
        String[] preScanNumber = titleText.split(" finneganscannumber:\\s*");
        if (preScanNumber.length >1) {
            preScanNumber = preScanNumber[1].split("\\s+([^\\s]*:)?");
            scan = Integer.valueOf(preScanNumber[0]);
        } else {
            preScanNumber = titleText.split(" Index:\\s*");
            preScanNumber = preScanNumber[1].split("\\s+([^\\s]*:)?");
            scan = Integer.valueOf(preScanNumber[0]);
        }

        String[] preRun = titleText.split("[rR]aw[fF]ile:\\s*");
        preRun = preRun[1].split("\\s+([^\\s]*:)?");
        run = preRun[0];

        String[] prePrecintensity = titleText.split("precintensity:\\s*");
        if (prePrecintensity.length >1) {
            prePrecintensity = prePrecintensity[1].split("\\s+([^\\s]*:)?");
            s.setPrecurserIntensity(Double.parseDouble(prePrecintensity[0]));
        }
        String[] preElution = titleText.split("elution from:\\s*");
        if (preElution.length >1) {
            preElution = preElution[1].split("\\s+([^\\s]*:)?");
            preElution = preElution[0].split("\\s+to\\s*");
            s.setElutionTimeStart(Double.parseDouble(preElution[0]));
            if (preElution.length >1)
                s.setElutionTimeEnd(Double.parseDouble(preElution[1]));
        }

        run = run.replace("\\.[a-zA-Z_]*$", "");
        s.setRun(run);
        s.setScanNumber(scan);
        
        if (Title.trim().endsWith("_peak_")) {
            s.setPrecurserCharge(m_defaultChargeState);
            s.setPrecoursorChargeAlternatives(m_UnknowChargeStates);
        }

    }


    /**
     * reads in one spectra from the file, and returns one spectra for each
     * "recognised" charge state (e.g. CHARGE=2+ and 3+)
     * @return list of spectra (only difference is the charge state)
     */
    protected ArrayList<Spectra> readScan() {
        ArrayList<Spectra> ret = new ArrayList<Spectra>(1);

        ArrayList<Integer> matchedPeptides = new ArrayList<Integer>();
        String line;
        Spectra s = null;
        String[] chargeStates = null;
        Matcher m;

        try {
            m_currentLine++;
            line = m_input.readLine();
            while (line != null) {
                if (line.startsWith("peaklist start")) {
                    //s = Spectra.getSpectra(); // we read a new spectra
                    s = new Spectra(); // we read a new spectra
                    s.setTolearance(m_ToleranceUnit);
                    s.setSource(m_source);
                } else if (line.startsWith("peaklist end")) { // finished with this spectra
                    
                    if (m_isUnknownChargeFile || s.getPrecoursorChargeAlternatives().length > 1) {
                        // the peaks.apl files contain each spectra twice - ones for doubly ones for triply charged
                        if (getCharge(chargeStates[0]) == 3) { // so we ignore the triply charged ones 
                            line = m_input.readLine();                            
                            continue;
                        }
                        
                        s.setPrecurserCharge(m_defaultChargeState);
                        s.setPrecoursorChargeAlternatives(m_UnknowChargeStates);
                    } else if (chargeStates.length > 1) {
                        s.setPrecurserCharge(m_defaultChargeState);
                        s.setPrecoursorChargeAlternatives(m_UnknowChargeStates);
                    } else {
                        int charge = getCharge(chargeStates[0]);
                        s.setPrecurserCharge(charge);
                        s.setPrecoursorChargeAlternatives(new int[]{charge});
                    }

                    if (s.getPrecurserCharge() >= m_MinChargeState || chargeStates.length > 1)
                        ret.add(s);
                    else
                        s.free();


                    if (!ret.isEmpty()) // if we found a valid spectra return here
                        return ret;

                } else if (line.startsWith("mz=")) { // is actually m/z

                    s.setPrecurserMZ(Double.parseDouble(line.substring(3)));

                } else if (line.startsWith("header=")) { // is actually m/z

                    parseTitle(line, s);

                } else if (line.startsWith("charge=")) { // charge state(s)

                    chargeStates = line.substring(7).split(" and ");

                } else if ((m = RE_PEAK_ENTRY.matcher(line)).matches()) {
                    s.addPeak(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)));
                } // else ignore
                line = m_input.readLine();
            }
        } catch (IOException ex) {
            Logger.getLogger(APLIterator.class.getName()).log(Level.SEVERE,
                    "IO-Error while reading line from " + getInputPath() +
                    ": line : " + m_currentLine + ".", ex);
        }
        return ret;
        
    }





    /**
     * if the input file was a file, then it gathers some information about it
     * like maximal precursor mass, number of entries and number of returnable spectra
     * @throws FileNotFoundException
     */
    @Override
    public void gatherData() throws FileNotFoundException {
        if (m_inputFile == null)
            throw new UnsupportedOperationException("Can't pre gather statistics on non-file based inputs");

        double maxUnknowChargeStates = m_UnknowChargeStates[m_UnknowChargeStates.length - 1];
        double unknownChargeSTateCount = m_UnknowChargeStates.length;

        
        BufferedReader input = null;
        
        try {
            input = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(m_inputFile))));
        } catch (IOException ex) {
            input = new BufferedReader(new InputStreamReader(new FileInputStream(m_inputFile)));
        }
                
        //BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(m_inputFile)));

        String line;
        int entries = 0;
        int spectra = 0;
        double maxmass = 0;
        Pattern linepattern = Pattern.compile("^(header=|charge=|mz=|peaklist end)\\s*([0-9\\+\\.E]*)?.*");

        double mass = 0;
        String charge =  "   ";
        int peakCount = 0;
        try {
            boolean isPeak = false;
            while ((line = input.readLine()) != null) {
                Matcher m = linepattern.matcher(line);
                if (m.matches()) {
                    String part = m.group(1);
                    if (part.contentEquals("mz=")) {
                        // is actually m/z
                        // System.err.println(">>>>>>>>>>>>>>>>>> " + line.substring(8));
                        mass = Double.parseDouble(m.group(2));
                    } else if (part.contentEquals("charge=")){
                        charge = line.substring(7).trim();
                    } else if (part.contentEquals("header=")){
                        if (line.trim().endsWith("_peak_")) {
                            isPeak = true;
                            peakCount++;
                        }
                    } else {
                        entries ++;

                        if (isPeak) { // unsure charge state
                            double cMass = (mass - Util.PROTON_MASS) * maxUnknowChargeStates + Util.PROTON_MASS;
                            if (cMass > maxmass)
                                 maxmass = cMass;
                        } else  {
                            double chargeValue = Double.parseDouble(charge);
                            if (chargeValue >= m_MinChargeState) {
                                double cMass = (mass - Util.PROTON_MASS) * chargeValue + Util.PROTON_MASS;
                                if (cMass > maxmass)
                                     maxmass = cMass;
                            }
                    }

                    }
                }

            }
            m_MaxPrecursorMass = maxmass;
            m_scanCount = entries - (peakCount/2);
        } catch (IOException ex) {
            Logger.getLogger(APLIterator.class.getName()).log(Level.SEVERE, null, ex);
        }



    }

    @Override
    public int countReadSpectra() {
        return m_countReadSpectra;
    }


    @Override
    public void close() {
        try {
            m_input.close();
            m_inputUnbufferd.close();
        } catch (IOException ex) {
            Logger.getLogger(MSMIterator.class.getName()).log(Level.SEVERE, "error while closing the msm-file", ex);
        }
    }

    @Override
    public boolean canRestart() {
        return m_inputFile != null;
    }

    @Override
    public void restart() {
        if (canRestart()) {
            m_nextID  = 0;
            close();
            try {
                inputFromFile(m_inputFile);
            } catch (FileNotFoundException ex) {
                String message = "Error while trying to reopen the input-file: " + m_inputFile;
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, message, ex);
                throw new RuntimeException(message, ex);
            }
        }
    }



}
