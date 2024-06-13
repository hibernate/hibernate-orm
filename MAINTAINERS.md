Guide for maintainers of Hibernate ORM
====

This guide is intended for maintainers of Hibernate ORM,
i.e. anybody with direct push access to the git repository.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Continuous integration

Continuous integration is split across two platforms:

* GitHub Actions at https://github.com/hibernate/hibernate-orm/actions
* a self-hosted Jenkins instance at https://ci.hibernate.org.

### GitHub Actions workflows

TODO: describe the workflows available.

### Jenkins main pipeline

https://ci.hibernate.org/job/hibernate-orm-pipeline/

This job takes care of testing additional DBs for:

* Primary branch builds
* Pull request builds

It is generally triggered on push,
but can also be triggered manually,
which is particularly useful to test more environments on a pull request.

See [Jenkinsfile](Jenkinsfile) for the job definition.

### Release pipeline

https://ci.hibernate.org/job/hibernate-orm-release/

This job takes care of releases. It is triggered manually.

See [ci/release/Jenkinsfile](ci/release/Jenkinsfile) for the job definition.

See [Releasing](#releasing) for more information.

## <a id="releasing"></a> Releasing

### Automated releases

On select maintenance branches (`6.2`, `6.4`, ...),
micro releases (`x.y.1`, `x.y.2`, ...) are performed as soon as you push to that branch.

Make sure to assign fix versions properly before merging pull requests.

No announcements are expected for such releases:
neither through X, blog posts, or email.

### Manual releases

On `main` and some maintenance branches (`6.5`, ...),
automated releases are disabled.

You must perform releases by manually triggering a CI job.

#### Preparing the release

In any case, before the release:

* Check that everything has been pushed to the upstream repository.
* Check that the [CI jobs](#continuous-integration) for the branch you want to release are green.
* Check Jira [Releases](https://hibernate.atlassian.net/projects/HHH?selectedItem=com.atlassian.jira.jira-projects-plugin%3Arelease-page):
  * Check that the release you are about to publish exists in Jira.
  * Check there are no outstanding issues assigned to that release.
  * Check there are no resolved/closed issues in the corresponding "work-in-progress version"
    (e.g. `6.6`, `6.6-next`, ... naming convention may vary);
    if there are, you might want to assign them to your release.

**If it is a new major or minor release**, before the release:

* Reset the migration guide to include only information relevant to the new major or minor.

**If it's a `.CR` or `.Final` release**, before the release:

* Check that the [migration guide](documentation/src/main/asciidoc/migration/index.adoc) is up to date.
  In particular, check the git history for API/SPI changes
  and document them in the migration guide.

#### Performing the release

Once you trigger the CI job, it automatically pushes artifacts to the
[OSSRH Maven Repository](https://repo1.maven.org/maven2/org/hibernate/orm/),
and the documentation to [docs.jboss.org](https://docs.jboss.org/hibernate/orm/).

* Do *not* mark the Jira Release as "released" or close issues,
  the release job does it for you.
* Do *not* update the repository (in particular changelog.txt and README.md), 
  the release job does it for you.
* Trigger the release on CI:
  * Go to CI, to [the "hibernate-orm-release" CI job](https://ci.hibernate.org/job/hibernate-orm-release/).
  * Click the "run" button (the green triangle on top of a clock, to the right) next to the branch you want to release.
  * **Be careful** when filling the form with the build parameters.
    Note only `RELEASE_VERSION` is absolutely necessary.
  * Note that for new branches where the job has never run, the first run may not ask for parameters and thus may fail:
    that's expected, just run it again.
* After the job succeeds, check the artifacts are available on Maven Central:
  https://repo1.maven.org/maven2/org/hibernate/orm/hibernate-core/.
  They should appear after a few minutes, sometimes a few hours.

#### Announcing the release

* Blog about release on [in.relation.to](https://github.com/hibernate/in.relation.to).
  Make sure to use the tags "Hibernate ORM" and "Releases" for the blog entry.
  Use [release-announcement.adoc](release-announcement.adoc) as a starting point.
* Update [hibernate.org](https://github.com/hibernate/hibernate.org) if necessary:
  * If it is a new major or minor release, add a `_data/projects/orm/releases/series.yml` file
    and a `orm/releases/<version>/index.adoc` file.
  * Adjust the release file in `_data/projects/orm/releases`: use a meaningful summary and set `announcement_url` to the blog post, if any.
  * Depending on which series you want to have displayed,
    make sure to adjust the `status`/`displayed` attributes of the `series.yml` file of the old series.
  * Push to the production branch.
* Send an email to `hibernate-announce@lists.jboss.org` and CC `hibernate-dev@lists.jboss.org`.
* Tweet about the release via the `@Hibernate` account.

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

* Reset [release-announcement.adoc](release-announcement.adoc).

**If it is a new major or minor release**:

* Reset the migration guide on the `main` branch if you forgot about it when preparing the release.
* Create a maintenance branch for the previous series, if necessary; see [branching](branching.adoc).
