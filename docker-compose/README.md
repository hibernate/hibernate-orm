# DB containers

Describes the approach of managing DB containers.

Starting DBs relies on the Docker (Podman) compose, driven through the [`db.sh`](../db.sh)
script at the root of the repository (run `./db.sh` with no arguments, or with `-h`, to list
all the supported DBs/versions).

## Directory structure

* [build-config](build-config): contains Dockerfiles for custom-built images like EDB
 or additional config files that are "mounted" to the corresponding DBs
* [latest](latest): contains a set of compose files with the latest versions of DBs.
 Each subdirectory is monitored by Dependabot (see `.github/dependabot.yml`), and PRs
 will be opened automatically when a new image is available.
* [versioned](versioned): contains compose files for previous versions of DBs,
 the ones that we are not testing as regularly as the latest ones.

Each DB's compose file lives in its own subdirectory (e.g. `latest/mariadb/docker-compose.yaml`,
`versioned/mariadb-12-1/docker-compose.yaml`), named `<db>-<major>-<minor>` for versioned ones.
The image is referenced through an env var with a default value pinned to a specific tag
and `@sha256` digest, e.g. `${MARIADB_IMAGE:-docker.io/library/mariadb:13.0@sha256:...}` in
`latest`, and `${DB_IMAGE_MARIADB_12_1:-docker.io/library/mariadb:12.1@sha256:...}` in `versioned`.
Pinning the digest keeps runs reproducible; Dependabot (for `latest`) or the person adding the
versioned file (for `versioned`) is responsible for resolving the digest for a new tag.

## Adding a brand-new DB

1. Add a new compose file under `latest/<db>/docker-compose.yaml`.
2. Always add a healthcheck that succeeds only when the DB is fully ready to accept requests.
   If the image doesn't have the tools required to implement the healthcheck itself,
   use the sidecar approach applied in [`latest/spanner/docker-compose.yaml`](latest/spanner/docker-compose.yaml).
3. Register the new `latest/<db>` directory under the `docker-compose` entry in
   [`.github/dependabot.yml`](../.github/dependabot.yml) so future image updates are picked up
   automatically. Directories are listed explicitly there; a new DB is invisible to Dependabot
   until it's added to that list.
4. Add the corresponding functions to [`db.sh`](../db.sh): a `<db>()` entry point that starts
   the latest version, and list it in the usage/help output near the bottom of the script.
5. If the DB should run in CI, add it to the test matrix in `.github/workflows/ci.yml`.

## Adding a new version of an existing DB

Dependabot opens PRs that bump the image in `latest/<db>/docker-compose.yaml` automatically.
Most of these bumps only refresh the image digest for the *same* tag (e.g. a rebuild of
`postgis:18-3.6`) or move a patch version (e.g. `cockroachdb:v26.3.1` -> `v26.3.2`); those can
be merged as-is, no further action needed.

When a bump instead moves to a genuinely new version (a new major/minor line, e.g.
MariaDB `12.3` -> `13.0`), and you want to keep the old version testable, do the following
*before* merging the Dependabot bump (or as a follow-up to it):

1. Create `versioned/<db>-<old-major>-<old-minor>/docker-compose.yaml` with the contents
   `latest/<db>/docker-compose.yaml` had *before* the bump, i.e. still pinned to the old
   image tag and digest. Switch its env var override to a version-specific name
   (e.g. `DB_IMAGE_MARIADB_12_3`) so it doesn't collide with the `latest` one.
2. In [`db.sh`](../db.sh), add a `<db>_<old-major>_<old-minor>()` function that starts the
   new versioned compose file, and a `<db>_<new-major>_<new-minor>()` function that starts
   `latest/<db>/docker-compose.yaml` — then point the plain `<db>()` entry point at the new
   latest-version function.
3. Add both new functions to the usage/help list in `db.sh`.

Not every bump needs a versioned copy — use your judgement on whether the previous version
is worth continuing to test (this repo doesn't keep one for every single minor release).

