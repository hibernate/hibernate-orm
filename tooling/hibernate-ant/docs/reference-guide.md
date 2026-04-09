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

# Hibernate Tools Ant : Reference Guide

## 1. The `<hibernatetool>` Task

### 1.1 `<hibernatetool>` Task Definition
To use the Ant tasks you need to have the `<hibernatetool>` task defined. 
That is done in your `build.xml` file by inserting the following XML.

```xml
<ivy:cachepath organisation="org.hibernate.tool" module="hibernate-tools-ant" revision="${hibernate-tools.version}"
               pathid="hibernate-tools.path" inline="true"/>
<ivy:cachepath organisation="${jdbc-driver.org}" module="${jdbc-driver.module}" revision="${jdbc-driver.version}"
               pathid="jdbc-driver.path" inline="true"/>

<path id="classpath">
    <path refid="hibernate-tools.path"/>
    <path refid="jdbc-driver.path"/>
</path>


<taskdef name="hibernatetool"
         classname="org.hibernate.tool.ant.HibernateToolTask"
         classpathref="classpath" />
```
The `<taskdef>`in the snippet above defines an Ant task called `hibernatetool` 
which now can be used anywhere in your `build.xml` file.

The snippet above also uses [Apache Ivy](https://ant.apache.org/ivy/) to handle the library dependencies. 
Of course you could also explicitly handle these dependencies yourself in the `build.xml` 
file. In addition, you will need to define properties (or replace the variables) for
the jdbc driver and for the version information. See an example in the snippet below:

```xml
<property name="hibernate-tools.version" value="7.4.0.Final"/>
<property name="jdbc-driver.org" value="com.h2database"/>
<property name="jdbc-driver.module" value="h2"/>
<property name="jdbc-driver.version" value="2.4.240"/>
```

### 1.2 `<hibernatetool>` Task Element

As an introductory example, look at the XML snippet below.
```xml
<target name="reveng">
    <hibernatetool destdir="generated-sources">
        <jdbcconfiguration propertyfile="hibernate.properties" />
        <hbm2java/>
    </hibernatetool>
</target>
```
The snippet creates an Ant target named `reveng` which contains an embedded `hibernatetool` task.
This task has the folder `generated-sources` set as its destination directory. Additionally
it specifies a JDBC configuration for which the connection information is to be found in the 
file `hibernate.properties`. Finally it contains a `hbm2java` element that will generate Java files
based on the JDBC configuration in the specified destination directory.

As illustrated, the `hibernatetool` task element can be configured with additional attributes
and nested elements. 

Let's walk through the different possibilities.

#### 1.2.1 The `destdir` Attribute

This mandatory attribute lets you control the folder where the artefacts will be generated. If the 
specified folder does not exist it will be created.

```xml
<hibernatetool destdir="path/to/generated/sources">
    ...
</hibernatetool>
```
In the snippet above the artefacts will be generated in the subfolder `path/to/generated/sources` of 
the working directory.

#### 1.2.2 The `templatepath` Attribute

The generation of artefacts is based on FreeMarker templates. By default, the templates are 
looked up on the classpath. If you want to have fine control over the artefact generation
you can create custom templates. The optional `templatepath` attribute lets you specify the
location of those templates.

```xml
<hibernatetool 
        destdir="..."
        templatepath="path/to/templates">
    ...
</hibernatetool>
```


#### 1.2.3 The `<classpath>` Element

The `<classpath>` element can be added within the `<hiberatetool>` task to specify where to look
for classes and/or resources that are used e.g. by the configured exporters. 

```xml
<hibernatetool 
        destdir="...">
    <classpath location="location/of/additional/classes"/>
    ...
</hibernatetool>

```

All the usual configuration possibilities for [Ant path-like structures](https://ant.apache.org/manual/using.html#path) 
are applicable. 

### 1.2.4 The `<property>` and `<propertySet>` elements

It is possible to define properties to be used by the configured exporters. To do 
this, you can use one or more `<property>` elements. As shown below, this element uses 
the attributes with name `key` and `value`. The `name` attribute sometimes seen in other
places for Ant build files is *not* supported.

```xml
<hibernatetool 
        destdir="...">
    <property key='someKey' value='someValue' />
    ...
</hibernatetool>
```

If you rather want to redefine already existing properties, you can use the `propertySet` 
element. The snippet below will redefine the properties prefixed with 'foo' and replace 
the prefix by 'bar'.

```xml
<hibernatetool 
        destdir="...">
    <propertySet>
        <propertyref prefix="foo"/>
        <mapper type="glob" from="foo*" to="bar*"/>
    </propertySet>
    ...
</hibernatetool>
```

All the possibilities documented in the [Ant documentation](https://ant.apache.org/manual/Types/propertyset.html)
for the `propertySet`element are supported.

### 1.2.5 The Configuration Elements

The model from which to generate artefacts with Hibernate Tools is created from a configuration, very much like
the configuration used in plain Hibernate ORM. As a matter of fact, this 'plain' Hibernate configuration can 
serve as a starting point for some of the generators. You can specify it with the `<configuration>`element for 
Hibernate native configurations or with the `<jpaconfiguration>` element for JPA annotated projects. 

In most cases however, you will use the `<jdbcconfiguration>`element as this one is specific to perform the
generation of artefacts starting from a relational database schema. See an example below. 

```xml
<hibernatetool 
        destdir="...">
    <jdbcconfiguration propertyfile="hibernate.properties"/>
    ...
</hibernatetool>

```
One - and only one - of the configuration possibilities needs to be specified in the `<hibernatetool>` task.
(As shown in the classpath example above tough, you can just specify an empty configuration if your
generators do not need to be configured)

More detailed information on the possible attributes and elements that can be used for the configuration 
elements is to be found in section 2 further down this guide.

### 1.2.6 The Exporter Elements

Hibernate Tools is very versatile and can be used to generate a wide variety of artefacts. This generation
is performed when adding one or more exporter elements to the `<hibernatetool>` task. 

```xml
<target name="reveng">
    <hibernatetool destdir="...">
        <jdbcconfiguration propertyfile="hibernate.properties" />
        <hbm2java/>
    </hibernatetool>
</target>
```

The most used exporters are
* `<hbm2java>` for the generation of Java files
* `<hbmtemplate>` when generating artefacts based on custom templates
* `<query>` for executing SQL and exporting the result to a file
* `<hbm2ddl>` for the generation of the database creation scripts
* `<hbm2cfgxml>` for the generation of the Hibernate configuration XML file

Some other predefined exporters are less used (or even deprecated)
* `<hbm2dao>` for the generation of Java files that are data access objects
* `<hbm2doc>` for the generation of JavaDoc style HTML files documenting the database structure
* `<hbm2hbmxml>` for the generation of HBM XML files (deprecated, HBM XML files will disappear with Hibernate 8.0)
* `<hbmlint>` for static analysis and reporting of possible issues 

Section 3 further down this guide will document in more detail the use of these exporters.

## 2. The Hibernate Tools Configurations 

As explained earlier, the reverse engineering and generation of artefacts with Hibernate Tools is based on a 
so-called configuration. We will detail the possibilities for these configurations in this section.

### 2.1 Native Configuration with `<configuration>` 

The first and simplest possibility to configure the `<hibernatetool>` task is by using the `<configuration>` 
element. 

```xml
<target name="reveng">
    <hibernatetool destdir="...">
        ...
        <configuration/>
        ...
    </hibernatetool>
</target>
```

The `<configuration>` element can be used without additional configuration but that is only of limited interest.
In practically all cases you will use additional attributes and/or embedded elements.

### 2.1.1 The `propertyfile` attribute

The first possible attribute is `propertyfile`. This property points to the file that will be used to read the 
Hibernate properties.

```xml
<target name="reveng">
    <hibernatetool destdir="...">
        ...
        <configuration propertyfile="hibernate.properties">
        ...
        </configuration>
        ...
    </hibernatetool>
</target>
```

Possible contents for the `hibernate.properties` file when using the H2 Sakila database are illustrated below:

```properties
hibernate.connection.driver_class=org.h2.Driver
hibernate.connection.url=jdbc:h2:tcp://localhost/./sakila
hibernate.connection.username=sa
hibernate.default_catalog=SAKILA
hibernate.default_schema=PUBLIC
```

### 2.1.2 The `configurationfile` attribute

The second possibility is to use the well known `hibernate.cfg.xml` file and specify it using the 
`configurationfile` attribute. 

```xml
<target name="reveng">
    <hibernatetool destdir="...">
        ...
        <configuration configurationfile="hibernate.cfg.xml" />
        ...
    </hibernatetool>
</target>
```

A possible example of such a `hibernate.cfg.xml` file is shown in the xml snippet below.

```xml
<hibernate-configuration>
    <session-factory>
        <property name="hibernate.connection.driver_class">org.h2.Driver</property>
        <property name="hibernate.connection.url">jdbc:h2:mem:</property>
        <mapping resource="Foo.hbm.xml"/>
    </session-factory>
</hibernate-configuration>
```

### 2.1.3 The `<fileset>` element

The `<fileset>` element can be used to specify additional resources (most probably mapping files)
that need to be included in the configuration. 

```xml
<target name="reveng">
    <hibernatetool destdir="...">
        ...
        <configuration propertyfile="...">
            <fileset file="Foo.hbm.xml"/>
        </configuration>
        ...
    </hibernatetool>
</target>
```
All the usual options for the [Ant FileSet](https://ant.apache.org/manual/Types/fileset.html) type apply. 

### 2.1.4 Additional Remarks

The `propertyfile` and `configurationfile` attributes and the `<fileset>` element can be freely mixed and 
matched. There are however two additional remarks to take into account:
1. The properties defined in `propertyfile` take precedence over the properties defined in `configurationfile`.
2. Files and resources specified in the `configurationfile` should be excluded from the files 
specified by the `<fileset>` to avoid duplicate import exceptions.

### 2.2 JPA Configuration

The second possibility to configure the `<hibernatetool>` task is by using the `<jpaconfiguration>`
element. A `<jpaconfiguration>` tag is used when you want to read the metamodel from JPA. 
In other words, when you do not have a `hibernate.cfg.xml`, but instead have a setup where 
you use a `persistence.xml` file packaged in a JPA compliant manner.

```xml
<target name="reveng">
    <hibernatetool destdir="...">
        ...
        <jpaconfiguration />
        ...
    </hibernatetool>
</target>
```

The `<jpaconfiguration>` tag will try and auto-configure it self based on the available classpath,
e.g. look for the `META-INF/persistence.xml` file.

### 2.2.1 The `persistenceunit` attribute

The `persistenceunit` attribute can be used to select a specific persistence unit. In the
xml excerpt below, the persistence unit called 'bar' will be selected.

```xml
<target name="reveng">
    <hibernatetool destdir="...">
        ...
        <jpaconfiguration persistenceunit="bar"/>
        ...
    </hibernatetool>
</target>
```

If no `persistenceunit` attribute is specified Hibernate Tools will automatically search for one, 
and if a unique one is found, use it. However, having multiple persistence units will result in an error.

### 2.2.2 Additional Configuration 

As the `<jpaconfiguration>` element inherits all the configuration elements and attributes above
are theoretically applicable.

#### 2.2.2.1 The `propertyfile` attribute

See section 2.1.1

#### 2.2.2.2 The `configurationfile` attribute

This attribute will result in an exception as the configuration is read from the `persistence.xml` file.

#### 2.2.2.3 The `<fileset>` element

This nested element is ignored.

### 2.3 JDBC Configuration

The third possibility for configuring the `<hibernatetool>` task is to use the `<jdbcconfiguration>`
element. `<jdbcconfiguration>` is used to perform reverse engineering of a database from a JDBC connection.
This configuration works by reading the connection properties either from a `hibernate.cfg.xml` file or a 
`hibernate.properties`.

```xml
<target name="reveng">
    <hibernatetool destdir="...">
        ...
        <jdbcconfiguration propertyfile="..."/>
        ...
    </hibernatetool>
</target>
```

### 2.3.1 

## 3. The Hibernate Tools Exporters