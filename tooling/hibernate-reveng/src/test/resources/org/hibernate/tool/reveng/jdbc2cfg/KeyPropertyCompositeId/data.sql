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
INSERT INTO PRODUCT (PRODUCT_ID, EXTRA_ID, DESCRIPTION, PRICE, NUMBER_AVAILABLE) VALUES('PC', '0', 'My PC', 100.0, 23)
INSERT INTO PRODUCT (PRODUCT_ID, EXTRA_ID, DESCRIPTION, PRICE, NUMBER_AVAILABLE) VALUES('MS', '1', 'My Mouse', 101.0, 23)
INSERT INTO CUSTOMER (CUSTOMER_ID, NAME, ADDRESS) VALUES('MAX', 'Max Rydahl Andersen', 'Neuchatel')
INSERT INTO CUSTOMER_ORDER (CUSTOMER_ID, ORDER_NUMBER, ORDER_DATE) VALUES ('MAX', 1, null)
INSERT INTO LINE_ITEM (CUSTOMER_ID_REF, ORDER_NUMBER, PRODUCT_ID, EXTRA_PROD_ID, QUANTITY) VALUES ('MAX', 1, 'PC', '0', 10)
INSERT INTO LINE_ITEM (CUSTOMER_ID_REF, ORDER_NUMBER, PRODUCT_ID, EXTRA_PROD_ID, QUANTITY) VALUES ('MAX', 1, 'MS', '1', 12)
