@echo off
chcp 65001 >nul
set JAVA_HOME=C:\Program Files\Java\jdk-18.0.2.1
echo Building YRItems...
call gradlew.bat build --no-daemon
if %ERRORLEVEL% EQU 0 (
    echo.
    echo Build successful!
    echo JAR location: E:\ServerPLUGINS\网易NK服务器插件\YRItems.jar
) else (
    echo.
    echo Build failed!
)
pause
