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
<#foreach table in tables> 
  <#if table.isPhysicalTable()>
  /* Node ${table.name} */
  <@nodeName table/> [ label = "<@columnLabels name=table.name columns=table.columnIterator/>" ]  
  
  <@propertyEdges root=table.name?replace(".","_dot_") foreignKeys=table.foreignKeyIterator/>     
  </#if>
</#foreach>

}

<#macro nodeName table>${table.name?replace(".","_dot_")}</#macro>

<#macro columnLabels name columns>
<@compress single_line=true>
             { 
                ${name?replace(".","\\.")}|
                <#foreach p in columns>
                   <${p.name}>${p.name}\l
                   <#if p_has_next>|</#if>
                </#foreach>
              }</@compress></#macro>

<#macro propertyEdges root foreignKeys>
  /* edges/nodes for ${root} */
  <#foreach fk in foreignKeys>
     ${root} -> <@nodeName fk.referencedTable/> [ 
        label="${fk.name}" 
        ]
  </#foreach>
</#macro>