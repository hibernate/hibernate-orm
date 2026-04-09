<!--
  ~ Copyright 2010 - 2025 Red Hat, Inc.
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

# Hibernate Tools Oracle DB Integration Tests

This project contains tests specific for Oracle databases. The tests in this project will run when the `all` profile is used and when the database is online.

The project was developed against the Oracle 12c database running on localhost in a Docker container.

## Running the Tests

To do this on your own machine execute the following steps:

1. Install Docker.

2. Pull the Docker image of the Oracle DB: `docker pull sath89/oracle-12c`

3. Start the database: `docker run -d -p 8080:8080 -p 1521:1521 sath89/oracle-12c`. This step takes a while the first time. 
 Use `docker logs` and specify the id of your docker container to monitor the progress. 
 When the logs say `Database is ready to use` you can move to 
step 4.

4. Connect to the database using your favorite client (e.g. [DBeaver](http://dbeaver.jkiss.org))
   * hostname: `localhost`
   * port: `1521`
   * service name: `xe.oracle.docker`
   * username: `system`
   * password: `oracle` 
 
   The complete JDBC URL is: `jdbc:oracle:thin:@//localhost:1521/xe.oracle.docker`

5. Create the Hibernate Tools test (`HTT`) database. You can do this by executing the following SQL: 

   ```
   create user HTT
          identified by HTT
          default tablespace USERS
          temporary tablespace TEMP
          quota unlimited on USERS;
   grant all privileges to HTT with admin option;
   ```

6. You can now connect with username `HTT` and password `HTT` and verify the existence of the `HTT` schema.
If that is the case you are ready to run the tests. 