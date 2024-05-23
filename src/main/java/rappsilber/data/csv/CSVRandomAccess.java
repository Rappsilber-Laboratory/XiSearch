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
package rappsilber.data.csv;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import rappsilber.ms.statistics.utils.UpdatableChar;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class CSVRandomAccess extends CsvParser {
    ArrayList<String[]> m_data = new ArrayList<String[]>();;
    int m_current = -1;
    Boolean m_loading = false;
    private final static Pattern NONNUMERIC = Pattern.compile("[^0-9E\\-.+]*");    
    private final static Pattern NUMERIC = Pattern.compile("[^0-9]*([\\+\\-]?(?:[0-9]+\\.?[0-9]*|\\.[0-9]+)(?:\\s?E[\\+\\s]?[0-9]+)?).*");    
    
    public class CSVCondition {
        int field;
        String value;

        public CSVCondition(int field, String value) {
            this.field = field;
            this.value = value;
        }
        
        boolean fits(int row) {
            return m_data.get(row)[field].contentEquals(value);
        } 
        
    }
    
    public static enum CSVSortType {
        numeric, alphanumeric
    }
    
    public static abstract class CSVSort {
        int field;

        public CSVSort(int field) {
            this.field = field;
        }
        
        abstract int compare(String[] row1, String[] row2);
        
    }

    public static class CSVSortNumeric extends CSVSort {

        public CSVSortNumeric(int field) {
            super(field);
        }
        
        int compare(String[] row1, String[] row2) {
            Double d1;
            Double d2;
            try {
                d1 = Double.parseDouble(row1[field]);
            } catch (Exception nfe) {
                d1 = Double.NEGATIVE_INFINITY;
            } 
            try {
                d2 = Double.parseDouble(row2[field]);
            } catch (Exception nfe) {
                d2 = Double.NEGATIVE_INFINITY;
            }
            return d1.compareTo(d2);
        }
        
    }

    public static class CSVSortAlphaNumeric extends CSVSort {

        public CSVSortAlphaNumeric(int field) {
            super(field);
        }
        
        int compare(String[] row1, String[] row2) {
            return row1[field].compareTo(row2[field]);
        }
        
    }
    
    
    public static interface LoadListener {
        void listen(int row,int column);
    };
    
    
    ArrayList<LoadListener> m_listenerCompleted = new ArrayList<LoadListener>();
    ArrayList<LoadListener> m_listenerProgress = new ArrayList<LoadListener>();
    ArrayList<LoadListener> m_listenerSort = new ArrayList<LoadListener>();
    ArrayList<LoadListener> m_listenerColumnsChanged = new ArrayList<LoadListener>();
    ArrayList<java.awt.event.ActionListener> m_listenerHeaderChanged = new ArrayList<java.awt.event.ActionListener>();
    

    public CSVRandomAccess(char delimiter, char quote) {
        super(delimiter, quote);
    }

    public CSVRandomAccess(char delimiter, char quote, String data) {
        this(delimiter, quote);
        String[] s = data.split("\n");
        int mc = 0;
        for (String line : s) {
            ArrayList<String> sl = splitLine(line);
            String[] sla = new String[sl.size()];
            sla = sl.toArray(sla);
            m_data.add(sla);
            if (sla.length > mc) {
                mc = sla.length;
            }
        }
        setMaxColumns(mc);
    }
    
    private CSVRandomAccess() {
        super();
    }

    public CSVCondition getCondition(int field, String value) {
        return new CSVCondition(field, value);
    }
    
    public void openFile(File f, boolean hasHeader) throws FileNotFoundException, IOException {
        super.openFile(f,false);
        m_loading = true;
        int row = 0;
        synchronized(m_data) {
            if (hasHeader && super.next()) {
                m_current = 0;
                m_data.add(super.getValues());
                setCurrentLineAsHeader();
                notifyProgress(row);
            }
            while (super.next()) {
                if (row++ % 10 == 0) {
                    notifyProgress(row);
                }
                m_data.add(super.getValues());
            }
            m_loading = false;
        }
//        m_loading.notifyAll();
        notifyComplete();
    }

    protected boolean supernext() throws IOException {
        return super.next();
    }

    protected String[] supergetValues() {
        return super.getValues();
    }
    
    
    public void openFileAsync(File f, boolean hasHeader) throws FileNotFoundException, IOException {
        super.openFile(f,false);
        m_loading = true;
        
        if (hasHeader && super.next()) {
            m_current = 0;
            m_data.add(super.getValues());
            setCurrentLineAsHeader();
            notifyProgress(0);
        }
        
        Runnable runnable = new Runnable() {

            public void run() {
                    try {
                    synchronized(m_data) {
                        int row = 0;
                        while (supernext()) {
                            if (row++ % 10 == 0) {
                                notifyProgress(row);
                            }
                            m_data.add(supergetValues());
                        }
                        m_loading = false;
                    }
        //        m_loading.notifyAll();
                    notifyComplete();

                } catch (IOException ex) {
                    Logger.getLogger(CSVRandomAccess.class.getName()).log(Level.SEVERE, null, ex);
                    notifyComplete();
                }
            }
        };
        new Thread(runnable).start();
    }
    
    
    @Override
    public boolean next() {
        if (m_data.size() > 0) {
            if (m_current<0) {
               m_current = 0;
                setCurrentValues(m_data.get(m_current));
                return true;
            }else if (m_current<m_data.size() -1) {
                m_current ++;
                setCurrentValues(m_data.get(m_current));
                return true;
            }
        }
        m_current = m_data.size();
        return false;
    }

    public boolean previous() {
        if (m_current>0) {
            m_current --;
            setCurrentValues(m_data.get(m_current));
            return true;
        }
        m_current = -1;
        return false;
    }
    
    public boolean activeRow() {
        return m_current >=0 && m_current< m_data.size();
    }
    
    public void setRow(int row) {
        m_current = row -1;
        next();
        if (row<0) {
            m_current=-1;
        }
    }
    
    public void deleteCurrentLine() {
        synchronized(m_data) {
            deleteRow(m_current); 
        }
        
    }
    
    public void deleteRow(int row) {
        synchronized(m_data) {
            m_data.remove(row);
            if (row >= m_current) {
                m_current--;
            }
        }
    }
    
    public int findFirstRow(CSVCondition[] conditions) {
        row: for (int i = 0; i<m_data.size(); i++) {
            for (int c =0; c< conditions.length; c++) {
                if (!conditions[c].fits(i)) {
                    continue row;
                }
            }
            m_current = i;
            return i;
        }
        return -1;
    }

    public int findFromCurrent(CSVCondition[] conditions) {
        
        row: for (int i = Math.max(m_current+1,0); i<m_data.size(); i++) {
            for (int c =0; c< conditions.length; c++) {
                if (!conditions[c].fits(i)) {
                    continue row;
                }
            }
            m_current = i;
            return i;
        }
        return -1;
    }
    
    @Override
    public CSVRandomAccess  clone() {
        CSVRandomAccess ccsv = new CSVRandomAccess(getDelimiter().charAt(0), getQuote().charAt(0));
        super.transfer(ccsv);
        synchronized(m_data) {
            ccsv.m_data  = (ArrayList<String[]>) m_data.clone();
        }
        ccsv.m_current  = m_current;
        ccsv.m_loading  = m_loading;
        return ccsv;
    }
    
    /**
     * takes the current line, and sets it as header
     */    
    public void setCurrentLineAsHeader() {
        setHeader(getCurrentLine());
        deleteCurrentLine();
        fireHeaderChanged();
    }
    
    /**
     * takes the current line, and sets it as header
     */    
    public void setLineAsHeader(int row) {
        int oldCurrentlin = m_current;
        m_current = row;
        setCurrentLineAsHeader();
        m_current = oldCurrentlin;
//        fireHeaderChanged();
    }

    /**
     * takes the current line, and sets it as header
     */    
    public void insertLine(int row,String line) {        
        ArrayList<String> sl = splitLine(line);
        String[] d = new String[sl.size()];
        d = sl.toArray(d);
        insertLine(row, d);
    }

    public void insertLine(int row,String[] line) {        
        m_data.add(row, line);
        
        if (m_current >= row) {
            m_current++;
        }
    }
    
    
    public static CSVRandomAccess guessCsvRA(File f, int guessLines, boolean hasHeader, char[] delimiters, char[] quotes) throws FileNotFoundException, IOException {
      
        UpdatableChar delimiter = new UpdatableChar();
        UpdatableChar quote = new UpdatableChar();
        Boolean unique = guessDelimQuote(f, guessLines, delimiters, quotes, delimiter, quote);
                
        
        if (unique == null) {
            return null;
        }
        
        CSVRandomAccess csv = new CSVRandomAccess(delimiter.value, quote.value);
        csv.openFile(f, hasHeader);
        return csv;
    }

    public static CSVRandomAccess guessCsvAsync(final File f, int guessLines, final boolean hasHeader, char[] delimiters, char[] quotes) throws FileNotFoundException, IOException {
      
        UpdatableChar delimiter = new UpdatableChar();
        UpdatableChar quote = new UpdatableChar();
        Boolean unique = guessDelimQuote(f, guessLines, delimiters, quotes, delimiter, quote);
                
        
        if (unique == null) {
            return null;
        }
        
        final CSVRandomAccess csv = new CSVRandomAccess(delimiter.value, quote.value);

        Runnable runnable = new Runnable() {

            public void run() {
                try {
                    csv.openFile(f, hasHeader);
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(CSVRandomAccess.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(CSVRandomAccess.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        new Thread(runnable).start();
        return csv;
    }
    
    /**
     * Opens a csv-parser for the given file and tries to predict the quote 
     * character and the field-delimiter based on the first number of lines.<br/>
     * After it reads the first lines it will reopen the file with a configured 
     * csv-parser.<br/>
     * <b><i>This means, that the file must be re-readable e.g no pipe</i></b>
     * 
     * @param f the file to open
     * @param guessLines how many lines to read in for guessing the parameters
     * @return a {@link CavParser} for the file
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static CSVRandomAccess guessCsvRA(File f, int guessLines, boolean hasHeader) throws FileNotFoundException, IOException {
        return guessCsvRA(f, guessLines, hasHeader, TEST_DELIMITERS, TEST_QUOTES);
    }

    public static CSVRandomAccess guessCsvAsync(File f, int guessLines, boolean hasHeader) throws FileNotFoundException, IOException {
        return guessCsvAsync(f, guessLines, hasHeader, TEST_DELIMITERS, TEST_QUOTES);
    }
    
    public String getValue(String field, Integer row) {
        if (row ==null || row >= m_data.size()) {
            return MISSING_FIELD;
        }
        
        Integer column = getColumn(field);
        
        if (column == null) {
            return MISSING_FIELD;
        }
        
        String[] line = m_data.get(row); 
        if (column >= line.length) {
            return MISSING_FIELD;
        }
        
        return line[column];
    }
    
    
    public String getValue(Integer field, Integer row, CSVValueCalc defaultValue) {
        if (field == null || row == null || row >= m_data.size()) {
            return defaultValue.getValue(this, row);
        }

        String[] line = m_data.get(row); 

        if (field >= line.length) {
            return defaultValue.getValue(this, row);
        }

        // we have an entry for it but it is MISSING_FIELD -> so we should just replace it
        if (line[field] == MISSING_FIELD) {
            String v = defaultValue.getValue(this);
            setValue(v, field, row);
            return v;
        }
            
        return line[field];
    }
    
    public String getValue(String field, Integer row, CSVValueCalc defaultValue) {
        Integer column = getColumn(field);
        if (column == null) {
            return defaultValue.getValue(this,row);
        }
        return getValue(column, row, defaultValue);
    }        
    
    /**
     * Sets the value of the given field as value.toString()
     * <br/>
     * If the row did not previously had enough columns to accommodate the field,
     * the needed amount of fields will be added with MISSING_VALUE.
     * 
     * @param value the new value
     * @param field column of the 
     * @param row   row where the value should be changed
     */
    public void setValue(Object value, int field, int row) {
        if (row >= m_data.size()) {
            if (!value.equals(MISSING_FIELD)) {
                synchronized (m_data) {
                    for (int i = m_data.size(); i < row; i++) {
                        m_data.add(new String[0]);
                    }
                }
                String[] line = new String[field+1];
                for (int i = 0; i<field; i++) {
                    line[i] = MISSING_FIELD;
                }
                line[field] = value.toString();
                m_data.add(line);
                if (field >= getMaxColumns()) {
                    setMaxColumns(field + 1);
                }
            }
        } else {
            String[] line = m_data.get(row); 
            if (field >= line.length) {
                if (!value.equals(MISSING_FIELD)) {
                    String[] dummyField = java.util.Arrays.copyOf(line, field + 1);
                    for (int f = line.length; f< field; f++) {
                        dummyField[f] = MISSING_FIELD;
                    }
                    dummyField[field] = value.toString();
                    m_data.set(row, dummyField);
                    if (field >= getMaxColumns()) {
                        setMaxColumns(field + 1);
                    }
                }
            } else {
                line[field] = value.toString();
            }
        }
    }

    /**
     * Sets the value of the given field as value.toString()
     * <br/>
     * If the row did not previously had enough columns to accommodate the field,
     * the needed amount of fields will be added with MISSING_VALUE.
     * 
     * @param value the new value
     * @param field column of the 
     */
    public void setValue(Object value, int field) {
        if (m_current >= m_data.size()) {
            throw new ArrayIndexOutOfBoundsException("Behind the last row, therefore I can't set a value");
        }
        if (m_current < 0) {
            throw new ArrayIndexOutOfBoundsException("Before the first row, therefore I can't set a value");
        }
        setValue(value, field, m_current);
    }
    
    
    public String getValue(Integer field, Integer row) {
        if (row == null || row >= m_data.size()) {
            return MISSING_FIELD;
        }
        String[] line = m_data.get(row); 
        if (field == null || field >= line.length) {
            return MISSING_FIELD;
        }
        return line[field];
    }

    public double getDouble(Integer field, Integer row) {
        String v = getValue(field, row);
        if (v == MISSING_FIELD) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException nfe) {
            return Double.NaN;
        }
    }
    
    public Boolean getBool(Integer field, Integer row) {
        String v = getValue(field, row);
        if (v == MISSING_FIELD) {
            return false;
        }
        
        ISFALSE.split(v);
        return ! ISFALSE.matcher(v).matches();
    }

    public Boolean getBool(Integer field, Integer row, boolean defaultValue) {
        String v = getValue(field, row);
        if (v == MISSING_FIELD) {
            return defaultValue;
        }
        
        if (defaultValue) {
            return !ISFALSE.matcher(v).matches();
        } else {
            return ISTRUE.matcher(v).matches();
        }
    }


    public double getDouble(String fieldName, Integer row) {
        Integer field = getColumn(fieldName);
        if (field == null) {
            return Double.NaN;
        }
        return getDouble(field, row);
    }
    
    public Boolean getBool(String fieldName, Integer row) {
        Integer field = getColumn(fieldName);
        if (field == null) {
            return null;
        }
        return getBool(field, row);
    }

    public Boolean getBool(String fieldName, Integer row, boolean defaultValue) {
        Integer field = getColumn(fieldName);
        return getBool(field, row,defaultValue);
    }

    
    @Override
    public String getCurrentLine() {
        
        return getLine(m_current);
    }
    
    public String getLine(int row) {
        String[] line = m_data.get(row);
        return valuesToString(line);        
    }
    
    public String valuesToString(String[] values) {
        StringBuilder sb = new StringBuilder(values.length*10);
        for (int i = 0; i< values.length; i++) {
            sb.append(quoteValue(values[i]));
            sb.append(getDelimiter());
        }
        return sb.substring(0, sb.length() -1);        
        
    }
    
    @Override
    public synchronized  int addColumn() {
        int ret = super.addColumn();
        notifyColumnsChanged();
        return ret;
    }

    @Override
    public synchronized int addColumn(String name) {
        int ret = super.addColumn(name);
        notifyColumnsChanged();
        return ret;
    }
    

    public synchronized int addGetColumn(String name) {
        Integer c = getColumn(name);
        if (c == null) {
            int ret = super.addColumn(name);
            notifyColumnsChanged();
            return ret;
        }
        return c;
    }

    
    public int getRowCount() {
        return m_data.size();
    }
   
    public void addListenerComplete(LoadListener listener) {
        if (!m_listenerCompleted.contains(listener)) {
            m_listenerCompleted.add(listener);
        } else {
            System.err.println("some error here");
        }
    }
    public void addListenerProgress(LoadListener listener) {
        m_listenerProgress.add(listener);
    }

    public void removeListenerComplete(LoadListener listener) {
        m_listenerCompleted.remove(listener);
    }
    public void removeListenerProgress(LoadListener listener) {
        m_listenerProgress.remove(listener);
    }

    public void addListenerSort(LoadListener listener) {
        m_listenerSort.add(listener);
    }
    public void addListenerColumnsChanged(LoadListener listener) {
        m_listenerColumnsChanged.add(listener);
    }

    public void removeListenerSort(LoadListener listener) {
        m_listenerSort.remove(listener);
    }
    public void removeListenerColumnsChanged(LoadListener listener) {
        m_listenerColumnsChanged.remove(listener);
    }
    
    public void sortNumeric(final int column) {
        if (column > getMaxColumns()) {
            return;
        }
        Comparator<String[]> comp = new Comparator<String[]>(){
            
            public int compare(String[] s1, String[] s2) {
                if (s1.length <= column) {
                    if (s2.length <= column) {
                        return 0;
                    } else {
                        return 1;
                    }
                }
                if (s2.length <= column) {
                    return -1;
                }
                
                Matcher m1 = NUMERIC.matcher(s1[column]);
                Matcher m2 = NUMERIC.matcher(s2[column]);
                if (!m1.matches()) {
                    if (!m2.matches()) {
                        return 0;
                    } else {
                        return 1;
                    }
                }
                if (!m2.matches()) {
                    return -1;
                }
                double d1 = Double.parseDouble(m1.group(0));
                double d2 = Double.parseDouble(m2.group(0));
                    
                return Double.compare(d1, d2);
            }
        };
        synchronized(m_data) {
            java.util.Collections.sort(m_data, comp);
        }
        notifySort();
    }

    public void sortNumericReverse(final int column) {
        if (column > getMaxColumns()) {
            return;
        }
        Comparator<String[]> comp = new Comparator<String[]>(){

            public int compare(String[] s1, String[] s2) {
                if (s1.length <= column) {
                    if (s2.length <= column) {
                        return 0;
                    } else {
                        return -1;
                    }
                }
                if (s2.length <= column) {
                    return 1;
                }

                Matcher m1 = NUMERIC.matcher(s1[column]);
                Matcher m2 = NUMERIC.matcher(s2[column]);
                if (!m1.matches()) {
                    if (!m2.matches()) {
                        return 0;
                    } else {
                        return -1;
                    }
                }
                if (!m2.matches()) {
                    return 1;
                }
                double d1 = Double.parseDouble(m1.group(0));
                double d2 = Double.parseDouble(m2.group(0));
                return Double.compare(d2, d1);
            }
        };
        synchronized(m_data) {
            java.util.Collections.sort(m_data, comp);
        }
        notifySort();
    }

    public void sort(final CSVSort[] criteria) {
        Comparator<String[]> comp = new Comparator<String[]>(){
            
            public int compare(String[] s1, String[] s2) {
                for (int i =0; i< criteria.length;i++) {
                    int r = criteria[i].compare(s1, s2);
                    if (r!=0) {
                        return r;
                    }
                }
                return 0;
            }
        };
        synchronized(m_data) {
            java.util.Collections.sort(m_data, comp);
        }
        notifySort();
    }
    

    public void sortReverse(final CSVSort[] criteria) {
        Comparator<String[]> comp = new Comparator<String[]>(){
            
            public int compare(String[] s1, String[] s2) {
                for (int i =0; i< criteria.length;i++) {
                    int r = criteria[i].compare(s1, s2);
                    if (r!=0) {
                        return -r;
                    }
                }
                return 0;
            }
        };
        java.util.Collections.sort(m_data, comp);
        notifySort();
    }
    
    
    public void sortAlpha(final int column) {
        if (column > getMaxColumns()) {
            return;
        }
        Comparator<String[]> comp = new Comparator<String[]>(){
            
            public int compare(String[] s1, String[] s2) {
                if (s1.length <= column) {
                    if (s2.length <= column) {
                        return 0;
                    } else {
                        return 1;
                    }
                }
                if (s2.length <= column) {
                    return -1;
                }
                
                return s1[column].compareTo(s2[column]) ;
            }
        };
        java.util.Collections.sort(m_data, comp);
        notifySort();
    }

    public void sortAlphaReverse(final int column) {
        if (column > getMaxColumns()) {
            return;
        }
        Comparator<String[]> comp = new Comparator<String[]>(){
            
            public int compare(String[] s1, String[] s2) {
                if (s1.length <= column) {
                    if (s2.length <= column) {
                        return 0;
                    } else {
                        return -1;
                    }
                }
                if (s2.length <= column) {
                    return 1;
                }
                
                return s2[column].compareTo(s1[column]) ;
            }
        };
        java.util.Collections.sort(m_data, comp);
        notifySort();
    }
    
    public synchronized void ensureCapacity(int row) {
        String[] previous = m_data.get(row);
        int targetColumns = getMaxColumns();
        if (previous.length < targetColumns) {
            String[] ext = java.util.Arrays.copyOf(previous, targetColumns);
            java.util.Arrays.fill(ext, previous.length, targetColumns, MISSING_FIELD);
        }
    }
    
    protected synchronized void  notifyProgress(int rows) {
        
        for (LoadListener l : (ArrayList<LoadListener>) m_listenerProgress.clone()) {
            l.listen(rows,-1);
        }
    }
    
    protected synchronized void notifyComplete() {
        for (LoadListener l : (ArrayList<LoadListener>)m_listenerCompleted.clone()) {
            l.listen(m_data.size(),getMaxColumns());
        }
    }

    protected synchronized void notifyColumnsChanged() {
        for (LoadListener l : (ArrayList<LoadListener>)m_listenerColumnsChanged.clone()) {
            l.listen(m_data.size(),getMaxColumns());
        }
    }
    
    
    protected void notifySort() {
        synchronized(m_data) {
            for (LoadListener l : (ArrayList<LoadListener>)m_listenerSort.clone()) {
                l.listen(m_data.size(), -1);
            }
        }
    }
    
    public boolean isLoading() {
        return m_loading;
    }

    public void writeFile(PrintWriter out) {
        if (hasHeader()) {
            out.println(valuesToString(getHeader()));
        }
        for (String[] line : m_data) {
            out.println(valuesToString(line));
        }
    }
    
    public void clear() {
        synchronized(m_data) {
            m_data.clear();
            setHeader("");
            notifyColumnsChanged();
        }
    }
    
    
    public void switchHeader() {
        if (getHeader() == null) {
            if (!m_data.isEmpty()) {
                setHeader(m_data.get(0));
                deleteRow(0);
            }
        } else {
            insertLine(0, getHeader());
            cleanHeader();
        }
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
    
    public void setHeader(String[] header) {
        super.setHeader(header);
        fireHeaderChanged();
    }
    
    public void cleanHeader() {
        super.cleanHeader();
        fireHeaderChanged();
    }
    
}
