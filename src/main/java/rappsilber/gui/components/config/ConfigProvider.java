/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rappsilber.gui.components.config;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public interface ConfigProvider {
    String getConfig() throws IOException;
    void loadConfig(File f, boolean append);
    public void loadConfig(String config, boolean append);
    
}
