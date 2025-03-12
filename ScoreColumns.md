Xisearch calculates a series of subscores for each CSM and combines (most of) these into the final score. The following table gives an overview of most of these 

Explanations for most scores:

Score | Description | Source | Search-Library Dependent | Peptide1 | Peptide2 | Both Peptides |   | total | Non-Lossy/primary | Lossy | Conservative | Absolute | Relative
-- | -- | -- | -- | -- | -- | -- | -- | -- | -- | -- | -- | -- | --
Fragments
matched | number of fragments matched Each fragment is only counted ones - no independent of how often it is seen | FragmentCoverage |   | x | x | x |   | x | x | x | x | x | x
unique matched | same as "matched" but if a peak has more than one fragment matched to it, only one explanation is counted |   | x | x | x |   | x | x | x | x | x | x
multi matched | how many of the possible fragments where observed in more than one charge state |   | x | x | x |   |   |   |   | x |   | x
  |   |   |   |   |   |   |   |   |   |   |   |   |  
fragment CCPepStubCount | how many peptides are seen with crosslinker stub (0,1 or 2) |   |   |   |   | x |   |   |   |   |   |   |  
fragment CCPepDoubletCount | how many peptides are seen with crosslinker stub pairs (0,1 or 2) |   |   |   |   | x |   |   |   |   |   |   |  
sequencetag coverage | How much of the peptide is explained by consecutive fragments (minumum 3 consecutive fragments make a tag) | SequenceTagCoverage |   | x | x | x |   |   |   |   |   |   | x
MaxCharge | Maximum charge of any fragment matched | FragmentChargeState |   |   |   | x |   |   |   |   |   | x | x
MedianCharge | Median Charge any fragment matched |   |   |   | x |   |   |   |   |   | x | x
AverageCharge | Average charge any fragment matched |   |   |   | x |   |   |   |   |   | x | x
Spectrum
spectrum peaks explained | how many of the peaks in the spectra are explained by the current match | SpectraCoverage |   |   |   |   |   |   |   |   |   |   | x
specrum intensity explained | how much of the intensity is explained by the current match | SpectraCoverage/SpectraCoverageConservative |   |   |   |   |   |   | x |   | x |   | x
spectrum Top10 matched % | SpectraCoverage |   |   |   |   |   |   |   |   |   |   | x
spectrum Top10 matched % |   |   |   |   |   |   |   |   |   |   | x
spectra top20 matched% |   |   |   |   |   |   |   |   |   |   | x
spectra top40 matched% |   |   |   |   |   |   |   |   |   |   | x
spectra matched single% | how much of the intensity all peaks without isotope cluster are explained |   |   |   |   |   |   |   |   |   |   | x
spectra matched isotop% | how much of the intensity all peaks with isotope cluster are explained |   |   |   |   |   |   |   |   |   |   | x
spectra isotop% | how much of the spectrum intensity is contained in isotop clusters |   |   |   |   |   |   |   |   |   |   | x
Errors
Average MS2 error | Error |   |   |   |   |   |   |   |   |   | x | x
Average1-RelativeMS2Error | Avaerage of 1-(MS2Error/MaximalPermitedError) |   |   |   |   |   |   |   |   |   |   | x
Precoursor Error |   |   |   |   |   |   |   |   |   | x | x
1-ErrorRelative | 1-Precursor Error |   |   |   |   |   |   |   |   |   |   | x
SearchLibraryScore
mgcAlpha | Candidate Score for the alpha peptide     -loge(Π(peptides matching peak/number off all fragments)) | no group | x | x |   |   |   |   |   |   |   |   |  
mgcBeta | Candidate Score for the alpha peptide (not used for candidate selecteion)   |  | x | x |   |   |   |   |   |   |   |   |  
mgxScore | candidate score for the peptide pair   |  | x |   | x |   |   |   |   |   |   |   |  
mgxRank | the ranking in the list of mgx-candidates   |  | x |   |   | x |   |   |   |   |   |   |  
mgcScore | mgcAlpha+mgcBeta   |  | x |   |   | x |   |   |   |   |   |   |  
mgcDelta | difference to the second best mgc score   |  | x |   |   |   |   |   |   |   |   |   |  
FragmentLibraryScore | similare to mgxscore but on whole spectrum instead of a denoised version | FragmentLibraryScore | x |   |   |   |   |   |   |   |   |   |  
FragmentLibraryScoreExponential | scaleing of FragmentLibraryScore | FragmentLibraryScore | x |   |   |   |   |   |   |   |   |   |  
FragmentLibraryScoreLog | scaleing of FragmentLibraryScore | FragmentLibraryScore | x |   |   |   |   |   |   |   |   |   |  
Other
SpectrumQualityScore | combines several spectrum related scores to provide some inkling of how good the spectrum is matched and how good the spectrum itself is | CombineScore |   |   |   |   |   |   |   |   |   |   |  
betaCount | for the given alpha peptide how many beta peptides would match the gap-mass | NoGroup |   |   |   |   |   |   |   |   |   |   |  
betaCountInverse | 1/betaCount |   |   |   |   |   |   |   |   |   |   |  
Autovalidation | Passes autovalidation |   |   |   |   |   |   |   |   |   |   |  
LinkSiteDelta | how much better is the linksite then any other site (0 means its ambigious) |   |   |   |   |   |   |   |   |   |   |  
match score | final score  |   | x |   |   |   |   |   |   |   |   |   |  
delta | diferrence to the secon best final score |   |   |   |   |   |   |   |   |   |   |  
 |  |   |   |   |   |   |   |   |   |   |   |  
MatchScore | to be ignored
J48ModeledManual001 | to be ignored
AllScore | to be ignored
MeanSquareRootError | to be ignored
MeanSquareError | to be ignored
Modified | to be ignored
Containing | to be ignored
Crosslinked | to be ignored
BS3ReporterIonScore | to be ignored


columns:
* Source : where it is calculated (not really of interest here)
* Search-Library Dependent : these scores depend on the fasta and modification settings. i.e. if you search the same data with a different  e.g. a larger or smaller database even a match between the same peptides and the same spectrum will have somewhat different  values.
* Peptide1 : score exists as a version for peptide 1
* Peptide2 : score exists as a version for peptide 2
* Both Peptides: score exists as a version combining peptide 1 and 2
* total: every ion is counted - independent whether it is a neutral loss or not. E.g. a peak matched with y5+loss of water  would count as y5 seen.
* Non-Lossy/primary: only basic ions (a,b,c,x,y,z) are counted - e.g. a peak matched to y5 counts as y5 seen, but y5+loss of water  would not count
* Lossy: coutns only how many fragments are seen as neutral loss. e.g. y5 does not count but y5+loss of water does
* Conservative: every basic ion counts but also if a basic ion is not seen however three or more distinct versions with losses are seen count. E.g. if y5 is matched it counts but also y6 with 1 loss of water + y6 with 2 loss water and y6 with 3 loss of water seen would count as y6 seen.
* Absolute: absolute values taken (think of errors +5 ppm error and +5 ppm error are considered the same.
* relative: also taken as fraction of theoretical maximum. e.g. an error of 2ppm when searched with a maximum erro of 10 ppm would be 0.2 or seen 4 fragments matched for a 9 aminoacid long peptide would be 4/((9-1)*2)=0.25
