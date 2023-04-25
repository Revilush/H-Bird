@echo off
setlocal
call setEnv.bat
cd %webinf%/classes


java charting.ChartManager

endlocal