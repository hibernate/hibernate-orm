#! /bin/bash

if command -v podman > /dev/null; then
  CONTAINER_CLI=$(command -v podman)
  HEALTCHECK_PATH="{{.State.Healthcheck.Status}}"
  # Only use sudo for podman
  if command -v sudo > /dev/null; then
    PRIVILEGED_CLI="sudo"
  else
    PRIVILEGED_CLI=""
  fi
else
  CONTAINER_CLI=$(command -v docker)
  HEALTCHECK_PATH="{{.State.Health.Status}}"
  PRIVILEGED_CLI=""
fi

mysql() {
  mysql_8_1
}

mysql_5_7() {
    $CONTAINER_CLI rm -f mysql || true
    $CONTAINER_CLI run --name mysql -e MYSQL_USER=hibernate_orm_test -e MYSQL_PASSWORD=hibernate_orm_test -e MYSQL_DATABASE=hibernate_orm_test -e MYSQL_ROOT_PASSWORD=hibernate_orm_test -p3306:3306 -d ${DB_IMAGE_MYSQL_5_7:-docker.io/mysql:5.7.43} --character-set-server=utf8mb4 --collation-server=utf8mb4_bin --skip-character-set-client-handshake --log-bin-trust-function-creators=1
    # Give the container some time to start
    OUTPUT=
    n=0
    until [ "$n" -ge 5 ]
    do
        # Need to access STDERR. Thanks for the snippet https://stackoverflow.com/a/56577569/412446
        { OUTPUT="$( { $CONTAINER_CLI logs mysql; } 2>&1 1>&3 3>&- )"; } 3>&1;
        if [[ $OUTPUT == *"ready for connections"* ]]; then
          break;
        fi
        n=$((n+1))
        echo "Waiting for MySQL to start..."
        sleep 3
    done
    if [ "$n" -ge 5 ]; then
      echo "MySQL failed to start and configure after 15 seconds"
    else
      echo "MySQL successfully started"
    fi
}

mysql_8_0() {
    $CONTAINER_CLI rm -f mysql || true
    $CONTAINER_CLI run --name mysql -e MYSQL_USER=hibernate_orm_test -e MYSQL_PASSWORD=hibernate_orm_test -e MYSQL_ROOT_PASSWORD=hibernate_orm_test -e MYSQL_DATABASE=hibernate_orm_test -e MYSQL_ROOT_PASSWORD=hibernate_orm_test -p3306:3306 -d ${DB_IMAGE_MYSQL_8_0:-docker.io/mysql:8.0.31} --character-set-server=utf8mb4 --collation-server=utf8mb4_0900_as_cs --skip-character-set-client-handshake --log-bin-trust-function-creators=1
    # Give the container some time to start
    OUTPUT=
    n=0
    until [ "$n" -ge 5 ]
    do
        # Need to access STDERR. Thanks for the snippet https://stackoverflow.com/a/56577569/412446
        { OUTPUT="$( { $CONTAINER_CLI logs mysql; } 2>&1 1>&3 3>&- )"; } 3>&1;
        if [[ $OUTPUT == *"ready for connections"* ]]; then
          break;
        fi
        n=$((n+1))
        echo "Waiting for MySQL to start..."
        sleep 3
    done
    if [ "$n" -ge 5 ]; then
      echo "MySQL failed to start and configure after 15 seconds"
    else
      echo "MySQL successfully started"
    fi
}

mysql_8_1() {
    $CONTAINER_CLI rm -f mysql || true
    $CONTAINER_CLI run --name mysql -e MYSQL_USER=hibernate_orm_test -e MYSQL_PASSWORD=hibernate_orm_test -e MYSQL_ROOT_PASSWORD=hibernate_orm_test -e MYSQL_DATABASE=hibernate_orm_test -e MYSQL_ROOT_PASSWORD=hibernate_orm_test -p3306:3306 -d ${DB_IMAGE_MYSQL_8_1:-docker.io/mysql:8.1.0} --character-set-server=utf8mb4 --collation-server=utf8mb4_0900_as_cs --skip-character-set-client-handshake --log-bin-trust-function-creators=1
    # Give the container some time to start
    OUTPUT=
    n=0
    until [ "$n" -ge 5 ]
    do
        # Need to access STDERR. Thanks for the snippet https://stackoverflow.com/a/56577569/412446
        { OUTPUT="$( { $CONTAINER_CLI logs mysql; } 2>&1 1>&3 3>&- )"; } 3>&1;
        if [[ $OUTPUT == *"ready for connections"* ]]; then
          break;
        fi
        n=$((n+1))
        echo "Waiting for MySQL to start..."
        sleep 3
    done
    if [ "$n" -ge 5 ]; then
      echo "MySQL failed to start and configure after 15 seconds"
    else
      echo "MySQL successfully started"
    fi
}

mariadb() {
  mariadb_11_1
}

mariadb_10_3() {
    $CONTAINER_CLI rm -f mariadb || true
    $CONTAINER_CLI run --name mariadb -e MYSQL_USER=hibernate_orm_test -e MYSQL_PASSWORD=hibernate_orm_test -e MYSQL_DATABASE=hibernate_orm_test -e MYSQL_ROOT_PASSWORD=hibernate_orm_test -p3306:3306 -d ${DB_IMAGE_MARIADB_10_3:-docker.io/mariadb:10.3.39} --character-set-server=utf8mb4 --collation-server=utf8mb4_bin --skip-character-set-client-handshake
    OUTPUT=
    n=0
    until [ "$n" -ge 5 ]
    do
        # Need to access STDERR. Thanks for the snippet https://stackoverflow.com/a/56577569/412446
        { OUTPUT="$( { $CONTAINER_CLI logs mariadb; } 2>&1 1>&3 3>&- )"; } 3>&1;
        if [[ $OUTPUT == *"ready for connections"* ]]; then
          break;
        fi
        n=$((n+1))
        echo "Waiting for MariaDB to start..."
        sleep 3
    done
    if [ "$n" -ge 5 ]; then
      echo "MariaDB failed to start and configure after 15 seconds"
    else
      echo "MariaDB successfully started"
    fi
}

mariadb_10_9() {
    $CONTAINER_CLI rm -f mariadb || true
    $CONTAINER_CLI run --name mariadb -e MYSQL_USER=hibernate_orm_test -e MYSQL_PASSWORD=hibernate_orm_test -e MYSQL_DATABASE=hibernate_orm_test -e MYSQL_ROOT_PASSWORD=hibernate_orm_test -p3306:3306 -d ${DB_IMAGE_MARIADB_10_9:-docker.io/mariadb:10.9.3} --character-set-server=utf8mb4 --collation-server=utf8mb4_bin --skip-character-set-client-handshake
    OUTPUT=
    n=0
    until [ "$n" -ge 5 ]
    do
        # Need to access STDERR. Thanks for the snippet https://stackoverflow.com/a/56577569/412446
        { OUTPUT="$( { $CONTAINER_CLI logs mariadb; } 2>&1 1>&3 3>&- )"; } 3>&1;
        if [[ $OUTPUT == *"ready for connections"* ]]; then
          break;
        fi
        n=$((n+1))
        echo "Waiting for MariaDB to start..."
        sleep 3
    done
    if [ "$n" -ge 5 ]; then
      echo "MariaDB failed to start and configure after 15 seconds"
    else
      echo "MariaDB successfully started"
    fi
}

mariadb_11_1() {
    $CONTAINER_CLI rm -f mariadb || true
    $CONTAINER_CLI run --name mariadb -e MYSQL_USER=hibernate_orm_test -e MYSQL_PASSWORD=hibernate_orm_test -e MYSQL_DATABASE=hibernate_orm_test -e MYSQL_ROOT_PASSWORD=hibernate_orm_test -p3306:3306 -d ${DB_IMAGE_MARIADB_11_1:-docker.io/mariadb:11.1.2} --character-set-server=utf8mb4 --collation-server=utf8mb4_bin --skip-character-set-client-handshake
    OUTPUT=
    n=0
    until [ "$n" -ge 5 ]
    do
        # Need to access STDERR. Thanks for the snippet https://stackoverflow.com/a/56577569/412446
        { OUTPUT="$( { $CONTAINER_CLI logs mariadb; } 2>&1 1>&3 3>&- )"; } 3>&1;
        if [[ $OUTPUT == *"ready for connections"* ]]; then
          break;
        fi
        n=$((n+1))
        echo "Waiting for MariaDB to start..."
        sleep 3
    done
    if [ "$n" -ge 5 ]; then
      echo "MariaDB failed to start and configure after 15 seconds"
    else
      echo "MariaDB successfully started"
    fi
}

postgresql() {
  postgresql_15
}

postgresql_9_5() {
    $CONTAINER_CLI rm -f postgres || true
    $CONTAINER_CLI run --name postgres -e POSTGRES_USER=hibernate_orm_test -e POSTGRES_PASSWORD=hibernate_orm_test -e POSTGRES_DB=hibernate_orm_test -p5432:5432 -d ${DB_IMAGE_POSTGRESQL_9_5:-docker.io/postgis/postgis:9.5-2.5}
}

postgresql_10() {
    $CONTAINER_CLI rm -f postgres || true
    $CONTAINER_CLI run --name postgres -e POSTGRES_USER=hibernate_orm_test -e POSTGRES_PASSWORD=hibernate_orm_test -e POSTGRES_DB=hibernate_orm_test -p5432:5432 -d ${DB_IMAGE_POSTGRESQL_10:-docker.io/postgis/postgis:10-2.5}
}

postgresql_13() {
    $CONTAINER_CLI rm -f postgres || true
    $CONTAINER_CLI run --name postgres -e POSTGRES_USER=hibernate_orm_test -e POSTGRES_PASSWORD=hibernate_orm_test -e POSTGRES_DB=hibernate_orm_test -p5432:5432 -d ${DB_IMAGE_POSTGRESQL_13:-docker.io/postgis/postgis:13-3.1}
}

postgresql_14() {
    $CONTAINER_CLI rm -f postgres || true
    $CONTAINER_CLI run --name postgres -e POSTGRES_USER=hibernate_orm_test -e POSTGRES_PASSWORD=hibernate_orm_test -e POSTGRES_DB=hibernate_orm_test -p5432:5432 -d ${DB_IMAGE_POSTGRESQL_14:-docker.io/postgis/postgis:14-3.3}
}

postgresql_15() {
    $CONTAINER_CLI rm -f postgres || true
    $CONTAINER_CLI run --name postgres -e POSTGRES_USER=hibernate_orm_test -e POSTGRES_PASSWORD=hibernate_orm_test -e POSTGRES_DB=hibernate_orm_test -p5432:5432 --tmpfs /pgtmpfs:size=131072k -d ${DB_IMAGE_POSTGRESQL_15:-docker.io/postgis/postgis:15-3.3} \
      -c fsync=off -c synchronous_commit=off -c full_page_writes=off -c shared_buffers=256MB -c maintenance_work_mem=256MB -c max_wal_size=1GB -c checkpoint_timeout=1d
}

edb() {
    edb_15
}

edb_10() {
    $CONTAINER_CLI rm -f edb || true
    # We need to build a derived image because the existing image is mainly made for use by a kubernetes operator
    (cd edb; $CONTAINER_CLI build -t edb-test:10 -f edb10.Dockerfile .)
    $CONTAINER_CLI run --name edb -e POSTGRES_USER=hibernate_orm_test -e POSTGRES_PASSWORD=hibernate_orm_test -e POSTGRES_DB=hibernate_orm_test -p 5444:5444 -d edb-test:10
}

edb_14() {
    $CONTAINER_CLI rm -f edb || true
    # We need to build a derived image because the existing image is mainly made for use by a kubernetes operator
    (cd edb; $CONTAINER_CLI build -t edb-test:14 -f edb14.Dockerfile .)
    $CONTAINER_CLI run --name edb -e POSTGRES_USER=hibernate_orm_test -e POSTGRES_PASSWORD=hibernate_orm_test -e POSTGRES_DB=hibernate_orm_test -p 5444:5444 -d edb-test:14
}

edb_15() {
    $CONTAINER_CLI rm -f edb || true
    # We need to build a derived image because the existing image is mainly made for use by a kubernetes operator
    (cd edb; $CONTAINER_CLI build -t edb-test:15 -f edb15.Dockerfile .)
    $CONTAINER_CLI run --name edb -e POSTGRES_USER=hibernate_orm_test -e POSTGRES_PASSWORD=hibernate_orm_test -e POSTGRES_DB=hibernate_orm_test -p 5444:5444 -d edb-test:15
}

db2() {
  db2_11_5
}

db2_11_5() {
    $PRIVILEGED_CLI $CONTAINER_CLI rm -f db2 || true
    $PRIVILEGED_CLI $CONTAINER_CLI run --name db2 --privileged -e DB2INSTANCE=orm_test -e DB2INST1_PASSWORD=orm_test -e DBNAME=orm_test -e LICENSE=accept -e AUTOCONFIG=false -e ARCHIVE_LOGS=false -e TO_CREATE_SAMPLEDB=false -e REPODB=false -p 50000:50000 -d icr.io/db2_community/db2:11.5.9.0
    # Give the container some time to start
    OUTPUT=
    while [[ $OUTPUT != *"INSTANCE"* ]]; do
        echo "Waiting for DB2 to start..."
        sleep 10
        OUTPUT=$($PRIVILEGED_CLI $CONTAINER_CLI logs db2 2>&1)
    done
    $PRIVILEGED_CLI $CONTAINER_CLI exec -t db2 su - orm_test bash -c ". /database/config/orm_test/sqllib/db2profile; /database/config/orm_test/sqllib/bin/db2 'connect to orm_test'; /database/config/orm_test/sqllib/bin/db2 'CREATE USER TEMPORARY TABLESPACE usr_tbsp MANAGED BY AUTOMATIC STORAGE'"
}

db2_10_5() {
    $PRIVILEGED_CLI $CONTAINER_CLI rm -f db2 || true
    # The sha represents the tag 10.5.0.5-3.10.0
    $PRIVILEGED_CLI $CONTAINER_CLI run --name db2 --privileged -e DB2INST1_PASSWORD=db2inst1-pwd -e LICENSE=accept -p 50000:50000 -d ${DB_IMAGE_DB2_10_5:-docker.io/ibmoms/db2express-c@sha256:a499afd9709a1f69fb41703e88def9869955234c3525547e2efc3418d1f4ca2b} db2start
    # Give the container some time to start
    OUTPUT=
    while [[ $OUTPUT != *"DB2START"* ]]; do
        echo "Waiting for DB2 to start..."
        sleep 10
        OUTPUT=$($PRIVILEGED_CLI $CONTAINER_CLI logs db2 2>&1)
    done
    $PRIVILEGED_CLI $CONTAINER_CLI exec -t db2 su - db2inst1 bash -c "/home/db2inst1/sqllib/bin/db2 create database orm_test &&
    /home/db2inst1/sqllib/bin/db2 'connect to orm_test' &&
    /home/db2inst1/sqllib/bin/db2 'CREATE BUFFERPOOL BP8K pagesize 8K' &&
    /home/db2inst1/sqllib/bin/db2 'CREATE SYSTEM TEMPORARY TABLESPACE STB_8 PAGESIZE 8K BUFFERPOOL BP8K' &&
    /home/db2inst1/sqllib/bin/db2 'CREATE BUFFERPOOL BP16K pagesize 16K' &&
    /home/db2inst1/sqllib/bin/db2 'CREATE SYSTEM TEMPORARY TABLESPACE STB_16 PAGESIZE 16K BUFFERPOOL BP16K' &&
    /home/db2inst1/sqllib/bin/db2 'CREATE BUFFERPOOL BP32K pagesize 32K' &&
    /home/db2inst1/sqllib/bin/db2 'CREATE SYSTEM TEMPORARY TABLESPACE STB_32 PAGESIZE 32K BUFFERPOOL BP32K' &&
    /home/db2inst1/sqllib/bin/db2 'CREATE USER TEMPORARY TABLESPACE usr_tbsp MANAGED BY AUTOMATIC STORAGE'"
}

db2_spatial() {
    $PRIVILEGED_CLI $CONTAINER_CLI rm -f db2spatial || true
    temp_dir=$(mktemp -d)
    cat <<EOF >${temp_dir}/ewkt.sql
create or replace function db2gse.asewkt(geometry db2gse.st_geometry)
returns clob(2G)
specific db2gse.asewkt1
language sql
deterministic
no external action
reads sql data
return 'srid=' || varchar(db2gse.st_srsid(geometry)) || ';' || db2gse.st_astext(geometry)
;

-- Create SQL function to create a geometry from EWKT format
create or replace function db2gse.geomfromewkt(instring varchar(32000))
returns db2gse.st_geometry
specific db2gse.fromewkt1
language sql
deterministic
no external action
reads sql data
return db2gse.st_geometry(
substr(instring,posstr(instring,';')+1, length(instring) - posstr(instring,';')),
integer(substr(instring,posstr(instring,'=')+1,posstr(instring,';')-(posstr(instring,'=')+1)))
)
;
-- Create a DB2 transform group to return and accept EWKT
CREATE TRANSFORM FOR db2gse.ST_Geometry EWKT (
       FROM SQL WITH FUNCTION db2gse.asewkt(db2gse.ST_Geometry),
       TO   SQL WITH FUNCTION db2gse.geomfromewkt(varchar(32000)) )
	;

-- Redefine the default DB2_PROGRAM to return and accept EWKT instead of WKT
DROP TRANSFORM DB2_PROGRAM FOR db2gse.ST_Geometry;
CREATE TRANSFORM FOR db2gse.ST_Geometry DB2_PROGRAM (
       FROM SQL WITH FUNCTION db2gse.asewkt(db2gse.ST_Geometry),
       TO   SQL WITH FUNCTION db2gse.geomfromewkt(varchar(32000)) )
;
EOF
    $PRIVILEGED_CLI $CONTAINER_CLI run --name db2spatial --privileged -e DB2INSTANCE=orm_test -e DB2INST1_PASSWORD=orm_test -e DBNAME=orm_test -e LICENSE=accept -e AUTOCONFIG=false -e ARCHIVE_LOGS=false -e TO_CREATE_SAMPLEDB=false -e REPODB=false \
        -v ${temp_dir}:/conf  \
        -p 50000:50000 -d ${DB_IMAGE_DB2_SPATIAL:-docker.io/ibmcom/db2:11.5.5.0}

    # Give the container some time to start
    OUTPUT=
    while [[ $OUTPUT != *"Setup has completed."* ]]; do
        echo "Waiting for DB2 to start..."
        sleep 10
        OUTPUT=$($PRIVILEGED_CLI $CONTAINER_CLI logs db2spatial 2>&1)
    done
    sleep 10
    echo "Enabling spatial extender"
    $PRIVILEGED_CLI $CONTAINER_CLI exec -t db2spatial su - orm_test bash -c "/database/config/orm_test/sqllib/db2profile && /database/config/orm_test/sqllib/bin/db2se enable_db orm_test"
    echo "Installing required transform group"
    $PRIVILEGED_CLI $CONTAINER_CLI exec -t db2spatial su - orm_test bash -c "/database/config/orm_test/sqllib/db2profile && /database/config/orm_test/sqllib/bin/db2 'connect to orm_test' && /database/config/orm_test/sqllib/bin/db2 -tvf /conf/ewkt.sql"

}

mssql() {
  mssql_2022
}

mssql_2017() {
    $CONTAINER_CLI rm -f mssql || true
    #This sha256 matches a specific tag of mcr.microsoft.com/mssql/server:2017-latest :
    $CONTAINER_CLI run --name mssql -d -p 1433:1433 -e "SA_PASSWORD=Hibernate_orm_test" -e ACCEPT_EULA=Y ${DB_IMAGE_MSSQL_2017:-mcr.microsoft.com/mssql/server@sha256:7d194c54e34cb63bca083542369485c8f4141596805611e84d8c8bab2339eede}
    sleep 5
    n=0
    until [ "$n" -ge 5 ]
    do
        # We need a database that uses a non-lock based MVCC approach
        # https://github.com/microsoft/homebrew-mssql-release/issues/2#issuecomment-682285561
        $CONTAINER_CLI exec mssql bash -c 'echo "create database hibernate_orm_test collate SQL_Latin1_General_CP1_CS_AS; alter database hibernate_orm_test set READ_COMMITTED_SNAPSHOT ON" | /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P Hibernate_orm_test -i /dev/stdin' && break
        echo "Waiting for SQL Server to start..."
        n=$((n+1))
        sleep 5
    done
    if [ "$n" -ge 5 ]; then
      echo "SQL Server failed to start and configure after 25 seconds"
    else
      echo "SQL Server successfully started"
    fi
}

mssql_2022() {
    $CONTAINER_CLI rm -f mssql || true
    #This sha256 matches a specific tag of mcr.microsoft.com/mssql/server:2022-latest :
    $CONTAINER_CLI run --name mssql -d -p 1433:1433 -e "SA_PASSWORD=Hibernate_orm_test" -e ACCEPT_EULA=Y ${DB_IMAGE_MSSQL_2022:-mcr.microsoft.com/mssql/server@sha256:b94071acd4612bfe60a73e265097c2b6388d14d9d493db8f37cf4479a4337480}
    sleep 5
    n=0
    until [ "$n" -ge 5 ]
    do
        # We need a database that uses a non-lock based MVCC approach
        # https://github.com/microsoft/homebrew-mssql-release/issues/2#issuecomment-682285561
        $CONTAINER_CLI exec mssql bash -c 'echo "create database hibernate_orm_test collate SQL_Latin1_General_CP1_CS_AS; alter database hibernate_orm_test set READ_COMMITTED_SNAPSHOT ON" | /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P Hibernate_orm_test -i /dev/stdin' && break
        echo "Waiting for SQL Server to start..."
        n=$((n+1))
        sleep 5
    done
    if [ "$n" -ge 5 ]; then
      echo "SQL Server failed to start and configure after 25 seconds"
    else
      echo "SQL Server successfully started"
    fi
}

sybase() {
    $CONTAINER_CLI rm -f sybase || true
    # Yup, that sucks, but on ubuntu we need to use -T11889 as per: https://github.com/DataGrip/docker-env/issues/12
    $CONTAINER_CLI run -d -p 9000:5000 -p 9001:5001 --name sybase --entrypoint /bin/bash ${DB_IMAGE_SYBASE:-docker.io/nguoianphu/docker-sybase} -c "source /opt/sybase/SYBASE.sh
/opt/sybase/ASE-16_0/bin/dataserver \
-d/opt/sybase/data/master.dat \
-e/opt/sybase/ASE-16_0/install/MYSYBASE.log \
-c/opt/sybase/ASE-16_0/MYSYBASE.cfg \
-M/opt/sybase/ASE-16_0 \
-N/opt/sybase/ASE-16_0/sysam/MYSYBASE.properties \
-i/opt/sybase \
-sMYSYBASE \
-T11889
RET=\$?
exit 0
"

    sybase_check() {
    $CONTAINER_CLI exec sybase bash -c "source /opt/sybase/SYBASE.sh;
/opt/sybase/OCS-16_0/bin/isql -Usa -P myPassword -S MYSYBASE <<EOF
Select name from sysdatabases where status2 & 48 > 0
go
quit
EOF
"
}
    START_STATUS=0
    j=1
    while (( $j < 30 )); do
      echo "Waiting for Sybase to start..."
      sleep 1
      j=$((j+1))
      START_STATUS=$(sybase_check | grep '(0 rows affected)' | wc -c)
      if (( $START_STATUS > 0 )); then
        break
      fi
    done
    if (( $j == 30 )); then
      echo "Failed starting Sybase"
      $CONTAINER_CLI ps -a
      $CONTAINER_CLI logs sybase
      sybase_check
      exit 1
    fi

    export SYBASE_DB=hibernate_orm_test
    export SYBASE_USER=hibernate_orm_test
    export SYBASE_PASSWORD=hibernate_orm_test
    $CONTAINER_CLI exec sybase bash -c "source /opt/sybase/SYBASE.sh;
cat <<-EOSQL > init1.sql
use master
go
disk resize name='master', size='256m'
go
create database $SYBASE_DB on master = '96m'
go
sp_dboption $SYBASE_DB, \"single user\", true
go
alter database $SYBASE_DB log on master = '50m'
go
use $SYBASE_DB
go
exec sp_extendsegment logsegment, $SYBASE_DB, master
go
use master
go
sp_dboption $SYBASE_DB, \"single user\", false
go
use $SYBASE_DB
go
checkpoint
go
use master
go
create login $SYBASE_USER with password $SYBASE_PASSWORD
go
exec sp_dboption $SYBASE_DB, 'abort tran on log full', true
go
exec sp_dboption $SYBASE_DB, 'allow nulls by default', true
go
exec sp_dboption $SYBASE_DB, 'ddl in tran', true
go
exec sp_dboption $SYBASE_DB, 'trunc log on chkpt', true
go
exec sp_dboption $SYBASE_DB, 'full logging for select into', true
go
exec sp_dboption $SYBASE_DB, 'full logging for alter table', true
go
sp_dboption $SYBASE_DB, \"select into\", true
go
sp_dboption tempdb, 'ddl in tran', true
go
EOSQL

/opt/sybase/OCS-16_0/bin/isql -Usa -P myPassword -S MYSYBASE -i ./init1.sql

echo =============== CREATING DB ==========================
cat <<-EOSQL > init2.sql
use $SYBASE_DB
go
sp_adduser '$SYBASE_USER', '$SYBASE_USER', null
go
grant create default to $SYBASE_USER
go
grant create table to $SYBASE_USER
go
grant create view to $SYBASE_USER
go
grant create rule to $SYBASE_USER
go
grant create function to $SYBASE_USER
go
grant create procedure to $SYBASE_USER
go
commit
go
EOSQL

/opt/sybase/OCS-16_0/bin/isql -Usa -P myPassword -S MYSYBASE -i ./init2.sql"
    echo "Sybase successfully started"
}

oracle_setup() {
    HEALTHSTATUS=
    until [ "$HEALTHSTATUS" == "healthy" ];
    do
        echo "Waiting for Oracle to start..."
        sleep 5;
        # On WSL, health-checks intervals don't work for Podman, so run them manually
        if command -v podman > /dev/null; then
          $PRIVILEGED_CLI $CONTAINER_CLI healthcheck run oracle > /dev/null
        fi
        HEALTHSTATUS="`$PRIVILEGED_CLI $CONTAINER_CLI inspect -f $HEALTCHECK_PATH oracle`"
        HEALTHSTATUS=${HEALTHSTATUS##+( )} #Remove longest matching series of spaces from the front
        HEALTHSTATUS=${HEALTHSTATUS%%+( )} #Remove longest matching series of spaces from the back
    done
    sleep 2;
    echo "Oracle successfully started"
    # We increase file sizes to avoid online resizes as that requires lots of CPU which is restricted in XE
    $PRIVILEGED_CLI $CONTAINER_CLI exec oracle bash -c "source /home/oracle/.bashrc; bash -c \"
cat <<EOF | \$ORACLE_HOME/bin/sqlplus / as sysdba
set timing on
-- Remove DISABLE_OOB parameter from Listener configuration and restart it
!echo Enabling OOB for Listener...
!echo NAMES.DIRECTORY_PATH=\(EZCONNECT,TNSNAMES\) > /opt/oracle/oradata/dbconfig/XE/sqlnet.ora
!lsnrctl reload

-- Increasing redo logs
alter database add logfile group 4 '\$ORACLE_BASE/oradata/XE/redo04.log' size 500M reuse;
alter database add logfile group 5 '\$ORACLE_BASE/oradata/XE/redo05.log' size 500M reuse;
alter database add logfile group 6 '\$ORACLE_BASE/oradata/XE/redo06.log' size 500M reuse;
alter system switch logfile;
alter system switch logfile;
alter system switch logfile;
alter system checkpoint;
alter database drop logfile group 1;
alter database drop logfile group 2;
alter database drop logfile group 3;
!rm \$ORACLE_BASE/oradata/XE/redo01.log
!rm \$ORACLE_BASE/oradata/XE/redo02.log
!rm \$ORACLE_BASE/oradata/XE/redo03.log

-- Increasing SYSAUX data file
alter database datafile '\$ORACLE_BASE/oradata/XE/sysaux01.dbf' resize 600M;

-- Modifying database init parameters
alter system set open_cursors=1000 sid='*' scope=both;
alter system set session_cached_cursors=500 sid='*' scope=spfile;
alter system set db_securefile=ALWAYS sid='*' scope=spfile;
alter system set dispatchers='(PROTOCOL=TCP)(SERVICE=XEXDB)(DISPATCHERS=0)' sid='*' scope=spfile;
alter system set recyclebin=OFF sid='*' SCOPE=SPFILE;

-- Comment the 2 next lines to be able to use Diagnostics Pack features
alter system set sga_target=0m sid='*' scope=both;
-- alter system set statistics_level=BASIC sid='*' scope=spfile;

-- Restart the database
SHUTDOWN IMMEDIATE;
STARTUP MOUNT;
ALTER DATABASE OPEN;

-- Switch to the XEPDB1 pluggable database
alter session set container=xepdb1;

-- Modify XEPDB1 datafiles and tablespaces
alter database datafile '\$ORACLE_BASE/oradata/XE/XEPDB1/system01.dbf' resize 320M;
alter database datafile '\$ORACLE_BASE/oradata/XE/XEPDB1/sysaux01.dbf' resize 360M;
alter database datafile '\$ORACLE_BASE/oradata/XE/XEPDB1/undotbs01.dbf' resize 400M;
alter database datafile '\$ORACLE_BASE/oradata/XE/XEPDB1/undotbs01.dbf' autoextend on next 16M;
alter database tempfile '\$ORACLE_BASE/oradata/XE/XEPDB1/temp01.dbf' resize 400M;
alter database tempfile '\$ORACLE_BASE/oradata/XE/XEPDB1/temp01.dbf' autoextend on next 16M;
alter database datafile '\$ORACLE_BASE/oradata/XE/XEPDB1/users01.dbf' resize 100M;
alter database datafile '\$ORACLE_BASE/oradata/XE/XEPDB1/users01.dbf' autoextend on next 16M;
alter tablespace USERS nologging;
alter tablespace SYSTEM nologging;
alter tablespace SYSAUX nologging;

create user hibernate_orm_test identified by hibernate_orm_test quota unlimited on users;
grant all privileges to hibernate_orm_test;
EOF\""
}

oracle_free_setup() {
    HEALTHSTATUS=
    until [ "$HEALTHSTATUS" == "healthy" ];
    do
        echo "Waiting for Oracle Free to start..."
        sleep 5;
        # On WSL, health-checks intervals don't work for Podman, so run them manually
        if command -v podman > /dev/null; then
          $PRIVILEGED_CLI $CONTAINER_CLI healthcheck run oracle > /dev/null
        fi
        HEALTHSTATUS="`$PRIVILEGED_CLI $CONTAINER_CLI inspect -f $HEALTCHECK_PATH oracle`"
        HEALTHSTATUS=${HEALTHSTATUS##+( )} #Remove longest matching series of spaces from the front
        HEALTHSTATUS=${HEALTHSTATUS%%+( )} #Remove longest matching series of spaces from the back
    done
    sleep 2;
    echo "Oracle successfully started"
    # We increase file sizes to avoid online resizes as that requires lots of CPU which is restricted in XE
    $PRIVILEGED_CLI $CONTAINER_CLI exec oracle bash -c "source /home/oracle/.bashrc; bash -c \"
cat <<EOF | \$ORACLE_HOME/bin/sqlplus / as sysdba
set timing on
-- Remove DISABLE_OOB parameter from Listener configuration and restart it
!echo Enabling OOB for Listener...
!echo NAMES.DIRECTORY_PATH=\(EZCONNECT,TNSNAMES\) > /opt/oracle/oradata/dbconfig/FREE/sqlnet.ora
!lsnrctl reload
-- Increasing redo logs
alter database add logfile group 4 '\$ORACLE_BASE/oradata/FREE/redo04.log' size 500M reuse;
alter database add logfile group 5 '\$ORACLE_BASE/oradata/FREE/redo05.log' size 500M reuse;
alter database add logfile group 6 '\$ORACLE_BASE/oradata/FREE/redo06.log' size 500M reuse;
alter system switch logfile;
alter system switch logfile;
alter system switch logfile;
alter system checkpoint;
alter database drop logfile group 1;
alter database drop logfile group 2;
alter database drop logfile group 3;
!rm \$ORACLE_BASE/oradata/FREE/redo01.log
!rm \$ORACLE_BASE/oradata/FREE/redo02.log
!rm \$ORACLE_BASE/oradata/FREE/redo03.log

-- Increasing SYSAUX data file
alter database datafile '\$ORACLE_BASE/oradata/FREE/sysaux01.dbf' resize 600M;

-- Modifying database init parameters
alter system set open_cursors=1000 sid='*' scope=both;
alter system set session_cached_cursors=500 sid='*' scope=spfile;
alter system set db_securefile=ALWAYS sid='*' scope=spfile;
alter system set dispatchers='(PROTOCOL=TCP)(SERVICE=FREEXDB)(DISPATCHERS=0)' sid='*' scope=spfile;
alter system set recyclebin=OFF sid='*' SCOPE=SPFILE;

-- Comment the 2 next lines to be able to use Diagnostics Pack features
alter system set sga_target=0m sid='*' scope=both;
-- alter system set statistics_level=BASIC sid='*' scope=spfile;

-- Restart the database
SHUTDOWN IMMEDIATE;
STARTUP MOUNT;
ALTER DATABASE OPEN;

-- Switch to the FREEPDB1 pluggable database
alter session set container=freepdb1;

-- Modify FREEPDB1 datafiles and tablespaces
alter database datafile '\$ORACLE_BASE/oradata/FREE/FREEPDB1/system01.dbf' resize 320M;
alter database datafile '\$ORACLE_BASE/oradata/FREE/FREEPDB1/sysaux01.dbf' resize 360M;
alter database datafile '\$ORACLE_BASE/oradata/FREE/FREEPDB1/undotbs01.dbf' resize 400M;
alter database datafile '\$ORACLE_BASE/oradata/FREE/FREEPDB1/undotbs01.dbf' autoextend on next 16M;
alter database tempfile '\$ORACLE_BASE/oradata/FREE/FREEPDB1/temp01.dbf' resize 400M;
alter database tempfile '\$ORACLE_BASE/oradata/FREE/FREEPDB1/temp01.dbf' autoextend on next 16M;
alter database datafile '\$ORACLE_BASE/oradata/FREE/FREEPDB1/users01.dbf' resize 100M;
alter database datafile '\$ORACLE_BASE/oradata/FREE/FREEPDB1/users01.dbf' autoextend on next 16M;
alter tablespace USERS nologging;
alter tablespace SYSTEM nologging;
alter tablespace SYSAUX nologging;

create user hibernate_orm_test identified by hibernate_orm_test quota unlimited on users;
grant all privileges to hibernate_orm_test;
EOF\""
}

oracle_setup_old() {
    HEALTHSTATUS=
    until [ "$HEALTHSTATUS" == "healthy" ];
    do
        echo "Waiting for Oracle to start..."
        sleep 5;
        # On WSL, health-checks intervals don't work for Podman, so run them manually
        if command -v podman > /dev/null; then
          $PRIVILEGED_CLI $CONTAINER_CLI healthcheck run oracle > /dev/null
        fi
        HEALTHSTATUS="`$PRIVILEGED_CLI $CONTAINER_CLI inspect -f $HEALTCHECK_PATH oracle`"
        HEALTHSTATUS=${HEALTHSTATUS##+( )} #Remove longest matching series of spaces from the front
        HEALTHSTATUS=${HEALTHSTATUS%%+( )} #Remove longest matching series of spaces from the back
    done
    # We increase file sizes to avoid online resizes as that requires lots of CPU which is restricted in XE
    $PRIVILEGED_CLI $CONTAINER_CLI exec oracle bash -c "source /home/oracle/.bashrc; bash -c \"
cat <<EOF | \$ORACLE_HOME/bin/sqlplus / as sysdba
alter database tempfile '\$ORACLE_BASE/oradata/XE/temp.dbf' resize 400M;
alter database datafile '\$ORACLE_BASE/oradata/XE/system.dbf' resize 1000M;
alter database datafile '\$ORACLE_BASE/oradata/XE/sysaux.dbf' resize 700M;
alter database datafile '\$ORACLE_BASE/oradata/XE/undotbs1.dbf' resize 300M;
alter database add logfile group 4 '\$ORACLE_BASE/oradata/XE/redo04.log' size 500M reuse;
alter database add logfile group 5 '\$ORACLE_BASE/oradata/XE/redo05.log' size 500M reuse;
alter database add logfile group 6 '\$ORACLE_BASE/oradata/XE/redo06.log' size 500M reuse;

alter system switch logfile;
alter system switch logfile;
alter system switch logfile;
alter system checkpoint;

alter database drop logfile group 1;
alter database drop logfile group 2;
alter system set open_cursors=1000 sid='*' scope=both;
alter system set session_cached_cursors=500 sid='*' scope=spfile;
alter system set recyclebin=OFF sid='*' SCOPE=spfile;
alter system set processes=150 scope=spfile;
alter system set filesystemio_options=asynch scope=spfile;
alter system set disk_asynch_io=true scope=spfile;

shutdown immediate;
startup;

create user hibernate_orm_test identified by hibernate_orm_test quota unlimited on users;
grant all privileges to hibernate_orm_test;
EOF\""
#  echo "Waiting for Oracle to restart after configuration..."
#  $CONTAINER_CLI stop oracle
#  $CONTAINER_CLI start oracle
#  HEALTHSTATUS=
#  until [ "$HEALTHSTATUS" == "healthy" ];
#  do
#      echo "Waiting for Oracle to start..."
#      sleep 5;
#      # On WSL, health-checks intervals don't work for Podman, so run them manually
#      if command -v podman > /dev/null; then
#        $CONTAINER_CLI healthcheck run oracle > /dev/null
#      fi
#      HEALTHSTATUS="`$CONTAINER_CLI inspect -f $HEALTCHECK_PATH oracle`"
#      HEALTHSTATUS=${HEALTHSTATUS##+( )} #Remove longest matching series of spaces from the front
#      HEALTHSTATUS=${HEALTHSTATUS%%+( )} #Remove longest matching series of spaces from the back
#  done
#  sleep 2;
  echo "Oracle successfully started"
}

disable_userland_proxy() {
  if [[ "$HEALTCHECK_PATH" == "{{.State.Health.Status}}" ]]; then
    if [[ ! -f /etc/docker/daemon.json ]]; then
      sudo service docker stop
      sudo bash -c "echo '{\"userland-proxy\": false}' > /etc/docker/daemon.json"
      sudo service docker start
    elif ! grep -q userland-proxy /etc/docker/daemon.json; then
      export docker_daemon_json=$(</etc/docker/daemon.json)
      sudo service docker stop
      sudo bash -c 'echo "${docker_daemon_json/\}/,}{\"userland-proxy\": false}" > /etc/docker/daemon.json'
      sudo service docker start
    fi
  fi
}

oracle_atps() {
  echo "Managing Oracle Autonomous Database..."
  export INFO=$(curl -s -k -L -X GET "https://api.atlas-controller.oraclecloud.com/ords/atlas/admin/database?type=autonomous2&hostname=`hostname`" -H 'accept: application/json')
  export HOST=$(echo $INFO | jq -r '.database' | jq -r '.host')
  export SERVICE=$(echo $INFO | jq -r '.database' | jq -r '.service')
  export PASSWORD=$(echo $INFO | jq -r '.database' | jq -r '.password')

  curl -k -s -X POST "https://${HOST}.oraclevcn.com:8443/ords/admin/_/sql" -H 'content-type: application/sql' -H 'accept: application/json' -basic -u admin:${PASSWORD} --data-ascii "create user hibernate_orm_test_$RUNID identified by \"Oracle_19_Password\" DEFAULT TABLESPACE DATA TEMPORARY TABLESPACE TEMP;alter user hibernate_orm_test_$RUNID quota unlimited on data;grant CREATE SESSION, RESOURCE, CREATE VIEW, CREATE SYNONYM to hibernate_orm_test_$RUNID;"
}

oracle_atps_tls() {
  echo "Managing Oracle Autonomous Database..."
  export INFO=$(curl -s -k -L -X GET "https://api.atlas-controller.oraclecloud.com/ords/atlas/admin/database?type=autonomous&hostname=`hostname`" -H 'accept: application/json')
  export HOST=$(echo $INFO | jq -r '.database' | jq -r '.host')
  export SERVICE=$(echo $INFO | jq -r '.database' | jq -r '.service')
  export PASSWORD=$(echo $INFO | jq -r '.database' | jq -r '.password')

  curl -s -X POST "https://${HOST}.oraclecloudapps.com/ords/admin/_/sql" -H 'content-type: application/sql' -H 'accept: application/json' -basic -u admin:${PASSWORD} --data-ascii "create user hibernate_orm_test_$RUNID identified by \"Oracle_19_Password\" DEFAULT TABLESPACE DATA TEMPORARY TABLESPACE TEMP;alter user hibernate_orm_test_$RUNID quota unlimited on data;grant CREATE SESSION, RESOURCE, CREATE VIEW, CREATE SYNONYM, CREATE DOMAIN to hibernate_orm_test_$RUNID;"
}

oracle_db19c() {
  echo "Managing Oracle Database 19c..."
  export INFO=$(curl -s -k -L -X GET "https://api.atlas-controller.oraclecloud.com/ords/atlas/admin/database?type=db19c&hostname=`hostname`" -H 'accept: application/json')
  export HOST=$(echo $INFO | jq -r '.database' | jq -r '.host')
  export SERVICE=$(echo $INFO | jq -r '.database' | jq -r '.service')
  export PASSWORD=$(echo $INFO | jq -r '.database' | jq -r '.password')

/home/opc/sqlcl/bin/sql -s system/$PASSWORD@$HOST:1521/$SERVICE <<EOF
    create user hibernate_orm_test_$RUNID identified by "Oracle_19_Password" DEFAULT TABLESPACE USERS TEMPORARY TABLESPACE TEMP;
    alter user hibernate_orm_test_$RUNID quota unlimited on users;
    grant CREATE SESSION, RESOURCE, CREATE VIEW, CREATE SYNONYM, CREATE ANY INDEX, EXECUTE ANY TYPE to hibernate_orm_test_$RUNID;
EOF

}

oracle_db21c() {
  echo "Managing Oracle Database 21c..."
  export INFO=$(curl -s -k -L -X GET "https://api.atlas-controller.oraclecloud.com/ords/atlas/admin/database?type=db21c&hostname=`hostname`" -H 'accept: application/json')
  export HOST=$(echo $INFO | jq -r '.database' | jq -r '.host')
  export SERVICE=$(echo $INFO | jq -r '.database' | jq -r '.service')
  export PASSWORD=$(echo $INFO | jq -r '.database' | jq -r '.password')

/home/opc/sqlcl/bin/sql -s system/$PASSWORD@$HOST:1521/$SERVICE <<EOF
    create user hibernate_orm_test_$RUNID identified by "Oracle_21_Password" DEFAULT TABLESPACE USERS TEMPORARY TABLESPACE TEMP;
    alter user hibernate_orm_test_$RUNID quota unlimited on users;
    grant CREATE SESSION, RESOURCE, CREATE VIEW, CREATE SYNONYM, CREATE ANY INDEX, EXECUTE ANY TYPE to hibernate_orm_test_$RUNID;
EOF
}

oracle_db23c() {
  echo "Managing Oracle Database 23c..."
  export INFO=$(curl -s -k -L -X GET "https://api.atlas-controller.oraclecloud.com/ords/atlas/admin/database?type=db23c&hostname=`hostname`" -H 'accept: application/json')
  export HOST=$(echo $INFO | jq -r '.database' | jq -r '.host')
  export SERVICE=$(echo $INFO | jq -r '.database' | jq -r '.service')
  export PASSWORD=$(echo $INFO | jq -r '.database' | jq -r '.password')

/home/opc/sqlcl/bin/sql -s system/$PASSWORD@$HOST:1521/$SERVICE <<EOF
    create user hibernate_orm_test_$RUNID identified by "Oracle_23_Password" DEFAULT TABLESPACE USERS TEMPORARY TABLESPACE TEMP;
    alter user hibernate_orm_test_$RUNID quota unlimited on users;
    grant DB_DEVELOPER_ROLE to hibernate_orm_test_$RUNID;
EOF
}

oracle() {
  oracle_23
}

oracle_11() {
    $PRIVILEGED_CLI $CONTAINER_CLI rm -f oracle || true
    # We need to use the defaults
    # SYSTEM/Oracle18
    $PRIVILEGED_CLI $CONTAINER_CLI run --name oracle -d -p 1521:1521 -e ORACLE_PASSWORD=Oracle18 \
      --health-cmd healthcheck.sh \
      --health-interval 5s \
      --health-timeout 5s \
      --health-retries 10 \
      ${DB_IMAGE_ORACLE_11:-docker.io/gvenzl/oracle-xe:11.2.0.2-full}
    oracle_setup_old
}

oracle_21() {
    $PRIVILEGED_CLI $CONTAINER_CLI rm -f oracle || true
    disable_userland_proxy
    # We need to use the defaults
    # SYSTEM/Oracle18
    $PRIVILEGED_CLI $CONTAINER_CLI run --name oracle -d -p 1521:1521 -e ORACLE_PASSWORD=Oracle18 \
       --cap-add cap_net_raw \
       --health-cmd healthcheck.sh \
       --health-interval 5s \
       --health-timeout 5s \
       --health-retries 10 \
       ${DB_IMAGE_ORACLE_21:-docker.io/gvenzl/oracle-xe:21.3.0-full}
    oracle_setup
}

oracle_23() {
    $PRIVILEGED_CLI $CONTAINER_CLI rm -f oracle || true
    disable_userland_proxy
    # We need to use the defaults
    # SYSTEM/Oracle18
    $PRIVILEGED_CLI $CONTAINER_CLI run --name oracle -d -p 1521:1521 -e ORACLE_PASSWORD=Oracle18 \
       --health-cmd healthcheck.sh \
       --health-interval 5s \
       --health-timeout 5s \
       --health-retries 10 \
       ${DB_IMAGE_ORACLE_23:-docker.io/gvenzl/oracle-free:23-full}
    oracle_free_setup
}

hana() {
    temp_dir=$(mktemp -d)
    echo '{"master_password" : "H1bernate_test"}' >$temp_dir/password.json
    chmod 777 -R $temp_dir
    $PRIVILEGED_CLI $CONTAINER_CLI rm -f hana || true
    $PRIVILEGED_CLI $CONTAINER_CLI run -d --name hana -p 39013:39013 -p 39017:39017 -p 39041-39045:39041-39045 -p 1128-1129:1128-1129 -p 59013-59014:59013-59014 \
      --memory=8g \
      --ulimit nofile=1048576:1048576 \
      --sysctl kernel.shmmax=1073741824 \
      --sysctl net.ipv4.ip_local_port_range='40000 60999' \
      --sysctl kernel.shmmni=4096 \
      --sysctl kernel.shmall=8388608 \
      -v $temp_dir:/config:Z \
      ${DB_IMAGE_HANA:-docker.io/saplabs/hanaexpress:2.00.061.00.20220519.1} \
      --passwords-url file:///config/password.json \
      --agree-to-sap-license
    # Give the container some time to start
    OUTPUT=
    while [[ $OUTPUT != *"Startup finished"* ]]; do
        echo "Waiting for HANA to start..."
        sleep 10
        OUTPUT=$($PRIVILEGED_CLI $CONTAINER_CLI logs hana 2>&1)
    done
    echo "HANA successfully started"
}

cockroachdb() {
  cockroachdb_23_1
}

cockroachdb_23_1() {
  $CONTAINER_CLI rm -f cockroach || true
  LOG_CONFIG="
sinks:
  stderr:
    channels: all
    filter: ERROR
    redact: false
    exit-on-error: true
"
  $CONTAINER_CLI run -d --name=cockroach -m 6g -p 26257:26257 -p 8080:8080 ${DB_IMAGE_COCKROACHDB_23_1:-docker.io/cockroachdb/cockroach:v23.1.8 start-single-node} \
    --insecure --store=type=mem,size=0.25 --advertise-addr=localhost --log="$LOG_CONFIG"
  OUTPUT=
  while [[ $OUTPUT != *"CockroachDB node starting"* ]]; do
        echo "Waiting for CockroachDB to start..."
        sleep 10
        # Note we need to redirect stderr to stdout to capture the logs
        OUTPUT=$($CONTAINER_CLI logs cockroach 2>&1)
  done
  echo "Enabling experimental box2d operators and some optimized settings for running the tests"
  #settings documented in https://www.cockroachlabs.com/docs/v22.1/local-testing.html#use-a-local-single-node-cluster-with-in-memory-storage
  $CONTAINER_CLI exec cockroach bash -c "cat <<EOF | ./cockroach sql --insecure
SET CLUSTER SETTING sql.spatial.experimental_box2d_comparison_operators.enabled = on;
SET CLUSTER SETTING kv.raft_log.disable_synchronization_unsafe = true;
SET CLUSTER SETTING kv.range_merge.queue_interval = '50ms';
SET CLUSTER SETTING jobs.registry.interval.gc = '30s';
SET CLUSTER SETTING jobs.registry.interval.cancel = '180s';
SET CLUSTER SETTING jobs.retention_time = '15s';
SET CLUSTER SETTING sql.stats.automatic_collection.enabled = false;
SET CLUSTER SETTING kv.range_split.by_load_merge_delay = '5s';
ALTER RANGE default CONFIGURE ZONE USING "gc.ttlseconds" = 600;
ALTER DATABASE system CONFIGURE ZONE USING "gc.ttlseconds" = 600;

quit
EOF
"
  echo "Cockroachdb successfully started"

}

cockroachdb_22_2() {
  $CONTAINER_CLI rm -f cockroach || true
  LOG_CONFIG="
sinks:
  stderr:
    channels: all
    filter: ERROR
    redact: false
    exit-on-error: true
"
  $CONTAINER_CLI run -d --name=cockroach -m 6g -p 26257:26257 -p 8080:8080 ${DB_IMAGE_COCKROACHDB_22_2:-docker.io/cockroachdb/cockroach:v22.2.2} start-single-node \
    --insecure --store=type=mem,size=0.25 --advertise-addr=localhost --log="$LOG_CONFIG"
  OUTPUT=
  while [[ $OUTPUT != *"CockroachDB node starting"* ]]; do
        echo "Waiting for CockroachDB to start..."
        sleep 10
        # Note we need to redirect stderr to stdout to capture the logs
        OUTPUT=$($CONTAINER_CLI logs cockroach 2>&1)
  done
  echo "Enabling experimental box2d operators and some optimized settings for running the tests"
  #settings documented in https://www.cockroachlabs.com/docs/v22.1/local-testing.html#use-a-local-single-node-cluster-with-in-memory-storage
  $CONTAINER_CLI exec cockroach bash -c "cat <<EOF | ./cockroach sql --insecure
SET CLUSTER SETTING sql.spatial.experimental_box2d_comparison_operators.enabled = on;
SET CLUSTER SETTING kv.raft_log.disable_synchronization_unsafe = true;
SET CLUSTER SETTING kv.range_merge.queue_interval = '50ms';
SET CLUSTER SETTING jobs.registry.interval.gc = '30s';
SET CLUSTER SETTING jobs.registry.interval.cancel = '180s';
SET CLUSTER SETTING jobs.retention_time = '15s';
SET CLUSTER SETTING sql.stats.automatic_collection.enabled = false;
SET CLUSTER SETTING kv.range_split.by_load_merge_delay = '5s';
ALTER RANGE default CONFIGURE ZONE USING "gc.ttlseconds" = 600;
ALTER DATABASE system CONFIGURE ZONE USING "gc.ttlseconds" = 600;
SET CLUSTER SETTING sql.defaults.serial_normalization=sql_sequence;

quit
EOF
"
  echo "Cockroachdb successfully started"

}

cockroachdb_22_1() {
  $CONTAINER_CLI rm -f cockroach || true
  LOG_CONFIG="
sinks:
  stderr:
    channels: all
    filter: ERROR
    redact: false
    exit-on-error: true
"
  $CONTAINER_CLI run -d --name=cockroach -m 6g -p 26257:26257 -p 8080:8080 ${DB_IMAGE_COCKROACHDB_22_1:-docker.io/cockroachdb/cockroach:v22.1.13} start-single-node \
    --insecure --store=type=mem,size=0.25 --advertise-addr=localhost --log="$LOG_CONFIG"
  OUTPUT=
  while [[ $OUTPUT != *"CockroachDB node starting"* ]]; do
        echo "Waiting for CockroachDB to start..."
        sleep 10
        # Note we need to redirect stderr to stdout to capture the logs
        OUTPUT=$($CONTAINER_CLI logs cockroach 2>&1)
  done
  echo "Enabling experimental box2d operators and some optimized settings for running the tests"
  #settings documented in https://www.cockroachlabs.com/docs/v22.1/local-testing.html#use-a-local-single-node-cluster-with-in-memory-storage
  $CONTAINER_CLI exec cockroach bash -c "cat <<EOF | ./cockroach sql --insecure
SET CLUSTER SETTING sql.spatial.experimental_box2d_comparison_operators.enabled = on;
SET CLUSTER SETTING kv.raft_log.disable_synchronization_unsafe = true;
SET CLUSTER SETTING kv.range_merge.queue_interval = '50ms';
SET CLUSTER SETTING jobs.registry.interval.gc = '30s';
SET CLUSTER SETTING jobs.registry.interval.cancel = '180s';
SET CLUSTER SETTING jobs.retention_time = '15s';
SET CLUSTER SETTING sql.stats.automatic_collection.enabled = false;
SET CLUSTER SETTING kv.range_split.by_load_merge_delay = '5s';
ALTER RANGE default CONFIGURE ZONE USING "gc.ttlseconds" = 600;
ALTER DATABASE system CONFIGURE ZONE USING "gc.ttlseconds" = 600;
SET CLUSTER SETTING sql.defaults.serial_normalization=sql_sequence;

quit
EOF
"
  echo "Cockroachdb successfully started"

}

cockroachdb_21_2() {
  $CONTAINER_CLI rm -f cockroach || true
  LOG_CONFIG="
sinks:
  stderr:
    channels: all
    filter: ERROR
    redact: false
    exit-on-error: true
"
  $CONTAINER_CLI run -d --name=cockroach -m 6g -p 26257:26257 -p 8080:8080 ${DB_IMAGE_COCKROACHDB_21_2:-docker.io/cockroachdb/cockroach:v21.2.16} start-single-node \
    --insecure --store=type=mem,size=640MiB --advertise-addr=localhost --log="$LOG_CONFIG"
  OUTPUT=
  while [[ $OUTPUT != *"CockroachDB node starting"* ]]; do
        echo "Waiting for CockroachDB to start..."
        sleep 10
        # Note we need to redirect stderr to stdout to capture the logs
        OUTPUT=$($CONTAINER_CLI logs cockroach 2>&1)
  done
  echo "Enabling experimental box2d operators and some optimized settings for running the tests"
  #settings documented in https://www.cockroachlabs.com/docs/v21.2/local-testing.html#use-a-local-single-node-cluster-with-in-memory-storage
  $CONTAINER_CLI exec cockroach bash -c "cat <<EOF | ./cockroach sql --insecure
SET CLUSTER SETTING sql.spatial.experimental_box2d_comparison_operators.enabled = on;
SET CLUSTER SETTING kv.raft_log.disable_synchronization_unsafe = true;
SET CLUSTER SETTING kv.range_merge.queue_interval = '50ms';
SET CLUSTER SETTING jobs.registry.interval.gc = '30s';
SET CLUSTER SETTING jobs.registry.interval.cancel = '180s';
SET CLUSTER SETTING jobs.retention_time = '15s';
SET CLUSTER SETTING schemachanger.backfiller.buffer_increment = '128 KiB';
SET CLUSTER SETTING sql.stats.automatic_collection.enabled = false;
SET CLUSTER SETTING kv.range_split.by_load_merge_delay = '5s';
SET CLUSTER SETTING timeseries.storage.enabled = false;
SET CLUSTER SETTING timeseries.storage.resolution_10s.ttl = '0s';
SET CLUSTER SETTING timeseries.storage.resolution_30m.ttl = '0s';
ALTER RANGE default CONFIGURE ZONE USING \"gc.ttlseconds\" = 10;
ALTER DATABASE system CONFIGURE ZONE USING \"gc.ttlseconds\" = 10;
ALTER DATABASE defaultdb CONFIGURE ZONE USING \"gc.ttlseconds\" = 10;
SET CLUSTER SETTING sql.defaults.serial_normalization=sql_sequence;
quit
EOF
"
  echo "Cockroachdb successfully started"

}

tidb() {
  tidb_5_1
}

tidb_5_1() {
    $CONTAINER_CLI rm -f tidb || true
    $CONTAINER_CLI run --name tidb -p4000:4000 -d ${DB_IMAGE_TIDB_5_1:-docker.io/pingcap/tidb:v5.1.4}
    # Give the container some time to start
    OUTPUT=
    n=0
    until [ "$n" -ge 5 ]
    do
        OUTPUT=$($CONTAINER_CLI logs tidb 2>&1)
        if [[ $OUTPUT == *"server is running"* ]]; then
          break;
        fi
        n=$((n+1))
        echo "Waiting for TiDB to start..."
        sleep 3
    done
    $CONTAINER_CLI run --link tidb:tidb -it --rm docker.io/mysql:8.0.31 mysql -htidb -P4000 -uroot -e "create database hibernate_orm_test; create user 'hibernate_orm_test' identified by 'hibernate_orm_test'; grant all on hibernate_orm_test.* to 'hibernate_orm_test';"
    if [ "$n" -ge 5 ]; then
      echo "TiDB failed to start and configure after 15 seconds"
    else
      echo "TiDB successfully started"
    fi
}

if [ -z ${1} ]; then
    echo "No db name provided"
    echo "Provide one of:"
    echo -e "\tcockroachdb"
    echo -e "\tcockroachdb_23_1"
    echo -e "\tcockroachdb_22_2"
    echo -e "\tcockroachdb_22_1"
    echo -e "\tcockroachdb_21_1"
    echo -e "\tdb2"
    echo -e "\tdb2_11_5"
    echo -e "\tdb2_10_5"
    echo -e "\tdb2_spatial"
    echo -e "\tedb"
    echo -e "\tedb_15"
    echo -e "\tedb_14"
    echo -e "\tedb_10"
    echo -e "\thana"
    echo -e "\tmariadb"
    echo -e "\tmariadb_11_1"
    echo -e "\tmariadb_10_9"
    echo -e "\tmariadb_10_3"
    echo -e "\tmssql"
    echo -e "\tmssql_2022"
    echo -e "\tmssql_2017"
    echo -e "\tmysql"
    echo -e "\tmysql_8_1"
    echo -e "\tmysql_8_0"
    echo -e "\tmysql_5_7"
    echo -e "\toracle"
    echo -e "\toracle_23"
    echo -e "\toracle_21"
    echo -e "\toracle_11"
    echo -e "\tpostgresql"
    echo -e "\tpostgresql_15"
    echo -e "\tpostgresql_14"
    echo -e "\tpostgresql_13"
    echo -e "\tpostgresql_10"
    echo -e "\tpostgresql_9_5"
    echo -e "\tsybase"
    echo -e "\ttidb"
    echo -e "\ttidb_5_1"
else
    ${1}
fi
