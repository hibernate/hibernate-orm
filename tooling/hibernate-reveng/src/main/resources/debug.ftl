<#macro dump var>
   <#if var?is_hash>
   {
     <#list var?keys as key>
        ${key}:< <at> dump var[key]/>,
     </#list>
   }
   <#elseif var?is_sequence>
   [
     <#list sequence as elem>
       < <at> dump elem/>,
     </#list>
   ]
   <#else>
     ${var}
   </#if>
</#macro>

<@dump .vars/>
