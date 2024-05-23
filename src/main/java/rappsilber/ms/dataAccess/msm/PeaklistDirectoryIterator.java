/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rappsilber.ms.dataAccess.msm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import rappsilber.config.RunConfig;
import rappsilber.ms.ToleranceUnit;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class PeaklistDirectoryIterator extends MSMListIterator {
    
    public PeaklistDirectoryIterator(File directory, ToleranceUnit t, int minCharge, RunConfig config) throws FileNotFoundException, IOException, ParseException {
        super(t, minCharge, config);
        for (File f : directory.listFiles()) {
            addFile(f.getAbsolutePath(), directory.getAbsolutePath(), t);
        }
        setNext();
    }
    
    
}
