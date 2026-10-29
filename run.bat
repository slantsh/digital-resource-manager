@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

if not exist "bin" mkdir "bin"

echo Compiling Digital Resource Manager from Main.java...
javac -cp "lib/*" -sourcepath src/main/java -d bin src/main/java/Main.java
if errorlevel 1 (
    echo Compilation failed!
    exit /b %errorlevel%
)

echo Compilation successful. Launching application...
java -cp "bin;lib/*" Main %*
