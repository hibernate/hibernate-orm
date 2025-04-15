#! /bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

java -version
if [ "$RDBMS" == "gaussdb" ]; then
  export EXTRA_GRADLE_ARGS="-x :hibernate-agroal:test"
fi
exec bash $DIR/build.sh