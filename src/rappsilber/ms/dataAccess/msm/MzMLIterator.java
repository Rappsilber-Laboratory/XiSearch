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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.match.PreliminaryMatch;
import rappsilber.ms.statistics.utils.UpdateableInteger;
import rappsilber.utils.Util;
import uk.ac.ebi.jmzml.model.mzml.BinaryDataArray;
import uk.ac.ebi.jmzml.model.mzml.CVParam;
import uk.ac.ebi.jmzml.model.mzml.PrecursorList;
import uk.ac.ebi.jmzml.xml.io.MzMLObjectIterator;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshaller;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class MzMLIterator extends AbstractMSMAccess {



    Spectra         m_current   = null;
    LinkedList<Spectra> m_next  = new LinkedList<Spectra>();
    File            m_inputFile = null;
    String          m_source = "";
//    String          m_inputPath = null;
    private int             m_countReadSpectra = 0;
    private int            m_nextID = 0;

    boolean         m_RunEmpty = false;
    int             m_discardedScans = 0;
    int[]           m_UnknowChargeStates = new int[]{3,4,5,6};
    int             m_defaultChargeState = 4;
    RunConfig       m_config;
    int             m_MaxChargeState = 7;
    private int             m_MinChargeState = 1;

    MzMLObjectIterator<uk.ac.ebi.jmzml.model.mzml.Spectrum> spectrumIterator;
    MzMLUnmarshaller unmarshaller;
    

    /**
     * provides a new msm-file based SpectraIterator
     * @param msmfile
     * @throws java.io.FileNotFoundException
     */

    /**
     * provides a new msm-file based SpectraIterator
     * @param msmfile
     * @throws java.io.FileNotFoundException
     */
    public MzMLIterator(File msmfile, ToleranceUnit t, int minCharge, RunConfig config) throws FileNotFoundException, ParseException, IOException  {
        this(msmfile, t, minCharge, config, 0);
    }

    /**
     * provides a new msm-file based SpectraIterator
     * @param msmfile
     * @throws java.io.FileNotFoundException
     */
    public MzMLIterator(File msmfile, ToleranceUnit t, int minCharge, RunConfig config, int firstID) throws FileNotFoundException, ParseException, IOException  {
        m_nextID = firstID;
        setToleranceUnit(t);
        m_MinChargeState = minCharge;
        m_config = config;

        m_UnknowChargeStates = new int[m_MaxChargeState - m_MinChargeState+1];
        for (int i = m_MinChargeState;i<=m_MaxChargeState; i++)
            m_UnknowChargeStates[i-m_MinChargeState] = i;

        inputFromFile(msmfile);
        
    }

    
    protected void inputFromFile(File msmfile) throws FileNotFoundException, ParseException, IOException {
        m_inputFile = msmfile;
        m_inputPath = msmfile.getAbsolutePath();
        m_source = msmfile.getName();

        unmarshaller = new MzMLUnmarshaller(msmfile);
        //Unmarshall mzML file
        spectrumIterator = unmarshaller.unmarshalCollectionFromXpath("/run/spectrumList/spectrum", uk.ac.ebi.jmzml.model.mzml.Spectrum.class);
        
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
        if (!m_next.isEmpty()) {
            m_current = m_next.getFirst();
            m_current.setSource(m_inputPath);
            m_next.removeFirst();
            if (m_next.isEmpty())
                try {
                    m_next.addAll(readScan());
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                return null;
            }
            m_countReadSpectra++;
            if (m_current.getTolearance() == null)
                m_current.setTolearance(getToleranceUnit());
            m_current.setReadID(m_nextID++);
                    
        } else
            m_current = null;
        
        
        return m_current;
    }


    

    /**
     * reads in one spectra from the file, and returns one spectra for each
     * "recognised" charge state (e.g. CHARGE=2+ and 3+)
     * @return list of spectra (only difference is the charge state)
     */
    protected ArrayList<Spectra> readScan() throws ParseException, IOException{
        ArrayList<Spectra> ret = new ArrayList<Spectra>(1);
        
        while (spectrumIterator.hasNext()) {
            uk.ac.ebi.jmzml.model.mzml.Spectrum spectrum = spectrumIterator.next();
            String mslevel = "";
            List<CVParam> specParam = spectrum.getCvParam();
            for (Iterator lCVParamIterator = specParam.iterator(); lCVParamIterator.hasNext();){
                CVParam lCVParam = (CVParam) lCVParamIterator.next();
                if (lCVParam.getAccession().equals("MS:1000511")){
                    //Retrieve the scan level e.g. 1 or 2 for MS1 or MS2
                    mslevel = lCVParam.getValue().trim();
                }
            }
            if (mslevel.contentEquals("2"))  {
                double rt = 0.0;
                List<CVParam> scanParam = spectrum.getScanList().getScan().get(0).getCvParam();
                for (Iterator lCVParamIterator = scanParam.iterator(); lCVParamIterator.hasNext();){
                    CVParam lCVParam = (CVParam) lCVParamIterator.next();
                    if (lCVParam.getAccession().equals("MS:1000016")){
                        //Locate the retention time
                        rt = Double.parseDouble(lCVParam.getValue().trim());
                        //Retrieve the first scan element and locate the CV term
                    }
                }

                // PRECURSOR INFORMATIONS
                PrecursorList plist = spectrum.getPrecursorList();
                List<CVParam> scanPrecParam =
                    plist.getPrecursor().get(0).getSelectedIonList().getSelectedIon().get(0).getCvParam();
                float parIonMz =0;
                float peakIntensity =0;
                int parCharge = 0;
                for (Iterator lCVParamIterator = scanPrecParam.iterator(); lCVParamIterator.hasNext();){
                    CVParam lCVParam = (CVParam) lCVParamIterator.next();
                    if (lCVParam.getAccession().equals("MS:1000744")){
                        parIonMz = Float.parseFloat(lCVParam.getValue().trim());
                    }
                    if (lCVParam.getAccession().equals("MS:1000041")){
                        parCharge = Integer.parseInt(lCVParam.getValue().trim());
                    }
                    if (lCVParam.getAccession().equals("MS:1000042")){
                        peakIntensity = Float.parseFloat(lCVParam.getValue().trim());
                    }
                }
                // read the peaks
                Number[] mzNumbers = null;
                Number[] intenNumbers = null;
                //... Reading mz Values ...//
                List<BinaryDataArray> bdal =
                        spectrum.getBinaryDataArrayList().getBinaryDataArray();
                for (BinaryDataArray bda:bdal){
                    List<CVParam> cvpList = bda.getCvParam();
                    for (CVParam cvp:cvpList){
                        if(cvp.getAccession().equals("MS:1000514")){
                            mzNumbers = bda.getBinaryDataAsNumberArray();
                        }
                        if(cvp.getAccession().equals("MS:1000515")){
                            intenNumbers = bda.getBinaryDataAsNumberArray();
                        }
                    }
                }
                ArrayList<SpectraPeak> peaks = new ArrayList<>(mzNumbers.length);
                for (int p = 0; p < mzNumbers.length; p++) {
                    peaks.add(new SpectraPeak(mzNumbers[p].doubleValue(), intenNumbers[p].doubleValue()));
                    
                }
                
                
                Spectra s = new Spectra(rt, peakIntensity, parIonMz, parCharge);

                s.setPeaks(peaks);
                
                s.setScanNumber(spectrum.getIndex());
                s.setRun(m_source);
                
                ret.add(s);
                return ret;
                
            }

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
        int ms2count = 0;
        double maxmass = 0;
        
        while (spectrumIterator.hasNext()) {
            uk.ac.ebi.jmzml.model.mzml.Spectrum spectrum = spectrumIterator.next();
            String mslevel = "";
            List<CVParam> specParam = spectrum.getCvParam();
            for (Iterator lCVParamIterator = specParam.iterator(); lCVParamIterator.hasNext();){
                CVParam lCVParam = (CVParam) lCVParamIterator.next();
                if (lCVParam.getAccession().equals("MS:1000511")){
                    //Retrieve the scan level e.g. 1 or 2 for MS1 or MS2
                    mslevel = lCVParam.getValue().trim();
                }
            }
            if (mslevel.contentEquals("2"))  {
                ms2count++;
                double rt = 0.0;
                List<CVParam> scanParam = spectrum.getScanList().getScan().get(0).getCvParam();
                for (Iterator lCVParamIterator = scanParam.iterator(); lCVParamIterator.hasNext();){
                    CVParam lCVParam = (CVParam) lCVParamIterator.next();
                    if (lCVParam.getAccession().equals("MS:1000016")){
                        //Locate the retention time
                        rt = Double.parseDouble(lCVParam.getValue().trim());
                        //Retrieve the first scan element and locate the CV term
                    }
                }

                // PRECURSOR INFORMATIONS
                PrecursorList plist = spectrum.getPrecursorList();
                List<CVParam> scanPrecParam =
                    plist.getPrecursor().get(0).getSelectedIonList().getSelectedIon().get(0).getCvParam();
                float parIonMz =0;
                float peakIntensity =0;
                int parCharge = 0;
                for (Iterator lCVParamIterator = scanPrecParam.iterator(); lCVParamIterator.hasNext();){
                    CVParam lCVParam = (CVParam) lCVParamIterator.next();
                    if (lCVParam.getAccession().equals("MS:1000744")){
                        parIonMz = Float.parseFloat(lCVParam.getValue().trim());
                    }
                    if (lCVParam.getAccession().equals("MS:1000041")){
                        parCharge = Integer.parseInt(lCVParam.getValue().trim());
                    }
                }
                if (parCharge !=0) {
                    double mass = Math.abs(parIonMz*parCharge);
                    if (mass > maxmass) {
                        maxmass = mass;
                    }
                }
            }
            

        }
        m_MaxPrecursorMass = maxmass;
        m_scanCount = ms2count;

    }

    @Override
    public int countReadSpectra() {
        return m_countReadSpectra;
    }

 
    
    public void close() {
        // not sure how to close that file so just hoping for the gc
        spectrumIterator = null;
        unmarshaller = null;
    }

    @Override
    public boolean canRestart() {
        return m_inputFile != null;
    }

    @Override
    public void restart() throws IOException {
        if (canRestart()) {
            spectrumIterator = unmarshaller.unmarshalCollectionFromXpath("/run/spectrumList/spectrum", uk.ac.ebi.jmzml.model.mzml.Spectrum.class);
            m_nextID  = 0;
            close();
            m_next.clear();
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
