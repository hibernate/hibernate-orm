<#if embeddedid?exists>   
   <composite-id>
 <#list embeddedid.properties as keyproperty>
	<#if !c2h.isManyToOne(keyproperty)>
	   <key-property name="${keyproperty.name}" type="${keyproperty.value.typeName}">
	   <#list keyproperty.columns as column>
         <#include "pkcolumn.hbm.ftl">
       </#list>
       </key-property>
	<#else>
	   <key-many-to-one name="${keyproperty.name}" class="${c2j.getJavaTypeName(keyproperty, false)}">
	   <#list keyproperty.columns as column>
          <#include "pkcolumn.hbm.ftl">
       </#list>
       </key-many-to-one>
	</#if>
 </#list>   
  </composite-id>   
<#elseif !c2j.isComponent(property)>
	<id 
        name="${property.name}"
        type="${property.value.typeName}"
 <#if c2h.isUnsavedValue(property)>
        unsaved-value="${c2h.getUnsavedValue(property)}"
 </#if>
 <#if !property.basicPropertyAccessor>
        access="${property.propertyAccessorName}"
 </#if>
    >
    <#assign metaattributable=property>
	<#include "meta.hbm.ftl">
    
 <#list property.columns as column>
 	    <#include "pkcolumn.hbm.ftl">
 </#list>
 <#if !c2h.isIdentifierGeneratorProperties(property)>
	    <generator class="${property.value.identifierGeneratorStrategy}" />
 <#else>
	    <generator class="${property.value.identifierGeneratorStrategy}">
        <#assign parameters = c2h.getIdentifierGeneratorProperties(property)>
        <#list c2h.getFilteredIdentifierGeneratorKeySet(property, props) as paramkey>
             <param name="${paramkey}">${parameters.get(paramkey)}</param>
        </#list>
		</generator>
 </#if>
    </id>
<#else>
    <composite-id
		name="${property.name}"
        class="${property.value.getComponentClassName()}"
<#if c2h.isUnsavedValue(property)>
        unsaved-value="${c2h.getUnsavedValue(property)}"
</#if>
<#if !property.basicPropertyAccessor>
        access="${property.propertyAccessorName}"
</#if>
    >		
    <#list property.value.properties as keyproperty>
	  <#if !c2h.isManyToOne(keyproperty)>
	        <key-property name="${keyproperty.name}" type="${keyproperty.value.typeName}">
	        <#list keyproperty.columns as column>
	           <#include "pkcolumn.hbm.ftl">
	        </#list>	
	        </key-property>
	  <#else>
			<key-many-to-one name="${keyproperty.name}" class="${c2j.getJavaTypeName(keyproperty, false)}">
			<#list keyproperty.columns as column>
                <#include "pkcolumn.hbm.ftl">
            </#list>
        	</key-many-to-one>
	  </#if>
    </#list>
    </composite-id>	
</#if>