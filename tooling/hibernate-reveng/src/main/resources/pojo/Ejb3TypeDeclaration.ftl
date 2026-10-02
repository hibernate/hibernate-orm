<#if ejb3?if_exists>
<#if pojo.isComponent()>
@${pojo.importType("jakarta.persistence.Embeddable")}
<#else>
@${pojo.importType("jakarta.persistence.Entity")}
@${pojo.importType("jakarta.persistence.Table")}(name="${clazz.table.name}"
<#if clazz.table.schema?exists>
    ,schema="${clazz.table.schema}"
</#if><#if clazz.table.catalog?exists>
    ,catalog="${clazz.table.catalog}"
</#if>
<#assign uniqueConstraint=pojo.generateAnnTableUniqueConstraint()>
<#if uniqueConstraint?has_content>
    , uniqueConstraints = ${uniqueConstraint} 
</#if>)
</#if>
</#if>