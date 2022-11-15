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
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import rappsilber.applications.XiDB;
import rappsilber.gui.components.GenericTextPopUpMenu;
import rappsilber.utils.Util;
import rappsilber.utils.XiVersion;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class XiDBConfigLoader extends javax.swing.JFrame {

    
    
    /** Creates new form XiDBStarter */
    public XiDBConfigLoader() {
        initComponents();
        this.setTitle("xi db starter - " + XiVersion.getVersionString());
//        loadList();
        dbSearch.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        GenericTextPopUpMenu copypaste = new GenericTextPopUpMenu();
        copypaste.installContextMenu(this);
        
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bgDBSource = new javax.swing.ButtonGroup();
        tpMain = new javax.swing.JTabbedPane();
        pSearch = new javax.swing.JPanel();
        dbSearch = new rappsilber.gui.components.db.GetSearch();
        btnRun = new javax.swing.JButton();
        pConfig = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtConfig = new javax.swing.JTextArea();
        fbConfigOut = new rappsilber.gui.components.FileBrowser();
        jLabel1 = new javax.swing.JLabel();
        btnWrite = new javax.swing.JButton();
        memory1 = new org.rappsilber.gui.components.memory.Memory();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        btnRun.setText("GetConfig");
        btnRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRunActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pSearchLayout = new javax.swing.GroupLayout(pSearch);
        pSearch.setLayout(pSearchLayout);
        pSearchLayout.setHorizontalGroup(
            pSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pSearchLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnRun)
                .addContainerGap())
            .addComponent(dbSearch, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        pSearchLayout.setVerticalGroup(
            pSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pSearchLayout.createSequentialGroup()
                .addComponent(dbSearch, javax.swing.GroupLayout.DEFAULT_SIZE, 305, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnRun)
                .addContainerGap())
        );

        tpMain.addTab("Select Search", pSearch);

        txtConfig.setColumns(20);
        txtConfig.setRows(5);
        jScrollPane1.setViewportView(txtConfig);

        fbConfigOut.setDescription("Config-File");
        fbConfigOut.setExtensions(new String[] {"conf", "config"});

        jLabel1.setText("output:");

        btnWrite.setText("write");
        btnWrite.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnWriteActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pConfigLayout = new javax.swing.GroupLayout(pConfig);
        pConfig.setLayout(pConfigLayout);
        pConfigLayout.setHorizontalGroup(
            pConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 668, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pConfigLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fbConfigOut, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnWrite)
                .addContainerGap())
        );
        pConfigLayout.setVerticalGroup(
            pConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pConfigLayout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 305, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel1)
                    .addComponent(fbConfigOut, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnWrite))
                .addContainerGap())
        );

        tpMain.addTab("Config", pConfig);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(tpMain))
                    .addComponent(memory1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tpMain)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(memory1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRunActionPerformed
        txtConfig.setText("");
        try {
            int[] IDs = dbSearch.getSelectedSearchIds();
            if (IDs.length == 0) {
                JOptionPane.showMessageDialog(this, "Nothing Selected");
                return;
            }
            final String name = dbSearch.getSelectedNames()[0];
            final int id = dbSearch.getSelectedSearchIds()[0];
            XiDB.m_db_connection = dbSearch.getConnectionString();
            
            System.setProperty("XI_DB_USER",dbSearch.getUser());
            System.setProperty("XI_DB_PASSWD",dbSearch.getPassword());
            
            ResultSet rs = dbSearch.getConnection().createStatement().executeQuery(
                    "SELECT description FROM xi_config_desc WHERE search_id in (-1, " + id +");");
            StringBuilder sb = new StringBuilder();
            while (rs.next()) {
                sb.append(rs.getString(1));
                sb.append("\n");
            }
            rs.close();
            txtConfig.setText(sb.toString());
            tpMain.setSelectedComponent(pConfig);
        } catch (SQLException ex) {
            Logger.getLogger(XiDBConfigLoader.class.getName()).log(Level.SEVERE, "Error loading config", ex);
            JOptionPane.showMessageDialog(rootPane, "Error Loading Config\n"+ex, "Error", JOptionPane.ERROR_MESSAGE);
        }
        
    }//GEN-LAST:event_btnRunActionPerformed

    private void btnWriteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnWriteActionPerformed
        File outfile = fbConfigOut.getFile();
        if (outfile.exists()) {
            if (JOptionPane.showConfirmDialog(rootPane, "File Exists! Overwrite?", "File Exists",JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                return;
            }
        }
        PrintWriter out = null;
        try {
            out = new PrintWriter(outfile);
            out.print(txtConfig.getText());
            out.close();
            
            JOptionPane.showMessageDialog(rootPane, "Writen");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(XiDBConfigLoader.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            out.close();
        }
    }//GEN-LAST:event_btnWriteActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(XiDBConfigLoader.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(XiDBConfigLoader.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(XiDBConfigLoader.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(XiDBConfigLoader.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new XiDBConfigLoader().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bgDBSource;
    private javax.swing.JButton btnRun;
    private javax.swing.JButton btnWrite;
    private rappsilber.gui.components.db.GetSearch dbSearch;
    private rappsilber.gui.components.FileBrowser fbConfigOut;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private org.rappsilber.gui.components.memory.Memory memory1;
    private javax.swing.JPanel pConfig;
    private javax.swing.JPanel pSearch;
    private javax.swing.JTabbedPane tpMain;
    private javax.swing.JTextArea txtConfig;
    // End of variables declaration//GEN-END:variables
}