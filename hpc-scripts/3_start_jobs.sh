for FOLDER in ./searches/*; do
    SEARCHNAME=${FOLDER#"./searches/"}
    SH="start_search_$SEARCHNAME.sh"
    cd ./searches/$SEARCHNAME
    sbatch $SH &
    cd ../../
    sleep 1
done