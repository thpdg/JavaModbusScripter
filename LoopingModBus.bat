@echo off
setlocal
REM "****************************************************************************"
REM "* Setup the runtime environment "
REM "****************************************************************************"
set APPPATH=.
set JAVAAPP="%JAVA_HOME%"\bin\java.exe
set CLSPATH=%APPPATH%
set CLSPATH=%CLSPATH%;%APPPATH%\jamod-1.0.jar
echo ============================================================================ >> ModBusPurge.log
echo %date%%time% Checking ModBus... >> ModBusPurge.log
echo %CLSPATH%
echo %JAVAAPP%
rem %JAVAAPP% -cp %CLSPATH% com.protedyne.CmdServer
echo Clearing State
%JAVAAPP% -cp %CLSPATH% CmdServer 172.16.200.214 1/-3/2/5/4 0/00/0/0/0 1
%JAVAAPP% -cp %CLSPATH% CmdServer 172.16.200.214 4 0 1000
echo Running
:repeatforever
echo %date%%time% Start... >> ModBusPurge.log
%JAVAAPP% -cp %CLSPATH% CmdServer 172.16.200.214 -1/3/-4/-5/4/-2/05/-5/05/-3/1/2 01/9/06/11/8/05/12/11/12/10/0/7 60
%JAVAAPP% -cp %CLSPATH% CmdServer 172.16.200.214 1 0 1000
echo %date%%time% ...End >> ModBusPurge.log
GOTO repeatforever
echo %date%%time% Checking ModBus...Complete >> ModBusPurge.log
endlocal
