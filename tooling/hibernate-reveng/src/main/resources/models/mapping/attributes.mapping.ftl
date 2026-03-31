        <attributes>
<#-- Embedded ID -->
<#if helper.getCompositeIdField()??>
<#assign cid = helper.getCompositeIdField()>
            <embedded-id name="${cid.getName()}">
<#list helper.getAttributeOverrides(cid) as ao>
                <attribute-override name="${ao.fieldName()}">
                    <column name="${ao.columnName()}"/>
                </attribute-override>
</#list>
            </embedded-id>
</#if>
<#-- ID fields (skip if composite ID) -->
<#if !helper.getCompositeIdField()??>
<#list helper.getIdFields() as field>
            <id name="${field.getName()}">
                <column name="${helper.getColumnName(field)}"<#if !helper.isNullable(field)> nullable="false"</#if><#if helper.isUnique(field)> unique="true"</#if>/>
<#if helper.getGenerationType(field)??>
                <generated-value strategy="${helper.getGenerationType(field)}"/>
</#if>
            </id>
</#list>
</#if>
<#-- Natural ID -->
<#if (helper.getNaturalIdFields()?size > 0)>
            <natural-id mutable="${helper.isNaturalIdMutable()?c}">
<#list helper.getNaturalIdFields() as field>
                <basic name="${field.getName()}">
                    <column name="${helper.getColumnName(field)}"<#if !helper.isNullable(field)> nullable="false"</#if><#if helper.isUnique(field)> unique="true"</#if>/>
                </basic>
</#list>
            </natural-id>
</#if>
<#-- Basic fields (non-PK, non-version, non-FK) -->
<#list helper.getBasicFields() as field>
            <basic name="${field.getName()}"<#if helper.isPropertyLazy(field)> fetch="LAZY"</#if><#if helper.isOptimisticLockExcluded(field)> optimistic-lock="false"</#if>>
<#if helper.getFormula(field)??>
                <formula>${helper.getFormula(field)}</formula>
<#else>
                <column name="${helper.getColumnName(field)}"<#if !helper.isNullable(field)> nullable="false"</#if><#if helper.isUnique(field)> unique="true"</#if><#if (helper.getLength(field) > 0)> length="${helper.getLength(field)?c}"</#if><#if (helper.getPrecision(field) > 0)> precision="${helper.getPrecision(field)?c}"</#if><#if (helper.getScale(field) > 0)> scale="${helper.getScale(field)?c}"</#if><#if helper.getColumnTable(field)??> table="${helper.getColumnTable(field)}"</#if>/>
</#if>
<#if helper.isLob(field)>
                <lob/>
</#if>
<#if helper.getTemporalType(field)??>
                <temporal>${helper.getTemporalType(field)}</temporal>
</#if>
            </basic>
</#list>
<#-- Version fields -->
<#list helper.getVersionFields() as field>
            <version name="${field.getName()}">
                <column name="${helper.getColumnName(field)}"<#if !helper.isNullable(field)> nullable="false"</#if>/>
            </version>
</#list>
<#-- Many-to-one -->
<#list helper.getManyToOneFields() as field>
            <many-to-one name="${field.getName()}" target-entity="${helper.getTargetEntityName(field)}"<#if helper.getManyToOneFetchType(field)??> fetch="${helper.getManyToOneFetchType(field)}"</#if><#if !helper.isManyToOneOptional(field)> optional="false"</#if>>
                <join-column name="${helper.getJoinColumnName(field)}"<#if helper.getReferencedColumnName(field)??> referenced-column-name="${helper.getReferencedColumnName(field)}"</#if>/>
            </many-to-one>
</#list>
<#-- One-to-many -->
<#list helper.getOneToManyFields() as field>
<#assign o2mHasCascade = (helper.getOneToManyCascadeTypes(field)?size > 0)>
<#assign o2mHasOrderBy = helper.getOrderBy(field)??>
<#assign o2mHasOrderCol = helper.getOrderColumnName(field)??>
<#assign o2mHasFilters = (helper.getCollectionFilters(field)?size > 0)>
<#assign o2mHasMapKey = helper.getMapKeyName(field)?? || helper.getMapKeyColumnName(field)??>
<#assign o2mHasChildren = o2mHasCascade || o2mHasOrderBy || o2mHasOrderCol || o2mHasFilters || o2mHasMapKey>
            <one-to-many name="${field.getName()}" target-entity="${helper.getOneToManyTargetEntity(field)}" mapped-by="${helper.getOneToManyMappedBy(field)}"<#if helper.getOneToManyFetchType(field)??> fetch="${helper.getOneToManyFetchType(field)}"</#if><#if helper.isOneToManyOrphanRemoval(field)> orphan-removal="true"</#if><#if o2mHasChildren>>
<#if helper.getMapKeyName(field)??>
                <map-key name="${helper.getMapKeyName(field)}"/>
</#if>
<#if helper.getMapKeyColumnName(field)??>
                <map-key-column name="${helper.getMapKeyColumnName(field)}"/>
</#if>
<#if o2mHasOrderBy>
                <order-by>${helper.getOrderBy(field)}</order-by>
</#if>
<#if o2mHasOrderCol>
                <order-column name="${helper.getOrderColumnName(field)}"/>
</#if>
<#if o2mHasCascade>
<#assign cascadeTypes = helper.getOneToManyCascadeTypes(field)>
<#include "cascade.mapping.ftl"/>
</#if>
<#list helper.getCollectionFilters(field) as fi>
                <filter name="${fi.name()}"<#if fi.condition()?has_content> condition="${fi.condition()}"</#if>/>
</#list>
            </one-to-many>
<#else/>/>
</#if>
</#list>
<#-- One-to-one -->
<#list helper.getOneToOneFields() as field>
<#assign o2oHasCascade = (helper.getOneToOneCascadeTypes(field)?size > 0)>
<#assign o2oHasChildren = helper.getJoinColumnName(field)?? || o2oHasCascade>
            <one-to-one name="${field.getName()}" target-entity="${helper.getTargetEntityName(field)}"<#if helper.getOneToOneMappedBy(field)??> mapped-by="${helper.getOneToOneMappedBy(field)}"</#if><#if helper.getOneToOneFetchType(field)??> fetch="${helper.getOneToOneFetchType(field)}"</#if><#if !helper.isOneToOneOptional(field)> optional="false"</#if><#if helper.isOneToOneOrphanRemoval(field)> orphan-removal="true"</#if><#if o2oHasChildren>>
<#if o2oHasCascade>
<#assign cascadeTypes = helper.getOneToOneCascadeTypes(field)>
<#include "cascade.mapping.ftl"/>
</#if>
<#if helper.getJoinColumnName(field)??>
                <join-column name="${helper.getJoinColumnName(field)}"<#if helper.getReferencedColumnName(field)??> referenced-column-name="${helper.getReferencedColumnName(field)}"</#if>/>
</#if>
            </one-to-one>
<#else/>/>
</#if>
</#list>
<#-- Many-to-many -->
<#list helper.getManyToManyFields() as field>
<#assign m2mHasCascade = (helper.getManyToManyCascadeTypes(field)?size > 0)>
<#assign m2mHasOrderBy = helper.getOrderBy(field)??>
<#assign m2mHasOrderCol = helper.getOrderColumnName(field)??>
<#assign m2mHasFilters = (helper.getCollectionFilters(field)?size > 0)>
<#assign m2mHasMapKey = helper.getMapKeyName(field)?? || helper.getMapKeyColumnName(field)??>
<#assign m2mHasChildren = helper.getJoinTableName(field)?? || m2mHasCascade || m2mHasOrderBy || m2mHasOrderCol || m2mHasFilters || m2mHasMapKey>
            <many-to-many name="${field.getName()}" target-entity="${helper.getManyToManyTargetEntity(field)}"<#if helper.getManyToManyMappedBy(field)??> mapped-by="${helper.getManyToManyMappedBy(field)}"</#if><#if helper.getManyToManyFetchType(field)??> fetch="${helper.getManyToManyFetchType(field)}"</#if><#if m2mHasChildren>>
<#if helper.getMapKeyName(field)??>
                <map-key name="${helper.getMapKeyName(field)}"/>
</#if>
<#if helper.getMapKeyColumnName(field)??>
                <map-key-column name="${helper.getMapKeyColumnName(field)}"/>
</#if>
<#if m2mHasOrderBy>
                <order-by>${helper.getOrderBy(field)}</order-by>
</#if>
<#if m2mHasOrderCol>
                <order-column name="${helper.getOrderColumnName(field)}"/>
</#if>
<#if m2mHasCascade>
<#assign cascadeTypes = helper.getManyToManyCascadeTypes(field)>
<#include "cascade.mapping.ftl"/>
</#if>
<#if helper.getJoinTableName(field)??>
                <join-table name="${helper.getJoinTableName(field)}">
<#if helper.getJoinTableJoinColumnName(field)??>
                    <join-column name="${helper.getJoinTableJoinColumnName(field)}"/>
</#if>
<#if helper.getJoinTableInverseJoinColumnName(field)??>
                    <inverse-join-column name="${helper.getJoinTableInverseJoinColumnName(field)}"/>
</#if>
                </join-table>
</#if>
<#list helper.getCollectionFilters(field) as fi>
                <filter name="${fi.name()}"<#if fi.condition()?has_content> condition="${fi.condition()}"</#if>/>
</#list>
            </many-to-many>
<#else/>/>
</#if>
</#list>
<#-- Element collections -->
<#list helper.getElementCollectionFields() as field>
            <element-collection name="${field.getName()}"<#if helper.getElementCollectionTargetClass(field)??> target-class="${helper.getElementCollectionTargetClass(field)}"</#if>>
<#if helper.getOrderBy(field)??>
                <order-by>${helper.getOrderBy(field)}</order-by>
</#if>
<#if helper.getOrderColumnName(field)??>
                <order-column name="${helper.getOrderColumnName(field)}"/>
</#if>
<#if helper.getElementCollectionColumnName(field)??>
                <column name="${helper.getElementCollectionColumnName(field)}"/>
</#if>
<#if helper.getElementCollectionTableName(field)??>
                <collection-table name="${helper.getElementCollectionTableName(field)}">
<#if helper.getElementCollectionKeyColumnName(field)??>
                    <join-column name="${helper.getElementCollectionKeyColumnName(field)}"/>
</#if>
                </collection-table>
</#if>
            </element-collection>
</#list>
<#-- Embedded fields -->
<#list helper.getEmbeddedFields() as field>
            <embedded name="${field.getName()}">
<#list helper.getAttributeOverrides(field) as ao>
                <attribute-override name="${ao.fieldName()}">
                    <column name="${ao.columnName()}"/>
                </attribute-override>
</#list>
            </embedded>
</#list>
<#-- Any fields -->
<#list helper.getAnyFields() as field>
            <any name="${field.getName()}">
                <discriminator>
<#list helper.getAnyDiscriminatorMappings(field) as mapping>
                    <mapping value="${mapping.value()}">${mapping.entityClass()}</mapping>
</#list>
                </discriminator>
                <key>
                    <java-class>${helper.getAnyKeyType(field)}</java-class>
<#if helper.getJoinColumnName(field)??>
                    <column name="${helper.getJoinColumnName(field)}"/>
</#if>
                </key>
            </any>
</#list>
<#-- Many-to-any fields -->
<#list helper.getManyToAnyFields() as field>
            <many-to-any name="${field.getName()}">
                <discriminator>
<#list helper.getAnyDiscriminatorMappings(field) as mapping>
                    <mapping value="${mapping.value()}">${mapping.entityClass()}</mapping>
</#list>
                </discriminator>
                <key>
                    <java-class>${helper.getAnyKeyType(field)}</java-class>
                </key>
<#if helper.getJoinTableName(field)??>
                <join-table name="${helper.getJoinTableName(field)}">
<#if helper.getJoinTableJoinColumnName(field)??>
                    <join-column name="${helper.getJoinTableJoinColumnName(field)}"/>
</#if>
<#if helper.getJoinTableInverseJoinColumnName(field)??>
                    <inverse-join-column name="${helper.getJoinTableInverseJoinColumnName(field)}"/>
</#if>
                </join-table>
</#if>
            </many-to-any>
</#list>
        </attributes>
