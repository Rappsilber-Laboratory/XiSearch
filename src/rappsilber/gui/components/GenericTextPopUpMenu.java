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
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
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
            } else if (c instanceof Container)  
                installContextMenu((Container) c);  
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

        this.add(selectAllFunc);
        this.add(copyFunc);
        this.add(cutFunc);
        this.add(copyAllFunc);
        this.add(cutAllFunc);
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
    
}
