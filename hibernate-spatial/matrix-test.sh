#! /bin/bash

TASK=matrix
if [[ -n $@ ]]; then
  TASK="$@"
fi
echo "TASK : $TASK"
../gradlew -Dhibernate-matrix-databases="/home/maesenka/workspaces/github/hibernate-core-metamodel/hibernate-spatial/databases" -Dhibernate-matrix-ignore='mysql50,mysql51,postgresql82,postgresql83,postgresql84' $TASK
