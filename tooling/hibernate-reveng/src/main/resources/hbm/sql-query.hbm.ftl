<#--
~ Copyright 2010 - 2025 Red Hat, Inc.
~
~ Licensed under the Apache License, Version 2.0 (the "License");
~ you may not use this file except in compliance with the License.
~ You may obtain a copy of the License at
~
~     http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing, software
~ distributed under the License is distributed on an "AS IS" basis,
~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
~ See the License for the specific language governing permissions and
~ limitations under the License.
-->
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