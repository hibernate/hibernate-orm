#! /bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

source "$DIR/db-params.sh"

if [ "$dbParam" != '' ]; then
  bash $DIR/../db.sh $dbParam
#  # Copy the jConnect driver out of the docker container to use it for the build
#  if [ "$RDBMS" == "sybase" ]; then
#    if command -v docker > /dev/null; then
#      CONTAINER_CLI=$(command -v docker)
#    elif command -v podman > /dev/null; then
#      CONTAINER_CLI=$(command -v podman)
#    else
#      echo "ERROR: Neither docker nor podman found on PATH"
#      exit 1
#    fi
#
#    $CONTAINER_CLI cp ${containerName}:/opt/sybase/jConnect-16_0/classes/jconn4.jar $DIR/../drivers/jconn4.jar
#  fi
fi