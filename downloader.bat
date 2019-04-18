@echo off
set JAVA_HOME=c:\Program Files\Java\jdk1.8.0_91\jre
set DOWNLOADER=downloader-1.0.0-SNAPSHOT.jar
set JSOUP=jsoup-1.9.2.jar

SET mypath_=%~dp0
SET mypath=%mypath_:~0,-1%

if exist %mypath%\%JSOUP% (
    set MYCLASSPATH=%mypath%\%JSOUP%
) else (
	set MYCLASSPATH=%mypath%\dependency-jars\%JSOUP%
)

"%JAVA_HOME%\bin\java" -cp "%MYCLASSPATH%;%mypath%\%DOWNLOADER%" downloader/Downloader %*