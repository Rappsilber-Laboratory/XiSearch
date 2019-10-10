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
package rappsilber.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.Locale;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import javax.swing.JOptionPane;
import javax.swing.SpinnerModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import rappsilber.applications.SimpleXiProcessLinearIncluded;
import rappsilber.applications.XiProcess;
import rappsilber.utils.XiProvider;
import rappsilber.config.LocalProperties;
import rappsilber.config.RunConfig;
import rappsilber.config.RunConfigFile;
import rappsilber.gui.components.CallBackWindow;
import rappsilber.gui.components.GenericTextPopUpMenu;
import rappsilber.gui.components.config.BasicConfig;
import rappsilber.ms.dataAccess.msm.AbstractMSMAccess;
import rappsilber.ms.dataAccess.output.CSVExportMatches;
import rappsilber.ms.dataAccess.output.ResultMultiplexer;
import rappsilber.ms.dataAccess.output.PeakListWriter;
import rappsilber.ms.dataAccess.output.ResultWriter;
import rappsilber.gui.logging.JTextAreaHandle;
import rappsilber.ms.dataAccess.StackedSpectraAccess;
import rappsilber.ms.dataAccess.filter.spectrafilter.BS3ReporterIonPeaksFilteredSpectrumAccess;
import rappsilber.ms.dataAccess.filter.spectrafilter.SetRunIDFilter;
import rappsilber.ms.dataAccess.msm.MSMListIterator;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ui.LoggingStatus;
import rappsilber.ui.StatusMultiplex;
import rappsilber.ui.TextBoxStatusInterface;
import rappsilber.utils.MyArrayUtils;
import rappsilber.utils.Util;
import rappsilber.utils.XiVersion;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class SimpleXiGui extends javax.swing.JFrame {
    private static final long serialVersionUID = -8719056362190680024L;

    JTextAreaHandle loggingHandle;
    RunXi xirunner = null;
    GenericTextPopUpMenu gtpm;
    String xiFDRPath;
    Process xifdrProcess;

    private class FDRInfo {
        boolean runXiFDR = ckFDR.isSelected();
        double psmFDR = (Double) spPsmFDR.getValue();
        double pepFDR = (Double) spPepFDR.getValue();
        double linkFDR = (Double) spLinkFDR.getValue();
        double protFDR = (Double) spProteinFDR.getValue();
        double ppiFDR = (Double) spPPIFdr.getValue();
        boolean boostFDR = ckBoost.isSelected();
        String boostLevel = cbBoost.getSelectedItem().toString();
        String inputFile = txtResultFile.getFile().getAbsolutePath();
        String xiconfig;
        File[] fastas = flFASTAFiles.getFiles();
    }


    private class WaitForXi implements Runnable {
        XiProcess m_xi;
        Component[] m_enable = null;
        Component[] m_disable = null;
        FDRInfo m_fdr;

        public WaitForXi(XiProcess xip, Component[] toEnable, Component[] toDisable, FDRInfo info) {
            m_xi = xip;
            m_enable = toEnable;
            m_disable  = toDisable;
            m_xi.prepareSearch();
            m_fdr=info;

        }

        public void run() {
            m_xi.startSearch();
            m_xi.waitEnd();
                
            if (m_fdr.runXiFDR)
                startXiFDR(m_fdr);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (Component c: m_enable)
                        c.setEnabled(true);
                    for (Component c: m_disable)
                        c.setEnabled(false);
                }
            });         
            JOptionPane.showMessageDialog(SimpleXiGui.this, "Search Finished." + (m_fdr.runXiFDR? " staring xiFDR" :""));
        }
    }

    
    
    private class XiState implements Runnable {
        AbstractMSMAccess m_input;
        ResultWriter  m_output;
        XiProcess m_xi;

        public XiState(XiProcess xip, AbstractMSMAccess input, ResultWriter  output) {
            m_xi = xip;
            m_input = input;
            m_output = output;
        }

        public void run() {
            while (!m_xi.isRunning()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    
                }
            }
            
            while (m_xi.isRunning()) {
                txtRunState.setText("Spectra read: "
                        + m_input.countReadSpectra() + " of " 
                        + m_input.getSpectraCount() // + "("
                        //+ m_input.getEntriesCount()
                        + "); writen "
                        + m_output.getResultCount());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    
                }
            }
            txtRunState.setText("Finished: Spectra read: "
                    + m_input.countReadSpectra() + " of "
                    + m_input.getSpectraCount()// + "("
                    //+ m_input.getEntriesCount()
                    + "); writen "
                    + m_output.getResultCount());
            
        }
    }

    private class RunXi implements Runnable {
        AbstractMSMAccess input;
        ResultWriter output;
        RunConfig conf;
//        SimpleXiProcessDev xi;
        XiProcess xi;
        FDRInfo fdr;

        protected RunXi(AbstractMSMAccess input, ResultWriter output, RunConfig conf, FDRInfo fdr) {
            this.input = input;
            this.output = output;
            this.conf = conf;
            this.fdr = fdr;
            //System.out.print(xi.ScoreStatistic());
        }

        public void run() {
            btnStartSearch.setEnabled(false);
            btnStartSearch1.setEnabled(false);
            btnLoadConfig.setEnabled(false);

            StackedSpectraAccess sc = null;

            if (conf.retrieveObject("enableIndicatorPeaks", false)){
                sc = new BS3ReporterIonPeaksFilteredSpectrumAccess();
                sc.setReader(input);
            }


//            StackedSpectraAccess cal = linearCalibration1.getCalibration();
//            if (cal != null) {
//                if (sc == null)
//                    cal.setReader(input);
//                else
//                    cal.setReader(sc);
//                sc = cal;
//            }
            


            try {
                if (input.getSpectraCount() <= 0) {
                    conf.getStatusInterface().setStatus("Parse peaklist");
                    input.gatherData(Math.max(conf.getSearchThreads()/2,1));
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "File not found:", ex);
                return;
            } catch (IOException ex) {
                Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, "error gathering information from peaklists:", ex);
                return;
            }

            // make sure the user stores the config alongside the result


            ResultMultiplexer result_multiplexer = new ResultMultiplexer();


            result_multiplexer.addResultWriter(output);
            try {
                conf.getStatusInterface().setStatus("Read Fasta");
                SequenceList seq = new SequenceList(conf);
                File[] fastas = flFASTAFiles.getFiles();
                for (int f = 0 ; f< fastas.length; f++) {
                    boolean d = flFASTAFiles.isSelected(f);
                    if (d)
                        seq.addFasta(fastas[f], SequenceList.DECOY_GENERATION.ISDECOY);
                    else
                        seq.addFasta(fastas[f], SequenceList.DECOY_GENERATION.ISTARGET);                        
                }
                
                xi = XiProvider.getXiSearch(seq,input, output, sc, conf, SimpleXiProcessLinearIncluded.class);
                callBackSettings1.doCallBack(seq.size());
            } catch (IOException ex) {
                Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
            }
//            xi = getXiSearch(result_multiplexer, sc, conf, SimpleXiProcessLinearIncluded.class);


            //

            xi.addFilter(new SetRunIDFilter());
            conf.getStatusInterface().setStatus("start search");
            new Thread(new WaitForXi(xi, new Component[]{btnStartSearch, btnStartSearch1, btnLoadConfig}, new Component[]{btnStop, btnStop1},  fdr)).start();

        }


        public void stop() {
            xi.stop();
        }



    }

    private class StreamToLog implements Runnable{
        BufferedReader in;

        public StreamToLog(BufferedReader in) {
            this.in = in;
        }
        
        @Override
        public void run() {
            String line;
            try {
                while ((line = in.readLine()) != null) {
                    final String fl = line;
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            txtFDRLog.append(fl +"\n");
                        }
                    });
                }
            } catch (IOException ex) {
                Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    /** Creates new form SimpleXiGui */
    public SimpleXiGui() {
        initComponents();
        this.setTitle("XiSearch v" + XiVersion.getVersionString());

        loggingHandle = new JTextAreaHandle(txtLog);
        loggingHandle.setFilter(new Filter() {

            public boolean isLoggable(LogRecord record) {
                return true;
            }
            
        });


        loggingHandle.setLevel(Level.ALL);

        for (Level l : new Level[]{Level.ALL,Level.FINEST, Level.FINER, Level.FINE, Level.CONFIG, Level.INFO, Level.WARNING, Level.SEVERE, Level.OFF} ) {
            cmbLogLevel.addItem(l);
        }


        Logger.getLogger("rappsilber").addHandler(loggingHandle);
        Logger.getLogger("rappsilber").setLevel(Level.ALL);
        Logger.getLogger("rappsilber").addHandler(loggingHandle);

        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Logger Connected");

//        txtMSMFile.setLocalPropertyKey(LocalProperties.LAST_MSM_FOLDER);
//        txtMSMFile.setExtensions(new String[]{".msm",".mgf",".apl",".msmlist"});
//        txtMSMFile.setDescription("MSM-files");

        flMSMFiles.setLocalPropertyKey(LocalProperties.LAST_MSM_FOLDER);
        flMSMFiles.setExtensions(new String[]{".msm",".msmlist",".apl",".mgf",".gz",".mzml"});
        flMSMFiles.setDescription("MSM-files");

//        txtFastaFile.setLocalPropertyKey(LocalProperties.LAST_SEQUNECE_FOLDER);
//        txtFastaFile.setExtensions(new String[]{".fasta",".fastalist"});
//        txtFastaFile.setDescription("FASTA-files");
        

        flFASTAFiles.setLocalPropertyKey(LocalProperties.LAST_SEQUNECE_FOLDER);
        flFASTAFiles.setExtensions(new String[]{".fasta",".fasta.gz",".txt",".fastalist"});
        flFASTAFiles.setDescription("FASTA-files");
        flFASTAFiles.setSelectionName("Decoy");

        txtPeakList.setLocalPropertyKey("PEAKLIST");
        txtPeakList.setExtensions(new String[]{".tsv"});
        txtPeakList.setDescription("PeakList as TSV-file");
        txtPeakList.setSave();

        txtResultFile.setLocalPropertyKey("CSV_RESULT");
        txtResultFile.setExtensions(new String[]{".csv",".tsv",".txt",".csv.gz",".tsv.gz",".txt.gz"});
        txtResultFile.setDescription("Result as CSV-file");
        txtResultFile.setSave();
        txtResultFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String csvOut = txtResultFile.getText();
                if (csvOut.toLowerCase().endsWith(".csv")||csvOut.toLowerCase().endsWith(".tsv")||csvOut.toLowerCase().endsWith(".gz")) {
                    csvOut=csvOut.replaceAll("(.*)_XiVersion[0-9\\.a-zA-Z]*(\\.txt\\.gz|\\.[ct]sv\\.gz|\\.gz|\\.[ct]sv|\\.txt)","$1$2");
                    csvOut=csvOut.replaceAll("(.*)(\\.txt\\.gz|\\.txt|\\.[tc]sv\\.gz|\\.[tc]sv|\\.gz)","$1_XiVersion"+XiVersion.getVersionString()+"$2");
                }
                txtResultFile.setText(csvOut);
            }
        });

        fbSaveConfig.setLocalPropertyKey("XLink_Config");
        fbSaveConfig.setExtensions(new String[]{".conf",".txt"});
        fbSaveConfig.setDescription("config-files");

        fbLoadConfig.setLocalPropertyKey("XLink_Config");
        fbLoadConfig.setExtensions(new String[]{".cfg",".config",".conf",".txt"});
        fbLoadConfig.setDescription("config-files");

        
        try {
            BufferedReader br = Util.readFromClassPath(".rappsilber.data.DefaultConfig.conf");
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = br.readLine()) != null)
                sb.append(line + "\n");
            br.close();
            txtConfig.setText(sb.toString());
            txtConfig.setCaretPosition(1);

        } catch (IOException ex) {
            Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
        }

        gtpm = new GenericTextPopUpMenu();
        gtpm.installContextMenu(this);
        
        //xiFDRPath = Util.getFileRelative("xiFDR.jar", true).getAbsolutePath();
        File xiFDRFile = Util.getFileRelative("xiFDR.jar", true);
        if (xiFDRFile == null) {
            ckFDR.setSelected(false);
            ckFDRActionPerformed(new ActionEvent(this, 0, "Nothing"));
        } else {
            xiFDRPath = xiFDRFile.getAbsolutePath();
            fbXIFDR.setFile(xiFDRPath);
        }

        ChangeListener max100Listener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                SpinnerModel sp = (SpinnerModel) e.getSource();
                if (((Double)sp.getValue()) >100)
                    sp.setValue(100d);
            }
        };        

        spPepFDR.getModel().addChangeListener(max100Listener);
        spProteinFDR.getModel().addChangeListener(max100Listener);
        spLinkFDR.getModel().addChangeListener(max100Listener);
        spPsmFDR.getModel().addChangeListener(max100Listener);
        spPPIFdr.getModel().addChangeListener(max100Listener);

        this.addWindowFocusListener(new WindowFocusListener() {
            boolean shown =false;
            @Override
            public void windowGainedFocus(WindowEvent e) {
                
                if (LocalProperties.getProperty("CheckForNewVersion") == null && !shown) {
                    tpMain.setSelectedComponent(pMemory);
                    shown =true;
                }
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
            }

        });
    }


    private void startFDR(FDRInfo fdr) throws IOException, InterruptedException {
        final StatusMultiplex stat = new StatusMultiplex();
        stat.addInterface(new LoggingStatus());
        stat.addInterface(new TextBoxStatusInterface(txtRunState));        

        // switch to the fdr-tab
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                tpMain.setSelectedComponent(pFDR);
            }
        });
        
        // setup the process
        ProcessBuilder builder = new ProcessBuilder();
        LinkedList<String> args = new LinkedList<>();
        // first argument is the same java executable used here
        args.add(Util.findJava());
        // -jar xiFDR.jar
        args.add("-jar");
        args.add(fbXIFDR.getFile().getAbsolutePath());
        
        // forward the desired FDRs
        if (fdr.psmFDR < 100) {
            args.add("--psmfdr=" + fdr.psmFDR);
        }
        if (fdr.pepFDR < 100) {
            args.add("--pepfdr=" + fdr.pepFDR);
        }
        if (fdr.protFDR < 100) {
            args.add("--proteinfdr=" + fdr.protFDR);
        }
        if (fdr.pepFDR < 100) {
            args.add("--linkfdr=" + fdr.linkFDR);
        }
        if (fdr.pepFDR < 100) {
            args.add("--ppifdr=" + fdr.ppiFDR);
        }
        // should we boost?
        if (fdr.boostFDR) {
            if (fdr.boostLevel.contentEquals("Residue Pairs")) {
                args.add("--boost=link");
            }
            if (fdr.boostLevel.contentEquals("Peptide Pairs")) {
                args.add("--boost=pep");
            }
            if (fdr.boostLevel.contentEquals("Protein Pairs")) {
                args.add("--boost=prot");
            }
        }
        
        // define the output
        String fdrOutDir = new File(fdr.inputFile).getParent();
        String fdrOutBase = new File(fdr.inputFile).getName();
        if (fdrOutBase.toLowerCase().matches(".*\\.(csv|tsv|txt)")) {
            fdrOutBase = fdrOutBase.substring(0, fdrOutBase.length()-4);
        }
        args.add("--csvOutDir=" + fdrOutDir);
        args.add("--csvBaseName=" + fdrOutBase);
        args.add("--xiconfig=" + fdr.xiconfig);
        for (File f : fdr.fastas) {
            args.add("--fasta=" + f.getAbsolutePath());
        }
        args.add(fdr.inputFile);
        
        builder.command(args);
 
        stat.setStatus("calling xiFDR: " +MyArrayUtils.toString(args, " "));
        Process process = builder.start();
        xifdrProcess = process;
        final BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        final BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()));
        
        Thread fdrError = new Thread(new StreamToLog(error));
        Thread fdrOUT = new Thread(new StreamToLog(out));
        fdrError.setName("fdrErrorLog");
        fdrOUT.setName("fdrOutLog");
        fdrError.start();
        fdrOUT.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            stat.setStatus("Error while running FDR");
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    tpMain.setSelectedComponent(pFDR);
                }
            });
            
        } else {
            stat.setStatus("xiFDR completed");
        }
        xifdrProcess = null;
            
    }

    
    public void startRun() {
//        String msmFile = txtMSMFile.getText();
        final Locale numberlocale = lpNumberLocale.getSelectLocale();
        final String csvOut = txtResultFile.getText();
        String c = txtConfig.getText();
        if (tpConfig.getSelectedComponent() instanceof BasicConfig) {
            try {
                c=((BasicConfig) tpConfig.getSelectedComponent()).getConfig();
            } catch (IOException ex) {
                Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        final String configString = c;
        final StatusMultiplex stat = new StatusMultiplex();

        stat.addInterface(new LoggingStatus());
        stat.addInterface(new TextBoxStatusInterface(txtRunState));


        
        if (csvOut == null || csvOut.trim().isEmpty()) {
            Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, "No result file for output selected");
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(rootPane, "No result file for output selected","Missing Output File", JOptionPane.ERROR_MESSAGE);
                    tpMain.setSelectedIndex(0);
                    txtResultFile.requestFocus();
                    stat.setStatus("No Result-file selected");
                }
            });
            return;
        }

        final FDRInfo fdr = new FDRInfo();

        
        Runnable runnable = new Runnable() {
            public void run() {
                File msm = null;
                RunConfig conf;
                XiProcess xi;
                
                String confString = null;
                try {
                    conf = new RunConfigFile(new StringReader(configString));
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, "Error while reading config file " + configString, ex);
                    JOptionPane.showMessageDialog(rootPane, ex.getLocalizedMessage(), "Error while reading config file " + configString, JOptionPane.ERROR_MESSAGE);
                    stat.setStatus("Error while reading config file " + configString + ":"+ ex);
                    return;
                } catch (IOException ex) {
                    Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, "Error while reading config file " + configString, ex);
                    JOptionPane.showMessageDialog(rootPane, ex.getLocalizedMessage(), "Error while reading config file " + configString, JOptionPane.ERROR_MESSAGE);
                    stat.setStatus("Error while reading config file " + configString + ":"+ ex);
                    return;
                } catch (ParseException ex) {
                    Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, "Error while reading config file ", ex);
                    JOptionPane.showMessageDialog(rootPane, ex.getLocalizedMessage(), "Parser Error while reading config file " + ex, JOptionPane.ERROR_MESSAGE);
                    stat.setStatus("Error while reading config file " + configString + ":"+ ex);
                    return;
                }
                conf.addStatusInterface(stat);
                
                if (flFASTAFiles.getFiles().length == 0) {
                    Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, "No FASTA file provided");
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            JOptionPane.showMessageDialog(rootPane, "FASTA file missing","FASTA file missing", JOptionPane.ERROR_MESSAGE);
                            tpMain.setSelectedIndex(0);
                            stat.setStatus("FASTA file missing");
                        }
                    });
                    return;
                    
                }
                
                stat.setStatus("opening peaklists");
                AbstractMSMAccess input;
                try {
                    MSMListIterator listInput = new MSMListIterator(conf.getFragmentTolerance(), 1, conf);

                    File[] list = flMSMFiles.getFiles();
                    if (list.length > 0) {
                        for (File f : list) {
                            listInput.addFile(f.getAbsolutePath(), "", conf.getFragmentTolerance());
                        }
                    } else {
                        Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, "No peak-list provided ");
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                tpMain.setSelectedIndex(0);
                                stat.setStatus("No peak-list provided");
                                JOptionPane.showMessageDialog(rootPane, "No peak-list provided","No peak-list provided ", JOptionPane.ERROR_MESSAGE);
                            }
                        });
                        return;
                    }
                    listInput.init();
                    
                    input = listInput;
                    
                } catch (ParseException|FileNotFoundException ex) {
                    Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, "Error while reading peak list ", ex);
                    JOptionPane.showMessageDialog(rootPane, ex.getLocalizedMessage(), "Error wile reading file ", JOptionPane.ERROR_MESSAGE);
                    stat.setStatus("Error while reading peak list:" + ex);
                    return;
                } catch (IOException ex) {
                    Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, "Error wile reading file", ex);
                    JOptionPane.showMessageDialog(rootPane, ex.getLocalizedMessage(), "Error wile reading file ", JOptionPane.ERROR_MESSAGE);
                    stat.setStatus("Error while reading peak list:" + ex);
                    return;
                }
                
                ResultWriter output;
                try {

                    if (csvOut == null || csvOut.trim().isEmpty()) {
                        Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, "No result file for output selected");
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(rootPane, "No result file for output selected","Missing Output File", JOptionPane.ERROR_MESSAGE);
                                tpMain.setSelectedIndex(0);
                                txtResultFile.requestFocus();
                                stat.setStatus("No Result-file selected");
                            }
                        });
                        return;
                    }
                    boolean gzip =false;
                    boolean tabSep=false;
                    if (csvOut.endsWith(".gz")) {
                        gzip=true;
                    }
                    
                    if (csvOut.endsWith("txt.gz")||csvOut.endsWith("tsv.gz")||csvOut.endsWith("txt")||csvOut.endsWith("tsv")) {
                        tabSep=true;
                    }

                    CSVExportMatches CSVOut = null;
                    if (csvOut.contentEquals("-")) {
                        CSVOut = new CSVExportMatches(System.out, conf,gzip);
                    } else {
                        CSVOut = new CSVExportMatches(new FileOutputStream(csvOut), conf,gzip);
                    }
                    if (tabSep) {
                        CSVOut.setDelimChar("\t");
                    }
                    CSVOut.setLocale(numberlocale);

                    ResultMultiplexer rm = new ResultMultiplexer();
                    rm.addResultWriter(CSVOut);

                    if (ckPeakAnnotations.isSelected()) {
                        String peakout = txtPeakList.getText();

                        if (peakout.toLowerCase().endsWith(".csv")||peakout.toLowerCase().endsWith(".gz")) {
                            peakout = peakout.substring(0, peakout.length()-4) + 
                                    XiVersion.getVersionString() + 
                                    peakout.substring(peakout.length()-5,peakout.length());
                        }
                        if (peakout == null || peakout.trim().isEmpty()) {
                            Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, "No file for annotated peak-list selected");
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    JOptionPane.showMessageDialog(rootPane, "No file for annotated peak-list selected","Missing Output File", JOptionPane.ERROR_MESSAGE);
                                    tpMain.setSelectedIndex(0);
                                    txtResultFile.requestFocus();
                                    stat.setStatus("No file for annotated peak-list selected");
                                }
                            });
                            return;
                        }
                        if (peakout.length() > 0) {
                            OutputStream out= new FileOutputStream((String) peakout);
                            if (peakout.endsWith("gz")) {
                                out = new GZIPOutputStream(out);
                            }

                            PeakListWriter plw = new PeakListWriter(out);
                            plw.setLocale(numberlocale);
                            rm.addResultWriter(plw);
                        }
                    }

                    output = rm;

                } catch (IOException ex) {
                    Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, "Error while creating output-file ", ex);
                    return;
                }
                
                // store the config alogside the result file
                try {
                    String configOut = txtResultFile.getFile().getAbsolutePath()+".config";
                    fdr.xiconfig = configOut;
                    PrintWriter pwconfig = new PrintWriter(
                            new File(configOut));
                    pwconfig.write(configString);
                    pwconfig.close();
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
                }

                stat.setStatus("starting search");
                xirunner = new RunXi(input, output, conf, fdr);
                Thread t = new Thread(xirunner);
                t.setName("xirunner");
                t.start();
                
            }
        };
        ThreadGroup searchThreadGroup = new ThreadGroup("XiSearchGroup");
        Thread setup = new Thread(searchThreadGroup,runnable);
        setup.setName("xi-setup");
        setup.start();

    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bgFDROptimized = new javax.swing.ButtonGroup();
        spProteinFDR = new javax.swing.JSpinner();
        lblProteinGroup = new javax.swing.JLabel();
        tpMain = new javax.swing.JTabbedPane();
        pRun = new javax.swing.JPanel();
        btnStartSearch = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        txtPeakList = new rappsilber.gui.components.FileBrowser();
        txtResultFile = new rappsilber.gui.components.FileBrowser();
        btnStop = new javax.swing.JButton();
        ckPeakAnnotations = new javax.swing.JCheckBox();
        jPanel1 = new javax.swing.JPanel();
        flMSMFiles = new rappsilber.gui.components.FileList();
        jPanel2 = new javax.swing.JPanel();
        flFASTAFiles = new rappsilber.gui.components.FileList();
        pConfig = new javax.swing.JPanel();
        tpConfig = new javax.swing.JTabbedPane();
        basicConfig1 = new rappsilber.gui.components.config.BasicConfig();
        pAdvancedonfig = new javax.swing.JPanel();
        fbLoadConfig = new rappsilber.gui.components.FileBrowser();
        btnLoadConfig = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtConfig = new javax.swing.JTextArea();
        fbSaveConfig = new rappsilber.gui.components.FileBrowser();
        btnSave = new javax.swing.JButton();
        btnStartSearch1 = new javax.swing.JButton();
        lpNumberLocale = new rappsilber.gui.components.LocalPicker();
        jLabel1 = new javax.swing.JLabel();
        btnStop1 = new javax.swing.JButton();
        pFDR = new javax.swing.JPanel();
        ckFDR = new javax.swing.JCheckBox();
        lblPsmFDR = new javax.swing.JLabel();
        spPsmFDR = new javax.swing.JSpinner();
        lblMaxFDR = new javax.swing.JLabel();
        lblPepFDR = new javax.swing.JLabel();
        spPepFDR = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        spLinkFDR = new javax.swing.JSpinner();
        jLabel10 = new javax.swing.JLabel();
        spPPIFdr = new javax.swing.JSpinner();
        cbBoost = new javax.swing.JComboBox<>();
        ckBoost = new javax.swing.JCheckBox();
        spFDRLog = new javax.swing.JScrollPane();
        txtFDRLog = new javax.swing.JTextArea();
        fbXIFDR = new rappsilber.gui.components.FileBrowser();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        btnStartFDR = new javax.swing.JButton();
        btnStopFDR = new javax.swing.JButton();
        pMemory = new javax.swing.JPanel();
        memory1 = new rappsilber.gui.components.memory.Memory();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtLog = new javax.swing.JTextArea();
        cmbLogLevel = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();
        callBackSettings1 = new rappsilber.gui.components.CallBackSettings();
        txtRunState = new javax.swing.JTextField();

        spProteinFDR.setModel(new javax.swing.SpinnerNumberModel(100.0d, 0.0d, null, 1.0d));
        spProteinFDR.setToolTipText("FDR value accepted for protein-groups");

        lblProteinGroup.setText("Protein Group");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Offline Xi");

        tpMain.setToolTipText("");

        btnStartSearch.setText("Start");
        btnStartSearch.setToolTipText("Start the search");
        btnStartSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartSearchActionPerformed(evt);
            }
        });

        jLabel6.setText("Result-output");
        jLabel6.setToolTipText("The search-result will be writen into this file");

        txtPeakList.setEnabled(false);

        txtResultFile.setToolTipText("The search-result will be writen into this file");

        btnStop.setText("Stop");
        btnStop.setToolTipText("Stop the current search");
        btnStop.setEnabled(false);
        btnStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopActionPerformed(evt);
            }
        });

        ckPeakAnnotations.setText("peak annotations");
        ckPeakAnnotations.setToolTipText("Write out a tab separated file containing all the peak-annotations");
        ckPeakAnnotations.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckPeakAnnotationsActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Peak List"));

        flMSMFiles.setToolTipText("The peak lists that should be searched");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(flMSMFiles, javax.swing.GroupLayout.DEFAULT_SIZE, 736, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(flMSMFiles, javax.swing.GroupLayout.DEFAULT_SIZE, 103, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("FASTA"));

        flFASTAFiles.setToolTipText("The fasta file that the peak list should be searched against");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(flFASTAFiles, javax.swing.GroupLayout.DEFAULT_SIZE, 736, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(flFASTAFiles, javax.swing.GroupLayout.DEFAULT_SIZE, 104, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout pRunLayout = new javax.swing.GroupLayout(pRun);
        pRun.setLayout(pRunLayout);
        pRunLayout.setHorizontalGroup(
            pRunLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pRunLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pRunLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pRunLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnStartSearch)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnStop))
                    .addGroup(pRunLayout.createSequentialGroup()
                        .addGroup(pRunLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6)
                            .addComponent(ckPeakAnnotations))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(pRunLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtPeakList, javax.swing.GroupLayout.DEFAULT_SIZE, 618, Short.MAX_VALUE)
                            .addComponent(txtResultFile, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        pRunLayout.setVerticalGroup(
            pRunLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pRunLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(12, 12, 12)
                .addGroup(pRunLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtResultFile, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pRunLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(txtPeakList, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ckPeakAnnotations))
                .addGap(18, 18, 18)
                .addGroup(pRunLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnStartSearch)
                    .addComponent(btnStop))
                .addContainerGap())
        );

        tpMain.addTab("Run", pRun);

        tpConfig.addTab("Basic Config", basicConfig1);

        fbLoadConfig.setExtensions(new String[] {"txt"});

        btnLoadConfig.setText("Load");
        btnLoadConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLoadConfigActionPerformed(evt);
            }
        });

        jScrollPane2.setAutoscrolls(true);

        txtConfig.setColumns(20);
        txtConfig.setRows(5);
        jScrollPane2.setViewportView(txtConfig);

        fbSaveConfig.setExtensions(new String[] {"txt"});

        btnSave.setText("save");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pAdvancedonfigLayout = new javax.swing.GroupLayout(pAdvancedonfig);
        pAdvancedonfig.setLayout(pAdvancedonfigLayout);
        pAdvancedonfigLayout.setHorizontalGroup(
            pAdvancedonfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pAdvancedonfigLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pAdvancedonfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pAdvancedonfigLayout.createSequentialGroup()
                        .addComponent(fbSaveConfig, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnSave))
                    .addGroup(pAdvancedonfigLayout.createSequentialGroup()
                        .addComponent(fbLoadConfig, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnLoadConfig))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 765, Short.MAX_VALUE))
                .addContainerGap())
        );
        pAdvancedonfigLayout.setVerticalGroup(
            pAdvancedonfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pAdvancedonfigLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pAdvancedonfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btnLoadConfig)
                    .addComponent(fbLoadConfig, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pAdvancedonfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(fbSaveConfig, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSave))
                .addContainerGap())
        );

        tpConfig.addTab("Text Config", pAdvancedonfig);

        btnStartSearch1.setText("Start");
        btnStartSearch1.setToolTipText("Start the search");
        btnStartSearch1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartSearchActionPerformed(evt);
            }
        });

        lpNumberLocale.setToolTipText("Defines how numbers are writen out to the result file");
        lpNumberLocale.setDefaultLocal(java.util.Locale.ENGLISH);

        jLabel1.setText("Number Format:");

        btnStop1.setText("Stop");
        btnStop1.setToolTipText("Stop the current search");
        btnStop1.setEnabled(false);
        btnStop1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pConfigLayout = new javax.swing.GroupLayout(pConfig);
        pConfig.setLayout(pConfigLayout);
        pConfigLayout.setHorizontalGroup(
            pConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tpConfig, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(pConfigLayout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lpNumberLocale, javax.swing.GroupLayout.PREFERRED_SIZE, 268, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnStartSearch1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnStop1)
                .addGap(3, 3, 3))
        );
        pConfigLayout.setVerticalGroup(
            pConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pConfigLayout.createSequentialGroup()
                .addComponent(tpConfig, javax.swing.GroupLayout.DEFAULT_SIZE, 387, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnStop1)
                    .addComponent(btnStartSearch1)
                    .addComponent(lpNumberLocale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addContainerGap())
        );

        tpMain.addTab("Config", pConfig);

        ckFDR.setSelected(true);
        ckFDR.setText("Do FDR");
        ckFDR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckFDRActionPerformed(evt);
            }
        });

        lblPsmFDR.setText("PSM");

        spPsmFDR.setModel(new javax.swing.SpinnerNumberModel(100.0d, 0.0d, null, 1.0d));
        spPsmFDR.setToolTipText("FDR value accepted for PSMs");

        lblMaxFDR.setText("Max FDRs");

        lblPepFDR.setText("Peptide Pair");

        spPepFDR.setModel(new javax.swing.SpinnerNumberModel(100.0d, 0.0d, null, 1.0d));
        spPepFDR.setToolTipText("FDR value accepted for Peptide Pairs (including linksite within the peptide)");

        jLabel4.setText("Residue Pairs");

        spLinkFDR.setModel(new javax.swing.SpinnerNumberModel(5.0d, 0.0d, null, 1.0d));
        spLinkFDR.setToolTipText("FDR value accepted for unique residue pairs (links)");

        jLabel10.setText("Protein Pairs");

        spPPIFdr.setModel(new javax.swing.SpinnerNumberModel(100.0d, 0.0d, null, 1.0d));
        spPPIFdr.setToolTipText("FDR value accepted for protein-pairs");

        cbBoost.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Peptide Pairs", "Residue Pairs", "Protein Pairs" }));
        cbBoost.setSelectedIndex(1);
        cbBoost.setToolTipText("Filter results to maximize this level");

        ckBoost.setSelected(true);
        ckBoost.setText("boost");
        ckBoost.setToolTipText("Apply prefilter to maximize the results ata fixed confidence");

        txtFDRLog.setColumns(20);
        txtFDRLog.setRows(5);
        spFDRLog.setViewportView(txtFDRLog);

        fbXIFDR.setToolTipText("The xiFDR version to use");
        fbXIFDR.setDescription("JAR-File");
        fbXIFDR.setExtensions(new String[] {"/xifdr.*jar/"});

        jLabel2.setText("xiFDR");

        jLabel3.setText("FDR-Log");

        btnStartFDR.setText("Start FDR");
        btnStartFDR.setToolTipText("starts a new FDR-calculation");
        btnStartFDR.setEnabled(false);
        btnStartFDR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartFDRActionPerformed(evt);
            }
        });

        btnStopFDR.setText("Stop FDR");
        btnStopFDR.setToolTipText("stops a currently running FDR-calculation");
        btnStopFDR.setEnabled(false);
        btnStopFDR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopFDRActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pFDRLayout = new javax.swing.GroupLayout(pFDR);
        pFDR.setLayout(pFDRLayout);
        pFDRLayout.setHorizontalGroup(
            pFDRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pFDRLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pFDRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(ckBoost)
                    .addComponent(ckFDR)
                    .addComponent(fbXIFDR, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel2)
                    .addGroup(pFDRLayout.createSequentialGroup()
                        .addGroup(pFDRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblPsmFDR)
                            .addComponent(lblPepFDR)
                            .addComponent(jLabel4)
                            .addComponent(jLabel10))
                        .addGap(25, 25, 25)
                        .addGroup(pFDRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(btnStopFDR, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(pFDRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(btnStartFDR, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(spPPIFdr)
                                .addComponent(spLinkFDR)
                                .addComponent(spPepFDR)
                                .addComponent(spPsmFDR)
                                .addComponent(lblMaxFDR, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(cbBoost, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pFDRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(spFDRLog, javax.swing.GroupLayout.DEFAULT_SIZE, 521, Short.MAX_VALUE)
                    .addGroup(pFDRLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        pFDRLayout.setVerticalGroup(
            pFDRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pFDRLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pFDRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pFDRLayout.createSequentialGroup()
                        .addComponent(ckFDR)
                        .addGap(18, 18, 18))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pFDRLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addGap(8, 8, 8)))
                .addGroup(pFDRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pFDRLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fbXIFDR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblMaxFDR)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(pFDRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spPsmFDR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblPsmFDR))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(pFDRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spPepFDR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblPepFDR))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(pFDRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spLinkFDR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(pFDRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spPPIFdr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel10))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(pFDRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cbBoost, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(ckBoost))
                        .addGap(18, 18, 18)
                        .addComponent(btnStartFDR)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnStopFDR)
                        .addGap(0, 70, Short.MAX_VALUE))
                    .addComponent(spFDRLog))
                .addContainerGap())
        );

        tpMain.addTab("FDR", pFDR);

        txtLog.setColumns(20);
        txtLog.setRows(5);
        jScrollPane1.setViewportView(txtLog);

        cmbLogLevel.setToolTipText("how detailed the log should be displayed");
        cmbLogLevel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbLogLevelActionPerformed(evt);
            }
        });

        jLabel5.setText("Log");

        javax.swing.GroupLayout pMemoryLayout = new javax.swing.GroupLayout(pMemory);
        pMemory.setLayout(pMemoryLayout);
        pMemoryLayout.setHorizontalGroup(
            pMemoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pMemoryLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pMemoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(memory1, javax.swing.GroupLayout.DEFAULT_SIZE, 770, Short.MAX_VALUE)
                    .addGroup(pMemoryLayout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addGap(7, 7, 7)
                        .addComponent(cmbLogLevel, javax.swing.GroupLayout.PREFERRED_SIZE, 268, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(pMemoryLayout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 427, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(callBackSettings1, javax.swing.GroupLayout.DEFAULT_SIZE, 331, Short.MAX_VALUE)))
                .addContainerGap())
        );
        pMemoryLayout.setVerticalGroup(
            pMemoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pMemoryLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(memory1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(pMemoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmbLogLevel, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pMemoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(callBackSettings1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 338, Short.MAX_VALUE))
                .addContainerGap())
        );

        tpMain.addTab("Log", pMemory);

        txtRunState.setEditable(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tpMain)
            .addComponent(txtRunState, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(tpMain)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtRunState, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnStartSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStartSearchActionPerformed

        startRun();
        btnStop.setEnabled(true);

    }//GEN-LAST:event_btnStartSearchActionPerformed

    private void btnStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopActionPerformed
        xirunner.stop();
        btnStop.setEnabled(false);
    }//GEN-LAST:event_btnStopActionPerformed

    private String doubleToString(double value) {
        DecimalFormat oneDigit = new DecimalFormat("0.0");
        String fUnit = "B";
        if (value > 1024*1024*900) {
            value /= 1024*1024*1024;
            fUnit = "GB";
        }else if (value > 1024*900) {
            value /= 1024*1024;
            fUnit = "MB";
        }else if (value > 900) {
            value /= 1024;
            fUnit = "KB";
        }
        return "" + oneDigit.format(value) + fUnit;

    }

    private void cmbLogLevelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbLogLevelActionPerformed
        loggingHandle.setLevel((Level) cmbLogLevel.getSelectedItem());
        Logger.getLogger("rappsilber").setLevel(Level.ALL);
    }//GEN-LAST:event_cmbLogLevelActionPerformed

    private void btnLoadConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLoadConfigActionPerformed
        File f = fbLoadConfig.getFile();
        if (f != null)
            loadConfig(fbLoadConfig.getFile(),false);
    }//GEN-LAST:event_btnLoadConfigActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        FileWriter fw = null;
        try {
            File f = fbSaveConfig.getFile();
            fw = new FileWriter(f);
            fw.write(txtConfig.getText());
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fw.close();
            } catch (IOException ex) {
                Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_btnSaveActionPerformed

    private void ckPeakAnnotationsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ckPeakAnnotationsActionPerformed
        txtPeakList.setEnabled(ckPeakAnnotations.isSelected());
    }//GEN-LAST:event_ckPeakAnnotationsActionPerformed

    private void ckFDRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ckFDRActionPerformed
        boolean en = ckFDR.isSelected();
        spPsmFDR.setEnabled(en);
        spPepFDR.setEnabled(en);
        spProteinFDR.setEnabled(en);
        spLinkFDR.setEnabled(en);
        spPPIFdr.setEnabled(en);
        cbBoost.setEnabled(en);
        ckBoost.setEnabled(en);
        fbXIFDR.setEnabled(en);
    }//GEN-LAST:event_ckFDRActionPerformed

    private void btnStartFDRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStartFDRActionPerformed
        if (xifdrProcess == null) {
            startXiFDR(new FDRInfo());
        } 
        
    }//GEN-LAST:event_btnStartFDRActionPerformed

    private void btnStopFDRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopFDRActionPerformed
        if (xifdrProcess != null) {
            xifdrProcess.destroy();
        }
    }//GEN-LAST:event_btnStopFDRActionPerformed

    protected void startXiFDR(final FDRInfo info) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                 btnStartFDR.setEnabled(false);
                 btnStopFDR.setEnabled(true);
            }
        });
        
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    startFDR(info);
                } catch (IOException ex) {
                    Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
                }
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                         btnStartFDR.setEnabled(true);
                         btnStopFDR.setEnabled(false);
                    }
                });
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        JOptionPane.showMessageDialog(SimpleXiGui.this, "xiFDR Finished.");
                    }
                });
                

            }
        };
        Thread xiFDRThread = new Thread(runnable);
        xiFDRThread.setName("xiFDR");
        xiFDRThread.start();
    }


    public void setConfigFile(String f) {
        
        loadConfig(new File(f),false);

    }

    public void addConfigFile(String f) {
        
        loadConfig(fbLoadConfig.getFile(),true);

    }
    
    public void setResultFile(String f) {
        txtResultFile.setText(f);
    }
    
    public void addMSMFile(String file) {
        this.flMSMFiles.addFile(new File(file));
    }


    public void addFastaFile(String file) {
        this.flFASTAFiles.addFile(new File(file));
    }
    
    private static class StartGui implements Runnable{
        private String[] args;
        public StartGui(String[] args) {
            this.args = args;
        }
        public void run() {
            SimpleXiGui sxi = new SimpleXiGui();
            if (args.length >0) {
                
                sxi.addMSMFile(args[0]);
                //String msmFile = args[0];
                sxi.addFastaFile(args[1]);
                //String fastaFile = args[1];
                sxi.setConfigFile(args[2]);
                //String configFile = args[2];
                sxi.setResultFile(args[3]);
                //String csvOut = args[3];
                //RunConfig conf;

            }
            sxi.setVisible(true);
        }
    }
    
    public void loadConfig(File f, boolean append) {
        //File f = btnLoadConfig.getFile();
        StringBuffer config = new StringBuffer();
        try {
            if (f!= null) {
                BufferedReader confIn = new BufferedReader(new FileReader(f));
                String line;
                while ((line = confIn.readLine()) != null) {
                    config.append(line);
                    config.append("\n");
                }
                confIn.close();
                if (append)
                    txtConfig.append(config.toString());
                else 
                    txtConfig.setText(config.toString());
            }
        }catch (IOException ioe) {
            System.err.println(ioe);
        }        
    }
    
    public void appendConfigLine(String line) {
        txtConfig.append(line);
    }
    
    
    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new StartGui(args));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private rappsilber.gui.components.config.BasicConfig basicConfig1;
    private javax.swing.ButtonGroup bgFDROptimized;
    private javax.swing.JButton btnLoadConfig;
    private javax.swing.JButton btnSave;
    private javax.swing.JButton btnStartFDR;
    private javax.swing.JButton btnStartSearch;
    private javax.swing.JButton btnStartSearch1;
    private javax.swing.JButton btnStop;
    private javax.swing.JButton btnStop1;
    private javax.swing.JButton btnStopFDR;
    private rappsilber.gui.components.CallBackSettings callBackSettings1;
    private javax.swing.JComboBox<String> cbBoost;
    private javax.swing.JCheckBox ckBoost;
    private javax.swing.JCheckBox ckFDR;
    private javax.swing.JCheckBox ckPeakAnnotations;
    private javax.swing.JComboBox cmbLogLevel;
    private rappsilber.gui.components.FileBrowser fbLoadConfig;
    private rappsilber.gui.components.FileBrowser fbSaveConfig;
    private rappsilber.gui.components.FileBrowser fbXIFDR;
    private rappsilber.gui.components.FileList flFASTAFiles;
    public rappsilber.gui.components.FileList flMSMFiles;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lblMaxFDR;
    private javax.swing.JLabel lblPepFDR;
    private javax.swing.JLabel lblProteinGroup;
    private javax.swing.JLabel lblPsmFDR;
    private rappsilber.gui.components.LocalPicker lpNumberLocale;
    private rappsilber.gui.components.memory.Memory memory1;
    private javax.swing.JPanel pAdvancedonfig;
    private javax.swing.JPanel pConfig;
    private javax.swing.JPanel pFDR;
    private javax.swing.JPanel pMemory;
    private javax.swing.JPanel pRun;
    private javax.swing.JScrollPane spFDRLog;
    private javax.swing.JSpinner spLinkFDR;
    private javax.swing.JSpinner spPPIFdr;
    private javax.swing.JSpinner spPepFDR;
    private javax.swing.JSpinner spProteinFDR;
    private javax.swing.JSpinner spPsmFDR;
    private javax.swing.JTabbedPane tpConfig;
    private javax.swing.JTabbedPane tpMain;
    private javax.swing.JTextArea txtConfig;
    private javax.swing.JTextArea txtFDRLog;
    private javax.swing.JTextArea txtLog;
    private rappsilber.gui.components.FileBrowser txtPeakList;
    private rappsilber.gui.components.FileBrowser txtResultFile;
    private javax.swing.JTextField txtRunState;
    // End of variables declaration//GEN-END:variables

}
