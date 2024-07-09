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
import rappsilber.config.AbstractRunConfig;
import rappsilber.config.RunConfig;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.dataAccess.msm.AbstractMSMAccess;
import rappsilber.ms.dataAccess.output.MSMWriter;
import rappsilber.ms.spectra.Spectra;


/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class ScanFilteredSpectrumAccess extends AbstractSpectraFilter{
    public enum MatchType {
        run_scan,
        peakfile_index;
    }

//    HashMap<String,HashSet<Integer>> m_SelectedRunScans = new HashMap<String, HashSet<Integer>>();
    HashMap<String,HashMap<Integer, String>> m_SelectedRunScans = new HashMap<String, HashMap<Integer,String>>();
    private int m_countScans = 0;
    private MatchType m_match_what = MatchType.run_scan;
    private boolean m_whiteList = true;
    private String m_extraheader = "";
    /**
     * if this is true then the filter will claim it has seen every spectra ones we have passed as many spectra as where whitelisted
     */
    private boolean m_assumeUnique = true;
//    int m_readSpectra = 0;
//
////    SpectraAccess m_reader;
//
//    Spectra m_current = null;
//    Spectra m_next = null;
//
    public ScanFilteredSpectrumAccess(RunConfig conf) {
    }

    
    public ScanFilteredSpectrumAccess(RunConfig conf,String settings) {
        String[] set = settings.split(";");
        
        for (String s: set) {
            
            String[] args = s.split(":");
            String r = args[0].trim();
            String a = r.toLowerCase();
            if (a.contentEquals("whitelist")) {
                if (args.length == 1) {
                    m_whiteList  = true;
                } else {
                    m_whiteList=AbstractRunConfig.getBoolean(args[1].trim().toLowerCase(), m_whiteList);
                }
            } else if (a.contentEquals("blacklist")) {
                if (args.length == 1) {
                    m_whiteList  = false;
                } else {
                    m_whiteList=!AbstractRunConfig.getBoolean(args[1].trim().toLowerCase(), !m_whiteList);
                }
            } else  if (args.length == 1 || args[1].trim().contentEquals("*")){
                this.SelectScan(r, null);
            } else {
                String scans = args[1].toLowerCase().trim();
                for (String scan : scans.split(",")) {
                    this.SelectScan(r, Integer.parseInt(scan));
                }
            }
        }
    }

    @Override
    public boolean hasNext() {
        if (!(m_assumeUnique && m_whiteList)) {
            return super.hasNext(); //To change body of generated methods, choose Tools | Templates.
        }
        return m_readSpectra < m_countScans && super.hasNext();
    }
    
    


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
                if (m_countScans == 0 && line.trim().length() >0) {
                    setExtraHeader(line);
                }
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


    public void SelectScan(String run, Integer scan) {
        SelectScan(run, scan, "");
    }
    public void SelectScan(String run, Integer scan, String extra) {
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
            if (scan != null) {
                scans.put(scan,extra);
            } else {
                m_assumeUnique = false;
            }
        } else {
            if (scan == null)  {
                scans.clear();
                m_assumeUnique = false;
            }
            if (!scans.isEmpty()) {
                scans.put(scan,extra);
            }
        }
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
        String r = null;
        int si = 0;
        String[] extensions = null;
        if (m_match_what == MatchType.run_scan) {
            r = s.getRun();
            si = s.getScanNumber();
            extensions = new String[]{"raw","RAW","mzml", "mzML", "Raw"};
        }  else if (m_match_what == MatchType.peakfile_index) {
            r = s.getPeakFileName();
            si = s.getReadID();
            extensions = new String[]{"mgf","apl","MGF", "APL"};
        }
        
            
        HashMap<Integer, String> scans = m_SelectedRunScans.get(r);
        if (scans == null) {
            String sn = r + ".raw";
            scans = m_SelectedRunScans.get(sn);
        }
        
        if (scans == null) {
            for (String e : extensions) {
                String sn = r + ".RAW";
                scans = m_SelectedRunScans.get(sn);
                if (scans != null) {
                    break;
                }
            }
        }
        // try without extension
        if (scans == null && r.contains(".")) {
            String sn = r.substring(0,r.lastIndexOf("."));
            scans = m_SelectedRunScans.get(sn);
        }
        
        if (scans == null) {
            r=r.toLowerCase();
            scans = m_SelectedRunScans.get(r);
            if (scans == null) {
                for (String e : extensions) {
                    String sn = r + ".RAW";
                    scans = m_SelectedRunScans.get(sn);
                    if (scans != null) {
                        break;
                    }
                }
            }
        }
        // try without extension
        if (scans == null && r.contains(".")) {
            String sn = r.substring(0,r.lastIndexOf("."));
            scans = m_SelectedRunScans.get(sn);
        }
            
        if (scans != null) {
            if (scans.isEmpty() || scans.containsKey(si)) {
                return m_whiteList;
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
                if (exclude) {
                    System.err.println("Listed scans will excluded");
                } else {
                    System.err.println("Listed scans will included");
                }                    
            } else {
                System.err.println("(DEFAULT) Listed scans will excluded");                
            }
            ScanFilteredSpectrumAccess sfsa = new ScanFilteredSpectrumAccess(!exclude);
            Pattern p = Pattern.compile("^\\s*(?:\\\")?([^\",]*)(?:\\\")?\\s*,\\s*(?:\\\")?([0-9]+|\\*)(?:.0)?(?:\\\")?\\s*(?:,.*)?$");
            br = new BufferedReader(new FileReader(targetScanFile));
            String line;
            while ((line = br.readLine()) != null) {
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    String[] data = line.split(",",3);
                    Integer scan = null;
                    String sscan  =m.group(2).trim();
                    if (sscan.contentEquals("*")) { 
                        scan = null;
                    } else {
                        scan = Integer.valueOf(sscan);
                    }
                    sfsa.SelectScan(m.group(1), scan);
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

    /**
     * @return the m_match_what
     */
    public MatchType matchWhat() {
        return m_match_what;
    }

    /**
     * @param m_match_what the m_match_what to set
     */
    public void matchWhat(MatchType m_match_what) {
        this.m_match_what = m_match_what;
    }

    
    
}
