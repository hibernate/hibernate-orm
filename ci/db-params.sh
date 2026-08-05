#! /bin/bash

dbParam=$RDBMS
containerName=
goal=
if [ "$RDBMS" == "h2" ] || [ "$RDBMS" == "" ]; then
  # This is the default.
  #   - special check for Jenkins CI jobs where we don't want to run releasePrepare
  if [[ "$CI_SYSTEM" != "jenkins" ]] && [[ "$TCK_RUN" != "true" ]]; then
    goal="releasePrepare"
    # Settings needed for `releasePrepare` execution - for asciidoctor doc rendering
    export GRADLE_OPTS=-Dorg.gradle.jvmargs='-Dlog4j2.disableJmx -Xmx4g -XX:MaxMetaspaceSize=768m -XX:+HeapDumpOnOutOfMemoryError -Duser.language=en -Duser.country=US -Duser.timezone=UTC -Dfile.encoding=UTF-8'
  fi
  dbParam=
elif [ "$RDBMS" == "hsqldb" ]; then
  goal="-Pdb=hsqldb"
  dbParam=
elif [ "$RDBMS" == "hsqldb_2_6" ]; then
  goal="-Pdb=hsqldb -PdbVersion=${DB_VERSION:-2.6}"
  dbParam=
elif [ "$RDBMS" == "mysql" ]; then
  goal="-Pdb=mysql"
  containerName=mysql
elif [ "$RDBMS" == "mysql_8_0" ]; then
  goal="-Pdb=mysql -PdbVersion=${DB_VERSION:-8.0}"
  containerName=mysql
elif [ "$RDBMS" == "mariadb" ]; then
  goal="-Pdb=mariadb"
  containerName=mariadb
elif [ "$RDBMS" == "mariadb_10_6" ]; then
  goal="-Pdb=mariadb -PdbVersion=${DB_VERSION:-10.6}"
  containerName=mariadb
elif [ "$RDBMS" == "postgresql" ]; then
  goal="-Pdb=postgresql"
  containerName=postgres
elif [ "$RDBMS" == "postgresql_14" ]; then
  goal="-Pdb=postgresql -PdbVersion=${DB_VERSION:-14}"
  containerName=postgres
elif [ "$RDBMS" == "gaussdb"  ]; then
  goal="-Pdb=gaussdb -DdbHost=localhost:8000"
  containerName=opengauss
elif [ "$RDBMS" == "edb" ]; then
  goal="-Pdb=edb -DdbHost=localhost:5444"
  containerName=edb
elif [ "$RDBMS" == "edb_14" ]; then
  goal="-Pdb=edb -DdbHost=localhost:5444 -PdbVersion=${DB_VERSION:-14}"
  containerName=edb
elif [ "$RDBMS" == "oracle" ]; then
  goal="-Pdb=oracle"
  containerName=oracle
elif [ "$RDBMS" == "oracle_xe" ]; then
  goal="-Pdb=oracle_xe -PdbVersion=${DB_VERSION:-18}"
  containerName=oracle
elif [ "$RDBMS" == "oracle_21" ]; then
  goal="-Pdb=oracle_xe -PdbVersion=${DB_VERSION:-21}"
  containerName=oracle
elif [ "$RDBMS" == "oracle_atps_tls" ]; then
  echo "Managing Oracle Autonomous Database..."
  export INFO=$(curl -s -k -L -X GET "https://api.atlas-controller.oraclecloud.com/ords/atlas/admin/database?type=autonomous&hostname=`hostname`" -H 'accept: application/json')
  export HOST=$(echo $INFO | jq -r '.database' | jq -r '.host')
  export SERVICE=$(echo $INFO | jq -r '.database' | jq -r '.service')
  goal="-Pdb=oracle_cloud_autonomous_tls -DrunID=$RUNID -DdbHost=$HOST -DdbService=$SERVICE"
elif [ "$RDBMS" == "oracle_atps" ]; then
  echo "Managing Oracle Autonomous Database..."
  export INFO=$(curl -s -k -L -X GET "https://api.atlas-controller.oraclecloud.com/ords/atlas/admin/database?type=autonomous2&hostname=`hostname`" -H 'accept: application/json')
  export HOST=$(echo $INFO | jq -r '.database' | jq -r '.host')
  export SERVICE=$(echo $INFO | jq -r '.database' | jq -r '.service')
  goal="-Pdb=oracle_cloud_autonomous -DrunID=$RUNID -DdbHost=$HOST -DdbService=$SERVICE"
elif [ "$RDBMS" == "oracle_db19c" ]; then
  echo "Managing Oracle Database 19c..."
  export INFO=$(curl -s -k -L -X GET "https://api.atlas-controller.oraclecloud.com/ords/atlas/admin/database?type=db19c&hostname=`hostname`" -H 'accept: application/json')
  export HOST=$(echo $INFO | jq -r '.database' | jq -r '.host')
  export SERVICE=$(echo $INFO | jq -r '.database' | jq -r '.service')
  goal="-Pdb=oracle_cloud_db19c -DrunID=$RUNID -DdbHost=$HOST -DdbService=$SERVICE"
elif [ "$RDBMS" == "oracle_db21c" ]; then
  echo "Managing Oracle Database 21c..."
  export INFO=$(curl -s -k -L -X GET "https://api.atlas-controller.oraclecloud.com/ords/atlas/admin/database?type=db21c&hostname=`hostname`" -H 'accept: application/json')
  export HOST=$(echo $INFO | jq -r '.database' | jq -r '.host')
  export SERVICE=$(echo $INFO | jq -r '.database' | jq -r '.service')
  goal="-Pdb=oracle_cloud_db21c -DrunID=$RUNID -DdbHost=$HOST -DdbService=$SERVICE"
elif [ "$RDBMS" == "oracle_db23c" ]; then
  echo "Managing Oracle Database 23c..."
  export INFO=$(curl -s -k -L -X GET "https://api.atlas-controller.oraclecloud.com/ords/atlas/admin/database?type=db23c&hostname=`hostname`" -H 'accept: application/json')
  export HOST=$(echo $INFO | jq -r '.database' | jq -r '.host')
  export SERVICE=$(echo $INFO | jq -r '.database' | jq -r '.service')
  goal="-Pdb=oracle_cloud_db23c -DrunID=$RUNID -DdbHost=$HOST -DdbService=$SERVICE"
# OTP
elif [ "$RDBMS" == "autonomous-transaction-processing-serverless-19c" ]; then
  echo "Managing OTP Database..."
  goal="-Pdb=oracle_test_pilot_database -PdbVersion=${DB_VERSION:-atps-19} -DrunID=$RUNID -DdbPassword=$TESTPILOT_PASSWORD -DdbConnectionStringSuffix=$TESTPILOT_CONNECTION_STRING_SUFFIX"
elif [ "$RDBMS" == "autonomous-transaction-processing-serverless-26ai" ]; then
  echo "Managing OTP Database..."
  goal="-Pdb=oracle_test_pilot_database -PdbVersion=${DB_VERSION:-atps-26} -DrunID=$RUNID -DdbPassword=$TESTPILOT_PASSWORD -DdbConnectionStringSuffix=$TESTPILOT_CONNECTION_STRING_SUFFIX"
elif [ "$RDBMS" == "autonomous-transaction-processing-serverless" ]; then
  echo "Managing OTP Database..."
  goal="-Pdb=oracle_test_pilot_database -PdbVersion=${DB_VERSION:-atps} -DrunID=$RUNID -DdbPassword=$TESTPILOT_PASSWORD -DdbConnectionStringSuffix=$TESTPILOT_CONNECTION_STRING_SUFFIX"
elif [ "$RDBMS" == "base-database-service-19c" ]; then
  echo "Managing OTP Database..."
  goal="-Pdb=oracle_test_pilot_database -PdbVersion=${DB_VERSION:-19} -DrunID=$RUNID -DdbPassword=$TESTPILOT_PASSWORD -DdbConnectionStringSuffix=$TESTPILOT_CONNECTION_STRING_SUFFIX"
elif [ "$RDBMS" == "base-database-service-21c" ]; then
  echo "Managing OTP Database..."
  goal="-Pdb=oracle_test_pilot_database -PdbVersion=${DB_VERSION:-21} -DrunID=$RUNID -DdbPassword=$TESTPILOT_PASSWORD -DdbConnectionStringSuffix=$TESTPILOT_CONNECTION_STRING_SUFFIX"
elif [ "$RDBMS" == "base-database-service-26ai" ]; then
  echo "Managing OTP Database..."
  goal="-Pdb=oracle_test_pilot_database -PdbVersion=${DB_VERSION:-26} -DrunID=$RUNID -DdbPassword=$TESTPILOT_PASSWORD -DdbConnectionStringSuffix=$TESTPILOT_CONNECTION_STRING_SUFFIX"
elif [ "$RDBMS" == "base-database-service-26ai-rac" ]; then
  echo "Managing OTP Database..."
  goal="-Pdb=oracle_test_pilot_database -PdbVersion=${DB_VERSION:-26-rac} -DrunID=$RUNID -DdbPassword=$TESTPILOT_PASSWORD -DdbConnectionStringSuffix=$TESTPILOT_CONNECTION_STRING_SUFFIX"
elif [ "$RDBMS" == "db2" ]; then
  goal="-Pdb=db2"
  containerName=db2
elif [ "$RDBMS" == "db2_11_5" ]; then
  goal="-Pdb=db2_old"
  containerName=db2
elif [ "$RDBMS" == "mssql" ]; then
  goal="-Pdb=mssql"
  containerName=mssql
elif [ "$RDBMS" == "mssql_2017" ]; then
  goal="-Pdb=mssql -PdbVersion=${DB_VERSION:-2017}"
  containerName=mssql
# Exclude some Sybase tests on CI because they use `xmltable` function which has a memory leak on the DB version in CI
elif [ "$RDBMS" == "sybase" ]; then
  goal="-Pdb=sybase -PexcludeTests=**.GenerateSeriesTest*"
  containerName=sybase
elif [ "$RDBMS" == "sybase_jconn" ]; then
  goal="-Pdb=sybase_jconn -PexcludeTests=**.GenerateSeriesTest*"
  dbParam=sybase
  containerName=sybase
elif [ "$RDBMS" == "teradata" ]; then
  goal="-Pdb=teradata"
elif [ "$RDBMS" == "tidb" ]; then
  goal="-Pdb=tidb"
  containerName=tidb
elif [ "$RDBMS" == "hana" ]; then
  goal="-Pdb=hana"
  containerName=hana
elif [ "$RDBMS" == "hana_cloud" ]; then
  goal="-Pdb=hana_cloud"
elif [ "$RDBMS" == "cockroachdb" ]; then
  goal="-Pdb=cockroachdb"
  containerName=cockroach
elif [ "$RDBMS" == "altibase" ]; then
  goal="-Pdb=altibase"
elif [ "$RDBMS" == "informix" ]; then
  goal="-Pdb=informix"
  containerName=informix
elif [ "$RDBMS" == "spannerpgsql" ]; then
  goal="-Pdb=spannerpgsql"
  containerName=spanner
  dbParam=spanner_pg
elif [ "$RDBMS" == "spanner" ]; then
  goal="-Pdb=spanner"
  containerName=spanner
else
  echo "Invalid value for RDBMS: $RDBMS"
  exit 1
fi

if [ "$DB_VERSION" != "" ]; then
  if [[ "$goal" != *"-PdbVersion="* ]]; then
    goal="${goal} -PdbVersion=${DB_VERSION}"
  fi
fi

function logAndExec() {
  echo 1>&2 "Executing:" "${@}"
  exec "${@}"
}
