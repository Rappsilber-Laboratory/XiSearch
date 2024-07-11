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

import java.awt.Component;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JFrame;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import rappsilber.config.LocalProperties;
import rappsilber.gui.GetFile;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class FileList extends javax.swing.JPanel {

    /**
     * @return the m_hasSelection
     */
    public boolean hasSelection() {
        return m_hasSelection;
    }

    /**
     * @param m_hasSelection the m_hasSelection to set
     */
    public void setSelection(boolean m_hasSelection) {
        this.m_hasSelection = m_hasSelection;
        m_model.updateFiles();
    }

    /**
     * @return the m_SelectionName
     */
    public String getSelectionName() {
        return m_SelectionName;
    }

    @Override
    public void setToolTipText(String text) {
        super.setToolTipText(text); //To change body of generated methods, choose Tools | Templates.
        tblFiles.setToolTipText(text);
        btnLoadMSM.setToolTipText("Select " + text.substring(0,1).toLowerCase() + text.substring(1));
    }

    
    /**
     * @param m_SelectionName the m_SelectionName to set
     */
    public void setSelectionName(String m_SelectionName) {
        this.m_SelectionName = m_SelectionName;
        setSelection(true);
        this.tblFiles.setModel(new TableModel() {

            public int getRowCount() {
                return 0;
            }

            public int getColumnCount() {
                return 0;
            }

            public String getColumnName(int columnIndex) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public Class<?> getColumnClass(int columnIndex) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                
            }

            public void addTableModelListener(TableModelListener l) {
                
            }

            public void removeTableModelListener(TableModelListener l) {
                
            }
        });
        this.tblFiles.setModel(m_model);
    }
   // private static final long serialVersionUID = 676003385063269187L;



    /**
     * @return the m_SelectionName
     */
    public String getLoadButtonText() {
        return btnLoadMSM.getText();
    }

    /**
     * @param m_SelectionName the m_SelectionName to set
     */
    public void setLoadButtonText(String text) {
        this. btnLoadMSM.setText(text);
    }
   // private static final long serialVersionUID = 676003385063269187L;
    
    
    private class FileTableModel implements TableModel {
        ArrayList<TableModelListener> m_listener = new ArrayList<TableModelListener>();
        HashMap<Integer, Boolean> selection = new HashMap<Integer, Boolean>();
        
        public int getRowCount() {
            return m_file.size();
        }

        public int getColumnCount() {
            if (hasSelection()) {
                return 3;
            }
            return 2;
        }

        public String getColumnName(int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return "File";
                case 1:
                    return "Path";
                case 2:
                    return getSelectionName();
            }

            return "";
        }

        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 2) {
                return Boolean.class;
            }
            return String.class;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 2;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            File f = m_file.get(rowIndex);
            if (columnIndex == 0) {
                return f.getName();
            }
            if (columnIndex == 2) {
                Boolean b = selection.get(Integer.valueOf(rowIndex));
                if (b == null) {
                    b = Boolean.FALSE;
                }
                return b; 
            }
                
            return f.getAbsolutePath();
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 2) {
                selection.put(rowIndex, (Boolean) aValue);
            }
        }

        public void addTableModelListener(TableModelListener l) {
            m_listener.add(l);
        }

        public void removeTableModelListener(TableModelListener l) {
            m_listener.remove(l);
        }
        
        public void updateFiles(){
            for (TableModelListener tml :m_listener) {
                tml.tableChanged(new TableModelEvent(this));
            }
            
        }
        
    }
    
    
    static final String DefaultLocalPropertyKey = "FileListLastAccessedFolder";

    
    
    ArrayList<java.awt.event.ActionListener> m_actionlisteners = new ArrayList<ActionListener>();
    private String[] m_extensions = new String[]{"*"};
    private String m_description = "Files";
    ArrayList<File> m_file = new ArrayList<File>();
    private String m_LocalPropertyKey = DefaultLocalPropertyKey;
    private FileTableModel m_model  = new FileTableModel();
    
    private boolean     m_hasSelection = false;
    private String      m_SelectionName = "select";

    /** Creates new form FileList */
    public FileList() {
        initComponents();
        tblFiles.setModel(m_model);
    }


    public void addFile(File path) {
        if (path  == null) {
            return;
        }
        m_file.add(path);
        
        if ((path.exists()) &&  path.isDirectory()) {
            LocalProperties.setFolder(m_LocalPropertyKey, path);
        } else if (path.getParent() != null) {
            LocalProperties.setFolder(m_LocalPropertyKey, path.getParent());
        }
        
        m_model.updateFiles();

    }
    
    public void removeFile(File path) {
        for (int i = 0; i<m_file.size(); i++) { 
            if (m_file.get(i).getAbsolutePath().contentEquals(path.getAbsolutePath())) {
                removeFile(i);
                return;
            }
        }
    }

    public void removeFile(int i) {
        m_file.remove(i);
        m_model.updateFiles();
    }
    
    
    
    public void detectFrames() {
        if (m_LocalPropertyKey == DefaultLocalPropertyKey) {
            Component p = this;
            String pathKey;
            while (p.getParent() != null) {
                p = p.getParent();
            }
            if (p instanceof JFrame) {
                pathKey = ((JFrame) p).getTitle();
                m_LocalPropertyKey = pathKey;
            }
        }
    }
    
    public File[] getFiles() {
        return m_file.toArray(new File[0]);
    }
    
    
    /**
     * @return the m_LocalPropertyKey
     */
    public String getLocalPropertyKey() {
        return m_LocalPropertyKey;
    }

    /**
     * @param LocalPropertyKey the LocalPropertyKey to set
     */
    public void setLocalPropertyKey(String LocalPropertyKey) {
        this.m_LocalPropertyKey = LocalPropertyKey;
    }

    /**
     * @return the extensions
     */
    public String[] getExtensions() {
        return m_extensions;
    }

    /**
     * @param extensions the extensions to set
     */
    public void setExtensions(String[] extensions) {
        this.m_extensions = extensions;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return m_description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.m_description = description;
    }
    
    
    public boolean isSelected(int i) {
        return (Boolean)m_model.getValueAt(i, 2);
    }
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btnDeleteMSM = new javax.swing.JButton();
        btnLoadMSM = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblFiles = new javax.swing.JTable();

        btnDeleteMSM.setText("Remove");
        btnDeleteMSM.setToolTipText("remove file from list");
        btnDeleteMSM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteMSMActionPerformed(evt);
            }
        });

        btnLoadMSM.setText("Select");
        btnLoadMSM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLoadMSMActionPerformed(evt);
            }
        });
        btnLoadMSM.addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorAdded(javax.swing.event.AncestorEvent evt) {
                formAncestorAdded(evt);
            }
            public void ancestorMoved(javax.swing.event.AncestorEvent evt) {
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent evt) {
            }
        });

        tblFiles.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "File", "Path"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(tblFiles);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(355, Short.MAX_VALUE)
                .addComponent(btnLoadMSM)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnDeleteMSM))
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 528, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 269, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnDeleteMSM)
                    .addComponent(btnLoadMSM)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void formAncestorAdded(javax.swing.event.AncestorEvent evt) {//GEN-FIRST:event_formAncestorAdded
        detectFrames();
    }//GEN-LAST:event_formAncestorAdded

    private void btnLoadMSMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLoadMSMActionPerformed

        File[] files = GetFile.getFiles(m_extensions, m_description, LocalProperties.getFolder(m_LocalPropertyKey).getAbsolutePath(),this);
        if (files != null) {
            for (File f : files) {
                addFile(f);
            }
        
            if ((files[0].exists()) &&  files[0].isDirectory()) {
                LocalProperties.setFolder(m_LocalPropertyKey, files[0]);
            } else {
                LocalProperties.setFolder(m_LocalPropertyKey, files[0].getParent());
            }
        }

    }//GEN-LAST:event_btnLoadMSMActionPerformed

    private void btnDeleteMSMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteMSMActionPerformed
        int[] rows = tblFiles.getSelectedRows();
        java.util.Arrays.sort(rows);
        for (int r = rows.length - 1; r>=0; r--) {
            removeFile(rows[r]);
        }
    }//GEN-LAST:event_btnDeleteMSMActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnDeleteMSM;
    private javax.swing.JButton btnLoadMSM;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable tblFiles;
    // End of variables declaration//GEN-END:variables
}
