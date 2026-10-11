@echo off
setlocal
cd /d "%~dp0"
where node >nul 2>nul
if errorlevel 1 (
  echo Node.js nao foi encontrado no PATH.
  pause
  exit /b 1
)
node "%~dp0serve-local.cjs" 0.0.0.0 8765
pause
