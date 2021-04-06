#! /bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

if [ "$RDBMS" == 'mysql' ]; then
  bash $DIR/../docker_db.sh mysql_5_7
elif [ "$RDBMS" == 'mysql8' ]; then
  bash $DIR/../docker_db.sh mysql_8_0
elif [ "$RDBMS" == 'mariadb' ]; then
  bash $DIR/../docker_db.sh mariadb
elif [ "$RDBMS" == 'postgresql' ]; then
  bash $DIR/../docker_db.sh postgresql_9_5
elif [ "$RDBMS" == 'db2' ]; then
  bash $DIR/../docker_db.sh db2
elif [ "$RDBMS" == 'oracle' ]; then
  bash $DIR/../docker_db.sh oracle
elif [ "$RDBMS" == 'mssql' ]; then
  bash $DIR/../docker_db.sh mssql
elif [ "$RDBMS" == 'hana' ]; then
  bash $DIR/../docker_db.sh hana
fi