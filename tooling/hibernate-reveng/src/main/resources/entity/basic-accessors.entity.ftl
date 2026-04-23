<#-- Composite ID accessor -->
<#if templateHelper.getCompositeIdField()??>
<#assign cid = templateHelper.getCompositeIdField()>
<#assign ann = relAnnotations.generateEmbeddedIdAnnotation(cid)>
<#if ann?has_content>
    ${ann}
</#if>
    ${meta.getPropertyGetModifiers(cid)} ${templateHelper.getJavaTypeName(cid)} ${templateHelper.getGetterName(cid.getName())}() {
        return this.${templateHelper.getFieldName(cid)};
    }

    ${meta.getPropertySetModifiers(cid)} void ${templateHelper.getSetterName(cid.getName())}(${templateHelper.getJavaTypeName(cid)} ${templateHelper.getFieldName(cid)}) {
        this.${templateHelper.getFieldName(cid)} = ${templateHelper.getFieldName(cid)};
    }

</#if>
<#-- Basic field accessors (respect gen-property) -->
<#list templateHelper.getBasicFields() as field>
<#if meta.isGenProperty(field)>
    <#assign idAnn = templateHelper.isPrimaryKey(field)?then(fieldAnnotations.generateIdAnnotations(field), "")>
    <#if idAnn?has_content>
    ${idAnn}
    </#if>
    <#assign versionAnn = templateHelper.isVersion(field)?then(fieldAnnotations.generateVersionAnnotation(), "")>
    <#if versionAnn?has_content>
    ${versionAnn}
    </#if>
    <#assign basicAnn = fieldAnnotations.generateBasicAnnotation(field)>
    <#if basicAnn?has_content>
    ${basicAnn}
    </#if>
    <#assign temporalAnn = fieldAnnotations.generateTemporalAnnotation(field)>
    <#if temporalAnn?has_content>
    ${temporalAnn}
    </#if>
    <#assign lobAnn = templateHelper.isLob(field)?then(fieldAnnotations.generateLobAnnotation(), "")>
    <#if lobAnn?has_content>
    ${lobAnn}
    </#if>
    <#assign convertAnn = fieldAnnotations.generateConvertAnnotation(field)>
    <#if convertAnn?has_content>
    ${convertAnn}
    </#if>
    <#assign naturalIdAnn = fieldAnnotations.generateNaturalIdAnnotation(field)>
    <#if naturalIdAnn?has_content>
    ${naturalIdAnn}
    </#if>
    <#assign olAnn = fieldAnnotations.generateOptimisticLockAnnotation(field)>
    <#if olAnn?has_content>
    ${olAnn}
    </#if>
    <#assign accessAnn = fieldAnnotations.generateAccessAnnotation(field)>
    <#if accessAnn?has_content>
    ${accessAnn}
    </#if>
    <#assign formulaAnn = fieldAnnotations.generateFormulaAnnotation(field)>
    <#if formulaAnn?has_content>
    ${formulaAnn}
    <#else>
    <#assign colAnn = fieldAnnotations.generateColumnAnnotation(field)>
    <#if colAnn?has_content>
    ${colAnn}
    </#if>
    </#if>
    <#assign ctAnn = fieldAnnotations.generateColumnTransformerAnnotation(field)>
    <#if ctAnn?has_content>
    ${ctAnn}
    </#if>
    ${meta.getPropertyGetModifiers(field)} ${templateHelper.getJavaTypeName(field)} ${templateHelper.getGetterName(field)}() {
        return this.${templateHelper.getFieldName(field)};
    }

    ${meta.getPropertySetModifiers(field)} void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getJavaTypeName(field)} ${templateHelper.getFieldName(field)}) {
        this.${templateHelper.getFieldName(field)} = ${templateHelper.getFieldName(field)};
    }

</#if>
</#list>
