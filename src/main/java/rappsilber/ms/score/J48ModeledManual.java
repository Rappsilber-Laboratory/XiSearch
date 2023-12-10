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
 * This class is the result of a weka J48 tree run on PolII data.
 * Input where the scores for validated scans, that where categorised into
 * confidence level A, B and C.
 * The random Tree where build with the following settings:
 *
 * weka.classifiers.trees.J48 -C 0.005 -M 10
 *
 *
 * the resulting tree was:
 * mgcBeta <= 14.673606
 * |   fragment unique matched conservative <= 8: Z (19809.0/23.0)
 * |   fragment unique matched conservative > 8
 * |   |   peptide2 unique matched non lossy <= 3
 * |   |   |   mgxDelta <= 7.430109: Z (669.76/81.53)
 * |   |   |   mgxDelta > 7.430109
 * |   |   |   |   Precursor Error <= -0.383511: Z (103.37/1.0)
 * |   |   |   |   Precursor Error > -0.383511
 * |   |   |   |   |   PrecursorAbsoluteErrorRelative <= 0.551086
 * |   |   |   |   |   |   ProteinLink2 <= 13: Z (30.47)
 * |   |   |   |   |   |   ProteinLink2 > 13
 * |   |   |   |   |   |   |   ProteinLink2 <= 1300
 * |   |   |   |   |   |   |   |   peptide1 conservative coverage <= 0.265625: Z (16.47/4.0)
 * |   |   |   |   |   |   |   |   peptide1 conservative coverage > 0.265625: C (394.95/64.47)
 * |   |   |   |   |   |   |   ProteinLink2 > 1300: Z (14.47)
 * |   |   |   |   |   PrecursorAbsoluteErrorRelative > 0.551086: Z (44.42)
 * |   |   peptide2 unique matched non lossy > 3
 * |   |   |   fragment conservative coverage <= 0.21875: Z (33.08/3.0)
 * |   |   |   fragment conservative coverage > 0.21875: B (72.0/34.0)
 * mgcBeta > 14.673606
 * |   fragment non lossy matched <= 8: Z (177.0/8.0)
 * |   fragment non lossy matched > 8
 * |   |   peptide2 unique matched conservative <= 3
 * |   |   |   mgxDelta <= 4.474409: Z (34.14/6.0)
 * |   |   |   mgxDelta > 4.474409
 * |   |   |   |   peptide2 unique matched non lossy <= 3
 * |   |   |   |   |   peptide2 non lossy matched <= 3: C (151.86/54.86)
 * |   |   |   |   |   peptide2 non lossy matched > 3: B (25.0/12.0)
 * |   |   |   |   peptide2 unique matched non lossy > 3: B (26.0/5.0)
 * |   |   peptide2 unique matched conservative > 3
 * |   |   |   peptide2 non lossy matched <= 4
 * |   |   |   |   peptide1 unique matched conservative coverage <= 0.35: B (24.0/3.0)
 * |   |   |   |   peptide1 unique matched conservative coverage > 0.35
 * |   |   |   |   |   mgcBeta <= 21.784019
 * |   |   |   |   |   |   spectra matched single% <= 0.340505: B (10.0/4.0)
 * |   |   |   |   |   |   spectra matched single% > 0.340505: C (23.0/8.0)
 * |   |   |   |   |   mgcBeta > 21.784019
 * |   |   |   |   |   |   peptide2 unique matched lossy <= 0: A (12.0/3.0)
 * |   |   |   |   |   |   peptide2 unique matched lossy > 0: B (25.0/8.0)
 * |   |   |   peptide2 non lossy matched > 4
 * |   |   |   |   fragment unique matched non lossy coverage <= 0.338983
 * |   |   |   |   |   peptide2 non lossy matched <= 9
 * |   |   |   |   |   |   spectra top100 matched% <= 0.64: B (77.0/24.0)
 * |   |   |   |   |   |   spectra top100 matched% > 0.64
 * |   |   |   |   |   |   |   spectra matched single% <= 0.455562: B (14.0/5.0)
 * |   |   |   |   |   |   |   spectra matched single% > 0.455562: A (11.0/1.0)
 * |   |   |   |   |   peptide2 non lossy matched > 9: A (12.0/4.0)
 * |   |   |   |   fragment unique matched non lossy coverage > 0.338983
 * |   |   |   |   |   spectrum intensity coverage <= 0.696249
 * |   |   |   |   |   |   spectra intensity nonlossy coverage <= 0.332046: B (21.0/4.0)
 * |   |   |   |   |   |   spectra intensity nonlossy coverage > 0.332046
 * |   |   |   |   |   |   |   fragment unique matched lossy coverage <= 0.477273
 * |   |   |   |   |   |   |   |   spectra matched single% <= 0.417135
 * |   |   |   |   |   |   |   |   |   fragment lossy matched <= 7: B (28.0/6.0)
 * |   |   |   |   |   |   |   |   |   fragment lossy matched > 7
 * |   |   |   |   |   |   |   |   |   |   peptide1 unique matched non lossy <= 11: A (19.0/4.0)
 * |   |   |   |   |   |   |   |   |   |   peptide1 unique matched non lossy > 11: B (17.0/6.0)
 * |   |   |   |   |   |   |   |   spectra matched single% > 0.417135: A (19.0/3.0)
 * |   |   |   |   |   |   |   fragment unique matched lossy coverage > 0.477273: A (13.0)
 * |   |   |   |   |   spectrum intensity coverage > 0.696249: A (350.0/53.0)
 *
 * === Stratified cross-validation ===
 * === Summary ===

 * Correctly Classified Instances       21694               97.383  %
 * Incorrectly Classified Instances       583                2.617  %
 * Kappa statistic                          0.776
 * Mean absolute error                      0.0187
 * Root mean squared error                  0.1015
 * Relative absolute error                 31.1421 %
 * Root relative squared error             58.5889 %
 * Total Number of Instances            22277

 * === Detailed Accuracy By Class ===

 *                TP Rate   FP Rate   Precision   Recall  F-Measure   ROC Area  Class
 *                  0.702     0.007      0.72      0.702     0.711      0.959    C
 *                  0.384     0.006      0.517     0.384     0.441      0.964    B
 *                  0.769     0.006      0.722     0.769     0.745      0.99     A
 *                  0.996     0.116      0.992     0.996     0.994      0.984    Z
 * Weighted Avg.    0.974     0.109      0.972     0.974     0.973      0.983

 * === Confusion Matrix ===

 *      a     b     c     d   <-- classified as
 *    412    36    15   124 |     a = C
 *     70   134   117    28 |     b = B
 *     20    76   353    10 |     c = A
 *     70    13     4 20795 |     d = Z
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */

public class J48ModeledManual extends AbstractScoreSpectraMatch {

    private final static double CLASS_A = 1;
    private final static double CLASS_B = 0.75;
    private final static double CLASS_C = 0;
    private final static double CLASS_Z = 0;
    private final        String m_scorename = getClass().getSimpleName();

    @Override
    public double score(MatchedXlinkedPeptide match) {
        double score = CLASS_Z;

        if (match.getScore("mgcBeta") <= 14.673606) {
            if (match.getScore("fragment unique matched conservative") <= 8) {
                score = CLASS_Z;

            } else {
                if (match.getScore("peptide2 unique matched non lossy") <= 3) {
                    if (match.getScore("mgxDelta") <= 7.430109) {
                        score = CLASS_Z;

                    } else {
                        if (match.getScore("Precursor Error") <= -0.383511) {
                            score = CLASS_Z;
                        } else {
                            if (match.getScore("PrecursorAbsoluteErrorRelative") <= 0.551086) {
                                if (match.getScore("ProteinLink2") <= 13) {
                                    score = CLASS_Z;

                                } else {
                                    if (match.getScore("ProteinLink2") <= 1300) {
                                        if (match.getScore("peptide1 conservative coverage") <= 0.265625) {
                                            score = CLASS_Z;

                                        } else {
                                            score = CLASS_C;
                                        }
                                    } else {
                                        score = CLASS_Z;
                                    }
                                }
                            } else {
                                score = CLASS_Z;
                            }
                        }
                    }
                } else {
                    if (match.getScore("fragment conservative coverage") <= 0.21875) {
                        score = CLASS_Z;
                    } else {
                        score = CLASS_B;
                    }
                }
            }
        } else {
            if (match.getScore("fragment non lossy matched") <= 8) {
                score = CLASS_Z;
            } else {
                if (match.getScore("peptide2 unique matched conservative") <= 3) {
                    if (match.getScore("mgxDelta") <= 4.474409) {
                        score = CLASS_Z;

                    } else {
                        if (match.getScore("peptide2 unique matched non lossy") <= 3) {
                            if (match.getScore("peptide2 non lossy matched") <= 3) {
                                score = CLASS_C;

                            } else {
                                score = CLASS_B;
                            }
                        } else {
                            score = CLASS_B;
                        }
                    }
                } else {
                    if (match.getScore("peptide2 non lossy matched") <= 4) {
                        if (match.getScore("peptide1 unique matched conservative coverage") <= 0.35) {
                            score = CLASS_B;

                        } else {
                            if (match.getScore("mgcBeta") <= 21.784019) {
                                if (match.getScore("spectra matched single%") <= 0.340505) {
                                    score = CLASS_B;

                                } else {
                                    score = CLASS_C;
                                }
                            } else {
                                if (match.getScore("peptide2 unique matched lossy") <= 0) {
                                    score = CLASS_A;

                                } else {
                                    score = CLASS_B;
                                }
                            }
                        }
                    } else {
                        if (match.getScore("fragment unique matched non lossy coverage") <= 0.338983) {
                            if (match.getScore("peptide2 non lossy matched") <= 9) {
                                if (match.getScore("spectra top100 matched%") <= 0.64) {
                                    score = CLASS_B;
                                } else {
                                    if (match.getScore("spectra matched single%") <= 0.455562) {
                                        score = CLASS_B;
                                    } else {
                                        score = CLASS_A;
                                    }
                                }
                            } else {
                                score = CLASS_A;
                            }
                        } else {
                            if (match.getScore("spectrum intensity coverage") <= 0.696249) {
                                if (match.getScore("spectra intensity nonlossy coverage") <= 0.332046) {
                                    score = CLASS_B;
                                } else {
                                    if (match.getScore("fragment unique matched lossy coverage") <= 0.477273) {
                                        if (match.getScore("spectra matched single%") <= 0.417135) {
                                            if (match.getScore("fragment lossy matched") <= 7) {
                                                score = CLASS_B;
                                            } else {
                                                if (match.getScore("peptide1 unique matched non lossy") <= 11) {
                                                    score = CLASS_A;
                                                } else {
                                                    score = CLASS_B;
                                                }
                                            }
                                        } else {
                                            score = CLASS_A;
                                        }
                                    } else {
                                        score = CLASS_A;
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
        super.addScore(match, m_scorename, score);
        return score;
    }

    @Override
    public double getOrder() {
        Normalizer n = new Normalizer();
        return n.getOrder() - 1;
    }

}
