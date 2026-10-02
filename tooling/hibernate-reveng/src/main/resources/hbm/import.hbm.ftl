<#list md.imports.keySet() as importKey>
<#assign importDef = md.imports.get(importKey)>
<#if !importKey.equals(importDef)>
    <import class="${importKey}" rename="${importDef}"/>
</#if></#list>