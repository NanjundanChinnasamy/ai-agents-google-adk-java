#!/usr/bin/env bash
echo "========================================================="
echo "  Starting Google ADK Official Web Dev UI on port 8080..."
echo "  Open in your browser: http://localhost:8080/dev-ui"
echo "========================================================="
./gradlew runDevUi --console=plain "$@"
