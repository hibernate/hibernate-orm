<#list ctx?keys as item>
<#-- ${templates.create("generictemplates/pojo/generic-content", item + ".txt")}  -->
<#assign captured><#include "generic-content.ftl"/></#assign>
${templates.createFile(captured, "${item}.txt")}
</#list>
