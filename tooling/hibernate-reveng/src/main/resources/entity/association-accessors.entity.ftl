<#-- ManyToOne accessors -->
<#list templateHelper.getManyToOneFields() as field>
<#assign ann = relAnnotations.generateManyToOneAnnotation(field)>
<#if ann?has_content>
    ${ann}
</#if>
<#assign fetchAnn = fieldAnnotations.generateFetchAnnotation(field)>
<#if fetchAnn?has_content>
    ${fetchAnn}
</#if>
<#assign notFoundAnn = fieldAnnotations.generateNotFoundAnnotation(field)>
<#if notFoundAnn?has_content>
    ${notFoundAnn}
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
<#-- OneToOne accessors -->
<#list templateHelper.getOneToOneFields() as field>
<#assign ann = relAnnotations.generateOneToOneAnnotation(field)>
<#if ann?has_content>
    ${ann}
</#if>
<#assign fetchAnn = fieldAnnotations.generateFetchAnnotation(field)>
<#if fetchAnn?has_content>
    ${fetchAnn}
</#if>
<#assign notFoundAnn = fieldAnnotations.generateNotFoundAnnotation(field)>
<#if notFoundAnn?has_content>
    ${notFoundAnn}
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
<#-- OneToMany accessors -->
<#list templateHelper.getOneToManyFields() as field>
<#assign ann = relAnnotations.generateOneToManyAnnotation(field)>
<#if ann?has_content>
    ${ann}
</#if>
<#assign fetchAnn = fieldAnnotations.generateFetchAnnotation(field)>
<#if fetchAnn?has_content>
    ${fetchAnn}
</#if>
<#assign bagAnn = fieldAnnotations.generateBagAnnotation(field)>
<#if bagAnn?has_content>
    ${bagAnn}
</#if>
<#assign collIdAnn = fieldAnnotations.generateCollectionIdAnnotation(field)>
<#if collIdAnn?has_content>
    ${collIdAnn}
</#if>
<#assign mapKeyAnn = fieldAnnotations.generateMapKeyAnnotation(field)>
<#if mapKeyAnn?has_content>
    ${mapKeyAnn}
</#if>
<#assign mapKeyColAnn = fieldAnnotations.generateMapKeyColumnAnnotation(field)>
<#if mapKeyColAnn?has_content>
    ${mapKeyColAnn}
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
<#assign sortAnn = fieldAnnotations.generateSortAnnotation(field)>
<#if sortAnn?has_content>
    ${sortAnn}
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
<#-- ManyToMany accessors -->
<#list templateHelper.getManyToManyFields() as field>
<#assign ann = relAnnotations.generateManyToManyAnnotation(field)>
<#if ann?has_content>
    ${ann}
</#if>
<#assign fetchAnn = fieldAnnotations.generateFetchAnnotation(field)>
<#if fetchAnn?has_content>
    ${fetchAnn}
</#if>
<#assign bagAnn = fieldAnnotations.generateBagAnnotation(field)>
<#if bagAnn?has_content>
    ${bagAnn}
</#if>
<#assign collIdAnn = fieldAnnotations.generateCollectionIdAnnotation(field)>
<#if collIdAnn?has_content>
    ${collIdAnn}
</#if>
<#assign mapKeyAnn = fieldAnnotations.generateMapKeyAnnotation(field)>
<#if mapKeyAnn?has_content>
    ${mapKeyAnn}
</#if>
<#assign mapKeyColAnn = fieldAnnotations.generateMapKeyColumnAnnotation(field)>
<#if mapKeyColAnn?has_content>
    ${mapKeyColAnn}
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
<#assign sortAnn = fieldAnnotations.generateSortAnnotation(field)>
<#if sortAnn?has_content>
    ${sortAnn}
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
