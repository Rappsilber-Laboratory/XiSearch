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

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
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
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.SpinnerModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.rappsilber.utils.ProcessLauncher;
import org.rappsilber.utils.ProcessLogToTextArea;
import org.rappsilber.utils.ProcessLogger;
import org.rappsilber.utils.RArrayUtils;
import org.rappsilber.utils.Version;
import rappsilber.applications.SimpleXiProcessMultipleCandidates;
import rappsilber.applications.XiProcess;
import rappsilber.config.LocalProperties;
import rappsilber.config.RunConfig;
import rappsilber.config.RunConfigFile;
import rappsilber.gui.components.GenericTextPopUpMenu;
import rappsilber.gui.components.config.ConfigProvider;
import rappsilber.gui.components.config.LoadDBConfig;
import rappsilber.gui.components.db.GetSearch;
import rappsilber.gui.logging.JTextAreaHandle;
import rappsilber.ms.dataAccess.StackedSpectraAccess;
import rappsilber.ms.dataAccess.filter.spectrafilter.BS3ReporterIonPeaksFilteredSpectrumAccess;
import rappsilber.ms.dataAccess.filter.spectrafilter.SetRunIDFilter;
import rappsilber.ms.dataAccess.msm.AbstractMSMAccess;
import rappsilber.ms.dataAccess.msm.MSMListIterator;
import rappsilber.ms.dataAccess.output.CSVExportMatches;
import rappsilber.ms.dataAccess.output.PeakListWriter;
import rappsilber.ms.dataAccess.output.ResultMultiplexer;
import rappsilber.ms.dataAccess.output.ResultWriter;
import rappsilber.ms.sequence.SequenceList;
import rappsilber.ui.LoggingStatus;
import rappsilber.ui.MemMapStatusControl;
import rappsilber.ui.StatusMultiplex;
import rappsilber.ui.TextBoxStatusInterface;
import rappsilber.utils.MyArrayUtils;
import rappsilber.utils.Util;
import rappsilber.utils.XiProvider;
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
    boolean search_done = false;
    ConfigProvider configProvider;
    ProcessLauncher xiSearchBackgroundProcess = null;

    String propertyFirst = "mzIdenMLOwnerFirst";
    String propertyLast = "mzIdenMLOwnerLast";
    String propertyEMail = "mzIdenMLOwnerEmail";
    String propertyAddress = "mzIdenMLOwnerAddress";
    String propertyOrg = "mzIdenMLOwnerOrg";
    String propertyLastXiVersion = "lastxiversion";
    private Version xifdr_version;
    

    private class FDRInfo {
        boolean runXiFDR = ckFDR.isSelected();
        double psmFDR = (Double) spPsmFDR.getValue();
        double pepFDR = (Double) spPepFDR.getValue();
        double linkFDR = (Double) spLinkFDR.getValue();
        double protFDR = (Double) spProteinFDR.getValue();
        double ppiFDR = (Double) spPPIFdr.getValue();
        boolean boostFDR = ckBoost.isSelected();
        boolean boostBetween = ckBoostBetween.isSelected();
        String boostLevel = cbBoost.getSelectedItem().toString();
        String inputFile = txtResultFile.getFile().getAbsolutePath();
        String xiconfig = txtResultFile.getFile().getAbsolutePath().replaceAll("\\.csv$", ".config");
        File[] fastas = flFASTAFiles.getFiles();
        Boolean showGui = ckFDRGUI.isSelected();
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
            
            search_done = true;
            if (m_fdr.runXiFDR && !m_xi.getConfig().hasError()) {
                startXiFDR(m_fdr);
            } else {
                btnStartFDR.setEnabled(ckFDR.isSelected());
            }
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (Component c: m_enable) {
                        c.setEnabled(true);
                    }
                    for (Component c: m_disable) {
                        c.setEnabled(false);
                    }
                }
            });         
            if ((! m_fdr.runXiFDR) || (m_xi.getConfig().hasError())) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (m_xi.getConfig().hasError()) {
                            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            StringWriter sw = new StringWriter();
                            PrintWriter pw = new PrintWriter(sw);
                            m_xi.getConfig().errorException().printStackTrace(pw);
                            
                            SimpleXiGui.this.feedBack1.settext(m_xi.getConfig().errorMessage() + "\n\n" + sw.toString() +
                                    
                                    "\n\n-----------------------------------------------"
                                    + "\nSpectrum:" + m_xi.getConfig().errorSpectrum().toString() +
                                    "\n-------------------------------------------------" +
                                    "\n config: \nmaxpeptidemass:" + m_xi.getConfig().getMaxPeptideMass() + "\n" + 
                                    MyArrayUtils.toString(m_xi.getConfig().getConfigLines(), "\n"));
                            pw.close();
                            
                            String m = "lutz";
                            m += '\u002E' +"fischer";
                            m += '\u0040' + "tu-berlin.de";
                                    
                            String message = "<hml><body>Search Stoped With Error:<br/>\n" + m_xi.getConfig().errorMessage();
                            message += "<br/>\nif possible please send the log to <a href='mailto:" + m + "'>"+m+"</a>"
                                    + " or rise an issue at <a href='https://github.com/Rappsilber-Laboratory/XiSearch/issues'>"
                                    + "https://github.com/Rappsilber-Laboratory/XiSearch/issues</a>"
                                    + "<p>The easiest way to send the mail is to use the feedback tab. <br/>"
                                    + "Please check if the info in the feedback field is ok to send before pressing send</p></body></hml>";
                            tpMain.setSelectedComponent(pFeedback);
                            JEditorPane ep = new JEditorPane("text/html",message);   
                            ep.setEditable(false);
                            //ep.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));
                            ep.addHyperlinkListener(new HyperlinkListener() {
                                @Override
                                public void hyperlinkUpdate(HyperlinkEvent e) {
                                    if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                                        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                                            try {
                                                Desktop.getDesktop().browse(e.getURL().toURI());
                                            } catch (URISyntaxException ex) {
                                                Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
                                            } catch (IOException ex) {
                                                Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
                                            }
                                        }
                                    }
                                }
                                

                            });                            
                            JOptionPane.showMessageDialog(SimpleXiGui.this, ep, "Search finished", JOptionPane.ERROR_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(SimpleXiGui.this, "Search Finished.");
                        }
                    }
                });
                
            }
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
            btnStartSearch2.setEnabled(false);
            
            cfgBasicConfig.setEnabled(false);
            cfgTextConfig.setEnabled(false);

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
                    input.gatherData(Math.max(conf.getPreSearchThreads()/2,1));
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
                    if (d) {
                        seq.addFasta(fastas[f], SequenceList.DECOY_GENERATION.ISDECOY);
                    } else {
                        seq.addFasta(fastas[f], SequenceList.DECOY_GENERATION.ISTARGET);
                    }                        
                }
                
                xi = XiProvider.getXiSearch(seq,input, output, sc, conf, SimpleXiProcessMultipleCandidates.class);
                callBackSettings1.doCallBack(seq.size());
            } catch (IOException ex) {
                Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
            }
//            xi = getXiSearch(result_multiplexer, sc, conf, SimpleXiProcessLinearIncluded.class);


            //

            xi.addFilter(new SetRunIDFilter());
            conf.getStatusInterface().setStatus("start search");
            ThreadGroup tg  = new ThreadGroup("xisearch");
            
            Thread wxi  = new Thread(tg, new WaitForXi(xi, new Component[]{btnStartSearch, btnStartSearch1, cfgBasicConfig, cfgTextConfig}, new Component[]{btnStop, btnStop1},  fdr));
            wxi.start();

        }


        public void stop() {
            conf.stopSearch();
            xi.stop();
        }



    }

    private class StreamToLog implements Runnable{
        BufferedReader in;
        PrintWriter logFile = null;

        public StreamToLog(BufferedReader in) {
            this.in = in;
        }
        public StreamToLog(BufferedReader in, PrintWriter logfile) {
            this.in = in;
            this.logFile = logfile;
        }
        
        @Override
        public void run() {
            String line;
            try {
                if (logFile == null) {
                    while ((line = in.readLine()) != null) {
                        final String fl = line;
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                SimpleXiGui.this.loggingHandle.publish(fl + "\n");
                            }
                        });
                    }
                } else {
                    while ((line = in.readLine()) != null) {
                        final String fl = line;
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                SimpleXiGui.this.loggingHandle.publish(fl + "\n");
                            }
                        });
                        logFile.println(line);
                    }
                    logFile.close();
                    
                }
            } catch (IOException ex) {
                Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    /** Creates new form SimpleXiGui */
    public SimpleXiGui() {
        initComponents();
        this.setTitle("xiSEARCH v" + XiVersion.getVersionString());
        this.txtVersion.setText(XiVersion.getVersionString());
        this.txtChangeLog.setText(XiVersion.changes);
        this.txtChangeLog.setCaretPosition(0);

        loggingHandle = new JTextAreaHandle(txtLog);
        loggingHandle.setFilter(new Filter() {

            public boolean isLoggable(LogRecord record) {
                return true;
            }
            
        });
        btnStartSearch1.setVisible(false);


        loggingHandle.setLevel(Level.ALL);
        loggingHandle.setFilter(new Filter() {

            public boolean isLoggable(LogRecord record) {
                return true;
            }
        });

        for (Level l : new Level[]{Level.ALL,Level.FINEST, Level.FINER, Level.FINE, Level.CONFIG, Level.INFO, Level.WARNING, Level.SEVERE, Level.OFF} ) {
            cmbLogLevel.addItem(l);
        }


        Logger.getLogger("").addHandler(loggingHandle);
        Logger.getLogger("").setLevel(Level.INFO);
        //Logger.getLogger("rappsilber").addHandler(loggingHandle);

        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Logger Connected");

//        txtMSMFile.setLocalPropertyKey(LocalProperties.LAST_MSM_FOLDER);
//        txtMSMFile.setExtensions(new String[]{".msm",".mgf",".apl",".msmlist"});
//        txtMSMFile.setDescription("MSM-files");

        flMSMFiles.setLocalPropertyKey(LocalProperties.LAST_MSM_FOLDER);
        flMSMFiles.setExtensions(new String[]{".msm",".msmlist",".apl",".mgf",".gz",".mzml",".zip"});
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
                    csvOut=csvOut.replaceAll("(.*)_Xi(?:Version)?[0-9\\.a-zA-Z]*(\\.txt\\.gz|\\.[ct]sv\\.gz|\\.gz|\\.[ct]sv|\\.txt)","$1$2");
                    csvOut=csvOut.replaceAll("(.*)(\\.txt\\.gz|\\.txt|\\.[tc]sv\\.gz|\\.[tc]sv|\\.gz)","$1_Xi"+XiVersion.getVersionString()+"$2");
                }
                txtResultFile.setText(csvOut);
            }
        });

        gtpm = new GenericTextPopUpMenu();
        gtpm.installContextMenu(this);
        
        //xiFDRPath = Util.getFileRelative("xiFDR.jar", true).getAbsolutePath();
        File xiFDRFile = Util.getFileRelative(Pattern.compile("xiFDR.*.jar",Pattern.CASE_INSENSITIVE), true);
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
                if (((Double)sp.getValue()) >100) {
                    sp.setValue(100d);
                }
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
                    tpMain.setSelectedComponent(pFeedback);
                    shown =true;
                }
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
            }

        });
        this.cfgBasicConfig.addTransferListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                rbTextConfig.setSelected(true);
                cfgTextConfig.setConfig(e.getActionCommand());
                spConfig.setViewportView(cfgTextConfig);
                configProvider = cfgTextConfig;
            }
        });
        this.cfgTextConfig.setBasicConfig(cfgBasicConfig);        
        
        this.cfgTextConfig.addTransferListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                rbBasicConfig.setSelected(true);
                cfgBasicConfig.setConfig(e.getActionCommand());
                spConfig.setViewportView(cfgBasicConfig);
                configProvider = cfgBasicConfig;
            }
        });
        this.cfgBasicConfig.setTextConfig(cfgTextConfig);        

        
        spConfig.setViewportView(cfgBasicConfig);
        configProvider = cfgBasicConfig;
        Runnable runnable = new Runnable() {
            public void run() {
                Boolean ret = callBackSettings1.showFormIfNeeded();
                if (ret == null) {
                    tpMain.setSelectedComponent(pFeedback);
                }
                
            }
        };
        new Thread(runnable).start();

        fbLoadConfig.setLocalPropertyKey("XLink_Config");
        fbLoadConfig.setExtensions(new String[]{".cfg",".config",".conf",".txt"});
        fbLoadConfig.setDescription("config-files");
        fbLoadConfig.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnLoadConfig.setEnabled(!fbLoadConfig.getText().trim().isEmpty());
                btnAddConfig.setEnabled(!fbLoadConfig.getText().trim().isEmpty());
            }
        });
        try {
            if (new GetSearch().getConnection() == null) {
                btnLoadDB.setVisible(false);
            } else {
                
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.FINE, "DB connection defined");
                    
        }
        callBackSettings1.doCallBack(0);
        // find all templates
        File filesource = Util.getFileRelative("templates", true);
        ArrayList<File> templates = new ArrayList<File>();
        
        if (filesource != null) {
            for (File f : filesource.listFiles()) {
                if (f.getName().endsWith(".conf")) {
                    templates.add(f);
                }
            }
            if (templates.size()>0) {
                // make sure the comboobox does not grow to show the whole strings
                cbtemplatesWide.setPrototypeDisplayValue("XXXXXXXXXXXXXXX");
                templates.sort(new Comparator<File>() {
                    @Override
                    public int compare(File o1, File o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
                int textsize = 0;
                for (File f : templates) {
                    BufferedReader br = null;
                    try {
                        br = new BufferedReader(new FileReader(f));
                        String description = br.readLine().trim();
                        String text = f.getName().substring(0,f.getName().length()-5);
                        if (description.startsWith("#==")) {
                            text += " -- " + description.substring(4).trim();
                        }
                        cbtemplatesWide.addItem(text);
                        textsize = Math.max(textsize, cbtemplatesWide.getGraphics().getFontMetrics().stringWidth(text));
                        
                    } catch (IOException ex) {
                        Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        try {
                            br.close();
                        } catch (IOException ex) {
                            Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                //cbtemplatesWide.setOpenWidth(900);
                cbtemplatesWide.setOpenWidth(textsize);
            } else {
                pTemplate.setVisible(false);
            }
                
        } else {
            btnTemplateLoad.setVisible(false);
            
        }
        
        cbtemplatesWide.setPreferredSize(new Dimension(150, cbtemplatesWide.getPreferredSize().height));
        
        fbXIFDR.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ckFDRActionPerformed(e);
            }
        });
        
        txtMZIDFirst.setText(LocalProperties.getProperty(propertyFirst, ""));
        txtMZIDLast.setText(LocalProperties.getProperty(propertyLast, ""));
        txtMZIDEMail.setText(LocalProperties.getProperty(propertyEMail, ""));
        txtMZIDAddress.setText(LocalProperties.getProperty(propertyAddress, ""));
        txtMZIDOrg.setText(LocalProperties.getProperty(propertyOrg, ""));
        Version lastVersion = new Version(LocalProperties.getProperty(propertyLastXiVersion, "0.0.0"));
        Version current = XiVersion.version;
        if (lastVersion.major<1 || (lastVersion.major == 1 && lastVersion.minor<8)) {
            JOptionPane.showMessageDialog(this, "Be aware, that the memory for the actual search is now defined in the GUI.\n"+
                    "\nYou can also use templates - any config file in the \"templates\" subfolder\n"
                    + "will be presented as template.", "Memory now defined in GUI", JOptionPane.INFORMATION_MESSAGE);
            this.cfgBasicConfig.spMemGB.requestFocus();
            this.cfgBasicConfig.spMemGB.setForeground(Color.red);
            this.cfgBasicConfig.lblMemGB.setForeground(Color.red);
            this.lblTemplate.setForeground(Color.red);
            this.btnTemplateLoad.setForeground(Color.red);
            this.cbtemplatesWide.setForeground(Color.red);
            ((TitledBorder)this.pTemplate.getBorder()).setTitleColor(Color.red);
        }
        if (lastVersion.major > 0 & (lastVersion.major<current.major || 
                (lastVersion.major==current.major && lastVersion.minor<current.minor) || 
                (lastVersion.major==current.major && lastVersion.minor==current.minor && lastVersion.minor<current.minor))) {
            this.tpMain.setSelectedComponent(this.pAbout);
        }
        LocalProperties.setProperty(propertyLastXiVersion, current.toString());
        fbXIFDRFocusLost(null);
    }

    private static int getJavaMajorVersion() {
        // get the version string from property
        String version = System.getProperty("java.version");
        
        // there was a change from 1.x.y to x.y
        if(version.startsWith("1.")) {
            // old version string
            version = version.substring(2, 3);
        } else {
            // new version string
            int dot = version.indexOf(".");
            if(dot != -1) { version = version.substring(0, dot); }
        } return Integer.parseInt(version);
        
    }

    private int startFDR(FDRInfo fdr) throws IOException, InterruptedException {
        final StatusMultiplex stat = new StatusMultiplex();
        stat.addInterface(new LoggingStatus());
        stat.addInterface(new TextBoxStatusInterface(txtRunState));        
        long startTime = Calendar.getInstance().getTimeInMillis();
        Process process = innerStartFDR(fdr, stat, true);
        
        int exitCode = process.waitFor();
        if (exitCode != 0 && Calendar.getInstance().getTimeInMillis()- startTime < 5000) {
            process = innerStartFDR(fdr, stat, false);

            exitCode = process.waitFor();
        }
        
        if (exitCode != 0) {
            stat.setStatus("Error while running FDR");
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    tpMain.setSelectedComponent(pFeedback);
                }
            });
            
        } else {
            stat.setStatus("xiFDR completed");
        }
        xifdrProcess = null;
        return exitCode;
            
    }

    protected Process innerStartFDR(FDRInfo fdr, final StatusMultiplex stat, boolean autoAddOpens) throws IOException {
        // setup the process
        ProcessBuilder builder = new ProcessBuilder();
        LinkedList<String> args = new LinkedList<>();
        // first argument is the same java executable used here
        args.add(Util.findJava());
        // -jar xiFDR.jar
        args.add("-Dfile.encoding=UTF-8");
        if (autoAddOpens && getJavaMajorVersion() >= 11) {
            args.add("--add-opens");
            args.add("java.base/java.lang=ALL-UNNAMED");
        }
        if (autoAddOpens && getJavaMajorVersion() >= 16) {
            args.add("--add-opens");
            args.add("java.base/java.util=ALL-UNNAMED");
        }
        if (autoAddOpens && getJavaMajorVersion() >= 9) {
            args.add("--add-opens");
            args.add("java.base/sun.reflect.annotation=ALL-UNNAMED");
        }
        args.add("-Xmx"+configProvider.getMemGB()+"G");
        args.add("-jar");
        args.add(fbXIFDR.getFile().getAbsolutePath());
        String fdrOutDir = new File(fdr.inputFile).getParent();
        String subDir="FDR";
        Locale xilocale = lpNumberLocale.getSelectLocale();
        args.add("--inputlocale=" + xilocale.toString());
        args.add("--outputlocale=" + xilocale.toString());
        // forward the desired FDRs
        if (fdr.psmFDR < 100) {
            args.add("--psmfdr=" + fdr.psmFDR);
            subDir += "_psmfdr" + fdr.psmFDR;
        }
        if (fdr.pepFDR < 100) {
            args.add("--pepfdr=" + fdr.pepFDR);
            subDir += "_pepfdr" + fdr.pepFDR;
        }
        if (fdr.protFDR < 100) {
            args.add("--proteinfdr=" + fdr.protFDR);
            subDir += "_proteinfdr" + fdr.protFDR;
        }
        if (fdr.linkFDR < 100) {
            args.add("--linkfdr=" + fdr.linkFDR);
            subDir += "_rpfdr" + fdr.linkFDR;
        }
        if (fdr.ppiFDR < 100) {
            args.add("--ppifdr=" + fdr.ppiFDR);
            subDir += "_ppifdr" + fdr.ppiFDR;
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
            if (fdr.boostBetween) {
                args.add("--boost-between");
            }
        }
        
        if (xifdr_version.compareTo(new Version("2.3.1")) >= 0 ) {
            args.add("--xiversion=" + XiVersion.getVersionString());
        }
        
        if (ckWriteMZID.isEnabled() && ckWriteMZID.isSelected()) {
            args.add("--writemzid");
        }
        if (txtMZIDLast.isEnabled() && txtMZIDLast.getText().trim().length()>0) {
            args.add("--mzidlast="+txtMZIDLast.getText());
        }
        if (txtMZIDAddress.isEnabled() && txtMZIDAddress.getText().trim().length()>0) {
            args.add("--mzidaddr="+txtMZIDAddress.getText());
        }
        if (txtMZIDFirst.isEnabled() && txtMZIDFirst.getText().trim().length()>0) {
            args.add("--mzidfirst="+txtMZIDFirst.getText());
        }
        if (txtMZIDEMail.isEnabled() && txtMZIDEMail.getText().trim().length()>0) {
            args.add("--mzidemail="+txtMZIDEMail.getText());
        }
        if (txtMZIDOrg.isEnabled() && txtMZIDOrg.getText().trim().length()>0) {
            args.add("--mzidorg="+txtMZIDOrg.getText());
        }
        
        
        // define the output
        String fdrOutBase = new File(fdr.inputFile).getName();
        if (fdrOutBase.toLowerCase().matches(".*\\.(csv|tsv|txt)")) {
            fdrOutBase = fdrOutBase.substring(0, fdrOutBase.length()-4);
        }
        
        File foutdir = new File(fdrOutDir + File.separator+ subDir);
        if (! foutdir.exists()) {
            foutdir.mkdir();
        }else if (!foutdir.isDirectory()) {
            int sid = 0;
            while ((!foutdir.exists()) || foutdir.isDirectory()) {
                foutdir = new File(fdrOutDir + File.separator+ subDir + sid);
            }
            if (! foutdir.exists()) {
                foutdir.mkdir();
            }
            subDir += sid;
        }

        args.add("--csvOutDir=" + fdrOutDir + File.separator+ subDir);
        args.add("--csvBaseName=" + fdrOutBase);
        args.add("--xiconfig=" + fdr.xiconfig);
        for (File f : fdr.fastas) {
            args.add("--fasta=" + f.getAbsolutePath());
        }
        if (fdr.showGui) {
            args.add("--gui" );
        }
        args.add(fdr.inputFile);
        builder.command(args);
        stat.setStatus("calling xiFDR: " +MyArrayUtils.toString(args, " "));
        File logFile = new File(fdrOutDir + File.separator+ subDir+File.separator+ fdrOutBase+ "_xiFDR.log");
        PrintWriter pwFDRLog = new PrintWriter(logFile);
        pwFDRLog.println("Runing xiFDR as :" + MyArrayUtils.toString(args," "));

        Process process = builder.start();
        xifdrProcess = process;
        final BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        final BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Thread fdrError = new Thread(new StreamToLog(error, pwFDRLog));
        Thread fdrOUT = new Thread(new StreamToLog(out, pwFDRLog));
        fdrError.setName("fdrErrorLog");
        fdrOUT.setName("fdrOutLog");
        fdrError.start();
        fdrOUT.start();
        return process;
    }

    private void startXiProcess() throws IOException, InterruptedException, URISyntaxException {
        final StatusMultiplex stat = new StatusMultiplex();
        stat.addInterface(new LoggingStatus());
        stat.addInterface(new TextBoxStatusInterface(txtRunState));        
        long startTime = Calendar.getInstance().getTimeInMillis();
        File thisJar = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        //if (!thisJar.getPath().toLowerCase().endsWith(".jar")) {
        //    thisJar = thisJar.getParentFile();
        //}
        
        innerStartXiSEARCH(System.getProperty("java.class.path"),8,stat, true);
            
    }
    
    protected void innerStartXiSEARCH(final String classpath, final int GB, final StatusMultiplex stat, final boolean autoAddOpens) throws IOException {
        // setup the process
        SimpleDateFormat logDate = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String configString;
        final FDRInfo fdr = new FDRInfo();
        try {
            configString = configProvider.getConfig();
        } catch (IOException ex) {
            Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        String configout = txtResultFile.getText().replaceAll("\\.[^\\.]*$", "") + ".config";
        FileWriter confWriter = new FileWriter(configout);
        confWriter.write(configString);
        confWriter.close();
        

        ArrayList<String> args = new ArrayList<>();
        // first argument is the same java executable used here
        args.add(Util.findJava());
        // -jar xiFDR.jar
        args.add("-Dfile.encoding=UTF-8");
        if (autoAddOpens && Util.getJavaMajorVersion() >= 11) {
            
            args.add("--add-opens");
            args.add("java.base/java.lang=ALL-UNNAMED");
        }
        args.add("-Xmx" + configProvider.getMemGB() + "g");

        args.add("-cp");
        args.add(classpath);
        args.add("rappsilber.applications.Xi");
        args.add("--config="+configout);

        if (ckPeakAnnotations.isSelected()){
            String peakout = txtPeakList.getText();

            if (peakout.toLowerCase().endsWith(".csv")||peakout.toLowerCase().endsWith(".gz")) {
                peakout = peakout.substring(0, peakout.length()-4) + 
                        XiVersion.getVersionString() + 
                        peakout.substring(peakout.length()-5,peakout.length());
            }

            if (peakout.trim().isEmpty()) {
                peakout = txtResultFile.getText().replaceAll("\\.[^\\.]*$", "") + ".annotations.tsv.gz";
            }
            args.add("--peaksout=" + peakout);
        }
        
        for (File f : flFASTAFiles.getFiles()){
            args.add("--fasta=" + f.getPath());
        }
        for (File f : flMSMFiles.getFiles()){
            args.add("--peaks=" + f.getPath());
        }
        args.add("--locale="+lpNumberLocale.getSelectLocale().toString());
        args.add("--output="+ txtResultFile.getText()); 
        //args.add("--log"); 

        Calendar c = Calendar.getInstance();
        String sdate = logDate.format(c.getTime());
        File statusexchange =  File.createTempFile("xiSearchStatus", sdate);
        final MemMapStatusControl readstatus = new MemMapStatusControl(statusexchange);
        this.memory2.setMemInfo(readstatus);
        readstatus.autoforwardStatus(stat);
        args.add("--statusexchange=" + statusexchange.getAbsolutePath());

        final ProcessLauncher launcher = new ProcessLauncher(args);
        xiSearchBackgroundProcess = launcher;
        Logger.getLogger(this.getClass().getName()).log(Level.INFO,"running: " + launcher.getCommandLine());
        
        ProcessLogger plout = null;
        try {
            String logFile = txtResultFile.getText().replaceAll("\\.[^\\.]*$", ".") + sdate+".log";
            plout = new ProcessLogger(logFile, true);
            plout.standardOutput("\n"+sdate+": Start Xi as " + MyArrayUtils.toString(args, " ") + "\n");
            launcher.addOutputListener(plout);
            launcher.addOutputListener(new ProcessLauncher.OutputListener() {
                StringBuilder sbErr = new StringBuilder();
                String gctext = "long time spend in garbage collection";
                String memexept  = "OutOfMemoryError";
                @Override
                public void standardOutput(char[] output) {
                }

                @Override
                public void errorOutput(char[] output) {
                    sbErr.append(output);
                    String serr = sbErr.toString();
                    if (serr.contains(gctext)) {
                        JOptionPane.showMessageDialog(SimpleXiGui.this, "xisearch is spending a lot of time in garbage collection!\n Probably running out of memory soon.", "GC time", JOptionPane.WARNING_MESSAGE);
                    }
                    if (serr.contains(memexept)) {
                        JOptionPane.showMessageDialog(SimpleXiGui.this, "Out Of Memmory Error detected!!\n\nTry to run with more memory or less threads!", "Out Of Memmory Error", JOptionPane.ERROR_MESSAGE);
                        launcher.abort();
                    }
                    if (sbErr.length() > gctext.length()) {
                        sbErr.delete(0, sbErr.length() - gctext.length()-1);
                    }
                }
                
                

                @Override
                public void close() {
                    
                }
            });
        } catch (FileNotFoundException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        launcher.addOutputListener(new ProcessLogToTextArea(txtLog));
        
        Runnable runnable = new Runnable() {
            public void run() {
                Long startTime  = Calendar.getInstance().getTimeInMillis();
                int exitCode = launcher.launch();
                readstatus.stopAutoforwardStatus();
                if (exitCode != 0) {
                    Long endTime = Calendar.getInstance().getTimeInMillis();
                    if (autoAddOpens && Util.getJavaMajorVersion() >= 11 && endTime - startTime < 10000) {
                        // died in less then 10 seconds - might be the --add-opens argument
                        // so try again
                        try {
                            SimpleXiGui.this.innerStartXiSEARCH(classpath, GB, stat, false);
                        } catch (IOException ex) {
                            Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        return;
                    } else {
                        stat.setStatus("Error while running xiSEARCH");
                    }
                } else {
                    SimpleXiGui.this.memory2.setMemInfo(null);

                    stat.setStatus("xiSEARCH completed");
                    if (fdr.runXiFDR) {
                        startXiFDR(fdr);
                    } else {
                        JOptionPane.showMessageDialog(rootPane, "xiSEARCH finished");
                    }

                }
                SimpleXiGui.this.memory2.setMemInfo(null);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tpMain.setSelectedComponent(pFeedback);
                        btnStartSearch1.setEnabled(true);
                        btnStartSearch2.setEnabled(true);
                        btnStartSearch.setEnabled(true);
                        cfgBasicConfig.setEnabled(true);
                        cfgTextConfig.setEnabled(true);

                        btnStop.setEnabled(false);
                        btnStop1.setEnabled(false);
                    }
                });
            }
        };
        new Thread(runnable,"xiSEARCH Process").start();
        
    }
    
    
    
    public void startRun() {
//        String msmFile = txtMSMFile.getText();
        final Locale numberlocale = lpNumberLocale.getSelectLocale();
        final String csvOut = txtResultFile.getText();
        String c;
        try {
            c = configProvider.getConfig();
        } catch (IOException ex) {
            Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
            return;
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

                        if (peakout.trim().isEmpty()) {
                            peakout = txtResultFile.getText().replaceAll("\\.[^\\.]*$", "") + ".annotations.tsv.gz";
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
                final Timer tt = new Timer("transfer xiprocess");
                tt.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (xirunner.xi != null) {
                            this.cancel();
                            SimpleXiGui.this.threadAdjust.setXiProcess(xirunner.xi);
                        }
                    }
                }, 1000, 1000);
                
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
        bgConfig = new javax.swing.ButtonGroup();
        cfgBasicConfig = new rappsilber.gui.components.config.BasicConfig();
        cfgTextConfig = new rappsilber.gui.components.config.TextConfig();
        txtPeakList = new rappsilber.gui.components.FileBrowser();
        ckWriteAnnotations = new javax.swing.JCheckBox();
        btnStartSearch = new javax.swing.JButton();
        btnStop = new javax.swing.JButton();
        txtRunState = new javax.swing.JTextField();
        jScrollPane3 = new javax.swing.JScrollPane();
        tpMain = new javax.swing.JTabbedPane();
        pRun = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        txtResultFile = new rappsilber.gui.components.FileBrowser();
        jPanel1 = new javax.swing.JPanel();
        flMSMFiles = new rappsilber.gui.components.FileList();
        jPanel2 = new javax.swing.JPanel();
        flFASTAFiles = new rappsilber.gui.components.FileList();
        pConfig = new javax.swing.JPanel();
        btnStartSearch1 = new javax.swing.JButton();
        lpNumberLocale = new rappsilber.gui.components.LocalPicker();
        jLabel1 = new javax.swing.JLabel();
        btnStop1 = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        rbBasicConfig = new javax.swing.JRadioButton();
        rbTextConfig = new javax.swing.JRadioButton();
        jPanel7 = new javax.swing.JPanel();
        spConfig = new javax.swing.JScrollPane();
        jPanel8 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jPanel4 = new javax.swing.JPanel();
        ckFDR = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        fbXIFDR = new rappsilber.gui.components.FileBrowser();
        spPsmFDR = new javax.swing.JSpinner();
        lblMaxFDR = new javax.swing.JLabel();
        lblPsmFDR = new javax.swing.JLabel();
        lblPepFDR = new javax.swing.JLabel();
        spPepFDR = new javax.swing.JSpinner();
        spLinkFDR = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        spPPIFdr = new javax.swing.JSpinner();
        cbBoost = new javax.swing.JComboBox<>();
        ckFDRGUI = new javax.swing.JCheckBox();
        ckBoost = new javax.swing.JCheckBox();
        ckWriteMZID = new javax.swing.JCheckBox();
        jLabel8 = new javax.swing.JLabel();
        txtMZIDFirst = new javax.swing.JTextField();
        txtMZIDLast = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        txtMZIDEMail = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        txtMZIDOrg = new javax.swing.JTextField();
        spMZIDFirstAddress = new javax.swing.JScrollPane();
        txtMZIDAddress = new javax.swing.JTextArea();
        jLabel13 = new javax.swing.JLabel();
        ckBoostBetween = new javax.swing.JCheckBox();
        btnStartFDR = new javax.swing.JButton();
        btnStopFDR = new javax.swing.JButton();
        fbLoadConfig = new rappsilber.gui.components.FileBrowser();
        btnLoadConfig = new javax.swing.JButton();
        btnAddConfig = new javax.swing.JButton();
        btnLoadDB = new javax.swing.JButton();
        pTemplate = new javax.swing.JPanel();
        lblTemplate = new javax.swing.JLabel();
        btnTemplateLoad = new javax.swing.JButton();
        cbtemplatesWide = new rappsilber.gui.components.WideComboBox();
        ckPeakAnnotations = new javax.swing.JCheckBox();
        btnStartSearch2 = new javax.swing.JButton();
        pFeedback = new javax.swing.JPanel();
        memory2 = new org.rappsilber.gui.components.memory.Memory();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtLog = new javax.swing.JTextArea();
        cmbLogLevel = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        callBackSettings1 = new rappsilber.gui.components.CallBackSettings();
        feedBack1 = new rappsilber.gui.components.FeedBack();
        threadAdjust = new rappsilber.gui.components.ThreadAdjust();
        pAbout = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        txtVersion = new javax.swing.JTextField();
        spChangeLog = new javax.swing.JScrollPane();
        txtChangeLog = new javax.swing.JTextArea();

        spProteinFDR.setModel(new javax.swing.SpinnerNumberModel(100.0d, 0.0d, null, 1.0d));
        spProteinFDR.setToolTipText("FDR value accepted for protein-groups");

        lblProteinGroup.setText("Protein Group");

        txtPeakList.setEnabled(false);

        ckWriteAnnotations.setText("Write Peak Annotation");
        ckWriteAnnotations.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckWriteAnnotationsActionPerformed(evt);
            }
        });

        btnStartSearch.setText("Start");
        btnStartSearch.setToolTipText("Start the search");
        btnStartSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartSearchActionPerformed(evt);
            }
        });

        btnStop.setText("Stop");
        btnStop.setToolTipText("Stop the current search");
        btnStop.setEnabled(false);
        btnStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopActionPerformed(evt);
            }
        });

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("xiSEARCH");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        txtRunState.setEditable(false);
        txtRunState.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtRunStateActionPerformed(evt);
            }
        });

        tpMain.setToolTipText("");

        jLabel6.setText("Results");
        jLabel6.setToolTipText("The search-result will be writen into this file");

        txtResultFile.setToolTipText("The search-result will be writen into this file");

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Peak List"));

        flMSMFiles.setToolTipText("The peak lists that should be searched");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(flMSMFiles, javax.swing.GroupLayout.DEFAULT_SIZE, 975, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(flMSMFiles, javax.swing.GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE)
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
                .addComponent(flFASTAFiles, javax.swing.GroupLayout.DEFAULT_SIZE, 975, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(flFASTAFiles, javax.swing.GroupLayout.DEFAULT_SIZE, 213, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout pRunLayout = new javax.swing.GroupLayout(pRun);
        pRun.setLayout(pRunLayout);
        pRunLayout.setHorizontalGroup(
            pRunLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pRunLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pRunLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(pRunLayout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtResultFile, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        pRunLayout.setVerticalGroup(
            pRunLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pRunLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(pRunLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtResultFile, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addContainerGap())
        );

        tpMain.addTab("Files", pRun);

        btnStartSearch1.setText("Start");
        btnStartSearch1.setToolTipText("Start the search");
        btnStartSearch1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartSearchActionPerformed(evt);
            }
        });

        lpNumberLocale.setToolTipText("Defines how numbers are writen out to the result file.");
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

        bgConfig.add(rbBasicConfig);
        rbBasicConfig.setSelected(true);
        rbBasicConfig.setText("Basic Config");
        rbBasicConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbBasicConfigActionPerformed(evt);
            }
        });

        bgConfig.add(rbTextConfig);
        rbTextConfig.setText("Advanced Config");
        rbTextConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbBasicConfigActionPerformed(evt);
            }
        });

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("Search Settigns"));

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spConfig)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spConfig, javax.swing.GroupLayout.Alignment.TRAILING)
        );

        jPanel8.setBorder(javax.swing.BorderFactory.createTitledBorder("FDR Settings"));

        ckFDR.setSelected(true);
        ckFDR.setText("Do FDR");
        ckFDR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckFDRActionPerformed(evt);
            }
        });

        jLabel2.setText("xiFDR");

        fbXIFDR.setToolTipText("The xiFDR version to use");
        fbXIFDR.setDescription("JAR-File");
        fbXIFDR.setExtensions(new String[] {"/xifdr.*jar/"});
        fbXIFDR.setPreferredSize(new java.awt.Dimension(40, 25));
        fbXIFDR.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                fbXIFDRFocusLost(evt);
            }
        });

        spPsmFDR.setModel(new javax.swing.SpinnerNumberModel(100.0d, 0.0d, null, 1.0d));
        spPsmFDR.setToolTipText("FDR value accepted for PSMs");

        lblMaxFDR.setText("Max FDRs");

        lblPsmFDR.setText("CSM");

        lblPepFDR.setText("Peptide Pair");

        spPepFDR.setModel(new javax.swing.SpinnerNumberModel(100.0d, 0.0d, null, 1.0d));
        spPepFDR.setToolTipText("FDR value accepted for Peptide Pairs (including linksite within the peptide)");

        spLinkFDR.setModel(new javax.swing.SpinnerNumberModel(5.0d, 0.0d, null, 1.0d));
        spLinkFDR.setToolTipText("FDR value accepted for unique residue pairs (links)");

        jLabel4.setText("Residue Pairs");

        jLabel10.setText("Protein Pairs");

        spPPIFdr.setModel(new javax.swing.SpinnerNumberModel(100.0d, 0.0d, null, 1.0d));
        spPPIFdr.setToolTipText("FDR value accepted for protein-pairs");

        cbBoost.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Peptide Pairs", "Residue Pairs", "Protein Pairs" }));
        cbBoost.setSelectedIndex(1);
        cbBoost.setToolTipText("Filter results to maximize this level");

        ckFDRGUI.setText("seperate GUI");
        ckFDRGUI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckFDRGUIActionPerformed(evt);
            }
        });

        ckBoost.setSelected(true);
        ckBoost.setText("boost");
        ckBoost.setToolTipText("Apply prefilter to maximize the results ata fixed confidence");
        ckBoost.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckBoostActionPerformed(evt);
            }
        });

        ckWriteMZID.setSelected(true);
        ckWriteMZID.setText("Write mzIdentML");
        ckWriteMZID.setToolTipText("Additionally to the generated FDR-CSV-files also an mzIdentML can be generated");
        ckWriteMZID.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckWriteMZIDActionPerformed(evt);
            }
        });

        jLabel8.setText("First Name");

        txtMZIDFirst.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtMZIDFirstActionPerformed(evt);
            }
        });

        txtMZIDLast.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtMZIDFirstActionPerformed(evt);
            }
        });

        jLabel9.setText("Last Name");

        txtMZIDEMail.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtMZIDFirstActionPerformed(evt);
            }
        });

        jLabel11.setText("E-Mail");

        jLabel12.setText("Organisation");

        txtMZIDOrg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtMZIDFirstActionPerformed(evt);
            }
        });

        txtMZIDAddress.setColumns(20);
        txtMZIDAddress.setRows(5);
        txtMZIDAddress.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtMZIDAddressFocusLost(evt);
            }
        });
        spMZIDFirstAddress.setViewportView(txtMZIDAddress);

        jLabel13.setText("Address");

        ckBoostBetween.setSelected(true);
        ckBoostBetween.setText("boost protein heteromeric");
        ckBoostBetween.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckBoostBetweenActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addComponent(fbXIFDR, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(12, 12, 12))
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel13)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(113, 113, 113)
                        .addComponent(lblMaxFDR))
                    .addComponent(ckWriteMZID)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(ckFDR)
                        .addGap(18, 18, 18)
                        .addComponent(ckFDRGUI))
                    .addComponent(jLabel2))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(spMZIDFirstAddress)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(ckBoostBetween, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel9)
                            .addComponent(jLabel11)
                            .addComponent(jLabel12)
                            .addComponent(jLabel10)
                            .addComponent(jLabel8)
                            .addComponent(lblPepFDR)
                            .addComponent(lblPsmFDR)
                            .addComponent(ckBoost)
                            .addComponent(jLabel4))
                        .addGap(12, 12, 12)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtMZIDOrg)
                            .addComponent(txtMZIDEMail)
                            .addComponent(txtMZIDLast)
                            .addComponent(txtMZIDFirst)
                            .addComponent(cbBoost, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(spPPIFdr)
                            .addComponent(spLinkFDR)
                            .addComponent(spPepFDR)
                            .addComponent(spPsmFDR))))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ckFDR)
                    .addComponent(ckFDRGUI))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fbXIFDR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblMaxFDR)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spPsmFDR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPsmFDR))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spPepFDR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPepFDR))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spLinkFDR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spPPIFdr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cbBoost, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ckBoost))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(ckBoostBetween)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ckWriteMZID)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(txtMZIDFirst, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtMZIDLast, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtMZIDEMail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtMZIDOrg, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spMZIDFirstAddress, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                .addContainerGap(19, Short.MAX_VALUE))
        );

        jScrollPane2.setViewportView(jPanel4);

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

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addComponent(btnStartFDR)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnStopFDR)
                .addContainerGap())
            .addComponent(jScrollPane2)
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 329, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnStopFDR)
                    .addComponent(btnStartFDR))
                .addContainerGap())
        );

        fbLoadConfig.setExtensions(new String[] {"txt"});

        btnLoadConfig.setText("Load  Config");
        btnLoadConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLoadConfigActionPerformed(evt);
            }
        });

        btnAddConfig.setText("Add Config");
        btnAddConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddConfigActionPerformed(evt);
            }
        });

        btnLoadDB.setText("Load From DB");
        btnLoadDB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLoadDBActionPerformed(evt);
            }
        });

        pTemplate.setBorder(javax.swing.BorderFactory.createTitledBorder("Templates"));

        lblTemplate.setText("Available templates:");
        lblTemplate.setToolTipText("templates are predefined configs. You can use these as a guideline on how to configure a search");

        btnTemplateLoad.setText("Load Template");
        btnTemplateLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTemplateLoadActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pTemplateLayout = new javax.swing.GroupLayout(pTemplate);
        pTemplate.setLayout(pTemplateLayout);
        pTemplateLayout.setHorizontalGroup(
            pTemplateLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pTemplateLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(btnTemplateLoad))
            .addGroup(pTemplateLayout.createSequentialGroup()
                .addComponent(lblTemplate)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(cbtemplatesWide, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        pTemplateLayout.setVerticalGroup(
            pTemplateLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pTemplateLayout.createSequentialGroup()
                .addComponent(lblTemplate)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cbtemplatesWide, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnTemplateLoad))
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(rbBasicConfig)
                .addGap(35, 35, 35)
                .addComponent(rbTextConfig)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(fbLoadConfig, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnLoadConfig)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnAddConfig)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnLoadDB))
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pTemplate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(rbTextConfig)
                    .addComponent(rbBasicConfig)
                    .addComponent(fbLoadConfig, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnLoadConfig)
                    .addComponent(btnAddConfig)
                    .addComponent(btnLoadDB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(pTemplate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );

        ckPeakAnnotations.setText("write peak annotations");
        ckPeakAnnotations.setToolTipText("Write out a tab separated file containing all the peak-annotations (mostly for debuging)");
        ckPeakAnnotations.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckPeakAnnotationsActionPerformed(evt);
            }
        });

        btnStartSearch2.setText("Start");
        btnStartSearch2.setToolTipText("Start the search");
        btnStartSearch2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartSearch2btnStartSearchActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pConfigLayout = new javax.swing.GroupLayout(pConfig);
        pConfig.setLayout(pConfigLayout);
        pConfigLayout.setHorizontalGroup(
            pConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pConfigLayout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lpNumberLocale, javax.swing.GroupLayout.PREFERRED_SIZE, 268, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(ckPeakAnnotations)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnStartSearch2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnStartSearch1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnStop1)
                .addGap(3, 3, 3))
            .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        pConfigLayout.setVerticalGroup(
            pConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pConfigLayout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnStop1)
                    .addComponent(btnStartSearch1)
                    .addComponent(lpNumberLocale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(ckPeakAnnotations)
                    .addComponent(btnStartSearch2))
                .addContainerGap())
        );

        tpMain.addTab("Parameters", pConfig);

        jSplitPane1.setDividerLocation(200);

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

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cmbLogLevel, 0, 162, Short.MAX_VALUE))
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmbLogLevel, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 490, Short.MAX_VALUE))
        );

        jSplitPane1.setLeftComponent(jPanel5);

        feedBack1.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Feedback"));

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(feedBack1, javax.swing.GroupLayout.DEFAULT_SIZE, 798, Short.MAX_VALUE)
            .addComponent(callBackSettings1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addComponent(callBackSettings1, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(feedBack1, javax.swing.GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE))
        );

        jSplitPane1.setRightComponent(jPanel6);

        javax.swing.GroupLayout pFeedbackLayout = new javax.swing.GroupLayout(pFeedback);
        pFeedback.setLayout(pFeedbackLayout);
        pFeedbackLayout.setHorizontalGroup(
            pFeedbackLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pFeedbackLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pFeedbackLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pFeedbackLayout.createSequentialGroup()
                        .addComponent(memory2, javax.swing.GroupLayout.DEFAULT_SIZE, 794, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(threadAdjust, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jSplitPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );
        pFeedbackLayout.setVerticalGroup(
            pFeedbackLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pFeedbackLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pFeedbackLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(memory2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(threadAdjust, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSplitPane1)
                .addContainerGap())
        );

        tpMain.addTab("Feedback", pFeedback);

        jLabel3.setText("xiSEARCH Version:");

        txtChangeLog.setColumns(20);
        txtChangeLog.setRows(5);
        spChangeLog.setViewportView(txtChangeLog);

        javax.swing.GroupLayout pAboutLayout = new javax.swing.GroupLayout(pAbout);
        pAbout.setLayout(pAboutLayout);
        pAboutLayout.setHorizontalGroup(
            pAboutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pAboutLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pAboutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(spChangeLog)
                    .addGroup(pAboutLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addGap(1, 1, 1)
                        .addComponent(txtVersion, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 746, Short.MAX_VALUE)))
                .addContainerGap())
        );
        pAboutLayout.setVerticalGroup(
            pAboutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pAboutLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pAboutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txtVersion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spChangeLog, javax.swing.GroupLayout.DEFAULT_SIZE, 535, Short.MAX_VALUE)
                .addContainerGap())
        );

        tpMain.addTab("About", pAbout);

        jScrollPane3.setViewportView(tpMain);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(txtRunState)
            .addComponent(jScrollPane3)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jScrollPane3)
                .addGap(0, 0, 0)
                .addComponent(txtRunState, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnStartSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStartSearchActionPerformed

        btnStartSearch2btnStartSearchActionPerformed(evt);

    }//GEN-LAST:event_btnStartSearchActionPerformed

    private void btnStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopActionPerformed
        if (xirunner != null) {
            xirunner.stop();
            xirunner = null;
        }
        if (xiSearchBackgroundProcess != null) {
            xiSearchBackgroundProcess.abort();
            xiSearchBackgroundProcess = null;
        }
        
        btnStop.setEnabled(false);
        btnStop1.setEnabled(false);
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
        //fbXIFDR.setEnabled(en);
        fbXIFDRFocusLost(null);
        
        btnStartFDR.setEnabled(search_done);
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

    private void rbBasicConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbBasicConfigActionPerformed
        if (rbBasicConfig.isSelected()) {
            spConfig.setViewportView(cfgBasicConfig);
            configProvider = cfgBasicConfig;
        }
        if (rbTextConfig.isSelected()) {
            spConfig.setViewportView(cfgTextConfig);
            configProvider = cfgTextConfig;
        }
        
    }//GEN-LAST:event_rbBasicConfigActionPerformed

    private void btnLoadConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLoadConfigActionPerformed
        File f = fbLoadConfig.getFile();
        if (f != null) {
            (configProvider).loadConfig(fbLoadConfig.getFile(),false);
        }
    }//GEN-LAST:event_btnLoadConfigActionPerformed

    private void btnAddConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddConfigActionPerformed
        File f = fbLoadConfig.getFile();
        if (f != null) {
            (configProvider).loadConfig(fbLoadConfig.getFile(),true);
        }
    }//GEN-LAST:event_btnAddConfigActionPerformed

    private void btnLoadDBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLoadDBActionPerformed
        final LoadDBConfig ldbc = new LoadDBConfig(this, true);
        ldbc.setResponse(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                LoadDBConfig lc = ldbc;
                try {
                    // I assume that by default people will not want all matches - but only topranking ones duing offline search
                    // and the number of cpus is rather the user can define themself as well
                    HashSet<String> ignore = new HashSet<>(RArrayUtils.toCollection(new String[] {"TOPMATCHESONLY", "USECPUS"}));
                    SimpleXiGui.this.cfgBasicConfig.loadConfig(ldbc.getConfig(), false, ignore);
                    // I assume that by default people will not want all matches - but only topranking ones duing offline search
                    ignore = new HashSet<>(RArrayUtils.toCollection(new String[] {"TOPMATCHESONLY"}));
                    SimpleXiGui.this.cfgTextConfig.loadConfig(ldbc.getConfig(), false, ignore);
                    SimpleXiGui.this.cfgTextConfig.loadConfig("TOPMATCHESONLY:true", true, null);
                } catch (IOException ex) {
                    Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        ldbc.setVisible(true);
    }//GEN-LAST:event_btnLoadDBActionPerformed

    private void btnTemplateLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTemplateLoadActionPerformed
        String filename = cbtemplatesWide.getSelectedItem().toString();
        File filesource = Util.getFileRelative("templates"+File.separator+filename + ".conf", true);
        if (filesource==null && filename.contains(" -- ")) {
            filename = filename.substring(0,filename.indexOf(" -- "));
            filesource = Util.getFileRelative("templates"+File.separator+filename + ".conf", true);
        }
        configProvider.loadConfig(filesource, false);
            

    }//GEN-LAST:event_btnTemplateLoadActionPerformed

    private void ckWriteAnnotationsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ckWriteAnnotationsActionPerformed
    }//GEN-LAST:event_ckWriteAnnotationsActionPerformed

    private void btnStartSearch2btnStartSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStartSearch2btnStartSearchActionPerformed
        try {
            startXiProcess();
            btnStartSearch1.setEnabled(false);
            btnStartSearch2.setEnabled(false);
            btnStartSearch.setEnabled(false);
            cfgBasicConfig.setEnabled(false);
            cfgTextConfig.setEnabled(false);
            
            btnStop.setEnabled(true);
            btnStop1.setEnabled(true);
        } catch (IOException ex) {
            Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
        }

    }//GEN-LAST:event_btnStartSearch2btnStartSearchActionPerformed

    private void ckFDRGUIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ckFDRGUIActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_ckFDRGUIActionPerformed

    private void txtRunStateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtRunStateActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtRunStateActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        if (xirunner != null || xiSearchBackgroundProcess != null) {
            if (JOptionPane.showConfirmDialog(this, "You are sure you want to quit?","Stop Search?", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                if (xiSearchBackgroundProcess != null) {
                    xiSearchBackgroundProcess.abort();
                }
                setDefaultCloseOperation(EXIT_ON_CLOSE);
            } else {
                setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            }
        }
    }//GEN-LAST:event_formWindowClosing

    private void fbXIFDRFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_fbXIFDRFocusLost
        String xiFDR = fbXIFDR.getText();
        boolean mzEn = ckFDR.isSelected() && !(xiFDR.contains("-1.") |
                xiFDR.contains("-2.1"));
        if (!xiFDR.isEmpty()) {
            Pattern p  = Pattern.compile(".*-([0-9]\\.[0-9]*[^\\-]*).*.jar");
            Matcher m = p.matcher(xiFDR);
            if (m.matches()) {
                this.xifdr_version = new Version(m.group(1));
            }
        }
        ckWriteMZID.setEnabled(mzEn);
        mzEn &= ckWriteMZID.isSelected();
        txtMZIDAddress.setEditable(mzEn);
        txtMZIDEMail.setEditable(mzEn);
        txtMZIDFirst.setEditable(mzEn);
        txtMZIDLast.setEditable(mzEn);
        txtMZIDOrg.setEditable(mzEn);

    }//GEN-LAST:event_fbXIFDRFocusLost

    private void txtMZIDFirstActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtMZIDFirstActionPerformed
        if (txtMZIDFirst.getText().trim().length()>0) {
            LocalProperties.setProperty(propertyFirst,txtMZIDFirst.getText());
        }
        if (txtMZIDLast.getText().trim().length()>0) {
            LocalProperties.getProperty(propertyLast,txtMZIDLast.getText());
        }
        if (txtMZIDEMail.getText().trim().length()>0) {
            LocalProperties.getProperty(propertyEMail,txtMZIDEMail.getText());
        }
        if (txtMZIDAddress.getText().trim().length()>0) {
            LocalProperties.getProperty(propertyAddress,txtMZIDAddress.getText());
        }
        if (txtMZIDOrg.getText().trim().length()>0) {
            LocalProperties.getProperty(propertyOrg,txtMZIDOrg.getText());
        }
    }//GEN-LAST:event_txtMZIDFirstActionPerformed

    private void txtMZIDAddressFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtMZIDAddressFocusLost
        txtMZIDFirstActionPerformed(null);
    }//GEN-LAST:event_txtMZIDAddressFocusLost

    private void ckWriteMZIDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ckWriteMZIDActionPerformed
        fbXIFDRFocusLost(null);
    }//GEN-LAST:event_ckWriteMZIDActionPerformed

    private void ckBoostBetweenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ckBoostBetweenActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_ckBoostBetweenActionPerformed

    private void ckBoostActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ckBoostActionPerformed
        cbBoost.setEnabled(ckBoost.isSelected());
        ckBoostBetween.setEnabled(ckBoost.isSelected());
    }//GEN-LAST:event_ckBoostActionPerformed

    protected void startXiFDR(final FDRInfo info) {

        
        final Runnable runnable = new Runnable() {
            public void run() {
                int rv = 0;
                try {
                    rv = startFDR(info);
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
                if (rv == 0) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            JOptionPane.showMessageDialog(SimpleXiGui.this, "xiFDR Finished.");
                        }
                    });
                } else {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            JOptionPane.showMessageDialog(SimpleXiGui.this, "Error during FDR calculation.");
                        }
                    });
                    
                }

            }
        };
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                btnStartFDR.setEnabled(false);
                btnStopFDR.setEnabled(true);
                Thread xiFDRThread = new Thread(runnable);
                xiFDRThread.setName("xiFDR");
                xiFDRThread.start();
            }
        });        
    }


    public void setConfigFile(String f) {
        
        
        cfgTextConfig.loadConfig(new File(f),false);
        rbTextConfig.setEnabled(true);
        rbBasicConfigActionPerformed(null);

    }

    public void addConfigFile(String f) {
        
        cfgTextConfig.loadConfig(new File(f),true);
        rbTextConfig.setEnabled(true);
        rbBasicConfigActionPerformed(null);

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
    
//    public void loadConfig(File f, boolean append) {
//        //File f = btnLoadConfig.getFile();
//        StringBuffer config = new StringBuffer();
//        try {
//            if (f!= null) {
//                BufferedReader confIn = new BufferedReader(new FileReader(f));
//                String line;
//                while ((line = confIn.readLine()) != null) {
//                    config.append(line);
//                    config.append("\n");
//                }
//                confIn.close();
//                if (append)
//                    txtConfig.append(config.toString());
//                else 
//                    txtConfig.setText(config.toString());
//            }
//        }catch (IOException ioe) {
//            System.err.println(ioe);
//        }        
//    }
    
    public void appendConfigLine(String line) {
        cfgTextConfig.appendConfig(line);
        rbTextConfig.setEnabled(true);
        rbBasicConfigActionPerformed(null);
    }
    
    
    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new StartGui(args));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bgConfig;
    private javax.swing.ButtonGroup bgFDROptimized;
    private javax.swing.JButton btnAddConfig;
    private javax.swing.JButton btnLoadConfig;
    private javax.swing.JButton btnLoadDB;
    private javax.swing.JButton btnStartFDR;
    private javax.swing.JButton btnStartSearch;
    private javax.swing.JButton btnStartSearch1;
    private javax.swing.JButton btnStartSearch2;
    private javax.swing.JButton btnStop;
    private javax.swing.JButton btnStop1;
    private javax.swing.JButton btnStopFDR;
    private javax.swing.JButton btnTemplateLoad;
    private rappsilber.gui.components.CallBackSettings callBackSettings1;
    private javax.swing.JComboBox<String> cbBoost;
    private rappsilber.gui.components.WideComboBox cbtemplatesWide;
    private rappsilber.gui.components.config.BasicConfig cfgBasicConfig;
    private rappsilber.gui.components.config.TextConfig cfgTextConfig;
    private javax.swing.JCheckBox ckBoost;
    private javax.swing.JCheckBox ckBoostBetween;
    private javax.swing.JCheckBox ckFDR;
    private javax.swing.JCheckBox ckFDRGUI;
    private javax.swing.JCheckBox ckPeakAnnotations;
    private javax.swing.JCheckBox ckWriteAnnotations;
    private javax.swing.JCheckBox ckWriteMZID;
    private javax.swing.JComboBox cmbLogLevel;
    private rappsilber.gui.components.FileBrowser fbLoadConfig;
    private rappsilber.gui.components.FileBrowser fbXIFDR;
    private rappsilber.gui.components.FeedBack feedBack1;
    private rappsilber.gui.components.FileList flFASTAFiles;
    public rappsilber.gui.components.FileList flMSMFiles;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JLabel lblMaxFDR;
    private javax.swing.JLabel lblPepFDR;
    private javax.swing.JLabel lblProteinGroup;
    private javax.swing.JLabel lblPsmFDR;
    private javax.swing.JLabel lblTemplate;
    private rappsilber.gui.components.LocalPicker lpNumberLocale;
    private org.rappsilber.gui.components.memory.Memory memory2;
    private javax.swing.JPanel pAbout;
    private javax.swing.JPanel pConfig;
    private javax.swing.JPanel pFeedback;
    private javax.swing.JPanel pRun;
    private javax.swing.JPanel pTemplate;
    private javax.swing.JRadioButton rbBasicConfig;
    private javax.swing.JRadioButton rbTextConfig;
    private javax.swing.JScrollPane spChangeLog;
    private javax.swing.JScrollPane spConfig;
    private javax.swing.JSpinner spLinkFDR;
    private javax.swing.JScrollPane spMZIDFirstAddress;
    private javax.swing.JSpinner spPPIFdr;
    private javax.swing.JSpinner spPepFDR;
    private javax.swing.JSpinner spProteinFDR;
    private javax.swing.JSpinner spPsmFDR;
    public rappsilber.gui.components.ThreadAdjust threadAdjust;
    private javax.swing.JTabbedPane tpMain;
    private javax.swing.JTextArea txtChangeLog;
    private javax.swing.JTextArea txtLog;
    private javax.swing.JTextArea txtMZIDAddress;
    private javax.swing.JTextField txtMZIDEMail;
    private javax.swing.JTextField txtMZIDFirst;
    private javax.swing.JTextField txtMZIDLast;
    private javax.swing.JTextField txtMZIDOrg;
    private rappsilber.gui.components.FileBrowser txtPeakList;
    private rappsilber.gui.components.FileBrowser txtResultFile;
    private javax.swing.JTextField txtRunState;
    private javax.swing.JTextField txtVersion;
    // End of variables declaration//GEN-END:variables

}
