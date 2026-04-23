<#-- Interface property accessors: method signatures only, no bodies -->
<#-- Composite ID accessor -->
<#if templateHelper.getCompositeIdField()??>
<#assign cid = templateHelper.getCompositeIdField()>
    ${meta.getPropertyGetModifiers(cid)} ${templateHelper.getJavaTypeName(cid)} ${templateHelper.getGetterName(cid.getName())}();
    ${meta.getPropertySetModifiers(cid)} void ${templateHelper.getSetterName(cid.getName())}(${templateHelper.getJavaTypeName(cid)} ${templateHelper.getFieldName(cid)});

</#if>
<#-- Basic field accessors -->
<#list templateHelper.getBasicFields() as field>
<#if meta.isGenProperty(field)>
    ${meta.getPropertyGetModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${templateHelper.getGetterName(field)}();
    ${meta.getPropertySetModifiers(field)} void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getJavaTypeName(field)} ${templateHelper.getFieldName(field)});

</#if>
</#list>
<#-- ManyToOne accessors -->
<#list templateHelper.getManyToOneFields() as field>
    ${meta.getPropertyGetModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${templateHelper.getGetterName(field)}();
    ${meta.getPropertySetModifiers(field)} void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getJavaTypeName(field)} ${templateHelper.getFieldName(field)});

</#list>
<#-- OneToOne accessors -->
<#list templateHelper.getOneToOneFields() as field>
    ${meta.getPropertyGetModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${templateHelper.getGetterName(field)}();
    ${meta.getPropertySetModifiers(field)} void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getJavaTypeName(field)} ${templateHelper.getFieldName(field)});

</#list>
<#-- OneToMany accessors -->
<#list templateHelper.getOneToManyFields() as field>
    ${meta.getPropertyGetModifiers(field)} ${templateHelper.getCollectionTypeName(field)} ${templateHelper.getGetterName(field.getName())}();
    ${meta.getPropertySetModifiers(field)} void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getCollectionTypeName(field)} ${templateHelper.getFieldName(field)});

</#list>
<#-- ManyToMany accessors -->
<#list templateHelper.getManyToManyFields() as field>
    ${meta.getPropertyGetModifiers(field)} ${templateHelper.getCollectionTypeName(field)} ${templateHelper.getGetterName(field.getName())}();
    ${meta.getPropertySetModifiers(field)} void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getCollectionTypeName(field)} ${templateHelper.getFieldName(field)});

</#list>
<#-- Embedded field accessors -->
<#list templateHelper.getEmbeddedFields() as field>
    ${meta.getPropertyGetModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${templateHelper.getGetterName(field)}();
    ${meta.getPropertySetModifiers(field)} void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getJavaTypeName(field)} ${templateHelper.getFieldName(field)});

</#list>
