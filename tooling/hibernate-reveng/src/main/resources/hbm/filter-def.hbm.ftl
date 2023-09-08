<#list md.filterDefinitions.keySet() as filterKey>
<#assign filterDef = md.filterDefinitions.get(filterKey)>
    <filter-def name="filterKey">
    <#foreach filterParaName in filterDef.parameterNames>
	    <filter-param name="${filterParaName}" type="${filterDef.getParameterType(filterParaName).name}" />
    </#foreach>
    </filter-def>    
</#list>