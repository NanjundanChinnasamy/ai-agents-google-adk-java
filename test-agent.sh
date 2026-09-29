#!/usr/bin/env bash
echo "======================================================="
echo "  Starting SocialSpark Agent Direct Console...         "
echo "======================================================="
./gradlew runAgent --console=plain -q "$@"
