@echo off
setlocal
call setEnv.bat
cd %webinf%/classes


java xbrl.AutoHistoryUpdator divSec > %webinf%\..\cmd_logs\divSec.logs

endlocal