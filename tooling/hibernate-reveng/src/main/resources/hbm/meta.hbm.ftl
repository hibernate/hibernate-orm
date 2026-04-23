<#list classInfo.getMetaAttributes()?keys as metaName>
<#list classInfo.getMetaAttribute(metaName) as metaValue>
    <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
