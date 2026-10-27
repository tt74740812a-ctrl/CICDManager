@echo off
cd /d "%~dp0"
if defined JAVA_HOME (set "JAVA=%JAVA_HOME%\bin\java.exe") else (set "JAVA=java")
"%JAVA%" -Dfile.encoding=UTF-8 -jar CICDManager.jar
pause