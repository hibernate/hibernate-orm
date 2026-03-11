<#-- Composite ID field -->
<#if templateHelper.getTable().getCompositeId()??>
<#assign cid = templateHelper.getTable().getCompositeId()>
    private ${templateHelper.getCompositeIdTypeName(cid)} ${cid.getFieldName()};

</#if>
<#-- Basic columns (skip FK columns, respect gen-property) -->
<#list templateHelper.getTable().getColumns() as col>
<#if !templateHelper.isForeignKeyColumn(col.getColumnName()) && templateHelper.isGenProperty(col)>
<#if templateHelper.hasFieldDescription(col)>
    /**
     * ${templateHelper.getFieldDescription(col)}
     */
</#if>
    private ${templateHelper.getJavaTypeName(col)} ${col.getFieldName()};

</#if>
</#list>
<#-- ManyToOne fields -->
<#list templateHelper.getTable().getForeignKeys() as fk>
    private ${templateHelper.getFieldTypeName(fk)} ${fk.getFieldName()};

</#list>
<#-- OneToOne fields -->
<#list templateHelper.getTable().getOneToOnes() as o2o>
    private ${templateHelper.getFieldTypeName(o2o)} ${o2o.getFieldName()};

</#list>
<#-- OneToMany fields -->
<#list templateHelper.getTable().getOneToManys() as o2m>
    private ${templateHelper.getCollectionTypeName(o2m)} ${o2m.getFieldName()} = new ${templateHelper.importType("java.util.HashSet")}<>(0);

</#list>
<#-- ManyToMany fields -->
<#list templateHelper.getTable().getManyToManys() as m2m>
    private ${templateHelper.getCollectionTypeName(m2m)} ${m2m.getFieldName()} = new ${templateHelper.importType("java.util.HashSet")}<>(0);

</#list>
<#-- Embedded fields -->
<#list templateHelper.getTable().getEmbeddedFields() as emb>
    private ${templateHelper.getEmbeddedTypeName(emb)} ${emb.getFieldName()};

</#list>