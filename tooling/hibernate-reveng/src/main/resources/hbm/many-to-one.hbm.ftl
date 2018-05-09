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
<#foreach column in property.columnIterator>
        <#include "column.hbm.ftl">
</#foreach>	
   </many-to-one>
