@echo off
echo Building the plugin jar...
call gradlew.bat build
echo Build finished! The jar is located in build\libs\
pause
