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

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class AutoAddTableModelListener implements  TableModelListener {

    protected Object[] newEmptyRow(TableModel tm) {

        Object[] ret = new Object[tm.getColumnCount()];
        for (int i = tm.getColumnCount();i-->0;) {
            ret[i]=null;
        }

        return ret;
    }

    public boolean isRowEmpty(TableModel tm, int row) {
        for (int i = tm.getColumnCount(); i-->0;) {
            Object v = tm.getValueAt(row, i);
            if (v != null && v.toString().length() > 0)
                return false;
        }
        return true;
    }

    public void tableChanged(TableModelEvent e) {
        if (e.getSource() instanceof DefaultTableModel) {
            DefaultTableModel dtm = (DefaultTableModel) e.getSource();
            // check for wmty lines
            int lastrow = e.getLastRow();

            if (lastrow +1 == dtm.getRowCount())
                lastrow--;

            int r = e.getFirstRow();

            while (r<= lastrow)
                if (isRowEmpty(dtm, r)) {
                    dtm.removeRow(r);
                    lastrow--;
                } else
                    r++;

            if (! isRowEmpty(dtm, dtm.getRowCount() -1))
                dtm.addRow(newEmptyRow(dtm));

        } else if (e.getSource() instanceof AbstractAddableTableModel) {
            AbstractAddableTableModel aatm = (AbstractAddableTableModel) e.getSource();
            // check for wmty lines
            int lastrow = e.getLastRow();

            if (lastrow +1 == aatm.getRowCount())
                lastrow--;

            int r = e.getFirstRow();

            while (r< lastrow)
                if (isRowEmpty(aatm, r)) {
                    aatm.removeRow(r);
                    lastrow--;
                } else
                    r++;

            if (! isRowEmpty(aatm, aatm.getRowCount() -1))
                aatm.addRow(newEmptyRow(aatm));
        }


    }

}
