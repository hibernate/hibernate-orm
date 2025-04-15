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
<#assign value = property.value>
	<any name="${property.name}" 
		id-type="${value.getIdentifierType()}" 
		meta-type="${value.getMetaType()}"
		<#if property.cascade != "none">
        cascade="${property.cascade}"
		</#if>
		<#if !property.basicPropertyAccessor>
        access="${property.propertyAccessorName}"
		</#if>>
	    	<#assign metaattributable=property>
		<#include "meta.hbm.ftl">
		<#if value.metaValues?exists>
		    <#list value.metaValues.entrySet() as entry>
              		<meta-value value="${entry.key}" class="${entry.value}"/>
           	</#list>
        </#if>
        <#list property.columns as column>
            <#include "column.hbm.ftl">
        </#list>
	</any>
	