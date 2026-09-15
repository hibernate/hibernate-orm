#! /bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

source "$DIR/db-params.sh"

logAndExec ./gradlew ciCheck ${goal} "${@}" -Plog-test-progress=true --stacktrace  --refresh-dependencies
