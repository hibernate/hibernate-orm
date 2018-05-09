    <version
        name="${property.name}"
        type="${property.value.typeName}"
<#if !property.basicPropertyAccessor>        access="${property.propertyAccessorName}"
</#if>    >
<#foreach column in property.columnIterator> 
       <#include "column.hbm.ftl">
</#foreach>
	</version>

