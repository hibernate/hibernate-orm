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
    <many-to-one
	    name="${property.name}"
	    class="${c2j.getJavaTypeName(property, false)}"
	    
<#if property.value.referencedPropertyName?exists> 
        property-ref="${property.value.referencedPropertyName}"
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
<#if property.cascade != "none">
        cascade="${property.cascade}"
</#if>
<#assign fetchmode = c2h.getFetchMode(property)>
<#if fetchmode != "default">
        fetch="${fetchmode}"
</#if>
<#if !property.optimisticLocked>
        optimistic-lock="false"
</#if>
    >
    <#assign metaattributable=property>
	<#include "meta.hbm.ftl">    
<#list property.columns as column>
        <#include "column.hbm.ftl">
</#list>	
   </many-to-one>
