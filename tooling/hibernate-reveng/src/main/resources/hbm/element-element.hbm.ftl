<element type="${elementValue.getTypeName()}">
  <#list elementValue.columns as column>
        <#include "column.hbm.ftl">
  </#list>
</element>
