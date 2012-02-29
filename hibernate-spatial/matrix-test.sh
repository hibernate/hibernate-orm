#! /bin/bash

TASK=matrix
if [[ -n $@ ]]; then
  TASK="$@"
fi

DBS=`pwd`/databases
echo "Database configurations read from : $DBS"
echo "TASK : $TASK"
../gradlew -Dhibernate-matrix-databases=$DBS -Dhibernate-matrix-ignore='mysql50,mysql51,postgresql82,postgresql83,postgresql84' $TASK
