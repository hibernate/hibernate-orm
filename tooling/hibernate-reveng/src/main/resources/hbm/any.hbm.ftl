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
	