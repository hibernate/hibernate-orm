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
<#-- Version -->
<#list helper.getVersionFields() as field>
    <version
        name="${field.getName()}"
        type="${helper.getHibernateTypeName(field)}">
        <column name="${helper.getColumnName(field)}" ${helper.getColumnAttributes(field)}/>
    </version>
</#list>
<#-- Properties -->
<#include "properties.hbm.ftl"/>
</${helper.getClassTag()}>
