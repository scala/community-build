#!/usr/bin/env bash

# This script is suitable for local use.
# It is also invoked by Jenkins (from scripts/jobs/integrate/community-build).

# usage examples:
#   ./run.sh
#   version=2.12.1-933bab2-nightly ./run.sh
#   version=2.12.1-933bab2-nightly ./run.sh project1
#   version=2.12.1-933bab2-nightly ./run.sh project1,project2,project3
# if no Scala version is specified, we use the last green run from
# https://scala-ci.typesafe.com/job/scala-2.13.x-integrate-community-build/

set -e
set -o pipefail

export LANG="en_US.UTF-8"
export HOME="$(pwd)"

function latestNightly() {
  >&2 echo "No Scala version specified. Using latest nightly."
  url='https://scala-ci.typesafe.com/job/scala-2.13.x-integrate-bootstrap/lastSuccessfulBuild/artifact/jenkins.properties/*view*/'
  curl -f -s -L $url | grep ^version= | cut -d= -f2
}

export version=${version-`latestNightly`}
echo "re-run as:"
echo version=$version ./run.sh ${@}

DBUILDVERSION=0.9.7-RC1
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

echo "dbuild-${DBUILDVERSION}/bin/dbuild" -n "$DBUILDCONFIG" "${@}"
"dbuild-${DBUILDVERSION}/bin/dbuild" -n "$DBUILDCONFIG" "${@}" 2>&1 | tee "dbuild-${DBUILDVERSION}/dbuild.out"
STATUS="$?"
BUILD_ID="$(grep '^\[info\]  uuid = ' "dbuild-${DBUILDVERSION}/dbuild.out" | sed -e 's/\[info\]  uuid = //')"
echo "The repeatable UUID of this build was: ${BUILD_ID}"
exit "$STATUS"
