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
package rappsilber.gui.components;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import rappsilber.data.csv.CSVRandomAccess;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class CSVPanel extends javax.swing.JPanel {



    private class CSVListTableModel extends AbstractTableModel implements CSVRandomAccess.LoadListener {

        int m_columcount = 0;
        int m_rowcount = 0;
        CSVPanel m_parent;

        public CSVListTableModel(CSVPanel p) {
            m_parent = p;
            if (getCSV() != null) {
                getCSV().addListenerComplete(this);
                getCSV().addListenerProgress(this);
            }
        }

        public int getRowCount() {
            CSVRandomAccess csv = getCSV();
            if (csv == null) {
                return 0;
            }
            m_rowcount = csv.getRowCount();
            return Math.max(m_rowcount,20)+1;
        }

        public int getColumnCount() {
            CSVRandomAccess csv = getCSV();
            if (csv == null) {
                return 0;
            }
            m_columcount = csv.getMaxColumns();
            return Math.max(m_columcount,20)+1;
        }

        public String getColumnName(int columnIndex) {
            return getCSV().getHeader(columnIndex);
//            if (getCSV().hasHeader()) {
//                return getCSV().getHeader(columnIndex);
//            } else {
//                return Integer.toString(columnIndex);
//            }

        }

        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return m_editable;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            String v =getCSV().getValue(columnIndex, rowIndex);
            return (v==null? "": v);
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (getCSV().getValue(columnIndex, rowIndex).equals(aValue)) {
                return;
            }
            
            getCSV().setValue(aValue, columnIndex, rowIndex);
            fireTableCellUpdated(rowIndex, columnIndex);
            if (columnIndex >= m_columcount) {
                m_columcount = getCSV().getMaxColumns();
                csvChanged();
                fireHeaderChanged();
            }
            
        }

        public synchronized void listen(int row, int column) {
            CSVRandomAccess csv = getCSV();
            if (csv==null) {
                   try {
                        SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                fireTableStructureChanged();
                            }
                        });
                    } catch (Error er) {
                        fireTableStructureChanged();
                    }
                   return;
            }
            if (getCSV().getMaxColumns() != m_columcount) {
                m_columcount = getCSV().getMaxColumns() + 1;
//                try {
                    try {
                        SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                fireTableStructureChanged();
                            }
                        });
                    } catch (Error er) {
                        fireTableStructureChanged();
                    }
//                } catch (InterruptedException ex) {
//                    Logger.getLogger(CSVPanel.class.getName()).log(Level.SEVERE, null, ex);
//                } catch (InvocationTargetException ex) {
//                    Logger.getLogger(CSVPanel.class.getName()).log(Level.SEVERE, null, ex);
//                }
            } else if (column < 0) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            fireTableDataChanged();
                        }
                    });
                } catch (Error er) {
                    fireTableDataChanged();
                }
            } else if (getCSV().getRowCount() != m_rowcount) {
                final int last = getCSV().getRowCount() - 1;
                final int first = m_rowcount - 1;
                try {
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            fireTableRowsInserted(first, last);
                        }
                    });
                }catch (Error er) {
                    fireTableRowsInserted(first, last);
                }
            }

        }

        public void csvChanged() {
            try {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        fireTableStructureChanged();
                    }
                });
            }catch (Error er) {
                fireTableStructureChanged();
            }
        }
    }
    
    private class RowNumberModel extends AbstractTableModel {
        CSVListTableModel m_model;

        public RowNumberModel(CSVListTableModel mainmodel) {
            m_model = mainmodel;
            m_model.addTableModelListener(new TableModelListener() {

                public void tableChanged(TableModelEvent e) {
                    fireTableDataChanged();
                }
            });
        }

        public int getRowCount() {
            
            return m_model.getRowCount();
        }

        public int getColumnCount() {
            return 1;
        }
        
        public String getColumnName(int column) {
            return "";
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return rowIndex + 1;
        }
        
       
        
        
    }

    private CSVRandomAccess m_csv;
//    ArrayList<String[]> m_csvData = new ArrayList<String[]>();
    //int                          m_maxColumns = 0;
    private CSVListTableModel m_model;
    private boolean m_showLoadPanel = true;
    private boolean m_showSavePanel = true;

    private boolean m_autoLoadFile = false;

    private boolean m_editable = false;


   
    private boolean m_hasHeader = false;

    private GenericJTablePopUpMenu m_popup = new GenericJTablePopUpMenu("Tabel Copy/Paste", true);
    /**
     * The button model's <code>ActionListener</code>.
     */
    protected ActionListener actionListener = null;    

    ArrayList<java.awt.event.ActionListener> m_listenerHeaderChanged = new ArrayList<java.awt.event.ActionListener>();

    
    java.awt.event.ActionListener m_csvheaderChanged = new java.awt.event.ActionListener() {

        public void actionPerformed(ActionEvent e) {
            fireHeaderChanged();
        }
        
    };


    /** Creates new form CSVPanel */
    public CSVPanel() {
        initComponents();
        String[] extensions = new String[]{".csv", ".tsv", ".txt"};
        fbCSVRead.setDescription("TextTables(CSV,TSV)");
        fbCSVWrite.setDescription("TextTables(CSV,TSV)");
        fbCSVRead.setExtensions(extensions);
        fbCSVWrite.setExtensions(extensions);
        tblCSV.getTableHeader().addMouseListener(new ColumnHeaderClickHandler() {

            int lastColumn = -1;
            boolean wasForward = true;

            @Override
            public void headerClicked(int column, MouseEvent evt) {
                
                if (column != lastColumn) {
                    if (evt.getButton()==1) {
                        m_csv.sortAlpha(column);
                    } else {
                        m_csv.sortNumeric(column);
                    }
                    wasForward = true;
                    lastColumn = column;
                } else {
                    if (wasForward) {
                        if (evt.getButton()==1) {
                            m_csv.sortAlphaReverse(column);
                        } else {
                            m_csv.sortNumericReverse(column);
                        }
                        wasForward = false;
                    } else {
                        if (evt.getButton()==1) {
                            m_csv.sortAlpha(column);
                        } else {
                            m_csv.sortNumeric(column);
                        }
                        wasForward = true;
                    }

                }
            }
        });
        
        if (m_model != null) {
            jScrollPane1.setRowHeaderView(new JTable(new RowNumberModel(m_model)));
        }        
        
        fbCSVRead.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (isAutoLoadFile() && fbCSVRead.getFile() != null && fbCSVRead.getFile().exists()) {
                    if (getCSV() == null || getCSV().getInputFile() == null || !fbCSVRead.getFile().equals(getCSV().getInputFile())) {
                        load();
                    }
                }
            }
        });
    }

    /**
     * @return the m_autoLoadFile
     */
    public boolean isAutoLoadFile() {
        return m_autoLoadFile;
    }
    
    
    /**
     * @return the m_editable
     */
    public boolean isEditable() {
        return m_editable;
    }

    /**
     * @param m_editable the m_editable to set
     */
    public void setEditable(boolean m_editable) {
        boolean olde = this.m_editable;
        this.m_editable = m_editable;
        if (m_editable) {
            if (getCSV() == null) {
                CSVRandomAccess csv = new CSVRandomAccess(',', '"');
                setCSV(csv);
            }
            if (!olde) {
                m_popup.installContextMenu(tblCSV);
            }
        } else {
            if (olde) {
                m_popup.removeContextMenu(tblCSV);
            }
                
        }
        csvChanged();
    }    
    
    /**
     * @param m_autoLoadFile the m_autoLoadFile to set
     */
    public void setAutoLoadFile(boolean m_autLoadFile) {
        this.m_autoLoadFile = m_autLoadFile;
        
        //btnLoad.setVisible(!m_autLoadFile);
    }
    
    public void setCSV(CSVRandomAccess csv) {
        if (m_csv != null) {
            m_csv.removeListenerComplete(m_model);
            m_csv.removeListenerComplete(m_model);
            m_csv.removeHeaderChangedListener(m_csvheaderChanged);
        }
        m_csv = csv;

        if (m_csv!= null) {
            m_csv.addHeaderChangedListener(m_csvheaderChanged);
        }
        
        if (m_model == null) {
            m_model = new CSVListTableModel(this);
            tblCSV.setModel(m_model);
            JTable rownumbers = new JTable(new RowNumberModel(m_model));
            rownumbers.getColumnModel().getColumn(0).setCellRenderer(tblCSV.getTableHeader().getDefaultRenderer());
            rownumbers.setBackground(this.getBackground());
//            rownumbers.setPreferredSize(new Dimension(20, -1));
           // TableColumn tc = rownumbers.getColumn(0);
            rownumbers.getColumnModel().getColumn(0).setWidth(50);
            rownumbers.setPreferredScrollableViewportSize(rownumbers.getPreferredSize());
            
//            rownumbers.getColumnModel().getColumn(0).setMaxWidth(50);
//            rownumbers.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
//                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
//
//                    //Cells are by default rendered as a JLabel.
//                    JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
//                    l.setBorder(new BevelBorder(BevelBorder.RAISED));
//                    l.setBackground(Color.LIGHT_GRAY);
//
//
//                    //Return the JLabel which renders the cell.
//                    return l;
//                }
//
//            });
//            rownumbers.getColumn(0).setWidth(100);
            jScrollPane1.setRowHeaderView(rownumbers);
        } else if (m_csv!= null) {
            m_csv.addListenerComplete(m_model);
            m_csv.addListenerProgress(m_model);
            m_csv.addListenerColumnsChanged(m_model);
            m_csv.addListenerSort(m_model);            
        }



        if (m_csv!= null) {
//            m_csv.addListenerComplete(m_model);
//            m_csv.addListenerProgress(m_model);
//            m_csv.addListenerColumnsChanged(m_model);
//            m_csv.addListenerSort(m_model);

            tblCSV.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            for (int i = 0; i < tblCSV.getColumnCount(); i++) {
                tblCSV.getColumnModel().getColumn(0).setPreferredWidth(30);
            }

            m_csv.addListenerComplete(new CSVRandomAccess.LoadListener() {

                public void listen(int row, int column) {
                    fireActionPerformed(new ActionEvent(this, 1, "load complete"));
                    m_model.csvChanged();
                }
                
            });
        }
        m_model.csvChanged();

    
    }

    
    /**
     * Adds an <code>ActionListener</code> to the button.
     * @param l the <code>ActionListener</code> to be added
     */
    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    /**
     * Removes an <code>ActionListener</code> from the button.
     * If the listener is the currently set <code>Action</code>
     * for the button, then the <code>Action</code>
     * is set to <code>null</code>.
     *
     * @param l the listener to be removed
     */
    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }   



    
    /**
     * Returns an array of all the <code>ActionListener</code>s added
     * to this AbstractButton with addActionListener().
     *
     * @return all of the <code>ActionListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.4
     */
    public ActionListener[] getActionListeners() {
        return (ActionListener[])(listenerList.getListeners(
            ActionListener.class));
    }
    
    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created using the <code>event</code>
     * parameter.
     *
     * @param event  the <code>ActionEvent</code> object
     * @see EventListenerList
     */
    protected void fireActionPerformed(ActionEvent event) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        ActionEvent e = null;
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ActionListener.class) {
                // Lazily create the event:
                if (e == null) {
                      String actionCommand = event.getActionCommand();
                      e = new ActionEvent(this,
                                          ActionEvent.ACTION_PERFORMED,
                                          actionCommand,
                                          event.getWhen(),
                                          event.getModifiers());
                }
                ((ActionListener)listeners[i+1]).actionPerformed(e);
            }
        }
    }    
    
    /**
     * @return the m_csv
     */
    public CSVRandomAccess getCSV() {
        return m_csv;
    }

    /**
     * @return the m_hasHeader
     */
    public boolean getHasHeader() {
        return m_hasHeader;
    }

    /**
     * @param m_hasHeader the m_hasHeader to set
     */
    public void setHasHeader(boolean m_hasHeader) {
        boolean old_hasHeader = this.m_hasHeader;
        this.m_hasHeader = m_hasHeader;
        ckHasHeader.setSelected(m_hasHeader);
        if (m_csv != null && old_hasHeader != m_hasHeader) {
            m_csv.switchHeader();
            csvChanged();
        }
    }

    public void setAutoCreateRowSorter(boolean create) {
        tblCSV.setAutoCreateRowSorter(create);
    }

    public boolean getAutoCreateRowSorter() {
        return tblCSV.getAutoCreateRowSorter();
    }

    public static void main(String args[]) {
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                JFrame window = new JFrame();
                window.setLayout(new BorderLayout());
                CSVPanel csv = new CSVPanel();
                window.setPreferredSize(csv.getPreferredSize());
                window.add(csv, BorderLayout.CENTER);

                window.setVisible(true);
            }
        });

    }

    public void load() {
        try {
            CSVRandomAccess csv = CSVRandomAccess.guessCsvAsync(fbCSVRead.getFile(), 30, ckHasHeader.isSelected());
            setCSV(csv);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CSVPanel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CSVPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
//        m_model = new CSVListTableModel(this);
//        tblCSV.setModel(m_model);
//        tblCSV.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//        for (int i = 0; i < tblCSV.getColumnCount(); i++) {
//            tblCSV.getColumnModel().getColumn(0).setPreferredWidth(30);
//        }
    }
    
    public void save() {
        try {
            PrintWriter pw = new PrintWriter(fbCSVWrite.getFile());
            m_csv.writeFile(pw);
            pw.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CSVPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @return the m_showLoadPanel
     */
    public boolean getShowLoadPanel() {
        return m_showLoadPanel;
    }
    
    /**
     * @param m_showLoadPanel the m_showLoadPanel to set
     */
    public void setShowLoadPanel(boolean showLoadPanel) {
        this.m_showLoadPanel = showLoadPanel;
        pLoad.setVisible(showLoadPanel);
    }


 
    
    /**
     * @return the m_showSavePanel
     */
    public boolean getShowSavePanel() {
        return m_showSavePanel;
    }

    /**
     * @param m_showSavePanel the m_showSavePanel to set
     */
    public void setShowSavePanel(boolean showSavePanel) {
        this.m_showSavePanel = showSavePanel;
        pSave.setVisible(showSavePanel);
    }
    
    public void csvChanged()  {
        ((CSVListTableModel) tblCSV.getModel()).csvChanged();        
    }
    
    
    public void addHeaderChangedListener(java.awt.event.ActionListener l) {
        m_listenerHeaderChanged.add(l);
    }
    
    public void removeHeaderChangedListener(java.awt.event.ActionListener l) {
        m_listenerHeaderChanged.remove(l);
    }
    
    protected void fireHeaderChanged() {
        ActionEvent evt = new ActionEvent(this,ActionEvent.ACTION_PERFORMED,"HEADER CHANGED" );
        for (java.awt.event.ActionListener e : m_listenerHeaderChanged) {
            e.actionPerformed(evt);
        }
                    
    }
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pLoad = new javax.swing.JPanel();
        fbCSVRead = new rappsilber.gui.components.FileBrowser();
        ckHasHeader = new javax.swing.JCheckBox();
        btnLoad = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblCSV = new javax.swing.JTable();
        pSave = new javax.swing.JPanel();
        fbCSVWrite = new rappsilber.gui.components.FileBrowser();
        btnSave = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();

        setLayout(new java.awt.BorderLayout());

        ckHasHeader.setText("has header");
        ckHasHeader.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckHasHeaderActionPerformed(evt);
            }
        });

        btnLoad.setText("Load");
        btnLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLoadActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pLoadLayout = new javax.swing.GroupLayout(pLoad);
        pLoad.setLayout(pLoadLayout);
        pLoadLayout.setHorizontalGroup(
            pLoadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pLoadLayout.createSequentialGroup()
                .addComponent(fbCSVRead, javax.swing.GroupLayout.DEFAULT_SIZE, 169, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(ckHasHeader)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnLoad, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        pLoadLayout.setVerticalGroup(
            pLoadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pLoadLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pLoadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(fbCSVRead, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(pLoadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnLoad, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(ckHasHeader)))
                .addGap(40, 40, 40))
        );

        add(pLoad, java.awt.BorderLayout.NORTH);

        tblCSV.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        tblCSV.setColumnSelectionAllowed(true);
        tblCSV.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        jScrollPane1.setViewportView(tblCSV);

        add(jScrollPane1, java.awt.BorderLayout.CENTER);

        btnSave.setText("Save");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        jButton1.setText("jButton1");
        jButton1.setMaximumSize(new java.awt.Dimension(10, 10));
        jButton1.setMinimumSize(new java.awt.Dimension(10, 10));
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pSaveLayout = new javax.swing.GroupLayout(pSave);
        pSave.setLayout(pSaveLayout);
        pSaveLayout.setHorizontalGroup(
            pSaveLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pSaveLayout.createSequentialGroup()
                .addComponent(fbCSVWrite, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
                .addGap(2, 2, 2)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSave))
        );
        pSaveLayout.setVerticalGroup(
            pSaveLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pSaveLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pSaveLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, pSaveLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnSave, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 13, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(fbCSVWrite, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        add(pSave, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    private void btnLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLoadActionPerformed
        load();
    }//GEN-LAST:event_btnLoadActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        save();
    }//GEN-LAST:event_btnSaveActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        ((CSVListTableModel) tblCSV.getModel()).csvChanged();
        
    }//GEN-LAST:event_jButton1ActionPerformed

    private void ckHasHeaderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ckHasHeaderActionPerformed
        
        if (m_csv != null) {
            m_csv.switchHeader();
            csvChanged();
        }
    
            
    }//GEN-LAST:event_ckHasHeaderActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnLoad;
    private javax.swing.JButton btnSave;
    private javax.swing.JCheckBox ckHasHeader;
    private rappsilber.gui.components.FileBrowser fbCSVRead;
    private rappsilber.gui.components.FileBrowser fbCSVWrite;
    private javax.swing.JButton jButton1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel pLoad;
    private javax.swing.JPanel pSave;
    private javax.swing.JTable tblCSV;
    // End of variables declaration//GEN-END:variables
}
