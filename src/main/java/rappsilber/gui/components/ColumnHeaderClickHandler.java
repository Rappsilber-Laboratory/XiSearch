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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

/**
 * Salvaged from
 * http://www.exampledepot.com/egs/javax.swing.table/ColHeadEvent.html
 * @author 
 */
public abstract class ColumnHeaderClickHandler extends MouseAdapter {
    public void mouseClicked(MouseEvent evt) {
        JTable table = ((JTableHeader)evt.getSource()).getTable();
        TableColumnModel colModel = table.getColumnModel();

        // The index of the column whose header was clicked
        int vColIndex = colModel.getColumnIndexAtX(evt.getX());
        int mColIndex = table.convertColumnIndexToModel(vColIndex);

        // Return if not clicked on any column header
        if (vColIndex == -1) {
            return;
        }
        headerClicked(mColIndex,evt);

        
//        // Determine if mouse was clicked between column heads
//        Rectangle headerRect = table.getTableHeader().getHeaderRect(vColIndex);
//        if (vColIndex == 0) {
//            headerRect.width -= 3;    // Hard-coded constant
//        } else {
//            headerRect.grow(-3, 0);   // Hard-coded constant
//        }
//        if (!headerRect.contains(evt.getX(), evt.getY())) {
//            // Mouse was clicked between column heads
//            // vColIndex is the column head closest to the click
//
//            // vLeftColIndex is the column head to the left of the click
//            int vLeftColIndex = vColIndex;
//            if (evt.getX() < headerRect.x) {
//                vLeftColIndex--;
//            }
//        }
    }
    
    public abstract void headerClicked(int column, MouseEvent evt);
    
}
