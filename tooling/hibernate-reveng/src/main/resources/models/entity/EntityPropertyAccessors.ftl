<#-- Composite ID accessor -->
<#if templateHelper.getTable().getCompositeId()??>
<#assign cid = templateHelper.getTable().getCompositeId()>
<#assign ann = templateHelper.generateEmbeddedIdAnnotation(cid)>
<#if ann?has_content>
    ${ann}
</#if>
    public ${templateHelper.getCompositeIdTypeName(cid)} ${templateHelper.getGetterName(cid.getFieldName())}() {
        return this.${cid.getFieldName()};
    }

    public void ${templateHelper.getSetterName(cid.getFieldName())}(${templateHelper.getCompositeIdTypeName(cid)} ${cid.getFieldName()}) {
        this.${cid.getFieldName()} = ${cid.getFieldName()};
    }

</#if>
<#-- Basic column accessors (skip FK columns, respect gen-property) -->
<#list templateHelper.getTable().getColumns() as col>
<#if !templateHelper.isForeignKeyColumn(col.getColumnName()) && templateHelper.isGenProperty(col)>
    <#assign idAnn = col.isPrimaryKey()?then(templateHelper.generateIdAnnotations(col), "")>
    <#if idAnn?has_content>
    ${idAnn}
    </#if>
    <#assign versionAnn = col.isVersion()?then(templateHelper.generateVersionAnnotation(), "")>
    <#if versionAnn?has_content>
    ${versionAnn}
    </#if>
    <#assign basicAnn = templateHelper.generateBasicAnnotation(col)>
    <#if basicAnn?has_content>
    ${basicAnn}
    </#if>
    <#assign temporalAnn = templateHelper.generateTemporalAnnotation(col)>
    <#if temporalAnn?has_content>
    ${temporalAnn}
    </#if>
    <#assign lobAnn = col.isLob()?then(templateHelper.generateLobAnnotation(), "")>
    <#if lobAnn?has_content>
    ${lobAnn}
    </#if>
    <#assign colAnn = templateHelper.generateColumnAnnotation(col)>
    <#if colAnn?has_content>
    ${colAnn}
    </#if>
    public ${templateHelper.getJavaTypeName(col)} ${templateHelper.getGetterName(col.getFieldName())}() {
        return this.${col.getFieldName()};
    }

    public void ${templateHelper.getSetterName(col.getFieldName())}(${templateHelper.getJavaTypeName(col)} ${col.getFieldName()}) {
        this.${col.getFieldName()} = ${col.getFieldName()};
    }

</#if>
</#list>
<#-- ManyToOne accessors -->
<#list templateHelper.getTable().getForeignKeys() as fk>
<#assign ann = templateHelper.generateManyToOneAnnotation(fk)>
<#if ann?has_content>
    ${ann}
</#if>
    public ${templateHelper.getFieldTypeName(fk)} ${templateHelper.getGetterName(fk.getFieldName())}() {
        return this.${fk.getFieldName()};
    }

    public void ${templateHelper.getSetterName(fk.getFieldName())}(${templateHelper.getFieldTypeName(fk)} ${fk.getFieldName()}) {
        this.${fk.getFieldName()} = ${fk.getFieldName()};
    }

</#list>
<#-- OneToOne accessors -->
<#list templateHelper.getTable().getOneToOnes() as o2o>
<#assign ann = templateHelper.generateOneToOneAnnotation(o2o)>
<#if ann?has_content>
    ${ann}
</#if>
    public ${templateHelper.getFieldTypeName(o2o)} ${templateHelper.getGetterName(o2o.getFieldName())}() {
        return this.${o2o.getFieldName()};
    }

    public void ${templateHelper.getSetterName(o2o.getFieldName())}(${templateHelper.getFieldTypeName(o2o)} ${o2o.getFieldName()}) {
        this.${o2o.getFieldName()} = ${o2o.getFieldName()};
    }

</#list>
<#-- OneToMany accessors -->
<#list templateHelper.getTable().getOneToManys() as o2m>
<#assign ann = templateHelper.generateOneToManyAnnotation(o2m)>
<#if ann?has_content>
    ${ann}
</#if>
    public ${templateHelper.getCollectionTypeName(o2m)} ${templateHelper.getGetterName(o2m.getFieldName())}() {
        return this.${o2m.getFieldName()};
    }

    public void ${templateHelper.getSetterName(o2m.getFieldName())}(${templateHelper.getCollectionTypeName(o2m)} ${o2m.getFieldName()}) {
        this.${o2m.getFieldName()} = ${o2m.getFieldName()};
    }

</#list>
<#-- ManyToMany accessors -->
<#list templateHelper.getTable().getManyToManys() as m2m>
<#assign ann = templateHelper.generateManyToManyAnnotation(m2m)>
<#if ann?has_content>
    ${ann}
</#if>
    public ${templateHelper.getCollectionTypeName(m2m)} ${templateHelper.getGetterName(m2m.getFieldName())}() {
        return this.${m2m.getFieldName()};
    }

    public void ${templateHelper.getSetterName(m2m.getFieldName())}(${templateHelper.getCollectionTypeName(m2m)} ${m2m.getFieldName()}) {
        this.${m2m.getFieldName()} = ${m2m.getFieldName()};
    }

</#list>
<#-- Embedded field accessors -->
<#list templateHelper.getTable().getEmbeddedFields() as emb>
<#assign ann = templateHelper.generateEmbeddedAnnotation(emb)>
<#if ann?has_content>
    ${ann}
</#if>
    public ${templateHelper.getEmbeddedTypeName(emb)} ${templateHelper.getGetterName(emb.getFieldName())}() {
        return this.${emb.getFieldName()};
    }

    public void ${templateHelper.getSetterName(emb.getFieldName())}(${templateHelper.getEmbeddedTypeName(emb)} ${emb.getFieldName()}) {
        this.${emb.getFieldName()} = ${emb.getFieldName()};
    }

</#list>