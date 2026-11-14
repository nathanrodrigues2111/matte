@echo off
rem matte: solid (non-glass) ColorOS 17 notification cards from a computer, no root.
rem Needs adb.exe (Android SDK Platform-Tools) in PATH or next to this file, and USB debugging on the phone.
rem Lost on every phone restart: run "matte.bat on" again after each restart.
setlocal
set "MODE=%~1"
if "%MODE%"=="" set "MODE=status"
if not defined ANDROID_SERIAL for /f "skip=1 tokens=1,2" %%a in ('adb devices') do if "%%b"=="device" if not defined ANDROID_SERIAL set "ANDROID_SERIAL=%%a"
if not defined ANDROID_SERIAL (echo matte: no phone found. Connect it by USB and allow USB debugging. & exit /b 1)
if /i "%MODE%"=="on" (
  adb shell setprop debug.systemui.gaussblur_level 4
  adb shell am crash com.android.systemui >nul 2>&1
  echo Status bar restarting, the screen flickers for a few seconds.
  exit /b 0
)
if /i "%MODE%"=="off" (
  adb shell setprop debug.systemui.gaussblur_level -1
  adb shell am crash com.android.systemui >nul 2>&1
  echo Status bar restarting, the screen flickers for a few seconds.
  exit /b 0
)
if /i "%MODE%"=="status" (
  for /f %%v in ('adb shell getprop debug.systemui.gaussblur_level') do if "%%v"=="4" (echo Matte: on ^(solid cards^)& exit /b 0)
  echo Matte: off ^(glass cards^)
  exit /b 0
)
echo usage: matte.bat on^|off^|status
exit /b 1
