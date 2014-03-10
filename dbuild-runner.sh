#!/usr/bin/env bash
set -e
set -o pipefail
export LANG="en_US.UTF-8"
export HOME="$(pwd)"

if [ "$#" -lt "2" ]
then
  echo "Usage: $0 <dbuild-file> <dbuild-version> [<dbuild-options>]"
  exit 1
fi
DBUILDCONFIG="$1"
DBUILDVERSION="$2"
shift;shift

if [ ! -f "$DBUILDCONFIG" ]
then
  echo "File not found: $DBUILDCONFIG"
  exit 1
fi

echo "dbuild version: $DBUILDVERSION"
echo "dbuild config: $DBUILDCONFIG"
#sed 's/"\([^@"]*\)@[^"]*\.[^"]*"/"\1@..."/g' <"$DBUILDCONFIG"

if [ ! -d "dbuild-${DBUILDVERSION}" ]
then
  wget "http://downloads.typesafe.com/dbuild/${DBUILDVERSION}/dbuild-${DBUILDVERSION}.tgz"
  tar xfz "dbuild-${DBUILDVERSION}.tgz"
  rm "dbuild-${DBUILDVERSION}.tgz"
fi

echo "dbuild-${DBUILDVERSION}/bin/dbuild" "${@}" "$DBUILDCONFIG"
"dbuild-${DBUILDVERSION}/bin/dbuild" "${@}" "$DBUILDCONFIG" 2>&1 | tee "dbuild-${DBUILDVERSION}/dbuild.out"
STATUS="$?"
BUILD_ID="$(grep '^\[info\]  uuid = ' "dbuild-${DBUILDVERSION}/dbuild.out" | sed -e 's/\[info\]  uuid = //')"
echo "The repeatable UUID of this build was: ${BUILD_ID}"
exit "$STATUS"
