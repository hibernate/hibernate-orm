<#assign value = property.value>
<#assign keyValue = value.getKey()>
<#assign elementValue = value.getElement()>
<#assign indexValue = value.getIndex()>
<#assign elementTag = c2h.getCollectionElementTag(property)>

<array name="${property.name}"
	<#if value.elementClassName?exists> element-class="${value.elementClassName}"</#if>
	<#include "collection-tableattr.hbm.ftl">
	<#if property.cascade != "none">
        cascade="${property.cascade}"
	</#if>
	<#if !property.basicPropertyAccessor>
        access="${property.propertyAccessorName}"
	</#if>
	<#if c2h.hasFetchMode(property)> fetch="${c2h.getFetchMode(property)}"</#if>>
 	<#assign metaattributable=property>
 	<#include "meta.hbm.ftl">
    <#include "key.hbm.ftl">
    <list-index>
    <#foreach column in indexValue.columnIterator>
    	<#include "column.hbm.ftl">
    </#foreach>  
    </list-index>
    <#include "${elementTag}-element.hbm.ftl">
</array>
