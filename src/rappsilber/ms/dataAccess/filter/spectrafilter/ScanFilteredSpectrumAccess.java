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
package rappsilber.ms.dataAccess.filter.spectrafilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.dataAccess.msm.AbstractMSMAccess;
import rappsilber.ms.dataAccess.output.MSMWriter;
import rappsilber.ms.spectra.Spectra;


/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class ScanFilteredSpectrumAccess extends AbstractSpectraFilter{
//    HashMap<String,HashSet<Integer>> m_SelectedRunScans = new HashMap<String, HashSet<Integer>>();
    HashMap<String,HashMap<Integer, String>> m_SelectedRunScans = new HashMap<String, HashMap<Integer,String>>();
    private int m_countScans = 0;
    private boolean m_whiteList = true;
    private String m_extraheader = "";
//    int m_readSpectra = 0;
//
////    SpectraAccess m_reader;
//
//    Spectra m_current = null;
//    Spectra m_next = null;
//



    public void readFilter(File f) throws FileNotFoundException, IOException {
        readFilter(new FileInputStream(f));
    }

    public void readFilter(InputStream filter) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(filter));
        String line;
//        System.err.println("Reading filter definition");
        while ((line = br.readLine()) != null) {

            String[] parts = line.split(",");
            //System.err.println(parts[0] + " , " + parts[1]);
            
           // System.err.println(line);
            if (parts.length > 1 && parts[0].length() > 0 &&  parts[1].matches("\\s*\"?[0-9\\.]+\"?\\s*")) {
                parts[0] = parts[0].replaceAll("^\\s*\"", "").replaceAll("\"\\s*$", "");
               // System.err.println(parts[0]);
                SelectScan(parts[0], Integer.parseInt(parts[1].replaceAll("(\"|\\s)", "")), line);

            } else {
                if (m_countScans == 0 && line.trim().length() >0) 
                    setExtraHeader(line);
            }

        }

    }


    public ScanFilteredSpectrumAccess() {
        this(true);
    }
    
    public ScanFilteredSpectrumAccess(boolean whitelist) {
        m_whiteList = whitelist;
    }

//    public ScanFilteredSpectrumAccess(SpectraAccess innerAccess, boolean whitelist) {
//        this(whitelist);
//        setReader(innerAccess);
//    }
//
//    public ScanFilteredSpectrumAccess(SpectraAccess innerAccess) {
//        this(innerAccess, true);
//    }
    
//    public void setReader(SpectraAccess innerAccess){
//        m_InnerAcces = innerAccess;
//        next();
//    }


    public void SelectScan(String run, int scan) {
        SelectScan(run, scan, "");
    }
    public void SelectScan(String run, int scan, String extra) {
        m_countScans++;
        HashMap<Integer, String> scans = m_SelectedRunScans.get(run);
        if (scans == null) {
            scans = new HashMap<Integer,String>();
            m_SelectedRunScans.put(run, scans);
            if (run.contains(".")) {
                m_SelectedRunScans.put(run.substring(0,run.lastIndexOf(".")),scans);
            } else {
                m_SelectedRunScans.put(run+".raw",scans);
            }
	    String r2 = run.toLowerCase();
	    if (!r2.contentEquals(run)) {
                m_SelectedRunScans.put(r2, scans);
                if (r2.contains(".")) {
                    m_SelectedRunScans.put(r2.substring(0,r2.lastIndexOf(".")),scans);
                } else {
                    m_SelectedRunScans.put(r2+".raw",scans);
                }
	    }
        }
        scans.put(scan,extra);
    }
    
    public int scansRegistered(){
        int c =0;
        for (HashMap<Integer, String> scans : m_SelectedRunScans.values()) {
            c+=scans.size();
        }
        return c;
    }
            

    @Override
    public boolean passScan(Spectra s) {
        
        HashMap<Integer, String> scans = m_SelectedRunScans.get(s.getRun());
        if (scans == null) {
            String sn = s.getRun() + ".raw";
            scans = m_SelectedRunScans.get(sn);
        }
        if (scans == null) {
            String sn = s.getRun() + ".RAW";
            scans = m_SelectedRunScans.get(sn);
        }
        if (scans == null) {
            String sn = s.getRun().toLowerCase();
            scans = m_SelectedRunScans.get(sn);
        }
        if (scans == null) {
            String sn = s.getRun().toLowerCase() + ".raw";
            scans = m_SelectedRunScans.get(sn);
        }
//        if (scans == null) {
//            File f = new File(s.getSource());
//            String sn = f.getName();
//            scans = m_SelectedRunScans.get(sn);
//        }
        if (scans != null) {
            if (scans.containsKey(s.getScanNumber())) {
                return m_whiteList;
            }
        } else if (s.getRun().contains(".")) {
            scans = m_SelectedRunScans.get(s.getRun().substring(0,s.getRun().lastIndexOf(".")));
            if (scans != null) {
                if (scans.containsKey(s.getScanNumber())) {
                    return m_whiteList;
                }
            }
            
        }
        return !m_whiteList;
    }
    
    public String getInformation(Spectra s) {
        
        HashMap<Integer, String> scans = m_SelectedRunScans.get(s.getRun());
        if (scans == null) {
            String sn = s.getRun() + ".raw";
            scans = m_SelectedRunScans.get(sn);
        }
//        if (scans == null) {
//            File f = new File(s.getSource());
//            String sn = f.getName();
//            scans = m_SelectedRunScans.get(sn);
//        }
        if (scans != null) {
            return scans.get(s.getScanNumber());
        } else if (s.getRun().contains(".")) {
            scans = m_SelectedRunScans.get(s.getRun().substring(0,s.getRun().lastIndexOf(".")));
            if (scans != null) {
                return scans.get(s.getScanNumber());
            }
            
        }

        
        return null;
    }

    
    public int getSelectedScanCount() {
        return m_countScans;
    }
    
    
    
    public static void main(String[] args) throws ParseException  {
        if (args.length <2 ||args.length >3 ) {
            System.out.println("Usage:\n"
                    + "java -cp XiSearch.jar rappsilber.ms.dataAccess.filter.spectrafilter.AbstractSpectraFilter list_of_scans.csv peaklist [true|false]\n"
                    + "list_of_scans.csv csv file containing two columns: run,scan\n"
                    + "peaklist          a peaklist as mgf,apl,zip or list\n"
                    + "true|false        optional wether to just write out the\n"
                    + "                  listed spectra (false=default) or\n"
                    + "                  all but the listed spectra (true)"
            );
            
        }
        
        BufferedReader br = null;
        try {
            String targetScanFile = args[0];
            String sourceScanFile = args[1];
            boolean exclude = false;
            if (args.length>2) {
                exclude = args[2].trim().toLowerCase().matches("(y|yes|t|true|1|ja|j)");
                if (exclude)
                    System.err.println("Listed scans will excluded");
                else
                    System.err.println("Listed scans will included");                    
            } else {
                System.err.println("(DEFAULT) Listed scans will excluded");                
            }
            ScanFilteredSpectrumAccess sfsa = new ScanFilteredSpectrumAccess(!exclude);
            Pattern p = Pattern.compile("^\\s*(?:\\\")?([^\",]*)(?:\\\")?\\s*,\\s*(?:\\\")?([0-9]+)(?:.0)?(?:\\\")?\\s*(?:,.*)?$");
            br = new BufferedReader(new FileReader(targetScanFile));
            String line;
            while ((line = br.readLine()) != null) {
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    String[] data = line.split(",",3);
                    sfsa.SelectScan(m.group(1), Integer.parseInt(m.group(2)));
                }
            }
            AbstractMSMAccess access = AbstractMSMAccess.getMSMIterator(sourceScanFile, new ToleranceUnit(0,"da"), 0, null);
            sfsa.setReader(access);
            MSMWriter out = new MSMWriter(System.out, "", "", "");
            out.writeHeader();
            while (sfsa.hasNext()) {
                out.writeSpectra(sfsa.next());
            }
            
            out.close();
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ScanFilteredSpectrumAccess.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ScanFilteredSpectrumAccess.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(ScanFilteredSpectrumAccess.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }

    /**
     * @return the m_extraheader
     */
    public String getExtraHeader() {
        return m_extraheader;
    }

    /**
     * @param m_extraheader the m_extraheader to set
     */
    public void setExtraHeader(String m_extraheader) {
        this.m_extraheader = m_extraheader;
    }

}
