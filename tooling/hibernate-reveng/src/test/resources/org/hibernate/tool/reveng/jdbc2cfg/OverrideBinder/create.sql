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
CREATE TABLE DUMMY (ID NUMERIC(10,0) NOT NULL, PRIMARY KEY (ID) )
CREATE TABLE DEFUNCT_TABLE ( ID NUMERIC(10,0) NOT NULL, NAME VARCHAR(20), SHORTNAME VARCHAR(5), FLAG VARCHAR(1), DUMID NUMERIC(10,0), PRIMARY KEY (ID), FOREIGN KEY (DUMID) REFERENCES DUMMY(ID))                
CREATE TABLE MISC_TYPES ( ID NUMERIC(10,0) NOT NULL, NAME VARCHAR(20), SHORTNAME VARCHAR(5), FLAG VARCHAR(1), PRIMARY KEY (ID) )
CREATE TABLE INTHEMIDDLE ( MISCID NUMERIC(10,0), DEFUNCTID NUMERIC(10,0), FOREIGN KEY (MISCID) REFERENCES MISC_TYPES(ID), FOREIGN KEY (DEFUNCTID) REFERENCES DEFUNCT_TABLE(ID) )
CREATE TABLE CUSTOMER ( CUSTID VARCHAR(10), NAME VARCHAR(20) )
CREATE TABLE ORDERS ( ORDERID VARCHAR(10), NAME VARCHAR(20),  CUSTID VARCHAR(10), COMPLETED NUMERIC(1,0) NOT NULL, VERIFIED NUMERIC(1) )
CREATE TABLE PARENT ( ID VARCHAR(10), NAME VARCHAR(20))
CREATE TABLE CHILDREN ( ID VARCHAR(10), PARENTID VARCHAR(10), NAME VARCHAR(20) )
CREATE TABLE EXCOLUMNS (ID VARCHAR(12), NAME VARCHAR(20), EXCOLUMN NUMERIC(10,0) )
