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

<#list md.entityBindings as entity>
  /* Node ${entity.entityName} */
  <@nodeName entity.entityName/> [ label = "<@propertyLabels name=entity.entityName properties=entity.properties/>", URL="${entity.entityName?replace(".","/")}.html" ]
  /* Subclass edges for ${entity.entityName} */
  <#list entity.getDirectSubclasses() as subclass>
     <@nodeName subclass.entityName/> -> <@nodeName entity.entityName/>  [ weight="10", arrowhead="onormal"  ]
  </#list>
  
  <@propertyEdges root=entity.entityName?replace(".","_dot_") properties=entity.properties/>     
</#list>

}

<#macro nodeName name>${name?replace(".","_dot_")}</#macro>

<#macro propertyLabels name properties>
<@compress single_line=true>
             { 
                ${name?replace(".","\\.")}|
                <#list properties as p>
                  <#if p.value.isSimpleValue()> 
                   ${p.name}\l
                 </#if>
                </#list>
              }</@compress></#macro>

<#macro dumpComponent compProperty>
      <#assign component=compProperty.value>
      /* Node component ${component} */
      ${c2h.getHibernateTypeName(compProperty)?replace(".","_dot_")} [ 
        label = "<@propertyLabels name=component.componentClassName properties=component.properties/>"
           ]
      <@propertyEdges root=component.componentClassName?replace(".","_dot_") properties=component.properties/>   
</#macro>

<#macro propertyEdges root properties>
  /* Property edges/nodes for ${root} */
  <#list properties as property>
     <#if c2h.getSafeHibernateTypeName(property)?exists>
     ${root} -> ${c2h.getHibernateTypeName(property)?replace(".","_dot_")} [ 
        label="${property.name}" 
        <#if c2j.isComponent(property)>
        arrowtail="diamond"
        </#if>
        ]
     </#if>
     <#if c2j.isComponent(property)>
     <@dumpComponent property/>
     </#if>
     
  </#list>
</#macro>