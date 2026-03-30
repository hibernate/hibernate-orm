<#-- Basic properties (skip PK, version, FK columns) -->
<#list helper.getBasicFields() as field>
    <property
        name="${field.getName()}"
        type="${helper.getHibernateTypeName(field)}"<#if helper.getAccessType(field)??>
        access="${helper.getAccessType(field)}"</#if><#if helper.getFormula(field)??>
        formula="${helper.getFormula(field)}"</#if>>
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
        <column name="${helper.getJoinColumnName(field)}"/>
    </many-to-one>
</#list>
<#-- One-to-one -->
<#list helper.getOneToOneFields() as field>
    <one-to-one
        name="${field.getName()}"
        class="${helper.getTargetEntityName(field)}"<#if helper.getOneToOneMappedBy(field)??>
        property-ref="${helper.getOneToOneMappedBy(field)}"</#if><#if helper.getOneToOneCascadeString(field)??>
        cascade="${helper.getOneToOneCascadeString(field)}"</#if><#if helper.isOneToOneConstrained(field)>
        constrained="true"</#if>/>
</#list>
<#-- Collections (one-to-many) -->
<#list helper.getOneToManyFields() as field>
<#assign collTag = helper.getCollectionTag(field)>
    <${collTag} name="${field.getName()}"<#if helper.isCollectionInverse(field)>
        inverse="true"</#if><#if helper.getCollectionCascadeString(field)??>
        cascade="${helper.getCollectionCascadeString(field)}"</#if><#if helper.getCollectionLazy(field)??>
        lazy="${helper.getCollectionLazy(field)}"</#if><#if helper.getCollectionFetchMode(field)??>
        fetch="${helper.getCollectionFetchMode(field)}"</#if><#if (helper.getCollectionBatchSize(field) gt 1)>
        batch-size="${helper.getCollectionBatchSize(field)?c}"</#if>>
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
        batch-size="${helper.getCollectionBatchSize(field)?c}"</#if>>
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
    </${collTag}>
<#else>
    <${collTag} name="${field.getName()}"
        inverse="true"<#if helper.getCollectionCascadeString(field)??>
        cascade="${helper.getCollectionCascadeString(field)}"</#if><#if helper.getCollectionLazy(field)??>
        lazy="${helper.getCollectionLazy(field)}"</#if><#if helper.getCollectionFetchMode(field)??>
        fetch="${helper.getCollectionFetchMode(field)}"</#if>>
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
<#list helper.getAnyMetaValues(field) as mv>
        <meta-value value="${mv.value()}" class="${mv.entityClass()}"/>
</#list>
        <column name="${helper.getColumnName(field)}"/>
<#if helper.getJoinColumnName(field)??>
        <column name="${helper.getJoinColumnName(field)}"/>
</#if>
    </any>
</#list>
