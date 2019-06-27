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
package rappsilber.ms.score;

import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;

/**
 * This class is the result of a weka RandomTree run on PolII data.
 * Input where the scores for validated scans, that where categorised into
 * confidence level A, B and C.
 * The random Tree where build with the following settings:
 *
 * weka.classifiers.trees.RandomTree -K 10 -M 20.0 -S 1 -depth 5
 *
 *  - consider 10 random scores on each decision (-K 10)
 *  - have at least 20 rows for each leaf (-M 20.0)
 *      - thats actually the weight but all rows have a weight of 1
 *  - "random" seed is 1
 *  - build a tree of at most a depth of 5 (-depth 5)
 *
 * the resulting tree was:
 * mgxScore < 59.71
 * |   fragment conservative coverage < 0.18
 * |   |   match score < 4.38 : Z (18738.26/0)
 * |   |   match score >= 4.38 : Z (7/1)
 * |   fragment conservative coverage >= 0.18
 * |   |   total fragment matches < 8.5
 * |   |   |   fragment unique matched conservative < 7.5 : Z (1019/0)
 * |   |   |   fragment unique matched conservative >= 7.5 : Z (21.9/2)
 * |   |   total fragment matches >= 8.5
 * |   |   |   Average1-RelativeMS2Error < 0.8
 * |   |   |   |   spectra intensity nonlossy coverage < 0.57 : Z (127/3)
 * |   |   |   |   spectra intensity nonlossy coverage >= 0.57 : Z (16/5)
 * |   |   |   Average1-RelativeMS2Error >= 0.8 : Z (34/13) mgxScore >= 59.71
 * |   fragment unique matched non lossy coverage < 0.3
 * |   |   mgcShiftedDelta < 95.47
 * |   |   |   peptide2 unique matched conservative < 2.5
 * |   |   |   |   Precoursor Absolute Error < 3.26 : Z (381.39/76)
 * |   |   |   |   Precoursor Absolute Error >= 3.26 : Z (145.73/0)
 * |   |   |   peptide2 unique matched conservative >= 2.5
 * |   |   |   |   spectra intensity nonlossy coverage < 0.43 : Z (66.73/28)
 * |   |   |   |   spectra intensity nonlossy coverage >= 0.43 : B (72/28)
 * |   |   mgcShiftedDelta >= 95.47
 * |   |   |   peptide2 non lossy matched < 2.5 : Z (26/10)
 * |   |   |   peptide2 non lossy matched >= 2.5
 * |   |   |   |   fragment coverage < 0.29 : B (12/4)
 * |   |   |   |   fragment coverage >= 0.29 : A (35/17)
 * |   fragment unique matched non lossy coverage >= 0.3
 * |   |   combinedDelta < 4.58
 * |   |   |   peptide2 unique matched non lossy coverage < 0.04
 * |   |   |   |   Precoursor Absolute Error < 2.28 : Z (177/39)
 * |   |   |   |   Precoursor Absolute Error >= 2.28 : Z (87/4)
 * |   |   |   peptide2 unique matched non lossy coverage >= 0.04
 * |   |   |   |   peptide2 matched conservative < 2.5 : Z (191/94)
 * |   |   |   |   peptide2 matched conservative >= 2.5 : B (151/90)
 * |   |   combinedDelta >= 4.58
 * |   |   |   peptide2 unique matched non lossy < 3.5
 * |   |   |   |   peptide2 unique matched conservative < 1.5 : C (231/81)
 * |   |   |   |   peptide2 unique matched conservative >= 1.5 : C (154/42)
 * |   |   |   peptide2 unique matched non lossy >= 3.5
 * |   |   |   |   peptide2 unique matched conservative < 4.5 : B (137/75)
 * |   |   |   |   peptide2 unique matched conservative >= 4.5 : A (447/105)
 *
 * === Stratified cross-validation ===
 * === Summary ===
 *
 * Correctly Classified Instances       21508               96.548  %
 * Incorrectly Classified Instances       769                3.452  %
 * Kappa statistic                          0.7067
 * Mean absolute error                      0.0225
 * Root mean squared error                  0.1082
 * Relative absolute error                 37.4476 %
 * Root relative squared error             62.4581 %
 * Total Number of Instances            22277
 *
 * === Detailed Accuracy By Class ===
 *
 * TP Rate   FP Rate   Precision   Recall  F-Measure   ROC Area  Class
 *   0.545     0.011      0.566     0.545     0.556      0.976    C
 *   0.295     0.006      0.448     0.295     0.356      0.965    B
 *   0.81      0.008      0.681     0.81      0.74       0.993    A
 *   0.992     0.16       0.989     0.992     0.991      0.991    Z
 *
 * === Confusion Matrix ===
 *
 *      a     b     c     d   <-- classified as
 *    320    52    24   191 |     a = C
 *     74   103   145    27 |     b = B
 *     28    54   372     5 |     c = A
 *    143    21     5 20713 |     d = Z
 *
 * This is not a perfect recovery - but at least a lot better then nothing.
 * For class A we can expect 5/(24+145+372)=0.0092421442 < 1% decoys.
 * For class B 21/(52+103+54)=0.1004784689 = 10% decoys.
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */

public class RandomTreeModeledManual extends AbstractScoreSpectraMatch {

    private final static double CLASS_A = 1;
    private final static double CLASS_B = 0.75;
    private final static double CLASS_C = 0.25;
    private final static double CLASS_Z = 0;
    private final        String m_scorename = getClass().getSimpleName();

    @Override
    public double score(MatchedXlinkedPeptide match) {
        double score = 0;
        if (match.getPeptide(0) == match.getPeptide(1))  {
            score = scoreHomo(match);
        } else {
            score = scoreNormal(match);
        }
//        if (match.getScore("MgxScore") < 59.71) {
//            score = CLASS_Z;
//        } else {
//            if (match.getScore("fragment unique matched non lossy coverage") < 0.3) {
//                if (match.getScore("mgcShiftedDelta") < 95.47) {
//                    if (match.getScore("peptide2 unique matched conservative") < 2.5) {
//                        score = 0;
//                    } else {
//                        if (match.getScore("spectra intensity nonlossy coverage") < 0.43) {
//                            score = CLASS_Z;
//                        } else {
//                            score = CLASS_B;
//                        }
//                    }
//                } else {
//                    if (match.getScore("peptide2 non lossy matched") < 2.5) {
//                        score = CLASS_Z;
//
//                    } else {
//                        if (match.getScore("fragment coverage") < 0.29) {
//                            score = CLASS_B;
//
//                        } else {
//                            score = CLASS_A;
//                        }
//                    }
//                }
//            } else {
//                if (match.getScore("combinedDelta") < 4.58) {
//                    if (match.getScore("peptide2 unique matched non lossy coverage") < 0.04) {
//                        score = CLASS_Z;
//
//                    } else {
//                        if (match.getScore("peptide2 matched conservative") < 2.5) {
//                            score = CLASS_Z;
//
//                        } else {
//                            score = CLASS_B;
//                        }
//                    }
//                } else {
//                    if (match.getScore("peptide2 unique matched non lossy") < 3.5) {
//                        score = CLASS_C;
//                    } else {
//                        if (match.getScore("peptide2 unique matched conservative") < 4.5) {
//                            score = CLASS_B;
//                        } else {
//                            score = CLASS_A;
//                        }
//                    }
//                }
//
//            }
//        }
        super.addScore(match, m_scorename, score);
        return score;
    }
    
    protected double scoreNormal(MatchedXlinkedPeptide match) {
        if (match.getScore("MgxScore") < 59.71) {
            return CLASS_Z;
        } else {
            if (match.getScore("fragment unique matched non lossy coverage") < 0.3) {
                if (match.getScore("mgcShiftedDelta") < 95.47) {
                    if (match.getScore("peptide2 unique matched conservative") < 2.5) {
                        return 0;
                    } else {
                        if (match.getScore("spectra intensity nonlossy coverage") < 0.43) {
                            return CLASS_Z;
                        } else {
                            return CLASS_B;
                        }
                    }
                } else {
                    if (match.getScore("peptide2 non lossy matched") < 2.5) {
                        return CLASS_Z;

                    } else {
                        if (match.getScore("fragment coverage") < 0.29) {
                            return CLASS_B;

                        } else {
                            return CLASS_A;
                        }
                    }
                }
            } else {
                if (match.getScore("combinedDelta") < 4.58) {
                    if (match.getScore("peptide2 unique matched non lossy coverage") < 0.04) {
                        return CLASS_Z;

                    } else {
                        if (match.getScore("peptide2 matched conservative") < 2.5) {
                            return CLASS_Z;

                        } else {
                            return CLASS_B;
                        }
                    }
                } else {
                    if (match.getScore("peptide2 unique matched non lossy") < 3.5) {
                        return CLASS_C;
                    } else {
                        if (match.getScore("peptide2 unique matched conservative") < 4.5) {
                            return CLASS_B;
                        } else {
                            return CLASS_A;
                        }
                    }
                }

            }
        }
        
    }

    protected double scoreHomo(MatchedXlinkedPeptide match) {
        if (match.getScore("MgxScore") < 59.71) {
            return CLASS_Z;
        } else {
            if (match.getScore("fragment unique matched non lossy coverage") < 0.3) {
                if (match.getScore("mgcShiftedDelta") < 95.47) {
                    if (match.getScore("peptide1 unique matched conservative") < 6.5) {
                        return 0;
                    } else {
                        if (match.getScore("spectra intensity nonlossy coverage") < 0.43) {
                            return CLASS_Z;
                        } else {
                            return CLASS_B;
                        }
                    }
                } else {
                    if (match.getScore("peptide1 non lossy matched") < 6.5) {
                        return CLASS_Z;

                    } else {
                        if (match.getScore("fragment coverage") < 0.29) {
                            return CLASS_B;

                        } else {
                            return CLASS_A;
                        }
                    }
                }
            } else {
                if (match.getScore("combinedDelta") < 4.58) {
                    if (match.getScore("peptide1 unique matched non lossy coverage") < 0.2) {
                        return CLASS_Z;

                    } else {
                        if (match.getScore("peptide2 matched conservative") < 6.5) {
                            return CLASS_Z;

                        } else {
                            return CLASS_B;
                        }
                    }
                } else {
                    if (match.getScore("peptide1 unique matched non lossy") < 7.5) {
                        return CLASS_C;
                    } else {
                        if (match.getScore("peptide1 unique matched conservative") < 9.5) {
                            return CLASS_B;
                        } else {
                            return CLASS_A;
                        }
                    }
                }

            }
        }
        
    }
    
    @Override
    public double getOrder() {
        Normalizer n = new Normalizer();
        return n.getOrder() - 1;
    }

}
