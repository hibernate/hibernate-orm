name=<#if item?if_exists>${item.getName()}</#if>
pojo=${pojo.getDeclarationName()}
unsetproperty=${unsetProperty?if_exists}
proptest=${proptest?if_exists}
hibernatetool.booleanProperty=${.data_model["hibernatetool.booleanProperty"]?if_exists?string}
booleanProperty=${booleanProperty?if_exists?string}
<#if booleanProperty?exists && !booleanProperty>booleanWasTrue=true</#if>
myTool.value=<#if myTool?exists>${myTool.unqualify("myTool.value")}</#if>
<#if refproperty?exists>
<#assign inline= refproperty?interpret>
refproperty=<@inline/>
</#if>
