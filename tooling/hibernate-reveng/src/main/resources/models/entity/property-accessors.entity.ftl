<#-- Composite ID accessor -->
<#if templateHelper.getCompositeIdField()??>
<#assign cid = templateHelper.getCompositeIdField()>
<#assign ann = templateHelper.generateEmbeddedIdAnnotation(cid)>
<#if ann?has_content>
    ${ann}
</#if>
    public ${templateHelper.getJavaTypeName(cid)} ${templateHelper.getGetterName(cid.getName())}() {
        return this.${cid.getName()};
    }

    public void ${templateHelper.getSetterName(cid.getName())}(${templateHelper.getJavaTypeName(cid)} ${cid.getName()}) {
        this.${cid.getName()} = ${cid.getName()};
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
    <#assign colAnn = templateHelper.generateColumnAnnotation(field)>
    <#if colAnn?has_content>
    ${colAnn}
    </#if>
    public ${templateHelper.getJavaTypeName(field)} ${templateHelper.getGetterName(field.getName())}() {
        return this.${field.getName()};
    }

    public void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getJavaTypeName(field)} ${field.getName()}) {
        this.${field.getName()} = ${field.getName()};
    }

</#if>
</#list>
<#-- ManyToOne accessors -->
<#list templateHelper.getManyToOneFields() as field>
<#assign ann = templateHelper.generateManyToOneAnnotation(field)>
<#if ann?has_content>
    ${ann}
</#if>
    public ${templateHelper.getJavaTypeName(field)} ${templateHelper.getGetterName(field.getName())}() {
        return this.${field.getName()};
    }

    public void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getJavaTypeName(field)} ${field.getName()}) {
        this.${field.getName()} = ${field.getName()};
    }

</#list>
<#-- OneToOne accessors -->
<#list templateHelper.getOneToOneFields() as field>
<#assign ann = templateHelper.generateOneToOneAnnotation(field)>
<#if ann?has_content>
    ${ann}
</#if>
    public ${templateHelper.getJavaTypeName(field)} ${templateHelper.getGetterName(field.getName())}() {
        return this.${field.getName()};
    }

    public void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getJavaTypeName(field)} ${field.getName()}) {
        this.${field.getName()} = ${field.getName()};
    }

</#list>
<#-- OneToMany accessors -->
<#list templateHelper.getOneToManyFields() as field>
<#assign ann = templateHelper.generateOneToManyAnnotation(field)>
<#if ann?has_content>
    ${ann}
</#if>
    public ${templateHelper.getCollectionTypeName(field)} ${templateHelper.getGetterName(field.getName())}() {
        return this.${field.getName()};
    }

    public void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getCollectionTypeName(field)} ${field.getName()}) {
        this.${field.getName()} = ${field.getName()};
    }

</#list>
<#-- ManyToMany accessors -->
<#list templateHelper.getManyToManyFields() as field>
<#assign ann = templateHelper.generateManyToManyAnnotation(field)>
<#if ann?has_content>
    ${ann}
</#if>
    public ${templateHelper.getCollectionTypeName(field)} ${templateHelper.getGetterName(field.getName())}() {
        return this.${field.getName()};
    }

    public void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getCollectionTypeName(field)} ${field.getName()}) {
        this.${field.getName()} = ${field.getName()};
    }

</#list>
<#-- Embedded field accessors -->
<#list templateHelper.getEmbeddedFields() as field>
<#assign ann = templateHelper.generateEmbeddedAnnotation(field)>
<#if ann?has_content>
    ${ann}
</#if>
    public ${templateHelper.getJavaTypeName(field)} ${templateHelper.getGetterName(field.getName())}() {
        return this.${field.getName()};
    }

    public void ${templateHelper.getSetterName(field.getName())}(${templateHelper.getJavaTypeName(field)} ${field.getName()}) {
        this.${field.getName()} = ${field.getName()};
    }

</#list>