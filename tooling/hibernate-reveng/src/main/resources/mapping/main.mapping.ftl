<?xml version="1.0" encoding="UTF-8"?>
<!-- Generated ${date?datetime} by Hibernate Tools ${version} -->
<entity-mappings xmlns="http://www.hibernate.org/xsd/orm/mapping"
                 version="7.0">
<#if entityInfo.getPackageName()??>
    <package>${entityInfo.getPackageName()}</package>
</#if>
<#if entityInfo.isEmbeddable()>
    <embeddable class="${entityInfo.getClassName()}">
<#include "attributes.mapping.ftl"/>
    </embeddable>
<#else>
<#list queries.getFilterDefs() as fd>
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
<#list queries.getNamedQueries() as nq>
    <named-query name="${nq.name()}">
        <query>${nq.query()}</query>
    </named-query>
</#list>
<#list queries.getNamedNativeQueries() as nnq>
    <named-native-query name="${nnq.name()}">
        <query>${nnq.query()}</query>
    </named-native-query>
</#list>
</#if>
</entity-mappings>
