@echo off
cd /d "%~dp0"
if defined JAVA_HOME (set "JAVAW=%JAVA_HOME%\bin\javaw.exe") else (set "JAVAW=javaw")
start "" "%JAVAW%" -Dfile.encoding=UTF-8 -jar CICDManager.jar