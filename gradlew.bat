@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem
@rem SPDX-License-Identifier: Apache-2.0
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:execute
@rem Setup the command line



@rem Execute Gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -jar "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" %*
set GRADLE_EXIT_CODE=%ERRORLEVEL%

@rem ========================================================================
@rem Post-build: EXE icon & version (required when EXE exists, FAILS if tools missing)
@rem ========================================================================
if %GRADLE_EXIT_CODE% equ 0 (
    call :resource_hacker_post_build
    if errorlevel 1 set GRADLE_EXIT_CODE=1
)

:end
@rem End local scope for the variables with windows NT shell
if %GRADLE_EXIT_CODE% equ 0 goto mainEnd

:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set EXIT_CODE=%GRADLE_EXIT_CODE%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%GRADLE_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
@goto :skip_resource_hacker

:resource_hacker_post_build
@echo.
@echo === Post-build: EXE icon ^& version ===
set BUILD_DIR=%APP_HOME%\JMCL\build\libs

@rem Find the latest JAR
for /f "delims=" %%f in ('dir /b /o-d "%BUILD_DIR%\*.jar" 2^>nul') do (
    set JAR_FILE=%BUILD_DIR%\%%f
    goto :found_jar
)
@echo No JAR found, skipping EXE post-processing.
goto :eof

:found_jar
@rem Find the latest EXE
for /f "delims=" %%f in ('dir /b /o-d "%BUILD_DIR%\*.exe" 2^>nul') do (
    set EXE_FILE=%BUILD_DIR%\%%f
    goto :found_exe
)
@echo No EXE found, skipping EXE post-processing.
goto :eof

:found_exe
@echo JAR: %JAR_FILE%
@echo EXE: %EXE_FILE%

@rem Extract HMCLauncher.exe from JAR
set STUB_EXE=%TEMP%\HMCLauncher_original.exe
set STUB_NEW=%TEMP%\HMCLauncher_original_new.exe
powershell -NoProfile -Command ^
    "$jar=[System.IO.Compression.ZipFile]::OpenRead('%JAR_FILE%');" ^
    "$entry=$jar.GetEntry('assets/HMCLauncher.exe');" ^
    "if($entry){" ^
    "  $stream=$entry.Open();" ^
    "  $fs=[System.IO.File]::Create('%STUB_EXE%');" ^
    "  $stream.CopyTo($fs);$fs.Close();$stream.Close();" ^
    "} else { throw 'HMCLauncher.exe not found in JAR' }"
if errorlevel 1 (
    @echo ERROR: Could not extract HMCLauncher.exe from JAR
    exit /b 1
)
@echo Extracted HMCLauncher.exe

@rem Extract version from JAR
for /f "tokens=2 delims==" %%v in ('powershell -NoProfile -Command ^
    "$jar=[System.IO.Compression.ZipFile]::OpenRead('%JAR_FILE%');" ^
    "$entry=$jar.GetEntry('assets/jvmmcl.properties');" ^
    "if($entry){$r=New-Object System.IO.StreamReader($entry.Open());" ^
    "$l=$r.ReadToEnd();$r.Close();" ^
    "$m=[regex]::Match($l,'jvmmcl.version=(.+)');" ^
    "if($m.Success){$m.Groups[1].Value}}"') do set RAW_VERSION=%%v
if "%RAW_VERSION%"=="" set RAW_VERSION=DEV2026.3.0
@echo Version: %RAW_VERSION%

@rem Generate ICO from JPG
set ICON_ICO=%TEMP%\jmcl_icon.ico
if not exist "%APP_HOME%\IMG_0132.JPG" (
    @echo ERROR: IMG_0132.JPG not found, cannot create EXE icon
    exit /b 1
)
"%JAVA_EXE%" -cp "%APP_HOME%" CreateIcon "%APP_HOME%\IMG_0132.JPG" "%ICON_ICO%"
if errorlevel 1 (
    @echo ERROR: CreateIcon failed
    exit /b 1
)
@echo ICO created

@rem Find Python
set PY_CMD=
for %%p in (python3 python py) do (
    where %%p >nul 2>nul
    if not errorlevel 1 (
        set PY_CMD=%%p
        goto :check_pefile
    )
)
@echo ERROR: Python not found ^(python3/python/py required^)
exit /b 1

:check_pefile
"%PY_CMD%" -c "import pefile" >nul 2>nul
if errorlevel 1 (
    @echo ERROR: pefile not installed. Run: pip install pefile
    exit /b 1
)

@rem Run set_exe_icon.py
set SET_ICON_SCRIPT=%APP_HOME%\set_exe_icon.py
if not exist "%SET_ICON_SCRIPT%" (
    @echo ERROR: set_exe_icon.py not found
    exit /b 1
)
"%PY_CMD%" "%SET_ICON_SCRIPT%" "%STUB_EXE%" "%ICON_ICO%" "%RAW_VERSION%"
if errorlevel 1 (
    @echo ERROR: set_exe_icon.py failed
    exit /b 1
)
if not exist "%STUB_NEW%" (
    @echo ERROR: set_exe_icon.py did not produce output
    exit /b 1
)

@rem Concatenate: new EXE stub + JAR = final EXE
copy /b "%STUB_NEW%" + "%JAR_FILE%" "%EXE_FILE%" >nul
@echo EXE icon ^& version updated: %EXE_FILE%
@goto :eof

:skip_resource_hacker
