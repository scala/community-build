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

strip0() {
  echo $(($(echo "$1" | sed 's/^0*//')))
}

if [ ! -d "dbuild-${DBUILDVERSION}" ]
then
  a=( ${DBUILDVERSION//./ } )
  maj=$(strip0 "${a[0]}")
  min=$(strip0 "${a[1]}")
  patch=$(strip0 "${a[2]}")
  if [[ ( $maj -lt 1 ) && ( $min -lt 9 ) ]]
  then
    # old location for dbuild <0.9.0 (stored on S3)
    wget "http://downloads.typesafe.com/dbuild/${DBUILDVERSION}/dbuild-${DBUILDVERSION}.tgz"
  else
    if [[ ( $maj -lt 1 ) && ( $min -eq 9 ) && ( $patch -lt 2) ]]
    then
      # new location for dbuild 0.9.[01] (regular artifact)
      wget "http://repo.typesafe.com/typesafe/temp-distributed-build-snapshots/com.typesafe.dbuild/dbuild/${DBUILDVERSION}/tgzs/dbuild-${DBUILDVERSION}.tgz"
    else
      # new location for dbuild >=0.9.2 (regular artifact)
      wget "http://repo.typesafe.com/typesafe/ivy-releases/com.typesafe.dbuild/dbuild/${DBUILDVERSION}/tgzs/dbuild-${DBUILDVERSION}.tgz"
    fi
  fi
  tar xfz "dbuild-${DBUILDVERSION}.tgz"
  rm "dbuild-${DBUILDVERSION}.tgz"
  # work around https://github.com/typesafehub/dbuild/issues/175
  # until we can move to a newer version of dbuild that has the fix;
  # see https://github.com/scala/community-builds/issues/108 .
  # (this isn't a problem on Jenkins because the scala-ci maven repo
  # has everything under the sun cached; attempting to run dbuild
  # locally was when I hit this. - ST 9/4/2015
  sed -i.bak "s/typesafe.artifactoryonline.com/repo.typesafe.com/g"  "dbuild-${DBUILDVERSION}/bin/"*.properties
  sed -i.bak "s/scalasbt.artifactoryonline.com/repo.scala-sbt.org/g" "dbuild-${DBUILDVERSION}/bin/"*.properties
fi

echo "dbuild-${DBUILDVERSION}/bin/dbuild" "${@}" "$DBUILDCONFIG"
"dbuild-${DBUILDVERSION}/bin/dbuild" "${@}" "$DBUILDCONFIG" 2>&1 | tee "dbuild-${DBUILDVERSION}/dbuild.out"
STATUS="$?"
BUILD_ID="$(grep '^\[info\]  uuid = ' "dbuild-${DBUILDVERSION}/dbuild.out" | sed -e 's/\[info\]  uuid = //')"
echo "The repeatable UUID of this build was: ${BUILD_ID}"
exit "$STATUS"
