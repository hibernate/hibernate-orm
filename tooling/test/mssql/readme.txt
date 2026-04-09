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

The project was developed against the Microsoft SQL Server for Linux database running on localhost in a Docker container.

To do this on your own machine execute the following steps:

1. Install Docker.

2. Pull the Docker image of SQL Server: 'docker pull microsoft/mssql-server-linux'

3. Start the database: 'docker run -d --name SqlServer -e 'ACCEPT_EULA=Y' -e 'SA_PASSWORD=P@55w0rd' -p 1433:1433 microsoft/mssql-server-linux'. 

4. Connect to the database using your favorite client (e.g. DBeaver: http://dbeaver.jkiss.org)
hostname: localhost
port: 1433
username: sa
password: P@55w0rd
The complete JDBC URL is: jdbc:sqlserver://localhost:1433

5. You are now ready to run the tests. The database creation scripts will create a dedicated 'htt' schema that is dropped by the database drop scripts.
In the future this schema, as well as a dedicated user, could be created in the database startup.  