#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

if [ -z "$1" ]; then
    ./gradlew runFinanceV11 --console=plain -q
else
    ./gradlew runFinanceV11 --console=plain -q --args="$1"
fi
