<#if embeddedid?exists>   
   <composite-id>
 <#foreach keyproperty in embeddedid.propertyIterator>
	<#if !c2h.isManyToOne(keyproperty)>
	   <key-property name="${keyproperty.name}" type="${keyproperty.value.typeName}">
       <#foreach column in keyproperty.columnIterator>
         <#include "pkcolumn.hbm.ftl">
       </#foreach>
       </key-property>
	<#else>
	   <key-many-to-one name="${keyproperty.name}" class="${c2j.getJavaTypeName(keyproperty, false)}">
       <#foreach column in keyproperty.columnIterator>
          <#include "pkcolumn.hbm.ftl">
       </#foreach>
       </key-many-to-one>
	</#if>
 </#foreach>   
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
    
 <#foreach column in property.columnIterator>
	    <#include "pkcolumn.hbm.ftl">
 </#foreach>
 <#if !c2h.isIdentifierGeneratorProperties(property)>
	    <generator class="${property.value.identifierGeneratorStrategy}" />
 <#else>
	    <generator class="${property.value.identifierGeneratorStrategy}">
        <#assign parameters = c2h.getIdentifierGeneratorProperties(property)>
        <#foreach paramkey in c2h.getFilteredIdentifierGeneratorKeySet(property, props)>
            <param name="${paramkey}">${parameters.get(paramkey)}</param>
        </#foreach>
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
    <#foreach keyproperty in property.value.propertyIterator>
	  <#if !c2h.isManyToOne(keyproperty)>
	        <key-property name="${keyproperty.name}" type="${keyproperty.value.typeName}">
	        <#foreach column in keyproperty.columnIterator>
	           <#include "pkcolumn.hbm.ftl">
	        </#foreach>	
	        </key-property>
	  <#else>
			<key-many-to-one name="${keyproperty.name}" class="${c2j.getJavaTypeName(keyproperty, false)}">
            <#foreach column in keyproperty.columnIterator>
                <#include "pkcolumn.hbm.ftl">
            </#foreach>
        	</key-many-to-one>
	  </#if>
    </#foreach>
    </composite-id>	
</#if>