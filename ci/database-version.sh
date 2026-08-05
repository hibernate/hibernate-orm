#! /bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

source "$DIR/db-params.sh"

###############################################################################
# Detect container cli (Docker/Podman)
# Docker has priority to make CI builds more stable/predictable
# (Jenkins is currently better configured to deal with Docker)
###############################################################################
if command -v docker > /dev/null; then
  CONTAINER_CLI=$(command -v docker)
  if [[ "$(docker version | grep Podman)" == "" ]]; then
    IS_DOCKER_RUNTIME=true
    IS_PODMAN=false
  else
    IS_DOCKER_RUNTIME=false
    IS_PODMAN=true
  fi
elif command -v podman > /dev/null; then
  CONTAINER_CLI=$(command -v podman)
  IS_DOCKER_RUNTIME=false
  IS_PODMAN=true
else
  echo "ERROR: Neither docker nor podman found on PATH"
  exit 1
fi

if [ "$containerName" != '' ]; then
  $CONTAINER_CLI inspect --format='{{index .RepoDigests 0}}' $($CONTAINER_CLI inspect --format='{{.Config.Image}}' $containerName)
fi