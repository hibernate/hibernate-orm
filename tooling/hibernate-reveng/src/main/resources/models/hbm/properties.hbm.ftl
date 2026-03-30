<#-- Basic properties (skip PK, version, FK columns) -->
<#list helper.getBasicFields() as field>
    <property
        name="${field.getName()}"
        type="${helper.getHibernateTypeName(field)}">
        <column name="${helper.getColumnName(field)}" ${helper.getColumnAttributes(field)}/>
    </property>
</#list>
<#-- Many-to-one -->
<#list helper.getManyToOneFields() as field>
    <many-to-one
        name="${field.getName()}"
        class="${helper.getTargetEntityName(field)}"<#if helper.isManyToOneLazy(field)>
        fetch="select"
        lazy="proxy"</#if><#if !helper.isManyToOneOptional(field)>
        not-null="true"</#if>>
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
<#-- Sets (one-to-many) -->
<#list helper.getOneToManyFields() as field>
    <set name="${field.getName()}"
        inverse="true"<#if helper.getOneToManyCascadeString(field)??>
        cascade="${helper.getOneToManyCascadeString(field)}"</#if><#if helper.isOneToManyEager(field)>
        lazy="false"</#if>>
        <key>
            <column name="${helper.getOneToManyMappedBy(field)}"/>
        </key>
        <one-to-many class="${helper.getOneToManyTargetEntity(field)}"/>
    </set>
</#list>
<#-- Sets (many-to-many) -->
<#list helper.getManyToManyFields() as field>
<#if helper.getJoinTableName(field)??>
    <set name="${field.getName()}"
        table="${helper.getJoinTableName(field)}"<#if helper.getManyToManyCascadeString(field)??>
        cascade="${helper.getManyToManyCascadeString(field)}"</#if>>
        <key>
            <column name="${helper.getJoinTableJoinColumnName(field)}"/>
        </key>
        <many-to-many class="${helper.getManyToManyTargetEntity(field)}">
            <column name="${helper.getJoinTableInverseJoinColumnName(field)}"/>
        </many-to-many>
    </set>
<#else>
    <set name="${field.getName()}"
        inverse="true"<#if helper.getManyToManyCascadeString(field)??>
        cascade="${helper.getManyToManyCascadeString(field)}"</#if>>
        <key>
            <column name="${field.getName()}"/>
        </key>
        <many-to-many class="${helper.getManyToManyTargetEntity(field)}"/>
    </set>
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
