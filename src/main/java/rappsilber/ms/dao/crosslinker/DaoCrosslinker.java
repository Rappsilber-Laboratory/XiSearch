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
package rappsilber.ms.dao.crosslinker;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import rappsilber.config.ConfigEntity;
import rappsilber.ms.crosslinker.CrossLinker;
import rappsilber.ms.crosslinker.SymetricSingleAminoAcidRestrictedCrossLinker;
import rappsilber.ms.dao.Dao;

/**
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class DaoCrosslinker implements Dao<ArrayList<CrossLinker>> {
    
    ArrayList<CrossLinker> m_crossLinker = new ArrayList<CrossLinker>(1);

    public DaoCrosslinker(ConfigEntity conf) {
        try {
            Class cl = conf.getConfigClass(SymetricSingleAminoAcidRestrictedCrossLinker.class);
            Constructor clc = cl.getConstructor(conf.getClass());
            m_crossLinker.add((CrossLinker) clc.newInstance(conf));
        } catch (InstantiationException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }

    }



    @Override
    public ArrayList<CrossLinker> get() {
        return m_crossLinker;
    }

    @Override
    public void ReadConfig(ConfigEntity conf) {
        try {
            Class cl = conf.getConfigClass(SymetricSingleAminoAcidRestrictedCrossLinker.class);
            Constructor clc = cl.getConstructor(conf.getClass());
            m_crossLinker.add((CrossLinker) clc.newInstance(conf));
        } catch (InstantiationException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public ArrayList<CrossLinker> get(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


}
