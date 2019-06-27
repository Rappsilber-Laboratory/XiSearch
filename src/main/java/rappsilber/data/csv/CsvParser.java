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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import rappsilber.ms.statistics.utils.UpdatableChar;
import rappsilber.ms.statistics.utils.UpdateableInteger;
import rappsilber.utils.HashMapList;

/**
 * implements a generic csv-parser, that enables a to access fields by name - including alternatives to these names.
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class CsvParser {
    /** array, that contains the header of the file **/
    private String[]                    m_header;
    /** list of list, containing alternatives for column-names **/
    private ArrayList<HashSet<String>>  m_headerAlternatives = new ArrayList<HashSet<String>>();
    /** maps the column-name to the column-index **/
    private HashMap<String,Integer>     m_headerToColumn = new HashMap<String, Integer>();
    /** the field delimeter **/
    private String                      m_delimiter = ",";
    /** quote character **/
    private String                      m_quote = "\"";
    /** just a short-cut to the duplicate quotes - for having the quote character within quotes **/
    private String                      m_doublequote = m_quote+m_quote;
    
    /** a pattern, that matches the values in a cell. it provides two groups - first for quoted cell-values and second for unquoted cell-values **/
    private Pattern                     m_cellValue;
                                        //Pattern.compile(m_delimiter +"?\\s*(?:\"((?:[^\"]*(?:\"\")?)*)\"|([^"+ m_delimiter +"]*))\\s*"+ m_delimiter + "?");
    /** the maximum number of columns found sofar in the csv-file **/
    private int                         m_maxColumns=0;
    /** how many columns where in the header-row **/
    private int                         m_countHeader;
    /** the current line, that is parsed **/
    private String                      m_currentLine;
    /** the current line split up into values **/
    private String[]                    m_currentValues;
    /** how many values where found in the current line **/
    private int                         m_foundValues=0;
    /** the number of the current line **/
    private int                         m_lineNumber=0;
    /** the reader for the file - if it reads from a file*/
    private BufferedReader              m_input;
    /** the file that is read - if a file is read **/
    private File                        m_inputFile;
    /** if we should guess the delimiter - what are the candidates**/
    protected static char[]             TEST_DELIMITERS = {',','\t','|'};
    /** if we should guess the quote chars - what are the candidates**/
    protected static char[]             TEST_QUOTES = {'\'','"'};
    /** a pattern defining the values, that would interpreted as true */
    protected final static Pattern      ISTRUE = Pattern.compile("^(T|1|-1|TRUE|Y|YES|\\+)$",Pattern.CASE_INSENSITIVE);
    /** a pattern defining the values, that would interpreted as false */
    protected final static Pattern      ISFALSE = Pattern.compile("^(F|0|FALSE|N|NO)?$",Pattern.CASE_INSENSITIVE);
    
    
    
    /** 
     * if a field is missing this is the value it gets 
     * As it is an independent object we can just determine if a field where missing by comparing (=)
     */
    public String                      MISSING_FIELD = new String("");

    /**
     * default constructor
     */
    public CsvParser() {
        setPattern(m_delimiter, m_quote);
    }

    /** 
     * constructor defining the field delimiter
     * @param delimiter 
     */
    public CsvParser(char delimiter) {
        setPattern(Character.toString(delimiter), m_quote);
    }
    
    /** 
     * constructor defining quote character
     * @param delimiter 
     */
    public CsvParser(char delimiter, char quote) {
        setDelimiter(delimiter);
        setQuote(quote);
    }
    
    
    protected void setPattern(String d, String q) {
        m_delimiter = d;
        m_quote = q;
        m_doublequote = m_quote + m_quote;
        if (d.matches("\\t")) {
            m_cellValue = Pattern.compile(" *(?:"+q+"((?:[^"+q+"]*(?:"+q+q+")?)*)"+q+"|([^"+ d +"]*[^ "+ d +"])|) *"+ d + "?");
        } else if (d.matches(" ")) {
            m_cellValue = Pattern.compile("\\t*(?:"+q+"((?:[^"+q+"]*(?:"+q+q+")?)*)"+q+"|([^"+ d +"]*[^\\t"+ d +"])|)\\t*"+ d + "?");
        } else
            m_cellValue = Pattern.compile("\\s*(?:"+q+"((?:[^"+q+"]*(?:"+q+q+")?)*)"+q+"|([^"+ d +"]*[^\\s"+ d +"])|)\\s*"+ d + "?");
    }

    
    public void setPattern(char d, char q) {
        setPattern(Character.toString(d), Character.toString(q));
    }

    
    protected void setPattern(Pattern p) {
        m_cellValue = p;
        m_delimiter = "";
        m_quote = "";
        m_doublequote = "";
    }
    
    public void setDelimiter(char d) {
        String delim = Character.toString(d);
        if (!("|.\\()[]^$".contains(delim)))
            setPattern(delim, m_quote);
        else
            setPattern("\\" + delim, m_quote);
    }

    public void setQuote(char q) {
        setPattern(m_delimiter, Character.toString(q));
    }

    public String getDelimiter() {
        return m_delimiter;
    }

    public String getQuote() {
        return m_quote;
    }

    public ArrayList<String> splitLine(String line) {
        return splitLine(line, new UpdateableInteger(0));
    }
    
    public ArrayList<String> splitLine(String line, UpdateableInteger countQuoted) {
        ArrayList<String> ret = new ArrayList<String>(m_maxColumns);
        Matcher m = m_cellValue.matcher(line);
        
        int c = 0;
        while (m.find()) {

            if (m.group(1) != null) {
                ret.add(m.group(1).replace(m_doublequote, m_quote));
            } else {
                ret.add(m.group(2));
                countQuoted.value++;
            }
            c++;
        }
//        if (c>m_maxColumns)
//            m_maxColumns = c;
        ret.remove(ret.size() - 1);
        return ret;
    }
    
    public void setHeader(String line) {
        ArrayList<String> hc = splitLine(line);
        // first set the header array
        String[] header = new String[hc.size()];
        hc.toArray(header);
        setHeader(header);
    }

    public void setHeader(String[] header) {
        m_header = header;
        if (header == null) {
            getHeaderToColumn().clear();
        } else {
            for (int i = 0; i< m_header.length; i++) {
                String h = m_header[i];
                if (h==null) 
                    h="Column "+Integer.toString(i);
                else 
                    h = h.trim();
                m_header[i]=h;
                getHeaderToColumn().put(h , i);
                String hl = h.toLowerCase();
                if (!h.contentEquals(hl) && getHeaderToColumn().get(h.toLowerCase()) == null)
                    getHeaderToColumn().put(hl , i);
                // now we look, if we have some alternative names for the column
                for (HashSet<String> names : getHeaderAlternatives()) {
                    if (names.contains(h) || names.contains(hl)) {
                        for (String ha : names) {
                            getHeaderToColumn().put(ha , i);
                        }
                        break;
                    }
                }
            }
        }
    }
    
    public void cleanHeader() {
        m_header = null;
        getHeaderToColumn().clear();
    }

    
    public String[] getHeader() {
        return m_header;
    }
    
    
    
    public String getHeader(int column) {
        if (m_header == null || column >= m_header.length) {
            return Integer.toString(column);
        }
        String ret = m_header[column];
        if (ret == null || ret.trim().length() == 0)
            return Integer.toString(column);
        return m_header[column];
    }
    
    public void setAlternative(String name, String alternative) {
        boolean isSet=false;
        for (HashSet<String> names : getHeaderAlternatives()) {
            if (names.contains(name)) {
                names.add(alternative);
                Integer col = getColumn(name);
                if (col == null) {
                    col = getColumn(alternative);
                    if (col != null)
                        for (String n : names) {
                            getHeaderToColumn().put(n, col);
                        }
                } else 
                    getHeaderToColumn().put(alternative, col);
                
                isSet = true;
                break;
            }
            if (names.contains(alternative)) {
                String reference = names.iterator().next();
                names.add(name);
                Integer col = getColumn(reference);
                if (col != null)
                    getHeaderToColumn().put(name, col);
                
                isSet = true;
                break;
            }
        }
        if (!isSet) {
            HashSet<String> names = new HashSet<String>(2);
            names.add(name);
            names.add(alternative);
            getHeaderAlternatives().add(names);
            if (getColumn(name) != null)
                getHeaderToColumn().put(alternative, getColumn(name));
            else if (getColumn(alternative) != null)
                getHeaderToColumn().put(name, getColumn(alternative));
        }
    }


    public void readHeader() throws IOException {
        next();
        setHeader(getCurrentLine());
    }
    
    public void setCurrentLine(String line) {
        m_lineNumber++;
        m_currentLine = line;
        m_currentValues = splitLine(line).toArray(new String[0]);
        for (int c = 0; c<m_currentValues.length;c++)
            if (m_currentValues[c] == null)
                m_currentValues[c] = MISSING_FIELD;
        int l = m_currentValues.length;
        if (l > m_maxColumns || m_maxColumns == 0) {
            if (m_maxColumns != 0) {
                Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "line " + 
                                            m_lineNumber + 
                                            " contains more fields than ever observed!\n" +
                                            line);
            }
            
            m_maxColumns = l;
            if (m_header != null) {
                String[] dheader = java.util.Arrays.copyOf(m_header, m_maxColumns);
                java.util.Arrays.fill(dheader, m_header.length, m_maxColumns, MISSING_FIELD);
                m_header = dheader;
            }
            
//            m_currentValues = new String[m_maxColumns];
            if (m_header != null) {
                if (l<m_header.length) {
                    Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "line " + m_lineNumber + " less fields than the header\n" +
                                                                    "Assume the missing values are at the end and will be empty");
                }
            }
        }
        
        // fill up with missing fields
//        for (int i= l; i<m_maxColumns; i++)
//            m_currentValues[i] = MISSING_FIELD;
        m_foundValues = l;
    }
    
    public void openFile(File f) throws FileNotFoundException, IOException {
        openFile(f, false);
    }
    
    public void openFile(File f, boolean hasHeader) throws FileNotFoundException, IOException {
        m_inputFile = f;
        m_input = new BufferedReader(new FileReader(f));
        if (hasHeader)
            readHeader();
    }

    public void openFile(BufferedReader r, boolean hasHeader) throws FileNotFoundException, IOException {
        m_input = r;
        if (hasHeader)
            readHeader();
    }
        
    /**
     * takes the current line, and sets it as header
     */    
    public void setCurrentLineAsHeader() {
        setHeader(getCurrentLine());
    }
    
    /**
     * creates a new csv-parser for the given file
     * @param f the file to parse
     * @param delimiter field-delimiter
     * @param quote quote character
     * @return a csv-parser for the given file
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static CsvParser getCSV(File f, char delimiter, char quote) throws FileNotFoundException, IOException {
        CsvParser csv = new CsvParser(delimiter, quote);
        csv.openFile(f);
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
    public static CsvParser guessCsv(File f, int guessLines) throws FileNotFoundException, IOException {
        return guessCsv(f, guessLines, TEST_DELIMITERS, TEST_QUOTES);
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
     * @param delimiters an array with chars that should be considered as delimiters
     * @param quotes an array with chars that should be considered as quote-characters
     * @return a {@link CavParser} for the file
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static CsvParser guessCsv(File f, int guessLines, char[] delimiters, char[] quotes) throws FileNotFoundException, IOException {
      
        UpdatableChar delimiter = new UpdatableChar();
        UpdatableChar quote = new UpdatableChar();
        Boolean unique = guessDelimQuote(f, guessLines, delimiters, quotes, delimiter, quote);
                
        
        if (unique == null) {
            return null;
        }
        
        CsvParser csv = new CsvParser(delimiter.value, quote.value);
        csv.openFile(f);
        return csv;
    }

    public static Boolean guessDelimQuote(File f, int guessLines, UpdatableChar delimiter, UpdatableChar quote) throws FileNotFoundException, IOException {
        return guessDelimQuote(f, guessLines, TEST_DELIMITERS, TEST_QUOTES, delimiter, quote);
    }
    
    public static Boolean guessDelimQuote(File f, int guessLines, char[] delimiters, char[] quotes, UpdatableChar delimiter, UpdatableChar quote) throws FileNotFoundException, IOException {
        ArrayList<String> allTestLines = new ArrayList<String>(guessLines);
        int lc = 0;
        String line;
        BufferedReader br = new BufferedReader(new FileReader(f));
        
        while ((line = br.readLine()) != null && lc++<guessLines) {
            allTestLines.add(line);
        }
        
        if (allTestLines.isEmpty()) {
            delimiter.value = delimiters[0];
            quote.value = quotes[0];
            return false;
        }
        
        
        int[] values = new int[allTestLines.size()];
        char delimChar = ' ';
        char quoteChar = ' ';
        int minFieldCount = 0;
        int maxFieldCount = 0;
        boolean unique = false;
        int quoteFound = 0;
        
        dloop: for (char d : delimiters) {
            int dMinFieldCount = 0;
            int dMaxFieldCount = 0;
            int dQuoteFound = 0;
            char dQuoteChar = ' ';
            
            qloop: for (char q : quotes) {
                int thisquoteFound = 0;
                CsvParser testCsv = new CsvParser(d, q);
                // how many fields would we have in the first line
                int c = testCsv.splitLine(allTestLines.get(0)).size();
                int qMinFieldCount = c;
                int qMaxFieldCount = c;
                // if it is only one field - we cant conclude anything -ignored
//                if (c  == 1 )
//                    continue qloop;
                
                UpdateableInteger countQuoted = new UpdateableInteger(0);
                for (String l : allTestLines) {
                    c=testCsv.splitLine(l,countQuoted).size();
                            
                    // if not all lines have the same number of fields we ignore this combination
                    if (qMinFieldCount > c)
                        qMinFieldCount = c;
                    else if (qMaxFieldCount < c)
                        qMaxFieldCount = c;
                    thisquoteFound += l.length() - l.replace(""+q, "").length();
                }
                
                if (dMinFieldCount == 0) {
                    dMinFieldCount = qMinFieldCount;
                    dMaxFieldCount = qMaxFieldCount;
                    dQuoteChar = q;
                    unique = true;
                    dQuoteFound = thisquoteFound;
                } else if (qMinFieldCount>=dMinFieldCount || 
                        (qMinFieldCount == dMinFieldCount && qMaxFieldCount < dMaxFieldCount) ||
                        (qMinFieldCount == dMinFieldCount && qMaxFieldCount == dMaxFieldCount && dQuoteFound<thisquoteFound)) {
                    dMinFieldCount = qMinFieldCount;
                    dMaxFieldCount = qMaxFieldCount;
                    dQuoteChar = q;
                    unique = true;
                    dQuoteFound = thisquoteFound;
                } else if (qMinFieldCount == dMinFieldCount && qMaxFieldCount == dMaxFieldCount && dQuoteFound == thisquoteFound) {
                    unique = false;
                }
            }
            if (dMinFieldCount>=minFieldCount || 
                (dMinFieldCount == minFieldCount && dMaxFieldCount > maxFieldCount)) {
                quoteChar  = dQuoteChar;
                delimChar = d;
                minFieldCount = dMinFieldCount;
                maxFieldCount = dMaxFieldCount;
            }
            
        }
        
        if (maxFieldCount == 0) {
            return null;
        }
        
        delimiter.value = delimChar;
        quote.value = quoteChar;
        return unique;
        
    }
    
    public boolean next() throws IOException {
        if (m_input == null)
            throw new UnsupportedOperationException("No file is currently open for reading");
        String line = m_input.readLine();
        while (line != null && line.isEmpty())
            line = m_input.readLine();
        
        if (line  == null)
            return false;
        setCurrentLine(line);
        return true;
    }
    
    public boolean onlyEmptyFields() {
        for (int i = 0; i< m_foundValues; i++) {
            if (!m_currentValues[i].isEmpty())
                return false;
        }
        return true;
    }
    
    public int addColumn() {
        return m_maxColumns++;
    }
    
    public int addColumn(String name) {
        int col = addColumn();
        if (m_header != null) {
            String[] dheader = java.util.Arrays.copyOf(m_header, col+1);
            dheader[col] = name;
            m_header = dheader;
        }
        getHeaderToColumn().put(name, col);
        if (!name.contentEquals(name.toLowerCase()) && getHeaderToColumn().get(name) == null)
            getHeaderToColumn().put(name.toLowerCase(), col);
        return col;
    }

    
    public synchronized int addGetColumn(String name) {
        Integer c = getColumn(name);
        if (c == null) {
            int ret = addColumn(name);
            return ret;
        }
        return c;
    }
        
    public boolean onlyMissingFields() {
        return m_foundValues == 0;
    }
    
    public boolean isEmpty() {
        return onlyEmptyFields();
    }

    public boolean isEmpty(String field) {
        Integer column = getHeaderToColumn().get(field);
        if (column == null)
            return true;
        return m_currentValues[column].isEmpty();
    }

    public boolean isEmpty(int field) {
        if (field > m_maxColumns)
            return true;
        return m_currentValues[field].isEmpty();
    }
    
    public boolean isMissing(int field) {
        if (field > m_maxColumns)
            return true;
        return m_currentValues[field] == MISSING_FIELD;
    }

    public boolean isMissing(String field) {
        Integer column = getHeaderToColumn().get(field);
        if (column == null)
            return true;
        return m_currentValues[column] == MISSING_FIELD;
    }
    
    public Integer getColumn(String name)  {
        Integer col = getHeaderToColumn().get(name);
        if (col == null && name.matches("[0-9]*")) {
            Integer name2col = Integer.parseInt(name);
            if (name2col < getMaxColumns())
                return name2col;
        }
        
        return col;
    }
    
    public String getValue(String field) {
        Integer column = getHeaderToColumn().get(field);
        if (column == null)
            return MISSING_FIELD;
        return m_currentValues[column];
    }

    public String getValue(int field) {
        if (field > m_maxColumns)
            return MISSING_FIELD;
        return m_currentValues[field];
    }

    public String getValue(Integer field) {
        if (field == null || field >= m_currentValues.length)
            return MISSING_FIELD;
        return m_currentValues[field];
    }
    
    public String getValue(Integer field, CSVValueCalc defaultValue) {
        if (field == null || field > m_maxColumns) {
            return defaultValue.getValue(this);
        }
        if (m_currentValues[field] == MISSING_FIELD) {
            String v = defaultValue.getValue(this);
            m_currentValues[field] = v;
            return v;
        }
        return m_currentValues[field];
    }
    
    public String getValue(String field, CSVValueCalc defaultValue) {
        Integer column = getHeaderToColumn().get(field);
        if (column == null)
            return defaultValue.getValue(this);
        
        return getValue(column, defaultValue);
    }    
    
    public Integer getInteger(Integer field) {
        String v = getValue(field);
        if (v == MISSING_FIELD)
            return null;
        
        return Integer.parseInt(v);
    }
    
    public Integer getInteger(Integer field, Integer defaultValue) {
        String v = getValue(field);
        if (v == MISSING_FIELD)
            return defaultValue;
        
        return Integer.parseInt(v);
    }
    
    public double getDouble(Integer field) {
        if (getValue(field) == MISSING_FIELD)
            return Double.NaN;
        try {
            return Double.parseDouble(m_currentValues[field]);
        } catch (NumberFormatException nfe) {
            return Double.NaN;
        }
    }

    public double getDouble(Integer field, Object defaultValue) {
        if (getValue(field) == MISSING_FIELD)
            return (Double)defaultValue;
        try {
            return Double.parseDouble(m_currentValues[field]);
        } catch (NumberFormatException nfe) {
            return (Double)defaultValue;
        }
    }
    
    public Boolean getBool(Integer field) {
        String v = getValue(field);
        if (v == MISSING_FIELD)
            return null;
        
        ISFALSE.split(v);
        return ! ISFALSE.matcher(v).matches();
    }

    public Boolean getBool(Integer field, boolean defaultValue) {
        String v = getValue(field);
        if (v == MISSING_FIELD)
            return defaultValue;
        
        if (defaultValue)
            return !ISFALSE.matcher(v).matches();
        else
            return ISTRUE.matcher(v).matches();
    }


    public double getDouble(String fieldName) {
        Integer field = getHeaderToColumn().get(fieldName);
        if (field == null) 
            return Double.NaN;
        return getDouble(field);
    }
    
    public double getDouble(String fieldName, double defaultValue) {
        int field = getHeaderToColumn().get(fieldName);
        return getDouble(field,defaultValue);
    }

    public Integer getInteger(String fieldName) {
        Integer field = getHeaderToColumn().get(fieldName);
        if (field == null) 
            return null;
        return getInteger(field);
    }

    public Integer getInteger(String fieldName,Integer defaultValue) {
        Integer field = getHeaderToColumn().get(fieldName);
        if (field == null) 
            return defaultValue;
        return getInteger(field,defaultValue);
    }
    
    
    public Boolean getBool(String fieldName) {
        Integer field = getHeaderToColumn().get(fieldName);
        if (field == null) 
            return null;
        return getBool(field);
    }

    public Boolean getBool(String fieldName, boolean defaultValue) {
        Integer field = getHeaderToColumn().get(fieldName);
        if (field == null) 
            return defaultValue;
        return getBool(field,defaultValue);
    }
    
    
//    public String[] getFoundValues() {
//        String fv[];
//        fv = java.util.Arrays.<String>copyOf(m_currentValues, m_foundValues);
//        return fv;
//    }

    public String[] getValues() {
        return m_currentValues;
    }
    
    /**
     * @return the m_foundValues
     */
    public int getFoundValuesCount() {
        return m_foundValues;
    }
    
    public int getMaxColumns() {
        return m_maxColumns;
    }

    
    protected void setMaxColumns(int columns) {
        m_maxColumns=columns;
    }    
    
    protected void transfer(CsvParser csv) {
        csv.m_cellValue = m_cellValue;
        csv.m_countHeader = m_countHeader;
        csv.m_currentLine = m_currentLine;
        csv.m_currentValues = m_currentValues.clone();
        csv.m_delimiter = m_delimiter;
        csv.m_doublequote  = m_doublequote;
        csv.m_foundValues = m_foundValues;
        csv.m_header  = m_header;
        csv.m_headerAlternatives  =   (ArrayList<HashSet<String>>) getHeaderAlternatives().clone();
        csv.m_headerToColumn = (HashMap<String,Integer>)getHeaderToColumn().clone();
        csv.m_input = m_input;
        csv.m_inputFile = getInputFile();
        csv.m_maxColumns = m_maxColumns;
        csv.m_quote = m_quote;
        
        
        
    }
    
    public boolean hasHeader() {
        return m_header != null;
    }

    /**
     * @return the m_currentLine
     */
    public String getCurrentLine() {
        return m_currentLine;
    }

    /**
     * @param currentValues the m_currentValues to set
     */
    protected void setCurrentValues(String[] currentValues) {
        this.m_currentValues = currentValues;
    }
    
    
    public String quoteValue(String value) {
        if (value == null)
            return MISSING_FIELD;
        if (value.contains(getDelimiter()) || value.startsWith(getQuote())) {
            return getQuote() + value.replace(getQuote(), getQuote() + getQuote()) + getQuote();
        }
        return value;
    }

    /**
     * @return the m_inputFile
     */
    public File getInputFile() {
        return m_inputFile;
    }
    
    
    public void close() throws IOException {
        m_input.close();
    }

    /**
     * list of list, containing alternatives for column-names
     * @return the m_headerAlternatives
     */
    public ArrayList<HashSet<String>> getHeaderAlternatives() {
        return m_headerAlternatives;
    }

    /**
     * maps the column-name to the column-index
     * @return the m_headerToColumn
     */
    public HashMap<String,Integer> getHeaderToColumn() {
        return m_headerToColumn;
    }
}
