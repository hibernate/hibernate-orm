#! /bin/bash

goal=
if [ "$RDBMS" == "h2" ] || [ "$RDBMS" == "" ]; then
  # This is the default.
  goal="clean test check releasePrepare"
  # Settings needed for asciidoctor doc rendering
	export GRADLE_OPTS=-Dorg.gradle.jvmargs='-Dlog4j2.disableJmx -Xmx4g -XX:MaxMetaspaceSize=768m -XX:+HeapDumpOnOutOfMemoryError -Duser.language=en -Duser.country=US -Duser.timezone=UTC -Dfile.encoding=UTF-8'
else
  echo "Invalid value for RDBMS: $RDBMS"
  exit 1
fi

function logAndExec() {
  echo 1>&2 "Executing:" "${@}"
  exec "${@}"
}

logAndExec ./gradlew ${goal} "${@}" -Plog-test-progress=true --stacktrace
