@echo off
rem hybrisserver for Windows: hybrisserver.bat [start|stop]
set COMPOSE=docker compose -f "%~dp0..\..\..\..\docker-compose.yml"
if "%1"=="stop" ( %COMPOSE% stop commerce & goto :eof )
if "%1"=="start" ( %COMPOSE% up -d commerce & goto :eof )
%COMPOSE% up -d commerce && %COMPOSE% logs -f commerce
