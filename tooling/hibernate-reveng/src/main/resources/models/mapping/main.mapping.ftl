<?xml version="1.0" encoding="UTF-8"?>
<entity-mappings xmlns="http://www.hibernate.org/xsd/orm/mapping"
                 version="8.0">
<#if helper.getPackageName()??>
    <package>${helper.getPackageName()}</package>
</#if>
<#if helper.isEmbeddable()>
    <embeddable class="${helper.getClassName()}">
<#include "attributes.mapping.ftl"/>
    </embeddable>
<#else>
<#list helper.getFilterDefs() as fd>
    <filter-def name="${fd.name()}">
<#if fd.defaultCondition()?has_content>
        <default-condition>${fd.defaultCondition()}</default-condition>
</#if>
<#list fd.parameters()?keys as paramName>
        <filter-param name="${paramName}" type="${fd.parameters()[paramName]}"/>
</#list>
    </filter-def>
</#list>
<#include "entity.mapping.ftl"/>
<#list helper.getNamedQueries() as nq>
    <named-query name="${nq.name()}">
        <query>${nq.query()}</query>
    </named-query>
</#list>
<#list helper.getNamedNativeQueries() as nnq>
    <named-native-query name="${nnq.name()}">
        <query>${nnq.query()}</query>
    </named-native-query>
</#list>
</#if>
</entity-mappings>
