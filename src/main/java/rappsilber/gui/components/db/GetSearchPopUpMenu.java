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

import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.text.JTextComponent;
import rappsilber.utils.MyArrayUtils;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class GetSearchPopUpMenu extends JPopupMenu {
    JMenuItem copySearchID;
    JMenuItem copySearchName;
    GetSearch searchlist;
    
    public GetSearchPopUpMenu(String label,GetSearch searchList) {
        super(label);
        this.searchlist=searchList;
        initPopupMenu();
    }

    public GetSearchPopUpMenu() {
        super();
        initPopupMenu();
    }


    public void installContextMenu(Container comp) {  
        final GetSearchPopUpMenu popup = this;
        for (Component c : comp.getComponents()) {  
            if (c instanceof JTextComponent) {  
                c.addMouseListener(new MouseAdapter() { 
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
            } else if (c instanceof Container) {
                installContextMenu((Container) c);
            }  
        }  
    }      
    
    public void show(Component c, int x, int y) {
        if (((JList)c).getSelectedIndices() != null && ((JList)c).getSelectedIndices().length>0) {
            this.copySearchID.setEnabled(true);
            this.copySearchName.setVisible(true);

            super.show(c, x, y);
        }
    }
    
    private void initPopupMenu() {

  
        
        copySearchName = new JMenuItem("Copy Name");
        copySearchName.setMnemonic(KeyEvent.VK_C);

        copySearchName.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ArrayList<String> ret = new ArrayList<>();
                for (Object o : ((JList)getInvoker()).getSelectedValuesList()) {
                    GetSearch.RunListBoxModel.line l = (GetSearch.RunListBoxModel.line) o;
                    ret.add(l.name);
                }
                Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
                StringSelection stringSelection = new StringSelection(MyArrayUtils.toString(ret, "|"));
                clpbrd.setContents(stringSelection, null);
            }
        });

        copySearchID = new JMenuItem("Cupy ID");
        copySearchID.setMnemonic(KeyEvent.VK_I);
        copySearchID.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ArrayList<Integer> ret = new ArrayList<>();
                for (Object o : ((JList)getInvoker()).getSelectedValuesList()) {
                    GetSearch.RunListBoxModel.line l = (GetSearch.RunListBoxModel.line) o;
                    ret.add(l.id);
                }
                Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
                StringSelection stringSelection = new StringSelection(MyArrayUtils.toString(ret, "|"));
                clpbrd.setContents(stringSelection, null);
            }
        });

        this.add(copySearchName);
        this.add(copySearchID);
    }
    
    
}
