    <property
        name="${property.name}"
<#if !property.value.typeParameters?exists>
	    type="${property.value.typeName}"
</#if>        
<#if !property.updateable>
        update="false"
</#if>
<#if !property.insertable>
        insert="false"
</#if>
<#if !property.basicPropertyAccessor>
        access="${property.propertyAccessorName}"
</#if>
<#if property.lazy>
        lazy="true"
</#if>
<#if !property.optimisticLocked>
        optimistic-lock="false"
</#if>
<#if property.value.hasFormula()>
<#assign formula = c2h.getFormulaForProperty(property)>
<#if formula?has_content>
        formula="${formula.text}"
</#if>
</#if>
    >
  <#assign metaattributable=property>
  <#include "meta.hbm.ftl">
  <#foreach column in property.columnIterator>
     <#if !column.isFormula()>
        <#include "column.hbm.ftl">
     </#if>
  </#foreach>	
  <#if property.value.typeParameters?exists>
  <type name="${property.value.typeName}">
  		<#foreach entry in property.value.typeParameters.entrySet()>
              		<param name="${entry.key}">${entry.value}</param>
        </#foreach>  	  
  </type>
  </#if>
  </property>

