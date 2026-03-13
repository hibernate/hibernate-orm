<${helper.getClassTag()}
    name="${helper.getFullClassName()}"
<#if helper.isSubclass()>
    extends="${helper.getFullParentClassName()}"
</#if>
<#if !helper.isSubclass()>
    table="${table.getTableName()}"
<#if table.getSchema()??>
    schema="${table.getSchema()}"
</#if>
<#if table.getCatalog()??>
    catalog="${table.getCatalog()}"
</#if>
</#if>
<#if table.getDiscriminatorValue()??>
    discriminator-value="${table.getDiscriminatorValue()}"
</#if>
>
<#if table.getComment()?? && table.getComment()?trim?length != 0>
    <comment>${table.getComment()}</comment>
</#if>
<#-- Meta attributes -->
<#include "meta.hbm.ftl"/>
<#-- ID / Key -->
<#if !helper.isSubclass()>
<#include "id.hbm.ftl"/>
<#elseif table.getPrimaryKeyJoinColumnName()??>
    <key>
        <column name="${table.getPrimaryKeyJoinColumnName()}"/>
    </key>
</#if>
<#-- Discriminator -->
<#if helper.needsDiscriminator()>
    <discriminator
        column="${table.getInheritance().getDiscriminatorColumnName()}"
        type="${helper.getDiscriminatorTypeName()}"<#if table.getInheritance().getDiscriminatorColumnLength() gt 0>
        length="${table.getInheritance().getDiscriminatorColumnLength()?c}"</#if>/>
</#if>
<#-- Version -->
<#list table.getColumns() as col>
<#if col.isVersion()>
    <version
        name="${col.getFieldName()}"
        type="${helper.getHibernateTypeName(col)}">
        <column name="${col.getColumnName()}" ${helper.getColumnAttributes(col)}/>
    </version>
</#if>
</#list>
<#-- Properties -->
<#include "properties.hbm.ftl"/>
</${helper.getClassTag()}>
