cat src/main/java/rappsilber/utils/XiVersion.java | grep -E "^\s*\".*+\s*$" | sed -r 's/^\s*"//' | sed -r 's/.n"\s*\+\s*$//g' > Changelog.md
