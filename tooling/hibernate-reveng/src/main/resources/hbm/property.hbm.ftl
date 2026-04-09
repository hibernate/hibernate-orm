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
    <property
        name="${property.name}"
<#if !property.value.typeParameters?exists>
	    type="${property.value.typeName}"
</#if>        
<#if !property.updateable>
        update="false"
</#if>
<#if !property.insertable>
        insert="false"
</#if>
<#if !property.basicPropertyAccessor>
        access="${property.propertyAccessorName}"
</#if>
<#if property.lazy>
        lazy="true"
</#if>
<#if !property.optimisticLocked>
        optimistic-lock="false"
</#if>
<#if property.value.hasFormula()>
<#assign formula = c2h.getFormulaForProperty(property)>
<#if formula?has_content>
        formula="${formula.text}"
</#if>
</#if>
    >
  <#assign metaattributable=property>
  <#include "meta.hbm.ftl">
  <#list property.selectables as column>
     <#if !column.isFormula()>
        <#include "column.hbm.ftl">
     </#if>
  </#list>	
  <#if property.value.typeParameters?exists>
  <type name="${property.value.typeName}">
  		<#list property.value.typeParameters.entrySet() as entry>
              		<param name="${entry.key}">${entry.value}</param>
        </#list>  	  
  </type>
  </#if>
  </property>

