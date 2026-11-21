#!/usr/bin/env bash
# The changes stream's test run: the whole self-test on a fresh dev server, the asset and language checks, then the
# release jar.
#
#   cmd //c "start /affinity FFFF0000 /b /wait bash tools/changes_suite.sh"
#
# The world is deleted first. Results: build/changes-suite.log.
cd "$(dirname "$0")/.." || exit 1
LOG=build/changes-suite.log
mkdir -p build
: > "$LOG"
rm -rf eclipse/world
./gradlew runServer -Pselftest=${SELFTEST:-all} >> "$LOG" 2>&1
grep -a "SELFTEST DONE\|SELFTEST FAIL\|P[1-5] \|Changes patch" eclipse/logs/latest.log >> "$LOG"
python -I tools/check_assets.py >> "$LOG" 2>&1
PYTHONIOENCODING=utf-8 python -I tools/lang/build_langs.py check >> "$LOG" 2>&1
if [ -z "$NOBUILD" ]; then ./gradlew build --rerun-tasks >> "$LOG" 2>&1; fi
echo "SUITE END" >> "$LOG"
