@ECHO OFF
SETLOCAL

SET "MAVEN_VERSION=3.9.9"
SET "WRAPPER_DIR=%~dp0.mvn\wrapper"
SET "MAVEN_HOME=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%"
SET "MAVEN_ZIP=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%-bin.zip"
SET "MVN_CMD=%MAVEN_HOME%\bin\mvn.cmd"

IF NOT EXIST "%MVN_CMD%" (
  ECHO Downloading Maven %MAVEN_VERSION%...
  IF NOT EXIST "%WRAPPER_DIR%" MKDIR "%WRAPPER_DIR%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $url='https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip'; Invoke-WebRequest -Uri $url -OutFile '%MAVEN_ZIP%'; Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%WRAPPER_DIR%' -Force"
  IF ERRORLEVEL 1 (
    ECHO Failed to download Maven.
    EXIT /B 1
  )
)

"%MVN_CMD%" %*
SET ERR=%ERRORLEVEL%
ENDLOCAL & EXIT /B %ERR%
