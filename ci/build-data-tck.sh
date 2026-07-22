#! /bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

TCK_RUN=true

source "$DIR/db-params.sh"

logAndExec ./gradlew :hibernate-tck-data-runner:test ${goal} -Prundatatck=true "${@}" -Plog-test-progress=true --stacktrace
