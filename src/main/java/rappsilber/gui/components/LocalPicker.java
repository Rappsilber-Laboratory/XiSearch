/*
 * Copyright 2018 Lutz Fischer <lfischer@staffmail.ed.ac.uk>.
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeSupport;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.text.JTextComponent;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class LocalPicker extends JComboBox{
    private Locale defaultLocal = Locale.getDefault();
    LocalComboBoxItem[] items;
    HashMap<Locale, LocalComboBoxItem> lookup;
    
    private class LocalComboBoxItem {
        Locale l;
        String display;
        public String toString() {
            return display;
        }

        public LocalComboBoxItem(Locale l) {
            display=l.getDisplayName() + "(" + l.toString() +")";
            this.l = l;
        }

        
    }
    
    
    public LocalPicker() {
        Locale[] ls = Locale.getAvailableLocales();
        LocalComboBoxItem[] lcb = new LocalComboBoxItem[ls.length];
        lookup = new HashMap<>(ls.length);
        for (int l =0; l<ls.length;l++) {
            lcb[l] = new LocalComboBoxItem(ls[l]);
            lookup.put(ls[l], lcb[l]);
        }
        java.util.Arrays.sort(lcb, new Comparator<LocalComboBoxItem>() {
            @Override
            public int compare(LocalComboBoxItem arg0, LocalComboBoxItem arg1) {
                return arg0.display.compareTo(arg1.display);
            }
        });
        LocalComboBoxItem[] lcbDefault = new LocalComboBoxItem[lcb.length+1];
        System.arraycopy(lcb, 0, lcbDefault, 1, lcb.length);
        lcbDefault[0] = new LocalComboBoxItem(Locale.getDefault());
        lookup.put(Locale.getDefault(),lcbDefault[0]);
        items = lcbDefault;
        setModel(new DefaultComboBoxModel<LocalComboBoxItem>(lcbDefault));
        this.setEditable(true);
        this.addActionListener(new ActionListener() {
            String old = "";
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (getSelectedIndex()<0) {
                    String locale = getSelectedItem().toString().toLowerCase();
                    if (locale.contentEquals(old))
                        return;
                    old = locale;
                    boolean isSet = false;
                    for (int i =0; i<items.length; i++) {
                        LocalComboBoxItem l  = items[i];
                        if (l.l.getDisplayName().toLowerCase().contentEquals(locale) 
                                || l.l.toString().toLowerCase().contentEquals(locale)) {
                            setSelectedIndex(i);
                            isSet = true;
                            break;
                            
                        }
                        if (l.l.getCountry().toLowerCase().contentEquals(locale)) {
                            setSelectedIndex(i);
                            isSet = true;
                        } else if (l.l.getDisplayScript().toLowerCase().contentEquals(locale)) {
                            setSelectedIndex(i);
                            isSet = true;
                        } else if (l.l.getDisplayLanguage().toLowerCase().contentEquals(locale)) {
                            setSelectedIndex(i);
                            isSet = true;
                        }
                    }
                    if (!isSet) {
                        JOptionPane.showMessageDialog(LocalPicker.this, "Could not identify the format");
                    } else {
                        ((JTextComponent)getEditor().getEditorComponent()).setCaretPosition(0);
                    }
                } else {
                    ((JTextComponent)getEditor().getEditorComponent()).setCaretPosition(0);
                }
                
            }
        });
    }

    /**
     * @return the defaultLocal
     */
    public Locale getDefaultLocal() {
        return defaultLocal;
    }

    /**
     * @param defaultLocal the defaultLocal to set
     */
    public void setDefaultLocal(Locale defaultLocal) {
        java.util.Locale oldDefaultLocal = this.defaultLocal;
        this.defaultLocal = defaultLocal;
        this.setSelectedItem(lookup.get(defaultLocal));
        propertyChangeSupport.firePropertyChange(PROP_DEFAULTLOCAL, oldDefaultLocal, defaultLocal);
    }
    private final transient PropertyChangeSupport propertyChangeSupport = new java.beans.PropertyChangeSupport(this);
    public static final String PROP_DEFAULTLOCAL = "defaultLocal";
    
    public Locale getSelectLocale() {
        return ((LocalComboBoxItem)this.getSelectedItem()).l;
    }
    
}
