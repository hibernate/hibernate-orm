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
<#-- ID / Key -->
<#if !helper.isSubclass()>
<#include "id.hbm.ftl"/>
<#elseif helper.getPrimaryKeyJoinColumnName()??>
    <key>
        <column name="${helper.getPrimaryKeyJoinColumnName()}"/>
    </key>
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
        <column name="${helper.getColumnName(field)}" ${helper.getColumnAttributes(field)}/>
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
            <column name="${helper.getColumnName(field)}" ${helper.getColumnAttributes(field)}/>
        </property>
</#list>
    </join>
</#list>
<#-- Filters -->
<#list helper.getFilters() as filter>
    <filter name="${filter.name()}"<#if filter.condition()?? && filter.condition()?length != 0> condition="${filter.condition()}"</#if>/>
</#list>
</${helper.getClassTag()}>
