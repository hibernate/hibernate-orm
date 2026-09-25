<many-to-any id-type="${elementValue.getIdentifierType()}" meta-type="${elementValue.getMetaType()}">
	<#if elementValue.metaValues?exists>
	<#list elementValue.metaValues.entrySet() as entry>
        	<meta-value value="${entry.key}" class="${entry.value}"/>
        </#list>
        </#if>
	<#list elementValue.columns as column>
		<#include "column.hbm.ftl">
	</#list>
</many-to-any>
			