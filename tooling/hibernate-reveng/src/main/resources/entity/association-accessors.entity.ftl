<#-- ManyToOne accessors -->
<#list templateHelper.getManyToOneFields() as field>
<#assign ann = templateHelper.generateManyToOneAnnotation(field)>
<#if ann?has_content>
    ${ann}
</#if>
<#assign fetchAnn = templateHelper.generateFetchAnnotation(field)>
<#if fetchAnn?has_content>
    ${fetchAnn}
</#if>
<#assign notFoundAnn = templateHelper.generateNotFoundAnnotation(field)>
<#if notFoundAnn?has_content>
    ${notFoundAnn}
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
<#-- OneToOne accessors -->
<#list templateHelper.getOneToOneFields() as field>
<#assign ann = templateHelper.generateOneToOneAnnotation(field)>
<#if ann?has_content>
    ${ann}
</#if>
<#assign fetchAnn = templateHelper.generateFetchAnnotation(field)>
<#if fetchAnn?has_content>
    ${fetchAnn}
</#if>
<#assign notFoundAnn = templateHelper.generateNotFoundAnnotation(field)>
<#if notFoundAnn?has_content>
    ${notFoundAnn}
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
<#-- OneToMany accessors -->
<#list templateHelper.getOneToManyFields() as field>
<#assign ann = templateHelper.generateOneToManyAnnotation(field)>
<#if ann?has_content>
    ${ann}
</#if>
<#assign fetchAnn = templateHelper.generateFetchAnnotation(field)>
<#if fetchAnn?has_content>
    ${fetchAnn}
</#if>
<#assign bagAnn = templateHelper.generateBagAnnotation(field)>
<#if bagAnn?has_content>
    ${bagAnn}
</#if>
<#assign collIdAnn = templateHelper.generateCollectionIdAnnotation(field)>
<#if collIdAnn?has_content>
    ${collIdAnn}
</#if>
<#assign mapKeyAnn = templateHelper.generateMapKeyAnnotation(field)>
<#if mapKeyAnn?has_content>
    ${mapKeyAnn}
</#if>
<#assign mapKeyColAnn = templateHelper.generateMapKeyColumnAnnotation(field)>
<#if mapKeyColAnn?has_content>
    ${mapKeyColAnn}
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
<#assign sortAnn = templateHelper.generateSortAnnotation(field)>
<#if sortAnn?has_content>
    ${sortAnn}
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
<#-- ManyToMany accessors -->
<#list templateHelper.getManyToManyFields() as field>
<#assign ann = templateHelper.generateManyToManyAnnotation(field)>
<#if ann?has_content>
    ${ann}
</#if>
<#assign fetchAnn = templateHelper.generateFetchAnnotation(field)>
<#if fetchAnn?has_content>
    ${fetchAnn}
</#if>
<#assign bagAnn = templateHelper.generateBagAnnotation(field)>
<#if bagAnn?has_content>
    ${bagAnn}
</#if>
<#assign collIdAnn = templateHelper.generateCollectionIdAnnotation(field)>
<#if collIdAnn?has_content>
    ${collIdAnn}
</#if>
<#assign mapKeyAnn = templateHelper.generateMapKeyAnnotation(field)>
<#if mapKeyAnn?has_content>
    ${mapKeyAnn}
</#if>
<#assign mapKeyColAnn = templateHelper.generateMapKeyColumnAnnotation(field)>
<#if mapKeyColAnn?has_content>
    ${mapKeyColAnn}
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
<#assign sortAnn = templateHelper.generateSortAnnotation(field)>
<#if sortAnn?has_content>
    ${sortAnn}
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
