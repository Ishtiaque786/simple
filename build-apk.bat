@echo off
setlocal
where gradle >nul 2>nul
if errorlevel 1 (
  echo Gradle command was not found.
  echo Easiest method: open this folder in Android Studio and choose:
  echo Build ^> Build Bundle(s) / APK(s) ^> Build APK(s)
  echo.
  echo Install Android Studio: https://developer.android.com/studio
  pause
  exit /b 1
)
echo Building Simple Player Android debug APK...
gradle assembleDebug
if errorlevel 1 (
  echo Build failed. Please send the error message.
  pause
  exit /b 1
)
echo.
echo APK created at:
echo app\build\outputs\apk\debug\app-debug.apk
pause
