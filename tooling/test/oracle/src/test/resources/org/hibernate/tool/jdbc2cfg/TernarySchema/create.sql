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
CREATE TABLE USERS ( ID INT NOT NULL, NAME VARCHAR(20), PRIMARY KEY(ID))
CREATE USER OTHERSCHEMA IDENTIFIED BY OTHERSCHEMA 
GRANT ALL PRIVILEGES TO OTHERSCHEMA WITH ADMIN OPTION
CREATE USER THIRDSCHEMA IDENTIFIED BY THIRDSCHEMA 
GRANT ALL PRIVILEGES TO THIRDSCHEMA WITH ADMIN OPTION
CREATE TABLE OTHERSCHEMA.ROLE ( ID INT NOT NULL, NAME VARCHAR(20), PRIMARY KEY(ID))
GRANT REFERENCES ON OTHERSCHEMA.ROLE TO THIRDSCHEMA
GRANT REFERENCES ON USERS TO THIRDSCHEMA
CREATE TABLE THIRDSCHEMA.USERROLES ( USERID INT NOT NULL, ROLEID INT NOT NULL, PRIMARY KEY(USERID, ROLEID), FOREIGN KEY (ROLEID) REFERENCES OTHERSCHEMA.ROLE(ID), FOREIGN KEY (USERID) REFERENCES USERS(ID))
CREATE TABLE PLAINROLE ( ID INT NOT NULL, NAME VARCHAR(20), PRIMARY KEY(ID))
CREATE TABLE PLAINUSERROLES ( USERID INT NOT NULL, ROLEID INT NOT NULL, PRIMARY KEY(USERID, ROLEID), FOREIGN KEY (ROLEID) REFERENCES PLAINROLE(ID), FOREIGN KEY (USERID) REFERENCES USERS(ID))