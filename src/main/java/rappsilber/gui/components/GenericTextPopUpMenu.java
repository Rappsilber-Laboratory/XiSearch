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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
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
import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class GenericTextPopUpMenu extends JPopupMenu {
    JMenuItem selectAllFunc;
    JMenuItem copyFunc;
    JMenuItem cutFunc;
    JMenuItem copyAllFunc;
    JMenuItem cutAllFunc;
    JMenuItem pasteFunc;
    JMenuItem searchFunc;
    
    public GenericTextPopUpMenu(String label) {
        super(label);
        initPopupMenu();
    }

    public GenericTextPopUpMenu() {
        super();
        initPopupMenu();
    }


    public void installContextMenu(Container comp) {  
        final GenericTextPopUpMenu popup = this;
        for (Component c : comp.getComponents()) {  
            if (c instanceof JTextComponent) {  
                c.addMouseListener(new MouseAdapter() { 
                    GenericTextPopUpMenu textpopup  = popup;
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
                if (c instanceof JTextArea) {
                    final JTextArea ta = (JTextArea) c;
                    ta.getInputMap().put(KeyStroke.getKeyStroke( KeyEvent.VK_F, KeyEvent.CTRL_MASK ),
                                                "doSearch");
                    ta.getActionMap().put("doSearch",
                                                 new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            search(ta);
                        }
                    });                
                }
            } else if (c instanceof Container) {
                installContextMenu((Container) c);
            }  
        }  
    }      
    
    public void show(Component c, int x, int y) {
       JTextComponent txt = ((JTextComponent) c);
       
       boolean hasSelection =txt.getSelectedText() != null;
       boolean hasText = !txt.getText().isEmpty();
       this.selectAllFunc.setVisible(hasText);
       this.copyFunc.setEnabled(hasSelection);
       this.cutFunc.setVisible(hasSelection);
       this.copyAllFunc.setVisible(hasText);
       this.cutAllFunc.setVisible(hasText);
       this.pasteFunc.setEnabled(clipboardHasText());
       
       this.searchFunc.setEnabled(txt instanceof JTextArea);
           
       super.show(c, x, y);
    }
    
    private void initPopupMenu() {

  
        selectAllFunc = new JMenuItem("Select all");
        selectAllFunc.setMnemonic(KeyEvent.VK_S);

        selectAllFunc.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ((JTextComponent) getInvoker()).selectAll();
            }
        });        
        
        copyFunc = new JMenuItem("Copy");
        copyFunc.setMnemonic(KeyEvent.VK_C);

        copyFunc.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ((JTextComponent) getInvoker()).copy();
            }
        });

        cutFunc = new JMenuItem("Cut");
        cutFunc.setMnemonic(KeyEvent.VK_T);
        cutFunc.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ((JTextComponent) getInvoker()).cut();
            }
        });

        copyAllFunc = new JMenuItem("Copy all");
        copyAllFunc.setMnemonic(KeyEvent.VK_A);

        copyAllFunc.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int selStart = ((JTextComponent) getInvoker()).getSelectionStart();
                int selEnd = ((JTextComponent) getInvoker()).getSelectionEnd();
                ((JTextComponent) getInvoker()).selectAll();
                ((JTextComponent) getInvoker()).copy();
                ((JTextComponent) getInvoker()).select(selStart, selEnd);
            }
        });

        cutAllFunc = new JMenuItem("Cut all");
        cutAllFunc.setMnemonic(KeyEvent.VK_L);
        cutAllFunc.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ((JTextComponent) getInvoker()).selectAll();
                ((JTextComponent) getInvoker()).cut();
            }
        });
        
        pasteFunc = new JMenuItem("Paste");
        pasteFunc.setMnemonic(KeyEvent.VK_P);
        pasteFunc.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ((JTextComponent) getInvoker()).paste();
            }
        });

        searchFunc = new JMenuItem("Search");
        searchFunc.setMnemonic(KeyEvent.VK_E);
        searchFunc.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                search(((JTextComponent) getInvoker()));
            }
        });

        
        this.add(selectAllFunc);
        this.add(new JPopupMenu.Separator());
        this.add(copyFunc);
        this.add(cutFunc);
        this.add(copyAllFunc);
        this.add(cutAllFunc);
        this.add(pasteFunc);
        this.add(new JPopupMenu.Separator());
        this.add(searchFunc);
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
    
    public void search(JTextComponent textField) {
        String text = textField.getText();
        if (text.length()==0) {
            return;
        }
        
        String searchWord=JOptionPane.showInputDialog(textField, "Search:", "Search", JOptionPane.PLAIN_MESSAGE);
        
        if (searchWord != null && searchWord.length() >0) {
            textField.requestFocusInWindow();
            textField.requestFocus();
            
            String searchWordLC = searchWord.toLowerCase();
            if (searchWordLC.contentEquals(searchWord)) {
                text=text.toLowerCase();
            }

            Highlighter.HighlightPainter painter = 
                new DefaultHighlighter.DefaultHighlightPainter( Color.cyan );
            
            textField.getHighlighter().removeAllHighlights();
            
            int currentOffset = textField.getCaretPosition();
            int selStart = textField.getSelectionStart();
            int selEnd = textField.getSelectionEnd();
            int searchStart = currentOffset;
            // do we have a selection?
            if (selStart==selEnd) {
                // no search the whole text
                selStart = 0;
                selEnd = text.length();
            }
            
            // is the current position inside the search area
            if (searchStart <selStart || searchStart >selEnd) {
                searchStart=selStart;
            }
            
            int offset = text.indexOf(searchWord, searchStart);
            int length = searchWord.length();
            if (offset ==-1 || offset>selEnd) {
                if (searchStart!=selStart) {
                    offset = text.indexOf(searchWord, searchStart);
                }
                
            }

            if (offset !=-1 && offset<=selEnd) {
                textField.setCaretPosition(offset);
            }

            while ( offset != -1 && offset<=selEnd)
            {
                textField.requestFocusInWindow();
                textField.requestFocus();
                try
                {
                    
                    textField.getHighlighter().addHighlight(offset, offset + length, painter);
                    offset = text.indexOf(searchWord, offset+1);
                }
                catch(BadLocationException ble) { Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,"Exception during search"); }
            }    

            if (searchStart!=selStart) {
                offset = text.indexOf(searchWord, selStart);
                while ( offset != -1  && offset<=selEnd)
                {
                    try
                    {

                        textField.getHighlighter().addHighlight(offset, offset + length, painter);
                        offset = text.indexOf(searchWord, offset+1);
                    }
                    catch(BadLocationException ble) { Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,"Exception during search"); }
                }    
            }

        }
    }
    
}
