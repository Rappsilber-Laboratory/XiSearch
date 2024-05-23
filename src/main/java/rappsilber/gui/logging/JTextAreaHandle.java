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
package rappsilber.gui.logging;

import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import javax.swing.JTextArea;

/**
 * a logging-handler, that forwards the logging-messages to a JTextArea.
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class JTextAreaHandle extends Handler {
    //the window to which the logging is done

    private JTextArea m_output = null;

    private StringBuffer m_log = new StringBuffer();

    private Formatter formatter = null;

    private Level level = null;

    private int m_maxlogsize = 1000000;

    private final Object publishSync = new Object();

    /**
     * private constructor, preventing initialisation
     *
     * @param out the JTextArea that should display the log
     */
    public JTextAreaHandle(JTextArea out) {
        configure();
        m_output = out;
    }

    /**
     * The getInstance method returns the singleton instance of the
     * WindowHandler object It is synchronised to prevent two threads trying to
     * create an instance simultaneously. @ return WindowHandler object
     */
//  public static synchronized JTextAreaHandle getInstance() {
//
//    if (handler == null) {
//      handler = new WindowHandler();
//    }
//    return handler;
//  }
    /**
     * This method loads the configuration properties from the JDK level
     * configuration file with the help of the LogManager class. It then sets
     * its level, filter and formatter properties.
     */
    private void configure() {
        LogManager manager = LogManager.getLogManager();
        String className = this.getClass().getName();
        String level = manager.getProperty(className + ".level");
        String filter = manager.getProperty(className + ".filter");
        String formatter = manager.getProperty(className + ".formatter");

        //accessing super class methods to set the parameters
        setLevel(level != null ? Level.parse(level) : Level.INFO);
        //setFilter(makeFilter(filter));
        setFormatter(makeFormatter(formatter));

    }

//  /**
//   * private method constructing a Filter object with the filter name.
//   *
//   * @param filterName
//   *            the name of the filter
//   * @return the Filter object
//   */
//  private Filter makeFilter(String filterName) {
//    Class c = null;
//    Filter f = null;
//    try {
//      c = Class.forName(filterName);
//      f = (Filter) c.newInstance();
//    } catch (Exception e) {
//      System.out.println("There was a problem to load the filter class: "
//          + filterName);
//    }
//    return f;
//  }
    /**
     * private method creating a Formatter object with the formatter name. If no
     * name is specified, it returns a SimpleFormatter object
     *
     * @param formatterName the name of the formatter
     * @return Formatter object
     */
    private Formatter makeFormatter(String formatterName) {
        Class c = null;
        Formatter f = null;

        try {
            c = Class.forName(formatterName);
            f = (Formatter) c.newInstance();
        } catch (Exception e) {
            System.err.println(e);
            f = new SimpleFormatter();
        }
        return f;
    }

    public void publish(String message) {
        synchronized (publishSync) {
            try {
                m_log.append(message);
                int l = m_log.length();
                if (l > m_maxlogsize) {
                    int delTo = m_log.indexOf("\n", l - m_maxlogsize);
                    if (delTo < 0) {
                        delTo = l - m_maxlogsize;
                    }
                    m_log.delete(0, delTo);
                }

                m_output.setText(m_log.toString());
                m_output.select(Integer.MAX_VALUE, Integer.MAX_VALUE);

                //x = m_output.getSelectionEnd();
                //m_output.select(x,x);
            } catch (Exception ex) {
                System.err.println(ex);
                reportError(null, ex, ErrorManager.WRITE_FAILURE);
            }
        }
    }

    /**
     * This is the overridden publish method of the abstract super class
     * Handler. This method writes the logging information to the associated
     * Java window. This method is synchronized to make it thread-safe. In case
     * there is a problem, it reports the problem with the ErrorManager, only
     * once and silently ignores the others.
     *
     * @record the LogRecord object
     *
     */
    public void publish(LogRecord record) {
        String message = null;
        //check if the record is loggable
        if (!isLoggable(record)) {
            return;
        }
        try {
            message = getFormatter().format(record);
        } catch (Exception e) {
            System.err.println(e);
            reportError(null, e, ErrorManager.FORMAT_FAILURE);
        }
        publish(message);
    }

    public void close() {
    }

    public void flush() {
    }
}
