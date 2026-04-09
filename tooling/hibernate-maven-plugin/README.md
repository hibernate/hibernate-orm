<!--
  ~ Copyright 2016 - 2025 Red Hat, Inc.
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

# Hibernate Tools Maven Plugin

## Overview

This [Maven](http://maven.apache.org/) plugin brings the power of the [Hibernate Tools API](../orm) to your Maven build. If you are looking for a quick tutorial on the use of the Hibernate Tools Maven plugin, we can refer you to the [5 minute tutorial](docs/5-minute-tutorial.md).

The plugin contains 3 goals. Issuing `mvn help:describe -Dplugin=org.hibernate.tool:hibernate-tools-maven` at the command line will give you an overview:

```
foo@bar ~ % mvn help:describe -Dplugin=org.hibernate.tool:hibernate-tools-maven        
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------------< org.apache.maven:standalone-pom >-------------------
[INFO] Building Maven Stub Project (No POM) 1
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] 
[INFO] --- maven-help-plugin:3.2.0:describe (default-cli) @ standalone-pom ---
[INFO] org.hibernate.tool:hibernate-tools-maven:6.0.0-SNAPSHOT

Name: Hibernate Tools Maven Plugin
Description: Maven plugin to provide hibernate-tools reverse engineering and
  code/schema generation abilities.
Group Id: org.hibernate.tool
Artifact Id: hibernate-tools-maven
Version: 6.0.0-SNAPSHOT
Goal Prefix: hibernate-tools

This plugin has 3 goals:

hibernate-tools:hbm2ddl
  Description: Mojo to generate DDL Scripts from an existing database.
    See
    https://docs.jboss.org/tools/latest/en/hibernatetools/html_single/#d0e4651

hibernate-tools:hbm2java
  Description: Mojo to generate Java JPA Entities from an existing database.
    See:
    https://docs.jboss.org/tools/latest/en/hibernatetools/html_single/#d0e4821

hibernate-tools:help
  Description: Display help information on hibernate-tools-maven.
    Call mvn hibernate-tools:help -Ddetail=true -Dgoal=<goal-name> to display
    parameter details.

For more information, run 'mvn help:describe [...] -Ddetail'

[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  1.489 s
[INFO] Finished at: 2020-03-10T10:19:38+01:00
[INFO] ------------------------------------------------------------------------
foo@bar ~ % 
```

## hibernate-tools-maven:hbm2ddl

The `hbm2ddl` goal allows you to start from a Hibernate metamodel and create DDL and possibly execute it against a database. 

You can issue `mvn org.hibernate.tool:hibernate-tools-maven:help -Ddetail=true -Dgoal=hbm2ddl` at the command line to get an overview of all the possible paremeters:

```
foo@bar ~ % mvn org.hibernate.tool:hibernate-tools-maven:help -Ddetail=true -Dgoal=hbm2ddl
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------------< org.apache.maven:standalone-pom >-------------------
[INFO] Building Maven Stub Project (No POM) 1
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] 
[INFO] --- hibernate-tools-maven:6.0.0-SNAPSHOT:help (default-cli) @ standalone-pom ---
[INFO] Hibernate Tools Maven Plugin 6.0.0-SNAPSHOT
  Maven plugin to provide hibernate-tools reverse engineering and code/schema
  generation abilities.

hibernate-tools:hbm2ddl
  Mojo to generate DDL Scripts from an existing database.
  See https://docs.jboss.org/tools/latest/en/hibernatetools/html_single/#d0e4651

  Available parameters:

    createCollectionForForeignKey (Default: true)
      If true, a collection will be mapped for each foreignkey.

    createManyToOneForForeignKey (Default: true)
      If true, a many-to-one association will be created for each foreignkey
      found.

    delimiter (Default: ;)
      Set the end of statement delimiter.

    detectManyToMany (Default: true)
      If true, tables which are pure many-to-many link tables will be mapped as
      such. A pure many-to-many table is one which primary-key contains exactly
      two foreign-keys pointing to other entity tables and has no other columns.

    detectOneToOne (Default: true)
      If true, a one-to-one association will be created for each foreignkey
      found.

    detectOptimisticLock (Default: true)
      If true, columns named VERSION or TIMESTAMP with appropriate types will be
      mapped with the appropriate optimistic locking corresponding to <version>
      or <timestamp>.

    format (Default: true)
      Should we format the sql strings?

    haltOnError (Default: true)
      Should we stop once an error occurs?

    outputDirectory (Default: ${project.build.directory}/generated-resources/)
      The directory into which the DDLs will be generated.

    outputFileName (Default: schema.ddl)
      The default filename of the generated DDL script.

    packageName
      The default package name to use when mappings for classes are created.

    propertyFile (Default:
    ${project.basedir}/src/main/hibernate/hibernate.properties)
      The name of a property file, e.g. hibernate.properties.

    revengFile
      The name of a property file, e.g. hibernate.properties.

    revengStrategy
      The class name of the reverse engineering strategy to use. Extend the
      DefaultReverseEngineeringStrategy and override the corresponding methods,
      e.g. to adapt the generate class names or to provide custom type mappings.

    schemaExportAction (Default: CREATE)
      The DDLs statements to create.
      - NONE: None - duh :P.
      - CREATE (default): Create only.
      - DROP: Drop only.
      - BOTH: Drop and then create.

    targetTypes (Default: SCRIPT)
      The type of output to produce.
      - DATABASE: Export to the database.
      - SCRIPT (default): Write to a script file.
      - STDOUT: Write to System.out.


[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  0.718 s
[INFO] Finished at: 2020-03-10T10:23:55+01:00
[INFO] ------------------------------------------------------------------------
foo@bar ~ % 
```

## hibernate-tools-maven:hbm2java

The `hbm2java` goal allows you to start from an existing database and generate JPA entities that will map to this database. A lot of possible options allow you to control the generation output. 

You can issue `mvn org.hibernate.tool:hibernate-tools-maven:help -Ddetail=true -Dgoal=hbm2java` at the command line to get an overview of all the possible paremeters:

```
foo@bar ~ % mvn org.hibernate.tool:hibernate-tools-maven:help -Ddetail=true -Dgoal=hbm2java
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------------< org.apache.maven:standalone-pom >-------------------
[INFO] Building Maven Stub Project (No POM) 1
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] 
[INFO] --- hibernate-tools-maven:6.0.0-SNAPSHOT:help (default-cli) @ standalone-pom ---
[INFO] Hibernate Tools Maven Plugin 6.0.0-SNAPSHOT
  Maven plugin to provide hibernate-tools reverse engineering and code/schema
  generation abilities.

hibernate-tools:hbm2java
  Mojo to generate Java JPA Entities from an existing database.
  See:
  https://docs.jboss.org/tools/latest/en/hibernatetools/html_single/#d0e4821

  Available parameters:

    createCollectionForForeignKey (Default: true)
      If true, a collection will be mapped for each foreignkey.

    createManyToOneForForeignKey (Default: true)
      If true, a many-to-one association will be created for each foreignkey
      found.

    detectManyToMany (Default: true)
      If true, tables which are pure many-to-many link tables will be mapped as
      such. A pure many-to-many table is one which primary-key contains exactly
      two foreign-keys pointing to other entity tables and has no other columns.

    detectOneToOne (Default: true)
      If true, a one-to-one association will be created for each foreignkey
      found.

    detectOptimisticLock (Default: true)
      If true, columns named VERSION or TIMESTAMP with appropriate types will be
      mapped with the appropriate optimistic locking corresponding to <version>
      or <timestamp>.

    ejb3 (Default: false)
      Code will contain JPA features, e.g. using annotations from
      jakarta.persistence and org.hibernate.annotations.

    jdk5 (Default: false)
      Code will contain JDK 5 constructs such as generics and static imports.

    outputDirectory (Default: ${project.build.directory}/generated-sources/)
      The directory into which the JPA entities will be generated.

    packageName
      The default package name to use when mappings for classes are created.

    propertyFile (Default:
    ${project.basedir}/src/main/hibernate/hibernate.properties)
      The name of a property file, e.g. hibernate.properties.

    revengFile
      The name of a property file, e.g. hibernate.properties.

    revengStrategy
      The class name of the reverse engineering strategy to use. Extend the
      DefaultReverseEngineeringStrategy and override the corresponding methods,
      e.g. to adapt the generate class names or to provide custom type mappings.

    templatePath
      A path used for looking up user-edited templates.


[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  0.814 s
[INFO] Finished at: 2020-03-10T10:37:54+01:00
[INFO] ------------------------------------------------------------------------
foo@bar ~ %
```

## hibernate-tools-maven:help

The `help` goal can be used to print out the previous information. You can issue `mvn org.hibernate.tool:hibernate-tools-maven:help -Ddetail=true -Dgoal=help` to get all the details of this goal.

```
foo@bar ~ % mvn org.hibernate.tool:hibernate-tools-maven:help -Ddetail=true -Dgoal=help
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------------< org.apache.maven:standalone-pom >-------------------
[INFO] Building Maven Stub Project (No POM) 1
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] 
[INFO] --- hibernate-tools-maven:6.0.0-SNAPSHOT:help (default-cli) @ standalone-pom ---
[INFO] Hibernate Tools Maven Plugin 6.0.0-SNAPSHOT
  Maven plugin to provide hibernate-tools reverse engineering and code/schema
  generation abilities.

hibernate-tools:help
  Display help information on hibernate-tools-maven.
  Call mvn hibernate-tools:help -Ddetail=true -Dgoal=<goal-name> to display
  parameter details.

  Available parameters:

    detail (Default: false)
      If true, display all settable properties for each goal.
      User property: detail

    goal
      The name of the goal for which to show help. If unspecified, all goals
      will be displayed.
      User property: goal

    indentSize (Default: 2)
      The number of spaces per indentation level, should be positive.
      User property: indentSize

    lineLength (Default: 80)
      The maximum length of a display line, should be positive.
      User property: lineLength


[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  0.675 s
[INFO] Finished at: 2020-03-10T10:16:19+01:00
[INFO] ------------------------------------------------------------------------
foo@bar ~ % 
```
