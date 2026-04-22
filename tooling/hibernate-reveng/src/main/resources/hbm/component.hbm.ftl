<#-- Components (embedded) -->
<#list helper.getEmbeddedFields() as field>
    <component name="${field.getName()}" class="${helper.getEmbeddableClassName(field)}"<#if helper.getAccessType(field)??> access="${helper.getAccessType(field)}"</#if>>
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
<#-- Dynamic components -->
<#list helper.getDynamicComponentFields() as field>
    <dynamic-component name="${field.getName()}">
<#list helper.getDynamicComponentProperties(field) as prop>
        <property name="${prop.name()}" type="${prop.type()}"/>
</#list>
    </dynamic-component>
</#list>
<#-- Any -->
<#list helper.getAnyFields() as field>
    <any name="${field.getName()}"
        id-type="${helper.getAnyIdType(field)}"
        meta-type="${helper.getAnyMetaType(field)}"<#if helper.getAccessType(field)??>
        access="${helper.getAccessType(field)}"</#if><#if helper.getAnyCascadeString(field)??>
        cascade="${helper.getAnyCascadeString(field)}"</#if>>
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
        table="${helper.getElementCollectionTableName(field)}"</#if><#if helper.getElementCollectionTableSchema(field)??>
        schema="${helper.getElementCollectionTableSchema(field)}"</#if><#if helper.getElementCollectionTableCatalog(field)??>
        catalog="${helper.getElementCollectionTableCatalog(field)}"</#if><#if helper.getCollectionCascadeString(field)??>
        cascade="${helper.getCollectionCascadeString(field)}"</#if><#if helper.getCollectionLazy(field)??>
        lazy="${helper.getCollectionLazy(field)}"</#if><#if helper.getCollectionFetchMode(field)??>
        fetch="${helper.getCollectionFetchMode(field)}"</#if><#if helper.getCollectionOrderBy(field)??>
        order-by="${helper.getCollectionOrderBy(field)}"</#if><#if helper.getSort(field)??>
        sort="${helper.getSort(field)}"</#if><#if helper.getAccessType(field)??>
        access="${helper.getAccessType(field)}"</#if>>
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
<#if collTag == "map">
<#if helper.hasMapKeyJoinColumn(field)>
        <map-key-many-to-many class="${helper.getMapKeyEntityClass(field)}" column="${helper.getMapKeyJoinColumnName(field)}"/>
<#else>
        <map-key column="${helper.getMapKeyColumnName(field)!field.getName() + '_KEY'}" type="${helper.getMapKeyType(field)!'string'}"/>
</#if>
</#if>
<#if helper.isElementCollectionOfEmbeddable(field)>
        <composite-element class="${helper.getCollectionElementType(field)}">
<#list helper.getCompositeElementProperties(field) as prop>
            <property name="${prop.getName()}"<#if helper.getHibernateTypeName(prop)??> type="${helper.getHibernateTypeName(prop)}"</#if>>
                <column name="${helper.getColumnName(prop)}"/>
            </property>
</#list>
<#list helper.getCompositeElementEmbeddeds(field) as embField>
            <nested-composite-element name="${embField.getName()}" class="${helper.getEmbeddableClassName(embField)}">
<#list helper.getCompositeElementProperties(embField) as nestedProp>
                <property name="${nestedProp.getName()}"<#if helper.getHibernateTypeName(nestedProp)??> type="${helper.getHibernateTypeName(nestedProp)}"</#if>>
                    <column name="${helper.getColumnName(nestedProp)}"/>
                </property>
</#list>
            </nested-composite-element>
</#list>
<#list helper.getCompositeElementManyToOnes(field) as m2oField>
            <many-to-one name="${m2oField.getName()}" class="${helper.getTargetEntityName(m2oField)}"<#if helper.getManyToOneCascadeString(m2oField)??> cascade="${helper.getManyToOneCascadeString(m2oField)}"</#if>/>
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
    <${collTag} name="${field.getName()}"<#if helper.getJoinTableName(field)??> table="${helper.getJoinTableName(field)}"</#if><#if helper.getCollectionCascadeString(field)??>
        cascade="${helper.getCollectionCascadeString(field)}"</#if><#if helper.getCollectionLazy(field)??>
        lazy="${helper.getCollectionLazy(field)}"</#if><#if helper.getCollectionFetchMode(field)??>
        fetch="${helper.getCollectionFetchMode(field)}"</#if><#if helper.getAccessType(field)??>
        access="${helper.getAccessType(field)}"</#if>>
<#list helper.getFieldMetaAttributes(field)?keys as metaName>
<#list helper.getFieldMetaAttribute(field, metaName) as metaValue>
        <meta attribute="${metaName}">${metaValue}</meta>
</#list>
</#list>
        <key>
<#list helper.getKeyColumnNames(field) as keyCol>
            <column name="${keyCol}"/>
</#list>
        </key>
<#if collTag == "map">
<#if helper.hasMapKeyJoinColumn(field)>
        <map-key-many-to-many class="${helper.getMapKeyEntityClass(field)}" column="${helper.getMapKeyJoinColumnName(field)}"/>
<#else>
        <map-key column="${helper.getMapKeyColumnName(field)!field.getName() + '_KEY'}" type="${helper.getMapKeyType(field)!'string'}"/>
</#if>
</#if>
        <many-to-any id-type="${helper.getAnyIdType(field)}" meta-type="${helper.getAnyMetaType(field)}">
<#list helper.getAnyMetaValues(field) as mv>
            <meta-value value="${mv.value()}" class="${mv.entityClass()}"/>
</#list>
<#if helper.getColumnName(field)??>
            <column name="${helper.getColumnName(field)}"/>
</#if>
<#if helper.getManyToAnyFkColumnName(field)??>
            <column name="${helper.getManyToAnyFkColumnName(field)}"/>
</#if>
        </many-to-any>
    </${collTag}>
</#list>
<#-- Properties groups -->
<#list helper.getPropertiesGroups() as group>
    <properties name="${group.name()}"<#if group.unique()>
        unique="true"</#if><#if !group.insert()>
        insert="false"</#if><#if !group.update()>
        update="false"</#if><#if !group.optimisticLock()>
        optimistic-lock="false"</#if>>
<#list group.fields() as field>
<#if helper.isBasicField(field)>
        <property name="${field.getName()}"<#if !helper.hasTypeParameters(field)>
            type="${helper.getHibernateTypeName(field)}"</#if><#if !helper.isPropertyUpdatable(field)>
            update="false"</#if><#if !helper.isPropertyInsertable(field)>
            insert="false"</#if><#if helper.getFormula(field)??>
            formula="${helper.getFormula(field)}"</#if>>
<#if helper.hasTypeParameters(field)>
            <type name="${helper.getHibernateTypeName(field)}">
<#list helper.getTypeParameters(field)?keys as paramName>
                <param name="${paramName}">${helper.getTypeParameters(field)[paramName]}</param>
</#list>
            </type>
</#if>
<#if !helper.getFormula(field)??>
            <column name="${helper.getColumnName(field)}" ${helper.getColumnAttributes(field)}/>
</#if>
        </property>
<#elseif helper.isManyToOneField(field)>
        <many-to-one
            name="${field.getName()}"
<#if helper.isManyToOneEntityNameRef(field)>
            entity-name="${helper.getManyToOneEntityName(field)}"<#else>
            class="${helper.getTargetEntityName(field)}"</#if><#if !helper.isManyToOneOptional(field)>
            not-null="true"</#if>>
<#list helper.getJoinColumnNames(field) as colName>
            <column name="${colName}"/>
</#list>
        </many-to-one>
</#if>
</#list>
    </properties>
</#list>
