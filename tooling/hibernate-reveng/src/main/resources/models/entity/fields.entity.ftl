<#-- Composite ID field -->
<#if templateHelper.getCompositeIdField()??>
<#assign cid = templateHelper.getCompositeIdField()>
    ${templateHelper.getFieldModifiers(cid)} ${templateHelper.getJavaTypeName(cid)} ${cid.getName()};

</#if>
<#-- Basic fields (respect gen-property) -->
<#list templateHelper.getBasicFields() as field>
<#if templateHelper.isGenProperty(field)>
<#if templateHelper.hasFieldDescription(field)>
    /**
     * ${templateHelper.getFieldDescription(field)}
     */
</#if>
<#if templateHelper.hasFieldDefaultValue(field)>
    ${templateHelper.getFieldModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${field.getName()} = ${templateHelper.getFieldDefaultValue(field)};
<#else>
    ${templateHelper.getFieldModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${field.getName()};
</#if>

</#if>
</#list>
<#-- ManyToOne fields -->
<#list templateHelper.getManyToOneFields() as field>
    ${templateHelper.getFieldModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${field.getName()};

</#list>
<#-- OneToOne fields -->
<#list templateHelper.getOneToOneFields() as field>
    ${templateHelper.getFieldModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${field.getName()};

</#list>
<#-- OneToMany fields -->
<#list templateHelper.getOneToManyFields() as field>
    ${templateHelper.getFieldModifiers(field)} ${templateHelper.getCollectionTypeName(field)} ${field.getName()} = new ${templateHelper.getCollectionInitializerType(field)}<>(0);

</#list>
<#-- ManyToMany fields -->
<#list templateHelper.getManyToManyFields() as field>
    ${templateHelper.getFieldModifiers(field)} ${templateHelper.getCollectionTypeName(field)} ${field.getName()} = new ${templateHelper.getCollectionInitializerType(field)}<>(0);

</#list>
<#-- Embedded fields -->
<#list templateHelper.getEmbeddedFields() as field>
    ${templateHelper.getFieldModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${field.getName()};

</#list>
<#-- ElementCollection fields -->
<#list templateHelper.getElementCollectionFields() as field>
    ${templateHelper.getFieldModifiers(field)} ${templateHelper.getCollectionTypeName(field)} ${field.getName()} = new ${templateHelper.getCollectionInitializerType(field)}<>(0);

</#list>
<#-- Any fields -->
<#list templateHelper.getAnyFields() as field>
    ${templateHelper.getFieldModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${field.getName()};

</#list>
<#-- ManyToAny fields -->
<#list templateHelper.getManyToAnyFields() as field>
    ${templateHelper.getFieldModifiers(field)} ${templateHelper.getCollectionTypeName(field)} ${field.getName()} = new ${templateHelper.getCollectionInitializerType(field)}<>(0);

</#list>