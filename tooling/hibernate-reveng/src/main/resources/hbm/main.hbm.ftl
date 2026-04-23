<?xml version="1.0"?>
<!DOCTYPE hibernate-mapping PUBLIC
	"-//Hibernate/Hibernate Mapping DTD 3.0//EN"
	"https://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">
<!-- Generated ${date?datetime} by Hibernate Tools ${version} -->

<hibernate-mapping<#if classInfo.getPackageName()??> package="${classInfo.getPackageName()}"</#if><#if settings.hasSchema()> schema="${settings.schema()}"</#if><#if settings.hasCatalog()> catalog="${settings.catalog()}"</#if><#if settings.hasNonDefaultAccess()> default-access="${settings.defaultAccess()}"</#if><#if settings.hasNonDefaultCascade()> default-cascade="${settings.defaultCascade()}"</#if><#if !settings.defaultLazy()> default-lazy="false"</#if><#if !settings.autoImport()> auto-import="false"</#if>>
<#list classInfo.getImports() as imp>
    <import class="${imp.className()}" rename="${imp.rename()}"/>
</#list>
<#include "class.hbm.ftl"/>
<#list queries.getFilterDefs() as fd>
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
<#list queries.getNamedQueries() as nq>
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
<#list queries.getNamedNativeQueries() as nq>
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
        <return alias="${ret.alias()}" class="${ret.entityClass()}"<#if ret.discriminatorColumn()?has_content> discriminator-value="${ret.discriminatorColumn()}"</#if>/>
<#else>
        <return alias="${ret.alias()}" class="${ret.entityClass()}"<#if ret.discriminatorColumn()?has_content> discriminator-value="${ret.discriminatorColumn()}"</#if>>
<#list ret.fieldMappings() as fm>
            <return-property name="${fm.name()}" column="${fm.column()}"/>
</#list>
        </return>
</#if>
</#list>
<#list nq.scalarReturns() as sr>
        <return-scalar column="${sr.column()}"/>
</#list>
<#list nq.returnJoins() as rj>
        <return-join alias="${rj.alias()}" property="${rj.property()}"/>
</#list>
<#list nq.loadCollections() as lc>
        <load-collection alias="${lc.alias()}" role="${lc.role()}"<#if lc.lockMode()?has_content> lock-mode="${lc.lockMode()}"</#if>/>
</#list>
        <![CDATA[${nq.query()}]]>
    </sql-query>
</#list>
</hibernate-mapping>
