<many-to-many entity-name="${property.getValue().getElement().referencedEntityName}" <#-- lookup needed classname -->
<#if property.value.referencedPropertyName?exists>
        property-ref="${property.value.referencedPropertyName}"
</#if>>
  <#foreach column in elementValue.columnIterator>
        <#include "column.hbm.ftl">
  </#foreach>
</many-to-many>
