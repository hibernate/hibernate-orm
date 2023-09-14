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