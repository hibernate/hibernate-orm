<#foreach queryDef in md.namedQueryDefinitions>
    <query 
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
</#if>    >
        <![CDATA[${queryDef.queryString.trim()}]]>
    </query> 
</#foreach>