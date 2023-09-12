
<properties
    name="${property.name}">
    <#assign metaattributable=property>
	<#include "meta.hbm.ftl">
	<#list c2h.getProperties(property.value) as property>
		<#include "${c2h.getTag(property)}.hbm.ftl"/>
	</#list>
</properties>