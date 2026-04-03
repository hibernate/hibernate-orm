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
digraph EntityGraph {
  compound=true;
    bgcolor="white";
    fontcolor="black"; fontname="Helvetica"; fontsize="10.0";
    ranksep="equally";
    label="Entity Graph";
    URL="http://tools.hibernate.org";
  edge [
        color="lightgrey",
        fontcolor="black",
        fontname="Helvetica",
        fontsize="8.0",
        labelangle=-25.0,
        labeldistance=1.5
        ];
  node [
       fontcolor="black",
       fontname="Helvetica",
       fontsize="10.0",
       shape=record,
       fillcolor="#D4E5FE",
       style="solid,filled"];

<#list entities as entity>
  /* Node ${entity.qualifiedDeclarationName} */
  <@nodeName entity.qualifiedDeclarationName/> [ label = "<@propertyLabels name=entity.qualifiedDeclarationName entity=entity/>", URL="${entity.qualifiedDeclarationName?replace(".","/")}.html" ]
  <#-- Subclass edge -->
  <#if entity.subclass>
  <@nodeName entity.qualifiedDeclarationName/> -> <@nodeName entity.superClass.qualifiedDeclarationName/>  [ weight="10", arrowhead="onormal"  ]
  </#if>

  <@propertyEdges root=entity.qualifiedDeclarationName?replace(".","_dot_") entity=entity/>
</#list>

}

<#macro nodeName name>${name?replace(".","_dot_")}</#macro>

<#macro propertyLabels name entity>
<@compress single_line=true>
             {
                ${name?replace(".","\\.")}|
                <#assign props = entity.allPropertiesIterator>
                <#list props as p>
                  <#if p.simpleValue>
                   ${p.name}\l
                 </#if>
                </#list>
              }</@compress></#macro>

<#macro propertyEdges root entity>
  /* Property edges/nodes for ${root} */
  <#assign props = entity.allPropertiesIterator>
  <#list props as property>
     <#if property.relationship || property.component>
     ${root} -> ${property.relationshipTargetName?replace(".","_dot_")} [
        label="${property.name}"
        <#if property.component>
        arrowtail="diamond"
        </#if>
        ]
     </#if>
  </#list>
</#macro>
