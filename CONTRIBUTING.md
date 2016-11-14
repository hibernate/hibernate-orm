# Guidelines for Contributing

Contributions from the community are essential in keeping Hibernate (any Open Source
project really) strong and successful.  While we try to keep requirements for
contributing to a minimum, there are a few guidelines we ask that you mind.

## Getting Started

If you are just getting started with Git, GitHub and/or contributing to Hibernate via
GitHub there are a few pre-requisite steps.

* Make sure you have signed a [Contributor License Agreement](https://cla.jboss.org) (CLA) for the Hibernate project
* Make sure you have a [Hibernate JIRA account](https://hibernate.atlassian.net)
* Make sure you have a [GitHub account](https://github.com/signup/free)
* [Fork](https://help.github.com/articles/fork-a-repo) the Hibernate repository.  As discussed in
the linked page, this also includes:
    * [Set](https://help.github.com/articles/set-up-git) up your local git install
    * Clone your fork
* See the wiki pages for setting up your IDE, whether you use [IntelliJ IDEA](https://community.jboss.org/wiki/ContributingToHibernateUsingIntelliJ)
or [Eclipse](https://community.jboss.org/wiki/ContributingToHibernateUsingEclipse).

## Create the working (topic) branch

Create a [topic branch](http://git-scm.com/book/en/Git-Branching-Branching-Workflows#Topic-Branches) on which you
will work.  The convention is to name the branch using the JIRA issue key.  If there is not already a JIRA issue
covering the work you want to do, create one.  Assuming you will be working from the master branch and working
on the JIRA HHH-123 : `git checkout -b HHH-123 master`


## Code

Do yo thing!

## Commit

* Make commits of logical units.
* Be sure to use the JIRA issue key in the commit message.  This is how JIRA will pick
up the related commits and display them on the JIRA issue.
* Make sure you have added the necessary tests for your changes.
* Run _all_ the tests to assure nothing else was accidentally broken.
* Make sure your source does not violate the checkstyles.

_Prior to committing, if you want to pull in the latest upstream changes (highly
appreciated btw), please use rebasing rather than merging.  Merging creates
"merge commits" that really muck up the project timeline._

## Submit

* If you have not already, sign the [Contributor License Agreement](https://cla.jboss.org).
* Push your changes to the topic branch in your fork of the repository.
* Initiate a [pull request](http://help.github.com/articles/creating-a-pull-request)
* Update the JIRA issue, adding a comment including a link to the created pull request
