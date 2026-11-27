@echo off
chcp 65001 >nul
cd /d "%~dp0"

set "EXE=desktopApp\build\compose\binaries\main\app\ZenMusic\ZenMusic.exe"

if exist "%EXE%" (
    start "" "%EXE%"
    exit /b 0
)

echo Первая сборка ZenMusic, это может занять пару минут...
call gradlew.bat :desktopApp:createDistributable
if exist "%EXE%" (
    start "" "%EXE%"
) else (
    echo Не удалось собрать приложение. Смотрите ошибки выше.
    pause
)
