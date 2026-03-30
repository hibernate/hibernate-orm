<?xml version="1.0" encoding="UTF-8"?>
<entity-mappings xmlns="http://www.hibernate.org/xsd/orm/mapping"
                 version="8.0">
<#if helper.getPackageName()??>
    <package>${helper.getPackageName()}</package>
</#if>
<#include "entity.mapping.ftl"/>
</entity-mappings>
