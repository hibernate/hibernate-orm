<#list md.namedNativeQueryDefinitions as queryDef>
    <sql-query 
        name="${queryDef.name}"
<#if queryDef.flushMode?exists>
        flush-mode="${queryDef.flushMode.toString().toLowerCase()}"
</#if>
<#if queryDef.isCacheable()>
	    cacheable="${queryDef.isCacheable()?string}"
</#if>
<#if queryDef.cacheRegion?exists>
	    cache-region="${queryDef.cacheRegion}"
</#if>
<#if queryDef.fetchSize?exists>
        fetch-size="${queryDef.fetchSize}"
</#if>
<#if queryDef.timeout?exists>
        timeout="${queryDef.timeout?c}"
</#if>    
>
<#if queryDef.querySpaces?exists>
<#list queryDef.querySpaces as tableName>
	    <synchronize table="${tableName}" />
</#list>
</#if>
<#if queryDef.queryReturns?exists>
<#list queryDef.queryReturns as returnDef>
<#assign returnTag = c2h.getNamedSQLReturnTag(returnDef)>
	    <${returnTag}
             alias="${returnDef.alias}"
<#if c2h.isNamedSQLReturnRoot(returnDef)>
             class="${returnDef.returnEntityName}"
<#elseif c2h.isNamedSQLReturnRole(returnDef)>
             property="${returnDef.ownerAlias}.${returnDef.ownerProperty}"
<#elseif c2h.isNamedSQLReturnCollection(returnDef)>
             role="${returnDef.ownerEntityName}.${returnDef.ownerProperty}"
</#if>
<#if returnDef.lockMode?exists>
             lock-mode="${returnDef.lockMode.toString().toLowerCase()}"
</#if>	    />
</#list>     
</#if>   
      <![CDATA[${queryDef.queryString.trim()}]]>
    </sql-query>
    
</#list>