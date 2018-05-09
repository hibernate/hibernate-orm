<nested-composite-element class="${elementValue.componentClassName}" name="subcomponent">
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
</nested-composite-element>
