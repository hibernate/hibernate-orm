<composite-element class="${elementValue.componentClassName}">
	<#assign metaattributable=property>
	<#include "meta.hbm.ftl">
	<#list elementValue.properties as property>
		<#assign tag=c2h.getTag(property)>
		<#if tag="component">
			<#assign tag="nested-composite-element">
			<#assign elementValue = property.value>
		</#if>
		<#include "${tag}.hbm.ftl"/>
	</#list>
</composite-element>
