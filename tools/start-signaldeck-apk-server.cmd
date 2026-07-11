@echo off
cd /d D:\SignalDeck
powershell -NoProfile -ExecutionPolicy Bypass -File "D:\SignalDeck\serve-signaldeck-apk.ps1" -Root "D:\SignalDeck\apk" -Prefix "http://+:8099/"
