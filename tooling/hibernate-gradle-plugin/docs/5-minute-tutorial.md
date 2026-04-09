<!--
  ~ Copyright 2004 - 2025 Red Hat, Inc.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" basis,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->

[![Hibernate](https://static.jboss.org/hibernate/images/hibernate_200x150.png)](https://tools.hibernate.org)

# Hibernate Tools Gradle : 5 Minute Tutorial

The best way to get to know the Hibernate Tools Gradle plugin is to start to use it. 
Hence we will provide a quick tutorial that gives you the first taste of it.
Before tackling this tutorial, make sure you have the [Gradle](https://gradle.org) build tool 
[installed](https://gradle.org/install/) and available on your machine.

## Create a Gradle Java Project

Let’s assume in this case that we start off with a very simple default Gradle Java application
that we create from a command-line window with the instruction below. 

```shell
gradle init --type java-application --dsl groovy
```

Gradle will ask you some details about your application. The conversation is shown below
for completenes but of course you can make your own choices.

```shell
Enter target Java version (min: 7, default: 21): 

Project name (default: 5-minute-tutorial): 

Select application structure:
  1: Single application project
  2: Application and library project
Enter selection (default: Single application project) [1..2] 1

Select test framework:
  1: JUnit 4
  2: TestNG
  3: Spock
  4: JUnit Jupiter
Enter selection (default: JUnit Jupiter) [1..4] 4

Generate build using new APIs and behavior (some features may change in the next minor release)? (default: no) [yes, no] 


> Task :init
Learn more about Gradle by exploring our Samples at https://docs.gradle.org/8.13/samples/sample_building_java_applications.html

BUILD SUCCESSFUL in 19s
1 actionable task: 1 executed
```

Now you should see two folders along with a number of Gradle specific files that have 
been created. It is beyond the scope of this short tutorial to explain all these artefacts.
We will focus mainly on the `build.gradle` file in the `app` folder. But to make the plugin work
you will need to modify the file `gradle.properties` that was generated in the root folder.

## Disable the Configuration Cache

Currently the Hibernate Tools Gradle plugin does not support the configuration cache. 
As the Gradle init task generates a `gradle.properties` file in the root folder that 
explicitly enables the use of the configuration cache, you will need to comment out 
or delete the line where this happens.

```properties
#org.gradle.configuration-cache=true
```

You could also get rid of the entire `gradle.properties` file as we don't need it for the purpose of this 
tutorial. Now we can tackle the `build.gradle` file.

## Modify the generated `app\build.gradle` file

We have to specify the use of the Gradle plugin in the `plugin` section of the `build.gradle` file.
So we add `id 'org.hibernate.tool.hibernate-tools-gradle' version '7.4.0.Final'` to that section.

```groovy
...
plugins {
    ...
    id 'org.hibernate.tool.hibernate-tools-gradle' version '7.4.0.Final'
}
...
```

Also we need to depend on the java library containing the [H2 database]() drivers.
This is done in the `dependencies` section of the `gradle.build` file, 
to which we add `implementation 'com.h2database:h2:2.4.240'`.

```groovy
...
dependencies {
    ...
    implementation 'com.h2database:h2:2.4.240'
}
...
```

You could as an alternative also replace the entire `gradle.build` file 
with the contents as shown below.

```groovy
plugins {
    id 'application'
    id 'org.hibernate.tool.hibernate-tools-gradle' version '7.4.0.Final'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation('com.h2database:h2:2.4.240')
}
```

With this in place, we need to make sure that the Hibernate Tools Gradle plugin knows where
to find the database from which to generate the artefacts. This is done by spefifying the 
Hibernate properties in the file `hibernate.properties`.

## Specify the Hibernate Properties

For the purpose of this tutorial introduction, let's assume that you have a database running, e.g.
[H2 Sakila database](https://github.com/hibernate/sakila-h2) reacheable at the following JDBC URL:
`jdbc:h2:tcp://localhost/./sakila`.

With this set up, the `hibernate.properties` file should contain the properties as specified below.

```properties
hibernate.connection.driver_class=org.h2.Driver
hibernate.connection.url=jdbc:h2:tcp://localhost/./sakila
hibernate.connection.username=sa
hibernate.default_catalog=SAKILA
hibernate.default_schema=PUBLIC
```

For the file to be found by the plugin, add it as a resource to the project in the 
`app/src/main/resources` subfolder.

## Run the Reverse Engineering

With all the previous elements in place, generating the Java classes from the Sakila database
becomes as simple as issuing `./gradlew generateJava` in your command line window.

```shell
me@machine 5-minute-tutorial % ./gradlew generateJava

> Task :app:generateJava
Starting Task 'generateJava'
Creating Java exporter
Loading the properties file : path/to/5-minute-tutorial/app/src/main/resources/hibernate.properties
Properties file is loaded
Starting Java export to directory: path/to/5-minute-tutorial/app/generated-sources...
...
Java export finished
Ending Task 'generateJava'
```

By default, you will find the files in the folder `app/generated-sources`.  

```
koen@Lateralus generated-sources % ls
Actor.java              City.java               Film.java               FilmCategory.java       Inventory.java          Rental.java
Address.java            Country.java            FilmActor.java          FilmCategoryId.java     Language.java           Staff.java
Category.java           Customer.java           FilmActorId.java        FilmText.java           Payment.java            Store.java
```

Congratulations! You have succesfully created Java classes for the Sakila database... Now it's
probably time to dive somewhat deeper in the available functionality.