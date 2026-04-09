############################################################################
# Hibernate Tools, Tooling for your Hibernate Projects                     #
#                                                                          #
# Copyright 2004-2025 Red Hat, Inc.                                        #
#                                                                          #
# Licensed under the Apache License, Version 2.0 (the "License");          #
# you may not use this file except in compliance with the License.         #
# You may obtain a copy of the License at                                  #
#                                                                          #
#     http://www.apache.org/licenses/LICENSE-2.0                           #
#                                                                          #
# Unless required by applicable law or agreed to in writing, software      #
# distributed under the License is distributed on an "AS IS" basis,        #
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. #
# See the License for the specific language governing permissions and      #
# limitations under the License.                                           #
############################################################################

The tests in this project will run when the 'all' profile is used and when the database is online.

The project was developed against the MySQL database running on localhost in a Docker container.

To do this on your own machine execute the following steps:

1. Install Docker.

2. Pull the Docker image of SQL Server: 'docker pull mysql'

3. Start the database: 'docker run -p3306:3306 --name MySql -e MYSQL_ROOT_PASSWORD=P@55w0rd -d mysql'. 

4. Connect to the database using your favorite client (e.g. DBeaver: http://dbeaver.jkiss.org)
hostname: localhost
port: 3306
username: root
password: P@55w0rd
The complete JDBC URL is: jdbc:mysql://localhost:3306

5. Create the 'htt' schema. You can do this by executing the following SQL: 

create schema htt;

5bis. Increase the amount of connections. There are connection leaks in the tests that make the tests fail.
Execute the following statement as the root user:

SET GLOBAL MAX_CONNECTIONS = 1000;

Note that this step should disappear when HBX-1404 is fixed.

6. You are now ready to run the tests. Use the following URL to connect: 'jdbc:mysql://localhost:3306/htt' 