/*
 * Copyright 2018 Lutz Fischer <lfischer@staffmail.ed.ac.uk>.
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
package rappsilber.ms.dataAccess.output;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import rappsilber.ms.sequence.ions.Fragment;
import rappsilber.ms.spectra.SpectraPeak;
import rappsilber.ms.spectra.annotation.SpectraPeakMatchedFragment;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.ms.statistics.utils.StreamingAverageStdDev;
import rappsilber.ms.statistics.utils.UpdateableInteger;
import rappsilber.utils.Util;

/**
 *
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class ErrorStatsCollector extends AbstractResultWriter {
    

    class ErrorCount extends TreeMap<Short,UpdateableInteger> {

        StreamingAverageStdDev meanstd = new StreamingAverageStdDev();
        
        public ErrorCount(double error) {
            Double e =  error*10000000;
            Short r = e.shortValue();
            UpdateableInteger i = new UpdateableInteger(1);
            put(r,i);
            meanstd.addValue(error);
        }
        
        public void register(double error) {
            Double e =  error*10000000;
            Short r = e.shortValue();
            UpdateableInteger i = get(r);
            if (i == null) {
                i = new UpdateableInteger(1);
                put(r,i);
            } else {
                i.value++;
            }
            meanstd.addValue(error);
        }

        private StringBuilder toStringBuilder(String prepent) {
            StringBuilder sb = new StringBuilder();
            for (short mz : keySet()) {
                sb.append(prepent).append(mz).append(",").append(get(mz)).append("\n");
            }

            sb.append(prepent).append("S").append(meanstd.stdDev()).append(",A").append(meanstd.average()).append("\n");
            
            return sb;        
        }
        
    }
    class MZErrorStats extends TreeMap<Short,ErrorCount> {
        StreamingAverageStdDev meanstd = new StreamingAverageStdDev();

        public MZErrorStats(Double mz,double error) {
            Short k = mz.shortValue();
            put(k,new ErrorCount(error));
            meanstd.addValue(error);
        }
        
        public void register(Double mz,double error) {
            Short k = mz.shortValue();
            ErrorCount ec = get(k);
            if (ec == null) {
                put(k,new ErrorCount(error));
            } else {
                ec.register(error);
            }
            meanstd.addValue(error);
        }

        private StringBuilder toStringBuilder(String prepent) {
            StringBuilder sb = new StringBuilder();
            for (short mz : keySet()) {
                sb.append(get(mz).toStringBuilder(prepent + mz + ","));
            }
            sb.append(prepent).append("S").append(meanstd.stdDev()).append(",A").append(meanstd.average()).append("\n");
            return sb;
        }
    }
    
    class ScoreErrorStats extends TreeMap<Short,MZErrorStats> {
        StreamingAverageStdDev meanstd = new StreamingAverageStdDev();

        public ScoreErrorStats(Double score, Double mz,double error) {
            Double dk = score *10;
            Short k =  dk.shortValue();
            put(k,new MZErrorStats(mz,error));
            meanstd.addValue(error);
        }
        
        public void register(Double score, Double mz,double error) {
            Double dk = score *10;
            Short k =  dk.shortValue();
            MZErrorStats mze = get(k);
            if (mze == null) {
                put(k,new MZErrorStats(mz,error));
            } else {
                mze.register(mz,error);
            }
            meanstd.addValue(error);
        }

        private StringBuilder toStringBuilder(String prepent) {
            StringBuilder sb = new StringBuilder();
            for (double score : keySet()) {
                sb.append(get(score).toStringBuilder(prepent + score + ","));
            }
            sb.append(prepent).append("S").append(meanstd.stdDev()).append(",A").append(meanstd.average()).append("\n");
            return sb;
        }
    }
    
    class RunStats extends HashMap<String , ScoreErrorStats>{
        StreamingAverageStdDev meanstd = new StreamingAverageStdDev();
        public void register(String run, Double score, Double mz,double error) {
            ScoreErrorStats ses = get(run);
            if (ses == null) {
                put(run,new ScoreErrorStats(score,mz,error));
            } else {
                ses.register(score,mz,error);
            }           
            meanstd.addValue(error);
        }
        
        public String toString(String prepent) {
            StringBuilder sb = new StringBuilder();
            for (String run : keySet()) {
                sb.append(get(run).toStringBuilder(prepent + run + ","));
            }
            sb.append(prepent).append("S").append(meanstd.stdDev()).append(",A").append(meanstd.average()).append("\n");
            return sb.toString();
        }
        public void write(BufferedResultWriter out) {
            
        }
    }
    
    
    RunStats ms1TopTarget = new RunStats();
    RunStats ms2TopTarget = new RunStats();
    RunStats ms1AllDecoy = new RunStats();
    RunStats ms2AllDecoy = new RunStats();
    
    @Override
    public void writeHeader() {
        
    }

    @Override
    public void writeResult(MatchedXlinkedPeptide match) throws IOException {
        String run = match.getSpectrum().getRun();
        double score = match.getScore();
        double expMZ = match.getExpMz();
        double calcMZ = match.getCalcMass()/match.getExpCharge()+Util.PROTON_MASS;
        double error = (expMZ-calcMZ)/calcMZ*1000000;
        double ms2Count = 0;
        double ms2SummError = 0;
        
        
        if (match.getMatchrank() == 1 && !match.isDecoy()) {
            ms1TopTarget.register(run, score, expMZ, error);
            int annotations =0;
            peakloop: for (SpectraPeak sp : match.getSpectrum().getTopPeaks(1000)) {
                for (SpectraPeakMatchedFragment mf :sp.getMatchedAnnotation()) {
                    // only considere the primary annotation for a peak
                    if (mf.isPrimary()) {
                        annotations++;
                        registerFragment(ms2TopTarget, mf, sp, run, score);
                        if (annotations>=10) {
                            break peakloop;
                        }
                        break;
                    }
                }
            }
        } else if (match.isDecoy()) {
            ms1AllDecoy.register(run, score, expMZ, error);
            int annotations =0;
            ArrayList<SpectraPeak> peaks =  match.getSpectrum().getTopPeaks(-1);
            peakloop: for (int p =peaks.size()-1;p>=0;p--) {
                SpectraPeak sp = peaks.get(p);
                for (SpectraPeakMatchedFragment mf :sp.getMatchedAnnotation()) {
                    // only considere the primary annotation for a peak
                    if (mf.isPrimary()) {
                        annotations++;
                        registerFragment(ms2AllDecoy, mf, sp, run, score);
                        if (annotations>=10) {
                            break peakloop;
                        }
                        break;
                    }
                }
            }
        }
    }

    public void registerFragment(RunStats rs ,SpectraPeakMatchedFragment mf, SpectraPeak sp, String run, double score) {
        Fragment f = mf.getFragment();
        int z = mf.getCharge();
        double fcMZ = f.getMass(z);
        double peakMZ = sp.getMZ();
        double peakError = (peakMZ-fcMZ)/fcMZ*1000000;
        // matched as missing monosisotopic
        if (mf.matchedMissing()) {
            // find the correct monoisotopic peak;
            double mi=Util.C13_MASS_DIFFERENCE/z;
            double me = Math.abs(peakMZ-mi-fcMZ);
            int i = 1;
            while (true) {
                i++;
                double cme = Math.abs(peakMZ-mi*i-fcMZ);
                if (cme<me) {
                    me = cme;
                } else {
                    break;
                }
            }
            rs.register(run, score, peakMZ, me/fcMZ*1000000);
        } else {
            rs.register(run, score, peakMZ, peakError);
        }
    }

    @Override
    public int getResultCount() {
        return 0;
    }

    @Override
    public int getTopResultCount() {
        return 0;
    }

    @Override
    public void flush() {
    }

    @Override
    public void finished() {
        super.finished(); //To change body of generated methods, choose Tools | Templates.
        
    }
    
    
    
}
