<#macro dump var>
   <#if var?is_hash>
   {
     <#foreach key in var?keys>
        ${key}:< <at> dump var[key]/>,
     </#foreach>
   }
   <#elseif var?is_sequence>
   [
     <#foreach elem in sequence>
       < <at> dump elem/>,
     </#foreach>
   ]
   <#else>
     ${var}
   </#if>
</#macro>

<@dump .vars/>
