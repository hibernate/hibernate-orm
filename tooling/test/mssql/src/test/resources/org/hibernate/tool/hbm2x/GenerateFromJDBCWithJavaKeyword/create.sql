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
CREATE TABLE HTT.MY_RETURN (RETURN_ID VARCHAR(20) NOT NULL, CONSTRAINT PK_MY_RETURN PRIMARY KEY (RETURN_ID))
CREATE TABLE HTT.MY_RETURN_HISTORY (ID VARCHAR(20) NOT NULL, MY_RETURN_REF VARCHAR(20), CONSTRAINT PK_MY_RETURN_HISTORY PRIMARY KEY (ID), CONSTRAINT FK_MY_RETURN_HISTORY_ID FOREIGN KEY (MY_RETURN_REF) REFERENCES HTT.MY_RETURN(RETURN_ID))		
