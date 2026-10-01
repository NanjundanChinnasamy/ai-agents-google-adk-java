@echo off
setlocal
cd /d "%~dp0"
if "%~1"=="" (
    call gradlew.bat runFinanceV8 --console=plain -q
) else (
    call gradlew.bat runFinanceV8 --console=plain -q --args="%~1"
)
