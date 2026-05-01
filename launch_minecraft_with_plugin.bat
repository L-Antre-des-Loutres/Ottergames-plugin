@echo off
setlocal EnableExtensions
cd /d "%~dp0"

if not exist "gradlew.bat" (
    echo [ERREUR] gradlew.bat introuvable dans le dossier du projet.
    exit /b 1
)

where java >nul 2>&1
if errorlevel 1 (
    echo [ERREUR] Java introuvable dans le PATH.
    exit /b 1
)

set "JAVA_VERSION="
set "JAVA_MAJOR="
set "JAVA_MAJOR_NUM="
for /f "tokens=3" %%V in ('java -XshowSettings:properties -version 2^>^&1 ^| findstr /c:"java.version = "') do set "JAVA_VERSION=%%V"
if not defined JAVA_VERSION (
    echo [ERREUR] Impossible de detecter la version de Java.
    exit /b 1
)
for /f "tokens=1 delims=." %%M in ("%JAVA_VERSION%") do set "JAVA_MAJOR=%%M"
if "%JAVA_MAJOR%"=="1" for /f "tokens=2 delims=." %%M in ("%JAVA_VERSION%") do set "JAVA_MAJOR=%%M"
set /a JAVA_MAJOR_NUM=%JAVA_MAJOR% >nul 2>&1
if errorlevel 1 (
    echo [ERREUR] Version Java non reconnue: %JAVA_VERSION%
    exit /b 1
)
if %JAVA_MAJOR_NUM% LSS 25 (
    echo [ERREUR] Paper 26.1.2 requiert Java 25+ ^(detecte: %JAVA_VERSION%^).
    echo Mets Java a jour: https://docs.papermc.io/misc/java-install
    exit /b 1
)

set "RUN_DIR=%~dp0run"
set "EULA_FILE=%RUN_DIR%\eula.txt"
set "PLUGINS_DIR=%RUN_DIR%\plugins"
set "PAPER_JAR=paper-26.1.2-53.jar"
set "PAPER_PATH=%RUN_DIR%\%PAPER_JAR%"
set "PAPER_URL=https://fill-data.papermc.io/v1/objects/6934188878fc351e1be5bfba5f2b8c4591224886e4b34e3de09dbec68a351caf/paper-26.1.2-53.jar"
set "PLUGIN_SOURCE="
set "PLUGIN_TARGET=%PLUGINS_DIR%\ottergames.jar"

if not exist "%RUN_DIR%" mkdir "%RUN_DIR%"
if not exist "%PLUGINS_DIR%" mkdir "%PLUGINS_DIR%"

if not exist "%EULA_FILE%" (
    > "%EULA_FILE%" echo eula=true
) else (
    findstr /i /c:"eula=true" "%EULA_FILE%" >nul
    if errorlevel 1 > "%EULA_FILE%" echo eula=true
)

echo [1/4] Build du plugin...
call ".\gradlew.bat" --no-daemon build
if errorlevel 1 (
    echo [ERREUR] Le build du plugin a echoue.
    exit /b 1
)

for /f "delims=" %%F in ('dir /b /a:-d /o:-d "%~dp0build\libs\ottergames*.jar" 2^>nul') do (
    if not defined PLUGIN_SOURCE set "PLUGIN_SOURCE=%~dp0build\libs\%%F"
)
if not defined PLUGIN_SOURCE (
    echo [ERREUR] Impossible de trouver le jar du plugin dans build\libs.
    exit /b 1
)
echo [INFO] Jar plugin selectionne: %PLUGIN_SOURCE%

echo [2/4] Installation du plugin dans le serveur...
copy /Y "%PLUGIN_SOURCE%" "%PLUGIN_TARGET%" >nul
if errorlevel 1 (
    echo [ERREUR] Impossible de copier le plugin vers %PLUGIN_TARGET%.
    exit /b 1
)

if not exist "%PAPER_PATH%" (
    echo [3/4] Telechargement de Paper 26.1.2-53...
    curl -L --fail --output "%PAPER_PATH%" "%PAPER_URL%"
    if errorlevel 1 (
        echo [ERREUR] Echec du telechargement de Paper 26.1.2-53.
        exit /b 1
    )
) else (
    echo [3/4] Paper 26.1.2-53 deja present.
)

echo [4/4] Demarrage du serveur local (Paper 26.1.2-53 + plugin Ottergames)...
start "Ottergames Server" cmd /k "cd /d ""%RUN_DIR%"" && java -Xms1G -Xmx1G -jar ""%PAPER_JAR%"" nogui"

echo Ouverture du launcher Minecraft...
call :findLauncher
if defined MINECRAFT_LAUNCHER (
    start "" "%MINECRAFT_LAUNCHER%"
    echo Launcher ouvert. Connecte-toi ensuite a localhost:25565
) else (
    echo [INFO] Launcher Minecraft non trouve automatiquement.
    echo Definis la variable d'environnement MINECRAFT_LAUNCHER vers MinecraftLauncher.exe
    echo puis relance ce script.
)

exit /b 0

:findLauncher
if defined MINECRAFT_LAUNCHER (
    if exist "%MINECRAFT_LAUNCHER%" goto :eof
    set "MINECRAFT_LAUNCHER="
)

for %%P in (
    "%ProgramFiles(x86)%\Minecraft Launcher\MinecraftLauncher.exe"
    "%ProgramFiles%\Minecraft Launcher\MinecraftLauncher.exe"
    "%LocalAppData%\Programs\Minecraft Launcher\MinecraftLauncher.exe"
    "%LocalAppData%\Microsoft\WindowsApps\MinecraftLauncher.exe"
) do (
    if exist "%%~P" (
        set "MINECRAFT_LAUNCHER=%%~P"
        goto :eof
    )
)

goto :eof
