<element type="${elementValue.getTypeName()}">
 <#foreach column in elementValue.columnIterator>
        <#include "column.hbm.ftl">
  </#foreach>
</element>
