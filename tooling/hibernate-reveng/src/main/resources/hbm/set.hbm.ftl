<#assign value = property.value>
<#assign keyValue = value.getKey()>
<#assign elementValue = value.getElement()>
<#assign elementTag = c2h.getCollectionElementTag(property)>

	<set name="${property.name}"
	<#include "collection-tableattr.hbm.ftl"> 
	inverse="${value.inverse?string}"
	lazy="${c2h.getCollectionLazy(value)}"	 		
	<#if property.cascade != "none">
        cascade="${property.cascade}"
	</#if>
	<#if !property.basicPropertyAccessor>
        access="${property.propertyAccessorName}"
	</#if>
	<#if c2h.hasFetchMode(property)> fetch="${c2h.getFetchMode(property)}"</#if>
	>
		<#assign metaattributable=property>
		<#include "meta.hbm.ftl">
		<#include "key.hbm.ftl">
		<#include "${elementTag}-element.hbm.ftl">
	</set>
