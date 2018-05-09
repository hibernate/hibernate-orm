<#foreach importKey in md.imports.keySet()>
<#assign importDef = md.imports.get(importKey)>
<#if !importKey.equals(importDef)>
    <import class="${importKey}" rename="${importDef}"/>
</#if></#foreach>