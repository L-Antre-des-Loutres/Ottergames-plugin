@echo off
set "JDK_DIR=%USERPROFILE%\.jdks"
set "JAVA_HOME="
for /d %%d in ("%JDK_DIR%\*") do (
    if exist "%%d\bin\java.exe" (
        set "JAVA_HOME=%%d"
    )
)

if "%JAVA_HOME%"=="" (
    echo [ERREUR] Java introuvable ! Lancez le build depuis IntelliJ une fois pour telecharger Java.
    pause
    exit /b
)

echo Java trouve : %JAVA_HOME%
echo Building the plugin jar...
call gradlew.bat build
echo Build finished! The jar is located in build\libs\
pause

