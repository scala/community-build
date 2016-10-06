#!/usr/bin/env bash

# This script is suitable for local use.
# It is also invoked by Jenkins (from scripts/jobs/integrate/community-build).

# usage: ./run.sh [<dbuild-options>]

set -e
set -o pipefail

export LANG="en_US.UTF-8"
export HOME="$(pwd)"

DBUILDVERSION=0.9.5
echo "dbuild version: $DBUILDVERSION"

DBUILDCONFIG=community.dbuild
echo "dbuild config file: $DBUILDCONFIG"

if [ ! -f "$DBUILDCONFIG" ]
then
  echo "File not found: $DBUILDCONFIG"
  exit 1
fi

if [ ! -d "dbuild-${DBUILDVERSION}" ]
then
  wget "http://repo.typesafe.com/typesafe/ivy-releases/com.typesafe.dbuild/dbuild/${DBUILDVERSION}/tgzs/dbuild-${DBUILDVERSION}.tgz"
  tar xfz "dbuild-${DBUILDVERSION}.tgz"
  rm "dbuild-${DBUILDVERSION}.tgz"
fi

# sigh, Ubuntu has nodejs but OS X has node
if hash nodejs 2>/dev/null; then
    export NODE=nodejs
else
    export NODE=node
fi

# use -n since running locally you don't want notifications sent,
# and on our Jenkins setup it doesn't actually work (for now anyway)

echo "dbuild-${DBUILDVERSION}/bin/dbuild" -n "${@}" "$DBUILDCONFIG"
"dbuild-${DBUILDVERSION}/bin/dbuild" -n "${@}" "$DBUILDCONFIG" 2>&1 | tee "dbuild-${DBUILDVERSION}/dbuild.out"
STATUS="$?"
BUILD_ID="$(grep '^\[info\]  uuid = ' "dbuild-${DBUILDVERSION}/dbuild.out" | sed -e 's/\[info\]  uuid = //')"
echo "The repeatable UUID of this build was: ${BUILD_ID}"
exit "$STATUS"
