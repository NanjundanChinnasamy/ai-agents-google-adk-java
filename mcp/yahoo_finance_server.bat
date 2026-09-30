@echo off
setlocal
set SCRIPT_DIR=%~dp0
java -jar "%SCRIPT_DIR%yahoo-finance-mcp.jar" %*
endlocal
