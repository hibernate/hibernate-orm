org.hibernate.gradle.testing.matrix.MatrixTestingPlugin

Goal:
Run hibernate-core functional test on other DBs besides default H2 easily.

Usage:
1. Databases configuration
In the root hibernate-core project, we have a 'databases' directory, this is the default DB profiles location, take it as an example and you can create your db profile easily.

Take a look of the sub-directories of 'databases' to see how each DB profile is defined.
Each default DB profile has a matrix.gradle, we use this to get jdbc driver from a maven repo.
but we also accept a 'jbdc' directory which contains jdbc driver directly to replace this matrix.gradle, this is useful for non-opensource jdbc drivers.

And each DB profile also needs a "resources/hibernate.properties", which, as you expected, defines DB connection info and others hibernate properties.

For now, we have 5 default DB profiles, mysql50, mysql51, postgresql82, postgresql83 and postgresql84.

You can create your DB profile under 'hibernate-core/databases', but if you do that, GIT will ask you check in or remove it everytime when you run git command.
so, you should create a 'databases/${your db profile}' in other place and use system property 'hibernate-matrix-databases' with the full path of your 'databases'.
Btw, to add a system properties, you can either use '-Dhibernate-matrix-databases' or add it to ${user.home}/.gradle/gradle.properties with a 'systemProp' perfix, like 'systemProp.hibernate-matrix-databases=${your databases directory path}'
and if this property is given, MatrixTestingPlugin will take profiles defined in there as well as default 'hibernate-core/databases'

MatrixTestingPlugin also has a system property "hibernate-matrix-ignore" which can be use to ignore some DB profiles or all of them, it accepts values like:
'all' -- ignore all DB profiles, so matrix testing will be ignored.
'${profile name1},${profile name2},${profile name3}'

2. Gradle Tasks

run './gradlew tasks --all', you will see there is a 'matrix' task and also 'matrix_${profile_name}' tasks against your DB profiles (not ignored).

3. Source Set separation
All functional tests[1] should go into src/matrix/java
All unit tests[2] should go into src/test/java

if you run gradle task 'test', tests in src/test/java and src/matrix/java will be ran, and resources of src/test/resource will be used.
so, "functional tests" defined in 'src/matrix/java' here are also 'unit test' which run on default H2.
all test results (test and matrix) are in target/test-results.

if you run gradle task 'matrix' (or its sub-task 'matrix_${profile name}), only tests in 'src/matrix/java' will be ran, and hibernate.properties come from your db profile/resources/hibernate.properties.

3. DBAllocation

For users who has access to JBoss QA lab (need redhat vpn), here is an better way to run matrix tests, you don't need to have a DB instance on your side, but you can use DB instance in JBoss QA Lab.
And the connection info can be queried by DBAllocation automatically.
This feature is disabled by default, to enable it, you need system property 'hibernate-matrix-dballcoation', it accepts value like: 'all', '${profile name1},${profile name2},${profile name3}'

for example, if you want to run matrix test on postgresql84, default DB profile, you can 
'./gradlew clean test matrix_postgresql84 -Dhibernate-matrix-dballocation=postgresql84'

some DBs need we tweak url with some configurations after get it from DB allocator, so, we can use this system property:
"hibernate-matrix-dballocation-url-postfix-${dbname} = configurations"
for example:

-Dhibernate-matrix-dballocation-url-postfix-sybase155="?SQLINITSTRING=set quoted_identifier on&amp;DYNAMIC_PREPARE=true"

what does this command do actually?
1. test
run 'src/test/java' on default H2, test results in 'target/test-results'
run 'src/matrix/java' on default H2, test results in 'target/test-results'

2. query postgresql 84 db instance connection info
3. run 'src/matrix/java' on postgresql 84 with 'databases/postgresql84/matrix.gradle' defined jdbc driver and 'databases/postgresql84/resources/hibernate.properties' and postgresql84 db instance connection info (this info will override those defined in hibernate.properties), test results in 'target/matrix/postgresql84/results'


[1] Functional Test in this document means test need to run on DB matrix
[2] Unit Test in this document means test does not care the underlying DB (H2) or does not need a DB involved.
* others may have different "functional test"/"unit test" definition :)
* use 'hibernate-matrix-dballocation-requestee' to define requestee, default is 'hibernate'