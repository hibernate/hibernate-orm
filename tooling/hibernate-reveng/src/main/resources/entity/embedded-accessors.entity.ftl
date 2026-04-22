<#-- Embedded field accessors -->
<#list templateHelper.getEmbeddedFields() as field>
<#assign ann = templateHelper.generateEmbeddedAnnotation(field)>
<#if ann?has_content>
    ${ann}
</#if>
<#assign accessAnn = templateHelper.generateAccessAnnotation(field)>
<#if accessAnn?has_content>
    ${accessAnn}
</#if>
    ${templateHelper.getPropertyGetModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${templateHelper.getGetterName(field)}() {
        return this.${templateHelper.getFieldName(field)};
    }

    ${templateHelper.getPropertySetModifiers(field)} void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getJavaTypeName(field)} ${templateHelper.getFieldName(field)}) {
        this.${templateHelper.getFieldName(field)} = ${templateHelper.getFieldName(field)};
    }

</#list>
<#-- ElementCollection accessors -->
<#list templateHelper.getElementCollectionFields() as field>
<#assign ann = templateHelper.generateElementCollectionAnnotation(field)>
<#if ann?has_content>
    ${ann}
</#if>
<#assign orderByAnn = templateHelper.generateOrderByAnnotation(field)>
<#if orderByAnn?has_content>
    ${orderByAnn}
</#if>
<#assign orderColAnn = templateHelper.generateOrderColumnAnnotation(field)>
<#if orderColAnn?has_content>
    ${orderColAnn}
</#if>
<#assign filterAnns = templateHelper.generateFilterAnnotations(field)>
<#if filterAnns?has_content>
    ${filterAnns}
</#if>
<#assign accessAnn = templateHelper.generateAccessAnnotation(field)>
<#if accessAnn?has_content>
    ${accessAnn}
</#if>
    ${templateHelper.getPropertyGetModifiers(field)} ${templateHelper.getCollectionTypeName(field)} ${templateHelper.getGetterName(field.getName())}() {
        return this.${templateHelper.getFieldName(field)};
    }

    ${templateHelper.getPropertySetModifiers(field)} void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getCollectionTypeName(field)} ${templateHelper.getFieldName(field)}) {
        this.${templateHelper.getFieldName(field)} = ${templateHelper.getFieldName(field)};
    }

</#list>
<#-- Any field accessors -->
<#list templateHelper.getAnyFields() as field>
<#assign ann = templateHelper.generateAnyAnnotation(field)>
<#if ann?has_content>
    ${ann}
</#if>
<#assign colAnn = templateHelper.generateColumnAnnotation(field)>
<#if colAnn?has_content>
    ${colAnn}
</#if>
    ${templateHelper.getPropertyGetModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${templateHelper.getGetterName(field)}() {
        return this.${templateHelper.getFieldName(field)};
    }

    ${templateHelper.getPropertySetModifiers(field)} void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getJavaTypeName(field)} ${templateHelper.getFieldName(field)}) {
        this.${templateHelper.getFieldName(field)} = ${templateHelper.getFieldName(field)};
    }

</#list>
<#-- ManyToAny field accessors -->
<#list templateHelper.getManyToAnyFields() as field>
<#assign ann = templateHelper.generateManyToAnyAnnotation(field)>
<#if ann?has_content>
    ${ann}
</#if>
    ${templateHelper.getPropertyGetModifiers(field)} ${templateHelper.getCollectionTypeName(field)} ${templateHelper.getGetterName(field.getName())}() {
        return this.${templateHelper.getFieldName(field)};
    }

    ${templateHelper.getPropertySetModifiers(field)} void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getCollectionTypeName(field)} ${templateHelper.getFieldName(field)}) {
        this.${templateHelper.getFieldName(field)} = ${templateHelper.getFieldName(field)};
    }

</#list>
