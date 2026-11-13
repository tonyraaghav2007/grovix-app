@echo off
title Grovix Central Cloud Server
color 0B
echo ========================================================================
echo   STARTING GROVIX CENTRAL CLOUD SERVER...
echo ========================================================================
cd /d "%~dp0"
node start-cloud.js
pause

