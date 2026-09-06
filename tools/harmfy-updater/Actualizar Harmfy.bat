@echo off
setlocal
title Harmfy Updater

set "SCRIPT_DIR=%~dp0"
set "PS_SCRIPT=%SCRIPT_DIR%updater.ps1"

where powershell.exe >nul 2>nul
if errorlevel 1 (
  echo No se encontro PowerShell en este Windows.
  echo Instala/activa Windows PowerShell y vuelve a intentar.
  pause
  exit /b 1
)

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%PS_SCRIPT%"
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
  echo.
  echo El actualizador termino con error. Codigo: %EXIT_CODE%
  pause
  exit /b %EXIT_CODE%
)

echo.
echo Listo. Puedes cerrar esta ventana.
pause
