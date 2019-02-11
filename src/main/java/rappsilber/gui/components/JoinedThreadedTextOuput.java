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

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import rappsilber.ms.statistics.utils.UpdateableInteger;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class JoinedThreadedTextOuput {
    /** list of the latest messages, that a thread wanted to display */
    HashMap<Thread,String> m_messages = new HashMap<Thread, String>();
    /** was a message already displayed? */
    HashMap<Thread,Boolean> m_messagesPublished = new HashMap<Thread, Boolean>();
    /** messages that have been displayed for a certain time can be "over-written" by other threads*/
    HashMap<Thread, UpdateableInteger> m_messagestimeout = new HashMap<Thread, UpdateableInteger>();
    
    /** 
     * what text-components should display the messages
     * new messages will be appended to the text displayed in these components
     */
    HashSet<JTextComponent> m_guiout = new HashSet<JTextComponent>();

    /** 
     * what JTextFields should display the messages.
     * new messages will overwrite existing messages
     */
    HashSet<JTextField> m_guioutTextField = new HashSet<JTextField>();
    /** 
     * where to log messages to?
     */
    HashSet<Logger> m_logout = new HashSet<Logger>();
    /** 
     * should we also write messages to a stream?
     */
    HashSet<OutputStream> m_streamout = new HashSet<OutputStream>();
    /**
     * prefer to work with printwriter. So each stream gets also a PrintWriter assigned.
     */
    HashMap<OutputStream, PrintWriter> m_printout = new HashMap<OutputStream, PrintWriter>();
    /**
     * A timer-thread running as deamon-taking care of cleaning up old messages.
     */
    java.util.Timer m_cleanupTimer = new java.util.Timer(true);
    private int m_timeout = 600;
    
    protected class CleanUpTask extends TimerTask {
        AtomicBoolean running = new AtomicBoolean(false);
        @Override
        public void run() {
            if (running.compareAndSet(false, true)) {
            
                ArrayList<Thread> delete = new ArrayList<Thread>();
                for (Thread t : m_messages.keySet()) {
                    if (!t.isAlive()) {
                        delete.add(t);
                    } else {
                        UpdateableInteger ui =m_messagestimeout.get(t);
                        if (ui!= null && ui.value-- ==0) {
                            delete.add(t);
                        }
                    }
                }
                for (Thread t : delete) {
                    m_messages.remove(t);
                    m_messagesPublished.remove(t);
                    m_messagestimeout.remove(t);
                }
                running.set(false);
            }
        }
        
    }
    

    public JoinedThreadedTextOuput() {
    
        m_cleanupTimer.scheduleAtFixedRate(new CleanUpTask(), 1000, 1000);
        
    }
    
    
    public void addTextOutput(JTextComponent out) {
        if (out instanceof JTextField)
            m_guioutTextField.add((JTextField) out);
        else
            m_guiout.add(out);
    }
    
    
    public void addLoggerOutput(Logger out) {
        m_logout.add(out);
    }             
    
    public void addStreamOutput(OutputStream out) {
        m_streamout.add(out);
        m_printout.put(out, new PrintWriter(out));
    }             
    
    public void removeTextOutput(JTextComponent out) {
        m_guiout.remove(out);
        m_guioutTextField.remove(out);
    }
    
    
    public void removeLoggerOutput(Logger out) {
        m_logout.remove(out);
    }             
    
    public void removeStreamOutput(OutputStream out) {
        m_streamout.remove(out);
        m_printout.remove(out);
    }            
    
    
    public void write(String message) {
        Thread origin = Thread.currentThread();
        m_messages.put(origin, message);
        m_messagesPublished.put(origin, Boolean.FALSE);
        m_messagestimeout.put(origin, new UpdateableInteger(m_timeout));
        publish();
    }
        
    public void writeNoTimeOut(String message) {
        Thread origin = Thread.currentThread();
        m_messages.put(origin, message);
        m_messagesPublished.put(origin, Boolean.FALSE);
        m_messagestimeout.put(origin, null);
        publish();
    }
           
    protected synchronized void publish() {
        // message send to text-fields
        final StringBuffer lineMessage = new StringBuffer();
        // messages send to other target - these include only unpublished messages
        final StringBuffer textMessage = new StringBuffer();
        ArrayList<Thread> delete = new ArrayList<Thread>();
        
        // join up the messages
        for (Thread t : m_messages.keySet()) {
            String m = m_messages.get(t);
            if (!m_messagesPublished.get(t)) {
                textMessage.append("\n");
                textMessage.append(m);
                for (Logger l : m_logout) {
                    l.log(Level.INFO, m);
                }
                for (PrintWriter pw : m_printout.values()) {
                    pw.println(m);
                }
                m_messagesPublished.put(t,Boolean.TRUE);
            }
            lineMessage.append(" | ");
            lineMessage.append(m);
            
            // if the originating thread is dead
            if (!t.isAlive()) {
                // we won't publish anything again from it
                delete.add(t);
            }
        }
        
        javax.swing.SwingUtilities.invokeLater( new Runnable() {

            public void run() {
                for (JTextComponent tc : m_guiout) {
                    tc.setText(tc.getText() + textMessage);
                }
                for (JTextField tf : m_guioutTextField) {
                    tf.setText(lineMessage.substring(3));
                }
            }
        });
        for (Thread t : delete) {
            m_messages.remove(t);
            m_messagesPublished.remove(t);
            m_messagestimeout.remove(t);
        }
        
        
    }
    
}
