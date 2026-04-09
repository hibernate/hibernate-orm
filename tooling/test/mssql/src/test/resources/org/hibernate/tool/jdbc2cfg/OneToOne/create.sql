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
CREATE SCHEMA HTT
CREATE TABLE HTT.LEFT_TABLE ( ID INT NOT NULL, PRIMARY KEY (ID) )
CREATE TABLE HTT.RIGHT_TABLE ( ID INT NOT NULL, PRIMARY KEY (ID) )
CREATE TABLE HTT.MIDDLE_TABLE ( LEFT_ID INT NOT NULL, RIGHT_ID INT NOT NULL, PRIMARY KEY (LEFT_ID), CONSTRAINT FK_MIDDLE_LEFT FOREIGN KEY (LEFT_ID) REFERENCES HTT.LEFT_TABLE, CONSTRAINT FK_MIDDLE_RIGHT FOREIGN KEY (RIGHT_ID) REFERENCES HTT.RIGHT_TABLE)
CREATE TABLE HTT.PERSON ( PERSON_ID INT NOT NULL, NAME VARCHAR(50), PRIMARY KEY (PERSON_ID) )
CREATE TABLE HTT.ADDRESS_PERSON ( ADDRESS_ID INT NOT NULL, NAME VARCHAR(50), PRIMARY KEY (ADDRESS_ID), CONSTRAINT FK_ADDRESS_PERSON FOREIGN KEY (ADDRESS_ID) REFERENCES HTT.PERSON)
CREATE TABLE HTT.MULTI_PERSON ( PERSON_ID INT NOT NULL, PERSON_COMPID INT NOT NULL, NAME VARCHAR(50), PRIMARY KEY (PERSON_ID, PERSON_COMPID) )
CREATE TABLE HTT.ADDRESS_MULTI_PERSON ( ADDRESS_ID INT NOT NULL, ADDRESS_COMPID INT NOT NULL, NAME VARCHAR(50), PRIMARY KEY (ADDRESS_ID, ADDRESS_COMPID), CONSTRAINT FK_ADDRESS_MULTI_PERSON FOREIGN KEY (ADDRESS_ID, ADDRESS_COMPID) REFERENCES HTT.MULTI_PERSON)		
