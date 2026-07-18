@echo off
set JAVA_HOME=C:\Users\cangcang\jdk21\jdk-21.0.11+10
set PATH=%JAVA_HOME%\bin;%PATH%

set JAR=JMCL\build\libs\JVM-MCL-DEV2026.3.0.jar
set CACHE=.gradle-user-home\caches\modules-2\files-2.1\org.openjfx

set FX_BASE=%CACHE%\javafx-base\21.0.8\c000208028c983b9ccf85b57c74e0e74f85849f0\javafx-base-21.0.8-win.jar
set FX_GFX=%CACHE%\javafx-graphics\21.0.8\f3aba10d2c9f767e41831aeed6e1cd5418299c03\javafx-graphics-21.0.8-win.jar
set FX_CTRL=%CACHE%\javafx-controls\21.0.8\ed4a6cb56461d8536e339abf8d7f771e4acf9054\javafx-controls-21.0.8-win.jar
set FX_WEB=%CACHE%\javafx-web\21.0.8\9327a24f925059173ca331d3f2787fb4c46596ea\javafx-web-21.0.8-win.jar

set CP=%JAR%;%FX_BASE%;%FX_GFX%;%FX_CTRL%;%FX_WEB%

echo Starting JMCL...
start javaw -cp "%CP%" -Xmx1g -Djvmmcl.offline.auth.restricted=false org.Open_code_Studio.jmcl.Main
echo JMCL started!
