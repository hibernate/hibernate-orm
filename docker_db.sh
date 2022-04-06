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

mysql_5_7() {
    $CONTAINER_CLI rm -f mysql || true
    $CONTAINER_CLI run --name mysql -e MYSQL_USER=hibernate_orm_test -e MYSQL_PASSWORD=hibernate_orm_test -e MYSQL_DATABASE=hibernate_orm_test -e MYSQL_ROOT_PASSWORD=hibernate_orm_test -p3306:3306 -d docker.io/mysql:5.7 --character-set-server=utf8mb4 --collation-server=utf8mb4_bin --skip-character-set-client-handshake --log-bin-trust-function-creators=1
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
    $CONTAINER_CLI run --name mysql -e MYSQL_USER=hibernate_orm_test -e MYSQL_PASSWORD=hibernate_orm_test -e MYSQL_ROOT_PASSWORD=hibernate_orm_test -e MYSQL_DATABASE=hibernate_orm_test -e MYSQL_ROOT_PASSWORD=hibernate_orm_test -p3306:3306 -d docker.io/mysql:8.0.21 --character-set-server=utf8mb4 --collation-server=utf8mb4_0900_as_cs --skip-character-set-client-handshake --log-bin-trust-function-creators=1
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
    $CONTAINER_CLI rm -f mariadb || true
    $CONTAINER_CLI run --name mariadb -e MYSQL_USER=hibernate_orm_test -e MYSQL_PASSWORD=hibernate_orm_test -e MYSQL_DATABASE=hibernate_orm_test -e MYSQL_ROOT_PASSWORD=hibernate_orm_test -p3306:3306 -d docker.io/mariadb:10.5.8 --character-set-server=utf8mb4 --collation-server=utf8mb4_bin --skip-character-set-client-handshake
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

postgresql_9_5() {
    $CONTAINER_CLI rm -f postgres || true
    $CONTAINER_CLI run --name postgres -e POSTGRES_USER=hibernate_orm_test -e POSTGRES_PASSWORD=hibernate_orm_test -e POSTGRES_DB=hibernate_orm_test -p5432:5432 -d docker.io/postgis/postgis:9.5-2.5
}

postgresql_13() {
    $CONTAINER_CLI rm -f postgres || true
    $CONTAINER_CLI run --name postgres -e POSTGRES_USER=hibernate_orm_test -e POSTGRES_PASSWORD=hibernate_orm_test -e POSTGRES_DB=hibernate_orm_test -p5432:5432 -d docker.io/postgis/postgis:13-3.1
}

edb() {
    #$CONTAINER_CLI login containers.enterprisedb.com
    $CONTAINER_CLI rm -f edb || true
    $CONTAINER_CLI run --name edb -e ACCEPT_EULA=Yes -e DATABASE_USER=hibernate_orm_test -e DATABASE_USER_PASSWORD=hibernate_orm_test -e ENTERPRISEDB_PASSWORD=hibernate_orm_test -e DATABASE_NAME=hibernate_orm_test -e PGPORT=5433 -p 5433:5433 --mount type=tmpfs,destination=/edbvolume -d containers.enterprisedb.com/edb/edb-as-lite:v11
}

db2() {
    echo $CONTAINER_CLI
    $PRIVILEGED_CLI $CONTAINER_CLI rm -f db2 || true
    $PRIVILEGED_CLI $CONTAINER_CLI run --name db2 --privileged -e DB2INSTANCE=orm_test -e DB2INST1_PASSWORD=orm_test -e DBNAME=orm_test -e LICENSE=accept -e AUTOCONFIG=false -e ARCHIVE_LOGS=false -e TO_CREATE_SAMPLEDB=false -e REPODB=false -p 50000:50000 -d docker.io/ibmcom/db2:11.5.7.0
    # Give the container some time to start
    OUTPUT=
    while [[ $OUTPUT != *"INSTANCE"* ]]; do
        echo "Waiting for DB2 to start..."
        sleep 10
        OUTPUT=$($PRIVILEGED_CLI $CONTAINER_CLI logs db2)
    done
    $PRIVILEGED_CLI $CONTAINER_CLI exec -t db2 su - orm_test bash -c ". /database/config/orm_test/sqllib/db2profile && /database/config/orm_test/sqllib/bin/db2 'connect to orm_test' && /database/config/orm_test/sqllib/bin/db2 'CREATE USER TEMPORARY TABLESPACE usr_tbsp MANAGED BY AUTOMATIC STORAGE'"
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
        -p 50000:50000 -d docker.io/ibmcom/db2:11.5.5.0

    # Give the container some time to start
    OUTPUT=
    while [[ $OUTPUT != *"Setup has completed."* ]]; do
        echo "Waiting for DB2 to start..."
        sleep 10
        OUTPUT=$($PRIVILEGED_CLI $CONTAINER_CLI logs db2spatial)
    done
    sleep 10
    echo "Enabling spatial extender"
    $PRIVILEGED_CLI $CONTAINER_CLI exec -t db2spatial su - orm_test bash -c "/database/config/orm_test/sqllib/db2profile && /database/config/orm_test/sqllib/bin/db2se enable_db orm_test"
    echo "Installing required transform group"
    $PRIVILEGED_CLI $CONTAINER_CLI exec -t db2spatial su - orm_test bash -c "/database/config/orm_test/sqllib/db2profile && /database/config/orm_test/sqllib/bin/db2 'connect to orm_test' && /database/config/orm_test/sqllib/bin/db2 -tvf /conf/ewkt.sql"

}

mssql() {
    $CONTAINER_CLI rm -f mssql || true
    $CONTAINER_CLI run --name mssql -d -p 1433:1433 -e "SA_PASSWORD=Hibernate_orm_test" -e ACCEPT_EULA=Y mcr.microsoft.com/mssql/server:2017-CU13
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
    $CONTAINER_CLI run -d -p 5000:5000 -p 5001:5001 --name sybase --entrypoint /bin/bash docker.io/nguoianphu/docker-sybase -c "source /opt/sybase/SYBASE.sh
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
          $CONTAINER_CLI healthcheck run oracle > /dev/null
        fi
        HEALTHSTATUS="`$CONTAINER_CLI inspect -f $HEALTCHECK_PATH oracle`"
        HEALTHSTATUS=${HEALTHSTATUS##+( )} #Remove longest matching series of spaces from the front
        HEALTHSTATUS=${HEALTHSTATUS%%+( )} #Remove longest matching series of spaces from the back
    done
    sleep 2;
    echo "Oracle successfully started"
    # We increase file sizes to avoid online resizes as that requires lots of CPU which is restricted in XE
    $CONTAINER_CLI exec oracle bash -c "source /home/oracle/.bashrc; bash -c \"
cat <<EOF | \$ORACLE_HOME/bin/sqlplus sys/Oracle18@localhost/XE as sysdba
alter database tempfile '/opt/oracle/oradata/XE/temp01.dbf' resize 400M;
alter database datafile '/opt/oracle/oradata/XE/system01.dbf' resize 1000M;
alter database datafile '/opt/oracle/oradata/XE/sysaux01.dbf' resize 600M;
alter database datafile '/opt/oracle/oradata/XE/undotbs01.dbf' resize 300M;
alter database add logfile group 4 '/opt/oracle/oradata/XE/redo04.log' size 500M reuse;
alter database add logfile group 5 '/opt/oracle/oradata/XE/redo05.log' size 500M reuse;
alter database add logfile group 6 '/opt/oracle/oradata/XE/redo06.log' size 500M reuse;

alter system switch logfile;
alter system switch logfile;
alter system switch logfile;
alter system checkpoint;

alter database drop logfile group 1;
alter database drop logfile group 2;
alter database drop logfile group 3;
alter system set open_cursors=1000 sid='*' scope=both;
EOF\""
}

oracle_legacy() {
    $CONTAINER_CLI rm -f oracle || true
    # We need to use the defaults
    # SYSTEM/Oracle18
    $CONTAINER_CLI run --shm-size=1536m --name oracle -d -p 1521:1521 --ulimit nofile=1048576:1048576 docker.io/quillbuilduser/oracle-18-xe
    oracle_setup
}

oracle() {
  oracle_18
}

oracle_11() {
    $CONTAINER_CLI rm -f oracle || true
    # We need to use the defaults
    # SYSTEM/Oracle18
    $CONTAINER_CLI run --name oracle -d -p 1521:1521 -e ORACLE_PASSWORD=Oracle18 \
      --health-cmd healthcheck.sh \
      --health-interval 5s \
      --health-timeout 5s \
      --health-retries 10 \
      docker.io/gvenzl/oracle-xe:11.2.0.2-full
    oracle_setup
}

oracle_18() {
    $CONTAINER_CLI rm -f oracle || true
    # We need to use the defaults
    # SYSTEM/Oracle18
    $CONTAINER_CLI run --name oracle -d -p 1521:1521 -e ORACLE_PASSWORD=Oracle18 \
       --health-cmd healthcheck.sh \
       --health-interval 5s \
       --health-timeout 5s \
       --health-retries 10 \
       docker.io/gvenzl/oracle-xe:18.4.0-full
    oracle_setup
}

oracle_21() {
    $CONTAINER_CLI rm -f oracle || true
    # We need to use the defaults
    # SYSTEM/Oracle18
    $CONTAINER_CLI run --name oracle -d -p 1521:1521 -e ORACLE_PASSWORD=Oracle18 \
       --health-cmd healthcheck.sh \
       --health-interval 5s \
       --health-timeout 5s \
       --health-retries 10 \
       docker.io/gvenzl/oracle-xe:21.3.0-full
    oracle_setup
}

oracle_ee() {
    #$CONTAINER_CLI login
    $CONTAINER_CLI rm -f oracle || true
    # We need to use the defaults
    # sys as sysdba/Oradoc_db1
    $CONTAINER_CLI run --name oracle -d -p 1521:1521 docker.io/store/oracle/database-enterprise:12.2.0.1-slim
    # Give the container some time to start
    OUTPUT=
    while [[ $OUTPUT != *"NLS_CALENDAR"* ]]; do
        echo "Waiting for Oracle to start..."
        sleep 10
        OUTPUT=$($CONTAINER_CLI logs oracle)
    done
    echo "Oracle successfully started"
    # We increase file sizes to avoid online resizes as that requires lots of CPU which is restricted in XE
    $CONTAINER_CLI exec oracle bash -c "source /home/oracle/.bashrc; \$ORACLE_HOME/bin/sqlplus sys/Oradoc_db1@ORCLCDB as sysdba <<EOF
create user c##hibernate_orm_test identified by hibernate_orm_test container=all;
grant connect, resource, dba to c##hibernate_orm_test container=all;
alter database tempfile '/u02/app/oracle/oradata/ORCL/temp01.dbf' resize 400M;
alter database datafile '/u02/app/oracle/oradata/ORCL/system01.dbf' resize 1000M;
alter database datafile '/u02/app/oracle/oradata/ORCL/sysaux01.dbf' resize 900M;
alter database datafile '/u02/app/oracle/oradata/ORCL/undotbs01.dbf' resize 300M;
alter database add logfile group 4 '/u02/app/oracle/oradata/ORCL/redo04.log' size 500M reuse;
alter database add logfile group 5 '/u02/app/oracle/oradata/ORCL/redo05.log' size 500M reuse;
alter database add logfile group 6 '/u02/app/oracle/oradata/ORCL/redo06.log' size 500M reuse;

alter system switch logfile;
alter system switch logfile;
alter system switch logfile;
alter system checkpoint;

alter database drop logfile group 1;
alter database drop logfile group 2;
alter database drop logfile group 3;
alter session set container=ORCLPDB1;
alter database datafile '/u02/app/oracle/oradata/ORCLCDB/orclpdb1/system01.dbf' resize 500M;
alter database datafile '/u02/app/oracle/oradata/ORCLCDB/orclpdb1/sysaux01.dbf' resize 500M;
EOF"
}

hana() {
    temp_dir=$(mktemp -d)
    echo '{"master_password" : "H1bernate_test"}' >$temp_dir/password.json
    chmod 777 -R $temp_dir
    $CONTAINER_CLI rm -f hana || true
    $CONTAINER_CLI run -d --name hana -p 39013:39013 -p 39017:39017 -p 39041-39045:39041-39045 -p 1128-1129:1128-1129 -p 59013-59014:59013-59014 \
      --memory=8g \
      --ulimit nofile=1048576:1048576 \
      --sysctl kernel.shmmax=1073741824 \
      --sysctl net.ipv4.ip_local_port_range='40000 60999' \
      --sysctl kernel.shmmni=4096 \
      --sysctl kernel.shmall=8388608 \
      -v $temp_dir:/config \
      docker.io/store/saplabs/hanaexpress:2.00.045.00.20200121.1 \
      --passwords-url file:///config/password.json \
      --agree-to-sap-license
    # Give the container some time to start
    OUTPUT=
    while [[ $OUTPUT != *"Startup finished"* ]]; do
        echo "Waiting for HANA to start..."
        sleep 10
        OUTPUT=$($CONTAINER_CLI logs hana)
    done
    echo "HANA successfully started"
}

cockroachdb() {
  $CONTAINER_CLI rm -f cockroach || true
  $CONTAINER_CLI run -d --name=cockroach -p 26257:26257 -p 8080:8080 docker.io/cockroachdb/cockroach:v20.2.4 start-single-node --insecure
  OUTPUT=
  while [[ $OUTPUT != *"CockroachDB node starting"* ]]; do
        echo "Waiting for CockroachDB to start..."
        sleep 10
        OUTPUT=$($CONTAINER_CLI logs cockroach)
  done
  echo "Enabling experimental box2d operators"
  $CONTAINER_CLI exec -it cockroach bash -c "cat <<EOF | ./cockroach sql --insecure
SET CLUSTER SETTING sql.spatial.experimental_box2d_comparison_operators.enabled = on;
quit
EOF
"
  echo "Cockroachdb successfully started"

}

if [ -z ${1} ]; then
    echo "No db name provided"
    echo "Provide one of:"
    echo -e "\tcockroachdb"
    echo -e "\tdb2"
    echo -e "\tdb2_spatial"
    echo -e "\tedb"
    echo -e "\thana"
    echo -e "\tmariadb"
    echo -e "\tmssql"
    echo -e "\tmysql_5_7"
    echo -e "\tmysql_8_0"
    echo -e "\toracle"
    echo -e "\toracle_11"
    echo -e "\toracle_18"
    echo -e "\toracle_21"
    echo -e "\toracle_ee"
    echo -e "\tpostgis"
    echo -e "\tpostgresql_13"
    echo -e "\tpostgresql_9_5"
    echo -e "\tsybase"
else
    ${1}
fi
