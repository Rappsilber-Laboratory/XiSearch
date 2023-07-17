COUNTER=0
mkdir -p "searches"
for FILENAME in ./peakfiles/recal*.mgf; do
    SHORT=${FILENAME#"./peakfiles/"}
    SHORT=${SHORT%".mgf"}
    mkdir "searches/$SHORT"
    SH_FILENAME="start_search_$SHORT.sh"
    SH_FILEPATH="searches/$SHORT/$SH_FILENAME"
    cp "1_search_template.sh" $SH_FILEPATH
    sed -i "s/PEAKFILEXYZ/$SHORT\.mgf/g" $SH_FILEPATH #"start_search_$SHORT.sh"
    sed -i "s/OUTPUTNAME/$SHORT/g" $SH_FILEPATH
    sed -i "s/COUNT/${COUNTER}/g" $SH_FILEPATH
    COUNTER=$[COUNTER + 1]
done