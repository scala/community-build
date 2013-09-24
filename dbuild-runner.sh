#!/usr/bin/env bash
set -ex
set -o pipefail
export LANG="en_US.UTF-8"
export HOME="$(pwd)"

if [ "$#" -ne "2" ]
then
  echo "Usage: $0 <dbuild-file> <dbuild-version>"
  exit 1
fi
DBUILDCONFIG="$1"
DBUILDVERSION="$2"

if [ ! -f "$DBUILDCONFIG" ]
then
  echo "File not found: $DBUILDCONFIG"
  exit 1
fi

echo "dbuild version: $DBUILDVERSION"
echo "dbuild config:"
cat "$DBUILDCONFIG"

if [ ! -d "dbuild-${DBUILDVERSION}" ]
then
  wget "http://downloads.typesafe.com/dbuild/${DBUILDVERSION}/dbuild-${DBUILDVERSION}.tgz"
  tar xfz "dbuild-${DBUILDVERSION}.tgz"
  rm "dbuild-${DBUILDVERSION}.tgz"
fi
cd "dbuild-${DBUILDVERSION}"

bin/dbuild "../$DBUILDCONFIG" 2>&1 | tee dbuild.out
set +x
BUILD_ID="$(grep '^\[info\]  uuid = ' dbuild.out | sed -e 's/\[info\]  uuid = //')"
echo "The repeatable UUID of this build was: ${BUILD_ID}"
