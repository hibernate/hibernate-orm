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
digraph TableGraph {
  compound=true;
    bgcolor="white"; 
    fontcolor="black"; fontname="Helvetica"; fontsize="10.0"; 
    ranksep="equally"; 
    label="Table Graph"; 
    URL="http://tools.hibernate.org";
  edge [
        color="lightgrey", 
        fontcolor="black", 
        fontname="Helvetica", 
        fontsize="8.0",         
        labelangle=-25.0, 
        labeldistance=1.5
        headport=nw,
        tailport=se
        ];
  node [
       fontcolor="black",
       fontname="Helvetica", 
       fontsize="10.0", 
       shape=record, 
       fillcolor="yellow", 
       style="solid,filled"];

/* TODO: multi schema tables */
<#list tables as table>
  <#if table.isPhysicalTable()>
  /* Node ${table.name} */
  <@nodeName table/> [ label = "<@columnLabels name=table.name columns=table.columns/>" ]  
  
  <@propertyEdges root=table.name?replace(".","_dot_") foreignKeys=table.foreignKeys.values()/>     
  </#if>
</#list>

}

<#macro nodeName table>${table.name?replace(".","_dot_")}</#macro>

<#macro columnLabels name columns>
<@compress single_line=true>
             { 
                ${name?replace(".","\\.")}|
                <#list columns as p>
                   <${p.name}>${p.name}\l
                   <#if p_has_next>|</#if>
                </#list>
              }</@compress></#macro>

<#macro propertyEdges root foreignKeys>
  /* edges/nodes for ${root} */
  <#list foreignKeys as fk>
     ${root} -> <@nodeName fk.referencedTable/> [ 
        label="${fk.name}" 
        ]
  </#list>
</#macro>