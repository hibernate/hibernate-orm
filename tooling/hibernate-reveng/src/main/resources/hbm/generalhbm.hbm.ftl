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
<?xml version="1.0"?>
<!DOCTYPE hibernate-mapping PUBLIC 
	"-//Hibernate/Hibernate Mapping DTD 3.0//EN"
	"http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">

<hibernate-mapping>	
<!-- 
	Auto-generated mapping file from
	the hibernate.org cfg2hbm engine
	for General Global Setttings
-->

<#if c2h.isImportData(md)>
<#include "import.hbm.ftl">
</#if>
<#if c2h.isNamedQueries(md)>
<#include "query.hbm.ftl"> 
</#if>
<#if c2h.isNamedSQLQueries(md)>
<#include "sql-query.hbm.ftl">
</#if>
<#if c2h.isFilterDefinitions(md)>
<#include "filter-def.hbm.ftl">
</#if>

</hibernate-mapping>
