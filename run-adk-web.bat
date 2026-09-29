@echo off
cd /d "%~dp0"
echo =========================================================
echo   Starting Google ADK Official Web Dev UI on port 8080...
echo   Open in your browser: http://localhost:8080/dev-ui
echo =========================================================
.\gradlew.bat runDevUi --console=plain %*
