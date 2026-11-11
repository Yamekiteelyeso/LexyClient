@echo off
where java >nul 2>nul || (echo Java 17 not found && exit /b 1)
where gradle >nul 2>nul || (echo Gradle 8.x not found && exit /b 1)
call gradle build
if errorlevel 1 exit /b 1
echo Mod JAR: build\libs\