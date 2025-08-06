@echo off
REM Extract the code from the URL
set url=%1
REM Remove the protocol prefix "jewel://"
set url=%url:jewel://=%

REM Call the jar with the code
java -jar "AuthConsoleApp.jar" "%url%"