${pojo.getShortName()}
${pojo.shortName}

<#list [1,2,3] as cl>
 <@greet person=cl/>
</#list>


<#macro greet person>
  <font size="+2">Hello ${person} ${propertythatdoesnotexist}!</font>
</#macro>  
