
<properties
    name="${property.name}">
    <#assign metaattributable=property>
	<#include "meta.hbm.ftl">
	<#foreach property in c2h.getProperties(property.value)>
		<#include "${c2h.getTag(property)}.hbm.ftl"/>
	</#foreach>
</properties>