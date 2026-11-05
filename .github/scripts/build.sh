#!/usr/bin/env bash
set -euo pipefail
log=$(mktemp)
trap 'rm -f "$log"' EXIT
for attempt in 1 2 3; do
  if ./gradlew build -x jacocoTestCoverageVerification 2>&1 | tee "$log"; then
    exit 0
  fi
  if ! grep -q "Execution failed for task ':jacocoTestReport'" "$log" ||
     ! grep -q 'Could not resolve.*org.jacoco' "$log"; then
    exit 1
  fi
  if [ "$attempt" -lt 3 ]; then sleep 10; fi
done
echo 'Coverage report dependencies unavailable; building with tests and without the optional report.'
./gradlew build -x jacocoTestCoverageVerification -x jacocoTestReport
