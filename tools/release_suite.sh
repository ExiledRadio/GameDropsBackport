#!/usr/bin/env bash
# The release test run: the whole self-test on a fresh dev server, the asset and language checks, then the release jar.
#
#   cmd //c "start /affinity FFFF0000 /b /wait bash tools/release_suite.sh"
#
# FAMILY=1 runs the server with the other backports' dev jars in eclipse/mods (Village & Pillage, Update Aquatic,
# Caves & Cliffs, Wild Update, Trails & Tales, Tricky Trials; WORKSPACES points at their checkouts' parent) and logs
# crashes, registry clashes and duplicate-content warnings; the jars are removed again afterwards. NOBUILD=1 skips the
# release build. The world is deleted first. Results: build/release-suite.log.
cd "$(dirname "$0")/.." || exit 1
LOG=build/release-suite.log
WORKSPACES=${WORKSPACES:-/e/MincraftModding/Workspaces}
mkdir -p build eclipse/mods
: > "$LOG"
rm -rf eclipse/world eclipse/crash-reports
if [ -n "$FAMILY" ]; then
    for d in RLCraftVillagerTrading UpdateAquaticBackport CavesAndCliffsBackport WildUpdateBackport TrailsAndTalesBackport TrickyTrialsBackport; do
        jar=$(ls -t "$WORKSPACES/$d"/build/libs/*-dev.jar 2>/dev/null | head -n 1)
        [ -n "$jar" ] && cp "$jar" eclipse/mods/ && echo "with $(basename "$jar")" >> "$LOG"
    done
fi
./gradlew runServer -Pselftest=${SELFTEST:-all} >> "$LOG" 2>&1
grep -a "SELFTEST DONE\|SELFTEST FAIL\|PATCHCHECK\|patch skipped\|Changes patch" eclipse/logs/latest.log >> "$LOG"
if [ -n "$FAMILY" ]; then
    echo "--- family: crashes, clashes, duplicates" >> "$LOG"
    ls eclipse/crash-reports 2>/dev/null >> "$LOG"
    grep -a -i "id mismatch\|clash\|duplicate\|already registered\|already exists\|Registry override\|Exception in\|Caught exception\|VerifyError\|NoSuchMethodError\|NoClassDefFoundError" eclipse/logs/latest.log \
        | grep -a -v "SELFTEST" | sort | uniq -c | sort -rn | head -60 >> "$LOG"
    rm -f eclipse/mods/*-dev.jar
fi
python -I tools/check_assets.py >> "$LOG" 2>&1
PYTHONIOENCODING=utf-8 python -I tools/lang/build_langs.py check >> "$LOG" 2>&1
PYTHONIOENCODING=utf-8 python -I tools/lang/vanilla_text.py check >> "$LOG" 2>&1
if [ -z "$NOBUILD" ]; then ./gradlew build --rerun-tasks >> "$LOG" 2>&1; fi
echo "SUITE END" >> "$LOG"
