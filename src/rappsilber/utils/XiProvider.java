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
package rappsilber.utils;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.applications.SimpleXiProcessLinearIncluded;
import rappsilber.applications.XiProcess;
import rappsilber.config.RunConfig;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.dataAccess.AbstractSpectraAccess;
import rappsilber.ms.dataAccess.StackedSpectraAccess;
import rappsilber.ms.dataAccess.output.ResultWriter;
import rappsilber.ms.sequence.SequenceList;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class XiProvider {

    public static XiProcess getXiSearch(SequenceList sl, AbstractSpectraAccess input, ResultWriter output, StackedSpectraAccess sc, RunConfig conf, Class defaultClass) {
            //            xi = new SimpleXiProcessDevMGX(new File(fastaFile), sequences, output, conf, sc);
            //            xi = new SimpleXiProcessDev(new File(fastaFile), sequences, output, conf, sc);
            //            xi = new SimpleXiProcess(new File(fastaFile), sequences, output, conf, sc);
            XiProcess xi = null;
            ArrayList<CrossLinker> cl = conf.getCrossLinker();
            String xiClassName = defaultClass.getName();

            
            if (cl.size()>0) {
                for (int i = 0; i< cl.size();i++) 
                    if (cl.get(i).getName().contentEquals("OpenModification")) {
                        xiClassName = "rappsilber.applications.SimpleXiProcessOpenModificationXlink";
                    } else
                    if (cl.get(i).getName().contentEquals("TargetModification")) {
                        xiClassName = "rappsilber.applications.SimpleXiProcessTargetModificationXlink";
                    }
            }
            

            xiClassName = conf.retrieveObject("XICLASS", xiClassName);

            Class xiclass;

            try {
                Logger.getLogger(XiProvider.class.getName()).log(Level.INFO, "Alternative version of Xi used:{0}", xiClassName);
                xiclass = Class.forName(xiClassName);
            } catch (ClassNotFoundException ex) {
                try {
                    Logger.getLogger(XiProvider.class.getName()).log(Level.INFO,"Alternative version of Xi used: rappsilber.applications." + xiClassName);
                    xiclass = Class.forName("rappsilber.applications." + xiClassName);
                    
                } catch (ClassNotFoundException ex2) {
                    xiclass = SimpleXiProcessLinearIncluded.class;
                    Logger.getLogger(XiProvider.class.getName()).log(Level.INFO,"Could not load alternative XiVersion - will run with: " + SimpleXiProcessLinearIncluded.class.getName(), new Exception(""));
                }
            }
            Constructor xiConstructor = null;
            try {
//                xiConstructor = xiclass.getConstructor(File.class, AbstractSpectraAccess.class, ResultWriter.class, RunConfig.class, StackedSpectraAccess.class);
                  xiConstructor = xiclass.getConstructor(SequenceList.class, AbstractSpectraAccess.class, ResultWriter.class, RunConfig.class, StackedSpectraAccess.class);
            } catch (Exception ex) {
                Logger.getLogger(XiProvider.class.getName()).log(Level.INFO,"Could not instanciate constructor will use SimpleXiProcessDevMGX" , new Exception(""));
                xi = new SimpleXiProcessLinearIncluded(sl, input, output, conf, sc);
//
//                xi = new SimpleXiProcessLinearIncluded(sl, input, output, conf, sc);
            }
            if (xi == null) {
                try {
                    xi = (XiProcess) xiConstructor.newInstance(sl, input, output, conf, sc);
                } catch (Exception ex) {
                    xi = new SimpleXiProcessLinearIncluded(sl, input, output, conf, sc);
                }
            }
            return xi;
        }

}
