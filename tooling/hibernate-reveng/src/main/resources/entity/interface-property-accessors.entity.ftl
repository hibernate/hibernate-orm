<#-- Interface property accessors: method signatures only, no bodies -->
<#-- Composite ID accessor -->
<#if templateHelper.getCompositeIdField()??>
<#assign cid = templateHelper.getCompositeIdField()>
    ${templateHelper.getPropertyGetModifiers(cid)} ${templateHelper.getJavaTypeName(cid)} ${templateHelper.getGetterName(cid.getName())}();
    ${templateHelper.getPropertySetModifiers(cid)} void ${templateHelper.getSetterName(cid.getName())}(${templateHelper.getJavaTypeName(cid)} ${templateHelper.getFieldName(cid)});

</#if>
<#-- Basic field accessors -->
<#list templateHelper.getBasicFields() as field>
<#if templateHelper.isGenProperty(field)>
    ${templateHelper.getPropertyGetModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${templateHelper.getGetterName(field)}();
    ${templateHelper.getPropertySetModifiers(field)} void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getJavaTypeName(field)} ${templateHelper.getFieldName(field)});

</#if>
</#list>
<#-- ManyToOne accessors -->
<#list templateHelper.getManyToOneFields() as field>
    ${templateHelper.getPropertyGetModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${templateHelper.getGetterName(field)}();
    ${templateHelper.getPropertySetModifiers(field)} void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getJavaTypeName(field)} ${templateHelper.getFieldName(field)});

</#list>
<#-- OneToOne accessors -->
<#list templateHelper.getOneToOneFields() as field>
    ${templateHelper.getPropertyGetModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${templateHelper.getGetterName(field)}();
    ${templateHelper.getPropertySetModifiers(field)} void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getJavaTypeName(field)} ${templateHelper.getFieldName(field)});

</#list>
<#-- OneToMany accessors -->
<#list templateHelper.getOneToManyFields() as field>
    ${templateHelper.getPropertyGetModifiers(field)} ${templateHelper.getCollectionTypeName(field)} ${templateHelper.getGetterName(field.getName())}();
    ${templateHelper.getPropertySetModifiers(field)} void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getCollectionTypeName(field)} ${templateHelper.getFieldName(field)});

</#list>
<#-- ManyToMany accessors -->
<#list templateHelper.getManyToManyFields() as field>
    ${templateHelper.getPropertyGetModifiers(field)} ${templateHelper.getCollectionTypeName(field)} ${templateHelper.getGetterName(field.getName())}();
    ${templateHelper.getPropertySetModifiers(field)} void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getCollectionTypeName(field)} ${templateHelper.getFieldName(field)});

</#list>
<#-- Embedded field accessors -->
<#list templateHelper.getEmbeddedFields() as field>
    ${templateHelper.getPropertyGetModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${templateHelper.getGetterName(field)}();
    ${templateHelper.getPropertySetModifiers(field)} void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getJavaTypeName(field)} ${templateHelper.getFieldName(field)});

</#list>
