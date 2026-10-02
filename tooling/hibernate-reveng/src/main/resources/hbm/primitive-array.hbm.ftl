<#assign value = property.value>
<#assign table = value.collectionTable.name>
<#assign dependentValue = value.getKey()>

<primitive-array name="${property.name}" table="${table}"
<#if !property.basicPropertyAccessor>
    access="${property.propertyAccessorName}"
</#if>>
 <#assign metaattributable=property>
 <#include "meta.hbm.ftl">
 <key>
       <#list dependentValue.selectables as column>
         <#include "column.hbm.ftl">
       </#list>
 </key>
 <index>
       <#list value.index.selectables as column>
         <#include "column.hbm.ftl">   
       </#list>
 </index>
 <element type="${value.getElementClass()}" >
       <#list value.element.selectables as column>
         <#include "column.hbm.ftl">   
       </#list>
 </element>
</primitive-array>