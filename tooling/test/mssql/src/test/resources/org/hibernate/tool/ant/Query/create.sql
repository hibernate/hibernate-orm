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
CREATE TABLE HTT.SERIALIZABLE_RESULT ( ID VARCHAR(255) NOT NULL, LENGTH INT NOT NULL, PRIMARY KEY (ID) )
INSERT INTO HTT.SERIALIZABLE_RESULT VALUES ( 'First', 1023)
INSERT INTO HTT.SERIALIZABLE_RESULT VALUES ( 'Second', 2047)
CREATE TABLE HTT.OBJECT_RESULT ( ID VARCHAR(255) NOT NULL, LENGTH INT NOT NULL, PRIMARY KEY (ID) )
INSERT INTO HTT.OBJECT_RESULT VALUES ( 'Third', 4095)
INSERT INTO HTT.OBJECT_RESULT VALUES ( 'Fourth', 8181)
