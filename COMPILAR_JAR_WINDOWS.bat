@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title Lexy Client - Compilar mod Fabric 1.20.1

echo.
echo =============================================
echo   LEXY CLIENT 1.4 - COMPILADOR WINDOWS

echo =============================================
echo.
where java >nul 2>&1
if errorlevel 1 (
 echo [ERROR] Java no se encuentra instalado.
 echo Instala JDK 17 o superior desde https://adoptium.net/
 pause
 exit /b 1
)
java -version
if not exist ".buildtools" mkdir ".buildtools"
if not exist ".buildtools\gradle-8.10.2\bin\gradle.bat" (
 echo.
 echo Descargando Gradle 8.10.2 desde su web oficial...
 powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.10.2-bin.zip' -OutFile '.buildtools\gradle.zip'; Expand-Archive -LiteralPath '.buildtools\gradle.zip' -DestinationPath '.buildtools' -Force; Remove-Item '.buildtools\gradle.zip' -Force"
 if errorlevel 1 (
  echo [ERROR] No se pudo descargar Gradle. Comprueba tu conexion.
  pause
  exit /b 1
 )
)
echo.
echo Compilando Lexy Client para Fabric 1.20.1...
call ".buildtools\gradle-8.10.2\bin\gradle.bat" --no-daemon clean build
if errorlevel 1 (
 echo.
 echo [ERROR] La compilacion ha fallado. Lee el error anterior.
 echo No se ha creado un JAR utilizable.
 pause
 exit /b 1
)
if not exist "build\libs\lexy-client-1.4.0.jar" (
 echo [ERROR] Gradle termino, pero no encontro el JAR final.
 dir "build\libs"
 pause
 exit /b 1
)
copy /Y "build\libs\lexy-client-1.4.0.jar" "%~dp0lexy-client-1.4.0.jar" >nul
if errorlevel 1 (
 echo [ERROR] No se pudo copiar el archivo.
 pause
 exit /b 1
)
echo.
echo [OK] JAR generado: %~dp0lexy-client-1.4.0.jar
 echo Copialo a .minecraft\mods con Fabric Loader y Fabric API instalados.
 pause