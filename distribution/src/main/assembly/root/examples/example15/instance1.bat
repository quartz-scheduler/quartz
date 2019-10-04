@echo off
setlocal
if not exist "%JAVA_HOME%\bin\java.exe" (
  echo "Please set JAVA_HOME"
  exit /b 1
)

rem Set Quartz to the base directory of the Quartz Distribution
@SET WD=%~d0%~p0
@SET QUARTZ=%WD%..\..

@rem setup the class path...
CALL "%WD%..\bin\buildcp.bat"
SET QUARTZ_CP=%TMP_CP%

rem !!!!!!! Please read important information. !!!!!!
rem If "java" is not in your path, please set the path 
rem for Java 2 Runtime Environment in the path variable below
rem for example :
rem @SET PATH=D:\jdk1.6.0_18;%PATH%
rem 

rem a configuration file for log4j logging
@SET LOG4J_PROPS="-Dlog4j.configuration=file:%WD%log4j.xml"

rem Set the location and name of the quartz.properties file
@SET QUARTZ_PROPS="-Dorg.quartz.properties=%WD%instance1.properties"

rem Set the path to your Terracotta server home here
@SET INSTALL_DIR=%WD%..\..\..

IF NOT EXIST "%INSTALL_DIR%\server\bin\start-tc-server.bat" (
echo "Modify the script to set INSTALL_DIR"
exit /B
)

dir /b "%INSTALL_DIR%\apis\toolkit\lib\terracotta-toolkit*.jar" > temp.tmp
FOR /F %%I IN (temp.tmp) DO SET TC_CP="%INSTALL_DIR%\apis\toolkit\lib\%%I";%TC_CP%
del temp.tmp


"%JAVA_HOME%\bin\java.exe" -cp %QUARTZ_CP%;%TC_CP% %QUARTZ_PROPS% %LOG4J_PROPS% -Dtc.install-root=%INSTALL_DIR% org.quartz.examples.example15.ClusterExample %1
endlocal