# DB containers

Describes the approach of managing DB containers.

Starting DBs relies on the Docker (Podman) compose. 

## Directory structure

* [build-config](build-config): contains Dockerfiles for custom-built images like EDB
 or additional config files that are "mounted" to the corresponding DBs
* [latest](latest): contains a set of compose files with the latest versions of DBs.
 This directory is monitored by Dependabot, and PRs will be opened if a new version of the DB is available.
* [versioned](versioned): contains compose files for previous versions of DBs, 
 the ones that we are not testing as regular as the latest ones.

## Adding new DBs or new versions of exiting DBs

Always add a new compose file to the [latest](latest) directory.
If you want to keep the previous version available: 
create a corresponding [versioned](versioned) compose file.

Always add a healthcheck that succeeds when the DB is fully ready to accept requests.
If you don't have the tools required to make the healthcheck within the image,
consider using the sidecar approach applied in [spanner.yml](latest/spanner.yml).

