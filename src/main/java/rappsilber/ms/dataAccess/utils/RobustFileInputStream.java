/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rappsilber.ms.dataAccess.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * wraps a FileInputStream that can survive temporary disconnects of the 
 * underling file-system.
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class RobustFileInputStream extends InputStream {
    File inputFile;
    FileInputStream inner;
    long pos = 0;
    
    public RobustFileInputStream(File inputFile) throws FileNotFoundException, IOException {
        this.inputFile = inputFile;
        boolean open = false;
        boolean throwFileNotFound = false;
        try {
            if (inputFile.getParentFile().exists() && !inputFile.exists())
                throwFileNotFound = true;
            if (inputFile.getParentFile().exists()) {
                inner = new FileInputStream(inputFile);
                open = true;
            }
        }catch (IOException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, 
                    "Could not open file \"" + inputFile.getAbsolutePath() + "\" - Will try again");
        }
        if (throwFileNotFound)
            throw new FileNotFoundException(inputFile.getAbsolutePath());

        if (!open) {
            int sleep = 500;
            int tries = 20;
            do {
                try {
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException ex) {}

                    // reopen the file
                    reopen();
                    return;
                } catch (IOException ioe) { 
                    if (tries--<=0) {
                        // no more tries
                        // forward the exception
                        throw ioe;
                    }
                    // increase the wait and try again
                    sleep+=sleep/2;
                }
            } while (true );
        
        }
    }

    public RobustFileInputStream(String inputFile) throws FileNotFoundException, IOException {
        this(new File(inputFile));
    }
    
    @Override
    public int read() throws IOException {
        try {
            int r =  inner.read();
            pos++;
            return r;
        } catch (IOException ioe) {
            return readAgain(20);
        }
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        try {
            int r =  inner.read(b, off, len);
            pos+=r;
            return r;
        } catch (IOException ioe) {
            return readAgain(20,b, off, len);
        }
    }
    
    /**
     * reopens the current file and skips to the current position
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public void reopen() throws FileNotFoundException, IOException {
        if (inputFile != null) {
            FileInputStream input = new FileInputStream(inputFile);
            input.skip(pos);
            
            try {
                if (inner != null)
                    inner.close();
            } catch(IOException ioe) {};
            inner = input;
            return;
        }
    }
    
    public int readAgain(int tries) throws IOException {
        int sleep = 500;
        do {
            try {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException ex) {}
                
                // reopen the file
                reopen();
                
                // read
                int r =  inner.read();
                
                //store the position
                pos++;
                
                return r;
            } catch (IOException ioe) { 
                if (tries--<=0) {
                    // no more tries
                    // forward the exception
                    throw ioe;
                }
                // increase the wait and try again
                sleep+=sleep/2;
            }
        } while (true );
    }
        

    public int readAgain(int tries, byte b[], int off, int len) throws IOException {
        int sleep = 500;
        do {
            try {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException ex) {}
                
                // reopen the file
                reopen();
                
                // read
                int r =  inner.read(b, off, len);
                
                //store the position
                pos+=r;
                
                return r;
            } catch (IOException ioe) { 
                if (tries--<=0) {
                    // no more tries
                    // forward the exception
                    throw ioe;
                }
                // increase the wait and try again
                sleep+=sleep/2;
            }
        } while (true );
    }

    
}
