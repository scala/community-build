#!/usr/bin/env bash

# This script is suitable for local use.
# It is also invoked by Jenkins (from scripts/jobs/integrate/community-build).

set -e
set -o pipefail

# redundant to delete both at start and end, but just in case
# these were left lying around...
echo "removing temporary files..."
rm -rf target-*/project-builds || true

export LANG="en_US.UTF-8"
export HOME="$(pwd)"

# Defaults
scala_version_default="2.13.1-bin-a0244b8"  # Jul 15
scala_version="$scala_version_default"
root_dir=$(pwd)
config_dir="configs"
dbuild_file="$config_dir/community.dbuild"
project_refs_conf="$config_dir/project-refs.conf"
resolvers_file_default="$config_dir/resolvers.conf"
resolvers_file=$resolvers_file_default
debug="false"
jvm_props=""
local_mode="false"
notify="false"
dbuild_args=""

Usage(){
    ex=$1
    shift
echo >&2 "Usage: `basename $0` [<options>] [project,projects...]
Function: Downloads dbuild if not already installed locally and runs dbuild with defined options.

Options:
  -c <dir>           directory for the configuration
                     This resets  -f, -p and -r options so specify this first: default $config_dir
  -d                 print more debugging information
  -D <key=value>...  one or more Java-style properties: default $jvm_props
  -f <file>          dbuild file: default $dbuild_file
  -h                 prints this help message and exits
  -l                 local mode, equivalent to  '-r none': default $local_mode
  -n                 enable notifications defined in the configuration files: default $notify
  -p <file>          project refs file: default $project_refs_conf
  -r <file>          resolvers file, if 'none' then disable parsing: default $resolvers_file
  -v <version>       scala version, can be overridden by 'version=2.12.1 ./run.sh' : default latest nightly

Examples:
  ./run.sh
  ./run.sh -c configs-tls
  version=2.12 ./run.sh
  version=2.12.1-bin-933bab2 ./run.sh project1
  version=2.12.1-bin-933bab2 ./run.sh project1,project2,project3

If no Scala version is specified, we use whatever's hardcoded in run.sh.
"
  echo >&2 "$@"
  exit $ex
}


while getopts c:dD:f:hlnp:r:s:v: c; do
  case $c in
    c) config_dir="$OPTARG"
       dbuild_file="$config_dir/community.dbuild"
       project_refs_conf="$config_dir/project-refs.conf"
       resolvers_file_default="$config_dir/resolvers.conf"
       resolvers_file=$resolvers_file_default
       ;;
    d) debug="true"
       ;;
    D) jvm_props="$OPTARG"
       ;;
    f) dbuild_file="$OPTARG"
       ;;
    h) Usage 0
       ;;
    l) local_mode="true"
       ;;
    n) notify="true"
       ;;
    p) project_refs_conf="$OPTARG"
       ;;
    r) resolvers_file="$OPTARG"
       ;;
    v) scala_version="$OPTARG"
       ;;
  esac
done
shift `expr $OPTIND - 1`

if [ "$debug" = "true" ]; then
  echo "
cbuild settings:
  config directory:       $config_dir
  debug:                  $debug
  dbuild file:            $dbuild_file
  dbuild_args:            ${@}
  jvm_props:              $jvm_props
  local_mode:             $local_mode
  notify:                 $notify
  project refs conf:      $project_refs_conf
  resolvers_file_default: $resolvers_file_default
  resolvers_file          $resolvers_file
  root directory:         $root_dir
  scala_version_default:  $scala_version_default
  scala_version:          $scala_version
"
fi

export version=${version-$scala_version}
if [ "$version" = "$scala_version_default" ]; then
 >&2 echo "No Scala version specified. Using latest nightly."
fi

echo "re-run as:"
echo version=$version ./run.sh ${@}

# In a config, we cannot use a variable in an include statement, so have a workaround
# whereby we copy the contents of the real file to a temporary file of known name.
mkdir -p .dbuild
if [ "$resolvers_file" = "none" ]; then
 cat $resolvers_file_default > .dbuild/resolvers.conf
else
  cat $resolvers_file > .dbuild/resolvers.conf
fi
cat $project_refs_conf > .dbuild/project-refs.conf

# Set dbuild version and config file
DBUILDVERSION=0.9.16
echo "dbuild version: $DBUILDVERSION"

DBUILDCONFIG=$dbuild_file
echo "dbuild config file: $DBUILDCONFIG"

if [ ! -f "$DBUILDCONFIG" ]
then
  echo "File not found: $DBUILDCONFIG"
  exit 1
fi

# Download and extract dbuild if we haven't already got it
if [ ! -d "dbuild-${DBUILDVERSION}" ]
then
  wget "http://repo.lightbend.com/typesafe/ivy-releases/com.typesafe.dbuild/dbuild/${DBUILDVERSION}/tgzs/dbuild-${DBUILDVERSION}.tgz"
  tar xfz "dbuild-${DBUILDVERSION}.tgz"
  rm "dbuild-${DBUILDVERSION}.tgz"
fi

# sigh, Ubuntu has nodejs but OS X has node
if hash nodejs 2>/dev/null; then
    export NODE=nodejs
else
    export NODE=node
fi

# Set the options we want to pass into dbuild
if [ "$debug" = "true" ];then
  dbuild_args="$dbuild_args -d"
fi

if [ -n "$jvm_props" ];then
  dbuild_args="$dbuild_args -Dkey=\"$jvm_props\""
fi

if [ "$local_mode" = "true" ];then
  dbuild_args="$dbuild_args -l"
else
  if [ "$resolvers_file" = "none" ]; then
    dbuild_args="$dbuild_args -r"
  fi
  # use -n by default since running locally you don't want notifications sent,
  # and on our Jenkins setup it doesn't actually work (for now anyway)
  if [ "$notify" = "false" ];then
    dbuild_args="$dbuild_args -n"
  fi
fi

# And finally, call dbuild
echo "dbuild-${DBUILDVERSION}/bin/dbuild"  "$dbuild_args" "$DBUILDCONFIG" "${@}"
("dbuild-${DBUILDVERSION}/bin/dbuild"  "$dbuild_args" "$DBUILDCONFIG" "${@}" 2>&1 | tee "dbuild-${DBUILDVERSION}/dbuild.out") || STATUS="$?"
BUILD_ID="$(grep '^\[info\]  uuid = ' "dbuild-${DBUILDVERSION}/dbuild.out" | sed -e 's/\[info\]  uuid = //')"
echo "The repeatable UUID of this build was: ${BUILD_ID}"

# we may have run out of disk, so make space ASAP
echo "removing temporary files..."
rm -rf target-*/project-builds

# report summary information (line counts, green project counts, ...?)
cd report
sbt -Dsbt.supershell=false -error "run ../dbuild-${DBUILDVERSION}/dbuild.out"

# we've captured $STATUS above, but in this version of the script, it isn't used,
# instead the reporting stuff is in charge of calling sys.exit if it decides to
# exit $STATUS
