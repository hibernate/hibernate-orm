<#assign value = property.value>
<#assign keyValue = value.getKey()>
<#assign elementValue = value.getElement()>
<#assign indexValue = value.getIndex()>
<#assign elementTag = c2h.getCollectionElementTag(property)>

	<map name="${property.name}"
	<#include "collection-tableattr.hbm.ftl">
	lazy="${c2h.getCollectionLazy(value)}"
	<#if property.cascade != "none">
        cascade="${property.cascade}"
	</#if>
	<#if !property.basicPropertyAccessor>
        access="${property.propertyAccessorName}"
	</#if>
	<#if c2h.hasFetchMode(property)> fetch="${c2h.getFetchMode(property)}"</#if>>
		<#assign metaattributable=property><#include "meta.hbm.ftl">
		<#-- TODO table attributes-->
    		<#include "key.hbm.ftl">
    		<#if c2h.isManyToOne(indexValue)>
    		<map-key-many-to-many class="${indexValue.getReferencedEntityName()}">
    			<#list indexValue.selectables as column>
     			<#include "column.hbm.ftl">
			</#list>  
    		</map-key-many-to-many>
    		<#else>
    		<map-key type="${indexValue.typeName}">
    			<#list indexValue.selectables as column>
    			<#include "column.hbm.ftl">
			</#list> 
		</map-key>
    		</#if>
    		<#include "${elementTag}-element.hbm.ftl">
	</map>