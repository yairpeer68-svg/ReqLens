@echo off
setlocal
cd /d "%~dp0"
echo ============================================================
echo ReqLens 0.5-alpha - Android debug build
echo ============================================================
where java >nul 2>nul || (echo ERROR: Java 17 is required.& pause & exit /b 1)
where gradle >nul 2>nul || (echo ERROR: Gradle 8.13 is required in PATH.& echo Install Gradle or generate/use a Gradle wrapper.& pause & exit /b 1)
if "%ANDROID_HOME%"=="" if "%ANDROID_SDK_ROOT%"=="" (echo ERROR: ANDROID_HOME or ANDROID_SDK_ROOT must point to Android SDK.& pause & exit /b 1)
java -version
gradle --version
gradle --no-daemon clean :app:assembleDebug
if errorlevel 1 (echo BUILD FAILED.& pause & exit /b 1)
if not exist "%USERPROFILE%\Desktop" mkdir "%USERPROFILE%\Desktop"
copy /Y "app\build\outputs\apk\debug\app-debug.apk" "%USERPROFILE%\Desktop\ReqLens-0.5-alpha-debug.apk" >nul
echo PASS: APK copied to Desktop as ReqLens-0.5-alpha-debug.apk
pause
