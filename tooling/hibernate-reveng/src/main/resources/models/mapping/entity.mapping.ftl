    <entity class="${helper.getClassName()}">
<#if helper.getSubselect()??>
        <table-expression>${helper.getSubselect()}</table-expression>
<#else>
<#include "table.mapping.ftl"/>
</#if>
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
<#if helper.isConcreteProxy()>
        <lazy>false</lazy>
</#if>
<#if !helper.isMutable()>
        <mutable>false</mutable>
</#if>
<#if helper.getOptimisticLockMode()??>
        <optimistic-locking>${helper.getOptimisticLockMode()}</optimistic-locking>
</#if>
<#include "attributes.mapping.ftl"/>
<#list helper.getFilters() as filter>
        <filter name="${filter.name()}">
<#if filter.condition()?has_content>
            <condition>${filter.condition()}</condition>
</#if>
        </filter>
</#list>
    </entity>
