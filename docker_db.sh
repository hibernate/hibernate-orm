#! /bin/bash

###############################################################################
# Detect container cli (Docker/Podman)
# Docker has priority to make CI builds more stable/predictable
# (Jenkins is currently better configured to deal with Docker)
###############################################################################
if command -v docker > /dev/null; then
  CONTAINER_CLI=$(command -v docker)
  if [[ "$(docker version | grep Podman)" == "" ]]; then
    IS_DOCKER_RUNTIME=true
    IS_PODMAN=false
  else
    IS_DOCKER_RUNTIME=false
    IS_PODMAN=true
  fi
elif command -v podman > /dev/null; then
  CONTAINER_CLI=$(command -v podman)
  IS_DOCKER_RUNTIME=false
  IS_PODMAN=true
else
  echo "ERROR: Neither docker nor podman found on PATH"
  exit 1
fi

# Set runtime-specific defaults
if [[ "$IS_DOCKER_RUNTIME" == "true" ]]; then
  HEALTHCHECK_PATH="{{.State.Health.Status}}"
  PRIVILEGED_CLI=""
elif [[ "$IS_PODMAN" == "true" ]]; then
  HEALTHCHECK_PATH="{{.State.Healthcheck.Status}}"
  if command -v sudo > /dev/null; then
    PRIVILEGED_CLI="sudo"
  else
    PRIVILEGED_CLI=""
  fi
fi

DB_COUNT=1
if [[ "$(uname -s)" == "Darwin" ]]; then
  IS_OSX=true
  DB_COUNT=$(($(sysctl -n hw.physicalcpu)/2))
  # PostGIS images only support amd64, so we force emulation on macOS
  export POSTGRESQL_PLATFORM="linux/amd64"
else
  IS_OSX=false
  DB_COUNT=$(($(nproc)/2))
fi

###############################################################################
# Helper functions to start/stop a database using compose files
###############################################################################
compose_up() {
    local compose_file="docker-compose/$1"

    echo "Starting database using $compose_file"

    $CONTAINER_CLI compose -f "$compose_file" up -d --wait $REMOVE_ORPHANS || {
        echo "Error: Docker compose failed to start."
        exit 1
    }
}

compose_down() {
    $CONTAINER_CLI compose -f "docker-compose/$1" down -v 2>/dev/null || true
    $CONTAINER_CLI rm -f "$2" 2>/dev/null || true
}

###############################################################################

mysql() {
    mysql_9_6
}

mysql_8_0() {
    compose_down "versioned/mysql-8.0.yml" "mysql"
    compose_up "versioned/mysql-8.0.yml"
    mysql_post_setup
}

mysql_8_1() {
    compose_down "versioned/mysql-8.1.yml" "mysql"
    compose_up "versioned/mysql-8.1.yml"
    mysql_post_setup
}

mysql_8_2() {
    compose_down "versioned/mysql-8.2.yml" "mysql"
    compose_up "versioned/mysql-8.2.yml"
    mysql_post_setup
}

mysql_9_2() {
    compose_down "versioned/mysql-9.2.yml" "mysql"
    compose_up "versioned/mysql-9.2.yml"
    mysql_post_setup
}

mysql_9_4() {
    compose_down "versioned/mysql-9.4.yml" "mysql"
    compose_up "versioned/mysql-9.4.yml"
    mysql_post_setup
}

mysql_9_5() {
    compose_down "versioned/mysql-9.5.yml" "mysql"
    compose_up "versioned/mysql-9.5.yml"
    mysql_post_setup
}

mysql_9_6() {
    compose_down "latest/mysql.yml" "mysql"
    compose_up "latest/mysql.yml"
    mysql_post_setup
}

# MySQL post-setup: wait for ready state, install components, create test databases
mysql_post_setup() {
    # Install components
    # file://component_classic_hashing - This is for legacy hashing algorithms on MySQL 9.6: SHA1 and MD5.
    $CONTAINER_CLI exec mysql bash -c "mysql -u root -phibernate_orm_test -e \"INSTALL COMPONENT 'file://component_classic_hashing'\"" 2>/dev/null

    databases=()
    for n in $(seq 1 $DB_COUNT)
    do
      databases+=("hibernate_orm_test_${n}")
    done
    create_cmd=
    for i in "${!databases[@]}";do
      create_cmd+="create database ${databases[i]}; grant all privileges on ${databases[i]}.* to 'hibernate_orm_test'@'%';"
    done
    $CONTAINER_CLI exec mysql bash -c "mysql -u root -phibernate_orm_test -e \"${create_cmd}\"" 2>/dev/null
    echo "MySQL databases were successfully setup"
}

###############################################################################

mariadb() {
  mariadb_12_2
}

mariadb_10_6() {
    compose_down "versioned/mariadb-10-6.yml" "mariadb"
    compose_up "versioned/mariadb-10-6.yml"
    mariadb_post_setup
}

mariadb_10_11() {
    compose_down "versioned/mariadb-10-11.yml" "mariadb"
    compose_up "versioned/mariadb-10-11.yml"
    mariadb_post_setup
}

mariadb_11_4() {
    compose_down "versioned/mariadb-11-4.yml" "mariadb"
    compose_up "versioned/mariadb-11-4.yml"
    mariadb_post_setup
}

mariadb_11_8() {
    compose_down "versioned/mariadb-11-8.yml" "mariadb"
    compose_up "versioned/mariadb-11-8.yml"
    mariadb_post_setup
}

mariadb_12_0() {
    compose_down "versioned/mariadb-12-0.yml" "mariadb"
    compose_up "versioned/mariadb-12-0.yml"
    mariadb_post_setup
}

mariadb_12_1() {
    compose_down "versioned/mariadb-12-1.yml" "mariadb"
    compose_up "versioned/mariadb-12-1.yml"
    mariadb_post_setup
}

mariadb_12_2() {
    compose_down "latest/mariadb.yml" "mariadb"
    compose_up "latest/mariadb.yml"
    mariadb_post_setup
}

mariadb_verylatest() {
    compose_down "versioned/mariadb-verylatest.yml" "mariadb"
    compose_up "versioned/mariadb-verylatest.yml"
    mariadb_post_setup
}

mariadb_post_setup() {
    databases=()
    for n in $(seq 1 $DB_COUNT)
    do
      databases+=("hibernate_orm_test_${n}")
    done
    create_cmd=
    for i in "${!databases[@]}";do
      create_cmd+="create database ${databases[i]}; grant all privileges on ${databases[i]}.* to 'hibernate_orm_test'@'%';"
    done
    $CONTAINER_CLI exec mariadb bash -c "mariadb -u root -phibernate_orm_test -e \"${create_cmd}\"" 2>/dev/null
    echo "MariaDB databases were successfully setup"
}

###############################################################################

postgresql() {
  postgresql_18
}

postgresql_14() {
    compose_down "versioned/postgresql-14.yml" "postgres"
    compose_up "versioned/postgresql-14.yml"
    postgresql_setup 14
}

postgresql_15() {
    compose_down "versioned/postgresql-15.yml" "postgres"
    compose_up "versioned/postgresql-15.yml"
    postgresql_setup 15
}

postgresql_16() {
    compose_down "versioned/postgresql-16.yml" "postgres"
    compose_up "versioned/postgresql-16.yml"
    postgresql_setup 16
}

postgresql_17() {
    compose_down "versioned/postgresql-17.yml" "postgres"
    compose_up "versioned/postgresql-17.yml"
    postgresql_setup 17
}

postgresql_18() {
    compose_down "latest/postgresql.yml" "postgres"
    compose_up "latest/postgresql.yml"
    postgresql_setup 18
}

postgresql_setup() {
    local pg_version="$1"
    $CONTAINER_CLI exec postgres bash -c "/usr/share/postgresql-common/pgdg/apt.postgresql.org.sh -y && apt install -y postgresql-${pg_version}-pgvector" 2>/dev/null || true

    databases=()
    for n in $(seq 1 $DB_COUNT)
    do
      databases+=("hibernate_orm_test_${n}")
    done
    create_cmd=
    for i in "${!databases[@]}";do
      create_cmd+="psql -U hibernate_orm_test -d postgres -c \"create database ${databases[i]};\";"
    done
    $CONTAINER_CLI exec postgres bash -c "until pg_isready -U hibernate_orm_test; do sleep 1; done"
    $CONTAINER_CLI exec postgres bash -c "${create_cmd}"
    $CONTAINER_CLI exec postgres bash -c 'psql -U hibernate_orm_test -d hibernate_orm_test -c "create extension vector;"'
    for i in "${!databases[@]}";do
      $CONTAINER_CLI exec postgres bash -c "psql -U hibernate_orm_test -d ${databases[i]} -c \"create extension vector; create extension postgis;\""
    done
}

###############################################################################

gaussdb() {
    compose_down "latest/gaussdb.yml" "opengauss"
    compose_up "latest/gaussdb.yml"
}

###############################################################################

edb() {
    edb_17
}

edb_14() {
    compose_down "versioned/edb-14.yml" "edb"
    compose_up "versioned/edb-14.yml"
    edb_setup 14
}

edb_15() {
    compose_down "versioned/edb-15.yml" "edb"
    compose_up "versioned/edb-15.yml"
    edb_setup 15
}

edb_16() {
    compose_down "versioned/edb-16.yml" "edb"
    compose_up "versioned/edb-16.yml"
    edb_setup 16
}

edb_17() {
    compose_down "latest/edb.yml" "edb"
    compose_up "latest/edb.yml"
    edb_setup 17
}

edb_setup() {
  databases=()
  for n in $(seq 1 $DB_COUNT)
  do
    databases+=("hibernate_orm_test_${n}")
  done
  create_cmd=
  for i in "${!databases[@]}";do
    create_cmd+="psql -U hibernate_orm_test -d postgres -c \"create database ${databases[i]};\";"
  done
  $CONTAINER_CLI exec edb bash -c "until pg_isready -U hibernate_orm_test; do sleep 1; done"
  # Database seems to restart after it was ready once, so give it some time
  sleep 1
  $CONTAINER_CLI exec edb bash -c "until pg_isready -U hibernate_orm_test; do sleep 1; done"
  $CONTAINER_CLI exec edb bash -c "${create_cmd}"
  $CONTAINER_CLI exec edb bash -c 'psql -U hibernate_orm_test -d hibernate_orm_test -c "create extension vector;"'
  for i in "${!databases[@]}";do
    $CONTAINER_CLI exec edb bash -c "psql -U hibernate_orm_test -d ${databases[i]} -c \"create extension postgis;\""
  done
}

###############################################################################

db2() {
  db2_12_1
}

db2_11_5() {
    compose_down "versioned/db2-11.5.yml" "db2"
    db2_compose_up "versioned/db2-11.5.yml"
    db2_post_setup
}

db2_12_1() {
    compose_down "latest/db2.yml" "db2"
    db2_compose_up "latest/db2.yml"
    db2_post_setup
    db2_setup
}

# OSX/Mac M1 workaround for DB2 containers
# See: https://community.ibm.com/community/user/discussion/db2-luw-115xx-mac-m1-ready#bm017584d2-8d76-42a6-8f76-018dac8e78f2
# See: https://stackoverflow.com/questions/70175677/ibmcom-db2-docker-image-fails-on-m1
db2_osx_setup() {
    if [[ "$IS_OSX" != "true" ]]; then
        return
    fi
    export db2_install_script=$HOME/db2install.sh
    rm -f ${db2_install_script} || true
    cat <<'EOF' >${db2_install_script}
#!/bin/bash
find /var/db2_setup -type f -not -path '*/\.*' -exec sed -i "s/su - db2inst1 -c '/su - db2inst1 -c '. .profile \&\& /g" {} +
find /var/db2_setup -type f -not -path '*/\.*' -exec sed -i "s/su - db2inst1 -c \"/su - db2inst1 -c \". .profile \&\& /g" {} +
find /var/db2_setup -type f -not -path '*/\.*' -exec sed -i "s/su - \${DB2INSTANCE?} -c '/su - \${DB2INSTANCE?} -c '. .profile \&\& /g" {} +
find /var/db2_setup -type f -not -path '*/\.*' -exec sed -i "s/su - \${DB2INSTANCE?} -c \"/su - \${DB2INSTANCE?} -c \". .profile \&\& /g" {} +
find /var/db2_setup -type f -not -path '*/\.*' -exec sed -i "s/su - \${instance?} -c '/su - \${instance?} -c '. .profile \&\& /g" {} +
find /var/db2_setup -type f -not -path '*/\.*' -exec sed -i "s/su - \${instance?} -c \"/su - \${instance?} -c \". .profile \&\& /g" {} +
find /var/db2_setup -type f -not -path '*/\.*' -exec sed -i "s/su - \${instance_name?} -c '/su - \${instance_name?} -c '. .profile \&\& /g" {} +
find /var/db2_setup -type f -not -path '*/\.*' -exec sed -i "s/su - \${instance_name?} -c \"/su - \${instance_name?} -c \". .profile \&\& /g" {} +
find /var/db2_setup -type f -not -path '*/\.*' -exec sed -i "s/su - db2inst1 -c \\\\\"/su - db2inst1 -c \\\". .profile \&\& /g" {} +
. /var/db2_setup/lib/setup_db2_instance.sh
EOF
    chmod 777 ${db2_install_script}
    if [[ "$IS_PODMAN" == "false" ]]; then
        export DOCKER_DEFAULT_PLATFORM=linux/amd64
    fi
}

db2_compose_up() {
    local compose_file="docker-compose/$1"
    local osx_override="${2:-docker-compose/build-config/db2/osx-override.yml}"
    db2_osx_setup
    if [[ "$IS_OSX" == "true" ]]; then
        echo "Starting database using $compose_file (with OSX override)"
        $CONTAINER_CLI compose -f "$compose_file" -f "$osx_override" up -d --wait $REMOVE_ORPHANS
    else
        compose_up "$1"
    fi
}

db2_post_setup() {
    $CONTAINER_CLI exec -t db2 su - orm_test bash -c ". /database/config/orm_test/sqllib/db2profile; /database/config/orm_test/sqllib/bin/db2 'connect to orm_test'; /database/config/orm_test/sqllib/bin/db2 'CREATE USER TEMPORARY TABLESPACE usr_tbsp MANAGED BY AUTOMATIC STORAGE'"
}

db2_setup() {
    pids=()
    for n in $(seq 1 $DB_COUNT)
    do
      $CONTAINER_CLI exec -t db2 su - orm_test bash -c ". /database/config/orm_test/sqllib/db2profile; /database/config/orm_test/sqllib/bin/db2 'connect to orm_test'; /database/config/orm_test/sqllib/bin/db2 'create tenant ORM_${n}';" &
      pids[${i}]=$!
    done
    for pid in ${pids[*]}; do
        wait $pid
    done
}

db2_spatial() {
    compose_down "latest/db2_spatial.yml" "db2spatial"
    db2_compose_up "latest/db2_spatial.yml" "docker-compose/build-config/db2_spatial/osx-override.yml"
    db2_spatial_post_setup
}

db2_spatial_post_setup() {
    echo "Enabling spatial extender"
    $PRIVILEGED_CLI $CONTAINER_CLI exec -t db2spatial su - orm_test bash -c "/database/config/orm_test/sqllib/db2profile && /database/config/orm_test/sqllib/bin/db2se enable_db orm_test"
    echo "Installing required transform group"
    $PRIVILEGED_CLI $CONTAINER_CLI exec -t db2spatial su - orm_test bash -c "/database/config/orm_test/sqllib/db2profile && /database/config/orm_test/sqllib/bin/db2 'connect to orm_test' && /database/config/orm_test/sqllib/bin/db2 -tvf /conf/ewkt.sql"
}

###############################################################################

mssql() {
  mssql_2025
}

mssql_2017() {
    compose_down "versioned/mssql-2017.yml" "mssql"
    compose_up "versioned/mssql-2017.yml"
    mssql_post_setup
}

mssql_2022() {
    compose_down "versioned/mssql-2022.yml" "mssql"
    compose_up "versioned/mssql-2022.yml"
    mssql_post_setup
}

mssql_2025() {
    compose_down "latest/mssql.yml" "mssql"
    compose_up "latest/mssql.yml"
    mssql_post_setup
}

mssql_post_setup() {
  echo "Creating MSSQL databases..."
  for n in $(seq 1 $DB_COUNT)
  do
    # Determine which sqlcmd to use based on container
    local sqlcmd_path="/opt/mssql-tools18/bin/sqlcmd"
    if ! $CONTAINER_CLI exec mssql test -f "$sqlcmd_path" 2>/dev/null; then
      sqlcmd_path="/opt/mssql-tools/bin/sqlcmd"
    fi

    $CONTAINER_CLI exec mssql bash -c "echo \"create database hibernate_orm_test_${n} collate SQL_Latin1_General_CP1_CS_AS; alter database hibernate_orm_test_${n} set READ_COMMITTED_SNAPSHOT ON\" | $sqlcmd_path -C -S localhost -U sa -P Hibernate_orm_test -i /dev/stdin"
  done
  echo "MSSQL databases successfully setup"
}

###############################################################################

sybase() {
    compose_down "latest/sybase.yml" "sybase"
    compose_up "latest/sybase.yml"

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
exec sp_configure 'enable xml', 1
go
exec sp_configure 'heap memory per user', 0, '16K'
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

###############################################################################

oracle_setup() {
    sleep 2;
    users=()
    for n in $(seq 1 $DB_COUNT)
    do
      users+=("hibernate_orm_test_${n}")
    done
    create_cmd=
    for i in "${!users[@]}";do
      create_cmd+="
create user ${users[i]} identified by hibernate_orm_test quota unlimited on users;
grant create session to ${users[i]};
grant all privileges to ${users[i]};"
    done
    
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
grant create session to hibernate_orm_test;
grant all privileges to hibernate_orm_test;
${create_cmd}
EOF\""
}

oracle_free_setup() {
    sleep 2;
    users=()
    for n in $(seq 1 $DB_COUNT)
    do
      users+=("hibernate_orm_test_${n}")
    done
    create_cmd=
    for i in "${!users[@]}";do
      create_cmd+="
create user ${users[i]} identified by hibernate_orm_test quota unlimited on users;
grant create session to ${users[i]};
grant create domain to ${users[i]};
grant all privileges on schema ${users[i]} to ${users[i]};"
    done

    # We increase file sizes to avoid online resizes as that requires lots of CPU which is restricted in XE
    $CONTAINER_CLI exec oracle bash -c "source /home/oracle/.bashrc; bash -c \"
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
grant create session to hibernate_orm_test;
grant create domain to hibernate_orm_test;
grant all privileges on schema hibernate_orm_test to hibernate_orm_test;
${create_cmd}
EOF\""
}

disable_userland_proxy() {
  if [[ "$IS_DOCKER_RUNTIME" == "true" ]]; then
    if [[ "$HEALTHCHECK_PATH" == "{{.State.Health.Status}}" ]]; then
      if [[ ! -f /etc/docker/daemon.json ]]; then
        echo "Didn't find /etc/docker/daemon.json but need to disable userland-proxy..."
        echo "Stopping docker..."
        sudo service docker stop
        echo "Creating /etc/docker/daemon.json..."
        sudo bash -c "echo '{\"userland-proxy\": false}' > /etc/docker/daemon.json"
        echo "Starting docker..."
        sudo service docker start
        echo "Docker successfully started with userland proxies disabled"
      elif ! grep -q userland-proxy /etc/docker/daemon.json; then
        echo "Userland proxy is still enabled in /etc/docker/daemon.json, but need to disable it..."
        export docker_daemon_json=$(</etc/docker/daemon.json)
        echo "Stopping docker..."
        sudo service docker stop
        echo "Updating /etc/docker/daemon.json..."
        sudo bash -c "export docker_daemon_json='$docker_daemon_json'; echo \"\${docker_daemon_json/\}/,}\\\"userland-proxy\\\": false}\" > /etc/docker/daemon.json"
        echo "Starting docker..."
        sudo service docker start
        echo "Service status:"
        sudo journalctl -xeu docker.service
        echo "Docker successfully started with userland proxies disabled"
      fi
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
    grant CREATE SESSION, RESOURCE, CREATE VIEW, CREATE SYNONYM to hibernate_orm_test_$RUNID;
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
    grant CREATE SESSION, RESOURCE, CREATE VIEW, CREATE SYNONYM to hibernate_orm_test_$RUNID;
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

oracle_18() {
    disable_userland_proxy
    compose_down "versioned/oracle-18.yml" "oracle"
    compose_up "versioned/oracle-18.yml"
    oracle_setup
}

oracle_21() {
    disable_userland_proxy
    compose_down "versioned/oracle-21.yml" "oracle"
    compose_up "versioned/oracle-21.yml"
    oracle_setup
}

oracle_23() {
    disable_userland_proxy
    compose_down "latest/oracle.yml" "oracle"
    compose_up "latest/oracle.yml"
    oracle_free_setup
}

###############################################################################

hana() {
    compose_down "latest/hana.yml" "hana"
    compose_up "latest/hana.yml"
    hana_setup
    echo "HANA successfully started"
}

hana_setup() {
  databases=()
  for n in $(seq 1 $DB_COUNT)
  do
    databases+=("hibernate_orm_test_${n}")
  done
  create_cmd=
  for i in "${!databases[@]}";do
    create_cmd+="
create user ${databases[i]} password H1bernate_test NO FORCE_FIRST_PASSWORD_CHANGE;
grant create schema to ${databases[i]};"
  done
  # The first command seems to be ignored?! So let's just run something useless
  $CONTAINER_CLI exec hana bash -c "cat <<EOF | /usr/sap/HXE/HDB90/exe/hdbsql -n localhost:39017 -u SYSTEM -p H1bernate_test -stdin
select 1;
$create_cmd
\q
EOF
"
}

###############################################################################

cockroachdb() {
  cockroachdb_25_4
}

cockroachdb_25_4() {
  compose_down "latest/cockroachdb.yml" "cockroach"
  compose_up "latest/cockroachdb.yml"
  cockroachdb_post_setup
}

cockroachdb_24_3() {
  compose_down "versioned/cockroachdb-24.3.yml" "cockroach"
  compose_up "versioned/cockroachdb-24.3.yml"
  cockroachdb_post_setup
}

cockroachdb_24_1() {
  compose_down "versioned/cockroachdb-24.1.yml" "cockroach"
  compose_up "versioned/cockroachdb-24.1.yml"
  cockroachdb_post_setup
}

cockroachdb_23_2() {
  compose_down "versioned/cockroachdb-23.2.yml" "cockroach"
  compose_up "versioned/cockroachdb-23.2.yml"
  cockroachdb_post_setup_23_2
}

cockroachdb_post_setup() {
  echo "Enabling experimental box2d operators and optimized settings for tests"
  $CONTAINER_CLI exec cockroach bash -c "cat <<EOF | ./cockroach sql --insecure
SET CLUSTER SETTING sql.spatial.experimental_box2d_comparison_operators.enabled = on;
SET CLUSTER SETTING kv.range_merge.queue_interval = '50ms';
SET CLUSTER SETTING jobs.registry.interval.gc = '30s';
SET CLUSTER SETTING jobs.registry.interval.cancel = '180s';
SET CLUSTER SETTING jobs.retention_time = '5s';
SET CLUSTER SETTING sql.stats.automatic_collection.enabled = false;
ALTER RANGE default CONFIGURE ZONE USING \"gc.ttlseconds\" = 300;
ALTER DATABASE system CONFIGURE ZONE USING \"gc.ttlseconds\" = 300;
quit
EOF
"
  cockroachdb_setup
  echo "CockroachDB successfully started"
}

cockroachdb_post_setup_23_2() {
  echo "Enabling experimental box2d operators and optimized settings for tests (v23.2 specific)"
  $CONTAINER_CLI exec cockroach bash -c "cat <<EOF | ./cockroach sql --insecure
SET CLUSTER SETTING sql.spatial.experimental_box2d_comparison_operators.enabled = on;
SET CLUSTER SETTING kv.raft_log.disable_synchronization_unsafe = true;
SET CLUSTER SETTING kv.range_merge.queue_interval = '50ms';
SET CLUSTER SETTING jobs.registry.interval.gc = '30s';
SET CLUSTER SETTING jobs.registry.interval.cancel = '180s';
SET CLUSTER SETTING jobs.retention_time = '5s';
SET CLUSTER SETTING sql.stats.automatic_collection.enabled = false;
SET CLUSTER SETTING kv.range_split.by_load_merge_delay = '5s';
SET CLUSTER SETTING sql.defaults.serial_normalization = 'sql_sequence_cached';
ALTER RANGE default CONFIGURE ZONE USING \"gc.ttlseconds\" = 300;
ALTER DATABASE system CONFIGURE ZONE USING \"gc.ttlseconds\" = 300;
quit
EOF
"
  cockroachdb_setup
  echo "CockroachDB successfully started"
}

cockroachdb_setup() {
  databases=()
  for n in $(seq 1 $DB_COUNT)
  do
    databases+=("hibernate_orm_test_${n}")
  done
  create_cmd=
  for i in "${!databases[@]}";do
    create_cmd+="create database ${databases[i]};"
  done
  $CONTAINER_CLI exec cockroach bash -c "cat <<EOF | ./cockroach sql --insecure
$create_cmd

quit
EOF
"
}

###############################################################################

tidb() {
  tidb_8_5
}

tidb_8_5() {
    tidb_prepare_setup
    compose_down "latest/tidb.yml" "tidb"
    compose_up "latest/tidb.yml"
}

tidb_5_4() {
    tidb_prepare_setup_5_4
    compose_down "versioned/tidb-5.4.yml" "tidb"
    compose_up "versioned/tidb-5.4.yml"
}

tidb_prepare_setup() {
    # Create databases and configure settings
    databases=()
    for n in $(seq 1 $DB_COUNT); do
      databases+=("hibernate_orm_test_${n}")
    done

    create_cmd="SET GLOBAL tidb_enable_check_constraint=ON;"
    create_cmd+="SET GLOBAL tidb_enable_shared_lock_promotion=ON;"
    create_cmd+="CREATE DATABASE IF NOT EXISTS hibernate_orm_test;"
    create_cmd+="CREATE USER IF NOT EXISTS 'hibernate_orm_test'@'%' IDENTIFIED BY 'hibernate_orm_test';"
    create_cmd+="GRANT ALL ON hibernate_orm_test.* TO 'hibernate_orm_test'@'%';"
    for i in "${!databases[@]}";do
      create_cmd+="CREATE DATABASE IF NOT EXISTS ${databases[i]}; GRANT ALL ON ${databases[i]}.* TO 'hibernate_orm_test'@'%';"
    done
    export TIDB_DB_SETUP=$create_cmd
}

tidb_prepare_setup_5_4() {
    export TIDB_DB_SETUP="create database hibernate_orm_test; create user 'hibernate_orm_test' identified by 'hibernate_orm_test'; grant all on hibernate_orm_test.* to 'hibernate_orm_test';"
}

###############################################################################

informix() {
  informix_15
}

informix_15() {
    compose_down "latest/informix.yml" "informix"
    compose_up "latest/informix.yml"
    informix_post_setup
}

informix_14_10() {
    compose_down "versioned/informix-14.10.yml" "informix"
    compose_up "versioned/informix-14.10.yml"
    informix_post_setup
}

informix_12_10() {
    compose_down "versioned/informix-12.10.yml" "informix"
    compose_up "versioned/informix-12.10.yml"
    informix_post_setup
}

informix_post_setup() {
    $PRIVILEGED_CLI $CONTAINER_CLI exec informix bash -l -c "export DB_LOCALE=en_US.utf8;export CLIENT_LOCALE=en_US.utf8;echo \"execute function task('create dbspace from storagepool', 'datadbs', '100 MB', '4');execute function task('create sbspace from storagepool', 'sbspace', '20 M', '0');create database dev in datadbs with log;\" > post_init.sql;dbaccess sysadmin post_init.sql"
}

###############################################################################

spanner() {
  spanner_emulator GOOGLE_STANDARD_SQL
}

spanner_pg() {
  spanner_emulator POSTGRESQL
}

spanner_emulator() {
  local dialect=${1:-GOOGLE_STANDARD_SQL}
  if [[ $DB_COUNT -gt 8 ]]; then
    DB_COUNT=4
  elif [[ $DB_COUNT -gt 2 ]]; then
    DB_COUNT=$(( DB_COUNT / 2 ))
  fi

  # Start all emulator containers using compose
  for n in $(seq 1 ${DB_COUNT}); do
    local container_name="spanner_${n}"
    local port=$((9010 + n))
    local rest_port=$((9020 + n))

    echo "Starting Spanner emulator instance ${n} on port ${port}..."
    SPANNER_CONTAINER_NAME=${container_name} SPANNER_GRPC_PORT=${port} SPANNER_REST_PORT=${rest_port} \
      compose_down "latest/spanner.yml" "${container_name}"
    SPANNER_CONTAINER_NAME=${container_name} SPANNER_GRPC_PORT=${port} SPANNER_REST_PORT=${rest_port} \
      $CONTAINER_CLI compose -p "spanner-${n}" -f "docker-compose/latest/spanner.yml" up -d --wait
  done

  # Configure instances
  for n in $(seq 1 ${DB_COUNT}); do
    local rest_port=$((9020 + n))
    echo "Configuring Spanner emulator instance ${n} on port ${rest_port}..."
    local host="localhost:${rest_port}"
    local create_statement

    curl -s -X POST "http://${host}/v1/projects/orm-test-project/instances" \
      -H "Content-Type: application/json" \
      -d '{
            "instanceId": "orm-test-instance",
            "instance": {
              "config": "emulator-config",
              "displayName": "Test Instance",
              "nodeCount": 1
            }
          }' >/dev/null || true

    if [[ "$dialect" == "POSTGRESQL" ]]; then
       create_statement="CREATE DATABASE \"orm-test-db\""
    else
       create_statement="CREATE DATABASE \`orm-test-db\`"
    fi

    curl -s -X POST "http://${host}/v1/projects/orm-test-project/instances/orm-test-instance/databases" \
      -H "Content-Type: application/json" \
      -d "{
            \"createStatement\": \"${create_statement//\"/\\\"}\",
            \"databaseDialect\": \"${dialect}\"
          }" >/dev/null

    local update_statements=""
    if [[ "$dialect" == "POSTGRESQL" ]]; then
      update_statements='"ALTER DATABASE \"orm-test-db\" SET \"spanner.default_time_zone\" = '"'UTC'"'", "ALTER DATABASE \"orm-test-db\" SET \"spanner.version_retention_period\" = '"'10s'"'"'
    else
      update_statements='"ALTER DATABASE `orm-test-db` SET OPTIONS ( default_time_zone = '"'UTC'"', version_retention_period = '"'10s'"' )"'
    fi

    curl -s -X PATCH "http://${host}/v1/projects/orm-test-project/instances/orm-test-instance/databases/orm-test-db/ddl" \
      -H "Content-Type: application/json" \
      -d '{
            "statements": [
              '"${update_statements}"'
            ]
          }' >/dev/null || true
  done
}

###############################################################################
# Script args handling:
###############################################################################
REMOVE_ORPHANS="--remove-orphans"
while [[ "${1:-}" == -* ]]; do
    case "$1" in
        -k|--keep-orphans)
            REMOVE_ORPHANS=""
            shift
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

if [ -z ${1} ]; then
    echo "No db name provided"
    echo "Provide one of:"
    echo -e "\tcockroachdb"
    echo -e "\tcockroachdb_25_4"
    echo -e "\tcockroachdb_24_3"
    echo -e "\tcockroachdb_24_1"
    echo -e "\tcockroachdb_23_2"
    echo -e "\tdb2"
    echo -e "\tdb2_12_1"
    echo -e "\tdb2_11_5"
    echo -e "\tdb2_spatial"
    echo -e "\tedb"
    echo -e "\tedb_17"
    echo -e "\tedb_16"
    echo -e "\tedb_15"
    echo -e "\tedb_14"
    echo -e "\thana"
    echo -e "\tmariadb"
    echo -e "\tmariadb_verylatest"
    echo -e "\tmariadb_12_2"
    echo -e "\tmariadb_12_1"
    echo -e "\tmariadb_12_0"
    echo -e "\tmariadb_11_8"
    echo -e "\tmariadb_11_4"
    echo -e "\tmariadb_10_11"
    echo -e "\tmariadb_10_6"
    echo -e "\tmssql"
    echo -e "\tmssql_2025"
    echo -e "\tmssql_2022"
    echo -e "\tmssql_2017"
    echo -e "\tmysql"
    echo -e "\tmysql_9_6"
    echo -e "\tmysql_9_5"
    echo -e "\tmysql_9_4"
    echo -e "\tmysql_9_2"
    echo -e "\tmysql_8_2"
    echo -e "\tmysql_8_1"
    echo -e "\tmysql_8_0"
    echo -e "\toracle"
    echo -e "\toracle_23"
    echo -e "\toracle_21"
    echo -e "\toracle_18"
    echo -e "\toracle_atps"
    echo -e "\toracle_atps_tls"
    echo -e "\toracle_db19c"
    echo -e "\toracle_db21c"
    echo -e "\toracle_db23c"
    echo -e "\tgaussdb"
    echo -e "\tpostgresql"
    echo -e "\tpostgresql_18"
    echo -e "\tpostgresql_17"
    echo -e "\tpostgresql_16"
    echo -e "\tpostgresql_15"
    echo -e "\tpostgresql_14"
    echo -e "\tsybase"
    echo -e "\ttidb"
    echo -e "\ttidb_8_5"
    echo -e "\ttidb_5_4"
    echo -e "\tinformix"
    echo -e "\tinformix_15"
    echo -e "\tinformix_14_10"
    echo -e "\tinformix_12_10"
    echo -e "\tspanner"
    echo -e "\tspanner_pg"
    echo -e "\tspanner_emulator"
else
    ${1}
fi
