${pojo.getShortName()}
${pojo.shortName}

<#list [1,2,3] as cl>
<#include "freeinc.ftl"/> 
<#include "freeinc.ftl"/> 
<@greet person=cl/>
</#list>


<#macro greet person>
  <font size="+2">Hello ${person} ${pojo.shortName}!</font>
</#macro>  


<#list .data_model?keys as item>
 ${item}
</#list>