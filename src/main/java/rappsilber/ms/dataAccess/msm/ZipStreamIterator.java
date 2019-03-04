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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import rappsilber.config.RunConfig;
import rappsilber.ms.ToleranceUnit;
import rappsilber.ms.dataAccess.utils.RobustFileInputStream;
import rappsilber.ms.spectra.Spectra;
import rappsilber.ms.statistics.utils.ObjectContainer;
import rappsilber.utils.Util;

/**
 * An alternative form of the ZipMSMListIterator, that can work with RobustFileInputStream.
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class ZipStreamIterator extends AbstractMSMAccess {
    private File inputfile;
    private InputStream instream;
    private Spectra currentSpectra = null;
    private Spectra nextSpectra = null;
    private AbstractMSMAccess currentAccess;
    private int countReadSpectra;
    private ZipInputStream zipinput; 
    
    ToleranceUnit tolerance;
    RunConfig config;
    int minCharge;
            
    
    private class ZipEntryStream extends InputStream {

        @Override
        public int read() throws IOException {
            return zipinput.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return zipinput.read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            zipinput.closeEntry();
        }
    }

    public ZipStreamIterator(File infile, ToleranceUnit tolerance, RunConfig config, int minCharge) throws IOException, ParseException {
        this(new RobustFileInputStream(infile), tolerance, config, minCharge);
    
        this.inputfile = infile;
        setInputPath(infile.getAbsolutePath());
        
                
    }

    public ZipStreamIterator(InputStream instream, ToleranceUnit tolerance, RunConfig config, int minCharge) throws IOException, ParseException {
        this.tolerance = tolerance;
        this.config = config;
        this.minCharge = minCharge;
        open(instream);
        readNext();
    }

    protected void open(InputStream instream1) throws IOException {
        this.instream = instream1;
        zipinput = new ZipInputStream(instream1);
    }
    
    
    
    @Override
    public Spectra current() {
        return currentSpectra;
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

    @Override
    public int countReadSpectra() {
        return countReadSpectra;
    }

    public void setReadSpectra(int count) {
        this.countReadSpectra = count;
    }
    
    @Override
    public boolean canRestart() {
        return inputfile != null;
    }

    @Override
    public void restart() throws IOException {
        try {
            zipinput.close();
        } catch (IOException ioe) {}
        currentAccess = null;
        nextSpectra=null;
        currentSpectra = null;
        open(new RobustFileInputStream(inputfile));
        try {
            readNext();
        } catch (ParseException ex) {
            Logger.getLogger(ZipStreamIterator.class.getName()).log(Level.SEVERE, null, ex);
            throw new IOException(ex);
        }
    }

    @Override
    public void close() {
        try {
            zipinput.close();
        } catch (IOException ex) {
            Logger.getLogger(ZipStreamIterator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public boolean hasNext() {
        return nextSpectra != null;
    }

    @Override
    public Spectra next() {
        currentSpectra = nextSpectra;
        if (currentSpectra != null) {
            try {
                readNext();
            } catch (IOException | ParseException ex) {
                Logger.getLogger(ZipStreamIterator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return currentSpectra;
    }

    private void readNext() throws IOException, ParseException {
        if (currentAccess != null) {
            if (currentAccess.hasNext()) {
                nextSpectra = currentAccess.next();
                return;
            }
            currentAccess.close();
        }
        ZipEntry ze;
        
        while ((ze = zipinput.getNextEntry()) != null) {
            if (!ze.isDirectory()) {
                double ratio = ze.getSize()/ze.getCompressedSize();
                if (ratio > 100)
                    throw new RuntimeException("Zip-file contains something, that would extract to " + Util.twoDigits.format(ratio) + " times the size.\n" +
                            "Assuming an error occoured!");
                if (ze.getName().toLowerCase().endsWith(".zip")) {
                    currentAccess = new ZipStreamIterator(new ZipEntryStream(), tolerance, config, minCharge);
                } else
                    currentAccess = AbstractMSMAccess.getMSMIterator(ze.getName(), new ZipEntryStream(), tolerance, minCharge, config);
                if (currentAccess != null && currentAccess.hasNext()) {
                    nextSpectra = currentAccess.next();
                    return;
                }
                    
                
            }
        }
        nextSpectra = null;
    }



    



}
