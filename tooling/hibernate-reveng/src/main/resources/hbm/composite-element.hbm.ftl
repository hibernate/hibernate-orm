<composite-element class="${elementValue.componentClassName}">
	<#assign metaattributable=property>
	<#include "meta.hbm.ftl">
	<#foreach property in elementValue.getPropertyIterator()>
		<#assign tag=c2h.getTag(property)>
		<#if tag="component">
			<#assign tag="nested-composite-element">
			<#assign elementValue = property.value>
		</#if>
		<#include "${tag}.hbm.ftl"/>
	</#foreach>
</composite-element>
