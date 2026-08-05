#! /bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

source "$DIR/db-params.sh"

if [ "$dbParam" != '' ]; then
  bash $DIR/../db.sh $dbParam
fi