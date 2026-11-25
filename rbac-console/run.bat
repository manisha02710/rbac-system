@echo off
REM Option A (Maven):    mvn -q compile exec:java
REM Option B (no Maven): put mysql-connector-j-8.x.jar inside the lib folder, then run this file.
chcp 65001 >nul
if not exist out mkdir out
dir /s /b src\main\java\*.java > sources.txt
javac -encoding UTF-8 -d out -cp "lib/*" @sources.txt || exit /b 1
xcopy /y /q src\main\resources\schema.sql out\ >nul
java -Dfile.encoding=UTF-8 -cp "out;lib/*" com.rbac.Main
