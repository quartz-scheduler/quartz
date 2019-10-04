@echo off

rem All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.

setlocal

@SET WD=%~d0%~p0

rem Set the path to your Terracotta server home here
@SET TC_HOME=%WD%..\..\..\server

IF NOT EXIST "%TC_HOME%\bin\start-tc-server.bat" (
echo "Modify the script to set TC_HOME"
exit /B
)

start "terracotta" "%TC_HOME%\bin\start-tc-server.bat"

endlocal
