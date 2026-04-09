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
CREATE TABLE GROUPS (NAME VARCHAR(255) NOT NULL, PRIMARY KEY (NAME))
CREATE TABLE USERS (NAME VARCHAR(255) NOT NULL, PASSWORD VARCHAR(255), GROUPNAME VARCHAR(255), PRIMARY KEY (NAME), FOREIGN KEY (GROUPNAME) REFERENCES GROUPS(NAME))
CREATE TABLE SESSION_ATTRIBUTES (ID INT NOT NULL, NAME VARCHAR(255) NOT NULL, STRINGDATA VARCHAR(255), USERNAME VARCHAR(255) NOT NULL, PRIMARY KEY (ID), FOREIGN KEY (USERNAME) REFERENCES USERS(NAME))