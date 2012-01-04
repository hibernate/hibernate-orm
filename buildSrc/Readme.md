##Hibernate Matrix Testing

### Goal

The idea of matrix testing is to allow testing in a varied set of configurations.  Specifically for Hibernate, this
correlates to running the same set of tests against against multiple databases.  This goal is achieved through
2 Gradle plugins.

Note that the second plugin (org.hibernate.build.gradle.testing.matrix.MatrixTestingPlugin) applies the first
one (org.hibernate.build.gradle.testing.database.DatabaseProfilePlugin) automatically, so generally scripts would
not even reference it.  The reason for the split is historical and these 2 may get merged later...


### org.hibernate.build.gradle.testing.database.DatabaseProfilePlugin

This plugin is responsible for determining which databases are available for testing in the given environment.  It
does this by performing a directory search.  Well actually it can perform up to 2 directory searches:

*    The standard profile directory is named _databases_ at the base directory of the root project
*    A custom profile directory, which can be named by setting a system property named _hibernate-matrix-databases_

These directories are searched recursively.  We leverage this in Hibernate to allow the standard _databases_ directory
to hold local profiles too.  That is achieved by a _.gitignore_ which says to ignore any directory named
_local_ under the directory _databases_.  So one option to provide custom profiles is to drop them in there.  That
has the benefit of not having to specify _hibernate-matrix-databases_
Within these directories, the plugin looks for sub-directories which either:

*    contain a file named _matrix.gradle_.  _matrix.gradle_ is a limited DSL Gradle file which currently understands
     just a specialized org.gradle.api.artifacts.Configuration reference named _jdbcDependency_.  All that is a fancy
     way to say that _matrix.gradle_ allows you to specify some dependencies this database profile needs (JDBC drivers,
     etc).  Any dependency artifacts named here get resolved using whatever resolvers (Maven, etc) are associated with
     the build.  For example

        jdbcDependency {
            "mysql:mysql-connector-java:5.1.17"
        }
*    contain a directory named _jdbc_ which is assumed to hold jar file(s) needed for the profile.

Such directories become the basis of a database profile made available to the build.  The name of the profile
(which becomes important when we discuss the next plugin) is taken from the directory name.  Database profiles can
also contain a _resources_ directory.

An example layout using _matrix.gradle_ might be

        ├── mysql50
        │   ├── jdbc
        │   │   └── mysql-connector-java-5.1.9.jar
        │   └── resources
        │       └── hibernate.properties

Or

        ├── mysql50
        │   ├── matrix.gradle
        │   └── resources
        │       └── hibernate.properties


Either would result in a database profile named _mysql50_

Profiles can be ignored using the *hibernate-matrix-ignore* setting which accepts either

*   a comma-separated list of the database profile names to be skipped
*   the magic value **all** which indicates to ignore all profiles


### org.hibernate.build.gradle.testing.matrix.MatrixTestingPlugin

The MatrixTestingPlugin essentially generates a bunch of Gradle tasks dynamically and adds them to your build.  It does
this based on all the database profiles found.  Running `gradle tasks --all` will list all tasks available to the build
including these generated ones.

For each database profile the plugin will generate a task named *matrix_{profile}* that executes the tests against
that particular database profile.  It also generates a task named *matrix* that groups together all the
profile-specific tasks so that running `gradle matrix` will run all the profiles.

*see section below discussing SourceSet separation*


### Database Allocator (JBoss internally, VPN required)

For developers on the Red Hat VPN, one option is to use the databases in the JBoss QA lab for testing.  Note that
this tends to result in **very** slow builds but the obvious trade off is not having to install and manage these
databases locally.

The JBoss QA team developed a servlet to allow management of "database allocations" including requesting an
allocation be set up.  The MatrixTestingPlugin is able to play with that feature allowing you to ask the build
to allocate the database for you.  This feature is disabled by default, to enable it, you need this system property
named _hibernate-matrix-dballcoation_ which accepts either

*   a comma-separate list of profile names
*   the magic value **all** which indicates to allocate for all **supported** databases (see
    org.hibernate.build.qalab.DatabaseAllocator.SUPPORTED_DB_NAMES for details)

For example, if you want to run matrix test on PostgreSQL 8.4, knowing that the database name for that is
_postgresql84_, you can use this command:

        gradle matrix_postgresql84 -Dhibernate-matrix-dballocation=postgresql84

which would

1.  talk to the database allocator service and make a database instance available
2.  use the information returned from the allocator service to properly set up the connection information
    Hibernate would need to connect to that instance.
3.  run the tests against the postgresql84 profile

For some databases we need adjust the connection url with some options after get it from the database allocator.  In
these cases we can use the system property _hibernate-matrix-dballocation-url-postfix-${dbname}_.  For example
    `-Dhibernate-matrix-dballocation-url-postfix-sybase155="?SQLINITSTRING=set quoted_identifier on&amp;DYNAMIC_PREPARE=true"`

A useful parameter to the allocator service when allocating a database is the _requester_ which is basically just a
string meant to identify who is making the request.  By default the Hibernate build uses _hibernate_.  But you can
specify an alternate requester using the system property _hibernate-matrix-dballocation-requestee_


### Testing SourceSets

If you are not familiar with Gradle's notion of
[SourceSet](http://gradle.org/current/docs/javadoc/org/gradle/api/tasks/SourceSet.html), you should be :)

The Hibernate build defines 2 different testing related SourceSets in a number of modules (currently hibernate-core,
hibernate-entitymanager and hibernate-envers):

*   _test_ - tests that **should not** be run against the profiles from the MatrixTestingPlugin
*   _matrix_ - tests that **should** be run against the profiles from the MatrixTestingPlugin

Tests in _test_ include unit tests as well as a few functional tests which use a database but where the particular
database should not at all affect the outcome.  Tests in _matrix_ are functional tests where the outcome of the tests
are highly dependent on the database being used (how pessimistic locks are acquired, etc).

As always, Wikipedia is a great source of information

*   [Functional Testing](http://en.wikipedia.org/wiki/Functional_testing)
*   [Unit Testing](http://en.wikipedia.org/wiki/Unit_testing)

hibernate-core directory layout (for discussion):

    hibernate-core
    ├── hibernate-core.gradle
    ├── src
        ├── main
        │   ├── antlr
        │   ├── java
        │   ├── javadoc
        │   ├── resources
        │   └── xjb
        ├── matrix
        │   └── java
        └── test
            ├── java
            └── resources

The directories of interest include

*   matrix/java

    all functional tests go into this directory

*   test/java

    all unit tests go into this directory

*   test/resources

    all resources for **functional tests and unit tests**.  Yes, resource files in this directory are shared for both, so you don't need to copy one file to both place, for example, log4j.properties.

To make _idea plugin_ (similar entries for _eclipse plugin_) works, we also have this defined in hibernate-core.gradle:

    sourceSets {
        matrix {
            java {
                srcDir 'src/matrix/java'
            }
            resources {
                srcDir 'src/matrix/resources'
            }
        }
    }

    ideaModule {
        sourceDirs += file( '$buildDir/generated-src/antlr/main' )
        testSourceDirs += file( 'src/matrix/java')
        testSourceDirs += file( 'src/matrix/resources')
    }
