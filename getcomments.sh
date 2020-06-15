echo all line :
find -iname "*.java" -exec cat \{\} \; | wc -l
echo -n comments :
find -iname "*.java" -exec cat \{\} \; | awk '{if ($0 ~ /^.*\/\*.*/ && $0 !~ /^.*\/\*.*\*\/.*/ && $0 !~ /^\s*\/\/.*/) { bo=1;} else if (bo==1 && $0 ~ /^.*\*\/.*/) {bo=0; cl=1;}  if (bo==1 || cl==1 || $0 ~ /^\s*\/\*.*$/ || $0 ~/^\s*\/\/.*/) {if (bo==1 || cl==1 || $0 !~ /.*\*\/.*[a-zA-Z0-9]+.*/) print bo " : " $0;} cl=0;}'  | wc -l

echo -n empty lines : 
find -iname "*.java" -exec cat \{\} \; | grep -E "^\s*$" | wc -l
