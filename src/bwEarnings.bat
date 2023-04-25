@echo off
setlocal
call setEnv.bat
cd %webinf%/classes


java xbrl.AutoHistoryUpdator bwEarnings

endlocal