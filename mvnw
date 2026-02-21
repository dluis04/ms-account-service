#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup batch script, version 3.3.2
# ----------------------------------------------------------------------------

set -euf
[ "${MVNW_VERBOSE-}" != debug ] || set -x

# OS specific support.
native_path() { printf '%s\n' "$1"; }
case "$(uname)" in
CYGWIN* | MINGW*)
  [ -z "${JAVA_HOME-}" ] || JAVA_HOME="$(cygpath --unix "$JAVA_HOME")"
  native_path() { cygpath --path --windows "$1"; }
  ;;
esac

# set JAVACMD and JAVACCMD
set_java_home() {
  # For Cygwin and MinGW, ensure paths are in Unix format before
  # anything is touched.
  if [ -n "${JAVA_HOME-}" ]; then
    if [ -x "$JAVA_HOME/jre/sh/java" ]; then
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
      JAVACCMD="$JAVA_HOME/jre/sh/javac"
    else
      JAVACMD="$JAVA_HOME/bin/java"
      JAVACCMD="$JAVA_HOME/bin/javac"

      if [ ! -x "$JAVACMD" ] || [ ! -x "$JAVACCMD" ]; then
        echo "The JAVA_HOME environment variable is not defined correctly, so mvnw cannot run." >&2
        echo "JAVA_HOME is set to \"$JAVA_HOME\", but \"\$JAVA_HOME/bin/java\" or \"\$JAVA_HOME/bin/javac\" does not exist." >&2
        return 1
      fi
    fi
  else
    JAVACMD="$(
      'set' +e
      'unset' -f command 2>/dev/null
      'command' -v java
    )" || :
    JAVACCMD="$(
      'set' +e
      'unset' -f command 2>/dev/null
      'command' -v javac
    )" || :

    if [ ! -x "${JAVACMD-}" ]; then
      echo "The java executable is not found. Please install Java or set JAVA_HOME." >&2
      return 1
    fi
    if [ ! -x "${JAVACCMD-}" ]; then
      echo "The javac executable is not found. Please install JDK or set JAVA_HOME." >&2
      return 1
    fi
  fi
}

# hash string like Java String::hashCode
hash_string() {
  str="${1:-}" h=0
  while [ -n "$str" ]; do
    char="${str%"${str#?}"}"
    h=$(((h * 31 + $(LC_CTYPE=C printf '%d' "'$char")) % 4294967296))
    str="${str#?}"
  done
  printf '%u' $h
}

verbose() { :; }
[ "${MVNW_VERBOSE-}" != true ] || verbose() { printf '%s\n' "${1-}"; }

die() {
  printf '%s\n' "$1" >&2
  exit 1
}

trim() {
  # Trims leading and trailing whitespace
  local var="$*"
  var="${var#"${var%%[![:space:]]*}"}"
  var="${var%"${var##*[![:space:]]}"}"
  printf '%s' "$var"
}

# parse distributionUrl and optional distributionSha256Sum
while IFS="=" read -r key value; do
  case "${key-}" in
  distributionUrl) distributionUrl="${value-}" ;;
  distributionSha256Sum) distributionSha256Sum="${value-}" ;;
  esac
done <"${0%/*}/.mvn/wrapper/maven-wrapper.properties"
[ -n "${distributionUrl-}" ] || die "cannot read distributionUrl property in ${0%/*}/.mvn/wrapper/maven-wrapper.properties"

case "${distributionUrl##*/}" in
maven-mvnd-*)
  MVN_CMD=mvnd.sh _MVNW_REPO_PATTERN=/maven/mvnd/
  case "${PROCESSOR_ARCHITECTURE-}${PROCESSOR_ARCHITEW6432-}:$(uname -a)" in
  *AMD64:CYGWIN* | *AMD64:MINGW*) distributionPlatform=windows-amd64 ;;
  :Darwin*x86_64) distributionPlatform=darwin-amd64 ;;
  :Darwin*arm64) distributionPlatform=darwin-aarch64 ;;
  :Linux*x86_64) distributionPlatform=linux-amd64 ;;
  *)
    echo "Cannot detect native platform for mvnd on $(uname)-$(uname -m), use pure java mvnd" >&2
    distributionPlatform=linux-amd64
    ;;
  esac
  distributionUrl="${distributionUrl%-bin.*}-$distributionPlatform.zip"
  ;;
*) MVN_CMD=mvn.cmd _MVNW_REPO_PATTERN=/org/apache/maven/ ;;
esac

# apply distributionSha256Sum checksum check
if [ -n "${distributionSha256Sum-}" ]; then
  distributionSha256SumCmd=""
  if [ -n "${distributionSha256SumCmd-}" ]; then
    :
  elif command -v sha256sum >/dev/null 2>&1; then
    distributionSha256SumCmd="sha256sum"
  elif command -v shasum >/dev/null 2>&1; then
    distributionSha256SumCmd="shasum -a 256"
  else
    echo "Checksum validation requires sha256sum or shasum to be available." >&2
    echo "Alternatively, set MVNW_VERBOSE=checksumSkip to disable validation." >&2
    die "cannot hash"
  fi
fi

BASE_HOME="${HOME-}"
[ -n "${BASE_HOME-}" ] || die "HOME is not set"

M2_HOME="${MVNW_REPODIR:-"$BASE_HOME/.m2"}/wrapper/dists"
mkdir -p "$M2_HOME"

DISTRIBUTION_ID=$(trim "${distributionUrl##*/}")
DISTRIBUTION_ID="${DISTRIBUTION_ID%.zip}"
DISTRIBUTION_DIR="$M2_HOME/$DISTRIBUTION_ID"

if [ -d "$DISTRIBUTION_DIR" ]; then
  MVN_HOME="$(find "$DISTRIBUTION_DIR" -maxdepth 1 -mindepth 1 -type d | head -1)"
  if [ -n "${MVN_HOME-}" ] && [ -f "$MVN_HOME/bin/$MVN_CMD" ]; then
    verbose "Found existing Maven installation at $MVN_HOME"
  else
    MVN_HOME=""
  fi
fi

if [ -z "${MVN_HOME-}" ]; then
  ZIP_FILE="$M2_HOME/${DISTRIBUTION_ID}.zip"
  verbose "Downloading Maven from $distributionUrl"
  if command -v curl >/dev/null 2>&1; then
    curl -fL "${MVNW_USERNAME:+--user "${MVNW_USERNAME}:${MVNW_PASSWORD-}"}" \
      --retry 3 --connect-timeout 10 \
      -o "$ZIP_FILE" "$distributionUrl" || die "failed to download $distributionUrl"
  elif command -v wget >/dev/null 2>&1; then
    wget --tries=3 --timeout=10 \
      ${MVNW_USERNAME:+--user="$MVNW_USERNAME" --password="${MVNW_PASSWORD-}"} \
      -O "$ZIP_FILE" "$distributionUrl" || die "failed to download $distributionUrl"
  else
    die "curl or wget is required to download the Maven distribution"
  fi

  if [ -n "${distributionSha256Sum-}" ]; then
    distributionSha256SumValue=$(eval "$distributionSha256SumCmd '$ZIP_FILE'" | cut -f1 -d' ')
    if [ "$distributionSha256SumValue" != "$distributionSha256Sum" ]; then
      echo "Error: Failed to validate Maven distribution SHA-256, your Maven distribution might be compromised." >&2
      echo "Investigate or use MVNW_VERBOSE=checksumSkip to disable validation." >&2
      die "checksum mismatch for $ZIP_FILE"
    fi
  fi

  unzip -q "$ZIP_FILE" -d "$DISTRIBUTION_DIR"
  rm -f "$ZIP_FILE"

  MVN_HOME="$(find "$DISTRIBUTION_DIR" -maxdepth 1 -mindepth 1 -type d | head -1)"
  [ -n "${MVN_HOME-}" ] || die "failed to find Maven home in $DISTRIBUTION_DIR"
fi

set_java_home

exec "$MVN_HOME/bin/$MVN_CMD" "$@"
