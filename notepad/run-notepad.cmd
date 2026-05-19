@echo off
setlocal
REM Launch NP with JavaFX modules. Adjust if Java or FX are elsewhere.
set "APP_DIR=%~dp0"
set "MODULEPATH=C:\Users\yeabs\Downloads\openjfx-26.0.1_windows-x64_bin-sdk\javafx-sdk-26.0.1\lib"

echo.
echo ===== Notepad DB Setup =====
set /p "INPUT_HOST=MySQL host [localhost]: "
if "%INPUT_HOST%"=="" set "INPUT_HOST=localhost"

set /p "INPUT_PORT=MySQL port [3306]: "
if "%INPUT_PORT%"=="" set "INPUT_PORT=3306"

set /p "INPUT_DB=Database name [notesdb]: "
if "%INPUT_DB%"=="" set "INPUT_DB=notesdb"

set /p "INPUT_USER=MySQL user [root]: "
if "%INPUT_USER%"=="" set "INPUT_USER=root"

set /p "INPUT_PASSWORD=MySQL password [empty]: "

set "NP_DB_HOST=%INPUT_HOST%"
set "NP_DB_PORT=%INPUT_PORT%"
set "NP_DB_NAME=%INPUT_DB%"
set "NP_DB_USER=%INPUT_USER%"
set "NP_DB_PASSWORD=%INPUT_PASSWORD%"

echo.
echo Launching with DB %NP_DB_HOST%:%NP_DB_PORT%/%NP_DB_NAME% as %NP_DB_USER%...

java --module-path "%MODULEPATH%" --add-modules javafx.controls,javafx.graphics -cp "%APP_DIR%;%APP_DIR%lib\*" NP
