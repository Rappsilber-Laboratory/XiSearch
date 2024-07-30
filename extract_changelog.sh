cat src/main/java/rappsilber/utils/XiVersion.java | grep -E "^\s*\".*+\s*$" | sed -r 's/^\s*"//' | sed -r 's/.n"\s*\+\s*$//g' | sed -r 's/^Version/\nVersion/'> Changelog.md
