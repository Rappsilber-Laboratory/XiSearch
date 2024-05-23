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
import javax.swing.JTextField;

/**
 * a logging-handler, that forwards the logging-messages to a JTextField.
 * Mainly to use the logging info also as status updates
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class JTextFieldHandle extends Handler {
    //the window to which the logging is done

    private JTextField m_output = null;

    private Formatter formatter = null;

    private Level level = null;

    private final Object publishSync = new Object();

    public class SingleLineFormatter extends Formatter {

        /**
         * Format the given LogRecord.
         *
         * @param record the log record to be formatted.
         * @return a formatted log record
         */
        public synchronized String format(LogRecord record) {

            return record.getMessage().replaceAll("[\r\n]+", " ");
        }
    }

    /**
     * private constructor, preventing initialisation
     *
     * @param out the JTextArea that should display the log
     */
    public JTextFieldHandle(JTextField out) {
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
        setFormatter(new SingleLineFormatter());

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
        synchronized (publishSync) {
            String message = null;
            //check if the record is loggable
            if (!isLoggable(record)) {
                return;
            }
            try {
                message = getFormatter().format(record);
            } catch (Exception e) {
                reportError(null, e, ErrorManager.FORMAT_FAILURE);
            }

            try {

                m_output.setText(message.replaceAll("[\r\n]+", " "));

            } catch (Exception ex) {
                reportError(null, ex, ErrorManager.WRITE_FAILURE);
            }
        }

    }

    public void close() {
    }

    public void flush() {
    }
}
