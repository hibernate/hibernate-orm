<${helper.getClassTag()}
    name="${helper.getClassName()}"
<#if helper.isSubclass()>
    extends="${helper.getParentClassName()}"
</#if>
<#if !helper.isSubclass()>
    table="${helper.getTableName()}"
<#if helper.getSchema()??>
    schema="${helper.getSchema()}"
</#if>
<#if helper.getCatalog()??>
    catalog="${helper.getCatalog()}"
</#if>
</#if>
<#if helper.getDiscriminatorValue()??>
    discriminator-value="${helper.getDiscriminatorValue()}"
</#if>
<#if !helper.isMutable()>
    mutable="false"
</#if>
<#if helper.isDynamicUpdate()>
    dynamic-update="true"
</#if>
<#if helper.isDynamicInsert()>
    dynamic-insert="true"
</#if>
<#if helper.isAbstract()>
    abstract="true"
</#if>
<#if helper.isConcreteProxy()>
    lazy="false"
</#if>
<#if (helper.getBatchSize() gt 1)>
    batch-size="${helper.getBatchSize()?c}"
</#if>
<#if helper.getWhere()??>
    where="${helper.getWhere()}"
</#if>
<#if helper.getOptimisticLockMode()??>
    optimistic-lock="${helper.getOptimisticLockMode()}"
</#if>
<#if helper.getRowId()??>
    rowid="${helper.getRowId()}"
</#if>
<#if helper.getSubselect()??>
    subselect="${helper.getSubselect()}"
</#if>
<#if helper.getEntityName()??>
    entity-name="${helper.getEntityName()}"
</#if>
>
<#if helper.getComment()?? && helper.getComment()?trim?length != 0>
    <comment>${helper.getComment()}</comment>
</#if>
<#-- Meta attributes -->
<#include "meta.hbm.ftl"/>
<#-- Cache -->
<#if helper.getCacheUsage()??>
    <cache usage="${helper.getCacheUsage()}"<#if helper.getCacheRegion()??> region="${helper.getCacheRegion()}"</#if><#if helper.getCacheInclude()??> include="${helper.getCacheInclude()}"</#if>/>
</#if>
<#-- ID / Key -->
<#if !helper.isSubclass()>
<#include "id.hbm.ftl"/>
<#elseif helper.getPrimaryKeyJoinColumnName()??>
    <key>
        <column name="${helper.getPrimaryKeyJoinColumnName()}"/>
    </key>
</#if>
<#-- Natural ID -->
<#if (helper.getNaturalIdFields()?size > 0)>
    <natural-id mutable="${helper.isNaturalIdMutable()?c}">
<#list helper.getNaturalIdFields() as field>
        <property
            name="${field.getName()}"
            type="${helper.getHibernateTypeName(field)}">
<#if helper.getColumnComment(field)??>
            <column name="${helper.getColumnName(field)}" ${helper.getColumnAttributes(field)}>
                <comment>${helper.getColumnComment(field)}</comment>
            </column>
<#else>
            <column name="${helper.getColumnName(field)}" ${helper.getColumnAttributes(field)}/>
</#if>
        </property>
</#list>
    </natural-id>
</#if>
<#-- Discriminator -->
<#if helper.needsDiscriminator()>
    <discriminator
        column="${helper.getDiscriminatorColumnName()}"
        type="${helper.getDiscriminatorTypeName()}"<#if (helper.getDiscriminatorColumnLength() gt 0)>
        length="${helper.getDiscriminatorColumnLength()?c}"</#if>/>
</#if>
<#-- Version / Timestamp -->
<#list helper.getVersionFields() as field>
<#if helper.isTimestamp(field)>
    <timestamp
        name="${field.getName()}"
        column="${helper.getColumnName(field)}"/>
<#else>
    <version
        name="${field.getName()}"
        type="${helper.getHibernateTypeName(field)}"<#if helper.getAccessType(field)??>
        access="${helper.getAccessType(field)}"</#if>>
<#if helper.getColumnComment(field)??>
        <column name="${helper.getColumnName(field)}" ${helper.getColumnAttributes(field)}>
            <comment>${helper.getColumnComment(field)}</comment>
        </column>
<#else>
        <column name="${helper.getColumnName(field)}" ${helper.getColumnAttributes(field)}/>
</#if>
    </version>
</#if>
</#list>
<#-- Properties -->
<#include "properties.hbm.ftl"/>
<#-- Joins (SecondaryTable) -->
<#list helper.getJoins() as join>
    <join table="${join.tableName()}">
        <key>
<#list join.keyColumns() as colName>
            <column name="${colName}"/>
</#list>
        </key>
<#list helper.getJoinProperties(join.tableName()) as field>
        <property
            name="${field.getName()}"
            type="${helper.getHibernateTypeName(field)}"<#if helper.getAccessType(field)??>
            access="${helper.getAccessType(field)}"</#if>>
<#if helper.getColumnComment(field)??>
            <column name="${helper.getColumnName(field)}" ${helper.getColumnAttributes(field)}>
                <comment>${helper.getColumnComment(field)}</comment>
            </column>
<#else>
            <column name="${helper.getColumnName(field)}" ${helper.getColumnAttributes(field)}/>
</#if>
        </property>
</#list>
    </join>
</#list>
<#-- SQL operations -->
<#if helper.getSQLInsert()??>
    <sql-insert<#if helper.getSQLInsert().callable()> callable="true"</#if>>${helper.getSQLInsert().sql()}</sql-insert>
</#if>
<#if helper.getSQLUpdate()??>
    <sql-update<#if helper.getSQLUpdate().callable()> callable="true"</#if>>${helper.getSQLUpdate().sql()}</sql-update>
</#if>
<#if helper.getSQLDelete()??>
    <sql-delete<#if helper.getSQLDelete().callable()> callable="true"</#if>>${helper.getSQLDelete().sql()}</sql-delete>
</#if>
<#-- Fetch profiles -->
<#list helper.getFetchProfiles() as fp>
    <fetch-profile name="${fp.name()}">
<#list fp.overrides() as fo>
        <fetch entity="${fo.entity()}" association="${fo.association()}" style="${fo.style()}"/>
</#list>
    </fetch-profile>
</#list>
<#-- Filters -->
<#list helper.getFilters() as filter>
    <filter name="${filter.name()}"<#if filter.condition()?? && filter.condition()?length != 0> condition="${filter.condition()}"</#if>/>
</#list>
</${helper.getClassTag()}>
