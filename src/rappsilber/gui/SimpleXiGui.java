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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import rappsilber.applications.SimpleXiProcess;
import rappsilber.applications.SimpleXiProcessLinearIncluded;
import rappsilber.applications.XiProcess;
import rappsilber.utils.XiProvider;
import rappsilber.config.LocalProperties;
import rappsilber.config.RunConfig;
import rappsilber.config.RunConfigFile;
import rappsilber.gui.components.GenericTextPopUpMenu;
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
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class SimpleXiGui extends javax.swing.JFrame {
    private static final long serialVersionUID = -8719056362190680024L;

    JTextAreaHandle loggingHandle;
    RunXi xirunner = null;
    GenericTextPopUpMenu gtpm;



    private class WaitForXi implements Runnable {
        XiProcess m_xi;
        JButton m_enable = null;

        public WaitForXi(XiProcess xip, JButton toEnable) {
            m_xi = xip;
            m_enable = toEnable;
            m_xi.prepareSearch();

        }

        public void run() {
            m_xi.startSearch();
            m_xi.waitEnd();
            m_enable.setEnabled(true);
            if (m_xi instanceof SimpleXiProcess)
                Logger.getLogger(SimpleXiGui.class.getName()).log(Level.INFO, ((SimpleXiProcess) m_xi).ScoreStatistic());
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
        String fastaFile;
        AbstractMSMAccess input;
        ResultWriter output;
        RunConfig conf;
//        SimpleXiProcessDev xi;
        XiProcess xi;

        protected RunXi(String fastaFile, AbstractMSMAccess input, ResultWriter output, RunConfig conf) {
            this.fastaFile = fastaFile;
            this.input = input;
            this.output = output;
            this.conf = conf;
            //System.out.print(xi.ScoreStatistic());
        }

        public void run() {
            btnStartSearch.setEnabled(false);

            StackedSpectraAccess sc = null;

            if (conf.retrieveObject("enableIndicatorPeaks", false)){
                sc = new BS3ReporterIonPeaksFilteredSpectrumAccess();
                sc.setReader(input);
            }


            StackedSpectraAccess cal = linearCalibration1.getCalibration();
            if (cal != null) {
                if (sc == null)
                    cal.setReader(input);
                else
                    cal.setReader(sc);
                sc = cal;
            }
            


            try {
                if (input.getSpectraCount() <= 0) {
                    input.gatherData(4);
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            }


            conf.storeObject("SQLBUFFER", "100");



            ResultMultiplexer result_multiplexer = new ResultMultiplexer();


            result_multiplexer.addResultWriter(output);
            try {
                SequenceList seq = new SequenceList(conf);
                if (fastaFile != null) {
                    seq.addFasta(new File(fastaFile));
                }
                File[] fastas = flFASTAFiles.getFiles();
                for (int f = 0 ; f< fastas.length; f++) {
                    boolean d = flFASTAFiles.isSelected(f);
                    if (d)
                        seq.addFasta(fastas[f], SequenceList.DECOY_GENERATION.ISDECOY);
                    else
                        seq.addFasta(fastas[f], SequenceList.DECOY_GENERATION.ISTARGET);                        
                }
                
                xi = XiProvider.getXiSearch(seq,input, output, sc, conf, SimpleXiProcessLinearIncluded.class);

            } catch (IOException ex) {
                Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
            }
//            xi = getXiSearch(result_multiplexer, sc, conf, SimpleXiProcessLinearIncluded.class);


            //

            xi.addFilter(new SetRunIDFilter());


            new Thread(new WaitForXi(xi, btnStartSearch)).start();


        }


        public void stop() {
            xi.stop();
        }



    }

    /** Creates new form SimpleXiGui */
    public SimpleXiGui() {
        initComponents();


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
        txtPeakList.setExtensions(new String[]{".csv"});
        txtPeakList.setDescription("PeakList as CSV-file");
        txtPeakList.setSave();

        txtResultFile.setLocalPropertyKey("CSV_RESULT");
        txtResultFile.setExtensions(new String[]{".csv"});
        txtResultFile.setDescription("Result as CSV-file");
        txtResultFile.setSave();

        fbSaveConfig.setLocalPropertyKey("XLink_Config");
        fbSaveConfig.setExtensions(new String[]{".conf",".txt"});
        fbSaveConfig.setDescription("config-files");

        fbLoadConfig.setLocalPropertyKey("XLink_Config");
        fbLoadConfig.setExtensions(new String[]{".conf",".txt"});
        fbLoadConfig.setDescription("config-files");

        
        try {
            BufferedReader br = Util.readFromClassPath(".rappsilber.data.DefaultConfig.conf");
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = br.readLine()) != null)
                sb.append(line + "\n");
            br.close();
            txtConfig.setText(sb.toString());

        } catch (IOException ex) {
            Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
        }

        gtpm = new GenericTextPopUpMenu();
        gtpm.installContextMenu(this);


    }



    public void startRun() {
//        String msmFile = txtMSMFile.getText();
       
        Runnable runnable = new Runnable() {
            public void run() {
                File msm = null;
                String fastaFile = null;
                String configFile = txtConfig.getText();
                String csvOut = txtResultFile.getText();
                RunConfig conf;
                XiProcess xi;

//        if (txtFastaFile.getFile() != null)
//            fastaFile = txtFastaFile.getText();
                try {
                    conf = new RunConfigFile(new StringReader(configFile));
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, "Error while reading config file " + configFile, ex);
                    return;
                } catch (IOException ex) {
                    Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, "Error while reading config file " + configFile, ex);
                    return;
                } catch (ParseException ex) {
                    Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, "Error while reading config file ", ex);
                    return;
                }
                StatusMultiplex stat = new StatusMultiplex();
                
                stat.addInterface(new LoggingStatus());
                stat.addInterface(new TextBoxStatusInterface(txtRunState));
                
                conf.addStatusInterface(stat);
                stat.setStatus("opening peaklists");
                AbstractMSMAccess input;
                try {
                    MSMListIterator listInput = new MSMListIterator(conf.getFragmentTolerance(), 1, conf);
//            if (!msmFile.isEmpty()) {
//                listInput.addFile(msmFile, "", conf.getFragmentTolerance());
//            }

                    //input = new MSMIterator(msm, conf.getFragmentTolerance());
                    File[] list = flMSMFiles.getFiles();
                    if (list.length > 0) {
                        for (File f : list) {
                            listInput.addFile(f.getAbsolutePath(), "", conf.getFragmentTolerance());
                        }
                    }
                    listInput.init();
                    
                    input = listInput;
                    
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, "Error while reading msm-file file ", ex);
                    JOptionPane.showMessageDialog(rootPane, ex.getLocalizedMessage(), "Error wile reading file ", JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (IOException ex) {
                    Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
                    JOptionPane.showMessageDialog(rootPane, ex.getLocalizedMessage(), "Error wile reading file ", JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (ParseException ex) {
                    Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
                    JOptionPane.showMessageDialog(rootPane, ex.getLocalizedMessage(), "Error wile reading file ", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                ResultWriter output;
                try {
                    ResultWriter cvs = null;
                
                    if (csvOut.endsWith(".gz")) {
                        cvs = new CSVExportMatches(new FileOutputStream(new File(csvOut)), conf,true);
                    } else {
                        cvs = new CSVExportMatches(new FileOutputStream(new File(csvOut)), conf,false);
                    }
                    ResultMultiplexer rm = new ResultMultiplexer();
                    rm.addResultWriter(cvs);
                    //rm.setFreeMatch(true);
                    String peakout = txtPeakList.getText();
                    if (peakout.length() > 0) {
                        ResultWriter stw = new PeakListWriter(new FileOutputStream((String) peakout));
                        rm.addResultWriter(stw);
                    }
//            if (Boolean.valueOf((String)conf.retrieveObject("XMASSOUTPUT"))) {
//                rm.addResultWriter(new XmassDB(conf, msm.getName() + "_xlink_forward_" + Calendar.getInstance().toString()));
//            }

//            rm.addResultWriter(new ErrorWriter(new FileOutputStream("/tmp/error.csv")));
//            Object bufferOut = conf.retrieveObject("BUFFEROUTPUT");
//            if (bufferOut != null && (Integer.valueOf((String)bufferOut) > 0 )) {
//                output = new BufferedResultWriter(rm, Integer.valueOf((String)bufferOut));
//            } else
                    output = rm;//new BufferedResultWriter(rm, 10);

                } catch (IOException ex) {
                    Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, "Error while creating output-file ", ex);
                    return;
                }
                stat.setStatus("starting search");
                xirunner = new RunXi(fastaFile, input, output, conf);
                Thread t = new Thread(xirunner);
                t.setName("xirunner");
                t.start();
                
            }
        };
        
        Thread setup = new Thread(runnable);
        setup.setName("xi-setup");
        setup.start();
        
        //System.out.print(xi.ScoreStatistic());

    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        txtRunState = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtLog = new javax.swing.JTextArea();
        jLabel4 = new javax.swing.JLabel();
        btnStartSearch = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        txtPeakList = new rappsilber.gui.components.FileBrowser();
        txtResultFile = new rappsilber.gui.components.FileBrowser();
        btnStop = new javax.swing.JButton();
        btnGC = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        flMSMFiles = new rappsilber.gui.components.FileList();
        jPanel4 = new javax.swing.JPanel();
        flFASTAFiles = new rappsilber.gui.components.FileList();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtConfig = new javax.swing.JTextArea();
        fbSaveConfig = new rappsilber.gui.components.FileBrowser();
        jLabel1 = new javax.swing.JLabel();
        btnSave = new javax.swing.JToggleButton();
        cmbLogLevel = new javax.swing.JComboBox();
        fbLoadConfig = new rappsilber.gui.components.FileBrowser();
        btnLoadConfig = new javax.swing.JButton();
        linearCalibration1 = new rappsilber.gui.components.LinearCalibration();
        jPanel5 = new javax.swing.JPanel();
        memory1 = new rappsilber.gui.components.memory.Memory();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Offline Xi");

        jLabel5.setText("Log");

        txtRunState.setEditable(false);

        txtLog.setColumns(20);
        txtLog.setRows(5);
        jScrollPane1.setViewportView(txtLog);

        jLabel4.setText("peak annotations");

        btnStartSearch.setText("Start");
        btnStartSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartSearchActionPerformed(evt);
            }
        });

        jLabel6.setText("result");

        btnStop.setText("Stop");
        btnStop.setEnabled(false);
        btnStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopActionPerformed(evt);
            }
        });

        btnGC.setText("GC");
        btnGC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGCActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 531, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(jLabel6)
                            .addComponent(jLabel5))
                        .addGap(16, 16, 16)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(txtRunState, javax.swing.GroupLayout.DEFAULT_SIZE, 181, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnGC)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnStartSearch)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnStop))
                            .addComponent(txtResultFile, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 389, Short.MAX_VALUE)
                            .addComponent(txtPeakList, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 389, Short.MAX_VALUE))))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(txtPeakList, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtResultFile, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addGap(21, 21, 21)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtRunState, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(btnStartSearch)
                    .addComponent(btnStop)
                    .addComponent(btnGC))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 220, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Run", jPanel1);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(flMSMFiles, javax.swing.GroupLayout.DEFAULT_SIZE, 531, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(flMSMFiles, javax.swing.GroupLayout.DEFAULT_SIZE, 316, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("MSM Files", jPanel3);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(flFASTAFiles, javax.swing.GroupLayout.DEFAULT_SIZE, 531, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(flFASTAFiles, javax.swing.GroupLayout.DEFAULT_SIZE, 316, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Fasta Files", jPanel4);

        jScrollPane2.setAutoscrolls(true);

        txtConfig.setColumns(20);
        txtConfig.setRows(5);
        jScrollPane2.setViewportView(txtConfig);

        fbSaveConfig.setExtensions(new String[] {"txt"});

        jLabel1.setText("Save");

        btnSave.setText("Save");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        cmbLogLevel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbLogLevelActionPerformed(evt);
            }
        });

        fbLoadConfig.setExtensions(new String[] {"txt"});

        btnLoadConfig.setText("Load");
        btnLoadConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLoadConfigActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 531, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fbSaveConfig, javax.swing.GroupLayout.DEFAULT_SIZE, 411, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnSave))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(fbLoadConfig, javax.swing.GroupLayout.DEFAULT_SIZE, 273, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnLoadConfig)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbLogLevel, javax.swing.GroupLayout.PREFERRED_SIZE, 177, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(cmbLogLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnLoadConfig))
                    .addComponent(fbLoadConfig, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 254, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel1)
                    .addComponent(btnSave)
                    .addComponent(fbSaveConfig, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Config", jPanel2);
        jTabbedPane1.addTab("Calibration", linearCalibration1);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(memory1, javax.swing.GroupLayout.DEFAULT_SIZE, 531, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(memory1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(309, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Memory", jPanel5);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addComponent(jTabbedPane1))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnStartSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStartSearchActionPerformed

        startRun();
        btnStop.setEnabled(true);

    }//GEN-LAST:event_btnStartSearchActionPerformed

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

    private void btnGCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGCActionPerformed
        Runtime r = Runtime.getRuntime();
        Logger.getLogger(SimpleXiGui.class.getName()).log(Level.INFO, "Before GC :  " + doubleToString(r.freeMemory()) + " free of " + doubleToString(r.maxMemory()) + " (total: " + doubleToString(r.totalMemory()) + ")");
        System.gc();
        Logger.getLogger(SimpleXiGui.class.getName()).log(Level.INFO, "After GC  :  " + doubleToString(r.freeMemory()) + " free of " + doubleToString(r.maxMemory()) + " (total: " + doubleToString(r.totalMemory()) + ")");

    }//GEN-LAST:event_btnGCActionPerformed

    private void cmbLogLevelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbLogLevelActionPerformed
        loggingHandle.setLevel((Level) cmbLogLevel.getSelectedItem());
        Logger.getLogger("rappsilber").setLevel(Level.ALL);
    }//GEN-LAST:event_cmbLogLevelActionPerformed

    private void btnLoadConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLoadConfigActionPerformed
        File f = fbLoadConfig.getFile();
        if (f != null)
            loadConfig(fbLoadConfig.getFile(),false);
    }//GEN-LAST:event_btnLoadConfigActionPerformed


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
    private javax.swing.JButton btnGC;
    private javax.swing.JButton btnLoadConfig;
    private javax.swing.JToggleButton btnSave;
    private javax.swing.JButton btnStartSearch;
    private javax.swing.JButton btnStop;
    private javax.swing.JComboBox cmbLogLevel;
    private rappsilber.gui.components.FileBrowser fbLoadConfig;
    private rappsilber.gui.components.FileBrowser fbSaveConfig;
    private rappsilber.gui.components.FileList flFASTAFiles;
    public rappsilber.gui.components.FileList flMSMFiles;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private rappsilber.gui.components.LinearCalibration linearCalibration1;
    private rappsilber.gui.components.memory.Memory memory1;
    private javax.swing.JTextArea txtConfig;
    private javax.swing.JTextArea txtLog;
    private rappsilber.gui.components.FileBrowser txtPeakList;
    private rappsilber.gui.components.FileBrowser txtResultFile;
    private javax.swing.JTextField txtRunState;
    // End of variables declaration//GEN-END:variables

}
