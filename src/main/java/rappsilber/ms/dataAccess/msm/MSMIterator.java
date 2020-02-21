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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import rappsilber.config.RunConfig;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.dataAccess.utils.RobustFileInputStream;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.spectra.match.PreliminaryMatch;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class MSMIterator extends AbstractMSMAccess {



    Spectra         m_current   = null;
    LinkedList<Spectra> m_next  = new LinkedList<Spectra>();
    InputStream m_inputUnbufferd = null;
    BufferedReader  m_input     = null;
    File            m_inputFile = null;
    String          m_source = "";
//    String          m_inputPath = null;
    int             m_currentLine = 0;
    boolean         m_indexed   = false;
    int             m_countReadSpectra = 0;
    private int            m_nextID = 0;
    private boolean titel_error_shown  = false;

    static Pattern  RE_PEAK_ENTRY = Pattern.compile("\\s*([0-9\\.]+(?:E\\+[0-9]+)?)\\s*([0-9\\.]+(?:[Ee]\\+?[0-9]+)?)\\s*");

    static Pattern  RE_MASCOT_PREC_ENTRY = Pattern.compile("PEPMASS=([0-9\\.]+(?:[Ee]\\+?[0-9]+)?)\\s+([0-9\\.]+(?:[Ee]\\+?[0-9]+)?)");

    static Pattern[] RE_TITLE_TO_RUN = new Pattern[]{
        Pattern.compile("TITLE=\\s*RawFile\\:\\s*(.+)\\s+FinneganScanNumber\\:\\s+([0-9]+)(?:\\s+_sil_)?"), // old maxquant
        Pattern.compile("TITLE=[0-9]+: (?:Scan|Sum of [0-9]+ scans in range) (?:[0-9]+) \\(?:rt=([0-9\\.]+)\\) .*\\[(?:.*[\\/\\\\])([^\\/\\\\]*)\\]"), // some mascot version
        Pattern.compile("TITLE=\\s*(.+)\\s+Spectrum(?:[0-9]+)\\s*scans: (?:[0-9]*)(?:\\s+.*)?"), // qex
        Pattern.compile("TITLE=File\\:(.*[0-9A-Za-z])\\s*Scans\\:(?:[0-9]+)\\s*RT\\:.+\\s*Charge\\:(?:[0-9]+)[+].+"), // massmatrix
        Pattern.compile("TITLE=(.*)\\.(ß:[0-9]+)\\.[0-9]+\\.(?:[0-9]+)?(:?\\s.*)?$"), // MSCONVERT
        Pattern.compile("TITLE=.*\\s(?:scan|index)=(?:[0-9]+)_(.*)?$"), // OPENMS
//        Pattern.compile("TITLE=([^\\s\\.]*).*"), // generic - take the first string after TITLE= as run
        Pattern.compile("TITLE=([^\\s\\.]*).*"), // generic - take the first string after TITLE= as run
    };

    static Pattern[] RE_TITLE_TO_SCAN = new Pattern[]{
        Pattern.compile("TITLE=\\s*RawFile\\:\\s*(?:.+)\\s+FinneganScanNumber\\:\\s+([0-9]+)(?:\\s+.*)?"), // old maxquant
        Pattern.compile("TITLE=[0-9]+: (?:Scan|Sum of [0-9]+ scans in range) ([0-9]+) \\(?:rt=([0-9\\.]+)\\) .*\\[(?:.*[\\/\\\\])(?:[^\\/\\\\]*)\\]"), // some mascot version
        Pattern.compile("TITLE=\\s*(?:.+)\\s+Spectrum(?:[0-9]+)\\s*scans: ([0-9]*)(?:\\s+.*)?"), // qex
        Pattern.compile("TITLE=File\\:(?:.*[0-9A-Za-z])\\s*Scans\\:([0-9]+)\\s*RT\\:.+\\s*Charge\\:([0-9]+)[+].+"), // massmatrix
        Pattern.compile("TITLE=(?:.*)\\.([0-9]+)\\.[0-9]+\\.(?:[0-9]+)?(?:\\s.*)?$"), // MSCONVERT
        Pattern.compile("TITLE=.*\\s(?:scan|index)=([0-9]+)_(?:.*)?$"), // OPENMS
    //    Pattern.compile("TITLE=.*scan[s][:=]\\s*([0-9]*)"), // generic - take the first number that is longer then 3 digits
        Pattern.compile("TITLE=[^\\s\\.]*(?:[0-9]{0,2}[^0-9]+)*([0-9]{3,10}).*") // generic - take the first number that is longer then 3 digits
    };

    static Pattern[] RE_TITLE_TO_RETENTION = new Pattern[]{
        null, // old maxquant
        Pattern.compile("TITLE=[0-9]+: (?:Scan|Sum of [0-9]+ scans in range) (?:[0-9]+) \\(rt=([0-9\\.]+)\\) .*\\[(?:.*[\\/\\\\])([^\\/\\\\]*)\\]"), // some mascot version
        null, // qex
        null, // massmatrix
        null, // MSCONVERT
        null, // OPENMS
        null,  // generic
        null  // generic
    };

    static Pattern[] RE_TITLE_TO_CHARGE = new Pattern[]{
        null, // old maxquant
        null, // some mascot version
        null, // qex
        Pattern.compile("TITLE=File\\:(?:.*[0-9A-Za-z])\\s*Scans\\:(?:[0-9]+)\\s*RT\\:.+\\s*Charge\\:([0-9]+)[+].+"), // massmatrix
        Pattern.compile("TITLE=(?:.*)\\.(?:[0-9]+)\\.[0-9]+\\.([0-9]+)?(?:\\s.*)?$"), // MSCONVERT
        null, // OPENMS
        null,  // generic
        null  // generic
    };
    

    private Pattern  RE_USER_SUPPLIED_RUN_NAME = null;
    private Pattern  RE_USER_SUPPLIED_SCAN_NUMBER = null;
    
    private static String NUMBER_PATTERN="[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?";
    
    static Pattern RTINSECOND_PAIR = Pattern.compile("RTINSECONDS=\\s*("+NUMBER_PATTERN+")\\s*([-,;]?\\s*"+NUMBER_PATTERN+")?");

    boolean         m_RunEmpty = false;
    int             m_discardedScans = 0;
    int[]           m_UnknowChargeStates = new int[]{3,4,5,6};
    int             m_defaultChargeState = 4;
    RunConfig       m_config;
    int             m_MaxChargeState = 7;
    private int             m_MinChargeState = 1;
    
    
    static private String addCharges="ADDITIONALCHARGES=";

    /**
     * provides a new msm-file based SpectraIterator
     * @param msmfile
     * @throws java.io.FileNotFoundException
     */
    public MSMIterator(InputStream msmfile, String source, ToleranceUnit t, int minCharge, RunConfig config) throws FileNotFoundException, ParseException, IOException  {
        this(msmfile, source, t, minCharge, config, 0);
    }

    /**
     * provides a new msm-file based SpectraIterator
     * @param msmfile
     * @throws java.io.FileNotFoundException
     */
    public MSMIterator(InputStream msmfile, String source, ToleranceUnit t, int minCharge, RunConfig config, int firstID) throws FileNotFoundException, ParseException, IOException  {
        setToleranceUnit(t);
        m_nextID = firstID;
        m_MinChargeState = minCharge;
        m_inputUnbufferd = msmfile;
        m_input = new BufferedReader(new InputStreamReader(m_inputUnbufferd));
        m_config = config;
 
        m_UnknowChargeStates = new int[m_MaxChargeState - m_MinChargeState+1];
        for (int i = m_MinChargeState;i<=m_MaxChargeState; i++)
            m_UnknowChargeStates[i-m_MinChargeState] = i;

        m_source = source;
        readRegularExpressionsFromConfig();
        m_next.addAll(readScan());// read first scan
        setInputPath(source);
    }

    /**
     * provides a new msm-file based SpectraIterator
     * @param msmfile
     * @throws java.io.FileNotFoundException
     */
    public MSMIterator(File msmfile, ToleranceUnit t, int minCharge, RunConfig config) throws FileNotFoundException, ParseException, IOException  {
        this(msmfile, t, minCharge, config, 0);
    }

    /**
     * provides a new msm-file based SpectraIterator
     * @param msmfile
     * @throws java.io.FileNotFoundException
     */
    public MSMIterator(File msmfile, ToleranceUnit t, int minCharge, RunConfig config, int firstID) throws FileNotFoundException, ParseException, IOException  {
        m_nextID = firstID;
        setToleranceUnit(t);
        m_MinChargeState = minCharge;
        m_config = config;
        readRegularExpressionsFromConfig();

        m_UnknowChargeStates = new int[m_MaxChargeState - m_MinChargeState+1];
        for (int i = m_MinChargeState;i<=m_MaxChargeState; i++)
            m_UnknowChargeStates[i-m_MinChargeState] = i;

        inputFromFile(msmfile);
        
    }

    protected void readRegularExpressionsFromConfig() {
        String scan = null;
        String run = null;
        if (m_config!=null) {
            scan = m_config.retrieveObject("SCAN_RE", null);
            run = m_config.retrieveObject("RUN_RE", null);
        }
        if (run != null) {
            setUserSuppliedRunNameRE(Pattern.compile(run,Pattern.CASE_INSENSITIVE));
        }
        if (scan != null) {
            setUserSuppliedScanNumberRE(Pattern.compile(scan,Pattern.CASE_INSENSITIVE));
        }
    }
    
    protected void inputFromFile(File msmfile) throws FileNotFoundException, ParseException, IOException {
        m_inputFile = msmfile;
        m_inputPath = msmfile.getAbsolutePath();
        m_inputUnbufferd = new RobustFileInputStream(msmfile);
        m_source = msmfile.getName();
        setInputPath(m_inputPath);

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
     * @return the RE_USER_SUPPLIED_RUN_NAME
     */
    public Pattern getUserSuppliedRunNameRE() {
        return RE_USER_SUPPLIED_RUN_NAME;
    }

    /**
     * @param aRE_USER_SUPPLIED_RUN_NAME the RE_USER_SUPPLIED_RUN_NAME to set
     */
    public void setUserSuppliedRunNameRE(Pattern aRE_USER_SUPPLIED_RUN_NAME) {
        RE_USER_SUPPLIED_RUN_NAME = aRE_USER_SUPPLIED_RUN_NAME;
    }

    /**
     * @return the RE_USER_SUPPLIED_SCAN_NUMBER
     */
    public Pattern getUserSuppliedScanNumberRE() {
        return RE_USER_SUPPLIED_SCAN_NUMBER;
    }

    /**
     * @param aRE_USER_SUPPLIED_SCAN_NUMBER the RE_USER_SUPPLIED_SCAN_NUMBER to set
     */
    public void setUserSuppliedScanNumberRE(Pattern aRE_USER_SUPPLIED_SCAN_NUMBER) {
        RE_USER_SUPPLIED_SCAN_NUMBER = aRE_USER_SUPPLIED_SCAN_NUMBER;
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
            m_current = m_next.getFirst();
            m_current.setSource(m_inputPath);
            String peakFileName=m_source;
            m_current.setPeakFileName(peakFileName);
            m_next.removeFirst();
            if (m_next.isEmpty())
                try {
                    m_next.addAll(readScan());
                } catch (Exception ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                    m_config.getStatusInterface().setStatus("Error reading peaklist" + ex);
                    System.exit(-1);
                }
            m_countReadSpectra++;
            if (m_current.getTolearance() == null)
                m_current.setTolearance(getToleranceUnit());
//            if (m_next.isEmpty()) {
//                System.err.println("read everything");
//            }
            m_current.setReadID(m_nextID++);
            if (m_current.getAdditionalMZ() == null && m_config!=null) {
                m_current.setAdditionalMZ(m_config.getAdditionalPrecursorMZOffsets());
                if (m_current.getPrecoursorChargeAlternatives().length >1) {
                    HashSet<Double> mz = new HashSet<>();
                    if (m_config.getAdditionalPrecursorMZOffsets() != null) {
                        mz.addAll(m_config.getAdditionalPrecursorMZOffsets());
                    }
                    if (m_config.getAdditionalPrecursorMZOffsetsUnknowChargeStates() != null) {
                        mz.addAll(m_config.getAdditionalPrecursorMZOffsetsUnknowChargeStates());
                    }
                    m_current.setAdditionalMZ(mz);
                }
            }                    
        } else
            m_current = null;
        

        return m_current;
    }

    /**
     * converts the input string into an int while checking, that it follows the format [0-9]+\+
     * @param chargeString
     * @return the decoded chargestate
     * @throws java.lang.NumberFormatException
     */
    protected int getCharge(String chargeString) throws NumberFormatException {
        int charge;
        if (!chargeString.endsWith("+")) {
            try {
            charge = Integer.parseInt(chargeString);
            } catch (NumberFormatException nfe) {
                Logger.getLogger(MSMIterator.class.getName()).log(Level.WARNING, "Unknown charge state (" + chargeString + ")  while reading spectra from " + getInputPath() + ": line : " + m_currentLine + ". Assuming charge state 1");
                charge = 1;
            }
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
    protected void parseTitle(String Title, Spectra s) throws ParseException{

        //TITLE=RawFile: m090623_03.raw FinneganScanNumber: 2995
        Integer scan = null;
        String run = "";
        int charge = 0;
        s.setScanTitle(Title.substring(6));

        Matcher m = null;
        String RE_NOT_FOUND = null;
        if (RE_USER_SUPPLIED_RUN_NAME != null && RE_USER_SUPPLIED_SCAN_NUMBER != null) {
            m = RE_USER_SUPPLIED_RUN_NAME.matcher(Title);
            if (m.matches()) {
                run = m.group(1);
                m = RE_USER_SUPPLIED_SCAN_NUMBER.matcher(Title);
                if (m.matches()) {
                    scan = Integer.parseInt(m.group(1));
                } else {
                    RE_NOT_FOUND  = "Scan";
                }
            } else {
                    RE_NOT_FOUND  = "Run";
            }
        }

        if (m == null || !m.matches()) {
            for (int i = 0; i <RE_TITLE_TO_RUN.length; i++) {
                Pattern prun = RE_TITLE_TO_RUN[i];
                Pattern pscan = RE_TITLE_TO_SCAN[i]; 
                m = prun.matcher(Title);
                Matcher mscan = pscan.matcher(Title);
                if (m.matches() && mscan.matches()) {
                    run=m.group(1);
                    scan=Integer.valueOf(mscan.group(1));
                    if (RE_TITLE_TO_CHARGE[i] != null) {
                        Matcher mcharge = RE_TITLE_TO_CHARGE[i].matcher(Title);
                        if (mcharge.matches() && mcharge.group(1)!= null) {
                            charge = Integer.parseInt(m.group(1));
                        }
                    }
                    if (RE_TITLE_TO_RETENTION[i] != null) {
                        Matcher mrt = RE_TITLE_TO_RETENTION[i].matcher(Title);
                        if (mrt.matches()) {
                            if (mrt.group(1)!= null)
                                s.setElutionTimeStart(Double.parseDouble(mrt.group(1)));
                            if (mrt.groupCount()>1 && mrt.group(2)!= null)
                                s.setElutionTimeEnd(Double.parseDouble(mrt.group(2)));
                        }
                    }
                }
                
            }
        }
        
        if (!m.matches()) {

            String titleText = Title.trim();
    //        double elutionFrom = s.getElutionTimeStart();
    //        double elutionTo = s.getElutionTimeEnd();
            if (titleText.startsWith("TITLE=")) {
                titleText = Title.substring(6);
            }

            //        String[] ta = Title.split("(: | )");
            if (!Title.toLowerCase().contains(" finneganscannumber:") && !titel_error_shown) {
                titel_error_shown = true;
                String message = "Can't parse TITLE tag: \"" + Title +"\"\nSource: \"" 
                        + m_source + "\nline: "
                        + m_currentLine + "\nWon't be able to get scan_number and run name";
                if (RE_NOT_FOUND != null) {
                    message += "\nREGULARE EXPRESSION FAILED for "+ RE_NOT_FOUND;
                }
                Logger.getLogger(this.getClass().getSimpleName()).log(Level.SEVERE,message);
                //throw new ParseException(message,m_currentLine);               
            } else {
                String[] preScanNumber = titleText.split("(?i) finneganscannumber:\\s*");
                preScanNumber = preScanNumber[1].split("\\s+");
                scan = Integer.valueOf(preScanNumber[0]);
            }
                
            String[] preRun = titleText.split("(?i)rawfile:\\s*");
            if (preRun.length >1) {
                preRun = preRun[1].split("\\s*[^\\s]*:");
                run = preRun[0];
            } else {
                preRun = titleText.split("(?i)period:\\s*");
                if (preRun.length > 1) {
                    preRun = preRun[1].split("\\s*[^\\s]*:");
                    run = preRun[0];
                } else if (!titel_error_shown) {
                    String message = "Can't parse run from TITLE tag: \"" + Title +"\"\nSource: \"" 
                            + m_source + "\nline: "
                            + m_currentLine + "\nWon't be able to get scan_number and run name";
                    if (RE_NOT_FOUND != null) {
                        message += "\nREGULARE EXPRESSION FAILED for "+ RE_NOT_FOUND;
                    }
                    Logger.getLogger(this.getClass().getSimpleName()).log(Level.SEVERE,message);
                }
            }

            String[] prePrecintensity = titleText.split("(?i)precintensity:\\s*");
            if (prePrecintensity.length >1) {
                prePrecintensity = prePrecintensity[1].split("\\s*[^\\s]*:");
                s.setPrecurserIntensity(Double.parseDouble(prePrecintensity[0]));
            }
            String[] preElution = titleText.split("(?i)elution from:\\s*");
            if (preElution.length >1) {
                preElution = preElution[1].split("\\s*[^\\s]*:");
                preElution = preElution[0].split("(?i)\\s*to\\s*");
                s.setElutionTimeStart(Double.parseDouble(preElution[0]));
                if (preElution.length >1)
                    s.setElutionTimeEnd(Double.parseDouble(preElution[1]));
            }


        }
        run = run.replace("\\.[a-zA-Z_]*$", "");
        s.setRun(run);
        s.setScanNumber(scan);
        if (charge != 0)
            s.setPrecurserCharge(charge);

        //        }

    }


    /**
     * reads in one spectra from the file, and returns one spectra for each
     * "recognised" charge state (e.g. CHARGE=2+ and 3+)
     * @return list of spectra (only difference is the charge state)
     * @throws java.text.ParseException
     * @throws java.io.IOException
     */
    protected ArrayList<Spectra> readScan() throws ParseException, IOException{
        ArrayList<Spectra> ret = new ArrayList<Spectra>(1);

        ArrayList<Integer> matchedPeptides = new ArrayList<Integer>();
        String line;
        Spectra s = null;
        String[] chargeStates = null;
        Matcher m;

        m_currentLine++;
        line = m_input.readLine();
        boolean hasTitle=false;
        while (line != null) {
            if (line.startsWith("BEGIN IONS")) {
                //s = Spectra.getSpectra(); // we read a new spectra
                s = new Spectra(); // we read a new spectra
                s.setTolearance(m_ToleranceUnit);
                s.setSource(m_source);
            } else if (line.startsWith("END IONS")) { // finished with this spectra
                if (!hasTitle) {
                    ParseException e = new ParseException("found spectrum without a title tag - this would lead to trouble",m_currentLine);
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,"found spectrum without a title tag - this would lead to trouble", e);
                    throw e;
                }
                hasTitle=false;
                if (chargeStates==null || chargeStates.length > 1 || (chargeStates.length == 1 && chargeStates[0].trim().length() == 0  )) {
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

            } else if (line.startsWith("PEPMASS=")) { // is actually m/z
                Matcher match = RE_MASCOT_PREC_ENTRY.matcher(line);
                if (match.matches() ) {
                    s.setPrecurserMZ(Double.parseDouble(match.group(1)));
                    s.setPrecurserIntensity(Double.parseDouble(match.group(2)));
                } else
                    s.setPrecurserMZ(Double.parseDouble(line.substring(line.indexOf("=")+1)));
            } else if (line.startsWith("XLPEPMASSES=")) { // m/z candidate values for individual peptides
                s.setPeptideCandidateMasses(line.substring(line.indexOf("=")+1));
            } else if (line.startsWith("TITLE=")) { // is actually m/z
                parseTitle(line, s);
                hasTitle=true;
            } else if (line.startsWith("CHARGE=")) { // charge state(s)


                chargeStates = line.substring(line.indexOf("=")+1).split("( and | or )");

            } else if (line.startsWith("ADDITIONALCHARGES=")) { // charge state(s)
                HashSet<Integer> addChargeStates=new HashSet<Integer>();
                
                for (String sCharge : line.substring(line.indexOf("=")+1).split("( and | or |;)")) {
                    sCharge=sCharge.trim();
                    if (!sCharge.isEmpty()) {
                        addChargeStates.add(Integer.parseInt(sCharge));
                    }
                }
                s.setAdditionalCharge(addChargeStates);

            } else if (line.startsWith("ADDITIONALMZ=")) { // charge state(s)


                HashSet<Double> addMZ=new HashSet<Double>();
                
                for (String sMZ : line.substring(line.indexOf("=")+1).split("( and | or |;)")) {
                    sMZ=sMZ.trim();
                    if (!sMZ.isEmpty()) {
                        addMZ.add(Double.parseDouble(sMZ));
                    }
                }
                s.setAdditionalMZ(addMZ);

            } else if (line.startsWith("SCANS=")) { // Scans for this spectrum

                String[] scans = line.split("(=| |,|;|\\-)");
                s.setScanNumber(Integer.parseInt(scans[1]));

            } else if (line.startsWith("PEPTIDEMATCHES=")) { // charge state(s)
                String[] matches = line.substring(15).toLowerCase().split(":?matchgroup:");
                for (String match : matches) if (match.length() >0){
                    s.addPreliminaryMatch(new PreliminaryMatch(getSequences(), match));
                }
            } else if (line.startsWith("RTINSECONDS=")) { // charge state(s)
                Matcher rtm = RTINSECOND_PAIR.matcher(line);
                if (rtm.matches())  {
                    s.setElutionTimeStart(Double.parseDouble(rtm.group(1)));
                    if (rtm.group(2) != null) {
                        s.setElutionTimeEnd(Double.parseDouble(rtm.group(2)));
                    }
                } else
                    s.setElutionTimeStart(Double.parseDouble(line.substring(12)));
            } else if ((m = RE_PEAK_ENTRY.matcher(line)).matches()) {
                s.addPeak(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)));
            } // else ignore
            line = m_input.readLine();
        }
        return ret;
        
    }


    @Override
    public void gatherData() throws FileNotFoundException, IOException {
        gatherDataRE();
    }



    /**
     * if the input file was a file, then it gathers some information about it
     * like maximal precursor mass, number of entries and number of returnable spectra
     * @throws FileNotFoundException
     */
    public void gatherDataOld() throws FileNotFoundException {
        long nanostart=System.nanoTime();
        if (m_inputFile == null)
            throw new UnsupportedOperationException("Can't pre gather statistics on non-file based inputs");
//        if (m_inputFile == null)
//            return;

        double maxUnknowChargeStates = m_UnknowChargeStates[m_UnknowChargeStates.length - 1];
        double unknownChargeSTateCount = m_UnknowChargeStates.length;
        
        
        BufferedReader input = null;
        
        try {
            input = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(m_inputFile))));
        } catch (IOException ex) {
            input = new BufferedReader(new InputStreamReader(new FileInputStream(m_inputFile)));
        }
        

        String line;
        int entries = 0;
        int spectra = 0;
        double maxmass = 0;
        Pattern linepattern = Pattern.compile("^(CHARGE=|PEPMASS=|END IONS).*");

        double mass = 0;
        String charge =  "   ";
        try {
            while ((line = input.readLine()) != null) {
                if (linepattern.matcher(line).matches()) {
                    if (line.startsWith("PEPMASS=")) {
                        // is actually m/z
                        // System.err.println(">>>>>>>>>>>>>>>>>> " + line.substring(8));
                        Matcher match = RE_MASCOT_PREC_ENTRY.matcher(line);
                        if (match.matches() ) {
                            mass = Double.parseDouble(match.group(1));
                        } else
                            mass = Double.parseDouble(line.substring(8));
                    } else if (line.startsWith("CHARGE=")){
                        charge = line.substring(7).trim();
                    } else {
                        entries ++;
                        
                        if (charge.length() > 2) { // unsure charge state
                            double cMass = (mass - Util.PROTON_MASS) * maxUnknowChargeStates + Util.PROTON_MASS;
                            if (cMass > maxmass)
                                 maxmass = cMass;
                            spectra ++;
                        } else  {
                            double chargeValue = Double.parseDouble(charge.substring(0,1));
                            if (chargeValue >= m_MinChargeState) {
                                double cMass = (mass - Util.PROTON_MASS) * chargeValue + Util.PROTON_MASS;
                                if (cMass > maxmass)
                                     maxmass = cMass;
                                spectra ++;
                            }
                    }

                    }
                }

            }
            m_MaxPrecursorMass = maxmass;
            m_scanCount = entries;
        } catch (IOException ex) {
            Logger.getLogger(MSMIterator.class.getName()).log(Level.SEVERE, null, ex);
        }
        long nanoend=System.nanoTime();
        Logger.getLogger(this.getClass().getName()).log(Level.INFO,"time spend in gathering data:" + (nanoend - nanostart));



    }

    /**
     * if the input file was a file, then it gathers some information about it
     * like maximal precursor mass, number of entries and number of returnable spectra
     * @throws FileNotFoundException
     */
    public void gatherDataRE() throws FileNotFoundException, IOException {
        long nanostart=System.nanoTime();
        if (m_inputFile == null)
            throw new UnsupportedOperationException("Can't pre gather statistics on non-file based inputs");
//        if (m_inputFile == null)
//            return;

        double maxUnknowChargeStates = m_UnknowChargeStates[m_UnknowChargeStates.length - 1];
        double unknownChargeSTateCount = m_UnknowChargeStates.length;
        
        
        BufferedReader input = null;
        
        try {
            input = new BufferedReader(new InputStreamReader(new GZIPInputStream(new RobustFileInputStream(m_inputFile))));
        } catch (IOException ex) {
            input = new BufferedReader(new InputStreamReader(new RobustFileInputStream(m_inputFile)));
        }
        
//        input = new BufferedReader(new InputStreamReader(new FileInputStream(m_inputFile)));

        String line;
        int entries = 0;
        int spectra = 0;
        double maxmass = 0;
        Pattern linepattern = Pattern.compile("^(CHARGE=(.*)|PEPMASS=(.*)|END IONS).*");

        double mass = 0;
        String charge =  "   ";
        try {
            while ((line = input.readLine()) != null) {
                Matcher m = linepattern.matcher(line);
                if (m.matches()) {
                    if (m.group(3) !=null) {
                        // is actually m/z
                        // System.err.println(">>>>>>>>>>>>>>>>>> " + line.substring(8));
                        mass = Double.parseDouble(m.group(3).split(" ")[0]);
                    } else if (m.group(2) !=null){
                        charge = m.group(2);
                    } else {
                        entries ++;
                        
                        if (charge.length() > 2) { // unsure charge state
                            double cMass = (mass - Util.PROTON_MASS) * maxUnknowChargeStates + Util.PROTON_MASS;
                            if (cMass > maxmass)
                                 maxmass = cMass;
                            spectra ++;
                        } else  {
                            double chargeValue = Double.parseDouble(charge.substring(0,1));
                            if (chargeValue >= m_MinChargeState) {
                                double cMass = (mass - Util.PROTON_MASS) * chargeValue + Util.PROTON_MASS;
                                if (cMass > maxmass)
                                     maxmass = cMass;
                                spectra ++;
                            }
                    }

                    }
                }

            }
            m_MaxPrecursorMass = maxmass;
            m_scanCount = entries;
//            m_countSpectra = spectra;
        } catch (IOException ex) {
            Logger.getLogger(MSMIterator.class.getName()).log(Level.SEVERE, null, ex);
        }
        long nanoend=System.nanoTime();
        Logger.getLogger(this.getClass().getName()).log(Level.INFO,"time spend in gathering data:" + (nanoend - nanostart));
    }
    
    @Override
    public int countReadSpectra() {
        return m_countReadSpectra;
    }

 

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
    public void restart() throws IOException {
        if (canRestart()) {
            m_nextID  = 0;
            close();
            m_current = null;
            m_next.clear();
            try {
                inputFromFile(m_inputFile);
            } catch (FileNotFoundException|ParseException ex) {
                String message = "Error while trying to reopen the input-file: " + m_inputFile;
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, message, ex);
                m_config.getStatusInterface().setStatus("Error restarting peaklist" + ex);
                throw new RuntimeException(message, ex);
            }
        }
    }



    /**
     * @return the m_nextID
     */
    public int getNextID() {
        return m_nextID;
    }

    /**
     * @param m_nextID the m_nextID to set
     */
    public void setNextID(int m_nextID) {
        this.m_nextID = m_nextID;
    }



}
