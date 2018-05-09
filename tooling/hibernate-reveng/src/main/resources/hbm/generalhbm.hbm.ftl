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
