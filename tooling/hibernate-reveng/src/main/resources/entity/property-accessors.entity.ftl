<#-- Composite ID accessor -->
<#if templateHelper.getCompositeIdField()??>
<#assign cid = templateHelper.getCompositeIdField()>
<#assign ann = templateHelper.generateEmbeddedIdAnnotation(cid)>
<#if ann?has_content>
    ${ann}
</#if>
    ${templateHelper.getPropertyGetModifiers(cid)} ${templateHelper.getJavaTypeName(cid)} ${templateHelper.getGetterName(cid.getName())}() {
        return this.${templateHelper.getFieldName(cid)};
    }

    ${templateHelper.getPropertySetModifiers(cid)} void ${templateHelper.getSetterName(cid.getName())}(${templateHelper.getJavaTypeName(cid)} ${templateHelper.getFieldName(cid)}) {
        this.${templateHelper.getFieldName(cid)} = ${templateHelper.getFieldName(cid)};
    }

</#if>
<#-- Basic field accessors (respect gen-property) -->
<#list templateHelper.getBasicFields() as field>
<#if templateHelper.isGenProperty(field)>
    <#assign idAnn = templateHelper.isPrimaryKey(field)?then(templateHelper.generateIdAnnotations(field), "")>
    <#if idAnn?has_content>
    ${idAnn}
    </#if>
    <#assign versionAnn = templateHelper.isVersion(field)?then(templateHelper.generateVersionAnnotation(), "")>
    <#if versionAnn?has_content>
    ${versionAnn}
    </#if>
    <#assign basicAnn = templateHelper.generateBasicAnnotation(field)>
    <#if basicAnn?has_content>
    ${basicAnn}
    </#if>
    <#assign temporalAnn = templateHelper.generateTemporalAnnotation(field)>
    <#if temporalAnn?has_content>
    ${temporalAnn}
    </#if>
    <#assign lobAnn = templateHelper.isLob(field)?then(templateHelper.generateLobAnnotation(), "")>
    <#if lobAnn?has_content>
    ${lobAnn}
    </#if>
    <#assign convertAnn = templateHelper.generateConvertAnnotation(field)>
    <#if convertAnn?has_content>
    ${convertAnn}
    </#if>
    <#assign naturalIdAnn = templateHelper.generateNaturalIdAnnotation(field)>
    <#if naturalIdAnn?has_content>
    ${naturalIdAnn}
    </#if>
    <#assign olAnn = templateHelper.generateOptimisticLockAnnotation(field)>
    <#if olAnn?has_content>
    ${olAnn}
    </#if>
    <#assign accessAnn = templateHelper.generateAccessAnnotation(field)>
    <#if accessAnn?has_content>
    ${accessAnn}
    </#if>
    <#assign formulaAnn = templateHelper.generateFormulaAnnotation(field)>
    <#if formulaAnn?has_content>
    ${formulaAnn}
    <#else>
    <#assign colAnn = templateHelper.generateColumnAnnotation(field)>
    <#if colAnn?has_content>
    ${colAnn}
    </#if>
    </#if>
    <#assign ctAnn = templateHelper.generateColumnTransformerAnnotation(field)>
    <#if ctAnn?has_content>
    ${ctAnn}
    </#if>
    ${templateHelper.getPropertyGetModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${templateHelper.getGetterName(field)}() {
        return this.${templateHelper.getFieldName(field)};
    }

    ${templateHelper.getPropertySetModifiers(field)} void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getJavaTypeName(field)} ${templateHelper.getFieldName(field)}) {
        this.${templateHelper.getFieldName(field)} = ${templateHelper.getFieldName(field)};
    }

</#if>
</#list>
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