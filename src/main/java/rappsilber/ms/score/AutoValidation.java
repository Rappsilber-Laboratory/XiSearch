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

import java.util.ArrayList;
import rappsilber.ms.spectra.match.MatchedXlinkedPeptide;
import rappsilber.utils.Util;

/**
 * Basically I run weka on some training data and created randomtree and reptree 
 * classifies and if for both 7 out of ten agree that it is not a false positive 
 * a match gets flagged as auto-validated. 
 * @author Lutz Fischer <l.fischer@ed.ac.uk>
 */
public class AutoValidation extends AbstractScoreSpectraMatch {
    
    abstract class tree {

        public abstract int isFalsePositive(MatchedXlinkedPeptide match);
    }
    
    public final static String scorename = "Autovalidation";
    private ArrayList<tree> randomTrees = new ArrayList<tree>(10);
    private ArrayList<tree>  repTrees  = new ArrayList<tree>(10);

    public AutoValidation() {

        randomTrees.add(new tree() {
            public int isFalsePositive(MatchedXlinkedPeptide match) {
                if (match.getScore("fragment unique matched conservative") < 13.5) {
                    if (match.getScore("peptide2 sequencetag coverage%") < 0.17) {
                        if (match.getSpectrum().getPrecurserMass() < 2751.37) {
                            return 1;
                        } else {
                            if (match.getScore("fragment non lossy matched") < 15.5) {
                                    return 1;
                            } else {
                                if (match.getScore("mgcAlpha") < 85.17) {
                                    if ((match.getCalcMass()/match.getSpectrum().getPrecurserCharge())+Util.PROTON_MASS < 750.16) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        }
                    } else {
                        if (match.getScore("fragment matched conservative") < 10.5) {
                            if (match.getScore("spectra top100 matched%") < 0.25) {
                                return 1;
                            } else {
                                return 0;
                            }
                        } else {
                            if (match.getScore("mgcDelta") < 24.07) {
                                if (match.getScore("spectrum intensity coverage") < 0.32) {
                                    return 1;
                                } else {
                                    if (match.getScore("fragment sequencetag coverage%") < 0.33) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                if (match.getScore("AverageRelativeMS2Error") < 0.22) {
                                    if (match.getScore("SpectraCoverageConservative") < 0.22) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        }
                    }
                } else {
                    if (match.getScore("peptide2 non lossy matched") < 4.5) {
                        if (match.getScore("peptide2 unique matched non lossy") < 3.5) {
                            if (match.getSpectrum().getPrecurserMass() < 4846.54) {
                                if (match.getScore("peptide2 unique matched") < 5.5) {
                                    return 1;
                                } else {
                                    if (match.getScore("spectra matched single%") < 0.12) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                }
                            } else {
                                return 1;
                            }
                        } else {
                            if (match.getScore("peptide2 conservative coverage") < 0.27) {
                                if (match.getScore("spectra intensity nonlossy coverage") < 0.51) {
                                    return 1;
                                } else {
                                    if (match.getScore("1-ErrorRelative") < 0.81) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                if (match.getScore("spectrum intensity coverage") < 0.38) {
                                    return 1;
                                } else {
                                    if (match.getScore("1-ErrorRelative") < 0.81) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        }
                    } else {
                        if (match.getScore("peptide2 matched conservative") < 6.5) {
                            if (match.getScore("spectrum quality score") < 0.46) {
                                if (match.getScore("1-ErrorRelative") < 0.85) {
                                    return 1;
                                } else {
                                    if (match.getScore("fragment sequencetag coverage%") < 0.31) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                return 0;
                            }
                        } else {
                            if (match.getScore("spectra intensity nonlossy coverage") < 0.27) {
                                if (match.getScore("Pep2Score") < 0.35) {
                                    if (match.getScore("mgcShiftedDelta") < 116.51) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            } else {
                                return 0;
                            }
                        }
                    }
                }
            }
            
        });

        randomTrees.add(new tree() {
            public int isFalsePositive(MatchedXlinkedPeptide match) {
                if (match.getScore("fragment non lossy matched") < 13.5) {
                    if (match.getScore("fragment unique matched non lossy coverage") < 0.45) {
                        if (match.getScore("spectra top40 matched%") < 0.34) {
                            if (match.getScore("spectra top100 matched%") < 0.25) {
                                if (match.getScore("mgcBeta") < 30.63) {
                                    return 1;
                                } else {
                                    if (match.getScore("MeanSquareError") < 10.46) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                }
                            } else {
                                if (match.getScore("peptide2 conservative coverage") < 0.31) {
                                    return 1;
                                } else {
                                    if (match.getScore("1-ErrorRelative") < 0.91) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        } else {
                            if (match.getScore("fragment sequencetag coverage%") < 0.28) {
                                if ((match.getCalcMass()/match.getSpectrum().getPrecurserCharge())+Util.PROTON_MASS < 901.07) {
                                    return 1;
                                } else {
                                    if (match.getSpectrum().getPrecurserMass() < 2979.12) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                }
                            } else {
                                return 0;
                            }
                        }
                    } else {
                        if (match.getCalcMass() < 2373.72) {
                            if (match.getScore("peptide2 sequencetag coverage%") < 0.23) {
                                if (match.getScore("FragmentLibraryScoreExponential") < 1) {
                                    return 1;
                                } else {
                                    if (match.getScore("mgxScore") < 192.46) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                if (match.getScore("FragmentLibraryScoreLog") < 18.45) {
                                    if (match.getScore("spectrum quality score") < 0.38) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        } else {
                            if (match.getCalcMass() < 2603.28) {
                                if (match.getSpectrum().getPrecurserMass() < 2586.79) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            } else {
                                return 0;
                            }
                        }
                    }
                } else {
                    if (match.getScore("peptide2 unique matched non lossy coverage") < 0.34) {
                        if (match.getScore("peptide2 non lossy matched") < 5.5) {
                            if (match.getScore("FragmentLibraryScoreLog") < 56.32) {
                                return 1;
                            } else {
                                if (match.getScore("peptide2 unique matched conservative coverage") < 0.17) {
                                    if (match.getScore("spectrum peaks coverage") < 0.57) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    if (match.getScore("peptide2 matched conservative") < 3.5) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        } else {
                            if (match.getScore("spectrum intensity coverage") < 0.42) {
                                if (match.getScore("1-ErrorRelative") < 0.86) {
                                    return 1;
                                } else {
                                    if (match.getScore("mgcDelta") < 32.36) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                return 0;
                            }
                        }
                    } else {
                        if (match.getScore("peptide2 non lossy matched") < 4.5) {
                            if (match.getScore("peptide2 conservative coverage") < 0.39) {
                                if (match.getSpectrum().getPrecurserMass() < 4126.15) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            } else {
                                return 1;
                            }
                        } else {
                            if (match.getScore("mgxDelta") < 0.68) {
                                if (match.getScore("spectrum quality score") < 0.47) {
                                    if (match.getScore("MeanSquareError") < 33.82) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                } else {
                                    return 0;
                                }
                            } else {
                                return 0;
                            }
                        }
                    }
                }
            }
        });

        randomTrees.add(new tree() {
            public int isFalsePositive(MatchedXlinkedPeptide match) {
                if (match.getScore("total fragment matches") < 15.5) {
                    if (match.getScore("fragment matched conservative") < 10.5) {
                        if (match.getSpectrum().getPrecurserMass() < 2458.24) {
                            if (match.getScore("fragment multimatched%") < 0.07) {
                                if (match.getSpectrum().getPrecurserMass() < 1974.07) {
                                    return 1;
                                } else {
                                    if (match.getCalcMass() < 1974.55) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                }
                            } else {
                                return 1;
                            }
                        } else {
                            if (match.getCalcMass() < 2793.46) {
                                if ((match.getCalcMass()/match.getSpectrum().getPrecurserCharge())+Util.PROTON_MASS < 493.18) {
                                    if ((match.getCalcMass()/match.getSpectrum().getPrecurserCharge())+Util.PROTON_MASS < 478.89) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    if (match.getCalcMass() < 2458.43) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                }
                            } else {
                                if (match.getSpectrum().getPrecurserMass() < 2793.99) {
                                    return 0;
                                } else {
                                    return 1;
                                }
                            }
                        }
                    } else {
                        if (match.getScore("mgxDelta") < 6.26) {
                            if (match.getScore("peptide1 unique matched conservative coverage") < 0.56) {
                                if (match.getScore("fragment multimatched%") < 0.11) {
                                    return 1;
                                } else {
                                    if (match.getScore("mgcBeta") < 30.71) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                if (match.getScore("peptide2 non lossy coverage") < 0.39) {
                                    if (match.getSpectrum().getPrecurserMass() < 2749.02) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    if (match.getScore("fragment sequencetag coverage%") < 0.48) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        } else {
                            if (match.getScore("peptide2 unique matched non lossy coverage") < 0.35) {
                                return 1;
                            } else {
                                if (match.getScore("mgcBeta") < 9.5) {
                                    return 1;
                                } else {
                                    if (match.getScore("SpectraCoverageConservative") < 0.32) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (match.getScore("spectrum quality score") < 0.48) {
                        if (match.getScore("peptide2 unique matched conservative") < 4.5) {
                            if (match.getScore("peptide1 coverage") < 0.53) {
                                if (match.getScore("peptide2 matched") < 11.5) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            } else {
                                if (match.getScore("peptide2 unique matched lossy") < 9.5) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        } else {
                            if (match.getScore("peptide1 unique matched non lossy coverage") < 0.36) {
                                if (match.getScore("peptide2 non lossy coverage") < 0.37) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            } else {
                                if (match.getScore("fragment sequencetag coverage%") < 0.29) {
                                    if (match.getScore("FragmentLibraryScoreLog") < 56.03) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        }
                    } else {
                        if (match.getScore("peptide2 non lossy matched") < 4.5) {
                            if (match.getScore("1-ErrorRelative") < 0.82) {
                                return 1;
                            } else {
                                if (match.getScore("peptide2 unique matched non lossy") < 3.5) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        } else {
                            return 0;
                        }
                    }
                }
            }
        });

        randomTrees.add(new tree() {
            public int isFalsePositive(MatchedXlinkedPeptide match) {
                if (match.getScore("peptide2 matched conservative") < 4.5) {
                    if (match.getSpectrum().getPrecurserMass() < 2362.17) {
                        if (match.getScore("fragment non lossy coverage") < 0.46) {
                            if (match.getScore("spectrum intensity coverage") < 0.57) {
                                return 1;
                            } else {
                                if (match.getScore("peptide2 unique matched non lossy") < 3.5) {
                                    if (match.getCalcMass() < 2285.17) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        } else {
                            if (match.getScore("mgcDelta") < 8.55) {
                                if ((match.getCalcMass()/match.getSpectrum().getPrecurserCharge())+Util.PROTON_MASS < 407.32) {
                                    return 1;
                                } else {
                                    if (match.getScore("spectra matched single%") < 0.49) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                if (match.getScore("mgcDelta") < 34.57) {
                                    return 1;
                                } else {
                                    if (match.getScore("spectra top40 matched%") < 0.19) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        }
                    } else {
                        if (match.getSpectrum().getPrecurserMass() < 2793.51) {
                            if (match.getScore("fragment conservative coverage") < 0.45) {
                                if (match.getScore("mgxScore") < 126.46) {
                                    return 1;
                                } else {
                                    if (match.getScore("FragmentLibraryScoreLog") < 53.96) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                if (match.getScore("mgxDelta") < 8.17) {
                                    return 1;
                                } else {
                                    if (match.getScore("AverageMS2Error") < 3.45) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                }
                            }
                        } else {
                            if (match.getScore("total fragment matches") < 19.5) {
                                if (match.getScore("FragmentLibraryScoreLog") < 49.69) {
                                    return 1;
                                } else {
                                    if (match.getScore("peptide1 lossy coverage") < 0.33) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                return 1;
                            }
                        }
                    }
                } else {
                    if (match.getScore("FragmentLibraryScore") < 1) {
                        if (match.getScore("fragment unique matched non lossy coverage") < 0.41) {
                            if (match.getScore("peptide2 unique matched") < 0.5) {
                                return 0;
                            } else {
                                if (match.getScore("spectrum intensity coverage") < 0.4) {
                                    return 1;
                                } else {
                                    if (match.getScore("peptide1 lossy matched") < 5.5) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                }
                            }
                        } else {
                            if (match.getScore("mgxScore") < 71.71) {
                                if (match.getScore("fragment unique matched non lossy") < 13.5) {
                                    if (match.getCalcMass() < 2350.25) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            } else {
                                if (match.getScore("MeanSquareError") < 35.27) {
                                    return 0;
                                } else {
                                    if (match.getScore("spectrum peaks coverage") < 0.26) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        }
                    } else {
                        if (match.getScore("Pep2Score") < 0.3) {
                            if (match.getScore("mgcShiftedDelta") < 128.74) {
                                if (match.getScore("fragment non lossy matched") < 18.5) {
                                    return 1;
                                } else {
                                    if (match.getScore("mgcBeta") < 47.37) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                if (match.getScore("peptide2 sequencetag coverage%") < 0.11) {
                                    if (match.getScore("mgxDelta") < 3.86) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        } else {
                            return 0;
                        }
                    }
                }
            }
        });

        randomTrees.add(new tree() {
            public int isFalsePositive(MatchedXlinkedPeptide match) {
                if (match.getScore("peptide2 sequencetag coverage%") < 0.1) {
                    if (match.getScore("peptide1 matched") < 11.5) {
                        if (match.getScore("spectrum quality score") < 0.44) {
                            if (match.getScore("fragment conservative coverage") < 0.44) {
                                if (match.getScore("fragment lossy matched") < 12.5) {
                                    return 1;
                                } else {
                                    if (match.getScore("MeanSquareError") < 29.73) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                }
                            } else {
                                if (match.getSpectrum().getPrecurserMass() < 2603.28) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        } else {
                            if (match.getCalcMass() < 2316.66) {
                                if (match.getScore("fragment unique matched conservative coverage") < 0.43) {
                                    if (match.getScore("peptide2 lossy matched") < 8) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 1;
                                }
                            } else {
                                if (match.getScore("fragment conservative coverage") < 0.45) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        }
                    } else {
                        if (match.getScore("mgxScore") < 177.6) {
                            if (match.getScore("peptide2 non lossy matched") < 3.5) {
                                return 1;
                            } else {
                                if (match.getScore("betaCount") < 74.5) {
                                    return 1;
                                } else {
                                    if (match.getScore("fragment matched conservative") < 26.5) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        } else {
                            if (match.getScore("fragment non lossy matched") < 22.5) {
                                if (match.getScore("mgxRank") < 0.5) {
                                    if (match.getScore("peptide2 lossy matched") < 5.5) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 1;
                                }
                            } else {
                                if (match.getScore("peptide2 non lossy matched") < 3.5) {
                                    return 1;
                                } else {
                                    if (match.getScore("MeanSquareRootError") < 5.79) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (match.getScore("spectrum quality score") < 0.41) {
                        if (match.getScore("fragment matched conservative") < 13.5) {
                            if (match.getScore("fragment unique matched conservative") < 9.5) {
                                return 1;
                            } else {
                                if (match.getScore("fragment unique matched lossy coverage") < 0.28) {
                                    if ((match.getCalcMass()/match.getSpectrum().getPrecurserCharge())+Util.PROTON_MASS < 468.31) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                } else {
                                    if (match.getScore("mgcShiftedDelta") < 102.87) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        } else {
                            if (match.getScore("peptide1 coverage") < 0.41) {
                                if (match.getScore("peptide2 non lossy matched") < 6.5) {
                                    return 1;
                                } else {
                                    if (match.getScore("AverageMS2Error") < 4.73) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                }
                            } else {
                                if (match.getScore("SpectraCoverageConservative") < 0.19) {
                                    if (match.getScore("mgxScore") < 144.16) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    if (match.getScore("mgcAlpha") < 97.03) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                }
                            }
                        }
                    } else {
                        if (match.getScore("peptide2 unique matched") < 4.5) {
                            if (match.getScore("peptide2 non lossy matched") < 4.5) {
                                if (match.getScore("peptide2 unique matched conservative") < 2.5) {
                                    return 1;
                                } else {
                                    if (match.getScore("peptide1 non lossy coverage") < 0.38) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                if (match.getScore("peptide2 unique matched lossy coverage") < 0.41) {
                                    if (match.getScore("mgxDelta") < 0.43) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        } else {
                            if (match.getScore("peptide2 unique matched non lossy") < 4.5) {
                                if (match.getScore("peptide2 lossy coverage") < 0.21) {
                                    if (match.getScore("total fragment matches") < 27) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    if (match.getSpectrum().getPrecurserMass() < 2689.94) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                return 0;
                            }
                        }
                    }
                }
            }
        });

        randomTrees.add(new tree() {
            public int isFalsePositive(MatchedXlinkedPeptide match) {
                if (match.getScore("fragment matched conservative") < 13.5) {
                    if (match.getScore("mgcDelta") < 22.24) {
                        if ((match.getCalcMass()/match.getSpectrum().getPrecurserCharge())+Util.PROTON_MASS < 699.37) {
                            return 1;
                        } else {
                            if (match.getScore("Pep2Score") < 0.43) {
                                if (match.getScore("PrecoursorCharge") < 3.5) {
                                    return 1;
                                } else {
                                    if (match.getCalcMass() < 2793.6) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                }
                            } else {
                                if (match.getScore("peptide2 non lossy matched") < 5.5) {
                                    if ((match.getCalcMass()/match.getSpectrum().getPrecurserCharge())+Util.PROTON_MASS < 699.55) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                } else {
                                    if (match.getScore("peptide2 conservative coverage") < 0.39) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                }
                            }
                        }
                    } else {
                        if (match.getScore("fragment non lossy coverage") < 0.45) {
                            if (match.getScore("fragment conservative coverage") < 0.38) {
                                return 1;
                            } else {
                                if (match.getScore("1-ErrorRelative") < 0.92) {
                                    return 1;
                                } else {
                                    if (match.getScore("peptide1 matched conservative") < 4.5) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        } else {
                            if (match.getScore("spectrum intensity coverage") < 0.24) {
                                if ((match.getCalcMass()/match.getSpectrum().getPrecurserCharge())+Util.PROTON_MASS < 763.85) {
                                    if (match.getScore("PrecoursorCharge") < 4.5) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            } else {
                                return 0;
                            }
                        }
                    }
                } else {
                    if (match.getScore("peptide2 unique matched non lossy") < 4.5) {
                        if (match.getScore("fragment coverage") < 0.42) {
                            if (match.getSpectrum().getPrecurserMass() < 2919.44) {
                                if (match.getScore("peptide1 lossy coverage") < 0.08) {
                                    if (match.getScore("peptide1 unique matched conservative") < 12.5) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    if (match.getCalcMass() < 2336.28) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                }
                            } else {
                                if (match.getScore("SpectraCoverageConservative") < 0.59) {
                                    return 1;
                                } else {
                                    if (match.getScore("SpectraCoverageConservative") < 0.59) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                }
                            }
                        } else {
                            if (match.getScore("peptide2 unique matched conservative") < 2.5) {
                                if (match.getScore("PrecoursorCharge") < 4.5) {
                                    return 1;
                                } else {
                                    if (match.getScore("spectra matched single%") < 0.48) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                if (match.getScore("peptide2 sequencetag coverage%") < 0.2) {
                                    return 1;
                                } else {
                                    if (match.getScore("mgxDelta") < 7.07) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        }
                    } else {
                        if (match.getScore("peptide2 unique matched non lossy coverage") < 0.28) {
                            if (match.getScore("peptide2 unique matched non lossy") < 6.5) {
                                if (match.getScore("mgcDelta") < 32.58) {
                                    return 1;
                                } else {
                                    if (match.getScore("spectra top100 matched%") < 0.27) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                if (match.getScore("spectra matched isotop%") < 0.51) {
                                    if (match.getScore("peptide2 unique matched non lossy coverage") < 0.26) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        } else {
                            if (match.getScore("FragmentLibraryScore") < 1) {
                                if (match.getScore("spectrum intensity coverage") < 0.43) {
                                    if (match.getScore("fragment unique matched non lossy coverage") < 0.44) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            } else {
                                return 0;
                            }
                        }
                    }
                }
            }
        });

        randomTrees.add(new tree() {
            public int isFalsePositive(MatchedXlinkedPeptide match) {
                if (match.getScore("mgcBeta") < 29.95) {
                    if (match.getScore("spectrum quality score") < 0.45) {
                        if (match.getScore("fragment conservative coverage") < 0.43) {
                            if (match.getScore("mgxScore") < 180.22) {
                                return 1;
                            } else {
                                if (match.getScore("peptide1 multimatched%") < 0.24) {
                                    if (match.getScore("peptide1 conservative coverage") < 0.47) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 1;
                                }
                            }
                        } else {
                            if (match.getScore("peptide2 unique matched conservative") < 3.5) {
                                if ((match.getCalcMass()/match.getSpectrum().getPrecurserCharge())+Util.PROTON_MASS < 698.63) {
                                    return 1;
                                } else {
                                    if ((match.getCalcMass()/match.getSpectrum().getPrecurserCharge())+Util.PROTON_MASS < 932.1) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                if (match.getScore("fragment sequencetag coverage%") < 0.23) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        }
                    } else {
                        if (match.getScore("peptide2 unique matched non lossy") < 4.5) {
                            if (match.getScore("lossy fragment matches") < 13.5) {
                                if (match.getScore("fragment unique matched conservative") < 18.5) {
                                    return 1;
                                } else {
                                    if (match.getScore("peptide2 sequencetag coverage%") < 0.28) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                if (match.getScore("peptide2 conservative coverage") < 0.39) {
                                    if (match.getScore("spectrum quality score") < 0.81) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 1;
                                }
                            }
                        } else {
                            if (match.getScore("fragment sequencetag coverage%") < 0.23) {
                                return 1;
                            } else {
                                if (match.getScore("1-ErrorRelative") < 0.82) {
                                    if (match.getScore("fragment unique matched non lossy coverage") < 0.38) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        }
                    }
                } else {
                    if (match.getScore("peptide2 unique matched non lossy coverage") < 0.31) {
                        if (match.getScore("fragment unique matched conservative") < 14.5) {
                            if (match.getScore("Pep1Score") < 0.45) {
                                if (match.getScore("PrecoursorCharge") < 3.5) {
                                    if ((match.getCalcMass()/match.getSpectrum().getPrecurserCharge())+Util.PROTON_MASS < 1129.58) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 1;
                                }
                            } else {
                                if (match.getScore("spectrum quality score") < 0.6) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        } else {
                            if (match.getScore("peptide2 unique matched conservative") < 4.5) {
                                if (match.getScore("spectrum quality score") < 0.48) {
                                    return 1;
                                } else {
                                    if (match.getScore("MeanSquareRootError") < 4.3) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                }
                            } else {
                                if (match.getScore("mgcDelta") < 37) {
                                    if (match.getScore("spectrum quality score") < 0.45) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        }
                    } else {
                        if (match.getScore("spectrum quality score") < 0.4) {
                            if (match.getScore("spectrum peaks coverage") < 0.22) {
                                if (match.getScore("fragment unique matched lossy coverage") < 0.4) {
                                    if (match.getScore("fragment unique matched conservative coverage") < 0.43) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 1;
                                }
                            } else {
                                if (match.getScore("MeanSquareError") < 52.95) {
                                    if (match.getScore("peptide1 unique matched conservative") < 13.5) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        } else {
                            if (match.getScore("peptide2 matched") < 4.5) {
                                if (match.getScore("mgxDelta") < 9.82) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            } else {
                                return 0;
                            }
                        }
                    }
                }
            }
        });

        randomTrees.add(new tree() {
            public int isFalsePositive(MatchedXlinkedPeptide match) {
                if (match.getScore("spectra top100 matched%") < 0.23) {
                    if (match.getScore("total fragment matches") < 15.5) {
                        if (match.getScore("fragment non lossy matched") < 10.5) {
                            if ((match.getCalcMass()/match.getSpectrum().getPrecurserCharge())+Util.PROTON_MASS < 684.09) {
                                return 1;
                            } else {
                                if (match.getSpectrum().getPrecurserMass() < 2782.47) {
                                    return 1;
                                } else {
                                    if (match.getSpectrum().getPrecurserMass() < 2793.61) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                }
                            }
                        } else {
                            if (match.getScore("peptide2 conservative coverage") < 0.4) {
                                if (match.getScore("peptide2 sequencetag coverage%") < 0.22) {
                                    return 1;
                                } else {
                                    if (match.getScore("mgcBeta") < 49.62) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                if (match.getScore("mgxDelta") < 5) {
                                    return 1;
                                } else {
                                    if (match.getScore("spectra matched isotop%") < 0.43) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        }
                    } else {
                        if (match.getScore("fragment unique matched non lossy coverage") < 0.44) {
                            if (match.getScore("mgcShiftedDelta") < 118.93) {
                                if (match.getScore("peptide2 matched") < 11.5) {
                                    if (match.getScore("1-ErrorRelative") < 0.99) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    if (match.getScore("1-ErrorRelative") < 0.88) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                if (match.getScore("mgcBeta") < 37.2) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        } else {
                            if (match.getScore("peptide2 unique matched non lossy") < 4.5) {
                                if (match.getScore("peptide2 conservative coverage") < 0.39) {
                                    if (match.getScore("spectra matched single%") < 0.39) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 1;
                                }
                            } else {
                                if (match.getScore("SpectraCoverageConservative") < 0.4) {
                                    if (match.getScore("1-ErrorRelative") < 0.82) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        }
                    }
                } else {
                    if (match.getScore("peptide2 unique matched non lossy coverage") < 0.26) {
                        if (match.getScore("Pep2Score") < 0.23) {
                            if (match.getScore("total fragment matches") < 18.5) {
                                return 1;
                            } else {
                                if (match.getScore("peptide1 unique matched lossy coverage") < 0.05) {
                                    if (match.getScore("mgcBeta") < 43.94) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 1;
                                }
                            }
                        } else {
                            if (match.getScore("mgcBeta") < 49.15) {
                                return 1;
                            } else {
                                return 0;
                            }
                        }
                    } else {
                        if (match.getScore("spectra intensity nonlossy coverage") < 0.34) {
                            if (match.getScore("fragment matched conservative") < 12.5) {
                                if (match.getScore("peptide2 non lossy matched") < 4.5) {
                                    return 1;
                                } else {
                                    if (match.getSpectrum().getPrecurserMass() < 2166.13) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                }
                            } else {
                                if (match.getScore("mgcDelta") < 27.75) {
                                    if (match.getScore("peptide2 matched") < 4.5) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        } else {
                            if (match.getScore("mgxDelta") < 7.98) {
                                if (match.getScore("fragment non lossy matched") < 19.5) {
                                    if (match.getScore("peptide2 lossy matched") < 3.5) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    if (match.getScore("peptide2 matched conservative") < 4.5) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                return 0;
                            }
                        }
                    }
                }
            }
        });

        randomTrees.add(new tree() {
            public int isFalsePositive(MatchedXlinkedPeptide match) {
                if (match.getScore("total fragment matches") < 15.5) {
                    if (match.getScore("peptide2 conservative coverage") < 0.39) {
                        if (match.getCalcMass() < 2390.24) {
                            if (match.getScore("mgxDelta") < 8.51) {
                                if (match.getSpectrum().getPrecurserMass() < 1901.89) {
                                    return 1;
                                } else {
                                    if (match.getSpectrum().getPrecurserMass() < 1902.01) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                }
                            } else {
                                if (match.getScore("spectrum peaks coverage") < 0.21) {
                                    return 1;
                                } else {
                                    if (match.getScore("peptide2 unique matched conservative") < 2.5) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        } else {
                            if (match.getScore("fragment non lossy coverage") < 0.37) {
                                return 1;
                            } else {
                                if (match.getCalcMass() < 3168.68) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        }
                    } else {
                        if (match.getScore("total fragment matches") < 12.5) {
                            if (match.getScore("spectra matched single%") < 0.36) {
                                if (match.getScore("PrecoursorCharge") < 3.5) {
                                    if (match.getScore("peptide1 sequencetag coverage%") < 0.47) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 1;
                                }
                            } else {
                                if (match.getScore("peptide1 conservative coverage") < 0.41) {
                                    return 1;
                                } else {
                                    if (match.getScore("FragmentLibraryScore") < 1) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        } else {
                            if (match.getScore("mgxDelta") < 5.97) {
                                return 1;
                            } else {
                                if (match.getScore("spectrum peaks coverage") < 0.22) {
                                    if (match.getScore("peptide1 unique matched") < 7.5) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        }
                    }
                } else {
                    if (match.getScore("peptide2 unique matched conservative") < 4.5) {
                        if (match.getScore("peptide2 matched") < 3.5) {
                            if (match.getScore("mgcBeta") < 31.41) {
                                if (match.getScore("PrecoursorCharge") < 5.5) {
                                    return 1;
                                } else {
                                    if (match.getSpectrum().getPrecurserMass() < 2752.44) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                }
                            } else {
                                if (match.getScore("mgcShiftedDelta") < 182.37) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        } else {
                            if (match.getScore("SpectraCoverageConservative") < 0.42) {
                                if (match.getScore("peptide2 unique matched conservative") < 0.5) {
                                    if (match.getScore("mgcBeta") < 28.09) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 1;
                                }
                            } else {
                                if (match.getScore("mgxDelta") < 7.14) {
                                    return 1;
                                } else {
                                    if (match.getScore("peptide2 conservative coverage") < 0.28) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        }
                    } else {
                        if (match.getScore("peptide2 conservative coverage") < 0.27) {
                            if (match.getScore("mgxDelta") < -0.02) {
                                if (match.getScore("peptide2 sequencetag coverage%") < 0.19) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            } else {
                                return 0;
                            }
                        } else {
                            return 0;
                        }
                    }
                }
            }
        });

        randomTrees.add(new tree() {
            public int isFalsePositive(MatchedXlinkedPeptide match) {
                if (match.getScore("fragment unique matched non lossy") < 13.5) {
                    if (match.getScore("fragment sequencetag coverage%") < 0.34) {
                        if (match.getSpectrum().getPrecurserMass() < 2458.24) {
                            if (match.getScore("fragment unique matched conservative") < 9.5) {
                                if (match.getScore("peptide2 matched") < 5.5) {
                                    return 1;
                                } else {
                                    if (match.getScore("SpectraCoverageConservative") < 0.51) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                if (match.getScore("peptide2 unique matched conservative") < 3.5) {
                                    return 1;
                                } else {
                                    if (match.getScore("fragment unique matched lossy") < 13.5) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        } else {
                            if (match.getScore("peptide2 unique matched non lossy") < 6.5) {
                                return 1;
                            } else {
                                if (match.getScore("spectra matched single%") < 0.38) {
                                    if (match.getScore("fragment conservative coverage") < 0.36) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        }
                    } else {
                        if (match.getScore("peptide2 non lossy coverage") < 0.34) {
                            if (match.getSpectrum().getPrecurserMass() < 2791) {
                                return 1;
                            } else {
                                return 0;
                            }
                        } else {
                            if (match.getScore("mgcShiftedDelta") < 67.88) {
                                if (match.getScore("peptide2 lossy matched") < 3.5) {
                                    if (match.getScore("PrecoursorCharge") < 4.5) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            } else {
                                if (match.getScore("peptide2 matched conservative") < 4.5) {
                                    if (match.getScore("fragment unique matched non lossy") < 12.5) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        }
                    }
                } else {
                    if (match.getScore("peptide2 matched conservative") < 4.5) {
                        if (match.getScore("peptide2 unique matched non lossy coverage") < 0.27) {
                            if (match.getScore("mgcDelta") < 26.14) {
                                if (match.getScore("mgcBeta") < 40.58) {
                                    if (match.getScore("spectra intensity nonlossy coverage") < 0.67) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            } else {
                                if (match.getScore("peptide1 matched conservative") < 15.5) {
                                    if (match.getScore("peptide2 sequencetag coverage%") < 0.21) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    if (match.getScore("spectra top40 matched%") < 0.24) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        } else {
                            if (match.getScore("peptide2 non lossy matched") < 3.5) {
                                return 1;
                            } else {
                                if (match.getScore("peptide2 non lossy coverage") < 0.45) {
                                    if (match.getScore("mgxDelta") < 7.76) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 1;
                                }
                            }
                        }
                    } else {
                        if (match.getScore("mgcDelta") < 29.93) {
                            if (match.getScore("fragment unique matched conservative coverage") < 0.37) {
                                if (match.getScore("1-ErrorRelative") < 0.86) {
                                    return 1;
                                } else {
                                    if (match.getScore("peptide2 sequencetag coverage%") < 0.22) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                if (match.getScore("mgcBeta") < 19.76) {
                                    if (match.getScore("fragment non lossy coverage") < 0.47) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        } else {
                            if (match.getScore("peptide2 unique matched non lossy coverage") < 0.18) {
                                if (match.getScore("fragment non lossy matched") < 21) {
                                    if ((match.getCalcMass()/match.getSpectrum().getPrecurserCharge())+Util.PROTON_MASS < 1037.68) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            } else {
                                return 0;
                            }
                        }
                    }
                }
            }
        });
        
        
        repTrees.add(new tree() {
            public int isFalsePositive(MatchedXlinkedPeptide match) {
                if (match.getScore("peptide2 sequencetag coverage%") < 0.09) {
                    if (match.getSpectrum().getPrecurserMass() < 2269.16) {
                        if (match.getSpectrum().getPrecurserMass() < 1974.06) {
                            if (match.getPeptide1().length() <= 5) {
                                return 1;
                            } else {
                                if (match.getScore("peptide2 non lossy matched") < 4.5) {
                                    return 1;
                                } else {
                                    if (match.getScore("spectrum quality score") < 0.51) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        } else {
                            if (match.getScore("peptide1 sequencetag coverage%") < 0.6) {
                                return 1;
                            } else {
                                if (match.getScore("peptide2 non lossy matched") < 3.5) {
                                    return 1;
                                } else {
                                    if (match.getScore("mgxDelta") < 4.8) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        }
                    } else {
                        if (match.getSpectrum().getPrecurserMass() < 2793.51) {
                            return 1;
                        } else {
                            if (match.getSpectrum().getPrecurserMass() < 2793.6) {
                                return 0;
                            } else {
                                return 1;
                            }
                        }
                    }
                } else {
                    if (match.getScore("spectrum quality score") < 0.41) {
                        if (match.getScore("fragment sequencetag coverage%") < 0.3) {
                            if (match.getSpectrum().getPrecurserMass() < 2360.73) {
                                return 1;
                            } else {
                                if (match.getScore("mgxDelta") < 23.58) {
                                    if (match.getScore("peptide2 non lossy matched") < 9.5) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        } else {
                            if (match.getScore("peptide2 non lossy matched") < 4.5) {
                                return 1;
                            } else {
                                if (match.getScore("1-ErrorRelative") < 0.85) {
                                    return 1;
                                } else {
                                    if (match.getScore("mgcScore") < 62.82) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        }
                    } else {
                        if (match.getScore("peptide2 non lossy matched") < 4.5) {
                            if (match.getScore("1-ErrorRelative") < 0.85) {
                                return 1;
                            } else {
                                if (match.getScore("mgxDelta") < 7.05) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        } else {
                            if (match.getScore("mgxDelta") < 0.68) {
                                if (match.getScore("1-ErrorRelative") < 0.78) {
                                    if (match.getScore("mgcDelta") < 41.08) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            } else {
                                return 0;
                            }
                        }
                    }
                }
            }
        });

        repTrees.add(new tree() {
            public int isFalsePositive(MatchedXlinkedPeptide match) {
                if (match.getScore("peptide2 sequencetag coverage%") < 0.11) {
                    if (match.getSpectrum().getPrecurserMass() < 2362.17) {
                        if (match.getSpectrum().getPrecurserMass() < 1898.09) {
                            return 1;
                        } else {
                            if (match.getScore("fragment matched conservative") < 13.5) {
                                if (match.getScore("mgcBeta") < 41.26) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            } else {
                                if (match.getScore("peptide2 matched conservative") < 4.5) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        }
                    } else {
                        if (match.getSpectrum().getPrecurserMass() < 2793.51) {
                            if (match.getSpectrum().getPrecurserMass() < 2362.64) {
                                return 0;
                            } else {
                                return 1;
                            }
                        } else {
                            if (match.getSpectrum().getPrecurserMass() < 2793.6) {
                                return 0;
                            } else {
                                if (match.getScore("total fragment matches") < 19.5) {
                                    return 1;
                                } else {
                                    if (match.getScore("mgcDelta") < 31.36) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (match.getScore("total fragment matches") < 15.5) {
                        if (match.getScore("fragment non lossy coverage") < 0.42) {
                            if (match.getScore("mgcBeta") < 54.66) {
                                if (match.getSpectrum().getPrecurserMass() < 2743.94) {
                                    if (match.getScore("spectra matched single%") < 0.41) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 1;
                                }
                            } else {
                                return 0;
                            }
                        } else {
                            if (match.getScore("spectra top100 matched%") < 0.21) {
                                if (match.getScore("peptide2 unique matched") < 6.5) {
                                    return 1;
                                } else {
                                    if (match.getScore("mgxDelta") < 3.57) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                if (match.getScore("mgxDelta") < 5.87) {
                                    if (match.getScore("peptide2 sequencetag coverage%") < 0.39) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        }
                    } else {
                        if (match.getScore("PrecoursorAbsoluteErrorRelative") < 0.24) {
                            if (match.getScore("peptide2 matched conservative") < 5.5) {
                                if (match.getScore("mgxDelta") < 6.96) {
                                    if (match.getScore("spectra top100 matched%") < 0.29) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            } else {
                                return 0;
                            }
                        } else {
                            if (match.getScore("mgcBeta") < 36.99) {
                                if (match.getScore("mgxDelta") < 0.5) {
                                    return 1;
                                } else {
                                    if (match.getScore("fragment non lossy coverage") < 0.47) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                return 0;
                            }
                        }
                    }
                }
            }
        });

        repTrees.add(new tree() {
            public int isFalsePositive(MatchedXlinkedPeptide match) {
                if (match.getScore("peptide2 unique matched conservative") < 4.5) {
                    if (match.getPeptide2().length() < 5.5) {
                        return 1;
                    } else  {
                        if (match.getScore("peptide2 sequencetag coverage%") < 0.3) {
                            if (match.getSpectrum().getPrecurserMass() < 2384.2) {
                                if (match.getScore("fragment non lossy coverage") < 0.47) {
                                    return 1;
                                } else {
                                    if (match.getScore("spectra top100 matched%") < 0.24) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else  {
                                if (match.getSpectrum().getPrecurserMass() < 4766.05) {
                                    return 1;
                                } else  {
                                    return 1;
                                }
                            }
                        } else {
                            if (match.getScore("spectrum intensity coverage") < 0.27) {
                                if (match.getPeptide2().length() < 7.5) {
                                    return 1;
                                } else  {
                                    return 0;
                                }
                            } else {
                                if (match.getScore("1-ErrorRelative") < 0.7) {
                                    return 1;
                                } else  {
                                    return 0;
                                }
                            }
                        }
                    }
                } else {
                    if (match.getScore("spectrum intensity coverage") < 0.38) {
                        if (match.getScore("fragment non lossy coverage") < 0.43) {
                            if (match.getScore("mgcBeta") < 48.27) {
                                if (match.getScore("mgxDelta") < 20.59) {
                                    if (match.getPeptide2().length() < 13.5) {
                                        return 1;
                                    } else  {
                                        return 1;
                                    }
                                } else {
                                    if (match.getScore("mgxScore") < 155.01) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                return 0;
                            }
                        } else {
                            if (match.getScore("1-ErrorRelative") < 0.76) {
                                if (match.getScore("mgxDelta") < 18.4) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            } else {
                                if (match.getScore("spectra matched single%") < 0.18) {
                                    if (match.getScore("mgcBeta") < 27.44) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        }
                    } else {
                        if (match.getScore("peptide2 sequencetag coverage%") < 0.11) {
                            if (match.getScore("1-ErrorRelative") < 0.78) {
                                return 1;
                            } else {
                                if (match.getScore("mgxDelta") < 4.51) {
                                    if (match.getScore("mgcBeta") < 50.44) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        } else {
                            if (match.getScore("1-ErrorRelative") < 0.72) {
                                if (match.getScore("mgxDelta") < -0.19) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            } else {
                                return 0;
                            }
                        }
                    }
                }
            }
        });

        repTrees.add(new tree() {
            public int isFalsePositive(MatchedXlinkedPeptide match) {
                if (match.getScore("peptide2 unique matched conservative") < 4.5) {
                    if (match.getSpectrum().getPrecurserMass() < 2381.32) {
                        if (match.getScore("fragment sequencetag coverage%") < 0.36) {
                            return 1;
                        } else {
                            if (match.getScore("peptide2 non lossy matched") < 3.5) {
                                return 1;
                            } else {
                                if (match.getScore("spectra matched isotop%") < 0.41) {
                                    return 1;
                                } else {
                                    if (match.getScore("peptide2 unique matched conservative coverage") < 0.45) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                }
                            }
                        }
                    } else {
                        if (match.getScore("peptide2 non lossy matched") < 4.5) {
                            if (match.getSpectrum().getPrecurserMass() < 2793.51) {
                                return 1;
                            } else {
                                if (match.getSpectrum().getPrecurserMass() < 2793.6) {
                                    return 0;
                                } else {
                                    if (match.getPeptide1().length() < 11.5) {
                                        return 1;
                                    } else  {
                                        return 1;
                                    }
                                }
                            }
                        } else {
                            if (match.getScore("fragment sequencetag coverage%") < 0.3) {
                                if (match.getScore("fragment coverage") < 0.56) {
                                    if (match.getScore("peptide2 non lossy matched") < 9) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            } else {
                                if (match.getScore("1-ErrorRelative") < 0.76) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        }
                    }
                } else {
                    if (match.getScore("fragment sequencetag coverage%") < 0.2) {
                        if (match.getScore("spectra top100 matched%") < 0.3) {
                            if (match.getPeptide1().length() < 9.5) {
                                return 1;
                            } else {
                                if (match.getScore("mgcScore") < 114.73) {
                                    return 1;
                                } else {
                                    if (match.getScore("peptide2 unique matched conservative coverage") < 0.32) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        } else {
                            if (match.getScore("1-ErrorRelative") < 0.83) {
                                return 1;
                            } else {
                                return 0;
                            }
                        }
                    } else {
                        if (match.getScore("1-ErrorRelative") < 0.73) {
                            if (match.getScore("spectra intensity nonlossy coverage") < 0.39) {
                                if (match.getScore("peptide2 unique matched conservative") < 7.5) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            } else {
                                return 0;
                            }
                        } else {
                            if (match.getScore("Pep2Score") < 0.34) {
                                if (match.getScore("spectra top100 matched%") < 0.27) {
                                    if (match.getScore("mgcScore") < 115.82) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            } else {
                                return 0;
                            }
                        }
                    }
                }
            }
        });

        repTrees.add(new tree() {
            public int isFalsePositive(MatchedXlinkedPeptide match) {
                if (match.getScore("peptide2 sequencetag coverage%") < 0.11) {
                    if (match.getSpectrum().getPrecurserMZ() < 601.34) {
                        if (match.getSpectrum().getPrecurserMZ() < 394.02) {
                            return 1;
                        } else {
                            if (match.getScore("peptide2 matched conservative") < 4.5) {
                                return 1;
                            } else {
                                if (match.getScore("fragment sequencetag coverage%") < 0.2) {
                                    return 1;
                                } else {
                                    if (match.getScore("mgcBeta") < 27.04) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        }
                    } else {
                        if (match.getScore("mgcBeta") < 47.92) {
                            if (match.getSpectrum().getPrecurserMZ() < 699.39) {
                                return 1;
                            } else {
                                if (match.getSpectrum().getPrecurserMZ() < 699.41) {
                                    return 0;
                                } else {
                                    return 1;
                                }
                            }
                        } else {
                            if (match.getScore("peptide2 matched conservative") < 5.5) {
                                if (match.getScore("peptide2 unique matched lossy coverage") < 0.21) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            } else {
                                return 0;
                            }
                        }
                    }
                } else {
                    if (match.getScore("spectrum quality score") < 0.43) {
                        if (match.getScore("mgxScore") < 115.88) {
                            if (match.getScore("fragment sequencetag coverage%") < 0.34) {
                                return 1;
                            } else {
                                if (match.getScore("mgcBeta") < 18.4) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        } else {
                            if (match.getScore("mgcBeta") < 29.87) {
                                if (match.getScore("fragment unique matched non lossy coverage") < 0.44) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            } else {
                                if (match.getScore("mgxDelta") < 9.78) {
                                    if (match.getScore("spectra intensity nonlossy coverage") < 0.17) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        }
                    } else {
                        if (match.getScore("peptide2 non lossy matched") < 4.5) {
                            if (match.getScore("mgxDelta") < 7.97) {
                                if (match.getScore("peptide2 non lossy matched") < 3.5) {
                                    return 1;
                                } else {
                                    if (match.getScore("spectra top100 matched%") < 0.28) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                return 0;
                            }
                        } else {
                            return 0;
                        }
                    }
                }
            }
        });

        repTrees.add(new tree() {
            public int isFalsePositive(MatchedXlinkedPeptide match) {
                if (match.getScore("peptide2 non lossy matched") < 4.5) {
                    if (match.getPeptide1().length() < 11.5) {
                        if (match.getPeptide1().length() < 7.5) {
                            return 1;
                        } else {
                            if (match.getScore("PrecoursorCharge") < 3.5) {
                                return 1;
                            } else {
                                if (match.getSpectrum().getPrecurserMZ() < 689.1) {
                                    return 1;
                                } else {
                                    if (match.getSpectrum().getPrecurserMZ() < 699.65) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                }
                            }
                        }
                    } else {
                        return 1;
                    }
                } else {
                    if (match.getScore("spectra intensity nonlossy coverage") < 0.3) {
                        if (match.getScore("fragment unique matched non lossy coverage") < 0.41) {
                            if (match.getScore("peptide2 non lossy matched") < 9.5) {
                                return 1;
                            } else {
                                return 0;
                            }
                        } else {
                            if (match.getScore("Precoursor Absolute Error") < 1.88) {
                                if (match.getScore("FragmentLibraryScoreExponential") < 0.98) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            } else {
                                if (match.getScore("peptide2 non lossy matched") < 8.5) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        }
                    } else {
                        if (match.getScore("Precoursor Absolute Error") < 1.08) {
                            if (match.getScore("peptide2 conservative coverage") < 0.23) {
                                if (match.getScore("spectra intensity nonlossy coverage") < 0.5) {
                                    if (match.getScore("Precoursor Error") < 0.5) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            } else {
                                return 0;
                            }
                        } else {
                            if (match.getScore("mgxRank") < 1.5) {
                                if (match.getScore("peptide2 coverage") < 0.42) {
                                    if (match.getScore("mgxScore") < 146.38) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            } else {
                                if (match.getScore("fragment unique matched lossy coverage") < 0.29) {
                                    return 1;
                                } else {
                                    if (match.getScore("peptide2 non lossy matched") < 6.5) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });

        repTrees.add(new tree() {
            public int isFalsePositive(MatchedXlinkedPeptide match) {
                if (match.getScore("peptide2 sequencetag coverage%") < 0.11) {
                    if (match.getPeptide2().length() < 6.5) {
                        if (match.getPeptide2().length() < 5.5) {
                            return 1;
                        } else {
                            if (match.getScore("peptide2 matched conservative") < 4.5) {
                                return 1;
                            } else {
                                if (match.getScore("mgxDelta") < 4.36) {
                                    if (match.getScore("spectra top100 matched%") < 0.28) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        }
                    } else {
                        if (match.getScore("PrecoursorCharge") < 3.5) {
                            return 1;
                        } else {
                            if (match.getSpectrum().getPrecurserMZ() < 580.76) {
                                return 1;
                            } else {
                                if (match.getPeptide2().length() < 14.5) {
                                    return 1;
                                } else {
                                    if (match.getScore("fragment lossy matched") < 22) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (match.getScore("SpectraCoverageConservative") < 0.32) {
                        if (match.getScore("fragment unique matched conservative coverage") < 0.42) {
                            if (match.getScore("mgxDelta") < 23.35) {
                                if (match.getScore("peptide2 lossy matched") < 10.5) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            } else {
                                if (match.getScore("fragment non lossy matched") < 10.5) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        } else {
                            if (match.getScore("peptide2 matched") < 5.5) {
                                if (match.getScore("mgxDelta") < 13.88) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            } else {
                                return 0;
                            }
                        }
                    } else {
                        if (match.getScore("peptide2 unique matched conservative") < 4.5) {
                            if (match.getScore("mgxDelta") < 8.23) {
                                if (match.getPeptide2().length() < 5.5) {
                                    return 1;
                                } else {
                                    if (match.getScore("peptide2 sequencetag coverage%") < 0.32) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                if (match.getScore("fragment unique matched non lossy coverage") < 0.36) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        } else {
                            if (match.getScore("mgxRank") < 0.5) {
                                if (match.getScore("FragmentLibraryScoreLog") < 15.99) {
                                    if (match.getScore("spectra top100 matched%") < 0.21) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            } else {
                                return 0;
                            }
                        }
                    }
                }
            }
        });

        repTrees.add(new tree() {
            public int isFalsePositive(MatchedXlinkedPeptide match) {
                if (match.getScore("peptide2 sequencetag coverage%") < 0.09) {
                    if (match.getSpectrum().getPrecurserMass() < 2269.16) {
                        if (match.getSpectrum().getPrecurserMass() < 1611.36) {
                            return 1;
                        } else {
                            if (match.getScore("SpectraCoverageConservative") < 0.48) {
                                return 1;
                            } else {
                                if (match.getScore("peptide2 unique matched non lossy") < 4.5) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        }
                    } else {
                        if (match.getSpectrum().getPrecurserMass() < 2793.51) {
                            return 1;
                        } else {
                            if (match.getSpectrum().getPrecurserMass() < 2793.61) {
                                return 0;
                            } else {
                                return 1;
                            }
                        }
                    }
                } else {
                    if (match.getScore("SpectraCoverageConservative") < 0.32) {
                        if (match.getScore("SpectraCoverageConservative") < 0.25) {
                            return 1;
                        } else {
                            if (match.getScore("peptide2 unique matched non lossy") < 6.5) {
                                if (match.getScore("fragment sequencetag coverage%") < 0.3) {
                                    return 1;
                                } else {
                                    if (match.getScore("peptide2 matched") < 5.5) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                if (match.getScore("1-ErrorRelative") < 0.84) {
                                    if (match.getScore("spectra top100 matched%") < 0.18) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        }
                    } else {
                        if (match.getScore("peptide2 unique matched non lossy") < 4.5) {
                            if (match.getScore("1-ErrorRelative") < 0.82) {
                                if (match.getScore("FragmentLibraryScoreLog") < 58.46) {
                                    return 1;
                                } else {
                                    if (match.getScore("peptide1 coverage") < 0.52) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            } else {
                                if (match.getScore("spectra top100 matched%") < 0.3) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        } else {
                            if (match.getScore("1-ErrorRelative") < 0.74) {
                                if (match.getScore("peptide2 sequencetag coverage%") < 0.34) {
                                    if (match.getScore("mgcDelta") < 23.87) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            } else {
                                return 0;
                            }
                        }
                    }
                }
            }
        });

        repTrees.add(new tree() {
            public int isFalsePositive(MatchedXlinkedPeptide match) {
                if (match.getScore("peptide2 unique matched conservative") < 4.5) {
                    if (match.getSpectrum().getPrecurserMass() < 2283.27) {
                        if (match.getScore("spectrum quality score") < 0.45) {
                            return 1;
                        } else {
                            if (match.getScore("peptide2 matched conservative") < 3.5) {
                                if (match.getScore("spectra matched isotop%") < 0.38) {
                                    return 0;
                                } else {
                                    return 1;
                                }
                            } else {
                                if (match.getScore("1-ErrorRelative") < 0.73) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        }
                    } else {
                        if (match.getScore("fragment matched conservative") < 16.5) {
                            if (match.getSpectrum().getPrecurserMass() < 2793.51) {
                                if (match.getSpectrum().getPrecurserMZ() < 509.38) {
                                    if (match.getSpectrum().getPrecurserMZ() < 508.87) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 1;
                                }
                            } else {
                                if (match.getSpectrum().getPrecurserMass() < 2793.6) {
                                    return 0;
                                } else {
                                    return 1;
                                }
                            }
                        } else {
                            if (match.getScore("peptide2 matched conservative") < 3.5) {
                                if (match.getPeptide1().length() < 19.5) {
                                    if (match.getPeptide1().length() < 11.5) {
                                        return 0;
                                    } else {
                                        return 1;
                                    }
                                } else {
                                    return 1;
                                }
                            } else {
                                if (match.getScore("1-ErrorRelative") < 0.73) {
                                    return 1;
                                } else {
                                    if (match.getScore("Pep2Score") < 0.31) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (match.getScore("spectrum quality score") < 0.42) {
                        if (match.getScore("fragment unique matched conservative coverage") < 0.41) {
                            if (match.getScore("peptide2 matched conservative") < 9.5) {
                                return 1;
                            } else {
                                return 0;
                            }
                        } else {
                            if (match.getScore("1-ErrorRelative") < 0.71) {
                                return 1;
                            } else {
                                return 0;
                            }
                        }
                    } else {
                        if (match.getScore("peptide2 unique matched non lossy coverage") < 0.26) {
                            if (match.getScore("spectrum peaks coverage") < 0.45) {
                                if (match.getScore("Precoursor Absolute Error") < 1.37) {
                                    return 0;
                                } else {
                                    return 1;
                                }
                            } else {
                                return 0;
                            }
                        } else {
                            if (match.getScore("Precoursor Absolute Error") < 1.68) {
                                return 0;
                            } else {
                                if (match.getScore("spectrum quality score") < 0.49) {
                                    if (match.getScore("peptide2 unique matched conservative") < 7.5) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            }
                        }
                    }
                }
            }
        });

        repTrees.add(new tree() {
            public int isFalsePositive(MatchedXlinkedPeptide match) {
                if (match.getScore("peptide2 sequencetag coverage%") < 0.09) {
                    if (match.getPeptide2().length() < 6.5) {
                        return 1;
                    } else {
                        if (match.getPeptide1().length() < 8.5) {
                            return 1;
                        } else {
                            if (match.getPeptide1().length() < 14.5) {
                                if (match.getSpectrum().getPrecurserMZ() < 702.36) {
                                    if (match.getSpectrum().getPrecurserMZ() < 699.37) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 1;
                                }
                            } else {
                                return 1;
                            }
                        }
                    }
                } else {
                    if (match.getScore("spectrum quality score") < 0.43) {
                        if (match.getScore("total fragment matches") < 14.5) {
                            return 1;
                        } else {
                            if (match.getScore("PrecoursorAbsoluteErrorRelative") < 0.17) {
                                if (match.getScore("mgxDelta") < 6.56) {
                                    if (match.getScore("peptide1 sequencetag coverage%") < 0.27) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            } else {
                                if (match.getScore("mgxDelta") < 22.37) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        }
                    } else {
                        if (match.getScore("peptide2 non lossy matched") < 4.5) {
                            if (match.getScore("PrecoursorAbsoluteErrorRelative") < 0.21) {
                                if (match.getPeptide2().length() < 5.5) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            } else {
                                return 1;
                            }
                        } else {
                            if (match.getScore("PrecoursorAbsoluteErrorRelative") < 0.31) {
                                return 0;
                            } else {
                                if (match.getScore("mgxDelta") < -0.19) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }
                        }
                    }
                }
            }
        });


    }

    @Override
    public String[] scoreNames() {
        return new String[] {scorename};
    }
    
    

    public double score(MatchedXlinkedPeptide match) {
        if (match.getPeptides().length != 2) {
            addScore(match, scorename, 0);
            return 0;
        }
        
        int FPRandom = 0;
        for (tree t :randomTrees) {
            FPRandom+= t.isFalsePositive(match);
        }
        int FPRep = 0;
        for (tree t :randomTrees) {
            FPRep+= t.isFalsePositive(match);
        }
        if (FPRep <=3 && FPRandom <=3) {
            addScore(match, "Autovalidation", 1);
            return 1;
        } 
        
        addScore(match, "Autovalidation", 0);
        return 0;    

    }

    public double getOrder() {
        return 100001;
    }

}
