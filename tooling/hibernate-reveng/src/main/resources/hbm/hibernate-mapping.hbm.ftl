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
	"https://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">

<!-- Generated ${date} by Hibernate Tools ${version} -->
<#if hmgs?exists && hmgs.hasNonDefaultSettings()>
<hibernate-mapping
<#if hmgs.hasDefaultPackage()>
 package="${hmgs.defaultPackage}"
 </#if>
<#if hmgs.hasSchemaName()>
 schema="${hmgs.schemaName}"
 </#if>
<#if hmgs.hasCatalogName()>
 catalog="${hmgs.catalogName}"
</#if>
<#if hmgs.hasNonDefaultCascade()>
 default-cascade="${hmgs.defaultCascade}"
 </#if>
<#if hmgs.hasNonDefaultAccess()>
 default-access="${hmgs.defaultAccess}"
 </#if>
<#if !hmgs.isDefaultLazy()>
	default-lazy="false"
	</#if>
<#if !hmgs.isAutoImport()>
	auto-import="false"
</#if>>
<#else>
<hibernate-mapping>
</#if>

<#include "persistentclass.hbm.ftl"/>

</hibernate-mapping>