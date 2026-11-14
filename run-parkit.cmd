@echo off
setlocal
if not defined ANDROID_HOME if exist "D:\PleosDev\Sdk\platform-tools\adb.exe" set "ANDROID_HOME=D:\PleosDev\Sdk"
if not defined ANDROID_AVD_HOME if exist "D:\PleosDev\avd" set "ANDROID_AVD_HOME=D:\PleosDev\avd"
if not defined GRADLE_USER_HOME if exist "D:\PleosDev\gradle-cache" set "GRADLE_USER_HOME=D:\PleosDev\gradle-cache"
if not defined PARKIT_BUILD_ROOT if exist "D:\PleosDev\projects" set "PARKIT_BUILD_ROOT=D:\PleosDev\projects"
if not defined PARKIT_DATA_DIR if exist "%~dp0..\..\artifacts\parkit-capture-data\" set "PARKIT_DATA_DIR=%~dp0..\..\artifacts\parkit-capture-data"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\run-windows.ps1" %*
exit /b %ERRORLEVEL%
