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
import javax.swing.JOptionPane;

/**
 * a logging-handler, that forwards the logging-messages to a JTextArea.
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class JMessageBoxHandle extends Handler {
  //the window to which the logging is done
  private Formatter formatter = null;
  private boolean m_blockOnMessage = false;

  private Level level = null;


  /**
   * private constructor, preventing initialisation
   * @param out the JTextArea that should display the log
   */
  public JMessageBoxHandle(boolean blockOnMessage) {
    m_blockOnMessage = blockOnMessage;
    configure();
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
    setLevel(level != null ? Level.parse(level) : Level.WARNING);
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
   * @param formatterName
   *            the name of the formatter
   * @return Formatter object
   */
  private Formatter makeFormatter(String formatterName) {
    Class c = null;
    Formatter f = null;

    try {
      c = Class.forName(formatterName);
      f = (Formatter) c.newInstance();
    } catch (Exception e) {
      f = new SimpleFormatter();
    }
    return f;
  }

  protected void showDialog(LogRecord record) {
      if (record.getLevel().intValue() > Level.WARNING.intValue()) {
          int answer = JOptionPane.showConfirmDialog(null, record.getMessage() +"\n\n" + record.getThrown().toString() + "\n\nClose the application?", record.getLevel().getName(), JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
          if (answer == JOptionPane.YES_OPTION) {
              System.exit(1);
          }
      } else {
          JOptionPane.showMessageDialog(null, record.getMessage(), record.getLevel().getName(),JOptionPane.INFORMATION_MESSAGE);
      }
  }

  private class RecordRunnable implements Runnable {
    LogRecord record;
    public RecordRunnable(LogRecord record) {
        this.record = record;
    }
    public void run() {
        showDialog(record);
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
  public synchronized void publish(LogRecord record) {
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
      if (m_blockOnMessage || record.getLevel().intValue() >= Level.SEVERE.intValue()) {
        showDialog(record);
      } else {
          new Thread(new RecordRunnable(record)).start();
      }

      //x = m_output.getSelectionEnd();
      //m_output.select(x,x);
    } catch (Exception ex) {
      reportError(null, ex, ErrorManager.WRITE_FAILURE);
    }

  }

  public void close() {
  }

  public void flush() {
  }
}

