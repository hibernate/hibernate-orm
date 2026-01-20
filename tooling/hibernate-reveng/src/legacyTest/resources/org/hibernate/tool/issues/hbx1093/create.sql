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
CREATE TABLE ET_MANY_TO_MANY_COMP1 (ET_MANY_TO_MANY_COMP1_ID NUMBER, ET_MANY_TO_MANY_COMP11_ID NUMBER, FIELD VARCHAR2(10), PRIMARY KEY (ET_MANY_TO_MANY_COMP1_ID, ET_MANY_TO_MANY_COMP11_ID))
CREATE TABLE ET_MANY_TO_MANY_COMP2 ( ET_MANY_TO_MANY_COMP2_ID NUMBER, ET_MANY_TO_MANY_COMP22_ID NUMBER, FIELD VARCHAR2(10), PRIMARY KEY (ET_MANY_TO_MANY_COMP2_ID, ET_MANY_TO_MANY_COMP22_ID))
CREATE TABLE ET_MANY_TO_MANY_COMP_MAPPING ( FK_ET_MANY_TO_MANY_COMP1_ID NUMBER, FK_ET_MANY_TO_MANY_COMP11_ID NUMBER, FK_ET_MANY_TO_MANY_COMP2_ID NUMBER, FK_ET_MANY_TO_MANY_COMP22_ID NUMBER, FOREIGN KEY (FK_ET_MANY_TO_MANY_COMP1_ID,FK_ET_MANY_TO_MANY_COMP11_ID) REFERENCES ET_MANY_TO_MANY_COMP1(ET_MANY_TO_MANY_COMP1_ID,ET_MANY_TO_MANY_COMP11_ID), FOREIGN KEY (FK_ET_MANY_TO_MANY_COMP2_ID, FK_ET_MANY_TO_MANY_COMP22_ID) REFERENCES ET_MANY_TO_MANY_COMP2(ET_MANY_TO_MANY_COMP2_ID, ET_MANY_TO_MANY_COMP22_ID), PRIMARY KEY (FK_ET_MANY_TO_MANY_COMP1_ID,FK_ET_MANY_TO_MANY_COMP11_ID,FK_ET_MANY_TO_MANY_COMP2_ID,FK_ET_MANY_TO_MANY_COMP22_ID) )
