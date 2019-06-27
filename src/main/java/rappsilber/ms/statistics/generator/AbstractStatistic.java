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
package rappsilber.ms.statistics.generator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public abstract class AbstractStatistic implements Statistic {

    /**
     * writes the result of the statist to the given file
     * @param path  the path the the file to be writen
     * @param append if the file already exits, should it be overwriten or the
     *               data appended to the existing file
     * @throws FileNotFoundException
     */
    public void writeFile(String path, boolean append) throws FileNotFoundException {
        writeFile(new File(path), append);
    }

    /**
     * writes the result of the statist to the given file
     * @param f  the file to be writen
     * @param append if the file already exits, should it be overwriten or the
     *               data appended to the existing file
     * @throws FileNotFoundException
     */
    public void writeFile(File f, boolean append) throws FileNotFoundException {
        writeFile(new FileOutputStream(f, append));
    }
    
    public void writeFile(OutputStream output) {
        PrintStream out = new PrintStream(output);
        out.println(getTable());
        out.flush();
    }

}
