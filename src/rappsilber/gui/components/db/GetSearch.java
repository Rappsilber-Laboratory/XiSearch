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
package rappsilber.gui.components.db;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.AbstractListModel;
import javax.swing.JList;
import rappsilber.config.DBConnectionConfig;
import rappsilber.gui.XiDBStarter;
import rappsilber.utils.MyArrayUtils;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class GetSearch extends javax.swing.JPanel {


    public class RunListBoxModel extends  AbstractListModel{
        
        public class line {

            public line(String name, String notes, int id, String status, String user) {
                this.name = name;
                this.notes = notes;
                this.id = id;
                this.status = status;
                this.user = user;
            }

            public line(String name, String notes, int id, String status, String user, String fasta, String peaklist) {
                this.name = name;
                this.notes = notes;
                this.id = id;
                this.status = status;
                this.user = user;
                this.fasta = fasta;
                this.peaklist = peaklist;
            }
            
            String name;
            String notes;
            String status;
            int id;
            String user;
            String fasta;
            String peaklist;
            
            public String toString() {
                return "" + id + " - " + user + "-" + status + " - " + name + " - " + notes +" - "+ fasta + " - " + peaklist;
            }
            
        }
  //      ArrayList<String> name = new ArrayList<String>();
        //ArrayList<Integer> ids =new ArrayList<Integer>();
        ArrayList<String> notes =new ArrayList<String>();
        ArrayList<line> lines = new ArrayList<line>();
        ArrayList<line> filteredLines = lines;
        int selected = -1;

        public RunListBoxModel(ResultSet rs) {
            if (rs == null)
                return;
            try {
                while (rs.next()) {
//                    name.add(rs.getString("name"));
//                    ids.add(rs.getInt("id"));
                    lines.add(new line(rs.getString("name"), rs.getString("notes"), rs.getInt("id"), rs.getString("status"), rs.getString("user_name"), rs.getString("fasta"), rs.getString("p")));
                }
            } catch (SQLException ex) {
                Logger.getLogger(XiDBStarter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        
        
        public int getSize() {
            return filteredLines.size();
        }

        public line getElementAt(int index) {
            return filteredLines.get(index);
        }
        
        public int getId(int index) {
            return filteredLines.get(index).id;
        }

        public void setSelectedItem(Object anItem) {
            selected = filteredLines.indexOf(anItem);
            
        }

        public line getSelectedItem() {
            if (selected <0)
                return null;
            return filteredLines.get(selected);
        }

        public String getSelectedName() {
            return filteredLines.get(selected).name;
        }
        
        public int getSelectedID() {
            return filteredLines.get(selected).id;
        }
        
        
        public void filter(String filter) {
            if (filter.isEmpty()) {
                filteredLines = lines;
            } else {
                filteredLines=new ArrayList<line>();
                if (filter.contains(" OR ") && !filter.startsWith("~")) {
                    String[] fa = filter.trim().split("\\s+OR\\s+");
                    filter = "~(" + MyArrayUtils.toString(fa, "|") +  ")";
                }
                
                if (filter.startsWith("~")) {
                    Pattern p = Pattern.compile(filter.substring(1).trim());
                    for (line l: lines) {
                        if (p.matcher(l.toString()).find()) 
                            filteredLines.add(l);
                    }    
                    
                } else {
                    String[] af;
                    boolean cs= true;
                    if (filter.startsWith("=")) {
                        af = new String[] {filter.substring(1)};
                        cs = true;
                    } else
                        af = filter.toLowerCase().trim().split("\\s+");
                    lineloop: for (line l: lines) {
                        String comp;;
                        if (cs)
                            comp= l.toString().toLowerCase();
                        else
                            comp= l.toString();
                                   
                        boolean fail = false;
                        for (String s: af) {
                            if (!comp.contains(s))
                                continue lineloop;
                        }
                        filteredLines.add(l);
                    }
                }
            }
            
            super.fireContentsChanged(lstSearches, 0, lines.size());
        }
        
    }
    
    ArrayList<ActionListener> statusListener = new ArrayList<ActionListener>(1);
    RunListBoxModel m_model;
    
    DBConnectionConfig.DBServer xi;
    DBConnectionConfig.DBServer xi3;
    

    /**
     * Creates new form GetSearch
     */
    public GetSearch() {
        try {
            initComponents();
            DBConnectionConfig dbc = new DBConnectionConfig();
            dbc.readConfig();    
            
            for (DBConnectionConfig.DBServer s: dbc.getServers()) {
                if (s.name.contentEquals("xi"))
                    xi = s;
                if (s.name.contentEquals("xi3"))
                    xi3 = s;
            }
            
            cmbConnection.setModel(new javax.swing.DefaultComboBoxModel(dbc.getServerArray()));
            
            rbXi3ActionPerformed(null);
            final GetSearchPopUpMenu popup = new GetSearchPopUpMenu("COPY", this);
            this.lstSearches.addMouseListener(new MouseAdapter() { 
                    GetSearchPopUpMenu textpopup  = popup;
                    public void mouseReleased(final MouseEvent e) {  
                        if (e.isPopupTrigger()) {  
                            textpopup.show(e.getComponent(), e.getX(), e.getY());
                        }  
                    }  
                    public void mousePressed(final MouseEvent e) {  
                        if (e.isPopupTrigger()) {  
                            textpopup.show(e.getComponent(), e.getX(), e.getY());
                        }  
                    }  
                });  
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GetSearch.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(GetSearch.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void publishStatus(final String status)  {
        
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                ActionEvent ae = new ActionEvent(this, 0, status);
                for (ActionListener al : statusListener) {
                    al.actionPerformed(ae);
                }
            }
        });
    }
    
    /**
     * Returns the current selection mode for the list. This is a cover
     * method that delegates to the method of the same name on the
     * list's selection model.
     *
     * @return the current selection mode
     * @see #setSelectionMode
     */
    public int getSelectionMode() {
        return lstSearches.getSelectionMode();
    }
    
    /**
     * Sets the selection mode for the list. This is a cover method that sets
     * the selection mode directly on the {@link JList#setSelectionMode(int) } . 
     * <p>
     * The following list describes the accepted selection modes:
     * <ul>
     * <li>{@code ListSelectionModel.SINGLE_SELECTION} -
     *   Only one list index can be selected at a time. In this mode,
     *   {@code setSelectionInterval} and {@code addSelectionInterval} are
     *   equivalent, both replacing the current selection with the index
     *   represented by the second argument (the "lead").
     * <li>{@code ListSelectionModel.SINGLE_INTERVAL_SELECTION} -
     *   Only one contiguous interval can be selected at a time.
     *   In this mode, {@code addSelectionInterval} behaves like
     *   {@code setSelectionInterval} (replacing the current selection},
     *   unless the given interval is immediately adjacent to or overlaps
     *   the existing selection, and can be used to grow the selection.
     * <li>{@code ListSelectionModel.MULTIPLE_INTERVAL_SELECTION} -
     *   In this mode, there's no restriction on what can be selected.
     *   This mode is the default.
     * </ul>
     *
     * @param selectionMode the selection mode
     * @see #getSelectionMode
     * @throws IllegalArgumentException if the selection mode isn't
     *         one of those allowed
     * @beaninfo
     * description: The selection mode.
     *        enum: SINGLE_SELECTION            ListSelectionModel.SINGLE_SELECTION
     *              SINGLE_INTERVAL_SELECTION   ListSelectionModel.SINGLE_INTERVAL_SELECTION
     *              MULTIPLE_INTERVAL_SELECTION ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
     */
    public void setSelectionMode(int mode) {
        lstSearches.setSelectionMode(mode);
    }
    
    
    public void addStatusListener(ActionListener sl) {
        statusListener.add(sl);
    }
    public void removeStatusListener(ActionListener sl) {
        statusListener.remove(sl);
    }
    
    
    public Connection getConnection() {
        try {
            String txtconnection = null;
            
            if (cmbConnection.getModel().getSelectedItem() instanceof DBConnectionConfig.DBServer) {
                txtconnection = ((DBConnectionConfig.DBServer)cmbConnection.getModel().getSelectedItem()).connectionString; 
            } else
                txtconnection = cmbConnection.getModel().getSelectedItem().toString();
            
            Object o = Class.forName("org.postgresql.Driver");
            
            // Establish network connection to database
            return DriverManager.getConnection(txtconnection, txtUser.getText(), txtPasswd.getText());
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            publishStatus("Error setting up connection:" + ex);
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            publishStatus("Error setting up connection:" + ex);
        }
        return null;
    }
    
    public String getConnectionString() {
            if (cmbConnection.getModel().getSelectedItem() instanceof DBConnectionConfig.DBServer) {
                return ((DBConnectionConfig.DBServer)cmbConnection.getModel().getSelectedItem()).connectionString; 
            } else
                return cmbConnection.getModel().getSelectedItem().toString();
    }
    
    public String getUser() {
        return txtUser.getText();
    }
    
    public String getPassword() {
        return txtPasswd.getText();
    }
    
    public int[] getSelectedSearchIds() {
        int[] listIDs =  lstSearches.getSelectedIndices();
        int[] ret = new int[listIDs.length];
        
        for (int lid =0; lid<listIDs.length; lid++) {
            ret[lid] = m_model.getId(listIDs[lid]);
        }
        return ret;
    }

    public void setSelectedSearchIds(int[] ids) {
        this.txtFilter.setText("");
        txtFilterActionPerformed(null);

        HashSet<Integer> hashedIDs = new HashSet<Integer>(ids.length);
        for (int i : ids) {
            hashedIDs.add(i);
        }

        lstSearches.setSelectedIndices(new int[] {m_model.getSize() +1});
        
        ArrayList<RunListBoxModel.line> lines = m_model.lines;
        ArrayList<Integer> selected = new ArrayList<Integer>(ids.length);
        for (int i = 0; i< lines.size(); i++) {
            RunListBoxModel.line l =lines.get(i);
            
            if (hashedIDs.contains(l.id)) {
    //            selected.add(i);
                lstSearches.addSelectionInterval(i, i);
            }
        }
//        int[] selids = new int[selected.size()];
//        for (int i = 0; i< selids.length; i++) {
//            selids[i]=selected.get(i);
//        }
//        lstSearches.setSelectedIndices(ids);
        
    }
    
    public String[] getSelectedNames() {
        int[] listIDs =  lstSearches.getSelectedIndices();
        String[] ret = new String[listIDs.length];
        
        for (int lid =0; lid<listIDs.length; lid++) {
            
            ret[lid] = m_model.getElementAt(listIDs[lid]).name;
        }
        return ret;
    }
    
    private void loadList() {

        // Establish network connection to database
        final Connection connection = getConnection();
        if (connection == null) {
            m_model = new RunListBoxModel(null);
            return;
        }
        final boolean showHidden = ckHidden.isSelected();
        
        
//            setEnableWrite(false);
        publishStatus("Loading Runs ... ");

        Runnable runnable = new Runnable() {

            public void run() {
                try {
                    Statement s = connection.createStatement();
                    // get all queuing runs
                    String q = "SELECT s.id, s.name, s.notes, s.status, u.user_name, f.FASTA AS FASTA, p.p "
                            + " FROM search s LEFT OUTER JOIN "
                            + " users u ON s.uploadedby = u.id LEFT OUTER JOIN "
                            + " (SELECT search_id, array_to_string(array_agg(distinct sf.name), ',') AS FASTA"
                            + " FROM search_sequencedb ss  LEFT OUTER JOIN "
                            + "      sequence_file sf ON ss.seqdb_id = sf.id "
                            + " GROUP BY search_id) f ON s.id = f.search_id LEFT OUTER JOIN " 
                            + " (SELECT "
                            + "     array_to_string(array_agg(distinct a.name), ',') || '  (' || array_to_string(array_agg(distinct r.name), ',') || ')' p , search_id "
                            + "  FROM search_acquisition sa INNER JOIN acquisition a on sa.acq_id = a.id inner join "
                            + "       run r on sa.acq_id = r.acq_id AND sa.run_id = r.run_id "
                            + " GROUP BY search_id) p on s.id = p.search_id "
                            + (showHidden ? "": "WHERE (s.hidden is null OR s.hidden = false)")
                            + " ORDER BY s.id DESC;";
                    
                    ResultSet rs = s.executeQuery(q);//(SELECT max(id) from search);");

                    m_model = new RunListBoxModel(rs);
                    Runnable setModel = new Runnable() {

                        public void run() {
                            lstSearches.setModel(m_model);
                            if (m_model.getSize() > 0) {
                                publishStatus("");
//                                    setEnableWrite(false);
                            } else {
                                publishStatus("no open searches found");
//                                    setEnableWrite(true);
                            }
                        }
                    };
                    javax.swing.SwingUtilities.invokeLater(setModel);
                } catch (final SQLException ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                    publishStatus(ex.toString());
                }
                if (!(txtFilter.getText().isEmpty() || txtFilter.getText().equals("Filter"))) {
                    javax.swing.SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            txtFilterActionPerformed(null);
                        }
                    });
                }                
            }
        };
        new Thread(runnable).start();
    }
    
    
    
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jScrollPane1 = new javax.swing.JScrollPane();
        lstSearches = new javax.swing.JList();
        cmbConnection = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        btnRefresh = new javax.swing.JButton();
        txtFilter = new javax.swing.JTextField();
        rbXi2 = new javax.swing.JRadioButton();
        rbXi3 = new javax.swing.JRadioButton();
        rbXiCustom = new javax.swing.JRadioButton();
        lblUser = new javax.swing.JLabel();
        txtUser = new javax.swing.JTextField();
        lblPasswd = new javax.swing.JLabel();
        txtPasswd = new javax.swing.JPasswordField();
        ckHidden = new javax.swing.JCheckBox();

        jScrollPane1.setViewportView(lstSearches);

        cmbConnection.setEditable(true);
        cmbConnection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbConnectionActionPerformed(evt);
            }
        });

        jLabel1.setText("Runs");

        btnRefresh.setText("refresh");
        btnRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshActionPerformed(evt);
            }
        });

        txtFilter.setText("Filter");
        txtFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtFilterActionPerformed(evt);
            }
        });
        txtFilter.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txtFilterFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtFilterFocusLost(evt);
            }
        });

        buttonGroup1.add(rbXi2);
        rbXi2.setText("xi2");
        rbXi2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbXi2ActionPerformed(evt);
            }
        });

        buttonGroup1.add(rbXi3);
        rbXi3.setSelected(true);
        rbXi3.setText("xi3");
        rbXi3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbXi3ActionPerformed(evt);
            }
        });

        buttonGroup1.add(rbXiCustom);
        rbXiCustom.setText("custom");
        rbXiCustom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbXiCustomActionPerformed(evt);
            }
        });

        lblUser.setText("User:");

        lblPasswd.setText("Password:");

        ckHidden.setText("show hidden");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 644, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtFilter)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRefresh))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(rbXi2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(rbXi3)
                                .addGap(18, 18, 18)
                                .addComponent(rbXiCustom)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(cmbConnection, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(18, 18, 18)
                                .addComponent(lblUser)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtUser, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(lblPasswd)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(txtPasswd, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(ckHidden))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rbXi2)
                    .addComponent(rbXi3)
                    .addComponent(rbXiCustom)
                    .addComponent(ckHidden))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmbConnection, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblUser)
                    .addComponent(txtUser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPasswd)
                    .addComponent(txtPasswd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnRefresh)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 178, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
        loadList();
    }//GEN-LAST:event_btnRefreshActionPerformed

    private void txtFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtFilterActionPerformed
        if (m_model == null || m_model.getSize() == 0) {
            btnRefreshActionPerformed(evt);
        } else {
            String filter = txtFilter.getText();
            if (!filter.contentEquals("Filter")) {
                m_model.filter(filter);
            }
        }
    }//GEN-LAST:event_txtFilterActionPerformed

    private void txtFilterFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtFilterFocusGained
        if (txtFilter.getText().equals("Filter"))
        txtFilter.setText("");
    }//GEN-LAST:event_txtFilterFocusGained

    private void txtFilterFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtFilterFocusLost
        if (txtFilter.getText().isEmpty())
        txtFilter.setText("Filter");
    }//GEN-LAST:event_txtFilterFocusLost

    private void rbXi3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbXi3ActionPerformed
        cmbConnection.setSelectedItem(xi3);
        cmbConnectionActionPerformed(evt);
        rbXiCustomActionPerformed(evt);
    }//GEN-LAST:event_rbXi3ActionPerformed

    private void rbXiCustomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbXiCustomActionPerformed
//        cmbConnection.setSelectedItem(xi);
        cmbConnection.setVisible(rbXiCustom.isSelected());
        txtPasswd.setVisible(rbXiCustom.isSelected());
        lblPasswd.setVisible(rbXiCustom.isSelected());
        txtUser.setVisible(rbXiCustom.isSelected());
        lblUser.setVisible(rbXiCustom.isSelected());
    }//GEN-LAST:event_rbXiCustomActionPerformed

    private void rbXi2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbXi2ActionPerformed
        cmbConnection.setSelectedItem(xi);
        cmbConnectionActionPerformed(evt);
        rbXiCustomActionPerformed(evt);
    }//GEN-LAST:event_rbXi2ActionPerformed

    private void cmbConnectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbConnectionActionPerformed
        if (cmbConnection.getSelectedIndex()>=0 ) {
            DBConnectionConfig.DBServer s = (DBConnectionConfig.DBServer)cmbConnection.getSelectedItem();
            txtUser.setText(s.user);
            txtPasswd.setText(s.password);
        }
    }//GEN-LAST:event_cmbConnectionActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnRefresh;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JCheckBox ckHidden;
    private javax.swing.JComboBox cmbConnection;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblPasswd;
    private javax.swing.JLabel lblUser;
    private javax.swing.JList lstSearches;
    private javax.swing.JRadioButton rbXi2;
    private javax.swing.JRadioButton rbXi3;
    private javax.swing.JRadioButton rbXiCustom;
    private javax.swing.JTextField txtFilter;
    private javax.swing.JPasswordField txtPasswd;
    private javax.swing.JTextField txtUser;
    // End of variables declaration//GEN-END:variables
}
