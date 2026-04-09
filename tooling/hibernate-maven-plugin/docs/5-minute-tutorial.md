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

# Hibernate Tools Maven Plugin : 5 Minute Tutorial

The best way to get to know the Hibernate Tools Maven plugin is to start to use it. Hence we will provide a quick tutorial that gives you the first taste of it. In this example we will create a simple database containing one table and use the plugin to reverse engineer a JPA entity from it.

## Create a Maven Project

Use a command line tool or your preferred IDE to create a basic Maven project.

```
<project 
    xmlns="http://maven.apache.org/POM/4.0.0" 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    
  <modelVersion>4.0.0</modelVersion>
  
  <groupId>org.acme</groupId>
  <artifactId>bar</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  
  ...
  
</project>
```

## Create a Simple Database

We will use the [H2 database engine](https://www.h2database.com/html/main.html) and the [SQL Maven plugin](https://www.mojohaus.org/sql-maven-plugin/) to create a simple database. 
Modify your pom file to contain a build section as shown below.

```
<project 
    xmlns="http://maven.apache.org/POM/4.0.0" 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    
  ...
  
  <properties>
    <h2.version>1.4.200</h2.version>
    <sql.version>1.5</sql.version>
    ...
  </properties>
  
  <build>
    <plugins>   
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>sql-maven-plugin</artifactId>
        <version>${sql.version}</version>
        <dependencies>
          <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <version>${h2.version}</version>
          </dependency>
        </dependencies>
        <executions>
          <execution>
            <id>create-database</id>
            <phase>initialize</phase>
            <goals>
              <goal>execute</goal>
            </goals>
            <configuration>
              <driver>org.h2.Driver</driver>
              <url>jdbc:h2:${project.build.directory}/database/bardb</url>
              <username>sa</username>
              <password></password>
              <autocommit>true</autocommit>
              <sqlCommand>create table foo (id int not null primary key, baz varchar(256))</sqlCommand>
            </configuration>
          </execution>
        </executions>
      </plugin>
      
      ...
      
    </plugins>    
  </build>
  
</project>
```

Issuing `mvn clean initialize` on the command line in the root of your project will now create the 'bardb' database in the folder 'target/database'. We are now ready to reverse engineer the database table to an entity class.

## Generate entities

To properly generate our entity, we need to do two more things. The first is of course add a new plugin section to the pom file that configures the use of the Hibernate Tools Maven plugin.

```
<project 
    xmlns="http://maven.apache.org/POM/4.0.0" 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    
  ...
  
  <properties>
    ...
    <hibernate.version>6.0.0-SNAPSHOT</hibernate.version>
  </properties>
  
  <build>
    <plugins>   

    ...
    
      <plugin>
        <groupId>org.hibernate.tool</groupId>
        <artifactId>hibernate-tools-maven</artifactId>
        <version>${hibernate.version}</version>
        <dependencies>
          <dependency>
            <groupId>org.hibernate.tool</groupId>
            <artifactId>hibernate-tools-orm</artifactId>
            <version>${hibernate.version}</version>
          </dependency>
          <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <version>${h2.version}</version>
          </dependency>
        </dependencies>
        <executions>
          <execution>
            <id>entity-generation</id>
            <phase>generate-sources</phase>
            <goals>
              <goal>hbm2java</goal>
            </goals>
            <configuration>
              <ejb3>true</ejb3>
            </configuration>
          </execution>
        </executions>
      </plugin>      
    </plugins>    
  </build>
  
</project>
```

In principle, we are now ready to generate the entity, but issuing `mvn clean generate-sources` will result in an error, complaining about the fact that a 'hiberate.properties' file could not be found. 
By default the plugin looks in the folder 'src/main/resources' for this file. So it is for now sufficient to add a 'hibernate.properties' file in that location with the contents specified below.

```
hibernate.dialect=H2
hibernate.connection.driver_class=org.h2.Driver
hibernate.connection.url=jdbc:h2:./target/database/bardb
hibernate.connection.username=sa
hibernate.connection.password=
```

Now we are ready. Executing `mvn clean generate-sources` will create a 'Foo.java' entity in the default location, which is 'target/generated-sources'.






