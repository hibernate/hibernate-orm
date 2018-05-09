<many-to-any id-type="${elementValue.getIdentifierType()}" meta-type="${elementValue.getMetaType()}">
	<#if elementValue.metaValues?exists>
	<#foreach entry in elementValue.metaValues.entrySet()>
        	<meta-value value="${entry.key}" class="${entry.value}"/>
        </#foreach>
        </#if>
	<#foreach column in elementValue.columnIterator>
		<#include "column.hbm.ftl">
	</#foreach>
</many-to-any>
			