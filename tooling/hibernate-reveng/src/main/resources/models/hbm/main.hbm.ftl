<?xml version="1.0"?>
<!DOCTYPE hibernate-mapping PUBLIC
	"-//Hibernate/Hibernate Mapping DTD 3.0//EN"
	"https://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">

<hibernate-mapping<#if helper.getPackageName()??> package="${helper.getPackageName()}"</#if>>
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
    <query name="${nq.name()}"><![CDATA[${nq.query()}]]></query>
</#list>
<#list helper.getNamedNativeQueries() as nq>
    <sql-query name="${nq.name()}"><![CDATA[${nq.query()}]]></sql-query>
</#list>
</hibernate-mapping>
