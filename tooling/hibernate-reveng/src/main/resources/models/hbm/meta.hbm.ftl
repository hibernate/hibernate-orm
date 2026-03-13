<#list table.getMetaAttributes()?keys as metaName>
<#list table.getMetaAttribute(metaName) as metaValue>
    <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
