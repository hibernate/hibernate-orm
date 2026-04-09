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
CREATE TABLE HTT.CUSTOMER (CUSTOMER_ID VARCHAR(256) NOT NULL, NAME VARCHAR(256) NOT NULL, ADDRESS VARCHAR(256) NOT NULL, PRIMARY KEY (CUSTOMER_ID))
CREATE TABLE HTT.PRODUCT (PRODUCT_ID VARCHAR(256) NOT NULL, EXTRA_ID VARCHAR(256) NOT NULL, DESCRIPTION VARCHAR(256) NOT NULL, PRICE FLOAT, NUMBER_AVAILABLE FLOAT, PRIMARY KEY (PRODUCT_ID, EXTRA_ID))
CREATE TABLE HTT.SIMPLE_CUSTOMER_ORDER (CUSTOMER_ORDER_ID VARCHAR(256) NOT NULL, CUSTOMER_ID VARCHAR(256) NOT NULL, ORDER_NUMBER FLOAT NOT NULL, ORDER_DATE DATE, PRIMARY KEY (CUSTOMER_ORDER_ID), FOREIGN KEY (CUSTOMER_ID) REFERENCES HTT.CUSTOMER(CUSTOMER_ID))
CREATE TABLE HTT.SIMPLE_LINE_ITEM (LINE_ITEM_ID VARCHAR(256) NOT NULL, CUSTOMER_ORDER_ID_REF VARCHAR(256), PRODUCT_ID VARCHAR(256) NOT NULL, EXTRA_ID VARCHAR(256) NOT NULL, QUANTITY FLOAT, PRIMARY KEY (LINE_ITEM_ID), FOREIGN KEY (PRODUCT_ID,EXTRA_ID) REFERENCES HTT.PRODUCT(PRODUCT_ID,EXTRA_ID),  FOREIGN KEY (CUSTOMER_ORDER_ID_REF) REFERENCES HTT.SIMPLE_CUSTOMER_ORDER(CUSTOMER_ORDER_ID))
CREATE TABLE HTT.CUSTOMER_ORDER (CUSTOMER_ID VARCHAR(256) NOT NULL, ORDER_NUMBER FLOAT NOT NULL, ORDER_DATE DATE, PRIMARY KEY (CUSTOMER_ID, ORDER_NUMBER), FOREIGN KEY (CUSTOMER_ID) REFERENCES HTT.CUSTOMER(CUSTOMER_ID))
CREATE TABLE HTT.LINE_ITEM (CUSTOMER_ID_REF VARCHAR(256) NOT NULL, ORDER_NUMBER FLOAT NOT NULL, PRODUCT_ID VARCHAR(256) NOT NULL, EXTRA_PROD_ID VARCHAR(256) NOT NULL, QUANTITY FLOAT, PRIMARY KEY (CUSTOMER_ID_REF, ORDER_NUMBER, PRODUCT_ID, EXTRA_PROD_ID), FOREIGN KEY (PRODUCT_ID,EXTRA_PROD_ID) REFERENCES HTT.PRODUCT(PRODUCT_ID,EXTRA_ID), CONSTRAINT TO_CUSTOMER_ORDER FOREIGN KEY (CUSTOMER_ID_REF, ORDER_NUMBER) REFERENCES HTT.CUSTOMER_ORDER(CUSTOMER_ID,ORDER_NUMBER))
