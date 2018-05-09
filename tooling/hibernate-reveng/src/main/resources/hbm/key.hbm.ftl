		<key
		<#if property.value.referencedPropertyName?exists> 
        property-ref="${property.value.referencedPropertyName}"
</#if>> 
   		<#foreach column in keyValue.columnIterator>
   			<#include "column.hbm.ftl">
   		</#foreach>
		</key>
