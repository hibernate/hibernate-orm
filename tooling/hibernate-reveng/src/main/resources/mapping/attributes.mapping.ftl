        <attributes>
<#-- Embedded ID -->
<#if fieldInfo.getCompositeIdField()??>
<#assign cid = fieldInfo.getCompositeIdField()>
            <embedded-id name="${cid.getName()}">
<#list fieldInfo.getAttributeOverrides(cid) as ao>
                <attribute-override name="${ao.fieldName()}">
                    <column name="${ao.columnName()}"/>
                </attribute-override>
</#list>
            </embedded-id>
</#if>
<#-- ID fields (skip if composite ID) -->
<#if !fieldInfo.getCompositeIdField()??>
<#list fieldInfo.getIdFields() as field>
            <id name="${field.getName()}">
                <column name="${fieldInfo.getColumnName(field)}"<#if !fieldInfo.isNullable(field)> nullable="false"</#if><#if fieldInfo.isUnique(field)> unique="true"</#if>/>
<#if fieldInfo.getGenerationType(field)??>
                <generated-value strategy="${fieldInfo.getGenerationType(field)}"<#if fieldInfo.getGeneratorName(field)??> generator="${fieldInfo.getGeneratorName(field)}"</#if>/>
</#if>
<#if fieldInfo.getSequenceGenerator(field)??>
<#assign sg = fieldInfo.getSequenceGenerator(field)>
                <sequence-generator name="${sg.name()}"<#if sg.sequenceName()??> sequence-name="${sg.sequenceName()}"</#if><#if sg.allocationSize()??> allocation-size="${sg.allocationSize()?c}"</#if><#if sg.initialValue()??> initial-value="${sg.initialValue()?c}"</#if>/>
</#if>
<#if fieldInfo.getTableGenerator(field)??>
<#assign tg = fieldInfo.getTableGenerator(field)>
                <table-generator name="${tg.name()}"<#if tg.table()??> table="${tg.table()}"</#if><#if tg.pkColumnName()??> pk-column-name="${tg.pkColumnName()}"</#if><#if tg.valueColumnName()??> value-column-name="${tg.valueColumnName()}"</#if><#if tg.pkColumnValue()??> pk-column-value="${tg.pkColumnValue()}"</#if><#if tg.allocationSize()??> allocation-size="${tg.allocationSize()?c}"</#if><#if tg.initialValue()??> initial-value="${tg.initialValue()?c}"</#if>/>
</#if>
            </id>
</#list>
</#if>
<#-- Natural ID -->
<#if (fieldInfo.getNaturalIdFields()?size > 0)>
            <natural-id mutable="${fieldInfo.isNaturalIdMutable()?c}">
<#list fieldInfo.getNaturalIdFields() as field>
                <basic name="${field.getName()}">
                    <column name="${fieldInfo.getColumnName(field)}"<#if !fieldInfo.isNullable(field)> nullable="false"</#if><#if fieldInfo.isUnique(field)> unique="true"</#if>/>
                </basic>
</#list>
            </natural-id>
</#if>
<#-- Basic fields (non-PK, non-version, non-FK) -->
<#list fieldInfo.getBasicFields() as field>
            <basic name="${field.getName()}"<#if fieldInfo.getFieldAccessType(field)??> access="${fieldInfo.getFieldAccessType(field)}"</#if><#if fieldInfo.isPropertyLazy(field)> fetch="LAZY"</#if><#if fieldInfo.isOptimisticLockExcluded(field)> optimistic-lock="false"</#if>>
<#if fieldInfo.getFormula(field)??>
                <formula>${fieldInfo.getFormula(field)}</formula>
<#else>
                <column name="${fieldInfo.getColumnName(field)}"<#if !fieldInfo.isNullable(field)> nullable="false"</#if><#if fieldInfo.isUnique(field)> unique="true"</#if><#if !fieldInfo.isInsertable(field)> insertable="false"</#if><#if !fieldInfo.isUpdatable(field)> updatable="false"</#if><#if (fieldInfo.getLength(field) > 0)> length="${fieldInfo.getLength(field)?c}"</#if><#if (fieldInfo.getPrecision(field) > 0)> precision="${fieldInfo.getPrecision(field)?c}"</#if><#if (fieldInfo.getScale(field) > 0)> scale="${fieldInfo.getScale(field)?c}"</#if><#if fieldInfo.getColumnTable(field)??> table="${fieldInfo.getColumnTable(field)}"</#if><#if fieldInfo.getColumnDefinition(field)??> column-definition="${fieldInfo.getColumnDefinition(field)}"</#if>/>
</#if>
<#if fieldInfo.isLob(field)>
                <lob/>
</#if>
<#if fieldInfo.getTemporalType(field)??>
                <temporal>${fieldInfo.getTemporalType(field)}</temporal>
</#if>
<#if fieldInfo.getConverterClassName(field)??>
                <convert converter="${fieldInfo.getConverterClassName(field)}"/>
</#if>
            </basic>
</#list>
<#-- Version fields -->
<#list fieldInfo.getVersionFields() as field>
            <version name="${field.getName()}">
                <column name="${fieldInfo.getColumnName(field)}"<#if !fieldInfo.isNullable(field)> nullable="false"</#if>/>
            </version>
</#list>
<#-- Many-to-one -->
<#list fieldInfo.getManyToOneFields() as field>
            <many-to-one name="${field.getName()}" target-entity="${fieldInfo.getTargetEntityName(field)}"<#if fieldInfo.getFieldAccessType(field)??> access="${fieldInfo.getFieldAccessType(field)}"</#if><#if fieldInfo.getManyToOneFetchType(field)??> fetch="${fieldInfo.getManyToOneFetchType(field)}"</#if><#if fieldInfo.getFetchMode(field)??> fetch-mode="${fieldInfo.getFetchMode(field)}"</#if><#if !fieldInfo.isManyToOneOptional(field)> optional="false"</#if><#if fieldInfo.getNotFoundAction(field)??> not-found="${fieldInfo.getNotFoundAction(field)}"</#if>>
<#if fieldInfo.getFormula(field)??>
                <formula>${fieldInfo.getFormula(field)}</formula>
<#else>
<#list fieldInfo.getJoinColumns(field) as jc>
                <join-column name="${jc.name()}"<#if jc.referencedColumnName()??> referenced-column-name="${jc.referencedColumnName()}"</#if>/>
</#list>
</#if>
            </many-to-one>
</#list>
<#-- One-to-many -->
<#list fieldInfo.getOneToManyFields() as field>
<#assign o2mHasCascade = (fieldInfo.getOneToManyCascadeTypes(field)?size > 0)>
<#assign o2mHasOrderBy = fieldInfo.getOrderBy(field)??>
<#assign o2mHasOrderCol = fieldInfo.getOrderColumnName(field)??>
<#assign o2mHasFilters = (queries.getCollectionFilters(field)?size > 0)>
<#assign o2mHasMapKey = fieldInfo.getMapKeyName(field)?? || fieldInfo.getMapKeyColumnName(field)?? || fieldInfo.getMapKeyJoinColumnName(field)??>
<#assign o2mHasSort = fieldInfo.isSortNatural(field) || fieldInfo.getSortComparatorClass(field)??>
<#assign o2mHasChildren = o2mHasCascade || o2mHasOrderBy || o2mHasOrderCol || o2mHasFilters || o2mHasMapKey || o2mHasSort>
            <one-to-many name="${field.getName()}" target-entity="${fieldInfo.getOneToManyTargetEntity(field)}" mapped-by="${fieldInfo.getOneToManyMappedBy(field)}"<#if fieldInfo.getFieldAccessType(field)??> access="${fieldInfo.getFieldAccessType(field)}"</#if><#if fieldInfo.getOneToManyFetchType(field)??> fetch="${fieldInfo.getOneToManyFetchType(field)}"</#if><#if fieldInfo.getFetchMode(field)??> fetch-mode="${fieldInfo.getFetchMode(field)}"</#if><#if fieldInfo.isOneToManyOrphanRemoval(field)> orphan-removal="true"</#if><#if o2mHasChildren>>
<#if fieldInfo.getMapKeyName(field)??>
                <map-key name="${fieldInfo.getMapKeyName(field)}"/>
</#if>
<#if fieldInfo.getMapKeyColumnName(field)??>
                <map-key-column name="${fieldInfo.getMapKeyColumnName(field)}"/>
</#if>
<#if fieldInfo.getMapKeyJoinColumnName(field)??>
                <map-key-join-column name="${fieldInfo.getMapKeyJoinColumnName(field)}"/>
</#if>
<#if o2mHasOrderBy>
                <order-by>${fieldInfo.getOrderBy(field)}</order-by>
</#if>
<#if o2mHasOrderCol>
                <order-column name="${fieldInfo.getOrderColumnName(field)}"/>
</#if>
<#if o2mHasCascade>
<#assign cascadeTypes = fieldInfo.getOneToManyCascadeTypes(field)>
<#include "cascade.mapping.ftl"/>
</#if>
<#list queries.getCollectionFilters(field) as fi>
                <filter name="${fi.name()}"<#if fi.condition()?has_content> condition="${fi.condition()}"</#if>/>
</#list>
<#if fieldInfo.isSortNatural(field)>
                <sort-natural/>
</#if>
<#if fieldInfo.getSortComparatorClass(field)??>
                <sort-comparator class="${fieldInfo.getSortComparatorClass(field)}"/>
</#if>
            </one-to-many>
<#else/>/>
</#if>
</#list>
<#-- One-to-one -->
<#list fieldInfo.getOneToOneFields() as field>
<#assign o2oHasCascade = (fieldInfo.getOneToOneCascadeTypes(field)?size > 0)>
<#assign o2oJoinCols = fieldInfo.getJoinColumns(field)>
<#assign o2oHasFormula = fieldInfo.getFormula(field)??>
<#assign o2oHasChildren = (o2oJoinCols?size > 0) || o2oHasCascade || o2oHasFormula>
            <one-to-one name="${field.getName()}" target-entity="${fieldInfo.getTargetEntityName(field)}"<#if fieldInfo.getFieldAccessType(field)??> access="${fieldInfo.getFieldAccessType(field)}"</#if><#if fieldInfo.getOneToOneMappedBy(field)??> mapped-by="${fieldInfo.getOneToOneMappedBy(field)}"</#if><#if fieldInfo.getOneToOneFetchType(field)??> fetch="${fieldInfo.getOneToOneFetchType(field)}"</#if><#if fieldInfo.getFetchMode(field)??> fetch-mode="${fieldInfo.getFetchMode(field)}"</#if><#if !fieldInfo.isOneToOneOptional(field)> optional="false"</#if><#if fieldInfo.isOneToOneOrphanRemoval(field)> orphan-removal="true"</#if><#if fieldInfo.getNotFoundAction(field)??> not-found="${fieldInfo.getNotFoundAction(field)}"</#if><#if o2oHasChildren>>
<#if o2oHasCascade>
<#assign cascadeTypes = fieldInfo.getOneToOneCascadeTypes(field)>
<#include "cascade.mapping.ftl"/>
</#if>
<#if o2oHasFormula>
                <formula>${fieldInfo.getFormula(field)}</formula>
<#else>
<#list o2oJoinCols as jc>
                <join-column name="${jc.name()}"<#if jc.referencedColumnName()??> referenced-column-name="${jc.referencedColumnName()}"</#if>/>
</#list>
</#if>
            </one-to-one>
<#else/>/>
</#if>
</#list>
<#-- Many-to-many -->
<#list fieldInfo.getManyToManyFields() as field>
<#assign m2mHasCascade = (fieldInfo.getManyToManyCascadeTypes(field)?size > 0)>
<#assign m2mHasOrderBy = fieldInfo.getOrderBy(field)??>
<#assign m2mHasOrderCol = fieldInfo.getOrderColumnName(field)??>
<#assign m2mHasFilters = (queries.getCollectionFilters(field)?size > 0)>
<#assign m2mHasMapKey = fieldInfo.getMapKeyName(field)?? || fieldInfo.getMapKeyColumnName(field)?? || fieldInfo.getMapKeyJoinColumnName(field)??>
<#assign m2mHasSort = fieldInfo.isSortNatural(field) || fieldInfo.getSortComparatorClass(field)??>
<#assign m2mHasChildren = fieldInfo.getJoinTableName(field)?? || m2mHasCascade || m2mHasOrderBy || m2mHasOrderCol || m2mHasFilters || m2mHasMapKey || m2mHasSort>
            <many-to-many name="${field.getName()}" target-entity="${fieldInfo.getManyToManyTargetEntity(field)}"<#if fieldInfo.getFieldAccessType(field)??> access="${fieldInfo.getFieldAccessType(field)}"</#if><#if fieldInfo.getManyToManyMappedBy(field)??> mapped-by="${fieldInfo.getManyToManyMappedBy(field)}"</#if><#if fieldInfo.getManyToManyFetchType(field)??> fetch="${fieldInfo.getManyToManyFetchType(field)}"</#if><#if fieldInfo.getFetchMode(field)??> fetch-mode="${fieldInfo.getFetchMode(field)}"</#if><#if m2mHasChildren>>
<#if fieldInfo.getMapKeyName(field)??>
                <map-key name="${fieldInfo.getMapKeyName(field)}"/>
</#if>
<#if fieldInfo.getMapKeyColumnName(field)??>
                <map-key-column name="${fieldInfo.getMapKeyColumnName(field)}"/>
</#if>
<#if fieldInfo.getMapKeyJoinColumnName(field)??>
                <map-key-join-column name="${fieldInfo.getMapKeyJoinColumnName(field)}"/>
</#if>
<#if m2mHasOrderBy>
                <order-by>${fieldInfo.getOrderBy(field)}</order-by>
</#if>
<#if m2mHasOrderCol>
                <order-column name="${fieldInfo.getOrderColumnName(field)}"/>
</#if>
<#if m2mHasCascade>
<#assign cascadeTypes = fieldInfo.getManyToManyCascadeTypes(field)>
<#include "cascade.mapping.ftl"/>
</#if>
<#if fieldInfo.getJoinTableName(field)??>
                <join-table name="${fieldInfo.getJoinTableName(field)}"<#if fieldInfo.getJoinTableSchema(field)??> schema="${fieldInfo.getJoinTableSchema(field)}"</#if><#if fieldInfo.getJoinTableCatalog(field)??> catalog="${fieldInfo.getJoinTableCatalog(field)}"</#if>>
<#list fieldInfo.getJoinTableJoinColumnNames(field) as colName>
                    <join-column name="${colName}"/>
</#list>
<#list fieldInfo.getJoinTableInverseJoinColumnNames(field) as colName>
                    <inverse-join-column name="${colName}"/>
</#list>
                </join-table>
</#if>
<#list queries.getCollectionFilters(field) as fi>
                <filter name="${fi.name()}"<#if fi.condition()?has_content> condition="${fi.condition()}"</#if>/>
</#list>
<#if fieldInfo.isSortNatural(field)>
                <sort-natural/>
</#if>
<#if fieldInfo.getSortComparatorClass(field)??>
                <sort-comparator class="${fieldInfo.getSortComparatorClass(field)}"/>
</#if>
            </many-to-many>
<#else/>/>
</#if>
</#list>
<#-- Element collections -->
<#list fieldInfo.getElementCollectionFields() as field>
            <element-collection name="${field.getName()}"<#if fieldInfo.getFieldAccessType(field)??> access="${fieldInfo.getFieldAccessType(field)}"</#if><#if fieldInfo.getElementCollectionTargetClass(field)??> target-class="${fieldInfo.getElementCollectionTargetClass(field)}"</#if>>
<#if fieldInfo.getOrderBy(field)??>
                <order-by>${fieldInfo.getOrderBy(field)}</order-by>
</#if>
<#if fieldInfo.getOrderColumnName(field)??>
                <order-column name="${fieldInfo.getOrderColumnName(field)}"/>
</#if>
<#if fieldInfo.getElementCollectionColumnName(field)??>
                <column name="${fieldInfo.getElementCollectionColumnName(field)}"/>
</#if>
<#if fieldInfo.getElementCollectionTableName(field)??>
                <collection-table name="${fieldInfo.getElementCollectionTableName(field)}">
<#if fieldInfo.getElementCollectionKeyColumnName(field)??>
                    <join-column name="${fieldInfo.getElementCollectionKeyColumnName(field)}"/>
</#if>
                </collection-table>
</#if>
            </element-collection>
</#list>
<#-- Embedded fields -->
<#list fieldInfo.getEmbeddedFields() as field>
            <embedded name="${field.getName()}"<#if fieldInfo.getFieldAccessType(field)??> access="${fieldInfo.getFieldAccessType(field)}"</#if>>
<#list fieldInfo.getAttributeOverrides(field) as ao>
                <attribute-override name="${ao.fieldName()}">
                    <column name="${ao.columnName()}"/>
                </attribute-override>
</#list>
            </embedded>
</#list>
<#-- Any fields -->
<#list fieldInfo.getAnyFields() as field>
            <any name="${field.getName()}"<#if fieldInfo.getFieldAccessType(field)??> access="${fieldInfo.getFieldAccessType(field)}"</#if>>
                <discriminator>
<#list fieldInfo.getAnyDiscriminatorMappings(field) as mapping>
                    <mapping value="${mapping.value()}">${mapping.entityClass()}</mapping>
</#list>
                </discriminator>
                <key>
                    <java-class>${fieldInfo.getAnyKeyType(field)}</java-class>
<#if fieldInfo.getJoinColumnName(field)??>
                    <column name="${fieldInfo.getJoinColumnName(field)}"/>
</#if>
                </key>
            </any>
</#list>
<#-- Many-to-any fields -->
<#list fieldInfo.getManyToAnyFields() as field>
            <many-to-any name="${field.getName()}"<#if fieldInfo.getFieldAccessType(field)??> access="${fieldInfo.getFieldAccessType(field)}"</#if>>
                <discriminator>
<#list fieldInfo.getAnyDiscriminatorMappings(field) as mapping>
                    <mapping value="${mapping.value()}">${mapping.entityClass()}</mapping>
</#list>
                </discriminator>
                <key>
                    <java-class>${fieldInfo.getAnyKeyType(field)}</java-class>
                </key>
<#if fieldInfo.getJoinTableName(field)??>
                <join-table name="${fieldInfo.getJoinTableName(field)}"<#if fieldInfo.getJoinTableSchema(field)??> schema="${fieldInfo.getJoinTableSchema(field)}"</#if><#if fieldInfo.getJoinTableCatalog(field)??> catalog="${fieldInfo.getJoinTableCatalog(field)}"</#if>>
<#if fieldInfo.getJoinTableJoinColumnName(field)??>
                    <join-column name="${fieldInfo.getJoinTableJoinColumnName(field)}"/>
</#if>
<#if fieldInfo.getJoinTableInverseJoinColumnName(field)??>
                    <inverse-join-column name="${fieldInfo.getJoinTableInverseJoinColumnName(field)}"/>
</#if>
                </join-table>
</#if>
            </many-to-any>
</#list>
        </attributes>
