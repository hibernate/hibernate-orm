    <version
        name="${property.name}"
        type="${property.value.typeName}"
<#if !property.basicPropertyAccessor>        access="${property.propertyAccessorName}"
</#if>    >
<#list property.columns as column>
       <#include "column.hbm.ftl">
</#list>
	</version>

