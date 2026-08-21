<#--
  ~ Copyright 2004 - 2025 Red Hat, Inc.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" basis,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->
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
