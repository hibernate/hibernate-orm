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

<#foreach entity in md.entityBindings> 
  /* Node ${entity.entityName} */
  <@nodeName entity.entityName/> [ label = "<@propertyLabels name=entity.entityName properties=entity.propertyIterator/>", URL="${entity.entityName?replace(".","/")}.html" ]
  /* Subclass edges for ${entity.entityName} */
  <#foreach subclass in entity.getDirectSubclasses()>
     <@nodeName subclass.entityName/> -> <@nodeName entity.entityName/>  [ weight="10", arrowhead="onormal"  ]
  </#foreach>
  
  <@propertyEdges root=entity.entityName?replace(".","_dot_") properties=entity.propertyIterator/>     
</#foreach>

}

<#macro nodeName name>${name?replace(".","_dot_")}</#macro>

<#macro propertyLabels name properties>
<@compress single_line=true>
             { 
                ${name?replace(".","\\.")}|
                <#foreach p in properties>
                 <#if p.value.isSimpleValue()> 
                   ${p.name}\l
                 </#if>
                </#foreach>
              }</@compress></#macro>

<#macro dumpComponent compProperty>
      <#assign component=compProperty.value>
      /* Node component ${component} */
      ${c2h.getHibernateTypeName(compProperty)?replace(".","_dot_")} [ 
        label = "<@propertyLabels name=component.componentClassName properties=component.propertyIterator/>"
           ]
      <@propertyEdges root=component.componentClassName?replace(".","_dot_") properties=component.propertyIterator/>   
</#macro>

<#macro propertyEdges root properties>
  /* Property edges/nodes for ${root} */
  <#foreach property in properties>
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
     
  </#foreach>
</#macro>