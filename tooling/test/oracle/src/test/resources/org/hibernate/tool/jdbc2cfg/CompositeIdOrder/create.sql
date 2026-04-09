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
CREATE TABLE REQUEST (REQUEST_KEY NUMBER(11) NOT NULL, TIMEFRAME_KEY NUMBER(11))
CREATE UNIQUE INDEX PK_REQUEST ON REQUEST(REQUEST_KEY)
ALTER TABLE REQUEST ADD (CONSTRAINT PK_REQUEST PRIMARY KEY (REQUEST_KEY))
CREATE TABLE SCHEDULE (SCHEDULE_KEY NUMBER(11) NOT NULL, TITLE VARCHAR2(255) NOT NULL)
CREATE UNIQUE INDEX PK_SCHEDULE ON SCHEDULE (SCHEDULE_KEY)
ALTER TABLE SCHEDULE ADD (CONSTRAINT PK_SCHEDULE PRIMARY KEY (SCHEDULE_KEY))
CREATE TABLE COURSE (SCHEDULE_KEY NUMBER(11) NOT NULL, REQUEST_KEY NUMBER(11) NOT NULL, TIMEFRAME_KEY NUMBER(11))
CREATE UNIQUE INDEX PK_COURSE ON COURSE (SCHEDULE_KEY, REQUEST_KEY)
ALTER TABLE COURSE ADD (CONSTRAINT PK_COURSE PRIMARY KEY (SCHEDULE_KEY, REQUEST_KEY))
ALTER TABLE COURSE ADD (CONSTRAINT FK_COURSE__REQUEST FOREIGN KEY (REQUEST_KEY) REFERENCES REQUEST (REQUEST_KEY) ON DELETE CASCADE)
ALTER TABLE COURSE ADD (CONSTRAINT FK_COURSE__SCHEDULE FOREIGN KEY (SCHEDULE_KEY) REFERENCES SCHEDULE (SCHEDULE_KEY) ON DELETE CASCADE)
CREATE TABLE COURSE_TOPIC (SCHEDULE_KEY NUMBER(11) NOT NULL, REQUEST_KEY NUMBER(11) NOT NULL, TOPIC_KEY NUMBER(11))
ALTER TABLE COURSE_TOPIC ADD (CONSTRAINT FK_COURSE_TOPIC__COURSE FOREIGN KEY (SCHEDULE_KEY, REQUEST_KEY) REFERENCES COURSE (SCHEDULE_KEY,REQUEST_KEY) ON DELETE CASCADE)
ALTER TABLE COURSE_TOPIC ADD (CONSTRAINT PK_COURSE_TOPIC PRIMARY KEY (TOPIC_KEY))
