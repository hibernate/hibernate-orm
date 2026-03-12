    <entity class="${table.getEntityPackage()}.${table.getEntityClassName()}">
<#include "table.mapping.ftl"/>
<#include "inheritance.mapping.ftl"/>
<#include "attributes.mapping.ftl"/>
    </entity>
