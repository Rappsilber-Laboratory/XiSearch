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

import java.awt.Component;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import rappsilber.config.LocalProperties;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class FileDialogButton2 extends JButton {
    private static final long serialVersionUID = 676093585963269187L;

    static final String DefaultLocalPropertyKey = "LastAccessedFolder";

    /** the file that was selected **/
    private File m_file = null;
    /** extensions available in the dropdown list **/
    private String[] m_extensions = new String[]{"*"};
    /** Description of the filetype **/
    private String m_description = "Files";
    /** under what category to save the last accesed directory **/
    private String m_LocalPropertyKey = DefaultLocalPropertyKey;
    /** should there be a load or a save dialog **/
    private boolean m_doLoad = true;

    private ArrayList<ActionListener> m_listeners = new ArrayList<ActionListener>();

    public FileDialogButton2(String text, Icon icon) {
        super(text, icon);
    }

    public FileDialogButton2(Icon icon) {
        super(icon);
    }

    public FileDialogButton2(Action a) {
        super(a);
    }


    public void setFile(String path) {
        if (path.length() == 0)
            unsetFile();
        else
            setFile(new File(path));
    }

    public void setFile(File path) {
        m_file = path;
        if (path  == null) {
            return;
        }
        if ((path.exists()) &&  path.isDirectory())
            LocalProperties.setFolder(m_LocalPropertyKey, path);
        else
            LocalProperties.setFolder(m_LocalPropertyKey, path.getParent());

    }

    public void unsetFile() {
        m_file = null;
    }

    public void detectFrames() {
        if (m_LocalPropertyKey == DefaultLocalPropertyKey) {
            Component p = this;
            String pathKey;
            while (p.getParent() != null)
                p = p.getParent();
            if (p instanceof JFrame) {
                pathKey = ((JFrame) p).getTitle();
                m_LocalPropertyKey = pathKey;
            }
        }
    }


    private void formAncestorAdded(javax.swing.event.AncestorEvent evt) {
        detectFrames();
    }

    @Override
    public void addActionListener(java.awt.event.ActionListener listener) {
        m_listeners.add(listener);
    }




    /**
     * @return the m_LocalPropertyKey
     */
    public String getLocalPropertyKey() {
        return m_LocalPropertyKey;
    }

    /**
     * @param LocalPropertyKey the LocalPropertyKey to set
     */
    public void setLocalPropertyKey(String LocalPropertyKey) {
        this.m_LocalPropertyKey = LocalPropertyKey;
    }

    /**
     * @return the extensions
     */
    public String[] getExtensions() {
        return m_extensions;
    }

    /**
     * @param extensions the extensions to set
     */
    public void setExtensions(String[] extensions) {
        this.m_extensions = extensions;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return m_description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.m_description = description;
    }

    public void setLoad() {
        m_doLoad = true;
    }

    public void setSave() {
        m_doLoad = false;
    }

    public File getFile() {
        return m_file;
    }



}
