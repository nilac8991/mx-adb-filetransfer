rem check for already installed
@echo off
adb shell pm list packages com.zebra.mxadbfiletransfer >installed.txt
for /f %%i in ("installed.txt") do set size=%%~zi
if %size% gtr 0 goto skipinstall
rem !!!! change this file path to match your own PC !!!
adb install -r "$MX-ADB-File-Transfer-APK Location"
:skipinstall
rem ECHO %var%
rem if "%var" <> "" GOTO skipinstall
rem @echo off
setlocal ENABLEDELAYEDEXPANSION
rem Take the cmd-line, remove all until the first parameter
set "params=!cmdcmdline:~0,-1!"
set "params=!params:*" =!"
set count=0

rem Split the parameters on spaces but respect the quotes
for %%G IN (!params!) do (
  set /a count+=1
  set "item_!count!=%%~G"
  rem echo !count! %%~G
)

rem list the parameters
for /L %%n in (1,1,!count!) DO (
 adb push "!item_%%n!" /sdcard
 for %%a in ("!item_%%n!") do (
 rem @echo %%~nxa
 adb shell am broadcast -a com.zebra.mxadbfiletransfer.FILE_MOVE_ACTION --es source_file_path "/sdcard/%%~nxa" --es target_file_path "/enterprise/usr/%%~nxa" -n com.zebra.mxadbfiletransfer/.FileTransferReceiver
   timeout 2
 )
)

timeout 15
REM ** The exit is important, so the cmd.ex doesn't try to execute commands after ampersands
exit
