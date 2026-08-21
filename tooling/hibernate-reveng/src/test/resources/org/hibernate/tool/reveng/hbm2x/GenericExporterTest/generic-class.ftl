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
