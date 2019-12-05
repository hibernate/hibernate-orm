<img src="http://static.jboss.org/hibernate/images/hibernate_logo_whitebkg_200px.png" />


Hibernate ORM is a library providing Object/Relational Mapping (ORM) support
to applications, libraries, and frameworks.

It also provides an implementation of the JPA specification, which is the standard Java specification for ORM.

This is the repository of its source code: see [Hibernate.org](http://hibernate.org/orm/) for additional information.

[![Build Status](http://ci.hibernate.org/job/hibernate-orm-master-h2-main/badge/icon)](http://ci.hibernate.org/job/hibernate-orm-master-h2-main/)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/hibernate/hibernate-orm.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/hibernate/hibernate-orm/context:java)

Building from sources
=========

The build requires a Java 8 JDK as JAVA_HOME.

You will need [Git](https://git-scm.com/) to obtain the [source](https://github.com/hibernate/hibernate-orm/).

Hibernate uses [Gradle](https://gradle.org) as its build tool.  See the _Gradle Primer_ section below if you are new to
Gradle.

Contributors should read the [Contributing Guide](CONTRIBUTING.md).

See the guides for setting up [IntelliJ](https://developer.jboss.org/wiki/ContributingToHibernateUsingIntelliJ) or
[Eclipse](https://developer.jboss.org/wiki/ContributingToHibernateUsingEclipse) as your development environment.

Check out the _Getting Started_ section in CONTRIBUTING.md for getting started working on Hibernate source.


Continuous Integration
=========

Hibernate makes use of [Jenkins](http://jenkins-ci.org) for its CI needs.  The project is built continuous on each 
push to the upstream repository.   Overall there are a few different jobs, all of which can be seen at 
[http://ci.hibernate.org/view/ORM/](http://ci.hibernate.org/view/ORM/)


Gradle primer
=========

This section describes some of the basics developers and contributors new to Gradle might 
need to know to get productive quickly.  The Gradle documentation is very well done; 2 in 
particular that are indispensable:

* [Gradle User Guide](https://docs.gradle.org/current/userguide/userguide_single.html) is a typical user guide in that
it follows a topical approach to describing all of the capabilities of Gradle.
* [Gradle DSL Guide](https://docs.gradle.org/current/dsl/index.html) is unique and excellent in quickly
getting up to speed on certain aspects of Gradle.


Using the Gradle Wrapper
------------------------

For contributors who do not otherwise use Gradle and do not want to install it, Gradle offers a very cool
feature called the wrapper.  It lets you run Gradle builds without a previously installed Gradle distro in 
a zero-conf manner.  Hibernate configures the Gradle wrapper for you.  If you would rather use the wrapper and 
not install Gradle (or to make sure you use the version of Gradle intended for older builds) you would just use
the command `gradlew` (or `gradlew.bat`) rather than `gradle` (or `gradle.bat`) in the following discussions.
Note that `gradlew` is only available in the project's root dir, so depending on your working directory you may
need to adjust the path to `gradlew` as well.

Examples use the `gradle` syntax, but just swap `gradlew` (properly relative) for `gradle` if you wish to use 
the wrapper.

Another reason to use `gradlew` is that it uses the exact version of Gradle that the build is defined to work with.


Executing Tasks
------------------------

Gradle uses the concept of build tasks (equivalent to Ant targets or Maven phases/goals). You can get a list of
available tasks via 

    gradle tasks

To execute a task across all modules, simply perform that task from the root directory.  Gradle will visit each
sub-project and execute that task if the sub-project defines it.  To execute a task in a specific module you can 
either:

1. `cd` into that module directory and execute the task
2. name the "task path".  For example, to run the tests for the _hibernate-core_ module from the root directory you could say `gradle hibernate-core:test`

Common Java related tasks
------------------------

* _build_ - Assembles (jars) and tests this project
* _buildDependents_ - Assembles and tests this project and all projects that depend on it.  So think of running this in hibernate-core, Gradle would assemble and test hibernate-core as well as hibernate-envers (because envers depends on core)
* _classes_ - Compiles the main classes
* _testClasses_ - Compiles the test classes
* _compile_ (Hibernate addition) - Performs all compilation tasks including staging resources from both main and test
* _jar_ - Generates a jar archive with all the compiled classes
* _test_ - Runs the tests
* _publish_ - Think Maven deploy
* _publishToMavenLocal_ - Installs the project jar to your local maven cache (aka ~/.m2/repository).  Note that Gradle 
never uses this, but it can be useful for testing your build with other local Maven-based builds.
* _eclipse_ - Generates an Eclipse project
* _idea_ - Generates an IntelliJ/IDEA project (although the preferred approach is to use IntelliJ's Gradle import).
* _clean_ - Cleans the build directory


Testing and databases
=====================

Testing against a specific database can be achieved in 2 different ways:


Using the "Matrix Testing Plugin" for Gradle.
---------------------------------------------

Coming soon...


Using "profiles"
------------------------

The Hibernate build defines several database testing "profiles" in `databases.gradle`.  These
profiles can be activated by name using the `db` build property which can be passed either as
a JVM system prop (`-D`) or as a Gradle project property (`-P`).  Examples below use the Gradle
project property approach.

    gradle clean build -Pdb=pgsql

To run a test from your IDE, you need to ensure the property expansions happen.
Use the following command:

    gradle clean compile -Pdb=pgsql

_*NOTE: If you are running tests against a JDBC driver that is not available via Maven central be sure to add these drivers to your local Maven repo cache (~/.m2/repository) or (better) add it to a personal Maven repo server*_

Running database-specific tests from the IDE using "profiles"
-------------------------------------------------------------

You can run any test on any particular database that is configured in a `databases.gradle` profile.

All you have to do is run the following command:

    gradlew setDataBase -Pdb=pgsql
    
or you can use the shortcut version:    

    gradlew sDB -Pdb=pgsql
    
You can do this from the module which you are interested in testing or from the `hibernate-orm` root folder.

Afterward, just pick any test from the IDE and run it as usual. Hibernate will pick the database configuration from the `hibernate.properties`
file that was set up by the `setDataBase` Gradle task.
