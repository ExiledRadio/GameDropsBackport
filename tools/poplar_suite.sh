#!/usr/bin/env bash
# Stream 5's test run: the whole self-test on a fresh dev server, the asset and language checks, then the release jar.
#
#   cmd //c "start /affinity FFFF0000 /b /wait bash tools/poplar_suite.sh"
#
# The world is deleted first (the mod set or registrations may have changed). Results: build/poplar-suite.log.
cd "$(dirname "$0")/.." || exit 1
LOG=build/poplar-suite.log
mkdir -p build
: > "$LOG"
rm -rf eclipse/world
./gradlew runServer -Pselftest=all >> "$LOG" 2>&1
grep -a "SELFTEST DONE\|SELFTEST FAIL\|SELFTEST poplar" eclipse/logs/latest.log >> "$LOG"
python -I tools/check_assets.py >> "$LOG" 2>&1
PYTHONIOENCODING=utf-8 python -I tools/lang/build_langs.py check >> "$LOG" 2>&1
./gradlew build --rerun-tasks >> "$LOG" 2>&1
echo "SUITE END" >> "$LOG"
