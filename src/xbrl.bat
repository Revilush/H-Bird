@echo off
setlocal
call setEnv.bat
cd %webinf%/classes


java xbrl.AutoHistoryUpdator xbrl > %webinf%\..\cmd_logs\xbrl.logs

endlocal