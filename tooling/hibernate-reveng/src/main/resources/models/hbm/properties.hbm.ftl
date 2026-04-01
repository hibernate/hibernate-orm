<#-- Basic properties (skip PK, version, FK columns) -->
<#list helper.getBasicFields() as field>
    <property
        name="${field.getName()}"
        type="${helper.getHibernateTypeName(field)}"<#if helper.getAccessType(field)??>
        access="${helper.getAccessType(field)}"</#if><#if !helper.isPropertyUpdatable(field)>
        update="false"</#if><#if !helper.isPropertyInsertable(field)>
        insert="false"</#if><#if helper.isPropertyLazy(field)>
        lazy="true"</#if><#if helper.isOptimisticLockExcluded(field)>
        optimistic-lock="false"</#if><#if helper.getFormula(field)??>
        formula="${helper.getFormula(field)}"</#if>>
<#list helper.getFieldMetaAttributes(field)?keys as metaName>
<#list helper.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
<#if !helper.getFormula(field)??>
        <column name="${helper.getColumnName(field)}" ${helper.getColumnAttributes(field)}/>
</#if>
    </property>
</#list>
<#-- Many-to-one -->
<#list helper.getManyToOneFields() as field>
    <many-to-one
        name="${field.getName()}"
        class="${helper.getTargetEntityName(field)}"<#if helper.isManyToOneLazy(field)>
        fetch="select"
        lazy="proxy"</#if><#if helper.getFetchMode(field)??>
        fetch="${helper.getFetchMode(field)}"</#if><#if !helper.isManyToOneOptional(field)>
        not-null="true"</#if><#if helper.getAccessType(field)??>
        access="${helper.getAccessType(field)}"</#if><#if helper.getNotFoundAction(field)??>
        not-found="${helper.getNotFoundAction(field)}"</#if>>
<#list helper.getFieldMetaAttributes(field)?keys as metaName>
<#list helper.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
        <column name="${helper.getJoinColumnName(field)}"/>
    </many-to-one>
</#list>
<#-- One-to-one -->
<#list helper.getOneToOneFields() as field>
<#if (helper.getFieldMetaAttributes(field)?size == 0)>
    <one-to-one
        name="${field.getName()}"
        class="${helper.getTargetEntityName(field)}"<#if helper.getOneToOneMappedBy(field)??>
        property-ref="${helper.getOneToOneMappedBy(field)}"</#if><#if helper.getOneToOneCascadeString(field)??>
        cascade="${helper.getOneToOneCascadeString(field)}"</#if><#if helper.isOneToOneConstrained(field)>
        constrained="true"</#if>/>
<#else>
    <one-to-one
        name="${field.getName()}"
        class="${helper.getTargetEntityName(field)}"<#if helper.getOneToOneMappedBy(field)??>
        property-ref="${helper.getOneToOneMappedBy(field)}"</#if><#if helper.getOneToOneCascadeString(field)??>
        cascade="${helper.getOneToOneCascadeString(field)}"</#if><#if helper.isOneToOneConstrained(field)>
        constrained="true"</#if>>
<#list helper.getFieldMetaAttributes(field)?keys as metaName>
<#list helper.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
    </one-to-one>
</#if>
</#list>
<#-- Collections (one-to-many) -->
<#list helper.getOneToManyFields() as field>
<#assign collTag = helper.getCollectionTag(field)>
    <${collTag} name="${field.getName()}"<#if helper.isCollectionInverse(field)>
        inverse="true"</#if><#if helper.getCollectionCascadeString(field)??>
        cascade="${helper.getCollectionCascadeString(field)}"</#if><#if helper.getCollectionLazy(field)??>
        lazy="${helper.getCollectionLazy(field)}"</#if><#if helper.getCollectionFetchMode(field)??>
        fetch="${helper.getCollectionFetchMode(field)}"</#if><#if (helper.getCollectionBatchSize(field) gt 1)>
        batch-size="${helper.getCollectionBatchSize(field)?c}"</#if><#if helper.getCollectionOrderBy(field)??>
        order-by="${helper.getCollectionOrderBy(field)}"</#if><#if helper.getSort(field)??>
        sort="${helper.getSort(field)}"</#if>>
<#list helper.getFieldMetaAttributes(field)?keys as metaName>
<#list helper.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
<#if helper.getCollectionCacheUsage(field)??>
        <cache usage="${helper.getCollectionCacheUsage(field)}"<#if helper.getCollectionCacheRegion(field)??> region="${helper.getCollectionCacheRegion(field)}"</#if>/>
</#if>
        <key>
            <column name="${helper.getOneToManyMappedBy(field)}"/>
        </key>
<#if collTag == "list">
        <list-index column="${helper.getListIndexColumnName(field)}"/>
</#if>
<#if collTag == "map">
        <map-key column="${helper.getMapKeyColumnName(field)!field.getName() + '_KEY'}" type="${helper.getMapKeyType(field)!'string'}"/>
</#if>
        <one-to-many class="${helper.getOneToManyTargetEntity(field)}"/>
<#list helper.getCollectionFilters(field) as fi>
        <filter name="${fi.name()}"<#if fi.condition()?has_content> condition="${fi.condition()}"</#if>/>
</#list>
<#if helper.getCollectionSQLInsert(field)??>
        <sql-insert<#if helper.getCollectionSQLInsert(field).callable()> callable="true"</#if>>${helper.getCollectionSQLInsert(field).sql()}</sql-insert>
</#if>
<#if helper.getCollectionSQLUpdate(field)??>
        <sql-update<#if helper.getCollectionSQLUpdate(field).callable()> callable="true"</#if>>${helper.getCollectionSQLUpdate(field).sql()}</sql-update>
</#if>
<#if helper.getCollectionSQLDelete(field)??>
        <sql-delete<#if helper.getCollectionSQLDelete(field).callable()> callable="true"</#if>>${helper.getCollectionSQLDelete(field).sql()}</sql-delete>
</#if>
<#if helper.getCollectionSQLDeleteAll(field)??>
        <sql-delete-all<#if helper.getCollectionSQLDeleteAll(field).callable()> callable="true"</#if>>${helper.getCollectionSQLDeleteAll(field).sql()}</sql-delete-all>
</#if>
    </${collTag}>
</#list>
<#-- Collections (many-to-many) -->
<#list helper.getManyToManyFields() as field>
<#assign collTag = helper.getCollectionTag(field)>
<#if helper.getJoinTableName(field)??>
    <${collTag} name="${field.getName()}"
        table="${helper.getJoinTableName(field)}"<#if helper.getCollectionCascadeString(field)??>
        cascade="${helper.getCollectionCascadeString(field)}"</#if><#if helper.getCollectionLazy(field)??>
        lazy="${helper.getCollectionLazy(field)}"</#if><#if helper.getCollectionFetchMode(field)??>
        fetch="${helper.getCollectionFetchMode(field)}"</#if><#if (helper.getCollectionBatchSize(field) gt 1)>
        batch-size="${helper.getCollectionBatchSize(field)?c}"</#if><#if helper.getCollectionOrderBy(field)??>
        order-by="${helper.getCollectionOrderBy(field)}"</#if><#if helper.getSort(field)??>
        sort="${helper.getSort(field)}"</#if>>
<#list helper.getFieldMetaAttributes(field)?keys as metaName>
<#list helper.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
<#if helper.getCollectionCacheUsage(field)??>
        <cache usage="${helper.getCollectionCacheUsage(field)}"<#if helper.getCollectionCacheRegion(field)??> region="${helper.getCollectionCacheRegion(field)}"</#if>/>
</#if>
        <key>
            <column name="${helper.getJoinTableJoinColumnName(field)}"/>
        </key>
<#if collTag == "list">
        <list-index column="${helper.getListIndexColumnName(field)}"/>
</#if>
<#if collTag == "map">
        <map-key column="${helper.getMapKeyColumnName(field)!field.getName() + '_KEY'}" type="${helper.getMapKeyType(field)!'string'}"/>
</#if>
<#if collTag == "idbag">
        <collection-id column="${helper.getCollectionIdColumnName(field)}" type="long">
            <generator class="${helper.getCollectionIdGenerator(field)!'native'}"/>
        </collection-id>
</#if>
        <many-to-many class="${helper.getManyToManyTargetEntity(field)}">
            <column name="${helper.getJoinTableInverseJoinColumnName(field)}"/>
        </many-to-many>
<#list helper.getCollectionFilters(field) as fi>
        <filter name="${fi.name()}"<#if fi.condition()?has_content> condition="${fi.condition()}"</#if>/>
</#list>
<#if helper.getCollectionSQLInsert(field)??>
        <sql-insert<#if helper.getCollectionSQLInsert(field).callable()> callable="true"</#if>>${helper.getCollectionSQLInsert(field).sql()}</sql-insert>
</#if>
<#if helper.getCollectionSQLUpdate(field)??>
        <sql-update<#if helper.getCollectionSQLUpdate(field).callable()> callable="true"</#if>>${helper.getCollectionSQLUpdate(field).sql()}</sql-update>
</#if>
<#if helper.getCollectionSQLDelete(field)??>
        <sql-delete<#if helper.getCollectionSQLDelete(field).callable()> callable="true"</#if>>${helper.getCollectionSQLDelete(field).sql()}</sql-delete>
</#if>
<#if helper.getCollectionSQLDeleteAll(field)??>
        <sql-delete-all<#if helper.getCollectionSQLDeleteAll(field).callable()> callable="true"</#if>>${helper.getCollectionSQLDeleteAll(field).sql()}</sql-delete-all>
</#if>
    </${collTag}>
<#else>
    <${collTag} name="${field.getName()}"
        inverse="true"<#if helper.getCollectionCascadeString(field)??>
        cascade="${helper.getCollectionCascadeString(field)}"</#if><#if helper.getCollectionLazy(field)??>
        lazy="${helper.getCollectionLazy(field)}"</#if><#if helper.getCollectionFetchMode(field)??>
        fetch="${helper.getCollectionFetchMode(field)}"</#if>>
<#list helper.getFieldMetaAttributes(field)?keys as metaName>
<#list helper.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
<#if helper.getCollectionCacheUsage(field)??>
        <cache usage="${helper.getCollectionCacheUsage(field)}"<#if helper.getCollectionCacheRegion(field)??> region="${helper.getCollectionCacheRegion(field)}"</#if>/>
</#if>
        <key>
            <column name="${field.getName()}"/>
        </key>
        <many-to-many class="${helper.getManyToManyTargetEntity(field)}"/>
    </${collTag}>
</#if>
</#list>
<#-- Components (embedded) -->
<#list helper.getEmbeddedFields() as field>
    <component name="${field.getName()}" class="${helper.getEmbeddableClassName(field)}">
<#list helper.getFieldMetaAttributes(field)?keys as metaName>
<#list helper.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
<#list helper.getAttributeOverrides(field) as ao>
        <property name="${ao.fieldName()}">
            <column name="${ao.columnName()}"/>
        </property>
</#list>
    </component>
</#list>
<#-- Any -->
<#list helper.getAnyFields() as field>
    <any name="${field.getName()}"
        id-type="${helper.getAnyIdType(field)}"
        meta-type="${helper.getAnyMetaType(field)}"<#if helper.getAccessType(field)??>
        access="${helper.getAccessType(field)}"</#if>>
<#list helper.getFieldMetaAttributes(field)?keys as metaName>
<#list helper.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
<#list helper.getAnyMetaValues(field) as mv>
        <meta-value value="${mv.value()}" class="${mv.entityClass()}"/>
</#list>
        <column name="${helper.getColumnName(field)}"/>
<#if helper.getJoinColumnName(field)??>
        <column name="${helper.getJoinColumnName(field)}"/>
</#if>
    </any>
</#list>
<#-- Element collections -->
<#list helper.getElementCollectionFields() as field>
<#assign collTag = helper.getCollectionTag(field)>
    <${collTag} name="${field.getName()}"<#if helper.getElementCollectionTableName(field)??>
        table="${helper.getElementCollectionTableName(field)}"</#if><#if helper.getCollectionLazy(field)??>
        lazy="${helper.getCollectionLazy(field)}"</#if><#if helper.getCollectionFetchMode(field)??>
        fetch="${helper.getCollectionFetchMode(field)}"</#if><#if helper.getCollectionOrderBy(field)??>
        order-by="${helper.getCollectionOrderBy(field)}"</#if>>
<#list helper.getFieldMetaAttributes(field)?keys as metaName>
<#list helper.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
        <key>
<#if helper.getElementCollectionKeyColumnName(field)??>
            <column name="${helper.getElementCollectionKeyColumnName(field)}"/>
</#if>
        </key>
<#if collTag == "list">
        <list-index column="${helper.getListIndexColumnName(field)!'POSITION'}"/>
</#if>
<#if helper.isElementCollectionOfEmbeddable(field)>
        <composite-element class="${helper.getCollectionElementType(field)}">
<#list helper.getAttributeOverrides(field) as ao>
            <property name="${ao.fieldName()}">
                <column name="${ao.columnName()}"/>
            </property>
</#list>
        </composite-element>
<#else>
        <element type="${helper.getElementCollectionElementType(field)!'string'}">
<#if helper.getElementCollectionElementColumnName(field)??>
            <column name="${helper.getElementCollectionElementColumnName(field)}"/>
</#if>
        </element>
</#if>
    </${collTag}>
</#list>
<#-- ManyToAny -->
<#list helper.getManyToAnyFields() as field>
<#assign collTag = helper.getCollectionTag(field)>
    <${collTag} name="${field.getName()}"<#if helper.getJoinTableName(field)??> table="${helper.getJoinTableName(field)}"</#if>>
<#list helper.getFieldMetaAttributes(field)?keys as metaName>
<#list helper.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
        <key>
            <column name="${helper.getJoinTableJoinColumnName(field)}"/>
        </key>
        <many-to-any id-type="${helper.getAnyIdType(field)}" meta-type="${helper.getAnyMetaType(field)}">
<#list helper.getAnyMetaValues(field) as mv>
            <meta-value value="${mv.value()}" class="${mv.entityClass()}"/>
</#list>
<#if helper.getColumnName(field)??>
            <column name="${helper.getColumnName(field)}"/>
</#if>
<#if helper.getJoinColumnName(field)??>
            <column name="${helper.getJoinColumnName(field)}"/>
</#if>
        </many-to-any>
    </${collTag}>
</#list>
