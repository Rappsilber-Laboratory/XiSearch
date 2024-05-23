/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rappsilber.gui.components;

import java.awt.Dimension;
import java.util.Vector;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
//      http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4618607 
public class WideComboBox extends JComboBox{ 
    private Integer openWidth;
    public WideComboBox() { 
    } 

    public WideComboBox(final Object items[]){ 
        super(items); 
    } 

    public WideComboBox(Vector items) { 
        super(items); 
    } 

        public WideComboBox(ComboBoxModel aModel) { 
        super(aModel); 
    } 

    private boolean layingOut = false; 

    public void doLayout(){ 
        try{ 
            layingOut = true; 
                super.doLayout(); 
        }finally{ 
            layingOut = false; 
        } 
    } 

    public Dimension getSize(){ 
        Dimension dim = super.getSize(); 
        if(!layingOut) {  
            if (openWidth == null) {
                dim.width = Math.max(dim.width, getPreferredSize().width); 
            } else {
                dim.width = Math.max(dim.width, openWidth);
            }
        }
        return dim; 
    } 

    /**
     * Get the minimum width of the open combobox
     * @return the openWidth
     */
    public Integer getOpenWidth() {
        return openWidth;
    }

    /**
     * @param openWidth the openWidth to set
     */
    public void setOpenWidth(Integer openWidth) {
        this.openWidth = openWidth;
    }
}