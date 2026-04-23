<#-- Components (embedded) -->
<#list fields.getEmbeddedFields() as field>
    <component name="${field.getName()}" class="${fieldAttrs.getEmbeddableClassName(field)}"<#if fieldAttrs.getAccessType(field)??> access="${fieldAttrs.getAccessType(field)}"</#if>>
<#list fieldAttrs.getFieldMetaAttributes(field)?keys as metaName>
<#list fieldAttrs.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
<#list assocAttrs.getAttributeOverrides(field) as ao>
        <property name="${ao.fieldName()}">
            <column name="${ao.columnName()}"/>
        </property>
</#list>
    </component>
</#list>
<#-- Dynamic components -->
<#list fields.getDynamicComponentFields() as field>
    <dynamic-component name="${field.getName()}">
<#list fields.getDynamicComponentProperties(field) as prop>
        <property name="${prop.name()}" type="${prop.type()}"/>
</#list>
    </dynamic-component>
</#list>
<#-- Any -->
<#list fields.getAnyFields() as field>
    <any name="${field.getName()}"
        id-type="${fields.getAnyIdType(field)}"
        meta-type="${fields.getAnyMetaType(field)}"<#if fieldAttrs.getAccessType(field)??>
        access="${fieldAttrs.getAccessType(field)}"</#if><#if fields.getAnyCascadeString(field)??>
        cascade="${fields.getAnyCascadeString(field)}"</#if>>
<#list fieldAttrs.getFieldMetaAttributes(field)?keys as metaName>
<#list fieldAttrs.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
<#list fields.getAnyMetaValues(field) as mv>
        <meta-value value="${mv.value()}" class="${mv.entityClass()}"/>
</#list>
        <column name="${fieldAttrs.getColumnName(field)}"/>
<#if assocAttrs.getJoinColumnName(field)??>
        <column name="${assocAttrs.getJoinColumnName(field)}"/>
</#if>
    </any>
</#list>
<#-- Element collections -->
<#list fields.getElementCollectionFields() as field>
<#assign collTag = collections.getCollectionTag(field)>
    <${collTag} name="${field.getName()}"<#if fields.getElementCollectionTableName(field)??>
        table="${fields.getElementCollectionTableName(field)}"</#if><#if fields.getElementCollectionTableSchema(field)??>
        schema="${fields.getElementCollectionTableSchema(field)}"</#if><#if fields.getElementCollectionTableCatalog(field)??>
        catalog="${fields.getElementCollectionTableCatalog(field)}"</#if><#if collections.getCollectionCascadeString(field)??>
        cascade="${collections.getCollectionCascadeString(field)}"</#if><#if collections.getCollectionLazy(field)??>
        lazy="${collections.getCollectionLazy(field)}"</#if><#if collections.getCollectionFetchMode(field)??>
        fetch="${collections.getCollectionFetchMode(field)}"</#if><#if collections.getCollectionOrderBy(field)??>
        order-by="${collections.getCollectionOrderBy(field)}"</#if><#if collections.getSort(field)??>
        sort="${collections.getSort(field)}"</#if><#if fieldAttrs.getAccessType(field)??>
        access="${fieldAttrs.getAccessType(field)}"</#if>>
<#list fieldAttrs.getFieldMetaAttributes(field)?keys as metaName>
<#list fieldAttrs.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
        <key>
<#if fields.getElementCollectionKeyColumnName(field)??>
            <column name="${fields.getElementCollectionKeyColumnName(field)}"/>
</#if>
        </key>
<#if collTag == "list">
        <list-index column="${collections.getListIndexColumnName(field)!'POSITION'}"/>
</#if>
<#if collTag == "map">
<#if collections.hasMapKeyJoinColumn(field)>
        <map-key-many-to-many class="${collections.getMapKeyEntityClass(field)}" column="${collections.getMapKeyJoinColumnName(field)}"/>
<#else>
        <map-key column="${collections.getMapKeyColumnName(field)!field.getName() + '_KEY'}" type="${collections.getMapKeyType(field)!'string'}"/>
</#if>
</#if>
<#if fields.isElementCollectionOfEmbeddable(field)>
        <composite-element class="${collections.getCollectionElementType(field)}">
<#list fields.getCompositeElementProperties(field) as prop>
            <property name="${prop.getName()}"<#if fieldAttrs.getHibernateTypeName(prop)??> type="${fieldAttrs.getHibernateTypeName(prop)}"</#if>>
                <column name="${fieldAttrs.getColumnName(prop)}"/>
            </property>
</#list>
<#list fields.getCompositeElementEmbeddeds(field) as embField>
            <nested-composite-element name="${embField.getName()}" class="${fieldAttrs.getEmbeddableClassName(embField)}">
<#list fields.getCompositeElementProperties(embField) as nestedProp>
                <property name="${nestedProp.getName()}"<#if fieldAttrs.getHibernateTypeName(nestedProp)??> type="${fieldAttrs.getHibernateTypeName(nestedProp)}"</#if>>
                    <column name="${fieldAttrs.getColumnName(nestedProp)}"/>
                </property>
</#list>
            </nested-composite-element>
</#list>
<#list fields.getCompositeElementManyToOnes(field) as m2oField>
            <many-to-one name="${m2oField.getName()}" class="${fieldAttrs.getTargetEntityName(m2oField)}"<#if assocAttrs.getManyToOneCascadeString(m2oField)??> cascade="${assocAttrs.getManyToOneCascadeString(m2oField)}"</#if>/>
</#list>
        </composite-element>
<#else>
        <element type="${fields.getElementCollectionElementType(field)!'string'}">
<#if fields.getElementCollectionElementColumnName(field)??>
            <column name="${fields.getElementCollectionElementColumnName(field)}"/>
</#if>
        </element>
</#if>
    </${collTag}>
</#list>
<#-- ManyToAny -->
<#list fields.getManyToAnyFields() as field>
<#assign collTag = collections.getCollectionTag(field)>
    <${collTag} name="${field.getName()}"<#if assocAttrs.getJoinTableName(field)??> table="${assocAttrs.getJoinTableName(field)}"</#if><#if collections.getCollectionCascadeString(field)??>
        cascade="${collections.getCollectionCascadeString(field)}"</#if><#if collections.getCollectionLazy(field)??>
        lazy="${collections.getCollectionLazy(field)}"</#if><#if collections.getCollectionFetchMode(field)??>
        fetch="${collections.getCollectionFetchMode(field)}"</#if><#if fieldAttrs.getAccessType(field)??>
        access="${fieldAttrs.getAccessType(field)}"</#if>>
<#list fieldAttrs.getFieldMetaAttributes(field)?keys as metaName>
<#list fieldAttrs.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
        <key>
<#list assocAttrs.getKeyColumnNames(field) as keyCol>
            <column name="${keyCol}"/>
</#list>
        </key>
<#if collTag == "map">
<#if collections.hasMapKeyJoinColumn(field)>
        <map-key-many-to-many class="${collections.getMapKeyEntityClass(field)}" column="${collections.getMapKeyJoinColumnName(field)}"/>
<#else>
        <map-key column="${collections.getMapKeyColumnName(field)!field.getName() + '_KEY'}" type="${collections.getMapKeyType(field)!'string'}"/>
</#if>
</#if>
        <many-to-any id-type="${fields.getAnyIdType(field)}" meta-type="${fields.getAnyMetaType(field)}">
<#list fields.getAnyMetaValues(field) as mv>
            <meta-value value="${mv.value()}" class="${mv.entityClass()}"/>
</#list>
<#if fieldAttrs.getColumnName(field)??>
            <column name="${fieldAttrs.getColumnName(field)}"/>
</#if>
<#if assocAttrs.getManyToAnyFkColumnName(field)??>
            <column name="${assocAttrs.getManyToAnyFkColumnName(field)}"/>
</#if>
        </many-to-any>
    </${collTag}>
</#list>
<#-- Properties groups -->
<#list fields.getPropertiesGroups() as group>
    <properties name="${group.name()}"<#if group.unique()>
        unique="true"</#if><#if !group.insert()>
        insert="false"</#if><#if !group.update()>
        update="false"</#if><#if !group.optimisticLock()>
        optimistic-lock="false"</#if>>
<#list group.fields() as field>
<#if fields.isBasicField(field)>
        <property name="${field.getName()}"<#if !fieldAttrs.hasTypeParameters(field)>
            type="${fieldAttrs.getHibernateTypeName(field)}"</#if><#if !fieldAttrs.isPropertyUpdatable(field)>
            update="false"</#if><#if !fieldAttrs.isPropertyInsertable(field)>
            insert="false"</#if><#if fieldAttrs.getFormula(field)??>
            formula="${fieldAttrs.getFormula(field)}"</#if>>
<#if fieldAttrs.hasTypeParameters(field)>
            <type name="${fieldAttrs.getHibernateTypeName(field)}">
<#list fieldAttrs.getTypeParameters(field)?keys as paramName>
                <param name="${paramName}">${fieldAttrs.getTypeParameters(field)[paramName]}</param>
</#list>
            </type>
</#if>
<#if !fieldAttrs.getFormula(field)??>
            <column name="${fieldAttrs.getColumnName(field)}" ${fieldAttrs.getColumnAttributes(field)}/>
</#if>
        </property>
<#elseif fields.isManyToOneField(field)>
        <many-to-one
            name="${field.getName()}"
<#if assocAttrs.isManyToOneEntityNameRef(field)>
            entity-name="${assocAttrs.getManyToOneEntityName(field)}"<#else>
            class="${fieldAttrs.getTargetEntityName(field)}"</#if><#if !assocAttrs.isManyToOneOptional(field)>
            not-null="true"</#if>>
<#list assocAttrs.getJoinColumnNames(field) as colName>
            <column name="${colName}"/>
</#list>
        </many-to-one>
</#if>
</#list>
    </properties>
</#list>
