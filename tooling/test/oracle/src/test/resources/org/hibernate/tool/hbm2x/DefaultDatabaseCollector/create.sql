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
CREATE USER "cat.cat" IDENTIFIED BY "cat.cat" 
GRANT ALL PRIVILEGES TO "cat.cat" WITH ADMIN OPTION
CREATE TABLE "cat.cat"."cat.master" (ID INT NOT NULL, TT INT, CONSTRAINT MASTER_PK PRIMARY KEY (ID))
CREATE TABLE "cat.cat"."cat.child" (CHILDID INT NOT NULL, MASTERREF INT, CONSTRAINT CHILD_PK PRIMARY KEY (CHILDID), CONSTRAINT MASTERREF_FK FOREIGN KEY (MASTERREF) references "cat.cat"."cat.master"(ID))