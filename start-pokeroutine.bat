@echo off
setlocal
cd /d "%~dp0"
where node >nul 2>nul
if errorlevel 1 (
  echo Node.js nao foi encontrado no PATH.
  echo Instale Node.js de uma fonte oficial ou execute pelo Android Studio/servidor local.
  pause
  exit /b 1
)
node "%~dp0serve-local.cjs" 127.0.0.1 8765
pause
