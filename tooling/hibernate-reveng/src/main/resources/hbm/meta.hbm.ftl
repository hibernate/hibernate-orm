<#list helper.getMetaAttributes()?keys as metaName>
<#list helper.getMetaAttribute(metaName) as metaValue>
    <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
