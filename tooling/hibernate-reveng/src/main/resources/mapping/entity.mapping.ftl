    <entity class="${entityInfo.getClassName()}"<#if entityInfo.getClassAccessType()??> access="${entityInfo.getClassAccessType()}"</#if>>
<#if entityInfo.getSubselect()??>
        <table-expression>${entityInfo.getSubselect()}</table-expression>
<#else>
<#include "table.mapping.ftl"/>
</#if>
<#list entityInfo.getSecondaryTables() as st>
        <secondary-table name="${st.tableName()}">
<#list st.keyColumns() as kc>
            <primary-key-join-column name="${kc}"/>
</#list>
        </secondary-table>
</#list>
<#include "inheritance.mapping.ftl"/>
<#if entityInfo.getRowId()??>
        <rowid>${entityInfo.getRowId()}</rowid>
</#if>
<#if entityInfo.getSqlRestriction()??>
        <sql-restriction>${entityInfo.getSqlRestriction()}</sql-restriction>
</#if>
<#if entityInfo.isDynamicInsert()>
        <dynamic-insert>true</dynamic-insert>
</#if>
<#if entityInfo.isDynamicUpdate()>
        <dynamic-update>true</dynamic-update>
</#if>
<#if (entityInfo.getBatchSize() > 1)>
        <batch-size>${entityInfo.getBatchSize()?c}</batch-size>
</#if>
<#if entityInfo.getCacheAccessType()??>
        <caching access="${entityInfo.getCacheAccessType()}"<#if entityInfo.getCacheRegion()??> region="${entityInfo.getCacheRegion()}"</#if><#if !entityInfo.isCacheIncludeLazy()> includeLazy="false"</#if>/>
</#if>
<#if entityInfo.isConcreteProxy()>
        <lazy>false</lazy>
</#if>
<#if !entityInfo.isMutable()>
        <mutable>false</mutable>
</#if>
<#if entityInfo.getOptimisticLockMode()??>
        <optimistic-locking>${entityInfo.getOptimisticLockMode()}</optimistic-locking>
</#if>
<#if (queries.getEntityListenerClassNames()?size > 0)>
        <entity-listeners>
<#list queries.getEntityListenerClassNames() as listenerClass>
            <entity-listener class="${listenerClass}"/>
</#list>
        </entity-listeners>
</#if>
<#list queries.getLifecycleCallbacks() as callback>
        <${callback.elementName()} method-name="${callback.methodName()}"/>
</#list>
<#include "attributes.mapping.ftl"/>
<#if queries.getSQLInsert()??>
        <sql-insert<#if queries.getSQLInsert().callable()> callable="true"</#if>>${queries.getSQLInsert().sql()}</sql-insert>
</#if>
<#if queries.getSQLUpdate()??>
        <sql-update<#if queries.getSQLUpdate().callable()> callable="true"</#if>>${queries.getSQLUpdate().sql()}</sql-update>
</#if>
<#if queries.getSQLDelete()??>
        <sql-delete<#if queries.getSQLDelete().callable()> callable="true"</#if>>${queries.getSQLDelete().sql()}</sql-delete>
</#if>
<#if queries.getSQLDeleteAll()??>
        <sql-delete-all<#if queries.getSQLDeleteAll().callable()> callable="true"</#if>>${queries.getSQLDeleteAll().sql()}</sql-delete-all>
</#if>
<#list queries.getFetchProfiles() as fp>
        <fetch-profile name="${fp.name()}">
<#list fp.overrides() as fo>
            <fetch entity="${fo.entity()}" association="${fo.association()}" style="${fo.style()}"/>
</#list>
        </fetch-profile>
</#list>
<#list queries.getFilters() as filter>
        <filter name="${filter.name()}">
<#if filter.condition()?has_content>
            <condition>${filter.condition()}</condition>
</#if>
        </filter>
</#list>
    </entity>
