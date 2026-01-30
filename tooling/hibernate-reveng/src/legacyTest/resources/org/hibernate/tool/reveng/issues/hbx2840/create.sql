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
CREATE TABLE PARENT(ID1_1 INT NOT NULL, ID1_2 INT NOT NULL, PRIMARY KEY (ID1_1, ID1_2));
CREATE TABLE CHILD (ID2_1 INT NOT NULL, ID2_2 INT NOT NULL, PRIMARY KEY (ID2_1, ID2_2), FOREIGN KEY (ID2_1, ID2_2) REFERENCES PARENT (ID1_1, ID1_2));
