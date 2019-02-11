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
package rappsilber.ms.dataAccess.msm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import rappsilber.config.RunConfig;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.spectra.Spectra;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class ZipMSMListIterator extends MSMListIterator {
    ZipFile m_zipfile;
    
    public ZipMSMListIterator(File MSMZipFile, ToleranceUnit t , int minCharge, RunConfig config) throws FileNotFoundException, IOException, ParseException {
        super(t,minCharge,config);
        m_inputPath = MSMZipFile.getAbsolutePath();
        m_zipfile = new ZipFile(MSMZipFile,ZipFile.OPEN_READ);
        
        Enumeration entries = m_zipfile.entries(); 
        if (entries.hasMoreElements())
        {
            while (entries.hasMoreElements())  {
                ZipEntry ze = (ZipEntry) entries.nextElement();
                if (!ze.isDirectory()) {
                    if (ze.getCompressedSize() > 0) {
                        double ratio = ze.getSize()/ze.getCompressedSize();
                        if (ratio > 100)
                            throw new RuntimeException("Zip-file contains something, that would extract to " + Util.twoDigits.format(ratio) + " times the size.\n" +
                                    "Assuming an error occoured!");
                        addFile(ze.getName(), m_zipfile.getInputStream(ze), t);
                    }
                }
            }

            setNext();
        } else {
            setInnerIterator(getMSMiterators().iterator());
        }

    }

    @Override
    protected void publishNextSpectra(Spectra s){
    }

    
    
    
    @Override
    public void gatherData() throws FileNotFoundException {
        m_MaxPrecursorMass=0;
        m_scanCount = 0;
        Spectra s;
        while ((s = next()) != null) {
            m_MaxPrecursorMass = Math.max(m_MaxPrecursorMass, s.getPrecurserMass());
            m_scanCount ++;
        }
        setReadSpectra(0);
        try {
            restart();
        } catch (IOException ex) {
            Logger.getLogger(ZipMSMListIterator.class.getName()).log(Level.SEVERE, null, ex);
            
        }
    }
    
    protected void gatherData(AbstractMSMAccess inner) {
        double MaxPrecursorMass = 0;
        int scanCount = 0;
        Spectra s;
        
        while ((s = inner.next()) != null) {
            MaxPrecursorMass = Math.max(MaxPrecursorMass, s.getPrecurserMass());
            scanCount ++;
        }
        
        setReadSpectra(0);
        
        synchronized(gatherDataSync) {
            m_MaxPrecursorMass = Math.max(m_MaxPrecursorMass, MaxPrecursorMass);
            m_scanCount += scanCount;
        }
        
        
    }
    
    
    @Override
    public void restart() throws IOException, FileNotFoundException {
        if (m_zipfile.entries().hasMoreElements() ) {
            super.closeAndForget();
    //        m_zipfile.close();
    //        m_zipfile = new ZipFile(m_inputPath);

            Enumeration entries = m_zipfile.entries();
            while (entries.hasMoreElements())  {
                ZipEntry ze = (ZipEntry) entries.nextElement();

                try {
                    addFile(ze.getName(), m_zipfile.getInputStream(ze), getToleranceUnit());
                } catch (ParseException ex) {
                    Logger.getLogger(ZipMSMListIterator.class.getName()).log(Level.SEVERE, "Error reopening the zipfile ", ex);
                    m_config.getStatusInterface().setStatus("Error reopening the zipfile " + ex);
                    System.exit(-1);
                }
            }        

            setNext();
        }
    }
    
}
