@echo off
setlocal
REM "****************************************************************************"
REM "* Setup the runtime environment "
REM "****************************************************************************"
set APPPATH=.
set JAVAAPP="%JAVA_HOME%"\bin\java.exe
set CLSPATH=%APPPATH%
set CLSPATH=%CLSPATH%;%APPPATH%\jamod-1.0.jar
echo ============================================================================ >> SpecimenDropper.log
echo %date%%time% Checking ModBus... >> SpecimenDropper.log
echo %CLSPATH%
echo %JAVAAPP%
echo %CLSPATH% >> SpecimenDropper.log
echo %JAVAAPP% >> SpecimenDropper.log

echo Clearing State
echo Clearing State >> SpecimenDropper.log
%JAVAAPP% -cp %CLSPATH% CmdServer 172.16.200.218 -3/1/-2 09/1/03 60
echo Running >> SpecimenDropper.log
echo Running 

:repeatforever
echo %date%%time% Start... >> SpecimenDropper.log

%JAVAAPP% -cp %CLSPATH% CmdServer 172.16.200.218 2/-1/03/1/-2/-1/-3/1 4/02/10/1/03/02/09/1 60

echo %date%%time% ...End >> SpecimenDropper.log
GOTO repeatforever
echo %date%%time% Checking ModBus...Complete >> SpecimenDropper.log
endlocal
