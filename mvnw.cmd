@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.2
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET "__MVNW_ARG0_NAME__=%~nx0")
@SET __MVNW_CMD__=
@SET __MVNW_ERROR__=
@SET __MVNW_DRIVE_LETTER__=%~d0
@SET __MVNW_WDIR__=%~dp0
@SET __MVNW_PROPS__=%__MVNW_WDIR__%\.mvn\wrapper\maven-wrapper.properties

@IF NOT EXIST %__MVNW_PROPS__% (
  @ECHO Error: %__MVNW_PROPS__% not found. >&2
  @EXIT /B 1
)

@FOR /F "usebackq tokens=1,2 delims==" %%a IN ("%__MVNW_PROPS__%") DO (
  @IF "%%a"=="distributionUrl" SET "DISTRIBUTION_URL=%%b"
)

@IF "%DISTRIBUTION_URL%"=="" (
  @ECHO Error: distributionUrl not found in %__MVNW_PROPS__% >&2
  @EXIT /B 1
)

@SET "__MVNW_DISTRIBUTION_ID__=%DISTRIBUTION_URL:~0,-4%"
@FOR %%F IN ("%__MVNW_DISTRIBUTION_ID__%") DO SET "__MVNW_DISTRIBUTION_ID__=%%~nxF"

@SET "__MVNW_M2_HOME__=%USERPROFILE%\.m2\wrapper\dists"
@SET "__MVNW_DISTRIBUTION_DIR__=%__MVNW_M2_HOME__%\%__MVNW_DISTRIBUTION_ID__%"

@IF EXIST "%__MVNW_DISTRIBUTION_DIR__%\apache-maven-3.9.9\bin\mvn.cmd" (
  @SET "__MVNW_CMD__=%__MVNW_DISTRIBUTION_DIR__%\apache-maven-3.9.9\bin\mvn.cmd"
  @GOTO execute
)

@ECHO Downloading Maven from %DISTRIBUTION_URL% ...
@IF NOT EXIST "%__MVNW_M2_HOME__%\" (MKDIR "%__MVNW_M2_HOME__%")
@IF NOT EXIST "%__MVNW_DISTRIBUTION_DIR__%\" (MKDIR "%__MVNW_DISTRIBUTION_DIR__%")

@SET "__MVNW_ZIP__=%__MVNW_DISTRIBUTION_DIR__%\apache-maven-3.9.9-bin.zip"

@powershell -Command "& { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object System.Net.WebClient).DownloadFile('%DISTRIBUTION_URL%', '%__MVNW_ZIP__%') }"
@IF ERRORLEVEL 1 (
  @ECHO Error: download failed >&2
  @EXIT /B 1
)

@powershell -Command "& { Add-Type -AssemblyName System.IO.Compression.FileSystem; [System.IO.Compression.ZipFile]::ExtractToDirectory('%__MVNW_ZIP__%', '%__MVNW_DISTRIBUTION_DIR__%') }"
@IF ERRORLEVEL 1 (
  @ECHO Error: extraction failed >&2
  @EXIT /B 1
)

@DEL "%__MVNW_ZIP__%"
@SET "__MVNW_CMD__=%__MVNW_DISTRIBUTION_DIR__%\apache-maven-3.9.9\bin\mvn.cmd"

:execute
@IF NOT EXIST "%__MVNW_CMD__%" (
  @ECHO Error: Maven executable not found at %__MVNW_CMD__% >&2
  @EXIT /B 1
)

@"%__MVNW_CMD__%" %*
