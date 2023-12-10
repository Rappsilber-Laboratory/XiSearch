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
package rappsilber.applications;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import rappsilber.config.RunConfigFile;
import rappsilber.gui.SimpleXiGui;
import rappsilber.gui.components.DebugFrame;
import rappsilber.ms.dataAccess.msm.AbstractMSMAccess;
import rappsilber.ms.dataAccess.msm.MSMListIterator;
import rappsilber.ms.dataAccess.output.CSVExportMatches;
import rappsilber.ms.dataAccess.output.PeakListWriter;
import rappsilber.ms.dataAccess.output.ResultMultiplexer;
import rappsilber.ms.sequence.AminoModification;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ui.MemMapStatusControl;
import rappsilber.ui.StatusInterface;
import rappsilber.utils.ObjectWrapper;
import rappsilber.utils.Util;
import rappsilber.utils.XiProvider;
import rappsilber.utils.XiVersion;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class Xi {
    /**
     * the list of mgf/apl files to search
     */
    ArrayList<String> peaklistArgs = new ArrayList<>();
    
    /**
     * the fastaArgs-files to search
     */
    ArrayList<String> fastaArgs = new ArrayList<>();
    
    /**
     * The config file used to define parameters
     */
    ArrayList<String> configArgs = new ArrayList<>();

    /**
     * The output file where to write the results
     */
    ArrayList<String> outputArgs = new ArrayList<>();


    /**
     * Where to write an annotated peaks
     */
    ArrayList<String> annotatedPeaksOut = new ArrayList<>();
    
    /**
     * if --help is called then this will be set to true
     */
    boolean showHelp = false;
    /**
     * The extra xi parameters additionally to the configArgs
     */
    ArrayList<String> xiArgs = new ArrayList<>();
    
    /**
     * access to the peaklists
     */
    AbstractMSMAccess peaklist;
    
    /**
     * access tothe sequence files
     */
    SequenceList sequences;
    
    /**
     * The xi configArgs 
     */
    RunConfigFile xiconfig;
    
    /**
     * forward the arguments to the gui and start via gui
     */
    boolean useGui = false;

    boolean useDBGui = false;
    
    /**
     * show a window with status and logging information
     */
    boolean displayLog = false;
    
    private DebugFrame debugGui;
    private ObjectWrapper<String> locale = new ObjectWrapper<>("en");
    private ObjectWrapper<String> statusexchange = new ObjectWrapper<>((String)null);
    
    XiProcess m_xi_process;
    /**
     * the joined output
     */
    private ResultMultiplexer result_multiplexer = new ResultMultiplexer();

    
    public void printusage() {
        System.out.println(""
                + "java -cp Xlink.jar rappsilber.applications.Xi --config=[config-file] --peaks=[path to peaklist] --fasta=[path to fasta file] --conf=[some xi-parameters] --output=[csv-file] --peaksout=[csv-file] --exampleconfig=[path] --help \n"
                + "--config     a config file to read in \n"
                + "             can be repeated - later options add to or \n"
                + "             overwrite previous options\n"
                + "--peaks      peaklist to read \n"
                + "             .apl or .mgf are accepted or zipped versions\n"
                + "             can be repeated\n"
                + "--fasta      a fasta file against wich the peaklists are \n"
                + "             searched\n"
                + "             can be repeated\n"
                + "--output     where to write the csv-output\n"
                + "             - will output to stdout\n"
                + "             can be repeated\n"
                + "--xiconf     add an additional option to the config\n"
                + "--exampleconfig  writes out an example config and exits\n"
                + "--log        displays a logging window\n"
                + "--help       shows this message\n"
                + "--gui        forwards the arguments to the xi-gui\n"
                + "--dbgui      opens the database bound gui\n"
                + "--peaksout   write out annotated peaks\n"
                + "--locale     what local to use for writingh out numbers\n"
                + "--version    display version\n"
                + "--versiongui display version in a window\n"
                + "--changes    display change log\n"
                + "If no arguments are gvien the GUI is shown.");
    }
    
    
    public void writeDefaultConfig (String path) throws IOException {
        
        PrintStream out = null;
        if (path.contentEquals("-")) {
            out = System.out;
        }else {
            out = new PrintStream(path);
        }
        
        BufferedReader br = Util.readFromClassPath(".rappsilber.data.DefaultConfig.conf");
        String line = null;
        
        while ((line = br.readLine()) != null)
            out.append(line + "\n");
        
        br.close();
        out.close();
        System.exit(0);
    }    

    public int parseArgs(String[] args, ArrayList<String> unknownArgs) {
        HashMap<String,Object> argnames = new HashMap<>();
        int parsedArgs = 0;
        argnames.put("--config", configArgs);
        argnames.put("--peaks", peaklistArgs);
        argnames.put("--fasta", fastaArgs);
        argnames.put("--xiconf", xiArgs);
        argnames.put("--output",outputArgs);
        argnames.put("--peaksout",annotatedPeaksOut);
        argnames.put("--locale",locale);
        argnames.put("--statusexchange", statusexchange);
        if (args.length == 0) 
            useGui = true;
        
        for(String arg : args) {
            if (arg.contentEquals("--changes")) {
                XiVersion.main(new String[]{""});
                System.exit(0);
                parsedArgs++;
            } else if (arg.contentEquals("--version")) {
                XiVersion.main(new String[]{"-v"});
                System.exit(0);
                parsedArgs++;
            } else if (arg.contentEquals("--versiongui")) {
                XiVersion.main(new String[]{"--gui"});
                System.exit(0);
                parsedArgs++;
            } else if (arg.contentEquals("--help")) {
                showHelp = true;
                parsedArgs++;
            } else if (arg.contentEquals("--log")) {
                displayLog = true;
                parsedArgs++;
            } else if (arg.contentEquals("--gui")) {
                useGui = true;
                parsedArgs++;
            } else if (arg.contentEquals("--dbgui")) {
                useDBGui = true;
                parsedArgs++;
            } else if (arg.startsWith("--exampleconfig=")) {
                try {
                    writeDefaultConfig(arg.substring("--exampleconfig=".length()));
                } catch (IOException ex) {
                    Logger.getLogger(Xi.class.getName()).log(Level.SEVERE, null, ex);
                }
                parsedArgs++;
            } else if (arg.contentEquals("--exampleconfig")) {
                try {
                    writeDefaultConfig("-");
                } catch (IOException ex) {
                    Logger.getLogger(Xi.class.getName()).log(Level.SEVERE, null, ex);
                }
                parsedArgs++;
                
            } else {
                String[] argParts = arg.split("=", 2);
                Object argOption = argnames.get(argParts[0]);
                if (argOption == null) {
                    unknownArgs.add(arg);
                } else {
                    if (argOption instanceof ArrayList)
                        ((ArrayList)argOption).add(argParts[1]);
                    if (argOption instanceof ObjectWrapper) {
                        ((ObjectWrapper)argOption).value = argParts[1];
                    }
                    
                    parsedArgs++;
                }
            }
        }
        return parsedArgs;
    }
    
    public void setupMSMIterator() {
        // Setup the reader for MSM files
        try {
            int minCharge =  this.xiconfig.retrieveObject("MINCHARGE",1);

            String currentpath = new File(".").getAbsolutePath();
            peaklist = new MSMListIterator(peaklistArgs, currentpath, xiconfig.getFragmentTolerance(), minCharge, xiconfig);
            

            // enables filtering by max peptide mass in db
            //m_db_msm.gatherData();
            int cpus = xiconfig.getPreSearchThreads();

            
            String message = "detect maximum precursor mass ("  + cpus +")";
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, message);
            xiconfig.getStatusInterface().setStatus(message);
            peaklist.gatherData(cpus);
            //m_db_msm.gatherData();
            
            message = "Maximum mass : " + peaklist.getMaxPrecursorMass();
            System.err.println(message);
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, message);

            
        } catch (IOException|ParseException ex) {
//             System.err.println("XiDB: problem when setting up MSMIterator: " + ex.getMessage());
//             m_connectionPool.closeAllConnections();
//             System.exit(1);
            String message = this.getClass().getSimpleName() + ": problem when setting up peaklist: " + ex.getMessage();

            xiconfig.getStatusInterface().setStatus(message);
            ex.printStackTrace();
            System.exit(0);
        }
        
    }
    
    public void setupOutput() {
        for (String out : outputArgs) {
            try {
                boolean gzip = false;
                boolean tabSep=false;
                if (out.endsWith(".gz")) {
                    gzip=true;
                }
                if (out.endsWith("txt.gz")||out.endsWith("tsv.gz")||out.endsWith("txt")||out.endsWith("tsv")) {
                    tabSep=true;
                }
                
                CSVExportMatches CSVOut = null;
                if (out.contentEquals("-")) {
                    CSVOut = new CSVExportMatches(System.out, xiconfig,gzip);
                } else {
                    CSVOut = new CSVExportMatches(new FileOutputStream(out), xiconfig,gzip);
                }
                if (tabSep) {
                    CSVOut.setDelimChar("\t");
                }
                if (locale.value != null && !CSVOut.setLocale(locale.value)) {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "could not set the nu,bner locale: " + locale.value);
                    System.exit(1);
                }
                result_multiplexer.addResultWriter(CSVOut);
            } catch (IOException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "could not open ouput file:" + out, ex);
                System.exit(1);
            }
        }
        
        for (String out : annotatedPeaksOut) {
            if (out.contentEquals("-")) {
                PeakListWriter plw = new PeakListWriter(System.out);
                plw.setLocale(locale.value);
                result_multiplexer.addResultWriter(plw);
            } else {
                try {
                    OutputStream op = new FileOutputStream(out);
                    if (out.endsWith(".gz")) {
                        try {
                            op = new GZIPOutputStream(op);
                        } catch (IOException ex) {
                            Logger.getLogger(Xi.class.getName()).log(Level.SEVERE, "Error seting up compressed output", ex);
                        }
                    }
                    PeakListWriter plw = new PeakListWriter(op);
                    plw.setLocale(locale.value);
                    result_multiplexer.addResultWriter(plw);
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "could not open ouput file:" + out, ex);
                }
            }
        }

    }
    
    
    public void startXi() throws IOException, FileNotFoundException, ParseException {

        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Xi Version: {0}", XiVersion.getVersionString());
        
        xiconfig = new RunConfigFile();
        if (debugGui != null) {
            xiconfig.addStatusInterface(debugGui);
        }
        
        xiconfig.addStatusInterface(new StatusInterface() {
            String status="";
            @Override
            public void setStatus(String status) {
                if (!this.status.equals(status)) {
                    this.status  = status.replaceAll("\\{%o%\\}", this.status);;
                    System.err.println(status);
                }
            }

            @Override
            public String getStatus() {
                return status;
            }
        });
        if (statusexchange.value != null) {
            System.err.println("writing status to : " + statusexchange.value);
            MemMapStatusControl mmsc = new MemMapStatusControl(new File(statusexchange.value));
            xiconfig.addStatusInterface(mmsc);
            mmsc.autoWriteMemInfo();
            
        }
        //xiconfig.addStatusInterface(debugGui);
        
        for (String conf : configArgs) {
            xiconfig.ReadConfig(new FileReader(conf));
        }
        
        for (String conf : xiArgs) {
            try {
                xiconfig.evaluateConfigLine("custom:"+conf);
            } catch (ParseException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Extra configuration contained error:", ex);
                return;
            }
        }
        
        setupMSMIterator();
        
        File[] fastas = new File[fastaArgs.size()];
        int i = 0;
        for (String f : fastaArgs) {
            fastas[i++] = new File(f);
        }
        
        sequences = new SequenceList(fastas, xiconfig);
        
        setupOutput();
        
        
        m_xi_process = XiProvider.getXiSearch(sequences, peaklist, result_multiplexer, null, xiconfig, SimpleXiProcessMultipleCandidates.class);
        System.out.println("Xi - flavor:" + m_xi_process.getClass().getName());
        
        m_xi_process.prepareSearch();
        m_xi_process.startSearch();
        m_xi_process.waitEnd();
        result_multiplexer.finished();
    }
    
    public static void main(String[] args) throws IOException, FileNotFoundException, ParseException {
        ArrayList<String> unknownArgs = new ArrayList<>();
        final Xi xi = new Xi();
        DebugFrame df = null;
        int argsCount = xi.parseArgs(args, unknownArgs);
        
        // switch to automatically change modification symbols to lower case
        rappsilber.utils.Util.AutoCaseSymbols=true;
        
        if (xi.displayLog) {
            df = new DebugFrame("Xi-Version : " + XiVersion.getVersionString(), xi.m_xi_process);
            final DebugFrame mdf = df;
            xi.debugGui=df;
            
            System.err.println("Showing debug window!");
            new Thread(new Runnable() {

                public void run() {
                    
                    mdf.setVisible(true);
                }
            }).start();
        }
        
        if (unknownArgs.size()>0) {
            for (String ua : unknownArgs) {
                System.err.println("Unknown argument:" + ua);
            }
            xi.printusage();
            System.exit(-1);
        }

        if (xi.showHelp) {
            xi.printusage();
            System.exit(0);
        } 
        
        if(args.length == 0) {
            rappsilber.gui.SimpleXiGui.main(args);
            return;
        }
        
        if (xi.useGui) {
            java.awt.EventQueue.invokeLater(new Runnable() {
                Xi lxi= xi;
                @Override
                public void run() {
                    SimpleXiGui sxi = new SimpleXiGui();
                    for (String pl : xi.peaklistArgs)
                        sxi.addMSMFile(pl);
                    for (String f : xi.fastaArgs)
                        sxi.addFastaFile(f);
                    if (xi.configArgs.size()>0) {
                        sxi.setConfigFile(xi.configArgs.get(0));
                        for (int i = 1 ; i< xi.configArgs.size();i++)
                            sxi.addConfigFile(xi.configArgs.get(i));
                    }
                    for (String c : xi.xiArgs)  {
                        sxi.appendConfigLine(c);
                    }
                    
                    if (xi.outputArgs.size() > 0)
                        sxi.setResultFile(xi.outputArgs.get(0));

                    sxi.setVisible(true);
                }
            });

            return;
        }
        
        if (xi.useDBGui) {
            rappsilber.gui.XiDBStarterExt.main(new String[0]);
            return;
        }
        
        if (xi.peaklistArgs.size() == 0) {
            Logger.getLogger(xi.getClass().getName()).log(Level.SEVERE,"No Peaklist given");
            System.exit(-1);
        }

        if (xi.fastaArgs.size() == 0) {
            Logger.getLogger(xi.getClass().getName()).log(Level.SEVERE,"No FASTA file given");
            System.exit(-1);
        }

        if (xi.configArgs.size() == 0) {
            Logger.getLogger(xi.getClass().getName()).log(Level.SEVERE,"No config-file given");
            System.exit(-1);
        }

        if (xi.outputArgs.size() == 0) {
            Logger.getLogger(xi.getClass().getName()).log(Level.WARNING,"No config-file given - will write to stdout");
            xi.outputArgs.add("-");
        }

        final long watchdoginterval = 60000;
        TimerTask watchdogTask = new TimerTask() {
            List<GarbageCollectorMXBean> list = ManagementFactory.getGarbageCollectorMXBeans();
            long gctime;

            int maxCountDown=30;
            {
                for (GarbageCollectorMXBean bean : list) {
                    gctime += bean.getCollectionTime();
                }
            }
            @Override
            public void run() {
                try {
                    long newGCTime = 0;
                    for (GarbageCollectorMXBean bean : list) {
                        newGCTime += bean.getCollectionTime();
                    }
                    long deltgc = newGCTime-gctime;

                    gctime = newGCTime;
                    
                    if (deltgc > watchdoginterval*0.5) {
                        Logger.getLogger(Xi.class.getName()).log(Level.INFO, "gctime: " + Math.round(deltgc/1000) +"second of the last "+  
                                Math.round(watchdoginterval/1000) +" -> " + Math.round(deltgc/watchdoginterval*100) +" %");                        
                        if (deltgc > watchdoginterval*0.9) {
                            Logger.getLogger(Xi.class.getClass().getName()).log(Level.SEVERE, 
                                    "\n============================================================================\n" +
                                    "===== long time spend in garbage collection probably runing out of memory ====\n");
                        }
                    }
                } catch (Exception e) {
                    Logger.getLogger(this.getClass().getName()).log(Level.WARNING,"Error im watchdog : ", e);
                }
            }

            /**
             * starts the ping in its own thread so as not to interfere with the watchdog
             */
        };
        Timer watchdog = new Timer("GCWatchdog", true);
        
        watchdog.scheduleAtFixedRate(watchdogTask, 10, watchdoginterval);
        
        try {
            xi.startXi();
        } catch(Exception ex) {
            Logger.getLogger(xi.getClass().getName()).log(Level.SEVERE,"ERROR: ",ex);
        }
        
        if (xi.displayLog) {
            df.setVisible(false);
            df.dispose();
        }
        
    }
    
}
