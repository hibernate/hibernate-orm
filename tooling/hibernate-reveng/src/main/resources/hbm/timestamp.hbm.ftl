    <timestamp
        name="${property.name}"
<#if !property.basicPropertyAccessor>        access="${property.propertyAccessorName}"
</#if><#list property.columns as column> <#-- always only one column, but no direct access method.-->
        column="${column.quotedName}" 
</#list>    />

