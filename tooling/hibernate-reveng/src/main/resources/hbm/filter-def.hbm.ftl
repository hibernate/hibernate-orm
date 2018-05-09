<#foreach filterKey in md.filterDefinitions.keySet()>
<#assign filterDef = md.filterDefinitions.get(filterKey)>
    <filter-def name="filterKey">
<#foreach filterParaName in filterDef.parameterNames>
	<filter-param name="${filterParaName}" type="${filterDef.getParameterType(filterParaName).name}" />
</#foreach>
    </filter-def>    
</#foreach>