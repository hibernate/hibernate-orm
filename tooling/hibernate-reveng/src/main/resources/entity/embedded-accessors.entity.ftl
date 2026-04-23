<#-- Embedded field accessors -->
<#list templateHelper.getEmbeddedFields() as field>
<#assign ann = relAnnotations.generateEmbeddedAnnotation(field)>
<#if ann?has_content>
    ${ann}
</#if>
<#assign accessAnn = fieldAnnotations.generateAccessAnnotation(field)>
<#if accessAnn?has_content>
    ${accessAnn}
</#if>
    ${meta.getPropertyGetModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${templateHelper.getGetterName(field)}() {
        return this.${templateHelper.getFieldName(field)};
    }

    ${meta.getPropertySetModifiers(field)} void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getJavaTypeName(field)} ${templateHelper.getFieldName(field)}) {
        this.${templateHelper.getFieldName(field)} = ${templateHelper.getFieldName(field)};
    }

</#list>
<#-- ElementCollection accessors -->
<#list templateHelper.getElementCollectionFields() as field>
<#assign ann = fieldAnnotations.generateElementCollectionAnnotation(field)>
<#if ann?has_content>
    ${ann}
</#if>
<#assign orderByAnn = fieldAnnotations.generateOrderByAnnotation(field)>
<#if orderByAnn?has_content>
    ${orderByAnn}
</#if>
<#assign orderColAnn = fieldAnnotations.generateOrderColumnAnnotation(field)>
<#if orderColAnn?has_content>
    ${orderColAnn}
</#if>
<#assign filterAnns = fieldAnnotations.generateFilterAnnotations(field)>
<#if filterAnns?has_content>
    ${filterAnns}
</#if>
<#assign accessAnn = fieldAnnotations.generateAccessAnnotation(field)>
<#if accessAnn?has_content>
    ${accessAnn}
</#if>
    ${meta.getPropertyGetModifiers(field)} ${templateHelper.getCollectionTypeName(field)} ${templateHelper.getGetterName(field.getName())}() {
        return this.${templateHelper.getFieldName(field)};
    }

    ${meta.getPropertySetModifiers(field)} void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getCollectionTypeName(field)} ${templateHelper.getFieldName(field)}) {
        this.${templateHelper.getFieldName(field)} = ${templateHelper.getFieldName(field)};
    }

</#list>
<#-- Any field accessors -->
<#list templateHelper.getAnyFields() as field>
<#assign ann = fieldAnnotations.generateAnyAnnotation(field)>
<#if ann?has_content>
    ${ann}
</#if>
<#assign colAnn = fieldAnnotations.generateColumnAnnotation(field)>
<#if colAnn?has_content>
    ${colAnn}
</#if>
    ${meta.getPropertyGetModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${templateHelper.getGetterName(field)}() {
        return this.${templateHelper.getFieldName(field)};
    }

    ${meta.getPropertySetModifiers(field)} void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getJavaTypeName(field)} ${templateHelper.getFieldName(field)}) {
        this.${templateHelper.getFieldName(field)} = ${templateHelper.getFieldName(field)};
    }

</#list>
<#-- ManyToAny field accessors -->
<#list templateHelper.getManyToAnyFields() as field>
<#assign ann = fieldAnnotations.generateManyToAnyAnnotation(field)>
<#if ann?has_content>
    ${ann}
</#if>
    ${meta.getPropertyGetModifiers(field)} ${templateHelper.getCollectionTypeName(field)} ${templateHelper.getGetterName(field.getName())}() {
        return this.${templateHelper.getFieldName(field)};
    }

    ${meta.getPropertySetModifiers(field)} void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getCollectionTypeName(field)} ${templateHelper.getFieldName(field)}) {
        this.${templateHelper.getFieldName(field)} = ${templateHelper.getFieldName(field)};
    }

</#list>
