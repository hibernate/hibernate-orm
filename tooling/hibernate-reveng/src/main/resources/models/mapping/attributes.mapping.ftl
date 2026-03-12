        <attributes>
<#-- Embedded ID -->
<#if table.getCompositeId()??>
<#assign cid = table.getCompositeId()>
            <embedded-id name="${cid.getFieldName()}">
<#list cid.getAttributeOverrides() as ao>
                <attribute-override name="${ao.getFieldName()}">
                    <column name="${ao.getColumnName()}"/>
                </attribute-override>
</#list>
            </embedded-id>
</#if>
<#-- ID columns (skip if composite ID) -->
<#if !table.getCompositeId()??>
<#list table.getColumns() as col>
<#if col.isPrimaryKey()>
            <id name="${col.getFieldName()}">
                <column name="${col.getColumnName()}"<#if !col.isNullable()> nullable="false"</#if><#if col.isUnique()> unique="true"</#if>/>
<#if col.getGenerationType()??>
                <generated-value strategy="${col.getGenerationType().name()}"/>
</#if>
            </id>
</#if>
</#list>
</#if>
<#-- Basic columns (skip PK, version, FK columns) -->
<#list table.getColumns() as col>
<#if !col.isPrimaryKey() && !col.isVersion() && !table.isForeignKeyColumn(col.getColumnName())>
            <basic name="${col.getFieldName()}">
                <column name="${col.getColumnName()}"<#if !col.isNullable()> nullable="false"</#if><#if col.isUnique()> unique="true"</#if><#if (col.getLength() > 0)> length="${col.getLength()?c}"</#if><#if (col.getPrecision() > 0)> precision="${col.getPrecision()?c}"</#if><#if (col.getScale() > 0)> scale="${col.getScale()?c}"</#if>/>
<#if col.isLob()>
                <lob/>
</#if>
<#if col.getTemporalType()??>
                <temporal>${col.getTemporalType().name()}</temporal>
</#if>
            </basic>
</#if>
</#list>
<#-- Version columns -->
<#list table.getColumns() as col>
<#if col.isVersion()>
            <version name="${col.getFieldName()}">
                <column name="${col.getColumnName()}"<#if !col.isNullable()> nullable="false"</#if>/>
            </version>
</#if>
</#list>
<#-- Many-to-one -->
<#list table.getForeignKeys() as fk>
            <many-to-one name="${fk.getFieldName()}" target-entity="${fk.getTargetEntityPackage()}.${fk.getTargetEntityClassName()}"<#if fk.getFetchType()??> fetch="${fk.getFetchType().name()}"</#if><#if !fk.isOptional()> optional="false"</#if>>
                <join-column name="${fk.getForeignKeyColumnName()}"<#if fk.getReferencedColumnName()??> referenced-column-name="${fk.getReferencedColumnName()}"</#if>/>
            </many-to-one>
</#list>
<#-- One-to-many -->
<#list table.getOneToManys() as o2m>
<#assign o2mHasCascade = o2m.getCascadeTypes()?? && (o2m.getCascadeTypes()?size > 0)>
            <one-to-many name="${o2m.getFieldName()}" target-entity="${o2m.getElementEntityPackage()}.${o2m.getElementEntityClassName()}" mapped-by="${o2m.getMappedBy()}"<#if o2m.getFetchType()??> fetch="${o2m.getFetchType().name()}"</#if><#if o2m.isOrphanRemoval()> orphan-removal="true"</#if><#if o2mHasCascade>>
<#assign cascadeTypes = o2m.getCascadeTypes()>
<#include "cascade.mapping.ftl"/>
            </one-to-many>
<#else/>/>
</#if>
</#list>
<#-- One-to-one -->
<#list table.getOneToOnes() as o2o>
<#assign o2oHasCascade = o2o.getCascadeTypes()?? && (o2o.getCascadeTypes()?size > 0)>
<#assign o2oHasChildren = o2o.getForeignKeyColumnName()?? || o2oHasCascade>
            <one-to-one name="${o2o.getFieldName()}" target-entity="${o2o.getTargetEntityPackage()}.${o2o.getTargetEntityClassName()}"<#if o2o.getMappedBy()??> mapped-by="${o2o.getMappedBy()}"</#if><#if o2o.getFetchType()??> fetch="${o2o.getFetchType().name()}"</#if><#if !o2o.isOptional()> optional="false"</#if><#if o2o.isOrphanRemoval()> orphan-removal="true"</#if><#if o2oHasChildren>>
<#if o2oHasCascade>
<#assign cascadeTypes = o2o.getCascadeTypes()>
<#include "cascade.mapping.ftl"/>
</#if>
<#if o2o.getForeignKeyColumnName()??>
                <join-column name="${o2o.getForeignKeyColumnName()}"<#if o2o.getReferencedColumnName()??> referenced-column-name="${o2o.getReferencedColumnName()}"</#if>/>
</#if>
            </one-to-one>
<#else/>/>
</#if>
</#list>
<#-- Many-to-many -->
<#list table.getManyToManys() as m2m>
<#assign m2mHasCascade = m2m.getCascadeTypes()?? && (m2m.getCascadeTypes()?size > 0)>
<#assign m2mHasChildren = m2m.getJoinTableName()?? || m2mHasCascade>
            <many-to-many name="${m2m.getFieldName()}" target-entity="${m2m.getTargetEntityPackage()}.${m2m.getTargetEntityClassName()}"<#if m2m.getMappedBy()??> mapped-by="${m2m.getMappedBy()}"</#if><#if m2m.getFetchType()??> fetch="${m2m.getFetchType().name()}"</#if><#if m2mHasChildren>>
<#if m2mHasCascade>
<#assign cascadeTypes = m2m.getCascadeTypes()>
<#include "cascade.mapping.ftl"/>
</#if>
<#if m2m.getJoinTableName()??>
                <join-table name="${m2m.getJoinTableName()}">
<#if m2m.getJoinColumnName()??>
                    <join-column name="${m2m.getJoinColumnName()}"/>
</#if>
<#if m2m.getInverseJoinColumnName()??>
                    <inverse-join-column name="${m2m.getInverseJoinColumnName()}"/>
</#if>
                </join-table>
</#if>
            </many-to-many>
<#else/>/>
</#if>
</#list>
<#-- Embedded fields -->
<#list table.getEmbeddedFields() as emb>
            <embedded name="${emb.getFieldName()}">
<#list emb.getAttributeOverrides() as ao>
                <attribute-override name="${ao.getFieldName()}">
                    <column name="${ao.getColumnName()}"/>
                </attribute-override>
</#list>
            </embedded>
</#list>
        </attributes>
