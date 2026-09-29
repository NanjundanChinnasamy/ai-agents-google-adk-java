@echo off
cd /d "%~dp0"
echo =======================================================
echo   Starting SocialSpark Agent Direct Console...
echo =======================================================
.\gradlew.bat runAgent --console=plain -q %*
