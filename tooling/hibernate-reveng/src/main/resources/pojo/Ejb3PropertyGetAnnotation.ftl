<#if ejb3>
<#if pojo.hasIdentifierProperty()>
<#if property.equals(clazz.identifierProperty)>
 ${pojo.generateAnnIdGenerator()}
<#-- if this is the id property (getter)-->
<#-- explicitly set the column name for this property-->
</#if>
</#if>

<#if c2h.isOneToOne(property)>
${pojo.generateOneToOneAnnotation(property, md)}
<#elseif c2h.isManyToOne(property)>
${pojo.generateManyToOneAnnotation(property)}
<#--TODO support optional and targetEntity-->    
${pojo.generateJoinColumnsAnnotation(property, md)}
<#elseif c2h.isCollection(property)>
${pojo.generateCollectionAnnotation(property, md)}
<#else>
${pojo.generateBasicAnnotation(property)}
${pojo.generateAnnColumnAnnotation(property)}
</#if>
</#if>