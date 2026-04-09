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
CREATE TABLE HTT.WITH_VERSION (FIRST INT, SECOND INT, VERSION INT, NAME VARCHAR(256), PRIMARY KEY (FIRST))
CREATE TABLE HTT.NO_VERSION (FIRST INT, SECOND INT, NAME VARCHAR(256), PRIMARY KEY (SECOND))
CREATE TABLE HTT.WITH_REAL_TIMESTAMP (FIRST INT, SECOND INT, TIMESTAMP ROWVERSION, NAME VARCHAR(256), PRIMARY KEY (FIRST))
CREATE TABLE HTT.WITH_FAKE_TIMESTAMP (FIRST INT, SECOND INT, TIMESTAMP INT, NAME VARCHAR(256), PRIMARY KEY (FIRST))
