#! /bin/bash

goal=
if [ "$RDBMS" == "derby" ]; then
  goal="-Pdb=derby"
elif [ "$RDBMS" == "mariadb" ]; then
  goal="-Pdb=mariadb"
elif [ "$RDBMS" == "postgresql" ]; then
  goal="-Pdb=pgsql"
elif [ "$RDBMS" == "oracle" ]; then
  goal="-Pdb=oracle -Dhibernate.connection.url=jdbc:oracle:thin:@localhost:1521:XE -Dhibernate.connection.username=SYSTEM -Dhibernate.connection.password=Oracle18"
elif [ "$RDBMS" == "db2" ]; then
  goal="-Pdb=db2 -Dhibernate.connection.url=jdbc:db2://localhost:50000/orm_test -Dhibernate.connection.username=orm_test -Dhibernate.connection.password=orm_test"
elif [ "$RDBMS" == "mssql" ]; then
  goal="-Pdb=mssql -Dhibernate.connection.url=jdbc:sqlserver://localhost:1433;databaseName= -Dhibernate.connection.username=sa -Dhibernate.connection.password=hibernate_orm_test"
fi

exec ./gradlew check ${goal} -Plog-test-progress=true --stacktrace