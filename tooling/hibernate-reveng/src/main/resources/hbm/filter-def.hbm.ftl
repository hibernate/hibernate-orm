<#list md.filterDefinitions.keySet() as filterKey>
<#assign filterDef = md.filterDefinitions.get(filterKey)>
    <filter-def name="filterKey">
    <#list filterDef.parameterNames as filterParaName>
	    <filter-param name="${filterParaName}" type="${filterDef.getParameterType(filterParaName).name}" />
    </#list>
    </filter-def>    
</#list>