
<component
    name="${property.name}"
    class="${property.value.componentClassName}"
    <#if !property.basicPropertyAccessor>
        access="${property.propertyAccessorName}"
	</#if>>
    <#assign metaattributable=property>
	<#include "meta.hbm.ftl">
	<#foreach property in c2h.getProperties(property.value)>
		<#include "${c2h.getTag(property)}.hbm.ftl"/>
	</#foreach>
</component>