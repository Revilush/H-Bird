@echo off
setlocal
call setEnv.bat
cd %webinf%/classes


java xbrl.AutoHistoryUpdator options > %webinf%\..\cmd_logs\options.logs

endlocal