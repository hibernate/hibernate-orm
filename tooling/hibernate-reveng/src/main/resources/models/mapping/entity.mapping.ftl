    <entity class="${helper.getClassName()}">
<#if helper.getSubselect()??>
        <table-expression>${helper.getSubselect()}</table-expression>
<#else>
<#include "table.mapping.ftl"/>
</#if>
<#list helper.getSecondaryTables() as st>
        <secondary-table name="${st.tableName()}">
<#list st.keyColumns() as kc>
            <primary-key-join-column name="${kc}"/>
</#list>
        </secondary-table>
</#list>
<#include "inheritance.mapping.ftl"/>
<#if helper.getRowId()??>
        <rowid>${helper.getRowId()}</rowid>
</#if>
<#if helper.getSqlRestriction()??>
        <sql-restriction>${helper.getSqlRestriction()}</sql-restriction>
</#if>
<#if helper.isDynamicInsert()>
        <dynamic-insert>true</dynamic-insert>
</#if>
<#if helper.isDynamicUpdate()>
        <dynamic-update>true</dynamic-update>
</#if>
<#if (helper.getBatchSize() > 1)>
        <batch-size>${helper.getBatchSize()?c}</batch-size>
</#if>
<#if helper.getCacheAccessType()??>
        <caching access="${helper.getCacheAccessType()}"<#if helper.getCacheRegion()??> region="${helper.getCacheRegion()}"</#if><#if !helper.isCacheIncludeLazy()> includeLazy="false"</#if>/>
</#if>
<#if helper.isConcreteProxy()>
        <lazy>false</lazy>
</#if>
<#if !helper.isMutable()>
        <mutable>false</mutable>
</#if>
<#if helper.getOptimisticLockMode()??>
        <optimistic-locking>${helper.getOptimisticLockMode()}</optimistic-locking>
</#if>
<#if (helper.getEntityListenerClassNames()?size > 0)>
        <entity-listeners>
<#list helper.getEntityListenerClassNames() as listenerClass>
            <entity-listener class="${listenerClass}"/>
</#list>
        </entity-listeners>
</#if>
<#list helper.getLifecycleCallbacks() as callback>
        <${callback.elementName()} method-name="${callback.methodName()}"/>
</#list>
<#include "attributes.mapping.ftl"/>
<#if helper.getSQLInsert()??>
        <sql-insert<#if helper.getSQLInsert().callable()> callable="true"</#if>>${helper.getSQLInsert().sql()}</sql-insert>
</#if>
<#if helper.getSQLUpdate()??>
        <sql-update<#if helper.getSQLUpdate().callable()> callable="true"</#if>>${helper.getSQLUpdate().sql()}</sql-update>
</#if>
<#if helper.getSQLDelete()??>
        <sql-delete<#if helper.getSQLDelete().callable()> callable="true"</#if>>${helper.getSQLDelete().sql()}</sql-delete>
</#if>
<#if helper.getSQLDeleteAll()??>
        <sql-delete-all<#if helper.getSQLDeleteAll().callable()> callable="true"</#if>>${helper.getSQLDeleteAll().sql()}</sql-delete-all>
</#if>
<#list helper.getFetchProfiles() as fp>
        <fetch-profile name="${fp.name()}">
<#list fp.overrides() as fo>
            <fetch entity="${fo.entity()}" association="${fo.association()}" style="${fo.style()}"/>
</#list>
        </fetch-profile>
</#list>
<#list helper.getFilters() as filter>
        <filter name="${filter.name()}">
<#if filter.condition()?has_content>
            <condition>${filter.condition()}</condition>
</#if>
        </filter>
</#list>
    </entity>
