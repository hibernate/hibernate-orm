<?xml version="1.0"?>
<!DOCTYPE hibernate-mapping PUBLIC
	"-//Hibernate/Hibernate Mapping DTD 3.0//EN"
	"https://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">
<!-- Generated ${date?datetime} by Hibernate Tools ${version} -->

<hibernate-mapping<#if helper.getPackageName()??> package="${helper.getPackageName()}"</#if><#if settings.hasNonDefaultAccess()> default-access="${settings.defaultAccess()}"</#if><#if settings.hasNonDefaultCascade()> default-cascade="${settings.defaultCascade()}"</#if><#if !settings.defaultLazy()> default-lazy="false"</#if><#if !settings.autoImport()> auto-import="false"</#if>>
<#list helper.getImports() as imp>
    <import class="${imp.className()}" rename="${imp.rename()}"/>
</#list>
<#include "class.hbm.ftl"/>
<#list helper.getFilterDefs() as fd>
<#if fd.parameters()?size == 0>
    <filter-def name="${fd.name()}"<#if fd.defaultCondition()?? && fd.defaultCondition()?length != 0> condition="${fd.defaultCondition()}"</#if>/>
<#else>
    <filter-def name="${fd.name()}"<#if fd.defaultCondition()?? && fd.defaultCondition()?length != 0> condition="${fd.defaultCondition()}"</#if>>
<#list fd.parameters()?keys as paramName>
        <filter-param name="${paramName}" type="${fd.parameters()[paramName]}"/>
</#list>
    </filter-def>
</#if>
</#list>
<#list helper.getNamedQueries() as nq>
    <query
        name="${nq.name()}"<#if nq.flushMode()?has_content>
        flush-mode="${nq.flushMode()}"</#if><#if nq.cacheable()>
        cacheable="true"</#if><#if nq.cacheRegion()?has_content>
        cache-region="${nq.cacheRegion()}"</#if><#if (nq.fetchSize() > 0)>
        fetch-size="${nq.fetchSize()?c}"</#if><#if (nq.timeout() > 0)>
        timeout="${nq.timeout()?c}"</#if><#if nq.readOnly()>
        read-only="true"</#if><#if nq.comment()?has_content>
        comment="${nq.comment()}"</#if>><![CDATA[${nq.query()}]]></query>
</#list>
<#list helper.getNamedNativeQueries() as nq>
    <sql-query
        name="${nq.name()}"<#if nq.flushMode()?has_content>
        flush-mode="${nq.flushMode()}"</#if><#if nq.cacheable()>
        cacheable="true"</#if><#if nq.cacheRegion()?has_content>
        cache-region="${nq.cacheRegion()}"</#if><#if (nq.fetchSize() > 0)>
        fetch-size="${nq.fetchSize()?c}"</#if><#if (nq.timeout() > 0)>
        timeout="${nq.timeout()?c}"</#if><#if nq.readOnly()>
        read-only="true"</#if><#if nq.comment()?has_content>
        comment="${nq.comment()}"</#if>>
<#list nq.querySpaces() as tableName>
        <synchronize table="${tableName}"/>
</#list>
<#list nq.entityReturns() as ret>
<#if (ret.fieldMappings()?size == 0)>
        <return alias="${ret.entityClass()}" class="${ret.entityClass()}"<#if ret.discriminatorColumn()?has_content> discriminator-value="${ret.discriminatorColumn()}"</#if>/>
<#else>
        <return alias="${ret.entityClass()}" class="${ret.entityClass()}"<#if ret.discriminatorColumn()?has_content> discriminator-value="${ret.discriminatorColumn()}"</#if>>
<#list ret.fieldMappings() as fm>
            <return-property name="${fm.name()}" column="${fm.column()}"/>
</#list>
        </return>
</#if>
</#list>
<#list nq.scalarReturns() as sr>
        <return-scalar column="${sr.column()}"/>
</#list>
        <![CDATA[${nq.query()}]]>
    </sql-query>
</#list>
</hibernate-mapping>
