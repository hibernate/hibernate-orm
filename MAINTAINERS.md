Guide for maintainers of Hibernate ORM
====

This guide is intended for maintainers of Hibernate ORM,
i.e. anybody with direct push access to the git repository.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Classification and compatibility tooling

See the [Classification Tooling Guide](classification-tooling.adoc) for the repository tasks which generate and validate API, SPI, and internal classifications, validate provider boundaries, and analyze API and SPI migration compatibility.

Maintainers own compatibility-baseline selection and activation of the explicit compatibility checks in CI. API compatibility is evaluated within a major version, while SPI compatibility is evaluated within an `X.Y` release family. Release maintainers are also responsible for publishing the family classification metadata and its checksum through the documented release workflow.

## <a id="ci"></a> Continuous integration

Continuous integration happens on GitHub Actions,
using [RunsOn Fleet](https://runs-on.com/) for self-hosted runners,
with some jobs still on a self-hosted Jenkins instance at https://ci.hibernate.org.

Below is a list of all notable workflows/jobs.

### Tips

The Hibernate ORM build can use mirrors instead of public repositories.
The mirror configuration is in `gradle/mirror.gradle`, auto-activated from `settings.gradle`.
Each mirror is activated independently when its `MIRROR_*_URL` property is set.

Supported mirrors (each with `_URL`, `_USERNAME`, `_PASSWORD`, `_FALLBACK` suffixes):

| Prefix                            | Repository              |
|-----------------------------------|-------------------------|
| `MIRROR_MAVEN_CENTRAL`            | Maven Central           |
| `MIRROR_MAVEN_CENTRAL_SNAPSHOTS`  | Central Portal Snapshots|
| `MIRROR_GRADLE_PLUGIN_PORTAL`     | Gradle Plugin Portal    |

For example, to mirror Maven Central set `MIRROR_MAVEN_CENTRAL_URL` via any of
(in order of precedence):
* System property: `-DMIRROR_MAVEN_CENTRAL_URL=<url>`
* Environment variable: `MIRROR_MAVEN_CENTRAL_URL=<url>`

Optional properties (same resolution order per mirror):
* `*_USERNAME` / `*_PASSWORD` — mirror credentials
* `*_FALLBACK` — set to `true` to also keep the original repo as a fallback

Note: Gradle project properties (`-P`) are not supported because they don't
propagate to included builds (`local-build-plugins`).

To pick up locally built dependencies (e.g. a local Jakarta Persistence build),
pass `-DenableMavenLocalRepo=true` to the `gradle` command.
This adds `mavenLocal()` as a repository, which is disabled by default
to ensure reproducible builds on CI.

### Main pipeline

https://github.com/hibernate/hibernate-orm/actions/workflows/ci.yml

This workflow takes care of:

* Primary branch builds
* Pull request builds

It is generally triggered on push or pull request.

See [.github/workflows/ci.yml](.github/workflows/ci.yml)
and [.github/workflows/build.yml](.github/workflows/build.yml) for the workflow definitions.

The initial build compiles sources and renders documentation (no tests).
Test matrix entries then run in parallel, including:

* Multiple JDK versions (baseline build check, LTS releases, latest GA with preview features)
* Multiple database vendors, including label-triggered databases on PRs
* Label-triggered nightly JDK versions on PRs (`jdk` for all, or `jdk-<version>` for specific versions)
* Oracle Test Pilot databases
* Persistence TCK and Data TCK
* Static code analysis (format checks)

See [CONTRIBUTING.md](CONTRIBUTING.md#building-from-source)
for information about how to execute similar builds from the commandline.

### Nightly pipeline

https://github.com/hibernate/hibernate-orm/actions/workflows/nightly.yml

This workflow runs additional environments that are too expensive for every push/PR,
plus a reproducible build check across maintained branches.

* Minimum-version database compatibility
* Additional databases not tested on every push
* EA JDK versions

See [.github/workflows/nightly.yml](.github/workflows/nightly.yml)
and [.github/workflows/build.yml](.github/workflows/build.yml) for the workflow definitions.

### Jenkins pipeline

https://ci.hibernate.org/job/hibernate-orm-pipeline/

This job runs tests that require Jenkins-specific infrastructure:

* s390x architecture tests
* HANA Cloud tests (lockable resource, label-triggered on pull requests)
* sybase_jconn (proprietary driver credential, label-triggered on pull requests)

See [Jenkinsfile](Jenkinsfile) for the job definition.

### Jenkins nightly pipeline

https://ci.hibernate.org/job/hibernate-orm-nightly/

This job runs nightly tests that require Jenkins-specific infrastructure:

* sybase_jconn (proprietary driver credential)
* HANA Cloud tests (lockable resource)

See [nightly.Jenkinsfile](nightly.Jenkinsfile) for the job definition.

### Release pipeline

https://ci.hibernate.org/job/hibernate-orm-release/

This job takes care of releases. It is triggered manually.

See [ci/release/Jenkinsfile](ci/release/Jenkinsfile) for the job definition.

See [Releasing](#releasing) for more information.

## <a id="releasing"></a> Releasing

### Where is the information

If you're looking for information about how releases are implemented technically, see [release/README.adoc](release/README.adoc).

If you're looking for information about how to release Hibernate ORM, read on.

### Automated releases

On select maintenance branches (`6.2`, `6.4`, `7.0`, ...),
micro releases (`x.y.1`, `x.y.2`, ...) are performed on weekends
if, since the last release, commits were pushed with a message starting with `[HHH-` or `HHH-` --
which is taken as "someone fixed something worthy of a Jira issue".

Make sure to assign fix versions properly in Jira when merging pull requests.

No announcements are expected for such releases:
neither through X, blog posts, or email.

### Manual releases

On `main` and some maintenance branches (`6.5`, ...),
automated releases are disabled.

You must perform releases by manually triggering a CI job.

#### Preparing the release

In any case, before the release:

* Check that everything has been pushed to the upstream repository.
* Check that the [CI jobs](#continuous-integration) for the branch you want to release are green:
  * Check the [main CI workflow](https://github.com/hibernate/hibernate-orm/actions/workflows/ci.yml).
  * Check the [nightly CI workflow](https://github.com/hibernate/hibernate-orm/actions/workflows/nightly.yml).
    In particular to make sure that the build is reproducible.
* Check Jira [Releases](https://hibernate.atlassian.net/projects/HHH?selectedItem=com.atlassian.jira.jira-projects-plugin%3Arelease-page):
  * Check that the release you are about to publish exists in Jira.
  * Remove the fix version for anything rejected, etc.
  * Move unresolved issues to another version
  * Check there are no resolved/closed issues in the corresponding "work-in-progress version"
    (e.g. `6.6`, `6.6-next`, ... naming convention may vary);
    if there are, you might want to assign them to your release.
* Pull all upstream changes and perform `./gradlew releasePrepare` locally.

**If it's the first `Alpha`/`Beta` of a new major or minor release**, before the release:

* Reset the migration guide to include only information relevant to the new major or minor.

**If it's a `.CR` or `.Final` release**, before the release:

* Check that the [migration guide](documentation/src/main/asciidoc/migration/index.adoc) is up to date.
  In particular, check the git history for API/SPI changes
  and document them in the migration guide.

#### Performing the release

Trigger the CI release job to automatically publish artifacts and documentation...

* Do *not* mark the Jira Release as "released" or close issues,
  the release job triggers Jira automation that does it for you.
* Do *not* update the repository (in particular changelog.md and README.md), 
  the release job does it for you.
* Trigger the release on CI:
  * Go to CI, to [the "hibernate-orm-release" CI job](https://ci.hibernate.org/job/hibernate-orm-release/).
  * Click the "run" button (the green triangle on top of a clock, to the right) next to the branch you want to release.
  * **Be careful** when filling the form with the build parameters.
    Note only `RELEASE_VERSION` is absolutely necessary.
  * Note that for new branches where the job has never run, the first run may not ask for parameters and thus may fail:
    that's expected, just run it again.

After the job succeeds:

* Update [hibernate.org](https://github.com/hibernate/hibernate.org) as necessary:
  * If it is a new major or minor release (new "series"):
    * Add a `_data/projects/orm/releases/<series>/series.yml` file,
      a `orm/releases/<series>/index.adoc` file, and a `orm/documentation/<series>/index.adoc` file.
      Generally these files can be copied from previous series.
    * If this new series is to support a new JPA release, also be sure to update `orm/releases/index.adoc`
    * Adjust the release file in `_data/projects/orm/releases` that was created automatically by the release job:
      use a meaningful summary, if relevant, and set `announcement_url` to the blog post, if any.
    * None of the above is necessary for maintenance (micro) releases.
  * Depending on which series you want to have displayed,
    make sure to adjust the `status`/`displayed` attributes of the `series.yml` file of the old series.
  * Push to the production branch.
* Check that the artifacts are available on Maven Central:
  https://central.sonatype.com/artifact/org.hibernate.orm/hibernate-core.
  They should appear after a few minutes, sometimes a few hours.
* Make sure a GitHub release got created and that everything looks ok.


#### Announcing the release

If it is an `Alpha`, `Beta`, `CR` or first `Final` (`x.y.0.Final`) release, announce it:

* Blog about release on [in.relation.to](https://github.com/hibernate/in.relation.to).
  Make sure to use the tags "Hibernate ORM" and "Releases" for the blog entry.
* Send an email to `hibernate-announce@lists.jboss.org` and CC `hibernate-dev@lists.jboss.org`.
* Tweet about the release via the `@Hibernate` account.
* Announce it anywhere else you wish (BlueSky, etc).

#### Updating depending projects

If you just released the latest stable, you will need to update other projects:

* Approve and merge automatic updates that dependabot will send (it might take ~24h):
  * In the [test case templates](https://github.com/hibernate/hibernate-test-case-templates/tree/master/orm).
  * In the [demos](https://github.com/hibernate/hibernate-demos/tree/master/hibernate-orm).
* **If it's a `.Final` release**, upgrade the Hibernate ORM dependency manually:
  * In the [Quarkus BOM](https://github.com/quarkusio/quarkus/blob/main/bom/application/pom.xml).
  * In any other relevant project.

#### Updating Hibernate ORM

In any case:

* Reset [release_notes.md](release_notes.md).

**If it is a new major or minor release**:

* Reset the migration guide on the `main` branch if you forgot about it when preparing the release.
* Create a maintenance branch for the previous series, if necessary; see [branching](branching.adoc).


### Setting up the maintenance branch

Once the release series (e.g. 7.3) is branched out and goes into maintenance mode make sure to:
* Enable automated releases (for that branch)
  - Update [Jenkinsfile](ci/release/Jenkinsfile) and switch `RELEASE_ON_SCHEDULE` to `true`
* Remove the nightly Jenkins job from that branch ([nightly.Jenkinsfile](nightly.Jenkinsfile))
* Update GitHub workflows:
  - For [ci.yml](.github/workflows/ci.yml)
    + remove the branch push triggers (`on.pushbranches`)
    + update branch in the pull request triggers
* Enable Quarkus testing job (if the update to this version was already merged(test against main Quarkus branch)/released(test the corresponding version branch))
  - In [quarkus.Jenkinsfile](ci/quarkus.Jenkinsfile) switch `ENABLE_QUARKUS_BUILDS` to true and update `QUARKUS_BRANCH_TO_TEST` as necessary.
* Enable Hibernate Search dependency update job
  - In [Jenkinsfile](Jenkinsfile) add an execution just before the `parallel(executions)`:
  ```groovy
    executions.put('Hibernate Search Update Dependency', {
        build job: '/hibernate-search-dependency-update/{HIBERNATE_SEARCH_VERSION}', propagate: true, parameters: [string(name: 'UPDATE_JOB', value: 'orm{HIBERNATE_ORM_VERSION}'), string(name: 'ORM_REPOSITORY', value: helper.scmSource.remoteUrl), string(name: 'ORM_PULL_REQUEST_ID', value: helper.scmSource.pullRequest.id)]
    })
  ```
  - In this example above, replace `{HIBERNATE_SEARCH_VERSION}` and `{HIBERNATE_ORM_VERSION}` with actual release series, e.g. `8.3`/`7.3` 
* Update main build [Jenkinsfile](Jenkinsfile) (for the branch)
  - Enable JDK testing in the build by removing the conditions under `Don't build environments for newer JDKs`
  - Stop running this build for pushes to the branch
