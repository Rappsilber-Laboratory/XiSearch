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
 * weka.classifiers.trees.J48 -C 0.001 -M 10
 *
 *
 * the resulting tree was:
 *
 * Classifier Model
 * J48 pruned tree
 * ------------------
 *
 * mgcBeta <= 14.673606
 * |   fragment unique matched conservative <= 8: Z (19809.0/23.0)
 * |   fragment unique matched conservative > 8
 * |   |   peptide2 unique matched non lossy <= 3
 * |   |   |   mgxDelta <= 7.430109: Z (669.76/81.53)
 * |   |   |   mgxDelta > 7.430109
 * |   |   |   |   Precoursor Error <= -0.383511: Z (103.37/1.0)
 * |   |   |   |   Precoursor Error > -0.383511
 * |   |   |   |   |   PrecoursorAbsoluteErrorRelative <= 0.551086
 * |   |   |   |   |   |   LengthPeptide2 <= 2: Z (19.0)
 * |   |   |   |   |   |   LengthPeptide2 > 2
 * |   |   |   |   |   |   |   fragment coverage <= 0.22807: Z (19.47/3.0)
 * |   |   |   |   |   |   |   fragment coverage > 0.22807: C (417.9/87.42)
 * |   |   |   |   |   PrecoursorAbsoluteErrorRelative > 0.551086: Z (44.42)
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
 * |   |   |   fragment unique matched non lossy coverage <= 0.338983
 * |   |   |   |   peptide2 non lossy matched <= 9: B (133.0/47.0)
 * |   |   |   |   peptide2 non lossy matched > 9: A (12.0/4.0)
 * |   |   |   fragment unique matched non lossy coverage > 0.338983
 * |   |   |   |   spectrum intensity coverage <= 0.696249
 * |   |   |   |   |   spectra intensity nonlossy coverage <= 0.332046: B (25.0/6.0)
 * |   |   |   |   |   spectra intensity nonlossy coverage > 0.332046
 * |   |   |   |   |   |   fragment lossy matched <= 7: B (39.0/13.0)
 * |   |   |   |   |   |   fragment lossy matched > 7: A (69.0/24.0)
 * |   |   |   |   spectrum intensity coverage > 0.696249: A (397.0/83.0)
 *
 * Number of Leaves  : 	20
 *
 * Size of the tree : 	39
 *
 *
 * Time taken to build model: 14.18 seconds
 *
 * === Stratified cross-validation ===
 * === Summary ===
 *
 * Correctly Classified Instances       21668               97.2662 %
 * Incorrectly Classified Instances       609                2.7338 %
 * Kappa statistic                          0.7664
 * Mean absolute error                      0.0198
 * Root mean squared error                  0.1037
 * Relative absolute error                 33.0588 %
 * Root relative squared error             59.9095 %
 * Total Number of Instances            22277
 *
 * === Detailed Accuracy By Class ===
 *
 *                TP Rate   FP Rate   Precision   Recall  F-Measure   ROC Area  Class
 *                  0.702     0.008      0.705     0.702     0.704      0.961    C
 *                  0.341     0.005      0.506     0.341     0.408      0.951    B
 *                  0.776     0.007      0.705     0.776     0.739      0.992    A
 *                  0.995     0.123      0.992     0.995     0.993      0.984    Z
 * Weighted Avg.    0.973     0.116      0.971     0.973     0.971      0.983
 *
 * === Confusion Matrix ===
 *
 *      a     b     c     d   <-- classified as
 *    412    30    17   128 |     a = C
 *     68   119   128    34 |     b = B
 *     19    74   356    10 |     c = A
 *     85    12     4 20781 |     d = Z
 *
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */

public class J48ModeledManual002 extends AbstractScoreSpectraMatch {

    private final static double CLASS_A = 1;
    private final static double CLASS_B = 0.75;
    private final static double CLASS_C = 0;
    private final static double CLASS_Z = 0;
    private final        String m_scorename = getClass().getSimpleName();

    @Override
    public double score(MatchedXlinkedPeptide match) {
        double score = CLASS_Z;


        if (match.getScore("mgcBeta") <= 16) {
            if (match.getScore("fragment unique matched conservative") <= 8) {
                score = CLASS_Z;
            } else {
                if (match.getScore("peptide2 unique matched non lossy") <= 3) {
                    if (match.getScore("mgxDelta") <= 7.430109) {
                        score = CLASS_Z;
                    } else {
                        if (match.getScore("Precoursor Error") <= -0.383511) {
                            score = CLASS_Z;
                        } else {
                            if (match.getScore("PrecoursorAbsoluteErrorRelative") <= 0.551086) {
                                if (match.getScore("LengthPeptide2") <= 2) {
                                    score = CLASS_Z;
                                } else {
                                    if (match.getScore("fragment coverage") <= 0.24807) {
                                        score = CLASS_Z;
                                    } else {
                                        score = CLASS_C;

                                    }

                                }
                            } else {
                                score = CLASS_Z;

                            }
                        }
                    }
                } else {
                    if (match.getScore("fragment conservative coverage") <= 0.23875) {
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
                if (match.getScore("peptide2 unique matched conservative") <= 4) {
                    if (match.getScore("mgxDelta") <= 5) {
                        score = CLASS_Z;
                    } else {
                        if (match.getScore("peptide2 unique matched non lossy") <= 4) {
                            if (match.getScore("peptide2 non lossy matched") <= 4) {
                                score = CLASS_C;
                            } else {
                                score = CLASS_B;

                            }
                        } else {
                            score = CLASS_B;

                        }
                    }
                } else {
                    if (match.getScore("fragment unique matched non lossy coverage") <= 0.338983) {
                        if (match.getScore("peptide2 non lossy matched") <= 9) {
                            score = CLASS_B;
                        } else {
                            score = CLASS_A;

                        }
                    } else {
                        if (match.getScore("spectrum intensity coverage") <= 0.696249) {
                            if (match.getScore("spectra intensity nonlossy coverage") <= 0.332046) {
                                score = CLASS_B;
                            } else {
                                if (match.getScore("fragment lossy matched") <= 7) {
                                    score = CLASS_B;
                                } else {
                                    score = CLASS_A;

                                }
                            }
                        } else {
                            score = CLASS_A;

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
