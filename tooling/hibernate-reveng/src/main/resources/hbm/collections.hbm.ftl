<#-- Collections (one-to-many) -->
<#list fields.getOneToManyFields() as field>
<#assign collTag = collections.getCollectionTag(field)>
    <${collTag} name="${field.getName()}"<#if collections.isCollectionInverse(field)>
        inverse="true"</#if><#if collections.getCollectionCascadeString(field)??>
        cascade="${collections.getCollectionCascadeString(field)}"</#if><#if collections.getCollectionLazy(field)??>
        lazy="${collections.getCollectionLazy(field)}"</#if><#if collections.getCollectionFetchMode(field)??>
        fetch="${collections.getCollectionFetchMode(field)}"</#if><#if (collections.getCollectionBatchSize(field) gt 1)>
        batch-size="${collections.getCollectionBatchSize(field)?c}"</#if><#if collections.getCollectionOrderBy(field)??>
        order-by="${collections.getCollectionOrderBy(field)}"</#if><#if collections.getSort(field)??>
        sort="${collections.getSort(field)}"</#if><#if assocAttrs.getArrayElementClass(field)??>
        element-class="${assocAttrs.getArrayElementClass(field)}"</#if><#if fieldAttrs.getAccessType(field)??>
        access="${fieldAttrs.getAccessType(field)}"</#if>>
<#list fieldAttrs.getFieldMetaAttributes(field)?keys as metaName>
<#list fieldAttrs.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
<#if collections.getCollectionCacheUsage(field)??>
        <cache usage="${collections.getCollectionCacheUsage(field)}"<#if collections.getCollectionCacheRegion(field)??> region="${collections.getCollectionCacheRegion(field)}"</#if>/>
</#if>
        <key<#if assocAttrs.getPropertyRef(field)??> property-ref="${assocAttrs.getPropertyRef(field)}"</#if>>
<#list assocAttrs.getKeyColumnNames(field) as keyCol>
            <column name="${keyCol}"/>
</#list>
        </key>
<#if collTag == "list" || collTag == "array">
        <list-index column="${collections.getListIndexColumnName(field)}"/>
</#if>
<#if collTag == "map">
<#if collections.hasMapKeyJoinColumn(field)>
        <map-key-many-to-many class="${collections.getMapKeyEntityClass(field)}" column="${collections.getMapKeyJoinColumnName(field)}"/>
<#else>
        <map-key column="${collections.getMapKeyColumnName(field)!field.getName() + '_KEY'}" type="${collections.getMapKeyType(field)!'string'}"/>
</#if>
</#if>
        <one-to-many class="${assocAttrs.getOneToManyTargetEntity(field)}"/>
<#list collections.getCollectionFilters(field) as fi>
        <filter name="${fi.name()}"<#if fi.condition()?has_content> condition="${fi.condition()}"</#if>/>
</#list>
<#if collections.getCollectionSQLInsert(field)??>
        <sql-insert<#if collections.getCollectionSQLInsert(field).callable()> callable="true"</#if>>${collections.getCollectionSQLInsert(field).sql()}</sql-insert>
</#if>
<#if collections.getCollectionSQLUpdate(field)??>
        <sql-update<#if collections.getCollectionSQLUpdate(field).callable()> callable="true"</#if>>${collections.getCollectionSQLUpdate(field).sql()}</sql-update>
</#if>
<#if collections.getCollectionSQLDelete(field)??>
        <sql-delete<#if collections.getCollectionSQLDelete(field).callable()> callable="true"</#if>>${collections.getCollectionSQLDelete(field).sql()}</sql-delete>
</#if>
<#if collections.getCollectionSQLDeleteAll(field)??>
        <sql-delete-all<#if collections.getCollectionSQLDeleteAll(field).callable()> callable="true"</#if>>${collections.getCollectionSQLDeleteAll(field).sql()}</sql-delete-all>
</#if>
    </${collTag}>
</#list>
<#-- Collections (many-to-many) -->
<#list fields.getManyToManyFields() as field>
<#assign collTag = collections.getCollectionTag(field)>
<#if assocAttrs.hasJoinTable(field)>
    <${collTag} name="${field.getName()}"<#if assocAttrs.getJoinTableName(field)??>
        table="${assocAttrs.getJoinTableName(field)}"</#if><#if assocAttrs.getJoinTableSchema(field)??>
        schema="${assocAttrs.getJoinTableSchema(field)}"</#if><#if assocAttrs.getJoinTableCatalog(field)??>
        catalog="${assocAttrs.getJoinTableCatalog(field)}"</#if><#if collections.isCollectionInverse(field)>
        inverse="true"</#if><#if collections.getCollectionCascadeString(field)??>
        cascade="${collections.getCollectionCascadeString(field)}"</#if><#if collections.getCollectionLazy(field)??>
        lazy="${collections.getCollectionLazy(field)}"</#if><#if collections.getCollectionFetchMode(field)??>
        fetch="${collections.getCollectionFetchMode(field)}"</#if><#if (collections.getCollectionBatchSize(field) gt 1)>
        batch-size="${collections.getCollectionBatchSize(field)?c}"</#if><#if collections.getCollectionOrderBy(field)??>
        order-by="${collections.getCollectionOrderBy(field)}"</#if><#if collections.getSort(field)??>
        sort="${collections.getSort(field)}"</#if><#if fieldAttrs.getAccessType(field)??>
        access="${fieldAttrs.getAccessType(field)}"</#if>>
<#list fieldAttrs.getFieldMetaAttributes(field)?keys as metaName>
<#list fieldAttrs.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
<#if collections.getCollectionCacheUsage(field)??>
        <cache usage="${collections.getCollectionCacheUsage(field)}"<#if collections.getCollectionCacheRegion(field)??> region="${collections.getCollectionCacheRegion(field)}"</#if>/>
</#if>
        <key>
<#list assocAttrs.getJoinTableJoinColumnNames(field) as colName>
            <column name="${colName}"/>
</#list>
        </key>
<#if collTag == "list" || collTag == "array">
        <list-index column="${collections.getListIndexColumnName(field)}"/>
</#if>
<#if collTag == "map">
<#if collections.hasMapKeyJoinColumn(field)>
        <map-key-many-to-many class="${collections.getMapKeyEntityClass(field)}" column="${collections.getMapKeyJoinColumnName(field)}"/>
<#else>
        <map-key column="${collections.getMapKeyColumnName(field)!field.getName() + '_KEY'}" type="${collections.getMapKeyType(field)!'string'}"/>
</#if>
</#if>
<#if collTag == "idbag">
        <collection-id column="${collections.getCollectionIdColumnName(field)}" type="long">
            <generator class="${collections.getCollectionIdGenerator(field)!'native'}"/>
        </collection-id>
</#if>
        <many-to-many<#if assocAttrs.isManyToManyEntityNameRef(field)> entity-name="${assocAttrs.getManyToManyEntityName(field)}"<#else> class="${assocAttrs.getManyToManyTargetEntity(field)}"</#if>>
<#list assocAttrs.getJoinTableInverseJoinColumnNames(field) as colName>
            <column name="${colName}"/>
</#list>
<#list assocAttrs.getManyToManyFormulas(field) as formula>
            <formula>${formula}</formula>
</#list>
        </many-to-many>
<#list collections.getCollectionFilters(field) as fi>
        <filter name="${fi.name()}"<#if fi.condition()?has_content> condition="${fi.condition()}"</#if>/>
</#list>
<#if collections.getCollectionSQLInsert(field)??>
        <sql-insert<#if collections.getCollectionSQLInsert(field).callable()> callable="true"</#if>>${collections.getCollectionSQLInsert(field).sql()}</sql-insert>
</#if>
<#if collections.getCollectionSQLUpdate(field)??>
        <sql-update<#if collections.getCollectionSQLUpdate(field).callable()> callable="true"</#if>>${collections.getCollectionSQLUpdate(field).sql()}</sql-update>
</#if>
<#if collections.getCollectionSQLDelete(field)??>
        <sql-delete<#if collections.getCollectionSQLDelete(field).callable()> callable="true"</#if>>${collections.getCollectionSQLDelete(field).sql()}</sql-delete>
</#if>
<#if collections.getCollectionSQLDeleteAll(field)??>
        <sql-delete-all<#if collections.getCollectionSQLDeleteAll(field).callable()> callable="true"</#if>>${collections.getCollectionSQLDeleteAll(field).sql()}</sql-delete-all>
</#if>
    </${collTag}>
<#else>
    <${collTag} name="${field.getName()}"
        inverse="true"<#if collections.getCollectionCascadeString(field)??>
        cascade="${collections.getCollectionCascadeString(field)}"</#if><#if collections.getCollectionLazy(field)??>
        lazy="${collections.getCollectionLazy(field)}"</#if><#if collections.getCollectionFetchMode(field)??>
        fetch="${collections.getCollectionFetchMode(field)}"</#if><#if fieldAttrs.getAccessType(field)??>
        access="${fieldAttrs.getAccessType(field)}"</#if>>
<#list fieldAttrs.getFieldMetaAttributes(field)?keys as metaName>
<#list fieldAttrs.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
<#if collections.getCollectionCacheUsage(field)??>
        <cache usage="${collections.getCollectionCacheUsage(field)}"<#if collections.getCollectionCacheRegion(field)??> region="${collections.getCollectionCacheRegion(field)}"</#if>/>
</#if>
        <key>
            <column name="${field.getName()}"/>
        </key>
        <many-to-many class="${assocAttrs.getManyToManyTargetEntity(field)}"/>
    </${collTag}>
</#if>
</#list>
