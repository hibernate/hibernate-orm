		<key
		<#if property.value.referencedPropertyName?exists> 
        property-ref="${property.value.referencedPropertyName}"
</#if>> 
   		<#list keyValue.columns as column>
   			<#include "column.hbm.ftl">
   		</#list>
		</key>
