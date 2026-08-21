<#--
~ Copyright 2010 - 2025 Red Hat, Inc.
~
~ Licensed under the Apache License, Version 2.0 (the "License");
~ you may not use this file except in compliance with the License.
~ You may obtain a copy of the License at
~
~     http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing, software
~ distributed under the License is distributed on an "AS IS" basis,
~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
~ See the License for the specific language governing permissions and
~ limitations under the License.
-->
table="${value.collectionTable.quotedName}"
<#if value.collectionTable.catalog?exists && ((clazz.table.catalog?exists && clazz.table.catalog!=value.collectionTable.catalog) || (!clazz.table.catalog?exists && value.collectionTable.catalog?exists)) >
catalog="${value.collectionTable.catalog}"
</#if>
<#if value.collectionTable.schema?exists && ((clazz.table.schema?exists && clazz.table.schema!=value.collectionTable.schema) || (!clazz.table.schema?exists && value.collectionTable.schema?exists)) >
schema="${value.collectionTable.quotedSchema}"
</#if>