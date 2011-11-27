##Hibernate Matrix Testing

#### Goal

Run hibernate-core _functional_ tests on other DBs besides default H2 easily.

Well, although [Functional Testing](http://en.wikipedia.org/wiki/Functional_testing) and [Unit Testing](http://en.wikipedia.org/wiki/Unit_testing) are already well known in the developer world, but we ( hibernate team ) use a little different definition:

###### Unit Test

Test doesn't need DB involved or only the default DB (currently is [H2](http://www.h2database.com/)) is fine, then we call it is a _unit test_.

###### Functional Test

Test which is used to verify a hibernate function and needs to make sure this function works fine on all DBs.

Just to be clear, in hibernate codebase, most tests we have are _functional tests_ by this definition.

And all hibernate _functional tests_ are also _unit tests_, since they are also supposed to pass on the default DB (H2).

#### MatrixTestingPlugin

Since Hibernate Core has moved to [Gradle](http://www.gradle.org/) from [Maven](http://maven.apache.org/), so we created a gradle plugin, called _MatrixTestingPlugin_, to run hibernate functional tests on the DB matrix (this is why it is called _MatrixTestingPlugin_ :).

The source of this plugin is [here](https://github.com/hibernate/hibernate-core/tree/master/buildSrc), this is used by Hibernate Core only right now, so this post is specific to hibernate core project only, hope one day we could denote it to the gradle community.

#### How to use this plugin

1. apply this plugin in your gradle build script (of course!)

    In [hibernate-core/hibernate-core.gradle](https://github.com/hibernate/hibernate-core/blob/master/hibernate-core/hibernate-core.gradle) we have this line:
  
    `apply plugin: org.hibernate.gradle.testing.matrix.MatrixTestingPlugin`

2. SourceSet separation
    
    Although it is possible to define a logical separation in Gradle (see [this](http://gradle.org/current/docs/javadoc/org/gradle/api/tasks/SourceSet.html)), I would like physical separation of unit tests and functional tests, so, we have this project structure:

        localhost:hibernate-core stliu$ tree -L 3
        ├── hibernate-core.gradle
        ├── src
            ├── main
            │   ├── antlr
            │   ├── java
            │   ├── javadoc
            │   ├── resources
            │   └── xjb
            ├── matrix
            │   └── java
            └── test
                ├── java
                └── resources

    * matrix/java
        
        all functional tests go into this directory

    * test/java 

        all unit tests go into this directory

    * test/resources

        all resources for ***functional tests and unit tests***, yes, resource files in this directory are shared for both, so you don't need to copy one file to both place, for example, log4j.properties.

    To make _idea plugin_ (and _eclipse plugin_) works, we also have this defined in hibernate-core.gradle:  
        
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
         

3. DB profile

    A DB profile defines the JDBC driver and DB connection info ( and hibernate properties for this DB ) that hibernate should use to run _functional tests_ on this DB.

    A DB profile looks like this:

        ├── mysql50
        │   ├── jdbc
        │   │   └── mysql-connector-java-5.1.9.jar
        │   ├── matrix.gradle
        │   └── resources
        │       └── hibernate.properties

    There are two ways to define JDBC driver, as showed above, put the driver jar file into `jdbc` directory, or use `matrix.gradle` file, below is something you should put into your `matrix.gradle` file:

        jdbcDependency "mysql:mysql-connector-java:5.1.17"

    As you can see, just add the driver's GAV into `jdbcDependency` configuration, then MatrixTestingPlugin will look it up from maven repository defined in the `build.gradle`  and add it to the `testCompile` scope.

    For DB connection info, you should add it into `resources/hibernate.properties` (if you have Redhat VPN access, you can use the DB maintained by JBoss QA and get the DB connection automatically through DB Allocator, see below).

    NOTE: this `hibernate.properties` will overrides the one in `hibernate-core/test/resources/hibernate.properties` if same property name defined in both place.

    And, the DB profile name is the directory name, in this example, it is _mysql50_.

    The default DB profile location is in `hibernate-core/databases` directory, but you can also reallocate it to another place by using system property `hibernate-matrix-databases`, see below for more details.

#### Matrix Tasks

Once you have DB profiles defined, you could run `gradle tasks --all`, this will list all tasks you can use.

For example, there will be a `matrix` task, which depends on all of other `matrix_${profile_name}` tasks, each DB profile has a `matrix_${profile_name}` task, so if you want to run hibernate functional tests on all DB profiles, then just call `gradle matrix` or if you want to run them on mysql only, then `gradle mysql50`.

In this case ( run `gradle matrix` or its sub-task `gradle matrix_${profile_name}` ), only tests in `src/matrix/java` will be ran, and `hibernate.properties` come from your `db profile/resources/hibernate.properties` (and `test/resources/hibernate.properties`).

Matrix test results are in `target/matrix/${profile_name}/test-results`.

But, there is also a `test` task, this task runs ***all*** tests, both `src/matrix/java` and `src/test/java`, with `src/test/resources` on default DB (as above said, all _functional tests_ are also _unit tests_).

Unit test results are in `target/test-results`, so if you run `test` task, all test results are in here, instead of `target/matrix/${profile_name}/test-results`, just as normal.

#### Configuration Properties

There are two way to pass a system property to gradle build:  
    
    1. The original java way, -Dxxx=yyy
    2. Add it to ${user.home}/.gradle/gradle.properties with a 'systemProp' prefix, like 'systemProp.xxx=yyy' 

* hibernate-matrix-databases

    This property is used to define the location of DB profile container.  
    Accept value: absolute path of DB profile container directory.

* hibernate-matrix-ignore

    This property is used to ignore some DB profiles (or all of them), so if you run `matrix`, the ignored profile matrix task won't be run.

    Accept value : _all_ or _${profile name1},${profile name2},${profile name3}_


#### DB Allocator (JBoss internally, VPN required)
  
For users who has access to JBoss QA lab (need Redhat VPN), here is a better way to run matrix tests, you don't need to have a DB instance on your side, but you can use DB instance in JBoss QA Lab.  

And the connection info can be queried from DB Allocator automatically.  

This feature is disabled by default, to enable it, you need this system property

* hibernate-matrix-dballcoation
    
    Accept value: _all_ or _${profile name1},${profile name2},${profile name3}_

For example, if you want to run matrix test on postgresql84, you can use this command

        ./gradlew clean test matrix_postgresql84 -Dhibernate-matrix-dballocation=postgresql84

    what does this command do actually?
    1. test
        run 'src/test/java' on default H2, test results in 'target/test-results'
        run 'src/matrix/java' on default H2, test results in 'target/test-results'

    2. query postgresql 84 db instance connection info
    3. run 'src/matrix/java' on postgresql 84 with 'databases/postgresql84/matrix.gradle' defined jdbc driver and 'databases/postgresql84/resources/hibernate.properties' and postgresql84 db instance connection info (this info will override those defined in hibernate.properties), test results in 'target/matrix/postgresql84/results'

Some DBs need we tweak url with some configurations after get it from DB allocator, so, we can use this system property:

* hibernate-matrix-dballocation-url-postfix-${dbname}

    for example:

        `-Dhibernate-matrix-dballocation-url-postfix-sybase155="?SQLINITSTRING=set quoted_identifier on&amp;DYNAMIC_PREPARE=true"`

* hibernate-matrix-dballocation-requestee
    This property is used to define the DB Allocator requester name, default is _hibernate_
