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
ALTER TABLE HTT.LINE_ITEM DROP CONSTRAINT TO_CUSTOMER_ORDER
ALTER TABLE HTT.LINE_ITEM DROP CONSTRAINT TO_PRODUCT
ALTER TABLE HTT.CUSTOMER_ORDER DROP CONSTRAINT TO_CUSTOMER
ALTER TABLE HTT.SIMPLE_LINE_ITEM DROP CONSTRAINT TO_SIMPLE_CUSTOMER_ORDER
ALTER TABLE HTT.SIMPLE_LINE_ITEM DROP CONSTRAINT FROM_SIMPLE_TO_PRODUCT
ALTER TABLE HTT.SIMPLE_CUSTOMER_ORDER DROP CONSTRAINT FROM_SIMPLE_TO_CUSTOMER
DROP TABLE HTT.SIMPLE_LINE_ITEM
DROP TABLE HTT.PRODUCT
DROP TABLE HTT.CUSTOMER
DROP TABLE HTT.SIMPLE_CUSTOMER_ORDER
DROP TABLE HTT.CUSTOMER_ORDER                
DROP TABLE HTT.LINE_ITEM                           
DROP SCHEMA HTT
