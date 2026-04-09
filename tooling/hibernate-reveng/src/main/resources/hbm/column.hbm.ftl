<#--
~ Copyright 2010 - 2025 Red Hat, Inc.
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
<#if column.isFormula()>
<formula>${column.getFormula()}</formula>
<#else>
<column name="${column.quotedName}" ${c2h.columnAttributes(column)}<#if column.comment?exists && column.comment?trim?length!=0>>
<comment>${column.comment}</comment>
</column><#else>/>
</#if>
</#if>