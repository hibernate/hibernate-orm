FROM quay.io/enterprisedb/edb-postgres-advanced:13.20-3.5-postgis
USER root
# this 777 will be replaced by 700 at runtime (allows semi-arbitrary "--user" values)
RUN chown -R postgres:postgres /var/lib/edb && chmod 777 /var/lib/edb && rm /docker-entrypoint-initdb.d/10_postgis.sh

USER postgres
ENV LANG en_US.utf8
ENV PG_MAJOR 13
ENV PG_VERSION 13
ENV PGPORT 5444
ENV PGDATA /var/lib/edb/as$PG_MAJOR/data/
VOLUME /var/lib/edb/as$PG_MAJOR/data/

COPY docker-entrypoint.sh /usr/local/bin/
ENTRYPOINT ["docker-entrypoint.sh"]

# We set the default STOPSIGNAL to SIGINT, which corresponds to what PostgreSQL
# calls "Fast Shutdown mode" wherein new connections are disallowed and any
# in-progress transactions are aborted, allowing PostgreSQL to stop cleanly and
# flush tables to disk, which is the best compromise available to avoid data
# corruption.
#
# Users who know their applications do not keep open long-lived idle connections
# may way to use a value of SIGTERM instead, which corresponds to "Smart
# Shutdown mode" in which any existing sessions are allowed to finish and the
# server stops when all sessions are terminated.
#
# See https://www.postgresql.org/docs/12/server-shutdown.html for more details
# about available PostgreSQL server shutdown signals.
#
# See also https://www.postgresql.org/docs/12/server-start.html for further
# justification of this as the default value, namely that the example (and
# shipped) systemd service files use the "Fast Shutdown mode" for service
# termination.
#
STOPSIGNAL SIGINT
#
# An additional setting that is recommended for all users regardless of this
# value is the runtime "--stop-timeout" (or your orchestrator/runtime's
# equivalent) for controlling how long to wait between sending the defined
# STOPSIGNAL and sending SIGKILL (which is likely to cause data corruption).
#
# The default in most runtimes (such as Docker) is 10 seconds, and the
# documentation at https://www.postgresql.org/docs/12/server-start.html notes
# that even 90 seconds may not be long enough in many instances.

EXPOSE 5444
CMD ["postgres"]