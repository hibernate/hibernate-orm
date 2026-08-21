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
<#assign keyValue = value.getKey()>
<#assign elementValue = value.getElement()>
<#assign elementTag = c2h.getCollectionElementTag(property)>

	<idbag name="${property.name}" 
	<#include "collection-tableattr.hbm.ftl">
	lazy="${c2h.getCollectionLazy(value)}"
	<#if property.cascade != "none">
        cascade="${property.cascade}"
	</#if>
	<#if !property.basicPropertyAccessor>
        access="${property.propertyAccessorName}"
	</#if>
	<#if c2h.hasFetchMode(property)> fetch="${c2h.getFetchMode(property)}"</#if>>
	 <#assign metaattributable=property>
	 	<#include "meta.hbm.ftl">
	 	<collection-id type="${value.identifier.typeName}" 
	 		column="${value.getIdentifier().getColumns().iterator().next().getQuotedName()}">
	 		<generator class="${value.identifier.identifierGeneratorStrategy}"/>
	 	</collection-id>
 		<key> 
        	<#list keyValue.columns as column>
          		<#include "column.hbm.ftl">
        	</#list>
        	</key>
		<#include "${elementTag}-element.hbm.ftl">
     	</idbag>
     	