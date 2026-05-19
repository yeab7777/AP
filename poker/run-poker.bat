@echo off
setlocal

set "WORKDIR=%~dp0"
set "JAVAFX_LIB=C:\Users\yeabs\Downloads\openjfx-26.0.1_windows-x64_bin-sdk\javafx-sdk-26.0.1\lib"
set "MYSQL_JAR=C:\Users\yeabs\Downloads\mysql-connector-j-9.7.0\mysql-connector-j-9.7.0\mysql-connector-j-9.7.0.jar"

if not exist "%JAVAFX_LIB%" (
    echo JavaFX SDK not found at:
    echo %JAVAFX_LIB%
    pause
    exit /b 1
)

if not exist "%MYSQL_JAR%" (
    echo MySQL connector JAR not found at:
    echo %MYSQL_JAR%
    pause
    exit /b 1
)

pushd "%WORKDIR%"

javac --module-path "%JAVAFX_LIB%" --add-modules javafx.controls -cp ".;%MYSQL_JAR%" PokerGame.java
if errorlevel 1 (
    echo.
    echo Compile failed.
    popd
    pause
    exit /b 1
)

java --module-path "%JAVAFX_LIB%" --add-modules javafx.controls -cp ".;%MYSQL_JAR%" PokerGame
set EXIT_CODE=%ERRORLEVEL%

popd
pause
exit /b %EXIT_CODE%
