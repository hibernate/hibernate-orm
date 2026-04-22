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
        sort="${helper.getSort(field)}"</#if><#if helper.getArrayElementClass(field)??>
        element-class="${helper.getArrayElementClass(field)}"</#if><#if helper.getAccessType(field)??>
        access="${helper.getAccessType(field)}"</#if>>
<#list helper.getFieldMetaAttributes(field)?keys as metaName>
<#list helper.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
<#if helper.getCollectionCacheUsage(field)??>
        <cache usage="${helper.getCollectionCacheUsage(field)}"<#if helper.getCollectionCacheRegion(field)??> region="${helper.getCollectionCacheRegion(field)}"</#if>/>
</#if>
        <key<#if helper.getPropertyRef(field)??> property-ref="${helper.getPropertyRef(field)}"</#if>>
<#list helper.getKeyColumnNames(field) as keyCol>
            <column name="${keyCol}"/>
</#list>
        </key>
<#if collTag == "list" || collTag == "array">
        <list-index column="${helper.getListIndexColumnName(field)}"/>
</#if>
<#if collTag == "map">
<#if helper.hasMapKeyJoinColumn(field)>
        <map-key-many-to-many class="${helper.getMapKeyEntityClass(field)}" column="${helper.getMapKeyJoinColumnName(field)}"/>
<#else>
        <map-key column="${helper.getMapKeyColumnName(field)!field.getName() + '_KEY'}" type="${helper.getMapKeyType(field)!'string'}"/>
</#if>
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
<#if helper.hasJoinTable(field)>
    <${collTag} name="${field.getName()}"<#if helper.getJoinTableName(field)??>
        table="${helper.getJoinTableName(field)}"</#if><#if helper.getJoinTableSchema(field)??>
        schema="${helper.getJoinTableSchema(field)}"</#if><#if helper.getJoinTableCatalog(field)??>
        catalog="${helper.getJoinTableCatalog(field)}"</#if><#if helper.isCollectionInverse(field)>
        inverse="true"</#if><#if helper.getCollectionCascadeString(field)??>
        cascade="${helper.getCollectionCascadeString(field)}"</#if><#if helper.getCollectionLazy(field)??>
        lazy="${helper.getCollectionLazy(field)}"</#if><#if helper.getCollectionFetchMode(field)??>
        fetch="${helper.getCollectionFetchMode(field)}"</#if><#if (helper.getCollectionBatchSize(field) gt 1)>
        batch-size="${helper.getCollectionBatchSize(field)?c}"</#if><#if helper.getCollectionOrderBy(field)??>
        order-by="${helper.getCollectionOrderBy(field)}"</#if><#if helper.getSort(field)??>
        sort="${helper.getSort(field)}"</#if><#if helper.getAccessType(field)??>
        access="${helper.getAccessType(field)}"</#if>>
<#list helper.getFieldMetaAttributes(field)?keys as metaName>
<#list helper.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
<#if helper.getCollectionCacheUsage(field)??>
        <cache usage="${helper.getCollectionCacheUsage(field)}"<#if helper.getCollectionCacheRegion(field)??> region="${helper.getCollectionCacheRegion(field)}"</#if>/>
</#if>
        <key>
<#list helper.getJoinTableJoinColumnNames(field) as colName>
            <column name="${colName}"/>
</#list>
        </key>
<#if collTag == "list" || collTag == "array">
        <list-index column="${helper.getListIndexColumnName(field)}"/>
</#if>
<#if collTag == "map">
<#if helper.hasMapKeyJoinColumn(field)>
        <map-key-many-to-many class="${helper.getMapKeyEntityClass(field)}" column="${helper.getMapKeyJoinColumnName(field)}"/>
<#else>
        <map-key column="${helper.getMapKeyColumnName(field)!field.getName() + '_KEY'}" type="${helper.getMapKeyType(field)!'string'}"/>
</#if>
</#if>
<#if collTag == "idbag">
        <collection-id column="${helper.getCollectionIdColumnName(field)}" type="long">
            <generator class="${helper.getCollectionIdGenerator(field)!'native'}"/>
        </collection-id>
</#if>
        <many-to-many<#if helper.isManyToManyEntityNameRef(field)> entity-name="${helper.getManyToManyEntityName(field)}"<#else> class="${helper.getManyToManyTargetEntity(field)}"</#if>>
<#list helper.getJoinTableInverseJoinColumnNames(field) as colName>
            <column name="${colName}"/>
</#list>
<#list helper.getManyToManyFormulas(field) as formula>
            <formula>${formula}</formula>
</#list>
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
        fetch="${helper.getCollectionFetchMode(field)}"</#if><#if helper.getAccessType(field)??>
        access="${helper.getAccessType(field)}"</#if>>
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
