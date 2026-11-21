#!/bin/bash
# Official TimesTen images only provide an instance: no database is created and the daemon is not started.
# Start the daemon (which also starts the client/server listener on port 6625), create the `hibernate_orm_test`
# database, and a user with the same name, then keep the container running.
set -e

. /timesten/instance1/bin/ttenv.sh > /dev/null

ttDaemonAdmin -start

sed -i 's/^\[ODBC Data Sources\]$/[ODBC Data Sources]\nhibernate_orm_test=TimesTen 22.1 Driver/' "$TIMESTEN_HOME/conf/sys.odbc.ini"
cat >> "$TIMESTEN_HOME/conf/sys.odbc.ini" <<DSN

[hibernate_orm_test]
Driver=$TIMESTEN_HOME/install/lib/libtten.so
DataStore=$TIMESTEN_HOME/databases/hibernate_orm_test
PermSize=128
TempSize=64
DatabaseCharacterSet=AL32UTF8
DSN
mkdir -p "$TIMESTEN_HOME/databases"

ttIsql -v 1 -connstr "DSN=hibernate_orm_test;AutoCreate=1" <<SQL
create user hibernate_orm_test identified by hibernate_orm_test;
grant all to hibernate_orm_test;
exit;
SQL

exec sleep infinity
