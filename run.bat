@echo off
set "MVN=%USERPROFILE%\.maven\maven-3.10.0\bin\mvn.cmd"
if exist "%MVN%" (
    call "%MVN%" javafx:run
) else (
    echo Maven was not found at "%MVN%".
    echo Install Maven or update this path in run.bat.
    exit /b 1
)
