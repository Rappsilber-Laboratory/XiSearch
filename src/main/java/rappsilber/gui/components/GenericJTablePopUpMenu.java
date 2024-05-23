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
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import rappsilber.data.csv.CSVRandomAccess;
//import javax.swing.text.JTextComponent;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class GenericJTablePopUpMenu extends JPopupMenu  implements ActionListener  {
    
    private Clipboard system = Toolkit.getDefaultToolkit().getSystemClipboard();;
    
    JMenuItem selectAllFunc;
    JMenuItem copyFunc;
    JMenuItem cutFunc;
    JMenuItem copyAllFunc;
    JMenuItem cutAllFunc;
    JMenuItem pasteFunc;
    //JTable    table;
    boolean   autoIncrese = false;
    KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK, false);
    KeyStroke paste = KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK, false);
    
    GenericJTablePopUpMenu popup = this; 
    
    MouseAdapter mouseadapter = new MouseAdapter() { 
            GenericJTablePopUpMenu textpopup  = popup;
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
        };
    
    public GenericJTablePopUpMenu(String label,boolean autoIncrese, JTable table) {
        this(label, autoIncrese);
        installContextMenu(table);
    }

    public GenericJTablePopUpMenu(String label, boolean autoIncrese) {
        super(label);
        initPopupMenu();
        this.autoIncrese = autoIncrese;
        
    }
    
    public GenericJTablePopUpMenu() {
        initPopupMenu();
    }


    public void installContextMenu(JTable c) {  
        c.addMouseListener(mouseadapter);  
        
        c.registerKeyboardAction(this, "JTableCopy", copy, JComponent.WHEN_FOCUSED);
        c.registerKeyboardAction(this, "JTablePaste", paste, JComponent.WHEN_FOCUSED);
        
    }      

    public void removeContextMenu(JTable c) {  
        c.removeMouseListener(mouseadapter);  
        c.unregisterKeyboardAction(copy);
        c.unregisterKeyboardAction(paste);
    }      
    
    public void show(Component c, int x, int y) {
       JTable txt = ((JTable) c);
       boolean hasSelection =txt.getSelectedColumnCount() != 0;
//       boolean hasText = !txt.getText().isEmpty();
       this.selectAllFunc.setVisible(true);
       this.copyFunc.setEnabled(hasSelection);
//       this.cutFunc.setVisible(hasSelection);
       this.copyAllFunc.setVisible(true);
//       this.cutAllFunc.setVisible(hasText);
       this.pasteFunc.setEnabled(clipboardHasText());
           
       super.show(c, x, y);
    }
    
    private void initPopupMenu() {

  
        selectAllFunc = new JMenuItem("Select all");
        selectAllFunc.setMnemonic(KeyEvent.VK_S);

        selectAllFunc.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ((JTable) getInvoker()).selectAll();
            }
        });        
        
        copyFunc = new JMenuItem("Copy");
        copyFunc.setMnemonic(KeyEvent.VK_C);

        copyFunc.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                copy(((JTable) getInvoker()));
            }
        });

//        cutFunc = new JMenuItem("Cut");
//        cutFunc.setMnemonic(KeyEvent.VK_T);
//        cutFunc.addActionListener(new ActionListener() {
//
//            public void actionPerformed(ActionEvent e) {
//                ((JTextComponent) getInvoker()).cut();
//            }
//        });

        copyAllFunc = new JMenuItem("Copy all");
        copyAllFunc.setMnemonic(KeyEvent.VK_A);

        copyAllFunc.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JTable t = ((JTable) getInvoker());
                
//                int selStart = ((JTable) getInvoker()).getSelectionStart();
//                int selEnd = ((JTable) getInvoker()).getSelectionEnd();
                t.selectAll();
                copy(t);
                //((JTextComponent) getInvoker()).select(selStart, selEnd);
            }
        });

//        cutAllFunc = new JMenuItem("Cut all");
//        cutAllFunc.setMnemonic(KeyEvent.VK_L);
//        cutAllFunc.addActionListener(new ActionListener() {
//
//            public void actionPerformed(ActionEvent e) {
//                ((JTextComponent) getInvoker()).selectAll();
//                ((JTextComponent) getInvoker()).cut();
//            }
//        });
        
        pasteFunc = new JMenuItem("Paste");
        pasteFunc.setMnemonic(KeyEvent.VK_P);
        pasteFunc.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                paste((JTable) getInvoker());
            }
        });

        this.add(selectAllFunc);
        this.add(copyFunc);
//        this.add(cutFunc);
        this.add(copyAllFunc);
//        this.add(cutAllFunc);
        this.add(pasteFunc);
    }
    
    public boolean clipboardHasText() {
        Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        try {
            if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String text = (String)t.getTransferData(DataFlavor.stringFlavor);
                return true;
            }
        } catch (UnsupportedFlavorException e) {
        } catch (IOException e) {
        }
    
        return false;        
    }
    
    
    
    public void copy(JTable table) {
        StringSelection stsel;
        StringBuffer sbf = new StringBuffer();
        // Check to ensure we have selected only a contiguous block of
        // cells
        int numcols = table.getSelectedColumnCount();
        int numrows = table.getSelectedRowCount();
        int[] rowsselected = table.getSelectedRows();
        int[] colsselected = table.getSelectedColumns();
        if (!((numrows - 1 == rowsselected[rowsselected.length - 1] - rowsselected[0]
                && numrows == rowsselected.length)
                && (numcols - 1 == colsselected[colsselected.length - 1] - colsselected[0]
                && numcols == colsselected.length))) {
            JOptionPane.showMessageDialog(null, "Invalid Copy Selection",
                    "Invalid Copy Selection",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        for (int i = 0; i < numrows; i++) {
            for (int j = 0; j < numcols; j++) {
                sbf.append(table.getValueAt(rowsselected[i], colsselected[j]));
                if (j < numcols - 1) {
                    sbf.append("\t");
                }
            }
            sbf.append("\n");
        }
        stsel = new StringSelection(sbf.toString());
//        system = Toolkit.getDefaultToolkit().getSystemClipboard();
        system.setContents(stsel, stsel);
        
    }
    
    public void paste(JTable table) {
        String rowstring;
        String value;
//        System.out.println("Trying to Paste");
        int startRow = (table.getSelectedRows())[0];
        int startCol = (table.getSelectedColumns())[0];
        try {
            String trstring = (String) (system.getContents(this).getTransferData(DataFlavor.stringFlavor));
            
            CSVRandomAccess trcsv = new CSVRandomAccess('\t', '"', trstring);
//            System.out.println("String is:" + trstring);
            for (int r = 0; r< trcsv.getRowCount(); r ++) {
                for (int c =0; c< trcsv.getMaxColumns(); c++) {
                    try {
                        table.setValueAt(trcsv.getValue(c, r), startRow + r, startCol + c);
                    } catch (Exception e) {
                        Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Could not paste");
                    }
                }
            }
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }        
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().compareTo("JTableCopy") == 0) {
            copy((JTable) e.getSource());
        } if (e.getActionCommand().compareTo("JTablePaste") == 0) {
            paste((JTable) e.getSource());
        }
    }
    
}
