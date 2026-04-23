        <attributes>
<#-- Embedded ID -->
<#if fieldInfo.getCompositeIdField()??>
<#assign cid = fieldInfo.getCompositeIdField()>
            <embedded-id name="${cid.getName()}">
<#list assocInfo.getAttributeOverrides(cid) as ao>
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
            <basic name="${field.getName()}"<#if assocInfo.getFieldAccessType(field)??> access="${assocInfo.getFieldAccessType(field)}"</#if><#if assocInfo.isPropertyLazy(field)> fetch="LAZY"</#if><#if assocInfo.isOptimisticLockExcluded(field)> optimistic-lock="false"</#if>>
<#if assocInfo.getFormula(field)??>
                <formula>${assocInfo.getFormula(field)}</formula>
<#else>
                <column name="${fieldInfo.getColumnName(field)}"<#if !fieldInfo.isNullable(field)> nullable="false"</#if><#if fieldInfo.isUnique(field)> unique="true"</#if><#if !fieldInfo.isInsertable(field)> insertable="false"</#if><#if !fieldInfo.isUpdatable(field)> updatable="false"</#if><#if (fieldInfo.getLength(field) > 0)> length="${fieldInfo.getLength(field)?c}"</#if><#if (fieldInfo.getPrecision(field) > 0)> precision="${fieldInfo.getPrecision(field)?c}"</#if><#if (fieldInfo.getScale(field) > 0)> scale="${fieldInfo.getScale(field)?c}"</#if><#if assocInfo.getColumnTable(field)??> table="${assocInfo.getColumnTable(field)}"</#if><#if fieldInfo.getColumnDefinition(field)??> column-definition="${fieldInfo.getColumnDefinition(field)}"</#if>/>
</#if>
<#if fieldInfo.isLob(field)>
                <lob/>
</#if>
<#if fieldInfo.getTemporalType(field)??>
                <temporal>${fieldInfo.getTemporalType(field)}</temporal>
</#if>
<#if assocInfo.getConverterClassName(field)??>
                <convert converter="${assocInfo.getConverterClassName(field)}"/>
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
            <many-to-one name="${field.getName()}" target-entity="${assocInfo.getTargetEntityName(field)}"<#if assocInfo.getFieldAccessType(field)??> access="${assocInfo.getFieldAccessType(field)}"</#if><#if assocInfo.getManyToOneFetchType(field)??> fetch="${assocInfo.getManyToOneFetchType(field)}"</#if><#if assocInfo.getFetchMode(field)??> fetch-mode="${assocInfo.getFetchMode(field)}"</#if><#if !assocInfo.isManyToOneOptional(field)> optional="false"</#if><#if assocInfo.getNotFoundAction(field)??> not-found="${assocInfo.getNotFoundAction(field)}"</#if>>
<#if assocInfo.getFormula(field)??>
                <formula>${assocInfo.getFormula(field)}</formula>
<#else>
<#list assocInfo.getJoinColumns(field) as jc>
                <join-column name="${jc.name()}"<#if jc.referencedColumnName()??> referenced-column-name="${jc.referencedColumnName()}"</#if>/>
</#list>
</#if>
            </many-to-one>
</#list>
<#-- One-to-many -->
<#list fieldInfo.getOneToManyFields() as field>
<#assign o2mHasCascade = (assocInfo.getOneToManyCascadeTypes(field)?size > 0)>
<#assign o2mHasOrderBy = assocInfo.getOrderBy(field)??>
<#assign o2mHasOrderCol = assocInfo.getOrderColumnName(field)??>
<#assign o2mHasFilters = (queries.getCollectionFilters(field)?size > 0)>
<#assign o2mHasMapKey = assocInfo.getMapKeyName(field)?? || assocInfo.getMapKeyColumnName(field)?? || assocInfo.getMapKeyJoinColumnName(field)??>
<#assign o2mHasSort = assocInfo.isSortNatural(field) || assocInfo.getSortComparatorClass(field)??>
<#assign o2mHasChildren = o2mHasCascade || o2mHasOrderBy || o2mHasOrderCol || o2mHasFilters || o2mHasMapKey || o2mHasSort>
            <one-to-many name="${field.getName()}" target-entity="${assocInfo.getOneToManyTargetEntity(field)}" mapped-by="${assocInfo.getOneToManyMappedBy(field)}"<#if assocInfo.getFieldAccessType(field)??> access="${assocInfo.getFieldAccessType(field)}"</#if><#if assocInfo.getOneToManyFetchType(field)??> fetch="${assocInfo.getOneToManyFetchType(field)}"</#if><#if assocInfo.getFetchMode(field)??> fetch-mode="${assocInfo.getFetchMode(field)}"</#if><#if assocInfo.isOneToManyOrphanRemoval(field)> orphan-removal="true"</#if><#if o2mHasChildren>>
<#if assocInfo.getMapKeyName(field)??>
                <map-key name="${assocInfo.getMapKeyName(field)}"/>
</#if>
<#if assocInfo.getMapKeyColumnName(field)??>
                <map-key-column name="${assocInfo.getMapKeyColumnName(field)}"/>
</#if>
<#if assocInfo.getMapKeyJoinColumnName(field)??>
                <map-key-join-column name="${assocInfo.getMapKeyJoinColumnName(field)}"/>
</#if>
<#if o2mHasOrderBy>
                <order-by>${assocInfo.getOrderBy(field)}</order-by>
</#if>
<#if o2mHasOrderCol>
                <order-column name="${assocInfo.getOrderColumnName(field)}"/>
</#if>
<#if o2mHasCascade>
<#assign cascadeTypes = assocInfo.getOneToManyCascadeTypes(field)>
<#include "cascade.mapping.ftl"/>
</#if>
<#list queries.getCollectionFilters(field) as fi>
                <filter name="${fi.name()}"<#if fi.condition()?has_content> condition="${fi.condition()}"</#if>/>
</#list>
<#if assocInfo.isSortNatural(field)>
                <sort-natural/>
</#if>
<#if assocInfo.getSortComparatorClass(field)??>
                <sort-comparator class="${assocInfo.getSortComparatorClass(field)}"/>
</#if>
            </one-to-many>
<#else/>/>
</#if>
</#list>
<#-- One-to-one -->
<#list fieldInfo.getOneToOneFields() as field>
<#assign o2oHasCascade = (assocInfo.getOneToOneCascadeTypes(field)?size > 0)>
<#assign o2oJoinCols = assocInfo.getJoinColumns(field)>
<#assign o2oHasFormula = assocInfo.getFormula(field)??>
<#assign o2oHasChildren = (o2oJoinCols?size > 0) || o2oHasCascade || o2oHasFormula>
            <one-to-one name="${field.getName()}" target-entity="${assocInfo.getTargetEntityName(field)}"<#if assocInfo.getFieldAccessType(field)??> access="${assocInfo.getFieldAccessType(field)}"</#if><#if assocInfo.getOneToOneMappedBy(field)??> mapped-by="${assocInfo.getOneToOneMappedBy(field)}"</#if><#if assocInfo.getOneToOneFetchType(field)??> fetch="${assocInfo.getOneToOneFetchType(field)}"</#if><#if assocInfo.getFetchMode(field)??> fetch-mode="${assocInfo.getFetchMode(field)}"</#if><#if !assocInfo.isOneToOneOptional(field)> optional="false"</#if><#if assocInfo.isOneToOneOrphanRemoval(field)> orphan-removal="true"</#if><#if assocInfo.getNotFoundAction(field)??> not-found="${assocInfo.getNotFoundAction(field)}"</#if><#if o2oHasChildren>>
<#if o2oHasCascade>
<#assign cascadeTypes = assocInfo.getOneToOneCascadeTypes(field)>
<#include "cascade.mapping.ftl"/>
</#if>
<#if o2oHasFormula>
                <formula>${assocInfo.getFormula(field)}</formula>
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
<#assign m2mHasCascade = (assocInfo.getManyToManyCascadeTypes(field)?size > 0)>
<#assign m2mHasOrderBy = assocInfo.getOrderBy(field)??>
<#assign m2mHasOrderCol = assocInfo.getOrderColumnName(field)??>
<#assign m2mHasFilters = (queries.getCollectionFilters(field)?size > 0)>
<#assign m2mHasMapKey = assocInfo.getMapKeyName(field)?? || assocInfo.getMapKeyColumnName(field)?? || assocInfo.getMapKeyJoinColumnName(field)??>
<#assign m2mHasSort = assocInfo.isSortNatural(field) || assocInfo.getSortComparatorClass(field)??>
<#assign m2mHasChildren = assocInfo.getJoinTableName(field)?? || m2mHasCascade || m2mHasOrderBy || m2mHasOrderCol || m2mHasFilters || m2mHasMapKey || m2mHasSort>
            <many-to-many name="${field.getName()}" target-entity="${assocInfo.getManyToManyTargetEntity(field)}"<#if assocInfo.getFieldAccessType(field)??> access="${assocInfo.getFieldAccessType(field)}"</#if><#if assocInfo.getManyToManyMappedBy(field)??> mapped-by="${assocInfo.getManyToManyMappedBy(field)}"</#if><#if assocInfo.getManyToManyFetchType(field)??> fetch="${assocInfo.getManyToManyFetchType(field)}"</#if><#if assocInfo.getFetchMode(field)??> fetch-mode="${assocInfo.getFetchMode(field)}"</#if><#if m2mHasChildren>>
<#if assocInfo.getMapKeyName(field)??>
                <map-key name="${assocInfo.getMapKeyName(field)}"/>
</#if>
<#if assocInfo.getMapKeyColumnName(field)??>
                <map-key-column name="${assocInfo.getMapKeyColumnName(field)}"/>
</#if>
<#if assocInfo.getMapKeyJoinColumnName(field)??>
                <map-key-join-column name="${assocInfo.getMapKeyJoinColumnName(field)}"/>
</#if>
<#if m2mHasOrderBy>
                <order-by>${assocInfo.getOrderBy(field)}</order-by>
</#if>
<#if m2mHasOrderCol>
                <order-column name="${assocInfo.getOrderColumnName(field)}"/>
</#if>
<#if m2mHasCascade>
<#assign cascadeTypes = assocInfo.getManyToManyCascadeTypes(field)>
<#include "cascade.mapping.ftl"/>
</#if>
<#if assocInfo.getJoinTableName(field)??>
                <join-table name="${assocInfo.getJoinTableName(field)}"<#if assocInfo.getJoinTableSchema(field)??> schema="${assocInfo.getJoinTableSchema(field)}"</#if><#if assocInfo.getJoinTableCatalog(field)??> catalog="${assocInfo.getJoinTableCatalog(field)}"</#if>>
<#list assocInfo.getJoinTableJoinColumnNames(field) as colName>
                    <join-column name="${colName}"/>
</#list>
<#list assocInfo.getJoinTableInverseJoinColumnNames(field) as colName>
                    <inverse-join-column name="${colName}"/>
</#list>
                </join-table>
</#if>
<#list queries.getCollectionFilters(field) as fi>
                <filter name="${fi.name()}"<#if fi.condition()?has_content> condition="${fi.condition()}"</#if>/>
</#list>
<#if assocInfo.isSortNatural(field)>
                <sort-natural/>
</#if>
<#if assocInfo.getSortComparatorClass(field)??>
                <sort-comparator class="${assocInfo.getSortComparatorClass(field)}"/>
</#if>
            </many-to-many>
<#else/>/>
</#if>
</#list>
<#-- Element collections -->
<#list fieldInfo.getElementCollectionFields() as field>
            <element-collection name="${field.getName()}"<#if assocInfo.getFieldAccessType(field)??> access="${assocInfo.getFieldAccessType(field)}"</#if><#if assocInfo.getElementCollectionTargetClass(field)??> target-class="${assocInfo.getElementCollectionTargetClass(field)}"</#if>>
<#if assocInfo.getOrderBy(field)??>
                <order-by>${assocInfo.getOrderBy(field)}</order-by>
</#if>
<#if assocInfo.getOrderColumnName(field)??>
                <order-column name="${assocInfo.getOrderColumnName(field)}"/>
</#if>
<#if assocInfo.getElementCollectionColumnName(field)??>
                <column name="${assocInfo.getElementCollectionColumnName(field)}"/>
</#if>
<#if assocInfo.getElementCollectionTableName(field)??>
                <collection-table name="${assocInfo.getElementCollectionTableName(field)}">
<#if assocInfo.getElementCollectionKeyColumnName(field)??>
                    <join-column name="${assocInfo.getElementCollectionKeyColumnName(field)}"/>
</#if>
                </collection-table>
</#if>
            </element-collection>
</#list>
<#-- Embedded fields -->
<#list fieldInfo.getEmbeddedFields() as field>
            <embedded name="${field.getName()}"<#if assocInfo.getFieldAccessType(field)??> access="${assocInfo.getFieldAccessType(field)}"</#if>>
<#list assocInfo.getAttributeOverrides(field) as ao>
                <attribute-override name="${ao.fieldName()}">
                    <column name="${ao.columnName()}"/>
                </attribute-override>
</#list>
            </embedded>
</#list>
<#-- Any fields -->
<#list fieldInfo.getAnyFields() as field>
            <any name="${field.getName()}"<#if assocInfo.getFieldAccessType(field)??> access="${assocInfo.getFieldAccessType(field)}"</#if>>
                <discriminator>
<#list assocInfo.getAnyDiscriminatorMappings(field) as mapping>
                    <mapping value="${mapping.value()}">${mapping.entityClass()}</mapping>
</#list>
                </discriminator>
                <key>
                    <java-class>${assocInfo.getAnyKeyType(field)}</java-class>
<#if assocInfo.getJoinColumnName(field)??>
                    <column name="${assocInfo.getJoinColumnName(field)}"/>
</#if>
                </key>
            </any>
</#list>
<#-- Many-to-any fields -->
<#list fieldInfo.getManyToAnyFields() as field>
            <many-to-any name="${field.getName()}"<#if assocInfo.getFieldAccessType(field)??> access="${assocInfo.getFieldAccessType(field)}"</#if>>
                <discriminator>
<#list assocInfo.getAnyDiscriminatorMappings(field) as mapping>
                    <mapping value="${mapping.value()}">${mapping.entityClass()}</mapping>
</#list>
                </discriminator>
                <key>
                    <java-class>${assocInfo.getAnyKeyType(field)}</java-class>
                </key>
<#if assocInfo.getJoinTableName(field)??>
                <join-table name="${assocInfo.getJoinTableName(field)}"<#if assocInfo.getJoinTableSchema(field)??> schema="${assocInfo.getJoinTableSchema(field)}"</#if><#if assocInfo.getJoinTableCatalog(field)??> catalog="${assocInfo.getJoinTableCatalog(field)}"</#if>>
<#if assocInfo.getJoinTableJoinColumnName(field)??>
                    <join-column name="${assocInfo.getJoinTableJoinColumnName(field)}"/>
</#if>
<#if assocInfo.getJoinTableInverseJoinColumnName(field)??>
                    <inverse-join-column name="${assocInfo.getJoinTableInverseJoinColumnName(field)}"/>
</#if>
                </join-table>
</#if>
            </many-to-any>
</#list>
        </attributes>
