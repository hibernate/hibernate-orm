#! /bin/bash

mysql_5_7() {
    docker rm -f mysql || true
    docker run --name mysql -e MYSQL_USER=hibernate_orm_test -e MYSQL_PASSWORD=hibernate_orm_test -e MYSQL_DATABASE=hibernate_orm_test -p3306:3306 -d mysql:5.7 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
}

mysql_8_0() {
    docker rm -f mysql || true
    docker run --name mysql -e MYSQL_USER=hibernate_orm_test -e MYSQL_PASSWORD=hibernate_orm_test -e MYSQL_ROOT_PASSWORD=hibernate_orm_test -e MYSQL_DATABASE=hibernate_orm_test -p3306:3306 -d mysql:8.0.21 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
}

mariadb() {
    docker rm -f mariadb || true
    docker run --name mariadb -e MYSQL_USER=hibernate_orm_test -e MYSQL_PASSWORD=hibernate_orm_test -e MYSQL_DATABASE=hibernate_orm_test -e MYSQL_ROOT_PASSWORD=hibernate_orm_test -p3306:3306 -d mariadb:10.5.8 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
}

postgresql_9_5() {
    docker rm -f postgres || true
    docker run --name postgres -e POSTGRES_USER=hibernate_orm_test -e POSTGRES_PASSWORD=hibernate_orm_test -e POSTGRES_DB=hibernate_orm_test -p5432:5432 -d postgres:9.5
}

postgis(){
  docker rm -f postgis || true
  docker run --name postgis -e POSTGRES_USER=hibernate_orm_test -e POSTGRES_PASSWORD=hibernate_orm_test -e POSTGRES_DB=hibernate_orm_test -p5432:5432 -d postgis/postgis:11-2.5
}

db2() {
    docker rm -f db2 || true
    docker run --name db2 --privileged -e DB2INSTANCE=orm_test -e DB2INST1_PASSWORD=orm_test -e DBNAME=orm_test -e LICENSE=accept -e AUTOCONFIG=false -e ARCHIVE_LOGS=false -e TO_CREATE_SAMPLEDB=false -e REPODB=false -p 50000:50000 -d ibmcom/db2:11.5.5.0
    # Give the container some time to start
    OUTPUT=
    while [[ $OUTPUT != *"INSTANCE"* ]]; do
        echo "Waiting for DB2 to start..."
        sleep 10
        OUTPUT=$(docker logs db2)
    done
    docker exec -t db2 su - orm_test bash -c ". /database/config/orm_test/sqllib/db2profile && /database/config/orm_test/sqllib/bin/db2 'connect to orm_test' && /database/config/orm_test/sqllib/bin/db2 'CREATE USER TEMPORARY TABLESPACE usr_tbsp MANAGED BY AUTOMATIC STORAGE'"
}

db2_spatial() {
    docker rm -f db2spatial || true
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
    docker run --name db2spatial --privileged -e DB2INSTANCE=orm_test -e DB2INST1_PASSWORD=orm_test -e DBNAME=orm_test -e LICENSE=accept -e AUTOCONFIG=false -e ARCHIVE_LOGS=false -e TO_CREATE_SAMPLEDB=false -e REPODB=false \
        -v ${temp_dir}:/conf  \
        -p 50000:50000 -d ibmcom/db2:11.5.5.0

    # Give the container some time to start
    OUTPUT=
    while [[ $OUTPUT != *"Setup has completed."* ]]; do
        echo "Waiting for DB2 to start..."
        sleep 10
        OUTPUT=$(docker logs db2spatial)
    done
    sleep 10
    echo "Enabling spatial extender"
    docker exec -t db2spatial su - orm_test bash -c "/database/config/orm_test/sqllib/db2profile && /database/config/orm_test/sqllib/bin/db2se enable_db orm_test"
    echo "Installing required transform group"
    docker exec -t db2spatial su - orm_test bash -c "/database/config/orm_test/sqllib/db2profile && /database/config/orm_test/sqllib/bin/db2 'connect to orm_test' && /database/config/orm_test/sqllib/bin/db2 -tvf /conf/ewkt.sql"

}

mssql() {
    docker rm -f mssql || true
    docker run --name mssql -d -p 1433:1433 -e "SA_PASSWORD=Hibernate_orm_test" -e ACCEPT_EULA=Y mcr.microsoft.com/mssql/server:2017-CU13
    sleep 5
    n=0
    until [ "$n" -ge 5 ]
    do
        # We need a database that uses a non-lock based MVCC approach
        # https://github.com/microsoft/homebrew-mssql-release/issues/2#issuecomment-682285561
        docker exec mssql bash -c 'echo "create database hibernate_orm_test collate SQL_Latin1_General_CP1_CI_AS; alter database hibernate_orm_test set READ_COMMITTED_SNAPSHOT ON" | /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P Hibernate_orm_test -i /dev/stdin' && break
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

oracle() {
    docker rm -f oracle || true
    # We need to use the defaults
    # SYSTEM/Oracle18
    docker run --shm-size=1536m --name oracle -d -p 1521:1521 --ulimit nofile=1048576:1048576 quillbuilduser/oracle-18-xe
    until [ "`docker inspect -f {{.State.Health.Status}} oracle`" == "healthy" ];
    do
        echo "Waiting for Oracle to start..."
      sleep 10;
    done
    echo "Oracle successfully started"
    # We increase file sizes to avoid online resizes as that requires lots of CPU which is restricted in XE
    docker exec oracle bash -c "source /home/oracle/.bashrc; bash -c \"
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

oracle_ee() {
    docker rm -f oracle || true
    # We need to use the defaults
    # sys as sysdba/Oradoc_db1
    docker run --name oracle -d -p 1521:1521 store/oracle/database-enterprise:12.2.0.1-slim
    # Give the container some time to start
    OUTPUT=
    while [[ $OUTPUT != *"NLS_CALENDAR"* ]]; do
        echo "Waiting for Oracle to start..."
        sleep 10
        OUTPUT=$(docker logs oracle)
    done
    echo "Oracle successfully started"
    # We increase file sizes to avoid online resizes as that requires lots of CPU which is restricted in XE
    docker exec oracle bash -c "source /home/oracle/.bashrc; \$ORACLE_HOME/bin/sqlplus sys/Oradoc_db1@ORCLCDB as sysdba <<EOF
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
    docker rm -f hana || true
    docker run -d --name hana -p 39013:39013 -p 39017:39017 -p 39041-39045:39041-39045 -p 1128-1129:1128-1129 -p 59013-59014:59013-59014 \
      --memory=8g \
      --ulimit nofile=1048576:1048576 \
      --sysctl kernel.shmmax=1073741824 \
      --sysctl net.ipv4.ip_local_port_range='40000 60999' \
      --sysctl kernel.shmmni=4096 \
      --sysctl kernel.shmall=8388608 \
      -v $temp_dir:/config \
      store/saplabs/hanaexpress:2.00.045.00.20200121.1 \
      --passwords-url file:///config/password.json \
      --agree-to-sap-license
    # Give the container some time to start
    OUTPUT=
    while [[ $OUTPUT != *"Startup finished"* ]]; do
        echo "Waiting for HANA to start..."
        sleep 10
        OUTPUT=$(docker logs hana)
    done
    echo "HANA successfully started"
}

cockroachdb() {
  docker rm -f cockroach || true
  docker run -d --name=cockroach -p 26257:26257 -p 8080:8080 cockroachdb/cockroach:v20.2.4 start-single-node --insecure
  OUTPUT=
  while [[ $OUTPUT != *"CockroachDB node starting"* ]]; do
        echo "Waiting for CockroachDB to start..."
        sleep 10
        OUTPUT=$(docker logs cockroach)
  done
  echo "Enabling experimental box2d operators"
  docker exec -it cockroach bash -c "cat <<EOF | ./cockroach sql --insecure
SET CLUSTER SETTING sql.spatial.experimental_box2d_comparison_operators.enabled = on;
quit
EOF
"
  echo "Cockroachdb successfully started"

}

if [ -z ${1} ]; then
    echo "No db name provided"
    echo "Provide one of:"
    echo -e "\tmysql_5_7"
    echo -e "\tmysql_8_0"
    echo -e "\tmariadb"
    echo -e "\tpostgresql_9_5"
    echo -e "\tdb2"
    echo -e "\tmssql"
    echo -e "\toracle"
    echo -e "\tpostgis"
    echo -e "\tdb2_spatial"
    echo -e "\thana"
    echo -e "\tcockroachdb"
else
    ${1}
fi
