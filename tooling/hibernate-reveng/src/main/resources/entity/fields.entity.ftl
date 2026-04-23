<#-- Composite ID field -->
<#if templateHelper.getCompositeIdField()??>
<#assign cid = templateHelper.getCompositeIdField()>
    ${meta.getFieldModifiers(cid)} ${templateHelper.getJavaTypeName(cid)} ${templateHelper.getFieldName(cid)};

</#if>
<#-- Basic fields (respect gen-property) -->
<#list templateHelper.getBasicFields() as field>
<#if meta.isGenProperty(field)>
<#if meta.hasFieldDescription(field)>
    /**
     * ${meta.getFieldDescription(field)}
     */
</#if>
<#if meta.hasFieldDefaultValue(field)>
    ${meta.getFieldModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${templateHelper.getFieldName(field)} = ${meta.getFieldDefaultValue(field)};
<#else>
    ${meta.getFieldModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${templateHelper.getFieldName(field)};
</#if>

</#if>
</#list>
<#-- ManyToOne fields -->
<#list templateHelper.getManyToOneFields() as field>
    ${meta.getFieldModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${templateHelper.getFieldName(field)};

</#list>
<#-- OneToOne fields -->
<#list templateHelper.getOneToOneFields() as field>
    ${meta.getFieldModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${templateHelper.getFieldName(field)};

</#list>
<#-- OneToMany fields -->
<#list templateHelper.getOneToManyFields() as field>
    ${meta.getFieldModifiers(field)} ${templateHelper.getCollectionTypeName(field)} ${templateHelper.getFieldName(field)} = new ${templateHelper.getCollectionInitializerType(field)}<>(0);

</#list>
<#-- ManyToMany fields -->
<#list templateHelper.getManyToManyFields() as field>
    ${meta.getFieldModifiers(field)} ${templateHelper.getCollectionTypeName(field)} ${templateHelper.getFieldName(field)} = new ${templateHelper.getCollectionInitializerType(field)}<>(0);

</#list>
<#-- Embedded fields -->
<#list templateHelper.getEmbeddedFields() as field>
    ${meta.getFieldModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${templateHelper.getFieldName(field)};

</#list>
<#-- ElementCollection fields -->
<#list templateHelper.getElementCollectionFields() as field>
    ${meta.getFieldModifiers(field)} ${templateHelper.getCollectionTypeName(field)} ${templateHelper.getFieldName(field)} = new ${templateHelper.getCollectionInitializerType(field)}<>(0);

</#list>
<#-- Any fields -->
<#list templateHelper.getAnyFields() as field>
    ${meta.getFieldModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${templateHelper.getFieldName(field)};

</#list>
<#-- ManyToAny fields -->
<#list templateHelper.getManyToAnyFields() as field>
    ${meta.getFieldModifiers(field)} ${templateHelper.getCollectionTypeName(field)} ${templateHelper.getFieldName(field)} = new ${templateHelper.getCollectionInitializerType(field)}<>(0);

</#list>