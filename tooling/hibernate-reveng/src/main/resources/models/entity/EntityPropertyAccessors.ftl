<#-- Composite ID accessor -->
<#if pojo.getTable().getCompositeId()??>
<#assign cid = pojo.getTable().getCompositeId()>
    ${pojo.generateEmbeddedIdAnnotation(cid)}
    public ${pojo.getCompositeIdTypeName(cid)} ${pojo.getGetterName(cid.getFieldName())}() {
        return this.${cid.getFieldName()};
    }

    public void ${pojo.getSetterName(cid.getFieldName())}(${pojo.getCompositeIdTypeName(cid)} ${cid.getFieldName()}) {
        this.${cid.getFieldName()} = ${cid.getFieldName()};
    }

</#if>
<#-- Basic column accessors (skip FK columns, respect gen-property) -->
<#list pojo.getTable().getColumns() as col>
<#if !pojo.isForeignKeyColumn(col.getColumnName()) && pojo.isGenProperty(col)>
    <#if col.isPrimaryKey()>
    ${pojo.generateIdAnnotations(col)}
    </#if>
    <#if col.isVersion()>
    ${pojo.generateVersionAnnotation()}
    </#if>
    <#assign basicAnn = pojo.generateBasicAnnotation(col)>
    <#if basicAnn?has_content>
    ${basicAnn}
    </#if>
    <#assign temporalAnn = pojo.generateTemporalAnnotation(col)>
    <#if temporalAnn?has_content>
    ${temporalAnn}
    </#if>
    <#if col.isLob()>
    ${pojo.generateLobAnnotation()}
    </#if>
    ${pojo.generateColumnAnnotation(col)}
    public ${pojo.getJavaTypeName(col)} ${pojo.getGetterName(col.getFieldName())}() {
        return this.${col.getFieldName()};
    }

    public void ${pojo.getSetterName(col.getFieldName())}(${pojo.getJavaTypeName(col)} ${col.getFieldName()}) {
        this.${col.getFieldName()} = ${col.getFieldName()};
    }

</#if>
</#list>
<#-- ManyToOne accessors -->
<#list pojo.getTable().getForeignKeys() as fk>
    ${pojo.generateManyToOneAnnotation(fk)}
    public ${pojo.getFieldTypeName(fk)} ${pojo.getGetterName(fk.getFieldName())}() {
        return this.${fk.getFieldName()};
    }

    public void ${pojo.getSetterName(fk.getFieldName())}(${pojo.getFieldTypeName(fk)} ${fk.getFieldName()}) {
        this.${fk.getFieldName()} = ${fk.getFieldName()};
    }

</#list>
<#-- OneToOne accessors -->
<#list pojo.getTable().getOneToOnes() as o2o>
    ${pojo.generateOneToOneAnnotation(o2o)}
    public ${pojo.getFieldTypeName(o2o)} ${pojo.getGetterName(o2o.getFieldName())}() {
        return this.${o2o.getFieldName()};
    }

    public void ${pojo.getSetterName(o2o.getFieldName())}(${pojo.getFieldTypeName(o2o)} ${o2o.getFieldName()}) {
        this.${o2o.getFieldName()} = ${o2o.getFieldName()};
    }

</#list>
<#-- OneToMany accessors -->
<#list pojo.getTable().getOneToManys() as o2m>
    ${pojo.generateOneToManyAnnotation(o2m)}
    public ${pojo.getCollectionTypeName(o2m)} ${pojo.getGetterName(o2m.getFieldName())}() {
        return this.${o2m.getFieldName()};
    }

    public void ${pojo.getSetterName(o2m.getFieldName())}(${pojo.getCollectionTypeName(o2m)} ${o2m.getFieldName()}) {
        this.${o2m.getFieldName()} = ${o2m.getFieldName()};
    }

</#list>
<#-- ManyToMany accessors -->
<#list pojo.getTable().getManyToManys() as m2m>
    ${pojo.generateManyToManyAnnotation(m2m)}
    public ${pojo.getCollectionTypeName(m2m)} ${pojo.getGetterName(m2m.getFieldName())}() {
        return this.${m2m.getFieldName()};
    }

    public void ${pojo.getSetterName(m2m.getFieldName())}(${pojo.getCollectionTypeName(m2m)} ${m2m.getFieldName()}) {
        this.${m2m.getFieldName()} = ${m2m.getFieldName()};
    }

</#list>
<#-- Embedded field accessors -->
<#list pojo.getTable().getEmbeddedFields() as emb>
    ${pojo.generateEmbeddedAnnotation(emb)}
    public ${pojo.getEmbeddedTypeName(emb)} ${pojo.getGetterName(emb.getFieldName())}() {
        return this.${emb.getFieldName()};
    }

    public void ${pojo.getSetterName(emb.getFieldName())}(${pojo.getEmbeddedTypeName(emb)} ${emb.getFieldName()}) {
        this.${emb.getFieldName()} = ${emb.getFieldName()};
    }

</#list>