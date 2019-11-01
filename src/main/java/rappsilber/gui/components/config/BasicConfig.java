/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rappsilber.gui.components.config;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import rappsilber.gui.SimpleXiGui;
import rappsilber.gui.components.GenericTextPopUpMenu;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class BasicConfig extends javax.swing.JPanel implements ConfigProvider{
    boolean enabled = true;
    private class ReducedMultiClickSelection extends DefaultListSelectionModel {
        @Override
        public void setSelectionInterval(int index0, int index1) {
            if (enabled) {
                if(super.isSelectedIndex(index0)) {
                    super.removeSelectionInterval(index0, index1);
                }
                else {
                    super.addSelectionInterval(index0, index1);
                }
            }
        }
    }
    
    public class NameValuePair {
        public String name;
        public String value;
        public boolean stripComments = false;

        public NameValuePair(String name, String Value) {
            this.name = name;
            this.value = Value;
        }

        public NameValuePair(String entry) {
            String e[] = entry.split("=",2);
            this.name = e[0].trim();
            this.value = e[1].trim();
        }

        
        @Override
        public String toString() {
            if (stripComments && name.contains("#"))
                return name.substring(0,name.indexOf("#"));
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) 
                return true;
            if (obj instanceof NameValuePair) {
                NameValuePair nvp = (NameValuePair) obj;
                return name.contentEquals(nvp.name) && value.contentEquals(nvp.value);
            }
            return false;
        }
        
        
        
    }

    NameValuePair[] customSettings;
    /**
     * When given the config can be exported to this TextConfig-Control
     */
    private TextConfig textConfig;
    
    /**
     * event listener that get triggered when the config should be transferred to a text
     */
    private ArrayList<ActionListener> textConfigListener = new ArrayList<>();
    
    /**
     * Creates new form BasicConfig
     */
    public BasicConfig() {
        initComponents();
        lstVarMod.setSelectionModel(new ReducedMultiClickSelection());
        lstFixedMod.setSelectionModel(new ReducedMultiClickSelection());
        lstLinearMod.setSelectionModel(new ReducedMultiClickSelection());
        lstLosses.setSelectionModel(new ReducedMultiClickSelection());
        lstIons.setSelectionModel(new ReducedMultiClickSelection());
        try {
            initialise();
        } catch (IOException ex) {
            Logger.getLogger(BasicConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        updateTransferButton();
        
        BufferedReader confReader = null;
        try {
            File filesource = Util.getFileRelative("BasicConfig.conf", true);
            StringBuilder config = new StringBuilder();
            if (filesource == null) {
                confReader = Util.readFromClassPath(".rappsilber.data.BasicConfig.conf");
            } else {
                confReader = new BufferedReader(new FileReader(filesource));
            }
            StringBuilder sb = new StringBuilder();
            while (confReader.ready()) {
                sb.append(confReader.readLine()).append("\n");
            }
            txtBaseSettings.setText(sb.toString());
        } catch (IOException ex) {
            Logger.getLogger(BasicConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public void addTransferListener(ActionListener listener) {
        textConfigListener.add(listener);
        updateTransferButton(); 
    }

    public void removeTransferListener(ActionListener listener) {
        textConfigListener.remove(listener);
        updateTransferButton(); 
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.spMissCleavages.setEnabled(enabled);
        this.btnAddCustom.setEnabled(enabled);
        this.txtCustomSetting.setEditable(enabled);
        this.cbCrosslinker.setEnabled(enabled);
        this.cbEnzyme.setEnabled(enabled);
        this.spToleranceMS1.setEnabled(enabled);
        this.spToleranceMS2.setEnabled(enabled);
        this.spToleranceUnitMS1.setEnabled(enabled);
        this.spToleranceUnitMS2.setEnabled(enabled);
    }
    
    
    
    
    public String getConfig() throws IOException {
        File filesource = Util.getFileRelative("BasicConfig.conf", true);
        BufferedReader confReader = null;
        StringBuilder config = new StringBuilder();
        if (filesource == null) {
            confReader = Util.readFromClassPath(".rappsilber.data.BasicConfig.conf");
        } else {
            confReader = new BufferedReader(new FileReader(filesource));
        }
        

        config.append("\n##################");
        config.append("\n# variable modification\n");
        for (int p : lstVarMod.getSelectedIndices()) {
            config.append(
                    lstVarMod.getModel().getElementAt(p).
                            value.replaceAll("\\[MODE\\]", "variable")).append("\n");
        }
        config.append("\n##################");
        config.append("\n# Fixed modification\n");
        for (int p : lstFixedMod.getSelectedIndices()) {
            config.append(
                    lstFixedMod.getModel().getElementAt(p).
                            value.replaceAll("\\[MODE\\]", "fixed")).append("\n");
        }
        config.append("\n##################");
        config.append("\n# Linear modification\n");
        for (int p : lstLinearMod.getSelectedIndices()) {
            config.append(
                    lstLinearMod.getModel().getElementAt(p).
                            value.replaceAll("\\[MODE\\]", "linear")).append("\n");
        }

        config.append("\n##################");
        config.append("\n# Ions\n");
        for (int p : lstIons.getSelectedIndices()) {
            config.append(lstIons.getModel().getElementAt(p).value).append("\n");
        }
        
        config.append("\n##################");
        config.append("\n# Losses\n");
        for (int p : lstLosses.getSelectedIndices()) {
            config.append(lstLosses.getModel().getElementAt(p).value).append("\n");
        }
        
        config.append("\n##################");
        config.append("\n# Enzyme\n");
        config.append(((NameValuePair)cbEnzyme.getSelectedItem()).value).append("\n");
        config.append("\n##################");
        config.append("\n## how many misscleavages are considered");
        config.append("\nmissedcleavages:" + spMissCleavages.getValue());

        config.append("\n##################");
        config.append("\n# Crosslinker\n");
        config.append(((NameValuePair)cbCrosslinker.getSelectedItem()).value).append("\n");
        
        config.append("\n##################");
        config.append("\n# MS1 tolerance\n");
        config.append("tolerance:precursor:"+spToleranceMS1.getValue() + spToleranceUnitMS1.getSelectedItem()).append("\n");
        config.append("# MS2 tolerance\n");
        config.append("tolerance:fragment:"+spToleranceMS2.getValue() + spToleranceUnitMS2.getSelectedItem()).append("\n");

        config.append("\n\n# ---------------------------------------------\n");
        config.append("# Basic settings\n");
        config.append("# ---------------------------------------------\n");
        config.append(txtBaseSettings.getText());
        
        if (!txtCustomSetting.getText().isEmpty()) {
            config.append("\n\n# ---------------------------------------------\n");
            config.append("\n# Custom Settings\n");
            config.append("# ---------------------------------------------\n");
            config.append(txtCustomSetting.getText()).append("\n");
        }


        return config.toString();
    }
    
    public void initialise() throws IOException {
        File filesource = Util.getFileRelative("BasicConfigEntries.conf", true);
        BufferedReader confReader = null;
        if (filesource == null) {
            confReader = Util.readFromClassPath(".rappsilber.data.BasicConfigEntries.conf");
        } else {
            confReader = new BufferedReader(new FileReader(filesource));
        }
        ArrayList<NameValuePair> crosslinkers = new ArrayList<>();
        ArrayList<NameValuePair> modifications = new ArrayList<>();
        ArrayList<NameValuePair> losses = new ArrayList<>();
        ArrayList<NameValuePair> ions = new ArrayList<>();
        ArrayList<NameValuePair> enzymes = new ArrayList<>();
        ArrayList<NameValuePair> custom = new ArrayList<>();
        ArrayList<NameValuePair> currentlist = null;
        while (confReader.ready()) {
            String line = confReader.readLine();
            String lcline = line.trim().toLowerCase();
            switch(lcline){
                case "[crosslinker]":
                    currentlist = crosslinkers;
                    break;
                case "[modifications]":
                    currentlist = modifications;
                    break;
                case "[losses]":
                    currentlist = losses;
                    break;
                case "[ions]":
                    currentlist = ions;
                    break;
                case "[enzymes]":
                    currentlist = enzymes;
                    break;
                case "[custom]":
                    currentlist = custom;
                    break;
                default:
                    if (!(lcline.startsWith("#") || lcline.isEmpty())) {
                        currentlist.add(new NameValuePair(line));
                    }
            }
        }
        
        cbCrosslinker.setModel(new DefaultComboBoxModel<NameValuePair>(crosslinkers.toArray(new NameValuePair[0])));
        for (NameValuePair nvp : crosslinkers) {
            if (nvp.name.startsWith("[+]")) {
                nvp.name=nvp.name.substring(3);
                cbCrosslinker.setSelectedItem(nvp);
            }
        }
        
        cbEnzyme.setModel(new DefaultComboBoxModel<NameValuePair>(enzymes.toArray(new NameValuePair[0])));
        for (NameValuePair nvp : enzymes) {
            if (nvp.name.startsWith("[+]")) {
                nvp.name=nvp.name.substring(3);
                cbEnzyme.setSelectedItem(nvp);
            }
        }
        
        DefaultListModel<NameValuePair> fixed = new DefaultListModel<>();
        lstFixedMod.setModel(fixed);
        String fid = "[f]";
        DefaultListModel<NameValuePair> variable = new DefaultListModel<>();
        lstVarMod.setModel(variable);
        String vid = "[v]";
        DefaultListModel<NameValuePair> linear = new DefaultListModel<>();
        lstLinearMod.setModel(linear);
        String lid = "[l]";
        modifications.sort(new Comparator<NameValuePair>() {
            @Override
            public int compare(NameValuePair o1, NameValuePair o2) {
                int c1=1;
                int c2=1;
                if (o1.name.startsWith("[")) {
                    c1=0;
                }
                if (o2.name.startsWith("[")) {
                    c2=0;
                }
                if (c1!=c2) {
                    return Integer.compare(c1, c2);
                }
                return o1.name.compareTo(o2.name);
            }
        });
        
        int pos = 0;
        for (NameValuePair nvp : modifications) {
            fixed.add(pos,nvp);
            variable.add(pos,nvp);
            linear.add(pos,nvp);
            lstVarMod.removeSelectionInterval(pos, pos);
            lstFixedMod.removeSelectionInterval(pos, pos);
            lstLinearMod.removeSelectionInterval(pos, pos);
            String nv = nvp.name.toLowerCase();
            if (nv.startsWith(vid)) {
                nvp.name = nvp.name.substring(vid.length());
                lstVarMod.addSelectionInterval(pos, pos);
            } else if (nv.startsWith(fid)) {
                nvp.name = nvp.name.substring(fid.length());
                lstFixedMod.addSelectionInterval(pos, pos);
            } else if (nv.startsWith(lid)) {
                nvp.name = nvp.name.substring(lid.length());
                lstLinearMod.addSelectionInterval(pos, pos);
            }
            pos++;
        }
        
        DefaultListModel<NameValuePair> ionsM = new DefaultListModel<>();
        lstIons.setModel(ionsM);
        pos = 0;
        for (NameValuePair nvp : ions) {
            ionsM.add(pos, nvp);
            lstIons.removeSelectionInterval(pos, pos);
            String nv = nvp.name.toLowerCase();
            if (nv.startsWith("[+]")) {
                nvp.name = nvp.name.substring(vid.length());
                lstIons.addSelectionInterval(pos, pos);
            } 
            pos++;
        }

        DefaultListModel<NameValuePair> lossesM = new DefaultListModel<>();
        lstLosses.setModel(lossesM);
        pos = 0;
        for (NameValuePair nvp : losses) {
            lossesM.add(pos, nvp);
            lstLosses.removeSelectionInterval(pos, pos);
            String nv = nvp.name.toLowerCase();
            if (nv.startsWith("[+]")) {
                nvp.name = nvp.name.substring(vid.length());
                lstLosses.addSelectionInterval(pos, pos);
            } 
            pos++;
        }
        for (NameValuePair nvp : custom) {
            nvp.stripComments  =true;
        }
        this.customSettings = custom.toArray(new NameValuePair[custom.size()]);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        spBaseSettings = new javax.swing.JScrollPane();
        txtBaseSettings = new javax.swing.JTextArea();
        btnBaseSettings = new javax.swing.JButton();
        cbCrosslinker = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        spToleranceMS1 = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        spToleranceUnitMS1 = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        spToleranceMS2 = new javax.swing.JSpinner();
        spToleranceUnitMS2 = new javax.swing.JComboBox<>();
        jLabel5 = new javax.swing.JLabel();
        cbEnzyme = new javax.swing.JComboBox<>();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        spMissCleavages = new javax.swing.JSpinner();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        spFixedMods = new javax.swing.JScrollPane();
        lstFixedMod = new javax.swing.JList<>();
        jLabel6 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        lstVarMod = new javax.swing.JList<>();
        jLabel7 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        lstLinearMod = new javax.swing.JList<>();
        jLabel8 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        lstIons = new javax.swing.JList<>();
        jLabel9 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        lstLosses = new javax.swing.JList<>();
        jPanel8 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        txtCustomSetting = new javax.swing.JTextArea();
        btnAddCustom = new javax.swing.JButton();
        btnToText = new javax.swing.JButton();

        spBaseSettings.setMinimumSize(new java.awt.Dimension(100, 100));
        spBaseSettings.setPreferredSize(new java.awt.Dimension(300, 300));

        txtBaseSettings.setColumns(20);
        txtBaseSettings.setRows(5);
        spBaseSettings.setViewportView(txtBaseSettings);

        btnBaseSettings.setText("Base Settings");
        btnBaseSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBaseSettingsActionPerformed(evt);
            }
        });

        jLabel1.setText("Crosslinker");

        spToleranceMS1.setModel(new javax.swing.SpinnerNumberModel(6.0d, 0.0d, null, 1.0d));
        spToleranceMS1.setToolTipText("Tolerance for matching the precursor mass");

        jLabel2.setText("Tolerance");

        jLabel3.setText("MS1:");

        spToleranceUnitMS1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "ppm", "Da" }));
        spToleranceUnitMS1.setToolTipText("Error Unit");

        jLabel4.setText("MS2:");

        spToleranceMS2.setModel(new javax.swing.SpinnerNumberModel(20.0d, 0.0d, null, 1.0d));
        spToleranceMS2.setToolTipText("Tolerance for matching the fragments against the MS2 spectrum");

        spToleranceUnitMS2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "ppm", "Da" }));
        spToleranceUnitMS2.setToolTipText("Error Unit");

        jLabel5.setText("Modifications:");

        jLabel11.setText("Enzyme");

        jLabel12.setText("Misscleavages");

        spMissCleavages.setModel(new javax.swing.SpinnerNumberModel(2, 0, 100, 1));

        jPanel2.setLayout(new java.awt.GridLayout(2, 3));

        lstFixedMod.setToolTipText("selected modifications are applied before digest on the whole protein");
        spFixedMods.setViewportView(lstFixedMod);

        jLabel6.setText("Fixed");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(spFixedMods, javax.swing.GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spFixedMods, javax.swing.GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE))
        );

        jPanel2.add(jPanel3);

        lstVarMod.setToolTipText("Peptides containing amino-acids that can be modified by the selectd modifications are searched with and without the modification");
        jScrollPane2.setViewportView(lstVarMod);

        jLabel7.setText("Variable");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE))
        );

        jPanel2.add(jPanel4);

        jScrollPane3.setPreferredSize(new java.awt.Dimension(50, 50));

        lstLinearMod.setToolTipText("Peptides containing these modifications are only searched as linear peptides");
        jScrollPane3.setViewportView(lstLinearMod);

        jLabel8.setText("Variable (Linear Peptides)");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE))
        );

        jPanel2.add(jPanel5);

        lstIons.setToolTipText("What basic ions to considere for matching MS2 spectra");
        jScrollPane4.setViewportView(lstIons);

        jLabel9.setText("Ions");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE))
        );

        jPanel2.add(jPanel6);

        jLabel10.setText("Losses");

        lstLosses.setToolTipText("What losses to considere during matching MS2 spectra");
        jScrollPane5.setViewportView(lstLosses);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel10)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE))
        );

        jPanel2.add(jPanel7);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Custom Settings"));

        txtCustomSetting.setColumns(20);
        txtCustomSetting.setText("# this is a free text field\n# anything starting with '#' is ignored\n# everything else is passed on as search-parameter\n# click the '+' to see available templates\n");
        txtCustomSetting.setToolTipText("Free text field that can be used to supply additional settings");
        jScrollPane6.setViewportView(txtCustomSetting);

        btnAddCustom.setText("+");
        btnAddCustom.setToolTipText("Provides a list of custom settings for selection");
        btnAddCustom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddCustomActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 145, Short.MAX_VALUE)
                .addGap(2, 2, 2)
                .addComponent(btnAddCustom))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane6)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnAddCustom))
        );

        btnToText.setText("As Text-Config");
        btnToText.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnToTextActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnToText)
                .addContainerGap())
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 201, Short.MAX_VALUE)
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 72, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnToText)
                .addGap(0, 0, 0))
        );

        jPanel2.add(jPanel8);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(jLabel2)
                .addGap(22, 22, 22)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spToleranceMS1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spToleranceUnitMS1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spToleranceMS2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spToleranceUnitMS2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(2, 2, 2)
                        .addComponent(cbEnzyme, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spMissCleavages, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jLabel11))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel5)))
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(14, 14, 14)
                .addComponent(cbCrosslinker, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbCrosslinker, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spToleranceMS1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(spToleranceUnitMS1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spToleranceMS2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(spToleranceUnitMS2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbEnzyme, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11)
                    .addComponent(jLabel12)
                    .addComponent(spMissCleavages, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jLabel5)
                .addGap(0, 0, 0)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 231, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnAddCustomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddCustomActionPerformed
        final JFrame window = new JFrame();
        window.setLayout(new BorderLayout());
        final JList<NameValuePair> confEntries = new JList<>(customSettings);
        final JScrollPane spSettings = new JScrollPane(confEntries);
//        final JComboBox<NameValuePair> conf = new JComboBox<>(customSettings);
        final JButton add = new JButton("Add");
        final JPanel space = new JPanel();
        confEntries.setSelectionModel(new ReducedMultiClickSelection());

        add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String prev =txtCustomSetting.getText();
                StringBuilder sb = new StringBuilder(prev);
                for (NameValuePair nvp : confEntries.getSelectedValuesList()) {
                        sb.append("\n# " + nvp.name.replaceAll("#","\n#")+ "\n" + 
                        nvp.value);
                }
                
                txtCustomSetting.setText(sb.toString());
                    
            }
        });
        final JButton close = new JButton("close");
        close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                window.setVisible(false);
                window.dispose();
            }
        });
        final JPanel buttonpanel = new JPanel(new BorderLayout());
        buttonpanel.add(add, BorderLayout.CENTER);
        buttonpanel.add(close, BorderLayout.EAST);
        //window.setPreferredSize(conf.getPreferredSize());
        window.add(space,BorderLayout.NORTH);
        window.add(spSettings, BorderLayout.CENTER);
        window.add(buttonpanel, BorderLayout.SOUTH);
        window.pack();
        
        window.addWindowFocusListener( new WindowFocusListener() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                window.setVisible(false);
                window.dispose();


            }

            @Override
            public void windowGainedFocus(WindowEvent e) {

            }
        });
        window.setLocationRelativeTo(null);
        window.setLocation(this.getLocation());
        window.setVisible(true);

    }//GEN-LAST:event_btnAddCustomActionPerformed

    private void btnToTextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnToTextActionPerformed
        try {
            if (textConfig != null) {
                textConfig.setConfig(getConfig());
                textConfig.requestFocus();
                textConfig.requestFocusInWindow();
            }
            
            ActionEvent e = new ActionEvent(this, 0, getConfig());
            for (ActionListener al :textConfigListener) {
                al.actionPerformed(e);
            }
        } catch (IOException ex) {
            Logger.getLogger(BasicConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_btnToTextActionPerformed

    private void btnBaseSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBaseSettingsActionPerformed
        final JFrame baseSettingsWindow = new JFrame("Base Settigns");
        JButton close = new JButton("Close");
        JLabel l = new JLabel("These are the basic settings that are applied by default. You can change them freely to your liking!");
        baseSettingsWindow.getContentPane().setLayout(new BorderLayout(5, 5));        
        baseSettingsWindow.getContentPane().add(l,BorderLayout.NORTH);
        baseSettingsWindow.getContentPane().add(spBaseSettings,BorderLayout.CENTER);
        baseSettingsWindow.getContentPane().add(close,BorderLayout.SOUTH);
        close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                baseSettingsWindow.getContentPane().removeAll();
                baseSettingsWindow.dispose();
            }
        });
        
        
        baseSettingsWindow.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {
            }

            @Override
            public void windowClosing(WindowEvent e) {
            }

            @Override
            public void windowClosed(WindowEvent e) {
                baseSettingsWindow.getContentPane().removeAll();
                baseSettingsWindow.dispose();
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
            }

            @Override
            public void windowActivated(WindowEvent e) {
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }
        });
        baseSettingsWindow.setPreferredSize(spBaseSettings.getPreferredSize());
        baseSettingsWindow.pack();
        baseSettingsWindow.setVisible(true);
        GenericTextPopUpMenu p = new GenericTextPopUpMenu();
        p.installContextMenu(baseSettingsWindow);
    }//GEN-LAST:event_btnBaseSettingsActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddCustom;
    private javax.swing.JButton btnBaseSettings;
    private javax.swing.JButton btnToText;
    private javax.swing.JComboBox<NameValuePair> cbCrosslinker;
    private javax.swing.JComboBox<NameValuePair> cbEnzyme;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
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
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JList<NameValuePair> lstFixedMod;
    private javax.swing.JList<NameValuePair> lstIons;
    private javax.swing.JList<NameValuePair> lstLinearMod;
    private javax.swing.JList<NameValuePair> lstLosses;
    private javax.swing.JList<NameValuePair> lstVarMod;
    private javax.swing.JScrollPane spBaseSettings;
    private javax.swing.JScrollPane spFixedMods;
    private javax.swing.JSpinner spMissCleavages;
    private javax.swing.JSpinner spToleranceMS1;
    private javax.swing.JSpinner spToleranceMS2;
    private javax.swing.JComboBox<String> spToleranceUnitMS1;
    private javax.swing.JComboBox<String> spToleranceUnitMS2;
    private javax.swing.JTextArea txtBaseSettings;
    private javax.swing.JTextArea txtCustomSetting;
    // End of variables declaration//GEN-END:variables

    public static void main(String args[]) {
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                final JFrame window = new JFrame();
                window.setLayout(new BorderLayout());
                final BasicConfig conf = new BasicConfig();
                final JButton ok = new JButton("OK");
                ok.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            JOptionPane.showMessageDialog(window, conf.getConfig());
                            System.out.println(conf.getConfig());
                        } catch (IOException ex) {
                            Logger.getLogger(BasicConfig.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
                window.setPreferredSize(conf.getPreferredSize());
                window.add(conf, BorderLayout.CENTER);
                window.add(ok, BorderLayout.SOUTH);
                window.pack();
                window.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        System.exit(0);
                    }
                    
                });
                try {
                    conf.initialise();

                } catch (IOException ex) {
                    Logger.getLogger(SimpleXiGui.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                window.setVisible(true);
            }
        });

    }

    /**
     * When given the config can be exported to this TextConfig-Control
     * @return the textConfig
     */
    public TextConfig getTextConfig() {
        return textConfig;
    }

    /**
     * When given the config can be exported to this TextConfig-Control
     * @param textConfig the textConfig to set
     */
    public void setTextConfig(TextConfig textConfig) {
        rappsilber.gui.components.config.TextConfig oldTextConfig = this.textConfig;
        this.textConfig = textConfig;
        propertyChangeSupport.firePropertyChange(PROP_TEXTCONFIG, oldTextConfig, textConfig);
        updateTransferButton(); 
    }

    protected void updateTransferButton() {
        btnToText.setEnabled(textConfig != null || textConfigListener.size()>0);
        btnToText.setVisible(textConfig != null || textConfigListener.size()>0);
    }
    private final transient PropertyChangeSupport propertyChangeSupport = new java.beans.PropertyChangeSupport(this);
    public static final String PROP_TEXTCONFIG = "textConfig";
    
}
