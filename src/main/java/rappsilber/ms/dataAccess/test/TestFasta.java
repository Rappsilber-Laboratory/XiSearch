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
package rappsilber.ms.dataAccess.test;

import java.io.File;
import rappsilber.config.AbstractRunConfig;
import rappsilber.gui.components.ShowText;
import rappsilber.gui.components.getFileDialog;
import rappsilber.ms.sequence.Sequence;
import rappsilber.ms.sequence.SequenceList;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public abstract class TestFasta {
    public static class TestFastaStatistic {
        public long countProteins = 0;
        public long countAminoAcids = 0;
        public boolean wasError = false;
        public Exception errorException = null;
    
        public String toString() {
            String r  = "Proteins: " + countProteins + "\nAminoAcids: " + countAminoAcids + (wasError? "\nError occured" : "\nSuccess");
            if (wasError) {
                errorException.printStackTrace(System.out);
                r += "\n" + errorException.getMessage();
                StackTraceElement[] st = errorException.getStackTrace();
                for (int i=0; i< st.length;i++) {
                    r+= "\n" + st[i].toString();
                }
            }            
            return r;
        }

    }
     /**
     * Tests whether a file can be used as fasta-file.
     * If there is a problem with the file an error message will be reported (the message returned from the error message)
     * @param msmFile
     * @return null if everything is ok; the error message otherwise 
     */
    public static TestFastaStatistic testFasta(String msmFile) {
        TestFasta.TestFastaStatistic stats = new TestFastaStatistic();
        try {
            SequenceList sl = new SequenceList(SequenceList.DECOY_GENERATION.ISTARGET, new File(msmFile),AbstractRunConfig.DUMMYCONFIG);
            for (Sequence s : sl) {
                stats.countProteins ++;
                stats.countAminoAcids += s.length();
            }
        } catch (Exception e) {
            stats.errorException = e;
            stats.wasError = true;
        }
        // every thing is fine
        return stats;
    }
    
    public static void main(String[] args) {
        TestFastaStatistic result =null;
        
        if (args.length == 0) {
            File f = getFileDialog.getFile(new String[]{".fasta",".txt"},"Fasta-File");
            result = testFasta(f.getAbsolutePath());
            ShowText.showText(result.toString());
            
        }   else {
            result = testFasta(args[0]);
        }        
        
        System.out.println(result.toString());
    }
}
