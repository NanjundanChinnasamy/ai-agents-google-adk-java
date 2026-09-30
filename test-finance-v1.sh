#!/usr/bin/env bash
set -euo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

if [ $# -eq 0 ]; then
    ./gradlew runFinanceV1 --console=plain -q
else
    ./gradlew runFinanceV1 --console=plain -q --args="$*"
fi
