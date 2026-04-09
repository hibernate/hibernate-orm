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

# Hibernate Tools Ant : 5 Minute Tutorial

The best way to get to know the Hibernate Tools Ant task is to start to use it. 
Hence we will provide a quick tutorial that gives you the first taste of it. 
Before tackling this tutorial, make sure you have [Apache Ant](https://ant.apache.org) installed and available 
on your machine.

## Create a Java Project

Use a command line tool or your preferred IDE to create a basic Java project in a location
of your choice and add the well known ```build.xml``` file to its root.

```xml
<project xmlns:ivy="antlib:org.apache.ivy.ant">
  ...
</project>
```

As you can see, we will be using [Apache Ivy](https://ant.apache.org/ivy/) to handle the 
library dependencies for us.  

## Define the Project Class Path
We add an ```ìvy:cachepath``` tag for the 'hibernate-tools-ant' library that will supply us with the
reverse engineering functionality and another one for the 'h2' database that we will be using
for the purpose of this short tutorial.

```xml
<project xmlns:ivy="antlib:org.apache.ivy.ant">

    <property name="hibernate.tools.version" value="the-hibernate-tools-version-to-use, e.g. 7.4.0.Final"/>
    <property name="h2.version" value="the-h2-version-to-use, e.g. 2.4.240"/>

    <ivy:cachepath organisation="org.hibernate.tool" module="hibernate-tools-ant" revision="${hibernate.tools.version}"
                   pathid="hibernate-tools" inline="true"/>
    <ivy:cachepath organisation="com.h2database" module="h2" revision="${h2.version}"
                   pathid="h2" inline="true"/>

    <path id="classpath">
        <path refid="hibernate-tools"/>
        <path refid="h2"/>
    </path>

    ...

</project>
```
Don't forget to substitute the property values for the two version properties with the actual 
versions that you want to use. When this is done, we combine the two ivy paths into the 
classpath that we will use to perform our reverse engineering.

## Add the Reverse Engineering Task
Before we can add the `reveng` target that will perform the actual reverse engineering, 
we need to add the task definition. We define the task with the name `hibernatetool` 
but you can choose any name you like and use the Java class `org.hibernate.tool.ant.HibernateToolTask` that is to be found in the hibernate-tool-ant 
library on the class path created earlier.

```xml
<project xmlns:ivy="antlib:org.apache.ivy.ant">
    
    ...
    
    <taskdef name="hibernatetool"
             classname="org.hibernate.tool.ant.HibernateToolTask"
             classpathref="classpath" />

    <target name="reveng">
        <hibernatetool destdir="generated-sources">
            <jdbcconfiguration propertyfile="hibernate.properties" />
            <hbm2java/>
        </hibernatetool>
    </target>

</project>
```
As a final step, at least with respect to the `build.xml` file, we create the `reveng` target
and add the `hibernatetool` task. The `destdir` property on this task points to the folder
where the artefacts will be generated. The nested `jdbcconfiguration` task uses a Hibernate 
properties file (i.e. `hibernate.properties`) for some specific settings to create the reverse 
engineering configuration. The nested `hbm2java` task will result in the generation of Java
source files. Now as a last step before we can run ant, we need to add this 
`hibernate.properties` file. 

## Specify the Hibernate Properties

For the purpose of this tutorial introduction, let's assume that you have a database running, e.g.
[H2 Sakila database](https://github.com/hibernate/sakila-h2) reacheable at the following JDBC URL:
`jdbc:h2:tcp://localhost/./sakila`.

With this assumption, the `hibernate.properties` file, to be found in the root of our Java
project should have the following content:

```properties
hibernate.connection.driver_class=org.h2.Driver
hibernate.connection.url=jdbc:h2:tcp://localhost/./sakila
hibernate.connection.username=sa
hibernate.default_catalog=SAKILA
hibernate.default_schema=PUBLIC
```

## Run the Reverse Engineering

With all the previous elements in place, generating the Java classes from the Sakila database
becomes as simple as issuing `ant reveng` in a command line window.

Congratulations! You have succesfully created Java classes for the Sakila database... Now it's
probably time to dive somewhat deeper in the available functionality.