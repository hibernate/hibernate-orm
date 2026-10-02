<#if metaattributable.getMetaAttributes()?exists>
<#list metaattributable.getMetaAttributes().keySet() as key>
 <#list metaattributable.getMetaAttributes().get(key).values as value>
  <meta attribute="${key}" inherit="false">${value}</meta>
 </#list>
</#list>
</#if>