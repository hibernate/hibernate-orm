<${classInfo.getClassTag()} name="${classInfo.getClassName()}"
<#if classInfo.isSubclass()>
    extends="${classInfo.getParentClassName()}"
</#if>
<#if !classInfo.isSubclass()>
    table="${classInfo.getTableName()}"
<#if classInfo.getSchema()??>
    schema="${classInfo.getSchema()}"
</#if>
<#if classInfo.getCatalog()??>
    catalog="${classInfo.getCatalog()}"
</#if>
</#if>
<#if classInfo.getDiscriminatorValue()??>
    discriminator-value="${classInfo.getDiscriminatorValue()}"
</#if>
<#if !classInfo.isMutable()>
    mutable="false"
</#if>
<#if classInfo.isDynamicUpdate()>
    dynamic-update="true"
</#if>
<#if classInfo.isDynamicInsert()>
    dynamic-insert="true"
</#if>
<#if classInfo.isAbstract()>
    abstract="true"
</#if>
<#if classInfo.getProxy()??>
    proxy="${classInfo.getProxy()}"
<#elseif classInfo.isConcreteProxy()>
    lazy="false"
</#if>
<#if (classInfo.getBatchSize() gt 1)>
    batch-size="${classInfo.getBatchSize()?c}"
</#if>
<#if classInfo.getWhere()??>
    where="${classInfo.getWhere()}"
</#if>
<#if classInfo.getOptimisticLockMode()??>
    optimistic-lock="${classInfo.getOptimisticLockMode()}"
</#if>
<#if classInfo.getRowId()??>
    rowid="${classInfo.getRowId()}"
</#if>
<#if classInfo.getSubselect()??>
    subselect="${classInfo.getSubselect()}"
</#if>
<#if classInfo.getEntityName()??>
    entity-name="${classInfo.getEntityName()}"
</#if>
>
<#if classInfo.getComment()?? && classInfo.getComment()?trim?length != 0>
    <comment>${classInfo.getComment()}</comment>
</#if>
<#-- Meta attributes -->
<#include "meta.hbm.ftl"/>
<#-- Cache -->
<#if classInfo.getCacheUsage()??>
    <cache usage="${classInfo.getCacheUsage()}"<#if classInfo.getCacheRegion()??> region="${classInfo.getCacheRegion()}"</#if><#if classInfo.getCacheInclude()??> include="${classInfo.getCacheInclude()}"</#if>/>
</#if>
<#-- ID / Key -->
<#if !classInfo.isSubclass()>
<#include "id.hbm.ftl"/>
<#elseif classInfo.getPrimaryKeyJoinColumnName()??>
    <key>
        <column name="${classInfo.getPrimaryKeyJoinColumnName()}"/>
    </key>
</#if>
<#-- Natural ID -->
<#if (fields.getNaturalIdFields()?size > 0)>
    <natural-id mutable="${fields.isNaturalIdMutable()?c}">
<#list fields.getNaturalIdFields() as field>
        <property
            name="${field.getName()}"
            type="${fieldAttrs.getHibernateTypeName(field)}">
<#if fieldAttrs.getColumnComment(field)??>
            <column name="${fieldAttrs.getColumnName(field)}" ${fieldAttrs.getColumnAttributes(field)}>
                <comment>${fieldAttrs.getColumnComment(field)}</comment>
            </column>
<#else>
            <column name="${fieldAttrs.getColumnName(field)}" ${fieldAttrs.getColumnAttributes(field)}/>
</#if>
        </property>
</#list>
    </natural-id>
</#if>
<#-- Discriminator -->
<#if classInfo.needsDiscriminator()>
    <discriminator
        column="${classInfo.getDiscriminatorColumnName()}"
        type="${classInfo.getDiscriminatorTypeName()}"<#if (classInfo.getDiscriminatorColumnLength() gt 0)>
        length="${classInfo.getDiscriminatorColumnLength()?c}"</#if>/>
</#if>
<#-- Version / Timestamp -->
<#list fields.getVersionFields() as field>
<#if fieldAttrs.isTimestamp(field)>
    <timestamp
        name="${field.getName()}"
        column="${fieldAttrs.getColumnName(field)}"<#if fieldAttrs.getAccessType(field)??>
        access="${fieldAttrs.getAccessType(field)}"</#if>/>
<#else>
    <version
        name="${field.getName()}"
        type="${fieldAttrs.getHibernateTypeName(field)}"<#if fieldAttrs.getAccessType(field)??>
        access="${fieldAttrs.getAccessType(field)}"</#if>>
<#if fieldAttrs.getColumnComment(field)??>
        <column name="${fieldAttrs.getColumnName(field)}" ${fieldAttrs.getColumnAttributes(field)}>
            <comment>${fieldAttrs.getColumnComment(field)}</comment>
        </column>
<#else>
        <column name="${fieldAttrs.getColumnName(field)}" ${fieldAttrs.getColumnAttributes(field)}/>
</#if>
    </version>
</#if>
</#list>
<#-- Properties -->
<#include "properties.hbm.ftl"/>
<#-- Joins (SecondaryTable) -->
<#list queries.getJoins() as join>
    <join table="${join.tableName()}">
<#if queries.getJoinComment(join.tableName())??>
        <comment>${queries.getJoinComment(join.tableName())}</comment>
</#if>
        <key>
<#list join.keyColumns() as colName>
            <column name="${colName}"/>
</#list>
        </key>
<#list queries.getJoinProperties(join.tableName()) as field>
        <property
            name="${field.getName()}"
            type="${fieldAttrs.getHibernateTypeName(field)}"<#if fieldAttrs.getAccessType(field)??>
            access="${fieldAttrs.getAccessType(field)}"</#if>>
<#if fieldAttrs.getColumnComment(field)??>
            <column name="${fieldAttrs.getColumnName(field)}" ${fieldAttrs.getColumnAttributes(field)}>
                <comment>${fieldAttrs.getColumnComment(field)}</comment>
            </column>
<#else>
            <column name="${fieldAttrs.getColumnName(field)}" ${fieldAttrs.getColumnAttributes(field)}/>
</#if>
        </property>
</#list>
    </join>
</#list>
<#-- SQL operations -->
<#if queries.getSQLInsert()??>
    <sql-insert<#if queries.getSQLInsert().callable()> callable="true"</#if>>${queries.getSQLInsert().sql()}</sql-insert>
</#if>
<#if queries.getSQLUpdate()??>
    <sql-update<#if queries.getSQLUpdate().callable()> callable="true"</#if>>${queries.getSQLUpdate().sql()}</sql-update>
</#if>
<#if queries.getSQLDelete()??>
    <sql-delete<#if queries.getSQLDelete().callable()> callable="true"</#if>>${queries.getSQLDelete().sql()}</sql-delete>
</#if>
<#-- Fetch profiles -->
<#list queries.getFetchProfiles() as fp>
    <fetch-profile name="${fp.name()}">
<#list fp.overrides() as fo>
        <fetch entity="${fo.entity()}" association="${fo.association()}" style="${fo.style()}"/>
</#list>
    </fetch-profile>
</#list>
<#-- Filters -->
<#list queries.getFilters() as filter>
    <filter name="${filter.name()}"<#if filter.condition()?? && filter.condition()?length != 0> condition="${filter.condition()}"</#if>/>
</#list>
</${classInfo.getClassTag()}>
