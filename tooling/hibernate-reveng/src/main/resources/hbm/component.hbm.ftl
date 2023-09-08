
<component
    name="${property.name}"
    class="${property.value.componentClassName}"
    <#if !property.basicPropertyAccessor>
        access="${property.propertyAccessorName}"
	</#if>>
    <#assign metaattributable=property>
	<#include "meta.hbm.ftl">
	<#list c2h.getProperties(property.value) as property>
		<#include "${c2h.getTag(property)}.hbm.ftl"/>
	</#list>
</component>