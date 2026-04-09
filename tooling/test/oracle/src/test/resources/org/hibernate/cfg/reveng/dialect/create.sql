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
CREATE TABLE person (  id              NUMBER(2) PRIMARY KEY,  a_varchar2_char VARCHAR2(10 CHAR),  a_varchar2_byte VARCHAR2(10 BYTE),  a_varchar_char  VARCHAR(10 CHAR),  a_varchar_byte  VARCHAR(10 BYTE),  a_nvarchar      NVARCHAR2(10),  a_char_char     CHAR(10 CHAR),  a_char_byte     CHAR(10 BYTE),  a_nchar_char    NCHAR(10),  a_nchar_byte    NCHAR(10),  a_number_int    NUMBER(10),  a_number_dec    NUMBER(10, 2),  a_float         FLOAT(10))