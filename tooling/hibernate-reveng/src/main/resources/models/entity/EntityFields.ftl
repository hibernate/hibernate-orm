<#-- Composite ID field -->
<#if pojo.getTable().getCompositeId()??>
<#assign cid = pojo.getTable().getCompositeId()>
    private ${pojo.getCompositeIdTypeName(cid)} ${cid.getFieldName()};

</#if>
<#-- Basic columns (skip FK columns) -->
<#list pojo.getTable().getColumns() as col>
<#if !pojo.isForeignKeyColumn(col.getColumnName())>
    private ${pojo.getJavaTypeName(col)} ${col.getFieldName()};

</#if>
</#list>
<#-- ManyToOne fields -->
<#list pojo.getTable().getForeignKeys() as fk>
    private ${pojo.getFieldTypeName(fk)} ${fk.getFieldName()};

</#list>
<#-- OneToOne fields -->
<#list pojo.getTable().getOneToOnes() as o2o>
    private ${pojo.getFieldTypeName(o2o)} ${o2o.getFieldName()};

</#list>
<#-- OneToMany fields -->
<#list pojo.getTable().getOneToManys() as o2m>
    private ${pojo.getCollectionTypeName(o2m)} ${o2m.getFieldName()} = new ${pojo.importType("java.util.HashSet")}<>(0);

</#list>
<#-- ManyToMany fields -->
<#list pojo.getTable().getManyToManys() as m2m>
    private ${pojo.getCollectionTypeName(m2m)} ${m2m.getFieldName()} = new ${pojo.importType("java.util.HashSet")}<>(0);

</#list>
<#-- Embedded fields -->
<#list pojo.getTable().getEmbeddedFields() as emb>
    private ${pojo.getEmbeddedTypeName(emb)} ${emb.getFieldName()};

</#list>