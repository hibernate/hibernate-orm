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
CREATE TABLE BASIC ( A INT NOT NULL, NAME VARCHAR(20), PRIMARY KEY (A)  )
CREATE TABLE SOMECOLUMNSNOPK ( PK VARCHAR(25) NOT NULL, B CHAR, C INT NOT NULL )
CREATE TABLE MULTIKEYED ( ORDERID VARCHAR(10), CUSTOMERID VARCHAR(10), NAME VARCHAR(10), PRIMARY KEY(ORDERID, CUSTOMERID) )
CREATE SCHEMA OTHERSCHEMA
CREATE TABLE OTHERSCHEMA.BASIC ( A INT NOT NULL, NAME VARCHAR(20), PRIMARY KEY (A)  )
