<#-- Basic properties (skip PK, version, FK columns) -->
<#list table.getColumns() as col>
<#if !col.isPrimaryKey() && !col.isVersion() && !table.isForeignKeyColumn(col.getColumnName())>
    <property
        name="${col.getFieldName()}"
        type="${helper.getHibernateTypeName(col)}">
        <column name="${col.getColumnName()}" ${helper.getColumnAttributes(col)}/>
    </property>
</#if>
</#list>
<#-- Many-to-one -->
<#list table.getForeignKeys() as fk>
    <many-to-one
        name="${fk.getFieldName()}"
        class="${fk.getTargetEntityPackage()}.${fk.getTargetEntityClassName()}"<#if fk.getFetchType()??><#if fk.getFetchType().name() == "LAZY">
        fetch="select"
        lazy="proxy"<#else>
        fetch="join"</#if></#if><#if !fk.isOptional()>
        not-null="true"</#if>>
        <column name="${fk.getForeignKeyColumnName()}"/>
    </many-to-one>
</#list>
<#-- One-to-one -->
<#list table.getOneToOnes() as o2o>
    <one-to-one
        name="${o2o.getFieldName()}"
        class="${o2o.getTargetEntityPackage()}.${o2o.getTargetEntityClassName()}"<#if o2o.getMappedBy()??>
        property-ref="${o2o.getMappedBy()}"</#if><#if o2o.getCascadeTypes()?? && (o2o.getCascadeTypes()?size gt 0)>
        cascade="${helper.getCascadeString(o2o.getCascadeTypes())}"</#if><#if o2o.getForeignKeyColumnName()??>
        constrained="true"</#if>/>
</#list>
<#-- Sets (one-to-many) -->
<#list table.getOneToManys() as o2m>
    <set name="${o2m.getFieldName()}"
        inverse="true"<#if o2m.getCascadeTypes()?? && (o2m.getCascadeTypes()?size gt 0)>
        cascade="${helper.getCascadeString(o2m.getCascadeTypes())}"</#if><#if o2m.getFetchType()??><#if o2m.getFetchType().name() == "EAGER">
        lazy="false"</#if></#if>>
        <key>
            <column name="${o2m.getMappedBy()}"/>
        </key>
        <one-to-many class="${o2m.getElementEntityPackage()}.${o2m.getElementEntityClassName()}"/>
    </set>
</#list>
<#-- Sets (many-to-many) -->
<#list table.getManyToManys() as m2m>
<#if m2m.getJoinTableName()??>
    <set name="${m2m.getFieldName()}"
        table="${m2m.getJoinTableName()}"<#if m2m.getCascadeTypes()?? && (m2m.getCascadeTypes()?size gt 0)>
        cascade="${helper.getCascadeString(m2m.getCascadeTypes())}"</#if>>
        <key>
            <column name="${m2m.getJoinColumnName()}"/>
        </key>
        <many-to-many class="${m2m.getTargetEntityPackage()}.${m2m.getTargetEntityClassName()}">
            <column name="${m2m.getInverseJoinColumnName()}"/>
        </many-to-many>
    </set>
<#else>
    <set name="${m2m.getFieldName()}"
        inverse="true"<#if m2m.getCascadeTypes()?? && (m2m.getCascadeTypes()?size gt 0)>
        cascade="${helper.getCascadeString(m2m.getCascadeTypes())}"</#if>>
        <key>
            <column name="${m2m.getFieldName()}"/>
        </key>
        <many-to-many class="${m2m.getTargetEntityPackage()}.${m2m.getTargetEntityClassName()}"/>
    </set>
</#if>
</#list>
<#-- Components (embedded) -->
<#list table.getEmbeddedFields() as emb>
    <component name="${emb.getFieldName()}" class="${emb.getEmbeddablePackage()}.${emb.getEmbeddableClassName()}">
<#list emb.getAttributeOverrides() as ao>
        <property name="${ao.getFieldName()}">
            <column name="${ao.getColumnName()}"/>
        </property>
</#list>
    </component>
</#list>
